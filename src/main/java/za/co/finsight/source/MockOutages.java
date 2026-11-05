package za.co.finsight.source;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import za.co.finsight.config.FinsightProperties;
import za.co.finsight.domain.SourceSystem;

/** Lets operators and tests simulate a provider outage for a given customer. */
@Component
public class MockOutages {

    private final Set<String> outages;

    public MockOutages(FinsightProperties properties) {
        this.outages = properties.mock().outages().stream()
                .map(o -> key(o.customerId(), o.source()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void assertAvailable(String customerId, SourceSystem source) {
        if (outages.contains(key(customerId, source))) {
            throw new SourceUnavailableException(source, "simulated outage configured for this customer");
        }
    }

    private static String key(String customerId, SourceSystem source) {
        return customerId + "|" + source;
    }
}
