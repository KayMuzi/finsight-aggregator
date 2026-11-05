package za.co.finsight.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.finsight.domain.SyncRun;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {

    List<SyncRun> findTop20ByCustomerIdOrderByStartedAtDesc(String customerId);
}
