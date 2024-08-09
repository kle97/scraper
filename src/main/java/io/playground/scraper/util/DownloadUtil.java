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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class DownloadUtil {
    private static final String CHROME_DRIVER_FOLDER = "chromedriver";
    private static final String CHROME_DRIVER_DOWNLOAD_URL = "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json";
    private static final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DownloadUtil() {}
    
    public static String downloadLatestChromeDriver() {
        try {
            String chromedriverFolderName = "chromedriver-" + OSUtil.getOSNameForChromeDriver();
            String driverExtension = OSUtil.isWindows() ? ".exe" : "";
            String fileName = CHROME_DRIVER_FOLDER + FileSystems.getDefault().getSeparator() + chromedriverFolderName 
                    + FileSystems.getDefault().getSeparator() + "chromedriver" + driverExtension;
            
            if (!Files.exists(Path.of(fileName))) {
                HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(CHROME_DRIVER_DOWNLOAD_URL))
                                                     .timeout(Duration.ofSeconds(10))
                                                     .GET()
                                                     .build();
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                DownloadInfo downloadInfo = objectMapper.readValue(response.body(), DownloadInfo.class);
                DownloadChannel downloadChannel = downloadInfo.channels().stableChannel();
                List<DownloadPlatform> downloadPlatforms = downloadChannel.downloads().chromedriver();

                String url = "";
                for (DownloadPlatform platform : downloadPlatforms) {
                    if (platform.platform().equals(OSUtil.getOSNameForChromeDriver())) {
                        url = platform.url();
                        break;
                    }
                }
                
                httpRequest = HttpRequest.newBuilder(URI.create(url))
                                         .timeout(Duration.ofSeconds(10))
                                         .GET()
                                         .build();
                String zipFileName = CHROME_DRIVER_FOLDER + FileSystems.getDefault().getSeparator()
                        + url.substring(url.lastIndexOf("/") + 1);
                String version = downloadChannel.version().replace(".", "-");
                zipFileName = zipFileName.replace(".zip", "-" + version + ".zip");
                File file = new File(zipFileName);
                Path chromeDriverPath = Path.of(CHROME_DRIVER_FOLDER);
                Files.createDirectories(chromeDriverPath);
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofFile(file.toPath(),
                                                                              StandardOpenOption.WRITE,
                                                                              StandardOpenOption.CREATE));

                unzipFile(zipFileName);
                Files.deleteIfExists(Path.of(zipFileName));
                log.info("Downloaded chrome driver at: '{}'", fileName);
            } else {
                log.info("Chrome driver '{}' already exists!", fileName);
            }
            return fileName;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void unzipFile(String zipFileName) {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFileName))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File destinationDir = new File(CHROME_DRIVER_FOLDER);
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
