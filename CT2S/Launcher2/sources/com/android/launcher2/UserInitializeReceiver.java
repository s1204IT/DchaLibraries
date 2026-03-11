package com.android.launcher2;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import com.android.launcher.R;
import java.io.IOException;
import java.util.ArrayList;

public class UserInitializeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Resources resources = context.getResources();
        String packageName = resources.getResourcePackageName(R.array.wallpapers);
        ArrayList<Integer> list = new ArrayList<>();
        addWallpapers(resources, packageName, R.array.wallpapers, list);
        addWallpapers(resources, packageName, R.array.extra_wallpapers, list);
        WallpaperManager wpm = (WallpaperManager) context.getSystemService("wallpaper");
        for (int i = 1; i < list.size(); i++) {
            int resid = list.get(i).intValue();
            if (!wpm.hasResourceWallpaper(resid)) {
                try {
                    wpm.setResource(resid);
                    return;
                } catch (IOException e) {
                    return;
                }
            }
        }
    }

    private void addWallpapers(Resources resources, String packageName, int resid, ArrayList<Integer> outList) {
        String[] extras = resources.getStringArray(resid);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                outList.add(Integer.valueOf(res));
            }
        }
    }
}
