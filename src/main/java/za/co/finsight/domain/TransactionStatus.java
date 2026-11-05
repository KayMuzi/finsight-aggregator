package za.co.finsight.domain;

public enum TransactionStatus {
    /** Counted in all aggregates and budgets. */
    POSTED,
    /** Same money already seen via another source. Stored for audit, excluded from aggregates. */
    DUPLICATE
}
