package com.android.bluetooth.opp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import com.google.android.collect.Lists;
import java.io.File;
import java.util.ArrayList;

public class BluetoothOppBatch {
    private static final String TAG = "BtOppBatch";
    private static final boolean V = Constants.VERBOSE;
    private final Context mContext;
    public final BluetoothDevice mDestination;
    public final int mDirection;
    public int mId;
    private BluetoothOppBatchListener mListener;
    private final ArrayList<BluetoothOppShareInfo> mShares;
    public int mStatus;
    public final long mTimestamp;

    public interface BluetoothOppBatchListener {
        void onBatchCanceled();

        void onShareAdded(int i);

        void onShareDeleted(int i);
    }

    public BluetoothOppBatch(Context context, BluetoothOppShareInfo bluetoothOppShareInfo) {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        this.mShares = Lists.newArrayList();
        this.mTimestamp = bluetoothOppShareInfo.mTimestamp;
        this.mDirection = bluetoothOppShareInfo.mDirection;
        this.mDestination = defaultAdapter.getRemoteDevice(bluetoothOppShareInfo.mDestination);
        this.mStatus = 0;
        this.mShares.add(bluetoothOppShareInfo);
        if (V) {
            Log.v(TAG, "New Batch created for info " + bluetoothOppShareInfo.mId);
        }
    }

    public void addShare(BluetoothOppShareInfo bluetoothOppShareInfo) {
        this.mShares.add(bluetoothOppShareInfo);
        if (this.mListener != null) {
            this.mListener.onShareAdded(bluetoothOppShareInfo.mId);
        }
    }

    public void cancelBatch() {
        if (V) {
            Log.v(TAG, "batch " + this.mId + " is canceled");
        }
        if (this.mListener != null) {
            this.mListener.onBatchCanceled();
        }
        for (int size = this.mShares.size() - 1; size >= 0; size--) {
            BluetoothOppShareInfo bluetoothOppShareInfo = this.mShares.get(size);
            if (bluetoothOppShareInfo.mStatus < 200) {
                if (bluetoothOppShareInfo.mDirection == 1 && bluetoothOppShareInfo.mFilename != null) {
                    new File(bluetoothOppShareInfo.mFilename).delete();
                }
                if (V) {
                    Log.v(TAG, "Cancel batch for info " + bluetoothOppShareInfo.mId);
                }
                Constants.updateShareStatus(this.mContext, bluetoothOppShareInfo.mId, BluetoothShare.STATUS_CANCELED);
            }
        }
        this.mShares.clear();
    }

    public boolean hasShare(BluetoothOppShareInfo bluetoothOppShareInfo) {
        return this.mShares.contains(bluetoothOppShareInfo);
    }

    public boolean isEmpty() {
        return this.mShares.size() == 0;
    }

    public int getNumShares() {
        return this.mShares.size();
    }

    public void registerListern(BluetoothOppBatchListener bluetoothOppBatchListener) {
        this.mListener = bluetoothOppBatchListener;
    }

    public BluetoothOppShareInfo getPendingShare() {
        for (int i = 0; i < this.mShares.size(); i++) {
            BluetoothOppShareInfo bluetoothOppShareInfo = this.mShares.get(i);
            if (bluetoothOppShareInfo.mStatus == 190) {
                return bluetoothOppShareInfo;
            }
        }
        return null;
    }

    public void RemoveShareInfo(BluetoothOppShareInfo bluetoothOppShareInfo) {
        if (V) {
            Log.i(TAG, " RemoveShareInfo before size=" + this.mShares.size());
        }
        this.mShares.remove(bluetoothOppShareInfo);
        if (V) {
            Log.i(TAG, " RemoveShareInfo after size=" + this.mShares.size());
        }
    }
}
