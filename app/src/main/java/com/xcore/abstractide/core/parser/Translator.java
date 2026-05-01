package com.xcore.abstractide.core.parser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Аналог: translator.json
 * Правила трансляции блоков в код
 */
public class Translator {

    private final Map<String, String> blockTemplates = new LinkedHashMap<>();
    private final String indent = "    ";
    private final String newline = "\n";

    public Translator() {
        blockTemplates.put("Builtins.Print", "print(${values})");
        blockTemplates.put("Builtins.Input", "${target} = input(${prompt})");
        blockTemplates.put("DataTypes.String", "''");
        blockTemplates.put("DataTypes.Integer", "0");
        blockTemplates.put("Reference", "${name}");
    }

    public String getTemplate(String fullName) {
        return blockTemplates.get(fullName);
    }

    public String getIndent() { return indent; }
    public String getNewline() { return newline; }
}