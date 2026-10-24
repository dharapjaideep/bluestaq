package com.bluestaq.demo.service;

import com.bluestaq.demo.dto.AddMemberRequest;
import com.bluestaq.demo.dto.TeamRequest;
import com.bluestaq.demo.dto.TeamResponse;
import com.bluestaq.demo.exception.TeamCreatorOnlyException;
import com.bluestaq.demo.exception.TeamMembershipConflictException;
import com.bluestaq.demo.exception.TeamNotFoundException;
import com.bluestaq.demo.exception.UserNotFoundException;
import com.bluestaq.demo.model.Team;
import com.bluestaq.demo.model.User;
import com.bluestaq.demo.repository.TeamRepository;
import com.bluestaq.demo.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team creation and membership, with the access rules enforced here:
 * <ul>
 *   <li>Only members can see or change a team; for anyone else it does not exist (404).</li>
 *   <li>Any member can add an existing user (teams are small and trusted).</li>
 *   <li>Members can remove themselves; only the creator can remove others (403).</li>
 *   <li>The creator can never be removed (409), so every team keeps at least one member.</li>
 * </ul>
 * Note access is derived from membership at query time (see {@link NoteService}), so removing a
 * member revokes their access to the team's notes immediately.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * Creates a team with the caller as its creator and first member.
     *
     * @param email   the authenticated caller's email
     * @param request the validated team name
     * @return the created team
     */
    @Transactional
    public TeamResponse create(String email, TeamRequest request) {
        User user = currentUser(email);
        Team team = new Team(request.name().trim(), user);
        team.addMember(user);
        return TeamResponse.from(teamRepository.save(team));
    }

    /**
     * Lists the teams the caller is a member of, with their members.
     *
     * @param email the authenticated caller's email
     * @return the caller's teams, by name
     */
    @Transactional(readOnly = true)
    public List<TeamResponse> list(String email) {
        User user = currentUser(email);
        return teamRepository.findAllByMember(user.getId()).stream()
                .map(TeamResponse::from)
                .toList();
    }

    /**
     * Adds an existing user to a team.
     *
     * @param email   the authenticated caller's email
     * @param teamId  the team's id
     * @param request the validated email of the user to add
     * @return the team with its updated members
     * @throws TeamNotFoundException           if the team does not exist or the caller is not a member
     * @throws UserNotFoundException           if no user has that email
     * @throws TeamMembershipConflictException if the user is already a member
     */
    @Transactional
    public TeamResponse addMember(String email, Long teamId, AddMemberRequest request) {
        Team team = teamForMember(teamId, currentUser(email));
        User newMember = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .orElseThrow(() -> new UserNotFoundException("No user with this email"));
        if (team.hasMember(newMember.getId())) {
            throw new TeamMembershipConflictException("User is already a member of this team");
        }
        team.addMember(newMember);
        try {
            // saveAndFlush so the team_members primary key is checked here, not at commit time.
            return TeamResponse.from(teamRepository.saveAndFlush(team));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent adds of the same user can both pass hasMember; the composite
            // primary key is the real guard, so the losing request is reported as a duplicate.
            throw new TeamMembershipConflictException("User is already a member of this team");
        }
    }

    /**
     * Removes a member from a team.
     *
     * @param email  the authenticated caller's email
     * @param teamId the team's id
     * @param userId the id of the member to remove
     * @throws TeamNotFoundException           if the team does not exist or the caller is not a member
     * @throws TeamCreatorOnlyException        if a non-creator tries to remove someone else
     * @throws TeamMembershipConflictException if the member to remove is the team's creator
     * @throws UserNotFoundException           if that user is not a member of the team
     */
    @Transactional
    public void removeMember(String email, Long teamId, Long userId) {
        User user = currentUser(email);
        Team team = teamForMember(teamId, user);
        // Permission is checked before the target's state, so non-creators learn nothing about
        // other members beyond what the member list already shows them.
        if (!userId.equals(user.getId()) && !team.isCreatedBy(user.getId())) {
            throw new TeamCreatorOnlyException("Only the team's creator can remove other members");
        }
        if (team.isCreatedBy(userId)) {
            throw new TeamMembershipConflictException("The team's creator can't be removed");
        }
        if (!team.removeMember(userId)) {
            throw new UserNotFoundException("User is not a member of this team");
        }
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(User.normalizeEmail(email))
                // Should not happen: HTTP Basic loaded this same user earlier in this request.
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private Team teamForMember(Long teamId, User user) {
        // Security: a missing team and a team the caller is not in both give 404, so team ids
        // cannot be probed by non-members.
        if (!teamRepository.isMember(teamId, user.getId())) {
            throw new TeamNotFoundException();
        }
        return teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
    }
}
