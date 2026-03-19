package com.android.printservice.recommendation;

import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.printservice.recommendation.RecommendationInfo;
import android.printservice.recommendation.RecommendationService;
import android.util.Log;
import com.android.printservice.recommendation.RemotePrintServicePlugin;
import com.android.printservice.recommendation.plugin.google.CloudPrintPlugin;
import com.android.printservice.recommendation.plugin.hp.HPRecommendationPlugin;
import com.android.printservice.recommendation.plugin.mdnsFilter.MDNSFilterPlugin;
import com.android.printservice.recommendation.plugin.mdnsFilter.VendorConfig;
import com.android.printservice.recommendation.plugin.mopria.MopriaRecommendationPlugin;
import com.android.printservice.recommendation.plugin.samsung.SamsungRecommendationPlugin;
import com.android.printservice.recommendation.plugin.xerox.XeroxPrintServiceRecommendationPlugin;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class RecommendationServiceImpl extends RecommendationService implements RemotePrintServicePlugin.OnChangedListener {
    private WifiManager.MulticastLock mMultiCastLock;
    private ArrayList<RemotePrintServicePlugin> mPlugins;

    public void onConnected() {
        int i;
        WifiManager wifiManager = (WifiManager) getSystemService(WifiManager.class);
        if (wifiManager != null) {
            if (this.mMultiCastLock == null) {
                this.mMultiCastLock = wifiManager.createMulticastLock("PrintServiceRecService");
            }
            this.mMultiCastLock.acquire();
        }
        this.mPlugins = new ArrayList<>();
        try {
            Iterator<VendorConfig> it = VendorConfig.getAllConfigs(this).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                VendorConfig next = it.next();
                try {
                    this.mPlugins.add(new RemotePrintServicePlugin(new MDNSFilterPlugin(this, next.name, next.packageName, next.mDNSNames), this, false));
                } catch (Exception e) {
                    Log.e("PrintServiceRecService", "Could not initiate simple MDNS plugin for " + next.packageName, e);
                }
            }
            try {
                this.mPlugins.add(new RemotePrintServicePlugin(new CloudPrintPlugin(this), this, true));
            } catch (Exception e2) {
                Log.e("PrintServiceRecService", "Could not initiate " + getString(R.string.plugin_vendor_google_cloud_print) + " plugin", e2);
            }
            try {
                this.mPlugins.add(new RemotePrintServicePlugin(new HPRecommendationPlugin(this), this, false));
            } catch (Exception e3) {
                Log.e("PrintServiceRecService", "Could not initiate " + getString(R.string.plugin_vendor_hp) + " plugin", e3);
            }
            try {
                this.mPlugins.add(new RemotePrintServicePlugin(new MopriaRecommendationPlugin(this), this, true));
            } catch (Exception e4) {
                Log.e("PrintServiceRecService", "Could not initiate " + getString(R.string.plugin_vendor_morpia) + " plugin", e4);
            }
            try {
                this.mPlugins.add(new RemotePrintServicePlugin(new SamsungRecommendationPlugin(this), this, true));
            } catch (Exception e5) {
                Log.e("PrintServiceRecService", "Could not initiate " + getString(R.string.plugin_vendor_samsung) + " plugin", e5);
            }
            try {
                this.mPlugins.add(new RemotePrintServicePlugin(new XeroxPrintServiceRecommendationPlugin(this), this, false));
            } catch (Exception e6) {
                Log.e("PrintServiceRecService", "Could not initiate " + getString(R.string.plugin_vendor_xerox) + " plugin", e6);
            }
            int size = this.mPlugins.size();
            for (i = 0; i < size; i++) {
                try {
                    this.mPlugins.get(i).start();
                } catch (RemotePrintServicePlugin.PluginException e7) {
                    Log.e("PrintServiceRecService", "Could not start plugin", e7);
                }
            }
        } catch (IOException | XmlPullParserException e8) {
            throw new RuntimeException("Could not parse vendorconfig", e8);
        }
    }

    public void onDisconnected() {
        int size = this.mPlugins.size();
        for (int i = 0; i < size; i++) {
            try {
                this.mPlugins.get(i).stop();
            } catch (RemotePrintServicePlugin.PluginException e) {
                Log.e("PrintServiceRecService", "Could not stop plugin", e);
            }
        }
        if (this.mMultiCastLock != null) {
            this.mMultiCastLock.release();
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        onChanged();
    }

    @Override
    public void onChanged() {
        ArrayList arrayList = new ArrayList();
        int size = this.mPlugins.size();
        for (int i = 0; i < size; i++) {
            RemotePrintServicePlugin remotePrintServicePlugin = this.mPlugins.get(i);
            try {
                List<InetAddress> printers = remotePrintServicePlugin.getPrinters();
                if (!printers.isEmpty()) {
                    arrayList.add(new RecommendationInfo(remotePrintServicePlugin.packageName, getString(remotePrintServicePlugin.name), printers, remotePrintServicePlugin.recommendsMultiVendorService));
                }
            } catch (Exception e) {
                Log.e("PrintServiceRecService", "Could not read state of plugin for " + ((Object) remotePrintServicePlugin.packageName), e);
            }
        }
        updateRecommendations(arrayList);
    }
}
