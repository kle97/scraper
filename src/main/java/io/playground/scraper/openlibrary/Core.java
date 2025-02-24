package io.playground.scraper.openlibrary;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Core {
    
    public static void main(String[] args) throws IOException {
        var redirectProcessor = new RedirectProcessor();
        var authorProcessor = new AuthorProcessor();
        var workProcessor = new WorkProcessor();
        var editionProcessor = new EditionProcessor();
        
        redirectProcessor.processRedirect();
        authorProcessor.processAuthor();
        workProcessor.processWork();
        editionProcessor.processEdition();
    }
}
    