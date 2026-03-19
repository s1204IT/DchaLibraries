package com.android.managedprovisioning.model;

import android.content.Context;
import android.webkit.URLUtil;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.common.Utils;

public class CustomizationParams {

    @VisibleForTesting
    public static final int DEFAULT_STATUS_BAR_COLOR_ID = 17170443;
    public final int mainColor;
    public final String orgName;
    public final int statusBarColor;
    public final String supportUrl;

    public static CustomizationParams createInstance(ProvisioningParams provisioningParams, Context context, Utils utils) {
        int color;
        int accentColor;
        if (provisioningParams.mainColor != null) {
            accentColor = provisioningParams.mainColor.intValue();
            color = accentColor;
        } else {
            color = context.getColor(17170443);
            accentColor = utils.getAccentColor(context);
        }
        return new CustomizationParams(accentColor, color, provisioningParams.organizationName, URLUtil.isNetworkUrl(provisioningParams.supportUrl) ? provisioningParams.supportUrl : null);
    }

    private CustomizationParams(int i, int i2, String str, String str2) {
        this.mainColor = i;
        this.statusBarColor = i2;
        this.orgName = str;
        this.supportUrl = str2;
    }
}
