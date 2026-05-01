package com.xcore.abstractide.core.builder;

import com.xcore.abstractide.core.model.BlockModel;
import com.xcore.abstractide.core.model.Connection;
import com.xcore.abstractide.core.model.ProjectModel;
import com.xcore.abstractide.core.parser.Translator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Аналог: builderman.py
 * Сборщик кода из графа блоков с поддержкой:
 * - Обход графа в порядке выполнения
 * - Подстановка шаблонов ${variable}
 * - Контейнеры (тело циклов, условий)
 * - Ветвления (if/elif/else, switch/case)
 * - Reference и Call блоки
 */
public class CodeBuilder {

    private static final Logger LOGGER = Logger.getLogger(CodeBuilder.class.getName());

    private final Translator translator;
    private final String indent;
    private final String newline;

    // Кэш значений
    private final Map<Integer, Object> variableValues = new LinkedHashMap<>();
    private final Map<Integer, String> variableNames = new LinkedHashMap<>();

    public CodeBuilder() {
        this.translator = new Translator();
        this.indent = translator.getIndent();
        this.newline = translator.getNewline();
    }

    // ========== ОСНОВНОЙ МЕТОД ==========

    /**
     * Собрать код из проекта
     */
    public String build(ProjectModel project) {
        // Анализ переменных
        analyzeVariables(project);

        // Найти корневые блоки
        List<Integer> rootBlocks = findRootBlocks(project);

        // Порядок выполнения
        List<Integer> executionOrder = sortBlocksByExecution(project, rootBlocks);

        LOGGER.info("Execution order: " + executionOrder);

        // Генерация кода
        StringBuilder code = new StringBuilder();
        Set<Integer> generatedBlocks = new HashSet<>();

        for (int blockId : executionOrder) {
            if (generatedBlocks.contains(blockId)) continue;

            BlockModel block = project.getBlock(blockId);
            if (block == null) continue;

            String blockCode = generateBlockCode(block, project, generatedBlocks, "");
            if (blockCode != null && !blockCode.isEmpty()) {
                code.append(blockCode).append(newline);
            }
        }

        return code.toString();
    }

    /**
     * Собрать и сохранить в файл
     */
    public String buildAndSave(ProjectModel project, String outputDir) {
        String code = build(project);
        if (code != null && !code.isEmpty()) {
            saveToFile(code, outputDir, project);
        }
        return code;
    }

    // ========== АНАЛИЗ ПЕРЕМЕННЫХ ==========

    private void analyzeVariables(ProjectModel project) {
        variableValues.clear();
        variableNames.clear();

        for (BlockModel block : project.getAllBlocks()) {
            String className = block.getType().getClassName();
            String subclassName = block.getType().getSubclassName();

            if (("DataTypes".equals(className) && "Variable".equals(subclassName)) ||
                    ("Definitions".equals(className) && "Variable".equals(subclassName))) {

                String varName = block.getName();
                if (varName == null || varName.isEmpty()) {
                    varName = "var_" + block.getId();
                }
                variableNames.put(block.getId(), varName);
            }
        }
    }

    // ========== ПОРЯДОК ВЫПОЛНЕНИЯ ==========

    private List<Integer> findRootBlocks(ProjectModel project) {
        Set<Integer> allBlocks = new HashSet<>();
        for (BlockModel block : project.getAllBlocks()) {
            allBlocks.add(block.getId());
        }

        Set<Integer> blocksWithInput = new HashSet<>();
        for (Connection conn : project.getConnections()) {
            if ("input".equals(conn.getToPort())) {
                blocksWithInput.add(conn.getToBlockId());
            }
        }

        allBlocks.removeAll(blocksWithInput);
        List<Integer> roots = new ArrayList<>(allBlocks);
        Collections.sort(roots);
        return roots;
    }

    private List<Integer> sortBlocksByExecution(ProjectModel project, List<Integer> rootBlocks) {
        Set<Integer> visited = new HashSet<>();
        List<Integer> order = new ArrayList<>();

        // Построить граф зависимостей
        Map<Integer, List<Integer>> graph = new LinkedHashMap<>();
        for (BlockModel block : project.getAllBlocks()) {
            graph.put(block.getId(), new ArrayList<>());
        }

        for (Connection conn : project.getConnections()) {
            if ("input".equals(conn.getToPort())) {
                List<Integer> deps = graph.get(conn.getToBlockId());
                if (deps != null) {
                    deps.add(conn.getFromBlockId());
                }
            }
        }

        // DFS с учётом контейнеров и next_id
        for (int rootId : rootBlocks) {
            dfsExecution(rootId, graph, project, visited, order);
        }

        // Оставшиеся блоки
        for (int blockId : graph.keySet()) {
            if (!visited.contains(blockId)) {
                dfsExecution(blockId, graph, project, visited, order);
            }
        }

        return order;
    }

    private void dfsExecution(int blockId, Map<Integer, List<Integer>> graph,
                              ProjectModel project, Set<Integer> visited, List<Integer> order) {
        if (visited.contains(blockId)) return;
        visited.add(blockId);

        // Сначала зависимости
        List<Integer> deps = graph.get(blockId);
        if (deps != null) {
            for (int depId : deps) {
                dfsExecution(depId, graph, project, visited, order);
            }
        }

        order.add(blockId);

        // Следующий в цепочке
        BlockModel block = project.getBlock(blockId);
        if (block != null && block.getNextId() != null) {
            dfsExecution(block.getNextId(), graph, project, visited, order);
        }
    }

    // ========== ГЕНЕРАЦИЯ КОДА ==========

    private String generateBlockCode(BlockModel block, ProjectModel project,
                                     Set<Integer> generatedBlocks, String baseIndent) {
        int blockId = block.getId();
        String className = block.getType().getClassName();
        String subclassName = block.getType().getSubclassName();
        String fullName = className + "." + subclassName;

        LOGGER.fine("Processing block " + blockId + ": " + fullName);

        // ===== СПЕЦИАЛЬНЫЕ БЛОКИ =====

        // Variable
        if (("DataTypes".equals(className) && "Variable".equals(subclassName)) ||
                ("Definitions".equals(className) && "Variable".equals(subclassName))) {
            return generateVariable(block, project, generatedBlocks, baseIndent);
        }

        // Value
        if ("DataTypes".equals(className) && "Value".equals(subclassName)) {
            return generateValue(block);
        }

        // Operator
        if ("Operators".equals(className)) {
            return generateOperator(block, project, baseIndent);
        }

        // Function definition
        if ("Functions".equals(className) && "Function".equals(subclassName)) {
            return generateFunction(block, project, generatedBlocks, baseIndent);
        }

        // If block
        if ("ControlFlow".equals(className) && "If".equals(subclassName)) {
            return generateIfBlock(block, project, generatedBlocks, baseIndent);
        }

        // Switch block
        if ("ControlFlow".equals(className) && "Switch".equals(subclassName)) {
            return generateSwitchBlock(block, project, generatedBlocks, baseIndent);
        }

        // Print / Builtins
        if ("Builtins".equals(className)) {
            return generateBuiltin(block, project, baseIndent);
        }

        // Call block
        if (block.isCallBlock()) {
            return generateCallBlock(block, project, baseIndent);
        }

        // ===== ОБЩИЙ СЛУЧАЙ: поиск в translator =====

        String template = translator.getTemplate(fullName);
        if (template == null) {
            LOGGER.warning("No translation rule for " + fullName);
            return "# Unknown block: " + fullName;
        }

        // Подстановка свойств
        String code = template;
        Map<String, Object> props = block.getProperties();
        if (props != null) {
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                if (!entry.getKey().startsWith("_")) {
                    code = code.replace("${" + entry.getKey() + "}",
                            String.valueOf(entry.getValue()));
                }
            }
        }

        // Обработка контейнеров
        if (block.isContainerBlock()) {
            StringBuilder body = new StringBuilder();
            for (int childId : block.getContainerItems()) {
                BlockModel child = project.getBlock(childId);
                if (child != null) {
                    String childCode = generateBlockCode(child, project, generatedBlocks,
                            baseIndent + indent);
                    if (childCode != null) {
                        body.append(childCode).append(newline);
                    }
                }
            }
            code = code.replace("${body}", body.toString().trim());
        }

        generatedBlocks.add(blockId);
        return baseIndent + code;
    }

    // ========== ГЕНЕРАТОРЫ ДЛЯ КОНКРЕТНЫХ ТИПОВ ==========

    private String generateVariable(BlockModel block, ProjectModel project,
                                    Set<Integer> generatedBlocks, String baseIndent) {
        String varName = variableNames.getOrDefault(block.getId(), block.getName());
        String value = getInputValue(block.getId(), project);

        if (value != null) {
            generatedBlocks.add(block.getId());
            return baseIndent + varName + " = " + value;
        }

        generatedBlocks.add(block.getId());
        return baseIndent + varName + " = None";
    }

    private String generateValue(BlockModel block) {
        Object value = block.getProperties().get("value");
        if (value instanceof String) return "'" + value + "'";
        if (value instanceof Boolean) return (Boolean) value ? "True" : "False";
        return value != null ? value.toString() : "None";
    }

    private String generateOperator(BlockModel block, ProjectModel project, String baseIndent) {
        String operator = String.valueOf(block.getProperties().getOrDefault("operator", "+"));

        // Собрать операнды из контейнера
        List<String> operands = new ArrayList<>();
        for (int childId : block.getContainerItems()) {
            BlockModel child = project.getBlock(childId);
            if (child != null) {
                String val = getBlockValue(child.getId(), project);
                if (val != null) operands.add(val);
            }
        }

        if (operands.size() >= 2) {
            StringBuilder expr = new StringBuilder();
            for (int i = 0; i < operands.size(); i++) {
                if (i > 0) expr.append(" ").append(operator).append(" ");
                expr.append(operands.get(i));
            }
            return baseIndent + expr.toString();
        }

        return baseIndent + operands.get(0);
    }

    private String generateFunction(BlockModel block, ProjectModel project,
                                    Set<Integer> generatedBlocks, String baseIndent) {
        String funcName = block.getName();
        if (funcName.startsWith("new_")) funcName = funcName.substring(4);

        StringBuilder params = new StringBuilder();
        StringBuilder body = new StringBuilder();

        for (int childId : block.getContainerItems()) {
            BlockModel child = project.getBlock(childId);
            if (child != null && child.isContainerBlock()) {
                // Параметры функции
                for (int paramId : child.getContainerItems()) {
                    BlockModel param = project.getBlock(paramId);
                    if (param != null) {
                        if (params.length() > 0) params.append(", ");
                        params.append(param.getName());
                    }
                }
            } else if (child != null && !child.isContainerBlock()) {
                // Тело функции
                String childCode = generateBlockCode(child, project, generatedBlocks,
                        indent);
                if (childCode != null) body.append(childCode).append(newline);
            }
        }

        StringBuilder code = new StringBuilder();
        code.append("def ").append(funcName).append("(").append(params).append("):").append(newline);
        code.append(body);
        code.append(newline);

        generatedBlocks.add(block.getId());
        return code.toString();
    }

    private String generateIfBlock(BlockModel block, ProjectModel project,
                                   Set<Integer> generatedBlocks, String baseIndent) {
        StringBuilder code = new StringBuilder();

        // Условие
        String condition = getBlockValue(block.getConditionId(), project);
        if (condition == null) condition = "True";

        code.append(baseIndent).append("if ").append(condition).append(":").append(newline);

        // True branch
        for (int childId : block.getTrueBranch()) {
            BlockModel child = project.getBlock(childId);
            if (child != null) {
                String childCode = generateBlockCode(child, project, generatedBlocks,
                        baseIndent + indent);
                if (childCode != null) code.append(childCode).append(newline);
            }
        }

        // False branch (else)
        if (!block.getFalseBranch().isEmpty()) {
            code.append(baseIndent).append("else:").append(newline);
            for (int childId : block.getFalseBranch()) {
                BlockModel child = project.getBlock(childId);
                if (child != null) {
                    String childCode = generateBlockCode(child, project, generatedBlocks,
                            baseIndent + indent);
                    if (childCode != null) code.append(childCode).append(newline);
                }
            }
        }

        generatedBlocks.add(block.getId());
        return code.toString();
    }

    private String generateSwitchBlock(BlockModel block, ProjectModel project,
                                       Set<Integer> generatedBlocks, String baseIndent) {
        StringBuilder code = new StringBuilder();
        String switchVar = getBlockValue(block.getConditionId(), project);
        if (switchVar == null) switchVar = "value";

        List<BlockModel.CaseData> cases = block.getCases();
        for (int i = 0; i < cases.size(); i++) {
            BlockModel.CaseData caseData = cases.get(i);
            String caseValue = caseData.value != null ? String.valueOf(caseData.value) : String.valueOf(i);

            if (i == 0) {
                code.append(baseIndent).append("if ").append(switchVar).append(" == ").append(caseValue).append(":").append(newline);
            } else {
                code.append(baseIndent).append("elif ").append(switchVar).append(" == ").append(caseValue).append(":").append(newline);
            }

            for (int childId : caseData.blocks) {
                BlockModel child = project.getBlock(childId);
                if (child != null) {
                    String childCode = generateBlockCode(child, project, generatedBlocks,
                            baseIndent + indent);
                    if (childCode != null) code.append(childCode).append(newline);
                }
            }
        }

        if (!block.getDefaultCase().isEmpty()) {
            code.append(baseIndent).append("else:").append(newline);
            for (int childId : block.getDefaultCase()) {
                BlockModel child = project.getBlock(childId);
                if (child != null) {
                    String childCode = generateBlockCode(child, project, generatedBlocks,
                            baseIndent + indent);
                    if (childCode != null) code.append(childCode).append(newline);
                }
            }
        }

        generatedBlocks.add(block.getId());
        return code.toString();
    }

    private String generateBuiltin(BlockModel block, ProjectModel project, String baseIndent) {
        String subclassName = block.getType().getSubclassName();

        if ("Print".equals(subclassName)) {
            String values = getPrintValues(block.getId(), project);
            String sep = String.valueOf(block.getProperties().getOrDefault("sep", "' '"));
            String end = String.valueOf(block.getProperties().getOrDefault("end", "'\\n'"));
            return baseIndent + "print(" + values + ", sep=" + sep + ", end=" + end + ")";
        }

        if ("Input".equals(subclassName)) {
            String prompt = String.valueOf(block.getProperties().getOrDefault("prompt", ""));
            String target = findTargetVariable(block.getId(), project);
            if (target != null) {
                return baseIndent + target + " = input(" + prompt + ")";
            }
            return baseIndent + "input(" + prompt + ")";
        }

        return baseIndent + subclassName.toLowerCase() + "()";
    }

    private String generateCallBlock(BlockModel block, ProjectModel project, String baseIndent) {
        String funcName = block.getSourceBlockName();
        if (funcName != null && funcName.startsWith("new_")) funcName = funcName.substring(4);

        StringBuilder args = new StringBuilder();
        // TODO: собрать аргументы из argument containers

        return baseIndent + funcName + "(" + args + ")";
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private String getBlockValue(int blockId, ProjectModel project) {
        BlockModel block = project.getBlock(blockId);
        if (block == null) return null;

        String className = block.getType().getClassName();
        String subclassName = block.getType().getSubclassName();

        if (("DataTypes".equals(className) && "Variable".equals(subclassName)) ||
                ("Definitions".equals(className) && "Variable".equals(subclassName))) {
            return variableNames.getOrDefault(blockId, block.getName());
        }

        if ("DataTypes".equals(className) && "Value".equals(subclassName)) {
            return generateValue(block);
        }

        if ("Operators".equals(className)) {
            return generateOperator(block, project, "");
        }

        if (block.isCallBlock()) {
            return generateCallBlock(block, project, "").trim();
        }

        return block.getName();
    }

    private String getInputValue(int blockId, ProjectModel project) {
        for (Connection conn : project.getConnections()) {
            if (conn.getToBlockId() == blockId && "input".equals(conn.getToPort())) {
                return getBlockValue(conn.getFromBlockId(), project);
            }
        }
        return null;
    }

    private String findTargetVariable(int blockId, ProjectModel project) {
        for (Connection conn : project.getConnections()) {
            if (conn.getFromBlockId() == blockId && "output".equals(conn.getFromPort())) {
                int toId = conn.getToBlockId();
                BlockModel toBlock = project.getBlock(toId);
                if (toBlock != null) {
                    String cn = toBlock.getType().getClassName();
                    String sn = toBlock.getType().getSubclassName();
                    if (("DataTypes".equals(cn) && "Variable".equals(sn)) ||
                            ("Definitions".equals(cn) && "Variable".equals(sn))) {
                        String varName = toBlock.getName();
                        variableNames.put(toId, varName);
                        return varName;
                    }
                }
            }
        }
        return null;
    }

    private String getPrintValues(int blockId, ProjectModel project) {
        List<String> values = new ArrayList<>();
        for (Connection conn : project.getConnections()) {
            if (conn.getToBlockId() == blockId && "input".equals(conn.getToPort())) {
                String val = getBlockValue(conn.getFromBlockId(), project);
                if (val != null) values.add(val);
            }
        }
        return String.join(", ", values);
    }

    // ========== СОХРАНЕНИЕ В ФАЙЛ ==========

    private void saveToFile(String code, String outputDir, ProjectModel project) {
        try {
            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();

            String projectName = project.getName().replace(" ", "_");
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = projectName + "_" + timestamp + ".py";
            File file = new File(dir, filename);

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(code);
            }

            LOGGER.info("Code saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.severe("Error saving file: " + e.getMessage());
        }
    }
}