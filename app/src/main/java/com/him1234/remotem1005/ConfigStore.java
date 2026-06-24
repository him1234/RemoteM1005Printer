package com.him1234.remotem1005;

import android.content.Context;
import android.content.SharedPreferences;

/** 保存 Orange Pi Web 后端地址和可选 PIN，普通界面与系统打印服务共用同一份配置。 */
final class ConfigStore {
    private static final String PREFS = "remote_m1005_config";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_PIN = "pin";
    private static final String DEFAULT_BASE_URL = "http://192.168.1.100:8080";

    private ConfigStore() {
    }

    static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return OrangePiClient.normalizeBaseUrl(prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL));
    }

    static String getPin(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String pin = prefs.getString(KEY_PIN, "");
        return pin == null ? "" : pin.trim();
    }

    static void save(Context context, String baseUrl, String pin) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BASE_URL, OrangePiClient.normalizeBaseUrl(baseUrl))
                .putString(KEY_PIN, pin == null ? "" : pin.trim())
                .apply();
    }
}
