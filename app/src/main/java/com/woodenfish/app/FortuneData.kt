package com.woodenfish.app

import kotlin.random.Random

/**
 * 求签数据：5 级（下下 / 下 / 中 / 上 / 上上），每级 3 支签。
 * 签诗为繁体竖排（楷书字体），等级大字用瘦金体（金色）。
 */
data class FortuneStick(
    val level: Int,              // 0=下下签 1=下签 2=中签 3=上签 4=上上签
    val poemTW: String,          // 繁体签诗，每句用 \n 分隔
    val poemEN: String,          // 英文签诗，每句用 \n 分隔
)

object FortuneData {
    const val LEVEL_COUNT = 5
    /** 等级名（简体） */
    val levelNamesCN = listOf("下下签", "下签", "中签", "上签", "上上签")
    /** 等级名（繁体） */
    val levelNamesTW = listOf("下下籤", "下籤", "中籤", "上籤", "上上籤")
    /** 等级名（英文） */
    val levelNamesEN = listOf("Worst Fortune", "Poor Fortune", "Fair Fortune", "Good Fortune", "Great Fortune")
    /** 签面大字（瘦金体渲染，避免使用字体缺失的繁体字） */
    val levelCoreCN = listOf("下下", "下", "中", "上", "上上")
    val levelCoreTW = listOf("下下", "下", "中", "上", "上上")
    val levelCoreEN = listOf("Worst", "Poor", "Fair", "Good", "Great")

    /** 解曰（每级统一） */
    val meaningCN = listOf(
        "诸事不利，静待时机。",
        "时运不济，宜守不宜攻。",
        "顺其自然，平淡是福。",
        "努力终有回报，渐入佳境。",
        "所求皆遂，万事亨通。",
    )
    val meaningTW = listOf(
        "諸事不利，靜待時機。",
        "時運不濟，宜守不宜攻。",
        "順其自然，平淡是福。",
        "努力終有回報，漸入佳境。",
        "所求皆遂，萬事亨通。",
    )
    val meaningEN = listOf(
        "Things are against you now. Wait for the right time.",
        "Luck is low — hold steady, do not push.",
        "Go with the flow; peace itself is fortune.",
        "Your efforts will pay off — things are improving.",
        "All wishes granted — everything goes well.",
    )

    val sticks: List<FortuneStick> = listOf(
        // ── 上上签 ──
        FortuneStick(4,
            "春風得意馬蹄輕\n一舉成名天下聞\n莫道前路無知己\n錦繡前程步步新",
            "Spring breeze carries the horse's swift steps\nfame spreads across the land overnight\nFear not that no one knows your path\na splendid future unfolds step by step"),
        FortuneStick(4,
            "枯木逢春再發芽\n柳暗花明又一村\n金榜題名終有日\n龍門一躍上青雲",
            "Dead wood sprouts anew in spring\nbeyond dark willows, another village blooms\nThe day of the golden list will come\none leap over the dragon gate to the clouds"),
        FortuneStick(4,
            "花開富貴滿堂春\n紫氣東來福入門\n事事順心皆如意\n家和業旺樂天倫",
            "Wealth and honor fill the hall in spring\npurple air from the east brings fortune to the door\nAll that you wish for comes to pass\nfamily harmony, thriving joy"),
        // ── 上签 ──
        FortuneStick(3,
            "寶劍鋒從磨礪出\n梅花香自苦寒來\n今朝且把憂煩散\n明日東風送暖回",
            "A sharp blade is honed by grinding\nplum blossoms bloom from bitter cold\nCast aside today's worries\ntomorrow's east wind brings warmth again"),
        FortuneStick(3,
            "雲開月出見天明\n柳暗花明路自平\n守得本心常不動\n功成不必問前程",
            "Clouds part, the moon shines, the sky is clear\nwhere willows darken and flowers bloom, the road levels itself\nHold fast to your heart and stay unmoved\nsuccess comes without asking the future"),
        FortuneStick(3,
            "輕舟已過萬重山\n風雨歸來見彩虹\n舊事隨風皆散去\n新程萬里任君行",
            "The light boat has passed ten thousand mountains\nthrough wind and rain, a rainbow returns\nOld troubles scatter with the wind\na new journey of ten thousand miles awaits your steps"),
        // ── 中签 ──
        FortuneStick(2,
            "凡事隨緣莫強求\n花開花落自有時\n守得初心常在念\n福報綿綿自有期",
            "Do not force what comes\nflowers bloom and fall in their own time\nKeep your original heart\ngood fortune will come in its own season"),
        FortuneStick(2,
            "迷霧重重不見山\n且行且看莫心煩\n時機未到休急躁\n靜待春風綠滿川",
            "Fog upon fog hides the mountains\nwalk on and look, do not fret\nThe time is not yet — wait patiently\nspring wind will green the plains"),
        FortuneStick(2,
            "半是晴天半是陰\n人生得意且徐行\n今朝種下菩提樹\n他日蔭涼自在心",
            "Half clear sky, half overcast\nwalk steadily through life's ups and downs\nPlant the bodhi tree today\ntomorrow its shade will cool your heart"),
        // ── 下签 ──
        FortuneStick(1,
            "逆水行舟用力撐\n一篙鬆勁退千尋\n此時宜守不宜進\n靜待東風再啟程",
            "Rowing upstream takes all your strength\none loose stroke and you drift back a thousand spans\nNow is the time to hold, not advance\nwait for the east wind to set sail again"),
        FortuneStick(1,
            "迷霧遮攔前行路\n行事宜當再三思\n忍得一時心頭氣\n免得百日身後憂",
            "Mist veils the road ahead\nthink thrice before you act\nEndure a moment of anger\nsave yourself a hundred days of worry"),
        FortuneStick(1,
            "風雨欲來風滿樓\n未雨綢繆早綢繆\n眼前雖有千般事\n步步為營莫出頭",
            "Wind and rain are brewing in the tower\nprepare your shelter before the storm\nThough a thousand matters press upon you\nstep carefully and stay low"),
        // ── 下下签 ──
        FortuneStick(0,
            "屋漏偏逢連夜雨\n行船又遇頂頭風\n此際宜潛龍勿用\n韜光養晦待天明",
            "The leaking roof meets nights of rain\nthe sailing boat meets headwinds again\nNow is the time to lie low\nbide your strength and wait for dawn"),
        FortuneStick(0,
            "前路崎嶇多坎坷\n行事宜當步步慎\n一失足成千古恨\n再回首已百年身",
            "The road ahead is rough and rugged\nwalk with great care at every step\nOne misstep becomes a thousand years of regret\nlooking back, a hundred years have passed"),
        FortuneStick(0,
            "黑雲壓城城欲摧\n進退維谷步步難\n且將心火暫壓下\n寒冬過盡自逢春",
            "Black clouds press the city, ready to crumble\nevery path is blocked, advance and retreat alike\nCalm the fire in your heart for now\nwhen winter ends, spring will come"),
    )

    fun draw(): FortuneStick = sticks[Random.nextInt(sticks.size)]
}
