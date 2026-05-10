package br.edu.chat.model;

public class Group {
    private int id;
    private String name;
    private int createdByUserId;

    public Group() {
    }

    public Group(String name, int createdByUserId) {
        this.name = name;
        this.createdByUserId = createdByUserId;
    }

    public Group(int id, String name, int createdByUserId) {
        this.id = id;
        this.name = name;
        this.createdByUserId = createdByUserId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdByUserId=" + createdByUserId +
                '}';
    }
}
