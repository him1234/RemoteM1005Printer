package com.him1234.remotem1005;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** 全面屏适配：把标题栏和内容避开状态栏、刘海区和底部导航栏。 */
final class InsetUtils {
    private InsetUtils() {
    }

    static void apply(Activity activity, View toolbar, View content) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        int toolbarLeft = toolbar.getPaddingLeft();
        int toolbarTop = toolbar.getPaddingTop();
        int toolbarRight = toolbar.getPaddingRight();
        int toolbarBottom = toolbar.getPaddingBottom();
        int contentLeft = content.getPaddingLeft();
        int contentTop = content.getPaddingTop();
        int contentRight = content.getPaddingRight();
        int contentBottom = content.getPaddingBottom();

        View root = activity.getWindow().getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            toolbar.setPadding(toolbarLeft + bars.left, toolbarTop + bars.top, toolbarRight + bars.right, toolbarBottom);
            content.setPadding(contentLeft + bars.left, contentTop, contentRight + bars.right, contentBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
