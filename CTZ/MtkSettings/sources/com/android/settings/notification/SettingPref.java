package com.android.settings.notification;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import com.android.settings.SettingsPreferenceFragment;

public class SettingPref {
    protected final int mDefault;
    protected DropDownPreference mDropDown;
    private final String mKey;
    protected final String mSetting;
    protected TwoStatePreference mTwoState;
    protected final int mType;
    private final Uri mUri;
    private final int[] mValues;

    public SettingPref(int i, String str, String str2, int i2, int... iArr) {
        this.mType = i;
        this.mKey = str;
        this.mSetting = str2;
        this.mDefault = i2;
        this.mValues = iArr;
        this.mUri = getUriFor(this.mType, this.mSetting);
    }

    public boolean isApplicable(Context context) {
        return true;
    }

    protected String getCaption(Resources resources, int i) {
        throw new UnsupportedOperationException();
    }

    public Preference init(SettingsPreferenceFragment settingsPreferenceFragment) {
        final Activity activity = settingsPreferenceFragment.getActivity();
        Preference preferenceFindPreference = settingsPreferenceFragment.getPreferenceScreen().findPreference(this.mKey);
        if (preferenceFindPreference != null && !isApplicable(activity)) {
            settingsPreferenceFragment.getPreferenceScreen().removePreference(preferenceFindPreference);
            preferenceFindPreference = null;
        }
        if (preferenceFindPreference instanceof TwoStatePreference) {
            this.mTwoState = (TwoStatePreference) preferenceFindPreference;
        } else if (preferenceFindPreference instanceof DropDownPreference) {
            this.mDropDown = (DropDownPreference) preferenceFindPreference;
            CharSequence[] charSequenceArr = new CharSequence[this.mValues.length];
            CharSequence[] charSequenceArr2 = new CharSequence[this.mValues.length];
            for (int i = 0; i < this.mValues.length; i++) {
                charSequenceArr[i] = getCaption(activity.getResources(), this.mValues[i]);
                charSequenceArr2[i] = Integer.toString(this.mValues[i]);
            }
            this.mDropDown.setEntries(charSequenceArr);
            this.mDropDown.setEntryValues(charSequenceArr2);
        }
        update(activity);
        if (this.mTwoState != null) {
            preferenceFindPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object obj) {
                    SettingPref.this.setSetting(activity, ((Boolean) obj).booleanValue() ? 1 : 0);
                    return true;
                }
            });
            return this.mTwoState;
        }
        if (this.mDropDown == null) {
            return null;
        }
        preferenceFindPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                return SettingPref.this.setSetting(activity, Integer.parseInt((String) obj));
            }
        });
        return this.mDropDown;
    }

    protected boolean setSetting(Context context, int i) {
        return putInt(this.mType, context.getContentResolver(), this.mSetting, i);
    }

    public Uri getUri() {
        return this.mUri;
    }

    public String getKey() {
        return this.mKey;
    }

    public void update(Context context) {
        int i = getInt(this.mType, context.getContentResolver(), this.mSetting, this.mDefault);
        if (this.mTwoState != null) {
            this.mTwoState.setChecked(i != 0);
        } else if (this.mDropDown != null) {
            this.mDropDown.setValue(Integer.toString(i));
        }
    }

    private static Uri getUriFor(int i, String str) {
        switch (i) {
            case 1:
                return Settings.Global.getUriFor(str);
            case 2:
                return Settings.System.getUriFor(str);
            default:
                throw new IllegalArgumentException();
        }
    }

    protected static boolean putInt(int i, ContentResolver contentResolver, String str, int i2) {
        switch (i) {
            case 1:
                return Settings.Global.putInt(contentResolver, str, i2);
            case 2:
                return Settings.System.putInt(contentResolver, str, i2);
            default:
                throw new IllegalArgumentException();
        }
    }

    protected static int getInt(int i, ContentResolver contentResolver, String str, int i2) {
        switch (i) {
            case 1:
                return Settings.Global.getInt(contentResolver, str, i2);
            case 2:
                return Settings.System.getInt(contentResolver, str, i2);
            default:
                throw new IllegalArgumentException();
        }
    }
}
