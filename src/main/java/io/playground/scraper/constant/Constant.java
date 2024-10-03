package io.playground.scraper.constant;

import java.nio.file.FileSystems;

public class Constant {
    
    public static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    public static final String EXTERNAL_RESOURCES_PATH = "resources" + SEPARATOR;
    public static final String SCREENSHOT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "screenshots" + SEPARATOR;
    public static final String REPORT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "reports" + SEPARATOR;
    public static final String TEMP_PROFILE_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "temp" + SEPARATOR;
    public static final String FAKE_USER_AGENT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "useragent" + SEPARATOR;
    
    public static final String EXTENSION_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "extension" + SEPARATOR;
    public static final String PREVENT_WEB_RTC_LEAK_EXTENSION_PATH = EXTENSION_FOLDER_PATH + "WebRTC-Leak-Prevent-1.0.14";
    
    public static final String PROXY_SERVER_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "proxy" + SEPARATOR;
    public static final String ORACLE_PROXY_SERVER_FILE_PATH = PROXY_SERVER_FOLDER_PATH + "oracle.txt";
    public static final String PROVIDER_PROXY_SERVER_FILE_PATH = PROXY_SERVER_FOLDER_PATH + "3rd-provider.txt";

    public static final String ELEMENT_NOT_FOUND = "ELEMENT_NOT_FOUND";

    public static final String DATA_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "data" + SEPARATOR;
    
    public static final String ALTERNATIVE_TO_LINK = "https://alternativeto.net";
    public static final String ALTERNATIVE_TO_FOLDER_PATH = DATA_FOLDER_PATH + "alternative-to" + SEPARATOR;
    public static final String ALTERNATIVE_TO_CATEGORIES_PATH = ALTERNATIVE_TO_FOLDER_PATH + "alternative-to-categories.json5";
    
}
