package com.beryoza.urlshortener;

import java.util.*;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для управления короткими ссылками.
 *
 * Отвечает за:
 * - Генерацию коротких ссылок.
 * - Проверку сроков действия и лимитов переходов.
 * - Увеличение счётчика переходов при доступе к ссылке.
 */
public class ShortLinkService {

    // Репозиторий для работы с хранилищем коротких ссылок.
    private final ShortLinkRepository shortLinkRepository;
    private final List<String> notifications; // Для хранения уведомлений

    /**
     * Конструктор. Подключает репозиторий коротких ссылок.
     *
     * @param shortLinkRepository репозиторий для работы с хранилищем
     */
    public ShortLinkService(ShortLinkRepository shortLinkRepository) {
        this.shortLinkRepository = shortLinkRepository;
        this.notifications = new ArrayList<>();
    }

    /**
     * Создаёт новую короткую ссылку.
     *
     * Метод генерирует уникальный идентификатор ссылки, проверяет входные данные
     * и применяет системные ограничения на время жизни и лимит переходов.
     * Устаревшие ссылки удаляются перед созданием новой.
     *
     * @param originalUrl длинная ссылка, которую нужно сократить
     * @param userUuid    идентификатор пользователя, создающего ссылку
     * @param userTTL     время жизни ссылки (в часах), заданное пользователем
     * @param userLimit   лимит переходов, заданный пользователем
     * @return уникальный идентификатор короткой ссылки (shortId)
     * @throws IllegalArgumentException если URL или UUID пользователя некорректны
     *
     * Пример:
     * {@code
     * String shortId = createShortLink("http://example.com", userUuid, 24, 100);
     * }
     */
    public String createShortLink(String originalUrl, UUID userUuid, int userTTL, int userLimit) {
        // Очищаем устаревшие ссылки
        cleanUpExpiredLinks();

        // Проверяем входные данные
        if (originalUrl == null || originalUrl.isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }
        if (userUuid == null) {
            throw new IllegalArgumentException("UUID пользователя не может быть null");
        }

        // Проверяем TTL и лимит переходов
        int finalTtl = Math.min(Config.getMaxTtl(), Math.max(Config.getMinTtl(), userTTL));
        int finalLimit = Math.min(Config.getMaxLimit(), Math.max(Config.getMinLimit(), userLimit));

        // Сгенерировать уникальный shortId
        String shortId = generateShortId();

        // Рассчитать время истечения ссылки
        long expiryTime = System.currentTimeMillis() + Math.max(1, finalTtl * 3600000L);

        // Создать объект ShortLink
        ShortLink link = new ShortLink(
                shortId,
                originalUrl,
                System.currentTimeMillis(),
                expiryTime,
                finalLimit,
                0,
                userUuid
        );

        // Сохранить ссылку в репозитории
        shortLinkRepository.save(link);

        System.out.println("Ссылка создана: " + shortId + "\nВремя жизни: " + finalTtl + " часов, Лимит переходов: " + finalLimit);

        // Возвратить shortId
        return shortId;
    }

    /**
     * Возвращает оригинальную ссылку по её короткому идентификатору (shortId).
     *
     * Перед возвращением ссылка проверяется на:
     * - Истечение срока действия (expiryTime).
     * - Превышение лимита переходов (currentCount >= limit).
     *
     * Если ссылка недоступна по одной из причин, уведомляет пользователя и бросает исключение.
     *
     * @param shortId короткий идентификатор ссылки
     * @return объект ShortLink, содержащий оригинальную ссылку
     * @throws RuntimeException если ссылка недоступна или не найдена
     */
    public ShortLink getOriginalUrl(String shortId) {
        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если не нашли, уведомляем пользователя и бросаем исключение
        if (link == null) {
            System.out.println("Ссылка не найдена");
            notifyUser("Ссылка с идентификатором " + shortId + " не найдена.");
            throw new RuntimeException("Ссылка с идентификатором " + shortId + " не найдена.");
        }

        // Проверяем срок действия ссылки
        System.out.println("Текущая метка времени: " + System.currentTimeMillis());
        System.out.println("Время истечения: " + link.getExpiryTime());
        if (System.currentTimeMillis() > link.getExpiryTime()) {
            notifyUser("Срок действия ссылки с идентификатором " + shortId + " истёк.");
            throw new RuntimeException("Срок действия короткой ссылки истек");
        }

        // Проверяем лимит переходов
        System.out.println("Текущий счётчик: " + link.getCurrentCount());
        System.out.println("Лимит переходов: " + link.getLimit());
        if (link.getCurrentCount() >= link.getLimit()) {
            notifyUser("Ссылка с идентификатором " + shortId + " превысила лимит переходов.");
            throw new RuntimeException("Количество коротких ссылок превысило установленный лимит");
        }

        // Увеличиваем счётчик переходов
        link.setCurrentCount(link.getCurrentCount() + 1);
        System.out.println("Счётчик обновлён: " + link.getCurrentCount());

        // Сохраняем изменения
        shortLinkRepository.save(link);

        // Открываем ссылку в браузере
        openInBrowser(link.getOriginalUrl());

        // Возвращаем найденную ссылку
        return link;
    }

    /**
     * Изменяет лимит переходов для существующей короткой ссылки.
     *
     * @param shortId  идентификатор короткой ссылки
     * @param newLimit новый лимит переходов
     * @param userUuid UUID пользователя, инициировавшего запрос
     * @throws RuntimeException если ссылка не найдена, пользователь не имеет доступа или новый лимит недопустим
     */
    public void editRedirectLimit(String shortId, int newLimit, UUID userUuid) {
        // Проверяем входные данные
        if (shortId == null || shortId.isEmpty()) {
            throw new IllegalArgumentException("Идентификатор ссылки не может быть пустым");
        }
        if (userUuid == null) {
            throw new IllegalArgumentException("UUID пользователя не может быть null");
        }

        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если ссылка не найдена, бросаем исключение
        if (link == null) {
            throw new RuntimeException("Ссылка с идентификатором " + shortId + " не найдена.");
        }

        // Проверяем права доступа
        if (!link.getUserUuid().equals(userUuid)) {
            throw new RuntimeException("Пользователь с UUID " + userUuid + " не имеет прав на изменение ссылки " + shortId + ".");
        }

        // Проверяем новый лимит на соответствие системным ограничениям
        int adjustedLimit = Math.min(Config.getMaxLimit(), Math.max(Config.getMinLimit(), newLimit));
        if (adjustedLimit != newLimit) {
            notifyUser("Лимит " + newLimit + " был скорректирован до " + adjustedLimit + " в соответствии с системными ограничениями.");
        }

        // Устанавливаем новый лимит
        link.setLimit(adjustedLimit);

        // Сохраняем обновлённую ссылку в репозитории
        shortLinkRepository.save(link);

        // Уведомляем пользователя об успешном изменении
        notifyUser("Лимит переходов для ссылки " + shortId + " успешно изменён на " + adjustedLimit + ".");
    }

    /**
     * Изменяет время жизни короткой ссылки.
     *
     * @param shortId  идентификатор короткой ссылки
     * @param newTTL   новый срок действия в часах
     * @param userUuid UUID пользователя, инициировавшего запрос
     * @throws RuntimeException если ссылка не найдена, пользователь не имеет прав или новый срок недопустим
     */
    public void editExpiryTime(String shortId, int newTTL, UUID userUuid) {
        // Проверяем входные данные
        if (shortId == null || shortId.isEmpty()) {
            throw new IllegalArgumentException("Идентификатор ссылки не может быть пустым");
        }
        if (userUuid == null) {
            throw new IllegalArgumentException("UUID пользователя не может быть null");
        }

        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если ссылка не найдена, бросаем исключение
        if (link == null) {
            throw new RuntimeException("Ссылка с идентификатором " + shortId + " не найдена.");
        }

        // Проверяем права доступа
        if (!link.getUserUuid().equals(userUuid)) {
            throw new RuntimeException("Пользователь с UUID " + userUuid + " не имеет прав на изменение ссылки " + shortId + ".");
        }

        // Проверяем новый срок действия на соответствие ограничениям
        int adjustedTTL = Math.min(Config.getMaxTtl(), Math.max(Config.getMinTtl(), newTTL));
        if (adjustedTTL != newTTL) {
            notifyUser("Срок действия " + newTTL + " часов был скорректирован до " + adjustedTTL + ".");
        }

        // Устанавливаем новый срок действия
        link.setExpiryTime(System.currentTimeMillis() + adjustedTTL * 3600000L);

        // Сохраняем обновлённую ссылку
        shortLinkRepository.save(link);

        // Уведомляем пользователя об успешном изменении
        notifyUser("Время жизни ссылки " + shortId + " успешно изменено на " + adjustedTTL + " часов.");
    }

    /**
     * Удаляет короткую ссылку по её идентификатору.
     *
     * @param shortId  идентификатор короткой ссылки
     * @param userUuid UUID пользователя, инициировавшего запрос
     * @throws RuntimeException если ссылка не найдена или пользователь не имеет прав на удаление
     */
    public void deleteShortLink(String shortId, UUID userUuid) {
        // Проверяем, что userUuid не null
        if (userUuid == null) {
            throw new IllegalArgumentException("UUID пользователя не может быть null");
        }

        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если ссылка не найдена, бросаем исключение
        if (link == null) {
            notifyUser("Ссылка с идентификатором " + shortId + " не найдена.");
            throw new RuntimeException("Короткая ссылка не найдена");
        }

        // Проверяем права доступа
        if (!link.getUserUuid().equals(userUuid)) {
            notifyUser("Пользователь с UUID " + userUuid + " не имеет прав на удаление ссылки с идентификатором " + shortId + ".");
            throw new RuntimeException("Нет доступа к удалению ссылки");
        }

        // Удаляем ссылку из репозитория
        shortLinkRepository.deleteByShortId(shortId);

        // Уведомляем пользователя об успешном удалении
        notifyUser("Ссылка с идентификатором " + shortId + " успешно удалена.");
    }

    /**
     * Удаляет устаревшие ссылки.
     *
     * Метод проверяет все ссылки в репозитории и удаляет те, у которых истёк срок действия
     * или превышен лимит переходов. Если указан UUID пользователя, очищаются только его ссылки.
     *
     * @param userUuid UUID пользователя (если null, очищаются все ссылки)
     */
    public void cleanUpExpiredLinks(UUID userUuid) {
        // Преобразуем коллекцию в список
        List<ShortLink> allLinks = new ArrayList<>(shortLinkRepository.findAll());
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        for (ShortLink link : allLinks) {
            // Проверяем принадлежность пользователя (если указан UUID)
            if (userUuid != null && !link.getUserUuid().equals(userUuid)) {
                continue; // Пропускаем ссылки других пользователей
            }

            // Проверяем срок действия и лимит переходов
            if (currentTime > link.getExpiryTime() || link.getCurrentCount() >= link.getLimit()) {
                // Уведомляем пользователя об удалении
                notifyUser("Удалена ссылка с идентификатором " + link.getShortId() +
                        (currentTime > link.getExpiryTime() ? " (истёк срок действия)." : " (достигнут лимит переходов)."));

                // Удаляем ссылку
                shortLinkRepository.deleteByShortId(link.getShortId());
                removedCount++;
            }
        }

        // Вывод итогового сообщения
        if (removedCount > 0) {
            System.out.println("Очистка завершена. Удалено " + removedCount + " устаревших ссылок.");
        } else {
            System.out.println("Очистка завершена. Устаревших ссылок не найдено.");
        }
    }

   public void cleanUpExpiredLinks() {
        cleanUpExpiredLinks(null); // Очистка всех ссылок
    }

    /**
     * Генерирует уникальный короткий идентификатор ссылки.
     *
     * @return строка с уникальным идентификатором
     */
    private String generateShortId() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder shortId = new StringBuilder();
        Random random = new Random();

        // Генерируем строку длиной 6 символов
        for (int i = 0; i < 6; i++) {
            shortId.append(chars.charAt(random.nextInt(chars.length())));
        }

        return shortId.toString();
    }

    /**
     * Отправляет уведомление пользователю.
     *
     * @param message текст уведомления
     */
    private void notifyUser(String message) {
        notifications.add(message); // Добавляем сообщение в список
        System.out.println("Добавлено уведомление: " + message);
    }

    /**
     * Возвращает список уведомлений, добавленных в процессе работы.
     *
     * Уведомления представляют собой сообщения, генерируемые системой,
     * например, при превышении лимита переходов или истечении срока действия ссылки.
     *
     * @return список строковых сообщений уведомлений
     */
    public List<String> getNotifications() {
        System.out.println("Текущие уведомления: " + notifications);
        return notifications; // Метод для получения уведомлений
    }

    /**
     * Очищает список уведомлений.
     *
     * Этот метод можно использовать для сброса всех уведомлений, чтобы
     * подготовить систему к новой серии операций или тестов.
     */
    public void clearNotifications() {
        notifications.clear();
    }

    /**
     * Открывает оригинальный URL в браузере.
     *
     * @param url оригинальная ссылка для открытия
     */
    void openInBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Ссылка открыта в браузере: " + url);
            } catch (IOException | URISyntaxException e) {
                System.err.println("Ошибка при попытке открыть ссылку: " + e.getMessage());
            }
        } else {
            System.err.println("Операция Desktop не поддерживается на данной системе.");
        }
    }

    /**
     * Получает ссылки пользователя и, при необходимости, выводит их.
     *
     * @param userUuid   UUID пользователя
     * @param printLinks Если true, ссылки будут выведены в консоль
     */
    public void getUserLinks(UUID userUuid, boolean printLinks) {
        List<ShortLink> userLinks = new ArrayList<>();
        for (ShortLink link : shortLinkRepository.findAll()) {
            if (link.getUserUuid().equals(userUuid)) {
                userLinks.add(link);
            }
        }

        if (printLinks) {
            if (userLinks.isEmpty()) {
                System.out.println("У вас нет созданных ссылок.");
            } else {
                System.out.println("Ваши ссылки:");
                for (ShortLink link : userLinks) {
                    System.out.printf("- %s -> %s (лимит: %d, переходов: %d, истекает через: %d часов)%n",
                            link.getShortId(), link.getOriginalUrl(), link.getLimit(), link.getCurrentCount(),
                            (link.getExpiryTime() - System.currentTimeMillis()) / 3600000);
                }
            }
        }

    }




}
