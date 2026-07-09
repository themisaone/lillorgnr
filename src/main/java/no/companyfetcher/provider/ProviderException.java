package no.companyfetcher.provider;

public class ProviderException extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        NETWORK,
        PARSE,
        RATE_LIMIT
    }

    private final Reason reason;

    public ProviderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ProviderException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
