package com.mediatek.lbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.mediatek.lbs.em2.utils.AgpsInterface;
import com.mediatek.lbs.em2.utils.SuplProfile;
import com.mediatek.settings.FeatureOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class LbsReceiver extends BroadcastReceiver {
    private Context mContext;
    private String mCurOperatorCode;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        String action = intent.getAction();
        Log.d("LbsReceiver", "Receive : " + action);
        if (FeatureOption.MTK_AGPS_APP && FeatureOption.MTK_GPS_SUPPORT) {
            if (action.equals("com.mediatek.agps.OMACP_UPDATED")) {
                handleAgpsOmaProfileUpdate(context, intent);
            } else if (action.equals("com.mediatek.omacp.settings")) {
                handleOmaCpSetting(context, intent);
            } else if (action.equals("com.mediatek.omacp.capability")) {
                handleOmaCpCapability(context, intent);
            }
        }
    }

    private void handleAgpsOmaProfileUpdate(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        context.getSharedPreferences("omacp_profile", 0).edit().putString("name", extras.getString("name")).putString("addr", extras.getString("addr")).putInt("port", extras.getInt("port")).putInt("tls", extras.getInt("tls")).putString("code", extras.getString("code")).putString("addrType", extras.getString("addrType")).putString("providerId", extras.getString("providerId")).putString("defaultApn", extras.getString("defaultApn")).putBoolean("changed", true).commit();
    }

    private void handleOmaCpSetting(Context context, Intent intent) {
        HashMap map;
        if (!FeatureOption.MTK_OMACP_SUPPORT) {
            Log.d("LbsReceiver", "handleOmaCpSetting, MTK OMACP NOT SUPPOR ");
            return;
        }
        String stringExtra = intent.getStringExtra("appId");
        if (stringExtra == null || !stringExtra.equals("ap0004")) {
            Log.d("LbsReceiver", "get the OMA CP broadcast, but it's not for AGPS");
            return;
        }
        int intExtra = intent.getIntExtra("subId", 0);
        String stringExtra2 = intent.getStringExtra("PROVIDER-ID");
        String stringExtra3 = intent.getStringExtra("NAME");
        String str = "";
        String str2 = "";
        String str3 = "";
        Bundle extras = intent.getExtras();
        ArrayList arrayList = (ArrayList) extras.get("APPADDR");
        if (arrayList != null && !arrayList.isEmpty() && (map = (HashMap) arrayList.get(0)) != null) {
            str2 = (String) map.get("ADDR");
            str3 = (String) map.get("ADDRTYPE");
        }
        if (str2 == null || str2.equals("")) {
            Log.d("LbsReceiver", "Invalid oma cp pushed supl address");
            dealWithOmaUpdataResult(false, "Invalide oma cp pushed supl address");
            return;
        }
        ArrayList arrayList2 = (ArrayList) extras.get("TO-NAPID");
        if (arrayList2 != null && !arrayList2.isEmpty()) {
            str = (String) arrayList2.get(0);
        }
        initSIMStatus(intExtra);
        String str4 = this.mCurOperatorCode;
        if (str4 == null || "".equals(str4)) {
            dealWithOmaUpdataResult(false, "invalide profile code:" + str4);
            return;
        }
        Intent intent2 = new Intent("com.mediatek.agps.OMACP_UPDATED");
        intent2.putExtra("code", str4);
        intent2.putExtra("addr", str2);
        try {
            AgpsInterface agpsInterface = new AgpsInterface();
            SuplProfile suplProfile = agpsInterface.getAgpsConfig().curSuplProfile;
            suplProfile.addr = str2;
            if (stringExtra2 != null && !"".equals(stringExtra2)) {
                intent2.putExtra("providerId", stringExtra2);
                suplProfile.providerId = stringExtra2;
            }
            if (stringExtra3 != null && !"".equals(stringExtra3)) {
                intent2.putExtra("name", stringExtra3);
                suplProfile.name = stringExtra3;
            }
            if (str != null && !"".equals(str)) {
                intent2.putExtra("defaultApn", str);
                suplProfile.defaultApn = str;
            }
            if (str3 != null && !"".equals(str3)) {
                intent2.putExtra("addrType", str3);
                suplProfile.addressType = str3;
            }
            intent2.putExtra("port", 7275);
            suplProfile.port = 7275;
            intent2.putExtra("tls", 1);
            suplProfile.tls = true;
            this.mContext.sendBroadcast(intent2);
            agpsInterface.setSuplProfile(suplProfile);
        } catch (IOException e) {
            Log.d("LbsReceiver", "IOException happened when new AgpsInterface object");
        }
        dealWithOmaUpdataResult(true, "OMA CP update successfully finished");
    }

    private void handleOmaCpCapability(Context context, Intent intent) {
        if (!FeatureOption.MTK_OMACP_SUPPORT) {
            Log.d("LbsReceiver", "handleOmaCpCapability, MTK OMACP NOT SUPPOR ");
            return;
        }
        Intent intent2 = new Intent();
        intent2.setAction("com.mediatek.omacp.capability.result");
        intent2.putExtra("appId", "ap0004");
        intent2.putExtra("supl", true);
        intent2.putExtra("supl_provider_id", false);
        intent2.putExtra("supl_server_name", true);
        intent2.putExtra("supl_to_napid", false);
        intent2.putExtra("supl_server_addr", true);
        intent2.putExtra("supl_addr_type", false);
        Log.d("LbsReceiver", "Feedback OMA CP capability information");
        context.sendBroadcast(intent2);
    }

    private void initSIMStatus(int i) {
        int simState;
        this.mCurOperatorCode = "";
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (TelephonyManager.getDefault().getPhoneCount() >= 2) {
            int slotIndex = SubscriptionManager.getSlotIndex(i);
            simState = telephonyManager.getSimState(slotIndex);
            Log.d("LbsReceiver", "SubId : " + i + " SlotId : " + slotIndex + " simStatus: " + simState);
            if (5 == simState) {
                this.mCurOperatorCode = telephonyManager.getSimOperator(i);
            }
        } else {
            simState = telephonyManager.getSimState();
            if (5 == simState) {
                this.mCurOperatorCode = telephonyManager.getSimOperator();
            }
        }
        Log.d("LbsReceiver", "SubId : " + i + " Status : " + simState + " OperatorCode : " + this.mCurOperatorCode);
    }

    private void dealWithOmaUpdataResult(boolean z, String str) {
        Toast.makeText(this.mContext, "Deal with OMA CP operation : " + str, 1).show();
        Log.d("LbsReceiver", "Deal with OMA UP operation : " + str);
        Intent intent = new Intent();
        intent.setAction("com.mediatek.omacp.settings.result");
        intent.putExtra("appId", "ap0004");
        intent.putExtra("result", z);
        this.mContext.sendBroadcast(intent);
    }
}
