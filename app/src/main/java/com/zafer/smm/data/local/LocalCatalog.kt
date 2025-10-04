package com.zafer.smm.data.local

data class LocalService(val id: Int, val name: String, val price: Double)
data class LocalSection(val key: String, val title: String, val services: List<LocalService>)

object LocalCatalog {
    val sections: List<LocalSection> = listOf(
        LocalSection(key = "followers", title = "قسم المتابعين", services = listOf(
            LocalService(id = 1, name = "متابعين تيكتوك 100", price = 1.0),
            LocalService(id = 2, name = "متابعين تيكتوك 200", price = 2.0),
            LocalService(id = 3, name = "متابعين تيكتوك 300", price = 3.0),
            LocalService(id = 4, name = "متابعين تيكتوك 400", price = 4.0),
            LocalService(id = 5, name = "متابعين تيكتوك 500", price = 5.0),
            LocalService(id = 6, name = "متابعين تيكتوك 1000", price = 9.0),
            LocalService(id = 7, name = "متابعين تيكتوك 2000", price = 18.0),
            LocalService(id = 8, name = "متابعين تيكتوك 3000", price = 27.0),
            LocalService(id = 9, name = "متابعين تيكتوك 4000", price = 36.0),
            LocalService(id = 10, name = "متابعين تيكتوك 5000", price = 45.0)
        )),
        LocalSection(key = "likes", title = "قسم الإعجابات", services = listOf(
            LocalService(id = 1, name = "لايكات 1k", price = 2.5),
            LocalService(id = 2, name = "لايكات 2k", price = 5.0),
            LocalService(id = 3, name = "لايكات 3k", price = 7.5),
            LocalService(id = 4, name = "لايكات 4k", price = 10.0),
            LocalService(id = 5, name = "لايكات 5k", price = 12.5)
        )),
        LocalSection(key = "views", title = "قسم المشاهدات", services = listOf(
            LocalService(id = 1, name = "مشاهدات تيكتوك 1k", price = 0.5),
            LocalService(id = 2, name = "مشاهدات تيكتوك 2k", price = 1.0),
            LocalService(id = 3, name = "مشاهدات تيكتوك 3k", price = 1.5),
            LocalService(id = 4, name = "مشاهدات تيكتوك 4k", price = 2.0),
            LocalService(id = 5, name = "مشاهدات تيكتوك 5k", price = 2.5),
            LocalService(id = 6, name = "مشاهدات تيكتوك 10k", price = 4.5)
        )),
        LocalSection(key = "live_views", title = "قسم مشاهدات البث المباشر", services = listOf(
            LocalService(id = 1, name = "مشاهدات بث مباشر 1k", price = 3.0),
            LocalService(id = 2, name = "مشاهدات بث مباشر 2k", price = 6.0),
            LocalService(id = 3, name = "مشاهدات بث مباشر 3k", price = 9.0),
            LocalService(id = 4, name = "مشاهدات بث مباشر 4k", price = 12.0),
            LocalService(id = 5, name = "مشاهدات بث مباشر 5k", price = 15.0)
        )),
        LocalSection(key = "pubg", title = "قسم شحن شدات ببجي", services = listOf(
            LocalService(id = 1, name = "ببجي 60 UC", price = 1.2),
            LocalService(id = 2, name = "ببجي 120 UC", price = 2.3),
            LocalService(id = 3, name = "ببجي 180 UC", price = 3.5),
            LocalService(id = 4, name = "ببجي 240 UC", price = 4.7),
            LocalService(id = 5, name = "ببجي 325 UC", price = 6.0),
            LocalService(id = 6, name = "ببجي 660 UC", price = 11.5),
            LocalService(id = 7, name = "ببجي 1800 UC", price = 30.0)
        )),
        LocalSection(key = "itunes", title = "قسم شراء رصيد ايتونز", services = listOf(
            LocalService(id = 1, name = "بطاقة iTunes $5", price = 4.9),
            LocalService(id = 2, name = "بطاقة iTunes $10", price = 9.7),
            LocalService(id = 3, name = "بطاقة iTunes $15", price = 14.4),
            LocalService(id = 4, name = "بطاقة iTunes $20", price = 19.0),
            LocalService(id = 5, name = "بطاقة iTunes $25", price = 23.7),
            LocalService(id = 6, name = "بطاقة iTunes $50", price = 47.0)
        )),
        LocalSection(key = "telegram", title = "قسم خدمات التليجرام", services = listOf(
            LocalService(id = 1, name = "أعضاء قناة 1k", price = 9.0),
            LocalService(id = 2, name = "أعضاء قناة 2k", price = 17.5),
            LocalService(id = 3, name = "أعضاء قناة 3k", price = 25.0),
            LocalService(id = 4, name = "أعضاء كروب 1k", price = 10.0),
            LocalService(id = 5, name = "أعضاء كروب 2k", price = 19.0)
        )),
        LocalSection(key = "ludo", title = "قسم خدمات اللودو", services = listOf(
            LocalService(id = 1, name = "لودو 100 ألماسة", price = 0.9),
            LocalService(id = 2, name = "لودو 200 ألماسة", price = 1.7),
            LocalService(id = 3, name = "لودو 500 ألماسة", price = 4.1),
            LocalService(id = 4, name = "لودو 1000 ألماسة", price = 8.0),
            LocalService(id = 5, name = "لودو 2000 ألماسة", price = 15.5)
        )),
        LocalSection(key = "mobile_recharge", title = "قسم شراء رصيد الهاتف", services = listOf(
            LocalService(id = 1, name = "شراء رصيد 2دولار اثير", price = 2.0),
            LocalService(id = 2, name = "شراء رصيد 5دولار اثير", price = 5.0),
            LocalService(id = 3, name = "شراء رصيد 10دولار اثير", price = 10.0),
            LocalService(id = 4, name = "شراء رصيد 20دولار اثير", price = 20.0),
            LocalService(id = 5, name = "شراء رصيد 40دولار اثير", price = 40.0),
            LocalService(id = 6, name = "شراء رصيد 2دولار اسيا", price = 2.0),
            LocalService(id = 7, name = "شراء رصيد 5دولار اسيا", price = 5.0),
            LocalService(id = 8, name = "شراء رصيد 10دولار اسيا", price = 10.0),
            LocalService(id = 9, name = "شراء رصيد 20دولار اسيا", price = 20.0),
            LocalService(id = 10, name = "شراء رصيد 40دولار اسيا", price = 40.0),
            LocalService(id = 11, name = "شراء رصيد 2دولار كورك", price = 2.0),
            LocalService(id = 12, name = "شراء رصيد 5دولار كورك", price = 5.0),
            LocalService(id = 13, name = "شراء رصيد 10دولار كورك", price = 10.0),
            LocalService(id = 14, name = "شراء رصيد 20دولار كورك", price = 20.0),
            LocalService(id = 15, name = "شراء رصيد 40دولار كورك", price = 40.0)
        )),
    )
}
