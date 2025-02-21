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
import java.util.Map;

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

        Map<String, Integer> filteredAuthorsMap = getMapFromJsonFile(OPEN_LIBRARY_FILTERED_AUTHOR_PATH);

        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "author-" + fileTimestamp + Constant.SEPARATOR;
        Files.createDirectories(Path.of(directoryPath));

        String authorCsvFile = directoryPath + "author" + ".csv";
        String authorAlternateNameCsvFile = directoryPath + "author-alternate-name" + ".csv";
        String authorLinkCsvFile = directoryPath + "author-link" + ".csv";
        String filteredAuthorCsvFile = directoryPath + "filtered-author" + ".csv";
        String filteredAuthorAlternateNameCsvFile = directoryPath + "filtered-author-alternate-name" + ".csv";
        String filteredAuthorLinkCsvFile = directoryPath + "filtered-author-link" + ".csv";


        int currentAuthorId = 0;
        int startAuthorId = currentAuthorId;
        BufferedWriter authorWriter = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
        try (BufferedReader authorReader = Files.newBufferedReader(latestAuthorPath, ENCODING);
             BufferedWriter authorAltNameWriter = Files.newBufferedWriter(Path.of(authorAlternateNameCsvFile), ENCODING);
             BufferedWriter authorLinkWriter = Files.newBufferedWriter(Path.of(authorLinkCsvFile), ENCODING);
             BufferedWriter filteredAuthorWriter = Files.newBufferedWriter(Path.of(filteredAuthorCsvFile), ENCODING);
             BufferedWriter filteredAuthorAltNameWriter = Files.newBufferedWriter(Path.of(filteredAuthorAlternateNameCsvFile), ENCODING);
             BufferedWriter filteredAuthorLinkWriter = Files.newBufferedWriter(Path.of(filteredAuthorLinkCsvFile), ENCODING);
             BufferedWriter authorIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING);
        ) {
            String line;
            String title = "name,birth_date,death_date,date,bio,photo,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at";
            authorWriter.write(title);
            authorWriter.newLine();
            authorAltNameWriter.write("name,author_id");
            authorAltNameWriter.newLine();
            authorLinkWriter.write("title,url,author_id");
            authorLinkWriter.newLine();

            filteredAuthorWriter.write(title);
            filteredAuthorWriter.newLine();
            filteredAuthorAltNameWriter.write("name,author_id");
            filteredAuthorAltNameWriter.newLine();
            filteredAuthorLinkWriter.write("title,url,author_id");
            filteredAuthorLinkWriter.newLine();
            authorIdMapWriter.write("{");
            while ((line = authorReader.readLine()) != null) {
                if (currentAuthorId - startAuthorId >= MAX_ENTRY_PER_FILE) {
                    authorWriter.close();
                    authorCsvFile = directoryPath + "author-" + currentAuthorId + ".csv";
                    authorWriter = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
                    authorWriter.write(title);
                    authorWriter.newLine();
                    startAuthorId = currentAuthorId;
                }

                String key = line.substring(line.indexOf("/authors/OL") + 11, line.indexOf("A"));
                line = line.substring(line.indexOf("{"));
                Author author = JacksonUtil.readValue(line, Author.class);
                String value = toData(author.name()) + toData(author.birthDate()) + toData(author.deathDate())
                        + toData(author.date()) + toData(author.bio())
                        + toData(author.photo()) + toData("OL" + author.olKey() + "A")
                        + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP, true);
                authorWriter.write(value);
                authorWriter.newLine();

                if (filteredAuthorsMap.containsKey(key)) {
                    filteredAuthorWriter.write(value);
                    filteredAuthorWriter.newLine();
                }

                currentAuthorId++;
                authorIdMapWriter.write("\"" + author.olKey() + "\": " + currentAuthorId + ", ");

                if (author.alternateNames() != null && !author.alternateNames().isEmpty()) {
                    for (String alternateName : author.alternateNames()) {
                        authorAltNameWriter.write(toData(alternateName) + currentAuthorId);
                        authorAltNameWriter.newLine();

                        if (filteredAuthorsMap.containsKey(key)) {
                            filteredAuthorAltNameWriter.write(toData(alternateName) + filteredAuthorsMap.get(key));
                            filteredAuthorAltNameWriter.newLine();
                        }
                    }
                }

                if (author.links() != null && !author.links().isEmpty()) {
                    for (Link link : author.links()) {
                        authorLinkWriter.write(toData(link.title()) + toData(link.url()) + currentAuthorId);
                        authorLinkWriter.newLine();

                        if (filteredAuthorsMap.containsKey(key)) {
                            filteredAuthorLinkWriter.write(toData(link.title()) + toData(link.url()) + filteredAuthorsMap.get(key));
                            filteredAuthorLinkWriter.newLine();
                        }
                    }
                }
            }
            authorWriter.close();
        }

        long stopTime = System.currentTimeMillis();
        log.info("Processing authors elapsed time: {}", (stopTime - startTime));
    }
}
