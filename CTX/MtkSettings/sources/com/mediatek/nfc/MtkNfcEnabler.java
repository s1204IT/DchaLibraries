package com.mediatek.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;

public class MtkNfcEnabler implements SwitchBar.OnSwitchChangeListener {
    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private final NfcAdapter mNfcAdapter;
    private int mNfcState = 1;
    private QueryTask mQueryTask = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.nfc.action.ADAPTER_STATE_CHANGED".equals(intent.getAction())) {
                MtkNfcEnabler.this.mNfcState = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1);
                Log.d("@M_MtkNfcEnabler", "Receive nfc state changed : " + MtkNfcEnabler.this.mNfcState);
                if (MtkNfcEnabler.this.mNfcAdapter != null) {
                    MtkNfcEnabler.this.mQueryTask = new QueryTask();
                    MtkNfcEnabler.this.mQueryTask.execute(new Void[0]);
                }
            }
        }
    };
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mUpdateSwitchButtonOnly;

    public MtkNfcEnabler(Context context, SwitchBar switchBar, NfcAdapter nfcAdapter) {
        Log.d("@M_MtkNfcEnabler", "MtkNfcEnabler, switchBar = " + switchBar);
        this.mContext = context;
        this.mSwitchBar = switchBar;
        this.mSwitch = switchBar.getSwitch();
        this.mNfcAdapter = nfcAdapter;
        setupSwitchBar();
        this.mIntentFilter = new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED");
    }

    public void setupSwitchBar() {
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    public void resume() {
        Log.d("@M_MtkNfcEnabler", "Resume");
        if (this.mNfcAdapter != null) {
            this.mQueryTask = new QueryTask();
            this.mQueryTask.execute(new Void[0]);
        }
        handleNfcStateChanged(this.mNfcState);
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    public void pause() {
        Log.d("@M_MtkNfcEnabler", "Pause");
        if (this.mNfcAdapter == null) {
            return;
        }
        if (this.mQueryTask != null) {
            this.mQueryTask.cancel(true);
            Log.d("@M_MtkNfcEnabler", "mQueryTask.cancel(true)");
        }
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private void setSwitchChecked(boolean z) {
        if (z != this.mSwitch.isChecked()) {
            Log.d("@M_MtkNfcEnabler", "setSwitchChecked()  mUpdateSwitchButtonOnly = true ");
            this.mUpdateSwitchButtonOnly = true;
            this.mSwitch.setChecked(z);
            this.mUpdateSwitchButtonOnly = false;
            Log.d("@M_MtkNfcEnabler", "setSwitchChecked()  mUpdateSwitchButtonOnly = false ");
        }
    }

    @Override
    public void onSwitchChanged(Switch r4, boolean z) {
        Log.d("@M_MtkNfcEnabler", "onSwitchChanged: " + z + ", mNfcState: " + this.mNfcState);
        if (this.mNfcAdapter == null || this.mUpdateSwitchButtonOnly) {
            return;
        }
        if (z && (this.mNfcState == 1 || this.mNfcState == 4)) {
            Log.d("@M_MtkNfcEnabler", "onSwitchChanged: enable NFC ");
            this.mNfcAdapter.enable();
            r4.setEnabled(false);
        } else {
            if (z) {
                return;
            }
            if (this.mNfcState == 3 || this.mNfcState == 2) {
                Log.d("@M_MtkNfcEnabler", "onSwitchChanged: disable NFC ");
                this.mNfcAdapter.disable();
                r4.setEnabled(false);
            }
        }
    }

    private void handleNfcStateChanged(int i) {
        Log.d("@M_MtkNfcEnabler", "handleNfcStateChanged  newState = " + i);
        updateSwitch(i);
    }

    private void updateSwitch(int i) {
        if (this.mSwitch == null) {
        }
        switch (i) {
            case 1:
                setSwitchChecked(false);
                this.mSwitch.setEnabled(true);
                break;
            case 2:
                setSwitchChecked(true);
                this.mSwitch.setEnabled(false);
                break;
            case 3:
                setSwitchChecked(true);
                this.mSwitch.setEnabled(true);
                break;
            case 4:
                setSwitchChecked(false);
                this.mSwitch.setEnabled(false);
                break;
            default:
                setSwitchChecked(false);
                break;
        }
    }

    private class QueryTask extends AsyncTask<Void, Void, Integer> {
        private QueryTask() {
        }

        @Override
        protected Integer doInBackground(Void... voidArr) {
            MtkNfcEnabler.this.mNfcState = MtkNfcEnabler.this.mNfcAdapter.getAdapterState();
            Log.d("@M_MtkNfcEnabler", "[QueryTask] doInBackground  mNfcState: " + MtkNfcEnabler.this.mNfcState);
            return Integer.valueOf(MtkNfcEnabler.this.mNfcState);
        }

        @Override
        protected void onPostExecute(Integer num) {
            Log.d("@M_MtkNfcEnabler", "[QueryTask] onPostExecute: " + num);
            MtkNfcEnabler.this.handleNfcStateChanged(num.intValue());
        }
    }
}
