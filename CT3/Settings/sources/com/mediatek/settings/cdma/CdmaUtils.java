package com.mediatek.settings.cdma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.settings.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

public class CdmaUtils {
    public static boolean isSupportCdma(int subId) {
        boolean isSupportCdma = false;
        if (TelephonyManager.getDefault().getCurrentPhoneType(subId) == 2) {
            isSupportCdma = true;
        }
        Log.d("CdmaUtils", " isSupportCdma = " + isSupportCdma + ", subId = " + subId);
        return isSupportCdma;
    }

    public static boolean isCdmaCard(int slotId) {
        boolean isCdmaCard = false;
        if ((TelephonyManagerEx.getDefault().getIccAppFamily(slotId) & 2) != 0) {
            isCdmaCard = true;
        }
        Log.d("CdmaUtils", "slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

    public static void checkCdmaSimStatus(Context context, int simDetectNum) {
        Log.d("CdmaUtils", "startCdmaWaringDialog, simDetectNum = " + simDetectNum);
        boolean twoCdmaInsert = true;
        if (simDetectNum > 1) {
            for (int i = 0; i < simDetectNum; i++) {
                if (!isCdmaCard(i)) {
                    twoCdmaInsert = false;
                }
            }
        } else {
            twoCdmaInsert = false;
        }
        Log.d("CdmaUtils", "twoCdmaInsert = " + twoCdmaInsert);
        if (!twoCdmaInsert) {
            return;
        }
        Intent intent = new Intent("com.mediatek.settings.cdma.SIM_DIALOG");
        intent.addFlags(402653184);
        intent.putExtra("dialog_type", 0);
        context.startActivity(intent);
    }

    public static void startAlertCdmaDialog(Context context, int targetSubId, int actionType) {
        Intent intent = new Intent("com.mediatek.settings.cdma.SIM_DIALOG");
        intent.addFlags(402653184);
        intent.putExtra("dialog_type", 1);
        intent.putExtra("target_subid", targetSubId);
        intent.putExtra("action_type", actionType);
        context.startActivity(intent);
    }

    public static boolean isCdmaCardCompetion(Context context) {
        boolean isCdmaCard = true;
        boolean isCompetition = true;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        if (simCount == 2) {
            int i = 0;
            while (true) {
                if (i < simCount) {
                    isCdmaCard = isCdmaCard ? isCdmaCard(i) : false;
                    SubscriptionInfo subscriptionInfo = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(i);
                    if (subscriptionInfo != null) {
                        if (!isCompetition) {
                            isCompetition = false;
                        } else {
                            isCompetition = TelephonyManagerEx.getDefault().isInHomeNetwork(subscriptionInfo.getSubscriptionId());
                        }
                        i++;
                    } else {
                        isCompetition = false;
                        break;
                    }
                } else {
                    break;
                }
            }
        } else {
            isCdmaCard = false;
            isCompetition = false;
        }
        Log.d("CdmaUtils", "isCdmaCard: " + isCdmaCard + " isCompletition: " + isCompetition + " is Suppport SIM switch: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCard && isCompetition && !FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH;
    }

    public static boolean isCdmaCardCompetionForData(Context context) {
        return isCdmaCardCompetion(context);
    }

    public static boolean isNonOmhSimInOmhDevice(int subId) {
        boolean isOmhEnable = TelephonyManagerEx.getDefault().isOmhEnable(subId);
        boolean isOmhCard = TelephonyManagerEx.getDefault().isOmhCard(subId);
        Log.d("CdmaUtils", "isOmhEnable = " + isOmhEnable + "isOmhCard = " + isOmhCard);
        return isOmhEnable && !isOmhCard;
    }

    public static void startOmhWarningDialog(Context context) {
        Intent intent = new Intent("com.mediatek.settings.cdma.SIM_DIALOG");
        intent.addFlags(402653184);
        intent.putExtra("dialog_type", 2);
        context.startActivity(intent);
    }

    public static void startOmhDataPickDialog(Context context, int targetSubId) {
        Intent intent = new Intent("com.mediatek.settings.cdma.SIM_DIALOG");
        intent.addFlags(402653184);
        intent.putExtra("dialog_type", 3);
        intent.putExtra("target_subid", targetSubId);
        intent.putExtra("action_type", 0);
        context.startActivity(intent);
    }

    public static void recordNonOmhSub(Context context, int subId) {
        Log.d("CdmaUtils", "recordNonOmhSub, subId = " + subId);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return;
        }
        SharedPreferences sp = context.getSharedPreferences("sub_info", 0);
        String subList = sp.getString("non_omh_sub_list", "");
        Log.d("CdmaUtils", "recordNonOmhSub, subList = " + subList);
        StringBuilder builder = new StringBuilder(subList);
        if (subList.isEmpty()) {
            builder.append(Integer.toString(subId));
        } else {
            builder.append(",").append(Integer.toString(subId));
        }
        sp.edit().putString("non_omh_sub_list", builder.toString()).commit();
    }

    public static boolean hasNonOmhRecord(Context context, int subId) {
        SharedPreferences sp = context.getSharedPreferences("sub_info", 0);
        String subList = sp.getString("non_omh_sub_list", "");
        Log.d("CdmaUtils", "hasNonOmhRecord, subId = " + subId + ", subList = " + subList);
        String[] records = subList.split(",");
        if (records != null) {
            for (int i = 0; i < records.length; i++) {
                if (!TextUtils.isEmpty(records[i]) && TextUtils.isDigitsOnly(records[i]) && Integer.parseInt(records[i]) == subId) {
                    Log.d("CdmaUtils", "record hit~~");
                    return true;
                }
            }
        }
        return false;
    }
}
