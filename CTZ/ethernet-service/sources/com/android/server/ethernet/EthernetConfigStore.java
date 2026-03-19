package com.android.server.ethernet;

import android.net.IpConfiguration;
import android.os.Environment;
import android.util.ArrayMap;
import com.android.server.net.IpConfigStore;

public class EthernetConfigStore {
    private static final String ipConfigFile = Environment.getDataDirectory() + "/misc/ethernet/ipconfig.txt";
    private IpConfiguration mIpConfigurationForDefaultInterface;
    private IpConfigStore mStore = new IpConfigStore();
    private final Object mSync = new Object();
    private ArrayMap<String, IpConfiguration> mIpConfigurations = new ArrayMap<>(0);

    public void read() {
        synchronized (this.mSync) {
            ArrayMap<String, IpConfiguration> ipConfigurations = IpConfigStore.readIpConfigurations(ipConfigFile);
            if (ipConfigurations.containsKey("0")) {
                this.mIpConfigurationForDefaultInterface = ipConfigurations.remove("0");
            }
            this.mIpConfigurations = ipConfigurations;
        }
    }

    public void write(String str, IpConfiguration ipConfiguration) {
        synchronized (this.mSync) {
            boolean zEquals = true;
            try {
                if (ipConfiguration == null) {
                    if (this.mIpConfigurations.remove(str) == null) {
                        zEquals = false;
                    }
                } else {
                    zEquals = true ^ ipConfiguration.equals(this.mIpConfigurations.put(str, ipConfiguration));
                }
                if (zEquals) {
                    this.mStore.writeIpConfigurations(ipConfigFile, this.mIpConfigurations);
                }
            } finally {
            }
        }
    }

    public ArrayMap<String, IpConfiguration> getIpConfigurations() {
        ArrayMap<String, IpConfiguration> arrayMap;
        synchronized (this.mSync) {
            arrayMap = new ArrayMap<>(this.mIpConfigurations);
        }
        return arrayMap;
    }

    public IpConfiguration getIpConfigurationForDefaultInterface() {
        IpConfiguration ipConfiguration;
        synchronized (this.mSync) {
            ipConfiguration = this.mIpConfigurationForDefaultInterface == null ? null : new IpConfiguration(this.mIpConfigurationForDefaultInterface);
        }
        return ipConfiguration;
    }
}
