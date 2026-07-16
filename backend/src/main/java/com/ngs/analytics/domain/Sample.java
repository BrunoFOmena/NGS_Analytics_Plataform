package com.ngs.analytics.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samples")
public class Sample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String notes;

    /** Optional FASTA reference metadata (Phase 3). */
    private String fastaReferenceName;
    private String fastaStoragePath;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFastaReferenceName() {
        return fastaReferenceName;
    }

    public void setFastaReferenceName(String fastaReferenceName) {
        this.fastaReferenceName = fastaReferenceName;
    }

    public String getFastaStoragePath() {
        return fastaStoragePath;
    }

    public void setFastaStoragePath(String fastaStoragePath) {
        this.fastaStoragePath = fastaStoragePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
