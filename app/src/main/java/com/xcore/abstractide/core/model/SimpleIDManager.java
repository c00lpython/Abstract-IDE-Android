package com.xcore.abstractide.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Аналог: models.py — SimpleIDManager
 * Менеджер простых числовых ID (1, 2, 3...)
 * Поддерживает переиспользование освобождённых ID
 */
public class SimpleIDManager {

    private int maxId = 0;
    private final List<Integer> freeIds = new ArrayList<>();

    /**
     * Получить следующий свободный ID
     */
    public int getNextId() {
        if (!freeIds.isEmpty()) {
            return freeIds.remove(0);
        }
        maxId++;
        return maxId;
    }

    /**
     * Освободить ID для переиспользования
     */
    public void releaseId(int blockId) {
        if (blockId > 0 && !freeIds.contains(blockId)) {
            freeIds.add(blockId);
            Collections.sort(freeIds);
        }
    }

    /**
     * Зарегистрировать существующий ID (при загрузке)
     */
    public void registerId(int blockId) {
        if (blockId > maxId) {
            maxId = blockId;
        }
        freeIds.remove((Integer) blockId);
    }

    /**
     * Сбросить менеджер
     */
    public void reset() {
        maxId = 0;
        freeIds.clear();
    }

    public int getMaxId() { return maxId; }
    public List<Integer> getFreeIds() { return new ArrayList<>(freeIds); }
}