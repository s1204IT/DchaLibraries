package jp.co.omronsoft.iwnnime.ml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import java.util.List;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.controlpanel.KeyBoardSkinAddListPreference;

public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri;
        String installPackageName;
        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false) && (uri = intent.getData()) != null && (installPackageName = uri.getSchemeSpecificPart()) != null) {
            List<ResolveInfo> resolveInfo = WnnUtility.getPackageInfo(context, KeyBoardSkinAddListPreference.KEYBOARDSKINADD_ACTION);
            setKeyBoardImage(context, resolveInfo, installPackageName);
        }
    }

    private void setKeyBoardImage(Context context, List<ResolveInfo> resolveInfo, String installPackageName) {
        for (ResolveInfo info : resolveInfo) {
            ActivityInfo actInfo = info.activityInfo;
            String packagename = actInfo.packageName;
            if (packagename.equals(installPackageName)) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(ControlPanelPrefFragment.KEYBOARD_IMAGE_KEY, actInfo.name);
                editor.commit();
                WnnUtility.resetIme(false);
                return;
            }
        }
    }
}
