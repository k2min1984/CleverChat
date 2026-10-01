package kr.co.cleverchat.domain.crawl.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.domain.crawl.model.CrawlCoverage;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlJob;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CrawlMapper {
    List<CrawlTarget> findTargets(@Param("useYn") String useYn);

    List<CrawlTarget> findDueTargets(@Param("limit") int limit);

    CrawlTarget findTargetById(@Param("id") Long id);

    Long lockTargetForRun(@Param("id") Long id);

    CrawlTarget findTargetByUrl(@Param("url") String url);

    void insertTarget(CrawlTarget target);

    int updateTarget(CrawlTarget target);

    int updateTargetRunStatus(
            @Param("id") Long id, @Param("status") String status, @Param("message") String message);

    int updateTargetSchedule(CrawlTarget target);

    int updateTargetRobots(@Param("id") Long id, @Param("allowed") Boolean allowed);

    int scheduleNextRun(@Param("id") Long id, @Param("intervalMinutes") int intervalMinutes);

    int updateNextRunAt(@Param("id") Long id, @Param("nextRunAt") OffsetDateTime nextRunAt);

    CrawlDocument findDocumentByTargetAndHash(
            @Param("targetId") Long targetId, @Param("contentHash") String contentHash);

    CrawlDocument findDocumentByTargetAndUrlHash(
            @Param("targetId") Long targetId, @Param("urlHash") String urlHash);

    CrawlDocument findDocumentById(@Param("id") Long id);

    int updateDocument(CrawlDocument document);

    List<CrawlDocument> findDocuments(@Param("targetId") Long targetId, @Param("limit") int limit);

    long countDocuments(@Param("targetId") Long targetId, @Param("query") String query);

    List<CrawlDocument> findDocumentPage(
            @Param("targetId") Long targetId,
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("offset") int offset);

    List<CrawlRunLog> findRunLogs(
            @Param("targetId") Long targetId,
            @Param("status") String status,
            @Param("failureCode") String failureCode,
            @Param("limit") int limit);

    List<CrawlJob> findJobs(@Param("targetId") Long targetId, @Param("limit") int limit);

    CrawlJob enqueueJob(
            @Param("targetId") Long targetId,
            @Param("triggerType") String triggerType,
            @Param("requestedBy") Long requestedBy);

    CrawlJob claimNextJob(@Param("maxAttempts") int maxAttempts);

    int completeJob(
            @Param("id") Long id, @Param("status") String status, @Param("message") String message);

    int recoverTimedOutJobs(
            @Param("cutoff") OffsetDateTime cutoff,
            @Param("maxAttempts") int maxAttempts,
            @Param("message") String message);

    void insertDocument(CrawlDocument document);

    List<CrawlDocument> findDocumentsMissingTokens(@Param("limit") int limit);

    int updateDocumentTokens(@Param("id") Long id, @Param("tokens") String tokens);

    void insertRunLog(CrawlRunLog runLog);

    void insertCoverage(CrawlCoverage coverage);

    List<CrawlCoverage> findCoverages(@Param("targetId") Long targetId, @Param("limit") int limit);

    CrawlRunLog findRunLogById(@Param("id") Long id);

    List<CrawlRunLog> findFailedRunLogs(
            @Param("reviewed") Boolean reviewed,
            @Param("failureCode") String failureCode,
            @Param("limit") int limit);

    int reviewRunLog(
            @Param("id") Long id,
            @Param("reviewedBy") Long reviewedBy,
            @Param("comment") String comment);

    long countRunLogsBefore(@Param("cutoff") OffsetDateTime cutoff);

    long countDocumentsBefore(@Param("cutoff") OffsetDateTime cutoff);

    int deleteRunLogsBefore(@Param("cutoff") OffsetDateTime cutoff);

    int deleteDocumentsBefore(@Param("cutoff") OffsetDateTime cutoff);
}
