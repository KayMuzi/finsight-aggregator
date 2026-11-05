package za.co.finsight.categorisation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;

class CategorisationEngineTest {

    private final CategorisationEngine engine;

    CategorisationEngineTest() throws Exception {
        this.engine = new CategorisationEngine(new ObjectMapper());
    }

    @Test
    void salaryCreditIsIncome() {
        assertThat(engine.categorise(tx("32000.00", "SALARY ACME PTY LTD", "Acme", null)))
                .isEqualTo(Category.INCOME);
    }

    @Test
    void sameKeywordOnDebitIsNotIncome() {
        assertThat(engine.categorise(tx("-32000.00", "SALARY ACME PTY LTD", "Acme", null)))
                .isNotEqualTo(Category.INCOME);
    }

    @Test
    void mccDrivesCategoryWhenNameIsUnhelpful() {
        assertThat(engine.categorise(tx("-289.50", "CARD Uber Eats", "Uber Eats", "5812")))
                .isEqualTo(Category.DINING);
    }

    @Test
    void keywordMatchesWholeWordsOnly() {
        assertThat(engine.categorise(tx("-30.00", "POS PURCHASE SPARKLING WATER", "Sparkling Water", null)))
                .isEqualTo(Category.OTHER);
    }

    @Test
    void groceryKeywordMatchesBankNarrative() {
        assertThat(engine.categorise(tx("-1180.45", "POS PURCHASE CHECKERS HYPER", "Checkers Hyper", null)))
                .isEqualTo(Category.GROCERIES);
    }

    @Test
    void unknownTransactionFallsBackToOther() {
        assertThat(engine.categorise(tx("-1000.00", "ATM CASH WITHDRAWAL", "Atm Cash Withdrawal", null)))
                .isEqualTo(Category.OTHER);
    }

    @Test
    void savingsTransferIsNotClassifiedAsGenericTransfer() {
        assertThat(engine.categorise(tx("-2000.00", "TRANSFER TO SAVINGS ACCOUNT", "Transfer To Savings Account", null)))
                .isEqualTo(Category.SAVINGS);
    }

    private static NormalizedTransaction tx(String amount, String description, String merchant, String mcc) {
        return new NormalizedTransaction(SourceSystem.BANK, "EXT-1", "CUST-T", Instant.parse("2026-10-01T10:00:00Z"),
                new BigDecimal(amount), "ZAR", description, merchant, mcc);
    }
}
