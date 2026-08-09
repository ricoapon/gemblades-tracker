package nl.ricoapon.database;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Data access for the {@code game_turn} table. JDBI maps the snake_case columns onto the
 * {@link GameTurn} record's camelCase components automatically.
 */
@RegisterConstructorMapper(GameTurn.class)
public interface GameTurnDao {
    @SqlUpdate("""
            INSERT INTO game_turn (
                game_id, turn_number,
                money_gained, money_spent,
                power_gained, power_spent,
                fame_gained, fame_spent,
                voters_gained, voters_spent,
                starting_deck_size
            ) VALUES (
                :gameId, :turnNumber,
                :moneyGained, :moneySpent,
                :powerGained, :powerSpent,
                :fameGained, :fameSpent,
                :votersGained, :votersSpent,
                :startingDeckSize
            )
            """)
    void insert(@BindMethods GameTurn turn);

    @SqlQuery("SELECT * FROM game_turn WHERE game_id = :gameId ORDER BY turn_number")
    List<GameTurn> findByGameId(@Bind("gameId") String gameId);
}
