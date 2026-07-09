package no.companyfetcher.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.companyfetcher.config.Configuration;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.parser.ProffParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProffWebProvider implements CompanyProvider {

    private static final Logger log = LoggerFactory.getLogger(ProffWebProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String BASE_URL = "https://www.proff.no";
    private static final String BRREG_URL = "https://data.brreg.no/enhetsregisteret/api/enheter/";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final Pattern COMPANY_LINK_PATTERN =
            Pattern.compile("/selskap/[^\"'\\s]+?(\\d{9})(?:[\"'/?#]|$)");

    private final HttpClient httpClient;
    private final ProffParser parser;
    private final int accountingYear;

    public ProffWebProvider(Configuration configuration) {
        this(configuration.getAccountingYear());
    }

    public ProffWebProvider(int accountingYear) {
        this.accountingYear = accountingYear;
        this.parser = new ProffParser();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CompanyData fetch(String orgNumber) {
        String normalizedOrgNumber = normalizeOrgNumber(orgNumber);
        String companyUrl = resolveCompanyUrl(normalizedOrgNumber);
        Document document = fetchDocument(companyUrl);
        return parser.parse(normalizedOrgNumber, document, accountingYear);
    }

    private String resolveCompanyUrl(String orgNumber) {
        for (String candidateUrl : candidateUrls(orgNumber)) {
            Document page = fetchDocumentOptional(candidateUrl);
            if (page == null) {
                continue;
            }

            if (candidateUrl.contains("/bransjes")) {
                String fromSearch = findCompanyLink(page, orgNumber);
                if (fromSearch != null) {
                    log.debug("Resolved {} via search to {}", orgNumber, fromSearch);
                    return fromSearch;
                }
                continue;
            }

            if (pageContainsOrgNumber(page, orgNumber)) {
                log.debug("Resolved {} via direct URL {}", orgNumber, candidateUrl);
                return candidateUrl;
            }
        }

        throw new ProviderException(
                ProviderException.Reason.NOT_FOUND,
                "Could not resolve Proff URL for org number " + orgNumber
        );
    }

    private List<String> candidateUrls(String orgNumber) {
        List<String> urls = new ArrayList<>();
        urls.add(BASE_URL + "/bransjes%C3%B8k?q=" + orgNumber);

        String companyName = lookupCompanyName(orgNumber);
        if (companyName != null) {
            for (String slug : toSlugVariants(companyName)) {
                urls.add(BASE_URL + "/selskap/" + slug + "/-/-/" + orgNumber);
                urls.add(BASE_URL + "/selskap/" + slug + "/" + orgNumber);
            }
        }

        return urls;
    }

    private String lookupCompanyName(String orgNumber) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BRREG_URL + orgNumber))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode name = root.get("navn");
            return name == null ? null : name.asText();
        } catch (Exception e) {
            log.debug("Brreg lookup failed for {}: {}", orgNumber, e.getMessage());
            return null;
        }
    }

    private Set<String> toSlugVariants(String companyName) {
        Set<String> slugs = new LinkedHashSet<>();
        String normalized = companyName.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        slugs.add(encodePathSegment(normalized.replace(' ', '-')));
        slugs.add(encodePathSegment(normalized.replace(' ', '-')
                .replace("ø", "o")
                .replace("å", "a")
                .replace("æ", "ae")));
        return slugs;
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String findCompanyLink(Document document, String orgNumber) {
        for (Element link : document.select("a[href]")) {
            String href = link.attr("href");
            if (href.contains("/selskap/") && href.contains(orgNumber)) {
                return toAbsoluteUrl(href);
            }
        }

        Matcher matcher = COMPANY_LINK_PATTERN.matcher(document.html());
        while (matcher.find()) {
            if (orgNumber.equals(matcher.group(1))) {
                return toAbsoluteUrl(matcher.group());
            }
        }

        return null;
    }

    private Document fetchDocument(String url) {
        Document document = fetchDocumentOptional(url);
        if (document == null) {
            throw new ProviderException(ProviderException.Reason.NOT_FOUND, "Proff page not found: " + url);
        }
        return document;
    }

    private Document fetchDocumentOptional(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "nb-NO,nb;q=0.9,en;q=0.8")
                    .header("Referer", BASE_URL + "/")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 500) {
                throw new ProviderException(
                        ProviderException.Reason.NETWORK,
                        "Proff returned HTTP " + status + " for " + url
                );
            }
            if (status == 429) {
                throw new ProviderException(
                        ProviderException.Reason.RATE_LIMIT,
                        "Proff rate limit reached for " + url
                );
            }
            if (status == 404 || status == 202) {
                return null;
            }

            Document document = Jsoup.parse(response.body(), url);
            if (isNotFoundPage(document)) {
                return null;
            }

            return document;
        } catch (ProviderException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException(ProviderException.Reason.NETWORK, "Request interrupted", e);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Reason.NETWORK, "Network error fetching " + url, e);
        }
    }

    private boolean pageContainsOrgNumber(Document document, String orgNumber) {
        return document.text().replaceAll("\\s", "").contains(orgNumber);
    }

    private boolean isNotFoundPage(Document document) {
        Element nextData = document.selectFirst("script#__NEXT_DATA__");
        if (nextData != null) {
            String json = nextData.data();
            if (json.contains("\"page\":\"/404\"") || json.contains("\"page\":\"404\"")) {
                return true;
            }
        }

        Element heading = document.selectFirst("h1");
        return heading != null && heading.text().contains("Å nei!");
    }

    private String toAbsoluteUrl(String href) {
        if (href.startsWith("http")) {
            return href;
        }
        if (!href.startsWith("/")) {
            href = "/" + href;
        }
        return BASE_URL + href;
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
}
