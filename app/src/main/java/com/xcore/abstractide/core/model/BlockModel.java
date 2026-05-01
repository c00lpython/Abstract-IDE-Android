package com.xcore.abstractide.core.model;

import com.google.gson.annotations.SerializedName;
import java.util.*;

/**
 * Аналог: models.py — класс Block.
 * Поддерживает: контейнеры, ветвления, droplist, reference, call blocks,
 * словари, argument containers, condition chains, switch/case.
 */
public class BlockModel {

    // ========== ОСНОВНЫЕ ПОЛЯ ==========

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private BlockType type;

    @SerializedName("position")
    private Map<String, Double> position;

    @SerializedName("size")
    private Map<String, Double> size;

    @SerializedName("color")
    private String color;

    // ========== СВЯЗИ (ГРАФ) ==========

    @SerializedName("parent_id")
    private Integer parentId;

    @SerializedName("children_ids")
    private List<Integer> childrenIds = new ArrayList<>();

    @SerializedName("links_in")
    private List<Integer> linksIn = new ArrayList<>();

    @SerializedName("links_out")
    private List<Integer> linksOut = new ArrayList<>();

    @SerializedName("next_id")
    private Integer nextId;

    @SerializedName("previous_id")
    private Integer previousId;

    // ========== СВОЙСТВА ==========

    @SerializedName("properties")
    private Map<String, Object> properties = new LinkedHashMap<>();

    // ========== ВРЕМЕННЫЕ МЕТКИ ==========

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("modified_at")
    private String modifiedAt;

    // ========== ТРАНЗИЕНТНЫЕ ПОЛЯ (не сериализуются) ==========

    private transient boolean isContainer;
    private transient Map<String, Object> containerConfig = new HashMap<>();
    private transient List<Integer> containerItems = new ArrayList<>();

    private transient boolean isDroplist;
    private transient List<String> droplistOptions = new ArrayList<>();
    private transient String droplistSelected;

    private transient boolean isReference;
    private transient Integer sourceId;
    private transient String sourceType;

    private transient boolean isCallBlock;
    private transient Integer sourceBlockId;
    private transient String sourceBlockName;
    private transient String sourceBlockType;

    private transient boolean isBranching;
    private transient boolean isSwitch;
    private transient String branchType;
    private transient Integer conditionId;
    private transient List<Integer> trueBranch = new ArrayList<>();
    private transient List<Integer> falseBranch = new ArrayList<>();
    private transient List<CaseData> cases = new ArrayList<>();
    private transient List<Integer> defaultCase = new ArrayList<>();

    private transient List<int[]> dictPairs = new ArrayList<>();

    private transient String containerType;

    private transient Map<String, ArgumentContainer> argumentContainers = new LinkedHashMap<>();

    // ========== КОНСТРУКТОРЫ ==========

    public BlockModel() {
        this.position = new LinkedHashMap<>();
        this.position.put("x", 0.0);
        this.position.put("y", 0.0);
        this.size = new LinkedHashMap<>();
        this.size.put("width", 150.0);
        this.size.put("height", 80.0);
        this.color = "#3498db";
        this.createdAt = java.time.LocalDateTime.now().toString();
        this.modifiedAt = this.createdAt;
    }

    // ========== ИНИЦИАЛИЗАЦИЯ ПОСЛЕ GSON ==========

    public void initTransients() {
        if (properties == null) properties = new LinkedHashMap<>();

        this.isContainer = getBoolProp("_is_container");
        this.containerConfig = getMapProp("_container_config");
        this.containerItems = getIntListProp("_container_items");
        this.isDroplist = getBoolProp("_is_droplist");
        this.droplistOptions = getStringListProp("_droplist_options");
        this.droplistSelected = getStringProp("_droplist_selected");
        this.isReference = getBoolProp("_is_reference");
        this.sourceId = getIntProp("_source_id");
        this.sourceType = getStringProp("_source_type");
        this.isCallBlock = getBoolProp("_is_call_block");
        this.sourceBlockId = getIntProp("_source_block_id");
        this.sourceBlockName = getStringProp("_source_block_name");
        this.sourceBlockType = getStringProp("_source_block_type");
        this.isBranching = getBoolProp("_is_branching");
        this.isSwitch = getBoolProp("_is_switch");
        this.branchType = getStringProp("_branch_type");
        this.conditionId = getIntProp("condition_id");
        this.trueBranch = getIntListProp("true_branch");
        this.falseBranch = getIntListProp("false_branch");
        this.defaultCase = getIntListProp("default");
        this.containerType = containerConfig.containsKey("container_type") ?
                String.valueOf(containerConfig.get("container_type")) : null;

        // Восстановление cases
        this.cases = new ArrayList<>();
        Object casesObj = properties.get("cases");
        if (casesObj instanceof List) {
            for (Object item : (List<?>) casesObj) {
                if (item instanceof Map) {
                    CaseData cd = new CaseData();
                    cd.valueId = toInt(((Map<?, ?>) item).get("value_id"));
                    cd.value = ((Map<?, ?>) item).get("value");
                    Object blocksObj = ((Map<?, ?>) item).get("blocks");
                    if (blocksObj instanceof List) {
                        for (Object b : (List<?>) blocksObj) {
                            cd.blocks.add(toInt(b));
                        }
                    }
                    cases.add(cd);
                }
            }
        }

        // Восстановление dict_pairs
        this.dictPairs = new ArrayList<>();
        Object pairsObj = properties.get("_dict_pairs");
        if (pairsObj instanceof List) {
            for (Object item : (List<?>) pairsObj) {
                if (item instanceof List && ((List<?>) item).size() == 2) {
                    int key = toInt(((List<?>) item).get(0));
                    int val = toInt(((List<?>) item).get(1));
                    dictPairs.add(new int[]{key, val});
                }
            }
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ PROPERTIES ==========

    private boolean getBoolProp(String key) {
        Object val = properties.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return "true".equalsIgnoreCase((String) val);
        return false;
    }

    private String getStringProp(String key) {
        Object val = properties.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private Integer getIntProp(String key) {
        Object val = properties.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapProp(String key) {
        Object val = properties.get(key);
        if (val instanceof Map) return (Map<String, Object>) val;
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getIntListProp(String key) {
        List<Integer> result = new ArrayList<>();
        Object val = properties.get(key);
        if (val instanceof List) {
            for (Object item : (List<?>) val) {
                result.add(toInt(item));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringListProp(String key) {
        List<String> result = new ArrayList<>();
        Object val = properties.get(key);
        if (val instanceof List) {
            for (Object item : (List<?>) val) {
                if (item != null) result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (NumberFormatException e) { }
        }
        return -1;
    }

    // ========== ГЕТТЕРЫ / СЕТТЕРЫ ==========

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BlockType getType() { return type; }
    public void setType(BlockType type) { this.type = type; }

    public Map<String, Double> getPosition() { return position; }
    public void setPosition(Map<String, Double> position) { this.position = position; }

    public Map<String, Double> getSize() { return size; }
    public void setSize(Map<String, Double> size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public List<Integer> getChildrenIds() { return childrenIds; }
    public void setChildrenIds(List<Integer> childrenIds) { this.childrenIds = childrenIds; }

    public List<Integer> getLinksIn() { return linksIn; }
    public void setLinksIn(List<Integer> linksIn) { this.linksIn = linksIn; }

    public List<Integer> getLinksOut() { return linksOut; }
    public void setLinksOut(List<Integer> linksOut) { this.linksOut = linksOut; }

    public Integer getNextId() { return nextId; }
    public void setNextId(Integer nextId) { this.nextId = nextId; }

    public Integer getPreviousId() { return previousId; }
    public void setPreviousId(Integer previousId) { this.previousId = previousId; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }

    // ========== КОНТЕЙНЕРЫ ==========

    public boolean isContainerBlock() { return isContainer; }
    public Map<String, Object> getContainerConfig() { return containerConfig; }
    public List<Integer> getContainerItems() { return containerItems; }
    public String getContainerType() { return containerType != null ? containerType : "generic"; }

    public void addToContainer(int blockId) {
        if (!containerItems.contains(blockId)) {
            containerItems.add(blockId);
            properties.put("_container_items", containerItems);
            touch();
        }
    }

    public boolean removeFromContainer(int blockId) {
        if (containerItems.remove((Integer) blockId)) {
            properties.put("_container_items", containerItems);
            touch();
            return true;
        }
        return false;
    }

    // ========== ВЕТВЛЕНИЯ ==========

    public boolean isBranchingBlock() { return isBranching; }
    public boolean isSwitchBlock() { return isSwitch; }
    public String getBranchType() { return branchType; }
    public Integer getConditionId() { return conditionId; }
    public List<Integer> getTrueBranch() { return trueBranch; }
    public List<Integer> getFalseBranch() { return falseBranch; }
    public List<CaseData> getCases() { return cases; }
    public List<Integer> getDefaultCase() { return defaultCase; }

    public void setConditionId(int conditionId) {
        this.conditionId = conditionId;
        properties.put("condition_id", conditionId);
        touch();
    }

    public void addToTrueBranch(int blockId) {
        if (!trueBranch.contains(blockId)) {
            trueBranch.add(blockId);
            properties.put("true_branch", trueBranch);
            if (!childrenIds.contains(blockId)) childrenIds.add(blockId);
            touch();
        }
    }

    public void addToFalseBranch(int blockId) {
        if (!falseBranch.contains(blockId)) {
            falseBranch.add(blockId);
            properties.put("false_branch", falseBranch);
            if (!childrenIds.contains(blockId)) childrenIds.add(blockId);
            touch();
        }
    }

    public int addCase(Integer valueBlockId, Object value) {
        CaseData cd = new CaseData();
        cd.valueId = valueBlockId;
        cd.value = value;
        cd.blocks = new ArrayList<>();
        cases.add(cd);
        properties.put("cases", serializeCases());
        if (valueBlockId != null && !childrenIds.contains(valueBlockId))
            childrenIds.add(valueBlockId);
        touch();
        return cases.size() - 1;
    }

    public boolean addToCase(int caseIndex, int blockId) {
        if (caseIndex >= 0 && caseIndex < cases.size()) {
            if (!cases.get(caseIndex).blocks.contains(blockId)) {
                cases.get(caseIndex).blocks.add(blockId);
                properties.put("cases", serializeCases());
                if (!childrenIds.contains(blockId)) childrenIds.add(blockId);
                touch();
                return true;
            }
        }
        return false;
    }

    public void addToDefault(int blockId) {
        if (!defaultCase.contains(blockId)) {
            defaultCase.add(blockId);
            properties.put("default", defaultCase);
            if (!childrenIds.contains(blockId)) childrenIds.add(blockId);
            touch();
        }
    }

    // ========== DROPLIST ==========

    public boolean isDroplistBlock() { return isDroplist; }
    public List<String> getDroplistOptions() { return droplistOptions; }
    public String getDroplistSelected() { return droplistSelected; }

    public boolean setDroplistSelected(String option) {
        if (droplistOptions.contains(option)) {
            this.droplistSelected = option;
            properties.put("_droplist_selected", option);
            touch();
            return true;
        }
        return false;
    }

    // ========== REFERENCE ==========

    public boolean isReferenceBlock() { return isReference; }
    public Integer getSourceId() { return sourceId; }
    public String getSourceType() { return sourceType; }

    public void setAsReference(int sourceId, String sourceType) {
        this.isReference = true;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        properties.put("_is_reference", true);
        properties.put("_source_id", sourceId);
        properties.put("_source_type", sourceType);
        touch();
    }

    // ========== CALL BLOCK ==========

    public boolean isCallBlock() { return isCallBlock; }
    public Integer getSourceBlockId() { return sourceBlockId; }
    public String getSourceBlockName() { return sourceBlockName; }
    public String getSourceBlockType() { return sourceBlockType; }

    // ========== СЛОВАРИ ==========

    public boolean isDictContainer() {
        return isContainer && "dictionary".equals(containerType);
    }

    public List<int[]> getDictPairs() { return dictPairs; }

    public boolean addDictPair(int keyId, int valueId, int index) {
        int[] pair = new int[]{keyId, valueId};
        if (index < 0 || index >= dictPairs.size()) {
            dictPairs.add(pair);
        } else {
            dictPairs.add(index, pair);
        }
        if (!childrenIds.contains(keyId)) childrenIds.add(keyId);
        if (!childrenIds.contains(valueId)) childrenIds.add(valueId);
        properties.put("_dict_pairs", serializeDictPairs());
        touch();
        return true;
    }

    // ========== ДОЧЕРНИЕ БЛОКИ ==========

    public void addChild(int childId) {
        if (!childrenIds.contains(childId)) {
            childrenIds.add(childId);
            touch();
        }
    }

    public boolean removeChild(int childId) {
        if (childrenIds.remove((Integer) childId)) {
            touch();
            return true;
        }
        return false;
    }

    // ========== ARGUMENT CONTAINERS ==========

    public ArgumentContainer getArgumentContainer(String containerName) {
        return getArgumentContainer(containerName, null);
    }

    public ArgumentContainer getArgumentContainer(String containerName, Map<String, Object> config) {
        if (!argumentContainers.containsKey(containerName)) {
            argumentContainers.put(containerName,
                    new ArgumentContainer(this, containerName, config));
        }
        return argumentContainers.get(containerName);
    }

    public boolean hasArgumentContainer(String containerName) {
        return argumentContainers.containsKey(containerName)
                || properties.containsKey(containerName);
    }

    public Map<String, ArgumentContainer> getAllArgumentContainers() {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof List && !argumentContainers.containsKey(entry.getKey())) {
                List<?> list = (List<?>) entry.getValue();
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    Map<?, ?> first = (Map<?, ?>) list.get(0);
                    if (first.containsKey("type") && first.containsKey("id")) {
                        argumentContainers.put(entry.getKey(),
                                new ArgumentContainer(this, entry.getKey()));
                    }
                }
            }
        }
        return argumentContainers;
    }

    public void saveArgumentContainers() {
        for (Map.Entry<String, ArgumentContainer> entry : argumentContainers.entrySet()) {
            properties.put(entry.getKey(), entry.getValue().toList());
        }
        touch();
    }

    // ========== СЕРИАЛИЗАЦИЯ ==========

    private List<Map<String, Object>> serializeCases() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CaseData cd : cases) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value_id", cd.valueId);
            map.put("value", cd.value);
            map.put("blocks", cd.blocks);
            result.add(map);
        }
        return result;
    }

    private List<List<Integer>> serializeDictPairs() {
        List<List<Integer>> result = new ArrayList<>();
        for (int[] pair : dictPairs) {
            result.add(Arrays.asList(pair[0], pair[1]));
        }
        return result;
    }

    public void prepareForSerialization() {
        saveArgumentContainers();
        properties.put("_is_container", isContainer);
        properties.put("_container_config", containerConfig);
        properties.put("_container_items", containerItems);
        properties.put("_is_droplist", isDroplist);
        properties.put("_droplist_options", droplistOptions);
        properties.put("_droplist_selected", droplistSelected);
        properties.put("_is_reference", isReference);
        properties.put("_source_id", sourceId);
        properties.put("_source_type", sourceType);
        properties.put("_is_call_block", isCallBlock);
        properties.put("_source_block_id", sourceBlockId);
        properties.put("_source_block_name", sourceBlockName);
        properties.put("_source_block_type", sourceBlockType);
        properties.put("_dict_pairs", serializeDictPairs());
        properties.put("_is_branching", isBranching);
        properties.put("_is_switch", isSwitch);
        properties.put("_branch_type", branchType);
        properties.put("condition_id", conditionId);
        properties.put("true_branch", trueBranch);
        properties.put("false_branch", falseBranch);
        properties.put("cases", serializeCases());
        properties.put("default", defaultCase);
    }

    // ========== УТИЛИТЫ ==========

    public void touch() {
        this.modifiedAt = java.time.LocalDateTime.now().toString();
    }

    public boolean isCode() {
        return type != null && "code".equals(type.getCategory());
    }

    public boolean isGui() {
        return type != null && "gui".equals(type.getCategory());
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

    public static class BlockType {
        @SerializedName("category")
        private String category;

        @SerializedName("class_name")
        private String className;

        @SerializedName("subclass_name")
        private String subclassName;

        @SerializedName("source_id")
        private Integer sourceId;

        @SerializedName("source_type")
        private String sourceType;

        @SerializedName("original_name")
        private String originalName;

        @SerializedName("is_dict_reference")
        private boolean isDictReference;

        public BlockType() {}

        public BlockType(String category, String className, String subclassName) {
            this.category = category;
            this.className = className;
            this.subclassName = subclassName;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getSubclassName() { return subclassName; }
        public void setSubclassName(String subclassName) { this.subclassName = subclassName; }

        public String getFullName() { return className + "." + subclassName; }
        public String getDisplayName() { return subclassName; }

        public boolean isCode() { return "code".equals(category); }
        public boolean isGui() { return "gui".equals(category); }

        public Integer getSourceId() { return sourceId; }
        public String getSourceType() { return sourceType; }
        public String getOriginalName() { return originalName; }
        public boolean isDictReference() { return isDictReference; }
    }

    public static class CaseData {
        public Integer valueId;
        public Object value;
        public List<Integer> blocks = new ArrayList<>();
    }
}