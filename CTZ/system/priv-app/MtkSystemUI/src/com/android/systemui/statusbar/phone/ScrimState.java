package com.android.systemui.statusbar.phone;

import android.graphics.Color;
import android.os.Trace;
import android.util.MathUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.ScrimView;

/* JADX WARN: Enum visitor error
jadx.core.utils.exceptions.JadxRuntimeException: Init of enum field 'KEYGUARD' uses external variables
	at jadx.core.dex.visitors.EnumVisitor.createEnumFieldByConstructor(EnumVisitor.java:451)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByField(EnumVisitor.java:372)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByWrappedInsn(EnumVisitor.java:337)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromFilledArray(EnumVisitor.java:322)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInsn(EnumVisitor.java:262)
	at jadx.core.dex.visitors.EnumVisitor.convertToEnum(EnumVisitor.java:151)
	at jadx.core.dex.visitors.EnumVisitor.visit(EnumVisitor.java:100)
 */
/* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
/* loaded from: classes.dex */
public class ScrimState {
    private static final /* synthetic */ ScrimState[] $VALUES;
    public static final ScrimState AOD;
    public static final ScrimState BOUNCER;
    public static final ScrimState BOUNCER_SCRIMMED;
    public static final ScrimState BRIGHTNESS_MIRROR;
    public static final ScrimState KEYGUARD;
    public static final ScrimState PULSING;
    public static final ScrimState UNINITIALIZED = new ScrimState("UNINITIALIZED", 0, -1);
    public static final ScrimState UNLOCKED;
    boolean mAnimateChange;
    long mAnimationDuration;
    float mAodFrontScrimAlpha;
    boolean mBlankScreen;
    float mCurrentBehindAlpha;
    int mCurrentBehindTint;
    float mCurrentInFrontAlpha;
    int mCurrentInFrontTint;
    boolean mDisplayRequiresBlanking;
    DozeParameters mDozeParameters;
    int mIndex;
    KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    ScrimView mScrimBehind;
    float mScrimBehindAlphaKeyguard;
    ScrimView mScrimInFront;
    boolean mWallpaperSupportsAmbientMode;

    public static ScrimState valueOf(String str) {
        return (ScrimState) Enum.valueOf(ScrimState.class, str);
    }

    public static ScrimState[] values() {
        return (ScrimState[]) $VALUES.clone();
    }

    static {
        int i = 1;
        KEYGUARD = new ScrimState("KEYGUARD", i, 0) { // from class: com.android.systemui.statusbar.phone.ScrimState.1
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                this.mBlankScreen = false;
                if (scrimState == ScrimState.AOD) {
                    this.mAnimationDuration = 500L;
                    if (this.mDisplayRequiresBlanking) {
                        this.mBlankScreen = true;
                    }
                } else {
                    this.mAnimationDuration = 220L;
                }
                this.mCurrentBehindAlpha = this.mScrimBehindAlphaKeyguard;
                this.mCurrentInFrontAlpha = 0.0f;
            }

            @Override // com.android.systemui.statusbar.phone.ScrimState
            public float getBehindAlpha(float f) {
                return MathUtils.map(0.0f, 1.0f, this.mScrimBehindAlphaKeyguard, 0.7f, f);
            }
        };
        int i2 = 2;
        BOUNCER = new ScrimState("BOUNCER", i2, i) { // from class: com.android.systemui.statusbar.phone.ScrimState.2
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                this.mCurrentBehindAlpha = 0.7f;
                this.mCurrentInFrontAlpha = 0.0f;
            }
        };
        int i3 = 3;
        BOUNCER_SCRIMMED = new ScrimState("BOUNCER_SCRIMMED", i3, i2) { // from class: com.android.systemui.statusbar.phone.ScrimState.3
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                this.mCurrentBehindAlpha = 0.0f;
                this.mCurrentInFrontAlpha = 0.7f;
            }
        };
        int i4 = 4;
        BRIGHTNESS_MIRROR = new ScrimState("BRIGHTNESS_MIRROR", i4, i3) { // from class: com.android.systemui.statusbar.phone.ScrimState.4
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                this.mCurrentBehindAlpha = 0.0f;
                this.mCurrentInFrontAlpha = 0.0f;
            }
        };
        int i5 = 5;
        AOD = new ScrimState("AOD", i5, i4) { // from class: com.android.systemui.statusbar.phone.ScrimState.5
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                boolean alwaysOn = this.mDozeParameters.getAlwaysOn();
                this.mBlankScreen = this.mDisplayRequiresBlanking;
                this.mCurrentBehindAlpha = (!this.mWallpaperSupportsAmbientMode || this.mKeyguardUpdateMonitor.hasLockscreenWallpaper()) ? 1.0f : 0.0f;
                this.mCurrentInFrontAlpha = alwaysOn ? this.mAodFrontScrimAlpha : 1.0f;
                this.mCurrentInFrontTint = -16777216;
                this.mCurrentBehindTint = -16777216;
                this.mAnimationDuration = 1000L;
                this.mAnimateChange = this.mDozeParameters.shouldControlScreenOff();
            }

            @Override // com.android.systemui.statusbar.phone.ScrimState
            public boolean isLowPowerState() {
                return true;
            }
        };
        int i6 = 6;
        PULSING = new ScrimState("PULSING", i6, i5) { // from class: com.android.systemui.statusbar.phone.ScrimState.6
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                this.mCurrentInFrontAlpha = 0.0f;
                this.mCurrentInFrontTint = -16777216;
                this.mCurrentBehindAlpha = (!this.mWallpaperSupportsAmbientMode || this.mKeyguardUpdateMonitor.hasLockscreenWallpaper()) ? 1.0f : 0.0f;
                this.mCurrentBehindTint = -16777216;
                this.mBlankScreen = this.mDisplayRequiresBlanking;
            }
        };
        UNLOCKED = new ScrimState("UNLOCKED", 7, i6) { // from class: com.android.systemui.statusbar.phone.ScrimState.7
            @Override // com.android.systemui.statusbar.phone.ScrimState
            public void prepare(ScrimState scrimState) {
                this.mCurrentBehindAlpha = 0.0f;
                this.mCurrentInFrontAlpha = 0.0f;
                this.mAnimationDuration = 300L;
                if (scrimState == ScrimState.AOD || scrimState == ScrimState.PULSING) {
                    updateScrimColor(this.mScrimInFront, 1.0f, -16777216);
                    updateScrimColor(this.mScrimBehind, 1.0f, -16777216);
                    this.mCurrentInFrontTint = -16777216;
                    this.mCurrentBehindTint = -16777216;
                    this.mBlankScreen = true;
                    return;
                }
                this.mCurrentInFrontTint = 0;
                this.mCurrentBehindTint = 0;
                this.mBlankScreen = false;
            }
        };
        $VALUES = new ScrimState[]{UNINITIALIZED, KEYGUARD, BOUNCER, BOUNCER_SCRIMMED, BRIGHTNESS_MIRROR, AOD, PULSING, UNLOCKED};
    }

    private ScrimState(String str, int i, int i2) {
        this.mBlankScreen = false;
        this.mAnimationDuration = 220L;
        this.mCurrentInFrontTint = 0;
        this.mCurrentBehindTint = 0;
        this.mAnimateChange = true;
        this.mIndex = i2;
    }

    public void init(ScrimView scrimView, ScrimView scrimView2, DozeParameters dozeParameters) {
        this.mScrimInFront = scrimView;
        this.mScrimBehind = scrimView2;
        this.mDozeParameters = dozeParameters;
        this.mDisplayRequiresBlanking = dozeParameters.getDisplayNeedsBlanking();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(scrimView.getContext());
    }

    public void prepare(ScrimState scrimState) {
    }

    public int getIndex() {
        return this.mIndex;
    }

    public float getFrontAlpha() {
        return this.mCurrentInFrontAlpha;
    }

    public float getBehindAlpha(float f) {
        return this.mCurrentBehindAlpha;
    }

    public int getFrontTint() {
        return this.mCurrentInFrontTint;
    }

    public int getBehindTint() {
        return this.mCurrentBehindTint;
    }

    public long getAnimationDuration() {
        return this.mAnimationDuration;
    }

    public boolean getBlanksScreen() {
        return this.mBlankScreen;
    }

    public void updateScrimColor(ScrimView scrimView, float f, int i) {
        Trace.traceCounter(4096L, scrimView == this.mScrimInFront ? "front_scrim_alpha" : "back_scrim_alpha", (int) (255.0f * f));
        Trace.traceCounter(4096L, scrimView == this.mScrimInFront ? "front_scrim_tint" : "back_scrim_tint", Color.alpha(i));
        scrimView.setTint(i);
        scrimView.setViewAlpha(f);
    }

    public boolean getAnimateChange() {
        return this.mAnimateChange;
    }

    public void setAodFrontScrimAlpha(float f) {
        this.mAodFrontScrimAlpha = f;
    }

    public void setScrimBehindAlphaKeyguard(float f) {
        this.mScrimBehindAlphaKeyguard = f;
    }

    public void setWallpaperSupportsAmbientMode(boolean z) {
        this.mWallpaperSupportsAmbientMode = z;
    }

    public boolean isLowPowerState() {
        return false;
    }
}
