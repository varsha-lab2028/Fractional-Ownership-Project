package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.OwnershipHistory;

import java.sql.*;
import java.util.*;
import java.time.*;

//list history - trigger proof
public class OwnershipHistoryDAO {
    //list by investor
    public List<OwnershipHistory> listByInvestorId(int investorId) throws SQLException {
        String sql = """
            SELECT history_id, investor_id, asset_id, units_before, units_after, change_date, change_type, trade_id, ipo_id
            FROM ownership_history
            WHERE investor_id = ?
            ORDER BY change_time DESC
        """;

        List<OwnershipHistory> out = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new OwnershipHistory(
                            rs.getInt("history_id"),
                            rs.getInt("investor_id"),
                            rs.getInt("asset_id"),
                            rs.getInt("units_before"),
                            rs.getInt("units_after"),
                            rs.getDate("change_date").toLocalDate(),
                            rs.getString("change_type"),
                            rs.getInt("trade_id"),
                            rs.getInt("ipo_id")
                    ));
                }
            }
        }
        return out;
    }
}
