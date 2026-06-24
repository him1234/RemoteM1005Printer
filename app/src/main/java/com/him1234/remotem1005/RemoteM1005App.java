package com.him1234.remotem1005;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/** 应用入口：Android 12+ 自动应用动态配色。 */
public class RemoteM1005App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
