package kr.co.cleverchat.domain.crawl.browser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CrawlNativeLibService {
    private static final int MAX_FILES = 200;
    private static final DateTimeFormatter BACKUP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CrawlNativeLibMapper mapper;
    private final CrawlBrowserProperties properties;

    public CrawlNativeLibService(CrawlNativeLibMapper mapper, CrawlBrowserProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<CrawlNativeLibBundle> bundles(Integer limit) {
        return mapper.findRecent(limit == null ? 50 : Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    @Audited(action = "CRAWL_NATIVE_LIB_UPLOAD", targetType = "CRAWL_NATIVE_LIB")
    @RequireRole("ADMIN")
    public CrawlNativeLibBundle upload(
            MultipartFile file,
            String bundleVersion,
            String expectedChecksumSha256,
            String signature,
            Long uploadedBy) {
        ensureUploadEnabled();
        if (file == null || file.isEmpty()) {
            throw validation("업로드할 zip 번들을 선택해 주세요.");
        }
        if (file.getSize() > properties.getNativeMaxBundleBytes()) {
            throw validation("네이티브 라이브러리 번들 용량이 허용값을 초과했습니다.");
        }
        String originalFileName = safeFileName(file.getOriginalFilename());
        if (!originalFileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw validation("zip 번들만 업로드할 수 있습니다.");
        }
        String checksum = checksum(file);
        String expected = normalizeChecksum(expectedChecksumSha256);
        if (!checksum.equalsIgnoreCase(expected)) {
            throw validation("네이티브 라이브러리 번들 체크섬이 일치하지 않습니다.");
        }
        String normalizedSignature = blankToNull(signature);
        if (normalizedSignature == null) {
            throw validation("네이티브 라이브러리 번들 서명 값을 입력해 주세요.");
        }
        if (!trustedSignatures().contains(normalizedSignature)) {
            throw validation("네이티브 라이브러리 번들 서명을 신뢰할 수 없습니다.");
        }
        Set<String> allowedSonames = allowedSonames();
        Path stagingRoot = normalizedDirectory(properties.getNativeStagingPath(), "스테이징 경로");
        Path bundleDir =
                stagingRoot.resolve("bundle-" + UUID.randomUUID()).normalize().toAbsolutePath();
        ensureWithin(stagingRoot, bundleDir);
        try {
            Files.createDirectories(bundleDir);
            ExtractResult extractResult = extractZip(file, bundleDir, allowedSonames);
            if (extractResult.sonames().isEmpty()) {
                throw validation("허용된 SONAME 파일이 번들에 없습니다.");
            }
            CrawlNativeLibBundle bundle = new CrawlNativeLibBundle();
            bundle.setBundleVersion(truncate(required(bundleVersion, "번들 버전을 입력해 주세요."), 100));
            bundle.setOriginalFileName(truncate(originalFileName, 255));
            bundle.setStagingPath(bundleDir.toString());
            bundle.setChecksumSha256(checksum);
            bundle.setSignature(truncate(normalizedSignature, 1000));
            bundle.setAllowedSonames(String.join(",", extractResult.sonames()));
            bundle.setStatus("VERIFIED");
            bundle.setUploadedBy(uploadedBy);
            bundle.setMessage("Uploaded to staging only. Activate separately to use.");
            mapper.insert(bundle);
            return mapper.findById(bundle.getNativeLibRegistryNo());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "네이티브 라이브러리 번들 저장에 실패했습니다.");
        } catch (RuntimeException e) {
            cleanupQuietly(bundleDir);
            throw e;
        }
    }

    @Transactional
    @Audited(action = "CRAWL_NATIVE_LIB_ACTIVATE", targetType = "CRAWL_NATIVE_LIB")
    @RequireRole("ADMIN")
    public CrawlNativeLibBundle activate(Long bundleId, Long activatedBy) {
        ensureUploadEnabled();
        CrawlNativeLibBundle bundle = mapper.findById(bundleId);
        if (bundle == null || !"VERIFIED".equals(bundle.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "적용 가능한 네이티브 라이브러리 번들을 찾을 수 없습니다.");
        }
        Path stagingDir = normalizedDirectory(bundle.getStagingPath(), "스테이징 번들 경로");
        Path activeDir = normalizedDirectory(properties.getNativeActivePath(), "활성 경로");
        if (stagingDir.equals(activeDir) || stagingDir.startsWith(activeDir)) {
            throw validation("스테이징 경로와 활성 경로는 분리되어야 합니다.");
        }
        Path parent = activeDir.getParent();
        if (parent == null) {
            throw validation("활성 경로의 상위 디렉터리를 확인해 주세요.");
        }
        try {
            Files.createDirectories(parent);
            Path nextDir = parent.resolve(activeDir.getFileName() + ".next-" + UUID.randomUUID());
            copyDirectory(stagingDir, nextDir);
            Path backupDir =
                    parent.resolve(
                            activeDir.getFileName()
                                    + ".backup-"
                                    + java.time.LocalDateTime.now().format(BACKUP_FORMAT));
            if (Files.exists(activeDir)) {
                move(activeDir, backupDir);
            }
            try {
                move(nextDir, activeDir);
            } catch (RuntimeException e) {
                if (Files.exists(backupDir) && !Files.exists(activeDir)) {
                    move(backupDir, activeDir);
                }
                throw e;
            }
            cleanupQuietly(backupDir);
            mapper.deactivateOthers(bundleId);
            int updated =
                    mapper.markActive(
                            bundleId,
                            activeDir.toString(),
                            activatedBy,
                            "Activated. New Chromium processes will use this library path.");
            if (updated == 0) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "번들을 적용할 수 없는 상태입니다.");
            }
            return mapper.findById(bundleId);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "네이티브 라이브러리 번들 적용에 실패했습니다.");
        }
    }

    private ExtractResult extractZip(MultipartFile file, Path bundleDir, Set<String> allowedSonames)
            throws IOException {
        List<String> sonames = new ArrayList<>();
        long extractedBytes = 0;
        int files = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                files++;
                if (files > MAX_FILES) {
                    throw validation("네이티브 라이브러리 번들 파일 수가 너무 많습니다.");
                }
                String fileName = safeFileName(entry.getName());
                if (!allowedSonames.contains(fileName)) {
                    throw validation("허용되지 않은 SONAME 파일이 포함되어 있습니다: " + fileName);
                }
                Path target = bundleDir.resolve(fileName).normalize().toAbsolutePath();
                ensureWithin(bundleDir, target);
                extractedBytes += copyEntry(zip, target);
                if (extractedBytes > properties.getNativeMaxBundleBytes()) {
                    throw validation("압축 해제된 번들 용량이 허용값을 초과했습니다.");
                }
                sonames.add(fileName);
            }
        }
        return new ExtractResult(List.copyOf(sonames));
    }

    private long copyEntry(ZipInputStream zip, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        long total = 0;
        byte[] buffer = new byte[8192];
        try (var out = Files.newOutputStream(target)) {
            int read;
            while ((read = zip.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }

    private String checksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream in = file.getInputStream()) {
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "네이티브 라이브러리 번들 체크섬 계산에 실패했습니다.");
        }
    }

    private Set<String> allowedSonames() {
        String value = properties.getNativeAllowedSonames();
        if (value == null || value.isBlank()) {
            throw validation("허용 SONAME 화이트리스트가 설정되지 않았습니다.");
        }
        Set<String> names = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String name = safeFileName(token);
            if (!name.endsWith(".so") && !name.contains(".so.")) {
                throw validation("허용 SONAME 값을 확인해 주세요: " + name);
            }
            names.add(name);
        }
        return names;
    }

    private Set<String> trustedSignatures() {
        String value = properties.getNativeTrustedSignatures();
        if (value == null || value.isBlank()) {
            throw validation("신뢰 서명 목록이 설정되지 않았습니다.");
        }
        Set<String> signatures = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String signature = blankToNull(token);
            if (signature != null) {
                signatures.add(signature);
            }
        }
        if (signatures.isEmpty()) {
            throw validation("신뢰 서명 목록이 설정되지 않았습니다.");
        }
        return signatures;
    }

    private void ensureUploadEnabled() {
        if (!properties.isNativeUploadEnabled()) {
            throw new BusinessException(
                    ErrorCode.STATE_CONFLICT, "네이티브 라이브러리 업로드 기능이 비활성화되어 있습니다.");
        }
    }

    private Path normalizedDirectory(String value, String label) {
        String path = required(value, label + "를 설정해 주세요.");
        Path normalized = Path.of(path).normalize().toAbsolutePath();
        if (Files.exists(normalized) && !Files.isDirectory(normalized)) {
            throw validation(label + "가 디렉터리가 아닙니다.");
        }
        return normalized;
    }

    private void ensureWithin(Path root, Path target) {
        if (!target.normalize().toAbsolutePath().startsWith(root.normalize().toAbsolutePath())) {
            throw validation("경로 조작이 감지되었습니다.");
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path)).normalize();
                ensureWithin(target.toAbsolutePath(), destination.toAbsolutePath());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void move(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioException) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "네이티브 라이브러리 경로 교체에 실패했습니다.");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "네이티브 라이브러리 경로 교체에 실패했습니다.");
        }
    }

    private void cleanupQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path current : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(current);
            }
        } catch (IOException ignored) {
        }
    }

    private String normalizeChecksum(String value) {
        String normalized = required(value, "체크섬을 입력해 주세요.").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw validation("SHA-256 체크섬 형식을 확인해 주세요.");
        }
        return normalized;
    }

    private String safeFileName(String value) {
        String normalized = required(value, "파일명을 확인해 주세요.").replace('\\', '/');
        if (normalized.contains("/") || normalized.contains("..")) {
            throw validation("파일명에 경로를 포함할 수 없습니다.");
        }
        return normalized.trim();
    }

    private String required(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw validation(message);
        }
        return normalized;
    }

    private BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private record ExtractResult(List<String> sonames) {}
}
