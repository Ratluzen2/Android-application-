package com.zafer.smm.data.local

import androidx.room.*
import com.zafer.smm.data.model.*

@Dao
interface SmmDao {

    // Users
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(u: UserProfileEntity)

    @Query("SELECT * FROM users WHERE deviceId=:deviceId")
    suspend fun getUser(deviceId: String): UserProfileEntity?

    @Query("UPDATE users SET balance = :balance WHERE deviceId = :deviceId")
    suspend fun updateBalance(deviceId: String, balance: Double)

    // Orders
    @Insert
    suspend fun insertOrder(o: OrderEntity): Long

    @Query("SELECT * FROM orders WHERE deviceId=:deviceId ORDER BY ts DESC")
    suspend fun myOrders(deviceId: String): List<OrderEntity>

    @Query("UPDATE orders SET status = :status, remoteOrderId = :remoteId WHERE localId = :localId")
    suspend fun updateOrderStatus(localId: Long, status: String, remoteId: Long?)

    // Wallet
    @Insert
    suspend fun insertWalletTx(tx: WalletTransactionEntity)

    @Query("SELECT * FROM wallet WHERE deviceId=:deviceId ORDER BY ts DESC")
    suspend fun wallet(deviceId: String): List<WalletTransactionEntity>

    // Leaders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeader(entry: LeaderboardEntryEntity)

    @Query("SELECT * FROM leaders ORDER BY totalSpent DESC LIMIT 50")
    suspend fun topLeaders(): List<LeaderboardEntryEntity>
}
