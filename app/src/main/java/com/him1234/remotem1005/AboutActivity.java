package com.him1234.remotem1005;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toolbar;

/** 关于页：说明软件用途、版本和限制。 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("关于此软件");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        shell.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        root.addView(title("Remote M1005 Printer"), matchWrap());
        root.addView(body("版本：1.1"), matchWrap());
        root.addView(body("用途：控制 Orange Pi Zero 上的 HP LaserJet M1005 打印、扫描和 LCD 状态服务。"), matchWrap());
        root.addView(body("打印限制：HP M1005 只支持黑白、单面输出。"), matchWrap());
        root.addView(body("系统打印服务需要在 Android 打印设置中手动启用。"), matchWrap());

        shell.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        InsetUtils.apply(this, toolbar, scroll);
        return shell;
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(android.R.style.TextAppearance_Material_Large);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView body(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
        view.setPadding(0, dp(10), 0, 0);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
