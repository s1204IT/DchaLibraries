package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import com.android.gallery3d.app.MovieActivity;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.RemoteConnection;
import com.mediatek.galleryportable.SystemPropertyUtils;
import com.mediatek.galleryportable.WfdConnectionAdapter;

public class PowerSavingManager {
    private static final int MHL_CONNECT = 2;
    private static final String MHL_EXT_SUPPORT = "1";
    private static final int NONE_CONNECT = 0;
    private static final String TAG = "VP_PowerSavingManager";
    private static final int WFD_CONNECT = 1;
    private Activity mActivity;
    private Context mContext;
    private MovieView mMovieView;
    private RemoteConnection.ConnectionEventListener mOnEventListener;
    private PowerSaving mPowerSaving;
    private RemoteConnection mRemoteConnection;
    private View mRootView;
    private Window mWindow;
    private boolean mIsReleased = false;
    private RemoteConnection.ConnectionEventListener mEventListener = new RemoteConnection.ConnectionEventListener() {
        @Override
        public void onEvent(int i) {
            Log.v(PowerSavingManager.TAG, "onEvent() what= " + i);
            if (i == 5) {
                PowerSavingManager.this.startPowerSaving();
            } else {
                if (i != 4) {
                    if (PowerSavingManager.this.mOnEventListener != null) {
                        PowerSavingManager.this.mOnEventListener.onEvent(i);
                        return;
                    }
                    return;
                }
                PowerSavingManager.this.endPowerSaving();
            }
        }
    };

    public PowerSavingManager(Activity activity, View view, RemoteConnection.ConnectionEventListener connectionEventListener) {
        this.mActivity = activity;
        this.mRootView = view;
        this.mContext = this.mActivity.getApplicationContext();
        this.mWindow = this.mActivity.getWindow();
        this.mOnEventListener = connectionEventListener;
        createRemoteDisPlay();
    }

    private void createRemoteDisPlay() {
        Log.v(TAG, "createRemoteDisPlay()");
        switch (getCurrentConnectWay()) {
            case 1:
                createWfdRemoteDisPlay(true);
                break;
            case 2:
                createMhlRemoteDisPlay();
                break;
            default:
                createWfdRemoteDisPlay(false);
                break;
        }
    }

    private void createWfdRemoteDisPlay(boolean z) {
        Log.v(TAG, "createWfdRemoteDisPlay()");
        this.mPowerSaving = new WfdPowerSaving(this.mContext, this.mWindow);
        this.mRemoteConnection = new WfdConnection(this.mActivity, this.mRootView, this.mEventListener, z);
    }

    private void createMhlRemoteDisPlay() {
        Log.v(TAG, "createMhlRemoteDisPlay()");
        this.mPowerSaving = new MhlPowerSaving(this.mContext, this.mWindow);
        this.mRemoteConnection = new MhlConnection(this.mActivity, this.mRootView, this.mEventListener);
    }

    private int getCurrentConnectWay() {
        int i;
        if (isWfdSupported()) {
            i = 1;
        } else if (isMhlSupported()) {
            i = 2;
        } else {
            i = 0;
        }
        Log.v(TAG, "getCurrentConnectWay() : " + i);
        return i;
    }

    public void refreshRemoteDisplay() {
        Log.v(TAG, "refreshRemoteDisplay() mIsReleased= " + this.mIsReleased);
        if (this.mIsReleased) {
            this.mPowerSaving.refreshParameter();
            this.mRemoteConnection.refreshConnection(isWfdSupported());
            this.mIsReleased = false;
        }
    }

    public void release() {
        Log.v(TAG, "release()");
        this.mRemoteConnection.doRelease();
        endPowerSaving();
        this.mIsReleased = true;
    }

    public void startPowerSaving() {
        Log.v(TAG, "startPowerSaving()");
        if (isPowerSavingEnable()) {
            this.mPowerSaving.startPowerSaving();
        }
    }

    public void endPowerSaving() {
        Log.v(TAG, "endPowerSaving()");
        if (isPowerSavingEnable()) {
            this.mPowerSaving.endPowerSaving();
        }
    }

    public boolean isInExtensionDisplay() {
        boolean zIsInExtensionDisplay = this.mRemoteConnection.isInExtensionDisplay();
        Log.v(TAG, "isInExtensionDisplay(): " + zIsInExtensionDisplay);
        return zIsInExtensionDisplay;
    }

    private boolean isPowerSavingEnable() {
        boolean z = this.mRemoteConnection.isConnected() && !((MovieActivity) this.mActivity).isMultiWindowMode();
        Log.v(TAG, "isPowerSavingEnable(): " + z);
        return z;
    }

    private boolean isWfdSupported() {
        return WfdConnectionAdapter.isWfdSupported(this.mContext);
    }

    private boolean isMhlSupported() {
        boolean zEquals = "1".equals(SystemPropertyUtils.get("mtk_hdmi_ext_mode"));
        Log.v(TAG, "isMhlSupported(): " + zEquals);
        return zEquals;
    }
}
