package com.android.bluetooth.opp;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.bluetooth.R;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BluetoothOppTransferActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private static final boolean D = true;
    public static final int DIALOG_RECEIVE_COMPLETE_FAIL = 2;
    public static final int DIALOG_RECEIVE_COMPLETE_SUCCESS = 1;
    public static final int DIALOG_RECEIVE_ONGOING = 0;
    public static final int DIALOG_SEND_COMPLETE_FAIL = 5;
    public static final int DIALOG_SEND_COMPLETE_SUCCESS = 4;
    public static final int DIALOG_SEND_ONGOING = 3;
    private static final String TAG = "BluetoothOppTransferActivity";
    private static final boolean V = false;
    private BluetoothAdapter mAdapter;
    boolean mIsComplete;
    private TextView mLine1View;
    private TextView mLine2View;
    private TextView mLine3View;
    private TextView mLine5View;
    private BluetoothTransferContentObserver mObserver;
    private AlertController.AlertParams mPara;
    private TextView mPercentView;
    private ProgressBar mProgressTransfer;
    private BluetoothOppTransferInfo mTransInfo;
    private Uri mUri;
    private int mWhichDialog;
    private View mView = null;
    private boolean mNeedUpdateButton = false;

    private class BluetoothTransferContentObserver extends ContentObserver {
        public BluetoothTransferContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            BluetoothOppTransferActivity.this.mNeedUpdateButton = true;
            BluetoothOppTransferActivity.this.updateProgressbar();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mUri = intent.getData();
        this.mTransInfo = new BluetoothOppTransferInfo();
        this.mTransInfo = BluetoothOppUtility.queryRecord(this, this.mUri);
        if (this.mTransInfo == null) {
            finish();
            return;
        }
        this.mIsComplete = BluetoothShare.isStatusCompleted(this.mTransInfo.mStatus);
        displayWhichDialog();
        if (!this.mIsComplete) {
            this.mObserver = new BluetoothTransferContentObserver();
            getContentResolver().registerContentObserver(BluetoothShare.CONTENT_URI, true, this.mObserver);
        }
        if (this.mWhichDialog != 3 && this.mWhichDialog != 0) {
            BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
        }
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        setUpDialog();
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        if (this.mObserver != null) {
            getContentResolver().unregisterContentObserver(this.mObserver);
        }
        super.onDestroy();
    }

    private void displayWhichDialog() {
        int direction = this.mTransInfo.mDirection;
        boolean isSuccess = BluetoothShare.isStatusSuccess(this.mTransInfo.mStatus);
        boolean isComplete = BluetoothShare.isStatusCompleted(this.mTransInfo.mStatus);
        if (direction == 1) {
            if (isComplete) {
                if (isSuccess) {
                    this.mWhichDialog = 1;
                    return;
                } else {
                    if (!isSuccess) {
                        this.mWhichDialog = 2;
                        return;
                    }
                    return;
                }
            }
            if (!isComplete) {
                this.mWhichDialog = 0;
                return;
            }
            return;
        }
        if (direction == 0) {
            if (isComplete) {
                if (isSuccess) {
                    this.mWhichDialog = 4;
                    return;
                } else {
                    if (!isSuccess) {
                        this.mWhichDialog = 5;
                        return;
                    }
                    return;
                }
            }
            if (!isComplete) {
                this.mWhichDialog = 3;
            }
        }
    }

    private void setUpDialog() {
        this.mPara = this.mAlertParams;
        this.mPara.mTitle = getString(R.string.download_title);
        if (this.mWhichDialog == 0 || this.mWhichDialog == 3) {
            this.mPara.mPositiveButtonText = getString(R.string.download_ok);
            this.mPara.mPositiveButtonListener = this;
            this.mPara.mNegativeButtonText = getString(R.string.download_cancel);
            this.mPara.mNegativeButtonListener = this;
        } else if (this.mWhichDialog == 1) {
            this.mPara.mPositiveButtonText = getString(R.string.download_succ_ok);
            this.mPara.mPositiveButtonListener = this;
        } else if (this.mWhichDialog == 2) {
            this.mPara.mIconAttrId = android.R.attr.alertDialogIcon;
            this.mPara.mPositiveButtonText = getString(R.string.download_fail_ok);
            this.mPara.mPositiveButtonListener = this;
        } else if (this.mWhichDialog == 4) {
            this.mPara.mPositiveButtonText = getString(R.string.upload_succ_ok);
            this.mPara.mPositiveButtonListener = this;
        } else if (this.mWhichDialog == 5) {
            this.mPara.mIconAttrId = android.R.attr.alertDialogIcon;
            this.mPara.mPositiveButtonText = getString(R.string.upload_fail_ok);
            this.mPara.mPositiveButtonListener = this;
            this.mPara.mNegativeButtonText = getString(R.string.upload_fail_cancel);
            this.mPara.mNegativeButtonListener = this;
        }
        this.mPara.mView = createView();
        setupAlert();
    }

    private View createView() {
        this.mView = getLayoutInflater().inflate(R.layout.file_transfer, (ViewGroup) null);
        this.mProgressTransfer = (ProgressBar) this.mView.findViewById(R.id.progress_transfer);
        this.mPercentView = (TextView) this.mView.findViewById(R.id.progress_percent);
        customizeViewContent();
        this.mNeedUpdateButton = false;
        updateProgressbar();
        return this.mView;
    }

    private void customizeViewContent() {
        if (this.mWhichDialog == 0 || this.mWhichDialog == 1) {
            this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
            String tmp = getString(R.string.download_line1, new Object[]{this.mTransInfo.mDeviceName});
            this.mLine1View.setText(tmp);
            this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
            String tmp2 = getString(R.string.download_line2, new Object[]{this.mTransInfo.mFileName});
            this.mLine2View.setText(tmp2);
            this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
            String tmp3 = getString(R.string.download_line3, new Object[]{Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes)});
            this.mLine3View.setText(tmp3);
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            if (this.mWhichDialog == 0) {
                tmp3 = getString(R.string.download_line5);
            } else if (this.mWhichDialog == 1) {
                tmp3 = getString(R.string.download_succ_line5);
            }
            this.mLine5View.setText(tmp3);
        } else if (this.mWhichDialog == 3 || this.mWhichDialog == 4) {
            this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
            String tmp4 = getString(R.string.upload_line1, new Object[]{this.mTransInfo.mDeviceName});
            this.mLine1View.setText(tmp4);
            this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
            String tmp5 = getString(R.string.download_line2, new Object[]{this.mTransInfo.mFileName});
            this.mLine2View.setText(tmp5);
            this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
            String tmp6 = getString(R.string.upload_line3, new Object[]{this.mTransInfo.mFileType, Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes)});
            this.mLine3View.setText(tmp6);
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            if (this.mWhichDialog == 3) {
                tmp6 = getString(R.string.upload_line5);
            } else if (this.mWhichDialog == 4) {
                tmp6 = getString(R.string.upload_succ_line5);
            }
            this.mLine5View.setText(tmp6);
        } else if (this.mWhichDialog == 2) {
            if (this.mTransInfo.mStatus == 494) {
                this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
                String tmp7 = getString(R.string.bt_sm_2_1, new Object[]{this.mTransInfo.mDeviceName});
                this.mLine1View.setText(tmp7);
                this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
                String tmp8 = getString(R.string.download_fail_line2, new Object[]{this.mTransInfo.mFileName});
                this.mLine2View.setText(tmp8);
                this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
                String tmp9 = getString(R.string.bt_sm_2_2, new Object[]{Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes)});
                this.mLine3View.setText(tmp9);
            } else {
                this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
                String tmp10 = getString(R.string.download_fail_line1);
                this.mLine1View.setText(tmp10);
                this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
                String tmp11 = getString(R.string.download_fail_line2, new Object[]{this.mTransInfo.mFileName});
                this.mLine2View.setText(tmp11);
                this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
                String tmp12 = getString(R.string.download_fail_line3, new Object[]{BluetoothOppUtility.getStatusDescription(this, this.mTransInfo.mStatus, this.mTransInfo.mDeviceName)});
                this.mLine3View.setText(tmp12);
            }
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            this.mLine5View.setVisibility(8);
        } else if (this.mWhichDialog == 5) {
            this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
            String tmp13 = getString(R.string.upload_fail_line1, new Object[]{this.mTransInfo.mDeviceName});
            this.mLine1View.setText(tmp13);
            this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
            String tmp14 = getString(R.string.upload_fail_line1_2, new Object[]{this.mTransInfo.mFileName});
            this.mLine2View.setText(tmp14);
            this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
            String tmp15 = getString(R.string.download_fail_line3, new Object[]{BluetoothOppUtility.getStatusDescription(this, this.mTransInfo.mStatus, this.mTransInfo.mDeviceName)});
            this.mLine3View.setText(tmp15);
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            this.mLine5View.setVisibility(8);
        }
        if (BluetoothShare.isStatusError(this.mTransInfo.mStatus)) {
            this.mProgressTransfer.setVisibility(8);
            this.mPercentView.setVisibility(8);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) throws Throwable {
        switch (which) {
            case -2:
                if (this.mWhichDialog == 0 || this.mWhichDialog == 3) {
                    getContentResolver().delete(this.mUri, null, null);
                    String msg = "";
                    if (this.mWhichDialog == 0) {
                        msg = getString(R.string.bt_toast_3, new Object[]{this.mTransInfo.mDeviceName});
                    } else if (this.mWhichDialog == 3) {
                        msg = getString(R.string.bt_toast_6, new Object[]{this.mTransInfo.mDeviceName});
                    }
                    Toast.makeText((Context) this, (CharSequence) msg, 0).show();
                    ((NotificationManager) getSystemService("notification")).cancel(this.mTransInfo.mID);
                } else if (this.mWhichDialog == 5) {
                    BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                }
                break;
            case -1:
                if (this.mWhichDialog == 1) {
                    BluetoothOppUtility.openReceivedFile(this, this.mTransInfo.mFileName, this.mTransInfo.mFileType, this.mTransInfo.mTimeStamp, this.mUri);
                    BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                    ((NotificationManager) getSystemService("notification")).cancel(this.mTransInfo.mID);
                } else if (this.mWhichDialog == 5) {
                    BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                    ((NotificationManager) getSystemService("notification")).cancel(this.mTransInfo.mID);
                    Uri uri = BluetoothOppUtility.originalUri(Uri.parse(this.mTransInfo.mFileUri));
                    BluetoothOppSendFileInfo sendFileInfo = BluetoothOppSendFileInfo.generateFileInfo(this, uri, this.mTransInfo.mFileType, false);
                    Uri uri2 = BluetoothOppUtility.generateUri(uri, sendFileInfo);
                    BluetoothOppUtility.putSendFileInfo(uri2, sendFileInfo);
                    this.mTransInfo.mFileUri = uri2.toString();
                    BluetoothOppUtility.retryTransfer(this, this.mTransInfo);
                    BluetoothDevice remoteDevice = this.mAdapter.getRemoteDevice(this.mTransInfo.mDestAddr);
                    Toast.makeText((Context) this, (CharSequence) getString(R.string.bt_toast_4, new Object[]{BluetoothOppManager.getInstance(this).getDeviceName(remoteDevice)}), 0).show();
                } else if (this.mWhichDialog == 4) {
                    BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                    ((NotificationManager) getSystemService("notification")).cancel(this.mTransInfo.mID);
                }
                break;
        }
        finish();
    }

    private void updateProgressbar() {
        this.mTransInfo = BluetoothOppUtility.queryRecord(this, this.mUri);
        if (this.mTransInfo != null) {
            if (this.mTransInfo.mTotalBytes == 0) {
                this.mProgressTransfer.setMax(100);
            } else {
                this.mProgressTransfer.setMax(this.mTransInfo.mTotalBytes);
            }
            this.mProgressTransfer.setProgress(this.mTransInfo.mCurrentBytes);
            this.mPercentView.setText(BluetoothOppUtility.formatProgressText(this.mTransInfo.mTotalBytes, this.mTransInfo.mCurrentBytes));
            if (!this.mIsComplete && BluetoothShare.isStatusCompleted(this.mTransInfo.mStatus) && this.mNeedUpdateButton) {
                displayWhichDialog();
                updateButton();
                customizeViewContent();
            }
        }
    }

    private void updateButton() {
        if (this.mWhichDialog == 1) {
            this.mAlert.getButton(-2).setVisibility(8);
            this.mAlert.getButton(-1).setText(getString(R.string.download_succ_ok));
            return;
        }
        if (this.mWhichDialog == 2) {
            this.mAlert.setIcon(this.mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            this.mAlert.getButton(-2).setVisibility(8);
            this.mAlert.getButton(-1).setText(getString(R.string.download_fail_ok));
        } else if (this.mWhichDialog == 4) {
            this.mAlert.getButton(-2).setVisibility(8);
            this.mAlert.getButton(-1).setText(getString(R.string.upload_succ_ok));
        } else if (this.mWhichDialog == 5) {
            this.mAlert.setIcon(this.mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            this.mAlert.getButton(-1).setText(getString(R.string.upload_fail_ok));
            this.mAlert.getButton(-2).setText(getString(R.string.upload_fail_cancel));
        }
    }
}
