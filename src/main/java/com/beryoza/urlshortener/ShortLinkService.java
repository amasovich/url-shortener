package com.beryoza.urlshortener;

import java.util.*;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

    /**
     * Конструктор. Подключает репозиторий коротких ссылок.
     *
     * @param shortLinkRepository репозиторий для работы с хранилищем
     */
    public ShortLinkService(ShortLinkRepository shortLinkRepository) {
        this.shortLinkRepository = shortLinkRepository;
    }

    /**
     * Создаёт новую короткую ссылку.
     *
     * @param originalUrl длинная ссылка, которую нужно сократить
     * @param userUuid    идентификатор пользователя, создающего ссылку
     * @param userTTL     время жизни ссылки (в часах), заданное пользователем
     * @param userLimit   лимит переходов, заданный пользователем
     * @return уникальный идентификатор короткой ссылки (shortId)
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
        //long expiryTime = System.currentTimeMillis() + (finalTtl * 3600000L);
        long expiryTime = System.currentTimeMillis() + Math.max(1, finalTtl * 3600000L);

        // Логгирование
        System.out.println("Создаётся ссылка с shortId: " + shortId);
        System.out.println("TTL: " + finalTtl + " часов, лимит: " + finalLimit);
        System.out.println("Время истечения: " + expiryTime);

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

        System.out.println("Очищаем устаревшие ссылки");
        // Очищаем устаревшие или исчерпанные ссылки
        cleanUpExpiredLinks();

        // Увеличиваем счётчик переходов
        link.setCurrentCount(link.getCurrentCount() + 1);
        System.out.println("Счётчик обновлён: " + link.getCurrentCount());

        // Открываем ссылку в браузере
        openInBrowser(link.getOriginalUrl());

        // Возвращаем найденную ссылку
        return link;
    }

    /**
     * Удаляет все ссылки, которые больше не должны быть доступны.
     *
     * Это включает:
     * - Ссылки с истёкшим сроком действия (expiryTime).
     * - Ссылки, у которых превышен лимит переходов (currentCount >= limit).
     *
     * Каждая удалённая ссылка будет указана в консоли с причиной удаления
     * (просрочена или достигнут лимит переходов).
     */
    /** public void cleanUpExpiredLinks() {
        // Получаем все ссылки из репозитория
        Iterator<ShortLink> iterator = shortLinkRepository.findAll().iterator();

        while (iterator.hasNext()) {
            ShortLink link = iterator.next();
            boolean isExpired = System.currentTimeMillis() > link.getExpiryTime();
            boolean isLimitExceeded = link.getCurrentCount() >= link.getLimit();

            // Если срок действия истёк или лимит переходов превышен, удаляем ссылку
            if (isExpired || isLimitExceeded) {
                shortLinkRepository.deleteByShortId(link.getShortId());
                System.out.println("Удалена ссылка с shortId: " + link.getShortId() +
                        (isExpired ? " (просрочена)" : " (достигнут лимит переходов)"));
            }
        }
    } **/
    public void cleanUpExpiredLinks() {
        // Преобразуем коллекцию в список
        List<ShortLink> allLinks = new ArrayList<>(shortLinkRepository.findAll());
        long currentTime = System.currentTimeMillis();

        for (ShortLink link : allLinks) {
            if (currentTime > link.getExpiryTime() || link.getCurrentCount() >= link.getLimit()) {
                // Уведомляем пользователя об удалении
                notifyUser("Удалена ссылка с идентификатором " + link.getShortId() +
                        (currentTime > link.getExpiryTime() ? " (истёк срок действия)." : " (достигнут лимит переходов)."));

                // Удаляем ссылку
                shortLinkRepository.deleteByShortId(link.getShortId());
            }
        }
    }

    /**
     * Уведомляет пользователя о состоянии ссылки.
     *
     * @param message текст сообщения для вывода в консоль
     */
    private void notifyUser(String message) {
        System.out.println("Уведомление пользователя: " + message);
    }

    /**
     * Открывает указанную ссылку в браузере.
     *
     * @param url оригинальный URL для открытия
     */
    private void openInBrowser(String url) {
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
     * Генерирует уникальный идентификатор для короткой ссылки.
     *
     * Формат: случайная строка из 8 символов (буквы и цифры).
     *
     * @return сгенерированный shortId
     */
    private String generateShortId() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder shortId = new StringBuilder();
        Random random = new Random();

        // Генерируем строку длиной 8 символов
        for (int i = 0; i < 8; i++) {
            shortId.append(chars.charAt(random.nextInt(chars.length())));
        }

        return shortId.toString();
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
        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если ссылка не найдена, бросаем исключение
        if (link == null) {
            notifyUser("Ссылка с идентификатором " + shortId + " не найдена.");
            throw new RuntimeException("Короткая ссылка не найдена");
        }

        // Проверяем права доступа
        if (!link.getUserUuid().equals(userUuid)) {
            notifyUser("Пользователь с UUID " + userUuid + " не имеет прав на изменение ссылки с идентификатором " + shortId + ".");
            throw new RuntimeException("Нет доступа к изменению лимита переходов");
        }

        // Проверяем новый лимит на соответствие системным ограничениям
        int adjustedLimit = Math.min(Config.getMaxLimit(), Math.max(Config.getMinLimit(), newLimit));
        if (adjustedLimit != newLimit) {
            notifyUser("Указанный лимит " + newLimit + " был скорректирован до " + adjustedLimit + " в соответствии с системными ограничениями.");
        }

        // Устанавливаем новый лимит
        link.setLimit(adjustedLimit);

        // Сохраняем обновлённую ссылку в репозитории
        shortLinkRepository.save(link);

        // Уведомляем пользователя об успешном изменении
        notifyUser("Лимит переходов для ссылки с идентификатором " + shortId + " успешно изменён на " + adjustedLimit + ".");
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
     * Изменяет время жизни короткой ссылки.
     *
     * @param shortId  идентификатор короткой ссылки
     * @param newTTL   новый срок действия в часах
     * @param userUuid UUID пользователя, инициировавшего запрос
     * @throws RuntimeException если ссылка не найдена, пользователь не имеет прав или новый срок недопустим
     */
    public void editExpiryTime(String shortId, int newTTL, UUID userUuid) {
        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если ссылка не найдена, бросаем исключение
        if (link == null) {
            notifyUser("Ссылка с идентификатором " + shortId + " не найдена.");
            throw new RuntimeException("Короткая ссылка не найдена");
        }

        // Проверяем права доступа
        if (!link.getUserUuid().equals(userUuid)) {
            notifyUser("Пользователь с UUID " + userUuid + " не имеет прав на изменение ссылки с идентификатором " + shortId + ".");
            throw new RuntimeException("Нет доступа к изменению времени жизни ссылки");
        }

        // Получаем системные ограничения
        int maxTTL = Config.getMaxTtl();
        int minTTL = Config.getMinTtl();

        // Проверяем новый срок действия
        int adjustedTTL = Math.min(maxTTL, Math.max(minTTL, newTTL));
        if (adjustedTTL != newTTL) {
            notifyUser("Указанный срок действия " + newTTL + " часов был скорректирован до " + adjustedTTL + " в соответствии с системными ограничениями (" + minTTL + "-" + maxTTL + " часов).");
        }

        // Устанавливаем новый срок действия
        link.setExpiryTime(System.currentTimeMillis() + adjustedTTL * 3600000L);

        // Сохраняем обновлённую ссылку в репозитории
        shortLinkRepository.save(link);

        // Уведомляем пользователя об успешном изменении
        notifyUser("Время жизни ссылки с идентификатором " + shortId + " успешно изменено на " + adjustedTTL + " часов.");
    }

}
