package com.dga.resource.controller;

import com.dga.resource.entity.ResourceLink;
import com.dga.resource.repository.ResourceLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.IDN;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin
public class ResourceLinkController {

    @Autowired
    private ResourceLinkRepository repository;

    public static class QuickAddRequest {
        private String url;
        private String category;
        private String env;
        private Boolean recommended;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        public Boolean getRecommended() {
            return recommended;
        }

        public void setRecommended(Boolean recommended) {
            this.recommended = recommended;
        }
    }

    @GetMapping
    public List<ResourceLink> list() {
        List<ResourceLink> existing = repository.findByIsDeletedFalseOrderByRecommendedDescSortOrderAscNameAsc();
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }

        ResourceLink luna = new ResourceLink();
        luna.setName("测试环境堡垒机（Luna）");
        luna.setUrl("http://10.0.19.86/luna/");
        luna.setDescription("堡垒机 - 仅限内网访问");
        luna.setCategory("DevOps工具");
        luna.setEnv("TEST");
        luna.setRecommended(true);
        luna.setSortOrder(10);

        ResourceLink ambari = new ResourceLink();
        ambari.setName("测试环境 HDP 集群（Ambari）");
        ambari.setUrl("http://10.0.25.14:8080/");
        ambari.setDescription("大数据集群管理控制台");
        ambari.setCategory("大数据组件");
        ambari.setEnv("TEST");
        ambari.setRecommended(true);
        ambari.setSortOrder(20);

        repository.saveAll(Arrays.asList(luna, ambari));
        return repository.findByIsDeletedFalseOrderByRecommendedDescSortOrderAscNameAsc();
    }

    @GetMapping("/favicon")
    public ResponseEntity<byte[]> favicon(@RequestParam("url") String url,
                                          @RequestParam(value = "name", required = false) String name) {
        String input = url == null ? "" : url.trim();
        if (input.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url required");
        }

        String normalized = input;
        if (!normalized.matches("(?i)^https?://.*$")) {
            normalized = "http://" + normalized;
        }

        FaviconData data = null;
        try {
            URL resourceUrl = new URL(normalized);
            String origin = resourceUrl.getProtocol() + "://" + resourceUrl.getHost() + (resourceUrl.getPort() > 0 ? ":" + resourceUrl.getPort() : "");
            data = fetchFavicon(origin + "/favicon.ico");
        } catch (Exception e) {
        }

        if (data == null) {
            try {
                String html = fetchHtml(normalized);
                String iconHref = extractIconHref(html);
                if (iconHref != null && !iconHref.trim().isEmpty()) {
                    URL base = new URL(normalized);
                    URL iconUrl = new URL(base, iconHref.trim());
                    data = fetchFavicon(iconUrl.toString());
                }
            } catch (Exception e) {
            }
        }

        if (data == null) {
            data = generateServiceIcon(normalized, name);
        }

        String contentType = data.contentType;
        MediaType mt;
        try {
            mt = (contentType == null || contentType.trim().isEmpty()) ? MediaType.valueOf("image/x-icon") : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            mt = MediaType.valueOf("image/x-icon");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mt);
        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(data.bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/quick-add")
    public ResourceLink quickAdd(@RequestBody QuickAddRequest req) {
        String input = req == null ? "" : (req.getUrl() == null ? "" : req.getUrl().trim());
        if (input.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url required");
        }

        String normalized = input;
        if (!normalized.matches("(?i)^https?://.*$")) {
            normalized = "http://" + normalized;
        }

        URL u;
        try {
            u = new URL(normalized);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid url");
        }

        String protocol = u.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid url");
        }

        String urlKey = normalized.trim();
        repository.findByUrlAndIsDeletedFalse(urlKey).ifPresent(r -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL already exists");
        });

        String html = fetchHtml(normalized);
        String title = extractTitle(html);
        String desc = extractDescription(html);

        ResourceLink link = new ResourceLink();
        link.setUrl(urlKey);
        link.setName(trimToMax(firstNonEmpty(title, hostLabel(u)), 255));
        link.setDescription(trimToMax(desc, 500));
        link.setCategory(trimToMax(safeTrim(req == null ? null : req.getCategory()), 50));
        link.setEnv(trimToMax(safeTrim(req == null ? null : req.getEnv()), 20));
        link.setRecommended(req != null && req.getRecommended() != null && req.getRecommended());
        return repository.save(link);
    }

    @PostMapping
    public ResourceLink create(@RequestBody ResourceLink resource) {
        if (resource.getUrl() != null) {
            String url = resource.getUrl().trim();
            repository.findByUrlAndIsDeletedFalse(url).ifPresent(r -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL already exists");
            });
        }
        return repository.save(resource);
    }

    @PutMapping("/{id}")
    public ResourceLink update(@PathVariable("id") Long id, @RequestBody ResourceLink resource) {
        ResourceLink existing = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (existing.getDeleted() != null && existing.getDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }

        if (resource.getUrl() != null) {
            String url = resource.getUrl().trim();
            repository.findByUrlAndIsDeletedFalse(url).ifPresent(r -> {
                if (!r.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL already exists");
                }
            });
        }

        if (resource.getName() != null) existing.setName(resource.getName());
        if (resource.getUrl() != null) existing.setUrl(resource.getUrl());
        if (resource.getDescription() != null) existing.setDescription(resource.getDescription());
        if (resource.getCategory() != null) existing.setCategory(resource.getCategory());
        if (resource.getEnv() != null) existing.setEnv(resource.getEnv());
        if (resource.getLogoUrl() != null) existing.setLogoUrl(resource.getLogoUrl());
        if (resource.getRecommended() != null) existing.setRecommended(resource.getRecommended());
        if (resource.getStatus() != null) existing.setStatus(resource.getStatus());
        if (resource.getSortOrder() != null) existing.setSortOrder(resource.getSortOrder());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        ResourceLink existing = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        existing.setDeleted(true);
        repository.save(existing);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonEmpty(String a, String b) {
        String x = a == null ? "" : a.trim();
        if (!x.isEmpty()) return x;
        String y = b == null ? "" : b.trim();
        return y;
    }

    private static String trimToMax(String s, int max) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static String hostLabel(URL u) {
        try {
            String host = u.getHost();
            if (host == null || host.trim().isEmpty()) return "New Site";
            String ascii = IDN.toASCII(host);
            int port = u.getPort();
            if (port > 0) {
                return ascii + ":" + port;
            }
            return ascii;
        } catch (Exception e) {
            return "New Site";
        }
    }

    private static String fetchHtml(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(4500);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("User-Agent", "dga-resource-link-quick-add");

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                return "";
            }

            String contentType = conn.getContentType();
            Charset charset = charsetFromContentType(contentType);

            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                int total = 0;
                int max = 512 * 1024;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > max) break;
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), charset);
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static Charset charsetFromContentType(String ct) {
        if (ct == null) return StandardCharsets.UTF_8;
        String lower = ct.toLowerCase();
        int idx = lower.indexOf("charset=");
        if (idx < 0) return StandardCharsets.UTF_8;
        String part = lower.substring(idx + "charset=".length()).trim();
        int semi = part.indexOf(';');
        String name = (semi >= 0 ? part.substring(0, semi) : part).trim().replace("\"", "");
        if (name.isEmpty()) return StandardCharsets.UTF_8;
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static String extractTitle(String html) {
        if (html == null || html.isEmpty()) return "";
        String t = firstMatch(Pattern.compile("(?is)<title[^>]*>(.*?)</title>"), html);
        return normalizeText(t);
    }

    private static String extractDescription(String html) {
        if (html == null || html.isEmpty()) return "";
        String a = firstMatch(Pattern.compile("(?is)<meta[^>]+name\\s*=\\s*['\\\"]description['\\\"][^>]*content\\s*=\\s*['\\\"](.*?)['\\\"][^>]*>"), html);
        if (a == null || a.trim().isEmpty()) {
            a = firstMatch(Pattern.compile("(?is)<meta[^>]+content\\s*=\\s*['\\\"](.*?)['\\\"][^>]*name\\s*=\\s*['\\\"]description['\\\"][^>]*>"), html);
        }
        if (a == null || a.trim().isEmpty()) {
            a = firstMatch(Pattern.compile("(?is)<meta[^>]+property\\s*=\\s*['\\\"]og:description['\\\"][^>]*content\\s*=\\s*['\\\"](.*?)['\\\"][^>]*>"), html);
        }
        if (a == null || a.trim().isEmpty()) {
            a = firstMatch(Pattern.compile("(?is)<meta[^>]+content\\s*=\\s*['\\\"](.*?)['\\\"][^>]*property\\s*=\\s*['\\\"]og:description['\\\"][^>]*>"), html);
        }
        return normalizeText(a);
    }

    private static String firstMatch(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (!m.find()) return "";
        return m.group(1);
    }

    private static String normalizeText(String s) {
        if (s == null) return "";
        String v = s.replaceAll("(?is)<[^>]+>", " ");
        v = v.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
        v = v.replaceAll("\\s+", " ").trim();
        return v;
    }

    private static FaviconData fetchFavicon(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(1500);
        conn.setReadTimeout(2500);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "image/*");
        conn.setRequestProperty("User-Agent", "dga-resource-favicon-proxy");

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("favicon http status " + status);
        }

        String contentType = conn.getContentType();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (!(ct.startsWith("image/") || ct.startsWith("application/octet-stream"))) {
            throw new RuntimeException("favicon content-type " + ct);
        }

        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            int max = 256 * 1024;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > max) {
                    throw new RuntimeException("favicon too large");
                }
                out.write(buf, 0, n);
            }
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                throw new RuntimeException("favicon empty");
            }
            return new FaviconData(bytes, contentType);
        }
    }

    private static String extractIconHref(String html) {
        if (html == null || html.isEmpty()) return "";
        String a = firstMatch(Pattern.compile("(?is)<link[^>]+rel\\s*=\\s*['\\\"](?:shortcut\\s+icon|icon)['\\\"][^>]*href\\s*=\\s*['\\\"](.*?)['\\\"][^>]*>"), html);
        if (a == null || a.trim().isEmpty()) {
            a = firstMatch(Pattern.compile("(?is)<link[^>]+href\\s*=\\s*['\\\"](.*?)['\\\"][^>]*rel\\s*=\\s*['\\\"](?:shortcut\\s+icon|icon)['\\\"][^>]*>"), html);
        }
        return a == null ? "" : a.trim();
    }

    private static FaviconData generateServiceIcon(String url, String name) {
        String lowerUrl = url == null ? "" : url.toLowerCase();
        String lowerName = name == null ? "" : name.toLowerCase();
        String lower = lowerUrl + " " + lowerName;
        if (lower.contains("oceanbase") || lower.contains("oceabase") || lower.contains("ob")) {
            try {
                String site = "https://www.oceanbase.com/";
                String html = fetchHtml(site);
                String iconHref = extractIconHref(html);
                if (iconHref != null && !iconHref.trim().isEmpty()) {
                    URL base = new URL(site);
                    URL iconUrl = new URL(base, iconHref.trim());
                    return fetchFavicon(iconUrl.toString());
                }
            } catch (Exception e) {
            }
        }

        Color color;
        if (lower.contains("ambari")) {
            color = new Color(0x00, 0x7a, 0xcc);
        } else if (lower.contains("yarn") || lower.contains(":8088")) {
            color = new Color(0x28, 0xa7, 0x45);
        } else if (lower.contains("grafana")) {
            color = new Color(0xf4, 0x51, 0x1e);
        } else if (lower.contains("kibana")) {
            color = new Color(0x00, 0x83, 0xa5);
        } else if (lower.contains("ranger")) {
            color = new Color(0x52, 0xc4, 0xe7);
        } else if (lower.contains("luna") || lower.contains("jump") || lower.contains("bastion")) {
            color = new Color(0x9b, 0x59, 0xb6);
        } else if (lower.contains("oceanbase") || lower.contains("oceabase") || lower.contains("ob")) {
            color = new Color(0x00, 0x6b, 0xd9);
        } else {
            color = new Color(0x95, 0xa5, 0xa6);
        }

        try {
            int size = 32;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(color);
            g.fillRect(0, 0, size, size);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            byte[] bytes = out.toByteArray();
            return new FaviconData(bytes, "image/png");
        } catch (Exception e) {
            return new FaviconData(new byte[0], "image/png");
        }
    }

    private static class FaviconData {
        private final byte[] bytes;
        private final String contentType;

        private FaviconData(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType;
        }
    }
}
