package com.android.server.net;

import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.net.DelayedDiskWrite;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

public class IpConfigStore {

    private static final int[] f9androidnetIpConfiguration$IpAssignmentSwitchesValues = null;

    private static final int[] f10androidnetIpConfiguration$ProxySettingsSwitchesValues = null;
    private static final boolean DBG = false;
    protected static final String DNS_KEY = "dns";
    protected static final String EOS = "eos";
    protected static final String EXCLUSION_LIST_KEY = "exclusionList";
    protected static final String GATEWAY_KEY = "gateway";
    protected static final String ID_KEY = "id";
    protected static final int IPCONFIG_FILE_VERSION = 2;
    protected static final String IP_ASSIGNMENT_KEY = "ipAssignment";
    protected static final String LINK_ADDRESS_KEY = "linkAddress";
    protected static final String PROXY_HOST_KEY = "proxyHost";
    protected static final String PROXY_PAC_FILE = "proxyPac";
    protected static final String PROXY_PORT_KEY = "proxyPort";
    protected static final String PROXY_SETTINGS_KEY = "proxySettings";
    private static final String TAG = "IpConfigStore";
    protected final DelayedDiskWrite mWriter;

    private static int[] m2242getandroidnetIpConfiguration$IpAssignmentSwitchesValues() {
        if (f9androidnetIpConfiguration$IpAssignmentSwitchesValues != null) {
            return f9androidnetIpConfiguration$IpAssignmentSwitchesValues;
        }
        int[] iArr = new int[IpConfiguration.IpAssignment.values().length];
        try {
            iArr[IpConfiguration.IpAssignment.DHCP.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IpConfiguration.IpAssignment.STATIC.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IpConfiguration.IpAssignment.UNASSIGNED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f9androidnetIpConfiguration$IpAssignmentSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m2243getandroidnetIpConfiguration$ProxySettingsSwitchesValues() {
        if (f10androidnetIpConfiguration$ProxySettingsSwitchesValues != null) {
            return f10androidnetIpConfiguration$ProxySettingsSwitchesValues;
        }
        int[] iArr = new int[IpConfiguration.ProxySettings.values().length];
        try {
            iArr[IpConfiguration.ProxySettings.NONE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IpConfiguration.ProxySettings.PAC.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IpConfiguration.ProxySettings.STATIC.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IpConfiguration.ProxySettings.UNASSIGNED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f10androidnetIpConfiguration$ProxySettingsSwitchesValues = iArr;
        return iArr;
    }

    public IpConfigStore(DelayedDiskWrite writer) {
        this.mWriter = writer;
    }

    public IpConfigStore() {
        this(new DelayedDiskWrite());
    }

    private boolean writeConfig(DataOutputStream out, int configKey, IpConfiguration config) throws IOException {
        boolean written = false;
        try {
            switch (m2242getandroidnetIpConfiguration$IpAssignmentSwitchesValues()[config.ipAssignment.ordinal()]) {
                case 1:
                    out.writeUTF(IP_ASSIGNMENT_KEY);
                    out.writeUTF(config.ipAssignment.toString());
                    written = true;
                    break;
                case 2:
                    out.writeUTF(IP_ASSIGNMENT_KEY);
                    out.writeUTF(config.ipAssignment.toString());
                    StaticIpConfiguration staticIpConfiguration = config.staticIpConfiguration;
                    if (staticIpConfiguration != null) {
                        if (staticIpConfiguration.ipAddress != null) {
                            LinkAddress ipAddress = staticIpConfiguration.ipAddress;
                            out.writeUTF(LINK_ADDRESS_KEY);
                            out.writeUTF(ipAddress.getAddress().getHostAddress());
                            out.writeInt(ipAddress.getPrefixLength());
                        }
                        if (staticIpConfiguration.gateway != null) {
                            out.writeUTF(GATEWAY_KEY);
                            out.writeInt(0);
                            out.writeInt(1);
                            out.writeUTF(staticIpConfiguration.gateway.getHostAddress());
                        }
                        for (InetAddress inetAddr : staticIpConfiguration.dnsServers) {
                            out.writeUTF(DNS_KEY);
                            out.writeUTF(inetAddr.getHostAddress());
                        }
                    }
                    written = true;
                    break;
                case 3:
                    break;
                default:
                    loge("Ignore invalid ip assignment while writing");
                    break;
            }
            switch (m2243getandroidnetIpConfiguration$ProxySettingsSwitchesValues()[config.proxySettings.ordinal()]) {
                case 1:
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    written = true;
                    break;
                case 2:
                    ProxyInfo proxyPacProperties = config.httpProxy;
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    out.writeUTF(PROXY_PAC_FILE);
                    out.writeUTF(proxyPacProperties.getPacFileUrl().toString());
                    written = true;
                    break;
                case 3:
                    ProxyInfo proxyProperties = config.httpProxy;
                    String exclusionList = proxyProperties.getExclusionListAsString();
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    out.writeUTF(PROXY_HOST_KEY);
                    out.writeUTF(proxyProperties.getHost());
                    out.writeUTF(PROXY_PORT_KEY);
                    out.writeInt(proxyProperties.getPort());
                    if (exclusionList != null) {
                        out.writeUTF(EXCLUSION_LIST_KEY);
                        out.writeUTF(exclusionList);
                    }
                    written = true;
                    break;
                case 4:
                    break;
                default:
                    loge("Ignore invalid proxy settings while writing");
                    break;
            }
            if (written) {
                out.writeUTF(ID_KEY);
                out.writeInt(configKey);
            }
        } catch (NullPointerException e) {
            loge("Failure in writing " + config + e);
        }
        out.writeUTF(EOS);
        return written;
    }

    public void writeIpAndProxyConfigurations(String filePath, final SparseArray<IpConfiguration> networks) {
        this.mWriter.write(filePath, new DelayedDiskWrite.Writer() {
            @Override
            public void onWriteCalled(DataOutputStream out) throws IOException {
                out.writeInt(2);
                for (int i = 0; i < networks.size(); i++) {
                    IpConfigStore.this.writeConfig(out, networks.keyAt(i), (IpConfiguration) networks.valueAt(i));
                }
            }
        });
    }

    public SparseArray<IpConfiguration> readIpAndProxyConfigurations(String filePath) throws Throwable {
        DataInputStream in;
        SparseArray<IpConfiguration> networks = new SparseArray<>();
        DataInputStream dataInputStream = null;
        try {
            try {
                in = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
            } catch (Throwable th) {
                th = th;
            }
        } catch (EOFException e) {
        } catch (IOException e2) {
            e = e2;
        }
        try {
            int version = in.readInt();
            if (version != 2 && version != 1) {
                loge("Bad version on IP configuration file, ignore read");
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e3) {
                    }
                }
                return null;
            }
            while (true) {
                int id = -1;
                IpConfiguration.IpAssignment ipAssignment = IpConfiguration.IpAssignment.DHCP;
                IpConfiguration.ProxySettings proxySettings = IpConfiguration.ProxySettings.NONE;
                StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                String proxyHost = null;
                String pacFileUrl = null;
                int proxyPort = -1;
                String utf = null;
                while (true) {
                    String key = in.readUTF();
                    try {
                        if (key.equals(ID_KEY)) {
                            id = in.readInt();
                        } else if (key.equals(IP_ASSIGNMENT_KEY)) {
                            ipAssignment = IpConfiguration.IpAssignment.valueOf(in.readUTF());
                        } else if (key.equals(LINK_ADDRESS_KEY)) {
                            LinkAddress linkAddr = new LinkAddress(NetworkUtils.numericToInetAddress(in.readUTF()), in.readInt());
                            if ((linkAddr.getAddress() instanceof Inet4Address) && staticIpConfiguration.ipAddress == null) {
                                staticIpConfiguration.ipAddress = linkAddr;
                            } else {
                                loge("Non-IPv4 or duplicate address: " + linkAddr);
                            }
                        } else if (key.equals(GATEWAY_KEY)) {
                            if (version == 1) {
                                InetAddress gateway = NetworkUtils.numericToInetAddress(in.readUTF());
                                if (staticIpConfiguration.gateway == null) {
                                    staticIpConfiguration.gateway = gateway;
                                } else {
                                    loge("Duplicate gateway: " + gateway.getHostAddress());
                                }
                            } else {
                                LinkAddress linkAddress = in.readInt() == 1 ? new LinkAddress(NetworkUtils.numericToInetAddress(in.readUTF()), in.readInt()) : null;
                                InetAddress gateway2 = in.readInt() == 1 ? NetworkUtils.numericToInetAddress(in.readUTF()) : null;
                                RouteInfo route = new RouteInfo(linkAddress, gateway2);
                                if (route.isIPv4Default() && staticIpConfiguration.gateway == null) {
                                    staticIpConfiguration.gateway = gateway2;
                                } else {
                                    loge("Non-IPv4 default or duplicate route: " + route);
                                }
                            }
                        } else if (key.equals(DNS_KEY)) {
                            staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(in.readUTF()));
                        } else if (key.equals(PROXY_SETTINGS_KEY)) {
                            proxySettings = IpConfiguration.ProxySettings.valueOf(in.readUTF());
                        } else if (key.equals(PROXY_HOST_KEY)) {
                            proxyHost = in.readUTF();
                        } else if (key.equals(PROXY_PORT_KEY)) {
                            proxyPort = in.readInt();
                        } else if (key.equals(PROXY_PAC_FILE)) {
                            pacFileUrl = in.readUTF();
                        } else if (key.equals(EXCLUSION_LIST_KEY)) {
                            utf = in.readUTF();
                        } else if (!key.equals(EOS)) {
                            loge("Ignore unknown key " + key + "while reading");
                        } else if (id != -1) {
                            IpConfiguration config = new IpConfiguration();
                            networks.put(id, config);
                            switch (m2242getandroidnetIpConfiguration$IpAssignmentSwitchesValues()[ipAssignment.ordinal()]) {
                                case 1:
                                    config.ipAssignment = ipAssignment;
                                    break;
                                case 2:
                                    config.staticIpConfiguration = staticIpConfiguration;
                                    config.ipAssignment = ipAssignment;
                                    break;
                                case 3:
                                    loge("BUG: Found UNASSIGNED IP on file, use DHCP");
                                    config.ipAssignment = IpConfiguration.IpAssignment.DHCP;
                                    break;
                                default:
                                    loge("Ignore invalid ip assignment while reading.");
                                    config.ipAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
                                    break;
                            }
                            switch (m2243getandroidnetIpConfiguration$ProxySettingsSwitchesValues()[proxySettings.ordinal()]) {
                                case 1:
                                    config.proxySettings = proxySettings;
                                    break;
                                case 2:
                                    ProxyInfo proxyPacProperties = new ProxyInfo(pacFileUrl);
                                    config.proxySettings = proxySettings;
                                    config.httpProxy = proxyPacProperties;
                                    break;
                                case 3:
                                    ProxyInfo proxyInfo = new ProxyInfo(proxyHost, proxyPort, utf);
                                    config.proxySettings = proxySettings;
                                    config.httpProxy = proxyInfo;
                                    break;
                                case 4:
                                    loge("BUG: Found UNASSIGNED proxy on file, use NONE");
                                    config.proxySettings = IpConfiguration.ProxySettings.NONE;
                                    break;
                                default:
                                    loge("Ignore invalid proxy settings while reading");
                                    config.proxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
                                    break;
                            }
                        }
                    } catch (IllegalArgumentException e4) {
                        loge("Ignore invalid address while reading" + e4);
                    }
                }
            }
        } catch (EOFException e5) {
            dataInputStream = in;
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Exception e6) {
                }
            }
            return networks;
        } catch (IOException e7) {
            e = e7;
            dataInputStream = in;
            loge("Error parsing configuration: " + e);
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Exception e8) {
                }
            }
            return networks;
        } catch (Throwable th2) {
            th = th2;
            dataInputStream = in;
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Exception e9) {
                }
            }
            throw th;
        }
    }

    protected void loge(String s) {
        Log.e(TAG, s);
    }

    protected void log(String s) {
        Log.d(TAG, s);
    }
}
