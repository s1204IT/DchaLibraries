package com.android.wallpaper.livepicker;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import java.io.IOException;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class LiveWallpaperChange extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parcelable obj = getIntent().getParcelableExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT");
        if (obj == null || !(obj instanceof ComponentName)) {
            Log.w("CHANGE_LIVE_WALLPAPER", "No LIVE_WALLPAPER_COMPONENT extra supplied");
            finish();
            return;
        }
        ComponentName comp = (ComponentName) obj;
        Intent queryIntent = new Intent("android.service.wallpaper.WallpaperService");
        queryIntent.setPackage(comp.getPackageName());
        List<ResolveInfo> list = getPackageManager().queryIntentServices(queryIntent, 128);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo ri = list.get(i);
                if (ri.serviceInfo.name.equals(comp.getClassName())) {
                    try {
                        WallpaperInfo info = new WallpaperInfo(this, ri);
                        Intent intent = new Intent("android.service.wallpaper.WallpaperService");
                        intent.setClassName(info.getPackageName(), info.getServiceName());
                        LiveWallpaperPreview.showPreview(this, 0, intent, info);
                        return;
                    } catch (IOException e) {
                        Log.w("CHANGE_LIVE_WALLPAPER", "Bad wallpaper " + ri.serviceInfo, e);
                        finish();
                        return;
                    } catch (XmlPullParserException e2) {
                        Log.w("CHANGE_LIVE_WALLPAPER", "Bad wallpaper " + ri.serviceInfo, e2);
                        finish();
                        return;
                    }
                }
            }
        }
        Log.w("CHANGE_LIVE_WALLPAPER", "Not a live wallpaper: " + comp);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode);
        finish();
    }
}
