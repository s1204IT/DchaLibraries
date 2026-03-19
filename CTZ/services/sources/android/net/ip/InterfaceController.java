package android.net.ip;

import android.net.INetd;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.OsConstants;
import java.net.InetAddress;

public class InterfaceController {
    private static final boolean DBG = false;
    private final String mIfName;
    private final SharedLog mLog;
    private final INetworkManagementService mNMS;
    private final INetd mNetd;

    public InterfaceController(String str, INetworkManagementService iNetworkManagementService, INetd iNetd, SharedLog sharedLog) {
        this.mIfName = str;
        this.mNMS = iNetworkManagementService;
        this.mNetd = iNetd;
        this.mLog = sharedLog;
    }

    public boolean setIPv4Address(LinkAddress linkAddress) {
        InterfaceConfiguration interfaceConfiguration = new InterfaceConfiguration();
        interfaceConfiguration.setLinkAddress(linkAddress);
        try {
            this.mNMS.setInterfaceConfig(this.mIfName, interfaceConfiguration);
            return true;
        } catch (RemoteException | IllegalStateException e) {
            logError("IPv4 configuration failed: %s", e);
            return false;
        }
    }

    public boolean clearIPv4Address() {
        try {
            InterfaceConfiguration interfaceConfiguration = new InterfaceConfiguration();
            interfaceConfiguration.setLinkAddress(new LinkAddress("0.0.0.0/0"));
            this.mNMS.setInterfaceConfig(this.mIfName, interfaceConfiguration);
            return true;
        } catch (RemoteException | IllegalStateException e) {
            logError("Failed to clear IPv4 address on interface %s: %s", this.mIfName, e);
            return false;
        }
    }

    public boolean enableIPv6() {
        try {
            this.mNMS.enableIpv6(this.mIfName);
            return true;
        } catch (RemoteException | IllegalStateException e) {
            logError("enabling IPv6 failed: %s", e);
            return false;
        }
    }

    public boolean disableIPv6() {
        try {
            this.mNMS.disableIpv6(this.mIfName);
            return true;
        } catch (RemoteException | IllegalStateException e) {
            logError("disabling IPv6 failed: %s", e);
            return false;
        }
    }

    public boolean setIPv6PrivacyExtensions(boolean z) {
        try {
            this.mNMS.setInterfaceIpv6PrivacyExtensions(this.mIfName, z);
            return true;
        } catch (RemoteException | IllegalStateException e) {
            logError("error setting IPv6 privacy extensions: %s", e);
            return false;
        }
    }

    public boolean setIPv6AddrGenModeIfSupported(int i) {
        try {
            this.mNMS.setIPv6AddrGenMode(this.mIfName, i);
        } catch (RemoteException e) {
            logError("Unable to set IPv6 addrgen mode: %s", e);
            return false;
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode != OsConstants.EOPNOTSUPP) {
                logError("Unable to set IPv6 addrgen mode: %s", e2);
                return false;
            }
        }
        return true;
    }

    public boolean addAddress(LinkAddress linkAddress) {
        return addAddress(linkAddress.getAddress(), linkAddress.getPrefixLength());
    }

    public boolean addAddress(InetAddress inetAddress, int i) {
        try {
            this.mNetd.interfaceAddAddress(this.mIfName, inetAddress.getHostAddress(), i);
            return true;
        } catch (ServiceSpecificException | RemoteException e) {
            logError("failed to add %s/%d: %s", inetAddress, Integer.valueOf(i), e);
            return false;
        }
    }

    public boolean removeAddress(InetAddress inetAddress, int i) {
        try {
            this.mNetd.interfaceDelAddress(this.mIfName, inetAddress.getHostAddress(), i);
            return true;
        } catch (ServiceSpecificException | RemoteException e) {
            logError("failed to remove %s/%d: %s", inetAddress, Integer.valueOf(i), e);
            return false;
        }
    }

    public boolean clearAllAddresses() {
        try {
            this.mNMS.clearInterfaceAddresses(this.mIfName);
            return true;
        } catch (Exception e) {
            logError("Failed to clear addresses: %s", e);
            return false;
        }
    }

    private void logError(String str, Object... objArr) {
        this.mLog.e(String.format(str, objArr));
    }
}
