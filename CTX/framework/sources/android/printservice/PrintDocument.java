package android.printservice;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.PrintDocumentInfo;
import android.print.PrintJobId;
import android.util.Log;
import java.io.IOException;

public final class PrintDocument {
    private static final String LOG_TAG = "PrintDocument";
    private final PrintDocumentInfo mInfo;
    private final PrintJobId mPrintJobId;
    private final IPrintServiceClient mPrintServiceClient;

    PrintDocument(PrintJobId printJobId, IPrintServiceClient iPrintServiceClient, PrintDocumentInfo printDocumentInfo) {
        this.mPrintJobId = printJobId;
        this.mPrintServiceClient = iPrintServiceClient;
        this.mInfo = printDocumentInfo;
    }

    public PrintDocumentInfo getInfo() {
        PrintService.throwIfNotCalledOnMainThread();
        return this.mInfo;
    }

    public ParcelFileDescriptor getData() throws Throwable {
        ParcelFileDescriptor parcelFileDescriptor;
        ParcelFileDescriptor parcelFileDescriptor2;
        PrintService.throwIfNotCalledOnMainThread();
        AutoCloseable autoCloseable = null;
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                parcelFileDescriptor2 = parcelFileDescriptorArrCreatePipe[0];
                parcelFileDescriptor = parcelFileDescriptorArrCreatePipe[1];
            } catch (IOException e) {
                return null;
            }
            try {
                this.mPrintServiceClient.writePrintJobData(parcelFileDescriptor, this.mPrintJobId);
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e2) {
                    }
                }
                return parcelFileDescriptor2;
            } catch (RemoteException e3) {
                e = e3;
                Log.e(LOG_TAG, "Error calling getting print job data!", e);
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
                return null;
            } catch (IOException e4) {
                e = e4;
                Log.e(LOG_TAG, "Error calling getting print job data!", e);
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
                return null;
            }
        } catch (RemoteException e5) {
            e = e5;
            parcelFileDescriptor = null;
        } catch (IOException e6) {
            e = e6;
            parcelFileDescriptor = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
                try {
                    autoCloseable.close();
                } catch (IOException e7) {
                }
            }
            throw th;
        }
    }
}
