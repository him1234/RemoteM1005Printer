package com.him1234.remotem1005;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.ComponentActivity;

/** 设置页：配置 Orange Pi 后端地址和可选 PIN。 */
public class SettingsActivity extends ComponentActivity {
    private EditText baseUrlInput;
    private EditText pinInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        baseUrlInput.setText(ConfigStore.getBaseUrl(this));
        pinInput.setText(ConfigStore.getPin(this));
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("设置");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        shell.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        root.addView(title("连接配置"), matchWrap());
        root.addView(body("填写 Orange Pi 上 printer-web 服务地址。示例：http://192.168.1.50:8080"), matchWrap());
        baseUrlInput = input("后端地址", false);
        pinInput = input("访问 PIN", true);
        root.addView(labeledInput("后端地址", baseUrlInput));
        root.addView(labeledInput("访问 PIN，未启用就留空", pinInput));
        root.addView(button("保存配置", v -> saveConfig()));

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

    private EditText input(String hint, boolean password) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setSingleLine(true);
        if (password) {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        }
        return edit;
    }

    private View labeledInput(String label, EditText edit) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(12);
        layout.setLayoutParams(lp);
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextAppearance(android.R.style.TextAppearance_Material_Caption);
        labelView.setTypeface(labelView.getTypeface(), android.graphics.Typeface.BOLD);
        labelView.setPadding(0, 0, 0, dp(4));
        layout.addView(labelView, matchWrap());
        layout.addView(edit, matchWrap());
        return layout;
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView body(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
        view.setPadding(0, dp(8), 0, 0);
        return view;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(16);
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
