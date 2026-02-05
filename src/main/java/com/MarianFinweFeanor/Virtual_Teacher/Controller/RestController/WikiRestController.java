package com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController;

import com.MarianFinweFeanor.Virtual_Teacher.Service.WikiServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Wiki.WikiSearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wiki")
public class WikiRestController {
    private final WikiServiceImpl wikiServiceImpl;

    public WikiRestController(WikiServiceImpl wikiServiceImpl) {
        this.wikiServiceImpl = wikiServiceImpl;
    }

    @GetMapping("/search")
    public List<WikiSearchResult> search(@RequestParam("q") String q) {
        return wikiServiceImpl.searchResults(q);
    }
}
