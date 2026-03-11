package com.android.systemui.statusbar.car;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.keyguard.AlphaOptimizedImageButton;
import com.android.systemui.R;
import java.net.URISyntaxException;

public class CarNavigationButton extends AlphaOptimizedImageButton {
    private boolean mBroadcastIntent;
    private Context mContext;
    private int mIconResourceId;
    private String mIntent;
    private String mLongIntent;
    private boolean mSelected;
    private float mSelectedAlpha;
    private int mSelectedIconResourceId;
    private float mUnselectedAlpha;

    public CarNavigationButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSelected = false;
        this.mSelectedAlpha = 1.0f;
        this.mUnselectedAlpha = 1.0f;
        this.mContext = context;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CarNavigationButton);
        this.mIntent = typedArrayObtainStyledAttributes.getString(1);
        this.mLongIntent = typedArrayObtainStyledAttributes.getString(2);
        this.mBroadcastIntent = typedArrayObtainStyledAttributes.getBoolean(0, false);
        this.mSelectedAlpha = typedArrayObtainStyledAttributes.getFloat(3, this.mSelectedAlpha);
        this.mUnselectedAlpha = typedArrayObtainStyledAttributes.getFloat(5, this.mUnselectedAlpha);
        this.mIconResourceId = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        this.mSelectedIconResourceId = typedArrayObtainStyledAttributes.getResourceId(4, this.mIconResourceId);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setScaleType(ImageView.ScaleType.CENTER);
        setAlpha(this.mUnselectedAlpha);
        try {
            if (this.mIntent != null) {
                final Intent uri = Intent.parseUri(this.mIntent, 1);
                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        CarNavigationButton.lambda$onFinishInflate$0(this.f$0, uri, view);
                    }
                });
            }
            try {
                if (this.mLongIntent != null) {
                    final Intent uri2 = Intent.parseUri(this.mLongIntent, 1);
                    setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public final boolean onLongClick(View view) {
                            return CarNavigationButton.lambda$onFinishInflate$1(this.f$0, uri2, view);
                        }
                    });
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to attach long press intent", e);
            }
        } catch (URISyntaxException e2) {
            throw new RuntimeException("Failed to attach intent", e2);
        }
    }

    public static void lambda$onFinishInflate$0(CarNavigationButton carNavigationButton, Intent intent, View view) {
        try {
            if (carNavigationButton.mBroadcastIntent) {
                carNavigationButton.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            } else {
                carNavigationButton.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            }
        } catch (Exception e) {
            Log.e("CarNavigationButton", "Failed to launch intent", e);
        }
    }

    public static boolean lambda$onFinishInflate$1(CarNavigationButton carNavigationButton, Intent intent, View view) {
        try {
            carNavigationButton.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return true;
        } catch (Exception e) {
            Log.e("CarNavigationButton", "Failed to launch intent", e);
            return true;
        }
    }

    @Override
    public void setSelected(boolean z) {
        super.setSelected(z);
        this.mSelected = z;
        setAlpha(this.mSelected ? this.mSelectedAlpha : this.mUnselectedAlpha);
        setImageResource(this.mSelected ? this.mSelectedIconResourceId : this.mIconResourceId);
    }
}
