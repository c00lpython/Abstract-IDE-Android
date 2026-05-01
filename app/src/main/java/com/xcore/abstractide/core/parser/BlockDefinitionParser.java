package com.xcore.abstractide.core.parser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xcore.abstractide.core.model.BlockModel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Аналог: json_parser.py + BlockFactory._load_definitions()
 * Загружает определения блоков из CodeBlocks.json и GUIBlocks.json
 */
public class BlockDefinitionParser {

    private final Map<String, Map<String, BlockDefinition>> definitions = new LinkedHashMap<>();
    private final Gson gson = new Gson();

    public BlockDefinitionParser() {
        definitions.put("code", new LinkedHashMap<>());
        definitions.put("gui", new LinkedHashMap<>());
        loadCodeBlocks();
    }

    /**
     * Загрузить CodeBlocks.json из assets
     */
    private void loadCodeBlocks() {
        try {
            // TODO: загрузить из assets/blocks/CodeBlocks.json
            // Пока используем хардкод-заглушку
            parseJson("{}", "code");
        } catch (Exception e) {
            System.err.println("Error loading CodeBlocks.json: " + e.getMessage());
        }
    }

    /**
     * Распарсить JSON и добавить определения
     */
    @SuppressWarnings("unchecked")
    public void parseJson(String json, String category) {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> root = gson.fromJson(json, type);

        Map<String, Object> blockClasses = (Map<String, Object>) root.get("block_classes");
        if (blockClasses == null) return;

        for (Map.Entry<String, Object> classEntry : blockClasses.entrySet()) {
            String className = classEntry.getKey();
            Map<String, Object> classInfo = (Map<String, Object>) classEntry.getValue();

            Map<String, Object> subclasses = (Map<String, Object>) classInfo.get("subclasses");
            if (subclasses == null) continue;

            for (Map.Entry<String, Object> subEntry : subclasses.entrySet()) {
                String subclassName = subEntry.getKey();
                Map<String, Object> subclassInfo = (Map<String, Object>) subEntry.getValue();
                String fullName = className + "." + subclassName;

                BlockDefinition def = new BlockDefinition();
                def.fullName = fullName;
                def.className = className;
                def.subclassName = subclassName;
                def.description = String.valueOf(subclassInfo.getOrDefault("description", ""));
                def.color = String.valueOf(subclassInfo.getOrDefault("color", "#3498db"));
                def.type = String.valueOf(subclassInfo.getOrDefault("type", ""));
                def.isContainer = Boolean.TRUE.equals(subclassInfo.get("is_container"));

                // Container config
                Map<String, Object> containerConfig = (Map<String, Object>) subclassInfo.get("container_config");
                if (containerConfig != null) {
                    def.containerConfig = containerConfig;
                }

                // Properties
                Map<String, Object> props = (Map<String, Object>) subclassInfo.get("properties");
                if (props != null) {
                    for (Map.Entry<String, Object> propEntry : props.entrySet()) {
                        String propName = propEntry.getKey();
                        Map<String, Object> propInfo = (Map<String, Object>) propEntry.getValue();
                        BlockProperty prop = new BlockProperty();
                        prop.name = propName;
                        prop.type = String.valueOf(propInfo.getOrDefault("type", "string"));
                        prop.description = String.valueOf(propInfo.getOrDefault("description", ""));
                        prop.defaultValue = propInfo.get("default");
                        prop.required = Boolean.TRUE.equals(propInfo.get("required"));
                        def.properties.put(propName, prop);
                    }
                }

                // Droplist
                Map<String, Object> droplist = (Map<String, Object>) subclassInfo.get("droplist");
                if (droplist != null && Boolean.TRUE.equals(droplist.get("enabled"))) {
                    def.hasDroplist = true;
                    def.droplistOptions = (List<String>) droplist.get("options");
                    def.droplistDefault = String.valueOf(droplist.getOrDefault("default", ""));
                }

                definitions.get(category).put(fullName, def);
            }
        }
    }

    /**
     * Получить определение блока
     */
    public BlockDefinition getDefinition(String fullName, String category) {
        Map<String, BlockDefinition> catDefs = definitions.get(category);
        if (catDefs == null) catDefs = definitions.get("code");
        return catDefs != null ? catDefs.get(fullName) : null;
    }

    /**
     * Получить все определения
     */
    public List<BlockDefinition> getAllDefinitions(String category) {
        Map<String, BlockDefinition> catDefs = definitions.get(category);
        if (catDefs == null) catDefs = definitions.get("code");
        if (catDefs == null) return new ArrayList<>();
        return new ArrayList<>(catDefs.values());
    }

    /**
     * Получить все определения (code + gui)
     */
    public List<BlockDefinition> getAllDefinitions() {
        List<BlockDefinition> all = new ArrayList<>();
        for (Map<String, BlockDefinition> catDefs : definitions.values()) {
            all.addAll(catDefs.values());
        }
        return all;
    }

    /**
     * Создать блок по определению
     */
    public BlockModel createBlock(String fullName, String category) {
        BlockDefinition def = getDefinition(fullName, category);
        if (def == null) return null;

        BlockModel block = new BlockModel();
        block.setType(new BlockModel.BlockType(category, def.className, def.subclassName));
        block.setName(def.subclassName);
        block.setColor(def.color != null ? def.color : "#3498db");

        // Установить свойства по умолчанию
        for (BlockProperty prop : def.properties.values()) {
            if (prop.defaultValue != null) {
                block.getProperties().put(prop.name, prop.defaultValue);
            }
        }

        // Настроить контейнер
        if (def.isContainer) {
            block.getProperties().put("_is_container", true);
            if (def.containerConfig != null) {
                block.getProperties().put("_container_config", def.containerConfig);
            }
            block.getProperties().put("_container_items", new ArrayList<>());
        }

        // Настроить droplist
        if (def.hasDroplist) {
            block.getProperties().put("_is_droplist", true);
            block.getProperties().put("_droplist_options", def.droplistOptions);
            block.getProperties().put("_droplist_selected", def.droplistDefault != null ?
                    def.droplistDefault : (def.droplistOptions != null && !def.droplistOptions.isEmpty() ?
                    def.droplistOptions.get(0) : ""));
        }

        block.initTransients();
        return block;
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

    public static class BlockDefinition {
        public String fullName;
        public String className;
        public String subclassName;
        public String description;
        public String color;
        public String type;
        public boolean isContainer;
        public Map<String, Object> containerConfig;
        public Map<String, BlockProperty> properties = new LinkedHashMap<>();
        public boolean hasDroplist;
        public List<String> droplistOptions;
        public String droplistDefault;

        @Override
        public String toString() {
            return fullName + " (" + type + ")";
        }
    }

    public static class BlockProperty {
        public String name;
        public String type;
        public String description;
        public Object defaultValue;
        public boolean required;
    }
}