package android.support.v7.view;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.content.res.ConfigurationHelper;
import android.support.v4.view.ViewConfigurationCompat;
import android.view.ViewConfiguration;

public class ActionBarPolicy {
    private Context mContext;

    public static ActionBarPolicy get(Context context) {
        return new ActionBarPolicy(context);
    }

    private ActionBarPolicy(Context context) {
        this.mContext = context;
    }

    public int getMaxActionButtons() {
        Resources res = this.mContext.getResources();
        int widthDp = ConfigurationHelper.getScreenWidthDp(res);
        int heightDp = ConfigurationHelper.getScreenHeightDp(res);
        int smallest = ConfigurationHelper.getSmallestScreenWidthDp(res);
        if (smallest <= 600 && widthDp <= 600) {
            if (widthDp <= 960 || heightDp <= 720) {
                if (widthDp > 720 && heightDp > 960) {
                    return 5;
                }
                if (widthDp >= 500) {
                    return 4;
                }
                if (widthDp <= 640 || heightDp <= 480) {
                    if (widthDp > 480 && heightDp > 640) {
                        return 4;
                    }
                    if (widthDp >= 360) {
                        return 3;
                    }
                    return 2;
                }
                return 4;
            }
            return 5;
        }
        return 5;
    }

    public boolean showsOverflowMenuButton() {
        return Build.VERSION.SDK_INT >= 19 || !ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(this.mContext));
    }

    public int getEmbeddedMenuWidthLimit() {
        return this.mContext.getResources().getDisplayMetrics().widthPixels / 2;
    }
}
