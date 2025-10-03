package com.zafer.smm.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zafer.smm.data.model.OrderItem

object Prefs {
    private const val FILE = "smm_prefs"
    private const val K_DEVICE_ID = "device_id"
    private const val K_WALLET = "wallet"
    private const val K_ORDERS = "orders_json"
    private const val K_IS_ADMIN = "is_admin"
    private const val K_OWNER_PIN = "owner_pin"

    private lateinit var sp: SharedPreferences
    private val gson = Gson()

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (!sp.contains(K_OWNER_PIN)) {
            sp.edit().putString(K_OWNER_PIN, "0000").apply() // PIN افتراضي للمالك
        }
        if (!sp.contains(K_WALLET)) {
            sp.edit().putString(K_WALLET, "0.0").apply()
        }
        if (!sp.contains(K_ORDERS)) {
            sp.edit().putString(K_ORDERS, "[]").apply()
        }
    }

    fun getDeviceId(): String? = sp.getString(K_DEVICE_ID, null)
    fun setDeviceId(id: String) { sp.edit().putString(K_DEVICE_ID, id).apply() }

    fun getWallet(): Double = sp.getString(K_WALLET, "0.0")!!.toDoubleOrNull() ?: 0.0
    fun setWallet(amount: Double) { sp.edit().putString(K_WALLET, amount.toString()).apply() }
    fun addToWallet(delta: Double) { setWallet(getWallet() + delta) }

    fun isAdmin(): Boolean = sp.getBoolean(K_IS_ADMIN, false)
    fun setAdmin(v: Boolean) { sp.edit().putBoolean(K_IS_ADMIN, v).apply() }

    fun checkPin(pin: String): Boolean = sp.getString(K_OWNER_PIN, "0000") == pin
    fun changePin(newPin: String) { sp.edit().putString(K_OWNER_PIN, newPin).apply() }

    fun getOrders(): List<OrderItem> {
        val json = sp.getString(K_ORDERS, "[]") ?: "[]"
        val type = object : TypeToken<List<OrderItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveOrders(list: List<OrderItem>) {
        val json = gson.toJson(list)
        sp.edit().putString(K_ORDERS, json).apply()
    }

    fun addOrder(order: OrderItem) {
        val cur = getOrders().toMutableList()
        cur.add(0, order) // الأحدث أولاً
        saveOrders(cur)
    }

    fun updateOrderStatus(orderId: Long, newStatus: String?, newCharge: Double?, newRemains: Int?) {
        val cur = getOrders().toMutableList()
        val idx = cur.indexOfFirst { it.orderId == orderId }
        if (idx >= 0) {
            val old = cur[idx]
            cur[idx] = old.copy(
                status = newStatus ?: old.status,
                charge = newCharge ?: old.charge,
                remains = newRemains ?: old.remains
            )
            saveOrders(cur)
        }
    }
}
