package no.companyfetcher.service;

import no.companyfetcher.input.MtbInputReader;
import no.companyfetcher.model.MtbCalcData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MtbCalcServiceTest {

    @Test
    void calculatesFeeAndMembershipValues() {
        MtbCalcService service = new MtbCalcService(TestAccountingConfiguration.load());

        List<MtbCalcData> rows = service.calculateAll(List.of(
                new MtbInputReader.MtbInputRow("ARNØY LAKS", 4250.0),
                new MtbInputReader.MtbInputRow("ELVEVOLL SETTEFISK", null)
        ));

        assertEquals(72520L, rows.get(0).fee());
        assertEquals(7100L, rows.get(0).medlCont());
        assertEquals(6100L, rows.get(0).servAvgift());
        assertNull(rows.get(1).fee());
        assertEquals(7100L, rows.get(1).medlCont());
    }
}
