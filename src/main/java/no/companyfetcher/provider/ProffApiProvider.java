package no.companyfetcher.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.companyfetcher.config.Configuration;
import no.companyfetcher.model.CompanyData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ProffApiProvider implements CompanyProvider {

    private static final Logger log = LoggerFactory.getLogger(ProffApiProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;
    private final int accountingYear;

    public ProffApiProvider(Configuration configuration) {
        this.apiKey = configuration.getProffApiKey();
        this.accountingYear = configuration.getAccountingYear();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        if (apiKey.isBlank()) {
            throw new IllegalStateException("PROFF_API_KEY environment variable or proff.api.key is required for PROFF_API provider");
        }
    }

    @Override
    public CompanyData fetch(String orgNumber) {
        String normalizedOrgNumber = orgNumber.replaceAll("\\D", "");
        String url = "https://api.proff.no/api/companies/register/NO/" + normalizedOrgNumber;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Token " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new ProviderException(ProviderException.Reason.NOT_FOUND, "Company not found: " + normalizedOrgNumber);
            }
            if (response.statusCode() >= 400) {
                throw new ProviderException(
                        ProviderException.Reason.NETWORK,
                        "Proff API returned HTTP " + response.statusCode()
                );
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            return mapToCompanyData(normalizedOrgNumber, root);
        } catch (ProviderException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException(ProviderException.Reason.NETWORK, "Request interrupted", e);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Reason.NETWORK, "Proff API request failed", e);
        }
    }

    private CompanyData mapToCompanyData(String orgNumber, JsonNode root) {
        String companyName = textAt(root, "name", "legalName", "companyName");
        Long revenue = amountAt(root, accountingYear, "revenue", "operatingIncome", "SDI");
        Long salaryCost = amountAt(root, accountingYear, "salaryCost", "wageCosts", "LTP");
        Long ebit = amountAt(root, accountingYear, "ebit", "operatingProfit", "DR");

        String status = "OK";
        if (revenue == null && salaryCost == null && ebit == null) {
            status = "FAILED: NO_DATA";
        } else if (revenue == null || salaryCost == null || ebit == null) {
            status = "PARTIAL";
        }

        return new CompanyData(orgNumber, companyName, accountingYear, revenue, salaryCost, ebit, status);
    }

    private String textAt(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private Long amountAt(JsonNode node, int year, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = findYearNode(node, year, fieldName);
            if (value != null && value.isNumber()) {
                return Math.round(value.asDouble() * 1000);
            }
            if (value != null && !value.isNull()) {
                try {
                    return Math.round(Double.parseDouble(value.asText().replace(" ", "")) * 1000);
                } catch (NumberFormatException ignored) {
                    // try next field
                }
            }
        }
        return null;
    }

    private JsonNode findYearNode(JsonNode node, int year, String fieldName) {
        JsonNode direct = node.get(fieldName);
        if (direct != null) {
            return direct;
        }

        JsonNode accounts = node.get("accounts");
        if (accounts != null && accounts.isArray()) {
            for (JsonNode account : accounts) {
                JsonNode yearNode = account.get("year");
                if (yearNode != null && yearNode.asInt() == year) {
                    JsonNode value = account.get(fieldName);
                    if (value != null) {
                        return value;
                    }
                }
            }
        }

        return null;
    }
}
