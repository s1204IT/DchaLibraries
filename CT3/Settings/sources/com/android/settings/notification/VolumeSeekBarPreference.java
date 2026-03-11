package com.android.settings.notification;

import android.content.Context;
import android.net.Uri;
import android.preference.SeekBarVolumizer;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SeekBarPreference;
import java.util.Objects;

public class VolumeSeekBarPreference extends SeekBarPreference {
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

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumeSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setStream(int stream) {
        this.mStream = stream;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void onActivityResume() {
        if (!this.mStopped) {
            return;
        }
        init();
    }

    public void onActivityPause() {
        this.mStopped = true;
        if (this.mVolumizer == null) {
            return;
        }
        this.mVolumizer.stop();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (this.mStream == 0) {
            Log.w("VolumeSeekBarPreference", "No stream found, not binding volumizer");
            return;
        }
        this.mSeekBar = (SeekBar) view.findViewById(android.R.id.locked);
        this.mIconView = (ImageView) view.findViewById(android.R.id.icon);
        this.mSuppressionTextView = (TextView) view.findViewById(R.id.suppression_text);
        init();
    }

    private void init() {
        if (this.mSeekBar == null) {
            return;
        }
        SeekBarVolumizer.Callback sbvc = new SeekBarVolumizer.Callback() {
            public void onSampleStarting(SeekBarVolumizer sbv) {
                if (VolumeSeekBarPreference.this.mCallback == null) {
                    return;
                }
                VolumeSeekBarPreference.this.mCallback.onSampleStarting(sbv);
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (VolumeSeekBarPreference.this.mCallback == null) {
                    return;
                }
                VolumeSeekBarPreference.this.mCallback.onStreamValueChanged(VolumeSeekBarPreference.this.mStream, progress);
            }

            public void onMuted(boolean muted, boolean zenMuted) {
                if (VolumeSeekBarPreference.this.mMuted == muted && VolumeSeekBarPreference.this.mZenMuted == zenMuted) {
                    return;
                }
                VolumeSeekBarPreference.this.mMuted = muted;
                VolumeSeekBarPreference.this.mZenMuted = zenMuted;
                VolumeSeekBarPreference.this.updateIconView();
            }
        };
        Uri mediaVolumeUri = this.mStream == 3 ? getMediaVolumeUri() : null;
        if (this.mVolumizer == null) {
            this.mVolumizer = new SeekBarVolumizer(getContext(), this.mStream, mediaVolumeUri, sbvc);
        }
        this.mVolumizer.start();
        this.mVolumizer.setSeekBar(this.mSeekBar);
        updateIconView();
        this.mCallback.onStreamValueChanged(this.mStream, this.mSeekBar.getProgress());
        updateSuppressionText();
        if (isEnabled()) {
            return;
        }
        this.mSeekBar.setEnabled(false);
        this.mVolumizer.stop();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        this.mCallback.onStreamValueChanged(this.mStream, progress);
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

    public void showIcon(int resId) {
        if (this.mIconResId == resId) {
            return;
        }
        this.mIconResId = resId;
        updateIconView();
    }

    public void setMuteIcon(int resId) {
        if (this.mMuteIconResId == resId) {
            return;
        }
        this.mMuteIconResId = resId;
        updateIconView();
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.media_volume);
    }

    public void setSuppressionText(String text) {
        if (Objects.equals(text, this.mSuppressionText)) {
            return;
        }
        this.mSuppressionText = text;
        updateSuppressionText();
    }

    private void updateSuppressionText() {
        if (this.mSuppressionTextView == null || this.mSeekBar == null) {
            return;
        }
        this.mSuppressionTextView.setText(this.mSuppressionText);
        boolean showSuppression = !TextUtils.isEmpty(this.mSuppressionText);
        this.mSuppressionTextView.setVisibility(showSuppression ? 0 : 4);
        this.mSeekBar.setVisibility(showSuppression ? 4 : 0);
    }
}
