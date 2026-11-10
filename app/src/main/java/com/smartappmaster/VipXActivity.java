package com.smartappmaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VipXActivity extends Activity {
    private static final int BG = Color.rgb(9, 13, 24);
    private static final int CARD = Color.rgb(22, 31, 49);
    private static final int ACCENT = Color.rgb(190, 153, 82);
    private static final int WHITE = Color.rgb(245, 247, 250);
    private static final int MUTED = Color.rgb(164, 176, 194);
    private static final int EXPORT_APK = 4101;

    private LinearLayout list;
    private EditText search;
    private final List<Item> apps = new ArrayList<>();
    private String selectedPackage = "";
    private String selectedName = "";
    private String pendingApkPath = "";

    private static final class Item {
        final String name, pkg;
        Item(String name, String pkg) { this.name = name; this.pkg = pkg; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        loadApps();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = label("VIP X", 30, WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = label("منصة تحليل التطبيقات واستخراج النسخ", 14, ACCENT, true);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);
        TextView notice = label("يفحص معلومات الحزمة وملفات APK المتاحة. لا يدّعي الوصول إلى قواعد البيانات الخاصة أو ملفات التطبيقات المحمية.", 12, MUTED, false);
        notice.setPadding(0, dp(14), 0, dp(14));
        root.addView(notice);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("ابحث عن تطبيق مثبت...");
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.setBackgroundColor(CARD);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { render(s.toString()); }
            public void afterTextChanged(android.text.Editable e) {}
        });

        TextView heading = label("التطبيقات المثبتة", 18, WHITE, true);
        heading.setPadding(0, dp(18), 0, dp(8));
        root.addView(heading);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        TextView footer = label("VIP X • App Inspector • الإصدار الأول", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(20), 0, 0);
        root.addView(footer);
    }

    private void loadApps() {
        apps.clear();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        for (android.content.pm.ResolveInfo ri : pm.queryIntentActivities(intent, 0)) {
            String pkg = ri.activityInfo.packageName;
            boolean seen = false;
            for (Item item : apps) if (item.pkg.equals(pkg)) { seen = true; break; }
            if (!seen) apps.add(new Item(ri.loadLabel(pm).toString(), pkg));
        }
        Collections.sort(apps, Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)));
        render("");
    }

    private void render(String filter) {
        if (list == null) return;
        list.removeAllViews();
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Item item : apps) {
            if (!item.name.toLowerCase(Locale.ROOT).contains(q) && !item.pkg.toLowerCase(Locale.ROOT).contains(q)) continue;
            count++;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackgroundColor(CARD);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(8);
            list.addView(card, cp);
            card.addView(label(item.name, 16, WHITE, true));
            card.addView(label(item.pkg, 10, MUTED, false));
            Button inspect = actionButton("تحليل الحزمة واستخراج نسخة");
            inspect.setOnClickListener(v -> inspect(item));
            card.addView(inspect);
        }
        if (count == 0) list.addView(label("لا توجد نتائج.", 13, MUTED, false));
    }

    private void inspect(Item item) {
        selectedPackage = item.pkg;
        selectedName = item.name;
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(item.pkg, 0);
            PackageInfo pi = pm.getPackageInfo(item.pkg, 0);
            File apk = new File(ai.sourceDir);
            String version = Build.VERSION.SDK_INT >= 28 ? String.valueOf(pi.getLongVersionCode()) : String.valueOf(pi.versionCode);
            String installer = "غير معروف";
            if (Build.VERSION.SDK_INT >= 30) {
                String v = pm.getInstallSourceInfo(item.pkg).getInstallingPackageName();
                if (v != null) installer = v;
            } else {
                String v = pm.getInstallerPackageName(item.pkg);
                if (v != null) installer = v;
            }
            String splits = ai.splitSourceDirs == null ? "لا توجد ملفات تقسيم ظاهرة" : ai.splitSourceDirs.length + " ملف/ملفات split";
            String hash = sha256(apk);
            String report = "الاسم: " + item.name +
                    "\nمعرّف الحزمة: " + item.pkg +
                    "\nالإصدار: " + pi.versionName + " (code " + version + ")" +
                    "\nمصدر التثبيت: " + installer +
                    "\nملف APK الأساسي: " + apk.getName() +
                    "\nالحجم: " + (apk.length() / 1024) + " KB" +
                    "\nSHA-256: " + hash +
                    "\nملفات إضافية: " + splits +
                    "\n\nحدود الوصول: أندرويد يعزل قواعد البيانات وملفات البيانات الخاصة بالتطبيقات الأخرى. هذه الأداة لا تستخرجها ولا تتجاوز حمايتها. النسخة المستخرجة قد لا تكون قابلة للتثبيت وحدها إذا كان التطبيق يعتمد على split APKs.";
            new AlertDialog.Builder(this)
                    .setTitle("تقرير تحليل التطبيق")
                    .setMessage(report)
                    .setPositiveButton("استخراج APK الأساسي", (d, w) -> chooseApkDestination(apk))
                    .setNeutralButton("نسخ التقرير", (d, w) -> {
                        android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(android.content.ClipData.newPlainText("VIP X report", report));
                        toast("تم نسخ تقرير التحليل.");
                    })
                    .setNegativeButton("إغلاق", null)
                    .show();
        } catch (Exception e) {
            toast("تعذر تحليل هذه الحزمة: " + e.getMessage());
        }
    }

    private void chooseApkDestination(File source) {
        pendingApkPath = source.getAbsolutePath();
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType("application/vnd.android.package-archive");
        create.putExtra(Intent.EXTRA_TITLE, safeName(selectedName) + ".apk");
        startActivityForResult(create, EXPORT_APK);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXPORT_APK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri destination = data.getData();
                try (FileInputStream in = new FileInputStream(pendingApkPath);
                     OutputStream out = getContentResolver().openOutputStream(destination, "w")) {
                    if (out == null) throw new IllegalStateException("تعذر فتح وجهة الحفظ");
                    byte[] buffer = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                    out.flush();
                }
                toast("اكتمل استخراج ملف APK الأساسي إلى المكان الذي اخترته.");
            } catch (Exception e) {
                toast("فشل استخراج APK: " + e.getMessage());
            }
        }
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }

    private String safeName(String value) {
        return value == null ? "application" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Button actionButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(WHITE);
        b.setBackgroundColor(ACCENT);
        b.setOnClickListener(v -> {});
        return b;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
