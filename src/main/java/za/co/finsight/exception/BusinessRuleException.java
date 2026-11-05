package za.co.finsight.exception;

/** Request is well-formed but violates a business rule, for example budgeting a non-budgetable category. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
