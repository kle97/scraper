package io.playground.scraper.constant;

import java.nio.file.FileSystems;

public class Constant {
    
    public static final String EXTERNAL_RESOURCES_PATH = "resources" + FileSystems.getDefault().getSeparator();
    public static final String SCREENSHOT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "screenshots" + FileSystems.getDefault().getSeparator();
    public static final String REPORT_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "reports" + FileSystems.getDefault().getSeparator();
    public static final String TEMP_PROFILE_FOLDER_PATH = EXTERNAL_RESOURCES_PATH + "temp" + FileSystems.getDefault().getSeparator();
    
}
