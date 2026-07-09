package no.companyfetcher.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.provider.ProviderException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProffParser {

    private static final Logger log = LoggerFactory.getLogger(ProffParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("-?[\\d\\s.,]+");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})");

    public CompanyData parse(String orgNumber, Document document, int accountingYear) {
        String companyName = extractCompanyName(document);
        Map<String, Long> figures = extractFigures(document, accountingYear);

        Long revenue = figures.get("SDI");
        Long salaryCost = figures.get("LTP");
        Long ebit = figures.get("DR");

        String status = determineStatus(revenue, salaryCost, ebit);

        return new CompanyData(
                orgNumber,
                companyName,
                accountingYear,
                revenue,
                salaryCost,
                ebit,
                status
        );
    }

    private String extractCompanyName(Document document) {
        Element heading = document.selectFirst("h1");
        if (heading != null && !heading.text().isBlank()) {
            return heading.text().trim();
        }

        Element title = document.selectFirst("title");
        if (title != null) {
            String titleText = title.text();
            int dashIndex = titleText.indexOf(" - ");
            if (dashIndex > 0) {
                return titleText.substring(0, dashIndex).trim();
            }
        }

        throw new ProviderException(ProviderException.Reason.PARSE, "Company name not found");
    }

    private Map<String, Long> extractFigures(Document document, int accountingYear) {
        Map<String, Long> fromJson = extractFromNextData(document, accountingYear);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }

        Map<String, Long> fromTable = extractFromAccountingTable(document, accountingYear);
        if (!fromTable.isEmpty()) {
            return fromTable;
        }

        throw new ProviderException(
                ProviderException.Reason.NOT_FOUND,
                "Accounting data for year " + accountingYear + " not found"
        );
    }

    private Map<String, Long> extractFromNextData(Document document, int accountingYear) {
        Map<String, Long> figures = new HashMap<>();
        Element script = document.selectFirst("script#__NEXT_DATA__");
        if (script == null) {
            return figures;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(script.data());
            figures.putAll(extractFromCompanyAccounts(root, accountingYear));
            if (figures.isEmpty()) {
                collectAccountingNodes(root, accountingYear, figures);
            }
        } catch (Exception e) {
            log.debug("Could not parse __NEXT_DATA__: {}", e.getMessage());
        }

        return figures;
    }

    private Map<String, Long> extractFromCompanyAccounts(JsonNode root, int accountingYear) {
        Map<String, Long> figures = new HashMap<>();
        JsonNode company = root.at("/props/pageProps/company");
        if (company.isMissingNode()) {
            return figures;
        }

        for (String accountsField : new String[]{"companyAccounts", "corporateAccounts"}) {
            JsonNode accountsByYear = company.get(accountsField);
            if (accountsByYear == null || !accountsByYear.isArray()) {
                continue;
            }

            for (JsonNode yearRecord : accountsByYear) {
                if (!matchesYear(yearRecord, accountingYear)) {
                    continue;
                }

                extractAccountCodes(yearRecord.get("accounts"), figures);
                if (!figures.isEmpty()) {
                    return figures;
                }
            }
        }

        return figures;
    }

    private void extractAccountCodes(JsonNode accounts, Map<String, Long> figures) {
        if (accounts == null || !accounts.isArray()) {
            return;
        }

        for (JsonNode account : accounts) {
            String code = account.path("code").asText(null);
            if (code == null) {
                continue;
            }

            if ("SDI".equals(code) || "LTP".equals(code) || "DR".equals(code)) {
                putFigure(figures, code, parseAmount(account.path("amount").asText(null)));
            }
        }
    }

    private boolean matchesYear(JsonNode node, int accountingYear) {
        JsonNode year = node.get("year");
        if (year != null && !year.isNull()) {
            return year.asInt() == accountingYear;
        }
        return containsYear(node, accountingYear);
    }

    private void collectAccountingNodes(JsonNode node, int accountingYear, Map<String, Long> figures) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            if (matchesYear(node, accountingYear)) {
                extractAccountCodes(node.get("accounts"), figures);
                putFigure(figures, "SDI", findAmount(node, "SDI", "operatingIncome", "sumDriftsinntekter", "revenue"));
                putFigure(figures, "LTP", findAmount(node, "LTP", "salaryCost", "lonnskostnader", "wageCosts"));
                putFigure(figures, "DR", findAmount(node, "DR", "operatingProfit", "driftsresultat", "ebit"));
            }

            node.fields().forEachRemaining(entry -> collectAccountingNodes(entry.getValue(), accountingYear, figures));
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectAccountingNodes(child, accountingYear, figures);
            }
        }
    }

    private boolean containsYear(JsonNode node, int accountingYear) {
        String yearText = accountingYear + "";
        for (String field : new String[]{"year", "accountingYear", "period", "periodEnd", "periodStart", "date"}) {
            JsonNode value = node.get(field);
            if (value != null && value.asText().contains(yearText)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeAccountingRecord(JsonNode node) {
        return hasAnyField(node, "SDI", "operatingIncome", "sumDriftsinntekter", "revenue")
                || hasAnyField(node, "DR", "operatingProfit", "driftsresultat", "ebit");
    }

    private boolean hasAnyField(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name) && !node.get(name).isNull()) {
                return true;
            }
        }
        return false;
    }

    private Long findAmount(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                Long parsed = parseAmount(value.asText());
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private Map<String, Long> extractFromAccountingTable(Document document, int accountingYear) {
        Map<String, Long> figures = new HashMap<>();

        for (Element table : document.select("table")) {
            if (!tableContainsYear(table, accountingYear)) {
                continue;
            }

            for (Element row : table.select("tr")) {
                Elements cells = row.select("th, td");
                if (cells.size() < 2) {
                    continue;
                }

                String label = normalizeLabel(cells.get(0).text());
                Long amount = parseAmount(cells.get(cells.size() - 1).text());
                if (amount == null) {
                    continue;
                }

                if (label.contains("sum driftsinntekter")) {
                    figures.put("SDI", amount);
                } else if (label.contains("lønnskostnader") || label.contains("lonnskostnader")) {
                    figures.put("LTP", amount);
                } else if (label.contains("driftsresultat")) {
                    figures.put("DR", amount);
                }
            }
        }

        if (figures.isEmpty()) {
            figures.putAll(extractFromLabelValuePairs(document, accountingYear));
        }

        return figures;
    }

    private Map<String, Long> extractFromLabelValuePairs(Document document, int accountingYear) {
        Map<String, Long> figures = new HashMap<>();
        String yearMarker = String.valueOf(accountingYear);

        for (Element element : document.select("tr, li, div, span, p")) {
            String text = element.text();
            if (!text.contains(yearMarker)) {
                continue;
            }

            String normalized = normalizeLabel(text);
            Long amount = parseAmount(text);
            if (amount == null) {
                continue;
            }

            if (normalized.contains("sum driftsinntekter")) {
                figures.put("SDI", amount);
            } else if (normalized.contains("lønnskostnader") || normalized.contains("lonnskostnader")) {
                figures.put("LTP", amount);
            } else if (normalized.contains("driftsresultat")) {
                figures.put("DR", amount);
            }
        }

        return figures;
    }

    private boolean tableContainsYear(Element table, int accountingYear) {
        String tableText = table.text();
        Matcher matcher = YEAR_PATTERN.matcher(tableText);
        while (matcher.find()) {
            if (Integer.parseInt(matcher.group(1)) == accountingYear) {
                return true;
            }
        }
        return false;
    }

    private void putFigure(Map<String, Long> figures, String key, Long value) {
        if (value != null) {
            figures.putIfAbsent(key, value);
        }
    }

    private String normalizeLabel(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace(" (ebit)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    Long parseAmount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }

        String numeric = matcher.group()
                .replace("\u00a0", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");

        if (numeric.isBlank() || "-".equals(numeric)) {
            return null;
        }

        double thousands;
        try {
            thousands = Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            return null;
        }

        return Math.round(thousands * 1000);
    }

    private String determineStatus(Long revenue, Long salaryCost, Long ebit) {
        if (revenue == null && salaryCost == null && ebit == null) {
            return "FAILED: NO_DATA";
        }
        if (revenue == null || salaryCost == null || ebit == null) {
            return "PARTIAL";
        }
        return "OK";
    }
}
