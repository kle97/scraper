package io.playground.scraper.openlibrary;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RatingProcessor extends BaseProcessor {

    public Map<String, List<Integer>> getRatingMap() throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, List<Integer>> result = new HashMap<>();
        Path latestRatingPath = getLatestFile(OPEN_LIBRARY_RATING_PATH_PATTERN);
        if (latestRatingPath == null) {
            log.error("There's no unprocessed rating dump file!");
            return result;
        }

        Map<String, Integer> workRedirectMap = getMapFromJsonFile(OPEN_LIBRARY_WORK_REDIRECT_MAP_PATH);

        try (BufferedReader reader = Files.newBufferedReader(latestRatingPath, ENCODING);
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                String workKey = tokens[0].substring(tokens[0].indexOf("OL") + 2, tokens[0].indexOf("W"));
                if (workRedirectMap.containsKey(workKey)) {
                    workKey = String.valueOf(workRedirectMap.get(workKey));
                }
                int score = Integer.parseInt(tokens[tokens.length - 2]);

                if (result.containsKey(workKey)) {
                    result.get(workKey).add(score);
                } else {
                    List<Integer> scores = new ArrayList<>();
                    scores.add(score);
                    result.put(workKey, scores);
                }
            }
        }
        long stopTime = System.currentTimeMillis();
        log.info("Processing ratings elapsed time: {}", msToProperTime(stopTime - startTime));
        return result;
    }

    public Map<String, List<Integer>> getWorkRatingMap() throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, List<Integer>> result = new HashMap<>();
        Path latestRatingPath = getLatestFile(OPEN_LIBRARY_RATING_PATH_PATTERN);
        if (latestRatingPath == null) {
            log.error("There's no unprocessed rating dump file!");
            return result;
        }

        Map<String, Integer> workRedirectMap = getMapFromJsonFile(OPEN_LIBRARY_WORK_REDIRECT_MAP_PATH);

        try (BufferedReader reader = Files.newBufferedReader(latestRatingPath, ENCODING);
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                if (!(tokens[1].startsWith("OL") && tokens[1].endsWith("M"))) {
                    String workKey = tokens[0].substring(tokens[0].indexOf("OL") + 2, tokens[0].indexOf("W"));
                    if (workRedirectMap.containsKey(workKey)) {
                        workKey = String.valueOf(workRedirectMap.get(workKey));
                    }
                    int score = Integer.parseInt(tokens[tokens.length - 2]);

                    if (result.containsKey(workKey)) {
                        result.get(workKey).add(score);
                    } else {
                        List<Integer> scores = new ArrayList<>();
                        scores.add(score);
                        result.put(workKey, scores);
                    }
                }
            }
        }
        long stopTime = System.currentTimeMillis();
        log.info("Processing work ratings elapsed time: {}", msToProperTime(stopTime - startTime));
        return result;
    }

    public Map<String, List<Integer>> getEditionRatingMap() throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, List<Integer>> result = new HashMap<>();
        Path latestRatingPath = getLatestFile(OPEN_LIBRARY_RATING_PATH_PATTERN);
        if (latestRatingPath == null) {
            log.error("There's no unprocessed rating dump file!");
            return result;
        }

        Map<String, Integer> editionRedirectMap = getMapFromJsonFile(OPEN_LIBRARY_EDITION_REDIRECT_MAP_PATH);

        try (BufferedReader reader = Files.newBufferedReader(latestRatingPath, ENCODING)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                if (tokens.length > 1) {
                    if (tokens[1].startsWith("OL") && tokens[1].endsWith("M")) {
                        String editionKey = tokens[1].substring(tokens[1].indexOf("OL") + 2, tokens[1].indexOf("M"));
                        if (editionRedirectMap.containsKey(editionKey)) {
                            editionKey = String.valueOf(editionRedirectMap.get(editionKey));
                        }
                        int score = Integer.parseInt(tokens[tokens.length - 2]);

                        if (result.containsKey(editionKey)) {
                            result.get(editionKey).add(score);
                        } else {
                            List<Integer> scores = new ArrayList<>();
                            scores.add(score);
                            result.put(editionKey, scores);
                        }
                    }
                }
            }
        }
        long stopTime = System.currentTimeMillis();
        log.info("Processing edition ratings elapsed time: {}", msToProperTime(stopTime - startTime));
        return result;
    }
}
