package com.mediatek.camera.feature.setting.shutterspeed;

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

class ShutterSpeedIndicatorView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedIndicatorView.class.getSimpleName());
    private Activity mActivity;
    private ImageView mIndicatorView;
    private Handler mMainHandler;
    private String[] mOriginalEntryValues;
    private int[] mOriginalIndicator;

    public ShutterSpeedIndicatorView(Activity activity) {
        this.mActivity = activity;
        this.mMainHandler = new MainHandler(activity.getMainLooper());
        this.mIndicatorView = (ImageView) activity.getLayoutInflater().inflate(R.layout.shutter_speed_indicator, (ViewGroup) null);
        this.mOriginalEntryValues = activity.getResources().getStringArray(R.array.shutter_speed_entryvalues);
        TypedArray typedArrayObtainTypedArray = activity.getResources().obtainTypedArray(R.array.shutter_speed_indicators);
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
        return 50;
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
            LogHelper.d(ShutterSpeedIndicatorView.TAG, "[updateIndicator]+ speed : " + str);
            int i = 0;
            while (true) {
                if (i < ShutterSpeedIndicatorView.this.mOriginalEntryValues.length) {
                    if (ShutterSpeedIndicatorView.this.mOriginalEntryValues[i].equals(str)) {
                        break;
                    } else {
                        i++;
                    }
                } else {
                    i = -1;
                    break;
                }
            }
            if (i < 0) {
                ShutterSpeedIndicatorView.this.mIndicatorView.setVisibility(8);
                return;
            }
            ShutterSpeedIndicatorView.this.mIndicatorView.setImageDrawable(ShutterSpeedIndicatorView.this.mActivity.getResources().getDrawable(ShutterSpeedIndicatorView.this.mOriginalIndicator[i]));
            ShutterSpeedIndicatorView.this.mIndicatorView.setVisibility(0);
            ShutterSpeedIndicatorView.this.mIndicatorView.setContentDescription(str);
        }
    }
}
