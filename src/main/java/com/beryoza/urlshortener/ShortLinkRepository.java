package com.beryoza.urlshortener;

import java.util.*;

/**
 * Репозиторий для хранения коротких ссылок в памяти (in-memory).
 * Ключом будет shortId (String), а значением — объект ShortLink.
 */
public class ShortLinkRepository {

    /**
     * Хранилище коротких ссылок. Ключ — это shortId (String),
     * значение — объект ShortLink.
     */
    private final Map<String, ShortLink> storage = new HashMap<>();

    /**
     * Сохраняем/обновляем короткую ссылку в хранилище.
     *
     * @param link объект ShortLink, где shortId должен быть уникальным
     * @return тот же link (просто возвращаем для удобства)
     */
    public ShortLink save(ShortLink link) {
        storage.put(link.getShortId(), link);
        return link;
    }

    /**
     * Ищем ShortLink по его короткому идентификатору (shortId).
     *
     * @param shortId короткий идентификатор (например, "abc123")
     * @return ShortLink или null, если не найден
     */
    public ShortLink findByShortId(String shortId) {
        return storage.get(shortId);
    }

    /**
     * Удаляем ссылку из хранилища по shortId.
     * Если нет такой — ничего не произойдет.
     *
     * @param shortId короткий идентификатор ссылки
     */
    public void deleteByShortId(String shortId) {
        storage.remove(shortId);
    }

    /**
     * Возвращает все ссылки для отладки или статистики.
     *
     * @return коллекция ShortLink из внутреннего хранилища
     */
    public Collection<ShortLink> findAll() {
        return storage.values();
    }
}
