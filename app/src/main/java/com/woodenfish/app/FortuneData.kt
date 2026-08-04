package com.woodenfish.app

import kotlin.random.Random

/**
 * 求签数据：5 级（下下 / 下 / 中 / 上 / 上上），每级 10 支签，共 50 支。
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
        // ── 上上签（10）──
        FortuneStick(4,
            "春風得意馬蹄輕\n一舉成名天下聞\n莫道前路無知己\n錦繡前程步步新",
            "Spring breeze carries the horse's swift steps\nfame spreads across the land overnight\nFear not that no one knows your path\na splendid future unfolds step by step"),
        FortuneStick(4,
            "枯木逢春再發芽\n柳暗花明又一村\n金榜題名終有日\n龍門一躍上青雲",
            "Dead wood sprouts anew in spring\nbeyond dark willows, another village blooms\nThe day of the golden list will come\none leap over the dragon gate to the clouds"),
        FortuneStick(4,
            "花開富貴滿堂春\n紫氣東來福入門\n事事順心皆如意\n家和業旺樂天倫",
            "Wealth and honor fill the hall in spring\npurple air from the east brings fortune to the door\nAll that you wish for comes to pass\nfamily harmony, thriving joy"),
        FortuneStick(4,
            "紫氣東來祥雲繞\n金玉滿堂福星照\n心想事成皆如意\n鵬程萬里步步高",
            "Purple clouds drift in from the east\nfortune's star shines on halls of gold\nWhatever you wish comes true\nthe roc's journey climbs step by step"),
        FortuneStick(4,
            "龍騰虎躍正當時\n風雲際會展宏圖\n千磨萬擊還堅勁\n直掛雲帆濟滄海",
            "The dragon soars, the tiger leaps — now is the time\nmeet the moment and spread your grand plan\nGround by a thousand trials, still firm\nhoist the sail and cross the great sea"),
        FortuneStick(4,
            "久旱逢甘霖\n他鄉遇故知\n洞房花燭夜\n金榜題名時",
            "Rain after long drought\na dear friend met abroad\nThe wedding night's candles\nthe golden list's name"),
        FortuneStick(4,
            "月滿西樓花滿枝\n東風送暖入春池\n良辰美景君須記\n正是揚帆起航時",
            "The moon fills the western tower, blossoms fill the boughs\nthe east wind warms the spring pond\nRemember this fine hour\nit is time to set sail"),
        FortuneStick(4,
            "寶馬香車迎貴人\n錦上添花又一春\n前程似錦風光好\n一路高歌入青雲",
            "Fine horses greet your patron\nbrocade gains another bloom\nA future bright as silk\nsing your way into the clouds"),
        FortuneStick(4,
            "泰山可倚福星臨\n萬事順遂樂無憂\n家和業興人安康\n笑口常開福自來",
            "A mountain to lean on, a lucky star overhead\nall things go smoothly, joy without worry\nFamily at peace, career thriving, health secure\nsmile wide and fortune comes"),
        FortuneStick(4,
            "天時地利人和備\n水到渠成事竟成\n更上一層樓外樓\n俯瞰群山小天下",
            "The time, the place, the people all align\nwhen water flows, the channel forms, and deeds are done\nClimb one more story above the tower\nlook down on the mountains, and the world grows small"),
        // ── 上签（10）──
        FortuneStick(3,
            "寶劍鋒從磨礪出\n梅花香自苦寒來\n今朝且把憂煩散\n明日東風送暖回",
            "A sharp blade is honed by grinding\nplum blossoms bloom from bitter cold\nCast aside today's worries\ntomorrow's east wind brings warmth again"),
        FortuneStick(3,
            "雲開月出見天明\n柳暗花明路自平\n守得本心常不動\n功成不必問前程",
            "Clouds part, the moon shines, the sky is clear\nwhere willows darken and flowers bloom, the road levels itself\nHold fast to your heart and stay unmoved\nsuccess comes without asking the future"),
        FortuneStick(3,
            "輕舟已過萬重山\n風雨歸來見彩虹\n舊事隨風皆散去\n新程萬里任君行",
            "The light boat has passed ten thousand mountains\nthrough wind and rain, a rainbow returns\nOld troubles scatter with the wind\na new journey of ten thousand miles awaits your steps"),
        FortuneStick(3,
            "東邊日出西邊雨\n道是無晴卻有晴\n莫愁前路多風雨\n雨過天晴見彩虹",
            "Sun in the east, rain in the west\nseeming no sun, yet sun there is\nDo not fear storms ahead\nafter rain the sky clears and a rainbow appears"),
        FortuneStick(3,
            "好風憑藉力\n送我上青雲\n但行耕耘事\n莫問收穫時",
            "A good wind at my back\nlifts me to the clouds\nJust tend your fields\ndo not ask when the harvest comes"),
        FortuneStick(3,
            "春種一粒粟\n秋收萬顆子\n今日勤耕耘\n明朝碩果累",
            "One grain sown in spring\nten thousand reaped in autumn\nWork hard today\ntomorrow the fruit hangs heavy"),
        FortuneStick(3,
            "山重水複疑無路\n柳暗花明又一村\n峰迴路轉終有日\n守得雲開見月明",
            "Mountains repeat, waters wind — the road seems lost\npast dark willows and bright flowers, another village\nThe path turns at last\nkeep watch till clouds part and the moon shines"),
        FortuneStick(3,
            "千里之行始於足下\n九層之台起於累土\n步步為營穩中進\n功成名就自然來",
            "A thousand-mile journey starts with one step\na nine-story tower rises from a handful of earth\nAdvance steadily\neverything comes in its own time"),
        FortuneStick(3,
            "梅須遜雪三分白\n雪卻輸梅一段香\n各有千秋莫攀比\n做好自己便是贏",
            "The plum yields to snow in whiteness\nsnow yields to plum in fragrance\nEach has its own merit — do not compare\nbeing your best is winning"),
        FortuneStick(3,
            "隨風潛入夜\n潤物細無聲\n默默耕耘久\n功到自然成",
            "Riding the wind, it steals into the night\nmoistening all things without a sound\nWork quietly for long\nwhen the time comes, success follows naturally"),
        // ── 中签（10）──
        FortuneStick(2,
            "凡事隨緣莫強求\n花開花落自有時\n守得初心常在念\n福報綿綿自有期",
            "Do not force what comes\nflowers bloom and fall in their own time\nKeep your original heart\ngood fortune will come in its own season"),
        FortuneStick(2,
            "迷霧重重不見山\n且行且看莫心煩\n時機未到休急躁\n靜待春風綠滿川",
            "Fog upon fog hides the mountains\nwalk on and look, do not fret\nThe time is not yet — wait patiently\nspring wind will green the plains"),
        FortuneStick(2,
            "半是晴天半是陰\n人生得意且徐行\n今朝種下菩提樹\n他日蔭涼自在心",
            "Half clear sky, half overcast\nwalk steadily through life's ups and downs\nPlant the bodhi tree today\ntomorrow its shade will cool your heart"),
        FortuneStick(2,
            "平平淡淡才是真\n簡簡單單過一生\n知足常樂心自寬\n笑看風雲變幻中",
            "Plain and simple is the truth\nlive simply through the years\nContentment brings joy and a wide heart\nsmile as the winds and clouds shift"),
        FortuneStick(2,
            "事緩則圓\n急則生變\n靜心以待\n水落石出",
            "Slow work rounds the matter\nhaste breeds change\nWait with a calm heart\nthe stone emerges when water falls"),
        FortuneStick(2,
            "一花一世界\n一葉一菩提\n平常心是道\n隨緣莫強求",
            "One flower, one world\none leaf, one bodhi\nAn ordinary heart is the way\ngo with fate, do not force"),
        FortuneStick(2,
            "三分天註定\n七分靠打拼\n盡力而為之\n結果隨它去",
            "Three parts destined\nseven parts effort\nDo your best\nlet the result be what it may"),
        FortuneStick(2,
            "忙中有閒樂\n苦中亦有甜\n且行且珍惜\n當下即是福",
            "Joy hides in busy days\nsweetness in bitterness too\nWalk on and cherish each step\nthe present itself is fortune"),
        FortuneStick(2,
            "春江水暖鴨先知\n冷暖自知莫問人\n按部就班慢慢來\n好事多磨終有成",
            "The ducks know first when the spring river warms\nyou alone know your own warmth and cold\nTake things step by step\ngood things come after grinding"),
        FortuneStick(2,
            "月有陰晴圓缺\n人有悲歡離合\n此事古難全\n但願人長久",
            "The moon waxes and wanes\npeople part and meet\nNothing is ever perfect\nmay we all endure"),
        // ── 下签（10）──
        FortuneStick(1,
            "逆水行舟用力撐\n一篙鬆勁退千尋\n此時宜守不宜進\n靜待東風再啟程",
            "Rowing upstream takes all your strength\none loose stroke and you drift back a thousand spans\nNow is the time to hold, not advance\nwait for the east wind to set sail again"),
        FortuneStick(1,
            "迷霧遮攔前行路\n行事宜當再三思\n忍得一時心頭氣\n免得百日身後憂",
            "Mist veils the road ahead\nthink thrice before you act\nEndure a moment of anger\nsave yourself a hundred days of worry"),
        FortuneStick(1,
            "風雨欲來風滿樓\n未雨綢繆早綢繆\n眼前雖有千般事\n步步為營莫出頭",
            "Wind and rain are brewing in the tower\nprepare your shelter before the storm\nThough a thousand matters press upon you\nstep carefully and stay low"),
        FortuneStick(1,
            "屋漏更遭連夜雨\n行船偏遇打頭風\n此際且把腳步穩\n忍過寒冬是新春",
            "The leaking roof meets night after night of rain\nthe boat meets the headwind again\nSteady your steps now\nendure the winter and spring returns"),
        FortuneStick(1,
            "心急吃不了熱豆腐\n欲速則不達\n慢工出細活\n穩紮穩打方為上",
            "Haste cannot eat hot tofu\nspeed does not reach the goal\nSlow work makes fine work\nsolid steps win"),
        FortuneStick(1,
            "人情似紙張張薄\n世事如棋局局新\n逢人且說三分話\n未可全拋一片心",
            "Human feeling is thin as paper\nthe world is a chessboard, ever new\nSpeak but three parts to strangers\nnever bare your whole heart"),
        FortuneStick(1,
            "強扭的瓜不甜\n強求的緣不圓\n順勢而為方自在\n強行出頭必受挫",
            "A melon forced from the vine is not sweet\na bond forced is not round\nGo with the current and be free\nforce your way and you will stumble"),
        FortuneStick(1,
            "多言數窮\n不如守中\n言多必失\n禍從口出\n謹言慎行\n平安是福",
            "Much talk often ends in silence\nbetter hold the middle\nMany words bring loss\ndisaster comes from the mouth\nSpeak and act with care\npeace itself is fortune"),
        FortuneStick(1,
            "樹欲靜而風不止\n事與願違時常有\n且把心態放平穩\n靜觀其變待時機",
            "The tree wishes stillness but the wind will not stop\nplans often go awry\nKeep your heart level\nwatch quietly and wait for the moment"),
        FortuneStick(1,
            "小不忍則亂大謀\n忍一時風平浪靜\n退一步海闊天空\n來日方長再圖之",
            "Without small patience, great plans collapse\nendure a moment and the waves calm\nstep back and the sea and sky open wide\nThe days ahead are long — plan anew"),
        // ── 下下签（10）──
        FortuneStick(0,
            "屋漏偏逢連夜雨\n行船又遇頂頭風\n此際宜潛龍勿用\n韜光養晦待天明",
            "The leaking roof meets nights of rain\nthe sailing boat meets headwinds again\nNow is the time to lie low\nbide your strength and wait for dawn"),
        FortuneStick(0,
            "前路崎嶇多坎坷\n行事宜當步步慎\n一失足成千古恨\n再回首已百年身",
            "The road ahead is rough and rugged\nwalk with great care at every step\nOne misstep becomes a thousand years of regret\nlooking back, a hundred years have passed"),
        FortuneStick(0,
            "黑雲壓城城欲摧\n進退維谷步步難\n且將心火暫壓下\n寒冬過盡自逢春",
            "Black clouds press the city, ready to crumble\nevery path is blocked, advance and retreat alike\nCalm the fire in your heart for now\nwhen winter ends, spring will come"),
        FortuneStick(0,
            "諸事不順莫灰心\n船遲又遇打頭風\n蟄伏潛行待天明\n否極泰來終有時",
            "Do not lose heart when all goes wrong\nthe slow boat meets the headwind too\nLie low and bide till dawn\nafter the worst, the best must come"),
        FortuneStick(0,
            "禍不單行今日至\n福無雙至明朝來\n且將苦難當磨練\n否極泰來終有時",
            "Misfortunes rarely come alone\nfortune seldom comes in pairs — but tomorrow may bring it\nTake hardship as tempering\nafter the dark, light returns"),
        FortuneStick(0,
            "山窮水盡疑無路\n進退兩難心茫然\n此時切忌亂投醫\n守株待兔也是策",
            "Mountains end, waters run dry, the road seems gone\nadvance and retreat alike are blocked\nDo not grasp at straws\neven waiting by the stump is a strategy"),
        FortuneStick(0,
            "破財消災莫心疼\n塞翁失馬焉知福\n得失之間藏天機\n淡然處之是上策",
            "Losing wealth buys off disaster — do not grieve\nthe old man lost his horse; who knows if it is fortune\nHeaven's design hides in gain and loss\ntake it lightly"),
        FortuneStick(0,
            "病來如山倒\n去病如抽絲\n身體是本錢\n保重為第一",
            "Sickness falls like a mountain\nrecovery unwinds like silk thread\nHealth is your capital\ntake care of it first"),
        FortuneStick(0,
            "運交華蓋欲何求\n未敢翻身已碰頭\n低調潛行莫張揚\n韜光養晦度難關",
            "Luck runs thin\nbefore you turn, you hit your head\nMove low and quiet, do not show off\nhide your light and pass the trial"),
        FortuneStick(0,
            "獨木難支大廈\n孤掌難鳴空谷\n且借他人之力\n共渡眼前難關",
            "One beam cannot hold a mansion\none palm cannot clap in an empty valley\nBorrow the strength of others\ncross this pass together"),
    )

    fun draw(): FortuneStick = sticks[Random.nextInt(sticks.size)]
}
