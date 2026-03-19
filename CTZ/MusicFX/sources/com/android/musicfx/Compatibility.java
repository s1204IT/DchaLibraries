package com.android.musicfx;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.util.Iterator;
import java.util.List;

public class Compatibility {
    private static final boolean LOG = Log.isLoggable("MusicFXCompat", 3);

    public static class Redirector extends Activity {
        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            Compatibility.log("Compatibility Activity called from " + getCallingPackage());
            Intent intent = new Intent(getIntent());
            intent.addFlags(33554432);
            SharedPreferences sharedPreferences = getSharedPreferences("musicfx", 0);
            String string = sharedPreferences.getString("defaultpanelpackage", null);
            String string2 = sharedPreferences.getString("defaultpanelname", null);
            Compatibility.log("read " + string + "/" + string2 + " as default");
            if (string == null || string2 == null) {
                Log.e("MusicFXCompat", "no default set!");
                intent.setComponent(new ComponentName(this, (Class<?>) ActivityMusic.class));
                Intent intent2 = new Intent(this, (Class<?>) Service.class);
                intent2.putExtra("defPackage", getPackageName());
                intent2.putExtra("defName", ActivityMusic.class.getName());
                startService(intent2);
            } else {
                intent.setComponent(new ComponentName(string, string2));
            }
            startActivity(intent);
            finish();
        }
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Compatibility.log("received");
            Intent intent2 = new Intent(context, (Class<?>) Service.class);
            intent2.putExtra("reason", intent);
            context.startService(intent2);
        }
    }

    public static class Service extends IntentService {
        PackageManager mPackageManager;

        public Service() {
            super("CompatibilityService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Compatibility.log("handleintent");
            if (this.mPackageManager == null) {
                this.mPackageManager = getPackageManager();
            }
            String stringExtra = intent.getStringExtra("defPackage");
            String stringExtra2 = intent.getStringExtra("defName");
            if (stringExtra != null && stringExtra2 != null) {
                setDefault(stringExtra, stringExtra2);
                return;
            }
            Intent intent2 = (Intent) intent.getParcelableExtra("reason");
            Bundle extras = intent2.getExtras();
            if (extras != null) {
                extras.size();
            }
            Compatibility.log("intentservice saw: " + intent2 + " " + extras);
            Uri data = intent2.getData();
            if (data != null) {
                pickDefaultControlPanel(data.toString().substring(8));
            }
        }

        private void pickDefaultControlPanel(String str) {
            List<ResolveInfo> listQueryIntentActivities = this.mPackageManager.queryIntentActivities(new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"), 512);
            Compatibility.log("found: " + listQueryIntentActivities.size());
            SharedPreferences sharedPreferences = getSharedPreferences("musicfx", 0);
            ResolveInfo resolveInfo = null;
            String string = sharedPreferences.getString("defaultpanelpackage", null);
            String string2 = sharedPreferences.getString("defaultpanelname", null);
            Compatibility.log("saved default: " + string2);
            Iterator<ResolveInfo> it = listQueryIntentActivities.iterator();
            ResolveInfo resolveInfo2 = null;
            ResolveInfo resolveInfo3 = null;
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ResolveInfo next = it.next();
                if (next.activityInfo.name.equals(Redirector.class.getName())) {
                    Compatibility.log("skipping " + next);
                } else {
                    Compatibility.log("considering " + next);
                    if (next.activityInfo.name.equals(string2) && next.activityInfo.packageName.equals(string) && next.activityInfo.enabled) {
                        Compatibility.log("default: " + string2);
                        resolveInfo = next;
                        break;
                    }
                    if (next.activityInfo.packageName.equals(str)) {
                        Compatibility.log("choosing newly installed package " + str);
                    } else if (resolveInfo2 != null || next.activityInfo.packageName.equals(getPackageName())) {
                        resolveInfo3 = next;
                    }
                    resolveInfo2 = next;
                }
            }
            if (resolveInfo == null) {
                if (resolveInfo2 != null) {
                    resolveInfo = resolveInfo2;
                } else {
                    if (resolveInfo3 == null) {
                        Log.e("MusicFXCompat", "No control panels found!");
                        return;
                    }
                    resolveInfo = resolveInfo3;
                }
            }
            setDefault(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }

        private void setDefault(String str, String str2) {
            setupReceivers(this.mPackageManager.queryBroadcastReceivers(new Intent("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION"), 512), str);
            setupReceivers(this.mPackageManager.queryBroadcastReceivers(new Intent("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION"), 512), str);
            SharedPreferences.Editor editorEdit = getSharedPreferences("musicfx", 0).edit();
            editorEdit.putString("defaultpanelpackage", str);
            editorEdit.putString("defaultpanelname", str2);
            editorEdit.commit();
            Compatibility.log("wrote " + str + "/" + str2 + " as default");
        }

        private void setupReceivers(List<ResolveInfo> list, String str) {
            for (ResolveInfo resolveInfo : list) {
                ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                if (resolveInfo.activityInfo.packageName.equals(str)) {
                    Compatibility.log("enabling receiver " + resolveInfo);
                    this.mPackageManager.setComponentEnabledSetting(componentName, 1, 1);
                } else {
                    Compatibility.log("disabling receiver " + resolveInfo);
                    this.mPackageManager.setComponentEnabledSetting(componentName, 2, 1);
                }
            }
        }
    }

    private static void log(String str) {
        if (LOG) {
            Log.d("MusicFXCompat", str);
        }
    }
}
