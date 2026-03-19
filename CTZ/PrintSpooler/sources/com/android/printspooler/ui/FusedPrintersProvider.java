package com.android.printspooler.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Loader;
import android.content.pm.PackageItemInfo;
import android.content.pm.ServiceInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.print.PrinterDiscoverySession;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class FusedPrintersProvider extends Loader<List<PrinterInfo>> implements LocationListener {
    private PrinterDiscoverySession mDiscoverySession;
    private final List<Pair<PrinterInfo, Location>> mFavoritePrinters;
    private Location mLocation;
    private final Object mLocationLock;
    private final LocationManager mLocationManager;
    private Location mLocationOfLastPrinterUpdate;
    private final PersistenceManager mPersistenceManager;
    private final List<PrinterInfo> mPrinters;
    private boolean mPrintersUpdatedBefore;
    private PrinterId mTrackedPrinter;

    private Location getCurrentLocation() {
        Location location;
        synchronized (this.mLocationLock) {
            location = this.mLocation;
        }
        return location;
    }

    public FusedPrintersProvider(Activity activity, int i) {
        super(activity);
        this.mPrinters = new ArrayList();
        this.mFavoritePrinters = new ArrayList();
        this.mLocationLock = new Object();
        this.mPersistenceManager = new PersistenceManager(activity, i);
        this.mLocationManager = (LocationManager) activity.getSystemService("location");
    }

    public void addHistoricalPrinter(PrinterInfo printerInfo) {
        this.mPersistenceManager.addPrinterAndWritePrinterHistory(printerInfo);
    }

    private void updateAndAddPrinter(List<PrinterInfo> list, PrinterInfo printerInfo, Map<PrinterId, PrinterInfo> map) {
        PrinterInfo printerInfoRemove = map.remove(printerInfo.getId());
        if (printerInfoRemove != null) {
            list.add(printerInfoRemove);
        } else {
            list.add(printerInfo);
        }
    }

    private void computeAndDeliverResult(Map<PrinterId, PrinterInfo> map, List<Pair<PrinterInfo, Location>> list) {
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet(4);
        Location currentLocation = getCurrentLocation();
        int size = list.size();
        if (currentLocation != null) {
            for (int i = 0; i < size && arrayList.size() != 4; i++) {
                PrinterInfo printerInfo = (PrinterInfo) list.get(i).first;
                Location location = (Location) list.get(i).second;
                if (location != null && !hashSet.contains(printerInfo.getId()) && location.distanceTo(currentLocation) <= 100.0f) {
                    updateAndAddPrinter(arrayList, printerInfo, map);
                    hashSet.add(printerInfo.getId());
                }
            }
        }
        for (int i2 = 0; i2 < size && arrayList.size() != 4; i2++) {
            PrinterInfo printerInfo2 = (PrinterInfo) list.get(i2).first;
            if (!hashSet.contains(printerInfo2.getId())) {
                updateAndAddPrinter(arrayList, printerInfo2, map);
                hashSet.add(printerInfo2.getId());
            }
        }
        int size2 = this.mPrinters.size();
        for (int i3 = 0; i3 < size2; i3++) {
            PrinterInfo printerInfoRemove = map.remove(this.mPrinters.get(i3).getId());
            if (printerInfoRemove != null) {
                arrayList.add(printerInfoRemove);
            }
        }
        arrayList.addAll(map.values());
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
        this.mLocationManager.requestLocationUpdates(LocationRequest.create().setQuality(201).setInterval(30000L), this, Looper.getMainLooper());
        Location lastLocation = this.mLocationManager.getLastLocation();
        if (lastLocation != null) {
            onLocationChanged(lastLocation);
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(1);
        this.mLocationManager.requestSingleUpdate(criteria, this, Looper.getMainLooper());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                FusedPrintersProvider.this.deliverResult(new ArrayList(FusedPrintersProvider.this.mPrinters));
            }
        });
        onForceLoad();
    }

    @Override
    protected void onStopLoading() {
        onCancelLoad();
        this.mLocationManager.removeUpdates(this);
    }

    @Override
    protected void onForceLoad() {
        loadInternal();
    }

    private void loadInternal() {
        if (this.mDiscoverySession == null) {
            this.mDiscoverySession = ((PrintManager) getContext().getSystemService("print")).createPrinterDiscoverySession();
            this.mPersistenceManager.readPrinterHistory();
        } else if (this.mPersistenceManager.isHistoryChanged()) {
            this.mPersistenceManager.readPrinterHistory();
        }
        if (this.mPersistenceManager.isReadHistoryCompleted() && !this.mDiscoverySession.isPrinterDiscoveryStarted()) {
            this.mDiscoverySession.setOnPrintersChangeListener(new PrinterDiscoverySession.OnPrintersChangeListener() {
                public void onPrintersChanged() {
                    FusedPrintersProvider.this.updatePrinters(FusedPrintersProvider.this.mDiscoverySession.getPrinters(), FusedPrintersProvider.this.mFavoritePrinters, FusedPrintersProvider.this.getCurrentLocation());
                }
            });
            int size = this.mFavoritePrinters.size();
            ArrayList arrayList = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(((PrinterInfo) this.mFavoritePrinters.get(i).first).getId());
            }
            this.mDiscoverySession.startPrinterDiscovery(arrayList);
            updatePrinters(this.mDiscoverySession.getPrinters(), this.mFavoritePrinters, getCurrentLocation());
        }
    }

    private void updatePrinters(List<PrinterInfo> list, List<Pair<PrinterInfo, Location>> list2, Location location) {
        if (this.mPrintersUpdatedBefore && this.mPrinters.equals(list) && this.mFavoritePrinters.equals(list2) && Objects.equals(this.mLocationOfLastPrinterUpdate, location)) {
            return;
        }
        this.mLocationOfLastPrinterUpdate = location;
        this.mPrintersUpdatedBefore = true;
        this.mPersistenceManager.updateHistoricalPrintersIfNeeded(list);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            PrinterInfo printerInfo = list.get(i);
            linkedHashMap.put(printerInfo.getId(), printerInfo);
        }
        computeAndDeliverResult(linkedHashMap, list2);
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
        }
    }

    @Override
    protected void onAbandon() {
        onStopLoading();
    }

    private boolean isLocationAcceptable(Location location) {
        return location != null && location.getElapsedRealtimeNanos() > SystemClock.elapsedRealtimeNanos() - 600000000000L && location.hasAccuracy() && location.getAccuracy() < 50.0f;
    }

    @Override
    public void onLocationChanged(Location location) {
        synchronized (this.mLocationLock) {
            if (isLocationAcceptable(location) && !location.equals(this.mLocation) && (this.mLocation == null || location.getElapsedRealtimeNanos() > this.mLocation.getElapsedRealtimeNanos() + 2.7E10d || !this.mLocation.hasAccuracy() || location.getAccuracy() < this.mLocation.getAccuracy())) {
                this.mLocation = location;
                if (areHistoricalPrintersLoaded()) {
                    updatePrinters(this.mDiscoverySession.getPrinters(), this.mFavoritePrinters, this.mLocation);
                }
            }
        }
    }

    @Override
    public void onStatusChanged(String str, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String str) {
    }

    @Override
    public void onProviderDisabled(String str) {
    }

    public boolean areHistoricalPrintersLoaded() {
        return this.mPersistenceManager.mReadHistoryCompleted;
    }

    public void setTrackedPrinter(PrinterId printerId) {
        if (isStarted() && this.mDiscoverySession != null && this.mDiscoverySession.isPrinterDiscoveryStarted()) {
            if (this.mTrackedPrinter != null) {
                if (this.mTrackedPrinter.equals(printerId)) {
                    return;
                } else {
                    this.mDiscoverySession.stopPrinterStateTracking(this.mTrackedPrinter);
                }
            }
            this.mTrackedPrinter = printerId;
            if (printerId != null) {
                this.mDiscoverySession.startPrinterStateTracking(printerId);
            }
        }
    }

    public boolean isFavoritePrinter(PrinterId printerId) {
        int size = this.mFavoritePrinters.size();
        for (int i = 0; i < size; i++) {
            if (((PrinterInfo) this.mFavoritePrinters.get(i).first).getId().equals(printerId)) {
                return true;
            }
        }
        return false;
    }

    public void forgetFavoritePrinter(PrinterId printerId) {
        int size = this.mFavoritePrinters.size();
        ArrayList arrayList = new ArrayList(size - 1);
        for (int i = 0; i < size; i++) {
            if (!((PrinterInfo) this.mFavoritePrinters.get(i).first).getId().equals(printerId)) {
                arrayList.add(this.mFavoritePrinters.get(i));
            }
        }
        this.mPersistenceManager.removeHistoricalPrinterAndWritePrinterHistory(printerId);
        updatePrinters(this.mDiscoverySession.getPrinters(), arrayList, getCurrentLocation());
    }

    private final class PersistenceManager implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {
        private boolean mAreEnabledServicesUpdated;
        private List<PrintServiceInfo> mEnabledServices;
        private List<Pair<PrinterInfo, Location>> mHistoricalPrinters;
        private volatile long mLastReadHistoryTimestamp;
        private boolean mReadHistoryCompleted;
        private ReadTask mReadTask;
        private final AtomicFile mStatePersistFile;

        private PersistenceManager(final Activity activity, final int i) {
            this.mHistoricalPrinters = new ArrayList();
            this.mStatePersistFile = new AtomicFile(new File(activity.getFilesDir(), "printer_history.xml"), "printer-history");
            this.mEnabledServices = ((PrintManager) activity.getSystemService("print")).getPrintServices(1);
            this.mAreEnabledServicesUpdated = true;
            new Handler(activity.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    activity.getLoaderManager().initLoader(i, null, PersistenceManager.this);
                }
            });
        }

        @Override
        public Loader<List<PrintServiceInfo>> onCreateLoader(int i, Bundle bundle) {
            return new PrintServicesLoader((PrintManager) FusedPrintersProvider.this.getContext().getSystemService("print"), FusedPrintersProvider.this.getContext(), 1);
        }

        @Override
        public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> list) {
            this.mAreEnabledServicesUpdated = true;
            this.mEnabledServices = list;
            if (FusedPrintersProvider.this.isStarted()) {
                FusedPrintersProvider.this.forceLoad();
            }
        }

        @Override
        public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
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
            this.mReadTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null);
        }

        public void updateHistoricalPrintersIfNeeded(List<PrinterInfo> list) {
            int size = list.size();
            boolean zUpdateHistoricalPrinterIfNeeded = false;
            for (int i = 0; i < size; i++) {
                zUpdateHistoricalPrinterIfNeeded |= updateHistoricalPrinterIfNeeded(list.get(i));
            }
            if (zUpdateHistoricalPrinterIfNeeded) {
                writePrinterHistory();
            }
        }

        public boolean updateHistoricalPrinterIfNeeded(PrinterInfo printerInfo) {
            int size = this.mHistoricalPrinters.size();
            boolean z = false;
            for (int i = 0; i < size; i++) {
                PrinterInfo printerInfo2 = (PrinterInfo) this.mHistoricalPrinters.get(i).first;
                if (printerInfo2.getId().equals(printerInfo.getId()) && !printerInfo2.equalsIgnoringStatus(printerInfo)) {
                    this.mHistoricalPrinters.set(i, new Pair<>(printerInfo, (Location) this.mHistoricalPrinters.get(i).second));
                    if (!printerInfo2.getName().equals(printerInfo.getName()) && Objects.equals(printerInfo2.getDescription(), printerInfo.getDescription())) {
                        z = true;
                    }
                }
            }
            return z;
        }

        public void addPrinterAndWritePrinterHistory(PrinterInfo printerInfo) {
            if (this.mHistoricalPrinters.size() >= 50) {
                this.mHistoricalPrinters.remove(0);
            }
            Location currentLocation = FusedPrintersProvider.this.getCurrentLocation();
            if (!FusedPrintersProvider.this.isLocationAcceptable(currentLocation)) {
                currentLocation = null;
            }
            this.mHistoricalPrinters.add(new Pair<>(printerInfo, currentLocation));
            writePrinterHistory();
        }

        public void removeHistoricalPrinterAndWritePrinterHistory(PrinterId printerId) {
            boolean z = false;
            for (int size = this.mHistoricalPrinters.size() - 1; size >= 0; size--) {
                if (((PrinterInfo) this.mHistoricalPrinters.get(size).first).getId().equals(printerId)) {
                    this.mHistoricalPrinters.remove(size);
                    z = true;
                }
            }
            if (z) {
                writePrinterHistory();
            }
        }

        private void writePrinterHistory() {
            new WriteTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new ArrayList(this.mHistoricalPrinters));
        }

        public boolean isHistoryChanged() {
            return this.mAreEnabledServicesUpdated || this.mLastReadHistoryTimestamp != this.mStatePersistFile.getBaseFile().lastModified();
        }

        private List<Pair<PrinterInfo, Location>> sortFavoritePrinters(List<Pair<PrinterInfo, Location>> list) {
            ArrayMap arrayMap = new ArrayMap();
            int size = list.size();
            float f = 1.0f;
            for (int i = size - 1; i >= 0; i--) {
                PrinterId id = ((PrinterInfo) list.get(i).first).getId();
                PrinterRecord printerRecord = (PrinterRecord) arrayMap.get(id);
                if (printerRecord == null) {
                    printerRecord = new PrinterRecord();
                    arrayMap.put(id, printerRecord);
                }
                printerRecord.printers.add(list.get(i));
                printerRecord.weight += f;
                f = (float) (((double) f) * 0.949999988079071d);
            }
            ArrayList arrayList = new ArrayList(arrayMap.values());
            Collections.sort(arrayList);
            int size2 = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i2 = 0; i2 < size2; i2++) {
                arrayList2.addAll(((PrinterRecord) arrayList.get(i2)).printers);
            }
            return arrayList2;
        }

        private final class PrinterRecord implements Comparable<PrinterRecord> {
            public final List<Pair<PrinterInfo, Location>> printers = new ArrayList();
            public float weight;

            public PrinterRecord() {
            }

            @Override
            public int compareTo(PrinterRecord printerRecord) {
                return Float.floatToIntBits(printerRecord.weight) - Float.floatToIntBits(this.weight);
            }
        }

        private final class ReadTask extends AsyncTask<Void, Void, List<Pair<PrinterInfo, Location>>> {
            private ReadTask() {
            }

            @Override
            protected List<Pair<PrinterInfo, Location>> doInBackground(Void... voidArr) {
                return doReadPrinterHistory();
            }

            @Override
            protected void onPostExecute(List<Pair<PrinterInfo, Location>> list) {
                ArraySet arraySet = new ArraySet();
                int size = PersistenceManager.this.mEnabledServices.size();
                for (int i = 0; i < size; i++) {
                    ServiceInfo serviceInfo = ((PrintServiceInfo) PersistenceManager.this.mEnabledServices.get(i)).getResolveInfo().serviceInfo;
                    arraySet.add(new ComponentName(((PackageItemInfo) serviceInfo).packageName, ((PackageItemInfo) serviceInfo).name));
                }
                PersistenceManager.this.mAreEnabledServicesUpdated = false;
                for (int size2 = list.size() - 1; size2 >= 0; size2--) {
                    if (!arraySet.contains(((PrinterInfo) list.get(size2).first).getId().getServiceName())) {
                        list.remove(size2);
                    }
                }
                PersistenceManager.this.mHistoricalPrinters = list;
                FusedPrintersProvider.this.mFavoritePrinters.clear();
                FusedPrintersProvider.this.mFavoritePrinters.addAll(PersistenceManager.this.sortFavoritePrinters(PersistenceManager.this.mHistoricalPrinters));
                PersistenceManager.this.mReadHistoryCompleted = true;
                FusedPrintersProvider.this.updatePrinters(FusedPrintersProvider.this.mDiscoverySession.getPrinters(), FusedPrintersProvider.this.mFavoritePrinters, FusedPrintersProvider.this.getCurrentLocation());
                PersistenceManager.this.mReadTask = null;
                FusedPrintersProvider.this.loadInternal();
            }

            @Override
            protected void onCancelled(List<Pair<PrinterInfo, Location>> list) {
                PersistenceManager.this.mReadTask = null;
            }

            private List<Pair<PrinterInfo, Location>> doReadPrinterHistory() {
                FileInputStream fileInputStreamOpenRead;
                try {
                    try {
                        fileInputStreamOpenRead = PersistenceManager.this.mStatePersistFile.openRead();
                        try {
                            ArrayList arrayList = new ArrayList();
                            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                            xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                            parseState(xmlPullParserNewPullParser, arrayList);
                            PersistenceManager.this.mLastReadHistoryTimestamp = PersistenceManager.this.mStatePersistFile.getBaseFile().lastModified();
                            return arrayList;
                        } catch (IOException | IllegalStateException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
                            Slog.w("FusedPrintersProvider", "Failed parsing ", e);
                            IoUtils.closeQuietly(fileInputStreamOpenRead);
                            return Collections.emptyList();
                        }
                    } catch (FileNotFoundException e2) {
                        return new ArrayList();
                    }
                } finally {
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                }
            }

            private void parseState(XmlPullParser xmlPullParser, List<Pair<PrinterInfo, Location>> list) throws XmlPullParserException, IOException {
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 2, "printers");
                xmlPullParser.next();
                while (parsePrinter(xmlPullParser, list)) {
                    if (isCancelled()) {
                        return;
                    } else {
                        xmlPullParser.next();
                    }
                }
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "printers");
            }

            private boolean parsePrinter(XmlPullParser xmlPullParser, List<Pair<PrinterInfo, Location>> list) throws XmlPullParserException, IOException {
                Location location;
                skipEmptyTextTags(xmlPullParser);
                if (!accept(xmlPullParser, 2, "printer")) {
                    return false;
                }
                String attributeValue = xmlPullParser.getAttributeValue(null, "name");
                String attributeValue2 = xmlPullParser.getAttributeValue(null, "description");
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 2, "printerId");
                PrinterId printerId = new PrinterId(ComponentName.unflattenFromString(xmlPullParser.getAttributeValue(null, "serviceName")), xmlPullParser.getAttributeValue(null, "localId"));
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "printerId");
                xmlPullParser.next();
                skipEmptyTextTags(xmlPullParser);
                if (accept(xmlPullParser, 2, "location")) {
                    location = new Location("");
                    location.setLongitude(Double.parseDouble(xmlPullParser.getAttributeValue(null, "longitude")));
                    location.setLatitude(Double.parseDouble(xmlPullParser.getAttributeValue(null, "latitude")));
                    location.setAccuracy(Float.parseFloat(xmlPullParser.getAttributeValue(null, "accuracy")));
                    xmlPullParser.next();
                    skipEmptyTextTags(xmlPullParser);
                    expect(xmlPullParser, 3, "location");
                    xmlPullParser.next();
                } else {
                    location = null;
                }
                PrinterInfo.Builder builder = new PrinterInfo.Builder(printerId, attributeValue, 3);
                builder.setDescription(attributeValue2);
                list.add(new Pair<>(builder.build(), location));
                skipEmptyTextTags(xmlPullParser);
                expect(xmlPullParser, 3, "printer");
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

        private final class WriteTask extends AsyncTask<List<Pair<PrinterInfo, Location>>, Void, Void> {
            private WriteTask() {
            }

            @Override
            protected Void doInBackground(List<Pair<PrinterInfo, Location>>... listArr) throws Throwable {
                doWritePrinterHistory(listArr[0]);
                return null;
            }

            private void doWritePrinterHistory(List<Pair<PrinterInfo, Location>> list) throws Throwable {
                FileOutputStream fileOutputStreamStartWrite;
                FileOutputStream fileOutputStream = null;
                try {
                    try {
                        fileOutputStreamStartWrite = PersistenceManager.this.mStatePersistFile.startWrite();
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                    fileOutputStreamStartWrite = fileOutputStream;
                }
                try {
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "printers");
                    int size = list.size();
                    for (int i = 0; i < size; i++) {
                        PrinterInfo printerInfo = (PrinterInfo) list.get(i).first;
                        fastXmlSerializer.startTag(null, "printer");
                        fastXmlSerializer.attribute(null, "name", printerInfo.getName());
                        String description = printerInfo.getDescription();
                        if (description != null) {
                            fastXmlSerializer.attribute(null, "description", description);
                        }
                        PrinterId id = printerInfo.getId();
                        fastXmlSerializer.startTag(null, "printerId");
                        fastXmlSerializer.attribute(null, "localId", id.getLocalId());
                        fastXmlSerializer.attribute(null, "serviceName", id.getServiceName().flattenToString());
                        fastXmlSerializer.endTag(null, "printerId");
                        Location location = (Location) list.get(i).second;
                        if (location != null) {
                            fastXmlSerializer.startTag(null, "location");
                            fastXmlSerializer.attribute(null, "longitude", String.valueOf(location.getLongitude()));
                            fastXmlSerializer.attribute(null, "latitude", String.valueOf(location.getLatitude()));
                            fastXmlSerializer.attribute(null, "accuracy", String.valueOf(location.getAccuracy()));
                            fastXmlSerializer.endTag(null, "location");
                        }
                        fastXmlSerializer.endTag(null, "printer");
                    }
                    fastXmlSerializer.endTag(null, "printers");
                    fastXmlSerializer.endDocument();
                    PersistenceManager.this.mStatePersistFile.finishWrite(fileOutputStreamStartWrite);
                    IoUtils.closeQuietly(fileOutputStreamStartWrite);
                } catch (IOException e2) {
                    e = e2;
                    fileOutputStream = fileOutputStreamStartWrite;
                    Slog.w("FusedPrintersProvider", "Failed to write printer history, restoring backup.", e);
                    PersistenceManager.this.mStatePersistFile.failWrite(fileOutputStream);
                    IoUtils.closeQuietly(fileOutputStream);
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fileOutputStreamStartWrite);
                    throw th;
                }
            }
        }
    }
}
