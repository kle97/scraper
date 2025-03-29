package io.playground.scraper.openlibrary;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.openlibrary.model.Author;
import io.playground.scraper.openlibrary.model.FixedWorkInfo;
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

    public void processWork(boolean isFirstPass) throws IOException {
        long startTime = System.currentTimeMillis();
        Path latestWorkPath = getLatestFile(OPEN_LIBRARY_WORK_PATH_PATTERN);
        if (latestWorkPath == null) {
            log.error("There's no unprocessed work dump file!");
            return;
        }
        clearOldProcessedFiles("work-*");

        Map<String, Integer> authorIdMap = getMapFromJsonFile(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        Map<String, FixedWorkInfo> fixedWorkIdMap = !isFirstPass
                ? getMapFromJsonFile(OPEN_LIBRARY_FIXED_WORK_ID_MAP_PATH, String.class, FixedWorkInfo.class)
                : new HashMap<>();
        Map<String, Integer> authorRedirectMap = getMapFromJsonFile(OPEN_LIBRARY_AUTHOR_REDIRECT_MAP_PATH);
        Map<String, List<Integer>> ratingMap = new RatingProcessor().getRatingMap();

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
        String workAuthorsCsvFile = directoryPath + "author-work" + ".csv";
//        String ratingCsvFile = directoryPath + "rating" + ".csv";

        String filteredWorkCsvFile = directoryPath + "filtered-work" + ".csv";
        String filteredSubjectCsvFile = directoryPath + "filtered-subject" + ".csv";
        String filteredWorkSubjectCsvFile = directoryPath + "filtered-work-subject" + ".csv";
        String filteredWorkAuthorCsvFile = directoryPath + "filtered-author-work" + ".csv";
//        String filteredRatingCsvFile = directoryPath + "filtered-rating" + ".csv";

        int currentWorkId = 0;
        int currentFilteredWorkId = 0;
        int startWorkId = currentWorkId;
        BufferedWriter workWriter = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
        try (BufferedReader reader = Files.newBufferedReader(latestWorkPath, ENCODING);
             BufferedWriter workIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter impairedWorkIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_IMPAIRED_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter filteredWorkIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_FILTERED_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter subjectWriter = Files.newBufferedWriter(Path.of(subjectCsvFile), ENCODING);
             BufferedWriter workSubjectWriter = Files.newBufferedWriter(Path.of(workSubjectCsvFile), ENCODING);
             BufferedWriter workAuthorWriter = Files.newBufferedWriter(Path.of(workAuthorsCsvFile), ENCODING);
//             BufferedWriter ratingWriter = Files.newBufferedWriter(Path.of(ratingCsvFile), ENCODING);
             BufferedWriter filteredWorkWriter = Files.newBufferedWriter(Path.of(filteredWorkCsvFile), ENCODING);
             BufferedWriter filteredSubjectWriter = Files.newBufferedWriter(Path.of(filteredSubjectCsvFile), ENCODING);
             BufferedWriter filteredWorkSubjectWriter = Files.newBufferedWriter(Path.of(filteredWorkSubjectCsvFile), ENCODING);
//             BufferedWriter filteredRatingWriter = Files.newBufferedWriter(Path.of(filteredRatingCsvFile), ENCODING);
        ) {
            String workHeader = "title,description,ol_key" + auditTitle();
            String subjectHeader = "subject_name" + auditTitle();
            String workSubjectHeader = "work_id,subject_id" + auditTitle();
            String workAuthorHeader = "author_id,work_id" + auditTitle();
//            String ratingHeader = "score,work_id" + auditTitle();
            workWriter.write(workHeader);
            workWriter.newLine();
            subjectWriter.write(subjectHeader);
            subjectWriter.newLine();
            workSubjectWriter.write(workSubjectHeader);
            workSubjectWriter.newLine();
            workAuthorWriter.write(workAuthorHeader);
            workAuthorWriter.newLine();
//            ratingWriter.write(ratingHeader);
//            ratingWriter.newLine();

            filteredWorkWriter.write(workHeader);
            filteredWorkWriter.newLine();
            filteredSubjectWriter.write(subjectHeader);
            filteredSubjectWriter.newLine();
            filteredWorkSubjectWriter.write(workSubjectHeader);
            filteredWorkSubjectWriter.newLine();
//            filteredRatingWriter.write(ratingHeader);
//            filteredRatingWriter.newLine();
            workIdMapWriter.write("{");
            impairedWorkIdMapWriter.write("{");
            filteredWorkIdMapWriter.write("{");
            String line;
            while ((line = reader.readLine()) != null) {
                if (currentWorkId - startWorkId >= MAX_ENTRY_PER_FILE) {
                    workWriter.close();
                    workCsvFile = directoryPath + "work-" + currentWorkId + ".csv";
                    workWriter = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
                    workWriter.write(workHeader);
                    workWriter.newLine();
                    startWorkId = currentWorkId;
                }

                boolean isFiltered = false;
                line = line.substring(line.indexOf("{"));
                Work work = JacksonUtil.readValue(line, Work.class);
                String workTitle = work.title();
                String workDescription = work.description();
                if (fixedWorkIdMap.containsKey(work.olKeyString())) {
                    FixedWorkInfo fixedWorkInfo = fixedWorkIdMap.get(work.olKeyString());
                    workTitle = fixedWorkInfo.title();
                    workDescription = fixedWorkInfo.description();
                }

                String value = toDataWithAudit(workTitle, workDescription, work.key());
                workWriter.write(value);
                workWriter.newLine();
                currentWorkId++;
                workIdMapWriter.write("\"" + work.olKey() + "\": " + currentWorkId + ", ");

                boolean isInEnglish = false;
                if (work.title() != null) {
                    String[] tokens = work.title().split(" ");
                    int englishWordCount = 0;
                    for (String token : tokens) {
                        if (englishWordsMap.contains(token.trim().toLowerCase())) {
                            englishWordCount++;
                        }
                        if (englishWordCount >= 2) {
                            isInEnglish = true;
                            break;
                        }
                    }

                    if (!isInEnglish) {
                        impairedWorkIdMapWriter.write("\"" + work.olKey() + "\": " + false + ", ");
                    }
                }
                
                if (ratingMap.containsKey(work.olKeyString())) {
                    List<Integer> scores = ratingMap.get(work.olKeyString());
//                    for (int score : scores) {
//                        ratingWriter.write(toDataWithAudit(score, currentWorkId));
//                        ratingWriter.newLine();
//                    }
                    int total = scores.stream().reduce(0, Integer::sum);
                    double averageScore = (double) total / scores.size();
                    if (currentFilteredWorkId < MAX_NUMBER_OF_FILTERED_WORKS && isInEnglish && scores.size() > 40 && averageScore >= 4) {
                        isFiltered = true;
                        currentFilteredWorkId++;
                        filteredWorkWriter.write(value);
                        filteredWorkWriter.newLine();
//                        for (int score : scores) {
//                            filteredRatingWriter.write(toDataWithAudit(score, currentFilteredWorkId));
//                            filteredRatingWriter.newLine();
//                        }
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
                        workSubjectWriter.write(toDataWithAudit(currentWorkId, subjectId));
                        workSubjectWriter.newLine();
                    }

                    if (isFiltered) {
                        if (filteredSubjectMap.containsKey(subject)) {
                            filteredWorkSubjectWriter.write(toDataWithAudit(currentFilteredWorkId, filteredSubjectMap.get(subject)));
                            filteredWorkSubjectWriter.newLine();
                        } else {
                            int subjectId = filteredSubjectMap.size() + 1;
                            filteredSubjectMap.put(subject, subjectId);
                            filteredSubjectWriter.write(toDataWithAudit(subject));
                            filteredSubjectWriter.newLine();
                            filteredWorkSubjectWriter.write(toDataWithAudit(currentFilteredWorkId, subjectId));
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
                                filteredAuthorWorksMap.get(authorKey).add(currentFilteredWorkId);
                            } else {
                                List<Integer> filteredWorkIds = new ArrayList<>();
                                filteredWorkIds.add(currentFilteredWorkId);
                                filteredAuthorWorksMap.put(authorKey, filteredWorkIds);
                            }
                        }
                    }
                }
            }
            workIdMapWriter.write("}");
            impairedWorkIdMapWriter.write("}");
            filteredWorkIdMapWriter.write("}");
            workWriter.close();
        }

        if (!isFirstPass) {
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
                filteredAuthorWriter.write("author_name,birth_date,death_date,author_date,biography,photo,ol_key" + auditTitle());
                filteredAuthorWriter.newLine();
                filteredAuthorAltNameWriter.write("alternate_name,author_id" + auditTitle());
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
                                                       author.bio(), author.photo(), author.key());

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

                        for (int filteredWorkId : filteredAuthorWorksMap.get(authorKey)) {
                            filteredWorkAuthorWriter.write(toDataWithAudit(currentAuthorId, filteredWorkId));
                            filteredWorkAuthorWriter.newLine();
                        }
                    }

                }
            }
        }

        long stopTime = System.currentTimeMillis();
        log.info("Processing works elapsed time: {}", msToProperTime(stopTime - startTime));
    }


}
