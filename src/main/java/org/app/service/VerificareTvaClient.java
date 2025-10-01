package org.app.service;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VerificareTvaClient implements AutoCloseable {

    private static final String BASE = "https://www.verificaretva.ro/";
    private final CloseableHttpClient http;
    private final BasicCookieStore cookieStore = new BasicCookieStore();

    public VerificareTvaClient() {
        RequestConfig rc = RequestConfig.custom()
                .setConnectTimeout(20_000)
                .setSocketTimeout(20_000)
                .build();

        this.http = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(rc)
                .build();
    }

    public Optional<String> fetchCompanyNameByCui(String cui) throws Exception {
        // 1) GET homepage – collect cookies + hidden fields
        HttpGet get = new HttpGet(BASE);
        get.addHeader("User-Agent", "Mozilla/5.0");
        String getHtml;
        try (CloseableHttpResponse resp = http.execute(get)) {
            HttpEntity entity = resp.getEntity();
            getHtml = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            EntityUtils.consumeQuietly(entity);
        }

        Document doc = Jsoup.parse(getHtml);
        String viewstate          = attrVal(doc, "#__VIEWSTATE", "value");
        String viewstateGenerator = attrVal(doc, "#__VIEWSTATEGENERATOR", "value");
        String eventValidation    = attrVal(doc, "#__EVENTVALIDATION", "value");

        // 2) POST the form back
        HttpPost post = new HttpPost(BASE);
        post.addHeader("User-Agent", "Mozilla/5.0");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");

        List<BasicNameValuePair> form = new ArrayList<BasicNameValuePair>();
        form.add(new BasicNameValuePair("__EVENTTARGET", ""));
        form.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
        form.add(new BasicNameValuePair("__VIEWSTATE", viewstate));
        form.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", viewstateGenerator));
        form.add(new BasicNameValuePair("__EVENTVALIDATION", eventValidation));
        form.add(new BasicNameValuePair("ctl00$content$txtSearch", cui));
        form.add(new BasicNameValuePair("ctl00$content$txtData", ""));          // disabled in UI; ok to send empty
        form.add(new BasicNameValuePair("ctl00$content$Button1", "Cautare"));   // MUST include submit value

        post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8.name()));

        String postHtml;
        try (CloseableHttpResponse resp = http.execute(post)) {
            HttpEntity entity = resp.getEntity();
            postHtml = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            EntityUtils.consumeQuietly(entity);
        }

        // 3) Parse result table
        Document res = Jsoup.parse(postHtml);
        Element table = res.selectFirst("#content_tblRezultat");
        if (table == null) return Optional.empty();

        Element nameCell = table.selectFirst(
                "tr:has(td:matchesOwn(^\\s*Denumire\\s+Anaf\\s*$)) td:nth-of-type(2), " +
                        "tr:has(th:matchesOwn(^\\s*Denumire\\s+Anaf\\s*$)) th:nth-of-type(2)"
        );
        if (nameCell == null) return Optional.empty();

        String company = nameCell.text().trim();
        return company.isEmpty() ? Optional.empty() : Optional.of(company);
    }

    private static String attrVal(Document doc, String css, String attr) {
        Element e = doc.selectFirst(css);
        return e != null ? e.attr(attr) : "";
    }

    @Override
    public void close() throws Exception {
        http.close();
    }
}
