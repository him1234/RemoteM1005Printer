package com.him1234.remotem1005;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textview.MaterialTextView;

/** 关于页：说明软件用途、版本和限制。 */
public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("关于此软件");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        shell.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        MaterialCardView card = card();
        addToCard(card, title("Remote M1005 Printer"));
        addToCard(card, body("版本：1.1"));
        addToCard(card, body("用途：控制 Orange Pi Zero 上的 HP LaserJet M1005 打印、扫描和 LCD 状态服务。"));
        addToCard(card, body("打印限制：HP M1005 只支持黑白、单面输出。"));
        addToCard(card, body("系统打印服务需要在 Android 打印设置中手动启用。"));
        root.addView(card, matchWrap());

        shell.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        InsetUtils.apply(this, toolbar, scroll);
        return shell;
    }

    private MaterialTextView title(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        return view;
    }

    private MaterialTextView body(String text) {
        MaterialTextView view = new MaterialTextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        view.setPadding(0, dp(10), 0, 0);
        return view;
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
