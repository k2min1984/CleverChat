package kr.co.cleverchat.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MyBatisMapperSqlSafetyTest {

    @Test
    void mapperXmlDoesNotUseStringSubstitution() throws Exception {
        Path mapperRoot = Path.of("src/main/resources/mapper");
        try (var paths = Files.walk(mapperRoot)) {
            List<Path> unsafeMappers =
                    paths.filter(path -> path.toString().endsWith(".xml"))
                            .filter(path -> containsStringSubstitution(path))
                            .toList();

            assertThat(unsafeMappers)
                    .as(
                            "MyBatis mapper XML must use bound parameters instead of ${} string substitution.")
                    .isEmpty();
        }
    }

    private boolean containsStringSubstitution(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).contains("${");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read mapper XML: " + path, e);
        }
    }
}
