package io.playground.scraper.openlibrary;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    public static final String OPEN_LIBRARY_RATING_PATH_PATTERN = "ol_dump_ratings*.txt";

    public static final String OPEN_LIBRARY_AUTHOR_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "author-id-map.json5";
    public static final String OPEN_LIBRARY_WORK_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-id-map.json5";
    public static final String OPEN_LIBRARY_FILTERED_WORK_ID_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "filtered-work-id-map.json5";
    public static final String OPEN_LIBRARY_AUTHOR_REDIRECT_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "author-redirect-map.json5";
    public static final String OPEN_LIBRARY_WORK_REDIRECT_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "work-redirect-map.json5";
    public static final String OPEN_LIBRARY_EDITION_REDIRECT_MAP_PATH = OPEN_LIBRARY_PROCESSED_PATH + "edition-redirect-map.json5";
    
    public static final String OPEN_LIBRARY_LEFTOVER_TITLE_PATH = OPEN_LIBRARY_PROCESSED_PATH + "leftover-title.txt";
    
    public static final int MAX_NUMBER_OF_FILTERED_WORKS = 500;

    public static final Charset ENCODING = StandardCharsets.UTF_8;
    public static final int MAX_ENTRY_PER_FILE = 2_000_000;

    public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                            .withZone(ZoneId.systemDefault())
                                                            .format(Instant.now());

    protected String fileTimestamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                                      .withZone(ZoneId.systemDefault())
                                                      .format(Instant.now());

    protected void clearOldProcessedFiles(String pathPattern) {
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

    protected Path getLatestFile(String pathPattern) throws IOException {
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

    protected <K, V> Map<K, V> getMapFromJsonFile(String filePath) throws IOException {
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

    protected String toDataWithAudit(Object... values) {
        return toData(values) + auditData();
    }
    
    protected String toLastData(Object... values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i < values.length - 1) {
                result.append(toData(values[i], false));
            } else {
                result.append(toData(values[i], true));
            }
        }
        return result.toString();
    }
    
    protected String toData(Object... values) {
        StringBuilder result = new StringBuilder();
        for (Object value : values) {
            result.append(toData(value, false));
        }
        return result.toString();
    }
    
    protected String toData(Object value) {
        return toData(value, false);
    }

    protected String toData(Object value, boolean isLast) {
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

    protected String msToProperTime(long timeInMs) {
        if (timeInMs == 0) {
            return "0 milliseconds";
        }
        String result = timeInMs + " milliseconds or ";
        if (timeInMs > 3600000) {
            long hours = timeInMs / 3600000;
            result += hours + " hour" + (hours == 1 ? "" : "s");
            timeInMs %= 3600000;
            result += timeInMs > 0 ?  ", " : "";
        }
        if (timeInMs > 60000) {
            long minutes = timeInMs / 60000;
            result += minutes + " minute" + (minutes == 1 ? "" : "s");
            timeInMs = timeInMs % 60000;
            result += timeInMs > 0 ?  ", " : "";
        }
        if (timeInMs > 1000) {
            long seconds = timeInMs / 1000;
            result += seconds + " second" + (seconds == 1 ? "" : "s");
            timeInMs = timeInMs % 1000;
            result += timeInMs > 0 ?  ", " : "";
        }
        if (timeInMs > 0) {
            result += timeInMs + " millisecond" + (timeInMs == 1 ? "" : "s");
        }
        return result;
    }

    protected String auditTitle() {
        return ",last_created_by,last_created_at,last_modified_by,last_modified_at";
    }

    protected String auditData() {
        return toLastData("a2bec9ef-28a6-41cb-937b-adaf51dd5ca7", TIMESTAMP, "a2bec9ef-28a6-41cb-937b-adaf51dd5ca7", TIMESTAMP);
    }
    
    protected Set<String> getEnglishWords() throws IOException {
        Set<String> words = new HashSet<>();
        String englishWordsPath = Constant.RESOURCES_PATH + "words_alpha.txt";
        try (BufferedReader reader = Files.newBufferedReader(Path.of(englishWordsPath), ENCODING);) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!(line = line.trim().toLowerCase()).isEmpty()) {
                    words.add(line);
                }
            }
        }
        return words;
    }
}
