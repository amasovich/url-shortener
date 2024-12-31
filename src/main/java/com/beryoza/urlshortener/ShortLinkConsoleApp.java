package com.beryoza.urlshortener;

import java.util.*;

public class ShortLinkConsoleApp {

    private static final ShortLinkService ShortLinkService = new ShortLinkService(new ShortLinkRepository());
    private static final UserService UserService = new UserService(new UserRepository());
    private static UUID currentUser;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Добро пожаловать в сервис сокращения ссылок!");
        printHelp();

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();

            if (command.equals("exit")) {
                System.out.println("Завершение работы программы. До свидания!");
                break;
            }
            handleCommand(command);
        }
        scanner.close();
    }

    private static void handleCommand(String command) {
        String[] parts = command.split(" ");
        String action = parts[0];

        try {
            switch (action) {
                case "register":
                    registerUser(parts);
                    break;
                case "login":
                    loginUser(parts);
                    break;
                case "shorten":
                    shortenUrl(parts);
                    break;
                case "list":
                    listUrl();
                    break;
                case "delete":
                    deleteUrl(parts);
                    break;
                case "edit-limit":
                    editLimitUrl(parts);
                    break;
                case "edit-expiry":
                    editExpiryUrl(parts);
                    break;
                case "goto":
                    gotoLink(parts);
                    break;
                case "clean":
                    cleanUrl();
                    break;
                case "delete-user":
                    deleteUser(parts);
                    break;
                case "whoAmI":
                    whoAmI();
                    break;
                default:
                    System.out.println("Неизвестная команда. Введите 'help' для списка доступных команд.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("- register <name>: Регистрация нового пользователя");
        System.out.println("- login <uuid>: Авторизация пользователя");
        System.out.println("- shorten <url>: Создание короткой ссылки");
        System.out.println("- list: Просмотр всех ссылок пользователя");
        System.out.println("- delete <shortId>: Удаление ссылки");
        System.out.println("- edit-limit <shortId> <newLimit>: Изменение лимита переходов");
        System.out.println("- edit-expiry <shortId> <newTTL>: Изменение времени жизни ссылки");
        System.out.println("- goto <shortId>: Переход по ссылке");
        System.out.println("- clean: Удаление устаревших ссылок");
        System.out.println("- whoAmI: Показ текущего пользователя");
        System.out.println("- exit: Завершение работы приложения");
    }

    private static void registerUser(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите имя пользователя для регистрации.");
        }
        UserService.createUser(parts[1]);
    }

    private static void loginUser(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите UUID для авторизации.");
        }
        UUID uuid = UUID.fromString(parts[1]);
        UserService.loginUser(uuid, newUuid -> currentUser = newUuid); // Установка текущего пользователя
    }

    private static void deleteUser(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите UUID пользователя для удаления.");
        }
        UserService.deleteUser(UUID.fromString(parts[1]), currentUser);
    }

    private static void ensureLoggedIn() {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
    }

    private static void shortenUrl(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите URL.");
        }
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.createShortLink(parts[1], currentUser, Config.getMaxTtl(), Config.getMaxLimit());
    }

    private static void listUrl() {
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.getUserLinks(currentUser, true);
    }

    private static void deleteUrl(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите идентификатор короткой ссылки.");
        }
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.deleteShortLink(parts[1], currentUser);
    }

    private static void editLimitUrl(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("Введите идентификатор ссылки и новый лимит.");
        }
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.editRedirectLimit(parts[1], Integer.parseInt(parts[2]), currentUser);
    }

    private static void editExpiryUrl(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("Введите идентификатор ссылки и новое время жизни (в часах).");
        }
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.editExpiryTime(parts[1], Integer.parseInt(parts[2]), currentUser);
    }

    private static void gotoLink(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите идентификатор короткой ссылки.");
        }
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.openInBrowser(ShortLinkService.getOriginalUrl(parts[1]).getOriginalUrl());
    }

    private static void cleanUrl() {
        ensureLoggedIn(); // Проверка авторизации
        ShortLinkService.cleanUpExpiredLinks(currentUser);
    }

    private static void whoAmI() {
        ensureLoggedIn(); // Проверка авторизации
        User user = UserService.getUser(currentUser);
        System.out.println("Текущий пользователь: " + user.getUserName() + " (UUID: " + user.getUserUuid() + ")");
    }

}
