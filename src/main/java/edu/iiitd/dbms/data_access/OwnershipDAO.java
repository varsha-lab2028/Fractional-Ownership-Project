package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Ownership;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//finding the ownership, updating the units held by an investor and listing out the contents in ownership
//updating the units is essential for trading
public class OwnershipDAO {
    public Ownership findOwnership(int investorId, int assetId) throws SQLException {
        try(Connection connect = ServerConnector.DBConnection()){
            return findOwnership(connect, investorId, assetId);
        }
    }

    public Ownership findOwnership(Connection connect, int investorId, int assetId) throws SQLException {
        String sqlQuery = "SELECT investor_id, asset_id, units_held FROM ownership WHERE investor_id = ? AND asset_id = ?";
        try(PreparedStatement ps = connect.prepareStatement(sqlQuery)){
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    return new Ownership(rs.getInt("investor_id"), rs.getInt("asset_id"), rs.getInt("units_held"));
                }
                return null;
            }
        }
    }

    //updating units method
    public void updateUnits(Connection connect, int investorId, int assetId, int newUnitsHeld) throws SQLException {
        String sqlQuery = "INSERT INTO ownership(investor_id, asset_id, units_held) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE units_held = VALUES(units_held)";
        try(PreparedStatement ps = connect.prepareStatement(sqlQuery)){
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            ps.setInt(3, newUnitsHeld);
            ps.executeUpdate();
        }
    }

    //get by investor and asset
    public List<Ownership> listHoldings(int investorId) throws SQLException{
        String sqlQuery = "SELECT investor_id, asset_id, units_held FROM ownership WHERE investor_id = ? ORDER BY asset_id";
        List<Ownership> ownershipList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
            PreparedStatement ps = connect.prepareStatement(sqlQuery)){
                ps.setInt(1, investorId);
                try(ResultSet rs = ps.executeQuery()){
                    while(rs.next()){
                        ownershipList.add(new Ownership(
                                rs.getInt("investor_id"),
                                rs.getInt("asset_id"),
                                rs.getInt("units_held")
                        ));
                    }
                }
        }
        return ownershipList;
    }
}
