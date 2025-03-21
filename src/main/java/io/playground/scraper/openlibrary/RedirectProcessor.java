package io.playground.scraper.openlibrary;

import io.playground.scraper.openlibrary.model.Redirect;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class RedirectProcessor extends BaseProcessor {

    public void processRedirect() throws IOException {
        long startTime = System.currentTimeMillis();
        Path latestRedirectPath = getLatestFile(OPEN_LIBRARY_REDIRECT_PATH_PATTERN);
        if (latestRedirectPath == null) {
            log.error("There's no unprocessed redirect dump file!");
            return;
        }

        Files.createDirectories(Path.of(OPEN_LIBRARY_PROCESSED_PATH));

        try(BufferedReader redirectReader = Files.newBufferedReader(latestRedirectPath, ENCODING);
            BufferedWriter redirectAuthorMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_AUTHOR_REDIRECT_MAP_PATH), ENCODING);
            BufferedWriter redirectWorkMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_REDIRECT_MAP_PATH), ENCODING);
            BufferedWriter redirectEditionMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_EDITION_REDIRECT_MAP_PATH), ENCODING);
        ) {
            String line;
            redirectAuthorMapWriter.write("{");
            redirectWorkMapWriter.write("{");
            redirectEditionMapWriter.write("{");
            while ((line = redirectReader.readLine()) != null) {
                line = line.substring(line.indexOf("{"));
                Redirect redirect = JacksonUtil.readValue(line, Redirect.class);
                if (redirect.isAuthor()) {
                    redirectAuthorMapWriter.write("\"" + redirect.olKey() + "\": " + redirect.olKeyTarget() + ", ");
                } else if (redirect.isWork()) {
                    redirectWorkMapWriter.write("\"" + redirect.olKey() + "\": " + redirect.olKeyTarget() + ", ");
                } else if (redirect.isEdition()) {
                    redirectEditionMapWriter.write("\"" + redirect.olKey() + "\": " + redirect.olKeyTarget() + ", ");
                }
            }
            redirectAuthorMapWriter.write("}");
            redirectWorkMapWriter.write("}");
            redirectEditionMapWriter.write("}");
        }

        long stopTime = System.currentTimeMillis();
        log.info("Processing redirects elapsed time: {}", msToProperTime(stopTime - startTime));
    }
}
