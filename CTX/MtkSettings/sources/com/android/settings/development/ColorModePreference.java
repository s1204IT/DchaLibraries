package com.android.settings.development;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.Display;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class ColorModePreference extends SwitchPreference implements DisplayManager.DisplayListener {
    private int mCurrentIndex;
    private List<ColorModeDescription> mDescriptions;
    private Display mDisplay;
    private DisplayManager mDisplayManager;

    public static List<ColorModeDescription> getColorModeDescriptions(Context context) {
        ArrayList arrayList = new ArrayList();
        Resources resources = context.getResources();
        int[] intArray = resources.getIntArray(R.array.color_mode_ids);
        String[] stringArray = resources.getStringArray(R.array.color_mode_names);
        String[] stringArray2 = resources.getStringArray(R.array.color_mode_descriptions);
        for (int i = 0; i < intArray.length; i++) {
            if (intArray[i] != -1 && i != 1) {
                ColorModeDescription colorModeDescription = new ColorModeDescription();
                colorModeDescription.colorMode = intArray[i];
                colorModeDescription.title = stringArray[i];
                colorModeDescription.summary = stringArray2[i];
                arrayList.add(colorModeDescription);
            }
        }
        return arrayList;
    }

    public ColorModePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDisplayManager = (DisplayManager) getContext().getSystemService(DisplayManager.class);
    }

    public void startListening() {
        this.mDisplayManager.registerDisplayListener(this, new Handler(Looper.getMainLooper()));
    }

    public void stopListening() {
        this.mDisplayManager.unregisterDisplayListener(this);
    }

    @Override
    public void onDisplayAdded(int i) {
        if (i == 0) {
            updateCurrentAndSupported();
        }
    }

    @Override
    public void onDisplayChanged(int i) {
        if (i == 0) {
            updateCurrentAndSupported();
        }
    }

    @Override
    public void onDisplayRemoved(int i) {
    }

    public void updateCurrentAndSupported() {
        this.mDisplay = this.mDisplayManager.getDisplay(0);
        this.mDescriptions = getColorModeDescriptions(getContext());
        int colorMode = this.mDisplay.getColorMode();
        this.mCurrentIndex = -1;
        int i = 0;
        while (true) {
            if (i >= this.mDescriptions.size()) {
                break;
            }
            if (this.mDescriptions.get(i).colorMode != colorMode) {
                i++;
            } else {
                this.mCurrentIndex = i;
                break;
            }
        }
        setChecked(this.mCurrentIndex == 1);
    }

    @Override
    public boolean persistBoolean(boolean z) {
        if (this.mDescriptions.size() == 2) {
            ColorModeDescription colorModeDescription = this.mDescriptions.get(z ? 1 : 0);
            this.mDisplay.requestColorMode(colorModeDescription.colorMode);
            this.mCurrentIndex = this.mDescriptions.indexOf(colorModeDescription);
            return true;
        }
        return true;
    }

    static class ColorModeDescription {
        private int colorMode;
        private String summary;
        private String title;

        private ColorModeDescription() {
        }
    }
}
