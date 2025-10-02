package org.app.service;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VerificareTvaClient implements AutoCloseable {
    private static final String BASE = "https://www.confidas.ro";
    private final CloseableHttpClient http;

    public VerificareTvaClient() {
        RequestConfig rc = RequestConfig.custom()
                .setConnectTimeout(20_000)
                .setSocketTimeout(20_000)
                .build();
        this.http = HttpClients.custom()
                .setDefaultCookieStore(new BasicCookieStore())
                .setDefaultRequestConfig(rc)
                .build();
    }

    /** Returns the company name for a CUI (with or without "RO" prefix). */
    public Optional<String> fetchCompanyNameByCui(String rawCui) throws Exception {
        String cui = normalizeCui(rawCui);
        if (cui.isEmpty()) return Optional.empty();

        // 0) warm-up for cookies / anti-bot
        getHtml(BASE + "/");

        // 1) search results
        String searchUrl = BASE + "/cauta-firma?q=" + URLEncoder.encode("RO" + cui, StandardCharsets.UTF_8.name());
        String html = getHtml(searchUrl);
        Document doc = Jsoup.parse(html, BASE);

        // 2) Try to read name directly from the results table
        Optional<String> nameOnResults = extractNameFromResults(doc, cui);
        return nameOnResults;

//        // 3) Otherwise, follow the first /profil/ link and read from profile
//        Optional<String> firstLink = findFirstCompanyLink(doc, cui);
//        if (firstLink.isPresent()) {
//            String companyHtml = getHtml(firstLink.get());
//            return extractCompanyName(Jsoup.parse(companyHtml, BASE));
//        }
//
//        // 4) (Very unlikely) maybe we're already on the profile page
//        Optional<String> direct = extractCompanyName(doc);
//        if (direct.isPresent()) return direct;

    }

    private String getHtml(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        // Be generous with headers so Cloudflare/CDN gives us the same HTML as browsers
        get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
        get.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        get.addHeader("Accept-Language", "ro-RO,ro;q=0.9,en-US;q=0.8,en;q=0.7");
        get.addHeader("Referer", BASE + "/");
        get.addHeader("Connection", "keep-alive");

        try (CloseableHttpResponse resp = http.execute(get)) {
            HttpEntity entity = resp.getEntity();
            String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            EntityUtils.consumeQuietly(entity);
            return body;
        }
    }

    /** Prefer a /profil/ link that contains the searched CUI digits; else first link in the results table. */
    private static Optional<String> findFirstCompanyLink(Document doc, String cuiDigits) {
        Elements links = doc.select("a[href*=\"/profil/\"]");
        for (Element a : links) {
            String href = a.absUrl("href");
            if (href.matches(".*/profil/.*" + cuiDigits + ".*")) {
                return Optional.of(href);
            }
        }
        Element first = doc.selectFirst("table.responsive-table a[href*=\"/profil/\"]");
        return first != null ? Optional.of(first.absUrl("href")) : Optional.empty();
    }

    /** Pull the company name from the search results table row that matches the CUI. */
    private static Optional<String> extractNameFromResults(Document doc, String cuiDigits) {
        for (Element row : doc.select("table.responsive-table tr")) {
            Element cuiCell = row.selectFirst(".data-cui");
            if (cuiCell != null && cuiCell.text().replaceAll("\\D", "").contains(cuiDigits)) {
                Element nameLink = row.selectFirst("a[href*=\"/profil/\"]");
                if (nameLink != null) return Optional.of(nameLink.text().trim());
            }
        }
        // Fallback: first name link present in results table
        Element nameLink = doc.selectFirst("table.responsive-table a[href*=\"/profil/\"]");
        return nameLink != null ? Optional.of(nameLink.text().trim()) : Optional.empty();
    }

    /** Pull name from a profile page (several selectors + OG title fallback). */
    private static Optional<String> extractCompanyName(Document doc) {
        // Try headings that often host the company name
        for (String sel : new String[]{"h1.company-name", "h1.company__name", "h1.page-title", "h1"}) {
            Element h1 = doc.selectFirst(sel);
            if (h1 != null) {
                String t = h1.text().trim();
                if (!t.isEmpty() && !"Confidas".equalsIgnoreCase(t)) return Optional.of(t);
            }
        }
        // OpenGraph title like "ASSA ABLOY ROMANIA SRL - Profil companie | Confidas"
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null) {
            String t = og.attr("content").trim();
            if (!t.isEmpty()) {
                t = t.replaceFirst("\\s*\\|\\s*Confidas\\s*$", "");
                t = t.replaceFirst("\\s*-\\s*Profil.*$", "");
                if (!t.isEmpty()) return Optional.of(t);
            }
        }
        // Generic table fallback (if any)
        Element row = doc.selectFirst("tr:has(td:matchesOwn(^\\s*(Denumire|Nume)\\s*$))");
        if (row != null) {
            Element val = row.selectFirst("td:nth-of-type(2)");
            if (val != null) {
                String t = val.text().trim();
                if (!t.isEmpty()) return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    private static String normalizeCui(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("RO") || t.startsWith("ro")) t = t.substring(2);
        return t.replaceAll("[^0-9]", "");
    }

    @Override public void close() throws Exception { http.close(); }
}
