package com.zafer.smm.data.model

// استجابات kd1s القياسية
data class ServiceItem(
    val service: Int? = null,
    val name: String? = null,
    val rate: Double? = null, // السعر لكل 1000 غالبًا
    val min: Int? = null,
    val max: Int? = null,
    val category: String? = null
)

data class AddOrderResponse(
    val order: Long? = null,
    val error: String? = null
)

data class StatusResponse(
    val status: String? = null,
    val remains: String? = null,
    val charge: Double? = null,
    val error: String? = null
)

data class BalanceResponse(
    val balance: Double? = null,
    val currency: String? = null,
    val error: String? = null
)

// كتالوج موحّد لعرض (اسم/ID/مضاعف/سعر) من منطق البوت
data class LocalMappedService(
    val displayName: String,
    val serviceId: Int?,          // null = خدمة يدوية/عرض فقط
    val quantityMultiplier: Int?, // مثل 1000 / 10000 ...
    val priceUsd: Double?
)

// ============ كيانات Room ============
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val deviceId: String,
    val balance: Double = 0.0,
    val isOwner: Boolean = false
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val deviceId: String,
    val remoteOrderId: Long? = null,
    val serviceId: Int? = null,
    val name: String,
    val link: String,
    val quantity: Int,
    val priceUsd: Double,
    val status: String = "Pending",
    val ts: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val amount: Double,         // + للإيداع / - للخصم
    val type: String,           // "topup" / "order" / "gift" ...
    val note: String? = null,
    val ts: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaders")
data class LeaderboardEntryEntity(
    @PrimaryKey val deviceId: String,
    val totalSpent: Double = 0.0,
    val nameHint: String? = null
)
