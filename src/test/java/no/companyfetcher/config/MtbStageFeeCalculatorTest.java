package no.companyfetcher.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MtbStageFeeCalculatorTest {

    private final List<MtbStage> stages = MtbStagesLoader.load(Path.of("config.mtbstages"));

    @Test
    void calculatesFeeForExampleCapacity8260() {
        assertEquals(117416, MtbStageFeeCalculator.calculate(8260, stages));
    }

    @Test
    void calculatesFeeForCapacity4250() {
        assertEquals(72520, MtbStageFeeCalculator.calculate(4250, stages));
    }

    @Test
    void returnsZeroForZeroCapacity() {
        assertEquals(0, MtbStageFeeCalculator.calculate(0, stages));
    }
}
