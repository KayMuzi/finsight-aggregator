package za.co.finsight.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.finsight.budget.BudgetService;
import za.co.finsight.config.FinsightProperties;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.CategorySource;
import za.co.finsight.domain.Direction;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.TransactionBudgetStatus;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.domain.TransactionStatus;
import za.co.finsight.exception.NotFoundException;
import za.co.finsight.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class TransactionService {

    public record TransactionView(Long id, SourceSystem source, String externalId, Instant occurredAt,
                                  BigDecimal amount, String currency, Direction direction, String description,
                                  String merchantName, String mcc, Category category, CategorySource categorySource,
                                  TransactionBudgetStatus budgetStatus, TransactionStatus status, Long duplicateOfId) {

        static TransactionView from(TransactionRecord t) {
            return new TransactionView(t.getId(), t.getSource(), t.getExternalId(), t.getOccurredAt(),
                    t.getAmount(), t.getCurrency(), t.getDirection(), t.getDescription(), t.getMerchantName(),
                    t.getMcc(), t.getCategory(), t.getCategorySource(), t.getBudgetStatus(), t.getStatus(),
                    t.getDuplicateOfId());
        }
    }

    private final TransactionRepository transactions;
    private final BudgetService budgets;
    private final FinsightProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<TransactionView> search(String customerId, TransactionSpecifications.Filter filter, Pageable pageable) {
        return transactions.findAll(TransactionSpecifications.matching(customerId, filter), pageable)
                .map(TransactionView::from);
    }

    @Transactional(readOnly = true)
    public TransactionView get(String customerId, Long id) {
        return TransactionView.from(find(customerId, id));
    }

    /** User override. Takes precedence over rules on subsequent syncs and budgets are re-derived for the month. */
    @Transactional
    public TransactionView recategorise(String customerId, Long id, Category category) {
        TransactionRecord t = find(customerId, id);
        t.recategorise(category, clock.instant());
        ZoneId zone = properties.zone();
        budgets.recalculateMonth(customerId, YearMonth.from(t.getOccurredAt().atZone(zone)));
        return TransactionView.from(t);
    }

    private TransactionRecord find(String customerId, Long id) {
        return transactions.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("Transaction " + id + " not found"));
    }
}
