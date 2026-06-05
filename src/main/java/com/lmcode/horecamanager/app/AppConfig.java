package com.lmcode.horecamanager.app;

import java.nio.file.Path;

public final class AppConfig {
    public static final String APP_NAME = "HorecaManager Pro";
    public static final String PACKAGE_NAME = "com.lmcode.horecamanager";
    public static final String DATABASE_FILE = "horeca_manager.db";
    public static final Path DATABASE_PATH = Path.of(DATABASE_FILE).toAbsolutePath();
    public static final int MIN_WIDTH = 1200;
    public static final int MIN_HEIGHT = 760;
    public static final String DEFAULT_CURRENCY = "EUR";
    public static final double DEFAULT_VAT_RATE = 21.0;

    private AppConfig() {
    }
}
