package com.android.server.display;

import android.graphics.Point;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.WifiDisplay;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class PersistentDataStore {
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_DEVICE_ADDRESS = "deviceAddress";
    private static final String ATTR_DEVICE_ALIAS = "deviceAlias";
    private static final String ATTR_DEVICE_NAME = "deviceName";
    private static final String ATTR_LUX = "lux";
    private static final String ATTR_NITS = "nits";
    private static final String ATTR_PACKAGE_NAME = "package-name";
    private static final String ATTR_TIME_STAMP = "timestamp";
    private static final String ATTR_UNIQUE_ID = "unique-id";
    private static final String ATTR_USER_SERIAL = "user-serial";
    static final String TAG = "DisplayManager";
    private static final String TAG_BRIGHTNESS_CONFIGURATION = "brightness-configuration";
    private static final String TAG_BRIGHTNESS_CONFIGURATIONS = "brightness-configurations";
    private static final String TAG_BRIGHTNESS_CURVE = "brightness-curve";
    private static final String TAG_BRIGHTNESS_POINT = "brightness-point";
    private static final String TAG_COLOR_MODE = "color-mode";
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_DISPLAY_MANAGER_STATE = "display-manager-state";
    private static final String TAG_DISPLAY_STATES = "display-states";
    private static final String TAG_REMEMBERED_WIFI_DISPLAYS = "remembered-wifi-displays";
    private static final String TAG_STABLE_DEVICE_VALUES = "stable-device-values";
    private static final String TAG_STABLE_DISPLAY_HEIGHT = "stable-display-height";
    private static final String TAG_STABLE_DISPLAY_WIDTH = "stable-display-width";
    private static final String TAG_WIFI_DISPLAY = "wifi-display";
    private BrightnessConfigurations mBrightnessConfigurations;
    private boolean mDirty;
    private final HashMap<String, DisplayState> mDisplayStates;
    private Injector mInjector;
    private boolean mLoaded;
    private ArrayList<WifiDisplay> mRememberedWifiDisplays;
    private final StableDeviceValues mStableDeviceValues;

    public PersistentDataStore() {
        this(new Injector());
    }

    @VisibleForTesting
    PersistentDataStore(Injector injector) {
        this.mRememberedWifiDisplays = new ArrayList<>();
        this.mDisplayStates = new HashMap<>();
        this.mStableDeviceValues = new StableDeviceValues();
        this.mBrightnessConfigurations = new BrightnessConfigurations();
        this.mInjector = injector;
    }

    public void saveIfNeeded() {
        if (this.mDirty) {
            save();
            this.mDirty = false;
        }
    }

    public WifiDisplay getRememberedWifiDisplay(String str) {
        loadIfNeeded();
        int iFindRememberedWifiDisplay = findRememberedWifiDisplay(str);
        if (iFindRememberedWifiDisplay >= 0) {
            return this.mRememberedWifiDisplays.get(iFindRememberedWifiDisplay);
        }
        return null;
    }

    public WifiDisplay[] getRememberedWifiDisplays() {
        loadIfNeeded();
        return (WifiDisplay[]) this.mRememberedWifiDisplays.toArray(new WifiDisplay[this.mRememberedWifiDisplays.size()]);
    }

    public WifiDisplay applyWifiDisplayAlias(WifiDisplay wifiDisplay) {
        if (wifiDisplay != null) {
            loadIfNeeded();
            String deviceAlias = null;
            int iFindRememberedWifiDisplay = findRememberedWifiDisplay(wifiDisplay.getDeviceAddress());
            if (iFindRememberedWifiDisplay >= 0) {
                deviceAlias = this.mRememberedWifiDisplays.get(iFindRememberedWifiDisplay).getDeviceAlias();
            }
            String str = deviceAlias;
            if (!Objects.equals(wifiDisplay.getDeviceAlias(), str)) {
                return new WifiDisplay(wifiDisplay.getDeviceAddress(), wifiDisplay.getDeviceName(), str, wifiDisplay.isAvailable(), wifiDisplay.canConnect(), wifiDisplay.isRemembered());
            }
        }
        return wifiDisplay;
    }

    public WifiDisplay[] applyWifiDisplayAliases(WifiDisplay[] wifiDisplayArr) {
        if (wifiDisplayArr == null) {
            return wifiDisplayArr;
        }
        int length = wifiDisplayArr.length;
        WifiDisplay[] wifiDisplayArr2 = wifiDisplayArr;
        for (int i = 0; i < length; i++) {
            WifiDisplay wifiDisplayApplyWifiDisplayAlias = applyWifiDisplayAlias(wifiDisplayArr[i]);
            if (wifiDisplayApplyWifiDisplayAlias != wifiDisplayArr[i]) {
                if (wifiDisplayArr2 == wifiDisplayArr) {
                    wifiDisplayArr2 = new WifiDisplay[length];
                    System.arraycopy(wifiDisplayArr, 0, wifiDisplayArr2, 0, length);
                }
                wifiDisplayArr2[i] = wifiDisplayApplyWifiDisplayAlias;
            }
        }
        return wifiDisplayArr2;
    }

    public boolean rememberWifiDisplay(WifiDisplay wifiDisplay) {
        loadIfNeeded();
        int iFindRememberedWifiDisplay = findRememberedWifiDisplay(wifiDisplay.getDeviceAddress());
        if (iFindRememberedWifiDisplay >= 0) {
            if (this.mRememberedWifiDisplays.get(iFindRememberedWifiDisplay).equals(wifiDisplay)) {
                return false;
            }
            this.mRememberedWifiDisplays.set(iFindRememberedWifiDisplay, wifiDisplay);
        } else {
            this.mRememberedWifiDisplays.add(wifiDisplay);
        }
        setDirty();
        return true;
    }

    public boolean forgetWifiDisplay(String str) {
        loadIfNeeded();
        int iFindRememberedWifiDisplay = findRememberedWifiDisplay(str);
        if (iFindRememberedWifiDisplay >= 0) {
            this.mRememberedWifiDisplays.remove(iFindRememberedWifiDisplay);
            setDirty();
            return true;
        }
        return false;
    }

    private int findRememberedWifiDisplay(String str) {
        int size = this.mRememberedWifiDisplays.size();
        for (int i = 0; i < size; i++) {
            if (this.mRememberedWifiDisplays.get(i).getDeviceAddress().equals(str)) {
                return i;
            }
        }
        return -1;
    }

    public int getColorMode(DisplayDevice displayDevice) {
        DisplayState displayState;
        if (displayDevice.hasStableUniqueId() && (displayState = getDisplayState(displayDevice.getUniqueId(), false)) != null) {
            return displayState.getColorMode();
        }
        return -1;
    }

    public boolean setColorMode(DisplayDevice displayDevice, int i) {
        if (!displayDevice.hasStableUniqueId() || !getDisplayState(displayDevice.getUniqueId(), true).setColorMode(i)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Point getStableDisplaySize() {
        loadIfNeeded();
        return this.mStableDeviceValues.getDisplaySize();
    }

    public void setStableDisplaySize(Point point) {
        loadIfNeeded();
        if (this.mStableDeviceValues.setDisplaySize(point)) {
            setDirty();
        }
    }

    public void setBrightnessConfigurationForUser(BrightnessConfiguration brightnessConfiguration, int i, String str) {
        loadIfNeeded();
        if (this.mBrightnessConfigurations.setBrightnessConfigurationForUser(brightnessConfiguration, i, str)) {
            setDirty();
        }
    }

    public BrightnessConfiguration getBrightnessConfiguration(int i) {
        loadIfNeeded();
        return this.mBrightnessConfigurations.getBrightnessConfiguration(i);
    }

    private DisplayState getDisplayState(String str, boolean z) {
        loadIfNeeded();
        DisplayState displayState = this.mDisplayStates.get(str);
        if (displayState == null && z) {
            DisplayState displayState2 = new DisplayState();
            this.mDisplayStates.put(str, displayState2);
            setDirty();
            return displayState2;
        }
        return displayState;
    }

    public void loadIfNeeded() {
        if (!this.mLoaded) {
            load();
            this.mLoaded = true;
        }
    }

    private void setDirty() {
        this.mDirty = true;
    }

    private void clearState() {
        this.mRememberedWifiDisplays.clear();
    }

    private void load() {
        clearState();
        try {
            InputStream inputStreamOpenRead = this.mInjector.openRead();
            try {
                try {
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(new BufferedInputStream(inputStreamOpenRead), StandardCharsets.UTF_8.name());
                        loadFromXml(xmlPullParserNewPullParser);
                    } finally {
                        IoUtils.closeQuietly(inputStreamOpenRead);
                    }
                } catch (XmlPullParserException e) {
                    Slog.w(TAG, "Failed to load display manager persistent store data.", e);
                    clearState();
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to load display manager persistent store data.", e2);
                clearState();
            }
        } catch (FileNotFoundException e3) {
        }
    }

    private void save() {
        try {
            OutputStream outputStreamStartWrite = this.mInjector.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(new BufferedOutputStream(outputStreamStartWrite), StandardCharsets.UTF_8.name());
                saveToXml(fastXmlSerializer);
                fastXmlSerializer.flush();
                this.mInjector.finishWrite(outputStreamStartWrite, true);
            } catch (Throwable th) {
                this.mInjector.finishWrite(outputStreamStartWrite, false);
                throw th;
            }
        } catch (IOException e) {
            Slog.w(TAG, "Failed to save display manager persistent store data.", e);
        }
    }

    private void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(xmlPullParser, TAG_DISPLAY_MANAGER_STATE);
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals(TAG_REMEMBERED_WIFI_DISPLAYS)) {
                loadRememberedWifiDisplaysFromXml(xmlPullParser);
            }
            if (xmlPullParser.getName().equals(TAG_DISPLAY_STATES)) {
                loadDisplaysFromXml(xmlPullParser);
            }
            if (xmlPullParser.getName().equals(TAG_STABLE_DEVICE_VALUES)) {
                this.mStableDeviceValues.loadFromXml(xmlPullParser);
            }
            if (xmlPullParser.getName().equals(TAG_BRIGHTNESS_CONFIGURATIONS)) {
                this.mBrightnessConfigurations.loadFromXml(xmlPullParser);
            }
        }
    }

    private void loadRememberedWifiDisplaysFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals(TAG_WIFI_DISPLAY)) {
                String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_DEVICE_ADDRESS);
                String attributeValue2 = xmlPullParser.getAttributeValue(null, ATTR_DEVICE_NAME);
                String attributeValue3 = xmlPullParser.getAttributeValue(null, ATTR_DEVICE_ALIAS);
                if (attributeValue == null || attributeValue2 == null) {
                    throw new XmlPullParserException("Missing deviceAddress or deviceName attribute on wifi-display.");
                }
                if (findRememberedWifiDisplay(attributeValue) >= 0) {
                    throw new XmlPullParserException("Found duplicate wifi display device address.");
                }
                this.mRememberedWifiDisplays.add(new WifiDisplay(attributeValue, attributeValue2, attributeValue3, false, false, false));
            }
        }
    }

    private void loadDisplaysFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals(TAG_DISPLAY)) {
                String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_UNIQUE_ID);
                if (attributeValue == null) {
                    throw new XmlPullParserException("Missing unique-id attribute on display.");
                }
                if (this.mDisplayStates.containsKey(attributeValue)) {
                    throw new XmlPullParserException("Found duplicate display.");
                }
                DisplayState displayState = new DisplayState();
                displayState.loadFromXml(xmlPullParser);
                this.mDisplayStates.put(attributeValue, displayState);
            }
        }
    }

    private void saveToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        xmlSerializer.startTag(null, TAG_DISPLAY_MANAGER_STATE);
        xmlSerializer.startTag(null, TAG_REMEMBERED_WIFI_DISPLAYS);
        for (WifiDisplay wifiDisplay : this.mRememberedWifiDisplays) {
            xmlSerializer.startTag(null, TAG_WIFI_DISPLAY);
            xmlSerializer.attribute(null, ATTR_DEVICE_ADDRESS, wifiDisplay.getDeviceAddress());
            xmlSerializer.attribute(null, ATTR_DEVICE_NAME, wifiDisplay.getDeviceName());
            if (wifiDisplay.getDeviceAlias() != null) {
                xmlSerializer.attribute(null, ATTR_DEVICE_ALIAS, wifiDisplay.getDeviceAlias());
            }
            xmlSerializer.endTag(null, TAG_WIFI_DISPLAY);
        }
        xmlSerializer.endTag(null, TAG_REMEMBERED_WIFI_DISPLAYS);
        xmlSerializer.startTag(null, TAG_DISPLAY_STATES);
        for (Map.Entry<String, DisplayState> entry : this.mDisplayStates.entrySet()) {
            String key = entry.getKey();
            DisplayState value = entry.getValue();
            xmlSerializer.startTag(null, TAG_DISPLAY);
            xmlSerializer.attribute(null, ATTR_UNIQUE_ID, key);
            value.saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, TAG_DISPLAY);
        }
        xmlSerializer.endTag(null, TAG_DISPLAY_STATES);
        xmlSerializer.startTag(null, TAG_STABLE_DEVICE_VALUES);
        this.mStableDeviceValues.saveToXml(xmlSerializer);
        xmlSerializer.endTag(null, TAG_STABLE_DEVICE_VALUES);
        xmlSerializer.startTag(null, TAG_BRIGHTNESS_CONFIGURATIONS);
        this.mBrightnessConfigurations.saveToXml(xmlSerializer);
        xmlSerializer.endTag(null, TAG_BRIGHTNESS_CONFIGURATIONS);
        xmlSerializer.endTag(null, TAG_DISPLAY_MANAGER_STATE);
        xmlSerializer.endDocument();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("PersistentDataStore");
        printWriter.println("  mLoaded=" + this.mLoaded);
        printWriter.println("  mDirty=" + this.mDirty);
        printWriter.println("  RememberedWifiDisplays:");
        Iterator<WifiDisplay> it = this.mRememberedWifiDisplays.iterator();
        int i = 0;
        int i2 = 0;
        while (it.hasNext()) {
            printWriter.println("    " + i2 + ": " + it.next());
            i2++;
        }
        printWriter.println("  DisplayStates:");
        for (Map.Entry<String, DisplayState> entry : this.mDisplayStates.entrySet()) {
            printWriter.println("    " + i + ": " + entry.getKey());
            entry.getValue().dump(printWriter, "      ");
            i++;
        }
        printWriter.println("  StableDeviceValues:");
        this.mStableDeviceValues.dump(printWriter, "      ");
        printWriter.println("  BrightnessConfigurations:");
        this.mBrightnessConfigurations.dump(printWriter, "      ");
    }

    private static final class DisplayState {
        private int mColorMode;

        private DisplayState() {
        }

        public boolean setColorMode(int i) {
            if (i == this.mColorMode) {
                return false;
            }
            this.mColorMode = i;
            return true;
        }

        public int getColorMode() {
            return this.mColorMode;
        }

        public void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (xmlPullParser.getName().equals(PersistentDataStore.TAG_COLOR_MODE)) {
                    this.mColorMode = Integer.parseInt(xmlPullParser.nextText());
                }
            }
        }

        public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
            xmlSerializer.startTag(null, PersistentDataStore.TAG_COLOR_MODE);
            xmlSerializer.text(Integer.toString(this.mColorMode));
            xmlSerializer.endTag(null, PersistentDataStore.TAG_COLOR_MODE);
        }

        public void dump(PrintWriter printWriter, String str) {
            printWriter.println(str + "ColorMode=" + this.mColorMode);
        }
    }

    private static final class StableDeviceValues {
        private int mHeight;
        private int mWidth;

        private StableDeviceValues() {
        }

        private Point getDisplaySize() {
            return new Point(this.mWidth, this.mHeight);
        }

        public boolean setDisplaySize(Point point) {
            if (this.mWidth != point.x || this.mHeight != point.y) {
                this.mWidth = point.x;
                this.mHeight = point.y;
                return true;
            }
            return false;
        }

        public void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                String name = xmlPullParser.getName();
                byte b = -1;
                int iHashCode = name.hashCode();
                if (iHashCode != -1635792540) {
                    if (iHashCode == 1069578729 && name.equals(PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH)) {
                        b = 0;
                    }
                } else if (name.equals(PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT)) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        this.mWidth = loadIntValue(xmlPullParser);
                        break;
                    case 1:
                        this.mHeight = loadIntValue(xmlPullParser);
                        break;
                }
            }
        }

        private static int loadIntValue(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            try {
                return Integer.parseInt(xmlPullParser.nextText());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
            if (this.mWidth > 0 && this.mHeight > 0) {
                xmlSerializer.startTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH);
                xmlSerializer.text(Integer.toString(this.mWidth));
                xmlSerializer.endTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH);
                xmlSerializer.startTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT);
                xmlSerializer.text(Integer.toString(this.mHeight));
                xmlSerializer.endTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT);
            }
        }

        public void dump(PrintWriter printWriter, String str) {
            printWriter.println(str + "StableDisplayWidth=" + this.mWidth);
            printWriter.println(str + "StableDisplayHeight=" + this.mHeight);
        }
    }

    private static final class BrightnessConfigurations {
        private SparseArray<BrightnessConfiguration> mConfigurations = new SparseArray<>();
        private SparseLongArray mTimeStamps = new SparseLongArray();
        private SparseArray<String> mPackageNames = new SparseArray<>();

        private boolean setBrightnessConfigurationForUser(BrightnessConfiguration brightnessConfiguration, int i, String str) {
            BrightnessConfiguration brightnessConfiguration2 = this.mConfigurations.get(i);
            if (brightnessConfiguration2 == brightnessConfiguration) {
                return false;
            }
            if (brightnessConfiguration2 == null || !brightnessConfiguration2.equals(brightnessConfiguration)) {
                if (brightnessConfiguration != null) {
                    if (str == null) {
                        this.mPackageNames.remove(i);
                    } else {
                        this.mPackageNames.put(i, str);
                    }
                    this.mTimeStamps.put(i, System.currentTimeMillis());
                    this.mConfigurations.put(i, brightnessConfiguration);
                    return true;
                }
                this.mPackageNames.remove(i);
                this.mTimeStamps.delete(i);
                this.mConfigurations.remove(i);
                return true;
            }
            return false;
        }

        public BrightnessConfiguration getBrightnessConfiguration(int i) {
            return this.mConfigurations.get(i);
        }

        public void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int i;
            long j;
            int depth = xmlPullParser.getDepth();
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION.equals(xmlPullParser.getName())) {
                    try {
                        i = Integer.parseInt(xmlPullParser.getAttributeValue(null, PersistentDataStore.ATTR_USER_SERIAL));
                    } catch (NumberFormatException e) {
                        Slog.e(PersistentDataStore.TAG, "Failed to read in brightness configuration", e);
                        i = -1;
                    }
                    String attributeValue = xmlPullParser.getAttributeValue(null, PersistentDataStore.ATTR_PACKAGE_NAME);
                    String attributeValue2 = xmlPullParser.getAttributeValue(null, "timestamp");
                    if (attributeValue2 != null) {
                        try {
                            j = Long.parseLong(attributeValue2);
                        } catch (NumberFormatException e2) {
                            j = -1;
                        }
                        try {
                            BrightnessConfiguration brightnessConfigurationLoadConfigurationFromXml = loadConfigurationFromXml(xmlPullParser);
                            if (i < 0 && brightnessConfigurationLoadConfigurationFromXml != null) {
                                this.mConfigurations.put(i, brightnessConfigurationLoadConfigurationFromXml);
                                if (j != -1) {
                                    this.mTimeStamps.put(i, j);
                                }
                                if (attributeValue != null) {
                                    this.mPackageNames.put(i, attributeValue);
                                }
                            }
                        } catch (IllegalArgumentException e3) {
                            Slog.e(PersistentDataStore.TAG, "Failed to load brightness configuration!", e3);
                        }
                    } else {
                        j = -1;
                        BrightnessConfiguration brightnessConfigurationLoadConfigurationFromXml2 = loadConfigurationFromXml(xmlPullParser);
                        if (i < 0) {
                        }
                    }
                }
            }
        }

        private static BrightnessConfiguration loadConfigurationFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            Pair<float[], float[]> pairLoadCurveFromXml = null;
            String attributeValue = null;
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_CURVE.equals(xmlPullParser.getName())) {
                    attributeValue = xmlPullParser.getAttributeValue(null, PersistentDataStore.ATTR_DESCRIPTION);
                    pairLoadCurveFromXml = loadCurveFromXml(xmlPullParser);
                }
            }
            if (pairLoadCurveFromXml == null) {
                return null;
            }
            BrightnessConfiguration.Builder builder = new BrightnessConfiguration.Builder((float[]) pairLoadCurveFromXml.first, (float[]) pairLoadCurveFromXml.second);
            builder.setDescription(attributeValue);
            return builder.build();
        }

        private static Pair<float[], float[]> loadCurveFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_POINT.equals(xmlPullParser.getName())) {
                    arrayList.add(Float.valueOf(loadFloat(xmlPullParser.getAttributeValue(null, PersistentDataStore.ATTR_LUX))));
                    arrayList2.add(Float.valueOf(loadFloat(xmlPullParser.getAttributeValue(null, PersistentDataStore.ATTR_NITS))));
                }
            }
            int size = arrayList.size();
            float[] fArr = new float[size];
            float[] fArr2 = new float[size];
            for (int i = 0; i < size; i++) {
                fArr[i] = ((Float) arrayList.get(i)).floatValue();
                fArr2[i] = ((Float) arrayList2.get(i)).floatValue();
            }
            return Pair.create(fArr, fArr2);
        }

        private static float loadFloat(String str) {
            try {
                return Float.parseFloat(str);
            } catch (NullPointerException | NumberFormatException e) {
                Slog.e(PersistentDataStore.TAG, "Failed to parse float loading brightness config", e);
                return Float.NEGATIVE_INFINITY;
            }
        }

        public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
            for (int i = 0; i < this.mConfigurations.size(); i++) {
                int iKeyAt = this.mConfigurations.keyAt(i);
                BrightnessConfiguration brightnessConfigurationValueAt = this.mConfigurations.valueAt(i);
                xmlSerializer.startTag(null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION);
                xmlSerializer.attribute(null, PersistentDataStore.ATTR_USER_SERIAL, Integer.toString(iKeyAt));
                String str = this.mPackageNames.get(iKeyAt);
                if (str != null) {
                    xmlSerializer.attribute(null, PersistentDataStore.ATTR_PACKAGE_NAME, str);
                }
                long j = this.mTimeStamps.get(iKeyAt, -1L);
                if (j != -1) {
                    xmlSerializer.attribute(null, "timestamp", Long.toString(j));
                }
                saveConfigurationToXml(xmlSerializer, brightnessConfigurationValueAt);
                xmlSerializer.endTag(null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION);
            }
        }

        private static void saveConfigurationToXml(XmlSerializer xmlSerializer, BrightnessConfiguration brightnessConfiguration) throws IOException {
            xmlSerializer.startTag(null, PersistentDataStore.TAG_BRIGHTNESS_CURVE);
            if (brightnessConfiguration.getDescription() != null) {
                xmlSerializer.attribute(null, PersistentDataStore.ATTR_DESCRIPTION, brightnessConfiguration.getDescription());
            }
            Pair curve = brightnessConfiguration.getCurve();
            for (int i = 0; i < ((float[]) curve.first).length; i++) {
                xmlSerializer.startTag(null, PersistentDataStore.TAG_BRIGHTNESS_POINT);
                xmlSerializer.attribute(null, PersistentDataStore.ATTR_LUX, Float.toString(((float[]) curve.first)[i]));
                xmlSerializer.attribute(null, PersistentDataStore.ATTR_NITS, Float.toString(((float[]) curve.second)[i]));
                xmlSerializer.endTag(null, PersistentDataStore.TAG_BRIGHTNESS_POINT);
            }
            xmlSerializer.endTag(null, PersistentDataStore.TAG_BRIGHTNESS_CURVE);
        }

        public void dump(PrintWriter printWriter, String str) {
            for (int i = 0; i < this.mConfigurations.size(); i++) {
                int iKeyAt = this.mConfigurations.keyAt(i);
                long j = this.mTimeStamps.get(iKeyAt, -1L);
                String str2 = this.mPackageNames.get(iKeyAt);
                printWriter.println(str + "User " + iKeyAt + ":");
                if (j != -1) {
                    printWriter.println(str + "  set at: " + TimeUtils.formatForLogging(j));
                }
                if (str2 != null) {
                    printWriter.println(str + "  set by: " + str2);
                }
                printWriter.println(str + "  " + this.mConfigurations.valueAt(i));
            }
        }
    }

    @VisibleForTesting
    static class Injector {
        private final AtomicFile mAtomicFile = new AtomicFile(new File("/data/system/display-manager-state.xml"), "display-state");

        public InputStream openRead() throws FileNotFoundException {
            return this.mAtomicFile.openRead();
        }

        public OutputStream startWrite() throws IOException {
            return this.mAtomicFile.startWrite();
        }

        public void finishWrite(OutputStream outputStream, boolean z) {
            if (!(outputStream instanceof FileOutputStream)) {
                throw new IllegalArgumentException("Unexpected OutputStream as argument: " + outputStream);
            }
            FileOutputStream fileOutputStream = (FileOutputStream) outputStream;
            if (z) {
                this.mAtomicFile.finishWrite(fileOutputStream);
            } else {
                this.mAtomicFile.failWrite(fileOutputStream);
            }
        }
    }
}
