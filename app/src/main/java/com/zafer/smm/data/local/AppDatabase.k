package com.zafer.smm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zafer.smm.data.model.*

@Database(
    entities = [
        UserProfileEntity::class,
        OrderEntity::class,
        WalletTransactionEntity::class,
        LeaderboardEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SmmDao
}
