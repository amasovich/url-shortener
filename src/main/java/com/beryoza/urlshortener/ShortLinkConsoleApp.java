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
                    register(parts);
                    break;
                case "login":
                    login(parts);
                    break;
                case "shorten":
                    shorten(parts);
                    break;
                case "list":
                    list();
                    break;
                case "delete":
                    delete(parts);
                    break;
                case "edit-limit":
                    editLimit(parts);
                    break;
                case "goto":
                    gotoLink(parts);
                    break;
                case "clean":
                    clean();
                    break;
                case "whoami":
                    whoami();
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
        System.out.println("- goto <shortId>: Переход по ссылке");
        System.out.println("- clean: Удаление устаревших ссылок");
        System.out.println("- whoami: Показ текущего пользователя");
        System.out.println("- exit: Завершение работы приложения");
    }

    private static void register(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите имя пользователя для регистрации.");
        }
        String name = parts[1];
        User user = userService.register(name);
        System.out.println("Пользователь зарегистрирован. Ваш UUID: " + user.getUuid());
    }

    private static void login(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите UUID для авторизации.");
        }
        UUID uuid = UUID.fromString(parts[1]);
        if (userService.login(uuid)) {
            currentUser = uuid;
            System.out.println("Успешный вход. Добро пожаловать, " + userService.getUserByUuid(uuid).getName() + "!");
        } else {
            throw new IllegalArgumentException("Пользователь с таким UUID не найден.");
        }
    }

    private static void shorten(String[] parts) {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        if (parts.length < 4) {
            throw new IllegalArgumentException("Введите URL, время жизни (в часах) и лимит переходов.");
        }
        String url = parts[1];
        int ttlHours = Integer.parseInt(parts[2]);
        int limit = Integer.parseInt(parts[3]);
        String shortId = shortLinkService.createShortLink(url, currentUser, ttlHours, limit);
        System.out.println("Ссылка создана: " + shortId);
    }

    private static void list() {
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

    private static void delete(String[] parts) {
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

    private static void editLimit(String[] parts) {
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

    private static void gotoLink(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Введите идентификатор короткой ссылки.");
        }
        String shortId = parts[1];
        try {
            ShortLink link = shortLinkService.getOriginalUrl(shortId);
            Desktop.getDesktop().browse(new URI(link.getOriginalUrl()));
        } catch (Exception e) {
            System.out.println("Ошибка при открытии ссылки: " + e.getMessage());
        }
    }

    private static void clean() {
        if (currentUser == null) {
            throw new IllegalStateException("Необходимо авторизоваться для выполнения команды.");
        }
        int cleanedCount = shortLinkService.cleanUpExpiredLinks(currentUser);
        System.out.println("Очистка завершена. Удалено " + cleanedCount + " устаревших ссылок.");
    }

    private static void whoami() {
        if (currentUser == null) {
            System.out.println("Вы не авторизованы.");
        } else {
            User user = userService.getUserByUuid(currentUser);
            System.out.println("Текущий пользователь: " + user.getName() + " (UUID: " + user.getUuid() + ")");
        }
    }
}
