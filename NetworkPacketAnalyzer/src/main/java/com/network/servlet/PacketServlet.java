package com.network.servlet;

import com.network.model.NetworkPacket;
import com.network.util.DatabaseUtil;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet handling API requests for Network Packets.
 */
@WebServlet("/api/packets")
public class PacketServlet extends HttpServlet {

    /**
     * POST /api/packets
     * Accepts a raw log string, parses it, and stores it in the database.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String logLine = request.getParameter("logLine");
        
        // Handle Missing Parameter Exception
        if (logLine == null || logLine.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Log line parameter is missing or empty.");
            return;
        }

        try {
            // Task requirement: Extract packet type using substring()
            // Example format: "PACKET: TCP connection from 192.168.1.10"
            int colonIndex = logLine.indexOf(":");
            if (colonIndex == -1) {
                throw new IllegalArgumentException("Invalid log format. Must contain ':'");
            }
            
            String remainder = logLine.substring(colonIndex + 1).trim();
            int spaceIndex = remainder.indexOf(" ");
            if (spaceIndex == -1) {
                throw new IllegalArgumentException("Invalid log format. Missing description after packet type.");
            }
            
            // Extract type using substring
            String type = remainder.substring(0, spaceIndex);
            String desc = remainder.substring(spaceIndex + 1);
            
            String packetId = "PKT-" + System.currentTimeMillis();
            NetworkPacket packet = new NetworkPacket(packetId, type, desc);

            savePacketToDatabase(packet);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"success\", \"message\":\"Packet saved successfully.\"}");
            
        // Task requirement: Identify major exceptions to be generated and handle them
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection error: " + e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * GET /api/packets?type=TCP
     * Returns a JSON list of packets, optionally filtered by type.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filterType = request.getParameter("type");

        /* 
         * Task requirement: Store packets in a suitable data structure.
         * Significance: We use an ArrayList here because it allows for fast, continuous memory allocation 
         * and O(1) time complexity for sequential access when iterating to serialize into JSON format 
         * for the Web UI. It's the most efficient structure for fetching read-only records.
         */
        List<NetworkPacket> packets = new ArrayList<>();
        
        // Task requirement: Use StringBuffer to format packet information before displaying
        StringBuffer consoleOutput = new StringBuffer();
        consoleOutput.append("--- Fetching Network Packets ---\n");
        if (filterType != null && !filterType.isEmpty()) {
            consoleOutput.append("Filter Applied: Type = ").append(filterType).append("\n");
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT packet_id, packet_type, description, TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at FROM packets";
            if (filterType != null && !filterType.isEmpty()) {
                sql += " WHERE packet_type = ?";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (filterType != null && !filterType.isEmpty()) {
                    stmt.setString(1, filterType);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        NetworkPacket packet = new NetworkPacket(
                            rs.getString("packet_id"),
                            rs.getString("packet_type"),
                            rs.getString("description"),
                            rs.getString("created_at")
                        );
                        packets.add(packet);
                        // Using StringBuffer to format log
                        consoleOutput.append("Found: [").append(packet.getPacketId()).append("] ")
                                     .append(packet.getPacketType()).append(" - ")
                                     .append(packet.getDescription()).append("\n");
                    }
                }
            }
            
            // Print formatted buffer to server console
            System.out.println(consoleOutput.toString());

            // Send JSON response
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print(new Gson().toJson(packets));
            out.flush();

        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error while fetching packets.");
        }
    }

    /**
     * DELETE /api/packets?id=...
     * Deletes a packet from the database.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("clear_all".equals(action)) {
            clearAllPackets(response);
            return;
        }

        String packetId = request.getParameter("id");
        if (packetId == null || packetId.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Packet ID parameter is missing or empty.");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "DELETE FROM packets WHERE packet_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, packetId);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"success\", \"message\":\"Packet deleted successfully.\"}");
                } else {
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Packet not found.");
                }
            }
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error while deleting packet: " + e.getMessage());
        }
    }

    private void savePacketToDatabase(NetworkPacket packet) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO packets (packet_id, packet_type, description) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, packet.getPacketId());
                stmt.setString(2, packet.getPacketType());
                stmt.setString(3, packet.getDescription());
                stmt.executeUpdate();
            }
        }
    }

    private void clearAllPackets(HttpServletResponse response) throws IOException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "DELETE FROM packets";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"success\", \"message\":\"All packets cleared successfully.\"}");
            }
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error while clearing packets: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
