package com.android.commands.input;

import android.hardware.input.InputManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

    public static void main(String[] args) {
        new Input().run(args);
    }

    private void run(String[] args) {
        if (args.length < 1) {
            showUsage();
            return;
        }
        int index = 0;
        String command = args[0];
        int inputSource = 0;
        if (SOURCES.containsKey(command)) {
            inputSource = SOURCES.get(command).intValue();
            index = 0 + 1;
            command = args[index];
        }
        int length = args.length - index;
        try {
            if (command.equals("text")) {
                if (length == 2) {
                    sendText(getSource(inputSource, 257), args[index + 1]);
                    return;
                }
            } else if (command.equals("keyevent")) {
                if (length >= 2) {
                    boolean longpress = "--longpress".equals(args[index + 1]);
                    int start = longpress ? index + 2 : index + 1;
                    int inputSource2 = getSource(inputSource, 257);
                    if (length > start) {
                        for (int i = start; i < length; i++) {
                            int keyCode = KeyEvent.keyCodeFromString(args[i]);
                            if (keyCode == 0) {
                                keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + args[i]);
                            }
                            sendKeyEvent(inputSource2, keyCode, longpress);
                        }
                        return;
                    }
                }
            } else if (command.equals("tap")) {
                if (length == 3) {
                    sendTap(getSource(inputSource, 4098), Float.parseFloat(args[index + 1]), Float.parseFloat(args[index + 2]));
                    return;
                }
            } else {
                if (command.equals("swipe")) {
                    int duration = -1;
                    int inputSource3 = getSource(inputSource, 4098);
                    switch (length) {
                        case 6:
                            duration = Integer.parseInt(args[index + 5]);
                        case 5:
                            sendSwipe(inputSource3, Float.parseFloat(args[index + 1]), Float.parseFloat(args[index + 2]), Float.parseFloat(args[index + 3]), Float.parseFloat(args[index + 4]), duration);
                            break;
                    }
                    return;
                }
                if (command.equals("press")) {
                    int inputSource4 = getSource(inputSource, 65540);
                    if (length == 1) {
                        sendTap(inputSource4, 0.0f, 0.0f);
                        return;
                    }
                } else if (command.equals("roll")) {
                    int inputSource5 = getSource(inputSource, 65540);
                    if (length == 3) {
                        sendMove(inputSource5, Float.parseFloat(args[index + 1]), Float.parseFloat(args[index + 2]));
                        return;
                    }
                } else if (command.equals("rotationevent")) {
                    if (args.length == 2) {
                        sendRotationEvent(args[1]);
                        return;
                    }
                } else {
                    System.err.println("Error: Unknown command: " + command);
                    showUsage();
                    return;
                }
            }
        } catch (NumberFormatException e) {
        }
        System.err.println(INVALID_ARGUMENTS + command);
        showUsage();
    }

    private void sendText(int source, String text) {
        StringBuffer buff = new StringBuffer(text);
        boolean escapeFlag = false;
        int i = 0;
        while (i < buff.length()) {
            if (escapeFlag) {
                escapeFlag = false;
                if (buff.charAt(i) == 's') {
                    buff.setCharAt(i, ' ');
                    i--;
                    buff.deleteCharAt(i);
                }
            }
            if (buff.charAt(i) == '%') {
                escapeFlag = true;
            }
            i++;
        }
        char[] chars = buff.toString().toCharArray();
        KeyCharacterMap kcm = KeyCharacterMap.load(-1);
        KeyEvent[] events = kcm.getEvents(chars);
        for (KeyEvent e : events) {
            if (source != e.getSource()) {
                e.setSource(source);
            }
            injectKeyEvent(e);
        }
    }

    private void sendKeyEvent(int inputSource, int keyCode, boolean longpress) {
        long now = SystemClock.uptimeMillis();
        injectKeyEvent(new KeyEvent(now, now, 0, keyCode, 0, 0, -1, 0, 0, inputSource));
        if (longpress) {
            injectKeyEvent(new KeyEvent(now, now, 0, keyCode, 1, 0, -1, 0, 128, inputSource));
        }
        injectKeyEvent(new KeyEvent(now, now, 1, keyCode, 0, 0, -1, 0, 0, inputSource));
    }

    private void sendTap(int inputSource, float x, float y) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, 0, now, x, y, 1.0f);
        injectMotionEvent(inputSource, 1, now, x, y, 0.0f);
    }

    private void sendRotationEvent(String event) {
        int rotationCode = Integer.parseInt(event);
        if (rotationCode < 0 || rotationCode > 3) {
            System.err.println("Error: invalid rotation code");
            return;
        }
        Log.i("SendRotationEvent", event);
        try {
            IWindowManager.Stub.asInterface(ServiceManager.getService("window")).freezeRotation(rotationCode);
            IWindowManager.Stub.asInterface(ServiceManager.getService("window")).thawRotation();
        } catch (RemoteException e) {
            Log.i(TAG, "DeadOjbectException");
        }
    }

    private void sendSwipe(int inputSource, float x1, float y1, float x2, float y2, int duration) {
        if (duration < 0) {
            duration = 300;
        }
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, 0, now, x1, y1, 1.0f);
        long endTime = now + ((long) duration);
        while (now < endTime) {
            long elapsedTime = now - now;
            float alpha = elapsedTime / duration;
            injectMotionEvent(inputSource, 2, now, lerp(x1, x2, alpha), lerp(y1, y2, alpha), 1.0f);
            now = SystemClock.uptimeMillis();
        }
        injectMotionEvent(inputSource, 1, now, x2, y2, 0.0f);
    }

    private void sendMove(int inputSource, float dx, float dy) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, 2, now, dx, dy, 0.0f);
    }

    private void injectKeyEvent(KeyEvent event) {
        Log.i(TAG, "injectKeyEvent: " + event);
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private void injectMotionEvent(int inputSource, int action, long when, float x, float y, float pressure) {
        MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        event.setSource(inputSource);
        Log.i(TAG, "injectMotionEvent: " + event);
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private static final float lerp(float a, float b, float alpha) {
        return ((b - a) * alpha) + a;
    }

    private static final int getSource(int inputSource, int defaultSource) {
        return inputSource == 0 ? defaultSource : inputSource;
    }

    private void showUsage() {
        System.err.println("Usage: input [<source>] <command> [<arg>...]");
        System.err.println();
        System.err.println("The sources are: ");
        for (String src : SOURCES.keySet()) {
            System.err.println("      " + src);
        }
        System.err.println();
        System.err.println("The commands and default sources are:");
        System.err.println("      text <string> (Default: touchscreen)");
        System.err.println("      keyevent [--longpress] <key code number or name> ... (Default: keyboard)");
        System.err.println("      tap <x> <y> (Default: touchscreen)");
        System.err.println("      swipe <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
        System.err.println("      press (Default: trackball)");
        System.err.println("      roll <dx> <dy> (Default: trackball)");
        System.err.println("       input rotationevent <rotation_code:0->0 1->90 2->180 3->270>");
    }
}
