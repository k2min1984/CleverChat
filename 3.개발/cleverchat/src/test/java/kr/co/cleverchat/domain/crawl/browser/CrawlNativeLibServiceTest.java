package kr.co.cleverchat.domain.crawl.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class CrawlNativeLibServiceTest {
    private final CrawlNativeLibMapper mapper =
            org.mockito.Mockito.mock(CrawlNativeLibMapper.class);
    private final CrawlBrowserProperties properties = new CrawlBrowserProperties();
    private final CrawlNativeLibService service = new CrawlNativeLibService(mapper, properties);

    @TempDir Path tempDir;

    @Test
    void uploadRejectsWhenFeatureFlagIsOff() {
        assertThatThrownBy(
                        () ->
                                service.upload(
                                        file("libok.so", "native"),
                                        "v1",
                                        sha256(zip("libok.so", "native")),
                                        "sig",
                                        1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void uploadStoresOnlyVerifiedBundleInStaging() throws Exception {
        properties.setNativeUploadEnabled(true);
        properties.setNativeStagingPath(tempDir.resolve("staging").toString());
        properties.setNativeActivePath(tempDir.resolve("active").toString());
        properties.setNativeAllowedSonames("libok.so");
        properties.setNativeTrustedSignatures("signed-by-ops");
        byte[] zip = zip("libok.so", "native");
        when(mapper.findById(9L)).thenAnswer(invocation -> bundle(9L));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlNativeLibBundle bundle = invocation.getArgument(0);
                            bundle.setNativeLibRegistryNo(9L);
                            return null;
                        })
                .when(mapper)
                .insert(any(CrawlNativeLibBundle.class));
        ArgumentCaptor<CrawlNativeLibBundle> captor =
                ArgumentCaptor.forClass(CrawlNativeLibBundle.class);

        service.upload(
                file("bundle.zip", zip), "chromium-libs-1", sha256(zip), "signed-by-ops", 10L);

        verify(mapper).insert(captor.capture());
        CrawlNativeLibBundle stored = captor.getValue();
        assertThat(stored.getStatus()).isEqualTo("VERIFIED");
        assertThat(stored.getActivePath()).isNull();
        assertThat(Files.exists(Path.of(stored.getStagingPath()).resolve("libok.so"))).isTrue();
    }

    @Test
    void uploadRejectsUnexpectedSoname() {
        properties.setNativeUploadEnabled(true);
        properties.setNativeStagingPath(tempDir.resolve("staging").toString());
        properties.setNativeActivePath(tempDir.resolve("active").toString());
        properties.setNativeAllowedSonames("libok.so");
        properties.setNativeTrustedSignatures("signature");
        byte[] zip = zip("libbad.so", "native");

        assertThatThrownBy(
                        () ->
                                service.upload(
                                        file("bundle.zip", zip),
                                        "v1",
                                        sha256(zip),
                                        "signature",
                                        1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void activateCopiesVerifiedStagingBundleToActivePath() throws Exception {
        properties.setNativeUploadEnabled(true);
        properties.setNativeStagingPath(tempDir.resolve("staging").toString());
        properties.setNativeActivePath(tempDir.resolve("active").toString());
        Path staged = tempDir.resolve("staging/bundle-1");
        Files.createDirectories(staged);
        Files.writeString(staged.resolve("libok.so"), "native", StandardCharsets.UTF_8);
        CrawlNativeLibBundle bundle = bundle(7L);
        bundle.setStagingPath(staged.toString());
        when(mapper.findById(7L)).thenReturn(bundle);
        when(mapper.markActive(any(), any(), any(), any())).thenReturn(1);

        service.activate(7L, 10L);

        assertThat(Files.readString(tempDir.resolve("active/libok.so"))).isEqualTo("native");
        verify(mapper)
                .markActive(
                        7L,
                        tempDir.resolve("active").toAbsolutePath().toString(),
                        10L,
                        "Activated. New Chromium processes will use this library path.");
    }

    private CrawlNativeLibBundle bundle(Long id) {
        CrawlNativeLibBundle bundle = new CrawlNativeLibBundle();
        bundle.setNativeLibRegistryNo(id);
        bundle.setBundleVersion("v1");
        bundle.setOriginalFileName("bundle.zip");
        bundle.setChecksumSha256("0".repeat(64));
        bundle.setAllowedSonames("libok.so");
        bundle.setStatus("VERIFIED");
        return bundle;
    }

    private MockMultipartFile file(String name, String content) {
        return file(name, zip(name, content));
    }

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/zip", content);
    }

    private byte[] zip(String fileName, String content) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry(fileName));
                zip.write(content.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
