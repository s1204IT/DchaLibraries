package com.android.server.display;

import android.hardware.display.WifiDisplay;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import libcore.io.IoUtils;
import libcore.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class PersistentDataStore {
    static final String TAG = "DisplayManager";
    private boolean mDirty;
    private boolean mLoaded;
    private ArrayList<WifiDisplay> mRememberedWifiDisplays = new ArrayList<>();
    private final AtomicFile mAtomicFile = new AtomicFile(new File("/data/system/display-manager-state.xml"));

    public void saveIfNeeded() {
        if (this.mDirty) {
            save();
            this.mDirty = false;
        }
    }

    public WifiDisplay getRememberedWifiDisplay(String deviceAddress) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(deviceAddress);
        if (index >= 0) {
            return this.mRememberedWifiDisplays.get(index);
        }
        return null;
    }

    public WifiDisplay[] getRememberedWifiDisplays() {
        loadIfNeeded();
        return (WifiDisplay[]) this.mRememberedWifiDisplays.toArray(new WifiDisplay[this.mRememberedWifiDisplays.size()]);
    }

    public WifiDisplay applyWifiDisplayAlias(WifiDisplay display) {
        if (display != null) {
            loadIfNeeded();
            String alias = null;
            int index = findRememberedWifiDisplay(display.getDeviceAddress());
            if (index >= 0) {
                alias = this.mRememberedWifiDisplays.get(index).getDeviceAlias();
            }
            if (!Objects.equal(display.getDeviceAlias(), alias)) {
                return new WifiDisplay(display.getDeviceAddress(), display.getDeviceName(), alias, display.isAvailable(), display.canConnect(), display.isRemembered());
            }
        }
        return display;
    }

    public WifiDisplay[] applyWifiDisplayAliases(WifiDisplay[] displays) {
        WifiDisplay[] results = displays;
        if (results != null) {
            int count = displays.length;
            for (int i = 0; i < count; i++) {
                WifiDisplay result = applyWifiDisplayAlias(displays[i]);
                if (result != displays[i]) {
                    if (results == displays) {
                        results = new WifiDisplay[count];
                        System.arraycopy(displays, 0, results, 0, count);
                    }
                    results[i] = result;
                }
            }
        }
        return results;
    }

    public boolean rememberWifiDisplay(WifiDisplay display) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(display.getDeviceAddress());
        if (index >= 0) {
            WifiDisplay other = this.mRememberedWifiDisplays.get(index);
            if (other.equals(display)) {
                return false;
            }
            this.mRememberedWifiDisplays.set(index, display);
        } else {
            this.mRememberedWifiDisplays.add(display);
        }
        setDirty();
        return true;
    }

    public boolean forgetWifiDisplay(String deviceAddress) {
        int index = findRememberedWifiDisplay(deviceAddress);
        if (index < 0) {
            return false;
        }
        this.mRememberedWifiDisplays.remove(index);
        setDirty();
        return true;
    }

    private int findRememberedWifiDisplay(String deviceAddress) {
        int count = this.mRememberedWifiDisplays.size();
        for (int i = 0; i < count; i++) {
            if (this.mRememberedWifiDisplays.get(i).getDeviceAddress().equals(deviceAddress)) {
                return i;
            }
        }
        return -1;
    }

    private void loadIfNeeded() {
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
            InputStream is = this.mAtomicFile.openRead();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new BufferedInputStream(is), null);
                loadFromXml(parser);
            } catch (IOException ex) {
                Slog.w(TAG, "Failed to load display manager persistent store data.", ex);
                clearState();
            } catch (XmlPullParserException ex2) {
                Slog.w(TAG, "Failed to load display manager persistent store data.", ex2);
                clearState();
            } finally {
                IoUtils.closeQuietly(is);
            }
        } catch (FileNotFoundException e) {
        }
    }

    private void save() {
        try {
            FileOutputStream os = this.mAtomicFile.startWrite();
            boolean success = false;
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(new BufferedOutputStream(os), "utf-8");
                saveToXml(fastXmlSerializer);
                fastXmlSerializer.flush();
                success = true;
            } finally {
                if (success) {
                    this.mAtomicFile.finishWrite(os);
                } else {
                    this.mAtomicFile.failWrite(os);
                }
            }
        } catch (IOException ex) {
            Slog.w(TAG, "Failed to save display manager persistent store data.", ex);
        }
    }

    private void loadFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(parser, "display-manager-state");
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("remembered-wifi-displays")) {
                loadRememberedWifiDisplaysFromXml(parser);
            }
        }
    }

    private void loadRememberedWifiDisplaysFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("wifi-display")) {
                String deviceAddress = parser.getAttributeValue(null, "deviceAddress");
                String deviceName = parser.getAttributeValue(null, "deviceName");
                String deviceAlias = parser.getAttributeValue(null, "deviceAlias");
                if (deviceAddress == null || deviceName == null) {
                    throw new XmlPullParserException("Missing deviceAddress or deviceName attribute on wifi-display.");
                }
                if (findRememberedWifiDisplay(deviceAddress) >= 0) {
                    throw new XmlPullParserException("Found duplicate wifi display device address.");
                }
                this.mRememberedWifiDisplays.add(new WifiDisplay(deviceAddress, deviceName, deviceAlias, false, false, false));
            }
        }
    }

    private void saveToXml(XmlSerializer serializer) throws IOException {
        serializer.startDocument(null, true);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, "display-manager-state");
        serializer.startTag(null, "remembered-wifi-displays");
        for (WifiDisplay display : this.mRememberedWifiDisplays) {
            serializer.startTag(null, "wifi-display");
            serializer.attribute(null, "deviceAddress", display.getDeviceAddress());
            serializer.attribute(null, "deviceName", display.getDeviceName());
            if (display.getDeviceAlias() != null) {
                serializer.attribute(null, "deviceAlias", display.getDeviceAlias());
            }
            serializer.endTag(null, "wifi-display");
        }
        serializer.endTag(null, "remembered-wifi-displays");
        serializer.endTag(null, "display-manager-state");
        serializer.endDocument();
    }
}
