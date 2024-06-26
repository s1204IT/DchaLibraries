package com.android.settings.accessibility;

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
/* loaded from: classes.dex */
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

    @Override // com.android.settings.InstrumentedPreferenceFragment
    protected int getMetricsCategory() {
        return 3;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mCaptioningManager = (CaptioningManager) getSystemService("captioning");
        addPreferencesFromResource(R.xml.captioning_settings);
        initializeAllPreferences();
        updateAllPreferences();
        refreshShowingCustom();
        installUpdateListeners();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.captioning_preview, container, false);
        if (container instanceof PreferenceFrameLayout) {
            rootView.getLayoutParams().removeBorders = true;
        }
        View content = super.onCreateView(inflater, container, savedInstanceState);
        ((ViewGroup) rootView.findViewById(R.id.properties_fragment)).addView(content, -1, -1);
        return rootView;
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean enabled = this.mCaptioningManager.isEnabled();
        this.mPreviewText = view.findViewById(R.id.preview_text);
        this.mPreviewText.setVisibility(enabled ? 0 : 4);
        this.mPreviewWindow = view.findViewById(R.id.preview_window);
        this.mPreviewViewport = view.findViewById(R.id.preview_viewport);
        this.mPreviewViewport.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.android.settings.accessibility.CaptionPropertiesFragment.1
            @Override // android.view.View.OnLayoutChangeListener
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                CaptionPropertiesFragment.this.refreshPreviewText();
            }
        });
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        boolean enabled = this.mCaptioningManager.isEnabled();
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.setCheckedInternal(enabled);
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        getPreferenceScreen().setEnabled(enabled);
        refreshPreviewText();
        installSwitchBarToggleSwitch();
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        removeSwitchBarToggleSwitch();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshPreviewText() {
        SubtitleView preview;
        Context context = getActivity();
        if (context == null || (preview = this.mPreviewText) == null) {
            return;
        }
        int styleId = this.mCaptioningManager.getRawUserStyle();
        applyCaptionProperties(this.mCaptioningManager, preview, this.mPreviewViewport, styleId);
        Locale locale = this.mCaptioningManager.getLocale();
        if (locale != null) {
            CharSequence localizedText = AccessibilityUtils.getTextForLocale(context, locale, R.string.captioning_preview_text);
            preview.setText(localizedText);
        } else {
            preview.setText((int) R.string.captioning_preview_text);
        }
        CaptioningManager.CaptionStyle style = this.mCaptioningManager.getUserStyle();
        if (style.hasWindowColor()) {
            this.mPreviewWindow.setBackgroundColor(style.windowColor);
            return;
        }
        CaptioningManager.CaptionStyle defStyle = CaptioningManager.CaptionStyle.DEFAULT;
        this.mPreviewWindow.setBackgroundColor(defStyle.windowColor);
    }

    public static void applyCaptionProperties(CaptioningManager manager, SubtitleView previewText, View previewWindow, int styleId) {
        previewText.setStyle(styleId);
        Context context = previewText.getContext();
        context.getContentResolver();
        float fontScale = manager.getFontScale();
        if (previewWindow != null) {
            float virtualHeight = Math.max(previewWindow.getWidth() * 9, previewWindow.getHeight() * 16) / 16.0f;
            previewText.setTextSize(0.0533f * virtualHeight * fontScale);
        } else {
            float textSize = context.getResources().getDimension(R.dimen.caption_preview_text_size);
            previewText.setTextSize(textSize * fontScale);
        }
        Locale locale = manager.getLocale();
        if (locale != null) {
            CharSequence localizedText = AccessibilityUtils.getTextForLocale(context, locale, R.string.captioning_preview_characters);
            previewText.setText(localizedText);
            return;
        }
        previewText.setText((int) R.string.captioning_preview_characters);
    }

    protected void onInstallSwitchBarToggleSwitch() {
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() { // from class: com.android.settings.accessibility.CaptionPropertiesFragment.2
            @Override // com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                CaptionPropertiesFragment.this.mSwitchBar.setCheckedInternal(checked);
                Settings.Secure.putInt(CaptionPropertiesFragment.this.getActivity().getContentResolver(), "accessibility_captioning_enabled", checked ? 1 : 0);
                CaptionPropertiesFragment.this.getPreferenceScreen().setEnabled(checked);
                if (CaptionPropertiesFragment.this.mPreviewText != null) {
                    CaptionPropertiesFragment.this.mPreviewText.setVisibility(checked ? 0 : 4);
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
        Resources res = getResources();
        int[] presetValues = res.getIntArray(R.array.captioning_preset_selector_values);
        String[] presetTitles = res.getStringArray(R.array.captioning_preset_selector_titles);
        this.mPreset = (PresetPreference) findPreference("captioning_preset");
        this.mPreset.setValues(presetValues);
        this.mPreset.setTitles(presetTitles);
        this.mCustom = (PreferenceCategory) findPreference("custom");
        this.mShowingCustom = true;
        int[] colorValues = res.getIntArray(R.array.captioning_color_selector_values);
        String[] colorTitles = res.getStringArray(R.array.captioning_color_selector_titles);
        this.mForegroundColor = (ColorPreference) this.mCustom.findPreference("captioning_foreground_color");
        this.mForegroundColor.setTitles(colorTitles);
        this.mForegroundColor.setValues(colorValues);
        int[] opacityValues = res.getIntArray(R.array.captioning_opacity_selector_values);
        String[] opacityTitles = res.getStringArray(R.array.captioning_opacity_selector_titles);
        this.mForegroundOpacity = (ColorPreference) this.mCustom.findPreference("captioning_foreground_opacity");
        this.mForegroundOpacity.setTitles(opacityTitles);
        this.mForegroundOpacity.setValues(opacityValues);
        this.mEdgeColor = (ColorPreference) this.mCustom.findPreference("captioning_edge_color");
        this.mEdgeColor.setTitles(colorTitles);
        this.mEdgeColor.setValues(colorValues);
        int[] bgColorValues = new int[colorValues.length + 1];
        String[] bgColorTitles = new String[colorTitles.length + 1];
        System.arraycopy(colorValues, 0, bgColorValues, 1, colorValues.length);
        System.arraycopy(colorTitles, 0, bgColorTitles, 1, colorTitles.length);
        bgColorValues[0] = 0;
        bgColorTitles[0] = getString(R.string.color_none);
        this.mBackgroundColor = (ColorPreference) this.mCustom.findPreference("captioning_background_color");
        this.mBackgroundColor.setTitles(bgColorTitles);
        this.mBackgroundColor.setValues(bgColorValues);
        this.mBackgroundOpacity = (ColorPreference) this.mCustom.findPreference("captioning_background_opacity");
        this.mBackgroundOpacity.setTitles(opacityTitles);
        this.mBackgroundOpacity.setValues(opacityValues);
        this.mWindowColor = (ColorPreference) this.mCustom.findPreference("captioning_window_color");
        this.mWindowColor.setTitles(bgColorTitles);
        this.mWindowColor.setValues(bgColorValues);
        this.mWindowOpacity = (ColorPreference) this.mCustom.findPreference("captioning_window_opacity");
        this.mWindowOpacity.setTitles(opacityTitles);
        this.mWindowOpacity.setValues(opacityValues);
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
        int preset = this.mCaptioningManager.getRawUserStyle();
        this.mPreset.setValue(preset);
        float fontSize = this.mCaptioningManager.getFontScale();
        this.mFontSize.setValue(Float.toString(fontSize));
        ContentResolver cr = getContentResolver();
        CaptioningManager.CaptionStyle attrs = CaptioningManager.CaptionStyle.getCustomStyle(cr);
        this.mEdgeType.setValue(attrs.edgeType);
        this.mEdgeColor.setValue(attrs.edgeColor);
        int foregroundColor = attrs.hasForegroundColor() ? attrs.foregroundColor : 16777215;
        parseColorOpacity(this.mForegroundColor, this.mForegroundOpacity, foregroundColor);
        int backgroundColor = attrs.hasBackgroundColor() ? attrs.backgroundColor : 16777215;
        parseColorOpacity(this.mBackgroundColor, this.mBackgroundOpacity, backgroundColor);
        int windowColor = attrs.hasWindowColor() ? attrs.windowColor : 16777215;
        parseColorOpacity(this.mWindowColor, this.mWindowOpacity, windowColor);
        String rawTypeface = attrs.mRawTypeface;
        ListPreference listPreference = this.mTypeface;
        if (rawTypeface == null) {
            rawTypeface = "";
        }
        listPreference.setValue(rawTypeface);
        String rawLocale = this.mCaptioningManager.getRawLocale();
        LocalePreference localePreference = this.mLocale;
        if (rawLocale == null) {
            rawLocale = "";
        }
        localePreference.setValue(rawLocale);
    }

    private void parseColorOpacity(ColorPreference color, ColorPreference opacity, int value) {
        int colorValue;
        int opacityValue;
        if (!CaptioningManager.CaptionStyle.hasColor(value)) {
            colorValue = 16777215;
            opacityValue = (value & 255) << 24;
        } else if ((value >>> 24) == 0) {
            colorValue = 0;
            opacityValue = (value & 255) << 24;
        } else {
            colorValue = value | (-16777216);
            opacityValue = value & (-16777216);
        }
        opacity.setValue(16777215 | opacityValue);
        color.setValue(colorValue);
    }

    private int mergeColorOpacity(ColorPreference color, ColorPreference opacity) {
        int colorValue = color.getValue();
        int opacityValue = opacity.getValue();
        if (!CaptioningManager.CaptionStyle.hasColor(colorValue)) {
            int value = 16776960 | Color.alpha(opacityValue);
            return value;
        } else if (colorValue == 0) {
            int value2 = Color.alpha(opacityValue);
            return value2;
        } else {
            int value3 = (16777215 & colorValue) | ((-16777216) & opacityValue);
            return value3;
        }
    }

    private void refreshShowingCustom() {
        boolean customPreset = this.mPreset.getValue() == -1;
        if (!customPreset && this.mShowingCustom) {
            getPreferenceScreen().removePreference(this.mCustom);
            this.mShowingCustom = false;
        } else if (!customPreset || this.mShowingCustom) {
        } else {
            getPreferenceScreen().addPreference(this.mCustom);
            this.mShowingCustom = true;
        }
    }

    @Override // com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener
    public void onValueChanged(ListDialogPreference preference, int value) {
        ContentResolver cr = getActivity().getContentResolver();
        if (this.mForegroundColor == preference || this.mForegroundOpacity == preference) {
            int merged = mergeColorOpacity(this.mForegroundColor, this.mForegroundOpacity);
            Settings.Secure.putInt(cr, "accessibility_captioning_foreground_color", merged);
        } else if (this.mBackgroundColor == preference || this.mBackgroundOpacity == preference) {
            int merged2 = mergeColorOpacity(this.mBackgroundColor, this.mBackgroundOpacity);
            Settings.Secure.putInt(cr, "accessibility_captioning_background_color", merged2);
        } else if (this.mWindowColor == preference || this.mWindowOpacity == preference) {
            int merged3 = mergeColorOpacity(this.mWindowColor, this.mWindowOpacity);
            Settings.Secure.putInt(cr, "accessibility_captioning_window_color", merged3);
        } else if (this.mEdgeColor == preference) {
            Settings.Secure.putInt(cr, "accessibility_captioning_edge_color", value);
        } else if (this.mPreset == preference) {
            Settings.Secure.putInt(cr, "accessibility_captioning_preset", value);
            refreshShowingCustom();
        } else if (this.mEdgeType == preference) {
            Settings.Secure.putInt(cr, "accessibility_captioning_edge_type", value);
        }
        refreshPreviewText();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object value) {
        ContentResolver cr = getActivity().getContentResolver();
        if (this.mTypeface == preference) {
            Settings.Secure.putString(cr, "accessibility_captioning_typeface", (String) value);
        } else if (this.mFontSize == preference) {
            Settings.Secure.putFloat(cr, "accessibility_captioning_font_scale", Float.parseFloat((String) value));
        } else if (this.mLocale == preference) {
            Settings.Secure.putString(cr, "accessibility_captioning_locale", (String) value);
        }
        refreshPreviewText();
        return true;
    }
}
