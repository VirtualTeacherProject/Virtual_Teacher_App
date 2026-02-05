package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Wiki.WikiSearchResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class WikiServiceImpl {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<WikiSearchResult> searchResults(String query) {
        if (query == null || query.isBlank()) return List.of();

        String url = UriComponentsBuilder
                .fromHttpUrl("https://en.wikipedia.org/w/api.php")
                .queryParam("action", "query")
                .queryParam("list", "search")
                .queryParam("srsearch", query)
                .queryParam("format", "json")
                .queryParam("utf8", 1)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "VirtualTeacher/1.0" +
                "(https://virtualteacherapp-production.up.railway.app/; " +
                "contact: yolgezerestel@gmail.com)");
        // You can put your GitHub repo link if you don’t have a domain yet.

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        //Map<?, ?> body = restTemplate.getForObject(url, Map.class);
        Map<?, ?> body = response.getBody();
        if (body == null) return List.of();

        // body["query"]["search"] -> List of maps
        Object queryObj = body.get("query");
        if (!(queryObj instanceof Map<?, ?> queryMap)) return List.of();

        Object searchObj = queryMap.get("search");
        if (!(searchObj instanceof List<?> searchList)) return List.of();

        List<WikiSearchResult> results = new ArrayList<>();
        for (Object item : searchList) {
            if (!(item instanceof Map<?, ?> m)) continue;

            // MediaWiki search objects usually contain: pageid, title, snippet

            Object pageIdObj = m.get("pageid");
            Long pageId = (pageIdObj instanceof Number n) ? n.longValue() : null;

            String title = (m.get("title") instanceof String s) ? s : "";
            String snippet = (m.get("snippet") instanceof String s) ? s : "";

            snippet = snippet.replaceAll("<[^>]*>", "");

            results.add(new WikiSearchResult(pageId, title, snippet));
        }

        return results;

    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }


}
