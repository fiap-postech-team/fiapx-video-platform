package br.com.fiapx.processor;

import com.fasterxml.jackson.databind.*;
import io.minio.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProcessingListener {
    private final ObjectMapper json;
    private final RabbitTemplate rabbit;
    private final MinioClient minio;
    private final String bucket;

    public ProcessingListener(ObjectMapper j, RabbitTemplate r, MinioClient m, @Value("${app.storage.bucket}") String b) {
        json = j;
        rabbit = r;
        minio = m;
        bucket = b;
    }

    @RabbitListener(queues = "video.processing.v1")
    public void process(String body) throws Exception {
        JsonNode event = json.readTree(body);
        String jobId = event.required("jobId").asText();
        String sourceKey = event.required("sourceKey").asText();
        publish("video.job.started.v1", Map.of("eventId", UUID.randomUUID(), "jobId", jobId, "type", "PROCESSING", "occurredAt", java.time.Instant.now().toString(), "correlationId", jobId));
        Path work = Files.createTempDirectory("fiapx-" + jobId + "-");
        try {
            Path video = work.resolve("input");
            minio.downloadObject(DownloadObjectArgs.builder().bucket(bucket).object(sourceKey).filename(video.toString()).build());
            probe(video);
            Path frames = Files.createDirectory(work.resolve("frames"));
            run(List.of("ffmpeg", "-v", "error", "-i", video.toString(), "-vf", "fps=1", frames.resolve("frame-%06d.jpg").toString()));
            Path zip = work.resolve("frames.zip");
            zip(frames, zip);
            String result = "results/" + jobId + "/frames.zip";
            minio.uploadObject(UploadObjectArgs.builder().bucket(bucket).object(result).filename(zip.toString()).contentType("application/zip").build());
            publish("video.job.completed.v1", Map.of("eventId", UUID.randomUUID(), "jobId", jobId, "type", "COMPLETED", "resultKey", result, "occurredAt", java.time.Instant.now().toString(), "correlationId", jobId));
        } catch (Exception ex) {
            publish("video.job.failed.v1", Map.of("eventId", UUID.randomUUID(), "jobId", jobId, "type", "FAILED", "terminal", true, "reason", safe(ex.getMessage()), "occurredAt", java.time.Instant.now().toString(), "correlationId", jobId));
            throw ex;
        } finally {
            delete(work);
        }
    }

    private void probe(Path p) throws Exception {
        run(List.of("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", p.toString()));
    }

    private void run(List<String> command) throws Exception {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes());
        if (p.waitFor() != 0) throw new IOException("Media command failed: " + output);
    }

    private void publish(String key, Object value) throws Exception {
        rabbit.convertAndSend("video.events", key, json.writeValueAsString(value));
    }

    private void zip(Path source, Path target) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target)); var paths = Files.list(source)) {
            for (Path p : paths.toList()) {
                out.putNextEntry(new ZipEntry(p.getFileName().toString()));
                Files.copy(p, out);
                out.closeEntry();
            }
        }
    }

    private void delete(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private String safe(String s) {
        return s == null ? "processing failed" : s.substring(0, Math.min(s.length(), 500));
    }
}
