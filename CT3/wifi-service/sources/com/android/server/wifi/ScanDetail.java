package com.android.server.wifi;

import android.net.wifi.AnqpInformationElement;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiSsid;
import com.android.server.wifi.anqp.ANQPElement;
import com.android.server.wifi.anqp.Constants;
import com.android.server.wifi.anqp.HSFriendlyNameElement;
import com.android.server.wifi.anqp.RawByteElement;
import com.android.server.wifi.anqp.VenueNameElement;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointMatch;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.pps.HomeSP;
import java.util.List;
import java.util.Map;

public class ScanDetail {
    private final Map<HomeSP, PasspointMatch> mMatches;
    private volatile NetworkDetail mNetworkDetail;
    private final ScanResult mScanResult;
    private long mSeen;

    public ScanDetail(NetworkDetail networkDetail, WifiSsid wifiSsid, String bssid, String caps, int level, int frequency, long tsf, ScanResult.InformationElement[] informationElements, List<String> anqpLines) {
        this.mSeen = 0L;
        this.mNetworkDetail = networkDetail;
        this.mScanResult = new ScanResult(wifiSsid, bssid, networkDetail.getHESSID(), networkDetail.getAnqpDomainID(), networkDetail.getOsuProviders(), caps, level, frequency, tsf);
        this.mSeen = System.currentTimeMillis();
        this.mScanResult.channelWidth = networkDetail.getChannelWidth();
        this.mScanResult.centerFreq0 = networkDetail.getCenterfreq0();
        this.mScanResult.centerFreq1 = networkDetail.getCenterfreq1();
        this.mScanResult.informationElements = informationElements;
        this.mScanResult.anqpLines = anqpLines;
        if (networkDetail.is80211McResponderSupport()) {
            this.mScanResult.setFlag(2L);
        }
        this.mMatches = null;
    }

    public ScanDetail(WifiSsid wifiSsid, String bssid, String caps, int level, int frequency, long tsf, long seen) {
        this.mSeen = 0L;
        this.mNetworkDetail = null;
        this.mScanResult = new ScanResult(wifiSsid, bssid, 0L, -1, null, caps, level, frequency, tsf);
        this.mSeen = seen;
        this.mScanResult.channelWidth = 0;
        this.mScanResult.centerFreq0 = 0;
        this.mScanResult.centerFreq1 = 0;
        this.mScanResult.flags = 0L;
        this.mMatches = null;
    }

    public ScanDetail(ScanResult scanResult, NetworkDetail networkDetail, Map<HomeSP, PasspointMatch> matches) {
        this.mSeen = 0L;
        this.mScanResult = scanResult;
        this.mNetworkDetail = networkDetail;
        this.mMatches = matches;
        this.mSeen = this.mScanResult.seen;
    }

    public void updateResults(NetworkDetail networkDetail, int level, WifiSsid wssid, String ssid, String flags, int freq, long tsf) {
        this.mScanResult.level = level;
        this.mScanResult.wifiSsid = wssid;
        this.mScanResult.SSID = ssid;
        this.mScanResult.capabilities = flags;
        this.mScanResult.frequency = freq;
        this.mScanResult.timestamp = tsf;
        this.mSeen = System.currentTimeMillis();
        this.mScanResult.channelWidth = networkDetail.getChannelWidth();
        this.mScanResult.centerFreq0 = networkDetail.getCenterfreq0();
        this.mScanResult.centerFreq1 = networkDetail.getCenterfreq1();
        if (networkDetail.is80211McResponderSupport()) {
            this.mScanResult.setFlag(2L);
        }
        if (!networkDetail.isInterworking()) {
            return;
        }
        this.mScanResult.setFlag(1L);
    }

    public void propagateANQPInfo(Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        if (anqpElements.isEmpty()) {
            return;
        }
        this.mNetworkDetail = this.mNetworkDetail.complete(anqpElements);
        HSFriendlyNameElement fne = (HSFriendlyNameElement) anqpElements.get(Constants.ANQPElementType.HSFriendlyName);
        if (fne != null && !fne.getNames().isEmpty()) {
            this.mScanResult.venueName = fne.getNames().get(0).getText();
        } else {
            VenueNameElement vne = (VenueNameElement) anqpElements.get(Constants.ANQPElementType.ANQPVenueName);
            if (vne != null && !vne.getNames().isEmpty()) {
                this.mScanResult.venueName = vne.getNames().get(0).getText();
            }
        }
        RawByteElement osuProviders = (RawByteElement) anqpElements.get(Constants.ANQPElementType.HSOSUProviders);
        if (osuProviders == null) {
            return;
        }
        this.mScanResult.anqpElements = new AnqpInformationElement[1];
        this.mScanResult.anqpElements[0] = new AnqpInformationElement(5271450, 8, osuProviders.getPayload());
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
