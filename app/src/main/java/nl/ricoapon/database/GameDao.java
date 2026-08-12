package nl.ricoapon.database;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the {@code game} table. JDBI maps the snake_case columns onto the
 * {@link Game} bean's camelCase properties automatically.
 */
@RegisterBeanMapper(Game.class)
public interface GameDao {
    @SqlUpdate("""
            INSERT INTO game (id, run_id, finished, won, started_at, ended_at,
                              difficulty, length, required_voters, gauntlet)
            VALUES (:id, :runId, :finished, :won, :startedAt, :endedAt,
                    :difficulty, :length, :requiredVoters, :gauntlet)
            """)
    void insert(@BindBean Game game);

    @SqlUpdate("""
            UPDATE game
            SET finished = :finished, won = :won, ended_at = :endedAt
            WHERE id = :id
            """)
    void update(@BindBean Game game);

    @SqlQuery("""
            SELECT g.*, (SELECT COUNT(*) FROM game_turn t WHERE t.game_id = g.id) AS nr_of_turns
            FROM game g
            WHERE g.id = :id
            """)
    Optional<Game> findById(@Bind("id") String id);

    @SqlQuery("""
            SELECT g.*, (SELECT COUNT(*) FROM game_turn t WHERE t.game_id = g.id) AS nr_of_turns
            FROM game g
            ORDER BY g.started_at
            """)
    List<Game> findAll();
}
