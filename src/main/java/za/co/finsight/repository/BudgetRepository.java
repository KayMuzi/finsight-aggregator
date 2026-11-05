package za.co.finsight.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.finsight.domain.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByCustomerIdAndYearMonth(String customerId, String yearMonth);

    List<Budget> findByCustomerIdOrderByYearMonthDesc(String customerId);
}
