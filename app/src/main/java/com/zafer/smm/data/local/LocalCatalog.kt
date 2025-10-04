package com.zafer.smm.data.local

data class LocalService(val id: Int, val name: String, val price: Double)
data class LocalSection(val key: String, val title: String, val services: List<LocalService>)

object LocalCatalog {
    val sections: List<LocalSection> = listOf(
        LocalSection(
            key = "followers",
            title = "قسم المتابعين",
            services = listOf(
                LocalService(1, "متابعين تيكتوك 100", 1.0),
                LocalService(2, "متابعين تيكتوك 200", 2.0),
                LocalService(3, "متابعين تيكتوك 300", 3.0),
                LocalService(4, "متابعين تيكتوك 400", 4.0),
                LocalService(5, "متابعين تيكتوك 500", 5.0),
                LocalService(6, "متابعين تيكتوك 1000", 9.0),
                LocalService(7, "متابعين تيكتوك 2000", 18.0),
                LocalService(8, "متابعين تيكتوك 3000", 27.0),
                LocalService(9, "متابعين تيكتوك 4000", 36.0),
                LocalService(10, "متابعين تيكتوك 5000", 45.0)
            )
        ),
        LocalSection(
            key = "likes",
            title = "قسم الإعجابات",
            services = listOf(
                LocalService(1, "لايكات 1k", 2.5),
                LocalService(2, "لايكات 2k", 5.0),
                LocalService(3, "لايكات 3k", 7.5),
                LocalService(4, "لايكات 4k", 10.0),
                LocalService(5, "لايكات 5k", 12.5)
            )
        ),
        LocalSection(
            key = "views",
            title = "قسم المشاهدات",
            services = listOf(
                LocalService(1, "مشاهدات تيكتوك 1k", 0.5),
                LocalService(2, "مشاهدات تيكتوك 2k", 1.0),
                LocalService(3, "مشاهدات تيكتوك 3k", 1.5),
                LocalService(4, "مشاهدات تيكتوك 4k", 2.0),
                LocalService(5, "مشاهدات تيكتوك 5k", 2.5),
                LocalService(6, "مشاهدات تيكتوك 10k", 4.5)
            )
        ),
        LocalSection(
            key = "live_views",
            title = "قسم مشاهدات البث المباشر",
            services = listOf(
                LocalService(1, "مشاهدات بث مباشر 1k", 3.0),
                LocalService(2, "مشاهدات بث مباشر 2k", 6.0),
                LocalService(3, "مشاهدات بث مباشر 3k", 9.0),
                LocalService(4, "مشاهدات بث مباشر 4k", 12.0),
                LocalService(5, "مشاهدات بث مباشر 5k", 15.0)
            )
        ),
        LocalSection(
            key = "pubg",
            title = "قسم شحن شدات ببجي",
            services = listOf(
                LocalService(1, "ببجي 60 UC", 1.2),
                LocalService(2, "ببجي 120 UC", 2.3),
                LocalService(3, "ببجي 180 UC", 3.5),
                LocalService(4, "ببجي 240 UC", 4.7),
                LocalService(5, "ببجي 325 UC", 6.0),
                LocalService(6, "ببجي 660 UC", 11.5),
                LocalService(7, "ببجي 1800 UC", 30.0)
            )
        ),
        LocalSection(
            key = "itunes",
            title = "قسم شراء رصيد ايتونز",
            services = listOf(
                LocalService(1, "بطاقة iTunes \$5", 4.9),
                LocalService(2, "بطاقة iTunes \$10", 9.7),
                LocalService(3, "بطاقة iTunes \$15", 14.4),
                LocalService(4, "بطاقة iTunes \$20", 19.0),
                LocalService(5, "بطاقة iTunes \$25", 23.7),
                LocalService(6, "بطاقة iTunes \$50", 47.0)
            )
        ),
        LocalSection(
            key = "telegram",
            title = "قسم خدمات التليجرام",
            services = listOf(
                LocalService(1, "أعضاء قناة 1k", 9.0),
                LocalService(2, "أعضاء قناة 2k", 17.5),
                LocalService(3, "أعضاء قناة 3k", 25.0),
                LocalService(4, "أعضاء كروب 1k", 10.0),
                LocalService(5, "أعضاء كروب 2k", 19.0)
            )
        ),
        LocalSection(
            key = "ludo",
            title = "قسم خدمات اللودو",
            services = listOf(
                LocalService(1, "لودو 100 ألماسة", 0.9),
                LocalService(2, "لودو 200 ألماسة", 1.7),
                LocalService(3, "لودو 500 ألماسة", 4.1),
                LocalService(4, "لودو 1000 ألماسة", 8.0),
                LocalService(5, "لودو 2000 ألماسة", 15.5)
            )
        ),
        LocalSection(
            key = "mobile_recharge",
            title = "قسم شراء رصيد الهاتف",
            services = listOf(
                LocalService(1, "شراء رصيد 2دولار اثير", 2.0),
                LocalService(2, "شراء رصيد 5دولار اثير", 5.0),
                LocalService(3, "شراء رصيد 10دولار اثير", 10.0),
                LocalService(4, "شراء رصيد 20دولار اثير", 20.0),
                LocalService(5, "شراء رصيد 40دولار اثير", 40.0),
                LocalService(6, "شراء رصيد 2دولار اسيا", 2.0),
                LocalService(7, "شراء رصيد 5دولار اسيا", 5.0),
                LocalService(8, "شراء رصيد 10دولار اسيا", 10.0),
                LocalService(9, "شراء رصيد 20دولار اسيا", 20.0),
                LocalService(10, "شراء رصيد 40دولار اسيا", 40.0),
                LocalService(11, "شراء رصيد 2دولار كورك", 2.0),
                LocalService(12, "شراء رصيد 5دولار كورك", 5.0),
                LocalService(13, "شراء رصيد 10دولار كورك", 10.0),
                LocalService(14, "شراء رصيد 20دولار كورك", 20.0),
                LocalService(15, "شراء رصيد 40دولار كورك", 40.0)
            )
        )
    )
}
