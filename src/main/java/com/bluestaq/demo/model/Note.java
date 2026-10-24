package com.bluestaq.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A note owned by one user. A note with no team is personal (owner only); a note in a team is
 * also readable and editable by that team's members.
 */
@Entity
@Table(name = "notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA only
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 20000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    /** {@code null} for a personal note. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic-locking counter: Hibernate increments it on every update and refuses a write
     * whose version no longer matches the row (a concurrent save in the same moment). Stale
     * edits from earlier requests are caught in NoteService by comparing the client's version.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * Creates a new, unsaved note.
     *
     * @param title   the note's title
     * @param content the note's body, may be {@code null}
     * @param owner   the user who owns the note
     * @param team    the team to share it with, or {@code null} for a personal note
     */
    public Note(String title, String content, User owner, Team team) {
        this.title = title;
        this.content = content;
        this.owner = owner;
        this.team = team;
    }

    /**
     * Replaces the note's editable fields and bumps {@code updatedAt}. Access checks are the
     * caller's job.
     *
     * @param title   the new title
     * @param content the new body, may be {@code null}
     * @param team    the new team, or {@code null} for a personal note
     */
    public void update(String title, String content, Team team) {
        this.title = title;
        this.content = content;
        this.team = team;
        // Set here rather than in @PreUpdate, which only runs at flush: the response built right
        // after this call must already carry the new timestamp.
        this.updatedAt = Instant.now();
    }

    /**
     * Checks whether a user owns this note.
     *
     * @param userId the user's id
     * @return {@code true} if the user is the owner
     */
    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    /**
     * Returns the id of the note's team without loading the team.
     *
     * @return the team id, or {@code null} for a personal note
     */
    public Long getTeamId() {
        return team == null ? null : team.getId();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
}
