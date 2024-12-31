package com.beryoza.urlshortener;

import java.util.*;

/**
 * Репозиторий для хранения пользователей в памяти (in-memory).
 * Используем Map, где ключ — UUID пользователя, а значение — сам User.
 */
public class UserRepository {

    /**
     * Хранилище. Ключ — это userUuid (UUID),
     * значение — объект User.
     */
    private final Map<UUID, User> storage = new HashMap<>();

    /**
     * Сохраняем пользователя в хранилище. Если пользователь
     * с таким UUID уже есть, он будет перезаписан.
     *
     * @param user объект пользователя, должен иметь уникальный userUuid
     * @return тот же user (просто возвращаем для удобства)
     */
    public User saveUser(User user) {
        storage.put(user.getUserUuid(), user);
        return user;
    }

    /**
     * Ищем пользователя по его UUID.
     *
     * @param uuid идентификатор пользователя
     * @return найденный User или null, если такого нет
     */
    public User findUser(UUID uuid) {
        return storage.get(uuid);
    }

    /**
     * Удаляем пользователя по его UUID.
     * Если не нашли — ничего страшного, просто ничего не удалится.
     *
     * @param uuid идентификатор пользователя
     */
    public void deleteUser(UUID uuid) {
        storage.remove(uuid);
    }

    /**
     * Возвращает всех пользователей (например, для отладки).
     *
     * @return коллекция User из внутреннего хранилища
     */
    @SuppressWarnings("unused")
    public Collection<User> findAll() {
        return storage.values();
    }
}
