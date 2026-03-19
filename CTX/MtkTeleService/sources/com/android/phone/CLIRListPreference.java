package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.preference.ListPreference;
import android.telephony.CarrierConfigManager;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.CallSettingUtils;

public class CLIRListPreference extends ListPreference {
    private static final String LOG_TAG = "CLIRListPreference";
    private final boolean DBG;
    public int[] clirArray;
    private final MyHandler mHandler;
    private Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;

    public CLIRListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.DBG = true;
        this.mHandler = new MyHandler();
    }

    public CLIRListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean z) {
        CallSettingUtils.DialogType dialogTipsType = CallSettingUtils.getDialogTipsType(getContext(), this.mPhone.getSubId());
        if (dialogTipsType == CallSettingUtils.DialogType.DATA_OPEN || dialogTipsType == CallSettingUtils.DialogType.DATA_ROAMING) {
            CallSettingUtils.showDialogTips(getContext(), this.mPhone.getSubId(), dialogTipsType, null);
            return;
        }
        super.onDialogClosed(z);
        this.mPhone.setOutgoingCallerIdDisplay(findIndexOfValue(getValue()), this.mHandler.obtainMessage(1));
        if (this.mTcpListener != null) {
            this.mTcpListener.onStarted(this, false);
        }
    }

    public void init(TimeConsumingPreferenceListener timeConsumingPreferenceListener, boolean z, Phone phone) {
        if (ExtensionManager.getCallFeaturesSettingExt().escapeCLIRInit()) {
            Log.d(LOG_TAG, "init: escape");
            return;
        }
        this.mPhone = phone;
        this.mTcpListener = timeConsumingPreferenceListener;
        if (!z) {
            Log.i(LOG_TAG, "init: requesting CLIR");
            this.mPhone.getOutgoingCallerIdDisplay(this.mHandler.obtainMessage(0, 0, 0));
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, true);
            }
        }
    }

    public void handleGetCLIRResult(int[] iArr) {
        this.clirArray = iArr;
        int i = 1;
        boolean z = iArr[1] == 1 || iArr[1] == 3 || iArr[1] == 4;
        if (needToEnableClirSetting(getContext(), this.mPhone.getSubId())) {
            setEnabled(z);
        } else {
            setEnabled(false);
        }
        int i2 = iArr[1];
        if (i2 != 1) {
            switch (i2) {
                case 3:
                case 4:
                    switch (iArr[0]) {
                        case 2:
                            i = 2;
                            break;
                    }
                default:
                    i = 0;
                    break;
            }
        }
        setValueIndex(i);
        int i3 = R.string.sum_default_caller_id;
        switch (i) {
            case 1:
                i3 = R.string.sum_hide_caller_id;
                break;
            case 2:
                i3 = R.string.sum_show_caller_id;
                break;
        }
        setSummary(i3);
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_CLIR = 0;
        static final int MESSAGE_SET_CLIR = 1;

        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetCLIRResponse(message);
                    break;
                case 1:
                    handleSetCLIRResponse(message);
                    break;
            }
        }

        private void handleGetCLIRResponse(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (message.arg2 == 1) {
                CLIRListPreference.this.mTcpListener.onFinished(CLIRListPreference.this, false);
            } else {
                CLIRListPreference.this.mTcpListener.onFinished(CLIRListPreference.this, true);
            }
            CLIRListPreference.this.clirArray = null;
            if (asyncResult.exception != null) {
                Log.i(CLIRListPreference.LOG_TAG, "handleGetCLIRResponse: ar.exception=" + asyncResult.exception);
                if (asyncResult.exception instanceof CommandException) {
                    CLIRListPreference.this.handleCommandException(asyncResult.exception);
                    return;
                } else {
                    CLIRListPreference.this.mTcpListener.onError(CLIRListPreference.this, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                    return;
                }
            }
            if (asyncResult.userObj instanceof Throwable) {
                Log.i(CLIRListPreference.LOG_TAG, "handleGetCLIRResponse: ar.throwable=" + asyncResult.userObj);
                CLIRListPreference.this.mTcpListener.onError(CLIRListPreference.this, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                return;
            }
            int[] iArr = (int[]) asyncResult.result;
            if (iArr.length != 2) {
                CLIRListPreference.this.mTcpListener.onError(CLIRListPreference.this, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                return;
            }
            ExtensionManager.getCallFeaturesSettingExt().resetImsPdnOverSSComplete(CLIRListPreference.this.getContext(), message.arg2);
            Log.i(CLIRListPreference.LOG_TAG, "handleGetCLIRResponse: CLIR successfully queried, clirArray[0]=" + iArr[0] + ", clirArray[1]=" + iArr[1]);
            CLIRListPreference.this.handleGetCLIRResult(iArr);
        }

        private void handleSetCLIRResponse(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d(CLIRListPreference.LOG_TAG, "handleSetCLIRResponse: ar.exception=" + asyncResult.exception);
            }
            Log.d(CLIRListPreference.LOG_TAG, "handleSetCLIRResponse: re get");
            CLIRListPreference.this.mPhone.getOutgoingCallerIdDisplay(obtainMessage(0, 1, 1, asyncResult.exception));
        }
    }

    private boolean needToEnableClirSetting(Context context, int i) {
        boolean z;
        PersistableBundle configForSubId = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfigForSubId(i);
        if (configForSubId != null) {
            z = configForSubId.getBoolean("mtk_show_clir_setting_bool");
        } else {
            z = true;
        }
        Log.d(LOG_TAG, "enableClirSetting:" + z);
        return z;
    }

    private void handleCommandException(CommandException commandException) {
        setEnabled(false);
        setSummary("");
        if (commandException.getCommandError() != CommandException.Error.REQUEST_NOT_SUPPORTED) {
            this.mTcpListener.onException(this, commandException);
        } else {
            Log.d(LOG_TAG, "receive REQUEST_NOT_SUPPORTED CLIR !!");
        }
    }
}
