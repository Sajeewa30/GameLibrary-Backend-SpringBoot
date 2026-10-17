package com.backend.gamelibrarybackend;

import com.backend.gamelibrarybackend.service.DriveBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs a real backup with rclone, using a local folder as the "Drive" remote
 * (rclone treats a plain path as a remote). Skipped when rclone isn't installed.
 */
class DriveBackupServiceTest {

    @TempDir
    Path tmp;

    @Test
    void mirrorsUploadsAndSnapshotsTheDatabase() throws Exception {
        String rclone = System.getenv("RCLONE_TEST_PATH");
        Assumptions.assumeTrue(rclone != null && Files.isRegularFile(Path.of(rclone)),
                "set RCLONE_TEST_PATH to rclone.exe to run this test");

        Path db = tmp.resolve("app.db");
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:sqlite:" + db);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("create table t (v text)");
            st.execute("insert into t values ('hello')");
        }
        Path uploads = tmp.resolve("uploads");
        Files.createDirectories(uploads.resolve("user1"));
        Files.writeString(uploads.resolve("user1/a.jpg"), "a");
        Files.writeString(uploads.resolve("user1/b.jpg"), "b");
        Path drive = tmp.resolve("drive");

        DriveBackupService service = new DriveBackupService(ds, new ObjectMapper(), rclone,
                drive.toString(), uploads.toString(), tmp.resolve("work").toString());

        Map<String, Object> first = runToCompletion(service);
        assertThat(first.get("state")).as(String.valueOf(first)).isEqualTo("success");
        assertThat(drive.resolve("uploads/user1/a.jpg")).hasContent("a");
        assertThat(drive.resolve("gamelibrary.db")).exists();
        assertThat(((Number) first.get("filesUploaded")).longValue()).isEqualTo(2);

        // Second run: one new file, one deleted locally -> only those change on "Drive".
        Files.writeString(uploads.resolve("user1/c.jpg"), "c");
        Files.delete(uploads.resolve("user1/a.jpg"));
        Map<String, Object> second = runToCompletion(service);
        assertThat(second.get("state")).as(String.valueOf(second)).isEqualTo("success");
        assertThat(((Number) second.get("filesUploaded")).longValue()).isEqualTo(1);
        assertThat(((Number) second.get("filesDeleted")).longValue()).isEqualTo(1);
        assertThat(drive.resolve("uploads/user1/a.jpg")).doesNotExist();
        assertThat(drive.resolve("uploads/user1/c.jpg")).hasContent("c");
    }

    private static Map<String, Object> runToCompletion(DriveBackupService service) throws InterruptedException {
        service.start();
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        Map<String, Object> status = service.getStatus();
        while ("running".equals(status.get("state")) && System.nanoTime() < deadline) {
            Thread.sleep(200);
            status = service.getStatus();
        }
        return status;
    }
}
