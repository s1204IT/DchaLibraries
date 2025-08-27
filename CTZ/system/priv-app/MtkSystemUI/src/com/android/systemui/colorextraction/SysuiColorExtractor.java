package com.android.systemui.colorextraction;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.IWallpaperVisibilityListener;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.types.ExtractionType;
import com.android.internal.colorextraction.types.Tonal;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dumpable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

/* loaded from: classes.dex */
public class SysuiColorExtractor extends ColorExtractor implements Dumpable {
    private boolean mMediaBackdropVisible;
    private boolean mWallpaperVisible;
    private final ColorExtractor.GradientColors mWpHiddenColors;

    public SysuiColorExtractor(Context context) {
        this(context, new Tonal(context), true);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: com.android.systemui.colorextraction.SysuiColorExtractor */
    /* JADX WARN: Multi-variable type inference failed */
    @VisibleForTesting
    public SysuiColorExtractor(Context context, ExtractionType extractionType, boolean z) {
        super(context, extractionType);
        this.mWpHiddenColors = new ColorExtractor.GradientColors();
        updateDefaultGradients(getWallpaperColors(1));
        if (z) {
            try {
                setWallpaperVisible(WindowManagerGlobal.getWindowManagerService().registerWallpaperVisibilityListener(new AnonymousClass1(Handler.getMain()), 0));
            } catch (RemoteException e) {
                Log.w("SysuiColorExtractor", "Can't listen to wallpaper visibility changes", e);
            }
        }
        WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
        if (wallpaperManager != null) {
            wallpaperManager.removeOnColorsChangedListener(this);
            wallpaperManager.addOnColorsChangedListener(this, null, -1);
        }
    }

    /* renamed from: com.android.systemui.colorextraction.SysuiColorExtractor$1, reason: invalid class name */
    class AnonymousClass1 extends IWallpaperVisibilityListener.Stub {
        final /* synthetic */ Handler val$handler;

        AnonymousClass1(Handler handler) {
            this.val$handler = handler;
        }

        public void onWallpaperVisibilityChanged(final boolean z, int i) throws RemoteException {
            this.val$handler.post(new Runnable() { // from class: com.android.systemui.colorextraction.-$$Lambda$SysuiColorExtractor$1$cXNnFQ869baKbvxQWHTMa_T9XIA
                @Override // java.lang.Runnable
                public final void run() {
                    SysuiColorExtractor.this.setWallpaperVisible(z);
                }
            });
        }
    }

    private void updateDefaultGradients(WallpaperColors wallpaperColors) {
        Tonal.applyFallback(wallpaperColors, this.mWpHiddenColors);
    }

    public void onColorsChanged(WallpaperColors wallpaperColors, int i, int i2) {
        if (i2 != KeyguardUpdateMonitor.getCurrentUser()) {
            return;
        }
        super.onColorsChanged(wallpaperColors, i);
        if ((i & 1) != 0) {
            updateDefaultGradients(wallpaperColors);
        }
    }

    @VisibleForTesting
    ColorExtractor.GradientColors getFallbackColors() {
        return this.mWpHiddenColors;
    }

    public ColorExtractor.GradientColors getColors(int i) {
        return getColors(i, 1);
    }

    public ColorExtractor.GradientColors getColors(int i, int i2) {
        return getColors(i, i2, false);
    }

    public ColorExtractor.GradientColors getColors(int i, boolean z) {
        return getColors(i, 0, z);
    }

    public ColorExtractor.GradientColors getColors(int i, int i2, boolean z) {
        if (i == 1) {
            if (this.mWallpaperVisible || z) {
                return super.getColors(i, i2);
            }
            return this.mWpHiddenColors;
        }
        if (this.mMediaBackdropVisible) {
            return this.mWpHiddenColors;
        }
        return super.getColors(i, i2);
    }

    @VisibleForTesting
    void setWallpaperVisible(boolean z) {
        if (this.mWallpaperVisible != z) {
            this.mWallpaperVisible = z;
            triggerColorsChanged(1);
        }
    }

    public void setMediaBackdropVisible(boolean z) {
        if (this.mMediaBackdropVisible != z) {
            this.mMediaBackdropVisible = z;
            triggerColorsChanged(2);
        }
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("SysuiColorExtractor:");
        printWriter.println("  Current wallpaper colors:");
        printWriter.println("    system: " + this.mSystemColors);
        printWriter.println("    lock: " + this.mLockColors);
        ColorExtractor.GradientColors[] gradientColorsArr = (ColorExtractor.GradientColors[]) this.mGradientColors.get(1);
        ColorExtractor.GradientColors[] gradientColorsArr2 = (ColorExtractor.GradientColors[]) this.mGradientColors.get(2);
        printWriter.println("  Gradients:");
        printWriter.println("    system: " + Arrays.toString(gradientColorsArr));
        printWriter.println("    lock: " + Arrays.toString(gradientColorsArr2));
        printWriter.println("  Default scrim: " + this.mWpHiddenColors);
    }
}
