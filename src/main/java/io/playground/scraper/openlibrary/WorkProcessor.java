package io.playground.scraper.openlibrary;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.openlibrary.model.Author;
import io.playground.scraper.openlibrary.model.Link;
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
        Map<String, List<Integer>> filteredAuthorWorksMap = new HashMap<>();
        Set<String> englishWordsMap = getEnglishWords();

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
             BufferedWriter filteredWorkIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_FILTERED_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter subjectWriter = Files.newBufferedWriter(Path.of(subjectCsvFile), ENCODING);
             BufferedWriter workSubjectWriter = Files.newBufferedWriter(Path.of(workSubjectCsvFile), ENCODING);
             BufferedWriter workAuthorWriter = Files.newBufferedWriter(Path.of(workAuthorsCsvFile), ENCODING);
             BufferedWriter ratingWriter = Files.newBufferedWriter(Path.of(ratingCsvFile), ENCODING);
             BufferedWriter filteredWorkWriter = Files.newBufferedWriter(Path.of(filteredWorkCsvFile), ENCODING);
             BufferedWriter filteredSubjectWriter = Files.newBufferedWriter(Path.of(filteredSubjectCsvFile), ENCODING);
             BufferedWriter filteredWorkSubjectWriter = Files.newBufferedWriter(Path.of(filteredWorkSubjectCsvFile), ENCODING);
             BufferedWriter filteredRatingWriter = Files.newBufferedWriter(Path.of(filteredRatingCsvFile), ENCODING);
        ) {
            String workTitle = "title,cover,ol_key" + auditTitle();
            String subjectTitle = "name" + auditTitle();
            String workSubjectTitle = "subject_id,work_id" + auditTitle();
            String workAuthorTitle = "author_id,work_id" + auditTitle();
            String ratingTitle = "score,work_id" + auditTitle();
            workWriter.write(workTitle);
            workWriter.newLine();
            subjectWriter.write(subjectTitle);
            subjectWriter.newLine();
            workSubjectWriter.write(workSubjectTitle);
            workSubjectWriter.newLine();
            workAuthorWriter.write(workAuthorTitle);
            workAuthorWriter.newLine();
            ratingWriter.write(ratingTitle);
            ratingWriter.newLine();

            filteredWorkWriter.write(workTitle);
            filteredWorkWriter.newLine();
            filteredSubjectWriter.write(subjectTitle);
            filteredSubjectWriter.newLine();
            filteredWorkSubjectWriter.write(workSubjectTitle);
            filteredWorkSubjectWriter.newLine();
            filteredRatingWriter.write(ratingTitle);
            filteredRatingWriter.newLine();
            workIdMapWriter.write("{");
            filteredWorkIdMapWriter.write("{");
            String line;
            while ((line = reader.readLine()) != null) {
                if (currentWorkId - startWorkId >= MAX_ENTRY_PER_FILE) {
                    workWriter.close();
                    workCsvFile = directoryPath + "work-" + currentWorkId + ".csv";
                    workWriter = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
                    workWriter.write(workTitle);
                    workWriter.newLine();
                    startWorkId = currentWorkId;
                }
                boolean isInEnglish = false;
                boolean isFiltered = false;
                line = line.substring(line.indexOf("{"));
                Work work = JacksonUtil.readValue(line, Work.class);
                String value = toDataWithAudit(work.title(), work.cover(), "OL" + work.olKey() + "W");
                workWriter.write(value);
                workWriter.newLine();
                currentWorkId++;
                workIdMapWriter.write("\"" + work.olKey() + "\": " + currentWorkId + ", ");
                
                if (work.title() != null) {
                    String[] tokens = work.title().split(" ");
                    int englishWordCount = 0;
                    for (String token : tokens) {
                        if (englishWordsMap.contains(token.trim().toLowerCase())) {
                            englishWordCount++;
                        }
                        if (englishWordCount >= 3) {
                            isInEnglish = true;
                            break;
                        }
                    }
                }
                
                if (workRatingMap.containsKey(work.olKeyString())) {
                    List<Integer> scores = workRatingMap.get(work.olKeyString());
                    for (int score : scores) {
                        ratingWriter.write(toDataWithAudit(score, currentWorkId));
                        ratingWriter.newLine();
                    }
                    int total = scores.stream().reduce(0, Integer::sum);
                    double averageScore = (double) total / scores.size();
                    if (currentFilteredWorkId < MAX_NUMBER_OF_FILTERED_WORKS && isInEnglish && scores.size() > 50 && averageScore >= 4) {
                        isFiltered = true;
                        currentFilteredWorkId++;
                        filteredWorkWriter.write(value);
                        filteredWorkWriter.newLine();
                        for (int score : scores) {
                            filteredRatingWriter.write(toDataWithAudit(score, currentWorkId));
                            filteredRatingWriter.newLine();
                        }
                        filteredWorkIdMapWriter.write("\"" + work.olKey() + "\": " + currentFilteredWorkId + ", ");
                    }
                }

                List<String> subjectList = new ArrayList<>();
                if (work.subjects() != null) {
                    subjectList.addAll(work.subjects());
                }
                if (work.subjectPeople() != null) {
                    subjectList.addAll(work.subjectPeople());
                }
                if (work.subjectPlaces() != null) {
                    subjectList.addAll(work.subjectPlaces());
                }
                if (work.subjectTimes() != null) {
                    subjectList.addAll(work.subjectTimes());
                }
                for (String subject : subjectList) {
                    if (subjectMap.containsKey(subject)) {
                        workSubjectWriter.write(toDataWithAudit(subjectMap.get(subject), currentWorkId));
                        workSubjectWriter.newLine();
                    } else {
                        int subjectId = subjectMap.size() + 1;
                        subjectMap.put(subject, subjectId);
                        subjectWriter.write(toDataWithAudit(subject));
                        subjectWriter.newLine();
                        workSubjectWriter.write(toDataWithAudit(subjectId, currentWorkId));
                        workSubjectWriter.newLine();
                    }

                    if (isFiltered) {
                        if (filteredSubjectMap.containsKey(subject)) {
                            filteredWorkSubjectWriter.write(toDataWithAudit(filteredSubjectMap.get(subject), currentFilteredWorkId));
                            filteredWorkSubjectWriter.newLine();
                        } else {
                            int subjectId = filteredSubjectMap.size() + 1;
                            filteredSubjectMap.put(subject, subjectId);
                            filteredSubjectWriter.write(subject);
                            filteredSubjectWriter.newLine();
                            filteredWorkSubjectWriter.write(toDataWithAudit(subjectId, currentFilteredWorkId));
                            filteredWorkSubjectWriter.newLine();
                        }
                    }
                }
                
                if (work.authors() != null) {
                    for (String authorOlKey : work.authors()) {
                        String authorKey = authorOlKey.substring(authorOlKey.indexOf("OL") + 2, authorOlKey.indexOf("A"));
                        if (authorRedirectMap.containsKey(authorKey)) {
                            authorKey = String.valueOf(authorRedirectMap.get(authorKey));
                        }
                        workAuthorWriter.write(toDataWithAudit(authorIdMap.get(authorKey), currentWorkId));
                        workAuthorWriter.newLine();
                        
                        if (isFiltered) {
                            if (filteredAuthorWorksMap.containsKey(authorKey)) {
                                filteredAuthorWorksMap.get(authorKey).add(currentWorkId);
                            } else {
                                List<Integer> workIds = new ArrayList<>();
                                workIds.add(currentWorkId);
                                filteredAuthorWorksMap.put(authorKey, workIds);
                            }
                        }
                    }
                }
            }
            workIdMapWriter.write("}");
            filteredWorkIdMapWriter.write("}");
            workWriter.close();
        }

        Path latestAuthorPath = getLatestFile(OPEN_LIBRARY_AUTHOR_PATH_PATTERN);
        if (latestAuthorPath == null) {
            log.error("There's no unprocessed author file!");
            return;
        }
        
        String filteredAuthorCsvFile = directoryPath + "filtered-author" + ".csv";
        String filteredAuthorAlternateNameCsvFile = directoryPath + "filtered-author-alternate-name" + ".csv";
        String filteredAuthorLinkCsvFile = directoryPath + "filtered-author-link" + ".csv";

        int currentAuthorId = 0;
        try (BufferedReader authorReader = Files.newBufferedReader(latestAuthorPath, ENCODING);
             BufferedWriter filteredAuthorWriter = Files.newBufferedWriter(Path.of(filteredAuthorCsvFile), ENCODING);
             BufferedWriter filteredAuthorAltNameWriter = Files.newBufferedWriter(Path.of(filteredAuthorAlternateNameCsvFile), ENCODING);
             BufferedWriter filteredAuthorLinkWriter = Files.newBufferedWriter(Path.of(filteredAuthorLinkCsvFile), ENCODING);
             BufferedWriter filteredWorkAuthorWriter = Files.newBufferedWriter(Path.of(filteredWorkAuthorCsvFile), ENCODING);
        ) {
            String line;
            filteredAuthorWriter.write("name,birth_date,death_date,date,bio,photo,ol_key" + auditTitle());
            filteredAuthorWriter.newLine();
            filteredAuthorAltNameWriter.write("name,author_id" + auditTitle());
            filteredAuthorAltNameWriter.newLine();
            filteredAuthorLinkWriter.write("title,url,author_id" + auditTitle());
            filteredAuthorLinkWriter.newLine();
            filteredWorkAuthorWriter.write("author_id,work_id" + auditTitle());
            filteredWorkAuthorWriter.newLine();
            while ((line = authorReader.readLine()) != null) {
                String authorKey = line.substring(line.indexOf("/authors/OL") + 11, line.indexOf("A"));
                if (filteredAuthorWorksMap.containsKey(authorKey)) {
                    line = line.substring(line.indexOf("{"));
                    Author author = JacksonUtil.readValue(line, Author.class);
                    String value = toDataWithAudit(author.name(), author.birthDate(), author.deathDate(), author.date(),
                                                   author.bio(), author.photo(), "OL" + author.olKey() + "A");

                    currentAuthorId++;
                    filteredAuthorWriter.write(value);
                    filteredAuthorWriter.newLine();

                    if (author.alternateNames() != null) {
                        for (String alternateName : author.alternateNames()) {
                            filteredAuthorAltNameWriter.write(toDataWithAudit(alternateName, currentAuthorId));
                            filteredAuthorAltNameWriter.newLine();
                        }
                    }

                    if (author.links() != null) {
                        for (Link link : author.links()) {
                            filteredAuthorLinkWriter.write(toDataWithAudit(link.title(), link.url(), currentAuthorId));
                            filteredAuthorLinkWriter.newLine();
                        }
                    }
                    
                    for (int workId : filteredAuthorWorksMap.get(authorKey)) {
                        filteredWorkAuthorWriter.write(toDataWithAudit(currentAuthorId, workId));
                        filteredWorkAuthorWriter.newLine();
                    }
                }
                
            }
        }
        

        long stopTime = System.currentTimeMillis();
        log.info("Processing works elapsed time: {}", msToProperTime(stopTime - startTime));
    }

    private Map<String, List<Integer>> getRatingMap() throws IOException {
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
}
