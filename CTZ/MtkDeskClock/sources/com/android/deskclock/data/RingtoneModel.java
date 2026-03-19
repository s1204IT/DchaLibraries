package com.android.deskclock.data;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

final class RingtoneModel {
    private final Context mContext;
    private List<CustomRingtone> mCustomRingtones;
    private final BroadcastReceiver mLocaleChangedReceiver;
    private final SharedPreferences mPrefs;
    private final Map<Uri, String> mRingtoneTitles = new ArrayMap(16);

    RingtoneModel(Context context, SharedPreferences sharedPreferences) {
        this.mLocaleChangedReceiver = new LocaleChangedReceiver();
        this.mContext = context;
        this.mPrefs = sharedPreferences;
        this.mContext.getContentResolver().registerContentObserver(Settings.System.DEFAULT_ALARM_ALERT_URI, false, new SystemAlarmAlertChangeObserver());
        this.mContext.registerReceiver(this.mLocaleChangedReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
    }

    CustomRingtone addCustomRingtone(Uri uri, String str) {
        CustomRingtone customRingtone = getCustomRingtone(uri);
        if (customRingtone != null) {
            return customRingtone;
        }
        CustomRingtone customRingtoneAddCustomRingtone = CustomRingtoneDAO.addCustomRingtone(this.mPrefs, uri, str);
        getMutableCustomRingtones().add(customRingtoneAddCustomRingtone);
        Collections.sort(getMutableCustomRingtones());
        return customRingtoneAddCustomRingtone;
    }

    void removeCustomRingtone(Uri uri) {
        List<CustomRingtone> mutableCustomRingtones = getMutableCustomRingtones();
        for (CustomRingtone customRingtone : mutableCustomRingtones) {
            if (customRingtone.getUri().equals(uri)) {
                CustomRingtoneDAO.removeCustomRingtone(this.mPrefs, customRingtone.getId());
                mutableCustomRingtones.remove(customRingtone);
                return;
            }
        }
    }

    private CustomRingtone getCustomRingtone(Uri uri) {
        for (CustomRingtone customRingtone : getMutableCustomRingtones()) {
            if (customRingtone.getUri().equals(uri)) {
                return customRingtone;
            }
        }
        return null;
    }

    List<CustomRingtone> getCustomRingtones() {
        return Collections.unmodifiableList(getMutableCustomRingtones());
    }

    @SuppressLint({"NewApi"})
    void loadRingtonePermissions() {
        List<CustomRingtone> mutableCustomRingtones = getMutableCustomRingtones();
        if (mutableCustomRingtones.isEmpty()) {
            return;
        }
        List<UriPermission> persistedUriPermissions = this.mContext.getContentResolver().getPersistedUriPermissions();
        ArraySet arraySet = new ArraySet(persistedUriPermissions.size());
        Iterator<UriPermission> it = persistedUriPermissions.iterator();
        while (it.hasNext()) {
            arraySet.add(it.next().getUri());
        }
        ListIterator<CustomRingtone> listIterator = mutableCustomRingtones.listIterator();
        while (listIterator.hasNext()) {
            CustomRingtone next = listIterator.next();
            listIterator.set(next.setHasPermissions(arraySet.contains(next.getUri())));
        }
    }

    void loadRingtoneTitles() {
        if (!this.mRingtoneTitles.isEmpty()) {
            return;
        }
        RingtoneManager ringtoneManager = new RingtoneManager(this.mContext);
        ringtoneManager.setType(4);
        try {
            Cursor cursor = ringtoneManager.getCursor();
            Throwable th = null;
            try {
                try {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        String string = cursor.getString(1);
                        this.mRingtoneTitles.put(ringtoneManager.getRingtoneUri(cursor.getPosition()), string);
                        cursor.moveToNext();
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } finally {
            }
        } catch (Throwable th3) {
            LogUtils.e("Error loading ringtone title cache", th3);
        }
    }

    String getRingtoneTitle(Uri uri) {
        if (Alarm.NO_RINGTONE_URI.equals(uri)) {
            return this.mContext.getString(R.string.silent_ringtone_title);
        }
        CustomRingtone customRingtone = getCustomRingtone(uri);
        if (customRingtone != null) {
            return customRingtone.getTitle();
        }
        String str = this.mRingtoneTitles.get(uri);
        if (str == null) {
            Ringtone ringtone = RingtoneManager.getRingtone(this.mContext, uri);
            if (ringtone == null) {
                LogUtils.e("No ringtone for uri: %s", uri);
                return this.mContext.getString(R.string.unknown_ringtone_title);
            }
            String title = ringtone.getTitle(this.mContext);
            this.mRingtoneTitles.put(uri, title);
            return title;
        }
        return str;
    }

    private List<CustomRingtone> getMutableCustomRingtones() {
        if (this.mCustomRingtones == null) {
            this.mCustomRingtones = CustomRingtoneDAO.getCustomRingtones(this.mPrefs);
            Collections.sort(this.mCustomRingtones);
        }
        return this.mCustomRingtones;
    }

    private final class SystemAlarmAlertChangeObserver extends ContentObserver {
        private SystemAlarmAlertChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            RingtoneModel.this.mRingtoneTitles.clear();
        }
    }

    private final class LocaleChangedReceiver extends BroadcastReceiver {
        private LocaleChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            RingtoneModel.this.mRingtoneTitles.clear();
        }
    }
}
