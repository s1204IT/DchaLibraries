package com.android.settings.display;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.List;

public class ColorModePreferenceFragment extends RadioButtonPickerFragment implements ColorDisplayController.Callback {
    static final String KEY_COLOR_MODE_AUTOMATIC = "color_mode_automatic";
    static final String KEY_COLOR_MODE_BOOSTED = "color_mode_boosted";
    static final String KEY_COLOR_MODE_NATURAL = "color_mode_natural";
    static final String KEY_COLOR_MODE_SATURATED = "color_mode_saturated";
    private ColorDisplayController mController;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mController = new ColorDisplayController(context);
        this.mController.setListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (this.mController != null) {
            this.mController.setListener((ColorDisplayController.Callback) null);
            this.mController = null;
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.color_mode_settings;
    }

    void configureAndInstallPreview(LayoutPreference layoutPreference, PreferenceScreen preferenceScreen) {
        layoutPreference.setSelectable(false);
        preferenceScreen.addPreference(layoutPreference);
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen preferenceScreen) {
        configureAndInstallPreview(new LayoutPreference(preferenceScreen.getContext(), R.layout.color_mode_preview), preferenceScreen);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context context = getContext();
        int[] intArray = context.getResources().getIntArray(android.R.array.config_autoBrightnessDisplayValuesNits);
        ArrayList arrayList = new ArrayList();
        if (intArray != null) {
            for (int i : intArray) {
                if (i == 0) {
                    arrayList.add(new ColorModeCandidateInfo(context.getText(R.string.color_mode_option_natural), KEY_COLOR_MODE_NATURAL, true));
                } else if (i == 1) {
                    arrayList.add(new ColorModeCandidateInfo(context.getText(R.string.color_mode_option_boosted), KEY_COLOR_MODE_BOOSTED, true));
                } else if (i == 2) {
                    arrayList.add(new ColorModeCandidateInfo(context.getText(R.string.color_mode_option_saturated), KEY_COLOR_MODE_SATURATED, true));
                } else if (i == 3) {
                    arrayList.add(new ColorModeCandidateInfo(context.getText(R.string.color_mode_option_automatic), KEY_COLOR_MODE_AUTOMATIC, true));
                }
            }
        }
        return arrayList;
    }

    @Override
    protected String getDefaultKey() {
        int colorMode = this.mController.getColorMode();
        if (colorMode == 3) {
            return KEY_COLOR_MODE_AUTOMATIC;
        }
        if (colorMode == 2) {
            return KEY_COLOR_MODE_SATURATED;
        }
        if (colorMode == 1) {
            return KEY_COLOR_MODE_BOOSTED;
        }
        return KEY_COLOR_MODE_NATURAL;
    }

    @Override
    protected boolean setDefaultKey(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -2029194174) {
            if (iHashCode != -739564821) {
                if (iHashCode != -365217559) {
                    b = (iHashCode == 765917269 && str.equals(KEY_COLOR_MODE_SATURATED)) ? (byte) 2 : (byte) -1;
                } else if (str.equals(KEY_COLOR_MODE_NATURAL)) {
                    b = 0;
                }
            } else if (str.equals(KEY_COLOR_MODE_AUTOMATIC)) {
                b = 3;
            }
        } else if (str.equals(KEY_COLOR_MODE_BOOSTED)) {
            b = 1;
        }
        switch (b) {
            case 0:
                this.mController.setColorMode(0);
                return true;
            case 1:
                this.mController.setColorMode(1);
                return true;
            case 2:
                this.mController.setColorMode(2);
                return true;
            case 3:
                this.mController.setColorMode(3);
                return true;
            default:
                return true;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1143;
    }

    static class ColorModeCandidateInfo extends CandidateInfo {
        private final String mKey;
        private final CharSequence mLabel;

        ColorModeCandidateInfo(CharSequence charSequence, String str, boolean z) {
            super(z);
            this.mLabel = charSequence;
            this.mKey = str;
        }

        @Override
        public CharSequence loadLabel() {
            return this.mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return this.mKey;
        }
    }

    public void onAccessibilityTransformChanged(boolean z) {
        if (z) {
            getActivity().onBackPressed();
        }
    }
}
