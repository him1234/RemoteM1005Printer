package com.him1234.remotem1005;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/** 全面屏适配：把标题栏和内容避开状态栏、刘海区和底部导航栏。 */
final class InsetUtils {
    private InsetUtils() {
    }

    static void apply(Activity activity, View toolbar, View content) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        int toolbarLeft = toolbar.getPaddingLeft();
        int toolbarTop = toolbar.getPaddingTop();
        int toolbarRight = toolbar.getPaddingRight();
        int toolbarBottom = toolbar.getPaddingBottom();
        int contentLeft = content.getPaddingLeft();
        int contentTop = content.getPaddingTop();
        int contentRight = content.getPaddingRight();
        int contentBottom = content.getPaddingBottom();

        View root = activity.getWindow().getDecorView();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            } else {
                bars = android.graphics.Insets.of(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
            }
            toolbar.setPadding(
                    toolbarLeft + bars.left,
                    toolbarTop + bars.top,
                    toolbarRight + bars.right,
                    toolbarBottom);
            content.setPadding(
                    contentLeft + bars.left,
                    contentTop,
                    contentRight + bars.right,
                    contentBottom + bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
    }
}
