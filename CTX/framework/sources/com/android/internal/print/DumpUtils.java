package com.android.internal.print;

import android.app.Instrumentation;
import android.app.Notification;
import android.content.Context;
import android.os.DropBoxManager;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.service.print.PrinterCapabilitiesProto;
import com.android.internal.util.dump.DualDumpOutputStream;

public class DumpUtils {
    public static void writePrinterId(DualDumpOutputStream dualDumpOutputStream, String str, long j, PrinterId printerId) {
        long jStart = dualDumpOutputStream.start(str, j);
        com.android.internal.util.dump.DumpUtils.writeComponentName(dualDumpOutputStream, "service_name", 1146756268033L, printerId.getServiceName());
        dualDumpOutputStream.write("local_id", 1138166333442L, printerId.getLocalId());
        dualDumpOutputStream.end(jStart);
    }

    public static void writePrinterCapabilities(Context context, DualDumpOutputStream dualDumpOutputStream, String str, long j, PrinterCapabilitiesInfo printerCapabilitiesInfo) {
        long jStart = dualDumpOutputStream.start(str, j);
        writeMargins(dualDumpOutputStream, "min_margins", 1146756268033L, printerCapabilitiesInfo.getMinMargins());
        int size = printerCapabilitiesInfo.getMediaSizes().size();
        for (int i = 0; i < size; i++) {
            writeMediaSize(context, dualDumpOutputStream, "media_sizes", 2246267895810L, printerCapabilitiesInfo.getMediaSizes().get(i));
        }
        int size2 = printerCapabilitiesInfo.getResolutions().size();
        for (int i2 = 0; i2 < size2; i2++) {
            writeResolution(dualDumpOutputStream, "resolutions", 2246267895811L, printerCapabilitiesInfo.getResolutions().get(i2));
        }
        if ((printerCapabilitiesInfo.getColorModes() & 1) != 0) {
            dualDumpOutputStream.write("color_modes", 2259152797700L, 1);
        }
        if ((printerCapabilitiesInfo.getColorModes() & 2) != 0) {
            dualDumpOutputStream.write("color_modes", 2259152797700L, 2);
        }
        if ((printerCapabilitiesInfo.getDuplexModes() & 1) != 0) {
            dualDumpOutputStream.write("duplex_modes", PrinterCapabilitiesProto.DUPLEX_MODES, 1);
        }
        if ((printerCapabilitiesInfo.getDuplexModes() & 2) != 0) {
            dualDumpOutputStream.write("duplex_modes", PrinterCapabilitiesProto.DUPLEX_MODES, 2);
        }
        if ((printerCapabilitiesInfo.getDuplexModes() & 4) != 0) {
            dualDumpOutputStream.write("duplex_modes", PrinterCapabilitiesProto.DUPLEX_MODES, 4);
        }
        dualDumpOutputStream.end(jStart);
    }

    public static void writePrinterInfo(Context context, DualDumpOutputStream dualDumpOutputStream, String str, long j, PrinterInfo printerInfo) {
        long jStart = dualDumpOutputStream.start(str, j);
        writePrinterId(dualDumpOutputStream, Instrumentation.REPORT_KEY_IDENTIFIER, 1146756268033L, printerInfo.getId());
        dualDumpOutputStream.write("name", 1138166333442L, printerInfo.getName());
        dualDumpOutputStream.write("status", 1159641169923L, printerInfo.getStatus());
        dualDumpOutputStream.write("description", 1138166333444L, printerInfo.getDescription());
        PrinterCapabilitiesInfo capabilities = printerInfo.getCapabilities();
        if (capabilities != null) {
            writePrinterCapabilities(context, dualDumpOutputStream, "capabilities", 1146756268037L, capabilities);
        }
        dualDumpOutputStream.end(jStart);
    }

    public static void writeMediaSize(Context context, DualDumpOutputStream dualDumpOutputStream, String str, long j, PrintAttributes.MediaSize mediaSize) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write(Instrumentation.REPORT_KEY_IDENTIFIER, 1138166333441L, mediaSize.getId());
        dualDumpOutputStream.write("label", 1138166333442L, mediaSize.getLabel(context.getPackageManager()));
        dualDumpOutputStream.write("height_mils", 1120986464259L, mediaSize.getHeightMils());
        dualDumpOutputStream.write("width_mils", 1120986464260L, mediaSize.getWidthMils());
        dualDumpOutputStream.end(jStart);
    }

    public static void writeResolution(DualDumpOutputStream dualDumpOutputStream, String str, long j, PrintAttributes.Resolution resolution) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write(Instrumentation.REPORT_KEY_IDENTIFIER, 1138166333441L, resolution.getId());
        dualDumpOutputStream.write("label", 1138166333442L, resolution.getLabel());
        dualDumpOutputStream.write("horizontal_DPI", 1120986464259L, resolution.getHorizontalDpi());
        dualDumpOutputStream.write("veritical_DPI", 1120986464260L, resolution.getVerticalDpi());
        dualDumpOutputStream.end(jStart);
    }

    public static void writeMargins(DualDumpOutputStream dualDumpOutputStream, String str, long j, PrintAttributes.Margins margins) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write("top_mils", 1120986464257L, margins.getTopMils());
        dualDumpOutputStream.write("left_mils", 1120986464258L, margins.getLeftMils());
        dualDumpOutputStream.write("right_mils", 1120986464259L, margins.getRightMils());
        dualDumpOutputStream.write("bottom_mils", 1120986464260L, margins.getBottomMils());
        dualDumpOutputStream.end(jStart);
    }

    public static void writePrintAttributes(Context context, DualDumpOutputStream dualDumpOutputStream, String str, long j, PrintAttributes printAttributes) {
        long jStart = dualDumpOutputStream.start(str, j);
        PrintAttributes.MediaSize mediaSize = printAttributes.getMediaSize();
        if (mediaSize != null) {
            writeMediaSize(context, dualDumpOutputStream, "media_size", 1146756268033L, mediaSize);
            dualDumpOutputStream.write("is_portrait", 1133871366146L, printAttributes.isPortrait());
        }
        PrintAttributes.Resolution resolution = printAttributes.getResolution();
        if (resolution != null) {
            writeResolution(dualDumpOutputStream, MediaStore.Video.VideoColumns.RESOLUTION, 1146756268035L, resolution);
        }
        PrintAttributes.Margins minMargins = printAttributes.getMinMargins();
        if (minMargins != null) {
            writeMargins(dualDumpOutputStream, "min_margings", 1146756268036L, minMargins);
        }
        dualDumpOutputStream.write("color_mode", 1159641169925L, printAttributes.getColorMode());
        dualDumpOutputStream.write("duplex_mode", 1159641169926L, printAttributes.getDuplexMode());
        dualDumpOutputStream.end(jStart);
    }

    public static void writePrintDocumentInfo(DualDumpOutputStream dualDumpOutputStream, String str, long j, PrintDocumentInfo printDocumentInfo) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write("name", 1138166333441L, printDocumentInfo.getName());
        int pageCount = printDocumentInfo.getPageCount();
        if (pageCount != -1) {
            dualDumpOutputStream.write("page_count", 1120986464258L, pageCount);
        }
        dualDumpOutputStream.write("content_type", 1120986464259L, printDocumentInfo.getContentType());
        dualDumpOutputStream.write("data_size", 1112396529668L, printDocumentInfo.getDataSize());
        dualDumpOutputStream.end(jStart);
    }

    public static void writePageRange(DualDumpOutputStream dualDumpOutputStream, String str, long j, PageRange pageRange) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write(Telephony.BaseMmsColumns.START, 1120986464257L, pageRange.getStart());
        dualDumpOutputStream.write("end", 1120986464258L, pageRange.getEnd());
        dualDumpOutputStream.end(jStart);
    }

    public static void writePrintJobInfo(Context context, DualDumpOutputStream dualDumpOutputStream, String str, long j, PrintJobInfo printJobInfo) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write("label", 1138166333441L, printJobInfo.getLabel());
        PrintJobId id = printJobInfo.getId();
        if (id != null) {
            dualDumpOutputStream.write("print_job_id", 1138166333442L, id.flattenToString());
        }
        int state = printJobInfo.getState();
        if (state < 1 || state > 7) {
            dualDumpOutputStream.write("state", 1159641169923L, 0);
        } else {
            dualDumpOutputStream.write("state", 1159641169923L, state);
        }
        PrinterId printerId = printJobInfo.getPrinterId();
        if (printerId != null) {
            writePrinterId(dualDumpOutputStream, "printer", 1146756268036L, printerId);
        }
        String tag = printJobInfo.getTag();
        if (tag != null) {
            dualDumpOutputStream.write(DropBoxManager.EXTRA_TAG, 1138166333445L, tag);
        }
        dualDumpOutputStream.write("creation_time", 1112396529670L, printJobInfo.getCreationTime());
        PrintAttributes attributes = printJobInfo.getAttributes();
        if (attributes != null) {
            writePrintAttributes(context, dualDumpOutputStream, "attributes", 1146756268039L, attributes);
        }
        PrintDocumentInfo documentInfo = printJobInfo.getDocumentInfo();
        if (documentInfo != null) {
            writePrintDocumentInfo(dualDumpOutputStream, "document_info", 1146756268040L, documentInfo);
        }
        dualDumpOutputStream.write("is_canceling", 1133871366153L, printJobInfo.isCancelling());
        PageRange[] pages = printJobInfo.getPages();
        if (pages != null) {
            for (PageRange pageRange : pages) {
                writePageRange(dualDumpOutputStream, "pages", 2246267895818L, pageRange);
            }
        }
        dualDumpOutputStream.write("has_advanced_options", 1133871366155L, printJobInfo.getAdvancedOptions() != null);
        dualDumpOutputStream.write(Notification.CATEGORY_PROGRESS, 1108101562380L, printJobInfo.getProgress());
        CharSequence status = printJobInfo.getStatus(context.getPackageManager());
        if (status != null) {
            dualDumpOutputStream.write("status", 1138166333453L, status.toString());
        }
        dualDumpOutputStream.end(jStart);
    }
}
