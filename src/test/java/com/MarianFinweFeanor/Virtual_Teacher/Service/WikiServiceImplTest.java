package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Wiki.WikiSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private WikiServiceImpl wikiService;

    @BeforeEach
    void setUp() {
        wikiService = new WikiServiceImpl(restTemplate);
    }

    @Test
    void searchResults_shouldReturnEmptyList_whenQueryIsNull() {
        List<WikiSearchResult> result =
                wikiService.searchResults(null);

        assertTrue(result.isEmpty());

        verifyNoInteractions(restTemplate);
    }

    @Test
    void searchResults_shouldReturnEmptyList_whenQueryIsBlank() {
        List<WikiSearchResult> result =
                wikiService.searchResults("   ");

        assertTrue(result.isEmpty());

        verifyNoInteractions(restTemplate);
    }

    @Test
    void searchResults_shouldReturnEmptyList_whenResponseBodyIsNull() {
        ResponseEntity<Map> response =
                ResponseEntity.ok(null);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchResults_shouldReturnEmptyList_whenQueryObjectIsMissing() {
        Map<String, Object> body = Map.of(
                "somethingElse",
                "value"
        );

        ResponseEntity<Map> response =
                ResponseEntity.ok(body);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchResults_shouldParseWikipediaSearchResults() {
        Map<String, Object> item = Map.of(
                "pageid", 123,
                "title", "Java",
                "snippet", "Java is a <span>programming</span> language"
        );

        Map<String, Object> query = Map.of(
                "search",
                List.of(item)
        );

        Map<String, Object> body = Map.of(
                "query",
                query
        );

        ResponseEntity<Map> response =
                ResponseEntity.ok(body);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertEquals(1, result.size());

        WikiSearchResult first = result.get(0);

        assertEquals(123L, first.pageId());
        assertEquals("Java", first.title());

        // Currently named url(), but the service stores the cleaned snippet here.
        assertEquals(
                "Java is a programming language",
                first.snippet()
        );
    }

    @Test
    void searchResults_shouldReturnEmptyList_whenSearchObjectIsNotAList() {
        Map<String, Object> query = Map.of(
                "search",
                "not-a-list"
        );

        Map<String, Object> body = Map.of(
                "query",
                query
        );

        ResponseEntity<Map> response =
                ResponseEntity.ok(body);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchResults_shouldSkipItemsThatAreNotMaps() {
        Map<String, Object> validItem = Map.of(
                "pageid", 123,
                "title", "Java",
                "snippet", "Programming language"
        );

        Map<String, Object> query = Map.of(
                "search",
                List.of(
                        "invalid item",
                        validItem
                )
        );

        Map<String, Object> body = Map.of(
                "query",
                query
        );

        ResponseEntity<Map> response =
                ResponseEntity.ok(body);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertEquals(1, result.size());
        assertEquals(123L, result.get(0).pageId());
        assertEquals("Java", result.get(0).title());
    }

    @Test
    void searchResults_shouldUseDefaultValues_whenFieldsHaveUnexpectedTypes() {
        Map<String, Object> item = Map.of(
                "pageid", "not-a-number",
                "title", 12345,
                "snippet", 999
        );

        Map<String, Object> query = Map.of(
                "search",
                List.of(item)
        );

        Map<String, Object> body = Map.of(
                "query",
                query
        );

        ResponseEntity<Map> response =
                ResponseEntity.ok(body);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertEquals(1, result.size());

        WikiSearchResult first = result.get(0);

        assertNull(first.pageId());
        assertEquals("", first.title());
        assertEquals("", first.snippet());
    }

    @Test
    void searchResults_shouldReturnAllValidResults() {
        Map<String, Object> firstItem = Map.of(
                "pageid", 1,
                "title", "Java",
                "snippet", "Java language"
        );

        Map<String, Object> secondItem = Map.of(
                "pageid", 2,
                "title", "Spring Framework",
                "snippet", "Java application framework"
        );

        Map<String, Object> query = Map.of(
                "search",
                List.of(firstItem, secondItem)
        );

        Map<String, Object> body = Map.of(
                "query",
                query
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(body));

        List<WikiSearchResult> result =
                wikiService.searchResults("Java");

        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).pageId());
        assertEquals("Java", result.get(0).title());

        assertEquals(2L, result.get(1).pageId());
        assertEquals("Spring Framework", result.get(1).title());
    }

    @Test
    void searchResults_shouldCallWikipediaWithSearchQuery() {
        Map<String, Object> body = Map.of(
                "query",
                Map.of(
                        "search",
                        List.of()
                )
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(body));

        wikiService.searchResults("Spring Boot");

        verify(restTemplate).exchange(
                argThat((String url) ->
                        url.contains("wikipedia.org")
                                && url.contains("srsearch=")
                                && url.contains("Spring")
                ),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }





}