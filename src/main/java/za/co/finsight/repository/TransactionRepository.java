package za.co.finsight.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.TransactionRecord;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long>,
        JpaSpecificationExecutor<TransactionRecord> {

    /** Aggregate projections. Totals are signed sums of POSTED amounts (negative = money out). */
    interface CategoryTotal {
        Category getCategory();
        BigDecimal getTotal();
        Long getCount();
    }

    interface MerchantTotal {
        String getMerchant();
        BigDecimal getTotal();
        Long getCount();
    }

    interface SourceTotal {
        SourceSystem getSource();
        BigDecimal getTotal();
        Long getCount();
    }

    Optional<TransactionRecord> findByCustomerIdAndSourceAndExternalId(String customerId, SourceSystem source,
                                                                       String externalId);

    Optional<TransactionRecord> findByIdAndCustomerId(Long id, String customerId);

    long countByCustomerIdAndStatusAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            String customerId, za.co.finsight.domain.TransactionStatus status, Instant from, Instant to);

    @Query("""
            select t from TransactionRecord t
            where t.customerId = :customerId
              and t.source <> :source
              and t.amount = :amount
              and t.status = za.co.finsight.domain.TransactionStatus.POSTED
              and t.occurredAt between :from and :to
            order by t.occurredAt asc, t.id asc
            """)
    List<TransactionRecord> findPossibleDuplicates(@Param("customerId") String customerId,
                                                   @Param("source") SourceSystem source,
                                                   @Param("amount") BigDecimal amount,
                                                   @Param("from") Instant from,
                                                   @Param("to") Instant to);

    @Query("""
            select t from TransactionRecord t
            where t.customerId = :customerId
              and t.occurredAt >= :from and t.occurredAt < :to
            order by t.occurredAt asc, t.id asc
            """)
    List<TransactionRecord> findInWindow(@Param("customerId") String customerId,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to);

    @Query("""
            select t.category as category, sum(t.amount) as total, count(t) as count
            from TransactionRecord t
            where t.customerId = :customerId
              and t.status = za.co.finsight.domain.TransactionStatus.POSTED
              and t.occurredAt >= :from and t.occurredAt < :to
            group by t.category
            """)
    List<CategoryTotal> sumByCategory(@Param("customerId") String customerId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    @Query("""
            select t.source as source, sum(t.amount) as total, count(t) as count
            from TransactionRecord t
            where t.customerId = :customerId
              and t.status = za.co.finsight.domain.TransactionStatus.POSTED
              and t.occurredAt >= :from and t.occurredAt < :to
            group by t.source
            """)
    List<SourceTotal> sumBySource(@Param("customerId") String customerId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("""
            select t.merchantName as merchant, sum(t.amount) as total, count(t) as count
            from TransactionRecord t
            where t.customerId = :customerId
              and t.status = za.co.finsight.domain.TransactionStatus.POSTED
              and t.category in :categories
              and t.occurredAt >= :from and t.occurredAt < :to
            group by t.merchantName
            order by sum(t.amount) asc
            """)
    List<MerchantTotal> topMerchants(@Param("customerId") String customerId,
                                     @Param("categories") Collection<Category> categories,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}
