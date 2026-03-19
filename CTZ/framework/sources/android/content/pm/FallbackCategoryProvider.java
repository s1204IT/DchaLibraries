package android.content.pm;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FallbackCategoryProvider {
    private static final String TAG = "FallbackCategoryProvider";
    private static final ArrayMap<String, Integer> sFallbacks = new ArrayMap<>();

    public static void loadFallbacks() {
        sFallbacks.clear();
        if (SystemProperties.getBoolean("fw.ignore_fb_categories", false)) {
            Log.d(TAG, "Ignoring fallback categories");
            return;
        }
        AssetManager assetManager = new AssetManager();
        assetManager.addAssetPath("/system/framework/framework-res.apk");
        Throwable th = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new Resources(assetManager, null, null).openRawResource(R.raw.fallback_categories)));
            while (true) {
                try {
                    try {
                        String line = bufferedReader.readLine();
                        if (line != null) {
                            if (line.charAt(0) != '#') {
                                String[] strArrSplit = line.split(",");
                                if (strArrSplit.length == 2) {
                                    sFallbacks.put(strArrSplit[0], Integer.valueOf(Integer.parseInt(strArrSplit[1])));
                                }
                            }
                        } else {
                            Log.d(TAG, "Found " + sFallbacks.size() + " fallback categories");
                            bufferedReader.close();
                            return;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    if (th == null) {
                    }
                    throw th3;
                }
                if (th == null) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable th4) {
                        th.addSuppressed(th4);
                    }
                } else {
                    bufferedReader.close();
                }
                throw th3;
            }
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "Failed to read fallback categories", e);
        }
    }

    public static int getFallbackCategory(String str) {
        return sFallbacks.getOrDefault(str, -1).intValue();
    }
}
