package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class SettingPref {
    protected final int mDefault;
    protected DropDownPreference mDropDown;
    private final String mKey;
    protected final String mSetting;
    protected TwoStatePreference mTwoState;
    protected final int mType;
    private final Uri mUri;
    private final int[] mValues;

    public SettingPref(int type, String key, String setting, int def, int... values) {
        this.mType = type;
        this.mKey = key;
        this.mSetting = setting;
        this.mDefault = def;
        this.mValues = values;
        this.mUri = getUriFor(this.mType, this.mSetting);
    }

    public boolean isApplicable(Context context) {
        return true;
    }

    protected String getCaption(Resources res, int value) {
        throw new UnsupportedOperationException();
    }

    public Preference init(SettingsPreferenceFragment settings) {
        final Context context = settings.getActivity();
        Preference p = settings.getPreferenceScreen().findPreference(this.mKey);
        if (p != null && !isApplicable(context)) {
            settings.getPreferenceScreen().removePreference(p);
            p = null;
        }
        if (p instanceof TwoStatePreference) {
            this.mTwoState = (TwoStatePreference) p;
        } else if (p instanceof DropDownPreference) {
            this.mDropDown = (DropDownPreference) p;
            CharSequence[] entries = new CharSequence[this.mValues.length];
            CharSequence[] values = new CharSequence[this.mValues.length];
            for (int i = 0; i < this.mValues.length; i++) {
                entries[i] = getCaption(context.getResources(), this.mValues[i]);
                values[i] = Integer.toString(this.mValues[i]);
            }
            this.mDropDown.setEntries(entries);
            this.mDropDown.setEntryValues(values);
        }
        update(context);
        if (this.mTwoState != null) {
            p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SettingPref.this.setSetting(context, ((Boolean) newValue).booleanValue() ? 1 : 0);
                    return true;
                }
            });
            return this.mTwoState;
        }
        if (this.mDropDown == null) {
            return null;
        }
        p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return SettingPref.this.setSetting(context, Integer.parseInt((String) newValue));
            }
        });
        return this.mDropDown;
    }

    protected boolean setSetting(Context context, int value) {
        return putInt(this.mType, context.getContentResolver(), this.mSetting, value);
    }

    public Uri getUri() {
        return this.mUri;
    }

    public String getKey() {
        return this.mKey;
    }

    public void update(Context context) {
        int val = getInt(this.mType, context.getContentResolver(), this.mSetting, this.mDefault);
        if (this.mTwoState != null) {
            this.mTwoState.setChecked(val != 0);
        } else {
            if (this.mDropDown == null) {
                return;
            }
            this.mDropDown.setValue(Integer.toString(val));
        }
    }

    private static Uri getUriFor(int type, String setting) {
        switch (type) {
            case DefaultWfcSettingsExt.PAUSE:
                return Settings.Global.getUriFor(setting);
            case DefaultWfcSettingsExt.CREATE:
                return Settings.System.getUriFor(setting);
            default:
                throw new IllegalArgumentException();
        }
    }

    protected static boolean putInt(int type, ContentResolver cr, String setting, int value) {
        switch (type) {
            case DefaultWfcSettingsExt.PAUSE:
                return Settings.Global.putInt(cr, setting, value);
            case DefaultWfcSettingsExt.CREATE:
                return Settings.System.putInt(cr, setting, value);
            default:
                throw new IllegalArgumentException();
        }
    }

    protected static int getInt(int type, ContentResolver cr, String setting, int def) {
        switch (type) {
            case DefaultWfcSettingsExt.PAUSE:
                return Settings.Global.getInt(cr, setting, def);
            case DefaultWfcSettingsExt.CREATE:
                return Settings.System.getInt(cr, setting, def);
            default:
                throw new IllegalArgumentException();
        }
    }
}
