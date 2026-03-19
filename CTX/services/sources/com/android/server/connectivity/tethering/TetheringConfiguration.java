package com.android.server.connectivity.tethering;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.util.SharedLog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringJoiner;

public class TetheringConfiguration {

    @VisibleForTesting
    public static final int DUN_NOT_REQUIRED = 0;
    public static final int DUN_REQUIRED = 1;
    public static final int DUN_UNSPECIFIED = 2;
    private final String[] DEFAULT_IPV4_DNS = {"8.8.4.4", "8.8.8.8"};
    public final String[] defaultIPv4DNS;
    public final String[] dhcpRanges;
    public final int dunCheck;
    public final boolean isDunRequired;
    public final Collection<Integer> preferredUpstreamIfaceTypes;
    public final String[] provisioningApp;
    public final String provisioningAppNoUi;
    public final String[] tetherableBluetoothRegexs;
    public final String[] tetherableUsbRegexs;
    public final String[] tetherableWifiRegexs;
    private static final String TAG = TetheringConfiguration.class.getSimpleName();
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String[] DHCP_DEFAULT_RANGE = {"192.168.42.2", "192.168.42.254", "192.168.43.2", "192.168.43.254", "192.168.44.2", "192.168.44.254", "192.168.45.2", "192.168.45.254", "192.168.46.2", "192.168.46.254", "192.168.47.2", "192.168.47.254", "192.168.48.2", "192.168.48.254", "192.168.49.2", "192.168.49.254"};

    public TetheringConfiguration(Context context, SharedLog sharedLog) {
        SharedLog sharedLogForSubComponent = sharedLog.forSubComponent("config");
        this.tetherableUsbRegexs = getResourceStringArray(context, R.array.config_displayCompositionColorModes);
        this.tetherableWifiRegexs = getResourceStringArray(context, R.array.config_displayCompositionColorSpaces);
        this.tetherableBluetoothRegexs = getResourceStringArray(context, R.array.config_device_state_postures);
        this.dunCheck = checkDunRequired(context);
        sharedLogForSubComponent.log("DUN check returned: " + dunCheckString(this.dunCheck));
        this.preferredUpstreamIfaceTypes = getUpstreamIfaceTypes(context, this.dunCheck);
        this.isDunRequired = this.preferredUpstreamIfaceTypes.contains(4);
        this.dhcpRanges = getDhcpRanges(context);
        this.defaultIPv4DNS = copy(this.DEFAULT_IPV4_DNS);
        this.provisioningApp = getResourceStringArray(context, R.array.config_cell_retries_per_error_code);
        this.provisioningAppNoUi = getProvisioningAppNoUi(context);
        sharedLogForSubComponent.log(toString());
    }

    public boolean isUsb(String str) {
        return matchesDownstreamRegexs(str, this.tetherableUsbRegexs);
    }

    public boolean isWifi(String str) {
        return matchesDownstreamRegexs(str, this.tetherableWifiRegexs);
    }

    public boolean isBluetooth(String str) {
        return matchesDownstreamRegexs(str, this.tetherableBluetoothRegexs);
    }

    public boolean hasMobileHotspotProvisionApp() {
        return !TextUtils.isEmpty(this.provisioningAppNoUi);
    }

    public void dump(PrintWriter printWriter) {
        dumpStringArray(printWriter, "tetherableUsbRegexs", this.tetherableUsbRegexs);
        dumpStringArray(printWriter, "tetherableWifiRegexs", this.tetherableWifiRegexs);
        dumpStringArray(printWriter, "tetherableBluetoothRegexs", this.tetherableBluetoothRegexs);
        printWriter.print("isDunRequired: ");
        printWriter.println(this.isDunRequired);
        dumpStringArray(printWriter, "preferredUpstreamIfaceTypes", preferredUpstreamNames(this.preferredUpstreamIfaceTypes));
        dumpStringArray(printWriter, "dhcpRanges", this.dhcpRanges);
        dumpStringArray(printWriter, "defaultIPv4DNS", this.defaultIPv4DNS);
        dumpStringArray(printWriter, "provisioningApp", this.provisioningApp);
        printWriter.print("provisioningAppNoUi: ");
        printWriter.println(this.provisioningAppNoUi);
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(" ");
        stringJoiner.add(String.format("tetherableUsbRegexs:%s", makeString(this.tetherableUsbRegexs)));
        stringJoiner.add(String.format("tetherableWifiRegexs:%s", makeString(this.tetherableWifiRegexs)));
        stringJoiner.add(String.format("tetherableBluetoothRegexs:%s", makeString(this.tetherableBluetoothRegexs)));
        stringJoiner.add(String.format("isDunRequired:%s", Boolean.valueOf(this.isDunRequired)));
        stringJoiner.add(String.format("preferredUpstreamIfaceTypes:%s", makeString(preferredUpstreamNames(this.preferredUpstreamIfaceTypes))));
        stringJoiner.add(String.format("provisioningApp:%s", makeString(this.provisioningApp)));
        stringJoiner.add(String.format("provisioningAppNoUi:%s", this.provisioningAppNoUi));
        return String.format("TetheringConfiguration{%s}", stringJoiner.toString());
    }

    private static void dumpStringArray(PrintWriter printWriter, String str, String[] strArr) {
        printWriter.print(str);
        printWriter.print(": ");
        if (strArr != null) {
            StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
            for (String str2 : strArr) {
                stringJoiner.add(str2);
            }
            printWriter.print(stringJoiner.toString());
        } else {
            printWriter.print("null");
        }
        printWriter.println();
    }

    private static String makeString(String[] strArr) {
        if (strArr == null) {
            return "null";
        }
        StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
        for (String str : strArr) {
            stringJoiner.add(str);
        }
        return stringJoiner.toString();
    }

    private static String[] preferredUpstreamNames(Collection<Integer> collection) {
        if (collection != null) {
            String[] strArr = new String[collection.size()];
            int i = 0;
            Iterator<Integer> it = collection.iterator();
            while (it.hasNext()) {
                strArr[i] = ConnectivityManager.getNetworkTypeName(it.next().intValue());
                i++;
            }
            return strArr;
        }
        return null;
    }

    public static int checkDunRequired(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager != null) {
            return telephonyManager.getTetherApnRequired();
        }
        return 2;
    }

    private static String dunCheckString(int i) {
        switch (i) {
            case 0:
                return "DUN_NOT_REQUIRED";
            case 1:
                return "DUN_REQUIRED";
            case 2:
                return "DUN_UNSPECIFIED";
            default:
                return String.format("UNKNOWN (%s)", Integer.valueOf(i));
        }
    }

    private static Collection<Integer> getUpstreamIfaceTypes(Context context, int i) {
        int[] intArray = context.getResources().getIntArray(R.array.config_disabledUntilUsedPreinstalledImes);
        ArrayList arrayList = new ArrayList(intArray.length);
        for (int i2 : intArray) {
            if (i2 != 0) {
                switch (i2) {
                    case 4:
                        if (i == 0) {
                            continue;
                        }
                        break;
                    case 5:
                        if (i == 1) {
                            continue;
                        }
                        break;
                }
                arrayList.add(Integer.valueOf(i2));
            }
        }
        if (i == 1) {
            appendIfNotPresent(arrayList, 4);
        } else if (i == 0) {
            appendIfNotPresent(arrayList, 0);
            appendIfNotPresent(arrayList, 5);
        } else if (!containsOneOf(arrayList, 4, 0, 5)) {
            arrayList.add(0);
            arrayList.add(5);
        }
        prependIfNotPresent(arrayList, 9);
        return arrayList;
    }

    private static boolean matchesDownstreamRegexs(String str, String[] strArr) {
        for (String str2 : strArr) {
            if (str.matches(str2)) {
                return true;
            }
        }
        return false;
    }

    private static String[] getDhcpRanges(Context context) {
        String[] resourceStringArray = getResourceStringArray(context, R.array.config_disabledDreamComponents);
        if (resourceStringArray.length > 0 && resourceStringArray.length % 2 == 0) {
            return resourceStringArray;
        }
        return copy(DHCP_DEFAULT_RANGE);
    }

    private static String getProvisioningAppNoUi(Context context) {
        try {
            return context.getResources().getString(R.string.android_upgrading_complete);
        } catch (Resources.NotFoundException e) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
    }

    private static String[] getResourceStringArray(Context context, int i) {
        try {
            String[] stringArray = context.getResources().getStringArray(i);
            return stringArray != null ? stringArray : EMPTY_STRING_ARRAY;
        } catch (Resources.NotFoundException e) {
            return EMPTY_STRING_ARRAY;
        }
    }

    private static String[] copy(String[] strArr) {
        return (String[]) Arrays.copyOf(strArr, strArr.length);
    }

    private static void prependIfNotPresent(ArrayList<Integer> arrayList, int i) {
        if (arrayList.contains(Integer.valueOf(i))) {
            return;
        }
        arrayList.add(0, Integer.valueOf(i));
    }

    private static void appendIfNotPresent(ArrayList<Integer> arrayList, int i) {
        if (arrayList.contains(Integer.valueOf(i))) {
            return;
        }
        arrayList.add(Integer.valueOf(i));
    }

    private static boolean containsOneOf(ArrayList<Integer> arrayList, Integer... numArr) {
        for (Integer num : numArr) {
            if (arrayList.contains(num)) {
                return true;
            }
        }
        return false;
    }
}
