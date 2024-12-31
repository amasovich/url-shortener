package com.beryoza.urlshortener;

import java.awt.Desktop;
import java.net.URI;
import java.util.*;

public class ShortLinkConsoleApp {

    private static final ShortLinkService shortLinkService = new ShortLinkService(new ShortLinkRepository());
    private static final UserService userService = new UserService(new UserRepository());
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
        System.out.println("- shorten <url> <ttlHours> <limit>: Создание короткой ссылки");
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
        String name = parts[1];
        User user = userService.createUser(name);
        System.out.println("Пользователь зарегистрирован. Ваш UUID: " + user.getUserUuid());
    }

    private static void loginUser(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите UUID для авторизации.");
        }
        UUID uuid = UUID.fromString(parts[1]);
        User user = userService.loginUser(uuid); // Теперь возвращается объект User
        currentUser = uuid; // Сохраняем текущего пользователя
        System.out.println("Успешный вход. Добро пожаловать, " + user.getUserName() + "!");
    }

    private static void deleteUser(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите UUID пользователя для удаления.");
        }
        UUID uuid = UUID.fromString(parts[1]);
        if (currentUser != null && currentUser.equals(uuid)) {
            System.out.println("Вы не можете удалить текущего авторизованного пользователя. Сначала выйдите из системы.");
            return;
        }
        if (userService.getUser(uuid) == null) {
            System.out.println("Пользователь с таким UUID не найден.");
            return;
        }
        userService.deleteUser(uuid);
        System.out.println("Пользователь с UUID " + uuid + " успешно удалён.");
    }

    private static void shortenUrl(String[] parts) {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите URL.");
        }

        String url = parts[1];

        // Берём время жизни (TTL) и лимит из конфигурации
        int ttlHours = Config.getMaxTtl(); // Используем максимальный TTL из конфигурации
        int limit = Config.getMaxLimit(); // Используем максимальный лимит из конфигурации

        String shortId = shortLinkService.createShortLink(url, currentUser, ttlHours, limit);
        System.out.println("Ссылка создана: " + shortId + "\nВремя жизни: " + ttlHours + " часов, Лимит переходов: " + limit);
    }

    private static void listUrl() {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        List<ShortLink> links = shortLinkService.getUserLinks(currentUser);
        if (links.isEmpty()) {
            System.out.println("У вас нет созданных ссылок.");
        } else {
            System.out.println("Ваши ссылки:");
            for (ShortLink link : links) {
                System.out.printf("- %s -> %s (лимит: %d, переходов: %d, истекает через: %d часов)%n",
                        link.getShortId(), link.getOriginalUrl(), link.getLimit(), link.getCurrentCount(),
                        (link.getExpiryTime() - System.currentTimeMillis()) / 3600000);
            }
        }
    }

    private static void deleteUrl(String[] parts) {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите идентификатор короткой ссылки.");
        }
        String shortId = parts[1];
        shortLinkService.deleteShortLink(shortId, currentUser);
        System.out.println("Ссылка " + shortId + " успешно удалена.");
    }

    private static void editLimitUrl(String[] parts) {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        if (parts.length < 3) {
            throw new IllegalArgumentException("Введите идентификатор ссылки и новый лимит.");
        }
        String shortId = parts[1];
        int newLimit = Integer.parseInt(parts[2]);
        shortLinkService.editRedirectLimit(shortId, newLimit, currentUser);
        System.out.println("Лимит успешно изменён на " + newLimit + ".");
    }

    private static void editExpiryUrl(String[] parts) {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        if (parts.length < 3) {
            throw new IllegalArgumentException("Введите идентификатор ссылки и новое время жизни (в часах).");
        }
        String shortId = parts[1];
        int newTTL = Integer.parseInt(parts[2]);
        shortLinkService.editExpiryTime(shortId, newTTL, currentUser);
        System.out.println("Время жизни ссылки " + shortId + " успешно изменено на " + newTTL + " часов.");
    }

    private static void gotoLink(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите идентификатор короткой ссылки.");
        }
        String shortId = parts[1];
        ShortLink link = shortLinkService.getOriginalUrl(shortId);
        shortLinkService.openInBrowser(link.getOriginalUrl());
    }

    private static void cleanUrl() {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        int cleanedCount = shortLinkService.cleanUpExpiredLinks(currentUser);
        System.out.println("Очистка завершена. Удалено " + cleanedCount + " устаревших ссылок.");
    }

    private static void whoAmI() {
        if (currentUser == null) {
            System.out.println("Вы не авторизованы.");
        } else {
            User user = userService.getUser(currentUser);
            System.out.println("Текущий пользователь: " + user.getUserName() + " (UUID: " + user.getUserUuid() + ")");
        }
    }
}
