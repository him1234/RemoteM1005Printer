package com.him1234.remotem1005;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Material 3 风格主界面。 */
public class MainActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final String[] paperValues = new String[]{"A4", "A5", "A6", "B5", "JB5", "Letter", "Legal", "Executive", "Statement", "Folio", "Oficio", "16k", "Env10", "EnvDL", "EnvC5", "EnvMonarch"};
    private final String[] orientationValues = new String[]{"portrait", "landscape"};
    private final String[] dpiValues = new String[]{"75", "100", "150", "200", "300", "400", "600", "1200"};
    private final String[] modeValues = new String[]{"Color", "Gray", "Lineart"};

    private TextInputEditText baseUrlInput;
    private TextInputEditText pinInput;
    private MaterialAutoCompleteTextView paperSpinner;
    private MaterialAutoCompleteTextView orientationSpinner;
    private MaterialAutoCompleteTextView scanDpiSpinner;
    private MaterialAutoCompleteTextView scanModeSpinner;
    private MaterialTextView statusText;
    private ActivityResultLauncher<String[]> printFileLauncher;
    private ActivityResultLauncher<String> scanFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        printFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handlePrintPick);
        scanFileLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), this::handleScanTarget);
        setContentView(buildContent());
        loadConfigToUi();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(root);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Orange Pi HP M1005");
        toolbar.setSubtitle("远程打印与扫描");
        root.addView(toolbar, matchWrap());

        MaterialCardView connectionCard = card();
        addToCard(connectionCard, sectionLabel("连接设置"));
        baseUrlInput = textField("Orange Pi 地址，例如 http://192.168.1.50:8080", false);
        pinInput = textField("访问 PIN，未启用就留空", true);
        addToCard(connectionCard, labeledInput("后端地址", baseUrlInput));
        addToCard(connectionCard, labeledInput("访问 PIN", pinInput));
        addToCard(connectionCard, actionRow(
                button("保存配置", v -> saveConfig()),
                button("测试状态", v -> testStatus())
        ));
        root.addView(connectionCard, matchWrap());

        MaterialCardView printCard = card();
        addToCard(printCard, sectionLabel("文件打印"));
        paperSpinner = dropdown(paperValues);
        orientationSpinner = dropdown(orientationValues);
        addToCard(printCard, labeledInput("纸张", paperSpinner));
        addToCard(printCard, labeledInput("方向", orientationSpinner));
        addToCard(printCard, button("选择文件并上传打印", v -> pickPrintFile()));
        root.addView(printCard, matchWrap());

        MaterialCardView scanCard = card();
        addToCard(scanCard, sectionLabel("扫描到手机"));
        scanDpiSpinner = dropdown(dpiValues);
        scanModeSpinner = dropdown(modeValues);
        addToCard(scanCard, labeledInput("DPI", scanDpiSpinner));
        addToCard(scanCard, labeledInput("模式", scanModeSpinner));
        addToCard(scanCard, button("开始扫描并保存 PDF", v -> createScanTarget()));
        root.addView(scanCard, matchWrap());

        MaterialCardView lcdCard = card();
        addToCard(lcdCard, sectionLabel("LCD"));
        addToCard(lcdCard, button("唤醒 LCD 背光", v -> wakeLcd()));
        root.addView(lcdCard, matchWrap());

        MaterialCardView serviceCard = card();
        addToCard(serviceCard, sectionLabel("系统打印服务"));
        addToCard(serviceCard, button("打开 Android 打印设置", v -> openPrintSettings()));
        addToCard(serviceCard, button("打开 Orange Pi 网页", v -> openWebUi()));
        root.addView(serviceCard, matchWrap());

        statusText = new MaterialTextView(this);
        statusText.setText("Ready");
        statusText.setPadding(dp(8), dp(16), dp(8), 0);
        root.addView(statusText, matchWrap());
        return scroll;
    }

    private void loadConfigToUi() {
        baseUrlInput.setText(ConfigStore.getBaseUrl(this));
        pinInput.setText(ConfigStore.getPin(this));
    }

    private void saveConfig() {
        ConfigStore.save(this,
                baseUrlInput.getText() == null ? "" : baseUrlInput.getText().toString(),
                pinInput.getText() == null ? "" : pinInput.getText().toString());
        toast("配置已保存");
    }

    private void testStatus() {
        saveConfig();
        runTask("正在读取状态...", () -> {
            String printer = OrangePiClient.friendlyText(OrangePiClient.getText(ConfigStore.getBaseUrl(this), "/api/status"));
            String scanner = OrangePiClient.friendlyText(OrangePiClient.getText(ConfigStore.getBaseUrl(this), "/api/scanner"));
            return "打印机状态:\n" + printer + "\n\n扫描仪状态:\n" + scanner;
        });
    }

    private void openWebUi() {
        saveConfig();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigStore.getBaseUrl(this))));
    }

    private void openPrintSettings() {
        saveConfig();
        try {
            startActivity(new Intent(Settings.ACTION_PRINT_SETTINGS));
        } catch (Exception exc) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void pickPrintFile() {
        saveConfig();
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
        saveConfig();
        scanFileLauncher.launch("m1005-scan.pdf");
    }

    private void wakeLcd() {
        saveConfig();
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
        runTask("正在扫描，可能需要几十秒...", () -> {
            OrangePiClient.scanToUri(this, uri, dpi, mode);
            return "扫描完成，文件已保存到你刚才选择的位置。";
        });
    }

    private void runTask(String runningText, Task task) {
        statusText.setText(runningText);
        worker.execute(() -> {
            try {
                String result = task.run();
                runOnUiThread(() -> statusText.setText(result));
            } catch (Exception exc) {
                runOnUiThread(() -> statusText.setText("失败: " + exc.getMessage()));
            }
        });
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

    private LinearLayout contentOf(MaterialCardView card) {
        return (LinearLayout) card.getChildAt(0);
    }

    private void addToCard(MaterialCardView card, View child) {
        contentOf(card).addView(child);
    }

    private MaterialTextView sectionLabel(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
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

    private TextInputEditText textField(String hint, boolean password) {
        TextInputEditText edit = new TextInputEditText(this);
        edit.setSingleLine(true);
        if (password) {
            edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        }
        edit.setHint(hint);
        return edit;
    }

    private MaterialAutoCompleteTextView dropdown(String[] values) {
        MaterialAutoCompleteTextView view = new MaterialAutoCompleteTextView(this);
        view.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, values));
        view.setText(values[0], false);
        view.setHint("请选择");
        return view;
    }

    private View actionRow(MaterialButton left, MaterialButton right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
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

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
