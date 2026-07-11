package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.config.MtbStage;
import no.companyfetcher.config.MtbStageFeeCalculator;
import no.companyfetcher.config.MtbStagesLoader;
import no.companyfetcher.input.MtbInputReader;
import no.companyfetcher.model.MtbCalcData;

import java.nio.file.Path;
import java.util.List;

public class MtbCalcService {

    private final List<MtbStage> stages;
    private final long medlCont;
    private final long servAvgift;

    public MtbCalcService(Configuration configuration) {
        this.stages = MtbStagesLoader.load(Path.of(configuration.getMtbStagesFile()));
        this.medlCont = configuration.getMedlKontigent() / 2;
        this.servAvgift = configuration.getServAvgift() / 2;
    }

    public List<MtbCalcData> calculateAll(List<MtbInputReader.MtbInputRow> rows) {
        return rows.stream().map(this::calculate).toList();
    }

    private MtbCalcData calculate(MtbInputReader.MtbInputRow row) {
        Long fee = row.mtb() == null ? null : MtbStageFeeCalculator.calculate(row.mtb(), stages);
        return new MtbCalcData(row.name(), row.mtb(), fee, medlCont, servAvgift);
    }
}
