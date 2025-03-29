package io.playground.scraper.openlibrary;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.openlibrary.model.Edition;
import io.playground.scraper.openlibrary.model.FixedWorkInfo;
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
public class EditionProcessor extends BaseProcessor {
    
    public void processEdition() throws IOException {
        long startTime = System.currentTimeMillis();
        Path latestEditionPath = getLatestFile(OPEN_LIBRARY_EDITION_PATH_PATTERN);
        if (latestEditionPath == null) {
            log.error("There's no unprocessed edition dump file!");
            return;
        }
        clearOldProcessedFiles("edition-*");

        Map<String, Integer> workIdMap = getMapFromJsonFile(OPEN_LIBRARY_WORK_ID_MAP_PATH);
        Map<String, Boolean> impairedWorkIdMap = getMapFromJsonFile(OPEN_LIBRARY_IMPAIRED_WORK_ID_MAP_PATH);
        Map<String, Integer> workRedirectMap = getMapFromJsonFile(OPEN_LIBRARY_AUTHOR_REDIRECT_MAP_PATH);
        Map<String, Integer> filteredWorkIdMap = getMapFromJsonFile(OPEN_LIBRARY_FILTERED_WORK_ID_MAP_PATH);
        RatingProcessor ratingProcessor = new RatingProcessor();
        Map<String, List<Integer>> workRatingMap = ratingProcessor.getWorkRatingMap();
        Map<String, List<Integer>> editionRatingMap = ratingProcessor.getEditionRatingMap();

        Map<String, Integer> publisherMap = new HashMap<>();
        Map<String, Integer> filteredPublisherMap = new HashMap<>();

        Set<String> englishWordsMap = getEnglishWords();

        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "edition-" + fileTimestamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        
        String editionCsvFile = directoryPath + "edition" + ".csv";
        String publisherCsvFile = directoryPath + "publisher" + ".csv";
        String reviewCsvFile = directoryPath + "review" + ".csv";

        String filteredEditionFile = directoryPath + "filtered-edition" + ".csv";
        String filteredPublisherFile = directoryPath + "filtered-publisher" + ".csv";
        String filteredReviewCsvFile = directoryPath + "filtered-review" + ".csv";

        int currentEditionId = 0;
        int currentFilteredEditionId = 0;
        int startEditionId = currentEditionId;
        BufferedWriter editionWriter = Files.newBufferedWriter(Path.of(editionCsvFile), ENCODING);
        try (BufferedReader reader = Files.newBufferedReader(latestEditionPath, ENCODING);
             BufferedWriter filteredEditionWriter = Files.newBufferedWriter(Path.of(filteredEditionFile), ENCODING);
             BufferedWriter publisherWriter = Files.newBufferedWriter(Path.of(publisherCsvFile), ENCODING);
             BufferedWriter filteredPublisherWriter = Files.newBufferedWriter(Path.of(filteredPublisherFile), ENCODING);
             BufferedWriter malformedEditionWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_LEFTOVER_TITLE_PATH), ENCODING);
             BufferedWriter fixedWorkIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_FIXED_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter reviewWriter = Files.newBufferedWriter(Path.of(reviewCsvFile), ENCODING);
             BufferedWriter filteredReviewWriter = Files.newBufferedWriter(Path.of(filteredReviewCsvFile), ENCODING);
        ) {
            String editionHeader = "title,subtitle,description,pagination,number_of_pages,volumns,physical_format," +
                    "physical_dimensions,weight,isbn_10,isbn_13,oclc_number,lccn_number,dewey_number,lc_classifications," +
                    "language,publish_date,publish_country,publish_place," +
//                    "by_statement,contributions,identifier," +
                    "cover,ol_key,grade,average_rating,rating_count,publisher_id,work_id" + auditTitle();
            String publisherHeader = "publisher_name" + auditTitle();
            String reviewHeader = "score,review,profile_id,work_id" + auditTitle();
            
            editionWriter.write(editionHeader);
            editionWriter.newLine();
            publisherWriter.write(publisherHeader);
            publisherWriter.newLine();
            reviewWriter.write(reviewHeader);
            reviewWriter.newLine();

            filteredEditionWriter.write(editionHeader);
            filteredEditionWriter.newLine();
            filteredPublisherWriter.write(publisherHeader);
            filteredPublisherWriter.newLine();
            filteredReviewWriter.write(reviewHeader);
            filteredReviewWriter.newLine();
            fixedWorkIdMapWriter.write("{");
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (currentEditionId - startEditionId >= MAX_ENTRY_PER_FILE) {
                    editionWriter.close();
                    editionCsvFile = directoryPath + "edition-" + currentEditionId + ".csv";
                    editionWriter = Files.newBufferedWriter(Path.of(editionCsvFile), ENCODING);
                    editionWriter.write(editionHeader);
                    editionWriter.newLine();
                    startEditionId = currentEditionId;
                }
                
                try {
                    Integer.parseInt(line.substring(line.indexOf("/books/OL") + 9, line.indexOf("M")));
                } catch (Exception ignored) {
                }
                
                line = line.substring(line.indexOf("{"));
                Edition edition = JacksonUtil.readValue(line, Edition.class);
                String editionWorkKey = null;
                try {
                    editionWorkKey = String.valueOf(edition.workKey());
                } catch (Exception ignored) {
                }
                if (editionWorkKey == null) {
                    malformedEditionWriter.write(line);
                    malformedEditionWriter.newLine();
                    continue;
                } else if (workIdMap.containsKey(editionWorkKey)) {
                    editionWorkKey = String.valueOf(workIdMap.get(editionWorkKey));
                } else if (workRedirectMap.containsKey(editionWorkKey)) {
                    editionWorkKey = String.valueOf(workRedirectMap.get(editionWorkKey));
                } else {
                    malformedEditionWriter.write(line);
                    malformedEditionWriter.newLine();
                    continue;
                }

                boolean isInEnglish = false;
                if (edition.title() != null) {
                    String[] tokens = edition.title().split(" ");
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
                }
                
                int size = 1;
                if (edition.isbn10() != null) {
                    size = Math.max(edition.isbn10().size(), size);
                }
                if (edition.isbn13() != null) {
                    size = Math.max(edition.isbn13().size(), size);
                }

                boolean isWorkTitleFixed = !(impairedWorkIdMap.containsKey(editionWorkKey) && !impairedWorkIdMap.get(editionWorkKey));
                for (int i = 0; i < size; i++) {
                    String isbn10 = edition.isbn10() != null && edition.isbn10().size() > i ? edition.isbn10().get(i) : null;
                    String isbn13 = edition.isbn13() != null && edition.isbn13().size() > i ? edition.isbn13().get(i) : null;
                    String cover = edition.covers() != null && !edition.covers().isEmpty()
                            && edition.covers().getFirst() != null && !edition.covers().getFirst().equals("-1")
                            ? edition.covers().getFirst() : null;
                    int grade = 0;
                    if (!edition.description().isBlank()) {
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
                    grade += isInEnglish ? 100 : 0;

                    if (!isWorkTitleFixed && isInEnglish && !edition.description().isBlank()) {
                        String fixedValue = JacksonUtil.writeValueAsString(new FixedWorkInfo(edition.title(), edition.description()));
                        fixedWorkIdMapWriter.write("\"" + editionWorkKey + "\": " + fixedValue + ", ");
                        impairedWorkIdMap.put(editionWorkKey, true);
                    }

                    String publisherId = null;
                    if (edition.publisher() != null) {
                        if (publisherMap.containsKey(edition.publisher())) {
                            publisherId = String.valueOf(publisherMap.get(edition.publisher()));
                        } else {
                            int index = publisherMap.size() + 1;
                            publisherMap.put(edition.publisher(), index);
                            publisherId = String.valueOf(index);
                            publisherWriter.write(toDataWithAudit(edition.publisher()));
                            publisherWriter.newLine();
                        }
                    }

                    currentEditionId++;
                    int ratingCount = 0;
                    int totalRating = 0;
                    if (workRatingMap.containsKey(editionWorkKey)) {
                        List<Integer> scores = workRatingMap.get(editionWorkKey);
                        for (int score : scores) {
                            reviewWriter.write(toDataWithAudit(score, null, 1, currentEditionId));
                            reviewWriter.newLine();
                            ratingCount++;
                            totalRating += score;
                        }
                    }

                    if (editionRatingMap.containsKey(edition.key())) {
                        List<Integer> scores = editionRatingMap.get(edition.key());
                        for (int score : scores) {
                            reviewWriter.write(toDataWithAudit(score, null, 1, currentEditionId));
                            reviewWriter.newLine();
                            ratingCount++;
                            totalRating += score;
                        }
                    }

                    double averageRating = ratingCount > 0 ? ((double) totalRating / ratingCount) : 0;

                    String value = toDataWithAudit(edition.title(), edition.subtitle(), edition.description(),
                                                   edition.pagination(), edition.numberOfPages(), edition.numberOfVolumes(),
                                                   edition.physicalFormat(), edition.physicalDimensions(), edition.weight(),
                                                   isbn10, isbn13, edition.oclcNumber(i), edition.lccnNumber(i),
                                                   edition.deweyNumber(i), edition.lcClassification(i), edition.language(),
                                                   edition.publishDate(), edition.publishCountry(), edition.publishPlace(),
//                                                       edition.byStatement(), edition.contributions(), edition.identifiers(),
                                                   cover, edition.key(), grade, averageRating, ratingCount, publisherId, editionWorkKey);
                    editionWriter.write(value);
                    editionWriter.newLine();

                    if (filteredWorkIdMap.containsKey(editionWorkKey)) {
                        if (isInEnglish && grade >= 115) {
                            currentFilteredEditionId++;

                            int filteredRatingCount = 0;
                            int filteredTotalRating = 0;
                            if (workRatingMap.containsKey(editionWorkKey)) {
                                List<Integer> scores = workRatingMap.get(editionWorkKey);
                                for (int score : scores) {
                                    filteredReviewWriter.write(toDataWithAudit(score, null, 1, currentFilteredEditionId));
                                    filteredReviewWriter.newLine();
                                    filteredRatingCount++;
                                    filteredTotalRating += score;
                                }
                            }

                            if (editionRatingMap.containsKey(edition.key())) {
                                List<Integer> scores = editionRatingMap.get(edition.key());
                                for (int score : scores) {
                                    filteredReviewWriter.write(toDataWithAudit(score, null, 1, currentFilteredEditionId));
                                    filteredReviewWriter.newLine();
                                    filteredRatingCount++;
                                    filteredTotalRating += score;
                                }
                            }
                            double filteredAverageRating = filteredRatingCount > 0 ? ((double) filteredTotalRating / filteredRatingCount) : 0;

                            String filteredPublisherId = null;
                            if (edition.publisher() != null) {
                                if (filteredPublisherMap.containsKey(edition.publisher())) {
                                    filteredPublisherId = String.valueOf(filteredPublisherMap.get(edition.publisher()));
                                } else {
                                    int index = filteredPublisherMap.size() + 1;
                                    filteredPublisherMap.put(edition.publisher(), index);
                                    filteredPublisherId = String.valueOf(index);
                                    filteredPublisherWriter.write(toDataWithAudit(edition.publisher()));
                                    filteredPublisherWriter.newLine();
                                }
                            }

                            value = toDataWithAudit(edition.title(), edition.subtitle(), edition.description(),
                                                    edition.pagination(), edition.numberOfPages(), edition.numberOfVolumes(),
                                                    edition.physicalFormat(), edition.physicalDimensions(), edition.weight(),
                                                    isbn10, isbn13, edition.oclcNumber(i), edition.lccnNumber(i),
                                                    edition.deweyNumber(i), edition.lcClassification(i), edition.language(),
                                                    edition.publishDate(), edition.publishCountry(), edition.publishPlace(),
//                                                       edition.byStatement(), edition.contributions(), edition.identifiers(),
                                                    cover, edition.key(), grade, filteredAverageRating, filteredRatingCount,
                                                    filteredPublisherId, filteredWorkIdMap.get(editionWorkKey));
                            filteredEditionWriter.write(value);
                            filteredEditionWriter.newLine();
                        }
                    }
                }
            }
            fixedWorkIdMapWriter.write("}");
            editionWriter.close();
        }

        long stopTime = System.currentTimeMillis();
        log.info("Processing editions elapsed time: {}", msToProperTime(stopTime - startTime));
    }
}
