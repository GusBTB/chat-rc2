package br.edu.chat.model;

public class User {
    private int id;
    private String fullName;
    private String login;
    private String email;
    private String password;
    private UserStatus status;

    public User() {
    }

    public User(String fullName, String login, String email, String password, UserStatus status) {
        this.fullName = fullName;
        this.login = login;
        this.email = email;
        this.password = password;
        this.status = status;
    }

    public User(int id, String fullName, String login, String email, String password, UserStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.login = login;
        this.email = email;
        this.password = password;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", login='" + login + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                '}';
    }
}
