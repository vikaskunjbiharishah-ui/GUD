package `in`.growurdhan.domain.finance

/**
 * First-open walkthrough from src/lib/onboarding.ts.
 * Shown once, before signup. The copy is the store text, in English and Hindi.
 * Whether this phone has already seen it stays on the device.
 */
object Onboarding {
    const val SEEN_KEY = "gud.onboarding.seen"
    val slides = listOf(
        Slide("why", "shield", "A family vault. Not another brokerage login.", "GrowUrDhan is built so Indian families can see every member’s holdings in one private place. We do not sell a demat account. We do not call you. The books live on this phone.", "परिवार की तिजोरी। ब्रोकर का लॉगिन नहीं।", "GrowUrDhan भारतीय परिवारों के लिए है — एक ही जगह, सबके निवेश। हम डीमैट नहीं बेचते, कॉल नहीं करते। हिसाब इसी फ़ोन पर रहता है।"),
        Slide("quiet", "phone", "No mobile number. No inbox that never rests.", "Most “free” money apps want your number and email so someone can pitch you a SIP. GrowUrDhan signs you in with a vault email that never leaves the device. No OTP. No “relationship manager”.", "न मोबाइल नंबर, न बेचैन इनबॉक्स।", "ज़्यादातर मुफ़्त ऐप आपका नंबर इसलिए माँगते हैं ताकि SIP बेच सकें। यहाँ ईमेल सिर्फ़ ताला है — डिवाइस से बाहर नहीं जाता। OTP नहीं। कॉल नहीं।"),
        Slide("fraud", "lock", "Harder to steal, because there is nothing in the cloud to steal.", "If a site holds your CAS, a leak is their problem and yours. GrowUrDhan keeps statements on the phone, encrypted. Remote-access apps and other Android apps cannot open the vault. We cannot read it either.", "चोरी मुश्किल — बादल में रखने को कुछ है ही नहीं।", "CAS अगर किसी साइट पर है, तो लीक आपका और उनका दोनों का नुकसान है। यहाँ विवरण फ़ोन पर, एन्क्रिप्टेड। दूसरे ऐप तिजोरी नहीं खोल सकते। हम भी नहीं पढ़ सकते।"),
        Slide("cas", "file", "CAS is the monthly snapshot of everything you already own.", "A Consolidated Account Statement from CDSL, NSDL, CAMS or KFin lists your demat and mutual funds. Import that PDF each month. Add NPS, PPF, gold, property and loans yourself. That is the whole ritual.", "CAS वही मासिक तस्वीर है जो आपके पास पहले से है।", "CDSL, NSDL, CAMS या KFin का Consolidated Account Statement डीमैट और म्यूचुअल फंड दिखाता है। हर महीने वही PDF लगाएँ। NPS, PPF, सोना, घर और ऋण आप जोड़ें। बस इतना काम है।"),
        Slide("family", "users", "One house. Every PAN. One picture.", "Self, spouse, children, HUF — each person keeps their own statements, yet Home shows the family. Broker apps stop at one login. This is the view they do not sell, because they are not your family office.", "एक घर। हर PAN. एक तस्वीर।", "स्वयं, जीवनसाथी, बच्चे, HUF — बयान अलग, होम पर परिवार एक साथ। ब्रोकर ऐप एक लॉगिन पर रुक जाते हैं। यह नज़रिया वे नहीं देते — वे आपका परिवार नहीं हैं।"),
        Slide("price", "rupee", "Thirty days on us. Then ₹99 a year.", "Try every feature for 30 days through Google Play. After that, one annual subscription is ₹99 a year. It renews on Google Play and includes the vault and Gmail sync. There is no lifetime licence and no other paid plan.", "तीस दिन हमारे साथ। फिर ₹99 सालाना।", "Google Play से 30 दिन सब कुछ आज़माएँ। उसके बाद एक ही वार्षिक सदस्यता ₹99। यह हर साल Play पर नवीनीकृत होती है और तिजोरी तथा Gmail सिंक दोनों शामिल हैं। कोई आजीवन लाइसेंस या दूसरा प्लान नहीं है।")
    )

    fun seen(stored: String?): Boolean = stored == "1"
}

data class Slide(
    val id: String,
    val icon: String,
    val enTitle: String,
    val enBody: String,
    val hiTitle: String,
    val hiBody: String,
)
