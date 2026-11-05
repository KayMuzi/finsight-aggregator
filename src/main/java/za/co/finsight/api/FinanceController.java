package za.co.finsight.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import za.co.finsight.budget.BudgetService;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.TransactionBudgetStatus;
import za.co.finsight.ingestion.SyncService;
import za.co.finsight.transaction.TransactionService;
import za.co.finsight.transaction.TransactionSpecifications;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class FinanceController {

    public record CategoryInfo(Category category, boolean budgetable) {}

    private final SyncService syncService;
    private final TransactionService transactionService;

    @PostMapping("/customers/{customerId}/sync")
    public SyncService.SyncResult sync(@PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId) {
        return syncService.sync(customerId);
    }

    @GetMapping("/customers/{customerId}/syncs")
    public List<SyncService.SyncRunView> syncHistory(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId) {
        return syncService.history(customerId);
    }

    @GetMapping("/customers/{customerId}/transactions")
    public PageResponse<TransactionService.TransactionView> listTransactions(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) SourceSystem source,
            @RequestParam(required = false) TransactionBudgetStatus budgetStatus,
            @RequestParam(required = false) @Size(max = 64) String merchant,
            @RequestParam(defaultValue = "false") boolean includeDuplicates,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (from != null && to != null && !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'");
        }
        var filter = new TransactionSpecifications.Filter(from, to, category, source, budgetStatus,
                merchant, includeDuplicates);
        return PageResponse.from(transactionService.search(customerId, filter, pageable));
    }

    @GetMapping("/customers/{customerId}/transactions/{transactionId}")
    public TransactionService.TransactionView getTransaction(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable Long transactionId) {
        return transactionService.get(customerId, transactionId);
    }

    @PatchMapping(value = "/customers/{customerId}/transactions/{transactionId}/category",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public TransactionService.TransactionView recategorise(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable Long transactionId,
            @Valid @RequestBody ApiModels.CategoryUpdate body) {
        return transactionService.recategorise(customerId, transactionId, body.category());
    }

    @GetMapping("/categories")
    public List<CategoryInfo> categories() {
        return Arrays.stream(Category.values()).map(c -> new CategoryInfo(c, c.budgetable())).toList();
    }
}
