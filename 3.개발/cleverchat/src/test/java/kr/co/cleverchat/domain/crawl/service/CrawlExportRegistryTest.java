package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import kr.co.cleverchat.domain.settings.MaintenanceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrawlExportRegistryTest {
    @TempDir Path root;
    private final MaintenanceMapper mapper = mock(MaintenanceMapper.class);
    private final CrawlExportRegistry registry = new CrawlExportRegistry(mapper);
    private MaintenanceMapper.ExportFile entry;

    private Path register() throws Exception {
        Path file = root.resolve("target-1/2026-01-01/" + UUID.randomUUID() + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"content\":\"수집 자료\"}");
        doAnswer(
                        call -> {
                            entry =
                                    new MaintenanceMapper.ExportFile(
                                            1L,
                                            call.getArgument(1),
                                            call.getArgument(2),
                                            call.getArgument(3));
                            return null;
                        })
                .when(mapper)
                .register(anyLong(), anyString(), anyString(), anyString());
        registry.register(1L, root, file);
        when(mapper.expired(any())).thenAnswer(call -> List.of(entry));
        return file;
    }

    @Test
    void deletesOnlyRegisteredUnchangedSnapshot() throws Exception {
        Path owned = register();
        Path unrelated = owned.getParent().resolve("manual.json");
        Files.writeString(unrelated, "manual data");
        assertThat(registry.cleanup(OffsetDateTime.now())).contains("정리 1건");
        assertThat(owned).doesNotExist();
        assertThat(unrelated).exists();
        verify(mapper).cleaned(1L);
    }

    @Test
    void leavesFileWhoseContentWasChanged() throws Exception {
        Path file = register();
        Files.writeString(file, "user edited content");
        assertThat(registry.cleanup(OffsetDateTime.now())).contains("건너뜀 1건");
        assertThat(Files.readString(file)).isEqualTo("user edited content");
        verify(mapper, never()).cleaned(anyLong());
    }

    @Test
    void refusesPathOutsideTrackedTargetDirectory() throws Exception {
        register();
        Path unrelated = root.resolve("user.json");
        Files.writeString(unrelated, "owned by user");
        entry =
                new MaintenanceMapper.ExportFile(
                        1L, root.toString(), "user.json", entry.contentSha256());
        assertThat(registry.cleanup(OffsetDateTime.now())).contains("건너뜀 1건");
        assertThat(unrelated).exists();
    }

    @Test
    void missingPreviouslyRegisteredFileIsCompletedWithoutDeletingAnythingElse() throws Exception {
        Path file = register();
        Files.delete(file);
        registry.cleanup(OffsetDateTime.now());
        verify(mapper).cleaned(1L);
    }
}
