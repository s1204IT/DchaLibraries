package com.android.settings.datausage;

import android.net.NetworkTemplate;
import com.android.settingslib.NetworkPolicyEditor;

/* loaded from: classes.dex */
public interface DataUsageEditController {
    NetworkPolicyEditor getNetworkPolicyEditor();

    NetworkTemplate getNetworkTemplate();

    void updateDataUsage();
}
