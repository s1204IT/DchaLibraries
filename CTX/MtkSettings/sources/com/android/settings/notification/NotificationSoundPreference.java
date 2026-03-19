package com.android.settings.notification;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import com.android.settings.RingtonePreference;

public class NotificationSoundPreference extends RingtonePreference {
    private Uri mRingtone;

    public NotificationSoundPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return this.mRingtone;
    }

    public void setRingtone(Uri uri) {
        this.mRingtone = uri;
        setSummary(" ");
        updateRingtoneName(this.mRingtone);
    }

    @Override
    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (intent != null) {
            Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI");
            setRingtone(uri);
            callChangeListener(uri);
            return true;
        }
        return true;
    }

    private void updateRingtoneName(final Uri uri) {
        new AsyncTask<Object, Void, CharSequence>() {
            @Override
            protected CharSequence doInBackground(Object... objArr) {
                if (uri == null) {
                    return NotificationSoundPreference.this.getContext().getString(R.string.launch_warning_replace);
                }
                if (RingtoneManager.isDefault(uri)) {
                    return NotificationSoundPreference.this.getContext().getString(com.android.settings.R.string.notification_sound_default);
                }
                if ("android.resource".equals(uri.getScheme())) {
                    return NotificationSoundPreference.this.getContext().getString(com.android.settings.R.string.notification_unknown_sound_title);
                }
                return Ringtone.getTitle(NotificationSoundPreference.this.getContext(), uri, false, true);
            }

            @Override
            protected void onPostExecute(CharSequence charSequence) {
                NotificationSoundPreference.this.setSummary(charSequence);
            }
        }.execute(new Object[0]);
    }
}
