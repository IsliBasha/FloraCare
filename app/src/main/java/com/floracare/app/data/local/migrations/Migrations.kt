package com.floracare.app.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: extend the species table with provider/cache metadata so Perenual
 * responses can be cached in place of synth rows.
 *
 *  - `provider` — "local" (seeded or user-created) vs "perenual" (fetched)
 *  - `providerSpeciesId` — the remote numeric id as a String; indexed
 *  - `fetchedAt` — epoch-ms of last successful remote refresh (for SWR TTL)
 *  - `family`/`genus` — optional taxonomy from Perenual
 *  - `imageUrl` — Perenual's default image, used as a fallback hero when the
 *    user hasn't set a cover photo
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE species ADD COLUMN provider TEXT NOT NULL DEFAULT 'local'",
        )
        db.execSQL("ALTER TABLE species ADD COLUMN providerSpeciesId TEXT")
        db.execSQL("ALTER TABLE species ADD COLUMN fetchedAt INTEGER")
        db.execSQL("ALTER TABLE species ADD COLUMN family TEXT")
        db.execSQL("ALTER TABLE species ADD COLUMN genus TEXT")
        db.execSQL("ALTER TABLE species ADD COLUMN imageUrl TEXT")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_species_providerSpeciesId " +
                "ON species(providerSpeciesId)",
        )
    }
}
