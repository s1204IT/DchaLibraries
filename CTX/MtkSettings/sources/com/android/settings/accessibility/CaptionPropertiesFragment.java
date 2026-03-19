package com.android.settings.accessibility;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import com.android.internal.widget.SubtitleView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.ListDialogPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import com.android.settingslib.accessibility.AccessibilityUtils;
import java.util.Locale;

public class CaptionPropertiesFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, ListDialogPreference.OnValueChangedListener {
    private ColorPreference mBackgroundColor;
    private ColorPreference mBackgroundOpacity;
    private CaptioningManager mCaptioningManager;
    private PreferenceCategory mCustom;
    private ColorPreference mEdgeColor;
    private EdgeTypePreference mEdgeType;
    private ListPreference mFontSize;
    private ColorPreference mForegroundColor;
    private ColorPreference mForegroundOpacity;
    private LocalePreference mLocale;
    private PresetPreference mPreset;
    private SubtitleView mPreviewText;
    private View mPreviewViewport;
    private View mPreviewWindow;
    private boolean mShowingCustom;
    private SwitchBar mSwitchBar;
    private ToggleSwitch mToggleSwitch;
    private ListPreference mTypeface;
    private ColorPreference mWindowColor;
    private ColorPreference mWindowOpacity;

    @Override
    public int getMetricsCategory() {
        return 3;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mCaptioningManager = (CaptioningManager) getSystemService("captioning");
        addPreferencesFromResource(R.xml.captioning_settings);
        initializeAllPreferences();
        updateAllPreferences();
        refreshShowingCustom();
        installUpdateListeners();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.captioning_preview, viewGroup, false);
        if (viewGroup instanceof PreferenceFrameLayout) {
            viewInflate.getLayoutParams().removeBorders = true;
        }
        ((ViewGroup) viewInflate.findViewById(R.id.properties_fragment)).addView(super.onCreateView(layoutInflater, viewGroup, bundle), -1, -1);
        return viewInflate;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        boolean zIsEnabled = this.mCaptioningManager.isEnabled();
        this.mPreviewText = view.findViewById(R.id.preview_text);
        this.mPreviewText.setVisibility(zIsEnabled ? 0 : 4);
        this.mPreviewWindow = view.findViewById(R.id.preview_window);
        this.mPreviewViewport = view.findViewById(R.id.preview_viewport);
        this.mPreviewViewport.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                CaptionPropertiesFragment.this.refreshPreviewText();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        boolean zIsEnabled = this.mCaptioningManager.isEnabled();
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.setSwitchBarText(R.string.accessibility_caption_master_switch_title, R.string.accessibility_caption_master_switch_title);
        this.mSwitchBar.setCheckedInternal(zIsEnabled);
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        getPreferenceScreen().setEnabled(zIsEnabled);
        refreshPreviewText();
        installSwitchBarToggleSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeSwitchBarToggleSwitch();
    }

    private void refreshPreviewText() {
        SubtitleView subtitleView;
        Activity activity = getActivity();
        if (activity != null && (subtitleView = this.mPreviewText) != null) {
            applyCaptionProperties(this.mCaptioningManager, subtitleView, this.mPreviewViewport, this.mCaptioningManager.getRawUserStyle());
            Locale locale = this.mCaptioningManager.getLocale();
            if (locale != null) {
                subtitleView.setText(AccessibilityUtils.getTextForLocale(activity, locale, R.string.captioning_preview_text));
            } else {
                subtitleView.setText(R.string.captioning_preview_text);
            }
            CaptioningManager.CaptionStyle userStyle = this.mCaptioningManager.getUserStyle();
            if (userStyle.hasWindowColor()) {
                this.mPreviewWindow.setBackgroundColor(userStyle.windowColor);
            } else {
                this.mPreviewWindow.setBackgroundColor(CaptioningManager.CaptionStyle.DEFAULT.windowColor);
            }
        }
    }

    public static void applyCaptionProperties(CaptioningManager captioningManager, SubtitleView subtitleView, View view, int i) {
        subtitleView.setStyle(i);
        Context context = subtitleView.getContext();
        context.getContentResolver();
        float fontScale = captioningManager.getFontScale();
        if (view != null) {
            subtitleView.setTextSize((Math.max(9 * view.getWidth(), 16 * view.getHeight()) / 16.0f) * 0.0533f * fontScale);
        } else {
            subtitleView.setTextSize(context.getResources().getDimension(R.dimen.caption_preview_text_size) * fontScale);
        }
        Locale locale = captioningManager.getLocale();
        if (locale != null) {
            subtitleView.setText(AccessibilityUtils.getTextForLocale(context, locale, R.string.captioning_preview_characters));
        } else {
            subtitleView.setText(R.string.captioning_preview_characters);
        }
    }

    protected void onInstallSwitchBarToggleSwitch() {
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean z) {
                CaptionPropertiesFragment.this.mSwitchBar.setCheckedInternal(z);
                Settings.Secure.putInt(CaptionPropertiesFragment.this.getActivity().getContentResolver(), "accessibility_captioning_enabled", z ? 1 : 0);
                CaptionPropertiesFragment.this.getPreferenceScreen().setEnabled(z);
                if (CaptionPropertiesFragment.this.mPreviewText != null) {
                    CaptionPropertiesFragment.this.mPreviewText.setVisibility(z ? 0 : 4);
                }
                return false;
            }
        });
    }

    private void installSwitchBarToggleSwitch() {
        onInstallSwitchBarToggleSwitch();
        this.mSwitchBar.show();
    }

    private void removeSwitchBarToggleSwitch() {
        this.mSwitchBar.hide();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(null);
    }

    private void initializeAllPreferences() {
        this.mLocale = (LocalePreference) findPreference("captioning_locale");
        this.mFontSize = (ListPreference) findPreference("captioning_font_size");
        Resources resources = getResources();
        int[] intArray = resources.getIntArray(R.array.captioning_preset_selector_values);
        String[] stringArray = resources.getStringArray(R.array.captioning_preset_selector_titles);
        this.mPreset = (PresetPreference) findPreference("captioning_preset");
        this.mPreset.setValues(intArray);
        this.mPreset.setTitles(stringArray);
        this.mCustom = (PreferenceCategory) findPreference("custom");
        this.mShowingCustom = true;
        int[] intArray2 = resources.getIntArray(R.array.captioning_color_selector_values);
        String[] stringArray2 = resources.getStringArray(R.array.captioning_color_selector_titles);
        this.mForegroundColor = (ColorPreference) this.mCustom.findPreference("captioning_foreground_color");
        this.mForegroundColor.setTitles(stringArray2);
        this.mForegroundColor.setValues(intArray2);
        int[] intArray3 = resources.getIntArray(R.array.captioning_opacity_selector_values);
        String[] stringArray3 = resources.getStringArray(R.array.captioning_opacity_selector_titles);
        this.mForegroundOpacity = (ColorPreference) this.mCustom.findPreference("captioning_foreground_opacity");
        this.mForegroundOpacity.setTitles(stringArray3);
        this.mForegroundOpacity.setValues(intArray3);
        this.mEdgeColor = (ColorPreference) this.mCustom.findPreference("captioning_edge_color");
        this.mEdgeColor.setTitles(stringArray2);
        this.mEdgeColor.setValues(intArray2);
        int[] iArr = new int[intArray2.length + 1];
        String[] strArr = new String[stringArray2.length + 1];
        System.arraycopy(intArray2, 0, iArr, 1, intArray2.length);
        System.arraycopy(stringArray2, 0, strArr, 1, stringArray2.length);
        iArr[0] = 0;
        strArr[0] = getString(R.string.color_none);
        this.mBackgroundColor = (ColorPreference) this.mCustom.findPreference("captioning_background_color");
        this.mBackgroundColor.setTitles(strArr);
        this.mBackgroundColor.setValues(iArr);
        this.mBackgroundOpacity = (ColorPreference) this.mCustom.findPreference("captioning_background_opacity");
        this.mBackgroundOpacity.setTitles(stringArray3);
        this.mBackgroundOpacity.setValues(intArray3);
        this.mWindowColor = (ColorPreference) this.mCustom.findPreference("captioning_window_color");
        this.mWindowColor.setTitles(strArr);
        this.mWindowColor.setValues(iArr);
        this.mWindowOpacity = (ColorPreference) this.mCustom.findPreference("captioning_window_opacity");
        this.mWindowOpacity.setTitles(stringArray3);
        this.mWindowOpacity.setValues(intArray3);
        this.mEdgeType = (EdgeTypePreference) this.mCustom.findPreference("captioning_edge_type");
        this.mTypeface = (ListPreference) this.mCustom.findPreference("captioning_typeface");
    }

    private void installUpdateListeners() {
        this.mPreset.setOnValueChangedListener(this);
        this.mForegroundColor.setOnValueChangedListener(this);
        this.mForegroundOpacity.setOnValueChangedListener(this);
        this.mEdgeColor.setOnValueChangedListener(this);
        this.mBackgroundColor.setOnValueChangedListener(this);
        this.mBackgroundOpacity.setOnValueChangedListener(this);
        this.mWindowColor.setOnValueChangedListener(this);
        this.mWindowOpacity.setOnValueChangedListener(this);
        this.mEdgeType.setOnValueChangedListener(this);
        this.mTypeface.setOnPreferenceChangeListener(this);
        this.mFontSize.setOnPreferenceChangeListener(this);
        this.mLocale.setOnPreferenceChangeListener(this);
    }

    private void updateAllPreferences() {
        int i;
        int i2;
        this.mPreset.setValue(this.mCaptioningManager.getRawUserStyle());
        this.mFontSize.setValue(Float.toString(this.mCaptioningManager.getFontScale()));
        CaptioningManager.CaptionStyle customStyle = CaptioningManager.CaptionStyle.getCustomStyle(getContentResolver());
        this.mEdgeType.setValue(customStyle.edgeType);
        this.mEdgeColor.setValue(customStyle.edgeColor);
        if (customStyle.hasForegroundColor()) {
            i = customStyle.foregroundColor;
        } else {
            i = 16777215;
        }
        parseColorOpacity(this.mForegroundColor, this.mForegroundOpacity, i);
        if (customStyle.hasBackgroundColor()) {
            i2 = customStyle.backgroundColor;
        } else {
            i2 = 16777215;
        }
        parseColorOpacity(this.mBackgroundColor, this.mBackgroundOpacity, i2);
        parseColorOpacity(this.mWindowColor, this.mWindowOpacity, customStyle.hasWindowColor() ? customStyle.windowColor : 16777215);
        String str = customStyle.mRawTypeface;
        ListPreference listPreference = this.mTypeface;
        if (str == null) {
            str = "";
        }
        listPreference.setValue(str);
        String rawLocale = this.mCaptioningManager.getRawLocale();
        LocalePreference localePreference = this.mLocale;
        if (rawLocale == null) {
            rawLocale = "";
        }
        localePreference.setValue(rawLocale);
    }

    private void parseColorOpacity(ColorPreference colorPreference, ColorPreference colorPreference2, int i) {
        int i2;
        int i3;
        if (!CaptioningManager.CaptionStyle.hasColor(i)) {
            i2 = (i & 255) << 24;
            i3 = 16777215;
        } else if ((i >>> 24) == 0) {
            i3 = 0;
            i2 = (i & 255) << 24;
        } else {
            int i4 = i | (-16777216);
            i2 = i & (-16777216);
            i3 = i4;
        }
        colorPreference2.setValue(i2 | 16777215);
        colorPreference.setValue(i3);
    }

    private int mergeColorOpacity(ColorPreference colorPreference, ColorPreference colorPreference2) {
        int value = colorPreference.getValue();
        int value2 = colorPreference2.getValue();
        if (!CaptioningManager.CaptionStyle.hasColor(value)) {
            return 16776960 | Color.alpha(value2);
        }
        if (value == 0) {
            return Color.alpha(value2);
        }
        return (value & 16777215) | (value2 & (-16777216));
    }

    private void refreshShowingCustom() {
        boolean z = this.mPreset.getValue() == -1;
        if (!z && this.mShowingCustom) {
            getPreferenceScreen().removePreference(this.mCustom);
            this.mShowingCustom = false;
        } else if (z && !this.mShowingCustom) {
            getPreferenceScreen().addPreference(this.mCustom);
            this.mShowingCustom = true;
        }
    }

    @Override
    public void onValueChanged(ListDialogPreference listDialogPreference, int i) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        if (this.mForegroundColor == listDialogPreference || this.mForegroundOpacity == listDialogPreference) {
            Settings.Secure.putInt(contentResolver, "accessibility_captioning_foreground_color", mergeColorOpacity(this.mForegroundColor, this.mForegroundOpacity));
        } else if (this.mBackgroundColor == listDialogPreference || this.mBackgroundOpacity == listDialogPreference) {
            Settings.Secure.putInt(contentResolver, "accessibility_captioning_background_color", mergeColorOpacity(this.mBackgroundColor, this.mBackgroundOpacity));
        } else if (this.mWindowColor == listDialogPreference || this.mWindowOpacity == listDialogPreference) {
            Settings.Secure.putInt(contentResolver, "accessibility_captioning_window_color", mergeColorOpacity(this.mWindowColor, this.mWindowOpacity));
        } else if (this.mEdgeColor == listDialogPreference) {
            Settings.Secure.putInt(contentResolver, "accessibility_captioning_edge_color", i);
        } else if (this.mPreset == listDialogPreference) {
            Settings.Secure.putInt(contentResolver, "accessibility_captioning_preset", i);
            refreshShowingCustom();
        } else if (this.mEdgeType == listDialogPreference) {
            Settings.Secure.putInt(contentResolver, "accessibility_captioning_edge_type", i);
        }
        refreshPreviewText();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        if (this.mTypeface == preference) {
            Settings.Secure.putString(contentResolver, "accessibility_captioning_typeface", (String) obj);
        } else if (this.mFontSize == preference) {
            Settings.Secure.putFloat(contentResolver, "accessibility_captioning_font_scale", Float.parseFloat((String) obj));
        } else if (this.mLocale == preference) {
            Settings.Secure.putString(contentResolver, "accessibility_captioning_locale", (String) obj);
        }
        refreshPreviewText();
        return true;
    }
}
