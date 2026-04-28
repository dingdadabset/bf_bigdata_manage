package com.dga.metadata.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dga_metadata_tag")
public class MetadataTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_name", nullable = false)
    private String tagName;

    @Column(name = "tag_type")
    private String tagType;

    @Column(name = "color")
    private String color;

    @Column(name = "description")
    private String description;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (tagType == null) {
            tagType = "CUSTOM";
        }
    }
}
