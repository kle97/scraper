package io.playground.scraper.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class Core {
    public static final String OPEN_LIBRARY_PATH = Constant.EXTERNAL_RESOURCES_PATH + "openlibrary" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_UNPROCESSED_PATH = OPEN_LIBRARY_PATH + "unprocessed" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_PROCESSED_PATH = OPEN_LIBRARY_PATH + "processed" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_AUTHOR_PATH_PATTERN = "ol_dump_authors*.txt";
    public static final String OPEN_LIBRARY_WORk_PATH_PATTERN = "ol_dump_works*.txt";

    public static final String OPEN_LIBRARY_AUTHOR_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "author-id-map.json";
    public static final String OPEN_LIBRARY_WORK_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-id-map.json";

    public static final Charset ENCODING = StandardCharsets.UTF_8;
    public static final int MAX_ENTRY_PER_FILE = 1_000_000;

    public static void processWork() throws IOException {
        long startTime = System.currentTimeMillis();

        Map<String, Integer> authorIdMap = new HashMap<>();
        File authorIdMapFile = new File(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        if (authorIdMapFile.exists()) {
            authorIdMap = JacksonUtil.parseJsonFileAsMap(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH, String.class, Integer.class);
        }

        Map<String, Integer> workIdMap = new HashMap<>();
        File workIdMapFile = new File(OPEN_LIBRARY_WORK_ID_MAP_PATH);
        if (workIdMapFile.exists()) {
            workIdMap = JacksonUtil.parseJsonFileAsMap(OPEN_LIBRARY_WORK_ID_MAP_PATH, String.class, Integer.class);
        }

        List<Path> paths;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(OPEN_LIBRARY_UNPROCESSED_PATH), OPEN_LIBRARY_WORk_PATH_PATTERN)) {
            paths = StreamSupport.stream(dirStream.spliterator(), false)
                                 .sorted(Comparator.comparing(Path::toString).reversed()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (paths.isEmpty()) {
            return;
        }
        Path latestWorkPath = paths.get(0);

        List<Work> works = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(latestWorkPath, ENCODING)) {
            String line;
            int firstPublishDate = 0;
            int firstSentence = 0;
            int description = 0;
            int deweyNumber = 0;
            int lcClassifications = 0;
            int originalLanguage = 0;
            int otherTitles = 0;
            int links = 0;
            int notes = 0;
            int translatedTitles = 0;
            int subtitle = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("cover_edition")) {
//                    log.info(line);
                } else if (line.contains("\"first_publish_date\": ")) {
                    firstPublishDate++;
                    if (firstPublishDate == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"first_sentence\": {")) {
                    firstSentence++;
                    if (firstSentence == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"description\": {")) {
                    description++;
                    if (description == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"dewey_number\": ")) {
                    deweyNumber++;
                    if (deweyNumber == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"lc_classifications\": ")) {
                    lcClassifications++;
                    if (lcClassifications == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"original_languages\": ")) {
                    originalLanguage++;
                    if (originalLanguage == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"other_titles\": ")) {
                    otherTitles++;
                    if (otherTitles == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"links\": ")) {
                    links++;
                    if (links == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"notes\": {")) {
                    notes++;
                    if (notes == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"translated_titles\": ")) {
                    translatedTitles++;
                    if (translatedTitles == 1) {
                        log.info(line);
                    }
                } else if (line.contains("\"subtitle\": {")) {
                    subtitle++;
                    if (subtitle == 1) {
                        log.info(line);
                    }
                }
//                String key = line.substring(line.indexOf("/works"), line.indexOf("W") + 1);
//                line = line.substring(line.indexOf("{"));
//                if (!authorIdMap.containsKey(key)) {
//                    Work work = JacksonUtil.readValue(line, Work.class);
//                    if (!work.type().toLowerCase().contains("redirect")) {
//                        works.add(work);
//                    }
//                }
            }
            log.info("firstPublishDate {}", firstPublishDate);
            log.info("firstSentence {}", firstSentence);
            log.info("description {}", description);
            log.info("deweyNumber {}", deweyNumber);
            log.info("lcClassifications {}", lcClassifications);
            log.info("originalLanguage {}", originalLanguage);
            log.info("otherTitles {}", otherTitles);
            log.info("links {}", links);
            log.info("notes {}", notes);
            log.info("translatedTitles {}", translatedTitles);
            log.info("subtitle {}", subtitle);
        }

        long stopTime = System.currentTimeMillis();
        log.info("Reading dump file elapsed time: {}", (stopTime - startTime));
    }
    
    public static void processAuthor() throws IOException {
        long startTime = System.currentTimeMillis();

        Map<String, Integer> authorIdMap = new HashMap<>();
        File authorIdMapFile = new File(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        if (authorIdMapFile.exists()) {
            authorIdMap = JacksonUtil.parseJsonFileAsMap(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH, String.class, Integer.class);
        }

        List<Path> paths;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(OPEN_LIBRARY_UNPROCESSED_PATH), OPEN_LIBRARY_AUTHOR_PATH_PATTERN)) {
            paths = StreamSupport.stream(dirStream.spliterator(), false)
                                 .sorted(Comparator.comparing(Path::toString).reversed()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (paths.isEmpty()) {
            return;
        }
        Path latestAuthorPath = paths.get(0);

        List<Author> authors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(latestAuthorPath, ENCODING)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(line.indexOf("/authors"), line.indexOf("A") + 1);
                line = line.substring(line.indexOf("{"));
                if (!authorIdMap.containsKey(key)) {
                    Author author = JacksonUtil.readValue(line, Author.class);
                    authors.add(author);
                }
            }
        }

        long stopTime = System.currentTimeMillis();
        log.info("Reading dump file elapsed time: {}", (stopTime - startTime));

        if (authors.isEmpty()) {
            return;
        }
        startTime = System.currentTimeMillis();

        authors.sort(Comparator.comparing(Author::olKey));
//        clearProcessedFiles();

        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "author-" + timeStamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());

        int authorId = authorIdMap.getOrDefault("lastId", 0);
        int index = 0;
        int lastIndex;
        String header = "name,birth_date,death_date,date,bio,photo,ol_key";

        while (index < authors.size()) {
            lastIndex = index;
            index = Math.min(lastIndex + MAX_ENTRY_PER_FILE, authors.size());
            String authorCsvFile = directoryPath + "author-" + index + ".csv";
            String authorAlternateNameCsvFile = directoryPath + "author-alternate-name-" + index + ".csv";
            String authorLinkCsvFile = directoryPath + "author-link-" + index + ".csv";
            try (BufferedWriter writer1 = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
                 BufferedWriter writer2 = Files.newBufferedWriter(Path.of(authorAlternateNameCsvFile), ENCODING);
                 BufferedWriter writer3 = Files.newBufferedWriter(Path.of(authorLinkCsvFile), ENCODING)){
                writer1.write(header);
                writer1.newLine();
                writer2.write("name,author_id");
                writer2.newLine();
                writer3.write("title,url,author_id");
                writer3.newLine();
                for(int i = lastIndex; i < index; i++) {
                    Author author = authors.get(i);
                    String value = toData(author.name()) + toData(author.birthDate()) + toData(author.deathDate())
                            + toData(author.date()) + toData(author.bio())
                            + toData(author.photo()) + toData(author.key(), true);
                    writer1.write(value);
                    writer1.newLine();
                    authorId++;
                    authorIdMap.put("/authors/OL" + author.olKey() + "A", authorId);

                    if (author.alternateNames() != null && !author.alternateNames().isEmpty()) {
                        for (String alternateName : author.alternateNames()) {
                            writer2.write(toData(alternateName) + authorId);
                            writer2.newLine();
                        }
                    }

                    if (author.links() != null && !author.links().isEmpty()) {
                        for (Link link : author.links()) {
                            writer3.write(toData(link.title()) + toData(link.url()) + authorId);
                            writer3.newLine();
                        }
                    }
                }
            }
        }
        authorIdMap.put("lastId", authorId);

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING)) {
            writer.write(JacksonUtil.writeValueAsString(authorIdMap));
            writer.newLine();
        }
        stopTime = System.currentTimeMillis();
        log.info("Sorting and processing elapsed time: {}", (stopTime - startTime));
    }

    public static void main(String[] args) throws IOException {
//        processAuthor();
        processWork();
    }
    
    public static void clearProcessedFiles() {
        File directory = new File(OPEN_LIBRARY_PROCESSED_PATH);
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }

    public static String toData(String value) {
        return toData(value, false);
    }
    
    public static String toData(String value, boolean isLast) {
        if (value != null) {
            return isLast ? "'" + value + "'" : "'" + value + "',";
        } else {
            return isLast ? "'null'" : "'null',";
        }
    }
    
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Author(String name,
                         String personalName,
                         String title,
                         String birthDate,
                         String deathDate,
                         String date,
                         String wikipedia,
                         String key,
                         @JsonProperty("bio") JsonNode bioNode,
                         List<String> alternateNames,
                         List<Link> links,
                         List<String> photos,
                         List<String> sourceRecords) {
        public String bio() {
            if (bioNode != null) {
                if (bioNode.isTextual()) {
                    return bioNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
                } else if (bioNode.isObject()) {
                    return JacksonUtil.readValue(bioNode, NodeTypeValue.class).value().replaceAll("\\r|\\n|\\r\\n", " ");
                }
            }
            return null;
        }

        public String photo() {
            if (photos != null && !photos.isEmpty()) {
                return photos.get(0);
            }
            return null;
        }
        
        public int olKey() {
            return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("A")));
        }
    }
    
    public record Link(String title, String url) {
    }
    
    public record NodeTypeValue(String type, String value) {
    }

    public record Work(String title,
                       String subtitle,
                       String key,
                       @JsonProperty("description") JsonNode descriptionNode,
                       @JsonProperty("first_sentence") JsonNode firstSentenceNode,
                       @JsonProperty("authors") List<AuthorNode> authorNodes,
                       @JsonProperty("type") NodeKey typeNode,
                       String notes,
                       List<String> deweyNumber,
                       List<String> lcClassifications,
                       List<String> subjects,
                       List<String> subjectPlaces,
                       List<String> subjectPeople,
                       List<String> subjectTimes,
                       List<String> covers) {
        public List<String> authors() {
            return authorNodes.stream().map(node -> node.author().key()).collect(Collectors.toList());
        }

        public String type() {
            return typeNode.key();
        }

        public String description() {
            if (descriptionNode != null) {
                if (descriptionNode.isTextual()) {
                    return descriptionNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
                } else if (descriptionNode.isObject()) {
                    return JacksonUtil.readValue(descriptionNode, NodeTypeValue.class).value().replaceAll("\\r|\\n|\\r\\n", " ");
                }
            }
            return null;
        }

        public String firstSentence() {
            if (firstSentenceNode != null) {
                if (firstSentenceNode.isTextual()) {
                    return firstSentenceNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
                } else if (firstSentenceNode.isObject()) {
                    return JacksonUtil.readValue(firstSentenceNode, NodeTypeValue.class).value().replaceAll("\\r|\\n|\\r\\n", " ");
                }
            }
            return null;
        }

    }

    public record AuthorNode(NodeKey author) {
    }

    public record NodeKey(String key) {
    }
}
