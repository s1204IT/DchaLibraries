package com.android.bluetooth.hfpclient.connserv;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.content.Context;
import android.net.Uri;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.util.Log;
import java.util.UUID;

public class HfpClientConnection extends Connection {
    private static final boolean DBG = false;
    private static final String TAG = "HfpClientConnection";
    private boolean mAdded;
    private boolean mClientHasEcc;
    private boolean mClosed;
    private final Context mContext;
    private BluetoothHeadsetClientCall mCurrentCall;
    private final BluetoothDevice mDevice;
    private BluetoothHeadsetClient mHeadsetProfile;
    private boolean mLocalDisconnect;
    private int mPreviousCallState = -1;
    private boolean mClosing = false;

    public HfpClientConnection(Context context, BluetoothDevice bluetoothDevice, BluetoothHeadsetClient bluetoothHeadsetClient, BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        this.mDevice = bluetoothDevice;
        this.mContext = context;
        this.mHeadsetProfile = bluetoothHeadsetClient;
        if (bluetoothHeadsetClientCall == null) {
            throw new IllegalStateException("Call is null");
        }
        this.mCurrentCall = bluetoothHeadsetClientCall;
        handleCallChanged();
        finishInitializing();
    }

    public HfpClientConnection(Context context, BluetoothDevice bluetoothDevice, BluetoothHeadsetClient bluetoothHeadsetClient, Uri uri) {
        this.mDevice = bluetoothDevice;
        this.mContext = context;
        this.mHeadsetProfile = bluetoothHeadsetClient;
        if (this.mHeadsetProfile == null) {
            throw new IllegalStateException("HeadsetProfile is null, returning");
        }
        this.mCurrentCall = this.mHeadsetProfile.dial(this.mDevice, uri.getSchemeSpecificPart());
        if (this.mCurrentCall == null) {
            close(1);
            Log.e(TAG, "Failed to create the call, dial failed.");
        } else {
            this.mHeadsetProfile.connectAudio(bluetoothDevice);
            setInitializing();
            setDialing();
            finishInitializing();
        }
    }

    void finishInitializing() {
        this.mClientHasEcc = HfpClientConnectionService.hasHfpClientEcc(this.mHeadsetProfile, this.mDevice);
        int i = 0;
        setAudioModeIsVoip(false);
        setAddress(Uri.fromParts("tel", this.mCurrentCall.getNumber(), null), 1);
        if (getState() == 4 || getState() == 5) {
            i = 1;
        }
        setConnectionCapabilities(i | 12354);
    }

    public UUID getUUID() {
        return this.mCurrentCall.getUUID();
    }

    public void onHfpDisconnected() {
        this.mHeadsetProfile = null;
        close(1);
    }

    public void onAdded() {
        this.mAdded = true;
    }

    public BluetoothHeadsetClientCall getCall() {
        return this.mCurrentCall;
    }

    public boolean inConference() {
        return this.mAdded && this.mCurrentCall != null && this.mCurrentCall.isMultiParty() && getState() != 6;
    }

    public void enterPrivateMode() {
        this.mHeadsetProfile.enterPrivateMode(this.mDevice, this.mCurrentCall.getId());
        setActive();
    }

    public void updateCall(BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        if (bluetoothHeadsetClientCall == null) {
            Log.e(TAG, "Updating call to a null value.");
        } else {
            this.mCurrentCall = bluetoothHeadsetClientCall;
        }
    }

    public void handleCallChanged() {
        HfpClientConference hfpClientConference = (HfpClientConference) getConference();
        int state = this.mCurrentCall.getState();
        switch (state) {
            case 0:
                setActive();
                if (hfpClientConference != null) {
                    hfpClientConference.setActive();
                }
                break;
            case 1:
            case 6:
                setOnHold();
                if (hfpClientConference != null) {
                    hfpClientConference.setOnHold();
                }
                break;
            case 2:
            case 3:
                setDialing();
                break;
            case 4:
            case 5:
                setRinging();
                break;
            case 7:
                if (this.mPreviousCallState == 4 || this.mPreviousCallState == 5) {
                    close(5);
                } else if (this.mLocalDisconnect) {
                    close(2);
                } else {
                    close(3);
                }
                break;
            default:
                Log.wtf(TAG, "Unexpected phone state " + state);
                break;
        }
        this.mPreviousCallState = state;
    }

    public synchronized void close(int i) {
        if (this.mClosed) {
            return;
        }
        Log.d(TAG, "Setting " + this.mCurrentCall + " to disconnected " + getTelecomCallId());
        setDisconnected(new DisconnectCause(i));
        this.mClosed = true;
        this.mCurrentCall = null;
        destroy();
    }

    public synchronized boolean isClosing() {
        return this.mClosing;
    }

    public synchronized BluetoothDevice getDevice() {
        return this.mDevice;
    }

    @Override
    public synchronized void onPlayDtmfTone(char c) {
        if (!this.mClosed) {
            this.mHeadsetProfile.sendDTMF(this.mDevice, (byte) c);
        }
    }

    @Override
    public synchronized void onDisconnect() {
        if (!this.mClosed) {
            this.mHeadsetProfile.terminateCall(this.mDevice, this.mCurrentCall);
            this.mLocalDisconnect = true;
            this.mClosing = true;
        }
    }

    @Override
    public void onAbort() {
        onDisconnect();
    }

    @Override
    public synchronized void onHold() {
        if (!this.mClosed) {
            this.mHeadsetProfile.holdCall(this.mDevice);
        }
    }

    @Override
    public synchronized void onUnhold() {
        if (getConnectionService().getAllConnections().size() > 1) {
            Log.w(TAG, "Ignoring unhold; call hold on the foreground call");
        } else {
            if (!this.mClosed) {
                this.mHeadsetProfile.acceptCall(this.mDevice, 1);
            }
        }
    }

    @Override
    public synchronized void onAnswer() {
        if (!this.mClosed) {
            this.mHeadsetProfile.acceptCall(this.mDevice, 0);
        }
        this.mHeadsetProfile.connectAudio(this.mDevice);
    }

    @Override
    public synchronized void onReject() {
        if (!this.mClosed) {
            this.mHeadsetProfile.rejectCall(this.mDevice);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof HfpClientConnection)) {
            return false;
        }
        Uri address = ((HfpClientConnection) obj).getAddress();
        return getAddress() == address || (address != null && address.equals(getAddress()));
    }

    public String toString() {
        return "HfpClientConnection{" + getAddress() + "," + stateToString(getState()) + "," + this.mCurrentCall + "}";
    }
}
