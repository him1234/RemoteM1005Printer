package com.him1234.remotem1005;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** 扫描结果预览页：用户确认后再通过系统文件选择器保存。 */
public class ScanPreviewActivity extends ComponentActivity {
    static final String EXTRA_TEMP_PATH = "temp_path";
    static final String EXTRA_MIME_TYPE = "mime_type";
    static final String EXTRA_DEFAULT_NAME = "default_name";

    private File tempFile;
    private String mimeType;
    private String defaultName;
    private boolean savedOrDiscarded;
    private ActivityResultLauncher<String> saveLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getIntent().getStringExtra(EXTRA_TEMP_PATH);
        tempFile = path == null ? null : new File(path);
        mimeType = getIntent().getStringExtra(EXTRA_MIME_TYPE);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/pdf";
        }
        defaultName = getIntent().getStringExtra(EXTRA_DEFAULT_NAME);
        if (defaultName == null || defaultName.trim().isEmpty()) {
            defaultName = mimeType.equals("image/png") ? "scan.png" : "scan.pdf";
        }

        saveLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(mimeType), this::saveToUri);
        setContentView(buildContent());
    }

    @Override
    public void onBackPressed() {
        discardAndFinish();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && !savedOrDiscarded) {
            deleteTempFile();
        }
        super.onDestroy();
    }

    private View buildContent() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("扫描预览");
        toolbar.setSubtitle(mimeType.equals("image/png") ? "PNG 图片" : "PDF 文档");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> discardAndFinish());
        shell.addView(toolbar, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(root);

        TextView hint = new TextView(this);
        hint.setText("扫描结果已临时保存到 App 缓存。确认无误后点击保存，系统会让你选择保存位置。");
        hint.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
        hint.setPadding(0, 0, 0, dp(12));
        root.addView(hint, matchWrap());

        ImageView preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setBackgroundColor(Color.WHITE);
        preview.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        previewLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(preview, previewLp);

        TextView info = new TextView(this);
        info.setTextAppearance(android.R.style.TextAppearance_Material_Small);
        info.setPadding(0, dp(12), 0, 0);
        root.addView(info, matchWrap());

        if (tempFile == null || !tempFile.exists()) {
            info.setText("预览失败：扫描临时文件不存在。");
        } else if (mimeType.equals("image/png")) {
            preview.setImageURI(Uri.fromFile(tempFile));
            info.setText("文件大小: " + formatSize(tempFile.length()));
        } else {
            try {
                PdfPreview pdfPreview = renderPdfFirstPage(tempFile);
                preview.setImageBitmap(pdfPreview.bitmap);
                info.setText("PDF 共 " + pdfPreview.pageCount + " 页，当前预览第 1 页。文件大小: " + formatSize(tempFile.length()));
            } catch (Exception exc) {
                info.setText("PDF 预览失败，但仍可保存文件: " + exc.getMessage());
            }
        }

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(dp(16), dp(8), dp(16), dp(16));
        Button discard = new Button(this);
        discard.setText("放弃");
        discard.setOnClickListener(v -> discardAndFinish());
        Button save = new Button(this);
        save.setText("保存");
        save.setOnClickListener(v -> saveLauncher.launch(defaultName));
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        buttonLp.setMargins(dp(4), 0, dp(4), 0);
        buttons.addView(discard, buttonLp);
        buttons.addView(save, buttonLp);

        shell.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        shell.addView(buttons, matchWrap());
        InsetUtils.apply(this, toolbar, buttons);
        return shell;
    }

    private PdfPreview renderPdfFirstPage(File file) throws Exception {
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(fd);
             PdfRenderer.Page page = renderer.openPage(0)) {
            int maxWidth = Math.min(dp(1200), Math.max(dp(320), page.getWidth() * 2));
            float scale = maxWidth / (float) page.getWidth();
            int width = Math.max(1, Math.round(page.getWidth() * scale));
            int height = Math.max(1, Math.round(page.getHeight() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return new PdfPreview(bitmap, renderer.getPageCount());
        }
    }

    private void saveToUri(Uri uri) {
        if (uri == null) {
            return;
        }
        if (tempFile == null || !tempFile.exists()) {
            Toast.makeText(this, "扫描临时文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        try (InputStream in = new FileInputStream(tempFile);
             OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
            if (out == null) {
                throw new IllegalStateException("无法打开保存位置");
            }
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            savedOrDiscarded = true;
            deleteTempFile();
            Toast.makeText(this, "扫描文件已保存", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception exc) {
            Toast.makeText(this, "保存失败: " + exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void discardAndFinish() {
        savedOrDiscarded = true;
        deleteTempFile();
        finish();
    }

    private void deleteTempFile() {
        if (tempFile != null && tempFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(java.util.Locale.US, "%.1f MB", bytes / 1024f / 1024f);
        }
        if (bytes >= 1024L) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f);
        }
        return bytes + " B";
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class PdfPreview {
        final Bitmap bitmap;
        final int pageCount;

        PdfPreview(Bitmap bitmap, int pageCount) {
            this.bitmap = bitmap;
            this.pageCount = pageCount;
        }
    }
}
