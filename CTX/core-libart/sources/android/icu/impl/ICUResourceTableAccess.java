package android.icu.impl;

import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;

public class ICUResourceTableAccess {
    public static String getTableString(String str, ULocale uLocale, String str2, String str3, String str4) {
        return getTableString((ICUResourceBundle) UResourceBundle.getBundleInstance(str, uLocale.getBaseName()), str2, (String) null, str3, str4);
    }

    public static String getTableString(ICUResourceBundle iCUResourceBundle, String str, String str2, String str3, String str4) {
        ICUResourceBundle iCUResourceBundleFindWithFallback;
        ICUResourceBundle iCUResourceBundleFindWithFallback2;
        String strFindStringWithFallback;
        String currentLanguageID;
        String strFindStringWithFallback2;
        String str5 = null;
        while (true) {
            try {
                iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback(str);
            } catch (Exception e) {
            }
            if (iCUResourceBundleFindWithFallback == null) {
                return str4;
            }
            if (str2 != null) {
                iCUResourceBundleFindWithFallback2 = iCUResourceBundleFindWithFallback.findWithFallback(str2);
            } else {
                iCUResourceBundleFindWithFallback2 = iCUResourceBundleFindWithFallback;
            }
            if (iCUResourceBundleFindWithFallback2 != null) {
                strFindStringWithFallback2 = iCUResourceBundleFindWithFallback2.findStringWithFallback(str3);
                if (strFindStringWithFallback2 != null) {
                    break;
                }
                str5 = strFindStringWithFallback2;
                if (str2 == null) {
                }
                strFindStringWithFallback = iCUResourceBundleFindWithFallback.findStringWithFallback("Fallback");
                if (strFindStringWithFallback != null) {
                }
            } else {
                if (str2 == null) {
                    if (str.equals("Countries")) {
                        currentLanguageID = LocaleIDs.getCurrentCountryID(str3);
                    } else if (str.equals("Languages")) {
                        currentLanguageID = LocaleIDs.getCurrentLanguageID(str3);
                    } else {
                        currentLanguageID = null;
                    }
                    if (currentLanguageID != null) {
                        strFindStringWithFallback2 = iCUResourceBundleFindWithFallback.findStringWithFallback(currentLanguageID);
                        if (strFindStringWithFallback2 != null) {
                            break;
                        }
                        str5 = strFindStringWithFallback2;
                    }
                }
                strFindStringWithFallback = iCUResourceBundleFindWithFallback.findStringWithFallback("Fallback");
                if (strFindStringWithFallback != null) {
                    return str4;
                }
                if (strFindStringWithFallback.length() == 0) {
                    strFindStringWithFallback = "root";
                }
                if (strFindStringWithFallback.equals(iCUResourceBundleFindWithFallback.getULocale().getName())) {
                    return str4;
                }
                iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(iCUResourceBundle.getBaseName(), strFindStringWithFallback);
            }
            return (str5 == null || str5.length() <= 0) ? str4 : str5;
        }
    }
}
