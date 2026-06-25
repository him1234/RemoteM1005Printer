package com.him1234.remotem1005;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 主界面：保留常用操作，配置和关于入口放到右上角菜单。 */
public class MainActivity extends AppCompatActivity {
    private static final int MENU_SETTINGS = 1;
    private static final int MENU_ABOUT = 2;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final String[] paperValues = new String[]{"A4", "A5", "A6", "B5", "JB5", "Letter", "Legal", "Executive", "Statement", "Folio", "Oficio", "16k", "Env10", "EnvDL", "EnvC5", "EnvMonarch"};
    private final String[] orientationValues = new String[]{"portrait", "landscape"};
    private final String[] dpiValues = new String[]{"75", "100", "150", "200", "300", "400", "600", "1200"};
    private final String[] modeValues = new String[]{"Color", "Gray", "Lineart"};

    private MaterialAutoCompleteTextView paperSpinner;
    private MaterialAutoCompleteTextView orientationSpinner;
    private MaterialAutoCompleteTextView scanDpiSpinner;
    private MaterialAutoCompleteTextView scanModeSpinner;
    private MaterialTextView statusText;
    private Dialog loadingDialog;
    private MaterialTextView loadingText;
    private ActivityResultLauncher<String[]> printFileLauncher;
    private ActivityResultLauncher<String> scanFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        printFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handlePrintPick);
        scanFileLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), this::handleScanTarget);
        setContentView(buildContent());
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        dismissLoading();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, "设置")
                .setIcon(android.R.drawable.ic_menu_manage)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_ABOUT, 1, "关于此软件")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == MENU_SETTINGS) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == MENU_ABOUT) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Orange Pi HP M1005");
        toolbar.setSubtitle("远程打印与扫描");
        setSupportActionBar(toolbar);
        shell.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        MaterialCardView printCard = card();
        addToCard(printCard, sectionLabel("文件打印"));
        addToCard(printCard, bodyText("从手机选择 PDF、Word、图片或文本文件，提交到 Orange Pi 后端打印。"));
        paperSpinner = dropdown(paperValues);
        orientationSpinner = dropdown(orientationValues);
        addToCard(printCard, labeledInput("纸张", paperSpinner));
        addToCard(printCard, labeledInput("方向", orientationSpinner));
        addToCard(printCard, button("选择文件并上传打印", v -> pickPrintFile()));
        root.addView(printCard, matchWrap());

        MaterialCardView scanCard = card();
        addToCard(scanCard, sectionLabel("扫描到手机"));
        addToCard(scanCard, bodyText("高 DPI 扫描可能持续较长时间，等待期间请保持手机与 Orange Pi 在同一网络。"));
        scanDpiSpinner = dropdown(dpiValues);
        scanModeSpinner = dropdown(modeValues);
        addToCard(scanCard, labeledInput("DPI", scanDpiSpinner));
        addToCard(scanCard, labeledInput("模式", scanModeSpinner));
        addToCard(scanCard, button("开始扫描并保存 PDF", v -> createScanTarget()));
        root.addView(scanCard, matchWrap());

        MaterialCardView serviceCard = card();
        addToCard(serviceCard, sectionLabel("系统打印服务"));
        addToCard(serviceCard, bodyText("启用 Android 打印服务后，其他应用可以通过系统打印菜单选择这台 HP M1005。"));
        addToCard(serviceCard, button("打开 Android 打印设置", v -> openPrintSettings()));
        root.addView(serviceCard, matchWrap());

        MaterialCardView toolsCard = card();
        addToCard(toolsCard, sectionLabel("设备控制"));
        addToCard(toolsCard, button("测试设备状态", v -> testStatus()));
        addToCard(toolsCard, button("唤醒 LCD 背光", v -> wakeLcd()));
        addToCard(toolsCard, button("打开 Orange Pi 网页", v -> openWebUi()));
        root.addView(toolsCard, matchWrap());

        statusText = new MaterialTextView(this);
        statusText.setText("Ready");
        statusText.setPadding(dp(8), dp(16), dp(8), 0);
        root.addView(statusText, matchWrap());

        shell.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        InsetUtils.apply(this, toolbar, scroll);
        return shell;
    }

    private void testStatus() {
        runTask("正在读取设备状态...", () -> {
            String printer = OrangePiClient.friendlyText(OrangePiClient.getText(ConfigStore.getBaseUrl(this), "/api/status"));
            String scanner = OrangePiClient.friendlyText(OrangePiClient.getText(ConfigStore.getBaseUrl(this), "/api/scanner"));
            return "打印机状态:\n" + printer + "\n\n扫描仪状态:\n" + scanner;
        });
    }

    private void openWebUi() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigStore.getBaseUrl(this))));
    }

    private void openPrintSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_PRINT_SETTINGS));
        } catch (Exception exc) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void pickPrintFile() {
        printFileLauncher.launch(new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/jpeg",
                "image/png",
                "text/plain"
        });
    }

    private void createScanTarget() {
        scanFileLauncher.launch("m1005-scan.pdf");
    }

    private void wakeLcd() {
        Map<String, String> fields = new LinkedHashMap<>();
        String pin = ConfigStore.getPin(this);
        if (!pin.isEmpty()) {
            fields.put("pin", pin);
        }
        runTask("正在唤醒 LCD...", () -> OrangePiClient.friendlyText(OrangePiClient.postFormText(ConfigStore.getBaseUrl(this), "/lcd/wake", fields)));
    }

    private void handlePrintPick(Uri uri) {
        if (uri == null) {
            return;
        }
        String paper = String.valueOf(paperSpinner.getText());
        String orientation = String.valueOf(orientationSpinner.getText());
        runTask("正在上传打印文件...", () -> OrangePiClient.friendlyText(OrangePiClient.uploadPrintUri(this, uri, paper, orientation, 1)));
    }

    private void handleScanTarget(Uri uri) {
        if (uri == null) {
            return;
        }
        String dpi = String.valueOf(scanDpiSpinner.getText());
        String mode = String.valueOf(scanModeSpinner.getText());
        runTask("正在扫描，请等待...", () -> {
            OrangePiClient.scanToUri(this, uri, dpi, mode);
            return "扫描完成，文件已保存到你选择的位置。";
        });
    }

    private void runTask(String runningText, Task task) {
        statusText.setText(runningText);
        showLoading(runningText);
        worker.execute(() -> {
            try {
                String result = task.run();
                runOnUiThread(() -> {
                    dismissLoading();
                    statusText.setText(result);
                });
            } catch (Exception exc) {
                runOnUiThread(() -> {
                    dismissLoading();
                    statusText.setText("失败: " + exc.getMessage());
                });
            }
        });
    }

    private void showLoading(String text) {
        if (loadingDialog == null) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(dp(28), dp(24), dp(28), dp(18));
            ProgressBar progress = new ProgressBar(this);
            box.addView(progress);
            loadingText = new MaterialTextView(this);
            loadingText.setGravity(Gravity.CENTER);
            loadingText.setPadding(0, dp(16), 0, 0);
            box.addView(loadingText, matchWrap());
            loadingDialog = new MaterialAlertDialogBuilder(this)
                    .setView(box)
                    .create();
            loadingDialog.setCanceledOnTouchOutside(false);
        }
        loadingText.setText(text);
        loadingDialog.show();
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private interface Task {
        String run() throws Exception;
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setUseCompatPadding(true);
        card.setRadius(dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(content);
        return card;
    }

    private void addToCard(MaterialCardView card, View child) {
        ((LinearLayout) card.getChildAt(0)).addView(child);
    }

    private MaterialTextView sectionLabel(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private MaterialTextView bodyText(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        view.setPadding(0, 0, 0, dp(12));
        return view;
    }

    private View labeledInput(String label, View field) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, dp(12));
        MaterialTextView t = new MaterialTextView(this);
        t.setText(label);
        t.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        box.addView(t);
        box.addView(field, matchWrap());
        return box;
    }

    private MaterialAutoCompleteTextView dropdown(String[] values) {
        MaterialAutoCompleteTextView view = new MaterialAutoCompleteTextView(this);
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, values));
        view.setText(values[0], false);
        view.setHint("请选择");
        return view;
    }

    private MaterialButton button(String text, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        button.setLayoutParams(lp);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
