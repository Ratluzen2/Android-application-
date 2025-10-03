package com.zafer.smm.data

import com.zafer.smm.data.local.SmmDao
import com.zafer.smm.data.model.*
import com.zafer.smm.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmmRepository(private val dao: SmmDao) {

    // ======= خريطة الخدمات والأسعار من منطق البوت (أمثلة رئيسية) =======
    private val serviceApiMapping: Map<String, Pair<Int, Int>> = mapOf(
        "متابعين تيكتوك 1k" to (13912 to 1000),
        "متابعين تيكتوك 2k" to (13912 to 2000),
        "متابعين تيكتوك 3k" to (13912 to 3000),
        "متابعين تيكتوك 4k" to (13912 to 4000),
        "مشاهدات تيكتوك 1k"  to (9447 to 1000),
        "مشاهدات تيكتوك 10k" to (9543 to 10000),
        "مشاهدات تيكتوك 20k" to (9543 to 20000),
        "مشاهدات تيكتوك 30k" to (9543 to 30000),
        "مشاهدات تيكتوك 50k" to (9543 to 50000),
        "متابعين انستغرام 1k" to (13788 to 1000),
        "متابعين انستغرام 2k" to (13788 to 2000),
        "لايكات تيكتوك 1k" to (12320 to 1000),
        "لايكات انستغرام 1k" to (7973 to 1000),
        "مشاهدات انستغرام 10k" to (13531 to 10000)
        // أضف بقية القوائم بنفس النسق إن رغبت بتوسيعها أكثر
    )

    private val servicesDict: Map<String, Double> = mapOf(
        "متابعين تيكتوك 1k" to 3.50,
        "متابعين تيكتوك 2k" to 7.0,
        "متابعين تيكتوك 3k" to 10.50,
        "متابعين تيكتوك 4k" to 14.0,
        "مشاهدات تيكتوك 1k" to 0.10,
        "مشاهدات تيكتوك 10k" to 0.80,
        "مشاهدات تيكتوك 20k" to 1.60,
        "مشاهدات تيكتوك 30k" to 2.40,
        "مشاهدات تيكتوك 50k" to 3.20,
        "متابعين انستغرام 1k" to 3.0,
        "متابعين انستغرام 2k" to 6.0,
        "لايكات تيكتوك 1k" to 1.0,
        "لايكات انستغرام 1k" to 1.0,
        "مشاهدات انستغرام 10k" to 0.80
    )

    suspend fun buildLocalCatalog(): List<LocalMappedService> = withContext(Dispatchers.Default) {
        servicesDict.map { (name, price) ->
            val map = serviceApiMapping[name]
            LocalMappedService(
                displayName = name,
                serviceId = map?.first,
                quantityMultiplier = map?.second,
                priceUsd = price
            )
        }
    }

    // ======= Users & Wallet =======
    suspend fun ensureUser(deviceId: String, ownerDeviceId: String, adminPin: String) {
        withContext(Dispatchers.IO) {
            val u = dao.getUser(deviceId)
            if (u == null) {
                val isOwner = (deviceId == ownerDeviceId)
                dao.insertUser(UserProfileEntity(deviceId = deviceId, balance = 0.0, isOwner = isOwner))
            }
        }
    }

    suspend fun getUser(deviceId: String) = withContext(Dispatchers.IO) { dao.getUser(deviceId) }

    suspend fun topup(deviceId: String, amount: Double, note: String = "topup") {
        withContext(Dispatchers.IO) {
            val u = dao.getUser(deviceId) ?: return@withContext
            dao.updateBalance(deviceId, (u.balance + amount))
            dao.insertWalletTx(WalletTransactionEntity(deviceId = deviceId, amount = amount, type = "topup", note = note))
        }
    }

    suspend fun spend(deviceId: String, amount: Double, note: String = "order") {
        withContext(Dispatchers.IO) {
            val u = dao.getUser(deviceId) ?: return@withContext
            dao.updateBalance(deviceId, (u.balance - amount))
            dao.insertWalletTx(WalletTransactionEntity(deviceId = deviceId, amount = -amount, type = "order", note = note))
            val cur = dao.topLeaders().associateBy { it.deviceId }.toMutableMap()
            val prev = cur[deviceId]?.totalSpent ?: 0.0
            dao.upsertLeader(LeaderboardEntryEntity(deviceId, totalSpent = prev + amount))
        }
    }

    suspend fun wallet(deviceId: String) = withContext(Dispatchers.IO) { dao.wallet(deviceId) }
    suspend fun myOrders(deviceId: String) = withContext(Dispatchers.IO) { dao.myOrders(deviceId) }
    suspend fun leaders() = withContext(Dispatchers.IO) { dao.topLeaders() }

    // ======= kd1s API =======
    suspend fun placeOrder(deviceId: String, name: String, link: String, quantity: Int): Pair<Long?, String?> {
        val mapping = serviceApiMapping[name] ?: return 0L to "Service not mapped"
        val (serviceId, mult) = mapping
        val unit = servicesDict[name] ?: return 0L to "Price not found"
        val blocks = (quantity.toDouble() / mult.toDouble())
        val price = unit * blocks

        return withContext(Dispatchers.IO) {
            val user = dao.getUser(deviceId) ?: return@withContext 0L to "No user"
            if (user.balance < price) return@withContext 0L to "رصيد غير كافٍ"

            val res = ApiService.api.add(ApiService.API_KEY, service = serviceId, link = link, quantity = quantity)
            val remoteId = res.order
            // خصم الرصيد وتسجيل الطلب محليًا
            spend(deviceId, price, note = "order:$name")
            val localId = dao.insertOrder(
                OrderEntity(
                    deviceId = deviceId,
                    remoteOrderId = remoteId,
                    serviceId = serviceId,
                    name = name,
                    link = link,
                    quantity = quantity,
                    priceUsd = price,
                    status = if (remoteId != null) "Placed" else "Failed"
                )
            )
            (remoteId ?: localId) to res.error
        }
    }

    suspend fun checkStatus(orderId: Long): StatusResponse =
        withContext(Dispatchers.IO) { ApiService.api.status(ApiService.API_KEY, order = orderId) }

    suspend fun fetchBalanceFromProvider(): BalanceResponse =
        withContext(Dispatchers.IO) { ApiService.api.balance(ApiService.API_KEY) }
}
