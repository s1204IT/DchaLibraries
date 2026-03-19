package com.mediatek.providers.contacts;

public class AccountUtils {
    public static String getLocalAccountSelection() {
        return "(account_name IS NULL AND account_type IS NULL OR account_type IN ('Local Phone Account' , 'SIM Account' , 'USIM Account' , 'RUIM Account' , 'CSIM Account'))";
    }

    public static String getSyncAccountSelection() {
        return "(account_type NOT IN ('Local Phone Account' , 'SIM Account' , 'USIM Account' , 'RUIM Account' , 'CSIM Account'))";
    }

    public static String getLocalSupportGroupAccountSelection() {
        return "(account_name IS NULL AND account_type IS NULL OR account_type IN ('Local Phone Account' , 'USIM Account'))";
    }

    public static boolean isLocalAccount(String str, String str2) {
        return (str == null && str2 == null) || (str != null && "('Local Phone Account' , 'SIM Account' , 'USIM Account' , 'RUIM Account' , 'CSIM Account')".contains(str));
    }

    public static boolean isSimAccount(String str) {
        return (str == null || !"('Local Phone Account' , 'SIM Account' , 'USIM Account' , 'RUIM Account' , 'CSIM Account')".contains(str) || str.equals("Local Phone Account")) ? false : true;
    }

    public static boolean isPresenceAccount(String str) {
        return str != null && str.equals("com.mediatek.presence");
    }
}
