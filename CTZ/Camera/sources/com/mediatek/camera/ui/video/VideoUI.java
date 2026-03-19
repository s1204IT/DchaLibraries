package com.mediatek.camera.ui.video;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.video.videoui.IVideoUI;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.RotateStrokeTextView;
import com.mediatek.camera.common.widget.ScaleAnimationButton;
import com.mediatek.camera.common.widget.StrokeTextView;
import java.util.Locale;

public class VideoUI implements IVideoUI {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoUI.class.getSimpleName());
    private Activity mActivity;
    private IApp mApp;
    private RotateStrokeTextView mCurrentRecordingSizeView;
    private final Handler mMainHandler;
    private ViewGroup mParentViewGroup;
    private ScaleAnimationButton mPauseResumeButton;
    private ImageView mRecordingIndicator;
    private SeekBar mRecordingSizeSeekBar;
    private RotateStrokeTextView mRecordingSizeTotalView;
    private View mRecordingSizeViewGroup;
    private StrokeTextView mRecordingTimeView;
    private View mRecordingTimeViewGroup;
    private ScaleAnimationButton mStopButton;
    private IVideoUI.UISpec mUISpec;
    private ScaleAnimationButton mVssButton;
    private int mShowRecordingTimeViewIndicator = 0;
    private long mRecordingPausedDuration = 0;
    private long mRecordingTotalDuration = 0;
    private long mRecordingStartTime = 0;
    private boolean mIsInRecording = false;
    private View mRecordingRootView = null;
    private IVideoUI.VideoUIState mUIState = IVideoUI.VideoUIState.STATE_PREVIEW;
    private IAppUi.HintInfo mVideoErrorHint = new IAppUi.HintInfo();

    public VideoUI(IApp iApp, ViewGroup viewGroup) {
        this.mApp = iApp;
        this.mActivity = iApp.getActivity();
        this.mParentViewGroup = viewGroup;
        this.mMainHandler = new RecordingHandler(this.mActivity.getMainLooper());
        int identifier = iApp.getActivity().getResources().getIdentifier("hint_text_background", "drawable", iApp.getActivity().getPackageName());
        this.mVideoErrorHint.mBackground = iApp.getActivity().getDrawable(identifier);
        this.mVideoErrorHint.mType = IAppUi.HintType.TYPE_AUTO_HIDE;
        this.mVideoErrorHint.mDelayTime = 3000;
    }

    @Override
    public void initVideoUI(IVideoUI.UISpec uISpec) {
        this.mUISpec = uISpec;
    }

    static class AnonymousClass4 {
        static final int[] $SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState = new int[IVideoUI.VideoUIState.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[IVideoUI.VideoUIState.STATE_PREVIEW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[IVideoUI.VideoUIState.STATE_PRE_RECORDING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[IVideoUI.VideoUIState.STATE_RECORDING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[IVideoUI.VideoUIState.STATE_PAUSE_RECORDING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[IVideoUI.VideoUIState.STATE_RESUME_RECORDING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    @Override
    public void updateUIState(IVideoUI.VideoUIState videoUIState) {
        LogHelper.i(TAG, "[updateUIState] mUIState = " + this.mUIState + " new state = " + videoUIState);
        switch (AnonymousClass4.$SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[videoUIState.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                doUpdateUI(videoUIState);
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (this.mUIState == IVideoUI.VideoUIState.STATE_PREVIEW) {
                    doUpdateUI(videoUIState);
                }
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                if (this.mUIState == IVideoUI.VideoUIState.STATE_PRE_RECORDING) {
                    doUpdateUI(videoUIState);
                }
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                if (this.mUIState == IVideoUI.VideoUIState.STATE_RECORDING) {
                    doUpdateUI(videoUIState);
                }
                break;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                if (this.mUIState == IVideoUI.VideoUIState.STATE_PAUSE_RECORDING) {
                    doUpdateUI(videoUIState);
                }
                break;
        }
    }

    @Override
    public void updateOrientation(int i) {
        LogHelper.d(TAG, "[updateOrientation] orientation = " + i + " mUIState = " + this.mUIState);
        if (this.mUIState == IVideoUI.VideoUIState.STATE_PREVIEW) {
            return;
        }
        if (i == 0 || i == 90 || i == 180 || i == 270) {
            CameraUtil.rotateRotateLayoutChildView(this.mActivity, this.mRecordingRootView, i, true);
            return;
        }
        LogHelper.e(TAG, "error orientation = " + i);
    }

    @Override
    public void updateRecordingSize(final long j) {
        LogHelper.d(TAG, "[updateRecordingSize] mUIState = " + this.mUIState);
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (VideoUI.this.mUIState == IVideoUI.VideoUIState.STATE_PREVIEW) {
                    return;
                }
                if (j < 0 || j > VideoUI.this.mUISpec.recordingTotalSize) {
                    LogHelper.e(VideoUI.TAG, "[updateRecordingSize] size = " + j);
                    return;
                }
                int i = (int) ((j * 100) / VideoUI.this.mUISpec.recordingTotalSize);
                if (100 >= i) {
                    VideoUI.this.mCurrentRecordingSizeView.setText(VideoUI.this.formatFileSize(j));
                    VideoUI.this.mRecordingSizeSeekBar.setProgress(i);
                }
            }
        });
    }

    @Override
    public void unInitVideoUI() {
        this.mParentViewGroup.removeView(this.mRecordingRootView);
        this.mRecordingRootView = null;
        this.mUISpec = null;
    }

    @Override
    public void showInfo(int i) {
        LogHelper.i(TAG, "[showInfo] infoId = " + i);
        switch (i) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                this.mVideoErrorHint.mHintText = this.mActivity.getString(R.string.video_bad_performance_auto_stop);
                this.mApp.getAppUi().showScreenHint(this.mVideoErrorHint);
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mVideoErrorHint.mHintText = this.mActivity.getString(R.string.video_reach_size_limit);
                this.mApp.getAppUi().showScreenHint(this.mVideoErrorHint);
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                this.mVideoErrorHint.mHintText = this.mActivity.getString(R.string.video_recording_error);
                this.mApp.getAppUi().showScreenHint(this.mVideoErrorHint);
                break;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                this.mVideoErrorHint.mHintText = this.mActivity.getString(R.string.video_reach_size_limit);
                this.mApp.getAppUi().showScreenHint(this.mVideoErrorHint);
                break;
        }
    }

    private void doUpdateUI(IVideoUI.VideoUIState videoUIState) {
        this.mUIState = videoUIState;
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VideoUI.this.updateUI();
            }
        });
    }

    private class RecordingHandler extends Handler {
        RecordingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                VideoUI.this.updateRecordingTime();
            }
        }
    }

    private void updateRecordingTime() {
        if (!this.mIsInRecording) {
            return;
        }
        this.mRecordingTotalDuration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
        if (IVideoUI.VideoUIState.STATE_PAUSE_RECORDING == this.mUIState) {
            this.mRecordingTotalDuration = this.mRecordingPausedDuration;
        }
        showTime(this.mRecordingTotalDuration, false);
        this.mShowRecordingTimeViewIndicator = 1 - this.mShowRecordingTimeViewIndicator;
        if (IVideoUI.VideoUIState.STATE_PAUSE_RECORDING == this.mUIState && 1 == this.mShowRecordingTimeViewIndicator) {
            this.mRecordingTimeViewGroup.setVisibility(4);
        } else {
            this.mRecordingTimeViewGroup.setVisibility(0);
        }
        long j = 500;
        if (IVideoUI.VideoUIState.STATE_PAUSE_RECORDING != this.mUIState) {
            j = 1000 - (this.mRecordingTotalDuration % 1000);
        }
        this.mMainHandler.sendEmptyMessageDelayed(0, j);
    }

    private void showTime(long j, boolean z) {
        String time = formatTime(j, z);
        if (this.mRecordingTimeView != null) {
            this.mRecordingTimeView.setText(time);
        }
    }

    private String formatTime(long j, boolean z) {
        int i = ((int) j) / 1000;
        int i2 = ((int) (j % 1000)) / 10;
        int i3 = i % 60;
        int i4 = (i / 60) % 60;
        int i5 = i / 3600;
        if (!z) {
            return i5 > 0 ? String.format(Locale.ENGLISH, "%d:%02d:%02d", Integer.valueOf(i5), Integer.valueOf(i4), Integer.valueOf(i3)) : String.format(Locale.ENGLISH, "%02d:%02d", Integer.valueOf(i4), Integer.valueOf(i3));
        }
        if (i5 > 0) {
            return String.format(Locale.ENGLISH, "%d:%02d:%02d.%02d", Integer.valueOf(i5), Integer.valueOf(i4), Integer.valueOf(i3), Integer.valueOf(i2));
        }
        return String.format(Locale.ENGLISH, "%02d:%02d.%02d", Integer.valueOf(i4), Integer.valueOf(i3), Integer.valueOf(i2));
    }

    private void updateUI() {
        switch (AnonymousClass4.$SwitchMap$com$mediatek$camera$common$mode$video$videoui$IVideoUI$VideoUIState[this.mUIState.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                this.mIsInRecording = false;
                this.mMainHandler.removeMessages(0);
                hide();
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mRecordingStartTime = SystemClock.uptimeMillis();
                this.mRecordingPausedDuration = 0L;
                show();
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mIsInRecording = true;
                this.mRecordingStartTime = SystemClock.uptimeMillis();
                updateRecordingTime();
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                this.mRecordingPausedDuration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
                updateRecordingViewIcon();
                break;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                this.mRecordingStartTime = SystemClock.uptimeMillis() - this.mRecordingPausedDuration;
                this.mRecordingPausedDuration = 0L;
                this.mUIState = IVideoUI.VideoUIState.STATE_RECORDING;
                updateRecordingViewIcon();
                break;
        }
    }

    private void hide() {
        if (this.mRecordingRootView == null) {
            return;
        }
        this.mMainHandler.removeMessages(0);
        this.mRecordingRootView.setVisibility(4);
        this.mRecordingTimeViewGroup.setVisibility(4);
        this.mRecordingTimeView.setVisibility(4);
        this.mPauseResumeButton.setVisibility(4);
        this.mRecordingSizeViewGroup.setVisibility(4);
        this.mParentViewGroup.removeView(this.mRecordingRootView);
        this.mRecordingRootView = null;
    }

    private void show() {
        LogHelper.d(TAG, "[show] + mRecordingRootView = " + this.mRecordingRootView);
        if (this.mRecordingRootView == null) {
            this.mRecordingRootView = getView();
        }
        updateRecordingViewIcon();
        this.mRecordingRootView.setVisibility(0);
        this.mRecordingTimeViewGroup.setVisibility(0);
        this.mRecordingTimeView.setText(formatTime(0L, false));
        this.mRecordingTimeView.setVisibility(0);
        if (this.mUISpec.isSupportedPause) {
            this.mPauseResumeButton.setVisibility(0);
        } else {
            this.mPauseResumeButton.setVisibility(8);
        }
        if (this.mUISpec.isSupportedVss) {
            this.mVssButton.setVisibility(0);
        } else {
            this.mVssButton.setVisibility(8);
        }
        if (this.mUISpec.recordingTotalSize > 0) {
            this.mCurrentRecordingSizeView.setText("0");
            this.mRecordingSizeSeekBar.setProgress(0);
            this.mRecordingSizeTotalView.setText(formatFileSize(this.mUISpec.recordingTotalSize));
            this.mRecordingSizeViewGroup.setVisibility(0);
        } else {
            this.mRecordingSizeViewGroup.setVisibility(8);
        }
        LogHelper.d(TAG, "[show] - ");
    }

    private String formatFileSize(long j) {
        return (j / 1024) + "K";
    }

    private View getView() {
        LogHelper.d(TAG, "[getView] +");
        View viewInflate = this.mActivity.getLayoutInflater().inflate(R.layout.recording, this.mParentViewGroup, true);
        View viewFindViewById = viewInflate.findViewById(R.id.recording_root_group);
        this.mRecordingTimeViewGroup = viewInflate.findViewById(R.id.recording_time_group);
        this.mRecordingTimeView = (StrokeTextView) viewInflate.findViewById(R.id.recording_time);
        this.mRecordingIndicator = (ImageView) viewInflate.findViewById(R.id.recording_indicator);
        this.mPauseResumeButton = (ScaleAnimationButton) viewInflate.findViewById(R.id.btn_pause_resume);
        this.mPauseResumeButton.setOnClickListener(this.mUISpec.pauseResumeListener);
        this.mStopButton = (ScaleAnimationButton) viewInflate.findViewById(R.id.video_stop_shutter);
        this.mStopButton.setOnClickListener(this.mUISpec.stopListener);
        this.mVssButton = (ScaleAnimationButton) viewInflate.findViewById(R.id.btn_vss);
        this.mVssButton.setOnClickListener(this.mUISpec.vssListener);
        this.mRecordingSizeViewGroup = viewInflate.findViewById(R.id.recording_size_group);
        this.mCurrentRecordingSizeView = (RotateStrokeTextView) viewInflate.findViewById(R.id.recording_current);
        this.mRecordingSizeSeekBar = (SeekBar) viewInflate.findViewById(R.id.recording_progress);
        this.mRecordingSizeTotalView = (RotateStrokeTextView) viewInflate.findViewById(R.id.recording_total);
        this.mRecordingSizeSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        LogHelper.d(TAG, "[getView] - ");
        return viewFindViewById;
    }

    private void updateRecordingViewIcon() {
        int i;
        int i2;
        if (this.mUIState == IVideoUI.VideoUIState.STATE_PAUSE_RECORDING) {
            i = R.drawable.ic_pause_indicator;
            i2 = R.drawable.ic_resume_recording;
        } else {
            i = R.drawable.ic_recording_indicator;
            i2 = R.drawable.ic_pause_recording;
        }
        this.mRecordingIndicator.setImageResource(i);
        this.mPauseResumeButton.setImageResource(i2);
    }
}
