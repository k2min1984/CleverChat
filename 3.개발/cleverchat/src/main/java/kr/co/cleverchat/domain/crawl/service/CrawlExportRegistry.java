package kr.co.cleverchat.domain.crawl.service;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import kr.co.cleverchat.domain.settings.MaintenanceMapper;
import org.springframework.stereotype.Service;

@Service
public class CrawlExportRegistry {
    private final MaintenanceMapper mapper;

    public CrawlExportRegistry(MaintenanceMapper mapper) {
        this.mapper = mapper;
    }

    public void register(Long target, Path root, Path file) throws IOException {
        Path realRoot = root.toRealPath();
        Path realFile = file.toRealPath();
        if (!realFile.startsWith(realRoot)) throw new IOException("JSON 파일이 저장 폴더를 벗어났습니다.");
        mapper.register(
                target,
                realRoot.toString(),
                realRoot.relativize(realFile).toString(),
                hash(realFile));
    }

    public String cleanup(OffsetDateTime cutoff) {
        int removed = 0, skipped = 0;
        for (var entry : mapper.expired(cutoff)) {
            try {
                Path root = Path.of(entry.rootPath()).toAbsolutePath().normalize();
                Path relative = Path.of(entry.relativePath());
                if (relative.isAbsolute()
                        || relative.getNameCount() != 3
                        || !relative.getName(0).toString().matches("target-[0-9]+")
                        || !relative.getName(1).toString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                        || !relative.getFileName().toString().matches("[0-9a-f-]{36}\\.json"))
                    throw new IOException("등록 경로가 올바르지 않아 건너뜀");
                Path file = root.resolve(relative).normalize();
                if (!file.startsWith(root)) throw new IOException("저장 범위 밖 경로를 건너뜀");
                Path component = root;
                for (Path part : relative) {
                    component = component.resolve(part);
                    if (Files.isSymbolicLink(component)) throw new IOException("심볼릭 링크 경로를 건너뜀");
                }
                if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                    if (!root.toRealPath().equals(root)
                            || !file.toRealPath().equals(file)
                            || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                            || !hash(file).equals(entry.contentSha256()))
                        throw new IOException("저장 위치 또는 내용이 변경되어 건너뜀");
                    Files.delete(file);
                }
                mapper.cleaned(entry.exportFileNo());
                removed++;
            } catch (IOException | RuntimeException e) {
                mapper.failed(entry.exportFileNo(), "파일을 정리하지 못했습니다. 경로·내용 변경 또는 접근 권한을 확인하세요.");
                skipped++;
            }
        }
        return "JSON 정리 " + removed + "건, 건너뜀 " + skipped + "건";
    }

    private String hash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
