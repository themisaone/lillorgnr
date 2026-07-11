package no.companyfetcher.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.companyfetcher.config.Configuration;
import no.companyfetcher.config.MtbLicenseFilter;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.parser.FiskeridirParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FiskeridirApiProvider implements AquacultureProvider {

    private static final Logger log = LoggerFactory.getLogger(FiskeridirApiProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final FiskeridirParser parser;
    private final String apiBaseUrl;

    public FiskeridirApiProvider(Configuration configuration) {
        this(configuration.getFiskeridirApiBaseUrl(), MtbLicenseFilter.fromConfiguration(configuration));
    }

    FiskeridirApiProvider(String apiBaseUrl) {
        this(apiBaseUrl, MtbLicenseFilter.matfiskKommersiell());
    }

    FiskeridirApiProvider(String apiBaseUrl, MtbLicenseFilter filter) {
        this.apiBaseUrl = trimTrailingSlash(apiBaseUrl);
        this.parser = new FiskeridirParser(filter);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public AquacultureCapacityData fetch(String orgNumber) {
        String normalizedOrgNumber = normalizeOrgNumber(orgNumber);
        JsonNode entity = fetchJson("/api/v1/entities?entity-nr=" + normalizedOrgNumber);
        JsonNode licenses = fetchJson(
                "/api/v1/licenses?legal-entity-nr=" + normalizedOrgNumber + "&range=0-99"
        );
        return parser.parse(normalizedOrgNumber, entity, licenses);
    }

    private JsonNode fetchJson(String path) {
        String url = apiBaseUrl + path;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 404) {
                throw new ProviderException(ProviderException.Reason.NOT_FOUND, "Fiskeridir resource not found: " + url);
            }
            if (status == 429) {
                throw new ProviderException(ProviderException.Reason.RATE_LIMIT, "Fiskeridir rate limit reached");
            }
            if (status >= 500) {
                throw new ProviderException(ProviderException.Reason.NETWORK, "Fiskeridir returned HTTP " + status);
            }
            if (status >= 400) {
                throw new ProviderException(
                        ProviderException.Reason.PARSE,
                        "Fiskeridir returned HTTP " + status + " for " + url
                );
            }

            JsonNode body = OBJECT_MAPPER.readTree(response.body());
            if (body.has("errors")) {
                throw new ProviderException(
                        ProviderException.Reason.PARSE,
                        "Fiskeridir API error: " + body.get("errors")
                );
            }

            return body;
        } catch (ProviderException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException(ProviderException.Reason.NETWORK, "Request interrupted", e);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Reason.NETWORK, "Network error fetching " + url, e);
        }
    }

    private String normalizeOrgNumber(String orgNumber) {
        String digits = orgNumber.replaceAll("\\D", "");
        if (digits.length() != 9) {
            throw new ProviderException(
                    ProviderException.Reason.PARSE,
                    "Invalid organization number: " + orgNumber
            );
        }
        return digits;
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
