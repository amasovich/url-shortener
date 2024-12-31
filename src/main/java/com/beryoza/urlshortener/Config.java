package com.beryoza.urlshortener;

import java.io.IOException;
import java.util.Properties;

/**
 * Класс для загрузки параметров конфигурации из файла application.properties.
 * <p>
 * Этот класс использует системное свойство <code>env</code>, чтобы определить,
 * какой файл конфигурации загрузить. Если <code>env</code> равен <code>test</code>,
 * будет загружен файл <code>test config.properties</code>. По умолчанию используется
 * <code>application.properties</code>.
 */
public class Config {
    private static final Properties properties = new Properties();

    static {
        try {
            // Определяем окружение и имя файла конфигурации
            String env = System.getProperty("env", "default");
            String configFileName = "application.properties";
            if ("test".equals(env)) {
                configFileName = "test config.properties";
            }
            // Загружаем свойства из файла конфигурации
            properties.load(Config.class.getClassLoader().getResourceAsStream(configFileName));
        } catch (IOException | NullPointerException e) {
            System.err.println("Ошибка при загрузке конфигурационного файла: " + e.getMessage());
            e.printStackTrace(System.err); // Выводим стек вызовов в стандартный поток ошибок
            throw new RuntimeException("Не удалось загрузить конфигурационный файл: " + e.getMessage(), e);
        }
    }

    /**
     * Возвращает минимальное значение времени жизни ссылки (TTL) в часах.
     * <p>
     * Значение считывается из свойства <code>config.ttl.min</code>.
     * Если свойство отсутствует, используется значение по умолчанию <code>1</code>.
     *
     * @return минимальное значение времени жизни ссылки (TTL).
     */
    public static int getMinTtl() {
        return Integer.parseInt(properties.getProperty("config.ttl.min", "1"));
    }

    /**
     * Возвращает максимальное значение времени жизни ссылки (TTL) в часах.
     * <p>
     * Значение считывается из свойства <code>config.ttl.max</code>.
     * Если свойство отсутствует, используется значение по умолчанию <code>48</code>.
     *
     * @return максимальное значение времени жизни ссылки (TTL).
     */
    public static int getMaxTtl() {
        return Integer.parseInt(properties.getProperty("config.ttl.max", "48"));
    }

    /**
     * Возвращает минимальное значение лимита переходов по ссылке.
     * <p>
     * Значение считывается из свойства <code>config.limit.min</code>.
     * Если свойство отсутствует, используется значение по умолчанию <code>1</code>.
     *
     * @return минимальное значение лимита переходов.
     */
    public static int getMinLimit() {
        return Integer.parseInt(properties.getProperty("config.limit.min", "1"));
    }

    /**
     * Возвращает максимальное значение лимита переходов по ссылке.
     * <p>
     * Значение считывается из свойства <code>config.limit.max</code>.
     * Если свойство отсутствует, используется значение по умолчанию <code>100</code>.
     *
     * @return максимальное значение лимита переходов.
     */
    public static int getMaxLimit() {
        return Integer.parseInt(properties.getProperty("config.limit.max", "100"));
    }
}

