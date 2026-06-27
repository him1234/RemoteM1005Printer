package com.him1234.remotem1005;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 主界面：提供远程打印、扫描、状态查看和设备控制入口。 */
public class MainActivity extends Activity {
    private static final int MENU_SETTINGS = 1;
    private static final int MENU_ABOUT = 2;
    private static final int REQUEST_PRINT_FILE = 1001;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final String[] paperValues = new String[]{"A4", "A5", "A6", "B5", "JB5", "Letter", "Legal", "Executive", "Statement", "Folio", "Oficio", "16k", "Env10", "EnvDL", "EnvC5", "EnvMonarch"};
    private final String[] orientationValues = new String[]{"portrait", "landscape"};
    private final String[] dpiValues = new String[]{"75", "100", "150", "200", "300", "400", "600", "1200"};
    private final String[] modeValues = new String[]{"Color", "Gray", "Lineart"};
    private final String[] scanFormatValues = new String[]{"PDF", "PNG"};

    private Spinner paperSpinner;
    private Spinner orientationSpinner;
    private Spinner scanDpiSpinner;
    private Spinner scanModeSpinner;
    private Spinner scanFormatSpinner;
    private TextView topStatusText;
    private TextView topMetaText;
    private Dialog loadingDialog;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        refreshTopStatus("设备状态", "正在读取打印机和扫描仪状态...");
        refreshDeviceStatus(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDeviceStatus(false);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        dismissLoading();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PRINT_FILE && resultCode == RESULT_OK && data != null) {
            handlePrintPick(data.getData());
        }
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("Orange Pi HP M1005");
        toolbar.setSubtitle("远程打印与扫描");
        setupToolbarMenu(toolbar);
        shell.addView(toolbar, matchWrap());

        LinearLayout infoBar = new LinearLayout(this);
        infoBar.setOrientation(LinearLayout.VERTICAL);
        infoBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        topStatusText = text(android.R.style.TextAppearance_Material_Title, 0);
        topMetaText = text(android.R.style.TextAppearance_Material_Body1, 0);
        topMetaText.setPadding(0, dp(6), 0, 0);
        infoBar.addView(topStatusText, matchWrap());
        infoBar.addView(topMetaText, matchWrap());
        shell.addView(infoBar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        LinearLayout printSection = section(root, "文件打印");
        printSection.addView(bodyText("从手机选择 PDF、Word、图片或文本文件，提交到 Orange Pi 后端打印。"));
        paperSpinner = dropdown(paperValues);
        orientationSpinner = dropdown(orientationValues);
        printSection.addView(labeledInput("纸张", paperSpinner));
        printSection.addView(labeledInput("方向", orientationSpinner));
        printSection.addView(button("选择文件并上传打印", v -> pickPrintFile()));

        LinearLayout scanSection = section(root, "扫描到手机");
        scanSection.addView(bodyText("可选择 PDF 或 PNG 输出。扫描完成后先进入 App 内预览页，再由你选择保存或放弃。"));
        scanDpiSpinner = dropdown(dpiValues);
        scanModeSpinner = dropdown(modeValues);
        scanFormatSpinner = dropdown(scanFormatValues);
        scanSection.addView(labeledInput("DPI", scanDpiSpinner));
        scanSection.addView(labeledInput("模式", scanModeSpinner));
        scanSection.addView(labeledInput("输出格式", scanFormatSpinner));
        scanSection.addView(button("开始扫描并预览", v -> startScan()));

        LinearLayout serviceSection = section(root, "系统打印服务");
        serviceSection.addView(bodyText("启用 Android 打印服务后，其他 App 可以通过系统打印菜单选择这台 HP M1005。"));
        serviceSection.addView(button("打开 Android 打印设置", v -> openPrintSettings()));

        LinearLayout toolsSection = section(root, "设备控制");
        toolsSection.addView(button("刷新设备状态", v -> refreshDeviceStatus(true)));
        toolsSection.addView(button("唤醒 LCD 背光", v -> wakeLcd()));
        toolsSection.addView(button("打开 Orange Pi 网页", v -> openWebUi()));

        shell.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        InsetUtils.apply(this, toolbar, scroll);
        return shell;
    }

    private void setupToolbarMenu(Toolbar toolbar) {
        toolbar.getMenu().add(0, MENU_SETTINGS, 0, "设置")
                .setIcon(android.R.drawable.ic_menu_manage)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        toolbar.getMenu().add(0, MENU_ABOUT, 1, "关于此软件")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SETTINGS) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == MENU_ABOUT) {
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            }
            return false;
        });
    }

    private void refreshDeviceStatus(boolean showDialog) {
        if (showDialog) {
            showLoading("正在读取设备状态...");
        }
        worker.execute(() -> {
            try {
                String printer = OrangePiClient.friendlyText(OrangePiClient.getText(ConfigStore.getBaseUrl(this), "/api/status"));
                String scanner = OrangePiClient.friendlyText(OrangePiClient.getText(ConfigStore.getBaseUrl(this), "/api/scanner"));
                runOnUiThread(() -> {
                    if (showDialog) {
                        dismissLoading();
                    }
                    refreshTopStatus("设备状态", "打印机: " + compact(printer) + "\n扫描仪: " + compact(scanner));
                });
            } catch (Exception exc) {
                runOnUiThread(() -> {
                    if (showDialog) {
                        dismissLoading();
                    }
                    refreshTopStatus("设备状态读取失败", exc.getMessage());
                });
            }
        });
    }

    private String compact(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "未知";
        }
        return text.trim().replace("\r", " ").replace("\n", " ");
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
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/jpeg",
                "image/png",
                "text/plain"
        });
        startActivityForResult(intent, REQUEST_PRINT_FILE);
    }

    private void startScan() {
        String dpi = selected(scanDpiSpinner);
        String mode = selected(scanModeSpinner);
        String format = selected(scanFormatSpinner);
        showLoading("正在扫描，请等待...");
        refreshTopStatus("扫描中", "DPI " + dpi + " / " + mode + " / " + format);
        worker.execute(() -> {
            try {
                File temp = OrangePiClient.scanToTempFile(this, dpi, mode, format);
                Intent intent = new Intent(this, ScanPreviewActivity.class);
                intent.putExtra(ScanPreviewActivity.EXTRA_TEMP_PATH, temp.getAbsolutePath());
                intent.putExtra(ScanPreviewActivity.EXTRA_MIME_TYPE, OrangePiClient.scanMimeType(format));
                intent.putExtra(ScanPreviewActivity.EXTRA_DEFAULT_NAME, OrangePiClient.defaultScanFileName(format));
                runOnUiThread(() -> {
                    dismissLoading();
                    refreshTopStatus("扫描完成", "请在预览页选择保存或放弃。");
                    startActivity(intent);
                });
            } catch (Exception exc) {
                runOnUiThread(() -> {
                    dismissLoading();
                    refreshTopStatus("扫描失败", exc.getMessage());
                });
            }
        });
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
        String paper = selected(paperSpinner);
        String orientation = selected(orientationSpinner);
        runTask("正在上传打印文件...", () -> OrangePiClient.friendlyText(OrangePiClient.uploadPrintUri(this, uri, paper, orientation, 1)));
    }

    private void runTask(String runningText, Task task) {
        refreshTopStatus(runningText, "请稍候");
        showLoading(runningText);
        worker.execute(() -> {
            try {
                String result = task.run();
                runOnUiThread(() -> {
                    dismissLoading();
                    refreshTopStatus("操作完成", result);
                    refreshDeviceStatus(false);
                });
            } catch (Exception exc) {
                runOnUiThread(() -> {
                    dismissLoading();
                    refreshTopStatus("操作失败", exc.getMessage());
                });
            }
        });
    }

    private void refreshTopStatus(String title, String detail) {
        if (topStatusText != null) {
            topStatusText.setText(title == null ? "" : title);
        }
        if (topMetaText != null) {
            topMetaText.setText(detail == null ? "" : detail);
        }
    }

    private void showLoading(String text) {
        if (loadingDialog == null) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(dp(28), dp(24), dp(28), dp(18));
            ProgressBar progress = new ProgressBar(this);
            box.addView(progress);
            loadingText = text(android.R.style.TextAppearance_Material_Body1, 0);
            loadingText.setGravity(Gravity.CENTER);
            loadingText.setPadding(0, dp(16), 0, 0);
            box.addView(loadingText, matchWrap());
            loadingDialog = new AlertDialog.Builder(this)
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

    private LinearLayout section(LinearLayout root, String title) {
        TextView label = sectionLabel(title);
        LinearLayout.LayoutParams labelLp = matchWrap();
        labelLp.topMargin = root.getChildCount() == 0 ? 0 : dp(18);
        root.addView(label, labelLp);

        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(8), 0, dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(section, lp);
        return section;
    }

    private TextView sectionLabel(String text) {
        TextView view = text(android.R.style.TextAppearance_Material_Medium, android.graphics.Typeface.BOLD);
        view.setText(text);
        view.setAllCaps(false);
        return view;
    }

    private TextView bodyText(String text) {
        TextView view = text(android.R.style.TextAppearance_Material_Body1, 0);
        view.setText(text);
        view.setLineSpacing(0, 1.1f);
        view.setPadding(0, 0, 0, dp(12));
        return view;
    }

    private View labeledInput(String label, View field) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, dp(12));
        TextView text = text(android.R.style.TextAppearance_Material_Caption, android.graphics.Typeface.BOLD);
        text.setText(label);
        text.setPadding(0, 0, 0, dp(4));
        box.addView(text);
        box.addView(field, matchWrap());
        return box;
    }

    private Spinner dropdown(String[] values) {
        Spinner view = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        view.setAdapter(adapter);
        return view;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        button.setLayoutParams(lp);
        return button;
    }

    private String selected(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        return item == null ? "" : String.valueOf(item);
    }

    private TextView text(int appearance, int style) {
        TextView view = new TextView(this);
        view.setTextAppearance(appearance);
        if (style != 0) {
            view.setTypeface(view.getTypeface(), style);
        }
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
