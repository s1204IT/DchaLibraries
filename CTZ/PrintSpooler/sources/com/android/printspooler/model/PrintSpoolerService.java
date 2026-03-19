package com.android.printspooler.model;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.IPrintSpooler;
import android.print.IPrintSpoolerCallbacks;
import android.print.IPrintSpoolerClient;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.print.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.printspooler.R;
import com.android.printspooler.util.ApprovedPrintServices;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class PrintSpoolerService extends Service {
    private static PrintSpoolerService sInstance;
    private static final Object sLock = new Object();
    private IPrintSpoolerClient mClient;
    private CustomPrinterIconCache mCustomIconCache;
    private NotificationController mNotificationController;
    private PersistenceManager mPersistanceManager;
    private final Object mLock = new Object();
    private final List<PrintJobInfo> mPrintJobs = new ArrayList();

    @Override
    public void onCreate() {
        super.onCreate();
        this.mPersistanceManager = new PersistenceManager();
        this.mNotificationController = new NotificationController(this);
        this.mCustomIconCache = new CustomPrinterIconCache(getCacheDir());
        synchronized (this.mLock) {
            this.mPersistanceManager.readStateLocked();
            handleReadPrintJobsLocked();
        }
        synchronized (sLock) {
            sInstance = this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PrintSpooler();
    }

    private void dumpLocked(DualDumpOutputStream dualDumpOutputStream) {
        int size = this.mPrintJobs.size();
        for (int i = 0; i < size; i++) {
            DumpUtils.writePrintJobInfo(this, dualDumpOutputStream, "print_jobs", 2246267895809L, this.mPrintJobs.get(i));
        }
        File[] fileArrListFiles = getFilesDir().listFiles();
        if (fileArrListFiles != null) {
            for (File file : fileArrListFiles) {
                if (file.isFile() && file.getName().startsWith("print_job_")) {
                    dualDumpOutputStream.write("print_job_files", 2237677961218L, file.getName());
                }
            }
        }
        Set<String> approvedServices = new ApprovedPrintServices(this).getApprovedServices();
        if (approvedServices != null) {
            Iterator<String> it = approvedServices.iterator();
            while (it.hasNext()) {
                ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(it.next());
                if (componentNameUnflattenFromString != null) {
                    com.android.internal.util.dump.DumpUtils.writeComponentName(dualDumpOutputStream, "approved_services", 2246267895811L, componentNameUnflattenFromString);
                }
            }
        }
        dualDumpOutputStream.flush();
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Throwable th;
        String str;
        FileDescriptor fileDescriptor2 = (FileDescriptor) Preconditions.checkNotNull(fileDescriptor);
        int i = 0;
        boolean z = false;
        while (i < strArr.length && (str = strArr[i]) != null && str.length() > 0 && str.charAt(0) == '-') {
            i++;
            if ("--proto".equals(str)) {
                z = true;
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (z) {
                    dumpLocked(new DualDumpOutputStream(new ProtoOutputStream(fileDescriptor2)));
                } else {
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(fileDescriptor2);
                        try {
                            PrintWriter printWriter2 = new PrintWriter(fileOutputStream);
                            try {
                                dumpLocked(new DualDumpOutputStream(new IndentingPrintWriter(printWriter2, "  ")));
                                $closeResource(null, printWriter2);
                            } catch (Throwable th2) {
                                th = th2;
                                th = null;
                                $closeResource(th, printWriter2);
                                throw th;
                            }
                        } finally {
                            $closeResource(null, fileOutputStream);
                        }
                    } catch (IOException e) {
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void sendOnPrintJobQueued(PrintJobInfo printJobInfo) {
        Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((PrintSpoolerService) obj).onPrintJobQueued((PrintJobInfo) obj2);
            }
        }, this, printJobInfo));
    }

    private void sendOnAllPrintJobsForServiceHandled(ComponentName componentName) {
        Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((PrintSpoolerService) obj).onAllPrintJobsForServiceHandled((ComponentName) obj2);
            }
        }, this, componentName));
    }

    private void sendOnAllPrintJobsHandled() {
        Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((PrintSpoolerService) obj).onAllPrintJobsHandled();
            }
        }, this));
    }

    private void onPrintJobStateChanged(PrintJobInfo printJobInfo) {
        if (this.mClient != null) {
            try {
                this.mClient.onPrintJobStateChanged(printJobInfo);
            } catch (RemoteException e) {
                Slog.e("PrintSpoolerService", "Error notify for print job state change.", e);
            }
        }
    }

    private void onAllPrintJobsHandled() {
        if (this.mClient != null) {
            try {
                this.mClient.onAllPrintJobsHandled();
            } catch (RemoteException e) {
                Slog.e("PrintSpoolerService", "Error notify for all print job handled.", e);
            }
        }
    }

    private void onAllPrintJobsForServiceHandled(ComponentName componentName) {
        if (this.mClient != null) {
            try {
                this.mClient.onAllPrintJobsForServiceHandled(componentName);
            } catch (RemoteException e) {
                Slog.e("PrintSpoolerService", "Error notify for all print jobs per service handled.", e);
            }
        }
    }

    private void onPrintJobQueued(PrintJobInfo printJobInfo) {
        if (this.mClient != null) {
            try {
                this.mClient.onPrintJobQueued(printJobInfo);
            } catch (RemoteException e) {
                Slog.e("PrintSpoolerService", "Error notify for a queued print job.", e);
            }
        }
    }

    private void setClient(IPrintSpoolerClient iPrintSpoolerClient) {
        synchronized (this.mLock) {
            this.mClient = iPrintSpoolerClient;
            if (this.mClient != null) {
                Handler.getMain().sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((PrintSpoolerService) obj).checkAllPrintJobsHandled();
                    }
                }, this), 5000L);
            }
        }
    }

    public List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int i, int i2) {
        ArrayList arrayList;
        synchronized (this.mLock) {
            int size = this.mPrintJobs.size();
            arrayList = null;
            for (int i3 = 0; i3 < size; i3++) {
                PrintJobInfo printJobInfo = this.mPrintJobs.get(i3);
                PrinterId printerId = printJobInfo.getPrinterId();
                boolean z = true;
                boolean z2 = componentName == null || (printerId != null && componentName.equals(printerId.getServiceName()));
                boolean z3 = i2 == -2 || printJobInfo.getAppId() == i2;
                if (i != printJobInfo.getState() && i != -1 && ((i != -2 || !isStateVisibleToUser(printJobInfo.getState())) && ((i != -3 || !isActiveState(printJobInfo.getState())) && (i != -4 || !isScheduledState(printJobInfo.getState()))))) {
                    z = false;
                }
                if (z2 && z3 && z) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(printJobInfo);
                }
            }
        }
        return arrayList;
    }

    private boolean isStateVisibleToUser(int i) {
        return isActiveState(i) && (i == 6 || i == 5 || i == 7 || i == 4);
    }

    public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int i) {
        synchronized (this.mLock) {
            int size = this.mPrintJobs.size();
            for (int i2 = 0; i2 < size; i2++) {
                PrintJobInfo printJobInfo = this.mPrintJobs.get(i2);
                if (printJobInfo.getId().equals(printJobId) && (i == -2 || i == printJobInfo.getAppId())) {
                    return printJobInfo;
                }
            }
            return null;
        }
    }

    public void createPrintJob(PrintJobInfo printJobInfo) {
        synchronized (this.mLock) {
            addPrintJobLocked(printJobInfo);
            setPrintJobState(printJobInfo.getId(), 1, null);
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage($$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI.INSTANCE, this, printJobInfo));
        }
    }

    private void handleReadPrintJobsLocked() {
        File[] fileArrListFiles = getFilesDir().listFiles();
        ArrayMap arrayMap = null;
        if (fileArrListFiles != null) {
            ArrayMap arrayMap2 = null;
            for (File file : fileArrListFiles) {
                if (file.isFile() && file.getName().startsWith("print_job_")) {
                    if (arrayMap2 == null) {
                        arrayMap2 = new ArrayMap();
                    }
                    arrayMap2.put(PrintJobId.unflattenFromString(file.getName().substring("print_job_".length(), file.getName().indexOf(46))), file);
                }
            }
            arrayMap = arrayMap2;
        }
        int size = this.mPrintJobs.size();
        for (int i = 0; i < size; i++) {
            PrintJobInfo printJobInfo = this.mPrintJobs.get(i);
            if (arrayMap != null) {
                arrayMap.remove(printJobInfo.getId());
            }
            switch (printJobInfo.getState()) {
                case 2:
                case 3:
                case 4:
                    setPrintJobState(printJobInfo.getId(), 6, getString(R.string.no_connection_to_printer));
                    break;
            }
        }
        if (!this.mPrintJobs.isEmpty()) {
            this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
        }
        if (arrayMap != null) {
            int size2 = arrayMap.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ((File) arrayMap.valueAt(i2)).delete();
            }
        }
    }

    public void checkAllPrintJobsHandled() {
        synchronized (this.mLock) {
            if (!hasActivePrintJobsLocked()) {
                notifyOnAllPrintJobsHandled();
            }
        }
    }

    public void writePrintJobData(final ParcelFileDescriptor parcelFileDescriptor, final PrintJobId printJobId) {
        final PrintJobInfo printJobInfo;
        synchronized (this.mLock) {
            printJobInfo = getPrintJobInfo(printJobId, -2);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) throws Throwable {
                ?? fileInputStream;
                ?? r1;
                FileOutputStream fileOutputStream;
                ?? r12;
                ?? r13;
                FileOutputStream fileOutputStream2;
                FileOutputStream fileOutputStream3 = null;
                try {
                    try {
                        if (printJobInfo != null) {
                            fileInputStream = new FileInputStream(PrintSpoolerService.generateFileForPrintJob(PrintSpoolerService.this, printJobId));
                            try {
                                fileOutputStream2 = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                                fileInputStream = fileInputStream;
                            } catch (FileNotFoundException e) {
                                e = e;
                                fileOutputStream = null;
                                r12 = fileInputStream;
                                Log.e("PrintSpoolerService", "Error writing print job data!", e);
                                r13 = r12;
                                IoUtils.closeQuietly((AutoCloseable) r13);
                                IoUtils.closeQuietly(fileOutputStream);
                                IoUtils.closeQuietly(parcelFileDescriptor);
                                fileInputStream = "[END WRITE]";
                                Log.i("PrintSpoolerService", "[END WRITE]");
                                return null;
                            } catch (IOException e2) {
                                e = e2;
                                fileOutputStream = null;
                                r1 = fileInputStream;
                                Log.e("PrintSpoolerService", "Error writing print job data!", e);
                                r13 = r1;
                                IoUtils.closeQuietly((AutoCloseable) r13);
                                IoUtils.closeQuietly(fileOutputStream);
                                IoUtils.closeQuietly(parcelFileDescriptor);
                                fileInputStream = "[END WRITE]";
                                Log.i("PrintSpoolerService", "[END WRITE]");
                                return null;
                            } catch (Throwable th) {
                                th = th;
                                IoUtils.closeQuietly((AutoCloseable) fileInputStream);
                                IoUtils.closeQuietly(fileOutputStream3);
                                IoUtils.closeQuietly(parcelFileDescriptor);
                                throw th;
                            }
                        } else {
                            fileOutputStream2 = null;
                            fileInputStream = 0;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        fileOutputStream3 = fileOutputStream;
                    }
                } catch (FileNotFoundException e3) {
                    e = e3;
                    r12 = 0;
                    fileOutputStream = null;
                } catch (IOException e4) {
                    e = e4;
                    r1 = 0;
                    fileOutputStream = null;
                } catch (Throwable th3) {
                    th = th3;
                    fileInputStream = 0;
                }
                try {
                    byte[] bArr = new byte[8192];
                    while (true) {
                        int i = fileInputStream.read(bArr);
                        if (i < 0) {
                            IoUtils.closeQuietly((AutoCloseable) fileInputStream);
                            IoUtils.closeQuietly(fileOutputStream2);
                            IoUtils.closeQuietly(parcelFileDescriptor);
                            return null;
                        }
                        fileOutputStream2.write(bArr, 0, i);
                    }
                } catch (FileNotFoundException e5) {
                    fileOutputStream = fileOutputStream2;
                    e = e5;
                    r12 = fileInputStream;
                    Log.e("PrintSpoolerService", "Error writing print job data!", e);
                    r13 = r12;
                    IoUtils.closeQuietly((AutoCloseable) r13);
                    IoUtils.closeQuietly(fileOutputStream);
                    IoUtils.closeQuietly(parcelFileDescriptor);
                    fileInputStream = "[END WRITE]";
                    Log.i("PrintSpoolerService", "[END WRITE]");
                    return null;
                } catch (IOException e6) {
                    fileOutputStream = fileOutputStream2;
                    e = e6;
                    r1 = fileInputStream;
                    Log.e("PrintSpoolerService", "Error writing print job data!", e);
                    r13 = r1;
                    IoUtils.closeQuietly((AutoCloseable) r13);
                    IoUtils.closeQuietly(fileOutputStream);
                    IoUtils.closeQuietly(parcelFileDescriptor);
                    fileInputStream = "[END WRITE]";
                    Log.i("PrintSpoolerService", "[END WRITE]");
                    return null;
                } catch (Throwable th4) {
                    FileOutputStream fileOutputStream4 = fileOutputStream2;
                    th = th4;
                    fileOutputStream3 = fileOutputStream4;
                    IoUtils.closeQuietly((AutoCloseable) fileInputStream);
                    IoUtils.closeQuietly(fileOutputStream3);
                    IoUtils.closeQuietly(parcelFileDescriptor);
                    throw th;
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public static File generateFileForPrintJob(Context context, PrintJobId printJobId) {
        return new File(context.getFilesDir(), "print_job_" + printJobId.flattenToString() + ".pdf");
    }

    private void addPrintJobLocked(PrintJobInfo printJobInfo) {
        this.mPrintJobs.add(printJobInfo);
    }

    private void removeObsoletePrintJobs() {
        synchronized (this.mLock) {
            boolean z = false;
            for (int size = this.mPrintJobs.size() - 1; size >= 0; size--) {
                PrintJobInfo printJobInfo = this.mPrintJobs.get(size);
                if (isObsoleteState(printJobInfo.getState())) {
                    this.mPrintJobs.remove(size);
                    removePrintJobFileLocked(printJobInfo.getId());
                    z = true;
                }
            }
            if (z) {
                this.mPersistanceManager.writeStateLocked();
            }
        }
    }

    private void removePrintJobFileLocked(PrintJobId printJobId) {
        File fileGenerateFileForPrintJob = generateFileForPrintJob(this, printJobId);
        if (fileGenerateFileForPrintJob.exists()) {
            fileGenerateFileForPrintJob.delete();
        }
    }

    private void notifyPrintJobUpdated(PrintJobInfo printJobInfo) {
        Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage($$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI.INSTANCE, this, printJobInfo));
        this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
    }

    public boolean setPrintJobState(PrintJobId printJobId, int i, String str) {
        synchronized (this.mLock) {
            PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, -2);
            boolean z = false;
            if (printJobInfo != null) {
                if (printJobInfo.getState() == i) {
                    return false;
                }
                printJobInfo.setState(i);
                printJobInfo.setStatus(str);
                printJobInfo.setCancelling(false);
                MetricsLogger.histogram(this, "print_job_state", i);
                if (i != 2) {
                    switch (i) {
                        case 5:
                        case 7:
                            this.mPrintJobs.remove(printJobInfo);
                            removePrintJobFileLocked(printJobInfo.getId());
                        case 6:
                            PrinterId printerId = printJobInfo.getPrinterId();
                            if (printerId != null) {
                                ComponentName serviceName = printerId.getServiceName();
                                if (!hasActivePrintJobsForServiceLocked(serviceName)) {
                                    sendOnAllPrintJobsForServiceHandled(serviceName);
                                }
                            }
                            break;
                    }
                } else {
                    sendOnPrintJobQueued(new PrintJobInfo(printJobInfo));
                }
                if (shouldPersistPrintJob(printJobInfo)) {
                    this.mPersistanceManager.writeStateLocked();
                }
                if (!hasActivePrintJobsLocked()) {
                    notifyOnAllPrintJobsHandled();
                }
                notifyPrintJobUpdated(printJobInfo);
                z = true;
            }
            return z;
        }
    }

    public void setProgress(PrintJobId printJobId, float f) {
        synchronized (this.mLock) {
            getPrintJobInfo(printJobId, -2).setProgress(f);
            this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
        }
    }

    public void setStatus(PrintJobId printJobId, CharSequence charSequence) {
        synchronized (this.mLock) {
            PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, -2);
            if (printJobInfo != null) {
                printJobInfo.setStatus(charSequence);
                notifyPrintJobUpdated(printJobInfo);
            }
        }
    }

    public void setStatus(PrintJobId printJobId, int i, CharSequence charSequence) {
        synchronized (this.mLock) {
            PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, -2);
            if (printJobInfo != null) {
                printJobInfo.setStatus(i, charSequence);
                notifyPrintJobUpdated(printJobInfo);
            }
        }
    }

    public boolean hasActivePrintJobsLocked() {
        int size = this.mPrintJobs.size();
        for (int i = 0; i < size; i++) {
            if (isActiveState(this.mPrintJobs.get(i).getState())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActivePrintJobsForServiceLocked(ComponentName componentName) {
        int size = this.mPrintJobs.size();
        for (int i = 0; i < size; i++) {
            PrintJobInfo printJobInfo = this.mPrintJobs.get(i);
            if (isActiveState(printJobInfo.getState()) && printJobInfo.getPrinterId() != null && printJobInfo.getPrinterId().getServiceName().equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isObsoleteState(int i) {
        return isTerminalState(i) || i == 2;
    }

    private boolean isScheduledState(int i) {
        return i == 2 || i == 3 || i == 4;
    }

    private boolean isActiveState(int i) {
        return i == 1 || i == 2 || i == 3 || i == 4;
    }

    private boolean isTerminalState(int i) {
        return i == 5 || i == 7;
    }

    public boolean setPrintJobTag(PrintJobId printJobId, String str) {
        synchronized (this.mLock) {
            PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, -2);
            if (printJobInfo == null) {
                return false;
            }
            String tag = printJobInfo.getTag();
            if (tag == null) {
                if (str == null) {
                    return false;
                }
            } else if (tag.equals(str)) {
                return false;
            }
            printJobInfo.setTag(str);
            if (shouldPersistPrintJob(printJobInfo)) {
                this.mPersistanceManager.writeStateLocked();
            }
            return true;
        }
    }

    public void setPrintJobCancelling(PrintJobId printJobId, boolean z) {
        synchronized (this.mLock) {
            PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, -2);
            if (printJobInfo != null) {
                printJobInfo.setCancelling(z);
                if (shouldPersistPrintJob(printJobInfo)) {
                    this.mPersistanceManager.writeStateLocked();
                }
                this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
                Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage($$Lambda$PrintSpoolerService$gRIXcCK9lL2zxK9JFntdYvHjEBI.INSTANCE, this, printJobInfo));
            }
        }
    }

    public void updatePrintJobUserConfigurableOptionsNoPersistence(PrintJobInfo printJobInfo) {
        synchronized (this.mLock) {
            int size = this.mPrintJobs.size();
            for (int i = 0; i < size; i++) {
                PrintJobInfo printJobInfo2 = this.mPrintJobs.get(i);
                if (printJobInfo2.getId().equals(printJobInfo.getId())) {
                    printJobInfo2.setPrinterId(printJobInfo.getPrinterId());
                    printJobInfo2.setPrinterName(printJobInfo.getPrinterName());
                    printJobInfo2.setCopies(printJobInfo.getCopies());
                    printJobInfo2.setDocumentInfo(printJobInfo.getDocumentInfo());
                    printJobInfo2.setPages(printJobInfo.getPages());
                    printJobInfo2.setAttributes(printJobInfo.getAttributes());
                    printJobInfo2.setAdvancedOptions(printJobInfo.getAdvancedOptions());
                }
            }
            throw new IllegalArgumentException("No print job with id:" + printJobInfo.getId());
        }
    }

    private boolean shouldPersistPrintJob(PrintJobInfo printJobInfo) {
        return printJobInfo.getState() >= 2;
    }

    private void notifyOnAllPrintJobsHandled() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                PrintSpoolerService.this.sendOnAllPrintJobsHandled();
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null);
    }

    public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        this.mCustomIconCache.onCustomPrinterIconLoaded(printerId, icon);
    }

    public Icon getCustomPrinterIcon(PrinterId printerId) {
        return this.mCustomIconCache.getIcon(printerId);
    }

    public void clearCustomPrinterIconCache() {
        this.mCustomIconCache.clear();
    }

    private final class PersistenceManager {
        private final AtomicFile mStatePersistFile;
        private boolean mWriteStateScheduled;

        private PersistenceManager() {
            this.mStatePersistFile = new AtomicFile(new File(PrintSpoolerService.this.getFilesDir(), "print_spooler_state.xml"), "print-spooler");
        }

        public void writeStateLocked() {
            if (this.mWriteStateScheduled) {
                return;
            }
            this.mWriteStateScheduled = true;
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) {
                    synchronized (PrintSpoolerService.this.mLock) {
                        PersistenceManager.this.mWriteStateScheduled = false;
                        PersistenceManager.this.doWriteStateLocked();
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null);
        }

        private void doWriteStateLocked() throws Throwable {
            FileOutputStream fileOutputStreamStartWrite;
            Throwable th;
            IOException e;
            try {
                fileOutputStreamStartWrite = this.mStatePersistFile.startWrite();
                try {
                    try {
                        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                        fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                        fastXmlSerializer.startDocument(null, true);
                        fastXmlSerializer.startTag(null, "spooler");
                        List list = PrintSpoolerService.this.mPrintJobs;
                        int size = list.size();
                        for (int i = 0; i < size; i++) {
                            PrintJobInfo printJobInfo = (PrintJobInfo) list.get(i);
                            if (PrintSpoolerService.this.shouldPersistPrintJob(printJobInfo)) {
                                fastXmlSerializer.startTag(null, "job");
                                fastXmlSerializer.attribute(null, "id", printJobInfo.getId().flattenToString());
                                fastXmlSerializer.attribute(null, "label", printJobInfo.getLabel().toString());
                                fastXmlSerializer.attribute(null, "state", String.valueOf(printJobInfo.getState()));
                                fastXmlSerializer.attribute(null, "appId", String.valueOf(printJobInfo.getAppId()));
                                String tag = printJobInfo.getTag();
                                if (tag != null) {
                                    fastXmlSerializer.attribute(null, "tag", tag);
                                }
                                fastXmlSerializer.attribute(null, "creationTime", String.valueOf(printJobInfo.getCreationTime()));
                                fastXmlSerializer.attribute(null, "copies", String.valueOf(printJobInfo.getCopies()));
                                String printerName = printJobInfo.getPrinterName();
                                if (!TextUtils.isEmpty(printerName)) {
                                    fastXmlSerializer.attribute(null, "printerName", printerName);
                                }
                                fastXmlSerializer.attribute(null, "cancelling", String.valueOf(printJobInfo.isCancelling()));
                                float progress = printJobInfo.getProgress();
                                if (!Float.isNaN(progress)) {
                                    fastXmlSerializer.attribute(null, "progress", String.valueOf(progress));
                                }
                                CharSequence status = printJobInfo.getStatus(PrintSpoolerService.this.getPackageManager());
                                if (!TextUtils.isEmpty(status)) {
                                    fastXmlSerializer.attribute(null, "status", status.toString());
                                }
                                PrinterId printerId = printJobInfo.getPrinterId();
                                if (printerId != null) {
                                    fastXmlSerializer.startTag(null, "printerId");
                                    fastXmlSerializer.attribute(null, "localId", printerId.getLocalId());
                                    fastXmlSerializer.attribute(null, "serviceName", printerId.getServiceName().flattenToString());
                                    fastXmlSerializer.endTag(null, "printerId");
                                }
                                PageRange[] pages = printJobInfo.getPages();
                                if (pages != null) {
                                    for (int i2 = 0; i2 < pages.length; i2++) {
                                        fastXmlSerializer.startTag(null, "pageRange");
                                        fastXmlSerializer.attribute(null, "start", String.valueOf(pages[i2].getStart()));
                                        fastXmlSerializer.attribute(null, "end", String.valueOf(pages[i2].getEnd()));
                                        fastXmlSerializer.endTag(null, "pageRange");
                                    }
                                }
                                PrintAttributes attributes = printJobInfo.getAttributes();
                                if (attributes != null) {
                                    fastXmlSerializer.startTag(null, "attributes");
                                    fastXmlSerializer.attribute(null, "colorMode", String.valueOf(attributes.getColorMode()));
                                    fastXmlSerializer.attribute(null, "duplexMode", String.valueOf(attributes.getDuplexMode()));
                                    PrintAttributes.MediaSize mediaSize = attributes.getMediaSize();
                                    if (mediaSize != null) {
                                        fastXmlSerializer.startTag(null, "mediaSize");
                                        fastXmlSerializer.attribute(null, "id", mediaSize.getId());
                                        fastXmlSerializer.attribute(null, "widthMils", String.valueOf(mediaSize.getWidthMils()));
                                        fastXmlSerializer.attribute(null, "heightMils", String.valueOf(mediaSize.getHeightMils()));
                                        if (!TextUtils.isEmpty(mediaSize.mPackageName) && mediaSize.mLabelResId > 0) {
                                            fastXmlSerializer.attribute(null, "packageName", mediaSize.mPackageName);
                                            fastXmlSerializer.attribute(null, "labelResId", String.valueOf(mediaSize.mLabelResId));
                                        } else {
                                            fastXmlSerializer.attribute(null, "label", mediaSize.getLabel(PrintSpoolerService.this.getPackageManager()));
                                        }
                                        fastXmlSerializer.endTag(null, "mediaSize");
                                    }
                                    PrintAttributes.Resolution resolution = attributes.getResolution();
                                    if (resolution != null) {
                                        fastXmlSerializer.startTag(null, "resolution");
                                        fastXmlSerializer.attribute(null, "id", resolution.getId());
                                        fastXmlSerializer.attribute(null, "horizontalDip", String.valueOf(resolution.getHorizontalDpi()));
                                        fastXmlSerializer.attribute(null, "verticalDpi", String.valueOf(resolution.getVerticalDpi()));
                                        fastXmlSerializer.attribute(null, "label", resolution.getLabel());
                                        fastXmlSerializer.endTag(null, "resolution");
                                    }
                                    PrintAttributes.Margins minMargins = attributes.getMinMargins();
                                    if (minMargins != null) {
                                        fastXmlSerializer.startTag(null, "margins");
                                        fastXmlSerializer.attribute(null, "leftMils", String.valueOf(minMargins.getLeftMils()));
                                        fastXmlSerializer.attribute(null, "topMils", String.valueOf(minMargins.getTopMils()));
                                        fastXmlSerializer.attribute(null, "rightMils", String.valueOf(minMargins.getRightMils()));
                                        fastXmlSerializer.attribute(null, "bottomMils", String.valueOf(minMargins.getBottomMils()));
                                        fastXmlSerializer.endTag(null, "margins");
                                    }
                                    fastXmlSerializer.endTag(null, "attributes");
                                }
                                PrintDocumentInfo documentInfo = printJobInfo.getDocumentInfo();
                                if (documentInfo != null) {
                                    fastXmlSerializer.startTag(null, "documentInfo");
                                    fastXmlSerializer.attribute(null, "name", documentInfo.getName());
                                    fastXmlSerializer.attribute(null, "contentType", String.valueOf(documentInfo.getContentType()));
                                    fastXmlSerializer.attribute(null, "pageCount", String.valueOf(documentInfo.getPageCount()));
                                    fastXmlSerializer.attribute(null, "dataSize", String.valueOf(documentInfo.getDataSize()));
                                    fastXmlSerializer.endTag(null, "documentInfo");
                                }
                                Bundle advancedOptions = printJobInfo.getAdvancedOptions();
                                if (advancedOptions != null) {
                                    fastXmlSerializer.startTag(null, "advancedOptions");
                                    for (String str : advancedOptions.keySet()) {
                                        ?? r10 = advancedOptions.get(str);
                                        if (r10 instanceof String) {
                                            fastXmlSerializer.startTag(null, "advancedOption");
                                            fastXmlSerializer.attribute(null, "key", str);
                                            fastXmlSerializer.attribute(null, "type", "string");
                                            fastXmlSerializer.attribute(null, "value", r10);
                                            fastXmlSerializer.endTag(null, "advancedOption");
                                        } else if (r10 instanceof Integer) {
                                            String string = Integer.toString(r10.intValue());
                                            fastXmlSerializer.startTag(null, "advancedOption");
                                            fastXmlSerializer.attribute(null, "key", str);
                                            fastXmlSerializer.attribute(null, "type", "int");
                                            fastXmlSerializer.attribute(null, "value", string);
                                            fastXmlSerializer.endTag(null, "advancedOption");
                                        }
                                    }
                                    fastXmlSerializer.endTag(null, "advancedOptions");
                                }
                                fastXmlSerializer.endTag(null, "job");
                            }
                        }
                        fastXmlSerializer.endTag(null, "spooler");
                        fastXmlSerializer.endDocument();
                        this.mStatePersistFile.finishWrite(fileOutputStreamStartWrite);
                    } catch (IOException e2) {
                        e = e2;
                        Slog.w("PrintSpoolerService", "Failed to write state, restoring backup.", e);
                        this.mStatePersistFile.failWrite(fileOutputStreamStartWrite);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fileOutputStreamStartWrite);
                    throw th;
                }
            } catch (IOException e3) {
                fileOutputStreamStartWrite = null;
                e = e3;
            } catch (Throwable th3) {
                fileOutputStreamStartWrite = null;
                th = th3;
                IoUtils.closeQuietly(fileOutputStreamStartWrite);
                throw th;
            }
            IoUtils.closeQuietly(fileOutputStreamStartWrite);
        }

        public void readStateLocked() {
            try {
                FileInputStream fileInputStreamOpenRead = this.mStatePersistFile.openRead();
                try {
                    try {
                        try {
                            try {
                                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                                xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                                parseState(xmlPullParserNewPullParser);
                            } finally {
                                IoUtils.closeQuietly(fileInputStreamOpenRead);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            Slog.w("PrintSpoolerService", "Failed parsing ", e);
                        } catch (NumberFormatException e2) {
                            Slog.w("PrintSpoolerService", "Failed parsing ", e2);
                        }
                    } catch (NullPointerException e3) {
                        Slog.w("PrintSpoolerService", "Failed parsing ", e3);
                    } catch (XmlPullParserException e4) {
                        Slog.w("PrintSpoolerService", "Failed parsing ", e4);
                    }
                } catch (IOException e5) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", e5);
                } catch (IllegalStateException e6) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", e6);
                }
            } catch (FileNotFoundException e7) {
            }
        }

        private void parseState(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            xmlPullParser.next();
            skipEmptyTextTags(xmlPullParser);
            expect(xmlPullParser, 2, "spooler");
            xmlPullParser.next();
            while (parsePrintJob(xmlPullParser)) {
                xmlPullParser.next();
            }
            skipEmptyTextTags(xmlPullParser);
            expect(xmlPullParser, 3, "spooler");
        }

        private boolean parsePrintJob(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            boolean z;
            skipEmptyTextTags(xmlPullParser);
            if (!accept(xmlPullParser, 2, "job")) {
                return false;
            }
            PrintJobInfo printJobInfo = new PrintJobInfo();
            printJobInfo.setId(PrintJobId.unflattenFromString(xmlPullParser.getAttributeValue(null, "id")));
            printJobInfo.setLabel(xmlPullParser.getAttributeValue(null, "label"));
            printJobInfo.setState(Integer.parseInt(xmlPullParser.getAttributeValue(null, "state")));
            printJobInfo.setAppId(Integer.parseInt(xmlPullParser.getAttributeValue(null, "appId")));
            printJobInfo.setTag(xmlPullParser.getAttributeValue(null, "tag"));
            printJobInfo.setCreationTime(Long.parseLong(xmlPullParser.getAttributeValue(null, "creationTime")));
            printJobInfo.setCopies(Integer.parseInt(xmlPullParser.getAttributeValue(null, "copies")));
            printJobInfo.setPrinterName(xmlPullParser.getAttributeValue(null, "printerName"));
            String attributeValue = xmlPullParser.getAttributeValue(null, "progress");
            if (attributeValue != null) {
                float f = Float.parseFloat(attributeValue);
                if (f != -1.0f) {
                    printJobInfo.setProgress(f);
                }
            }
            printJobInfo.setStatus(xmlPullParser.getAttributeValue(null, "status"));
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "stateReason");
            if (attributeValue2 != null) {
                printJobInfo.setStatus(attributeValue2);
            }
            String attributeValue3 = xmlPullParser.getAttributeValue(null, "cancelling");
            if (TextUtils.isEmpty(attributeValue3)) {
                z = false;
            } else {
                z = Boolean.parseBoolean(attributeValue3);
            }
            printJobInfo.setCancelling(z);
            xmlPullParser.next();
            skipEmptyTextTags(xmlPullParser);
            if (accept(xmlPullParser, 2, "printerId")) {
                printJobInfo.setPrinterId(new PrinterId(ComponentName.unflattenFromString(xmlPullParser.getAttributeValue(null, "serviceName")), xmlPullParser.getAttributeValue(null, "localId")));
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "printerId");
                xmlPullParser.next();
            }
            skipEmptyTextTags(xmlPullParser);
            ArrayList arrayList = null;
            while (accept(xmlPullParser, 2, "pageRange")) {
                PageRange pageRange = new PageRange(Integer.parseInt(xmlPullParser.getAttributeValue(null, "start")), Integer.parseInt(xmlPullParser.getAttributeValue(null, "end")));
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(pageRange);
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "pageRange");
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
            }
            if (arrayList != null) {
                PageRange[] pageRangeArr = new PageRange[arrayList.size()];
                arrayList.toArray(pageRangeArr);
                printJobInfo.setPages(pageRangeArr);
            }
            skipEmptyTextTags(xmlPullParser);
            if (accept(xmlPullParser, 2, "attributes")) {
                PrintAttributes.Builder builder = new PrintAttributes.Builder();
                builder.setColorMode(Integer.parseInt(xmlPullParser.getAttributeValue(null, "colorMode")));
                String attributeValue4 = xmlPullParser.getAttributeValue(null, "duplexMode");
                if (attributeValue4 != null) {
                    builder.setDuplexMode(Integer.parseInt(attributeValue4));
                }
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                if (accept(xmlPullParser, 2, "mediaSize")) {
                    String attributeValue5 = xmlPullParser.getAttributeValue(null, "id");
                    xmlPullParser.getAttributeValue(null, "label");
                    int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "widthMils"));
                    int i2 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "heightMils"));
                    String attributeValue6 = xmlPullParser.getAttributeValue(null, "packageName");
                    String attributeValue7 = xmlPullParser.getAttributeValue(null, "labelResId");
                    builder.setMediaSize(new PrintAttributes.MediaSize(attributeValue5, xmlPullParser.getAttributeValue(null, "label"), attributeValue6, i, i2, attributeValue7 != null ? Integer.parseInt(attributeValue7) : 0));
                    xmlPullParser.next();
                    skipEmptyTextTags(xmlPullParser);
                    expect(xmlPullParser, 3, "mediaSize");
                    xmlPullParser.next();
                }
                skipEmptyTextTags(xmlPullParser);
                if (accept(xmlPullParser, 2, "resolution")) {
                    builder.setResolution(new PrintAttributes.Resolution(xmlPullParser.getAttributeValue(null, "id"), xmlPullParser.getAttributeValue(null, "label"), Integer.parseInt(xmlPullParser.getAttributeValue(null, "horizontalDip")), Integer.parseInt(xmlPullParser.getAttributeValue(null, "verticalDpi"))));
                    xmlPullParser.next();
                    skipEmptyTextTags(xmlPullParser);
                    expect(xmlPullParser, 3, "resolution");
                    xmlPullParser.next();
                }
                skipEmptyTextTags(xmlPullParser);
                if (accept(xmlPullParser, 2, "margins")) {
                    builder.setMinMargins(new PrintAttributes.Margins(Integer.parseInt(xmlPullParser.getAttributeValue(null, "leftMils")), Integer.parseInt(xmlPullParser.getAttributeValue(null, "topMils")), Integer.parseInt(xmlPullParser.getAttributeValue(null, "rightMils")), Integer.parseInt(xmlPullParser.getAttributeValue(null, "bottomMils"))));
                    xmlPullParser.next();
                    skipEmptyTextTags(xmlPullParser);
                    expect(xmlPullParser, 3, "margins");
                    xmlPullParser.next();
                }
                printJobInfo.setAttributes(builder.build());
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "attributes");
                xmlPullParser.next();
            }
            skipEmptyTextTags(xmlPullParser);
            if (accept(xmlPullParser, 2, "documentInfo")) {
                String attributeValue8 = xmlPullParser.getAttributeValue(null, "name");
                int i3 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "pageCount"));
                int i4 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "contentType"));
                int i5 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "dataSize"));
                PrintDocumentInfo printDocumentInfoBuild = new PrintDocumentInfo.Builder(attributeValue8).setPageCount(i3).setContentType(i4).build();
                printJobInfo.setDocumentInfo(printDocumentInfoBuild);
                printDocumentInfoBuild.setDataSize(i5);
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "documentInfo");
                xmlPullParser.next();
            }
            skipEmptyTextTags(xmlPullParser);
            if (accept(xmlPullParser, 2, "advancedOptions")) {
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                Bundle bundle = new Bundle();
                while (accept(xmlPullParser, 2, "advancedOption")) {
                    String attributeValue9 = xmlPullParser.getAttributeValue(null, "key");
                    String attributeValue10 = xmlPullParser.getAttributeValue(null, "value");
                    String attributeValue11 = xmlPullParser.getAttributeValue(null, "type");
                    if ("string".equals(attributeValue11)) {
                        bundle.putString(attributeValue9, attributeValue10);
                    } else if ("int".equals(attributeValue11)) {
                        bundle.putInt(attributeValue9, Integer.parseInt(attributeValue10));
                    }
                    xmlPullParser.next();
                    skipEmptyTextTags(xmlPullParser);
                    expect(xmlPullParser, 3, "advancedOption");
                    xmlPullParser.next();
                    skipEmptyTextTags(xmlPullParser);
                }
                printJobInfo.setAdvancedOptions(bundle);
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "advancedOptions");
                xmlPullParser.next();
            }
            PrintSpoolerService.this.mPrintJobs.add(printJobInfo);
            skipEmptyTextTags(xmlPullParser);
            expect(xmlPullParser, 3, "job");
            return true;
        }

        private void expect(XmlPullParser xmlPullParser, int i, String str) throws XmlPullParserException {
            if (!accept(xmlPullParser, i, str)) {
                throw new XmlPullParserException("Exepected event: " + i + " and tag: " + str + " but got event: " + xmlPullParser.getEventType() + " and tag:" + xmlPullParser.getName());
            }
        }

        private void skipEmptyTextTags(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            while (accept(xmlPullParser, 4, null) && "\n".equals(xmlPullParser.getText())) {
                xmlPullParser.next();
            }
        }

        private boolean accept(XmlPullParser xmlPullParser, int i, String str) throws XmlPullParserException {
            if (xmlPullParser.getEventType() != i) {
                return false;
            }
            return str != null ? str.equals(xmlPullParser.getName()) : xmlPullParser.getName() == null;
        }
    }

    public final class PrintSpooler extends IPrintSpooler.Stub {
        public PrintSpooler() {
        }

        public void getPrintJobInfos(IPrintSpoolerCallbacks iPrintSpoolerCallbacks, ComponentName componentName, int i, int i2, int i3) throws RemoteException {
            try {
                iPrintSpoolerCallbacks.onGetPrintJobInfosResult(PrintSpoolerService.this.getPrintJobInfos(componentName, i, i2), i3);
            } catch (Throwable th) {
                iPrintSpoolerCallbacks.onGetPrintJobInfosResult((List) null, i3);
                throw th;
            }
        }

        public void getPrintJobInfo(PrintJobId printJobId, IPrintSpoolerCallbacks iPrintSpoolerCallbacks, int i, int i2) throws RemoteException {
            try {
                iPrintSpoolerCallbacks.onGetPrintJobInfoResult(PrintSpoolerService.this.getPrintJobInfo(printJobId, i), i2);
            } catch (Throwable th) {
                iPrintSpoolerCallbacks.onGetPrintJobInfoResult((PrintJobInfo) null, i2);
                throw th;
            }
        }

        public void createPrintJob(PrintJobInfo printJobInfo) {
            PrintSpoolerService.this.createPrintJob(printJobInfo);
        }

        public void setPrintJobState(PrintJobId printJobId, int i, String str, IPrintSpoolerCallbacks iPrintSpoolerCallbacks, int i2) throws RemoteException {
            try {
                iPrintSpoolerCallbacks.onSetPrintJobStateResult(PrintSpoolerService.this.setPrintJobState(printJobId, i, str), i2);
            } catch (Throwable th) {
                iPrintSpoolerCallbacks.onSetPrintJobStateResult(false, i2);
                throw th;
            }
        }

        public void setPrintJobTag(PrintJobId printJobId, String str, IPrintSpoolerCallbacks iPrintSpoolerCallbacks, int i) throws RemoteException {
            try {
                iPrintSpoolerCallbacks.onSetPrintJobTagResult(PrintSpoolerService.this.setPrintJobTag(printJobId, str), i);
            } catch (Throwable th) {
                iPrintSpoolerCallbacks.onSetPrintJobTagResult(false, i);
                throw th;
            }
        }

        public void writePrintJobData(ParcelFileDescriptor parcelFileDescriptor, PrintJobId printJobId) {
            PrintSpoolerService.this.writePrintJobData(parcelFileDescriptor, printJobId);
        }

        public void setClient(IPrintSpoolerClient iPrintSpoolerClient) {
            Handler.getMain().executeOrSendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    ((PrintSpoolerService) obj).setClient((IPrintSpoolerClient) obj2);
                }
            }, PrintSpoolerService.this, iPrintSpoolerClient));
        }

        public void removeObsoletePrintJobs() {
            PrintSpoolerService.this.removeObsoletePrintJobs();
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            PrintSpoolerService.this.dump(fileDescriptor, printWriter, strArr);
        }

        public void setPrintJobCancelling(PrintJobId printJobId, boolean z) {
            PrintSpoolerService.this.setPrintJobCancelling(printJobId, z);
        }

        public void pruneApprovedPrintServices(List<ComponentName> list) {
            new ApprovedPrintServices(PrintSpoolerService.this).pruneApprovedServices(list);
        }

        public void setProgress(PrintJobId printJobId, float f) throws RemoteException {
            PrintSpoolerService.this.setProgress(printJobId, f);
        }

        public void setStatus(PrintJobId printJobId, CharSequence charSequence) throws RemoteException {
            PrintSpoolerService.this.setStatus(printJobId, charSequence);
        }

        public void setStatusRes(PrintJobId printJobId, int i, CharSequence charSequence) throws RemoteException {
            PrintSpoolerService.this.setStatus(printJobId, i, charSequence);
        }

        public PrintSpoolerService getService() {
            return PrintSpoolerService.this;
        }

        public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon, IPrintSpoolerCallbacks iPrintSpoolerCallbacks, int i) throws RemoteException {
            try {
                PrintSpoolerService.this.onCustomPrinterIconLoaded(printerId, icon);
            } finally {
                iPrintSpoolerCallbacks.onCustomPrinterIconCached(i);
            }
        }

        public void getCustomPrinterIcon(PrinterId printerId, IPrintSpoolerCallbacks iPrintSpoolerCallbacks, int i) throws RemoteException {
            try {
                iPrintSpoolerCallbacks.onGetCustomPrinterIconResult(PrintSpoolerService.this.getCustomPrinterIcon(printerId), i);
            } catch (Throwable th) {
                iPrintSpoolerCallbacks.onGetCustomPrinterIconResult((Icon) null, i);
                throw th;
            }
        }

        public void clearCustomPrinterIconCache(IPrintSpoolerCallbacks iPrintSpoolerCallbacks, int i) throws RemoteException {
            try {
                PrintSpoolerService.this.clearCustomPrinterIconCache();
            } finally {
                iPrintSpoolerCallbacks.customPrinterIconCacheCleared(i);
            }
        }
    }
}
