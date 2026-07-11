package no.companyfetcher.model;

public record MtbCalcData(
        String name,
        Double mtb,
        Long fee,
        Long medlCont,
        Long servAvgift
) {
}
