package za.co.finsight.domain;

/** Position of a transaction against its category budget at the time it occurred, in date order. */
public enum TransactionBudgetStatus {
    WITHIN_BUDGET,
    OVER_BUDGET,
    NO_BUDGET,
    NOT_APPLICABLE
}
