package za.co.finsight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Audit record of one aggregation run for a customer. */
@Entity
@Table(name = "sync_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private SyncStatus status;

    @Column(name = "fetched_count", nullable = false)
    private int fetchedCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Column(name = "already_known_count", nullable = false)
    private int alreadyKnownCount;

    @Column(name = "error_summary", length = 1000)
    private String errorSummary;

    public SyncRun(String customerId, Instant startedAt) {
        this.customerId = customerId;
        this.startedAt = startedAt;
    }

    public void complete(SyncStatus status, int fetched, int inserted, int duplicates, int alreadyKnown,
                         String errorSummary, Instant completedAt) {
        this.status = status;
        this.fetchedCount = fetched;
        this.insertedCount = inserted;
        this.duplicateCount = duplicates;
        this.alreadyKnownCount = alreadyKnown;
        this.errorSummary = errorSummary;
        this.completedAt = completedAt;
    }
}
