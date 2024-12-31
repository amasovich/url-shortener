package com.beryoza.urlshortener;

import java.util.UUID;

/**
 * Этот класс описывает короткую ссылку, созданную в рамках нашего сервиса.
 * <p>
 * Поля:
 *  - shortId : укорачиваемый идентификатор (строка, например "abc123").
 *  - originalUrl : исходный (длинный) URL, на который будет идти перенаправление.
 *  - createdAt : момент создания (в миллисекундах с 1970-го года).
 *  - expiryTime : момент, когда ссылка "протухнет" (тоже в мс). После этого времени она становится недоступной.
 *  - limit : лимит переходов (сколько раз по ней можно перейти).
 *  - currentCount : сколько раз уже перешли по ссылке. Если >= limit, ссылка блокируется.
 *  - userUuid : владелец (пользователь, которому принадлежит ссылка), идентифицируемый по UUID.
 */
public class ShortLink {

    /** Короткий идентификатор ссылки (пример: "abc123"). */
    private String shortId;

    /** Исходный (длинный) URL, куда перенаправляет короткая ссылка. */
    private String originalUrl;

    /** Время создания ссылки (System.currentTimeMillis()), чтобы понимать, когда она появилась. */
    private long createdAt;

    /** Время, когда срок действия ссылки истечёт. После этого она недоступна. */
    private long expiryTime;

    /** Лимит переходов: если достигнут, ссылка блокируется. */
    private int limit;

    /** Текущее число переходов — увеличиваем, когда пользователь переходит по ссылке. */
    private int currentCount;

    /** UUID владельца (пользователя), создавшего ссылку. */
    private UUID userUuid;

    /**
     * Пустой конструктор - на всякий случай, если понадобится
     * безаргументное создание (пример: сериализация).
     */
    public ShortLink() {
    }

    /**
     * Основной конструктор, задающий все поля.
     *
     * @param shortId      короткий идентификатор
     * @param originalUrl  оригинальный URL
     * @param createdAt    время создания (в мс)
     * @param expiryTime   время истечения срока (в мс)
     * @param limit        максимально допустимое число переходов
     * @param currentCount текущий счётчик переходов
     * @param userUuid     владелец ссылки (UUID)
     */
    public ShortLink(String shortId,
                     String originalUrl,
                     long createdAt,
                     long expiryTime,
                     int limit,
                     int currentCount,
                     UUID userUuid) {
        this.shortId = shortId;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiryTime = expiryTime;
        this.limit = limit;
        this.currentCount = currentCount;
        this.userUuid = userUuid;
    }

    /** Возвращает короткий идентификатор (пример: "abc123"). */
    public String getShortId() {
        return shortId;
    }

    /** Устанавливает короткий идентификатор. */
    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    /** Возвращает исходный (длинный) URL. */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /** Устанавливает исходный (длинный) URL. */
    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    /**
     * Время создания (в мс). System.currentTimeMillis() на момент создания.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Устанавливает время создания.
     * Можно задавать вручную, если восстанавливаем ссылку из БД, или при тестах.
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Время (в мс), когда ссылка перестаёт быть активной.
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    /** Устанавливает время истечения. */
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
     * Лимит переходов. Если currentCount >= limit,
     * считаем, что ссылка более недоступна.
     */
    public int getLimit() {
        return limit;
    }

    /** Устанавливает лимит переходов. */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /** Текущее количество переходов, постепенно увеличивается при каждом клике. */
    public int getCurrentCount() {
        return currentCount;
    }

    /** Устанавливает счётчик переходов (или увеличивает). */
    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    /** UUID пользователя, которому принадлежит ссылка. */
    public UUID getUserUuid() {
        return userUuid;
    }

    /**
     * Устанавливает UUID владельца.
     * Менять обычно не нужно,
     * но может пригодиться, если переносим ссылку другому пользователю (по идее, не по ТЗ).
     */
    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }

    /**
     * Удобный вывод для отладки или логов.
     */
    @Override
    public String toString() {
        return "ShortLink{" +
                "shortId='" + shortId + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", createdAt=" + createdAt +
                ", expiryTime=" + expiryTime +
                ", limit=" + limit +
                ", currentCount=" + currentCount +
                ", userUuid=" + userUuid +
                '}';
    }
}
