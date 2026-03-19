package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.RemoteConnection;
import com.mediatek.galleryportable.WfdConnectionAdapter;

public class WfdConnection extends RemoteConnection implements WfdConnectionAdapter.StateChangeListener {
    private static final int EXTENSION_MODE = 2;
    private static final int EXTENSION_MODE_LIST_END = 12;
    private static final int EXTENSION_MODE_LIST_START = 10;
    private static final int NORMAL_MODE = 1;
    private static final int ROUTE_DISCONNECTED_DELAY = 1000;
    private static final String TAG = "VP_WfdConnection";
    private WfdConnectionAdapter mAdapter;
    private int mCurrentMode;
    private boolean mIsConnected;

    public WfdConnection(Activity activity, View view, RemoteConnection.ConnectionEventListener connectionEventListener, boolean z) {
        super(activity, view, connectionEventListener);
        Log.v(TAG, "WfdConnection construct");
        this.mAdapter = new WfdConnectionAdapter(this);
        this.mCurrentMode = getCurrentPowerSavingMode();
        initConnection(z);
    }

    @Override
    public void refreshConnection(boolean z) {
        Log.v(TAG, "refreshConnection() isConnected= " + z);
        initConnection(z);
    }

    private void initConnection(boolean z) {
        Log.v(TAG, "initConnection()");
        this.mIsConnected = z;
        entreExtensionIfneed();
        registerReceiver();
    }

    private int getCurrentPowerSavingMode() {
        int i;
        if (isExtensionFeatureOn()) {
            i = 2;
        } else {
            i = 1;
        }
        Log.v(TAG, "getCurrentPowerSavingMode()= " + i);
        return i;
    }

    @Override
    public boolean isConnected() {
        Log.v(TAG, "isConnected()= " + this.mIsConnected);
        return this.mIsConnected;
    }

    @Override
    public boolean isInExtensionDisplay() {
        boolean z = this.mIsConnected && this.mCurrentMode == 2;
        Log.v(TAG, "isInExtensionDisplay()= " + z);
        return z;
    }

    @Override
    protected void entreExtensionIfneed() {
        Log.v(TAG, "entreExtensionIfneed() mIsConnected= " + this.mIsConnected + " mCurrentMode= " + this.mCurrentMode);
        if (this.mIsConnected && this.mCurrentMode == 2) {
            this.mOnEventListener.onEvent(1);
            enterExtensionMode();
        }
    }

    @Override
    public void doRelease() {
        Log.v(TAG, "doRelease()");
        unRegisterReceiver();
        if (isInExtensionDisplay()) {
            dismissPresentation();
            this.mHandler.removeCallbacks(this.mSelectMediaRouteRunnable);
            this.mHandler.removeCallbacks(this.mUnselectMediaRouteRunnable);
        }
    }

    private boolean isExtensionFeatureOn() {
        int powerSavingMode = WfdConnectionAdapter.getPowerSavingMode(this.mContext);
        if (powerSavingMode >= 10 && powerSavingMode <= 12) {
            return true;
        }
        return false;
    }

    private void registerReceiver() {
        this.mAdapter.registerReceiver(this.mContext);
    }

    private void unRegisterReceiver() {
        this.mAdapter.unRegisterReceiver(this.mContext);
    }

    private void leaveExtensionMode() {
        Log.v(TAG, "leaveExtensionMode()");
        this.mHandler.removeCallbacks(this.mUnselectMediaRouteRunnable);
        this.mHandler.postDelayed(this.mUnselectMediaRouteRunnable, 1000L);
        this.mOnEventListener.onEvent(3);
    }

    private void enterExtensionMode() {
        Log.v(TAG, "enterExtensionMode()");
        this.mHandler.removeCallbacks(this.mSelectMediaRouteRunnable);
        this.mHandler.post(this.mSelectMediaRouteRunnable);
    }

    private void disConnect() {
        Log.v(TAG, "disConnect()");
        Toast.makeText(this.mContext.getApplicationContext(), this.mContext.getString(R.string.wfd_disconnected), 1).show();
        this.mOnEventListener.onEvent(4);
        if (isInExtensionDisplay()) {
            leaveExtensionMode();
        }
    }

    @Override
    public void stateNotConnected() {
        if (this.mIsConnected) {
            disConnect();
            this.mIsConnected = false;
        }
    }

    @Override
    public void stateConnected() {
        this.mIsConnected = true;
        entreExtensionIfneed();
    }
}
