package android.net.wifi.p2p.nsd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WifiP2pUpnpServiceInfo extends WifiP2pServiceInfo {
    public static final int VERSION_1_0 = 16;

    private WifiP2pUpnpServiceInfo(List<String> list) {
        super(list);
    }

    public static WifiP2pUpnpServiceInfo newInstance(String str, String str2, List<String> list) {
        if (str == null || str2 == null) {
            throw new IllegalArgumentException("uuid or device cannnot be null");
        }
        UUID.fromString(str);
        ArrayList arrayList = new ArrayList();
        arrayList.add(createSupplicantQuery(str, null));
        arrayList.add(createSupplicantQuery(str, "upnp:rootdevice"));
        arrayList.add(createSupplicantQuery(str, str2));
        if (list != null) {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(createSupplicantQuery(str, it.next()));
            }
        }
        return new WifiP2pUpnpServiceInfo(arrayList);
    }

    private static String createSupplicantQuery(String str, String str2) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("upnp ");
        stringBuffer.append(String.format(Locale.US, "%02x ", 16));
        stringBuffer.append("uuid:");
        stringBuffer.append(str);
        if (str2 != null) {
            stringBuffer.append("::");
            stringBuffer.append(str2);
        }
        return stringBuffer.toString();
    }
}
