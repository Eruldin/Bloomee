package com.bloomee.app.domain.advice

import com.bloomee.app.domain.model.CyclePhase
import com.bloomee.app.domain.model.CycleStats
import com.bloomee.app.domain.model.DailyLog
import com.bloomee.app.domain.model.FlowLevel
import com.bloomee.app.domain.model.HydrationDay
import com.bloomee.app.domain.model.Symptom

data class AdviceCard(
    val id: String,
    val title: String,
    val body: String,
    val category: AdviceCategory
)

enum class AdviceCategory(val label: String) {
    CYCLE("Döngü"),
    NUTRITION("Beslenme"),
    MOVEMENT("Hareket"),
    HYDRATION("Su"),
    WELLBEING("İyi hissetme"),
    ALERT("Dikkat")
}

/** Offline, rule-based guidance. The optional AI assistant builds on top of these cards. */
object AdviceEngine {

    const val MEDICAL_DISCLAIMER =
        "Bu içerik genel bilgilendirme amaçlıdır ve tıbbi tavsiye yerine geçmez. " +
            "Şiddetli veya uzun süren belirtilerde bir hekime danışın."

    fun cardsFor(
        stats: CycleStats,
        todayLog: DailyLog?,
        hydration: HydrationDay
    ): List<AdviceCard> {
        val cards = mutableListOf<AdviceCard>()

        cards += phaseCard(stats.phase)
        cards += nutritionCard(stats.phase)
        cards += movementCard(stats.phase)

        if (hydration.progress < 0.5f) {
            val remaining = (hydration.goalMl - hydration.consumedMl).coerceAtLeast(0)
            cards += AdviceCard(
                id = "hydration_behind",
                title = "Su hedefinin gerisindesin",
                body = "Bugünkü hedefine $remaining ml kaldı. Bir bardak su, kramp ve baş ağrısı " +
                    "şiddetini azaltmaya yardımcı olabilir.",
                category = AdviceCategory.HYDRATION
            )
        } else if (hydration.progress >= 1f) {
            cards += AdviceCard(
                id = "hydration_done",
                title = "Su hedefin tamam",
                body = "Bugün ${hydration.consumedMl} ml içtin. Bu düzeni koruman enerjini dengede tutar.",
                category = AdviceCategory.HYDRATION
            )
        }

        val symptoms = todayLog?.symptoms.orEmpty()
        if (Symptom.CRAMPS in symptoms) {
            cards += AdviceCard(
                id = "symptom_cramps",
                title = "Kramp için",
                body = "Karın bölgesine 15-20 dakika sıcak uygulama, hafif esneme ve magnezyumdan " +
                    "zengin besinler (badem, ıspanak, muz) rahatlatıcı olabilir.",
                category = AdviceCategory.WELLBEING
            )
        }
        if (Symptom.HEADACHE in symptoms) {
            cards += AdviceCard(
                id = "symptom_headache",
                title = "Baş ağrısı",
                body = "Sıvı eksikliği ve düşük kan şekeri baş ağrısını tetikleyebilir. Su iç, " +
                    "ekranlardan kısa bir mola ver ve kafeini sınırlandır.",
                category = AdviceCategory.WELLBEING
            )
        }
        if (Symptom.INSOMNIA in symptoms || (todayLog?.sleepHours ?: 8.0) < 6.0) {
            cards += AdviceCard(
                id = "sleep",
                title = "Uyku düzeni",
                body = "Luteal fazda vücut sıcaklığı yükselir ve uyku bölünebilir. Odanı serin tut, " +
                    "yatmadan bir saat önce ekranı bırak.",
                category = AdviceCategory.WELLBEING
            )
        }

        if (stats.isLate) {
            cards += AdviceCard(
                id = "late_period",
                title = "Regl gecikmesi",
                body = "Beklenen tarihin ${-(stats.daysToNextPeriod ?: 0)} gün geçti. Stres, uyku " +
                    "düzensizliği ve yoğun egzersiz gecikmeye yol açabilir; sürerse hekime danış.",
                category = AdviceCategory.ALERT
            )
        }
        if (stats.isIrregular) {
            cards += AdviceCard(
                id = "irregular",
                title = "Döngün değişken",
                body = "Son döngülerinde ${stats.cycleLengthVariation.toInt()} güne varan sapma var. " +
                    "Tahminler bu yüzden geniş bir aralıkta; kayıt tutmaya devam ettikçe hassaslaşacak.",
                category = AdviceCategory.ALERT
            )
        }
        if (todayLog?.flow == FlowLevel.HEAVY) {
            cards += AdviceCard(
                id = "heavy_flow",
                title = "Yoğun kanama",
                body = "Yoğun regl günlerinde su ve demir alımı önemli. Saatte birden fazla " +
                    "tampon/ped gereksinimi birkaç saat sürerse veya bu düzen her döngüde " +
                    "tekrarlanırsa bir hekime danışmak iyi olur.",
                category = AdviceCategory.ALERT
            )
        }

        return cards
    }

    private fun phaseCard(phase: CyclePhase) = when (phase) {
        CyclePhase.MENSTRUAL -> AdviceCard(
            "phase", "Dinlenme zamanı",
            "Östrojen ve progesteron en düşük seviyede. Kendine yüklenme; kısa yürüyüş ve " +
                "uyku, enerji düşüşünü dengelemenin en iyi yolu.",
            AdviceCategory.CYCLE
        )
        CyclePhase.FOLLICULAR -> AdviceCard(
            "phase", "Enerjin yükseliyor",
            "Östrojen artıyor; odaklanma ve dayanıklılık en iyi dönemde. Yeni alışkanlıklar " +
                "başlatmak için uygun bir zaman.",
            AdviceCategory.CYCLE
        )
        CyclePhase.OVULATION -> AdviceCard(
            "phase", "Yumurtlama günü",
            "Doğurganlık zirvede. Bazı kişilerde tek taraflı hafif karın ağrısı normaldir.",
            AdviceCategory.CYCLE
        )
        CyclePhase.LUTEAL -> AdviceCard(
            "phase", "PMS dönemi yaklaşıyor",
            "Progesteron zirvede; şişkinlik, tatlı isteği ve duygusal dalgalanma olağan. " +
                "Tuz ve kafeini azaltmak belirtileri hafifletebilir.",
            AdviceCategory.CYCLE
        )
        CyclePhase.UNKNOWN -> AdviceCard(
            "phase", "İlk kaydını gir",
            "Son regl tarihini işaretlediğinde faz takibi, tahmin ve kişisel öneriler burada " +
                "görünmeye başlayacak.",
            AdviceCategory.CYCLE
        )
    }

    private fun nutritionCard(phase: CyclePhase) = when (phase) {
        CyclePhase.MENSTRUAL -> AdviceCard(
            "nutrition", "Demiri destekle",
            "Kanama demir kaybı demek. Kırmızı et, mercimek, kuru kayısı ve yanında C vitamini " +
                "(portakal, biber) emilimi artırır.",
            AdviceCategory.NUTRITION
        )
        CyclePhase.FOLLICULAR -> AdviceCard(
            "nutrition", "Hafif ve protein ağırlıklı",
            "Yumurta, yoğurt ve baklagiller kas onarımını destekler; lifli sebzeler östrojen " +
                "dengesine yardımcı olur.",
            AdviceCategory.NUTRITION
        )
        CyclePhase.OVULATION -> AdviceCard(
            "nutrition", "Antioksidan zamanı",
            "Yeşil yapraklılar, avokado ve omega-3 kaynakları (somon, ceviz) bu dönemde iyi gelir.",
            AdviceCategory.NUTRITION
        )
        CyclePhase.LUTEAL -> AdviceCard(
            "nutrition", "Kan şekerini dengede tut",
            "Kompleks karbonhidrat (yulaf, tam tahıl) ve magnezyum (bitter çikolata, badem) " +
                "tatlı krizini ve sinirliliği yumuşatır.",
            AdviceCategory.NUTRITION
        )
        CyclePhase.UNKNOWN -> AdviceCard(
            "nutrition", "Dengeli tabak",
            "Her öğünde protein, lif ve sağlıklı yağ bulundurmak döngü boyunca enerjini dengeler.",
            AdviceCategory.NUTRITION
        )
    }

    private fun movementCard(phase: CyclePhase) = when (phase) {
        CyclePhase.MENSTRUAL -> AdviceCard(
            "movement", "Yumuşak hareket",
            "Yoga, esneme ve 20 dakikalık tempolu olmayan yürüyüş krampları azaltır.",
            AdviceCategory.MOVEMENT
        )
        CyclePhase.FOLLICULAR -> AdviceCard(
            "movement", "Güç çalışmasına uygun",
            "Dayanıklılığın yüksek; ağırlık antrenmanı veya yüksek tempolu kardiyo için iyi bir dönem.",
            AdviceCategory.MOVEMENT
        )
        CyclePhase.OVULATION -> AdviceCard(
            "movement", "Performans zirvesi",
            "Kendini en güçlü hissedeceğin günler; ancak eklem esnekliği arttığı için ısınmayı ihmal etme.",
            AdviceCategory.MOVEMENT
        )
        CyclePhase.LUTEAL -> AdviceCard(
            "movement", "Tempoyu düşür",
            "Pilates, yüzme ve orta tempolu yürüyüş şişkinliği azaltır ve ruh halini dengeler.",
            AdviceCategory.MOVEMENT
        )
        CyclePhase.UNKNOWN -> AdviceCard(
            "movement", "Düzenli hareket",
            "Haftada 150 dakika orta tempolu hareket, döngü belirtilerini genel olarak hafifletir.",
            AdviceCategory.MOVEMENT
        )
    }

    fun partnerSummary(stats: CycleStats, partnerName: String, symptoms: Set<Symptom>): String {
        val name = partnerName.ifBlank { "Partnerin" }
        val phaseNote = when (stats.phase) {
            CyclePhase.MENSTRUAL -> "regl döneminde; enerjisi düşük olabilir"
            CyclePhase.FOLLICULAR -> "foliküler fazda; enerjisi yükseliyor"
            CyclePhase.OVULATION -> "yumurtlama gününde"
            CyclePhase.LUTEAL -> "luteal fazda; PMS belirtileri görülebilir"
            CyclePhase.UNKNOWN -> "henüz yeterli kayıt yok"
        }
        val symptomNote = if (symptoms.isEmpty()) {
            "Bugün belirti girilmemiş."
        } else {
            "Bugün bildirilen belirtiler: " + symptoms.joinToString { it.label } + "."
        }
        val action = when (stats.phase) {
            CyclePhase.MENSTRUAL -> "Sıcak bir içecek, ev işlerini üstlenmek ve sessiz bir akşam iyi gelir."
            CyclePhase.LUTEAL -> "Sabırlı ol, plan değişikliklerini esnet ve tatlı krizini yargılama."
            else -> "Birlikte hareket etmek ve sosyal planlar bu dönemde keyifli olur."
        }
        return "$name $phaseNote. $symptomNote $action"
    }
}
