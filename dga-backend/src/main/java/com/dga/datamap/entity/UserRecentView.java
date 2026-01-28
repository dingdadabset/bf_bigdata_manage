package com.dga.datamap.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_user_recent_views")
public class UserRecentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "view_type", nullable = false)
    private String viewType; // TABLE, DATASOURCE

    @Column(name = "view_content", nullable = false)
    private String viewContent; // e.g. db_name.table_name

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getViewContent() {
        return viewContent;
    }

    public void setViewContent(String viewContent) {
        this.viewContent = viewContent;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
