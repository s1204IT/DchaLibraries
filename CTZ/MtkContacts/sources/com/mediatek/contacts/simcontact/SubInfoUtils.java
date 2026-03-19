package com.mediatek.contacts.simcontact;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import com.android.contacts.util.PermissionsUtil;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.MtkSubscriptionInfo;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import java.util.List;

public class SubInfoUtils {
    public static List<SubscriptionInfo> getActivatedSubInfoList() {
        Context applicationContext = GlobalEnv.getApplicationContext();
        if (PermissionsUtil.hasPermission(applicationContext, "android.permission.READ_PHONE_STATE")) {
            return SubscriptionManager.from(applicationContext).getActiveSubscriptionInfoList();
        }
        Log.w("SubInfoUtils", "getActivatedSubInfoList has no basic permissions!");
        return null;
    }

    public static int getActivatedSubInfoCount() {
        Context applicationContext = GlobalEnv.getApplicationContext();
        if (PermissionsUtil.hasPermission(applicationContext, "android.permission.READ_PHONE_STATE")) {
            return SubscriptionManager.from(applicationContext).getActiveSubscriptionInfoCount();
        }
        Log.w("SubInfoUtils", "getActivatedSubInfoCount has no basic permissions!");
        return 0;
    }

    public static SubscriptionInfo getSubInfoUsingSubId(int i) {
        List<SubscriptionInfo> activatedSubInfoList;
        if (checkSubscriber(i) && (activatedSubInfoList = getActivatedSubInfoList()) != null && activatedSubInfoList.size() > 0) {
            for (SubscriptionInfo subscriptionInfo : activatedSubInfoList) {
                if (subscriptionInfo.getSubscriptionId() == i) {
                    return subscriptionInfo;
                }
            }
        }
        return null;
    }

    public static int[] getActiveSubscriptionIdList() {
        return SubscriptionManager.from(GlobalEnv.getApplicationContext()).getActiveSubscriptionIdList();
    }

    public static boolean iconTintChange(int i, int i2) {
        List<SubscriptionInfo> activatedSubInfoList = getActivatedSubInfoList();
        if (activatedSubInfoList == null) {
            return false;
        }
        for (SubscriptionInfo subscriptionInfo : activatedSubInfoList) {
            if (subscriptionInfo.getSubscriptionId() == i2 && i == subscriptionInfo.getIconTint()) {
                return false;
            }
        }
        return true;
    }

    public static int getSlotIdUsingSubId(int i) {
        if (!checkSubscriber(i)) {
            return -1;
        }
        SubscriptionInfo subInfoUsingSubId = getSubInfoUsingSubId(i);
        return subInfoUsingSubId == null ? getInvalidSlotId() : subInfoUsingSubId.getSimSlotIndex();
    }

    public static String getDisplaynameUsingSubId(int i) {
        SubscriptionInfo subInfoUsingSubId;
        CharSequence displayName;
        if (!checkSubscriber(i) || (subInfoUsingSubId = getSubInfoUsingSubId(i)) == null || (displayName = subInfoUsingSubId.getDisplayName()) == null) {
            return null;
        }
        return displayName.toString();
    }

    public static int getColorUsingSubId(int i) {
        SubscriptionInfo subInfoUsingSubId;
        if (checkSubscriber(i) && (subInfoUsingSubId = getSubInfoUsingSubId(i)) != null) {
            return subInfoUsingSubId.getIconTint();
        }
        return -1;
    }

    public static Uri getIccProviderUri(int i) {
        if (!checkSubscriber(i)) {
            return null;
        }
        if (SimCardUtils.isUsimOrCsimType(i)) {
            return ContentUris.withAppendedId(Uri.parse("content://icc/pbr/subId"), i);
        }
        return ContentUris.withAppendedId(Uri.parse("content://icc/adn/subId"), i);
    }

    public static boolean checkSubscriber(int i) {
        if (i >= 1) {
            return true;
        }
        return false;
    }

    public static boolean isActiveForSubscriber(int i) {
        if (getSubInfoUsingSubId(i) != null) {
            return true;
        }
        return false;
    }

    public static String getMtkPhoneBookServiceName() {
        return "mtksimphonebook";
    }

    public static int getInvalidSlotId() {
        return -1;
    }

    public static int getInvalidSubId() {
        return -1;
    }

    public static MtkSubscriptionInfo getSubscriptionInfo(int i) {
        Context applicationContext = GlobalEnv.getApplicationContext();
        if (PermissionsUtil.hasPermission(applicationContext, "android.permission.READ_PHONE_STATE")) {
            return MtkSubscriptionManager.getSubInfo(applicationContext.getPackageName(), i);
        }
        Log.w("SubInfoUtils", "getSubscriptionInfo has no basic permissions!");
        return null;
    }

    public static Bitmap getIconBitmap(int i) {
        MtkSubscriptionInfo subscriptionInfo;
        if (checkSubscriber(i) && (subscriptionInfo = getSubscriptionInfo(i)) != null) {
            return subscriptionInfo.createIconBitmap(GlobalEnv.getApplicationContext(), -1, ContactsSystemProperties.MTK_GEMINI_SUPPORT);
        }
        return null;
    }

    public static Drawable getIconDrawable(int i) {
        Bitmap iconBitmap = getIconBitmap(i);
        if (iconBitmap == null) {
            return null;
        }
        return new BitmapDrawable(iconBitmap);
    }
}
