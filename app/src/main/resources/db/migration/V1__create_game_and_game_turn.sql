CREATE TABLE game (
    id         TEXT    NOT NULL PRIMARY KEY,
    run_id     TEXT    NOT NULL,
    finished   INTEGER NOT NULL,
    won        INTEGER,
    started_at TEXT    NOT NULL,
    ended_at   TEXT
);

CREATE TABLE game_turn (
    game_id            TEXT    NOT NULL,
    turn_number        INTEGER NOT NULL,
    money_gained       INTEGER NOT NULL,
    money_spent        INTEGER NOT NULL,
    power_gained       INTEGER NOT NULL,
    power_spent        INTEGER NOT NULL,
    fame_gained        INTEGER NOT NULL,
    fame_spent         INTEGER NOT NULL,
    voters_gained      INTEGER NOT NULL,
    voters_spent       INTEGER NOT NULL,
    starting_deck_size INTEGER NOT NULL,
    PRIMARY KEY (game_id, turn_number),
    FOREIGN KEY (game_id) REFERENCES game (id)
);
