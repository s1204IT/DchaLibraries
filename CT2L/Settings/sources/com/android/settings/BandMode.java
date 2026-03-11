package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class BandMode extends Activity {
    private static final String[] BAND_NAMES = {"Automatic", "EURO Band", "USA Band", "JAPAN Band", "AUS Band", "AUS2 Band"};
    private ListView mBandList;
    private ArrayAdapter mBandListAdapter;
    private DialogInterface mProgressPanel;
    private BandListItem mTargetBand = null;
    private Phone mPhone = null;
    private AdapterView.OnItemClickListener mBandSelectionHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            BandMode.this.getWindow().setFeatureInt(5, -1);
            BandMode.this.mTargetBand = (BandListItem) parent.getAdapter().getItem(position);
            Message msg = BandMode.this.mHandler.obtainMessage(200);
            BandMode.this.mPhone.setBandMode(BandMode.this.mTargetBand.getBand(), msg);
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    BandMode.this.bandListLoaded(ar);
                    break;
                case 200:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    BandMode.this.getWindow().setFeatureInt(5, -2);
                    if (!BandMode.this.isFinishing()) {
                        BandMode.this.displayBandSelectionResult(ar2.exception);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(5);
        setContentView(R.layout.band_mode);
        setTitle(getString(R.string.band_mode_title));
        getWindow().setLayout(-1, -2);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mBandList = (ListView) findViewById(R.id.band);
        this.mBandListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        this.mBandList.setAdapter((ListAdapter) this.mBandListAdapter);
        this.mBandList.setOnItemClickListener(this.mBandSelectionHandler);
        loadBandList();
    }

    private static class BandListItem {
        private int mBandMode;

        public BandListItem(int bm) {
            this.mBandMode = 0;
            this.mBandMode = bm;
        }

        public int getBand() {
            return this.mBandMode;
        }

        public String toString() {
            return this.mBandMode >= BandMode.BAND_NAMES.length ? "Band mode " + this.mBandMode : BandMode.BAND_NAMES[this.mBandMode];
        }
    }

    private void loadBandList() {
        String str = getString(R.string.band_mode_loading);
        this.mProgressPanel = new AlertDialog.Builder(this).setMessage(str).show();
        Message msg = this.mHandler.obtainMessage(100);
        this.mPhone.queryAvailableBandMode(msg);
    }

    public void bandListLoaded(AsyncResult result) {
        int[] bands;
        int size;
        if (this.mProgressPanel != null) {
            this.mProgressPanel.dismiss();
        }
        clearList();
        boolean addBandSuccess = false;
        if (result.result != null && (size = (bands = (int[]) result.result)[0]) > 0) {
            for (int i = 1; i < size; i++) {
                BandListItem item = new BandListItem(bands[i]);
                this.mBandListAdapter.add(item);
            }
            addBandSuccess = true;
        }
        if (!addBandSuccess) {
            for (int i2 = 0; i2 < 6; i2++) {
                BandListItem item2 = new BandListItem(i2);
                this.mBandListAdapter.add(item2);
            }
        }
        this.mBandList.requestFocus();
    }

    public void displayBandSelectionResult(Throwable ex) {
        String status;
        String status2 = getString(R.string.band_mode_set) + " [" + this.mTargetBand.toString() + "] ";
        if (ex != null) {
            status = status2 + getString(R.string.band_mode_failed);
        } else {
            status = status2 + getString(R.string.band_mode_succeeded);
        }
        this.mProgressPanel = new AlertDialog.Builder(this).setMessage(status).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
    }

    private void clearList() {
        while (this.mBandListAdapter.getCount() > 0) {
            this.mBandListAdapter.remove(this.mBandListAdapter.getItem(0));
        }
    }
}
