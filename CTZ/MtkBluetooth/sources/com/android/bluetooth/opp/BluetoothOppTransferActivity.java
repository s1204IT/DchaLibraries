package com.android.bluetooth.opp;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
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
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import java.io.File;
import java.io.FileInputStream;

public class BluetoothOppTransferActivity extends AlertActivity implements DialogInterface.OnClickListener {
    public static final int DIALOG_RECEIVE_COMPLETE_FAIL = 2;
    public static final int DIALOG_RECEIVE_COMPLETE_SUCCESS = 1;
    public static final int DIALOG_RECEIVE_ONGOING = 0;
    public static final int DIALOG_SEND_COMPLETE_FAIL = 5;
    public static final int DIALOG_SEND_COMPLETE_SUCCESS = 4;
    public static final int DIALOG_SEND_ONGOING = 3;
    private static final String TAG = "BluetoothOppTransferActivity";
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
    private static final boolean D = Constants.DEBUG;
    private static final boolean V = Constants.VERBOSE;
    private View mView = null;
    private boolean mNeedUpdateButton = false;

    private class BluetoothTransferContentObserver extends ContentObserver {
        BluetoothTransferContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            if (BluetoothOppTransferActivity.V) {
                Log.v(BluetoothOppTransferActivity.TAG, "received db changes.");
            }
            BluetoothOppTransferActivity.this.mNeedUpdateButton = true;
            BluetoothOppTransferActivity.this.updateProgressbar();
        }
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mUri = getIntent().getData();
        this.mTransInfo = new BluetoothOppTransferInfo();
        this.mTransInfo = BluetoothOppUtility.queryRecord(this, this.mUri);
        if (this.mTransInfo == null) {
            if (V) {
                Log.e(TAG, "Error: Can not get data from db");
            }
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
        if (D) {
            Log.d(TAG, "onDestroy()");
        }
        if (this.mObserver != null) {
            getContentResolver().unregisterContentObserver(this.mObserver);
        }
        super.onDestroy();
    }

    private void displayWhichDialog() {
        int i = this.mTransInfo.mDirection;
        boolean zIsStatusSuccess = BluetoothShare.isStatusSuccess(this.mTransInfo.mStatus);
        boolean zIsStatusCompleted = BluetoothShare.isStatusCompleted(this.mTransInfo.mStatus);
        if (i == 1) {
            if (zIsStatusCompleted) {
                if (zIsStatusSuccess) {
                    this.mWhichDialog = 1;
                } else if (!zIsStatusSuccess) {
                    this.mWhichDialog = 2;
                }
            } else if (!zIsStatusCompleted) {
                this.mWhichDialog = 0;
            }
        } else if (i == 0) {
            if (zIsStatusCompleted) {
                if (zIsStatusSuccess) {
                    this.mWhichDialog = 4;
                } else if (!zIsStatusSuccess) {
                    this.mWhichDialog = 5;
                }
            } else if (!zIsStatusCompleted) {
                this.mWhichDialog = 3;
            }
        }
        if (V) {
            Log.v(TAG, " WhichDialog/dir/isComplete/failOrSuccess" + this.mWhichDialog + i + zIsStatusCompleted + zIsStatusSuccess);
        }
    }

    private void setUpDialog() {
        this.mPara = ((AlertActivity) this).mAlertParams;
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
            this.mLine1View.setText(getString(R.string.download_line1, this.mTransInfo.mDeviceName));
            this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
            this.mLine2View.setText(getString(R.string.download_line2, this.mTransInfo.mFileName));
            this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
            String string = getString(R.string.download_line3, Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes));
            this.mLine3View.setText(string);
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            if (this.mWhichDialog == 0) {
                string = getString(R.string.download_line5);
            } else if (this.mWhichDialog == 1) {
                string = getString(R.string.download_succ_line5);
            }
            this.mLine5View.setText(string);
        } else if (this.mWhichDialog == 3 || this.mWhichDialog == 4) {
            this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
            this.mLine1View.setText(getString(R.string.upload_line1, this.mTransInfo.mDeviceName));
            this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
            this.mLine2View.setText(getString(R.string.download_line2, this.mTransInfo.mFileName));
            this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
            String string2 = getString(R.string.upload_line3, this.mTransInfo.mFileType, Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes));
            this.mLine3View.setText(string2);
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            if (this.mWhichDialog == 3) {
                string2 = getString(R.string.upload_line5);
            } else if (this.mWhichDialog == 4) {
                string2 = getString(R.string.upload_succ_line5);
            }
            this.mLine5View.setText(string2);
        } else if (this.mWhichDialog == 2) {
            if (this.mTransInfo.mStatus == 494) {
                this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
                this.mLine1View.setText(getString(R.string.bt_sm_2_1, this.mTransInfo.mDeviceName));
                this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
                this.mLine2View.setText(getString(R.string.download_fail_line2, this.mTransInfo.mFileName));
                this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
                this.mLine3View.setText(getString(R.string.bt_sm_2_2, Formatter.formatFileSize(this, this.mTransInfo.mTotalBytes)));
            } else {
                this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
                this.mLine1View.setText(getString(R.string.download_fail_line1));
                this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
                this.mLine2View.setText(getString(R.string.download_fail_line2, this.mTransInfo.mFileName));
                this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
                this.mLine3View.setText(getString(R.string.download_fail_line3, BluetoothOppUtility.getStatusDescription(this, this.mTransInfo.mStatus, this.mTransInfo.mDeviceName)));
            }
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            this.mLine5View.setVisibility(8);
        } else if (this.mWhichDialog == 5) {
            this.mLine1View = (TextView) this.mView.findViewById(R.id.line1_view);
            this.mLine1View.setText(getString(R.string.upload_fail_line1, this.mTransInfo.mDeviceName));
            this.mLine2View = (TextView) this.mView.findViewById(R.id.line2_view);
            this.mLine2View.setText(getString(R.string.upload_fail_line1_2, this.mTransInfo.mFileName));
            this.mLine3View = (TextView) this.mView.findViewById(R.id.line3_view);
            this.mLine3View.setText(getString(R.string.download_fail_line3, BluetoothOppUtility.getStatusDescription(this, this.mTransInfo.mStatus, this.mTransInfo.mDeviceName)));
            this.mLine5View = (TextView) this.mView.findViewById(R.id.line5_view);
            this.mLine5View.setVisibility(8);
        }
        if (BluetoothShare.isStatusError(this.mTransInfo.mStatus)) {
            this.mProgressTransfer.setVisibility(8);
            this.mPercentView.setVisibility(8);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) throws Throwable {
        String str;
        BluetoothOppSendFileInfo bluetoothOppSendFileInfoGenerateFileInfo;
        long j;
        switch (i) {
            case -2:
                if (this.mWhichDialog != 0 && this.mWhichDialog != 3) {
                    if (this.mWhichDialog == 5) {
                        BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                    }
                } else {
                    getContentResolver().delete(this.mUri, null, null);
                    String string = "";
                    if (this.mWhichDialog == 0) {
                        string = getString(R.string.bt_toast_3, this.mTransInfo.mDeviceName);
                    } else if (this.mWhichDialog == 3) {
                        string = getString(R.string.bt_toast_6, this.mTransInfo.mDeviceName);
                    }
                    Toast.makeText((Context) this, (CharSequence) string, 0).show();
                    ((NotificationManager) getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(this.mTransInfo.mID);
                }
                break;
            case -1:
                if (this.mWhichDialog != 1) {
                    if (this.mWhichDialog != 5) {
                        if (this.mWhichDialog == 4) {
                            BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                            ((NotificationManager) getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(this.mTransInfo.mID);
                        }
                        break;
                    } else {
                        BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                        ((NotificationManager) getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(this.mTransInfo.mID);
                        BluetoothOppSendFileInfo bluetoothOppSendFileInfo = BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR;
                        Uri uriOriginalUri = BluetoothOppUtility.originalUri(Uri.parse(this.mTransInfo.mFileUri));
                        if (this.mTransInfo.mFilePath != null) {
                            str = this.mTransInfo.mFilePath;
                        } else {
                            str = this.mTransInfo.mFileName;
                        }
                        if (V) {
                            Log.d(TAG, "Query filePath = " + str);
                        }
                        if (str != null) {
                            try {
                                if (!str.equals("")) {
                                    File file = new File(str);
                                    FileInputStream fileInputStream = new FileInputStream(file);
                                    long j2 = this.mTransInfo.mTotalBytes;
                                    long length = file.length();
                                    if (V) {
                                        Log.d(TAG, "file length = " + length);
                                    }
                                    if (this.mTransInfo.mTotalBytes == length || length <= 0) {
                                        j = j2;
                                    } else {
                                        Log.e(TAG, "DB length is wrong (" + Long.toString(this.mTransInfo.mTotalBytes) + "), using file length (" + Long.toString(length) + ")");
                                        j = length;
                                    }
                                    this.mTransInfo.mFileName = this.mTransInfo.mFileName.substring(this.mTransInfo.mFileName.lastIndexOf("/") + 1);
                                    bluetoothOppSendFileInfoGenerateFileInfo = new BluetoothOppSendFileInfo(this.mTransInfo.mFileName, this.mTransInfo.mFileType, j, fileInputStream, 0);
                                    if (V) {
                                        Log.d(TAG, "name = " + this.mTransInfo.mFileName + ", path = " + this.mTransInfo.mFilePath);
                                    }
                                    bluetoothOppSendFileInfoGenerateFileInfo.mFilePath = this.mTransInfo.mFilePath;
                                } else {
                                    bluetoothOppSendFileInfoGenerateFileInfo = BluetoothOppSendFileInfo.generateFileInfo(this, uriOriginalUri, this.mTransInfo.mFileType, false);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                                e.printStackTrace();
                                bluetoothOppSendFileInfoGenerateFileInfo = BluetoothOppSendFileInfo.generateFileInfo(this, uriOriginalUri, this.mTransInfo.mFileType, false);
                            }
                            if (bluetoothOppSendFileInfoGenerateFileInfo != null) {
                                Uri uriGenerateUri = BluetoothOppUtility.generateUri(uriOriginalUri, bluetoothOppSendFileInfoGenerateFileInfo);
                                BluetoothOppUtility.putSendFileInfo(uriGenerateUri, bluetoothOppSendFileInfoGenerateFileInfo);
                                this.mTransInfo.mFileUri = uriGenerateUri.toString();
                                BluetoothOppUtility.retryTransfer(this, this.mTransInfo);
                            }
                            Toast.makeText((Context) this, (CharSequence) getString(R.string.bt_toast_4, BluetoothOppManager.getInstance(this).getDeviceName(this.mAdapter.getRemoteDevice(this.mTransInfo.mDestAddr))), 0).show();
                            break;
                        }
                    }
                } else {
                    BluetoothOppUtility.openReceivedFile(this, this.mTransInfo.mFileName, this.mTransInfo.mFileType, this.mTransInfo.mTimeStamp, this.mUri);
                    BluetoothOppUtility.updateVisibilityToHidden(this, this.mUri);
                    ((NotificationManager) getSystemService(BluetoothMapContract.RECEPTION_STATE_NOTIFICATION)).cancel(this.mTransInfo.mID);
                    break;
                }
                break;
        }
        finish();
    }

    private void updateProgressbar() {
        this.mTransInfo = BluetoothOppUtility.queryRecord(this, this.mUri);
        if (this.mTransInfo == null) {
            if (V) {
                Log.e(TAG, "Error: Can not get data from db");
                return;
            }
            return;
        }
        this.mProgressTransfer.setMax(100);
        if (this.mTransInfo.mTotalBytes != 0) {
            if (V) {
                Log.v(TAG, "mCurrentBytes: " + this.mTransInfo.mCurrentBytes + " mTotalBytes: " + this.mTransInfo.mTotalBytes + " (" + ((int) ((this.mTransInfo.mCurrentBytes * 100) / this.mTransInfo.mTotalBytes)) + "%)");
            }
            this.mProgressTransfer.setProgress((int) ((this.mTransInfo.mCurrentBytes * 100) / this.mTransInfo.mTotalBytes));
        } else {
            this.mProgressTransfer.setProgress(100);
        }
        this.mPercentView.setText(BluetoothOppUtility.formatProgressText(this.mTransInfo.mTotalBytes, this.mTransInfo.mCurrentBytes));
        if (!this.mIsComplete && BluetoothShare.isStatusCompleted(this.mTransInfo.mStatus) && this.mNeedUpdateButton) {
            if (this.mObserver != null) {
                getContentResolver().unregisterContentObserver(this.mObserver);
                this.mObserver = null;
            }
            displayWhichDialog();
            updateButton();
            customizeViewContent();
        }
    }

    private void updateButton() {
        if (this.mWhichDialog == 1) {
            ((AlertActivity) this).mAlert.getButton(-2).setVisibility(8);
            ((AlertActivity) this).mAlert.getButton(-1).setText(getString(R.string.download_succ_ok));
            return;
        }
        if (this.mWhichDialog == 2) {
            ((AlertActivity) this).mAlert.setIcon(((AlertActivity) this).mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            ((AlertActivity) this).mAlert.getButton(-2).setVisibility(8);
            ((AlertActivity) this).mAlert.getButton(-1).setText(getString(R.string.download_fail_ok));
        } else if (this.mWhichDialog == 4) {
            ((AlertActivity) this).mAlert.getButton(-2).setVisibility(8);
            ((AlertActivity) this).mAlert.getButton(-1).setText(getString(R.string.upload_succ_ok));
        } else if (this.mWhichDialog == 5) {
            ((AlertActivity) this).mAlert.setIcon(((AlertActivity) this).mAlert.getIconAttributeResId(android.R.attr.alertDialogIcon));
            ((AlertActivity) this).mAlert.getButton(-1).setText(getString(R.string.upload_fail_ok));
            ((AlertActivity) this).mAlert.getButton(-2).setText(getString(R.string.upload_fail_cancel));
        }
    }
}
