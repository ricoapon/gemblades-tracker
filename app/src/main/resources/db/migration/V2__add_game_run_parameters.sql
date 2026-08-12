-- Run parameters reported by the game when a run starts. Existing rows predate these columns, so
-- they default to 0 / false (0).
ALTER TABLE game ADD COLUMN difficulty      INTEGER NOT NULL DEFAULT 0;
ALTER TABLE game ADD COLUMN length          INTEGER NOT NULL DEFAULT 0;
ALTER TABLE game ADD COLUMN required_voters INTEGER NOT NULL DEFAULT 0;
ALTER TABLE game ADD COLUMN gauntlet        INTEGER NOT NULL DEFAULT 0;
