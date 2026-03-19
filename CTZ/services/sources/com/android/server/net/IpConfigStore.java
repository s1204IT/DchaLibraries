package com.android.server.net;

import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.net.DelayedDiskWrite;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;

public class IpConfigStore {
    private static final boolean DBG = false;
    protected static final String DNS_KEY = "dns";
    protected static final String EOS = "eos";
    protected static final String EXCLUSION_LIST_KEY = "exclusionList";
    protected static final String GATEWAY_KEY = "gateway";
    protected static final String ID_KEY = "id";
    protected static final int IPCONFIG_FILE_VERSION = 3;
    protected static final String IP_ASSIGNMENT_KEY = "ipAssignment";
    protected static final String LINK_ADDRESS_KEY = "linkAddress";
    protected static final String PROXY_HOST_KEY = "proxyHost";
    protected static final String PROXY_PAC_FILE = "proxyPac";
    protected static final String PROXY_PORT_KEY = "proxyPort";
    protected static final String PROXY_SETTINGS_KEY = "proxySettings";
    private static final String TAG = "IpConfigStore";
    protected final DelayedDiskWrite mWriter;

    public IpConfigStore(DelayedDiskWrite delayedDiskWrite) {
        this.mWriter = delayedDiskWrite;
    }

    public IpConfigStore() {
        this(new DelayedDiskWrite());
    }

    private static boolean writeConfig(DataOutputStream dataOutputStream, String str, IpConfiguration ipConfiguration) throws IOException {
        return writeConfig(dataOutputStream, str, ipConfiguration, 3);
    }

    @VisibleForTesting
    public static boolean writeConfig(DataOutputStream dataOutputStream, String str, IpConfiguration ipConfiguration, int i) throws IOException {
        boolean z = false;
        try {
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipConfiguration.ipAssignment.ordinal()]) {
                case 1:
                    dataOutputStream.writeUTF(IP_ASSIGNMENT_KEY);
                    dataOutputStream.writeUTF(ipConfiguration.ipAssignment.toString());
                    StaticIpConfiguration staticIpConfiguration = ipConfiguration.staticIpConfiguration;
                    if (staticIpConfiguration != null) {
                        if (staticIpConfiguration.ipAddress != null) {
                            LinkAddress linkAddress = staticIpConfiguration.ipAddress;
                            dataOutputStream.writeUTF(LINK_ADDRESS_KEY);
                            dataOutputStream.writeUTF(linkAddress.getAddress().getHostAddress());
                            dataOutputStream.writeInt(linkAddress.getPrefixLength());
                        }
                        if (staticIpConfiguration.gateway != null) {
                            dataOutputStream.writeUTF(GATEWAY_KEY);
                            dataOutputStream.writeInt(0);
                            dataOutputStream.writeInt(1);
                            dataOutputStream.writeUTF(staticIpConfiguration.gateway.getHostAddress());
                        }
                        for (InetAddress inetAddress : staticIpConfiguration.dnsServers) {
                            dataOutputStream.writeUTF(DNS_KEY);
                            dataOutputStream.writeUTF(inetAddress.getHostAddress());
                        }
                    }
                    z = true;
                    break;
                case 2:
                    dataOutputStream.writeUTF(IP_ASSIGNMENT_KEY);
                    dataOutputStream.writeUTF(ipConfiguration.ipAssignment.toString());
                    z = true;
                    break;
                case 3:
                    break;
                default:
                    loge("Ignore invalid ip assignment while writing");
                    break;
            }
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[ipConfiguration.proxySettings.ordinal()]) {
                case 1:
                    ProxyInfo proxyInfo = ipConfiguration.httpProxy;
                    String exclusionListAsString = proxyInfo.getExclusionListAsString();
                    dataOutputStream.writeUTF(PROXY_SETTINGS_KEY);
                    dataOutputStream.writeUTF(ipConfiguration.proxySettings.toString());
                    dataOutputStream.writeUTF(PROXY_HOST_KEY);
                    dataOutputStream.writeUTF(proxyInfo.getHost());
                    dataOutputStream.writeUTF(PROXY_PORT_KEY);
                    dataOutputStream.writeInt(proxyInfo.getPort());
                    if (exclusionListAsString != null) {
                        dataOutputStream.writeUTF(EXCLUSION_LIST_KEY);
                        dataOutputStream.writeUTF(exclusionListAsString);
                    }
                    z = true;
                    break;
                case 2:
                    ProxyInfo proxyInfo2 = ipConfiguration.httpProxy;
                    dataOutputStream.writeUTF(PROXY_SETTINGS_KEY);
                    dataOutputStream.writeUTF(ipConfiguration.proxySettings.toString());
                    dataOutputStream.writeUTF(PROXY_PAC_FILE);
                    dataOutputStream.writeUTF(proxyInfo2.getPacFileUrl().toString());
                    z = true;
                    break;
                case 3:
                    dataOutputStream.writeUTF(PROXY_SETTINGS_KEY);
                    dataOutputStream.writeUTF(ipConfiguration.proxySettings.toString());
                    z = true;
                    break;
                case 4:
                    break;
                default:
                    loge("Ignore invalid proxy settings while writing");
                    break;
            }
            if (z) {
                dataOutputStream.writeUTF(ID_KEY);
                if (i < 3) {
                    dataOutputStream.writeInt(Integer.valueOf(str).intValue());
                } else {
                    dataOutputStream.writeUTF(str);
                }
            }
        } catch (NullPointerException e) {
            loge("Failure in writing " + ipConfiguration + e);
        }
        dataOutputStream.writeUTF(EOS);
        return z;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$net$IpConfiguration$IpAssignment;
        static final int[] $SwitchMap$android$net$IpConfiguration$ProxySettings = new int[IpConfiguration.ProxySettings.values().length];

        static {
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.PAC.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.NONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.UNASSIGNED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$android$net$IpConfiguration$IpAssignment = new int[IpConfiguration.IpAssignment.values().length];
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.DHCP.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.UNASSIGNED.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    @Deprecated
    public void writeIpAndProxyConfigurationsToFile(String str, final SparseArray<IpConfiguration> sparseArray) {
        this.mWriter.write(str, new DelayedDiskWrite.Writer() {
            @Override
            public final void onWriteCalled(DataOutputStream dataOutputStream) throws IOException {
                IpConfigStore.lambda$writeIpAndProxyConfigurationsToFile$0(sparseArray, dataOutputStream);
            }
        });
    }

    static void lambda$writeIpAndProxyConfigurationsToFile$0(SparseArray sparseArray, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(3);
        for (int i = 0; i < sparseArray.size(); i++) {
            writeConfig(dataOutputStream, String.valueOf(sparseArray.keyAt(i)), (IpConfiguration) sparseArray.valueAt(i));
        }
    }

    public void writeIpConfigurations(String str, final ArrayMap<String, IpConfiguration> arrayMap) {
        this.mWriter.write(str, new DelayedDiskWrite.Writer() {
            @Override
            public final void onWriteCalled(DataOutputStream dataOutputStream) throws IOException {
                IpConfigStore.lambda$writeIpConfigurations$1(arrayMap, dataOutputStream);
            }
        });
    }

    static void lambda$writeIpConfigurations$1(ArrayMap arrayMap, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(3);
        for (int i = 0; i < arrayMap.size(); i++) {
            writeConfig(dataOutputStream, (String) arrayMap.keyAt(i), (IpConfiguration) arrayMap.valueAt(i));
        }
    }

    public static ArrayMap<String, IpConfiguration> readIpConfigurations(String str) {
        try {
            return readIpConfigurations(new BufferedInputStream(new FileInputStream(str)));
        } catch (FileNotFoundException e) {
            loge("Error opening configuration file: " + e);
            return new ArrayMap<>(0);
        }
    }

    @Deprecated
    public static SparseArray<IpConfiguration> readIpAndProxyConfigurations(String str) {
        try {
            return readIpAndProxyConfigurations(new BufferedInputStream(new FileInputStream(str)));
        } catch (FileNotFoundException e) {
            loge("Error opening configuration file: " + e);
            return new SparseArray<>();
        }
    }

    @Deprecated
    public static SparseArray<IpConfiguration> readIpAndProxyConfigurations(InputStream inputStream) throws Throwable {
        ArrayMap<String, IpConfiguration> ipConfigurations = readIpConfigurations(inputStream);
        if (ipConfigurations == null) {
            return null;
        }
        SparseArray<IpConfiguration> sparseArray = new SparseArray<>();
        for (int i = 0; i < ipConfigurations.size(); i++) {
            sparseArray.put(Integer.valueOf(ipConfigurations.keyAt(i)).intValue(), ipConfigurations.valueAt(i));
        }
        return sparseArray;
    }

    public static ArrayMap<String, IpConfiguration> readIpConfigurations(InputStream inputStream) throws Throwable {
        Throwable th;
        DataInputStream dataInputStream;
        DataInputStream dataInputStream2;
        ArrayMap<String, IpConfiguration> arrayMap = new ArrayMap<>();
        String str = null;
        try {
            try {
                dataInputStream = new DataInputStream(inputStream);
            } catch (Exception e) {
                return arrayMap;
            }
            try {
                int i = dataInputStream.readInt();
                if (i != 3 && i != 2 && i != 1) {
                    loge("Bad version on IP configuration file, ignore read");
                    try {
                        dataInputStream.close();
                    } catch (Exception e2) {
                    }
                    return null;
                }
                while (true) {
                    IpConfiguration.IpAssignment ipAssignment = IpConfiguration.IpAssignment.DHCP;
                    IpConfiguration.ProxySettings proxySettings = IpConfiguration.ProxySettings.NONE;
                    StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                    String utf = str;
                    String utf2 = utf;
                    String utf3 = utf2;
                    IpConfiguration.ProxySettings proxySettingsValueOf = proxySettings;
                    int i2 = -1;
                    IpConfiguration.IpAssignment ipAssignmentValueOf = ipAssignment;
                    String strValueOf = utf3;
                    while (true) {
                        String utf4 = dataInputStream.readUTF();
                        try {
                            if (utf4.equals(ID_KEY)) {
                                strValueOf = i < 3 ? String.valueOf(dataInputStream.readInt()) : dataInputStream.readUTF();
                            } else if (utf4.equals(IP_ASSIGNMENT_KEY)) {
                                ipAssignmentValueOf = IpConfiguration.IpAssignment.valueOf(dataInputStream.readUTF());
                            } else if (utf4.equals(LINK_ADDRESS_KEY)) {
                                LinkAddress linkAddress = new LinkAddress(NetworkUtils.numericToInetAddress(dataInputStream.readUTF()), dataInputStream.readInt());
                                if ((linkAddress.getAddress() instanceof Inet4Address) && staticIpConfiguration.ipAddress == null) {
                                    staticIpConfiguration.ipAddress = linkAddress;
                                } else {
                                    loge("Non-IPv4 or duplicate address: " + linkAddress);
                                }
                            } else if (utf4.equals(GATEWAY_KEY)) {
                                if (i == 1) {
                                    InetAddress inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(dataInputStream.readUTF());
                                    if (staticIpConfiguration.gateway == null) {
                                        staticIpConfiguration.gateway = inetAddressNumericToInetAddress;
                                    } else {
                                        loge("Duplicate gateway: " + inetAddressNumericToInetAddress.getHostAddress());
                                    }
                                } else {
                                    LinkAddress linkAddress2 = dataInputStream.readInt() == 1 ? new LinkAddress(NetworkUtils.numericToInetAddress(dataInputStream.readUTF()), dataInputStream.readInt()) : null;
                                    InetAddress inetAddressNumericToInetAddress2 = dataInputStream.readInt() == 1 ? NetworkUtils.numericToInetAddress(dataInputStream.readUTF()) : null;
                                    RouteInfo routeInfo = new RouteInfo(linkAddress2, inetAddressNumericToInetAddress2);
                                    if (routeInfo.isIPv4Default() && staticIpConfiguration.gateway == null) {
                                        staticIpConfiguration.gateway = inetAddressNumericToInetAddress2;
                                    } else {
                                        loge("Non-IPv4 default or duplicate route: " + routeInfo);
                                    }
                                }
                            } else if (utf4.equals(DNS_KEY)) {
                                staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(dataInputStream.readUTF()));
                            } else if (utf4.equals(PROXY_SETTINGS_KEY)) {
                                proxySettingsValueOf = IpConfiguration.ProxySettings.valueOf(dataInputStream.readUTF());
                            } else if (utf4.equals(PROXY_HOST_KEY)) {
                                utf = dataInputStream.readUTF();
                            } else if (utf4.equals(PROXY_PORT_KEY)) {
                                i2 = dataInputStream.readInt();
                            } else if (utf4.equals(PROXY_PAC_FILE)) {
                                utf3 = dataInputStream.readUTF();
                            } else if (utf4.equals(EXCLUSION_LIST_KEY)) {
                                utf2 = dataInputStream.readUTF();
                            } else if (utf4.equals(EOS)) {
                                if (strValueOf != null) {
                                    IpConfiguration ipConfiguration = new IpConfiguration();
                                    arrayMap.put(strValueOf, ipConfiguration);
                                    switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipAssignmentValueOf.ordinal()]) {
                                        case 1:
                                            ipConfiguration.staticIpConfiguration = staticIpConfiguration;
                                            ipConfiguration.ipAssignment = ipAssignmentValueOf;
                                            break;
                                        case 2:
                                            ipConfiguration.ipAssignment = ipAssignmentValueOf;
                                            break;
                                        case 3:
                                            loge("BUG: Found UNASSIGNED IP on file, use DHCP");
                                            ipConfiguration.ipAssignment = IpConfiguration.IpAssignment.DHCP;
                                            break;
                                        default:
                                            loge("Ignore invalid ip assignment while reading.");
                                            ipConfiguration.ipAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
                                            break;
                                    }
                                    switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[proxySettingsValueOf.ordinal()]) {
                                        case 1:
                                            ProxyInfo proxyInfo = new ProxyInfo(utf, i2, utf2);
                                            ipConfiguration.proxySettings = proxySettingsValueOf;
                                            ipConfiguration.httpProxy = proxyInfo;
                                            break;
                                        case 2:
                                            ProxyInfo proxyInfo2 = new ProxyInfo(utf3);
                                            ipConfiguration.proxySettings = proxySettingsValueOf;
                                            ipConfiguration.httpProxy = proxyInfo2;
                                            break;
                                        case 3:
                                            ipConfiguration.proxySettings = proxySettingsValueOf;
                                            break;
                                        case 4:
                                            loge("BUG: Found UNASSIGNED proxy on file, use NONE");
                                            ipConfiguration.proxySettings = IpConfiguration.ProxySettings.NONE;
                                            break;
                                        default:
                                            loge("Ignore invalid proxy settings while reading");
                                            ipConfiguration.proxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
                                            break;
                                    }
                                }
                                str = null;
                            } else {
                                loge("Ignore unknown key " + utf4 + "while reading");
                            }
                        } catch (IllegalArgumentException e3) {
                            loge("Ignore invalid address while reading" + e3);
                        }
                    }
                }
            } catch (EOFException e4) {
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                return arrayMap;
            } catch (IOException e5) {
                e = e5;
                dataInputStream2 = dataInputStream;
                try {
                    loge("Error parsing configuration: " + e);
                    if (dataInputStream2 != null) {
                        dataInputStream2.close();
                    }
                    return arrayMap;
                } catch (Throwable th2) {
                    th = th2;
                    dataInputStream = dataInputStream2;
                    if (dataInputStream != null) {
                        throw th;
                    }
                    try {
                        dataInputStream.close();
                        throw th;
                    } catch (Exception e6) {
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                if (dataInputStream != null) {
                }
            }
        } catch (EOFException e7) {
            dataInputStream = null;
        } catch (IOException e8) {
            e = e8;
            dataInputStream2 = null;
        } catch (Throwable th4) {
            th = th4;
            dataInputStream = null;
        }
    }

    protected static void loge(String str) {
        Log.e(TAG, str);
    }

    protected static void log(String str) {
        Log.d(TAG, str);
    }
}
