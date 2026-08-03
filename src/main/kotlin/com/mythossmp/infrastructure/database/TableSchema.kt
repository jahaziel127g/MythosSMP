package com.mythossmp.infrastructure.database

object TableSchema {
    const val PLAYERS_TABLE = """
        CREATE TABLE IF NOT EXISTS mythos_players (
            player_uuid VARCHAR(36) PRIMARY KEY NOT NULL,
            last_known_name VARCHAR(16) NOT NULL,
            active_vow VARCHAR(32) DEFAULT NULL,
            cooldown_multiplier REAL DEFAULT 1.0,
            ares_rage INTEGER DEFAULT 0,
            hades_charges INTEGER DEFAULT 0,
            zeus_charge REAL DEFAULT 0.0,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    const val TRUSTS_TABLE = """
        CREATE TABLE IF NOT EXISTS mythos_trusts (
            player_uuid VARCHAR(36) NOT NULL,
            trusted_uuid VARCHAR(36) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (player_uuid, trusted_uuid),
            FOREIGN KEY (player_uuid) REFERENCES mythos_players(player_uuid) ON DELETE CASCADE
        )
    """

    const val UNIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS mythos_unions (
            union_id VARCHAR(36) PRIMARY KEY NOT NULL,
            party_a_uuid VARCHAR(36) NOT NULL UNIQUE,
            party_b_uuid VARCHAR(36) NOT NULL UNIQUE,
            active INTEGER DEFAULT 1,
            creation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (party_a_uuid) REFERENCES mythos_players(player_uuid) ON DELETE CASCADE,
            FOREIGN KEY (party_b_uuid) REFERENCES mythos_players(player_uuid) ON DELETE CASCADE
        )
    """

    const val PLAYER_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_players_uuid ON mythos_players(player_uuid)
    """

    const val TRUST_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_trust_player ON mythos_trusts(player_uuid)
    """

    const val COMPLETED_TRIALS_TABLE = """
        CREATE TABLE IF NOT EXISTS mythos_completed_trials (
            player_uuid VARCHAR(36) NOT NULL,
            trial_key VARCHAR(32) NOT NULL,
            completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (player_uuid, trial_key),
            FOREIGN KEY (player_uuid) REFERENCES mythos_players(player_uuid) ON DELETE CASCADE
        )
    """

    const val UNION_INDEX = """
        CREATE INDEX IF NOT EXISTS idx_unions_lookup ON mythos_unions(party_a_uuid, party_b_uuid)
    """
}
