package com.beryoza.urlshortener;

import java.util.UUID;

/**
 * Класс описывает пользователя, которого мы будем
 * идентифицировать по-уникальному UUID.
 *
 * Пояснения:
 *  - UUID обязателен, так как всё управление ссылками (создание, редактирование)
 *    завязано именно на этот идентификатор.
 *  - name (имя) не строго необходимо по ТЗ, но может быть полезно
 *    для удобства или отладки.
 */
public class User {

    /** Уникальный идентификатор пользователя в формате UUID. */
    private UUID userUuid;

    /** Имя пользователя — если надо как-то его идентифицировать «по-человечески». */
    private String name;

    /**
     * Пустой конструктор на всякий случай: пригодится,
     * если вдруг потребуется безаргументное создание
     */
    public User() {
    }

    /**
     * Конструктор, где мы задаём сразу UUID и имя.
     *
     * @param userUuid уникальный идентификатор (обязателен)
     * @param name     имя пользователя (опционально)
     */
    public User(UUID userUuid, String name) {
        this.userUuid = userUuid;
        this.name = name;
    }

    /**
     * Возвращает UUID пользователя (ключ к действиям в системе).
     */
    public UUID getUserUuid() {
        return userUuid;
    }

    /**
     * Позволяет установить новый UUID (обычно не требуется менять).
     */
    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }

    /**
     * Возвращает имя пользователя (если задано).
     */
    public String getUserName() {
        return name;
    }

    /**
     * Устанавливает имя пользователя (если хотим его переименовать).
     */
    public void setUserName(String name) {
        this.name = name;
    }

    /**
     * Удобный вывод данных для отладки или логов.
     */
    @Override
    public String toString() {
        return "User { userUuid=" + userUuid + ", name='" + name + "' }";
    }
}