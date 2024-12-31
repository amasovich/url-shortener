package com.beryoza.urlshortener;

import java.io.IOException;
import java.util.Properties;

/**
 * Класс для загрузки параметров конфигурации из файла application.properties.
 */
public class Config {
    private static final Properties properties = new Properties();

    static {
        try {
            String env = System.getProperty("env", "default");
            String configFileName = "application.properties";
            if ("test".equals(env)) {
                configFileName = "testconfig.properties";
            }
            System.out.println("Загрузка файла конфигурации: " + configFileName);
            properties.load(Config.class.getClassLoader().getResourceAsStream(configFileName));
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось загрузить конфигурационный файл: " + e.getMessage(), e);
        }
    }

    public static int getMinTtl() {
        return Integer.parseInt(properties.getProperty("config.ttl.min", "1"));
    }

    public static int getMaxTtl() {
        return Integer.parseInt(properties.getProperty("config.ttl.max", "48"));
    }

    public static int getMinLimit() {
        return Integer.parseInt(properties.getProperty("config.limit.min", "1"));
    }

    public static int getMaxLimit() {
        return Integer.parseInt(properties.getProperty("config.limit.max", "100"));
    }
}
