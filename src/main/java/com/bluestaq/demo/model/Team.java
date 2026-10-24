package com.bluestaq.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A group of users who share notes. Members can read and edit the team's notes; see
 * {@link com.bluestaq.demo.service.NoteService} for the full access rules.
 */
@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA only
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Membership has no attributes of its own, so team_members is mapped as a plain join table.
    @ManyToMany
    @JoinTable(name = "team_members",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    /**
     * Creates a new, unsaved team. The creator is not added as a member automatically.
     *
     * @param name      the team's display name
     * @param createdBy the user creating the team
     */
    public Team(String name, User createdBy) {
        this.name = name;
        this.createdBy = createdBy;
    }

    /**
     * Adds a user to the team; adding an existing member has no effect.
     *
     * @param user the user to add
     */
    public void addMember(User user) {
        members.add(user);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
