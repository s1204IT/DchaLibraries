package com.android.inputmethodcommon;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import jp.co.omronsoft.iwnnime.ml.KeyboardLanguagePackData;

public abstract class InputMethodSettingsActivity extends PreferenceActivity implements InputMethodSettingsInterface {
    public final InputMethodSettingsImpl mSettings = new InputMethodSettingsImpl();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setInputMethodSettingsCategoryTitle(int resId) {
        this.mSettings.setInputMethodSettingsCategoryTitle(resId);
    }

    @Override
    public void setInputMethodSettingsCategoryTitle(CharSequence title) {
        this.mSettings.setInputMethodSettingsCategoryTitle(title);
    }

    @Override
    public void setSubtypeEnablerTitle(int resId) {
        this.mSettings.setSubtypeEnablerTitle(resId);
    }

    @Override
    public void setSubtypeEnablerTitle(CharSequence title) {
        this.mSettings.setSubtypeEnablerTitle(title);
    }

    @Override
    public void setSubtypeEnablerIcon(int resId) {
        this.mSettings.setSubtypeEnablerIcon(resId);
    }

    @Override
    public void setSubtypeEnablerIcon(Drawable drawable) {
        this.mSettings.setSubtypeEnablerIcon(drawable);
    }

    public void initInputMethodSettings(int subtypeEnablerTitle) {
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        langPack.setInputMethodSubtypeInstallLangPack(this);
        setSubtypeEnablerTitle(subtypeEnablerTitle);
    }

    public void updateInputMethodSettings() {
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        langPack.setInputMethodSubtypeInstallLangPack(this);
    }

    public void startInputMethodSubTypeSettings() {
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        InputMethodSettingsImpl inputMethodSettingsImpl = this.mSettings;
        InputMethodInfo imi = InputMethodSettingsImpl.getMyImi(this, imm);
        this.mSettings.startInputMethodSubTypeSettings(this, imi);
    }

    public String getEnabledSubtypesLabel() {
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        InputMethodSettingsImpl inputMethodSettingsImpl = this.mSettings;
        InputMethodInfo imi = InputMethodSettingsImpl.getMyImi(this, imm);
        InputMethodSettingsImpl inputMethodSettingsImpl2 = this.mSettings;
        return InputMethodSettingsImpl.getEnabledSubtypesLabel(this, imm, imi);
    }

    public boolean isStartSettings() {
        return this.mSettings.isStartSettings();
    }

    public void clearIsStartSettings() {
        this.mSettings.clearIsStartSettings();
    }
}
