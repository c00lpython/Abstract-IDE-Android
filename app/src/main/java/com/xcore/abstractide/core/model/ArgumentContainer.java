package com.xcore.abstractide.core.model;

import java.util.*;
import java.util.UUID;

/**
 * Аналог: models.py — ArgumentContainer
 * Контейнер для аргументов блока (множественные значения, как args в функциях)
 */
public class ArgumentContainer {

    private final BlockModel parentBlock;
    private final String containerName;
    private Map<String, Object> config;
    private List<ArgumentItem> items = new ArrayList<>();

    public ArgumentContainer(BlockModel parentBlock, String containerName) {
        this(parentBlock, containerName, null);
    }

    public ArgumentContainer(BlockModel parentBlock, String containerName, Map<String, Object> config) {
        this.parentBlock = parentBlock;
        this.containerName = containerName;
        this.config = config != null ? config : new LinkedHashMap<>();

        // Восстановить из свойств блока
        Object saved = parentBlock.getProperties().get(containerName);
        if (saved instanceof List) {
            for (Object item : (List<?>) saved) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    items.add(ArgumentItem.fromMap(map));
                }
            }
        } else if (this.config.containsKey("items") && this.config.get("items") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> savedItems = (List<Map<String, Object>>) this.config.get("items");
            for (Map<String, Object> map : savedItems) {
                items.add(ArgumentItem.fromMap(map));
            }
        }

        reindex();
    }

    // ======== CRUD ========

    public ArgumentItem addArgument(Integer sourceBlockId, Object value, String valueType) {
        String argId = "arg_" + parentBlock.getId() + "_" + items.size() + "_" +
                UUID.randomUUID().toString().substring(0, 4);

        String displayName;
        if ("variable".equals(valueType) && sourceBlockId != null) {
            displayName = "var_" + sourceBlockId;
        } else if ("value".equals(valueType)) {
            displayName = value != null ? value.toString() : "None";
        } else {
            displayName = "expr";
        }

        ArgumentItem arg = new ArgumentItem();
        arg.id = argId;
        arg.type = valueType;
        arg.sourceBlockId = sourceBlockId;
        arg.value = value;
        arg.displayName = displayName;
        arg.index = items.size();
        arg.createdAt = java.time.LocalDateTime.now().toString();

        items.add(arg);
        saveToBlock();
        return arg;
    }

    public boolean removeArgument(String argId) {
        boolean removed = items.removeIf(item -> item.id.equals(argId));
        if (removed) {
            reindex();
            saveToBlock();
        }
        return removed;
    }

    public boolean reorderArguments(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < items.size() && toIndex >= 0 && toIndex < items.size()) {
            ArgumentItem item = items.remove(fromIndex);
            items.add(toIndex, item);
            reindex();
            saveToBlock();
            return true;
        }
        return false;
    }

    public boolean updateArgument(String argId, Map<String, Object> updates) {
        for (ArgumentItem item : items) {
            if (item.id.equals(argId)) {
                if (updates.containsKey("source_block_id")) {
                    item.sourceBlockId = toInt(updates.get("source_block_id"));
                    item.displayName = "var_" + item.sourceBlockId;
                }
                if (updates.containsKey("value")) {
                    item.value = updates.get("value");
                }
                if (updates.containsKey("type")) {
                    item.type = String.valueOf(updates.get("type"));
                }
                saveToBlock();
                return true;
            }
        }
        return false;
    }

    public ArgumentItem getArgumentBySource(int sourceBlockId) {
        for (ArgumentItem item : items) {
            if (item.sourceBlockId != null && item.sourceBlockId == sourceBlockId
                    && "variable".equals(item.type)) {
                return item;
            }
        }
        return null;
    }

    public List<Integer> getConnectedBlocks() {
        List<Integer> result = new ArrayList<>();
        for (ArgumentItem item : items) {
            if ("variable".equals(item.type) && item.sourceBlockId != null) {
                result.add(item.sourceBlockId);
            }
        }
        return result;
    }

    // ======== СЕРИАЛИЗАЦИЯ ========

    public List<Map<String, Object>> toList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ArgumentItem item : items) {
            result.add(item.toMap());
        }
        return result;
    }

    private void saveToBlock() {
        parentBlock.getProperties().put(containerName, toList());
        parentBlock.touch();
    }

    private void reindex() {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).index = i;
        }
    }

    // ======== УТИЛИТЫ ========

    public int size() { return items.size(); }
    public ArgumentItem get(int index) { return items.get(index); }
    public List<ArgumentItem> getItems() { return items; }
    public boolean isEmpty() { return items.isEmpty(); }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (NumberFormatException e) { }
        }
        return -1;
    }

    // ======== ВЛОЖЕННЫЙ КЛАСС ========

    /**
     * Один элемент аргумента
     */
    public static class ArgumentItem {
        public String id;
        public String type;          // "variable", "value", "expression"
        public Integer sourceBlockId;
        public Object value;
        public String displayName;
        public int index;
        public String createdAt;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("type", type);
            map.put("source_block_id", sourceBlockId);
            map.put("value", value);
            map.put("display_name", displayName);
            map.put("index", index);
            map.put("created_at", createdAt);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static ArgumentItem fromMap(Map<String, Object> map) {
            ArgumentItem item = new ArgumentItem();
            item.id = String.valueOf(map.getOrDefault("id", ""));
            item.type = String.valueOf(map.getOrDefault("type", "variable"));
            Object sbi = map.get("source_block_id");
            item.sourceBlockId = sbi instanceof Number ? ((Number) sbi).intValue() : null;
            item.value = map.get("value");
            item.displayName = String.valueOf(map.getOrDefault("display_name", ""));
            Object idx = map.get("index");
            item.index = idx instanceof Number ? ((Number) idx).intValue() : 0;
            item.createdAt = String.valueOf(map.getOrDefault("created_at", ""));
            return item;
        }
    }
}