package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.AdminDAO;
import edu.iiitd.dbms.domain.Admin;

import java.sql.SQLException;
import java.util.List;

public class AdminDaoImpl {

    private final AdminDAO adminDAO;

    public AdminDaoImpl() {
        this.adminDAO = new AdminDAO();
    }

    //Return every admin record, ordered by admin_id
    public List<Admin> getAll() throws SQLException {
        return adminDAO.listAdmins();
    }

    //Fetch a single admin by primary key
    public Admin getById(int adminId) throws SQLException {
        return adminDAO.findByAdminId(adminId);
    }

    //Fetch all admins with a given role
    public List<Admin> getByRole(String role) throws SQLException {
        return adminDAO.findByAdminRole(role);
    }

    //Insert a new admin record
    public void insert(Admin admin) throws SQLException {
        adminDAO.insertAdmin(admin);
    }

    //Change the role of an existing admin
    public void updateRole(int adminId, String newRole) throws SQLException {
        adminDAO.updateAdminRole(adminId, newRole);
    }

    //Remove an admin by primary key
    public void delete(int adminId) throws SQLException {
        adminDAO.deleteAdmin(adminId);
    }
}
