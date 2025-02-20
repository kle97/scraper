package io.playground.scraper.openlibrary;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.openlibrary.model.*;
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
    public static final String OPEN_LIBRARY_WORK_TITLE_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-title.txt";
    public static final String OPEN_LIBRARY_LEFTOVER_TITLE_PATH = OPEN_LIBRARY_PROCESSED_PATH + "leftover-title.txt";

    public static final String OPEN_LIBRARY_FILTERED_WORK_PATH = OPEN_LIBRARY_PROCESSED_PATH + "filtered-work.json";
    public static final String OPEN_LIBRARY_FILTERED_AUTHOR_PATH = OPEN_LIBRARY_PROCESSED_PATH + "filtered-author.json";
    private static final Map<String, Integer> filteredWorksMap = new HashMap<>();
    private static final Map<String, Integer> filteredAuthorsMap = new HashMap<>();
    public static final int MAX_NUMBER_OF_FILTERED_WORKS = 1000;

    public static final Charset ENCODING = StandardCharsets.UTF_8;
    public static final int MAX_ENTRY_PER_FILE = 4_000_000;

    private static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                             .withZone(ZoneId.systemDefault())
                                                             .format(Instant.now());


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

        Map<String, Integer> workTitleMap = new HashMap<>();
        File workTitleFile = new File(OPEN_LIBRARY_WORK_TITLE_PATH);
        if (workTitleFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_WORK_TITLE_PATH), ENCODING);
            for (String line : lines) {
                String[] tokens = line.split(" ///// ");
                if (tokens.length > 1) {
                    workTitleMap.put(tokens[1], Integer.parseInt(tokens[0]));
                }
            }
        }


        filteredWorksMap.clear();
        if (paths.isEmpty()) {
            return;
        }
        Path latestEditionPath = paths.getFirst();

        Map<String, Integer> publisherMap = new HashMap<>();
        Map<String, Integer> filteredPublisherMap = new HashMap<>();

        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "edition-" + timeStamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String editionCsvFile = directoryPath + "edition" + ".csv";
        String publisherCsvFile = directoryPath + "publisher" + ".csv";
        String filteredEditionFile = directoryPath + "filtered-edition" + ".csv";
        String filteredPublisherFile = directoryPath + "filtered-publisher" + ".csv";

        try (BufferedReader reader = Files.newBufferedReader(latestEditionPath, ENCODING);
             BufferedWriter writer1 = Files.newBufferedWriter(Path.of(editionCsvFile), ENCODING);
             BufferedWriter writer2 = Files.newBufferedWriter(Path.of(filteredEditionFile), ENCODING);
             BufferedWriter writer3 = Files.newBufferedWriter(Path.of(publisherCsvFile), ENCODING);
             BufferedWriter writer4 = Files.newBufferedWriter(Path.of(filteredPublisherFile), ENCODING);
             BufferedWriter writer5 = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_LEFTOVER_TITLE_PATH), ENCODING);
        ) {
            writer1.write("title,subtitle,description,pagination,number_of_pages,physical_format,physical_dimensions,weight," +
                                  "isbn_10,isbn_13,oclc_number,lccn_number,dewey_number,lc_classifications," +
                                  "volumns,language,publisher_id,publish_date,publish_country," +
//                                  "publish_place,by_statement,contributions,identifier," +
                                  "cover,ol_key,grade,last_created_by,last_created_at,last_modified_by,last_modified_at,work_id");
            writer1.newLine();
            writer2.write("title,subtitle,description,pagination,number_of_pages,physical_format,physical_dimensions,weight," +
                                  "isbn_10,isbn_13,oclc_number,lccn_number,dewey_number,lc_classifications," +
                                  "volumns,language,publisher_id,publish_date,publish_country," +
//                                  "publish_place,by_statement,contributions,identifier," +
                                  "cover,ol_key,grade,last_created_by,last_created_at,last_modified_by,last_modified_at,work_id");
            writer2.newLine();

            writer3.write("name");
            writer3.newLine();
            writer4.write("name");
            writer4.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Integer.parseInt(line.substring(line.indexOf("/books/OL") + 9, line.indexOf("M")));
                } catch (Exception e) {
                    try {
                        line = line.substring(line.indexOf("{"));
                        Edition edition = JacksonUtil.readValue(line, Edition.class);
                        writer5.write(edition.title());
                        writer5.newLine();
                    } catch (Exception e2) {
                        log.error(e2.getMessage(), e2);
                    }
                    continue;
                }
                line = line.substring(line.indexOf("{"));
                Edition edition = JacksonUtil.readValue(line, Edition.class);
                Integer editionWorkKey = edition.workKey();
                int workKey;
                if (editionWorkKey == null || !workIdMap.containsKey(edition.workKey())) {
                    if (workTitleMap.containsKey(edition.title())) {
                        workKey = workIdMap.get(workTitleMap.get(edition.title()));
                    } else {
                        if (edition.title() != null) {
                            writer5.write(edition.title());
                            writer5.newLine();
                        }
                        continue;
                    }
                } else {
                    workKey = workIdMap.get(edition.workKey());
                }

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

                        String publisherId = "null";
                        if (edition.publisher() != null) {
                            if (publisherMap.containsKey(edition.publisher())) {
                                publisherId = String.valueOf(publisherMap.get(edition.publisher()));
                            } else {
                                int index = publisherMap.size() + 1;
                                publisherMap.put(edition.publisher(), index);
                                publisherId = String.valueOf(index);
                                writer3.write(edition.publisher());
                                writer3.newLine();
                            }
                        }

                        String value = toData(edition.title()) + toData(edition.subtitle()) + toData(edition.description())
                                + toData(edition.pagination()) + toData(edition.numberOfPages())
                                + toData(edition.physicalFormat()) + toData(edition.physicalDimensions())
                                + toData(edition.weight()) + toData(isbn10) + toData(isbn13)
                                + toData(edition.oclcNumber()) + toData(edition.lccnNumber()) + toData(edition.deweyNumber())
                                + toData(edition.lcClassification()) + toData(edition.numberOfVolumes())
                                + toData(edition.language()) + toData(publisherId) + toData(edition.publishDate())
                                + toData(edition.publishCountry())
//                                + toData(edition.publishPlace()) + toData(edition.byStatement())
//                                + toData(edition.contributions()) + toData(edition.identifiers())
                                + toData(cover) + toData("OL" + edition.olKey() + "M") + toData(grade)
                                + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP)
                                + toData(workKey, true);
                        writer1.write(value);
                        writer1.newLine();

                        if (filteredWorksMap.size() < MAX_NUMBER_OF_FILTERED_WORKS && grade >= 85
                                && edition.language() != null && edition.language().equalsIgnoreCase("English")
                                && editionWorkKey != null && !filteredWorksMap.containsKey(String.valueOf(editionWorkKey))) {
                            int filteredWorkId = filteredWorksMap.size() + 1;
                            filteredWorksMap.put(String.valueOf(editionWorkKey), filteredWorkId);
                        }
                        if (editionWorkKey != null && filteredWorksMap.containsKey(String.valueOf(editionWorkKey))) {
                            publisherId = "null";
                            if (edition.publisher() != null) {
                                if (filteredPublisherMap.containsKey(edition.publisher())) {
                                    publisherId = String.valueOf(filteredPublisherMap.get(edition.publisher()));
                                } else {
                                    int index = filteredPublisherMap.size() + 1;
                                    filteredPublisherMap.put(edition.publisher(), index);
                                    publisherId = String.valueOf(index);
                                    writer4.write(edition.publisher());
                                    writer4.newLine();
                                }
                            }
                            value = toData(edition.title()) + toData(edition.subtitle()) + toData(edition.description())
                                    + toData(edition.pagination()) + toData(edition.numberOfPages())
                                    + toData(edition.physicalFormat()) + toData(edition.physicalDimensions())
                                    + toData(edition.weight()) + toData(isbn10) + toData(isbn13)
                                    + toData(edition.oclcNumber()) + toData(edition.lccnNumber()) + toData(edition.deweyNumber())
                                    + toData(edition.lcClassification()) + toData(edition.numberOfVolumes())
                                    + toData(edition.language()) + toData(publisherId) + toData(edition.publishDate())
                                    + toData(edition.publishCountry())
//                                + toData(edition.publishPlace()) + toData(edition.byStatement())
//                                + toData(edition.contributions()) + toData(edition.identifiers())
                                    + toData(cover) + toData("OL" + edition.olKey() + "M") + toData(grade)
                                    + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP)
                                    + toData(filteredWorksMap.get(String.valueOf(editionWorkKey)), true);
                            writer2.write(value);
                            writer2.newLine();
                        }
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

                    String publisherId = "null";
                    if (edition.publisher() != null) {
                        if (publisherMap.containsKey(edition.publisher())) {
                            publisherId = String.valueOf(publisherMap.get(edition.publisher()));
                        } else {
                            int index = publisherMap.size() + 1;
                            publisherMap.put(edition.publisher(), index);
                            publisherId = String.valueOf(index);
                            writer3.write(edition.publisher());
                            writer3.newLine();
                        }
                    }

                    String value = toData(edition.title()) + toData(edition.subtitle()) + toData(edition.description())
                            + toData(edition.pagination()) + toData(edition.numberOfPages())
                            + toData(edition.physicalFormat()) + toData(edition.physicalDimensions())
                            + toData(edition.weight()) + toData(isbn10) + toData(isbn13)
                            + toData(edition.oclcNumber()) + toData(edition.lccnNumber()) + toData(edition.deweyNumber())
                            + toData(edition.lcClassification()) + toData(edition.numberOfVolumes())
                            + toData(edition.language()) + toData(publisherId) + toData(edition.publishDate())
                            + toData(edition.publishCountry())
//                            + toData(edition.publishPlace()) + toData(edition.byStatement())
//                            + toData(edition.contributions()) + toData(edition.identifiers())
                            + toData(cover) + toData("OL" + edition.olKey() + "M") + toData(grade)
                            + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP)
                            + toData(workKey, true);
                    writer1.write(value);
                    writer1.newLine();
                }
            }

            Files.write(Path.of(OPEN_LIBRARY_FILTERED_WORK_PATH), List.of(JacksonUtil.writeValueAsString(filteredWorksMap)),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

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

        filteredWorksMap.clear();
        if (new File(OPEN_LIBRARY_FILTERED_WORK_PATH).exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_FILTERED_WORK_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst();
                filteredWorksMap.putAll(JacksonUtil.readValue(line, new TypeReference<>() {}));
            }
        }

        Set<String> leftoverTitle = new HashSet<>();
        File leftoverTitleFile = new File(OPEN_LIBRARY_LEFTOVER_TITLE_PATH);
        if (leftoverTitleFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_LEFTOVER_TITLE_PATH), ENCODING);
            leftoverTitle.addAll(lines);
        }

        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "work-" + timeStamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String workSubjectCsvFile = directoryPath + "work-subject" + ".csv";
        String workAuthorsCsvFile = directoryPath + "work-author" + ".csv";

        String filteredWorkCsvFile = directoryPath + "filtered-work" + ".csv";
        String filteredWorkSubjectCsvFile = directoryPath + "filtered-work-subject" + ".csv";
        String filteredWorkAuthorCsvFile = directoryPath + "filtered-work-author" + ".csv";

        int workId = 0;
        int startWorkId = workId;
        try (BufferedReader reader = Files.newBufferedReader(latestWorkPath, ENCODING);
             BufferedWriter writer = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING,
                                                             StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
             BufferedWriter writer2 = Files.newBufferedWriter(Path.of(workSubjectCsvFile), ENCODING);
             BufferedWriter writer3 = Files.newBufferedWriter(Path.of(workAuthorsCsvFile), ENCODING);
             BufferedWriter writer4 = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_TITLE_PATH), ENCODING);
             BufferedWriter writer5 = Files.newBufferedWriter(Path.of(filteredWorkCsvFile), ENCODING);
             BufferedWriter writer6 = Files.newBufferedWriter(Path.of(filteredWorkSubjectCsvFile), ENCODING);
             BufferedWriter writer7 = Files.newBufferedWriter(Path.of(filteredWorkAuthorCsvFile), ENCODING);
             ) {
            String line;
            writer2.write("name,work_id");
            writer2.newLine();
            writer3.write("author_id,work_id");
            writer3.newLine();
            writer6.write("name,work_id");
            writer6.newLine();
            writer7.write("author_id,work_id");
            writer7.newLine();
            writer.write("{");

            String workCsvFile = directoryPath + "work" + ".csv";
            BufferedWriter writer1 = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
            writer1.write("title,cover,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at");
            writer1.newLine();
            writer5.write("title,cover,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at");
            writer5.newLine();
            while ((line = reader.readLine()) != null) {
                String key = line.substring(line.indexOf("/works/OL") + 9, line.indexOf("W"));
                line = line.substring(line.indexOf("{"));
                if (workId - startWorkId >= MAX_ENTRY_PER_FILE) {
                    writer1.close();
                    workCsvFile = directoryPath + "work-" + workId + ".csv";
                    writer1 = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
                    writer1.write("title,cover,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at");
                    writer1.newLine();
                    startWorkId = workId;
                }

                Work work = JacksonUtil.readValue(line, Work.class);
                String value = toData(work.title()) + toData(work.cover()) + toData("OL" + work.olKey() + "W")
                        + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP, true);
                writer1.write(value);
                writer1.newLine();
                if (filteredWorksMap.containsKey(key)) {
                    writer5.write(value);
                    writer5.newLine();
                }
                workId++;
                int olKey = work.olKey();
                writer.write("\"" + olKey + "\": " + workId + ", ");


                if (leftoverTitle.contains(work.title())) {
                    writer4.write(olKey + " ///// " + work.title());
                    writer4.newLine();
                }

                if (work.subjects() != null && !work.subjects().isEmpty()) {
                    for (String subject : work.subjects()) {
                        writer2.write(toData(subject) + workId);
                        writer2.newLine();
                        if (filteredWorksMap.containsKey(key)) {
                            writer6.write(toData(subject) + filteredWorksMap.get(key));
                            writer6.newLine();
                        }
                    }
                }

                if (work.authors() != null && !work.authors().isEmpty()) {
                    for (String authorOlKey : work.authors()) {
                        int authorKey = Integer.parseInt(authorOlKey.substring(authorOlKey.indexOf("OL") + 2, authorOlKey.indexOf("A")));
                        writer3.write(toData(authorIdMap.get(authorKey)) + workId);
                        writer3.newLine();

                        if (filteredWorksMap.containsKey(key)) {
                            int filteredAuthorId = filteredAuthorsMap.getOrDefault(String.valueOf(authorKey), filteredAuthorsMap.size() + 1);
                            filteredAuthorsMap.putIfAbsent(String.valueOf(authorKey), filteredAuthorId);
                            writer7.write(toData(filteredAuthorId) + filteredWorksMap.get(key));
                            writer7.newLine();
                        }
                    }
                }
            }
            writer1.close();

            Files.write(Path.of(OPEN_LIBRARY_FILTERED_AUTHOR_PATH), List.of(JacksonUtil.writeValueAsString(filteredAuthorsMap)),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
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
        }

        if (paths.isEmpty()) {
            return;
        }
        Path latestAuthorPath = paths.getFirst();

        int authorId = 0;

        filteredAuthorsMap.clear();
        if (new File(OPEN_LIBRARY_FILTERED_AUTHOR_PATH).exists()) {
            List<String> lines = Files.readAllLines(Path.of(OPEN_LIBRARY_FILTERED_AUTHOR_PATH), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst();
                filteredAuthorsMap.putAll(JacksonUtil.readValue(line, new TypeReference<>() {}));
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

        String filteredAuthorCsvFile = directoryPath + "filtered-author" + ".csv";
        String filteredAuthorAlternateNameCsvFile = directoryPath + "filtered-author-alternate-name" + ".csv";
        String filteredAuthorLinkCsvFile = directoryPath + "filtered-author-link" + ".csv";

        try (BufferedReader reader = Files.newBufferedReader(latestAuthorPath, ENCODING);
             BufferedWriter writer = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH), ENCODING,
                                                             StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
             BufferedWriter writer1 = Files.newBufferedWriter(Path.of(authorCsvFile), ENCODING);
             BufferedWriter writer2 = Files.newBufferedWriter(Path.of(authorAlternateNameCsvFile), ENCODING);
             BufferedWriter writer3 = Files.newBufferedWriter(Path.of(authorLinkCsvFile), ENCODING);
             BufferedWriter writer4 = Files.newBufferedWriter(Path.of(filteredAuthorCsvFile), ENCODING);
             BufferedWriter writer5 = Files.newBufferedWriter(Path.of(filteredAuthorAlternateNameCsvFile), ENCODING);
             BufferedWriter writer6 = Files.newBufferedWriter(Path.of(filteredAuthorLinkCsvFile), ENCODING);
        ) {
            String line;
            writer1.write("name,birth_date,death_date,date,bio,photo,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at");
            writer1.newLine();
            writer2.write("name,author_id");
            writer2.newLine();
            writer3.write("title,url,author_id");
            writer3.newLine();

            writer4.write("name,birth_date,death_date,date,bio,photo,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at");
            writer4.newLine();
            writer5.write("name,author_id");
            writer5.newLine();
            writer6.write("title,url,author_id");
            writer6.newLine();
            writer.write("{");
            while ((line = reader.readLine()) != null) {
                String key = line.substring(line.indexOf("/authors/OL") + 11, line.indexOf("A"));
                line = line.substring(line.indexOf("{"));
                Author author = JacksonUtil.readValue(line, Author.class);
                String value = toData(author.name()) + toData(author.birthDate()) + toData(author.deathDate())
                        + toData(author.date()) + toData(author.bio())
                        + toData(author.photo()) + toData("OL" + author.olKey() + "A")
                        + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP, true);
                writer1.write(value);
                writer1.newLine();
                if (filteredAuthorsMap.containsKey(key)) {
                    writer4.write(value);
                    writer4.newLine();
                }
                authorId++;
                writer.write("\"" + author.olKey() + "\": " + authorId + ", ");

                if (author.alternateNames() != null && !author.alternateNames().isEmpty()) {
                    for (String alternateName : author.alternateNames()) {
                        writer2.write(toData(alternateName) + authorId);
                        writer2.newLine();

                        if (filteredAuthorsMap.containsKey(key)) {
                            writer5.write(toData(alternateName) + filteredAuthorsMap.get(key));
                            writer5.newLine();
                        }
                    }
                }

                if (author.links() != null && !author.links().isEmpty()) {
                    for (Link link : author.links()) {
                        writer3.write(toData(link.title()) + toData(link.url()) + authorId);
                        writer3.newLine();

                        if (filteredAuthorsMap.containsKey(key)) {
                            writer6.write(toData(link.title()) + toData(link.url()) + filteredAuthorsMap.get(key));
                            writer6.newLine();
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

    public static String toData(Object value) {
        return toData(value, false);
    }
    
    public static String toData(Object value, boolean isLast) {
        if (value != null) {
            if (value instanceof String) {
                return isLast ? "'" + value + "'" : "'" + value + "',";
            } else {
                return isLast ? String.valueOf(value) : value + ",";
            }
        } else {
            return isLast ? "null" : "null,";
        }
    }

}
