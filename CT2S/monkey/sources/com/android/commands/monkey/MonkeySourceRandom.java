package com.android.commands.monkey;

import android.content.ComponentName;
import android.graphics.PointF;
import android.hardware.display.DisplayManagerGlobal;
import android.os.SystemClock;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

public class MonkeySourceRandom implements MonkeyEventSource {
    public static final int FACTORZ_COUNT = 11;
    public static final int FACTOR_ANYTHING = 10;
    public static final int FACTOR_APPSWITCH = 8;
    public static final int FACTOR_FLIP = 9;
    public static final int FACTOR_MAJORNAV = 6;
    public static final int FACTOR_MOTION = 1;
    public static final int FACTOR_NAV = 5;
    public static final int FACTOR_PINCHZOOM = 2;
    public static final int FACTOR_ROTATION = 4;
    public static final int FACTOR_SYSOPS = 7;
    public static final int FACTOR_TOUCH = 0;
    public static final int FACTOR_TRACKBALL = 3;
    private static final int GESTURE_DRAG = 1;
    private static final int GESTURE_PINCH_OR_ZOOM = 2;
    private static final int GESTURE_TAP = 0;
    private static final int[] SCREEN_ROTATION_DEGREES;
    private ArrayList<ComponentName> mMainApps;
    private MonkeyEventQueue mQ;
    private Random mRandom;
    private static final int[] NAV_KEYS = {19, 20, 21, 22};
    private static final int[] MAJOR_NAV_KEYS = {82, 23};
    private static final int[] SYS_KEYS = {3, 4, 5, 6, 24, 25, 164, 91};
    private static final boolean[] PHYSICAL_KEY_EXISTS = new boolean[KeyEvent.getMaxKeyCode() + 1];
    private float[] mFactors = new float[11];
    private int mEventCount = 0;
    private int mVerbose = 0;
    private long mThrottle = 0;
    private boolean mKeyboardOpen = false;

    static {
        for (int i = 0; i < PHYSICAL_KEY_EXISTS.length; i++) {
            PHYSICAL_KEY_EXISTS[i] = true;
        }
        for (int i2 = 0; i2 < SYS_KEYS.length; i2++) {
            PHYSICAL_KEY_EXISTS[SYS_KEYS[i2]] = KeyCharacterMap.deviceHasKey(SYS_KEYS[i2]);
        }
        SCREEN_ROTATION_DEGREES = new int[]{0, 1, 2, 3};
    }

    public static String getKeyName(int keycode) {
        return KeyEvent.keyCodeToString(keycode);
    }

    public static int getKeyCode(String keyName) {
        return KeyEvent.keyCodeFromString(keyName);
    }

    public MonkeySourceRandom(Random random, ArrayList<ComponentName> MainApps, long throttle, boolean randomizeThrottle) {
        this.mFactors[0] = 15.0f;
        this.mFactors[1] = 10.0f;
        this.mFactors[3] = 15.0f;
        this.mFactors[4] = 0.0f;
        this.mFactors[5] = 25.0f;
        this.mFactors[6] = 15.0f;
        this.mFactors[7] = 2.0f;
        this.mFactors[8] = 2.0f;
        this.mFactors[9] = 1.0f;
        this.mFactors[10] = 13.0f;
        this.mFactors[2] = 2.0f;
        this.mRandom = random;
        this.mMainApps = MainApps;
        this.mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
    }

    private boolean adjustEventFactors() {
        float userSum = 0.0f;
        float defaultSum = 0.0f;
        int defaultCount = 0;
        for (int i = 0; i < 11; i++) {
            if (this.mFactors[i] <= 0.0f) {
                userSum -= this.mFactors[i];
            } else {
                defaultSum += this.mFactors[i];
                defaultCount++;
            }
        }
        if (userSum > 100.0f) {
            System.err.println("** Event weights > 100%");
            return false;
        }
        if (defaultCount == 0 && (userSum < 99.9f || userSum > 100.1f)) {
            System.err.println("** Event weights != 100%");
            return false;
        }
        float defaultsTarget = 100.0f - userSum;
        float defaultsAdjustment = defaultsTarget / defaultSum;
        for (int i2 = 0; i2 < 11; i2++) {
            if (this.mFactors[i2] <= 0.0f) {
                this.mFactors[i2] = -this.mFactors[i2];
            } else {
                float[] fArr = this.mFactors;
                fArr[i2] = fArr[i2] * defaultsAdjustment;
            }
        }
        if (this.mVerbose > 0) {
            System.out.println("// Event percentages:");
            for (int i3 = 0; i3 < 11; i3++) {
                System.out.println("//   " + i3 + ": " + this.mFactors[i3] + "%");
            }
        }
        if (!validateKeys()) {
            return false;
        }
        float sum = 0.0f;
        for (int i4 = 0; i4 < 11; i4++) {
            sum += this.mFactors[i4] / 100.0f;
            this.mFactors[i4] = sum;
        }
        return true;
    }

    private static boolean validateKeyCategory(String catName, int[] keys, float factor) {
        if (factor < 0.1f) {
            return true;
        }
        for (int i : keys) {
            if (PHYSICAL_KEY_EXISTS[i]) {
                return true;
            }
        }
        System.err.println("** " + catName + " has no physical keys but with factor " + factor + "%.");
        return false;
    }

    private boolean validateKeys() {
        return validateKeyCategory("NAV_KEYS", NAV_KEYS, this.mFactors[5]) && validateKeyCategory("MAJOR_NAV_KEYS", MAJOR_NAV_KEYS, this.mFactors[6]) && validateKeyCategory("SYS_KEYS", SYS_KEYS, this.mFactors[7]);
    }

    public void setFactors(float[] factors) {
        int c = 11;
        if (factors.length < 11) {
            c = factors.length;
        }
        for (int i = 0; i < c; i++) {
            this.mFactors[i] = factors[i];
        }
    }

    public void setFactors(int index, float v) {
        this.mFactors[index] = v;
    }

    private void generatePointerEvent(Random random, int gesture) {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        PointF p1 = randomPoint(random, display);
        PointF v1 = randomVector(random);
        long downAt = SystemClock.uptimeMillis();
        this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(0).setDownTime(downAt).addPointer(0, p1.x, p1.y).setIntermediateNote(false));
        if (gesture == 1) {
            int count = random.nextInt(10);
            for (int i = 0; i < count; i++) {
                randomWalk(random, display, p1, v1);
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(downAt).addPointer(0, p1.x, p1.y).setIntermediateNote(true));
            }
        } else if (gesture == 2) {
            PointF p2 = randomPoint(random, display);
            PointF v2 = randomVector(random);
            randomWalk(random, display, p1, v1);
            this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(261).setDownTime(downAt).addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y).setIntermediateNote(true));
            int count2 = random.nextInt(10);
            for (int i2 = 0; i2 < count2; i2++) {
                randomWalk(random, display, p1, v1);
                randomWalk(random, display, p2, v2);
                this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(2).setDownTime(downAt).addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y).setIntermediateNote(true));
            }
            randomWalk(random, display, p1, v1);
            randomWalk(random, display, p2, v2);
            this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(262).setDownTime(downAt).addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y).setIntermediateNote(true));
        }
        randomWalk(random, display, p1, v1);
        this.mQ.addLast((MonkeyEvent) new MonkeyTouchEvent(1).setDownTime(downAt).addPointer(0, p1.x, p1.y).setIntermediateNote(false));
    }

    private PointF randomPoint(Random random, Display display) {
        return new PointF(random.nextInt(display.getWidth()), random.nextInt(display.getHeight()));
    }

    private PointF randomVector(Random random) {
        return new PointF((random.nextFloat() - 0.5f) * 50.0f, (random.nextFloat() - 0.5f) * 50.0f);
    }

    private void randomWalk(Random random, Display display, PointF point, PointF vector) {
        point.x = Math.max(Math.min(point.x + (random.nextFloat() * vector.x), display.getWidth()), 0.0f);
        point.y = Math.max(Math.min(point.y + (random.nextFloat() * vector.y), display.getHeight()), 0.0f);
    }

    private void generateTrackballEvent(Random random) {
        int i = 0;
        while (i < 10) {
            int dX = random.nextInt(10) - 5;
            int dY = random.nextInt(10) - 5;
            this.mQ.addLast((MonkeyEvent) new MonkeyTrackballEvent(2).addPointer(0, dX, dY).setIntermediateNote(i > 0));
            i++;
        }
        if (random.nextInt(10) == 0) {
            long downAt = SystemClock.uptimeMillis();
            this.mQ.addLast((MonkeyEvent) new MonkeyTrackballEvent(0).setDownTime(downAt).addPointer(0, 0.0f, 0.0f).setIntermediateNote(true));
            this.mQ.addLast((MonkeyEvent) new MonkeyTrackballEvent(1).setDownTime(downAt).addPointer(0, 0.0f, 0.0f).setIntermediateNote(false));
        }
    }

    private void generateRotationEvent(Random random) {
        this.mQ.addLast((MonkeyEvent) new MonkeyRotationEvent(SCREEN_ROTATION_DEGREES[random.nextInt(SCREEN_ROTATION_DEGREES.length)], random.nextBoolean()));
    }

    private void generateEvents() {
        int lastKey;
        float cls = this.mRandom.nextFloat();
        if (cls < this.mFactors[0]) {
            generatePointerEvent(this.mRandom, 0);
            return;
        }
        if (cls < this.mFactors[1]) {
            generatePointerEvent(this.mRandom, 1);
            return;
        }
        if (cls < this.mFactors[2]) {
            generatePointerEvent(this.mRandom, 2);
            return;
        }
        if (cls < this.mFactors[3]) {
            generateTrackballEvent(this.mRandom);
            return;
        }
        if (cls < this.mFactors[4]) {
            generateRotationEvent(this.mRandom);
            return;
        }
        while (true) {
            if (cls < this.mFactors[5]) {
                lastKey = NAV_KEYS[this.mRandom.nextInt(NAV_KEYS.length)];
            } else if (cls < this.mFactors[6]) {
                lastKey = MAJOR_NAV_KEYS[this.mRandom.nextInt(MAJOR_NAV_KEYS.length)];
            } else if (cls < this.mFactors[7]) {
                lastKey = SYS_KEYS[this.mRandom.nextInt(SYS_KEYS.length)];
            } else if (cls < this.mFactors[8]) {
                MonkeyActivityEvent e = new MonkeyActivityEvent(this.mMainApps.get(this.mRandom.nextInt(this.mMainApps.size())));
                this.mQ.addLast((MonkeyEvent) e);
                return;
            } else {
                if (cls < this.mFactors[9]) {
                    MonkeyFlipEvent e2 = new MonkeyFlipEvent(this.mKeyboardOpen);
                    this.mKeyboardOpen = this.mKeyboardOpen ? false : true;
                    this.mQ.addLast((MonkeyEvent) e2);
                    return;
                }
                lastKey = this.mRandom.nextInt(KeyEvent.getMaxKeyCode() - 1) + 1;
            }
            if (lastKey != 26 && lastKey != 6 && lastKey != 223 && PHYSICAL_KEY_EXISTS[lastKey]) {
                MonkeyKeyEvent e3 = new MonkeyKeyEvent(0, lastKey);
                this.mQ.addLast((MonkeyEvent) e3);
                MonkeyKeyEvent e4 = new MonkeyKeyEvent(1, lastKey);
                this.mQ.addLast((MonkeyEvent) e4);
                return;
            }
        }
    }

    @Override
    public boolean validate() {
        return adjustEventFactors();
    }

    @Override
    public void setVerbose(int verbose) {
        this.mVerbose = verbose;
    }

    public void generateActivity() {
        MonkeyActivityEvent e = new MonkeyActivityEvent(this.mMainApps.get(this.mRandom.nextInt(this.mMainApps.size())));
        this.mQ.addLast((MonkeyEvent) e);
    }

    @Override
    public MonkeyEvent getNextEvent() {
        if (this.mQ.isEmpty()) {
            generateEvents();
        }
        this.mEventCount++;
        MonkeyEvent e = this.mQ.getFirst();
        this.mQ.removeFirst();
        return e;
    }
}
