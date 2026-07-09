package no.companyfetcher.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final int retryCount;

    public RetryExecutor(int retryCount) {
        this.retryCount = Math.max(1, retryCount);
    }

    public <T> T execute(String operation, Supplier<T> supplier) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("{} failed on attempt {}/{}: {}", operation, attempt, retryCount, e.getMessage());
            }
        }

        throw lastFailure;
    }
}
