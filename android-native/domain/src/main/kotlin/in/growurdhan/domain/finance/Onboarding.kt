package `in`.growurdhan.domain.finance

/**
 * First-open walkthrough from src/lib/onboarding.ts.
 * English only. Hindi was removed on 2026-11-09. The phone does not offer a language switch.
 */
object Onboarding {
    const val SEEN_KEY = "gud.onboarding.seen"
    val slides = listOf(
        Slide(
            "why", "shield",
            "A family vault. Not another brokerage login.",
            "GrowUrDhan lets an Indian family see every member's holdings in one private place. We do not sell a demat account or call you, and the books live on this phone.",
            "परिवार का वॉल्ट — एक और ब्रोकर लॉगिन नहीं।",
            "भारतीय परिवार सभी सदस्यों के निवेश एक ही निजी जगह पर देख सकते हैं। हम डीमैट खाता नहीं बेचते, कॉल नहीं करते, और हिसाब-किताब इसी फ़ोन में रहता है।",
        ),
        Slide(
            "quiet", "phone",
            "No mobile number. No inbox that never rests.",
            "Most free money apps want your number so someone can pitch a SIP. GrowUrDhan signs you in with an email that stays on this phone, with no OTP and no relationship manager.",
            "न मोबाइल नंबर, न लगातार आते मैसेज।",
            "ज़्यादातर मुफ़्त मनी ऐप आपका नंबर इसलिए माँगते हैं ताकि कोई SIP बेच सके। साइन इन वॉल्ट ईमेल से होता है, जो फ़ोन पर ही रहता है, बिना OTP और बिना रिलेशनशिप मैनेजर।",
        ),
        Slide(
            "fraud", "lock",
            "Harder to steal, because there is nothing in the cloud to steal.",
            "If a site holds your CAS, a leak is their problem and yours. Statements stay on this phone, encrypted, and neither we nor other apps can open them.",
            "चोरी करना मुश्किल, क्योंकि क्लाउड पर चुराने लायक कुछ है ही नहीं।",
            "अगर CAS किसी साइट पर है, तो लीक का नुकसान उनका भी है और आपका भी। स्टेटमेंट एन्क्रिप्ट होकर इसी फ़ोन में रहते हैं, और हम उन्हें नहीं पढ़ सकते।",
        ),
        Slide(
            "cas", "file",
            "CAS is the monthly snapshot of everything you already own.",
            "A Consolidated Account Statement from CDSL, NSDL, CAMS or KFin lists your demat and mutual funds. Import that PDF each month, and add NPS, PPF, gold, property and loans yourself.",
            "CAS: आपके पास जो कुछ है, उसकी मासिक झलक।",
            "CDSL, NSDL, CAMS या KFin का स्टेटमेंट आपके डीमैट और म्यूचुअल फंड की सूची है। हर महीने वह PDF डालें, और NPS, PPF, सोना, प्रॉपर्टी व लोन ख़ुद जोड़ें।",
        ),
        Slide(
            "family", "users",
            "One house. Every PAN. One picture.",
            "Self, spouse, children and HUF each keep their own statements, and Home shows the family together. Broker apps stop at one login, because they are not your family office.",
            "एक घर। हर PAN। एक तस्वीर।",
            "ख़ुद, जीवनसाथी, बच्चे और HUF के स्टेटमेंट अलग रहते हैं, और होम पर पूरा परिवार एक साथ दिखता है। ब्रोकर ऐप एक लॉगिन तक रुकते हैं, क्योंकि वे आपके परिवार का हिसाब नहीं रखते।",
        ),
        Slide(
            "price", "rupee",
            "Thirty days on us. Then ₹99 a year.",
            "Try every feature for 30 days, then ₹99 a year on Google Play, with the vault and Gmail sync and no lifetime licence. You can cancel any time in Google Play.",
            "तीस दिन हमारी तरफ़ से मुफ़्त। फिर ₹99 सालाना।",
            "30 दिन मुफ़्त, फिर ₹99 प्रति वर्ष, जिसमें वॉल्ट और Gmail सिंक हैं। Google Play पर इसे कभी भी रद्द कर सकते हैं।",
        ),
    )

    fun seen(stored: String?): Boolean = stored == "1"

    /** Button copy. Always English. The Boolean is ignored so an old call cannot turn Hindi back on. */
    @Suppress("UNUSED_PARAMETER")
    fun chrome(hindi: Boolean = false): OnboardingChrome {
        return OnboardingChrome(
            next = "Next",
            back = "Back",
            skip = "Skip",
            start = "Continue",
            languageToggle = "",
            scrollCue = "Scroll for the rest of this slide.",
        )
    }
}

data class Slide(
    val id: String,
    val icon: String,
    val enTitle: String,
    val enBody: String,
    val hiTitle: String,
    val hiBody: String,
)

data class OnboardingChrome(
    val next: String,
    val back: String,
    val skip: String,
    val start: String,
    val languageToggle: String,
    val scrollCue: String,
)
