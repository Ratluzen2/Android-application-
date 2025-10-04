package com.zafer.smm.data.local

// كتالوج محلي مُستخرج من كود البوت (الأسماء والأسعار كما هي)
// يمكنك لاحقاً ربط زر "طلب الخدمة" بالباكند لإرسال الطلب الفعلي.

data class ServiceEntry(
    val name: String,
    val priceUSD: Double
)

enum class ServiceCategory(val title: String) {
    Followers("قسم المتابعين"),
    Likes("قسم الايكات"),
    Views("قسم المشاهدات"),
    LiveViews("قسم مشاهدات البث المباشر"),
    TikTokScore("قسم رفع سكور تيكتوك"),
    PUBG("قسم شحن شدات ببجي"),
    ITunes("قسم شراء رصيد ايتونز"),
    Telegram("قسم خدمات التليجرام"),
    Ludo("قسم خدمات الودو"),
    Mobile("قسم شراء رصيد الهاتف")
}

// ========== 1) من services_dict (متابعين/لايكات/مشاهدات/مشاهدات بث/رفع سكور) ==========
private val followers = listOf(
    ServiceEntry("متابعين تيكتوك 1k", 3.50),
    ServiceEntry("متابعين تيكتوك 2k", 7.0),
    ServiceEntry("متابعين تيكتوك 3k", 10.50),
    ServiceEntry("متابعين تيكتوك 4k", 14.0),
    ServiceEntry("متابعين انستغرام 1k", 3.0),
    ServiceEntry("متابعين انستغرام 2k", 6.0),
    ServiceEntry("متابعين انستغرام 3k", 9.0),
    ServiceEntry("متابعين انستغرام 4k", 12.0)
)

private val likes = listOf(
    ServiceEntry("لايكات تيكتوك 1k", 1.0),
    ServiceEntry("لايكات تيكتوك 2k", 2.0),
    ServiceEntry("لايكات تيكتوك 3k", 3.0),
    ServiceEntry("لايكات تيكتوك 4k", 4.0),
    ServiceEntry("لايكات انستغرام 1k", 1.0),
    ServiceEntry("لايكات انستغرام 2k", 2.0),
    ServiceEntry("لايكات انستغرام 3k", 3.0),
    ServiceEntry("لايكات انستغرام 4k", 4.0)
)

private val views = listOf(
    ServiceEntry("مشاهدات تيكتوك 1k", 0.10),
    ServiceEntry("مشاهدات تيكتوك 10k", 0.80),
    ServiceEntry("مشاهدات تيكتوك 20k", 1.60),
    ServiceEntry("مشاهدات تيكتوك 30k", 2.40),
    ServiceEntry("مشاهدات تيكتوك 50k", 3.20),
    ServiceEntry("مشاهدات انستغرام 10k", 0.80),
    ServiceEntry("مشاهدات انستغرام 20k", 1.60),
    ServiceEntry("مشاهدات انستغرام 30k", 2.40),
    ServiceEntry("مشاهدات انستغرام 50k", 3.20)
)

private val liveViews = listOf(
    ServiceEntry("مشاهدات بث تيكتوك 1k", 2.0),
    ServiceEntry("مشاهدات بث تيكتوك 2k", 4.0),
    ServiceEntry("مشاهدات بث تيكتوك 3k", 6.0),
    ServiceEntry("مشاهدات بث تيكتوك 4k", 8.0),
    ServiceEntry("مشاهدات بث انستغرام 1k", 2.0),
    ServiceEntry("مشاهدات بث انستغرام 2k", 4.0),
    ServiceEntry("مشاهدات بث انستغرام 3k", 6.0),
    ServiceEntry("مشاهدات بث انستغرام 4k", 8.0)
)

private val tikTokScore = listOf(
    ServiceEntry("رفع سكور بثك1k", 2.0),
    ServiceEntry("رفع سكور بثك2k", 4.0),
    ServiceEntry("رفع سكور بثك3k", 6.0),
    ServiceEntry("رفع سكور بثك10k", 20.0)
)

// ========== 2) من pubg_services ==========
private val pubg = listOf(
    ServiceEntry("ببجي 60 شدة", 2.0),
    ServiceEntry("ببجي 120 شده", 4.0),
    ServiceEntry("ببجي 180 شدة", 6.0),
    ServiceEntry("ببجي 240 شدة", 8.0),
    ServiceEntry("ببجي 325 شدة", 9.0),
    ServiceEntry("ببجي 660 شدة", 15.0),
    ServiceEntry("ببجي 1800 شدة", 40.0)
)

// ========== 3) من itunes_services ==========
private val iTunes = listOf(
    ServiceEntry("شراء رصيد 5 ايتونز", 9.0),
    ServiceEntry("شراء رصيد 10 ايتونز", 18.0),
    ServiceEntry("شراء رصيد 15 ايتونز", 27.0),
    ServiceEntry("شراء رصيد 20 ايتونز", 36.0),
    ServiceEntry("شراء رصيد 25 ايتونز", 45.0),
    ServiceEntry("شراء رصيد 30 ايتونز", 54.0),
    ServiceEntry("شراء رصيد 35 ايتونز", 63.0),
    ServiceEntry("شراء رصيد 40 ايتونز", 72.0),
    ServiceEntry("شراء رصيد 45 ايتونز", 81.0),
    ServiceEntry("شراء رصيد 50 ايتونز", 90.0)
)

// ========== 4) من telegram_services ==========
private val telegram = listOf(
    ServiceEntry("اعضاء قنوات تلي 1k", 3.0),
    ServiceEntry("اعضاء قنوات تلي 2k", 6.0),
    ServiceEntry("اعضاء قنوات تلي 3k", 9.0),
    ServiceEntry("اعضاء قنوات تلي 4k", 12.0),
    ServiceEntry("اعضاء قنوات تلي 5k", 15.0),
    ServiceEntry("اعضاء كروبات تلي 1k", 3.0),
    ServiceEntry("اعضاء كروبات تلي 2k", 6.0),
    ServiceEntry("اعضاء كروبات تلي 3k", 9.0),
    ServiceEntry("اعضاء كروبات تلي 4k", 12.0),
    ServiceEntry("اعضاء كروبات تلي 5k", 15.0)
)

// ========== 5) من ludo_services ==========
private val ludo = listOf(
    ServiceEntry("لودو 810 الماسة", 3.0),
    ServiceEntry("لودو 2280 الماسة", 7.0),
    ServiceEntry("لودو 5080 الماسة", 13.0),
    ServiceEntry("لودو 12750 الماسة", 28.0),
    ServiceEntry("لودو 66680 ذهب", 3.0),
    ServiceEntry("لودو 219500 ذهب", 7.0),
    ServiceEntry("لودو 1443000 ذهب", 13.0),
    ServiceEntry("لودو 3627000 ذهب", 28.0)
)

// ========== 6) رصيد الهاتف ==========
private val mobileRecharge = emptyList<ServiceEntry>() // لم تُذكر قوائم دقيقة داخل كود البوت

// خريطة الكتالوج الكاملة
val Catalog: Map<ServiceCategory, List<ServiceEntry>> = linkedMapOf(
    ServiceCategory.Followers to followers,
    ServiceCategory.Likes to likes,
    ServiceCategory.Views to views,
    ServiceCategory.LiveViews to liveViews,
    ServiceCategory.TikTokScore to tikTokScore,
    ServiceCategory.PUBG to pubg,
    ServiceCategory.ITunes to iTunes,
    ServiceCategory.Telegram to telegram,
    ServiceCategory.Ludo to ludo,
    ServiceCategory.Mobile to mobileRecharge
)
