package com.him1234.remotem1005;

import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Android 系统打印服务：把系统生成的 PDF spool 转发到 Orange Pi。 */
public class RemotePrintService extends PrintService {
    private static final String LOCAL_PRINTER_ID = "orange-pi-hp-m1005";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    public PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        return new Discovery();
    }

    @Override
    protected void onPrintJobQueued(PrintJob job) {
        worker.execute(() -> submitJob(job));
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob job) {
        job.cancel();
    }

    @Override
    public void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void submitJob(PrintJob job) {
        try {
            if (!job.start()) {
                return;
            }
            PrintJobInfo info = job.getInfo();
            String paper = choosePaper(info);
            String orientation = chooseOrientation(info);
            int copies = Math.max(1, info.getCopies());

            ParcelFileDescriptor pfd = job.getDocument().getData();
            if (pfd == null) {
                job.fail("Android 未提供打印数据");
                return;
            }

            OrangePiClient.uploadAndroidPrintJob(
                    this,
                    () -> new ParcelFileDescriptor.AutoCloseInputStream(pfd),
                    paper,
                    orientation,
                    copies
            );
            job.complete();
        } catch (Exception exc) {
            job.fail(exc.getMessage());
        }
    }

    private String choosePaper(PrintJobInfo info) {
        PrintAttributes attrs = info.getAttributes();
        if (attrs == null || attrs.getMediaSize() == null) {
            return "A4";
        }
        String id = attrs.getMediaSize().getId();
        if (id == null) {
            return "A4";
        }
        String upper = id.toUpperCase();
        if (upper.contains("LETTER")) {
            return "Letter";
        }
        if (upper.contains("LEGAL")) {
            return "Legal";
        }
        if (upper.contains("A5")) {
            return "A5";
        }
        if (upper.contains("A6")) {
            return "A6";
        }
        return "A4";
    }

    private String chooseOrientation(PrintJobInfo info) {
        PrintAttributes attrs = info.getAttributes();
        if (attrs != null && attrs.getMediaSize() != null && attrs.getMediaSize().isLandscape()) {
            return "landscape";
        }
        return "portrait";
    }

    private final class Discovery extends PrinterDiscoverySession {
        private PrinterId printerId;

        @Override
        public void onStartPrinterDiscovery(List<PrinterId> priorityList) {
            addPrinter();
        }

        @Override
        public void onStopPrinterDiscovery() {
        }

        @Override
        public void onValidatePrinters(List<PrinterId> printerIds) {
            addPrinter();
        }

        @Override
        public void onStartPrinterStateTracking(PrinterId printerId) {
            addPrinter();
        }

        @Override
        public void onStopPrinterStateTracking(PrinterId printerId) {
        }

        @Override
        public void onDestroy() {
        }

        private void addPrinter() {
            if (printerId == null) {
                printerId = generatePrinterId(LOCAL_PRINTER_ID);
            }
            PrinterInfo printer = new PrinterInfo.Builder(printerId, "HP LaserJet M1005 @ Orange Pi", PrinterInfo.STATUS_IDLE)
                    .setDescription(ConfigStore.getBaseUrl(RemotePrintService.this))
                    .build();
            addPrinters(Collections.singletonList(printer));
        }
    }
}
