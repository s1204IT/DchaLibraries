package com.android.contacts.common.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import com.android.contacts.R;

public final class ContactsPreferences implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context mContext;
    private final SharedPreferences mPreferences;
    private int mSortOrder = -1;
    private int mDisplayOrder = -1;
    private ChangeListener mListener = null;
    private Handler mHandler = new Handler();

    public interface ChangeListener {
        void onChange();
    }

    public ContactsPreferences(Context context) {
        this.mContext = context;
        this.mPreferences = this.mContext.getSharedPreferences(context.getPackageName(), 0);
        maybeMigrateSystemSettings();
    }

    public boolean isSortOrderUserChangeable() {
        return this.mContext.getResources().getBoolean(R.bool.config_sort_order_user_changeable);
    }

    public int getDefaultSortOrder() {
        return this.mContext.getResources().getBoolean(R.bool.config_default_sort_order_primary) ? 1 : 2;
    }

    public int getSortOrder() {
        if (!isSortOrderUserChangeable()) {
            return getDefaultSortOrder();
        }
        if (this.mSortOrder == -1) {
            this.mSortOrder = this.mPreferences.getInt("android.contacts.SORT_ORDER", getDefaultSortOrder());
        }
        return this.mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.mSortOrder = sortOrder;
        SharedPreferences.Editor editor = this.mPreferences.edit();
        editor.putInt("android.contacts.SORT_ORDER", sortOrder);
        editor.commit();
    }

    public boolean isDisplayOrderUserChangeable() {
        return this.mContext.getResources().getBoolean(R.bool.config_display_order_user_changeable);
    }

    public int getDefaultDisplayOrder() {
        return this.mContext.getResources().getBoolean(R.bool.config_default_display_order_primary) ? 1 : 2;
    }

    public int getDisplayOrder() {
        if (!isDisplayOrderUserChangeable()) {
            return getDefaultDisplayOrder();
        }
        if (this.mDisplayOrder == -1) {
            this.mDisplayOrder = this.mPreferences.getInt("android.contacts.DISPLAY_ORDER", getDefaultDisplayOrder());
        }
        return this.mDisplayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.mDisplayOrder = displayOrder;
        SharedPreferences.Editor editor = this.mPreferences.edit();
        editor.putInt("android.contacts.DISPLAY_ORDER", displayOrder);
        editor.commit();
    }

    public void registerChangeListener(ChangeListener listener) {
        if (this.mListener != null) {
            unregisterChangeListener();
        }
        this.mListener = listener;
        this.mDisplayOrder = -1;
        this.mSortOrder = -1;
        this.mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void unregisterChangeListener() {
        if (this.mListener != null) {
            this.mListener = null;
        }
        this.mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ("android.contacts.DISPLAY_ORDER".equals(key)) {
                    ContactsPreferences.this.mDisplayOrder = ContactsPreferences.this.getDisplayOrder();
                } else if ("android.contacts.SORT_ORDER".equals(key)) {
                    ContactsPreferences.this.mSortOrder = ContactsPreferences.this.getSortOrder();
                }
                if (ContactsPreferences.this.mListener != null) {
                    ContactsPreferences.this.mListener.onChange();
                }
            }
        });
    }

    private void maybeMigrateSystemSettings() {
        if (!this.mPreferences.contains("android.contacts.SORT_ORDER")) {
            int sortOrder = getDefaultSortOrder();
            try {
                sortOrder = Settings.System.getInt(this.mContext.getContentResolver(), "android.contacts.SORT_ORDER");
            } catch (Settings.SettingNotFoundException e) {
            }
            setSortOrder(sortOrder);
        }
        if (!this.mPreferences.contains("android.contacts.DISPLAY_ORDER")) {
            int displayOrder = getDefaultDisplayOrder();
            try {
                displayOrder = Settings.System.getInt(this.mContext.getContentResolver(), "android.contacts.DISPLAY_ORDER");
            } catch (Settings.SettingNotFoundException e2) {
            }
            setDisplayOrder(displayOrder);
        }
    }
}
