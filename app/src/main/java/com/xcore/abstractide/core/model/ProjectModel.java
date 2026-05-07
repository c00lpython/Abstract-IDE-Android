package com.xcore.abstractide.core.model;

import com.google.gson.annotations.SerializedName;
import java.util.*;

/**
 * Аналог: models.py — класс Project
 */
public class ProjectModel {

    @SerializedName("version")
    private String version = "2.0.0";

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("modified_at")
    private String modifiedAt;

    @SerializedName("blocks")
    private Map<String, BlockModel> blocks = new LinkedHashMap<>();

    @SerializedName("connections")
    private Map<String, Connection> connections = new LinkedHashMap<>();

    private transient SimpleIDManager idManager = new SimpleIDManager();
    private transient Map<String, List<Integer>> nameIndex = new LinkedHashMap<>();

    public ProjectModel() {
        this.id = "project_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        this.name = "Untitled Project";
        this.createdAt = java.time.LocalDateTime.now().toString();
        this.modifiedAt = this.createdAt;
    }

    public ProjectModel(String name) {
        this();
        this.name = name != null ? name : "Untitled Project";
    }

    public void initTransients() {
        idManager = new SimpleIDManager();
        nameIndex = new LinkedHashMap<>();
        if (blocks == null) blocks = new LinkedHashMap<>();
        if (connections == null) connections = new LinkedHashMap<>();
        for (BlockModel block : blocks.values()) {
            block.initTransients();
            idManager.registerId(block.getId());
            nameIndex.computeIfAbsent(block.getName(), k -> new ArrayList<>()).add(block.getId());
        }
    }

    public void addBlock(BlockModel block) {
        if (block.getId() == 0) block.setId(idManager.getNextId());
        else idManager.registerId(block.getId());
        blocks.put(String.valueOf(block.getId()), block);
        modifiedAt = java.time.LocalDateTime.now().toString();
        nameIndex.computeIfAbsent(block.getName(), k -> new ArrayList<>()).add(block.getId());
        if (block.getParentId() != null) {
            BlockModel parent = getBlock(block.getParentId());
            if (parent != null) parent.addChild(block.getId());
        }
    }

    // ✅ ДОБАВЛЕН МЕТОД removeBlock
    public void removeBlock(int blockId) {
        String key = String.valueOf(blockId);
        BlockModel block = blocks.get(key);
        if (block != null) {
            // Удаляем все связи, связанные с этим блоком
            List<String> toRemove = new ArrayList<>();
            for (Connection conn : connections.values()) {
                if (conn.getFromBlockId() == blockId || conn.getToBlockId() == blockId) {
                    toRemove.add(conn.getId());
                }
            }
            for (String connId : toRemove) {
                connections.remove(connId);
            }

            // Удаляем блок из родителя
            if (block.getParentId() != null) {
                BlockModel parent = getBlock(block.getParentId());
                if (parent != null) {
                    parent.getChildrenIds().remove((Integer) blockId);
                }
            }

            // Удаляем всех детей рекурсивно
            for (int childId : block.getChildrenIds()) {
                removeBlock(childId);
            }

            // Удаляем из индекса имен
            if (nameIndex.containsKey(block.getName())) {
                nameIndex.get(block.getName()).remove((Integer) blockId);
                if (nameIndex.get(block.getName()).isEmpty()) {
                    nameIndex.remove(block.getName());
                }
            }

            // Удаляем блок
            blocks.remove(key);
            modifiedAt = java.time.LocalDateTime.now().toString();
        }
    }

    // ✅ ДОБАВЛЕН МЕТОД removeConnection
    public void removeConnection(String connectionId) {
        if (connections.containsKey(connectionId)) {
            connections.remove(connectionId);
            modifiedAt = java.time.LocalDateTime.now().toString();
        }
    }

    public BlockModel getBlock(int blockId) {
        return blocks.get(String.valueOf(blockId));
    }

    public List<BlockModel> getAllBlocks() {
        return new ArrayList<>(blocks.values());
    }

    public Map<String, BlockModel> getBlocksMap() {
        return blocks;
    }

    public List<Connection> getConnections() {
        return new ArrayList<>(connections.values());
    }

    public void addConnection(Connection conn) {
        connections.put(conn.getId(), conn);
        modifiedAt = java.time.LocalDateTime.now().toString();
    }

    public void clear() {
        blocks.clear();
        connections.clear();
        nameIndex.clear();
        idManager.reset();
        modifiedAt = java.time.LocalDateTime.now().toString();
    }

    public void copyFrom(ProjectModel other) {
        this.id = other.id;
        this.name = other.name;
        this.version = other.version;
        this.createdAt = other.createdAt;
        this.modifiedAt = java.time.LocalDateTime.now().toString();
        this.blocks = other.blocks;
        this.connections = other.connections;
        initTransients();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }
    public SimpleIDManager getIdManager() { return idManager; }
    public int getBlockCount() { return blocks.size(); }
    public int getConnectionCount() { return connections.size(); }

    public void prepareForSerialization() {
        for (BlockModel block : blocks.values()) block.prepareForSerialization();
        modifiedAt = java.time.LocalDateTime.now().toString();
    }
}