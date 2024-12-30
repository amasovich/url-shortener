package com.beryoza.urlshortener;

import com.beryoza.urlshortener.UserRepository;
import java.util.UUID;


public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name) {
        // генерируем UUID
        User user = new User(UUID.randomUUID(), name);
        // сохраняем
        return userRepository.save(user);
    }

    public User getUser(UUID uuid) {
        return userRepository.findByUuid(uuid);
    }
}

