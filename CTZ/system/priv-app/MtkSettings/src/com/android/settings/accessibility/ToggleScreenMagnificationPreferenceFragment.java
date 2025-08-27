package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.VideoView;
import com.android.settings.R;
import com.android.settings.widget.SwitchBar;

/* loaded from: classes.dex */
public class ToggleScreenMagnificationPreferenceFragment extends ToggleFeaturePreferenceFragment implements SwitchBar.OnSwitchChangeListener {
    protected Preference mConfigWarningPreference;
    protected VideoPreference mVideoPreference;
    private boolean mLaunchFromSuw = false;
    private boolean mInitialSetting = false;

    protected class VideoPreference extends Preference {
        private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;
        private ImageView mVideoBackgroundView;

        public VideoPreference(Context context) {
            super(context);
        }

        @Override // android.support.v7.preference.Preference
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) throws Resources.NotFoundException {
            super.onBindViewHolder(preferenceViewHolder);
            Resources resources = ToggleScreenMagnificationPreferenceFragment.this.getPrefContext().getResources();
            final int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.screen_magnification_video_background_width);
            final int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.screen_magnification_video_width);
            final int dimensionPixelSize3 = resources.getDimensionPixelSize(R.dimen.screen_magnification_video_height);
            final int dimensionPixelSize4 = resources.getDimensionPixelSize(R.dimen.screen_magnification_video_margin_top);
            preferenceViewHolder.setDividerAllowedAbove(false);
            preferenceViewHolder.setDividerAllowedBelow(false);
            this.mVideoBackgroundView = (ImageView) preferenceViewHolder.findViewById(R.id.video_background);
            final VideoView videoView = (VideoView) preferenceViewHolder.findViewById(R.id.video);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { // from class: com.android.settings.accessibility.ToggleScreenMagnificationPreferenceFragment.VideoPreference.1
                @Override // android.media.MediaPlayer.OnPreparedListener
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                }
            });
            videoView.setAudioFocusRequest(0);
            Bundle arguments = ToggleScreenMagnificationPreferenceFragment.this.getArguments();
            if (arguments != null && arguments.containsKey("video_resource")) {
                videoView.setVideoURI(Uri.parse(String.format("%s://%s/%s", "android.resource", ToggleScreenMagnificationPreferenceFragment.this.getPrefContext().getPackageName(), Integer.valueOf(arguments.getInt("video_resource")))));
            }
            videoView.setMediaController(null);
            this.mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.settings.accessibility.ToggleScreenMagnificationPreferenceFragment.VideoPreference.2
                @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                public void onGlobalLayout() {
                    int width = VideoPreference.this.mVideoBackgroundView.getWidth();
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) videoView.getLayoutParams();
                    layoutParams.width = (dimensionPixelSize2 * width) / dimensionPixelSize;
                    layoutParams.height = (dimensionPixelSize3 * width) / dimensionPixelSize;
                    layoutParams.setMargins(0, (dimensionPixelSize4 * width) / dimensionPixelSize, 0, 0);
                    videoView.setLayoutParams(layoutParams);
                    videoView.invalidate();
                    videoView.start();
                }
            };
            this.mVideoBackgroundView.getViewTreeObserver().addOnGlobalLayoutListener(this.mLayoutListener);
        }

        @Override // android.support.v7.preference.Preference
        protected void onPrepareForRemoval() {
            this.mVideoBackgroundView.getViewTreeObserver().removeOnGlobalLayoutListener(this.mLayoutListener);
        }
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mVideoPreference = new VideoPreference(getPrefContext());
        this.mVideoPreference.setSelectable(false);
        this.mVideoPreference.setPersistent(false);
        this.mVideoPreference.setLayoutResource(R.layout.magnification_video_preference);
        this.mConfigWarningPreference = new Preference(getPrefContext());
        this.mConfigWarningPreference.setSelectable(false);
        this.mConfigWarningPreference.setPersistent(false);
        this.mConfigWarningPreference.setVisible(false);
        this.mConfigWarningPreference.setIcon(R.drawable.ic_warning_24dp);
        PreferenceScreen preferenceScreen = getPreferenceManager().getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        this.mVideoPreference.setOrder(0);
        this.mConfigWarningPreference.setOrder(2);
        preferenceScreen.addPreference(this.mVideoPreference);
        preferenceScreen.addPreference(this.mConfigWarningPreference);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        VideoView videoView = (VideoView) getView().findViewById(R.id.video);
        if (videoView != null) {
            videoView.start();
        }
        updateConfigurationWarningIfNeeded();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 7;
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(Switch r1, boolean z) {
        onPreferenceToggled(this.mPreferenceKey, z);
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onPreferenceToggled(String str, boolean z) {
        MagnificationPreferenceFragment.setChecked(getContentResolver(), str, z);
        updateConfigurationWarningIfNeeded();
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mSwitchBar.setCheckedInternal(MagnificationPreferenceFragment.isChecked(getContentResolver(), this.mPreferenceKey));
        this.mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onProcessArguments(Bundle bundle) {
        int i;
        super.onProcessArguments(bundle);
        if (bundle == null) {
            return;
        }
        if (bundle.containsKey("video_resource")) {
            this.mVideoPreference.setVisible(true);
            bundle.getInt("video_resource");
        } else {
            this.mVideoPreference.setVisible(false);
        }
        if (bundle.containsKey("from_suw")) {
            this.mLaunchFromSuw = bundle.getBoolean("from_suw");
        }
        if (bundle.containsKey("checked")) {
            this.mInitialSetting = bundle.getBoolean("checked");
        }
        if (bundle.containsKey("title_res") && (i = bundle.getInt("title_res")) > 0) {
            getActivity().setTitle(i);
        }
    }

    private void updateConfigurationWarningIfNeeded() {
        CharSequence configurationWarningStringForSecureSettingsKey = MagnificationPreferenceFragment.getConfigurationWarningStringForSecureSettingsKey(this.mPreferenceKey, getPrefContext());
        if (configurationWarningStringForSecureSettingsKey != null) {
            this.mConfigWarningPreference.setSummary(configurationWarningStringForSecureSettingsKey);
        }
        this.mConfigWarningPreference.setVisible(configurationWarningStringForSecureSettingsKey != null);
    }
}
