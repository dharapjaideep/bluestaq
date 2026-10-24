package com.bluestaq.demo.dto;

import com.bluestaq.demo.model.Team;
import com.bluestaq.demo.model.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * A team as returned by the API, with its members.
 *
 * @param id          the team's id
 * @param name        the team's name
 * @param createdById the id of the user who created the team
 * @param createdAt   when the team was created
 * @param members     the members' ids and emails, sorted by email
 */
public record TeamResponse(Long id, String name, Long createdById, Instant createdAt, List<UserResponse> members) {

    /**
     * Builds the API view of a team. Loads the members, so call it inside a transaction.
     *
     * @param team the entity
     * @return the response DTO
     */
    public static TeamResponse from(Team team) {
        List<UserResponse> members = team.getMembers().stream()
                .sorted(Comparator.comparing(User::getEmail))
                .map(UserResponse::from)
                .toList();
        return new TeamResponse(team.getId(), team.getName(), team.getCreatedBy().getId(),
                team.getCreatedAt(), members);
    }
}
