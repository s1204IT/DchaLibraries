package com.android.server.input;

import android.hardware.input.TouchCalibration;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class PersistentDataStore {
    static final String TAG = "InputManager";
    private boolean mDirty;
    private boolean mLoaded;
    private final HashMap<String, InputDeviceState> mInputDevices = new HashMap<>();
    private final AtomicFile mAtomicFile = new AtomicFile(new File("/data/system/input-manager-state.xml"), "input-state");

    public void saveIfNeeded() {
        if (this.mDirty) {
            save();
            this.mDirty = false;
        }
    }

    public TouchCalibration getTouchCalibration(String str, int i) {
        InputDeviceState inputDeviceState = getInputDeviceState(str, false);
        if (inputDeviceState == null) {
            return TouchCalibration.IDENTITY;
        }
        TouchCalibration touchCalibration = inputDeviceState.getTouchCalibration(i);
        if (touchCalibration == null) {
            return TouchCalibration.IDENTITY;
        }
        return touchCalibration;
    }

    public boolean setTouchCalibration(String str, int i, TouchCalibration touchCalibration) {
        if (getInputDeviceState(str, true).setTouchCalibration(i, touchCalibration)) {
            setDirty();
            return true;
        }
        return false;
    }

    public String getCurrentKeyboardLayout(String str) {
        InputDeviceState inputDeviceState = getInputDeviceState(str, false);
        if (inputDeviceState != null) {
            return inputDeviceState.getCurrentKeyboardLayout();
        }
        return null;
    }

    public boolean setCurrentKeyboardLayout(String str, String str2) {
        if (getInputDeviceState(str, true).setCurrentKeyboardLayout(str2)) {
            setDirty();
            return true;
        }
        return false;
    }

    public String[] getKeyboardLayouts(String str) {
        InputDeviceState inputDeviceState = getInputDeviceState(str, false);
        if (inputDeviceState == null) {
            return (String[]) ArrayUtils.emptyArray(String.class);
        }
        return inputDeviceState.getKeyboardLayouts();
    }

    public boolean addKeyboardLayout(String str, String str2) {
        if (getInputDeviceState(str, true).addKeyboardLayout(str2)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean removeKeyboardLayout(String str, String str2) {
        if (getInputDeviceState(str, true).removeKeyboardLayout(str2)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean switchKeyboardLayout(String str, int i) {
        InputDeviceState inputDeviceState = getInputDeviceState(str, false);
        if (inputDeviceState == null || !inputDeviceState.switchKeyboardLayout(i)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean removeUninstalledKeyboardLayouts(Set<String> set) {
        Iterator<InputDeviceState> it = this.mInputDevices.values().iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (it.next().removeUninstalledKeyboardLayouts(set)) {
                z = true;
            }
        }
        if (!z) {
            return false;
        }
        setDirty();
        return true;
    }

    private InputDeviceState getInputDeviceState(String str, boolean z) {
        loadIfNeeded();
        InputDeviceState inputDeviceState = this.mInputDevices.get(str);
        if (inputDeviceState == null && z) {
            InputDeviceState inputDeviceState2 = new InputDeviceState();
            this.mInputDevices.put(str, inputDeviceState2);
            setDirty();
            return inputDeviceState2;
        }
        return inputDeviceState;
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
        this.mInputDevices.clear();
    }

    private void load() {
        clearState();
        try {
            FileInputStream fileInputStreamOpenRead = this.mAtomicFile.openRead();
            try {
                try {
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(new BufferedInputStream(fileInputStreamOpenRead), StandardCharsets.UTF_8.name());
                        loadFromXml(xmlPullParserNewPullParser);
                    } finally {
                        IoUtils.closeQuietly(fileInputStreamOpenRead);
                    }
                } catch (XmlPullParserException e) {
                    Slog.w(TAG, "Failed to load input manager persistent store data.", e);
                    clearState();
                }
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to load input manager persistent store data.", e2);
                clearState();
            }
        } catch (FileNotFoundException e3) {
        }
    }

    private void save() {
        try {
            FileOutputStream fileOutputStreamStartWrite = this.mAtomicFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(new BufferedOutputStream(fileOutputStreamStartWrite), StandardCharsets.UTF_8.name());
                saveToXml(fastXmlSerializer);
                fastXmlSerializer.flush();
                this.mAtomicFile.finishWrite(fileOutputStreamStartWrite);
            } catch (Throwable th) {
                this.mAtomicFile.failWrite(fileOutputStreamStartWrite);
                throw th;
            }
        } catch (IOException e) {
            Slog.w(TAG, "Failed to save input manager persistent store data.", e);
        }
    }

    private void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(xmlPullParser, "input-manager-state");
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals("input-devices")) {
                loadInputDevicesFromXml(xmlPullParser);
            }
        }
    }

    private void loadInputDevicesFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals("input-device")) {
                String attributeValue = xmlPullParser.getAttributeValue(null, "descriptor");
                if (attributeValue == null) {
                    throw new XmlPullParserException("Missing descriptor attribute on input-device.");
                }
                if (this.mInputDevices.containsKey(attributeValue)) {
                    throw new XmlPullParserException("Found duplicate input device.");
                }
                InputDeviceState inputDeviceState = new InputDeviceState();
                inputDeviceState.loadFromXml(xmlPullParser);
                this.mInputDevices.put(attributeValue, inputDeviceState);
            }
        }
    }

    private void saveToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        xmlSerializer.startTag(null, "input-manager-state");
        xmlSerializer.startTag(null, "input-devices");
        for (Map.Entry<String, InputDeviceState> entry : this.mInputDevices.entrySet()) {
            String key = entry.getKey();
            InputDeviceState value = entry.getValue();
            xmlSerializer.startTag(null, "input-device");
            xmlSerializer.attribute(null, "descriptor", key);
            value.saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, "input-device");
        }
        xmlSerializer.endTag(null, "input-devices");
        xmlSerializer.endTag(null, "input-manager-state");
        xmlSerializer.endDocument();
    }

    private static final class InputDeviceState {
        static final boolean $assertionsDisabled = false;
        private static final String[] CALIBRATION_NAME = {"x_scale", "x_ymix", "x_offset", "y_xmix", "y_scale", "y_offset"};
        private String mCurrentKeyboardLayout;
        private ArrayList<String> mKeyboardLayouts;
        private TouchCalibration[] mTouchCalibration;

        private InputDeviceState() {
            this.mTouchCalibration = new TouchCalibration[4];
            this.mKeyboardLayouts = new ArrayList<>();
        }

        public TouchCalibration getTouchCalibration(int i) {
            try {
                return this.mTouchCalibration[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                Slog.w(PersistentDataStore.TAG, "Cannot get touch calibration.", e);
                return null;
            }
        }

        public boolean setTouchCalibration(int i, TouchCalibration touchCalibration) {
            try {
                if (touchCalibration.equals(this.mTouchCalibration[i])) {
                    return false;
                }
                this.mTouchCalibration[i] = touchCalibration;
                return true;
            } catch (ArrayIndexOutOfBoundsException e) {
                Slog.w(PersistentDataStore.TAG, "Cannot set touch calibration.", e);
                return false;
            }
        }

        public String getCurrentKeyboardLayout() {
            return this.mCurrentKeyboardLayout;
        }

        public boolean setCurrentKeyboardLayout(String str) {
            if (Objects.equals(this.mCurrentKeyboardLayout, str)) {
                return false;
            }
            addKeyboardLayout(str);
            this.mCurrentKeyboardLayout = str;
            return true;
        }

        public String[] getKeyboardLayouts() {
            if (this.mKeyboardLayouts.isEmpty()) {
                return (String[]) ArrayUtils.emptyArray(String.class);
            }
            return (String[]) this.mKeyboardLayouts.toArray(new String[this.mKeyboardLayouts.size()]);
        }

        public boolean addKeyboardLayout(String str) {
            int iBinarySearch = Collections.binarySearch(this.mKeyboardLayouts, str);
            if (iBinarySearch >= 0) {
                return false;
            }
            this.mKeyboardLayouts.add((-iBinarySearch) - 1, str);
            if (this.mCurrentKeyboardLayout == null) {
                this.mCurrentKeyboardLayout = str;
            }
            return true;
        }

        public boolean removeKeyboardLayout(String str) {
            int iBinarySearch = Collections.binarySearch(this.mKeyboardLayouts, str);
            if (iBinarySearch < 0) {
                return false;
            }
            this.mKeyboardLayouts.remove(iBinarySearch);
            updateCurrentKeyboardLayoutIfRemoved(str, iBinarySearch);
            return true;
        }

        private void updateCurrentKeyboardLayoutIfRemoved(String str, int i) {
            if (Objects.equals(this.mCurrentKeyboardLayout, str)) {
                if (!this.mKeyboardLayouts.isEmpty()) {
                    if (i == this.mKeyboardLayouts.size()) {
                        i = 0;
                    }
                    this.mCurrentKeyboardLayout = this.mKeyboardLayouts.get(i);
                    return;
                }
                this.mCurrentKeyboardLayout = null;
            }
        }

        public boolean switchKeyboardLayout(int i) {
            int i2;
            int size = this.mKeyboardLayouts.size();
            if (size < 2) {
                return false;
            }
            int iBinarySearch = Collections.binarySearch(this.mKeyboardLayouts, this.mCurrentKeyboardLayout);
            if (i <= 0) {
                i2 = ((iBinarySearch + size) - 1) % size;
            } else {
                i2 = (iBinarySearch + 1) % size;
            }
            this.mCurrentKeyboardLayout = this.mKeyboardLayouts.get(i2);
            return true;
        }

        public boolean removeUninstalledKeyboardLayouts(Set<String> set) {
            int size = this.mKeyboardLayouts.size();
            boolean z = false;
            while (true) {
                int i = size - 1;
                if (size > 0) {
                    String str = this.mKeyboardLayouts.get(i);
                    if (!set.contains(str)) {
                        Slog.i(PersistentDataStore.TAG, "Removing uninstalled keyboard layout " + str);
                        this.mKeyboardLayouts.remove(i);
                        updateCurrentKeyboardLayoutIfRemoved(str, i);
                        z = true;
                    }
                    size = i;
                } else {
                    return z;
                }
            }
        }

        public void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int iStringToSurfaceRotation;
            int depth = xmlPullParser.getDepth();
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (xmlPullParser.getName().equals("keyboard-layout")) {
                    String attributeValue = xmlPullParser.getAttributeValue(null, "descriptor");
                    if (attributeValue != null) {
                        String attributeValue2 = xmlPullParser.getAttributeValue(null, "current");
                        if (this.mKeyboardLayouts.contains(attributeValue)) {
                            throw new XmlPullParserException("Found duplicate keyboard layout.");
                        }
                        this.mKeyboardLayouts.add(attributeValue);
                        if (attributeValue2 != null && attributeValue2.equals("true")) {
                            if (this.mCurrentKeyboardLayout != null) {
                                throw new XmlPullParserException("Found multiple current keyboard layouts.");
                            }
                            this.mCurrentKeyboardLayout = attributeValue;
                        }
                    } else {
                        throw new XmlPullParserException("Missing descriptor attribute on keyboard-layout.");
                    }
                } else if (xmlPullParser.getName().equals("calibration")) {
                    String attributeValue3 = xmlPullParser.getAttributeValue(null, "format");
                    String attributeValue4 = xmlPullParser.getAttributeValue(null, "rotation");
                    if (attributeValue3 == null) {
                        throw new XmlPullParserException("Missing format attribute on calibration.");
                    }
                    if (!attributeValue3.equals("affine")) {
                        throw new XmlPullParserException("Unsupported format for calibration.");
                    }
                    if (attributeValue4 != null) {
                        try {
                            iStringToSurfaceRotation = stringToSurfaceRotation(attributeValue4);
                        } catch (IllegalArgumentException e) {
                            throw new XmlPullParserException("Unsupported rotation for calibration.");
                        }
                    } else {
                        iStringToSurfaceRotation = -1;
                    }
                    float[] affineTransform = TouchCalibration.IDENTITY.getAffineTransform();
                    int depth2 = xmlPullParser.getDepth();
                    while (XmlUtils.nextElementWithin(xmlPullParser, depth2)) {
                        String lowerCase = xmlPullParser.getName().toLowerCase();
                        String strNextText = xmlPullParser.nextText();
                        int i = 0;
                        while (true) {
                            if (i >= affineTransform.length || i >= CALIBRATION_NAME.length) {
                                break;
                            }
                            if (!lowerCase.equals(CALIBRATION_NAME[i])) {
                                i++;
                            } else {
                                affineTransform[i] = Float.parseFloat(strNextText);
                                break;
                            }
                        }
                    }
                    if (iStringToSurfaceRotation != -1) {
                        this.mTouchCalibration[iStringToSurfaceRotation] = new TouchCalibration(affineTransform[0], affineTransform[1], affineTransform[2], affineTransform[3], affineTransform[4], affineTransform[5]);
                    } else {
                        for (int i2 = 0; i2 < this.mTouchCalibration.length; i2++) {
                            this.mTouchCalibration[i2] = new TouchCalibration(affineTransform[0], affineTransform[1], affineTransform[2], affineTransform[3], affineTransform[4], affineTransform[5]);
                        }
                    }
                } else {
                    continue;
                }
            }
            Collections.sort(this.mKeyboardLayouts);
            if (this.mCurrentKeyboardLayout == null && !this.mKeyboardLayouts.isEmpty()) {
                this.mCurrentKeyboardLayout = this.mKeyboardLayouts.get(0);
            }
        }

        public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
            for (String str : this.mKeyboardLayouts) {
                xmlSerializer.startTag(null, "keyboard-layout");
                xmlSerializer.attribute(null, "descriptor", str);
                if (str.equals(this.mCurrentKeyboardLayout)) {
                    xmlSerializer.attribute(null, "current", "true");
                }
                xmlSerializer.endTag(null, "keyboard-layout");
            }
            for (int i = 0; i < this.mTouchCalibration.length; i++) {
                if (this.mTouchCalibration[i] != null) {
                    String strSurfaceRotationToString = surfaceRotationToString(i);
                    float[] affineTransform = this.mTouchCalibration[i].getAffineTransform();
                    xmlSerializer.startTag(null, "calibration");
                    xmlSerializer.attribute(null, "format", "affine");
                    xmlSerializer.attribute(null, "rotation", strSurfaceRotationToString);
                    for (int i2 = 0; i2 < affineTransform.length && i2 < CALIBRATION_NAME.length; i2++) {
                        xmlSerializer.startTag(null, CALIBRATION_NAME[i2]);
                        xmlSerializer.text(Float.toString(affineTransform[i2]));
                        xmlSerializer.endTag(null, CALIBRATION_NAME[i2]);
                    }
                    xmlSerializer.endTag(null, "calibration");
                }
            }
        }

        private static String surfaceRotationToString(int i) {
            switch (i) {
                case 0:
                    return "0";
                case 1:
                    return "90";
                case 2:
                    return "180";
                case 3:
                    return "270";
                default:
                    throw new IllegalArgumentException("Unsupported surface rotation value" + i);
            }
        }

        private static int stringToSurfaceRotation(String str) {
            if ("0".equals(str)) {
                return 0;
            }
            if ("90".equals(str)) {
                return 1;
            }
            if ("180".equals(str)) {
                return 2;
            }
            if ("270".equals(str)) {
                return 3;
            }
            throw new IllegalArgumentException("Unsupported surface rotation string '" + str + "'");
        }
    }
}
