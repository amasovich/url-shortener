package com.beryoza.urlshortener;

import java.util.Random;
import java.util.UUID;
import java.util.Iterator;

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

        // Проверяем TTL, чтобы он укладывался в системные пределы
        int finalTtl = Math.min(Config.getMaxTtl(), Math.max(Config.getMinTtl(), userTTL));

        // Проверяем лимит переходов, чтобы он укладывался в системные пределы
        int finalLimit = Math.min(Config.getMaxLimit(), Math.max(Config.getMinLimit(), userLimit));

        // Сгенерировать уникальный shortId
        String shortId = generateShortId();

        // Рассчитать время истечения ссылки
        long expiryTime = System.currentTimeMillis() + (finalTtl * 3600000L);

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

        // Вернуть shortId пользователю
        return shortId;
    }

    /**
     * Получает оригинальную ссылку по её короткому идентификатору.
     *
     * Перед возвращением проверяет:
     * - Срок действия (если истёк — ссылка недоступна).
     * - Лимит переходов (если превышен — ссылка недоступна).
     *
     * @param shortId короткий идентификатор ссылки
     * @return объект ShortLink, содержащий оригинальную ссылку
     * @throws RuntimeException если ссылка не найдена, истекла или превышен лимит
     */
    public ShortLink getOriginalUrl(String shortId) {
        // Очищаем устаревшие ссылки
        cleanUpExpiredLinks();

        // Ищем ссылку в репозитории
        ShortLink link = shortLinkRepository.findByShortId(shortId);

        // Если не нашли, бросаем исключение
        if (link == null) {
            throw new RuntimeException("ShortLink not found");
        }

        // Проверяем срок действия ссылки
        if (System.currentTimeMillis() > link.getExpiryTime()) {
            throw new RuntimeException("ShortLink has expired");
        }

        // Проверяем лимит переходов
        if (link.getCurrentCount() >= link.getLimit()) {
            throw new RuntimeException("ShortLink has exceeded its limit");
        }

        // Увеличиваем счётчик переходов
        link.setCurrentCount(link.getCurrentCount() + 1);

        // Возвращаем найденную ссылку
        return link;
    }

    /**
     * Удаляет все ссылки, срок действия которых истёк.
     */
    public void cleanUpExpiredLinks() {
        // Получаем все ссылки из репозитория
        Iterator<ShortLink> iterator = shortLinkRepository.findAll().iterator();

        while (iterator.hasNext()) {
            ShortLink link = iterator.next();

            // Если срок действия истёк, удаляем ссылку
            if (System.currentTimeMillis() > link.getExpiryTime()) {
                shortLinkRepository.deleteByShortId(link.getShortId());
                System.out.println("Удалена ссылка с shortId: " + link.getShortId() + " (просрочена)");
            }
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
}
