package com.android.deskclock.settings;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.android.deskclock.R;
import com.android.deskclock.RingtonePreviewKlaxon;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

public class AlarmVolumePreference extends Preference {
    private static final long ALARM_PREVIEW_DURATION_MS = 2000;
    private ImageView mAlarmIcon;
    private boolean mPreviewPlaying;
    private SeekBar mSeekbar;

    public AlarmVolumePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        final Context context = getContext();
        final AudioManager audioManager = (AudioManager) context.getSystemService("audio");
        preferenceViewHolder.itemView.setClickable(false);
        this.mSeekbar = (SeekBar) preferenceViewHolder.findViewById(R.id.alarm_volume_slider);
        this.mSeekbar.setMax(audioManager.getStreamMaxVolume(4));
        this.mSeekbar.setProgress(audioManager.getStreamVolume(4));
        this.mAlarmIcon = (ImageView) preferenceViewHolder.findViewById(R.id.alarm_icon);
        onSeekbarChanged();
        final ContentObserver contentObserver = new ContentObserver(this.mSeekbar.getHandler()) {
            @Override
            public void onChange(boolean z) {
                AlarmVolumePreference.this.mSeekbar.setProgress(audioManager.getStreamVolume(4));
            }
        };
        this.mSeekbar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                context.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, contentObserver);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                context.getContentResolver().unregisterContentObserver(contentObserver);
            }
        });
        this.mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (z) {
                    audioManager.setStreamVolume(4, i, 0);
                }
                AlarmVolumePreference.this.onSeekbarChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!AlarmVolumePreference.this.mPreviewPlaying && seekBar.getProgress() != 0) {
                    RingtonePreviewKlaxon.start(context, DataModel.getDataModel().getDefaultAlarmRingtoneUri());
                    AlarmVolumePreference.this.mPreviewPlaying = true;
                    seekBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            RingtonePreviewKlaxon.stop(context);
                            AlarmVolumePreference.this.mPreviewPlaying = false;
                        }
                    }, AlarmVolumePreference.ALARM_PREVIEW_DURATION_MS);
                }
            }
        });
    }

    private void onSeekbarChanged() {
        this.mSeekbar.setEnabled(doesDoNotDisturbAllowAlarmPlayback());
        this.mAlarmIcon.setImageResource(this.mSeekbar.getProgress() == 0 ? R.drawable.ic_alarm_off_24dp : R.drawable.ic_alarm_small);
    }

    private boolean doesDoNotDisturbAllowAlarmPlayback() {
        return !Utils.isNOrLater() || doesDoNotDisturbAllowAlarmPlaybackNPlus();
    }

    @TargetApi(24)
    private boolean doesDoNotDisturbAllowAlarmPlaybackNPlus() {
        return ((NotificationManager) getContext().getSystemService("notification")).getCurrentInterruptionFilter() != 3;
    }
}
