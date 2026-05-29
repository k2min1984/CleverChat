CREATE INDEX ix_scenario_keyword_keyword_trgm
    ON scenario_keyword USING GIN (keyword gin_trgm_ops)
    WHERE enabled = TRUE;

CREATE INDEX ix_scenario_synonym_synonym_trgm
    ON scenario_synonym USING GIN (synonym gin_trgm_ops)
    WHERE enabled = TRUE;

CREATE INDEX ix_crawl_document_title_trgm
    ON crawl_document USING GIN (title gin_trgm_ops)
    WHERE status = 'SUCCESS';

CREATE INDEX ix_crawl_document_content_trgm
    ON crawl_document USING GIN (content gin_trgm_ops)
    WHERE status = 'SUCCESS';

CREATE INDEX ix_crawl_document_search_tsv
    ON crawl_document USING GIN (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))
    )
    WHERE status = 'SUCCESS';
