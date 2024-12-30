package com.beryoza.urlshortener;

import java.util.UUID;

/**
 * Сервис для управления пользователями.
 * Отвечает за создание, поиск и (опционально) удаление пользователей.
 */
public class UserService {

    private final UserRepository userRepository;

    /**
     * Конструктор. Принимает UserRepository для взаимодействия с хранилищем пользователей.
     *
     * @param userRepository экземпляр репозитория пользователей
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Создаёт нового пользователя.
     * Генерирует UUID и сохраняет пользователя в репозитории.
     *
     * @param name имя пользователя (опционально)
     * @return созданный пользователь
     */
    public User createUser(String name) {
        User user = new User(UUID.randomUUID(), name);
        return userRepository.save(user);
    }

    /**
     * Находит пользователя по его UUID.
     *
     * @param uuid идентификатор пользователя
     * @return объект User, если найден, иначе null
     */
    public User getUser(UUID uuid) {
        return userRepository.findByUuid(uuid);
    }

    /**
     * Удаляет пользователя по его UUID.
     * Если пользователя с таким UUID нет, ничего не происходит.
     *
     * @param uuid идентификатор пользователя
     */
    public void deleteUser(UUID uuid) {
        userRepository.deleteByUuid(uuid);
    }
}
