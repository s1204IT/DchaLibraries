package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.view.View;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.RemoteConnection;
import com.mediatek.galleryportable.MhlConnectionAdapter;

public class MhlConnection extends RemoteConnection implements MhlConnectionAdapter.StateChangeListener {
    private static final int ROUTE_DISCONNECTED_DELAY = 1000;
    private static final String TAG = "VP_MhlConnection";
    private MhlConnectionAdapter mAdapter;
    private boolean mIsConnected;

    public MhlConnection(Activity activity, View view, RemoteConnection.ConnectionEventListener connectionEventListener) {
        super(activity, view, connectionEventListener);
        Log.v(TAG, "MhlConnection construct");
        this.mAdapter = new MhlConnectionAdapter(this);
        this.mIsConnected = false;
        entreExtensionIfneed();
    }

    @Override
    public void refreshConnection(boolean z) {
        Log.v(TAG, "refreshConnection()");
        entreExtensionIfneed();
    }

    @Override
    public boolean isConnected() {
        Log.v(TAG, "isConnected(): " + this.mIsConnected);
        return this.mIsConnected;
    }

    @Override
    public boolean isInExtensionDisplay() {
        Log.v(TAG, "isExtension(): " + this.mIsConnected);
        return this.mIsConnected;
    }

    @Override
    protected void entreExtensionIfneed() {
        Log.v(TAG, "entreExtensionIfneed()");
        registerReceiver();
    }

    @Override
    public void doRelease() {
        Log.v(TAG, "doRelease()");
        unRegisterReceiver();
        dismissPresentation();
        this.mHandler.removeCallbacks(this.mSelectMediaRouteRunnable);
        this.mHandler.removeCallbacks(this.mUnselectMediaRouteRunnable);
    }

    private void registerReceiver() {
        this.mAdapter.registerReceiver(this.mContext);
    }

    private void unRegisterReceiver() {
        this.mAdapter.unRegisterReceiver(this.mContext);
    }

    private void leaveExtensionMode() {
        Log.v(TAG, "leaveMhlExtensionMode()");
        this.mHandler.removeCallbacks(this.mUnselectMediaRouteRunnable);
        this.mHandler.postDelayed(this.mUnselectMediaRouteRunnable, 1000L);
    }

    private void enterExtensionMode() {
        Log.v(TAG, "enterMhlExtensionMode()");
        this.mHandler.removeCallbacks(this.mSelectMediaRouteRunnable);
        this.mHandler.post(this.mSelectMediaRouteRunnable);
    }

    private void connected() {
        Log.v(TAG, "connected()");
        this.mIsConnected = true;
        this.mOnEventListener.onEvent(1);
        enterExtensionMode();
    }

    private void disConnected() {
        Log.v(TAG, "disConnected()");
        this.mOnEventListener.onEvent(2);
        this.mOnEventListener.onEvent(4);
        leaveExtensionMode();
        this.mIsConnected = false;
    }

    public void stateNotConnected() {
        disConnected();
    }

    public void stateConnected() {
        connected();
    }
}
