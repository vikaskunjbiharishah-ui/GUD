package `in`.growurdhan.domain.finance

/**
 * First-open walkthrough from src/lib/onboarding.ts.
 * Shown once, before signup. English is the store text. Hindi was
 * owner-reviewed on 2026-10-29. The vault word in Hindi is वॉल्ट.
 * Whether this phone has already seen it stays on the device.
 */
object Onboarding {
    const val SEEN_KEY = "gud.onboarding.seen"
    val slides = listOf(
        Slide(
            "why", "shield",
            "A family vault. Not another brokerage login.",
            "GrowUrDhan is built so Indian families can see every member’s holdings in one private place. We do not sell a demat account. We do not call you. The books live on this phone.",
            "परिवार का वॉल्ट — एक और ब्रोकर लॉगिन नहीं।",
            "GrowUrDhan इसलिए बना है कि भारतीय परिवार सभी सदस्यों के निवेश एक ही निजी जगह पर देख सकें। हम डीमैट खाता नहीं बेचते और आपको कॉल नहीं करते। आपका हिसाब-किताब इसी फ़ोन में रहता है।",
        ),
        Slide(
            "quiet", "phone",
            "No mobile number. No inbox that never rests.",
            "Most “free” money apps want your number and email so someone can pitch you a SIP. GrowUrDhan signs you in with a vault email that never leaves the device. No OTP. No “relationship manager”.",
            "न मोबाइल नंबर, न लगातार आते मैसेज।",
            "ज़्यादातर \"मुफ़्त\" मनी ऐप आपका नंबर और ईमेल इसलिए माँगते हैं ताकि कोई आपको SIP बेच सके। GrowUrDhan में आप वॉल्ट ईमेल से साइन इन करते हैं, जो आपके फ़ोन से बाहर कभी नहीं जाता। न OTP, न \"रिलेशनशिप मैनेजर\"।",
        ),
        Slide(
            "fraud", "lock",
            "Harder to steal, because there is nothing in the cloud to steal.",
            "If a site holds your CAS, a leak is their problem and yours. GrowUrDhan keeps statements on the phone, encrypted. Remote-access apps and other Android apps cannot open the vault. We cannot read it either.",
            "चोरी करना मुश्किल, क्योंकि क्लाउड पर चुराने लायक कुछ है ही नहीं।",
            "अगर आपका CAS किसी साइट पर रखा है, तो लीक होने पर नुकसान उनका भी है और आपका भी। GrowUrDhan आपके स्टेटमेंट एन्क्रिप्ट करके फ़ोन में ही रखता है। रिमोट-एक्सेस ऐप और दूसरे Android ऐप वॉल्ट नहीं खोल सकते। हम भी इसे नहीं पढ़ सकते।",
        ),
        Slide(
            "cas", "file",
            "CAS is the monthly snapshot of everything you already own.",
            "A Consolidated Account Statement from CDSL, NSDL, CAMS or KFin lists your demat and mutual funds. Import that PDF each month. Add NPS, PPF, gold, property and loans yourself. That is the whole ritual.",
            "CAS: आपके पास जो कुछ है, उसकी मासिक झलक।",
            "CDSL, NSDL, CAMS या KFin का कंसोलिडेटेड अकाउंट स्टेटमेंट (CAS) आपके डीमैट और म्यूचुअल फंड की सूची देता है। हर महीने वह PDF ऐप में इम्पोर्ट करें। NPS, PPF, सोना, प्रॉपर्टी और लोन आप ख़ुद जोड़ें। बस इतना ही करना है।",
        ),
        Slide(
            "family", "users",
            "One house. Every PAN. One picture.",
            "Self, spouse, children, HUF — each person keeps their own statements, yet Home shows the family. Broker apps stop at one login. This is the view they do not sell, because they are not your family office.",
            "एक घर। हर PAN। एक तस्वीर।",
            "ख़ुद, जीवनसाथी, बच्चे, HUF — हर सदस्य के स्टेटमेंट अलग रहते हैं, फिर भी होम स्क्रीन पर पूरा परिवार एक साथ दिखता है। ब्रोकर ऐप एक ही लॉगिन तक सीमित रहते हैं। यह तस्वीर वे नहीं दिखाते, क्योंकि वे आपके पूरे परिवार का हिसाब रखने के लिए बने ही नहीं हैं।",
        ),
        Slide(
            "price", "rupee",
            "Thirty days on us. Then ₹99 a year.",
            "Try every feature for 30 days through Google Play. After that, one annual subscription is ₹99 a year. It renews on Google Play and includes the vault and Gmail sync. There is no lifetime licence and no other paid plan. You can cancel any time in Google Play.",
            "तीस दिन हमारी तरफ़ से मुफ़्त। फिर ₹99 सालाना।",
            "Google Play के ज़रिए 30 दिन तक सारे फ़ीचर आज़माएँ। उसके बाद एक ही वार्षिक सब्सक्रिप्शन है — ₹99 प्रति वर्ष। यह Google Play पर हर साल अपने-आप रिन्यू होता है, आप इसे वहीं कभी भी रद्द कर सकते हैं, और इसमें वॉल्ट व Gmail सिंक शामिल हैं। लाइफ़टाइम लाइसेंस या कोई दूसरा पेड प्लान नहीं है।",
        ),
    )

    fun seen(stored: String?): Boolean = stored == "1"

    /** Button and cue copy. Slide sentences stay in [slides]. */
    fun chrome(hindi: Boolean): OnboardingChrome = if (hindi) {
        OnboardingChrome(
            next = "आगे",
            back = "पीछे",
            skip = "छोड़ें",
            languageToggle = "अंग्रेज़ी",
            scrollCue = "और पढ़ने के लिए ऊपर खिसकाएँ।",
        )
    } else {
        OnboardingChrome(
            next = "Next",
            back = "Back",
            skip = "Skip",
            languageToggle = "हिन्दी",
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
    val languageToggle: String,
    val scrollCue: String,
)
