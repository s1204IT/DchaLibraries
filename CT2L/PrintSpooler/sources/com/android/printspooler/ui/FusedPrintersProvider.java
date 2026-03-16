package com.android.printspooler.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Loader;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.print.PrintManager;
import android.print.PrinterDiscoverySession;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class FusedPrintersProvider extends Loader<List<PrinterInfo>> {
    private PrinterDiscoverySession mDiscoverySession;
    private final List<PrinterInfo> mFavoritePrinters;
    private final PersistenceManager mPersistenceManager;
    private final List<PrinterInfo> mPrinters;
    private boolean mPrintersUpdatedBefore;
    private PrinterId mTrackedPrinter;

    public FusedPrintersProvider(Context context) {
        super(context);
        this.mPrinters = new ArrayList();
        this.mFavoritePrinters = new ArrayList();
        this.mPersistenceManager = new PersistenceManager(context);
    }

    public void addHistoricalPrinter(PrinterInfo printer) {
        this.mPersistenceManager.addPrinterAndWritePrinterHistory(printer);
    }

    private void computeAndDeliverResult(Map<PrinterId, PrinterInfo> discoveredPrinters, List<PrinterInfo> favoritePrinters) {
        ArrayList arrayList = new ArrayList();
        int favoritePrinterCount = favoritePrinters.size();
        for (int i = 0; i < favoritePrinterCount; i++) {
            PrinterInfo favoritePrinter = favoritePrinters.get(i);
            PrinterInfo updatedPrinter = discoveredPrinters.remove(favoritePrinter.getId());
            if (updatedPrinter != null) {
                arrayList.add(updatedPrinter);
            } else {
                arrayList.add(favoritePrinter);
            }
        }
        int printerCount = this.mPrinters.size();
        for (int i2 = 0; i2 < printerCount; i2++) {
            PrinterInfo printer = this.mPrinters.get(i2);
            PrinterInfo updatedPrinter2 = discoveredPrinters.remove(printer.getId());
            if (updatedPrinter2 != null) {
                arrayList.add(updatedPrinter2);
            }
        }
        arrayList.addAll(discoveredPrinters.values());
        this.mPrinters.clear();
        this.mPrinters.addAll(arrayList);
        if (isStarted()) {
            deliverResult(arrayList);
        } else {
            onContentChanged();
        }
    }

    @Override
    protected void onStartLoading() {
        if (!this.mPrinters.isEmpty()) {
            deliverResult(new ArrayList(this.mPrinters));
        }
        onForceLoad();
    }

    @Override
    protected void onStopLoading() {
        onCancelLoad();
    }

    @Override
    protected void onForceLoad() {
        loadInternal();
    }

    private void loadInternal() {
        if (this.mDiscoverySession == null) {
            PrintManager printManager = (PrintManager) getContext().getSystemService("print");
            this.mDiscoverySession = printManager.createPrinterDiscoverySession();
            this.mPersistenceManager.readPrinterHistory();
        } else if (this.mPersistenceManager.isHistoryChanged()) {
            this.mPersistenceManager.readPrinterHistory();
        }
        if (this.mPersistenceManager.isReadHistoryCompleted() && !this.mDiscoverySession.isPrinterDiscoveryStarted()) {
            this.mDiscoverySession.setOnPrintersChangeListener(new PrinterDiscoverySession.OnPrintersChangeListener() {
                public void onPrintersChanged() {
                    FusedPrintersProvider.this.updatePrinters(FusedPrintersProvider.this.mDiscoverySession.getPrinters(), FusedPrintersProvider.this.mFavoritePrinters);
                }
            });
            int favoriteCount = this.mFavoritePrinters.size();
            List<PrinterId> printerIds = new ArrayList<>(favoriteCount);
            for (int i = 0; i < favoriteCount; i++) {
                printerIds.add(this.mFavoritePrinters.get(i).getId());
            }
            this.mDiscoverySession.startPrinterDiscovery(printerIds);
            List<PrinterInfo> printers = this.mDiscoverySession.getPrinters();
            if (!printers.isEmpty()) {
                updatePrinters(printers, this.mFavoritePrinters);
            }
        }
    }

    private void updatePrinters(List<PrinterInfo> printers, List<PrinterInfo> favoritePrinters) {
        if (!this.mPrintersUpdatedBefore || !this.mPrinters.equals(printers) || !this.mFavoritePrinters.equals(favoritePrinters)) {
            this.mPrintersUpdatedBefore = true;
            this.mPersistenceManager.updatePrintersHistoricalNamesIfNeeded(printers);
            Map<PrinterId, PrinterInfo> printersMap = new LinkedHashMap<>();
            int printerCount = printers.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterInfo printer = printers.get(i);
                printersMap.put(printer.getId(), printer);
            }
            computeAndDeliverResult(printersMap, favoritePrinters);
        }
    }

    @Override
    protected boolean onCancelLoad() {
        return cancelInternal();
    }

    private boolean cancelInternal() {
        if (this.mDiscoverySession != null && this.mDiscoverySession.isPrinterDiscoveryStarted()) {
            if (this.mTrackedPrinter != null) {
                this.mDiscoverySession.stopPrinterStateTracking(this.mTrackedPrinter);
                this.mTrackedPrinter = null;
            }
            this.mDiscoverySession.stopPrinterDiscovery();
            return true;
        }
        if (this.mPersistenceManager.isReadHistoryInProgress()) {
            return this.mPersistenceManager.stopReadPrinterHistory();
        }
        return false;
    }

    @Override
    protected void onReset() {
        onStopLoading();
        this.mPrinters.clear();
        if (this.mDiscoverySession != null) {
            this.mDiscoverySession.destroy();
            this.mDiscoverySession = null;
        }
    }

    @Override
    protected void onAbandon() {
        onStopLoading();
    }

    public boolean areHistoricalPrintersLoaded() {
        return this.mPersistenceManager.mReadHistoryCompleted;
    }

    public void setTrackedPrinter(PrinterId printerId) {
        if (isStarted() && this.mDiscoverySession != null && this.mDiscoverySession.isPrinterDiscoveryStarted()) {
            if (this.mTrackedPrinter != null) {
                if (!this.mTrackedPrinter.equals(printerId)) {
                    this.mDiscoverySession.stopPrinterStateTracking(this.mTrackedPrinter);
                } else {
                    return;
                }
            }
            this.mTrackedPrinter = printerId;
            if (printerId != null) {
                this.mDiscoverySession.startPrinterStateTracking(printerId);
            }
        }
    }

    public boolean isFavoritePrinter(PrinterId printerId) {
        int printerCount = this.mFavoritePrinters.size();
        for (int i = 0; i < printerCount; i++) {
            PrinterInfo favoritePritner = this.mFavoritePrinters.get(i);
            if (favoritePritner.getId().equals(printerId)) {
                return true;
            }
        }
        return false;
    }

    public void forgetFavoritePrinter(PrinterId printerId) {
        List<PrinterInfo> newFavoritePrinters = null;
        int favoritePrinterCount = this.mFavoritePrinters.size();
        int i = 0;
        while (true) {
            if (i >= favoritePrinterCount) {
                break;
            }
            PrinterInfo favoritePrinter = this.mFavoritePrinters.get(i);
            if (!favoritePrinter.getId().equals(printerId)) {
                i++;
            } else {
                newFavoritePrinters = new ArrayList<>();
                newFavoritePrinters.addAll(this.mPrinters);
                newFavoritePrinters.remove(i);
                break;
            }
        }
        if (newFavoritePrinters != null) {
            this.mPersistenceManager.removeHistoricalPrinterAndWritePrinterHistory(printerId);
            updatePrinters(this.mDiscoverySession.getPrinters(), newFavoritePrinters);
        }
    }

    private final class PersistenceManager {
        private List<PrinterInfo> mHistoricalPrinters;
        private volatile long mLastReadHistoryTimestamp;
        private boolean mReadHistoryCompleted;
        private ReadTask mReadTask;
        private final AtomicFile mStatePersistFile;

        private PersistenceManager(Context context) {
            this.mHistoricalPrinters = new ArrayList();
            this.mStatePersistFile = new AtomicFile(new File(context.getFilesDir(), "printer_history.xml"));
        }

        public boolean isReadHistoryInProgress() {
            return this.mReadTask != null;
        }

        public boolean isReadHistoryCompleted() {
            return this.mReadHistoryCompleted;
        }

        public boolean stopReadPrinterHistory() {
            return this.mReadTask.cancel(true);
        }

        public void readPrinterHistory() {
            this.mReadTask = new ReadTask();
            this.mReadTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
        }

        public void updatePrintersHistoricalNamesIfNeeded(List<PrinterInfo> printers) {
            boolean writeHistory = false;
            int printerCount = printers.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterInfo printer = printers.get(i);
                writeHistory |= renamePrinterIfNeeded(printer);
            }
            if (writeHistory) {
                writePrinterHistory();
            }
        }

        public boolean renamePrinterIfNeeded(PrinterInfo printer) {
            boolean renamed = false;
            int printerCount = this.mHistoricalPrinters.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterInfo historicalPrinter = this.mHistoricalPrinters.get(i);
                if (historicalPrinter.getId().equals(printer.getId()) && !TextUtils.equals(historicalPrinter.getName(), printer.getName())) {
                    this.mHistoricalPrinters.set(i, printer);
                    renamed = true;
                }
            }
            return renamed;
        }

        public void addPrinterAndWritePrinterHistory(PrinterInfo printer) {
            if (this.mHistoricalPrinters.size() >= 50) {
                this.mHistoricalPrinters.remove(0);
            }
            this.mHistoricalPrinters.add(printer);
            writePrinterHistory();
        }

        public void removeHistoricalPrinterAndWritePrinterHistory(PrinterId printerId) {
            boolean writeHistory = false;
            int printerCount = this.mHistoricalPrinters.size();
            for (int i = printerCount - 1; i >= 0; i--) {
                PrinterInfo historicalPrinter = this.mHistoricalPrinters.get(i);
                if (historicalPrinter.getId().equals(printerId)) {
                    this.mHistoricalPrinters.remove(i);
                    writeHistory = true;
                }
            }
            if (writeHistory) {
                writePrinterHistory();
            }
        }

        private void writePrinterHistory() {
            new WriteTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new ArrayList(this.mHistoricalPrinters));
        }

        public boolean isHistoryChanged() {
            return this.mLastReadHistoryTimestamp != this.mStatePersistFile.getBaseFile().lastModified();
        }

        private List<PrinterInfo> computeFavoritePrinters(List<PrinterInfo> printers) {
            Map<PrinterId, PrinterRecord> recordMap = new ArrayMap<>();
            float currentWeight = 1.0f;
            int printerCount = printers.size();
            for (int i = printerCount - 1; i >= 0; i--) {
                PrinterInfo printer = printers.get(i);
                PrinterRecord record = recordMap.get(printer.getId());
                if (record == null) {
                    record = new PrinterRecord(printer);
                    recordMap.put(printer.getId(), record);
                }
                record.weight += currentWeight;
                currentWeight = (float) (((double) currentWeight) * 0.949999988079071d);
            }
            List<PrinterRecord> favoriteRecords = new ArrayList<>(recordMap.values());
            Collections.sort(favoriteRecords);
            int favoriteCount = Math.min(favoriteRecords.size(), 4);
            List<PrinterInfo> favoritePrinters = new ArrayList<>(favoriteCount);
            for (int i2 = 0; i2 < favoriteCount; i2++) {
                favoritePrinters.add(favoriteRecords.get(i2).printer);
            }
            return favoritePrinters;
        }

        private final class PrinterRecord implements Comparable<PrinterRecord> {
            public final PrinterInfo printer;
            public float weight;

            public PrinterRecord(PrinterInfo printer) {
                this.printer = printer;
            }

            @Override
            public int compareTo(PrinterRecord another) {
                return Float.floatToIntBits(another.weight) - Float.floatToIntBits(this.weight);
            }
        }

        private final class ReadTask extends AsyncTask<Void, Void, List<PrinterInfo>> {
            private ReadTask() {
            }

            @Override
            protected List<PrinterInfo> doInBackground(Void... args) {
                return doReadPrinterHistory();
            }

            @Override
            protected void onPostExecute(List<PrinterInfo> printers) {
                PrintManager printManager = (PrintManager) FusedPrintersProvider.this.getContext().getSystemService("print");
                List<PrintServiceInfo> services = printManager.getEnabledPrintServices();
                Set<ComponentName> enabledComponents = new ArraySet<>();
                int installedServiceCount = services.size();
                for (int i = 0; i < installedServiceCount; i++) {
                    ServiceInfo serviceInfo = services.get(i).getResolveInfo().serviceInfo;
                    ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                    enabledComponents.add(componentName);
                }
                int printerCount = printers.size();
                for (int i2 = printerCount - 1; i2 >= 0; i2--) {
                    ComponentName printerServiceName = printers.get(i2).getId().getServiceName();
                    if (!enabledComponents.contains(printerServiceName)) {
                        printers.remove(i2);
                    }
                }
                PersistenceManager.this.mHistoricalPrinters = printers;
                FusedPrintersProvider.this.mFavoritePrinters.clear();
                FusedPrintersProvider.this.mFavoritePrinters.addAll(PersistenceManager.this.computeFavoritePrinters(PersistenceManager.this.mHistoricalPrinters));
                PersistenceManager.this.mReadHistoryCompleted = true;
                FusedPrintersProvider.this.updatePrinters(FusedPrintersProvider.this.mDiscoverySession.getPrinters(), FusedPrintersProvider.this.mFavoritePrinters);
                PersistenceManager.this.mReadTask = null;
                FusedPrintersProvider.this.loadInternal();
            }

            @Override
            protected void onCancelled(List<PrinterInfo> printerInfos) {
                PersistenceManager.this.mReadTask = null;
            }

            private List<PrinterInfo> doReadPrinterHistory() {
                FileInputStream in;
                Exception e;
                List<PrinterInfo> printers;
                try {
                    try {
                        in = PersistenceManager.this.mStatePersistFile.openRead();
                        try {
                            printers = new ArrayList<>();
                            XmlPullParser parser = Xml.newPullParser();
                            parser.setInput(in, null);
                            parseState(parser, printers);
                            PersistenceManager.this.mLastReadHistoryTimestamp = PersistenceManager.this.mStatePersistFile.getBaseFile().lastModified();
                        } catch (IOException e2) {
                            e = e2;
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(in);
                            printers = Collections.emptyList();
                        } catch (IllegalStateException e3) {
                            e = e3;
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(in);
                            printers = Collections.emptyList();
                        } catch (IndexOutOfBoundsException e4) {
                            e = e4;
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(in);
                            printers = Collections.emptyList();
                        } catch (NullPointerException e5) {
                            e = e5;
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(in);
                            printers = Collections.emptyList();
                        } catch (NumberFormatException e6) {
                            e = e6;
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(in);
                            printers = Collections.emptyList();
                        } catch (XmlPullParserException e7) {
                            e = e7;
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(in);
                            printers = Collections.emptyList();
                        }
                        return printers;
                    } catch (FileNotFoundException e8) {
                        List<PrinterInfo> printers2 = new ArrayList<>();
                        return printers2;
                    }
                } finally {
                    IoUtils.closeQuietly(in);
                }
            }

            private void parseState(XmlPullParser parser, List<PrinterInfo> outPrinters) throws XmlPullParserException, IOException {
                parser.next();
                skipEmptyTextTags(parser);
                expect(parser, 2, "printers");
                parser.next();
                while (parsePrinter(parser, outPrinters)) {
                    if (!isCancelled()) {
                        parser.next();
                    } else {
                        return;
                    }
                }
                skipEmptyTextTags(parser);
                expect(parser, 3, "printers");
            }

            private boolean parsePrinter(XmlPullParser parser, List<PrinterInfo> outPrinters) throws XmlPullParserException, IOException {
                skipEmptyTextTags(parser);
                if (!accept(parser, 2, "printer")) {
                    return false;
                }
                String name = parser.getAttributeValue(null, "name");
                String description = parser.getAttributeValue(null, "description");
                int status = Integer.parseInt(parser.getAttributeValue(null, "status"));
                parser.next();
                skipEmptyTextTags(parser);
                expect(parser, 2, "printerId");
                String localId = parser.getAttributeValue(null, "localId");
                ComponentName service = ComponentName.unflattenFromString(parser.getAttributeValue(null, "serviceName"));
                PrinterId printerId = new PrinterId(service, localId);
                parser.next();
                skipEmptyTextTags(parser);
                expect(parser, 3, "printerId");
                parser.next();
                PrinterInfo.Builder builder = new PrinterInfo.Builder(printerId, name, status);
                builder.setDescription(description);
                PrinterInfo printer = builder.build();
                outPrinters.add(printer);
                skipEmptyTextTags(parser);
                expect(parser, 3, "printer");
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

        private final class WriteTask extends AsyncTask<List<PrinterInfo>, Void, Void> {
            private WriteTask() {
            }

            @Override
            protected Void doInBackground(List<PrinterInfo>... printers) {
                doWritePrinterHistory(printers[0]);
                return null;
            }

            private void doWritePrinterHistory(List<PrinterInfo> printers) {
                FileOutputStream out = null;
                try {
                    out = PersistenceManager.this.mStatePersistFile.startWrite();
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(out, "utf-8");
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "printers");
                    int printerCount = printers.size();
                    for (int i = 0; i < printerCount; i++) {
                        PrinterInfo printer = printers.get(i);
                        fastXmlSerializer.startTag(null, "printer");
                        fastXmlSerializer.attribute(null, "name", printer.getName());
                        fastXmlSerializer.attribute(null, "status", String.valueOf(3));
                        String description = printer.getDescription();
                        if (description != null) {
                            fastXmlSerializer.attribute(null, "description", description);
                        }
                        PrinterId printerId = printer.getId();
                        fastXmlSerializer.startTag(null, "printerId");
                        fastXmlSerializer.attribute(null, "localId", printerId.getLocalId());
                        fastXmlSerializer.attribute(null, "serviceName", printerId.getServiceName().flattenToString());
                        fastXmlSerializer.endTag(null, "printerId");
                        fastXmlSerializer.endTag(null, "printer");
                    }
                    fastXmlSerializer.endTag(null, "printers");
                    fastXmlSerializer.endDocument();
                    PersistenceManager.this.mStatePersistFile.finishWrite(out);
                } catch (IOException ioe) {
                    Slog.w("FusedPrintersProvider", "Failed to write printer history, restoring backup.", ioe);
                    PersistenceManager.this.mStatePersistFile.failWrite(out);
                } finally {
                    IoUtils.closeQuietly(out);
                }
            }
        }
    }
}
