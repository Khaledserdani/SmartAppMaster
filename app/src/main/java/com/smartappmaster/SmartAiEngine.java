package com.smartappmaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Local, transparent recommendation engine.
 * It does not inspect another app's private screens, messages, or network traffic.
 */
public final class SmartAiEngine {
    public static final class Analysis {
        public final String category;
        public final int confidence;
        public final String summary;
        public final List<String> recommendations;

        Analysis(String category, int confidence, String summary, List<String> recommendations) {
            this.category = category;
            this.confidence = confidence;
            this.summary = summary;
            this.recommendations = recommendations;
        }
    }

    private SmartAiEngine() {}

    public static Analysis analyze(String label, String packageName,
                                   boolean overlayAllowed, boolean splitSupported,
                                   int feedbackScore) {
        String value = ((label == null ? "" : label) + " "
                + (packageName == null ? "" : packageName)).toLowerCase(Locale.ROOT);
        String category;
        String summary;
        List<String> tips = new ArrayList<>();

        boolean game = containsAny(value, "game", "play", "battle", "craft", "pubg",
                "freefire", "free fire", "mobile legends", "clash", "roblox", "acecraft");
        boolean social = containsAny(value, "tango", "messenger", "whatsapp", "telegram",
                "instagram", "facebook", "snapchat", "discord", "social");
        boolean video = containsAny(value, "youtube", "netflix", "video", "player",
                "stream", "twitch", "tiktok", "primevideo", "prime video");

        if (game) {
            category = "لعبة";
            summary = "تم تصنيف التطبيق كلعبة. يمكن إضافة أدوات مساعدة مستقلة، لكن لا نغيّر ملفات اللعبة أو نتائجها ولا نتجاوز مشترياتها.";
            tips.add("مساعد عائم لتدوين الملاحظات والاختصارات أثناء اللعب.");
            tips.add("دليل إعدادات وتحسينات داخل اللعبة يختارها المستخدم بنفسه.");
            tips.add("تسجيل تقييمك للأدوات حتى تتحسن الاقتراحات التالية.");
        } else if (social) {
            category = "تواصل وبث";
            summary = "تم تصنيف التطبيق كتطبيق تواصل. يمكن توفير اختصارات ومساعد جانبي دون قراءة الرسائل الخاصة أو فتح بث محمي.";
            tips.add("مساعد عائم للوصول السريع إلى أدواتك.");
            tips.add("مساحة ملاحظات وترجمة للنص الذي تختار نسخه بنفسك.");
            tips.add("تذكيرك بميزات الحساب والاشتراك التي تملكها رسمياً.");
        } else if (video) {
            category = "فيديو وبث";
            summary = "تم تصنيف التطبيق كتطبيق فيديو. جودة الصورة تعتمد على المصدر والاشتراك والشبكة والجهاز؛ لا يمكن فرض جودة غير متاحة.";
            tips.add("دليل سريع للعثور على إعدادات الجودة داخل التطبيق.");
            tips.add("اختصار عائم وأدوات تنظيم المشاهدة.");
            tips.add("فحص توافق وضع تقسيم الشاشة عند دعم التطبيق.");
        } else {
            category = "تطبيق عام";
            summary = "لم تظهر مؤشرات كافية لتحديد نوع التطبيق بدقة. سنقترح أدوات عامة وآمنة ونحسنها حسب تقييمك.";
            tips.add("مساعد عائم للاختصارات والملاحظات.");
            tips.add("تشغيل التطبيق في شاشة مقسومة إذا دعمه Android والتطبيق.");
            tips.add("تسجيل تقييمك لتخصيص الاقتراحات.");
        }

        if (!overlayAllowed) {
            tips.add("المساعد العائم يحتاج إذن الظهور فوق التطبيقات من Android.");
        }
        if (!splitSupported) {
            tips.add("تقسيم الشاشة غير مدعوم على إصدار Android الحالي.");
        }

        int confidence = game || social || video ? 78 : 55;
        confidence += Math.max(-10, Math.min(10, feedbackScore));
        confidence = Math.max(40, Math.min(90, confidence));
        return new Analysis(category, confidence, summary, tips);
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }
}
