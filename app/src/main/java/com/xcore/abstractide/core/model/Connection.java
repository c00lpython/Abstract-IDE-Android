package com.xcore.abstractide.core.model;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

/**
 * Аналог: models.py — класс Connection
 * Соединение между двумя портами блоков
 */
public class Connection {

    @SerializedName("id")
    private String id;

    @SerializedName("from_block_id")
    private int fromBlockId;

    @SerializedName("from_port")
    private String fromPort;

    @SerializedName("to_block_id")
    private int toBlockId;

    @SerializedName("to_port")
    private String toPort;

    @SerializedName("data_type")
    private String dataType;

    @SerializedName("arg_container")
    private String argContainer;

    @SerializedName("arg_index")
    private Integer argIndex;

    @SerializedName("created_at")
    private String createdAt;

    // ========== КОНСТРУКТОРЫ ==========

    public Connection() {
        this.id = UUID.randomUUID().toString();
        this.dataType = "any";
        this.createdAt = java.time.LocalDateTime.now().toString();
    }

    public Connection(int fromBlockId, String fromPort, int toBlockId, String toPort) {
        this();
        this.fromBlockId = fromBlockId;
        this.fromPort = fromPort;
        this.toBlockId = toBlockId;
        this.toPort = toPort;
    }

    public Connection(int fromBlockId, String fromPort, int toBlockId, String toPort,
                      String connectionId, String dataType, String argContainer, Integer argIndex) {
        this.id = connectionId != null ? connectionId : UUID.randomUUID().toString();
        this.fromBlockId = fromBlockId;
        this.fromPort = fromPort;
        this.toBlockId = toBlockId;
        this.toPort = toPort;
        this.dataType = dataType != null ? dataType : "any";
        this.argContainer = argContainer;
        this.argIndex = argIndex;
        this.createdAt = java.time.LocalDateTime.now().toString();
    }

    // ========== ГЕТТЕРЫ / СЕТТЕРЫ ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getFromBlockId() { return fromBlockId; }
    public void setFromBlockId(int fromBlockId) { this.fromBlockId = fromBlockId; }

    public String getFromPort() { return fromPort; }
    public void setFromPort(String fromPort) { this.fromPort = fromPort; }

    public int getToBlockId() { return toBlockId; }
    public void setToBlockId(int toBlockId) { this.toBlockId = toBlockId; }

    public String getToPort() { return toPort; }
    public void setToPort(String toPort) { this.toPort = toPort; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getArgContainer() { return argContainer; }
    public void setArgContainer(String argContainer) { this.argContainer = argContainer; }

    public Integer getArgIndex() { return argIndex; }
    public void setArgIndex(Integer argIndex) { this.argIndex = argIndex; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // ========== УТИЛИТЫ ==========

    @Override
    public String toString() {
        return "Connection{" +
                "id='" + id + '\'' +
                ", " + fromBlockId + ":" + fromPort +
                " -> " + toBlockId + ":" + toPort +
                '}';
    }
}