package com.bluestaq.demo.repository;

import com.bluestaq.demo.model.Note;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Note}. The "accessible" queries apply the read rule in the database:
 * a user can see a note they own, or a note in a team they are a member of.
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Finds a note only if the user can access it.
     *
     * @param id     the note's id
     * @param userId the user's id
     * @return the note, or empty if it does not exist or the user cannot access it
     */
    @Query("""
            select n from Note n
            where n.id = :id
              and (n.owner.id = :userId
                   or exists (select 1 from Team t join t.members m
                              where t.id = n.team.id and m.id = :userId))
            """)
    Optional<Note> findAccessible(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Lists the notes a user can access, most recently updated first.
     *
     * @param userId  the user's id
     * @param teamId  only notes in this team, or {@code null} for no team filter
     * @param pattern a lower-case LIKE pattern matched against title and content, with
     *                {@code \} as the escape character; {@code "%"} matches everything
     * @return the matching notes
     */
    @Query("""
            select n from Note n
            where (n.owner.id = :userId
                   or exists (select 1 from Team t join t.members m
                              where t.id = n.team.id and m.id = :userId))
              and (:teamId is null or n.team.id = :teamId)
              and (lower(n.title) like :pattern escape '\\'
                   or lower(n.content) like :pattern escape '\\')
            order by n.updatedAt desc, n.id desc
            """)
    List<Note> findAccessible(@Param("userId") Long userId,
                              @Param("teamId") Long teamId,
                              @Param("pattern") String pattern);
}
