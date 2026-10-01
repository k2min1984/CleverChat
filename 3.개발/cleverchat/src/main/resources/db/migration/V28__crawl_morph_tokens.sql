-- Morphological search support for crawled documents.
-- content_tokens holds the space-joined Korean morphemes of (title + content),
-- produced by the Lucene Nori analyzer at crawl time. Searching against the
-- morpheme tokens (rather than the raw 'simple' tsvector) lets queries match on
-- stems instead of whitespace-delimited words.
ALTER TABLE tb_crawl_document ADD COLUMN content_tokens TEXT;

-- Backfilled by the application (Nori runs in the JVM, not in SQL); existing rows
-- stay NULL until reindexed.
CREATE INDEX ix_crawl_document_tokens_tsv
    ON tb_crawl_document USING GIN (
        to_tsvector('simple', coalesce(content_tokens, ''))
    )
    WHERE status = 'SUCCESS';
