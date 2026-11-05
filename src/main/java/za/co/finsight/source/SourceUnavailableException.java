package za.co.finsight.source;

import za.co.finsight.domain.SourceSystem;

public class SourceUnavailableException extends RuntimeException {

    public SourceUnavailableException(SourceSystem source, String reason) {
        super(source + " feed unavailable: " + reason);
    }
}
