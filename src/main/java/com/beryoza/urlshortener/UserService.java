package com.beryoza.urlshortener;

import java.util.UUID;
import java.util.function.Consumer;

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
     * Генерирует UUID, сохраняет пользователя в репозитории и выводит сообщение.
     *
     * @param name имя пользователя
     */
    public void createUser(String name) {
        User user = userRepository.saveUser(new User(UUID.randomUUID(), name));
        System.out.println("Пользователь зарегистрирован. Ваш UUID: " + user.getUserUuid());
    }

    /**
     * Метод авторизации пользователя. Устанавливает текущего пользователя.
     *
     * @param uuid           UUID пользователя
     * @param setCurrentUser Consumer для установки текущего пользователя
     */
    public void loginUser(UUID uuid, Consumer<UUID> setCurrentUser) {
        User user = userRepository.findUser(uuid);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с таким UUID не найден.");
        }
        setCurrentUser.accept(uuid); // Устанавливаем текущего пользователя
        System.out.println("Успешный вход. Добро пожаловать, " + user.getUserName() + "!");
    }

    /**
     * Находит пользователя по его UUID.
     *
     * @param uuid идентификатор пользователя
     * @return объект User, если найден, иначе null
     */
    public User getUser(UUID uuid) {
        User user = userRepository.findUser(uuid);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с UUID " + uuid + " не найден.");
        }
        return user;
    }

    /**
     * Удаляет пользователя по его UUID.
     *
     * @param uuid идентификатор пользователя
     * @param currentUserUUID UUID текущего пользователя
     * @throws IllegalArgumentException если пользователь не найден или если пользователь пытается удалить сам себя
     */
    public void deleteUser(UUID uuid, UUID currentUserUUID) {
        if (currentUserUUID != null && currentUserUUID.equals(uuid)) {
            throw new IllegalArgumentException("Вы не можете удалить текущего авторизованного пользователя. Сначала выйдите из системы.");
        }

        User user = userRepository.findUser(uuid);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с таким UUID не найден.");
        }

        userRepository.deleteUser(uuid);
        System.out.println("Пользователь с UUID " + uuid + " успешно удалён.");
    }




}
