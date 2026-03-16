package com.android.printspooler.model;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
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
import com.android.internal.os.HandlerCaller;
import com.android.internal.util.FastXmlSerializer;
import com.android.printspooler.R;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class PrintSpoolerService extends Service {
    private static PrintSpoolerService sInstance;
    private static final Object sLock = new Object();
    private IPrintSpoolerClient mClient;
    private HandlerCaller mHandlerCaller;
    private NotificationController mNotificationController;
    private PersistenceManager mPersistanceManager;
    private final Object mLock = new Object();
    private final List<PrintJobInfo> mPrintJobs = new ArrayList();

    @Override
    public void onCreate() {
        super.onCreate();
        this.mHandlerCaller = new HandlerCaller(this, getMainLooper(), new HandlerCallerCallback(), false);
        this.mPersistanceManager = new PersistenceManager();
        this.mNotificationController = new NotificationController(this);
        synchronized (this.mLock) {
            this.mPersistanceManager.readStateLocked();
            handleReadPrintJobsLocked();
        }
        synchronized (sLock) {
            sInstance = this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PrintSpooler();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            String prefix = args.length > 0 ? args[0] : "";
            pw.append((CharSequence) prefix).append((CharSequence) "print jobs:").println();
            int printJobCount = this.mPrintJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                PrintJobInfo printJob = this.mPrintJobs.get(i);
                pw.append((CharSequence) prefix).append((CharSequence) "  ").append((CharSequence) printJob.toString());
                pw.println();
            }
            pw.append((CharSequence) prefix).append((CharSequence) "print job files:").println();
            File[] files = getFilesDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("print_job_")) {
                        pw.append((CharSequence) prefix).append((CharSequence) "  ").append((CharSequence) file.getName()).println();
                    }
                }
            }
        }
    }

    private void sendOnPrintJobQueued(PrintJobInfo printJob) {
        Message message = this.mHandlerCaller.obtainMessageO(2, printJob);
        this.mHandlerCaller.executeOrSendMessage(message);
    }

    private void sendOnAllPrintJobsForServiceHandled(ComponentName service) {
        Message message = this.mHandlerCaller.obtainMessageO(3, service);
        this.mHandlerCaller.executeOrSendMessage(message);
    }

    private void sendOnAllPrintJobsHandled() {
        Message message = this.mHandlerCaller.obtainMessage(4);
        this.mHandlerCaller.executeOrSendMessage(message);
    }

    private final class HandlerCallerCallback implements HandlerCaller.Callback {
        private HandlerCallerCallback() {
        }

        public void executeMessage(Message message) {
            switch (message.what) {
                case 1:
                    synchronized (PrintSpoolerService.this.mLock) {
                        PrintSpoolerService.this.mClient = (IPrintSpoolerClient) message.obj;
                        if (PrintSpoolerService.this.mClient != null) {
                            Message msg = PrintSpoolerService.this.mHandlerCaller.obtainMessage(5);
                            PrintSpoolerService.this.mHandlerCaller.sendMessageDelayed(msg, 5000L);
                        }
                        break;
                    }
                    return;
                case 2:
                    PrintJobInfo printJob = (PrintJobInfo) message.obj;
                    if (PrintSpoolerService.this.mClient != null) {
                        try {
                            PrintSpoolerService.this.mClient.onPrintJobQueued(printJob);
                            return;
                        } catch (RemoteException re) {
                            Slog.e("PrintSpoolerService", "Error notify for a queued print job.", re);
                            return;
                        }
                    }
                    return;
                case 3:
                    ComponentName service = (ComponentName) message.obj;
                    if (PrintSpoolerService.this.mClient != null) {
                        try {
                            PrintSpoolerService.this.mClient.onAllPrintJobsForServiceHandled(service);
                            return;
                        } catch (RemoteException re2) {
                            Slog.e("PrintSpoolerService", "Error notify for all print jobs per service handled.", re2);
                            return;
                        }
                    }
                    return;
                case 4:
                    if (PrintSpoolerService.this.mClient != null) {
                        try {
                            PrintSpoolerService.this.mClient.onAllPrintJobsHandled();
                            return;
                        } catch (RemoteException re3) {
                            Slog.e("PrintSpoolerService", "Error notify for all print job handled.", re3);
                            return;
                        }
                    }
                    return;
                case 5:
                    PrintSpoolerService.this.checkAllPrintJobsHandled();
                    return;
                case 6:
                    if (PrintSpoolerService.this.mClient != null) {
                        PrintJobInfo printJob2 = (PrintJobInfo) message.obj;
                        try {
                            PrintSpoolerService.this.mClient.onPrintJobStateChanged(printJob2);
                            return;
                        } catch (RemoteException re4) {
                            Slog.e("PrintSpoolerService", "Error notify for print job state change.", re4);
                            return;
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public List<PrintJobInfo> getPrintJobInfos(ComponentName componentName, int state, int appId) throws Throwable {
        List<PrintJobInfo> foundPrintJobs;
        synchronized (this.mLock) {
            try {
                int printJobCount = this.mPrintJobs.size();
                int i = 0;
                List<PrintJobInfo> foundPrintJobs2 = null;
                while (i < printJobCount) {
                    try {
                        PrintJobInfo printJob = this.mPrintJobs.get(i);
                        PrinterId printerId = printJob.getPrinterId();
                        boolean sameComponent = componentName == null || (printerId != null && componentName.equals(printerId.getServiceName()));
                        boolean sameAppId = appId == -2 || printJob.getAppId() == appId;
                        boolean sameState = state == printJob.getState() || state == -1 || (state == -2 && isStateVisibleToUser(printJob.getState())) || ((state == -3 && isActiveState(printJob.getState())) || (state == -4 && isScheduledState(printJob.getState())));
                        if (sameComponent && sameAppId && sameState) {
                            foundPrintJobs = foundPrintJobs2 == null ? new ArrayList<>() : foundPrintJobs2;
                            foundPrintJobs.add(printJob);
                        } else {
                            foundPrintJobs = foundPrintJobs2;
                        }
                        i++;
                        foundPrintJobs2 = foundPrintJobs;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                return foundPrintJobs2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private boolean isStateVisibleToUser(int state) {
        return isActiveState(state) && (state == 6 || state == 5 || state == 7 || state == 4);
    }

    public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId) {
        PrintJobInfo printJob;
        synchronized (this.mLock) {
            int printJobCount = this.mPrintJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                printJob = this.mPrintJobs.get(i);
                if (printJob.getId().equals(printJobId) && (appId == -2 || appId == printJob.getAppId())) {
                    break;
                }
            }
            printJob = null;
        }
        return printJob;
    }

    public void createPrintJob(PrintJobInfo printJob) {
        synchronized (this.mLock) {
            addPrintJobLocked(printJob);
            setPrintJobState(printJob.getId(), 1, null);
            Message message = this.mHandlerCaller.obtainMessageO(6, printJob);
            this.mHandlerCaller.executeOrSendMessage(message);
        }
    }

    private void handleReadPrintJobsLocked() {
        ArrayMap<PrintJobId, File> fileForJobMap = null;
        File[] files = getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("print_job_")) {
                    if (fileForJobMap == null) {
                        fileForJobMap = new ArrayMap<>();
                    }
                    String printJobIdString = file.getName().substring("print_job_".length(), file.getName().indexOf(46));
                    PrintJobId printJobId = PrintJobId.unflattenFromString(printJobIdString);
                    fileForJobMap.put(printJobId, file);
                }
            }
        }
        int printJobCount = this.mPrintJobs.size();
        for (int i = 0; i < printJobCount; i++) {
            PrintJobInfo printJob = this.mPrintJobs.get(i);
            if (fileForJobMap != null) {
                fileForJobMap.remove(printJob.getId());
            }
            switch (printJob.getState()) {
                case 2:
                case 3:
                case 4:
                    setPrintJobState(printJob.getId(), 6, getString(R.string.no_connection_to_printer));
                    break;
            }
        }
        if (!this.mPrintJobs.isEmpty()) {
            this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
        }
        if (fileForJobMap != null) {
            int orphanFileCount = fileForJobMap.size();
            for (int i2 = 0; i2 < orphanFileCount; i2++) {
                fileForJobMap.valueAt(i2).delete();
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

    public void writePrintJobData(final ParcelFileDescriptor fd, final PrintJobId printJobId) {
        final PrintJobInfo printJob;
        synchronized (this.mLock) {
            printJob = getPrintJobInfo(printJobId, -2);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) throws Throwable {
                FileInputStream in = null;
                FileOutputStream out = null;
                try {
                    try {
                        if (printJob != null) {
                            File file = PrintSpoolerService.generateFileForPrintJob(PrintSpoolerService.this, printJobId);
                            FileInputStream in2 = new FileInputStream(file);
                            try {
                                out = new FileOutputStream(fd.getFileDescriptor());
                                in = in2;
                            } catch (FileNotFoundException e) {
                                fnfe = e;
                                in = in2;
                                Log.e("PrintSpoolerService", "Error writing print job data!", fnfe);
                                IoUtils.closeQuietly(in);
                                IoUtils.closeQuietly(out);
                                IoUtils.closeQuietly(fd);
                                Log.i("PrintSpoolerService", "[END WRITE]");
                            } catch (IOException e2) {
                                ioe = e2;
                                in = in2;
                                Log.e("PrintSpoolerService", "Error writing print job data!", ioe);
                                IoUtils.closeQuietly(in);
                                IoUtils.closeQuietly(out);
                                IoUtils.closeQuietly(fd);
                                Log.i("PrintSpoolerService", "[END WRITE]");
                            } catch (Throwable th) {
                                th = th;
                                in = in2;
                                IoUtils.closeQuietly(in);
                                IoUtils.closeQuietly((AutoCloseable) null);
                                IoUtils.closeQuietly(fd);
                                throw th;
                            }
                        }
                        byte[] buffer = new byte[8192];
                        while (true) {
                            int readByteCount = in.read(buffer);
                            if (readByteCount < 0) {
                                break;
                            }
                            out.write(buffer, 0, readByteCount);
                        }
                        IoUtils.closeQuietly(in);
                        IoUtils.closeQuietly(out);
                        IoUtils.closeQuietly(fd);
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (FileNotFoundException e3) {
                    fnfe = e3;
                } catch (IOException e4) {
                    ioe = e4;
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    public static File generateFileForPrintJob(Context context, PrintJobId printJobId) {
        return new File(context.getFilesDir(), "print_job_" + printJobId.flattenToString() + ".pdf");
    }

    private void addPrintJobLocked(PrintJobInfo printJob) {
        this.mPrintJobs.add(printJob);
    }

    private void removeObsoletePrintJobs() {
        synchronized (this.mLock) {
            boolean persistState = false;
            int printJobCount = this.mPrintJobs.size();
            for (int i = printJobCount - 1; i >= 0; i--) {
                PrintJobInfo printJob = this.mPrintJobs.get(i);
                if (isObsoleteState(printJob.getState())) {
                    this.mPrintJobs.remove(i);
                    removePrintJobFileLocked(printJob.getId());
                    persistState = true;
                }
            }
            if (persistState) {
                this.mPersistanceManager.writeStateLocked();
            }
        }
    }

    private void removePrintJobFileLocked(PrintJobId printJobId) {
        File file = generateFileForPrintJob(this, printJobId);
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
        boolean success = false;
        synchronized (this.mLock) {
            PrintJobInfo printJob = getPrintJobInfo(printJobId, -2);
            if (printJob != null) {
                int oldState = printJob.getState();
                if (oldState == state) {
                    return false;
                }
                success = true;
                printJob.setState(state);
                printJob.setStateReason(error);
                printJob.setCancelling(false);
                switch (state) {
                    case 2:
                        sendOnPrintJobQueued(new PrintJobInfo(printJob));
                        break;
                    case 5:
                    case 7:
                        this.mPrintJobs.remove(printJob);
                        removePrintJobFileLocked(printJob.getId());
                    case 6:
                        PrinterId printerId = printJob.getPrinterId();
                        if (printerId != null) {
                            ComponentName service = printerId.getServiceName();
                            if (!hasActivePrintJobsForServiceLocked(service)) {
                                sendOnAllPrintJobsForServiceHandled(service);
                            }
                        }
                        break;
                }
                if (shouldPersistPrintJob(printJob)) {
                    this.mPersistanceManager.writeStateLocked();
                }
                if (!hasActivePrintJobsLocked()) {
                    notifyOnAllPrintJobsHandled();
                }
                Message message = this.mHandlerCaller.obtainMessageO(6, printJob);
                this.mHandlerCaller.executeOrSendMessage(message);
                this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
            }
            return success;
        }
    }

    public boolean hasActivePrintJobsLocked() {
        int printJobCount = this.mPrintJobs.size();
        for (int i = 0; i < printJobCount; i++) {
            PrintJobInfo printJob = this.mPrintJobs.get(i);
            if (isActiveState(printJob.getState())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActivePrintJobsForServiceLocked(ComponentName service) {
        int printJobCount = this.mPrintJobs.size();
        for (int i = 0; i < printJobCount; i++) {
            PrintJobInfo printJob = this.mPrintJobs.get(i);
            if (isActiveState(printJob.getState()) && printJob.getPrinterId() != null && printJob.getPrinterId().getServiceName().equals(service)) {
                return true;
            }
        }
        return false;
    }

    private boolean isObsoleteState(int printJobState) {
        return isTerminalState(printJobState) || printJobState == 2;
    }

    private boolean isScheduledState(int printJobState) {
        return printJobState == 2 || printJobState == 3 || printJobState == 4;
    }

    private boolean isActiveState(int printJobState) {
        return printJobState == 1 || printJobState == 2 || printJobState == 3 || printJobState == 4;
    }

    private boolean isTerminalState(int printJobState) {
        return printJobState == 5 || printJobState == 7;
    }

    public boolean setPrintJobTag(PrintJobId printJobId, String tag) {
        boolean z = false;
        synchronized (this.mLock) {
            PrintJobInfo printJob = getPrintJobInfo(printJobId, -2);
            if (printJob != null) {
                String printJobTag = printJob.getTag();
                if (printJobTag == null) {
                    if (tag != null) {
                        printJob.setTag(tag);
                        if (shouldPersistPrintJob(printJob)) {
                            this.mPersistanceManager.writeStateLocked();
                        }
                        z = true;
                    }
                } else if (printJobTag.equals(tag)) {
                }
            }
        }
        return z;
    }

    public void setPrintJobCancelling(PrintJobId printJobId, boolean cancelling) {
        synchronized (this.mLock) {
            PrintJobInfo printJob = getPrintJobInfo(printJobId, -2);
            if (printJob != null) {
                printJob.setCancelling(cancelling);
                if (shouldPersistPrintJob(printJob)) {
                    this.mPersistanceManager.writeStateLocked();
                }
                this.mNotificationController.onUpdateNotifications(this.mPrintJobs);
                Message message = this.mHandlerCaller.obtainMessageO(6, printJob);
                this.mHandlerCaller.executeOrSendMessage(message);
            }
        }
    }

    public void updatePrintJobUserConfigurableOptionsNoPersistence(PrintJobInfo printJob) {
        synchronized (this.mLock) {
            int printJobCount = this.mPrintJobs.size();
            for (int i = 0; i < printJobCount; i++) {
                PrintJobInfo cachedPrintJob = this.mPrintJobs.get(i);
                if (cachedPrintJob.getId().equals(printJob.getId())) {
                    cachedPrintJob.setPrinterId(printJob.getPrinterId());
                    cachedPrintJob.setPrinterName(printJob.getPrinterName());
                    cachedPrintJob.setCopies(printJob.getCopies());
                    cachedPrintJob.setDocumentInfo(printJob.getDocumentInfo());
                    cachedPrintJob.setPages(printJob.getPages());
                    cachedPrintJob.setAttributes(printJob.getAttributes());
                    cachedPrintJob.setAdvancedOptions(printJob.getAdvancedOptions());
                }
            }
            throw new IllegalArgumentException("No print job with id:" + printJob.getId());
        }
    }

    private boolean shouldPersistPrintJob(PrintJobInfo printJob) {
        return printJob.getState() >= 2;
    }

    private void notifyOnAllPrintJobsHandled() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                PrintSpoolerService.this.sendOnAllPrintJobsHandled();
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
    }

    private final class PersistenceManager {
        private final AtomicFile mStatePersistFile;
        private boolean mWriteStateScheduled;

        private PersistenceManager() {
            this.mStatePersistFile = new AtomicFile(new File(PrintSpoolerService.this.getFilesDir(), "print_spooler_state.xml"));
        }

        public void writeStateLocked() {
            if (!this.mWriteStateScheduled) {
                this.mWriteStateScheduled = true;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        synchronized (PrintSpoolerService.this.mLock) {
                            PersistenceManager.this.mWriteStateScheduled = false;
                            PersistenceManager.this.doWriteStateLocked();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
            }
        }

        private void doWriteStateLocked() {
            FileOutputStream out = null;
            try {
                out = this.mStatePersistFile.startWrite();
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(out, "utf-8");
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, "spooler");
                List<PrintJobInfo> printJobs = PrintSpoolerService.this.mPrintJobs;
                int printJobCount = printJobs.size();
                for (int j = 0; j < printJobCount; j++) {
                    PrintJobInfo printJob = printJobs.get(j);
                    if (PrintSpoolerService.this.shouldPersistPrintJob(printJob)) {
                        fastXmlSerializer.startTag(null, "job");
                        fastXmlSerializer.attribute(null, "id", printJob.getId().flattenToString());
                        fastXmlSerializer.attribute(null, "label", printJob.getLabel().toString());
                        fastXmlSerializer.attribute(null, "state", String.valueOf(printJob.getState()));
                        fastXmlSerializer.attribute(null, "appId", String.valueOf(printJob.getAppId()));
                        String tag = printJob.getTag();
                        if (tag != null) {
                            fastXmlSerializer.attribute(null, "tag", tag);
                        }
                        fastXmlSerializer.attribute(null, "creationTime", String.valueOf(printJob.getCreationTime()));
                        fastXmlSerializer.attribute(null, "copies", String.valueOf(printJob.getCopies()));
                        String printerName = printJob.getPrinterName();
                        if (!TextUtils.isEmpty(printerName)) {
                            fastXmlSerializer.attribute(null, "printerName", printerName);
                        }
                        String stateReason = printJob.getStateReason();
                        if (!TextUtils.isEmpty(stateReason)) {
                            fastXmlSerializer.attribute(null, "stateReason", stateReason);
                        }
                        fastXmlSerializer.attribute(null, "cancelling", String.valueOf(printJob.isCancelling()));
                        PrinterId printerId = printJob.getPrinterId();
                        if (printerId != null) {
                            fastXmlSerializer.startTag(null, "printerId");
                            fastXmlSerializer.attribute(null, "localId", printerId.getLocalId());
                            fastXmlSerializer.attribute(null, "serviceName", printerId.getServiceName().flattenToString());
                            fastXmlSerializer.endTag(null, "printerId");
                        }
                        PageRange[] pages = printJob.getPages();
                        if (pages != null) {
                            for (int i = 0; i < pages.length; i++) {
                                fastXmlSerializer.startTag(null, "pageRange");
                                fastXmlSerializer.attribute(null, "start", String.valueOf(pages[i].getStart()));
                                fastXmlSerializer.attribute(null, "end", String.valueOf(pages[i].getEnd()));
                                fastXmlSerializer.endTag(null, "pageRange");
                            }
                        }
                        PrintAttributes attributes = printJob.getAttributes();
                        if (attributes != null) {
                            fastXmlSerializer.startTag(null, "attributes");
                            int colorMode = attributes.getColorMode();
                            fastXmlSerializer.attribute(null, "colorMode", String.valueOf(colorMode));
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
                            PrintAttributes.Margins margins = attributes.getMinMargins();
                            if (margins != null) {
                                fastXmlSerializer.startTag(null, "margins");
                                fastXmlSerializer.attribute(null, "leftMils", String.valueOf(margins.getLeftMils()));
                                fastXmlSerializer.attribute(null, "topMils", String.valueOf(margins.getTopMils()));
                                fastXmlSerializer.attribute(null, "rightMils", String.valueOf(margins.getRightMils()));
                                fastXmlSerializer.attribute(null, "bottomMils", String.valueOf(margins.getBottomMils()));
                                fastXmlSerializer.endTag(null, "margins");
                            }
                            fastXmlSerializer.endTag(null, "attributes");
                        }
                        PrintDocumentInfo documentInfo = printJob.getDocumentInfo();
                        if (documentInfo != null) {
                            fastXmlSerializer.startTag(null, "documentInfo");
                            fastXmlSerializer.attribute(null, "name", documentInfo.getName());
                            fastXmlSerializer.attribute(null, "contentType", String.valueOf(documentInfo.getContentType()));
                            fastXmlSerializer.attribute(null, "pageCount", String.valueOf(documentInfo.getPageCount()));
                            fastXmlSerializer.attribute(null, "dataSize", String.valueOf(documentInfo.getDataSize()));
                            fastXmlSerializer.endTag(null, "documentInfo");
                        }
                        Bundle advancedOptions = printJob.getAdvancedOptions();
                        if (advancedOptions != null) {
                            fastXmlSerializer.startTag(null, "advancedOptions");
                            for (String key : advancedOptions.keySet()) {
                                Object value = advancedOptions.get(key);
                                if (value instanceof String) {
                                    String stringValue = (String) value;
                                    fastXmlSerializer.startTag(null, "advancedOption");
                                    fastXmlSerializer.attribute(null, "key", key);
                                    fastXmlSerializer.attribute(null, "type", "string");
                                    fastXmlSerializer.attribute(null, "value", stringValue);
                                    fastXmlSerializer.endTag(null, "advancedOption");
                                } else if (value instanceof Integer) {
                                    String intValue = Integer.toString(((Integer) value).intValue());
                                    fastXmlSerializer.startTag(null, "advancedOption");
                                    fastXmlSerializer.attribute(null, "key", key);
                                    fastXmlSerializer.attribute(null, "type", "int");
                                    fastXmlSerializer.attribute(null, "value", intValue);
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
                this.mStatePersistFile.finishWrite(out);
            } catch (IOException e) {
                Slog.w("PrintSpoolerService", "Failed to write state, restoring backup.", e);
                this.mStatePersistFile.failWrite(out);
            } finally {
                IoUtils.closeQuietly(out);
            }
        }

        public void readStateLocked() {
            try {
                FileInputStream in = this.mStatePersistFile.openRead();
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(in, null);
                    parseState(parser);
                } catch (IOException ioe) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", ioe);
                } catch (XmlPullParserException xppe) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", xppe);
                } catch (IllegalStateException ise) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", ise);
                } catch (IndexOutOfBoundsException iobe) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", iobe);
                } catch (NullPointerException npe) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", npe);
                } catch (NumberFormatException nfe) {
                    Slog.w("PrintSpoolerService", "Failed parsing ", nfe);
                } finally {
                    IoUtils.closeQuietly(in);
                }
            } catch (FileNotFoundException e) {
                Log.i("PrintSpoolerService", "No existing print spooler state.");
            }
        }

        private void parseState(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.next();
            skipEmptyTextTags(parser);
            expect(parser, 2, "spooler");
            parser.next();
            while (parsePrintJob(parser)) {
                parser.next();
            }
            skipEmptyTextTags(parser);
            expect(parser, 3, "spooler");
        }

        private boolean parsePrintJob(XmlPullParser parser) throws XmlPullParserException, IOException {
            skipEmptyTextTags(parser);
            if (!accept(parser, 2, "job")) {
                return false;
            }
            PrintJobInfo printJob = new PrintJobInfo();
            PrintJobId printJobId = PrintJobId.unflattenFromString(parser.getAttributeValue(null, "id"));
            printJob.setId(printJobId);
            String label = parser.getAttributeValue(null, "label");
            printJob.setLabel(label);
            int state = Integer.parseInt(parser.getAttributeValue(null, "state"));
            printJob.setState(state);
            int appId = Integer.parseInt(parser.getAttributeValue(null, "appId"));
            printJob.setAppId(appId);
            String tag = parser.getAttributeValue(null, "tag");
            printJob.setTag(tag);
            String creationTime = parser.getAttributeValue(null, "creationTime");
            printJob.setCreationTime(Long.parseLong(creationTime));
            String copies = parser.getAttributeValue(null, "copies");
            printJob.setCopies(Integer.parseInt(copies));
            String printerName = parser.getAttributeValue(null, "printerName");
            printJob.setPrinterName(printerName);
            String stateReason = parser.getAttributeValue(null, "stateReason");
            printJob.setStateReason(stateReason);
            String cancelling = parser.getAttributeValue(null, "cancelling");
            printJob.setCancelling(!TextUtils.isEmpty(cancelling) ? Boolean.parseBoolean(cancelling) : false);
            parser.next();
            skipEmptyTextTags(parser);
            if (accept(parser, 2, "printerId")) {
                String localId = parser.getAttributeValue(null, "localId");
                ComponentName service = ComponentName.unflattenFromString(parser.getAttributeValue(null, "serviceName"));
                printJob.setPrinterId(new PrinterId(service, localId));
                parser.next();
                skipEmptyTextTags(parser);
                expect(parser, 3, "printerId");
                parser.next();
            }
            skipEmptyTextTags(parser);
            List<PageRange> pageRanges = null;
            while (accept(parser, 2, "pageRange")) {
                int start = Integer.parseInt(parser.getAttributeValue(null, "start"));
                int end = Integer.parseInt(parser.getAttributeValue(null, "end"));
                PageRange pageRange = new PageRange(start, end);
                if (pageRanges == null) {
                    pageRanges = new ArrayList<>();
                }
                pageRanges.add(pageRange);
                parser.next();
                skipEmptyTextTags(parser);
                expect(parser, 3, "pageRange");
                parser.next();
                skipEmptyTextTags(parser);
            }
            if (pageRanges != null) {
                PageRange[] pageRangesArray = new PageRange[pageRanges.size()];
                pageRanges.toArray(pageRangesArray);
                printJob.setPages(pageRangesArray);
            }
            skipEmptyTextTags(parser);
            if (accept(parser, 2, "attributes")) {
                PrintAttributes.Builder builder = new PrintAttributes.Builder();
                String colorMode = parser.getAttributeValue(null, "colorMode");
                builder.setColorMode(Integer.parseInt(colorMode));
                parser.next();
                skipEmptyTextTags(parser);
                if (accept(parser, 2, "mediaSize")) {
                    String id = parser.getAttributeValue(null, "id");
                    parser.getAttributeValue(null, "label");
                    int widthMils = Integer.parseInt(parser.getAttributeValue(null, "widthMils"));
                    int heightMils = Integer.parseInt(parser.getAttributeValue(null, "heightMils"));
                    String packageName = parser.getAttributeValue(null, "packageName");
                    String labelResIdString = parser.getAttributeValue(null, "labelResId");
                    int labelResId = labelResIdString != null ? Integer.parseInt(labelResIdString) : 0;
                    String label2 = parser.getAttributeValue(null, "label");
                    PrintAttributes.MediaSize mediaSize = new PrintAttributes.MediaSize(id, label2, packageName, widthMils, heightMils, labelResId);
                    builder.setMediaSize(mediaSize);
                    parser.next();
                    skipEmptyTextTags(parser);
                    expect(parser, 3, "mediaSize");
                    parser.next();
                }
                skipEmptyTextTags(parser);
                if (accept(parser, 2, "resolution")) {
                    String id2 = parser.getAttributeValue(null, "id");
                    String label3 = parser.getAttributeValue(null, "label");
                    int horizontalDpi = Integer.parseInt(parser.getAttributeValue(null, "horizontalDip"));
                    int verticalDpi = Integer.parseInt(parser.getAttributeValue(null, "verticalDpi"));
                    PrintAttributes.Resolution resolution = new PrintAttributes.Resolution(id2, label3, horizontalDpi, verticalDpi);
                    builder.setResolution(resolution);
                    parser.next();
                    skipEmptyTextTags(parser);
                    expect(parser, 3, "resolution");
                    parser.next();
                }
                skipEmptyTextTags(parser);
                if (accept(parser, 2, "margins")) {
                    int leftMils = Integer.parseInt(parser.getAttributeValue(null, "leftMils"));
                    int topMils = Integer.parseInt(parser.getAttributeValue(null, "topMils"));
                    int rightMils = Integer.parseInt(parser.getAttributeValue(null, "rightMils"));
                    int bottomMils = Integer.parseInt(parser.getAttributeValue(null, "bottomMils"));
                    PrintAttributes.Margins margins = new PrintAttributes.Margins(leftMils, topMils, rightMils, bottomMils);
                    builder.setMinMargins(margins);
                    parser.next();
                    skipEmptyTextTags(parser);
                    expect(parser, 3, "margins");
                    parser.next();
                }
                printJob.setAttributes(builder.build());
                skipEmptyTextTags(parser);
                expect(parser, 3, "attributes");
                parser.next();
            }
            skipEmptyTextTags(parser);
            if (accept(parser, 2, "documentInfo")) {
                String name = parser.getAttributeValue(null, "name");
                int pageCount = Integer.parseInt(parser.getAttributeValue(null, "pageCount"));
                int contentType = Integer.parseInt(parser.getAttributeValue(null, "contentType"));
                int dataSize = Integer.parseInt(parser.getAttributeValue(null, "dataSize"));
                PrintDocumentInfo info = new PrintDocumentInfo.Builder(name).setPageCount(pageCount).setContentType(contentType).build();
                printJob.setDocumentInfo(info);
                info.setDataSize(dataSize);
                parser.next();
                skipEmptyTextTags(parser);
                expect(parser, 3, "documentInfo");
                parser.next();
            }
            skipEmptyTextTags(parser);
            if (accept(parser, 2, "advancedOptions")) {
                parser.next();
                skipEmptyTextTags(parser);
                Bundle advancedOptions = new Bundle();
                while (accept(parser, 2, "advancedOption")) {
                    String key = parser.getAttributeValue(null, "key");
                    String value = parser.getAttributeValue(null, "value");
                    String type = parser.getAttributeValue(null, "type");
                    if ("string".equals(type)) {
                        advancedOptions.putString(key, value);
                    } else if ("int".equals(type)) {
                        advancedOptions.putInt(key, Integer.valueOf(value).intValue());
                    }
                    parser.next();
                    skipEmptyTextTags(parser);
                    expect(parser, 3, "advancedOption");
                    parser.next();
                    skipEmptyTextTags(parser);
                }
                printJob.setAdvancedOptions(advancedOptions);
                skipEmptyTextTags(parser);
                expect(parser, 3, "advancedOptions");
                parser.next();
            }
            PrintSpoolerService.this.mPrintJobs.add(printJob);
            skipEmptyTextTags(parser);
            expect(parser, 3, "job");
            return true;
        }

        private void expect(XmlPullParser parser, int type, String tag) throws XmlPullParserException, IOException {
            if (!accept(parser, type, tag)) {
                throw new XmlPullParserException("Exepected event: " + type + " and tag: " + tag + " but got event: " + parser.getEventType() + " and tag:" + parser.getName());
            }
        }

        private void skipEmptyTextTags(XmlPullParser parser) throws XmlPullParserException, IOException {
            while (accept(parser, 4, null) && "\n".equals(parser.getText())) {
                parser.next();
            }
        }

        private boolean accept(XmlPullParser parser, int type, String tag) throws XmlPullParserException, IOException {
            if (parser.getEventType() != type) {
                return false;
            }
            if (tag != null) {
                if (!tag.equals(parser.getName())) {
                    return false;
                }
            } else if (parser.getName() != null) {
                return false;
            }
            return true;
        }
    }

    public final class PrintSpooler extends IPrintSpooler.Stub {
        public PrintSpooler() {
        }

        public void getPrintJobInfos(IPrintSpoolerCallbacks callback, ComponentName componentName, int state, int appId, int sequence) throws RemoteException {
            List<PrintJobInfo> printJobs = null;
            try {
                printJobs = PrintSpoolerService.this.getPrintJobInfos(componentName, state, appId);
            } finally {
                callback.onGetPrintJobInfosResult(printJobs, sequence);
            }
        }

        public void getPrintJobInfo(PrintJobId printJobId, IPrintSpoolerCallbacks callback, int appId, int sequence) throws RemoteException {
            PrintJobInfo printJob = null;
            try {
                printJob = PrintSpoolerService.this.getPrintJobInfo(printJobId, appId);
            } finally {
                callback.onGetPrintJobInfoResult(printJob, sequence);
            }
        }

        public void createPrintJob(PrintJobInfo printJob) {
            PrintSpoolerService.this.createPrintJob(printJob);
        }

        public void setPrintJobState(PrintJobId printJobId, int state, String error, IPrintSpoolerCallbacks callback, int sequece) throws RemoteException {
            boolean success = false;
            try {
                success = PrintSpoolerService.this.setPrintJobState(printJobId, state, error);
            } finally {
                callback.onSetPrintJobStateResult(success, sequece);
            }
        }

        public void setPrintJobTag(PrintJobId printJobId, String tag, IPrintSpoolerCallbacks callback, int sequece) throws RemoteException {
            boolean success = false;
            try {
                success = PrintSpoolerService.this.setPrintJobTag(printJobId, tag);
            } finally {
                callback.onSetPrintJobTagResult(success, sequece);
            }
        }

        public void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
            PrintSpoolerService.this.writePrintJobData(fd, printJobId);
        }

        public void setClient(IPrintSpoolerClient client) {
            Message message = PrintSpoolerService.this.mHandlerCaller.obtainMessageO(1, client);
            PrintSpoolerService.this.mHandlerCaller.executeOrSendMessage(message);
        }

        public void removeObsoletePrintJobs() {
            PrintSpoolerService.this.removeObsoletePrintJobs();
        }

        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            PrintSpoolerService.this.dump(fd, writer, args);
        }

        public void setPrintJobCancelling(PrintJobId printJobId, boolean cancelling) {
            PrintSpoolerService.this.setPrintJobCancelling(printJobId, cancelling);
        }

        public PrintSpoolerService getService() {
            return PrintSpoolerService.this;
        }
    }
}
