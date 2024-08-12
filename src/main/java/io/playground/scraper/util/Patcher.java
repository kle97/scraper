package io.playground.scraper.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Patcher {

    private Patcher() {}

    public static String patchChromeDriver(String chromeDriverFileName) {
        try {
            byte[] patternBytes = "window.cdc_".getBytes(StandardCharsets.UTF_8);
            byte terminateToken1Byte = ";".getBytes(StandardCharsets.UTF_8)[0];
            byte terminateToken2Byte = "|".getBytes(StandardCharsets.UTF_8)[0];

            Path chromeDriverPath = Paths.get(chromeDriverFileName);
            byte[] data = Files.readAllBytes(chromeDriverPath);

            boolean anyMatch = false;
            for (int i = 0; i < data.length; i++) {
                boolean matches = true;
                for (int j = 0; j < patternBytes.length; j++) {
                    if (patternBytes[j] != data[i + j]) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    anyMatch = true;
                    int j = i;
                    int length = 0;
                    int terminateToken2Matches = 0;
                    while (data[j] != terminateToken1Byte) {
                        if (data[j] == terminateToken2Byte) {
                            terminateToken2Matches++;
                            if (terminateToken2Matches == 2) {
                                length++;
                                break;
                            }
                        }
                        length++;
                        j++;
                    }

                    length++;
                    byte[] replacementBytes = " ".repeat(length).getBytes(StandardCharsets.UTF_8);
                    System.arraycopy(replacementBytes, 0, data, i, length);
                    i += replacementBytes.length;
                }
            }

            if (anyMatch) {
                Files.write(chromeDriverPath, data);
                log.info("Patched chrome driver '{}'!", chromeDriverFileName);
            } else {
                log.info("Chrome driver '{}' is already patched!", chromeDriverFileName);
            }
            return chromeDriverPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
