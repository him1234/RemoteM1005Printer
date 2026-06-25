package com.him1234.remotem1005;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

/** 设置页：配置 Orange Pi 后端地址和可选 PIN。 */
public class SettingsActivity extends AppCompatActivity {
    private TextInputEditText baseUrlInput;
    private TextInputEditText pinInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        baseUrlInput.setText(ConfigStore.getBaseUrl(this));
        pinInput.setText(ConfigStore.getPin(this));
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("设置");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        shell.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        MaterialCardView card = card();
        addToCard(card, title("连接配置"));
        addToCard(card, body("填写 Orange Pi 上 printer-web 服务地址。示例：http://192.168.1.50:8080"));
        baseUrlInput = input("后端地址", false);
        pinInput = input("访问 PIN，未启用就留空", true);
        addToCard(card, inputLayout("后端地址", baseUrlInput));
        addToCard(card, inputLayout("访问 PIN", pinInput));
        addToCard(card, button("保存配置", v -> saveConfig()));
        root.addView(card, matchWrap());

        shell.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        InsetUtils.apply(this, toolbar, scroll);
        return shell;
    }

    private void saveConfig() {
        ConfigStore.save(this,
                baseUrlInput.getText() == null ? "" : baseUrlInput.getText().toString(),
                pinInput.getText() == null ? "" : pinInput.getText().toString());
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private TextInputEditText input(String hint, boolean password) {
        TextInputEditText edit = new TextInputEditText(this);
        edit.setSingleLine(true);
        if (password) {
            edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        }
        return edit;
    }

    private TextInputLayout inputLayout(String hint, TextInputEditText edit) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.addView(edit);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(12);
        layout.setLayoutParams(lp);
        return layout;
    }

    private MaterialTextView title(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        return view;
    }

    private MaterialTextView body(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        view.setPadding(0, dp(8), 0, 0);
        return view;
    }

    private MaterialButton button(String text, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(16);
        button.setLayoutParams(lp);
        return button;
    }

    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setUseCompatPadding(true);
        card.setRadius(dp(16));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(content);
        return card;
    }

    private void addToCard(MaterialCardView card, View child) {
        ((LinearLayout) card.getChildAt(0)).addView(child);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
