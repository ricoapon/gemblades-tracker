package nl.ricoapon.database;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Data access for the {@code game_turn} table. JDBI maps the snake_case columns onto the
 * {@link GameTurn} bean's camelCase properties automatically.
 */
@RegisterBeanMapper(GameTurn.class)
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
    void insert(@BindBean GameTurn turn);

    @SqlUpdate("""
            UPDATE game_turn SET
                money_gained = :moneyGained, money_spent = :moneySpent,
                power_gained = :powerGained, power_spent = :powerSpent,
                fame_gained = :fameGained, fame_spent = :fameSpent,
                voters_gained = :votersGained, voters_spent = :votersSpent,
                starting_deck_size = :startingDeckSize
            WHERE game_id = :gameId AND turn_number = :turnNumber
            """)
    void update(@BindBean GameTurn turn);

    @SqlQuery("SELECT * FROM game_turn WHERE game_id = :gameId ORDER BY turn_number")
    List<GameTurn> findByGameId(@Bind("gameId") String gameId);
}
