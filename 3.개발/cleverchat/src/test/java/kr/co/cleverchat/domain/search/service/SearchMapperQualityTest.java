package kr.co.cleverchat.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SearchMapperQualityTest {

    private static final Path V14_MIGRATION =
            Path.of("src/main/resources/db/migration/V14__search_quality_indexes.sql");

    @Test
    void searchQualityMigrationAddsMissingTrigramAndFtsIndexes() throws Exception {
        String sql = Files.readString(V14_MIGRATION, StandardCharsets.UTF_8);

        assertThat(sql)
                .contains(
                        "ix_scenario_keyword_keyword_trgm",
                        "ix_scenario_synonym_synonym_trgm",
                        "ix_crawl_document_title_trgm",
                        "ix_crawl_document_content_trgm",
                        "ix_crawl_document_search_tsv");
    }
}
