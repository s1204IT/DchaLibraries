package com.android.internal.telephony.cdma;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.radio.V1_0.DataCallFailCause;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.WakeLockStateMachine;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public final class CdmaServiceCategoryProgramHandler extends WakeLockStateMachine {
    final CommandsInterface mCi;
    private final BroadcastReceiver mScpResultsReceiver;

    CdmaServiceCategoryProgramHandler(Context context, CommandsInterface commandsInterface) {
        super("CdmaServiceCategoryProgramHandler", context, null);
        this.mScpResultsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                sendScpResults();
                CdmaServiceCategoryProgramHandler.this.log("mScpResultsReceiver finished");
                CdmaServiceCategoryProgramHandler.this.sendMessage(2);
            }

            private void sendScpResults() {
                int resultCode = getResultCode();
                if (resultCode != -1 && resultCode != 1) {
                    CdmaServiceCategoryProgramHandler.this.loge("SCP results error: result code = " + resultCode);
                    return;
                }
                Bundle resultExtras = getResultExtras(false);
                if (resultExtras == null) {
                    CdmaServiceCategoryProgramHandler.this.loge("SCP results error: missing extras");
                    return;
                }
                String string = resultExtras.getString("sender");
                if (string == null) {
                    CdmaServiceCategoryProgramHandler.this.loge("SCP results error: missing sender extra.");
                    return;
                }
                ArrayList parcelableArrayList = resultExtras.getParcelableArrayList("results");
                if (parcelableArrayList == null) {
                    CdmaServiceCategoryProgramHandler.this.loge("SCP results error: missing results extra.");
                    return;
                }
                BearerData bearerData = new BearerData();
                bearerData.messageType = 2;
                bearerData.messageId = SmsMessage.getNextMessageId();
                bearerData.serviceCategoryProgramResults = parcelableArrayList;
                byte[] bArrEncode = BearerData.encode(bearerData);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(100);
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                try {
                    try {
                        try {
                            dataOutputStream.writeInt(DataCallFailCause.OEM_DCFAILCAUSE_6);
                            dataOutputStream.writeInt(0);
                            dataOutputStream.writeInt(0);
                            CdmaSmsAddress cdmaSmsAddress = CdmaSmsAddress.parse(PhoneNumberUtils.cdmaCheckAndProcessPlusCodeForSms(string));
                            dataOutputStream.write(cdmaSmsAddress.digitMode);
                            dataOutputStream.write(cdmaSmsAddress.numberMode);
                            dataOutputStream.write(cdmaSmsAddress.ton);
                            dataOutputStream.write(cdmaSmsAddress.numberPlan);
                            dataOutputStream.write(cdmaSmsAddress.numberOfDigits);
                            dataOutputStream.write(cdmaSmsAddress.origBytes, 0, cdmaSmsAddress.origBytes.length);
                            dataOutputStream.write(0);
                            dataOutputStream.write(0);
                            dataOutputStream.write(0);
                            dataOutputStream.write(bArrEncode.length);
                            dataOutputStream.write(bArrEncode, 0, bArrEncode.length);
                            CdmaServiceCategoryProgramHandler.this.mCi.sendCdmaSms(byteArrayOutputStream.toByteArray(), null);
                            dataOutputStream.close();
                        } catch (IOException e) {
                            CdmaServiceCategoryProgramHandler.this.loge("exception creating SCP results PDU", e);
                            dataOutputStream.close();
                        }
                    } catch (Throwable th) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e2) {
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                }
            }
        };
        this.mContext = context;
        this.mCi = commandsInterface;
    }

    static CdmaServiceCategoryProgramHandler makeScpHandler(Context context, CommandsInterface commandsInterface) {
        CdmaServiceCategoryProgramHandler cdmaServiceCategoryProgramHandler = new CdmaServiceCategoryProgramHandler(context, commandsInterface);
        cdmaServiceCategoryProgramHandler.start();
        return cdmaServiceCategoryProgramHandler;
    }

    @Override
    protected boolean handleSmsMessage(Message message) {
        if (message.obj instanceof SmsMessage) {
            return handleServiceCategoryProgramData((SmsMessage) message.obj);
        }
        loge("handleMessage got object of type: " + message.obj.getClass().getName());
        return false;
    }

    private boolean handleServiceCategoryProgramData(SmsMessage smsMessage) {
        ArrayList<? extends Parcelable> smsCbProgramData = smsMessage.getSmsCbProgramData();
        if (smsCbProgramData == null) {
            loge("handleServiceCategoryProgramData: program data list is null!");
            return false;
        }
        Intent intent = new Intent("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED");
        intent.setPackage(this.mContext.getResources().getString(R.string.action_bar_home_description_format));
        intent.putExtra("sender", smsMessage.getOriginatingAddress());
        intent.putParcelableArrayListExtra("program_data", smsCbProgramData);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        this.mContext.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS", 16, this.mScpResultsReceiver, getHandler(), -1, (String) null, (Bundle) null);
        return true;
    }
}
