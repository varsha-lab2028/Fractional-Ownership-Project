package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Investor;

import java.sql.*;
import java.util.*;

//read
public class InvestorDAO {
    private Investor matchInvestorColumns(ResultSet rs) throws SQLException {
        return new Investor(
                rs.getInt("investor_id"),
                rs.getString("investor_name"),
                rs.getString("email"),
                rs.getDate("registration_date").toLocalDate()
        );
    }

    public List<Investor> listInvestors() throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, wallet_balance
            FROM investor
            ORDER BY investor_id
        """;
        List<Investor> investorList = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) investorList.add(matchInvestorColumns(rs));
        }
        return investorList;
    }

    public Investor findByInvestorId(int investorId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            return findByInvestorId(connect, investorId);
        }
    }

    public Investor findByInvestorId(Connection connect, int investorId) throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, wallet_balance
            FROM investor
            WHERE investor_id = ?
        """;
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return matchInvestorColumns(rs);
                } else {
                    return null;
                }
            }
        }
    }
}
