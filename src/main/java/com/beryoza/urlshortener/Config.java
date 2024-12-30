package com.beryoza.urlshortener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Класс для загрузки параметров конфигурации из файла application.properties.
 */
public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Конфигурационный файл не найден!");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить конфигурационный файл!", e);
        }
    }

    public static int getMinTtl() {
        return Integer.parseInt(properties.getProperty("config.ttl.min", "1")); // Значение по умолчанию: 1 час
    }

    public static int getMaxTtl() {
        return Integer.parseInt(properties.getProperty("config.ttl.max", "48")); // Значение по умолчанию: 48 часов
    }

    public static int getMinLimit() {
        return Integer.parseInt(properties.getProperty("config.limit.min", "1")); // Значение по умолчанию: 1 переход
    }

    public static int getMaxLimit() {
        return Integer.parseInt(properties.getProperty("config.limit.max", "100")); // Значение по умолчанию: 100 переходов
    }
}
