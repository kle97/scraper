package io.playground.scraper.openlibrary;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.openlibrary.model.Author;
import io.playground.scraper.openlibrary.model.Link;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class AuthorProcessor extends BaseProcessor {

    public void processAuthor() throws IOException {
        long startTime = System.currentTimeMillis();
        Path latestAuthorPath = getLatestFile(OPEN_LIBRARY_AUTHOR_PATH_PATTERN);
        if (latestAuthorPath == null) {
            log.error("There's no unprocessed author file!");
            return;
        }
        clearOldProcessedFiles("author-*");
        
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "author-" + fileTimestamp + Constant.SEPARATOR;
        Files.createDirectories(Path.of(directoryPath));

        String authorCsvFile = directoryPath + "author" + ".csv";
        String authorAlternateNameCsvFile = directoryPath + "author-alternate-name" + ".csv";
        String authorLinkCsvFile = directoryPath + "author-link" + ".csv";

        int currentAuthorId = 0;
        int startAuthorId = currentAuthorId;
        BufferedWriter authorWriter = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
        try (BufferedReader authorReader = Files.newBufferedReader(latestAuthorPath, ENCODING);
             BufferedWriter authorAltNameWriter = Files.newBufferedWriter(Path.of(authorAlternateNameCsvFile), ENCODING);
             BufferedWriter authorLinkWriter = Files.newBufferedWriter(Path.of(authorLinkCsvFile), ENCODING);
             BufferedWriter authorIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING);
        ) {
            String line;
            String authorHeader = "author_name,birth_date,death_date,author_date,biography,photo,ol_key" + auditTitle();
            String authorAltNameHeader = "alternate_name,author_id" + auditTitle();
            String authorLinkHeader = "title,url,author_id" + auditTitle();
            authorWriter.write(authorHeader);
            authorWriter.newLine();
            authorAltNameWriter.write(authorAltNameHeader);
            authorAltNameWriter.newLine();
            authorLinkWriter.write(authorLinkHeader);
            authorLinkWriter.newLine();

            authorIdMapWriter.write("{");
            while ((line = authorReader.readLine()) != null) {
                if (currentAuthorId - startAuthorId >= MAX_ENTRY_PER_FILE) {
                    authorWriter.close();
                    authorCsvFile = directoryPath + "author-" + currentAuthorId + ".csv";
                    authorWriter = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
                    authorWriter.write(authorHeader);
                    authorWriter.newLine();
                    startAuthorId = currentAuthorId;
                }
                
                line = line.substring(line.indexOf("{"));
                Author author = JacksonUtil.readValue(line, Author.class);
                String value = toDataWithAudit(author.name(), author.birthDate(), author.deathDate(), author.date(), 
                                               author.bio(), author.photo(), author.key());
                authorWriter.write(value);
                authorWriter.newLine();
                currentAuthorId++;
                authorIdMapWriter.write("\"" + author.olKey() + "\": " + currentAuthorId + ", ");

                if (author.alternateNames() != null) {
                    for (String alternateName : author.alternateNames()) {
                        authorAltNameWriter.write(toDataWithAudit(alternateName, currentAuthorId));
                        authorAltNameWriter.newLine();
                    }
                }

                if (author.links() != null) {
                    for (Link link : author.links()) {
                        authorLinkWriter.write(toDataWithAudit(link.title(), link.url(), currentAuthorId));
                        authorLinkWriter.newLine();
                    }
                }
            }
            authorIdMapWriter.write("}");
            authorWriter.close();
        }

        long stopTime = System.currentTimeMillis();
        log.info("Processing authors elapsed time: {}", msToProperTime(stopTime - startTime));
    }
}
