package com.smartappmaster;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 15, 27);
    private static final int PANEL = Color.rgb(20, 30, 47);
    private static final int PANEL_2 = Color.rgb(28, 42, 63);
    private static final int BLUE = Color.rgb(55, 132, 230);
    private static final int TEXT = Color.rgb(245, 248, 252);
    private static final int MUTED = Color.rgb(157, 174, 196);

    private LinearLayout root;
    private LinearLayout appList;
    private TextView selectedLabel;
    private TextView deviceSummary;
    private TextView privacyStatus;
    private EditText searchBox;
    private SharedPreferences prefs;
    private String selectedPackage = "";
    private String selectedLabelText = "";
    private final List<AppEntry> apps = new ArrayList<>();
    private WindowManager floatingWindowManager;
    private View floatingAssistantView;

    private static class AppEntry {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }
        prefs = getSharedPreferences("smart_app_master", MODE_PRIVATE);
        selectedPackage = prefs.getString("selected_package", "");
        selectedLabelText = prefs.getString("selected_label", "");
        buildScreen();
        loadApps();
        updateDeviceSummary();
        updatePrivacyStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null) {
            updatePrivacyStatus();
            updateDeviceSummary();
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("SMART APP MASTER", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = text("مركز ذكي لإدارة تطبيقاتك وتهيئة هاتفك", 14, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);
        addGap(root, 18);

        addSection("حالة الهاتف");
        deviceSummary = text("جارٍ قراءة معلومات الجهاز...", 14, TEXT, false);
        addCard(deviceSummary);
        Button refresh = button("تحديث معلومات الهاتف");
        refresh.setOnClickListener(v -> updateDeviceSummary());
        root.addView(refresh);

        addGap(root, 18);
        addSection("وضع الخصوصية العام");
        privacyStatus = text("", 13, MUTED, false);
        addCard(privacyStatus);
        Button privacyButton = button("تبديل وضع عدم الإزعاج");
        privacyButton.setOnClickListener(v -> toggleDoNotDisturb());
        root.addView(privacyButton);
        TextView privacyNote = text("هذا الوضع يؤثر على إشعارات الهاتف عموماً، وليس Tango وحده. أول مرة قد يطلب Android منح صلاحية خاصة. إعداد إخفاء محتوى الإشعارات تضبطه من إعدادات النظام.", 12, MUTED, false);
        addCard(privacyNote);
        Button dndSettings = secondaryButton("إعدادات عدم الإزعاج في Android");
        dndSettings.setOnClickListener(v -> openDndSettings());
        root.addView(dndSettings);

        addGap(root, 18);
        addSection("التطبيق المحدد");
        selectedLabel = text("اختر تطبيقاً من القائمة بالأسفل", 15, TEXT, true);
        addCard(selectedLabel);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        Button launch = button("فتح التطبيق");
        launch.setOnClickListener(v -> launchSelected());
        actions.addView(launch);
        Button notifications = secondaryButton("إعدادات إشعارات التطبيق");
        notifications.setOnClickListener(v -> openSelectedNotificationSettings());
        actions.addView(notifications);
        Button appInfo = secondaryButton("معلومات التطبيق والصلاحيات");
        appInfo.setOnClickListener(v -> openSelectedAppInfo());
        actions.addView(appInfo);
        Button storage = secondaryButton("مساحة التخزين");
        storage.setOnClickListener(v -> openSelectedStorage());
        actions.addView(storage);
        root.addView(actions);

        addGap(root, 18);
        addSection("إضافات تجريبية فوق التطبيقات");
        TextView featureNote = text("هذه أدوات مستقلة تعمل فوق التطبيق المحدد ضمن حدود Android. لا تفتح محتوى مدفوعاً ولا تتجاوز خصوصية أو حماية أي تطبيق.", 12, MUTED, false);
        addCard(featureNote);
        Button floating = button("تشغيل المساعد العائم فوق التطبيقات");
        floating.setOnClickListener(v -> showFloatingAssistant());
        root.addView(floating);
        Button split = secondaryButton("طلب فتح التطبيق في شاشة مقسومة");
        split.setOnClickListener(v -> openSelectedSplitScreen());
        root.addView(split);
        Button analyze = secondaryButton("تحليل إمكانيات التطبيق واقتراح أدوات");
        analyze.setOnClickListener(v -> analyzeSelectedApp());
        root.addView(analyze);

        addGap(root, 18);
        addSection("قائمة التطبيقات");
        TextView hint = text("اختر التطبيق لإدارة إعداداته من خلال Android أو تشغيله مباشرة.", 12, MUTED, false);
        root.addView(hint);
        addGap(root, 8);
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(15);
        searchBox.setTextColor(TEXT);
        searchBox.setHintTextColor(MUTED);
        searchBox.setHint("ابحث باسم التطبيق...");
        searchBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchBox.setBackground(round(PANEL_2, 12));
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderApps(s.toString()); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        root.addView(searchBox, new LinearLayout.LayoutParams(-1, dp(50)));
        addGap(root, 8);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        root.addView(appList);

        addGap(root, 18);
        TextView footer = text("Smart App Master • الإصدار 1.0.0", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private void loadApps() {
        apps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : results) {
            String packageName = info.activityInfo.packageName;
            String label = info.loadLabel(pm).toString();
            boolean exists = false;
            for (AppEntry entry : apps) {
                if (entry.packageName.equals(packageName)) { exists = true; break; }
            }
            if (!exists) apps.add(new AppEntry(label, packageName));
        }
        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        if (!selectedPackage.isEmpty()) {
            boolean found = false;
            for (AppEntry app : apps) {
                if (app.packageName.equals(selectedPackage)) {
                    selectedLabelText = app.label;
                    found = true;
                    break;
                }
            }
            if (!found) {
                selectedPackage = "";
                selectedLabelText = "";
                prefs.edit().remove("selected_package").remove("selected_label").apply();
            }
        }
        updateSelectedLabel();
        renderApps(searchBox == null ? "" : searchBox.getText().toString());
    }

    private void renderApps(String filter) {
        if (appList == null) return;
        appList.removeAllViews();
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (AppEntry app : apps) {
            if (!q.isEmpty() && !app.label.toLowerCase(Locale.ROOT).contains(q)
                    && !app.packageName.toLowerCase(Locale.ROOT).contains(q)) continue;
            count++;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(11), dp(14), dp(11));
            item.setBackground(round(app.packageName.equals(selectedPackage) ? PANEL_2 : PANEL, 13));
            item.setClickable(true);
            item.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(7);
            appList.addView(item, lp);

            TextView name = text(app.label, 15, TEXT, true);
            item.addView(name);
            TextView pkg = text(app.packageName, 10, MUTED, false);
            item.addView(pkg);
            item.setOnClickListener(v -> {
                selectedPackage = app.packageName;
                selectedLabelText = app.label;
                prefs.edit().putString("selected_package", selectedPackage)
                        .putString("selected_label", selectedLabelText).apply();
                updateSelectedLabel();
                renderApps(searchBox.getText().toString());
                Toast.makeText(this, "تم اختيار " + app.label, Toast.LENGTH_SHORT).show();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
            });
        }
        if (count == 0) {
            appList.addView(text("لا توجد نتائج مطابقة.", 14, MUTED, false));
        }
    }

    private void updateSelectedLabel() {
        if (selectedLabel == null) return;
        if (selectedPackage.isEmpty()) selectedLabel.setText("اختر تطبيقاً من القائمة بالأسفل");
        else selectedLabel.setText(selectedLabelText + "\n" + selectedPackage);
    }

    private void launchSelected() {
        if (!hasSelection()) return;
        Intent intent = getPackageManager().getLaunchIntentForPackage(selectedPackage);
        if (intent == null) {
            toast("هذا التطبيق لا يوفر اختصار تشغيل.");
            return;
        }
        try {
            prefs.edit().putLong("last_open_" + selectedPackage, System.currentTimeMillis()).apply();
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast("تعذر فتح التطبيق.");
        }
    }

    private void openSelectedNotificationSettings() {
        if (!hasSelection()) return;
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, selectedPackage);
            startActivity(intent);
        } catch (Exception e) {
            openSelectedAppInfo();
        }
    }

    private void openSelectedAppInfo() {
        if (!hasSelection()) return;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + selectedPackage));
        startActivity(intent);
    }

    private void openSelectedStorage() {
        if (!hasSelection()) return;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + selectedPackage));
            startActivity(intent);
            toast("افتح قسم التخزين من صفحة معلومات التطبيق.");
        } catch (Exception e) {
            toast("إعداد التخزين غير متاح لهذا التطبيق.");
        }
    }

    private boolean hasSelection() {
        if (selectedPackage.isEmpty()) {
            toast("اختر تطبيقاً أولاً.");
            return false;
        }
        return true;
    }

    private void toggleDoNotDisturb() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toast("هذه الميزة تحتاج Android 6 أو أحدث.");
            return;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            toast("امنح Smart App Master صلاحية عدم الإزعاج من صفحة Android التي ستفتح.");
            openDndSettings();
            return;
        }
        int current = nm.getCurrentInterruptionFilter();
        boolean enabled = current != NotificationManager.INTERRUPTION_FILTER_NONE;
        nm.setInterruptionFilter(enabled ? NotificationManager.INTERRUPTION_FILTER_NONE
                : NotificationManager.INTERRUPTION_FILTER_ALL);
        updatePrivacyStatus();
        toast(enabled ? "تم تشغيل عدم الإزعاج على الهاتف." : "تم إيقاف عدم الإزعاج.");
    }

    private void openDndSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) { toast("تعذر فتح إعدادات Android."); }
        }
    }

    private void updatePrivacyStatus() {
        if (privacyStatus == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            privacyStatus.setText("الصلاحية غير ممنوحة. اضغط على زر الإعدادات لمنحها يدوياً.");
        } else if (nm.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_NONE) {
            privacyStatus.setText("عدم الإزعاج مفعّل حالياً على الهاتف.");
        } else {
            privacyStatus.setText("عدم الإزعاج متوقف حالياً. يمكنك تفعيله من الزر.");
        }
    }

    private void updateDeviceSummary() {
        if (deviceSummary == null) return;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memory);
        long availableMb = memory.availMem / (1024L * 1024L);
        long totalMb = memory.totalMem / (1024L * 1024L);
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        String batteryText = "غير متاح";
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            if (level >= 0 && scale > 0) batteryText = (level * 100 / scale) + "%";
            if (temp > 0) batteryText += String.format(Locale.US, " • حرارة البطارية %.1f°", temp / 10.0);
        }
        deviceSummary.setText("الذاكرة المتاحة: \u2066" + availableMb + " MB\u2069 من \u2066" + totalMb
                + " MB\u2069\nالبطارية: " + batteryText
                + "\nإصدار Android: " + Build.VERSION.RELEASE);
    }

    private void showFloatingAssistant() {
        if (!hasSelection()) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toast("المساعد العائم يحتاج Android 6 أو أحدث.");
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            toast("اسمح للمساعد بالظهور فوق التطبيقات، ثم ارجع إلى Smart App Master.");
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                toast("تعذر فتح إعداد صلاحية الظهور فوق التطبيقات.");
            }
            return;
        }
        removeFloatingAssistant();
        floatingWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        panel.setBackground(round(PANEL, 18));
        TextView heading = text("المساعد العائم • " + selectedLabelText, 15, TEXT, true);
        panel.addView(heading);
        TextView detail = text("اختصارات سريعة أثناء استعمال التطبيق. هذه النسخة تجريبية ولا تقرأ المحادثات أو محتوى الشاشة.", 11, MUTED, false);
        panel.addView(detail);
        Button open = button("فتح " + selectedLabelText);
        open.setOnClickListener(v -> launchSelected());
        panel.addView(open);
        Button inspect = secondaryButton("عرض تحليل التطبيق");
        inspect.setOnClickListener(v -> analyzeSelectedApp());
        panel.addView(inspect);
        Button close = secondaryButton("إغلاق النافذة العائمة");
        close.setOnClickListener(v -> removeFloatingAssistant());
        panel.addView(close);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(290), WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(12);
        params.y = dp(110);
        try {
            floatingWindowManager.addView(panel, params);
            floatingAssistantView = panel;
            toast("المساعد العائم يعمل فوق التطبيقات.");
        } catch (Exception e) {
            floatingAssistantView = null;
            toast("لم يتمكن Android من إظهار النافذة العائمة.");
        }
    }

    private void removeFloatingAssistant() {
        if (floatingWindowManager != null && floatingAssistantView != null) {
            try { floatingWindowManager.removeView(floatingAssistantView); }
            catch (Exception ignored) {}
        }
        floatingAssistantView = null;
    }

    private void openSelectedSplitScreen() {
        if (!hasSelection()) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            toast("تقسيم الشاشة يحتاج Android 7 أو أحدث.");
            return;
        }
        Intent intent = getPackageManager().getLaunchIntentForPackage(selectedPackage);
        if (intent == null) {
            toast("هذا التطبيق لا يوفر اختصار تشغيل.");
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchBounds(new Rect(0, 0,
                    getResources().getDisplayMetrics().widthPixels / 2,
                    getResources().getDisplayMetrics().heightPixels));
            startActivity(intent, options.toBundle());
            toast("أرسلنا طلب تقسيم الشاشة؛ القرار النهائي يعود إلى Android والتطبيق.");
        } catch (Exception e) {
            try { startActivity(intent); }
            catch (Exception ignored) { toast("تعذر تشغيل التطبيق المحدد."); }
        }
    }

    private void analyzeSelectedApp() {
        if (!hasSelection()) return;
        String version = "غير معروف";
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                version = getPackageManager().getPackageInfo(selectedPackage,
                        PackageManager.PackageInfoFlags.of(0)).versionName;
            } else {
                version = getPackageManager().getPackageInfo(selectedPackage, 0).versionName;
            }
        } catch (Exception ignored) {}
        String report = "التطبيق: " + selectedLabelText
                + "\nالحزمة: " + selectedPackage
                + "\nالإصدار: " + version
                + "\n\nالإضافات التي يمكن تجربتها من Smart App Master:"
                + "\n1. مساعد عائم واختصارات فوق التطبيق."
                + "\n2. طلب تقسيم الشاشة إذا سمح Android والتطبيق."
                + "\n3. تحليل توافق واقتراح إعدادات آمنة."
                + "\n\nلا يمكن فرض جودة بث أعلى من المصدر، أو إنشاء جواهر، أو فتح فيديو مدفوع أو بث خاص دون صلاحية. كما أن هذا التحليل محلي تجريبي وليس نموذج ذكاء اصطناعي متصل بعد.";
        new AlertDialog.Builder(this)
                .setTitle("تحليل تجريبي للتطبيق")
                .setMessage(report)
                .setPositiveButton("حسناً", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        removeFloatingAssistant();
        super.onDestroy();
    }

    private void addSection(String value) {
        TextView t = text(value, 18, TEXT, true);
        root.addView(t);
        addGap(root, 8);
    }

    private void addCard(View view) {
        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setPadding(dp(14), dp(13), dp(14), dp(13));
        holder.setBackground(round(PANEL, 14));
        holder.addView(view, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        root.addView(holder, lp);
    }

    private Button button(String value) {
        Button b = makeButton(value, BLUE);
        return b;
    }

    private Button secondaryButton(String value) {
        return makeButton(value, PANEL_2);
    }

    private Button makeButton(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(48));
        b.setBackground(round(color, 12));
        b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(5);
        lp.bottomMargin = dp(3);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(dp(3), 1.0f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void addGap(LinearLayout layout, int height) {
        View v = new View(this);
        layout.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }
}
