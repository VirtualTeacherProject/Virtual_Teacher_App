package com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController;

import com.MarianFinweFeanor.Virtual_Teacher.Service.WikiServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Wiki.WikiSearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wiki")
@Tag(name = "Wikipedia", description = "Wikipedia search integration")
public class WikiRestController {
    private final WikiServiceImpl wikiServiceImpl;

    public WikiRestController(WikiServiceImpl wikiServiceImpl) {
        this.wikiServiceImpl = wikiServiceImpl;
    }

    @Operation(summary = "Search Wikipedia articles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query"),
            @ApiResponse(responseCode = "500", description = "Wikipedia API error")
    })

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WikiSearchResult>> search(
            @RequestParam("q") String q) {
        return ResponseEntity.ok(wikiServiceImpl.searchResults(q));
    }
}
