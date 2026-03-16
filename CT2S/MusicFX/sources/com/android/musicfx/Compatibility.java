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
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Compatibility.log("Compatibility Activity called from " + getCallingPackage());
            Intent i = new Intent(getIntent());
            i.addFlags(33554432);
            SharedPreferences pref = getSharedPreferences("musicfx", 0);
            String defPackage = pref.getString("defaultpanelpackage", null);
            String defName = pref.getString("defaultpanelname", null);
            Compatibility.log("read " + defPackage + "/" + defName + " as default");
            if (defPackage == null || defName == null) {
                Log.e("MusicFXCompat", "no default set!");
                i.setComponent(new ComponentName(this, (Class<?>) ActivityMusic.class));
                Intent updateIntent = new Intent(this, (Class<?>) Service.class);
                updateIntent.putExtra("defPackage", getPackageName());
                updateIntent.putExtra("defName", ActivityMusic.class.getName());
                startService(updateIntent);
            } else {
                i.setComponent(new ComponentName(defPackage, defName));
            }
            startActivity(i);
            finish();
        }
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Compatibility.log("received");
            Intent updateIntent = new Intent(context, (Class<?>) Service.class);
            updateIntent.putExtra("reason", intent);
            context.startService(updateIntent);
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
            String defPackage = intent.getStringExtra("defPackage");
            String defName = intent.getStringExtra("defName");
            if (defPackage != null && defName != null) {
                setDefault(defPackage, defName);
                return;
            }
            Intent packageIntent = (Intent) intent.getParcelableExtra("reason");
            Bundle b = packageIntent.getExtras();
            if (b != null) {
                b.size();
            }
            Compatibility.log("intentservice saw: " + packageIntent + " " + b);
            Uri packageUri = packageIntent.getData();
            if (packageUri != null) {
                String updatedPackage = packageUri.toString().substring(8);
                pickDefaultControlPanel(updatedPackage);
            }
        }

        private void pickDefaultControlPanel(String updatedPackage) {
            ResolveInfo defPanel = null;
            ResolveInfo otherPanel = null;
            ResolveInfo thisPanel = null;
            Intent i = new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL");
            List<ResolveInfo> ris = this.mPackageManager.queryIntentActivities(i, 512);
            Compatibility.log("found: " + ris.size());
            SharedPreferences pref = getSharedPreferences("musicfx", 0);
            String savedDefPackage = pref.getString("defaultpanelpackage", null);
            String savedDefName = pref.getString("defaultpanelname", null);
            Compatibility.log("saved default: " + savedDefName);
            Iterator<ResolveInfo> it = ris.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ResolveInfo foo = it.next();
                if (foo.activityInfo.name.equals(Redirector.class.getName())) {
                    Compatibility.log("skipping " + foo);
                } else {
                    Compatibility.log("considering " + foo);
                    if (foo.activityInfo.name.equals(savedDefName) && foo.activityInfo.packageName.equals(savedDefPackage) && foo.activityInfo.enabled) {
                        Compatibility.log("default: " + savedDefName);
                        defPanel = foo;
                        break;
                    } else if (foo.activityInfo.packageName.equals(updatedPackage)) {
                        Compatibility.log("choosing newly installed package " + updatedPackage);
                        otherPanel = foo;
                    } else if (otherPanel == null && !foo.activityInfo.packageName.equals(getPackageName())) {
                        otherPanel = foo;
                    } else {
                        thisPanel = foo;
                    }
                }
            }
            if (defPanel == null) {
                if (otherPanel == null) {
                    if (thisPanel == null) {
                        Log.e("MusicFXCompat", "No control panels found!");
                        return;
                    }
                    otherPanel = thisPanel;
                }
                defPanel = otherPanel;
            }
            String defPackage = defPanel.activityInfo.packageName;
            String defName = defPanel.activityInfo.name;
            setDefault(defPackage, defName);
        }

        private void setDefault(String defPackage, String defName) {
            Intent i = new Intent("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
            List<ResolveInfo> ris = this.mPackageManager.queryBroadcastReceivers(i, 512);
            setupReceivers(ris, defPackage);
            Intent i2 = new Intent("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
            List<ResolveInfo> ris2 = this.mPackageManager.queryBroadcastReceivers(i2, 512);
            setupReceivers(ris2, defPackage);
            SharedPreferences pref = getSharedPreferences("musicfx", 0);
            SharedPreferences.Editor ed = pref.edit();
            ed.putString("defaultpanelpackage", defPackage);
            ed.putString("defaultpanelname", defName);
            ed.commit();
            Compatibility.log("wrote " + defPackage + "/" + defName + " as default");
        }

        private void setupReceivers(List<ResolveInfo> ris, String defPackage) {
            for (ResolveInfo foo : ris) {
                ComponentName comp = new ComponentName(foo.activityInfo.packageName, foo.activityInfo.name);
                if (foo.activityInfo.packageName.equals(defPackage)) {
                    Compatibility.log("enabling receiver " + foo);
                    this.mPackageManager.setComponentEnabledSetting(comp, 1, 1);
                } else {
                    Compatibility.log("disabling receiver " + foo);
                    this.mPackageManager.setComponentEnabledSetting(comp, 2, 1);
                }
            }
        }
    }

    private static void log(String out) {
        if (LOG) {
            Log.d("MusicFXCompat", out);
        }
    }
}
