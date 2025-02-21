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
import java.util.Map;

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

        Map<Integer, Integer> authorIdMap = getMapFromJsonFile(OPEN_LIBRARY_AUTHOR_ID_MAP_PATH);
        Map<String, Integer> filteredWorksMap = getMapFromJsonFile(OPEN_LIBRARY_FILTERED_WORK_PATH);

        String directoryPath = OPEN_LIBRARY_PROCESSED_PATH + "work-" + fileTimestamp + Constant.SEPARATOR;
        File directory = new File(directoryPath);
        Files.createDirectories(directory.toPath());
        String workCsvFile = directoryPath + "work" + ".csv";
        String subjectCsvFile = directoryPath + "subject" + ".csv";
        String workSubjectCsvFile = directoryPath + "work-subject" + ".csv";
        String workAuthorsCsvFile = directoryPath + "work-author" + ".csv";

        String filteredWorkCsvFile = directoryPath + "filtered-work" + ".csv";
        String filteredSubjectCsvFile = directoryPath + "filtered-subject" + ".csv";
        String filteredWorkSubjectCsvFile = directoryPath + "filtered-work-subject" + ".csv";
        String filteredWorkAuthorCsvFile = directoryPath + "filtered-work-author" + ".csv";

        int currentWorkId = 0;
        int startWorkId = currentWorkId;
        BufferedWriter workWriter = Files.newBufferedWriter(Path.of(workCsvFile), ENCODING);
        try (BufferedReader reader = Files.newBufferedReader(latestWorkPath, ENCODING);
             BufferedWriter workIdMapWriter = Files.newBufferedWriter(Path.of(OPEN_LIBRARY_WORK_ID_MAP_PATH), ENCODING);
             BufferedWriter subjectWriter = Files.newBufferedWriter(Path.of(subjectCsvFile), ENCODING);
             BufferedWriter workSubjectWriter = Files.newBufferedWriter(Path.of(workSubjectCsvFile), ENCODING);
             BufferedWriter workAuthorWriter = Files.newBufferedWriter(Path.of(workAuthorsCsvFile), ENCODING);
             BufferedWriter filteredWorkWriter = Files.newBufferedWriter(Path.of(filteredWorkCsvFile), ENCODING);
             BufferedWriter filteredSubjectWriter = Files.newBufferedWriter(Path.of(filteredSubjectCsvFile), ENCODING);
             BufferedWriter filteredWorkSubjectWriter = Files.newBufferedWriter(Path.of(filteredWorkSubjectCsvFile), ENCODING);
             BufferedWriter filteredWorkAuthorWriter = Files.newBufferedWriter(Path.of(filteredWorkAuthorCsvFile), ENCODING);
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

            filteredWorkWriter.write(title);
            filteredWorkWriter.newLine();
            filteredSubjectWriter.write("name");
            filteredSubjectWriter.newLine();
            filteredWorkSubjectWriter.write("subject_id,work_id");
            filteredWorkSubjectWriter.newLine();
            filteredWorkAuthorWriter.write("author_id,work_id");
            filteredWorkAuthorWriter.newLine();
            workIdMapWriter.write("{");

            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(line.indexOf("/works/OL") + 9, line.indexOf("W"));
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
                if (filteredWorksMap.containsKey(key)) {
                    filteredWorkWriter.write(value);
                    filteredWorkWriter.newLine();
                }
                currentWorkId++;
                workIdMapWriter.write("\"" + work.olKey() + "\": " + currentWorkId + ", ");


                if (work.subjects() != null && !work.subjects().isEmpty()) {
                    for (String subject : work.subjects()) {
                        workSubjectWriter.write(toData(subject) + currentWorkId);
                        workSubjectWriter.newLine();
                        if (filteredWorksMap.containsKey(key)) {
                            filteredWorkSubjectWriter.write(toData(subject) + filteredWorksMap.get(key));
                            filteredWorkSubjectWriter.newLine();
                        }
                    }
                }

                if (work.authors() != null && !work.authors().isEmpty()) {
                    for (String authorOlKey : work.authors()) {
                        int authorKey = Integer.parseInt(authorOlKey.substring(authorOlKey.indexOf("OL") + 2, authorOlKey.indexOf("A")));
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
}
