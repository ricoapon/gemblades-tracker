package nl.ricoapon.database;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Types;
import java.time.Instant;

/**
 * Entry point for the local SQLite database. Constructing a {@code Database} opens (creating it if
 * necessary) the SQLite file at the given path, runs any pending Flyway migrations so the schema is
 * up to date, and exposes typed DAOs for querying.
 *
 * <p>Timestamps are stored as ISO-8601 text and mapped to/from {@link Instant}; foreign key
 * enforcement (off by default in SQLite) is enabled on every connection.
 */
public class Database {
    private final Jdbi jdbi;

    public Database(Path dbPath) {
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        // Every connection enforces foreign keys, which SQLite disables by default.
        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.enforceForeignKeys(true);
        SQLiteDataSource dataSource = new SQLiteDataSource(sqliteConfig);
        dataSource.setUrl(url);

        // Run migrations first so the schema exists the moment the database is first opened.
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        this.jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        registerInstantMapping(jdbi);
    }

    /**
     * Registers bidirectional mapping between {@link Instant} and ISO-8601 {@code TEXT}, keeping
     * timestamp storage explicit rather than relying on the driver's timestamp handling.
     */
    private static void registerInstantMapping(Jdbi jdbi) {
        jdbi.registerColumnMapper(Instant.class, (rs, columnNumber, ctx) -> {
            String value = rs.getString(columnNumber);
            return value == null ? null : Instant.parse(value);
        });
        jdbi.registerArgument(new AbstractArgumentFactory<Instant>(Types.VARCHAR) {
            @Override
            protected Argument build(Instant value, ConfigRegistry config) {
                return (position, statement, ctx) -> {
                    if (value == null) {
                        statement.setNull(position, Types.VARCHAR);
                    } else {
                        statement.setString(position, value.toString());
                    }
                };
            }
        });
    }

    /**
     * The underlying JDBI instance, for callers that need direct access.
     */
    public Jdbi jdbi() {
        return jdbi;
    }

    public GameDao gameDao() {
        return jdbi.onDemand(GameDao.class);
    }

    public GameTurnDao gameTurnDao() {
        return jdbi.onDemand(GameTurnDao.class);
    }
}
