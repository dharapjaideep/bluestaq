package com.bluestaq.demo.repository;

import com.bluestaq.demo.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Team} and its memberships.
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Checks whether a user is a member of a team.
     *
     * @param teamId the team's id
     * @param userId the user's id
     * @return {@code true} if the team exists and the user is a member; {@code false} otherwise
     */
    @Query("select count(t) > 0 from Team t join t.members m where t.id = :teamId and m.id = :userId")
    boolean isMember(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
