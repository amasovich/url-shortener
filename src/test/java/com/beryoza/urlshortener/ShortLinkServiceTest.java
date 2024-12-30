package com.beryoza.urlshortener;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class ShortLinkServiceTest {

    private ShortLinkRepository shortLinkRepository;
    private ShortLinkService shortLinkService;

    private UserRepository userRepository;
    private UserService userService;



    @Before
    public void setup() {
        System.setProperty("env", "test");
        shortLinkRepository = new ShortLinkRepository();
        shortLinkService = new ShortLinkService(shortLinkRepository);

        userRepository = new UserRepository();
        userService = new UserService(userRepository);
    }

    @Test
    public void testUniqueShortLinksForDifferentUsers() {
        User user1 = userService.createUser("Alice");
        User user2 = userService.createUser("Bob");

        String url = "https://example.com";

        String shortLink1 = shortLinkService.createShortLink(url, user1.getUserUuid(), 24, 10);
        String shortLink2 = shortLinkService.createShortLink(url, user2.getUserUuid(), 24, 10);

        assertNotEquals("Ссылки для разных пользователей должны быть уникальными", shortLink1, shortLink2);
    }

    @Test
    public void testLimitExceeded() {
        User user = userService.createUser("Charlie");

        String url = "https://example.com";
        String shortLink = shortLinkService.createShortLink(url, user.getUserUuid(), 24, 1);

        // Первый переход должен сработать
        ShortLink resolvedLink = shortLinkService.getOriginalUrl(shortLink);
        assertEquals("URL должен соответствовать оригиналу", url, resolvedLink.getOriginalUrl());

        // Второй переход должен вызывать исключение
        try {
            shortLinkService.getOriginalUrl(shortLink);
            fail("Ожидалось исключение при превышении лимита переходов");
        } catch (RuntimeException e) {
            assertTrue(
                    "Исключение должно содержать текст про превышение лимита",
                    e.getMessage().contains("лимит переходов")
            );
        }
    }

    @Test
    public void testExpiredLinks() {
        User user = userService.createUser("Diana");

        String url = "https://example.com";
        // Создаём ссылку с минимально допустимым TTL (например, 1 мс)
        String shortLink = shortLinkService.createShortLink(url, user.getUserUuid(), 1, 10);

        try {
            Thread.sleep(5); // Ждём, чтобы ссылка гарантированно истекла
            shortLinkService.getOriginalUrl(shortLink);
            fail("Ожидалось исключение при истечении срока действия ссылки");
        } catch (RuntimeException e) {
            assertTrue(
                    "Исключение должно содержать текст про истечение срока",
                    e.getMessage().contains("истёк")
            );
        } catch (InterruptedException e) {
            fail("Тест был прерван во время ожидания истечения ссылки");
        }
    }

    @Test
    public void testUserNotificationOnUnavailableLink() {
        User user = userService.createUser("Eve");

        String url = "https://example.com";
        String shortLink = shortLinkService.createShortLink(url, user.getUserUuid(), 24, 1);

        // Первый переход допустим
        shortLinkService.getOriginalUrl(shortLink);

        // Второй переход должен быть заблокирован
        try {
            shortLinkService.getOriginalUrl(shortLink);
            fail("Ожидалось исключение при недоступности ссылки");
        } catch (RuntimeException e) {
            assertTrue(
                    "Исключение должно содержать текст про превышение лимита",
                    e.getMessage().contains("лимит переходов")
            );
        }
    }
}
