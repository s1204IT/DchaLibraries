package com.mediatek.camera.feature.setting.scenemode;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class SceneModeIndicatorView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SceneModeIndicatorView.class.getSimpleName());
    private Activity mActivity;
    private ImageView mIndicatorView;
    private Handler mMainHandler;
    private String[] mOriginalEntryValues;
    private int[] mOriginalIndicator;

    public SceneModeIndicatorView(Activity activity) {
        this.mActivity = activity;
        this.mMainHandler = new MainHandler(activity.getMainLooper());
        this.mIndicatorView = (ImageView) activity.getLayoutInflater().inflate(R.layout.scene_mode_indicator, (ViewGroup) null);
        this.mOriginalEntryValues = activity.getResources().getStringArray(R.array.scene_mode_entryvalues);
        TypedArray typedArrayObtainTypedArray = activity.getResources().obtainTypedArray(R.array.scene_mode_indicators);
        int length = typedArrayObtainTypedArray.length();
        this.mOriginalIndicator = new int[length];
        for (int i = 0; i < length; i++) {
            this.mOriginalIndicator[i] = typedArrayObtainTypedArray.getResourceId(i, 0);
        }
        typedArrayObtainTypedArray.recycle();
    }

    public ImageView getView() {
        return this.mIndicatorView;
    }

    public int getViewPriority() {
        return 7;
    }

    public void updateIndicator(String str) {
        this.mMainHandler.removeMessages(0);
        this.mMainHandler.obtainMessage(0, str).sendToTarget();
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                updateIndicator((String) message.obj);
            }
        }

        private void updateIndicator(String str) {
            LogHelper.d(SceneModeIndicatorView.TAG, "[updateIndicator], scene:" + str);
            int i = 0;
            while (true) {
                if (i < SceneModeIndicatorView.this.mOriginalEntryValues.length) {
                    if (SceneModeIndicatorView.this.mOriginalEntryValues[i].equals(str)) {
                        break;
                    } else {
                        i++;
                    }
                } else {
                    i = -1;
                    break;
                }
            }
            if (i <= 1) {
                SceneModeIndicatorView.this.mIndicatorView.setVisibility(8);
            } else {
                SceneModeIndicatorView.this.mIndicatorView.setImageDrawable(SceneModeIndicatorView.this.mActivity.getResources().getDrawable(SceneModeIndicatorView.this.mOriginalIndicator[i]));
                SceneModeIndicatorView.this.mIndicatorView.setVisibility(0);
            }
        }
    }
}
