package io.playground.scraper.openlibrary;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.openlibrary.model.Work;
import io.playground.scraper.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class WorkProcessor extends BaseProcessor {

    public void processWork() throws IOException {
        long startTime = System.currentTimeMillis();
        Path latestWorkPath = getLatestFile(OPEN_LIBRARY_WORK_PATH_PATTERN);
        if (latestWorkPath == null) {
            log.error("There's no unprocessed work dump file!");
            return;
        }
        clearOldProcessedFiles("work-*");

        Map<String, Integer> authorIdMap = getMapFromJsonFile(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        Map<String, Integer> authorRedirectMap = getMapFromJsonFile(OPEN_LIBRARY_AUTHOR_REDIRECT_MAP_PATH);
        Map<String, List<Integer>> workRatingMap = getRatingMap();

        Map<String, Integer> subjectMap = new HashMap<>();
        Map<String, Integer> filteredSubjectMap = new HashMap<>();
        Map<String, Integer> filteredWorkMap = new HashMap<>();
        Map<String, Integer> filteredAuthorMap = new HashMap<>();

        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "work-" + fileTimestamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String workCsvFile = directoryPath + "work" + ".csv";
        String subjectCsvFile = directoryPath + "subject" + ".csv";
        String workSubjectCsvFile = directoryPath + "work-subject" + ".csv";
        String workAuthorsCsvFile = directoryPath + "work-author" + ".csv";
        String ratingCsvFile = directoryPath + "rating" + ".csv";

        String filteredWorkCsvFile = directoryPath + "filtered-work" + ".csv";
        String filteredSubjectCsvFile = directoryPath + "filtered-subject" + ".csv";
        String filteredWorkSubjectCsvFile = directoryPath + "filtered-work-subject" + ".csv";
        String filteredWorkAuthorCsvFile = directoryPath + "filtered-work-author" + ".csv";
        String filteredRatingCsvFile = directoryPath + "filtered-rating" + ".csv";

        int currentWorkId = 0;
        int currentFilteredWorkId = 0;
        int startWorkId = currentWorkId;
        BufferedWriter workWriter = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
        try (BufferedReader reader = Files.newBufferedReader(latestWorkPath, ENCODING);
             BufferedWriter workIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter subjectWriter = Files.newBufferedWriter(Path.of(subjectCsvFile), ENCODING);
             BufferedWriter workSubjectWriter = Files.newBufferedWriter(Path.of(workSubjectCsvFile), ENCODING);
             BufferedWriter workAuthorWriter = Files.newBufferedWriter(Path.of(workAuthorsCsvFile), ENCODING);
             BufferedWriter ratingWriter = Files.newBufferedWriter(Path.of(ratingCsvFile), ENCODING);
             BufferedWriter filteredWorkWriter = Files.newBufferedWriter(Path.of(filteredWorkCsvFile), ENCODING);
             BufferedWriter filteredSubjectWriter = Files.newBufferedWriter(Path.of(filteredSubjectCsvFile), ENCODING);
             BufferedWriter filteredWorkSubjectWriter = Files.newBufferedWriter(Path.of(filteredWorkSubjectCsvFile), ENCODING);
             BufferedWriter filteredWorkAuthorWriter = Files.newBufferedWriter(Path.of(filteredWorkAuthorCsvFile), ENCODING);
             BufferedWriter filteredRatingWriter = Files.newBufferedWriter(Path.of(filteredRatingCsvFile), ENCODING);
        ) {
            String title = "title,cover,ol_key,last_created_by,last_created_at,last_modified_by,last_modified_at";
            workWriter.write(title);
            workWriter.newLine();
            subjectWriter.write("name");
            subjectWriter.newLine();
            workSubjectWriter.write("subject_id,work_id");
            workSubjectWriter.newLine();
            workAuthorWriter.write("author_id,work_id");
            workAuthorWriter.newLine();
            ratingWriter.write("score,work_id");
            ratingWriter.newLine();

            filteredWorkWriter.write(title);
            filteredWorkWriter.newLine();
            filteredSubjectWriter.write("name");
            filteredSubjectWriter.newLine();
            filteredWorkSubjectWriter.write("subject_id,work_id");
            filteredWorkSubjectWriter.newLine();
            filteredWorkAuthorWriter.write("author_id,work_id");
            filteredWorkAuthorWriter.newLine();
            filteredRatingWriter.write("score,work_id");
            filteredRatingWriter.newLine();
            workIdMapWriter.write("{");

            String line;
            while ((line = reader.readLine()) != null) {
                boolean isFiltered = false;
                line = line.substring(line.indexOf("{"));
                if (currentWorkId - startWorkId >= MAX_ENTRY_PER_FILE) {
                    workWriter.close();
                    workCsvFile = directoryPath + "work-" + currentWorkId + ".csv";
                    workWriter = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
                    workWriter.write(title);
                    workWriter.newLine();
                    startWorkId = currentWorkId;
                }

                Work work = JacksonUtil.readValue(line, Work.class);
                String value = toData(work.title()) + toData(work.cover()) + toData("OL" + work.olKey() + "W")
                        + toData(1) + toData(TIMESTAMP) + toData(1) + toData(TIMESTAMP, true);
                workWriter.write(value);
                workWriter.newLine();

                if (workRatingMap.containsKey(work.olKeyString())) {
                    List<Integer> scores = workRatingMap.get(work.olKeyString());
                    for (int score : scores) {
                        ratingWriter.write(toData(score) + currentWorkId);
                        ratingWriter.newLine();
                    }
                    int total = scores.stream().reduce(0, Integer::sum);
                    double averageScore = (double) total / scores.size();
                    if (scores.size() > 5 && averageScore >= 4) {
                        isFiltered = true;
                        currentFilteredWorkId++;
                        filteredWorkWriter.write(value);
                        filteredWorkWriter.newLine();
                        filteredWorkMap.put(work.olKeyString(), currentFilteredWorkId);
                        for (int score : scores) {
                            filteredRatingWriter.write(toData(score) + currentWorkId);
                            filteredRatingWriter.newLine();
                        }
                    }
                }
                currentWorkId++;
                workIdMapWriter.write("\"" + work.olKey() + "\": " + currentWorkId + ", ");

                if (work.subjects() != null && !work.subjects().isEmpty()) {
                    for (String subject : work.subjects()) {
                        if (subjectMap.containsKey(subject)) {
                            workSubjectWriter.write(toData(subjectMap.get(subject)) + currentWorkId);
                            workSubjectWriter.newLine();
                        } else {
                            int subjectId = subjectMap.size() + 1;
                            subjectMap.put(subject, subjectId);
                            subjectWriter.write(subject);
                            subjectWriter.newLine();
                            workSubjectWriter.write(toData(subjectId) + currentWorkId);
                            workSubjectWriter.newLine();
                        }

                        if (isFiltered) {
                            if (filteredSubjectMap.containsKey(subject)) {
                                filteredWorkSubjectWriter.write(toData(filteredSubjectMap.get(subject)) + currentFilteredWorkId);
                                filteredWorkSubjectWriter.newLine();
                            } else {
                                int subjectId = filteredSubjectMap.size() + 1;
                                filteredSubjectMap.put(subject, subjectId);
                                filteredSubjectWriter.write(subject);
                                filteredSubjectWriter.newLine();
                                filteredWorkSubjectWriter.write(toData(subjectId) + currentFilteredWorkId);
                                filteredWorkSubjectWriter.newLine();
                            }
                        }
                    }
                }

                if (work.authors() != null) {
                    for (String authorOlKey : work.authors()) {
                        String authorKey = authorOlKey.substring(authorOlKey.indexOf("OL") + 2, authorOlKey.indexOf("A"));
                        if (authorRedirectMap.containsKey(authorKey)) {
                            authorKey = String.valueOf(authorRedirectMap.get(authorKey));
                        }
                        workAuthorWriter.write(toData(authorIdMap.get(authorKey)) + currentWorkId);
                        workAuthorWriter.newLine();
                    }
                }
            }
        }

        workWriter.close();

        long stopTime = System.currentTimeMillis();
        log.info("Processing works elapsed time: {}", (stopTime - startTime));
    }

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
//        try (BufferedWriter workRedirectMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_RATING_MAP_PATH), ENCODING)) {
//            workRedirectMapWriter.write("{");
//            for (var entry : result.entrySet()) {
//                workRedirectMapWriter.write("\"" + entry.getKey() + "\": " + entry.getValue() + ", ");
//            }
//            workRedirectMapWriter.write("}");
//        }
        long stopTime = System.currentTimeMillis();
        log.info("Processing ratings elapsed time: {}", (stopTime - startTime));
        return result;
    }
}
