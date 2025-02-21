package io.playground.scraper.openlibrary;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class BaseProcessor {

    public static final String OPEN_LIBRARY_PATH = Constant.EXTERNAL_RESOURCES_PATH + "openlibrary" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_UNPROCESSED_PATH = OPEN_LIBRARY_PATH + "unprocessed" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_PROCESSED_PATH = OPEN_LIBRARY_PATH + "processed" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_AUTHOR_PATH_PATTERN = "ol_dump_authors*.txt";
    public static final String OPEN_LIBRARY_WORK_PATH_PATTERN = "ol_dump_works*.txt";
    public static final String OPEN_LIBRARY_EDITION_PATH_PATTERN = "ol_dump_editions*.txt";
    public static final String OPEN_LIBRARY_REDIRECT_PATH_PATTERN = "ol_dump_redirects*.txt";

    public static final String OPEN_LIBRARY_AUTHOR_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "author-id-map.json5";
    public static final String OPEN_LIBRARY_WORK_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-id-map.json5";
    public static final String OPEN_LIBRARY_EDITION_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "edition-id-map.json5";
    public static final String OPEN_LIBRARY_WORK_TITLE_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-title.txt";
    public static final String OPEN_LIBRARY_LEFTOVER_TITLE_PATH = OPEN_LIBRARY_PROCESSED_PATH + "leftover-title.txt";

    public static final String OPEN_LIBRARY_FILTERED_WORK_PATH = OPEN_LIBRARY_PROCESSED_PATH + "filtered-work.json5";
    public static final String OPEN_LIBRARY_FILTERED_AUTHOR_PATH = OPEN_LIBRARY_PROCESSED_PATH + "filtered-author.json5";
    private static final Map<String, Integer> filteredWorksMap = new HashMap<>();
    private static final Map<String, Integer> filteredAuthorsMap = new HashMap<>();
    public static final int MAX_NUMBER_OF_FILTERED_WORKS = 100;

    public static final Charset ENCODING = StandardCharsets.UTF_8;
    public static final int MAX_ENTRY_PER_FILE = 2_000_000;

    public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                            .withZone(ZoneId.systemDefault())
                                                            .format(Instant.now());

    protected String fileTimestamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                                      .withZone(ZoneId.systemDefault())
                                                      .format(Instant.now());

    public void clearOldProcessedFiles(String pathPattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**" + Constant.SEPARATOR + pathPattern);
        File[] directories = new File(OPEN_LIBRARY_PROCESSED_PATH).listFiles(file -> file.isDirectory() && matcher.matches(file.toPath()));
        for (File directory : directories) {
            for (File file : directory.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
            directory.delete();
        }
    }

    public Path getLatestFile(String pathPattern) throws IOException {
        List<Path> paths;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(OPEN_LIBRARY_UNPROCESSED_PATH), pathPattern)) {
            paths = StreamSupport.stream(dirStream.spliterator(), false)
                                 .sorted(Comparator.comparing(Path::toString).reversed())
                                 .toList();
        }

        if (paths.isEmpty()) {
            return null;
        }
        return paths.getFirst();
    }

    public <K, V> Map<K, V> getMapFromJsonFile(String filePath) throws IOException {
        Map<K, V> result = new HashMap<>();
        File idMapFile = new File(filePath);
        if (idMapFile.exists()) {
            List<String> lines = Files.readAllLines(Path.of(filePath), ENCODING);
            if (!lines.isEmpty()) {
                String line = lines.getFirst();
                result = JacksonUtil.readValue(line, new TypeReference<>() {});
            }
        }
        return result;
    }

//    public Map<String, Integer> getFilteredMap(String filePath) throws IOException {
//        Map<String, Integer> result = new HashMap<>();
//        if (new File(filePath).exists()) {
//            List<String> lines = Files.readAllLines(Path.of(filePath), ENCODING);
//            if (!lines.isEmpty()) {
//                String line = lines.getFirst();
//                result = JacksonUtil.readValue(line, new TypeReference<>() {});
//            }
//        }
//        return result;
//    }

    public String toData(Object value) {
        return toData(value, false);
    }

    public String toData(Object value, boolean isLast) {
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
