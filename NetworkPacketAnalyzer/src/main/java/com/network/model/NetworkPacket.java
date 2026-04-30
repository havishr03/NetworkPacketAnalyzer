package com.network.model;

/**
 * Model class representing a Network Packet.
 */
public class NetworkPacket {
    private String packetId;
    private String packetType;
    private String description;

    public NetworkPacket(String packetId, String packetType, String description) {
        this.packetId = packetId;
        this.packetType = packetType;
        this.description = description;
    }

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public String getPacketType() {
        return packetType;
    }

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
