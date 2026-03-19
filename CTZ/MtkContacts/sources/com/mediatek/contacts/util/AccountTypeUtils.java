package com.mediatek.contacts.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.sip.SipManager;
import android.telephony.TelephonyManager;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.google.common.collect.Lists;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import java.util.Locale;

public class AccountTypeUtils {
    public static Drawable getDisplayIconBySubId(Context context, int i, int i2, int i3, String str) {
        if (i2 != -1 && str != null) {
            Log.d("AccountTypeUtils", "[getDisplayIconBySubId] summaryrespackagename !=null");
            return context.getPackageManager().getDrawable(str, i3, null);
        }
        if (i2 == -1) {
            return null;
        }
        return SubInfoUtils.getIconDrawable(i);
    }

    public static boolean isAccountTypeIccCard(String str) {
        return "SIM Account".equals(str) || "USIM Account".equals(str) || "RUIM Account".equals(str) || "CSIM Account".equals(str);
    }

    public static String getDisplayAccountName(Context context, String str) {
        int subIdBySimAccountName = getSubIdBySimAccountName(context, str);
        return subIdBySimAccountName < 1 ? str : SubInfoUtils.getDisplaynameUsingSubId(subIdBySimAccountName);
    }

    public static int getSubIdBySimAccountName(Context context, String str) {
        for (AccountWithDataSet accountWithDataSet : AccountTypeManager.getInstance(context).getAccounts(true)) {
            if ((accountWithDataSet instanceof AccountWithDataSetEx) && accountWithDataSet.name.equals(str)) {
                return ((AccountWithDataSetEx) accountWithDataSet).getSubId();
            }
        }
        Log.d("AccountTypeUtils", "[getSubIdBySimAccountName]account " + Log.anonymize(str) + " is not sim account");
        return SubInfoUtils.getInvalidSubId();
    }

    public static void setStructuredPostalFiledList(DataKind dataKind, int i) {
        if (!Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage())) {
            dataKind.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, i));
            dataKind.fieldList.add(new AccountType.EditField("data5", R.string.postal_pobox, i));
            dataKind.fieldList.add(new AccountType.EditField("data6", R.string.postal_neighborhood, i));
            dataKind.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, i));
            dataKind.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, i));
            dataKind.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, i));
            dataKind.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, i).setOptional(true));
            return;
        }
        dataKind.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, i).setOptional(true));
        dataKind.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, i));
        dataKind.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, i));
        dataKind.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, i));
        dataKind.fieldList.add(new AccountType.EditField("data6", R.string.postal_neighborhood, i));
        dataKind.fieldList.add(new AccountType.EditField("data5", R.string.postal_pobox, i));
        dataKind.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, i));
    }

    public static boolean isAccountTypeSipSupport(Context context) {
        if (new TelephonyManager(context).isVoiceCapable() && SipManager.isVoipSupported(context)) {
            return true;
        }
        return false;
    }

    public static String getAccountTypeBySub(int i) {
        if (SimCardUtils.isSimInsertedBySub(i)) {
            return getSimAccountTypeBySub(i);
        }
        return null;
    }

    private static String getSimAccountTypeBySub(int i) {
        String str;
        String simTypeBySubId = SimCardUtils.getSimTypeBySubId(i);
        if (SimCardUtils.isUsimType(simTypeBySubId)) {
            str = "USIM Account";
        } else if (SimCardUtils.isSimType(simTypeBySubId)) {
            str = "SIM Account";
        } else if (SimCardUtils.isRuimType(simTypeBySubId)) {
            str = "RUIM Account";
        } else if (SimCardUtils.isCsimType(simTypeBySubId)) {
            str = "CSIM Account";
        } else {
            str = null;
        }
        Log.d("AccountTypeUtils", "[getAccountTypeUsingSubId]subId:" + i + ",AccountType:" + str);
        return str;
    }

    public static String getAccountNameUsingSubId(int i) {
        String str;
        String simTypeBySubId = SimCardUtils.getSimTypeBySubId(i);
        if (simTypeBySubId != null) {
            str = simTypeBySubId + i;
        } else {
            str = null;
        }
        Log.d("AccountTypeUtils", "[getAccountNameUsingSubId]subId:" + i + ",iccCardType =" + simTypeBySubId + ",accountName:" + Log.anonymize(str));
        return str;
    }

    public static boolean isUsim(String str) {
        return "USIM Account".equals(str);
    }

    public static boolean isSim(String str) {
        return "SIM Account".equals(str);
    }

    public static boolean isCsim(String str) {
        return "CSIM Account".equals(str);
    }

    public static boolean isRuim(String str) {
        return "RUIM Account".equals(str);
    }

    public static boolean isUsimOrCsim(String str) {
        return isUsim(str) || isCsim(str);
    }

    public static boolean isSimOrRuim(String str) {
        return isSim(str) || isRuim(str);
    }

    public static boolean isPhoneNumType(String str) {
        return "vnd.android.cursor.item/phone_v2".equals(str);
    }

    public static boolean isAasPhoneType(int i) {
        return 101 == i;
    }

    public static void addDataKindEmail(AccountType accountType) {
        try {
            DataKind dataKindAddKind = accountType.addKind(new DataKind("vnd.android.cursor.item/email_v2", R.string.emailLabelsGroup, 15, true));
            dataKindAddKind.actionHeader = new BaseAccountType.EmailActionInflater();
            dataKindAddKind.actionBody = new BaseAccountType.SimpleInflater("data1");
            dataKindAddKind.typeColumn = "data2";
            dataKindAddKind.typeOverallMax = 1;
            dataKindAddKind.typeList = Lists.newArrayList();
            dataKindAddKind.fieldList = Lists.newArrayList();
            dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
            Log.d("AccountTypeUtils", "[addDataKindEmail] add email data kind done");
        } catch (Exception e) {
            Log.d("AccountTypeUtils", "[addDataKindEmail] addkind Exception = " + e.getMessage());
        }
    }

    public static void removeDataKind(AccountType accountType, String str) {
        if (accountType.getKindForMimetype(str) != null) {
            Log.d("AccountTypeUtils", "[removeDataKind] mimeType = " + str + ", remove it");
            try {
                accountType.removeKind(str);
            } catch (Exception e) {
                Log.d("AccountTypeUtils", "[removeDataKind]remove kind Exception");
            }
        }
    }
}
