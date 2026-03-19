package com.android.music;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

public class DeleteItems extends Activity {
    private long[] mItemList;
    private String mDialogDesc = null;
    private DialogInterface.OnClickListener mButtonClicked = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                DeleteItems.this.showDialog(0);
                DeleteItems.this.mHandler.sendMessage(DeleteItems.this.mHandler.obtainMessage(0));
            } else if (i == -3) {
                DeleteItems.this.finish();
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            String quantityString;
            if (message.what == 0) {
                MusicUtils.removeTracks(DeleteItems.this.getApplicationContext(), DeleteItems.this.mItemList);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DeleteItems.this.doDeleteItems();
                    }
                }).start();
            } else if (message.what == 1) {
                int i = message.arg1;
                if (i != -1) {
                    if (i == 0) {
                        quantityString = DeleteItems.this.getString(R.string.delete_track_failed);
                    } else {
                        quantityString = DeleteItems.this.getResources().getQuantityString(R.plurals.NNNtracksdeleted, i, Integer.valueOf(i));
                    }
                    Toast.makeText(DeleteItems.this, quantityString, 0).show();
                }
                DeleteItems.this.finish();
            }
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DeleteItems.this.finish();
            MusicLogUtils.d("DeleteItems", "SD card is ejected, finish delete activity!");
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setVolumeControlStream(3);
        Bundle extras = getIntent().getExtras();
        this.mDialogDesc = String.format(getString(extras.getInt("delete_desc_string_id")), extras.getString("delete_desc_track_info"));
        this.mItemList = extras.getLongArray("items");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
        if (bundle == null) {
            showDialog(1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVisible(true);
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        switch (i) {
            case 0:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle(R.string.delete_progress_title);
                progressDialog.setMessage(getResources().getString(R.string.delete_progress_message));
                progressDialog.setIndeterminate(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setCancelable(false);
                progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int i2, KeyEvent keyEvent) {
                        if (84 == i2) {
                            MusicLogUtils.d("DeleteItems", "OnKeyListener");
                            return true;
                        }
                        return false;
                    }
                });
                return progressDialog;
            case 1:
                MusicDialog musicDialog = new MusicDialog(this, this.mButtonClicked, null);
                musicDialog.setTitle(R.string.delete_item);
                musicDialog.setPositiveButton(getResources().getString(R.string.delete_confirm_button_text));
                musicDialog.setNeutralButton(getResources().getString(R.string.cancel));
                musicDialog.setMessage(this.mDialogDesc);
                musicDialog.setCanceledOnTouchOutside(true);
                musicDialog.setCancelable(true);
                musicDialog.setSearchKeyListener();
                musicDialog.setIcon(android.R.drawable.ic_dialog_alert);
                return musicDialog;
            default:
                MusicLogUtils.e("DeleteItems", "onCreateDialog with undefined id!");
                return null;
        }
    }

    private void doDeleteItems() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, MusicUtils.deleteTracks(this, this.mItemList), -1));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mScanListener);
        super.onDestroy();
    }
}
