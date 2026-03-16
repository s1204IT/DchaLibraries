package com.android.inputmethodcommon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import jp.co.omronsoft.iwnnime.ml.KeyboardLanguagePackData;

public abstract class InputMethodSettingsFragment extends PreferenceFragment implements InputMethodSettingsInterface {
    private final InputMethodSettingsImpl mSettings = new InputMethodSettingsImpl();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(context));
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

    public void initInputMethodSettings(String screenKey, int categoryTitle, int subtypeEnablerTitle) {
        Context context = getActivity();
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        langPack.setInputMethodSubtypeInstallLangPack(context);
        getPreferenceScreen().setKey(screenKey);
        setInputMethodSettingsCategoryTitle(categoryTitle);
        setSubtypeEnablerTitle(subtypeEnablerTitle);
    }

    public void updateInputMethodSettings() {
        Context context = getActivity();
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        langPack.setInputMethodSubtypeInstallLangPack(context);
    }
}
