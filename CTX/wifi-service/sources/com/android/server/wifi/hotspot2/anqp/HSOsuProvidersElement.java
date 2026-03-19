package com.android.server.wifi.hotspot2.anqp;

import android.net.wifi.WifiSsid;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HSOsuProvidersElement extends ANQPElement {

    @VisibleForTesting
    public static final int MAXIMUM_OSU_SSID_LENGTH = 32;
    private final WifiSsid mOsuSsid;
    private final List<OsuProviderInfo> mProviders;

    @VisibleForTesting
    public HSOsuProvidersElement(WifiSsid wifiSsid, List<OsuProviderInfo> list) {
        super(Constants.ANQPElementType.HSOSUProviders);
        this.mOsuSsid = wifiSsid;
        this.mProviders = list;
    }

    public static HSOsuProvidersElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        if (i > 32) {
            throw new ProtocolException("Invalid SSID length: " + i);
        }
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        ArrayList arrayList = new ArrayList();
        for (int i2 = byteBuffer.get() & 255; i2 > 0; i2--) {
            arrayList.add(OsuProviderInfo.parse(byteBuffer));
        }
        return new HSOsuProvidersElement(WifiSsid.createFromByteArray(bArr), arrayList);
    }

    public WifiSsid getOsuSsid() {
        return this.mOsuSsid;
    }

    public List<OsuProviderInfo> getProviders() {
        return Collections.unmodifiableList(this.mProviders);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSOsuProvidersElement)) {
            return false;
        }
        HSOsuProvidersElement hSOsuProvidersElement = (HSOsuProvidersElement) obj;
        if (this.mOsuSsid != null ? this.mOsuSsid.equals(hSOsuProvidersElement.mOsuSsid) : hSOsuProvidersElement.mOsuSsid == null) {
            if (this.mProviders == null) {
                if (hSOsuProvidersElement.mProviders == null) {
                    return true;
                }
            } else if (this.mProviders.equals(hSOsuProvidersElement.mProviders)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mOsuSsid, this.mProviders);
    }

    public String toString() {
        return "OSUProviders{mOsuSsid=" + this.mOsuSsid + ", mProviders=" + this.mProviders + "}";
    }
}
