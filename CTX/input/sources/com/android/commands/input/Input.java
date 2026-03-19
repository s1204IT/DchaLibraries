package com.android.commands.input;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.util.HashMap;
import java.util.Map;

public class Input {
    private static final String INVALID_ARGUMENTS = "Error: Invalid arguments for command: ";
    private static final Map<String, Integer> SOURCES = new HashMap<String, Integer>() {
        {
            put("keyboard", 257);
            put("dpad", 513);
            put("gamepad", 1025);
            put("touchscreen", 4098);
            put("mouse", 8194);
            put("stylus", 16386);
            put("trackball", 65540);
            put("touchpad", 1048584);
            put("touchnavigation", 2097152);
            put("joystick", 16777232);
        }
    };
    private static final String TAG = "Input";

    public static void main(String[] strArr) {
        new Input().run(strArr);
    }

    private void run(String[] strArr) {
        String str;
        int i;
        int i2;
        int i3;
        if (strArr.length < 1) {
            showUsage();
            return;
        }
        int i4 = 0;
        String str2 = strArr[0];
        if (!SOURCES.containsKey(str2)) {
            str = str2;
            i = 0;
        } else {
            int iIntValue = SOURCES.get(str2).intValue();
            str = strArr[1];
            i = iIntValue;
            i4 = 1;
        }
        int length = strArr.length - i4;
        try {
            if (str.equals("text")) {
                if (length == 2) {
                    sendText(getSource(i, 257), strArr[i4 + 1]);
                    return;
                }
            } else if (str.equals("keyevent")) {
                if (length >= 2) {
                    int i5 = i4 + 1;
                    boolean zEquals = "--longpress".equals(strArr[i5]);
                    if (zEquals) {
                        i5 = i4 + 2;
                    }
                    int source = getSource(i, 257);
                    if (strArr.length > i5) {
                        while (i5 < strArr.length) {
                            int iKeyCodeFromString = KeyEvent.keyCodeFromString(strArr[i5]);
                            if (iKeyCodeFromString == 0) {
                                iKeyCodeFromString = KeyEvent.keyCodeFromString("KEYCODE_" + strArr[i5]);
                            }
                            sendKeyEvent(source, iKeyCodeFromString, zEquals);
                            i5++;
                        }
                        return;
                    }
                }
            } else if (str.equals("tap")) {
                if (length == 3) {
                    sendTap(getSource(i, 4098), Float.parseFloat(strArr[i4 + 1]), Float.parseFloat(strArr[i4 + 2]));
                    return;
                }
            } else {
                if (str.equals("swipe")) {
                    int source2 = getSource(i, 4098);
                    switch (length) {
                        case 5:
                            i3 = -1;
                            sendSwipe(source2, Float.parseFloat(strArr[i4 + 1]), Float.parseFloat(strArr[i4 + 2]), Float.parseFloat(strArr[i4 + 3]), Float.parseFloat(strArr[i4 + 4]), i3);
                            break;
                        case 6:
                            i3 = Integer.parseInt(strArr[i4 + 5]);
                            sendSwipe(source2, Float.parseFloat(strArr[i4 + 1]), Float.parseFloat(strArr[i4 + 2]), Float.parseFloat(strArr[i4 + 3]), Float.parseFloat(strArr[i4 + 4]), i3);
                            break;
                    }
                    return;
                }
                if (str.equals("draganddrop")) {
                    int source3 = getSource(i, 4098);
                    switch (length) {
                        case 5:
                            i2 = -1;
                            sendDragAndDrop(source3, Float.parseFloat(strArr[i4 + 1]), Float.parseFloat(strArr[i4 + 2]), Float.parseFloat(strArr[i4 + 3]), Float.parseFloat(strArr[i4 + 4]), i2);
                            break;
                        case 6:
                            i2 = Integer.parseInt(strArr[i4 + 5]);
                            sendDragAndDrop(source3, Float.parseFloat(strArr[i4 + 1]), Float.parseFloat(strArr[i4 + 2]), Float.parseFloat(strArr[i4 + 3]), Float.parseFloat(strArr[i4 + 4]), i2);
                            break;
                    }
                    return;
                }
                if (str.equals("press")) {
                    int source4 = getSource(i, 65540);
                    if (length == 1) {
                        sendTap(source4, 0.0f, 0.0f);
                        return;
                    }
                } else if (str.equals("roll")) {
                    int source5 = getSource(i, 65540);
                    if (length == 3) {
                        sendMove(source5, Float.parseFloat(strArr[i4 + 1]), Float.parseFloat(strArr[i4 + 2]));
                        return;
                    }
                } else {
                    System.err.println("Error: Unknown command: " + str);
                    showUsage();
                    return;
                }
            }
        } catch (NumberFormatException e) {
        }
        System.err.println(INVALID_ARGUMENTS + str);
        showUsage();
    }

    private void sendText(int i, String str) {
        StringBuffer stringBuffer = new StringBuffer(str);
        int i2 = 0;
        boolean z = false;
        while (i2 < stringBuffer.length()) {
            if (z) {
                if (stringBuffer.charAt(i2) == 's') {
                    stringBuffer.setCharAt(i2, ' ');
                    i2--;
                    stringBuffer.deleteCharAt(i2);
                }
                z = false;
            }
            if (stringBuffer.charAt(i2) == '%') {
                z = true;
            }
            i2++;
        }
        for (KeyEvent keyEvent : KeyCharacterMap.load(-1).getEvents(stringBuffer.toString().toCharArray())) {
            if (i != keyEvent.getSource()) {
                keyEvent.setSource(i);
            }
            injectKeyEvent(keyEvent);
        }
    }

    private void sendKeyEvent(int i, int i2, boolean z) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        injectKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i2, 0, 0, -1, 0, 0, i));
        if (z) {
            injectKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i2, 1, 0, -1, 0, 128, i));
        }
        injectKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 1, i2, 0, 0, -1, 0, 0, i));
    }

    private void sendTap(int i, float f, float f2) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        injectMotionEvent(i, 0, jUptimeMillis, f, f2, 1.0f);
        injectMotionEvent(i, 1, jUptimeMillis, f, f2, 0.0f);
    }

    private void sendSwipe(int i, float f, float f2, float f3, float f4, int i2) {
        int i3;
        if (i2 < 0) {
            i3 = 300;
        } else {
            i3 = i2;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        injectMotionEvent(i, 0, jUptimeMillis, f, f2, 1.0f);
        long j = ((long) i3) + jUptimeMillis;
        long jUptimeMillis2 = jUptimeMillis;
        while (jUptimeMillis2 < j) {
            float f5 = (jUptimeMillis2 - jUptimeMillis) / i3;
            injectMotionEvent(i, 2, jUptimeMillis2, lerp(f, f3, f5), lerp(f2, f4, f5), 1.0f);
            jUptimeMillis2 = SystemClock.uptimeMillis();
        }
        injectMotionEvent(i, 1, jUptimeMillis2, f3, f4, 0.0f);
    }

    private void sendDragAndDrop(int i, float f, float f2, float f3, float f4, int i2) {
        int i3;
        if (i2 < 0) {
            i3 = 300;
        } else {
            i3 = i2;
        }
        injectMotionEvent(i, 0, SystemClock.uptimeMillis(), f, f2, 1.0f);
        try {
            Thread.sleep(ViewConfiguration.getLongPressTimeout());
            long jUptimeMillis = SystemClock.uptimeMillis();
            long j = ((long) i3) + jUptimeMillis;
            long jUptimeMillis2 = jUptimeMillis;
            while (jUptimeMillis2 < j) {
                float f5 = (jUptimeMillis2 - jUptimeMillis) / i3;
                injectMotionEvent(i, 2, jUptimeMillis2, lerp(f, f3, f5), lerp(f2, f4, f5), 1.0f);
                jUptimeMillis2 = SystemClock.uptimeMillis();
            }
            injectMotionEvent(i, 1, jUptimeMillis2, f3, f4, 0.0f);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMove(int i, float f, float f2) {
        injectMotionEvent(i, 2, SystemClock.uptimeMillis(), f, f2, 0.0f);
    }

    private void injectKeyEvent(KeyEvent keyEvent) {
        Log.i(TAG, "injectKeyEvent: " + keyEvent);
        InputManager.getInstance().injectInputEvent(keyEvent, 2);
    }

    private int getInputDeviceId(int i) {
        for (int i2 : InputDevice.getDeviceIds()) {
            if (InputDevice.getDevice(i2).supportsSource(i)) {
                return i2;
            }
        }
        return 0;
    }

    private void injectMotionEvent(int i, int i2, long j, float f, float f2, float f3) {
        MotionEvent motionEventObtain = MotionEvent.obtain(j, j, i2, f, f2, f3, 1.0f, 0, 1.0f, 1.0f, getInputDeviceId(i), 0);
        motionEventObtain.setSource(i);
        Log.i(TAG, "injectMotionEvent: " + motionEventObtain);
        InputManager.getInstance().injectInputEvent(motionEventObtain, 2);
    }

    private static final float lerp(float f, float f2, float f3) {
        return ((f2 - f) * f3) + f;
    }

    private static final int getSource(int i, int i2) {
        return i == 0 ? i2 : i;
    }

    private void showUsage() {
        System.err.println("Usage: input [<source>] <command> [<arg>...]");
        System.err.println();
        System.err.println("The sources are: ");
        for (String str : SOURCES.keySet()) {
            System.err.println("      " + str);
        }
        System.err.println();
        System.err.println("The commands and default sources are:");
        System.err.println("      text <string> (Default: touchscreen)");
        System.err.println("      keyevent [--longpress] <key code number or name> ... (Default: keyboard)");
        System.err.println("      tap <x> <y> (Default: touchscreen)");
        System.err.println("      swipe <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
        System.err.println("      draganddrop <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
        System.err.println("      press (Default: trackball)");
        System.err.println("      roll <dx> <dy> (Default: trackball)");
    }
}
