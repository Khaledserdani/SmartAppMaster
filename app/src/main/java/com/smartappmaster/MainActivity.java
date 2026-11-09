package com.smartappmaster;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 17, 29);
    private final int panel = Color.rgb(22, 32, 49);
    private final int accent = Color.rgb(68, 151, 255);
    private LinearLayout root;
    private LinearLayout appList;
    private TextView selectedLabel;
    private SharedPreferences prefs;
    private String selectedPackage = "";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        prefs = getSharedPreferences("smart_app_master", MODE_PRIVATE);
        selectedPackage = prefs.getString("selected_package", "");
        buildScreen();
        loadApps();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(30));
        root.setBackgroundColor(bg);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("SMART APP MASTER", 24, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = text("مساعدك لإدارة وتحسين تجربة تطبيقاتك", 14, 0xFFB7C5D8, false);
        subtitle.setGravity(Gravity.CENTER);
        addGap(root, 8);
        root.addView(subtitle);

        addGap(root, 22);
        addSection("التطبيق المختار");
        selectedLabel = text("لم تختر أي تطبيق بعد", 16, Color.WHITE, true);
        addCard(selectedLabel);

        addGap(root, 14);
        addSection("مساعد الخصوصية");
        Switch privacy = new Switch(this);
        privacy.setText("تفعيل وضع الخصوصية داخل المساعد");
        privacy.setTextColor(Color.WHITE);
        privacy.setTextSize(15);
        privacy.setChecked(prefs.getBoolean("privacy_mode", false));
        privacy.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean("privacy_mode", checked).apply();
            Toast.makeText(this, checked ? "تم تفعيل الوضع داخل المساعد" : "تم إيقاف الوضع", Toast.LENGTH_SHORT).show();
        });
        addCard(privacy);
        TextView privacyNote = text("هذا الخيار يحفظ تفضيلك داخل التطبيق فقط. التحكم بإشعارات التطبيقات الأخرى يحتاج صلاحيات Android منفصلة.", 12, 0xFF9EACC0, false);
        addCard(privacyNote);

        addGap(root, 14);
        addSection("أدوات التطبيق");
        Button save = button("حفظ ملف إعدادات التطبيق المختار");
        save.setOnClickListener(v -> {
            if (selectedPackage.isEmpty()) {
                Toast.makeText(this, "اختر تطبيقاً من القائمة أولاً", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putBoolean("profile_" + selectedPackage, true).apply();
                Toast.makeText(this, "تم حفظ ملف الإعدادات الأساسي", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(save);

        Button launch = button("فتح التطبيق المختار");
        launch.setOnClickListener(v -> {
            if (selectedPackage.isEmpty()) {
                Toast.makeText(this, "اختر تطبيقاً أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = getPackageManager().getLaunchIntentForPackage(selectedPackage);
            if (intent != null) startActivity(intent);
            else Toast.makeText(this, "تعذر فتح هذا التطبيق", Toast.LENGTH_SHORT).show();
        });
        root.addView(launch);

        addGap(root, 20);
        addSection("التطبيقات المثبتة");
        TextView hint = text("اضغط على أي تطبيق لاختياره وإعداد ملفه الخاص.", 12, 0xFF9EACC0, false);
        root.addView(hint);
        addGap(root, 8);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        root.addView(appList);

        addGap(root, 18);
        TextView footer = text("الإصدار التجريبي 0.1.0 • الميزات المتقدمة تُضاف بعد الاختبار", 11, 0xFF718198, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);
    }

    private void loadApps() {
        appList.removeAllViews();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });

        for (ResolveInfo info : apps) {
            String packageName = info.activityInfo.packageName;
            String label = info.loadLabel(pm).toString();
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackground(round(panel, 14));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            appList.addView(item, lp);

            TextView name = text(label, 16, Color.WHITE, true);
            item.addView(name);
            TextView pkg = text(packageName, 10, 0xFF91A1B7, false);
            item.addView(pkg);
            item.setOnClickListener(v -> {
                selectedPackage = packageName;
                prefs.edit().putString("selected_package", packageName).apply();
                selectedLabel.setText(label + "\n" + packageName);
                Toast.makeText(this, "تم اختيار " + label, Toast.LENGTH_SHORT).show();
            });
            if (packageName.equals(selectedPackage)) {
                selectedLabel.setText(label + "\n" + packageName);
            }
        }
        if (apps.isEmpty()) {
            appList.addView(text("لم نعثر على تطبيقات قابلة للتشغيل.", 14, Color.WHITE, false));
        }
    }

    private void addSection(String value) {
        TextView t = text(value, 18, Color.WHITE, true);
        root.addView(t);
        addGap(root, 8);
    }

    private void addCard(View view) {
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(14), dp(12), dp(14), dp(12));
        holder.setBackground(round(panel, 14));
        holder.addView(view);
        root.addView(holder, new LinearLayout.LayoutParams(-1, -2));
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setBackground(round(accent, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
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
}
