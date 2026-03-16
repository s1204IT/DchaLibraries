package android.service.fingerprint;

import android.content.ContentResolver;
import android.net.ProxyInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;

public class FingerprintUtils {
    private static final boolean DEBUG = true;
    private static final String TAG = "FingerprintUtils";

    public static int[] getFingerprintIdsForUser(ContentResolver res, int userId) {
        String fingerIdsRaw = Settings.Secure.getStringForUser(res, Settings.Secure.USER_FINGERPRINT_IDS, userId);
        int[] result = new int[0];
        if (!TextUtils.isEmpty(fingerIdsRaw)) {
            String[] fingerStringIds = fingerIdsRaw.replace("[", ProxyInfo.LOCAL_EXCL_LIST).replace("]", ProxyInfo.LOCAL_EXCL_LIST).split(", ");
            result = new int[fingerStringIds.length];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Integer.decode(fingerStringIds[i]).intValue();
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Error when parsing finger id " + fingerStringIds[i]);
                }
            }
        }
        return result;
    }

    public static void addFingerprintIdForUser(int fingerId, ContentResolver res, int userId) {
        int[] fingerIds = getFingerprintIdsForUser(res, userId);
        if (fingerId != 0) {
            for (int i : fingerIds) {
                if (i == fingerId) {
                    return;
                }
            }
            int[] newList = Arrays.copyOf(fingerIds, fingerIds.length + 1);
            newList[fingerIds.length] = fingerId;
            Settings.Secure.putStringForUser(res, Settings.Secure.USER_FINGERPRINT_IDS, Arrays.toString(newList), userId);
        }
    }

    public static boolean removeFingerprintIdForUser(int fingerId, ContentResolver res, int userId) {
        if (fingerId == 0) {
            throw new IllegalStateException("Bad fingerId");
        }
        int[] fingerIds = getFingerprintIdsForUser(res, userId);
        int[] resultIds = Arrays.copyOf(fingerIds, fingerIds.length);
        int resultCount = 0;
        for (int i = 0; i < fingerIds.length; i++) {
            if (fingerId != fingerIds[i]) {
                resultIds[resultCount] = fingerIds[i];
                resultCount++;
            }
        }
        if (resultCount <= 0) {
            return false;
        }
        Settings.Secure.putStringForUser(res, Settings.Secure.USER_FINGERPRINT_IDS, Arrays.toString(Arrays.copyOf(resultIds, resultCount)), userId);
        return true;
    }
}
