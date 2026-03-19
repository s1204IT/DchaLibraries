package com.android.server.wifi;

import android.net.wifi.AnqpInformationElement;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiSsid;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.ANQPParser;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSFriendlyNameElement;
import com.android.server.wifi.hotspot2.anqp.RawByteElement;
import com.android.server.wifi.hotspot2.anqp.VenueNameElement;
import java.util.List;
import java.util.Map;

public class ScanDetail {
    private volatile NetworkDetail mNetworkDetail;
    private final ScanResult mScanResult;
    private long mSeen;

    public ScanDetail(NetworkDetail networkDetail, WifiSsid wifiSsid, String str, String str2, int i, int i2, long j, ScanResult.InformationElement[] informationElementArr, List<String> list) {
        this.mSeen = 0L;
        this.mNetworkDetail = networkDetail;
        this.mScanResult = new ScanResult(wifiSsid, str, networkDetail.getHESSID(), networkDetail.getAnqpDomainID(), networkDetail.getOsuProviders(), str2, i, i2, j);
        this.mSeen = System.currentTimeMillis();
        this.mScanResult.seen = this.mSeen;
        this.mScanResult.channelWidth = networkDetail.getChannelWidth();
        this.mScanResult.centerFreq0 = networkDetail.getCenterfreq0();
        this.mScanResult.centerFreq1 = networkDetail.getCenterfreq1();
        this.mScanResult.informationElements = informationElementArr;
        this.mScanResult.anqpLines = list;
        if (networkDetail.is80211McResponderSupport()) {
            this.mScanResult.setFlag(2L);
        }
        if (networkDetail.isInterworking()) {
            this.mScanResult.setFlag(1L);
        }
    }

    public ScanDetail(WifiSsid wifiSsid, String str, String str2, int i, int i2, long j, long j2) {
        this.mSeen = 0L;
        this.mNetworkDetail = null;
        this.mScanResult = new ScanResult(wifiSsid, str, 0L, -1, null, str2, i, i2, j);
        this.mSeen = j2;
        this.mScanResult.seen = this.mSeen;
        this.mScanResult.channelWidth = 0;
        this.mScanResult.centerFreq0 = 0;
        this.mScanResult.centerFreq1 = 0;
        this.mScanResult.flags = 0L;
    }

    public ScanDetail(ScanResult scanResult, NetworkDetail networkDetail) {
        this.mSeen = 0L;
        this.mScanResult = scanResult;
        this.mNetworkDetail = networkDetail;
        this.mSeen = this.mScanResult.seen == 0 ? System.currentTimeMillis() : this.mScanResult.seen;
    }

    public void propagateANQPInfo(Map<Constants.ANQPElementType, ANQPElement> map) {
        if (map.isEmpty()) {
            return;
        }
        this.mNetworkDetail = this.mNetworkDetail.complete(map);
        HSFriendlyNameElement hSFriendlyNameElement = (HSFriendlyNameElement) map.get(Constants.ANQPElementType.HSFriendlyName);
        if (hSFriendlyNameElement != null && !hSFriendlyNameElement.getNames().isEmpty()) {
            this.mScanResult.venueName = hSFriendlyNameElement.getNames().get(0).getText();
        } else {
            VenueNameElement venueNameElement = (VenueNameElement) map.get(Constants.ANQPElementType.ANQPVenueName);
            if (venueNameElement != null && !venueNameElement.getNames().isEmpty()) {
                this.mScanResult.venueName = venueNameElement.getNames().get(0).getText();
            }
        }
        RawByteElement rawByteElement = (RawByteElement) map.get(Constants.ANQPElementType.HSOSUProviders);
        if (rawByteElement != null) {
            this.mScanResult.anqpElements = new AnqpInformationElement[1];
            this.mScanResult.anqpElements[0] = new AnqpInformationElement(ANQPParser.VENDOR_SPECIFIC_HS20_OI, 8, rawByteElement.getPayload());
        }
    }

    public ScanResult getScanResult() {
        return this.mScanResult;
    }

    public NetworkDetail getNetworkDetail() {
        return this.mNetworkDetail;
    }

    public String getSSID() {
        return this.mNetworkDetail == null ? this.mScanResult.SSID : this.mNetworkDetail.getSSID();
    }

    public String getBSSIDString() {
        return this.mNetworkDetail == null ? this.mScanResult.BSSID : this.mNetworkDetail.getBSSIDString();
    }

    public String toKeyString() {
        NetworkDetail networkDetail = this.mNetworkDetail;
        if (networkDetail != null) {
            return networkDetail.toKeyString();
        }
        return String.format("'%s':%012x", this.mScanResult.BSSID, Long.valueOf(Utils.parseMac(this.mScanResult.BSSID)));
    }

    public long getSeen() {
        return this.mSeen;
    }

    public long setSeen() {
        this.mSeen = System.currentTimeMillis();
        this.mScanResult.seen = this.mSeen;
        return this.mSeen;
    }

    public String toString() {
        try {
            return String.format("'%s'/%012x", this.mScanResult.SSID, Long.valueOf(Utils.parseMac(this.mScanResult.BSSID)));
        } catch (IllegalArgumentException e) {
            return String.format("'%s'/----", this.mScanResult.BSSID);
        }
    }
}
