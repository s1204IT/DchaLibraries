package com.android.settings;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.Display;
import java.util.ArrayList;

public class ColorModePreference extends SwitchPreference implements DisplayManager.DisplayListener {
    private int mCurrentIndex;
    private ArrayList<ColorTransformDescription> mDescriptions;
    private android.view.Display mDisplay;
    private DisplayManager mDisplayManager;

    public ColorModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDisplayManager = (DisplayManager) getContext().getSystemService(DisplayManager.class);
    }

    public int getTransformsCount() {
        return this.mDescriptions.size();
    }

    public void startListening() {
        this.mDisplayManager.registerDisplayListener(this, new Handler(Looper.getMainLooper()));
    }

    public void stopListening() {
        this.mDisplayManager.unregisterDisplayListener(this);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        if (displayId != 0) {
            return;
        }
        updateCurrentAndSupported();
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (displayId != 0) {
            return;
        }
        updateCurrentAndSupported();
    }

    @Override
    public void onDisplayRemoved(int displayId) {
    }

    public void updateCurrentAndSupported() {
        ColorTransformDescription colorTransformDescription = null;
        this.mDisplay = this.mDisplayManager.getDisplay(0);
        this.mDescriptions = new ArrayList<>();
        Resources resources = getContext().getResources();
        int[] transforms = resources.getIntArray(android.R.array.config_clockTickVibePattern);
        String[] titles = resources.getStringArray(R.array.color_mode_names);
        String[] descriptions = resources.getStringArray(R.array.color_mode_descriptions);
        for (int i = 0; i < transforms.length; i++) {
            if (transforms[i] != -1 && i != 1) {
                ColorTransformDescription desc = new ColorTransformDescription(colorTransformDescription);
                desc.colorTransform = transforms[i];
                desc.title = titles[i];
                desc.summary = descriptions[i];
                this.mDescriptions.add(desc);
            }
        }
        Display.ColorTransform[] supportedColorTransforms = this.mDisplay.getSupportedColorTransforms();
        for (int i2 = 0; i2 < supportedColorTransforms.length; i2++) {
            int j = 0;
            while (true) {
                if (j >= this.mDescriptions.size()) {
                    break;
                }
                if (this.mDescriptions.get(j).colorTransform != supportedColorTransforms[i2].getColorTransform() || this.mDescriptions.get(j).transform != null) {
                    j++;
                } else {
                    this.mDescriptions.get(j).transform = supportedColorTransforms[i2];
                    break;
                }
            }
        }
        int i3 = 0;
        while (i3 < this.mDescriptions.size()) {
            if (this.mDescriptions.get(i3).transform == null) {
                this.mDescriptions.remove(i3);
                i3--;
            }
            i3++;
        }
        Display.ColorTransform currentTransform = this.mDisplay.getColorTransform();
        this.mCurrentIndex = -1;
        int i4 = 0;
        while (true) {
            if (i4 >= this.mDescriptions.size()) {
                break;
            }
            if (this.mDescriptions.get(i4).colorTransform != currentTransform.getColorTransform()) {
                i4++;
            } else {
                this.mCurrentIndex = i4;
                break;
            }
        }
        setChecked(this.mCurrentIndex == 1);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        if (this.mDescriptions.size() == 2) {
            ColorTransformDescription desc = this.mDescriptions.get(value ? 1 : 0);
            this.mDisplay.requestColorTransform(desc.transform);
            this.mCurrentIndex = this.mDescriptions.indexOf(desc);
        }
        return true;
    }

    private static class ColorTransformDescription {
        private int colorTransform;
        private String summary;
        private String title;
        private Display.ColorTransform transform;

        ColorTransformDescription(ColorTransformDescription colorTransformDescription) {
            this();
        }

        private ColorTransformDescription() {
        }
    }
}
