package com.mediatek.camera.feature.setting.whitebalance;

import android.app.Activity;
import android.content.res.TypedArray;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class WhiteBalanceIndicatorView {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(WhiteBalanceIndicatorView.class.getSimpleName());
    private Activity mActivity;
    private ImageView mIndicatorView;
    private String[] mOriginalEntryValues;
    private int[] mOriginalIndicator;

    public WhiteBalanceIndicatorView(Activity activity) {
        this.mActivity = activity;
        this.mIndicatorView = (ImageView) activity.getLayoutInflater().inflate(R.layout.white_balance_indicator, (ViewGroup) null);
        this.mOriginalEntryValues = activity.getResources().getStringArray(R.array.white_balance_entryvalues);
        TypedArray typedArrayObtainTypedArray = activity.getResources().obtainTypedArray(R.array.white_balance_indicators);
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
        return 8;
    }

    public void updateIndicator(final String str) {
        LogHelper.d(TAG, "[updateIndicator], scene:" + str);
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (true) {
                    if (i < WhiteBalanceIndicatorView.this.mOriginalEntryValues.length) {
                        if (WhiteBalanceIndicatorView.this.mOriginalEntryValues[i].equals(str)) {
                            break;
                        } else {
                            i++;
                        }
                    } else {
                        i = -1;
                        break;
                    }
                }
                LogHelper.d(WhiteBalanceIndicatorView.TAG, "[updateIndicator], index:" + i);
                if (i == 0) {
                    WhiteBalanceIndicatorView.this.mIndicatorView.setVisibility(8);
                } else {
                    WhiteBalanceIndicatorView.this.mIndicatorView.setImageDrawable(WhiteBalanceIndicatorView.this.mActivity.getResources().getDrawable(WhiteBalanceIndicatorView.this.mOriginalIndicator[i]));
                    WhiteBalanceIndicatorView.this.mIndicatorView.setVisibility(0);
                }
            }
        });
    }
}
