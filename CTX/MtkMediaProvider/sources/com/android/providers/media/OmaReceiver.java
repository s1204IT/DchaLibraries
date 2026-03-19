package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import java.util.ArrayList;

public class OmaReceiver extends BroadcastReceiver {
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        MtkLog.v("MediaProvider/OmacpReceiver", "onReceive(" + intent.getAction() + ")");
        this.mContext = context;
        if ("com.mediatek.omacp.settings".equalsIgnoreCase(intent.getAction())) {
            saveRtspSetting(intent.getExtras());
        } else if ("com.mediatek.omacp.capability".equalsIgnoreCase(intent.getAction())) {
            sendRtspCapability();
        }
    }

    private void sendRtspCapability() {
        Intent intent = new Intent("com.mediatek.omacp.capability.result");
        intent.putExtra("appId", "554");
        intent.putExtra("rtsp", true);
        intent.putExtra("rtsp_to_proxy", false);
        intent.putExtra("rtsp_to_napid", false);
        intent.putExtra("rtsp_net_info", false);
        intent.putExtra("rtsp_min_udp_port", true);
        intent.putExtra("rtsp_max_udp_port", true);
        intent.putExtra("rtsp_name", false);
        intent.putExtra("rtsp_provider_id", false);
        intent.putExtra("rtsp_max_bandwidth", false);
        this.mContext.sendBroadcast(intent);
        MtkLog.v("MediaProvider/OmacpReceiver", "sendRtspCapability()...");
    }

    private void saveRtspSetting(Bundle bundle) {
        MtkLog.v("MediaProvider/OmacpReceiver", "saveRtspSetting(" + bundle + ")");
        if (bundle != null) {
            String string = bundle.getString("APPID");
            if ("554".equalsIgnoreCase(string)) {
                Intent intent = new Intent("com.mediatek.omacp.settings.result");
                intent.putExtra("appId", "554");
                intent.putExtra("result", writeSetting(bundle));
                this.mContext.sendBroadcast(intent);
                return;
            }
            MtkLog.v("MediaProvider/OmacpReceiver", "not rtsp app id. appid=" + string);
            return;
        }
        MtkLog.w("MediaProvider/OmacpReceiver", "extras is null. cannot set rtsp configuration!");
    }

    private boolean writeSetting(Bundle bundle) {
        MtkLog.v("MediaProvider/OmacpReceiver", "writeSetting(" + bundle + ")");
        bundle.getInt("simId");
        String[] strArr = {"mtk_rtsp_name", "mtk_rtsp_provider_id", "mtk_rtsp_max_bandwidth", "mtk_rtsp_min_udp_port", "mtk_rtsp_max_udp_port", "mtk_rtsp_to_proxy", "mtk_rtsp_to_napid", "mtk_rtsp_netinfo", "mtk_rtsp_sim_id", "mtk_rtsp_proxy_host", "mtk_rtsp_proxy_port", "mtk_rtsp_proxy_enabled"};
        String[] strArr2 = {bundle.getString("NAME"), bundle.getString("PROVIDER-ID"), bundle.getString("MAX-BANDWIDTH"), bundle.getString("MIN-UDP-PORT"), bundle.getString("MAX-UDP-PORT"), catString(bundle.getStringArrayList("TO-PROXY"), ","), catString(bundle.getStringArrayList("TO-NAPID"), ","), catString(bundle.getStringArrayList("NETINFO"), ";"), String.valueOf(bundle.getInt("simId")), "", "-1", "0"};
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int length = strArr.length;
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            if (Settings.System.putString(contentResolver, strArr[i2], strArr2[i2])) {
                i++;
            }
        }
        MtkLog.v("MediaProvider/OmacpReceiver", "writeSetting() count=" + i);
        return i > 0;
    }

    private String catString(ArrayList<String> arrayList, String str) {
        String str2;
        int i;
        int i2 = 0;
        int size = arrayList != null ? arrayList.size() : 0;
        if (size > 0) {
            String str3 = "";
            while (true) {
                i = size - 1;
                if (i2 >= i) {
                    break;
                }
                str3 = str3 + arrayList.get(i2) + str;
                i2++;
            }
            str2 = str3 + arrayList.get(i);
        } else {
            str2 = null;
        }
        MtkLog.v("MediaProvider/OmacpReceiver", "catString() return " + str2);
        return str2;
    }
}
