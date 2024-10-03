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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
public class Core {
    public static final String OPEN_LIBRARY_PATH = Constant.EXTERNAL_RESOURCES_PATH + "openlibrary" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_UNPROCESSED_PATH = OPEN_LIBRARY_PATH + "unprocessed" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_PROCESSED_PATH = OPEN_LIBRARY_PATH + "processed" + Constant.SEPARATOR;
    public static final String OPEN_LIBRARY_AUTHOR_PATH_PATTERN = "ol_dump_author*.txt";
    
    public static final Charset ENCODING = StandardCharsets.UTF_8;
    public static final int MAX_ENTRY_PER_FILE = 1_000_000;
    
    public static void processAuthor() {
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
        
        int count = 0;
        String header = "name,birth_date,death_date,date,bio,ol_key";
        List<Author> authors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(latestAuthorPath, ENCODING)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.substring(line.indexOf("{"));
                Author author = JacksonUtil.readValue(line, Author.class);
                authors.add(author);
                count++;
                if (count == 10_001) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        authors.sort(Comparator.comparing(Author::olKey));
        clearProcessedFiles();
        int index = 0;
        int lastIndex;
        while (index < authors.size()) {
            lastIndex = index;
            index = Math.min(lastIndex + MAX_ENTRY_PER_FILE, authors.size());
            String csvFile = OPEN_LIBRARY_PROCESSED_PATH + "author-" + index + ".csv";
            try (BufferedWriter writer = Files.newBufferedWriter(Path.of(csvFile), ENCODING)){
                writer.write(header);
                writer.newLine();
                for(int i = lastIndex; i < index; i++) {
                    Author author = authors.get(i);
                    String value = toData(author.name()) + toData(author.birthDate()) + toData(author.deathDate())
                            + toData(author.date()) + toData(author.bio()) + toData(author.key(), true);
                    writer.write(value);
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        processAuthor();
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
                         List<String> sourceRecords) {
        public String bio() {
            if (bioNode != null) {
                if (bioNode.isTextual()) {
                    return bioNode.asText();
                } else if (bioNode.isObject()) {
                    return JacksonUtil.readValue(bioNode, SerializedValue.class).value();
                }
            }
            return null;
        }
        
        public int olKey() {
            return Integer.parseInt(key.substring(key.indexOf("OL") + 2, key.indexOf("A")));
        }
    }
    
    public record Link(String title, String url) {
    }
    
    public record SerializedValue(String type, String value) {
    }
}
