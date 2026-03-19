package com.android.bips.jni;

import android.print.PrintAttributes;
import android.print.PrinterCapabilitiesInfo;
import android.text.TextUtils;
import com.android.bips.BuiltInPrintService;
import com.android.bips.R;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

public class LocalPrinterCapabilities {
    public boolean borderless;
    public boolean color;
    public boolean duplex;
    public InetAddress inetAddress;
    public boolean isSupported;
    public String location;
    public String mediaDefault;
    public String name;
    public byte[] nativeData;
    public String path;
    public int[] supportedMediaSizes;
    public int[] supportedMediaTypes;
    public String uuid;

    public void buildCapabilities(BuiltInPrintService builtInPrintService, PrinterCapabilitiesInfo.Builder builder) {
        builder.setColorModes((this.color ? 2 : 0) | 1, this.color ? 2 : 1);
        MediaSizes mediaSizes = MediaSizes.getInstance(builtInPrintService);
        String str = this.mediaDefault;
        if (TextUtils.isEmpty(str) || mediaSizes.toMediaSize(str) == null) {
            str = "iso_a4_210x297mm";
        }
        ArrayList arrayList = new ArrayList();
        for (int i : this.supportedMediaSizes) {
            String mediaName = MediaSizes.toMediaName(i);
            if (mediaName != null) {
                arrayList.add(mediaName);
            }
        }
        if (arrayList.isEmpty()) {
            arrayList.addAll(MediaSizes.DEFAULT_MEDIA_NAMES);
        }
        if (!arrayList.contains(str)) {
            str = (String) arrayList.get(0);
        }
        for (String str2 : new HashSet(arrayList)) {
            builder.addMediaSize(mediaSizes.toMediaSize(str2), Objects.equals(str2, str));
        }
        builder.addResolution(new PrintAttributes.Resolution(BackendConstants.RESOLUTION_300_DPI, builtInPrintService.getString(R.string.resolution_300_dpi), 300, 300), true);
        if (this.duplex) {
            builder.setDuplexModes(7, 1);
        }
        if (this.borderless) {
            builder.setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0));
        }
    }

    public String toString() {
        return "LocalPrinterCapabilities{path=" + this.path + " name=" + this.name + " uuid=" + this.uuid + " location=" + this.location + " duplex=" + this.duplex + " borderless=" + this.borderless + " color=" + this.color + " isSupported=" + this.isSupported + " mediaDefault=" + this.mediaDefault + " supportedMediaTypes=" + Arrays.toString(this.supportedMediaTypes) + " supportedMediaSizes=" + Arrays.toString(this.supportedMediaSizes) + " inetAddress=" + this.inetAddress + "}";
    }
}
