package io.playground.scraper.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.neovisionaries.i18n.LocaleCode;
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
import java.nio.file.StandardOpenOption;
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
    public static final String OPEN_LIBRARY_WORK_PATH_PATTERN = "ol_dump_works*.txt";
    public static final String OPEN_LIBRARY_EDITION_PATH_PATTERN = "ol_dump_editions*.txt";

    public static final String OPEN_LIBRARY_AUTHOR_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "author-id-map.json";
    public static final String OPEN_LIBRARY_WORK_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-id-map.json";
    public static final String OPEN_LIBRARY_EDITION_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "edition-id-map.json";

    public static final Charset ENCODING = StandardCharsets.UTF_8;
    public static final int MAX_ENTRY_PER_FILE = 1_000_000;
    private static final Map<String, String> LOCALE_MAP = new HashMap<>();
    static {
        for (LocaleCode locale : LocaleCode.values()) {
            try {
                LOCALE_MAP.put(locale.getLanguage().getAlpha3().getAlpha3B().name(), locale.getLanguage().getName());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }


    public static void main(String[] args) throws IOException {
//        processAuthor();
//        processWork();
        processEdition();
    }

    public static void processEdition() throws IOException {
        long startTime = System.currentTimeMillis();
        List<Path> paths;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(OPEN_LIBRARY_UNPROCESSED_PATH), OPEN_LIBRARY_EDITION_PATH_PATTERN)) {
            paths = StreamSupport.stream(dirStream.spliterator(), false)
                                 .sorted(Comparator.comparing(Path::toString).reversed()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<Integer, Integer> workIdMap = new HashMap<>();
        File workIdMapFile = new File(OPEN_LIBRARY_WORK_ID_MAP_PATH);
        if (workIdMapFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst() + "}";
                workIdMap = JacksonUtil.readValue(line, new TypeReference<>() {});
            }
        }

        int editionId = 0;
        Map<Integer, Integer> editionIdMap = new HashMap<>();
        File editionIdMapFile = new File(OPEN_LIBRARY_EDITION_ID_MAP_PATH);
        if (editionIdMapFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_EDITION_ID_MAP_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst();
                editionId = Integer.parseInt(line.substring(line.lastIndexOf(": ") + 2, line.length() - 2));
                line = line + "}";
                editionIdMap = JacksonUtil.readValue(line, new TypeReference<>() {});
            }
        }


        if (paths.isEmpty()) {
            return;
        }
        Path latestEditionPath = paths.getFirst();

        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "edition-" + timeStamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String workCsvFile = directoryPath + "edition" + ".csv";

        try (BufferedReader reader = Files.newBufferedReader(latestEditionPath, ENCODING);
             BufferedWriter writer = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_EDITION_ID_MAP_PATH), ENCODING, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             BufferedWriter writer1 = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);) {
            writer1.write("title,subtitle,description,pagination,number_of_pages,physical_format,physical_dimensions,weight," +
                                  "isbn_10,isbn_13,oclc_number,lccn_number,dewey_number,lc_classifications," +
                                  "volumns,language,publisher,publish_date,publish_country,publish_place," +
                                  "by_statement,contributions,identifier,cover,ol_key,grade,work_id");
            writer1.newLine();
            if (editionId == 0) {
                writer.write("{");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                int key;
                try {
                    key = Integer.parseInt(line.substring(line.indexOf("/books/OL") + 9, line.indexOf("M")));
                } catch (Exception e) {
                    log.error(line, e);
                    continue;
                }
                if (!editionIdMap.containsKey(key)) {
                    if (!line.contains("\"key\": \"redirect\"")) {
                        line = line.substring(line.indexOf("{"));
                        Edition edition = JacksonUtil.readValue(line, Edition.class);
                        Integer editionWorkKey = edition.workKey();
                        if (editionWorkKey == null || !workIdMap.containsKey(edition.workKey())) {
                            log.info("No available work for edition '{}' ({})", edition.key(), editionWorkKey);
                            continue;
                        }

                        int workKey = workIdMap.get(edition.workKey());
                        if (edition.isbn10() != null && !edition.isbn10().isEmpty()
                                || edition.isbn13() != null && !edition.isbn13().isEmpty()) {
                            int size = 0;
                            if (edition.isbn10() != null && !edition.isbn10().isEmpty()) {
                                size = edition.isbn10().size();
                            }
                            if (edition.isbn13() != null && !edition.isbn13().isEmpty()) {
                                size = Math.max(edition.isbn13().size(), size);
                            }
                            for (int i = 0; i < size; i++) {
                                String isbn10 = edition.isbn10() != null && edition.isbn10().size() > i ? edition.isbn10().get(i) : null;
                                String isbn13 = edition.isbn13() != null && edition.isbn13().size() > i ? edition.isbn13().get(i) : null;
                                String cover = edition.covers() != null && !edition.covers().isEmpty()
                                        && edition.covers().getFirst() != null && !edition.covers().getFirst().equals("-1")
                                        ? edition.covers().getFirst() : null;
                                int grade = 0;
                                if (edition.description() != null) {
                                    grade += 5;
                                    if (edition.description().length() > 20) {
                                        grade += 20;
                                    }
                                }
                                grade += cover != null ? 15 : 0;
                                grade += isbn10 != null ? 20 : 0;
                                grade += isbn13 != null ? 20 : 0;
                                grade += edition.publisher() != null ? 10 : 0;
                                grade += edition.publishDate() != null ? 5 : 0;
                                grade += edition.numberOfPages() != null ? 5 : 0;

                                String value = toData(edition.title()) + toData(edition.subtitle()) + toData(edition.description())
                                        + toData(edition.pagination()) + toData(String.valueOf(edition.numberOfPages()))
                                        + toData(edition.physicalFormat()) + toData(edition.physicalDimensions())
                                        + toData(edition.weight()) + toData(isbn10) + toData(isbn13)
                                        + toData(edition.oclcNumber()) + toData(edition.lccnNumber()) + toData(edition.deweyNumber())
                                        + toData(edition.lcClassification()) + toData(String.valueOf(edition.numberOfVolumes()))
                                        + toData(edition.language()) + toData(edition.publisher()) + toData(edition.publishDate())
                                        + toData(edition.publishCountry()) + toData(edition.publishPlace()) + toData(edition.byStatement())
                                        + toData(edition.contributions()) + toData(edition.identifiers()) + toData(cover)
                                        + toData(String.valueOf(edition.olKey())) + toData(String.valueOf(grade))
                                        + toData(String.valueOf(workKey), true);
                                writer1.write(value);
                                writer1.newLine();
                                editionId++;
                                writer.write("\"" + edition.olKey() + "\": " + editionId + ", ");
                            }
                        } else {
                            String isbn10 = null;
                            String isbn13 = null;
                            String cover = edition.covers() != null && !edition.covers().isEmpty()
                                    && edition.covers().getFirst() != null && !edition.covers().getFirst().equals("-1")
                                    ? edition.covers().getFirst() : null;
                            int grade = 0;
                            if (edition.description() != null) {
                                grade += 5;
                                if (edition.description().length() > 20) {
                                    grade += 20;
                                }
                            }
                            grade += cover != null ? 15 : 0;
                            grade += edition.publisher() != null ? 10 : 0;
                            grade += edition.publishDate() != null ? 5 : 0;
                            grade += edition.numberOfPages() != null ? 5 : 0;

                            String value = toData(edition.title()) + toData(edition.subtitle()) + toData(edition.description())
                                    + toData(edition.pagination()) + toData(String.valueOf(edition.numberOfPages()))
                                    + toData(edition.physicalFormat()) + toData(edition.physicalDimensions())
                                    + toData(edition.weight()) + toData(isbn10) + toData(isbn13)
                                    + toData(edition.oclcNumber()) + toData(edition.lccnNumber()) + toData(edition.deweyNumber())
                                    + toData(edition.lcClassification()) + toData(String.valueOf(edition.numberOfVolumes()))
                                    + toData(edition.language()) + toData(edition.publisher()) + toData(edition.publishDate())
                                    + toData(edition.publishCountry()) + toData(edition.publishPlace()) + toData(edition.byStatement())
                                    + toData(edition.contributions()) + toData(edition.identifiers()) + toData(cover)
                                    + toData(String.valueOf(edition.olKey())) + toData(String.valueOf(grade))
                                    + toData(String.valueOf(workKey), true);
                            writer1.write(value);
                            writer1.newLine();
                            editionId++;
                            writer.write("\"" + edition.olKey() + "\": " + editionId + ", ");
                        }
                    }
                }
            }
//            List<String> terms = List.of("title", "publish_date", "languages", "genres",
//                                         "series", "physical_format", "number_of_pages", "pagination",
//                                         "lccn", "ocaid", "oclc_numbers", "isbn_10", "isbn_13", "volumes",
//                                         "physical_dimensions", "weight", "publishers", "dewey_decimal_class", "lc_classifications");
//            List<String> terms1 = new ArrayList<>(terms);
//            List<String> terms2 = new ArrayList<>(terms);
//            List<String> terms3 = new ArrayList<>(terms);
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String finalLine = line;
//                terms1.removeIf(t -> {
//                    boolean predicate = finalLine.contains(t + "\": {");
//                    if (predicate) {
//                        log.info(finalLine);
//                    }
//                    return predicate;
//                });
//                terms2.removeIf(t -> {
//                    boolean predicate = finalLine.contains(t + "\": [");
//                    if (predicate) {
//                        log.info(finalLine);
//                    }
//                    return predicate;
//                });
//                terms3.removeIf(t -> {
//                    boolean predicate = finalLine.contains(t + "\": \"");
//                    if (predicate) {
//                        log.info(finalLine);
//                    }
//                    return predicate;
//                });
//            }
        }

        long stopTime = System.currentTimeMillis();
        log.info("Processing editions elapsed time: {}", (stopTime - startTime));
    }

    public static void processWork() throws IOException {
        long startTime = System.currentTimeMillis();
        List<Path> paths;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(OPEN_LIBRARY_UNPROCESSED_PATH), OPEN_LIBRARY_WORK_PATH_PATTERN)) {
            paths = StreamSupport.stream(dirStream.spliterator(), false)
                                 .sorted(Comparator.comparing(Path::toString).reversed()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (paths.isEmpty()) {
            return;
        }
        Path latestWorkPath = paths.getFirst();

        Map<Integer, Integer> authorIdMap = new HashMap<>();
        File authorIdMapFile = new File(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        if (authorIdMapFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst() + "}";
                authorIdMap = JacksonUtil.readValue(line, new TypeReference<>() {});
            }
        }

        int workId = 0;
        Map<Integer, Integer> workIdMap = new HashMap<>();
        File workIdMapFile = new File(OPEN_LIBRARY_WORK_ID_MAP_PATH);
        if (workIdMapFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst();
                workId = Integer.parseInt(line.substring(line.lastIndexOf(": ") + 2, line.length() - 2));
                line = line + "}";
                workIdMap = JacksonUtil.readValue(line, new TypeReference<>() {});
            }
        }

        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "work-" + timeStamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String workCsvFile = directoryPath + "work" + ".csv";
        String workSubjectCsvFile = directoryPath + "work-subject" + ".csv";
        String workAuthorsCsvFile = directoryPath + "work-author" + ".csv";

        try (BufferedReader reader = Files.newBufferedReader(latestWorkPath, ENCODING);
             BufferedWriter writer = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             BufferedWriter writer1 = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
             BufferedWriter writer2 = Files.newBufferedWriter(Path.of(workSubjectCsvFile), ENCODING);
             BufferedWriter writer3 = Files.newBufferedWriter(Path.of(workAuthorsCsvFile), ENCODING)) {
            String line;
            writer1.write("title,cover,ol_key");
            writer1.newLine();
            writer2.write("name,work_id");
            writer2.newLine();
            writer3.write("author_id,work_id");
            writer3.newLine();
            if (workId == 0) {
                writer.write("{");
            }
            while ((line = reader.readLine()) != null) {
                String key = line.substring(line.indexOf("/works/OL") + 9, line.indexOf("W"));
                line = line.substring(line.indexOf("{"));
                if (!workIdMap.containsKey(Integer.parseInt(key))) {
                    if (!line.contains("\"key\": \"redirect\"")) {
                        Work work = JacksonUtil.readValue(line, Work.class);
                        String value = toData(work.title()) + toData(work.cover()) + toData(String.valueOf(work.olKey()), true);
                        writer1.write(value);
                        writer1.newLine();
                        workId++;
                        writer.write("\"" + work.olKey() + "\": " + workId + ", ");

                        if (work.subjects() != null && !work.subjects().isEmpty()) {
                            for (String subject : work.subjects()) {
                                writer2.write(toData(subject) + workId);
                                writer2.newLine();
                            }
                        }

                        if (work.authors() != null && !work.authors().isEmpty()) {
                            for (String authorOlKey : work.authors()) {
                                int authorKey = Integer.parseInt(authorOlKey.substring(authorOlKey.indexOf("OL") + 2, authorOlKey.indexOf("A")));
                                writer3.write(toData(String.valueOf(authorIdMap.get(authorKey))) + workId);
                                writer3.newLine();
                            }
                        }
                    }
                }
            }
        }
        long stopTime = System.currentTimeMillis();
        log.info("Processing works elapsed time: {}", (stopTime - startTime));
    }



    public static void processAuthor() throws IOException {
        long startTime = System.currentTimeMillis();
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
        Path latestAuthorPath = paths.getFirst();

        int authorId = 0;
        Map<Integer, Integer> authorIdMap = new HashMap<>();
        File authorIdMapFile = new File(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        if (authorIdMapFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst();
                authorId = Integer.parseInt(line.substring(line.lastIndexOf(": ") + 2, line.length() - 2));
                line = line + "}";
                authorIdMap = JacksonUtil.readValue(line, new TypeReference<>() {});
            }
        }

        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "author-" + timeStamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String authorCsvFile = directoryPath + "author" + ".csv";
        String authorAlternateNameCsvFile = directoryPath + "author-alternate-name" + ".csv";
        String authorLinkCsvFile = directoryPath + "author-link" + ".csv";
        try (BufferedReader reader = Files.newBufferedReader(latestAuthorPath, ENCODING);
             BufferedWriter writer = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             BufferedWriter writer1 = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
             BufferedWriter writer2 = Files.newBufferedWriter(Path.of(authorAlternateNameCsvFile), ENCODING);
             BufferedWriter writer3 = Files.newBufferedWriter(Path.of(authorLinkCsvFile), ENCODING)) {
            String line;
            writer1.write("name,birth_date,death_date,date,bio,photo,ol_key");
            writer1.newLine();
            writer2.write("name,author_id");
            writer2.newLine();
            writer3.write("title,url,author_id");
            writer3.newLine();
            if (authorId == 0) {
                writer.write("{");
            }
            while ((line = reader.readLine()) != null) {
                String key = line.substring(line.indexOf("/authors/OL") + 11, line.indexOf("A"));
                line = line.substring(line.indexOf("{"));
                if (!authorIdMap.containsKey(Integer.parseInt(key))) {
                    Author author = JacksonUtil.readValue(line, Author.class);
                    String value = toData(author.name()) + toData(author.birthDate()) + toData(author.deathDate())
                            + toData(author.date()) + toData(author.bio())
                            + toData(author.photo()) + toData(String.valueOf(author.olKey()), true);
                    writer1.write(value);
                    writer1.newLine();
                    authorId++;
                    writer.write("\"" + author.olKey() + "\": " + authorId + ", ");

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

        long stopTime = System.currentTimeMillis();
        log.info("Processing authors elapsed time: {}", (stopTime - startTime));
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
//                         String personalName,
                         String title,
                         String birthDate,
                         String deathDate,
                         String date,
//                         String wikipedia,
                         String key,
                         @JsonProperty("bio") JsonNode bioNode,
                         List<String> alternateNames,
                         List<Link> links,
                         List<String> photos) {
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
                return photos.getFirst();
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
//                       String subtitle,
                       String key,
//                       @JsonProperty("description") JsonNode descriptionNode,
//                       @JsonProperty("first_sentence") JsonNode firstSentenceNode,
                       @JsonProperty("authors") JsonNode authorNodes,
//                       @JsonProperty("type") NodeKey typeNode,
//                       String notes,
//                       @JsonProperty("deweyNumber") List<String> deweyNumbers,
//                       List<String> lcClassifications,
                       List<String> subjects,
//                       List<String> subjectPlaces,
//                       List<String> subjectPeople,
//                       List<String> subjectTimes,
                       List<String> covers) {
        public List<String> authors() {
            if (authorNodes != null) {
                if (authorNodes.isTextual()) {
                    return List.of(authorNodes.asText());
                } else {
                    try {
                        List<AuthorNode> nodes = JacksonUtil.readValue(authorNodes, new TypeReference<>() {});
                        return nodes.stream().map(node -> node.author().key()).collect(Collectors.toList());
                    } catch (Exception ignored) {
                    }
                }
            }
            return List.of();
        }

//        public String type() {
//            return typeNode.key();
//        }

//        public String deweyNumber() {
//            if (deweyNumbers != null && !deweyNumbers.isEmpty()) {
//                return String.join(", ", deweyNumbers.stream().filter(d -> d != null && !d.isEmpty()).toList());
//            }
//            return null;
//        }
//
//        public String lcClassification() {
//            if (lcClassifications != null && !lcClassifications.isEmpty()) {
//                return String.join(", ", lcClassifications.stream().filter(l -> l != null && !l.isEmpty()).toList());
//            }
//            return null;
//        }

        public String cover() {
            if (covers != null && !covers.isEmpty()) {
                for (String cover : covers()) {
                    if (cover != null && !cover.equals("-1")) {
                        return cover;
                    }
                }
            }
            return null;
        }

//        public String description() {
//            if (descriptionNode != null) {
//                if (descriptionNode.isTextual()) {
//                    return descriptionNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
//                } else if (descriptionNode.isObject()) {
//                    return JacksonUtil.readValue(descriptionNode, NodeTypeValue.class).value().replaceAll("\\r|\\n|\\r\\n", " ");
//                }
//            }
//            return null;
//        }

//        public String firstSentence() {
//            if (firstSentenceNode != null) {
//                if (firstSentenceNode.isTextual()) {
//                    return firstSentenceNode.asText().replaceAll("\\r|\\n|\\r\\n", " ");
//                } else if (firstSentenceNode.isObject()) {
//                    return JacksonUtil.readValue(firstSentenceNode, NodeTypeValue.class).value().replaceAll("\\r|\\n|\\r\\n", " ");
//                }
//            }
//            return null;
//        }

        public int olKey() {
            return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("W")));
        }

    }

    public record AuthorNode(NodeKey author) {
    }

    public record NodeKey(String key) {
    }

    public record Edition(String title,
                          String subtitle,
                          @JsonProperty("description") JsonNode descriptionNode,
                          List<NodeKey> works,
                          String key,

                          String pagination,
                          Integer numberOfPages,
                          String physicalFormat,
                          String physicalDimensions,
                          String weight,

                          List<String> isbn10,
                          List<String> isbn13,
                          List<String> oclcNumbers,
                          List<String> lccn,
                          List<String> deweyDecimalClass,
                          List<String> lcClassifications,

                          List<Volumes> volumes,
                          List<NodeKey> languages,
                          List<String> publishers,
                          String publishDate,
                          String publishCountry,
                          List<String> publishPlaces,

                          String byStatement,
                          @JsonProperty("contributions") List<String> contributionsNode,
                          @JsonProperty("identifiers") Identifiers identifiersNode,

                          List<String> covers) {

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

        public String publisher() {
            if (publishers != null && !publishers.isEmpty()) {
                return publishers.getFirst();
            }
            return null;
        }

        public String language() {
            if (languages != null && !languages.isEmpty()) {
                String key = languages.getFirst().key();
                String code = key.substring(11);
                return LOCALE_MAP.getOrDefault(code, null);
            }
            return null;
        }

        public Integer numberOfVolumes() {
            if (volumes != null) {
                return volumes.size();
            }
            return null;
        }

        public String publishPlace() {
            if (publishPlaces != null && !publishPlaces.isEmpty()) {
                return publishPlaces.getFirst();
            }
            return null;
        }

        public String identifiers() {
            if (identifiersNode != null) {
                StringBuilder sb = new StringBuilder();
                if (identifiersNode.goodreads() != null && !identifiersNode.goodreads().isEmpty()) {
                    sb.append("goodreads:").append(identifiersNode.goodreads().getFirst());
                }
                if (identifiersNode.librarything() != null && !identifiersNode.librarything().isEmpty()) {
                    sb.append("librarything:").append(identifiersNode.librarything().getFirst());
                }
                return sb.toString();
            }
            return null;
        }

        public String lcClassification() {
            if (lcClassifications != null && !lcClassifications.isEmpty()) {
                return lcClassifications.getFirst();
            }
            return null;
        }

        public String deweyNumber() {
            if (deweyDecimalClass != null && !deweyDecimalClass.isEmpty()) {
                return deweyDecimalClass.getFirst();
            }
            return null;
        }

        public String lccnNumber() {
            if (lccn != null && !lccn.isEmpty()) {
                return lccn.getFirst();
            }
            return null;
        }

        public String oclcNumber() {
            if (oclcNumbers != null && !oclcNumbers.isEmpty()) {
                return oclcNumbers.getFirst();
            }
            return null;
        }

        public String contributions() {
            if (contributionsNode != null && !contributionsNode.isEmpty()) {
                return String.join("; ", contributionsNode());
            }
            return null;
        }

        public int olKey() {
            return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("M")));
        }

        public Integer workKey() {
            if (works != null && !works.isEmpty()) {
                String work = works.getFirst().key();
                return Integer.parseInt(work.substring(work.indexOf("OL") + 2, work.indexOf("W")));
            }
            return null;
        }
    }

    public record Identifiers(List<String> goodreads, List<String> librarything) {
    }

    public record Volumes(String volumeNumber) {
    }
}
