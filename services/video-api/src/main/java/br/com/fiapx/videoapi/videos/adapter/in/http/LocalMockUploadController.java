package br.com.fiapx.videoapi.videos.adapter.in.http;

import br.com.fiapx.videoapi.videos.adapter.configuration.LocalMockVideoObjectStorage;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_local/mock-storage/uploads")
@ConditionalOnProperty(prefix = "app.video", name = "storage-mode", havingValue = "mock")
public final class LocalMockUploadController {
    private final LocalMockVideoObjectStorage storage;
    public LocalMockUploadController(LocalMockVideoObjectStorage storage) { this.storage = storage; }

    @PutMapping("/{capability}")
    @ResponseStatus(HttpStatus.OK)
    void put(@PathVariable String capability, @RequestHeader("Content-Type") String contentType,
             @RequestHeader("If-None-Match") String ifNoneMatch,
             @RequestHeader(value = "x-amz-checksum-sha256", required = false) String checksum,
             HttpServletRequest request) throws IOException {
        storage.put(capability, contentType, ifNoneMatch, checksum, request.getInputStream());
    }
}
