package com.android.internal.os;

import android.content.Context;
import android.content.res.XmlResourceParser;
import com.android.internal.R;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParserException;

public class PowerProfile {
    private static final String ATTR_NAME = "name";
    public static final String POWER_AUDIO = "dsp.audio";
    public static final String POWER_BATTERY_CAPACITY = "battery.capacity";
    public static final String POWER_BLUETOOTH_ACTIVE = "bluetooth.active";
    public static final String POWER_BLUETOOTH_AT_CMD = "bluetooth.at";
    public static final String POWER_BLUETOOTH_ON = "bluetooth.on";
    public static final String POWER_CPU_ACTIVE = "cpu.active";
    public static final String POWER_CPU_AWAKE = "cpu.awake";
    public static final String POWER_CPU_IDLE = "cpu.idle";
    public static final String POWER_CPU_SPEEDS = "cpu.speeds";
    public static final String POWER_FLASHLIGHT = "camera.flashlight";
    public static final String POWER_GPS_ON = "gps.on";
    public static final String POWER_NONE = "none";
    public static final String POWER_RADIO_ACTIVE = "radio.active";
    public static final String POWER_RADIO_ON = "radio.on";
    public static final String POWER_RADIO_SCANNING = "radio.scanning";
    public static final String POWER_SCREEN_FULL = "screen.full";
    public static final String POWER_SCREEN_ON = "screen.on";
    public static final String POWER_VIDEO = "dsp.video";
    public static final String POWER_WIFI_ACTIVE = "wifi.active";
    public static final String POWER_WIFI_BATCHED_SCAN = "wifi.batchedscan";
    public static final String POWER_WIFI_ON = "wifi.on";
    public static final String POWER_WIFI_SCAN = "wifi.scan";
    private static final String TAG_ARRAY = "array";
    private static final String TAG_ARRAYITEM = "value";
    private static final String TAG_DEVICE = "device";
    private static final String TAG_ITEM = "item";
    static final HashMap<String, Object> sPowerMap = new HashMap<>();

    public PowerProfile(Context context) {
        if (sPowerMap.size() == 0) {
            readPowerValuesFromXml(context);
        }
    }

    private void readPowerValuesFromXml(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.power_profile);
        boolean parsingArray = false;
        ArrayList<Double> array = new ArrayList<>();
        String arrayName = null;
        try {
            try {
                try {
                    XmlUtils.beginDocument(parser, "device");
                    while (true) {
                        XmlUtils.nextElement(parser);
                        String element = parser.getName();
                        if (element == null) {
                            break;
                        }
                        if (parsingArray && !element.equals("value")) {
                            sPowerMap.put(arrayName, array.toArray(new Double[array.size()]));
                            parsingArray = false;
                        }
                        if (element.equals(TAG_ARRAY)) {
                            parsingArray = true;
                            array.clear();
                            arrayName = parser.getAttributeValue(null, "name");
                        } else if (element.equals(TAG_ITEM) || element.equals("value")) {
                            String name = parsingArray ? null : parser.getAttributeValue(null, "name");
                            if (parser.next() == 4) {
                                String power = parser.getText();
                                double value = 0.0d;
                                try {
                                    value = Double.valueOf(power).doubleValue();
                                } catch (NumberFormatException e) {
                                }
                                if (element.equals(TAG_ITEM)) {
                                    sPowerMap.put(name, Double.valueOf(value));
                                } else if (parsingArray) {
                                    array.add(Double.valueOf(value));
                                }
                            }
                        }
                    }
                    if (parsingArray) {
                        sPowerMap.put(arrayName, array.toArray(new Double[array.size()]));
                    }
                } catch (XmlPullParserException e2) {
                    throw new RuntimeException(e2);
                }
            } catch (IOException e3) {
                throw new RuntimeException(e3);
            }
        } finally {
            parser.close();
        }
    }

    public double getAveragePower(String type) {
        if (sPowerMap.containsKey(type)) {
            Object data = sPowerMap.get(type);
            if (data instanceof Double[]) {
                return ((Double[]) data)[0].doubleValue();
            }
            return ((Double) sPowerMap.get(type)).doubleValue();
        }
        return 0.0d;
    }

    public double getAveragePower(String type, int level) {
        if (!sPowerMap.containsKey(type)) {
            return 0.0d;
        }
        Object data = sPowerMap.get(type);
        if (data instanceof Double[]) {
            Double[] values = (Double[]) data;
            if (values.length > level && level >= 0) {
                return values[level].doubleValue();
            }
            if (level >= 0) {
                return values[values.length - 1].doubleValue();
            }
            return 0.0d;
        }
        return ((Double) data).doubleValue();
    }

    public double getBatteryCapacity() {
        return getAveragePower(POWER_BATTERY_CAPACITY);
    }

    public int getNumSpeedSteps() {
        Object value = sPowerMap.get(POWER_CPU_SPEEDS);
        if (value == null || !(value instanceof Double[])) {
            return 1;
        }
        return ((Double[]) value).length;
    }
}
