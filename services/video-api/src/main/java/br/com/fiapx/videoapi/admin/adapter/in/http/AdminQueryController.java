package br.com.fiapx.videoapi.admin.adapter.in.http;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public final class AdminQueryController {
    private final JdbcTemplate jdbc;
    public AdminQueryController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/users")
    Page users(@RequestParam(required = false) String status, @RequestParam(required = false) String cursor,
               @RequestParam(defaultValue = "20") int limit) {
        check(limit); var c = Cursor.decode(cursor);
        var sql = "select u.id,u.email,u.status,u.created_at,u.updated_at,coalesce((select string_agg(r.role,',') from user_roles r where r.user_id=u.id),'') roles from users u where (? is null or u.status=?) "
            + "and (? is null or (created_at,id)<(?::timestamptz,?::uuid)) order by created_at desc,id desc limit ?";
        var rows = jdbc.queryForList(sql, status, status, c.time(), c.time(), c.id(), limit);
        return Page.of(rows, limit);
    }

    @GetMapping("/videos")
    Page videos(@RequestParam(required = false) String lifecycle, @RequestParam(required = false) UUID ownerId,
                @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
        check(limit); var c = Cursor.decode(cursor);
        var sql = "select id,user_id,upload_status,original_filename,declared_content_type,size_bytes,created_at,updated_at "
            + "from videos where (? is null or upload_status=?) and (? is null or user_id=?) "
            + "and (? is null or (created_at,id)<(?::timestamptz,?::uuid)) order by created_at desc,id desc limit ?";
        var rows = jdbc.queryForList(sql, lifecycle, lifecycle, ownerId, ownerId, c.time(), c.time(), c.id(), limit);
        return Page.of(rows, limit);
    }

    @GetMapping("/jobs")
    Page jobs(@RequestParam(required = false) String status, @RequestParam(required = false) UUID ownerId,
              @RequestParam(required = false) Instant createdFrom, @RequestParam(required = false) Instant createdTo,
              @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
        check(limit); var c = Cursor.decode(cursor);
        var sql = "select id,user_id,video_id,status,created_at,updated_at from jobs where (? is null or status=?) "
            + "and (? is null or user_id=?) and (? is null or created_at>=?) and (? is null or created_at<?) "
            + "and (? is null or (created_at,id)<(?::timestamptz,?::uuid)) order by created_at desc,id desc limit ?";
        var rows = jdbc.queryForList(sql, status, status, ownerId, ownerId, createdFrom, createdFrom, createdTo, createdTo,
            c.time(), c.time(), c.id(), limit);
        return Page.of(rows, limit);
    }

    private void check(int limit) { if (limit < 1 || limit > 100) throw new IllegalArgumentException("Invalid page size"); }
    record Page(List<Map<String,Object>> items, String nextCursor) {
        static Page of(List<Map<String,Object>> rows, int limit) {
            var next = rows.size() < limit ? null : Cursor.encode(rows.getLast());
            return new Page(rows, next);
        }
    }
    record Cursor(Instant time, UUID id) {
        static Cursor decode(String value) {
            if (value == null) return new Cursor(null, null);
            try { var p = new String(Base64.getUrlDecoder().decode(value)).split("\\|", 2); return new Cursor(Instant.parse(p[0]), UUID.fromString(p[1])); }
            catch (Exception e) { throw new IllegalArgumentException("Invalid cursor"); }
        }
        static String encode(Map<String,Object> row) {
            var value = row.get("created_at");
            var time = value instanceof java.sql.Timestamp t ? t.toInstant() : Instant.parse(value.toString());
            return Base64.getUrlEncoder().withoutPadding().encodeToString((time + "|" + row.get("id")).getBytes());
        }
    }
}
