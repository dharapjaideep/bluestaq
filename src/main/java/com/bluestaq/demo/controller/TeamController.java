package com.bluestaq.demo.controller;

import com.bluestaq.demo.dto.AddMemberRequest;
import com.bluestaq.demo.dto.TeamRequest;
import com.bluestaq.demo.dto.TeamResponse;
import com.bluestaq.demo.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Team endpoints. All require authentication (SecurityConfig's default rule). Thin delegator to
 * {@link TeamService}, which enforces the membership rules.
 */
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * Creates a team; the caller becomes its creator and first member.
     *
     * @param principal the authenticated caller
     * @param request   the team name, validated
     * @return the created team
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@AuthenticationPrincipal UserDetails principal,
                               @Valid @RequestBody TeamRequest request) {
        return teamService.create(principal.getUsername(), request);
    }

    /**
     * Lists the teams the caller is a member of.
     *
     * @param principal the authenticated caller
     * @return the caller's teams with their members
     */
    @GetMapping
    public List<TeamResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return teamService.list(principal.getUsername());
    }

    /**
     * Adds an existing user to a team. Any member can add.
     *
     * @param principal the authenticated caller
     * @param id        the team's id
     * @param request   the email of the user to add, validated
     * @return the team with its updated members
     */
    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse addMember(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
                                  @Valid @RequestBody AddMemberRequest request) {
        return teamService.addMember(principal.getUsername(), id, request);
    }

    /**
     * Removes a member from a team.
     *
     * @param principal the authenticated caller
     * @param id        the team's id
     * @param userId    the id of the member to remove
     */
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id,
                             @PathVariable Long userId) {
        teamService.removeMember(principal.getUsername(), id, userId);
    }
}
