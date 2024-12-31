package com.beryoza.urlshortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShortLinkServiceTest {

    private ShortLinkService shortLinkService;
    private ShortLinkRepository shortLinkRepository;

    @BeforeEach
    public void setUp() {
        shortLinkRepository = new ShortLinkRepository();
        shortLinkService = new ShortLinkService(shortLinkRepository);
        shortLinkService.clearNotifications(); // Очищаем уведомления перед каждым тестом
    }

    @Test
    public void testUniqueShortLinksForDifferentUsers() {
        String originalUrl = "http://example.com";
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        String shortId1 = shortLinkService.createShortLink(originalUrl, user1, 24, 10);
        String shortId2 = shortLinkService.createShortLink(originalUrl, user2, 24, 10);

        assertNotEquals(shortId1, shortId2, "Сокращённые ссылки для разных пользователей должны быть уникальными.");
    }

    @Test
    public void testLimitExceedBlocksLink() {
        String originalUrl = "http://example.com";
        UUID user = UUID.randomUUID();
        String shortId = shortLinkService.createShortLink(originalUrl, user, 24, 2);

        // Переход по ссылке 2 раза
        shortLinkService.getOriginalUrl(shortId);
        shortLinkService.getOriginalUrl(shortId);

        // Третий переход должен быть заблокирован
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shortLinkService.getOriginalUrl(shortId));
        assertTrue(exception.getMessage().contains("Количество коротких ссылок превысило установленный лимит"),
                "Ссылка должна блокироваться при превышении лимита переходов.");
    }

    @Test
    public void testExpiryTimeRemovesLink() {
        String originalUrl = "http://example.com";
        UUID user = UUID.randomUUID();

        String shortId = shortLinkService.createShortLink(originalUrl, user, 1, 10); // TTL = 1 час

        // Симулируем истечение времени
        ShortLink link = shortLinkRepository.findByShortId(shortId);
        assertNotNull(link, "Ссылка должна существовать до истечения срока.");

        // Устанавливаем время истечения вручную
        link.setExpiryTime(System.currentTimeMillis() - 1000);
        shortLinkService.cleanUpExpiredLinks();

        assertNull(shortLinkRepository.findByShortId(shortId), "Ссылка должна быть удалена после истечения срока.");
    }

    @Test
    public void testNotificationOnBlockedOrExpiredLink() {
        String originalUrl = "http://example.com";
        UUID user = UUID.randomUUID();

        // Создаём ссылку с лимитом = 1
        String shortId = shortLinkService.createShortLink(originalUrl, user, 1, 1);

        // Первый переход — успешно
        shortLinkService.getOriginalUrl(shortId);

        // Второй переход — превышение лимита, ожидаем исключение
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shortLinkService.getOriginalUrl(shortId));
        assertTrue(exception.getMessage().contains("Количество коротких ссылок превысило установленный лимит"),
                "Сообщение исключения должно содержать текст о превышении лимита.");

        // Проверяем только уведомление
        List<String> notifications = shortLinkService.getNotifications();
        System.out.println("Уведомления: " + notifications); // Для отладки
        assertFalse(notifications.isEmpty(), "Должно быть хотя бы одно уведомление.");
        assertTrue(notifications.stream().anyMatch(msg -> msg.contains("превысила лимит переходов")),
                "Одно из уведомлений должно содержать текст о превышении лимита переходов.");
    }


}
