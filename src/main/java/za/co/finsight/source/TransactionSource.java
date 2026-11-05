package za.co.finsight.source;

import java.util.List;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;

/**
 * Adapter for one external data provider. Implementations fetch the provider's native format and
 * map it into {@link NormalizedTransaction}. Failures are signalled with SourceUnavailableException
 * so that one provider outage does not stop the others.
 */
public interface TransactionSource {

    SourceSystem system();

    List<NormalizedTransaction> fetch(String customerId);
}
