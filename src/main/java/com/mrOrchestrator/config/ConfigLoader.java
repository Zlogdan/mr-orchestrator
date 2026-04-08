package com.mrOrchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;

/**
 * Загрузчик конфигурации из файла config.yaml
 */
public class ConfigLoader {

    private static final String CONFIG_FILE_NAME = "config.yaml";

    /**
     * Загружает конфигурацию из рабочей директории или classpath
     */
    public static AppConfig load() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // Сначала пробуем загрузить из рабочей директории
        File configFile = new File(CONFIG_FILE_NAME);
        if (configFile.exists()) {
            return mapper.readValue(configFile, AppConfig.class);
        }

        // Затем из classpath
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (is != null) {
                return mapper.readValue(is, AppConfig.class);
            }
        }

        throw new IllegalStateException("Файл конфигурации config.yaml не найден в рабочей директории или classpath");
    }
}
