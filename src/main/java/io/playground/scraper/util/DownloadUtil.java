package io.playground.scraper.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.playground.scraper.model.DownloadChannel;
import io.playground.scraper.model.DownloadInfo;
import io.playground.scraper.model.DownloadPlatform;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class DownloadUtil {
    private static final String CHROME_DRIVER_FOLDER = "chromedriver";
    private static final String CHROME_DRIVER_DOWNLOAD_URL = "https://chromedriver.storage.googleapis.com";
    private static final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DownloadUtil() {}

    public static String fetchLatestVersion() {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(CHROME_DRIVER_DOWNLOAD_URL + "/LATEST_RELEASE"))
                                             .timeout(Duration.ofSeconds(10))
                                             .GET()
                                             .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String downloadLatestChromeDriver() {
        try {
            String chromedriverFolderName = CHROME_DRIVER_FOLDER + FileSystems.getDefault().getSeparator() +
                    "chromedriver_" + OSUtil.getOSNameForChromeDriver();
            String driverExtension = OSUtil.isWindows() ? ".exe" : "";
            String fileName = chromedriverFolderName + FileSystems.getDefault().getSeparator() + "chromedriver" + driverExtension;

            Path chromeDriverPath = Paths.get(fileName);
            if (!Files.exists(chromeDriverPath)) {
                String url = CHROME_DRIVER_DOWNLOAD_URL + "/" + fetchLatestVersion()
                        + "/chromedriver_" + OSUtil.getOSNameForChromeDriver() + ".zip";
                HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                                                     .timeout(Duration.ofSeconds(10))
                                                     .GET()
                                                     .build();

                String zipFileName = CHROME_DRIVER_FOLDER + FileSystems.getDefault().getSeparator()
                        + "chromedriver_" + OSUtil.getOSNameForChromeDriver() + ".zip";
                Path zipFilePath = Paths.get(zipFileName);
                Path parentFolderPath = Paths.get(chromedriverFolderName);
                Files.createDirectories(parentFolderPath);

                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofFile(zipFilePath,
                                                                              StandardOpenOption.WRITE,
                                                                              StandardOpenOption.CREATE));

                unzipFile(zipFileName, parentFolderPath);
                Files.deleteIfExists(zipFilePath);
                log.info("Downloaded chrome driver at: '{}'", fileName);
            } else {
                log.info("Chrome driver '{}' already exists!", fileName);
            }
            return chromeDriverPath.toAbsolutePath().toString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void unzipFile(String zipFileName, Path parentFolder) {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFileName))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File destinationDir = parentFolder.toFile();
                File destionationFile = new File(destinationDir, zipEntry.getName());

                String destinationDirPath = destinationDir.getCanonicalPath();
                String destinationFilePath = destionationFile.getCanonicalPath();

                if (!destinationFilePath.startsWith(destinationDirPath + FileSystems.getDefault().getSeparator())) {
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    if (!destionationFile.isDirectory() && !destionationFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + destionationFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = destionationFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(destionationFile);
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
