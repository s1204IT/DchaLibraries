package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.SeekBarVolumizer;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import java.util.Objects;

public class VolumeSeekBarPreference extends SeekBarPreference {
    AudioManager mAudioManager;
    private Callback mCallback;
    private int mIconResId;
    private ImageView mIconView;
    private int mMuteIconResId;
    private boolean mMuted;
    private SeekBar mSeekBar;
    private boolean mStopped;
    private int mStream;
    private String mSuppressionText;
    private TextView mSuppressionTextView;
    private SeekBarVolumizer mVolumizer;
    private boolean mZenMuted;

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);

        void onStreamValueChanged(int i, int i2);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setLayoutResource(R.layout.preference_volume_slider);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setLayoutResource(R.layout.preference_volume_slider);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.preference_volume_slider);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public VolumeSeekBarPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_volume_slider);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public void setStream(int i) {
        this.mStream = i;
        setMax(this.mAudioManager.getStreamMaxVolume(this.mStream));
        setMin(this.mAudioManager.getStreamMinVolumeInt(this.mStream));
        setProgress(this.mAudioManager.getStreamVolume(this.mStream));
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void onActivityResume() {
        if (this.mStopped) {
            init();
        }
    }

    public void onActivityPause() {
        this.mStopped = true;
        if (this.mVolumizer != null) {
            this.mVolumizer.stop();
            this.mVolumizer = null;
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mSeekBar = (SeekBar) preferenceViewHolder.findViewById(android.R.id.matrix);
        this.mIconView = (ImageView) preferenceViewHolder.findViewById(android.R.id.icon);
        this.mSuppressionTextView = (TextView) preferenceViewHolder.findViewById(R.id.suppression_text);
        init();
    }

    private void init() {
        if (this.mSeekBar == null) {
            return;
        }
        SeekBarVolumizer.Callback callback = new SeekBarVolumizer.Callback() {
            public void onSampleStarting(SeekBarVolumizer seekBarVolumizer) {
                if (VolumeSeekBarPreference.this.mCallback != null) {
                    VolumeSeekBarPreference.this.mCallback.onSampleStarting(seekBarVolumizer);
                }
            }

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (VolumeSeekBarPreference.this.mCallback != null) {
                    VolumeSeekBarPreference.this.mCallback.onStreamValueChanged(VolumeSeekBarPreference.this.mStream, i);
                }
            }

            public void onMuted(boolean z, boolean z2) {
                if (VolumeSeekBarPreference.this.mMuted == z && VolumeSeekBarPreference.this.mZenMuted == z2) {
                    return;
                }
                VolumeSeekBarPreference.this.mMuted = z;
                VolumeSeekBarPreference.this.mZenMuted = z2;
                VolumeSeekBarPreference.this.updateIconView();
            }
        };
        Uri mediaVolumeUri = this.mStream == 3 ? getMediaVolumeUri() : null;
        if (this.mVolumizer == null) {
            this.mVolumizer = new SeekBarVolumizer(getContext(), this.mStream, mediaVolumeUri, callback);
        }
        this.mVolumizer.start();
        this.mVolumizer.setSeekBar(this.mSeekBar);
        updateIconView();
        updateSuppressionText();
        if (!isEnabled()) {
            this.mSeekBar.setEnabled(false);
            this.mVolumizer.stop();
        }
    }

    public void updateIconView() {
        if (this.mIconView == null) {
            return;
        }
        if (this.mIconResId != 0) {
            this.mIconView.setImageResource(this.mIconResId);
        } else if (this.mMuteIconResId != 0 && this.mMuted && !this.mZenMuted) {
            this.mIconView.setImageResource(this.mMuteIconResId);
        } else {
            this.mIconView.setImageDrawable(getIcon());
        }
    }

    public void showIcon(int i) {
        if (this.mIconResId == i) {
            return;
        }
        this.mIconResId = i;
        updateIconView();
    }

    public void setMuteIcon(int i) {
        if (this.mMuteIconResId == i) {
            return;
        }
        this.mMuteIconResId = i;
        updateIconView();
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.media_volume);
    }

    public void setSuppressionText(String str) {
        if (Objects.equals(str, this.mSuppressionText)) {
            return;
        }
        this.mSuppressionText = str;
        updateSuppressionText();
    }

    private void updateSuppressionText() {
        if (this.mSuppressionTextView != null && this.mSeekBar != null) {
            this.mSuppressionTextView.setText(this.mSuppressionText);
            this.mSuppressionTextView.setVisibility(TextUtils.isEmpty(this.mSuppressionText) ^ true ? 0 : 8);
        }
    }
}
