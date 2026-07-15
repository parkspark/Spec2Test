package com.example.gameqacopilot.project;

import com.example.gameqacopilot.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String name;
    private String description;
    private String gameGenre;
    private String platform;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Project() {}

    Project(User owner, ProjectCreateRequest request) {
        this.owner = owner;
        this.name = request.name();
        this.description = request.description();
        this.gameGenre = request.gameGenre();
        this.platform = request.platform();
        this.status = ProjectStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    String getGameGenre() {
        return gameGenre;
    }

    String getPlatform() {
        return platform;
    }

    ProjectStatus getStatus() {
        return status;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
