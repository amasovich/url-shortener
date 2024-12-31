package com.beryoza.urlshortener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ShortLinkServiceTest {

    private ShortLinkService shortLinkService;
    private ShortLinkRepository shortLinkRepository;

    @BeforeEach
    public void setUp() {
        shortLinkRepository = new ShortLinkRepository();
        shortLinkService = new ShortLinkService(shortLinkRepository);
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
        assertTrue(exception.getMessage().contains("превысила лимит переходов"), "Ссылка должна блокироваться при превышении лимита переходов.");
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

        String shortId = shortLinkService.createShortLink(originalUrl, user, 1, 1); // Лимит = 1

        // Переход по ссылке (должен быть успешным)
        shortLinkService.getOriginalUrl(shortId);

        // Второй переход (должен быть заблокирован)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shortLinkService.getOriginalUrl(shortId));
        assertTrue(exception.getMessage().contains("превысила лимит переходов"), "Пользователь должен получать уведомление о превышении лимита переходов.");

        // Создаём новую ссылку с истёкшим временем
        String shortIdExpired = shortLinkService.createShortLink(originalUrl, user, 1, 10);
        ShortLink linkExpired = shortLinkRepository.findByShortId(shortIdExpired);
        linkExpired.setExpiryTime(System.currentTimeMillis() - 1000);
        shortLinkService.cleanUpExpiredLinks();

        assertNull(shortLinkRepository.findByShortId(shortIdExpired), "Ссылка должна быть удалена.");
        assertThrows(RuntimeException.class, () -> shortLinkService.getOriginalUrl(shortIdExpired), "Пользователь должен получать уведомление об истечении срока жизни ссылки.");
    }
}
