package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

public abstract class PrintDocumentAdapter {
    public static final String EXTRA_PRINT_PREVIEW = "EXTRA_PRINT_PREVIEW";

    public abstract void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes2, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle);

    public abstract void onWrite(PageRange[] pageRangeArr, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback);

    public void onStart() {
    }

    public void onFinish() {
    }

    public static abstract class WriteResultCallback {
        public void onWriteFinished(PageRange[] pageRangeArr) {
        }

        public void onWriteFailed(CharSequence charSequence) {
        }

        public void onWriteCancelled() {
        }
    }

    public static abstract class LayoutResultCallback {
        public void onLayoutFinished(PrintDocumentInfo printDocumentInfo, boolean z) {
        }

        public void onLayoutFailed(CharSequence charSequence) {
        }

        public void onLayoutCancelled() {
        }
    }
}
