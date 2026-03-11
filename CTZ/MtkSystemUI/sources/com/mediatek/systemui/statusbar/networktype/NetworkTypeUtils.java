package com.mediatek.systemui.statusbar.networktype;

import android.telephony.ServiceState;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import java.util.HashMap;
import java.util.Map;

public class NetworkTypeUtils {
    static final Map<Integer, Integer> sNetworkTypeIcons = new HashMap<Integer, Integer>() {
        {
            put(5, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(6, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(12, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(14, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(4, Integer.valueOf(R.drawable.stat_sys_network_type_1x));
            put(7, Integer.valueOf(R.drawable.stat_sys_network_type_1x));
            put(2, Integer.valueOf(R.drawable.stat_sys_network_type_e));
            put(3, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(13, Integer.valueOf(R.drawable.stat_sys_network_type_4g));
            put(8, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(9, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(10, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(15, Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(18, 0);
        }
    };

    public static int getNetworkTypeIcon(ServiceState serviceState, NetworkControllerImpl.Config config, boolean z) {
        int i = 0;
        if (!z) {
            return 0;
        }
        int networkType = getNetworkType(serviceState);
        Integer numValueOf = sNetworkTypeIcons.get(Integer.valueOf(networkType));
        if (numValueOf == null) {
            if (networkType != 0) {
                i = config.showAtLeast3G ? R.drawable.stat_sys_network_type_3g : R.drawable.stat_sys_network_type_g;
            }
            numValueOf = Integer.valueOf(i);
        }
        return numValueOf.intValue();
    }

    private static int getNetworkType(ServiceState serviceState) {
        if (serviceState != null) {
            return serviceState.getDataNetworkType() != 0 ? serviceState.getDataNetworkType() : serviceState.getVoiceNetworkType();
        }
        return 0;
    }
}
