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
            String rootPattern = "window.";
            String pattern = rootPattern + "cdc_";
            String lastToken = "_";
            byte[] patternBytes = pattern.getBytes(StandardCharsets.UTF_8);

            Path chromeDriverPath = Paths.get(chromeDriverFileName);
            byte[] data = Files.readAllBytes(chromeDriverPath);

            String replacementString = "";
            byte[] replacement = null;
            for (int i = 0; i < data.length; i++) {
                boolean matches = true;
                for (int j = 0; j < pattern.length(); j++) {
                    if (patternBytes[j] != data[i + j]) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    if (replacement == null) {
                        int length = 0;
                        byte[] token = lastToken.getBytes(StandardCharsets.UTF_8);
                        int j = i + rootPattern.length();
                        boolean firstToken = true;
                        while (true) {
                            if (data[j] != token[0]) {
                                length++;
                                j++;
                            } else if (firstToken) {
                                length++;
                                j++;
                                firstToken = false;
                            } else {
                                break;
                            }
                        }
                        replacementString = RandomStringUtils.randomAlphanumeric(length);
                        replacement = replacementString.getBytes(StandardCharsets.UTF_8);
                    }

                    System.arraycopy(replacement, 0, data, i + rootPattern.length(), replacement.length);
                    i += replacement.length;
                }
            }

            if (!replacementString.isEmpty()) {
                Files.write(chromeDriverPath, data);
                log.info("Patched chrome driver '{}' with '{}'!", chromeDriverFileName, replacementString);
            } else {
                log.info("Chrome driver '{}' is already patched!", chromeDriverFileName);
            }
            return chromeDriverPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
