package com.him1234.remotem1005;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 与 Orange Pi printer-web 后端通信的轻量客户端。 */
final class OrangePiClient {
    private OrangePiClient() {
    }

    interface StreamOpener {
        InputStream open() throws IOException;
    }

    static String normalizeBaseUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "http://192.168.1.100:8080";
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    static String getText(String baseUrl, String path) throws IOException {
        HttpURLConnection conn = open(baseUrl, path, "GET", 8000, 12000);
        return finishText(conn);
    }

    static String postFormText(String baseUrl, String path, Map<String, String> fields) throws IOException {
        byte[] body = encodeFields(fields).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = open(baseUrl, path, "POST", 8000, 20000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body);
        return finishText(conn);
    }

    static String friendlyText(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (!text.startsWith("{")) {
            return text;
        }
        try {
            JSONObject json = new JSONObject(text);
            if (json.has("text")) {
                return json.optString("text");
            }
            if (json.has("message")) {
                return json.optString("message");
            }
            if (json.has("ok")) {
                return json.optBoolean("ok") ? "成功" : "失败";
            }
        } catch (Exception ignored) {
            // 非 JSON 返回体直接展示原文，便于现场排障。
        }
        return text;
    }

    static String uploadPrintUri(Context context, Uri uri, String paper, String orientation, int copies) throws IOException {
        String fileName = queryDisplayName(context, uri);
        String contentType = context.getContentResolver().getType(uri);
        return uploadMultipart(
                ConfigStore.getBaseUrl(context),
                "/print",
                printFields(context, paper, orientation, copies),
                "document",
                fileName,
                contentType == null ? "application/octet-stream" : contentType,
                () -> context.getContentResolver().openInputStream(uri),
                180000
        );
    }

    static String uploadAndroidPrintJob(Context context, StreamOpener opener, String paper, String orientation, int copies) throws IOException {
        return uploadMultipart(
                ConfigStore.getBaseUrl(context),
                "/print",
                printFields(context, paper, orientation, copies),
                "document",
                "android-print.pdf",
                "application/pdf",
                opener,
                180000
        );
    }

    static File scanToTempFile(Context context, String resolution, String mode, String format) throws IOException {
        String normalizedFormat = normalizeScanFormat(format);
        File output = new File(context.getCacheDir(), defaultScanFileName(normalizedFormat));
        if (output.exists() && !output.delete()) {
            throw new IOException("无法覆盖旧扫描临时文件");
        }

        Map<String, String> fields = new LinkedHashMap<>();
        addPin(context, fields);
        fields.put("resolution", resolution);
        fields.put("mode", mode);
        fields.put("format", normalizedFormat);

        byte[] body = encodeFields(fields).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = open(ConfigStore.getBaseUrl(context), "/scan", "POST", 8000, 0);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        conn.setRequestProperty("X-Requested-With", "fetch");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + readStream(conn.getErrorStream()));
        }

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new FileOutputStream(output)) {
            copy(in, out);
        } finally {
            conn.disconnect();
        }
        return output;
    }

    private static Map<String, String> printFields(Context context, String paper, String orientation, int copies) {
        Map<String, String> fields = new LinkedHashMap<>();
        addPin(context, fields);
        fields.put("copies", String.valueOf(Math.max(1, copies)));
        fields.put("paper", paper == null || paper.isEmpty() ? "A4" : paper);
        fields.put("orientation", "landscape".equals(orientation) ? "landscape" : "portrait");
        fields.put("fit_to_page", "1");
        return fields;
    }

    private static void addPin(Context context, Map<String, String> fields) {
        String pin = ConfigStore.getPin(context);
        if (!pin.isEmpty()) {
            fields.put("pin", pin);
        }
    }

    private static HttpURLConnection open(String baseUrl, String path, String method, int connectMs, int readMs) throws IOException {
        URL url = new URL(normalizeBaseUrl(baseUrl) + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(connectMs);
        conn.setReadTimeout(readMs);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setRequestProperty("X-Requested-With", "fetch");
        return conn;
    }

    private static String uploadMultipart(
            String baseUrl,
            String path,
            Map<String, String> fields,
            String fileField,
            String fileName,
            String contentType,
            StreamOpener opener,
            int readTimeoutMs
    ) throws IOException {
        String boundary = "----RemoteM1005" + System.currentTimeMillis();
        HttpURLConnection conn = open(baseUrl, path, "POST", 8000, readTimeoutMs);
        conn.setDoOutput(true);
        conn.setChunkedStreamingMode(0);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream()) {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                writeAscii(out, "--" + boundary + "\r\n");
                writeAscii(out, "Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n");
                writeUtf8(out, field.getValue());
                writeAscii(out, "\r\n");
            }

            writeAscii(out, "--" + boundary + "\r\n");
            writeAscii(out, "Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + safeFileName(fileName) + "\"\r\n");
            writeAscii(out, "Content-Type: " + contentType + "\r\n\r\n");
            try (InputStream in = opener.open()) {
                if (in == null) {
                    throw new IOException("无法读取待打印文件");
                }
                copy(in, out);
            }
            writeAscii(out, "\r\n--" + boundary + "--\r\n");
        }
        return finishText(conn);
    }

    private static String finishText(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        String body = readStream(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + body);
        }
        return body;
    }

    private static String encodeFields(Map<String, String> fields) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return sb.toString();
    }

    private static String queryDisplayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
            // 内容提供器不返回文件名时回退到默认名称。
        }
        return "android-upload.pdf";
    }

    private static String safeFileName(String value) {
        String name = value == null ? "android-upload.pdf" : value.trim();
        if (name.isEmpty()) {
            name = "android-upload.pdf";
        }
        return name.replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    static String normalizeScanFormat(String format) {
        String value = format == null ? "pdf" : format.trim().toLowerCase(Locale.US);
        if ("png".equals(value)) {
            return "png";
        }
        return "pdf";
    }

    static String scanMimeType(String format) {
        return "png".equals(normalizeScanFormat(format)) ? "image/png" : "application/pdf";
    }

    static String defaultScanFileName(String format) {
        String value = normalizeScanFormat(format);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        return "scan-" + stamp + "." + value;
    }

    private static void writeAscii(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeUtf8(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String readStream(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copy(in, buffer);
        return buffer.toString("UTF-8");
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
    }
}
