package no.companyfetcher.config;

import java.util.List;

public final class MtbStageFeeCalculator {

    private MtbStageFeeCalculator() {
    }

    public static long calculate(double totalCapacity, List<MtbStage> stages) {
        if (totalCapacity <= 0) {
            return 0;
        }

        long capacity = (long) Math.floor(totalCapacity);
        long previousBoundary = 0;
        double fee = 0.0;

        for (MtbStage stage : stages) {
            if (capacity <= previousBoundary) {
                break;
            }

            long tierTop = stage.unlimitedHigh()
                    ? capacity
                    : Math.min(capacity, stage.high());

            long units = tierTop - previousBoundary;
            if (units > 0) {
                fee += units * stage.price();
            }

            previousBoundary = stage.unlimitedHigh() ? capacity : stage.high();
            if (capacity <= stage.high() || stage.unlimitedHigh()) {
                break;
            }
        }

        return Math.round(fee);
    }
}
