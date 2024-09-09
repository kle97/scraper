package io.playground.scraper.constant;

import java.nio.file.FileSystems;

public class Constant {
    
    public static final String EXTERNAL_RESOURCES_PATH = "resources" + FileSystems.getDefault().getSeparator();
    public static final String SCREENSHOT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "screenshots" + FileSystems.getDefault().getSeparator();
    public static final String REPORT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "reports" + FileSystems.getDefault().getSeparator();
    public static final String TEMP_PROFILE_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "temp" + FileSystems.getDefault().getSeparator();
    public static final String FAKE_USER_AGENT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "useragent" + FileSystems.getDefault().getSeparator();
    
    public static final String EXTENSION_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "extension" + FileSystems.getDefault().getSeparator();
    public static final String PREVENT_WEB_RTC_LEAK_EXTENSION_PATH = EXTENSION_FOLDER_PATH + "WebRTC-Leak-Prevent-1.0.14";
    
    public static final String PROXY_SERVER_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "proxy" + FileSystems.getDefault().getSeparator();
    public static final String ORACLE_PROXY_SERVER_FILE_PATH = PROXY_SERVER_FOLDER_PATH + "oracle.txt";
    public static final String PROVIDER_PROXY_SERVER_FILE_PATH = PROXY_SERVER_FOLDER_PATH + "3rd-provider.txt";

    public static final String ELEMENT_NOT_FOUND = "ELEMENT_NOT_FOUND";
    
}
