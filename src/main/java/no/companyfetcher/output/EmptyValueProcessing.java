package no.companyfetcher.output;

public enum EmptyValueProcessing {
    UNTOUCHED,
    CLEAR;

    public boolean clearsEmptyCells() {
        return this == CLEAR;
    }

    public static EmptyValueProcessing parse(String value) {
        if (value == null || value.isBlank()) {
            return UNTOUCHED;
        }
        try {
            return EmptyValueProcessing.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Invalid empty.value.processing: " + value + ". Use UNTOUCHED or CLEAR."
            );
        }
    }
}
