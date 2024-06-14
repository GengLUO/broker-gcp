package be.kuleuven.dsgt4.broker.domain;

import java.util.UUID;

public class User {

    private UUID uid;
    private String email;
    private String role;

    public User(String email, String role) {
        this.uid = UUID.randomUUID();
        this.email = email;
        this.role = role;
    }

    public UUID getId() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isManager() {
        return this.role != null && this.role.equals("manager");
    }
}
