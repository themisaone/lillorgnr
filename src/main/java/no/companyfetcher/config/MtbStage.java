package no.companyfetcher.config;

public record MtbStage(long low, long high, double price, boolean unlimitedHigh) {

    public static MtbStage parse(String lowRaw, String highRaw, String priceRaw) {
        long low = Long.parseLong(lowRaw.trim());
        String highTrimmed = highRaw.trim();
        boolean unlimited = "-".equals(highTrimmed);
        long high = unlimited ? Long.MAX_VALUE : Long.parseLong(highTrimmed.replaceAll("\\s+", ""));
        double price = Double.parseDouble(priceRaw.trim());
        return new MtbStage(low, high, price, unlimited);
    }
}
