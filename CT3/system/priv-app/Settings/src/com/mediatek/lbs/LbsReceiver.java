package com.mediatek.lbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
        if (!FeatureOption.MTK_AGPS_APP || !FeatureOption.MTK_GPS_SUPPORT) {
            return;
        }
        if (action.equals("com.mediatek.agps.OMACP_UPDATED")) {
            handleAgpsOmaProfileUpdate(context, intent);
        } else if (action.equals("com.mediatek.omacp.settings")) {
            handleOmaCpSetting(context, intent);
        } else if (!action.equals("com.mediatek.omacp.capability")) {
        } else {
            handleOmaCpCapability(context, intent);
        }
    }

    private void handleAgpsOmaProfileUpdate(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SharedPreferences prefs = context.getSharedPreferences("omacp_profile", 0);
        prefs.edit().putString("name", bundle.getString("name")).putString("addr", bundle.getString("addr")).putInt("port", bundle.getInt("port")).putInt("tls", bundle.getInt("tls")).putString("code", bundle.getString("code")).putString("addrType", bundle.getString("addrType")).putString("providerId", bundle.getString("providerId")).putString("defaultApn", bundle.getString("defaultApn")).putBoolean("changed", true).commit();
    }

    private void handleOmaCpSetting(Context context, Intent intent) {
        HashMap<String, String> addrMap;
        if (!FeatureOption.MTK_OMACP_SUPPORT) {
            Log.d("LbsReceiver", "handleOmaCpSetting, MTK OMACP NOT SUPPOR ");
            return;
        }
        String appId = intent.getStringExtra("appId");
        if (appId == null || !appId.equals("ap0004")) {
            Log.d("LbsReceiver", "get the OMA CP broadcast, but it's not for AGPS");
            return;
        }
        int subId = intent.getIntExtra("subId", 0);
        String providerId = intent.getStringExtra("PROVIDER-ID");
        String slpName = intent.getStringExtra("NAME");
        String defaultApn = "";
        String address = "";
        String addressType = "";
        Bundle bundle = intent.getExtras();
        ArrayList<HashMap<String, String>> appAddrMapList = (ArrayList) bundle.get("APPADDR");
        if (appAddrMapList != null && !appAddrMapList.isEmpty() && (addrMap = appAddrMapList.get(0)) != null) {
            address = addrMap.get("ADDR");
            addressType = addrMap.get("ADDRTYPE");
        }
        if (address == null || address.equals("")) {
            Log.d("LbsReceiver", "Invalid oma cp pushed supl address");
            dealWithOmaUpdataResult(false, "Invalide oma cp pushed supl address");
            return;
        }
        ArrayList<String> defaultApnList = (ArrayList) bundle.get("TO-NAPID");
        if (defaultApnList != null && !defaultApnList.isEmpty()) {
            defaultApn = defaultApnList.get(0);
        }
        initSIMStatus(subId);
        String profileCode = this.mCurOperatorCode;
        if (profileCode == null || "".equals(profileCode)) {
            dealWithOmaUpdataResult(false, "invalide profile code:" + profileCode);
            return;
        }
        Intent mIntent = new Intent("com.mediatek.agps.OMACP_UPDATED");
        mIntent.putExtra("code", profileCode);
        mIntent.putExtra("addr", address);
        try {
            AgpsInterface agpsInterface = new AgpsInterface();
            SuplProfile profile = agpsInterface.getAgpsConfig().curSuplProfile;
            profile.addr = address;
            if (providerId != null && !"".equals(providerId)) {
                mIntent.putExtra("providerId", providerId);
                profile.providerId = providerId;
            }
            if (slpName != null && !"".equals(slpName)) {
                mIntent.putExtra("name", slpName);
                profile.name = slpName;
            }
            if (defaultApn != null && !"".equals(defaultApn)) {
                mIntent.putExtra("defaultApn", defaultApn);
                profile.defaultApn = defaultApn;
            }
            if (addressType != null && !"".equals(addressType)) {
                mIntent.putExtra("addrType", addressType);
                profile.addressType = addressType;
            }
            mIntent.putExtra("port", 7275);
            profile.port = 7275;
            mIntent.putExtra("tls", 1);
            profile.tls = true;
            this.mContext.sendBroadcast(mIntent);
            agpsInterface.setSuplProfile(profile);
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
        Intent it = new Intent();
        it.setAction("com.mediatek.omacp.capability.result");
        it.putExtra("appId", "ap0004");
        it.putExtra("supl", true);
        it.putExtra("supl_provider_id", false);
        it.putExtra("supl_server_name", true);
        it.putExtra("supl_to_napid", false);
        it.putExtra("supl_server_addr", true);
        it.putExtra("supl_addr_type", false);
        Log.d("LbsReceiver", "Feedback OMA CP capability information");
        context.sendBroadcast(it);
    }

    private void initSIMStatus(int subId) {
        int simStatus;
        this.mCurOperatorCode = "";
        TelephonyManager telMgr = (TelephonyManager) this.mContext.getSystemService("phone");
        if (TelephonyManager.getDefault().getPhoneCount() >= 2) {
            int slotId = SubscriptionManager.getSlotId(subId);
            simStatus = telMgr.getSimState(slotId);
            Log.d("LbsReceiver", "SubId : " + subId + " SlotId : " + slotId + " simStatus: " + simStatus);
            if (5 == simStatus) {
                this.mCurOperatorCode = telMgr.getSimOperator(subId);
            }
        } else {
            simStatus = telMgr.getSimState();
            if (5 == simStatus) {
                this.mCurOperatorCode = telMgr.getSimOperator();
            }
        }
        Log.d("LbsReceiver", "SubId : " + subId + " Status : " + simStatus + " OperatorCode : " + this.mCurOperatorCode);
    }

    private void dealWithOmaUpdataResult(boolean success, String message) {
        Toast.makeText(this.mContext, "Deal with OMA CP operation : " + message, 1).show();
        Log.d("LbsReceiver", "Deal with OMA UP operation : " + message);
        Intent it = new Intent();
        it.setAction("com.mediatek.omacp.settings.result");
        it.putExtra("appId", "ap0004");
        it.putExtra("result", success);
        this.mContext.sendBroadcast(it);
    }
}
