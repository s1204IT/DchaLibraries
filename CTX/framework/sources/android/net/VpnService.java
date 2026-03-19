package android.net;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.IConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.system.OsConstants;
import com.android.internal.net.VpnConfig;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class VpnService extends Service {
    public static final String SERVICE_INTERFACE = "android.net.VpnService";
    public static final String SERVICE_META_DATA_SUPPORTS_ALWAYS_ON = "android.net.VpnService.SUPPORTS_ALWAYS_ON";

    private static IConnectivityManager getService() {
        return IConnectivityManager.Stub.asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    }

    public static Intent prepare(Context context) {
        try {
            if (getService().prepareVpn(context.getPackageName(), null, context.getUserId())) {
                return null;
            }
        } catch (RemoteException e) {
        }
        return VpnConfig.getIntentForConfirmation();
    }

    @SystemApi
    public static void prepareAndAuthorize(Context context) {
        IConnectivityManager service = getService();
        String packageName = context.getPackageName();
        try {
            int userId = context.getUserId();
            if (!service.prepareVpn(packageName, null, userId)) {
                service.prepareVpn(null, packageName, userId);
            }
            service.setVpnPackageAuthorization(packageName, userId, true);
        } catch (RemoteException e) {
        }
    }

    public boolean protect(int i) {
        return NetworkUtils.protectFromVpn(i);
    }

    public boolean protect(Socket socket) {
        return protect(socket.getFileDescriptor$().getInt$());
    }

    public boolean protect(DatagramSocket datagramSocket) {
        return protect(datagramSocket.getFileDescriptor$().getInt$());
    }

    public boolean addAddress(InetAddress inetAddress, int i) {
        check(inetAddress, i);
        try {
            return getService().addVpnAddress(inetAddress.getHostAddress(), i);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean removeAddress(InetAddress inetAddress, int i) {
        check(inetAddress, i);
        try {
            return getService().removeVpnAddress(inetAddress.getHostAddress(), i);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean setUnderlyingNetworks(Network[] networkArr) {
        try {
            return getService().setUnderlyingNetworksForVpn(networkArr);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !"android.net.VpnService".equals(intent.getAction())) {
            return null;
        }
        return new Callback();
    }

    public void onRevoke() {
        stopSelf();
    }

    private class Callback extends Binder {
        private Callback() {
        }

        @Override
        protected boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) {
            if (i == 16777215) {
                VpnService.this.onRevoke();
                return true;
            }
            return false;
        }
    }

    private static void check(InetAddress inetAddress, int i) {
        if (inetAddress.isLoopbackAddress()) {
            throw new IllegalArgumentException("Bad address");
        }
        if (inetAddress instanceof Inet4Address) {
            if (i < 0 || i > 32) {
                throw new IllegalArgumentException("Bad prefixLength");
            }
        } else {
            if (inetAddress instanceof Inet6Address) {
                if (i < 0 || i > 128) {
                    throw new IllegalArgumentException("Bad prefixLength");
                }
                return;
            }
            throw new IllegalArgumentException("Unsupported family");
        }
    }

    public class Builder {
        private final VpnConfig mConfig = new VpnConfig();
        private final List<LinkAddress> mAddresses = new ArrayList();
        private final List<RouteInfo> mRoutes = new ArrayList();

        public Builder() {
            this.mConfig.user = VpnService.this.getClass().getName();
        }

        public Builder setSession(String str) {
            this.mConfig.session = str;
            return this;
        }

        public Builder setConfigureIntent(PendingIntent pendingIntent) {
            this.mConfig.configureIntent = pendingIntent;
            return this;
        }

        public Builder setMtu(int i) {
            if (i <= 0) {
                throw new IllegalArgumentException("Bad mtu");
            }
            this.mConfig.mtu = i;
            return this;
        }

        public Builder addAddress(InetAddress inetAddress, int i) {
            VpnService.check(inetAddress, i);
            if (inetAddress.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Bad address");
            }
            this.mAddresses.add(new LinkAddress(inetAddress, i));
            this.mConfig.updateAllowedFamilies(inetAddress);
            return this;
        }

        public Builder addAddress(String str, int i) {
            return addAddress(InetAddress.parseNumericAddress(str), i);
        }

        public Builder addRoute(InetAddress inetAddress, int i) {
            VpnService.check(inetAddress, i);
            int i2 = i / 8;
            byte[] address = inetAddress.getAddress();
            if (i2 < address.length) {
                address[i2] = (byte) (address[i2] << (i % 8));
                while (i2 < address.length) {
                    if (address[i2] == 0) {
                        i2++;
                    } else {
                        throw new IllegalArgumentException("Bad address");
                    }
                }
            }
            this.mRoutes.add(new RouteInfo(new IpPrefix(inetAddress, i), (InetAddress) null));
            this.mConfig.updateAllowedFamilies(inetAddress);
            return this;
        }

        public Builder addRoute(String str, int i) {
            return addRoute(InetAddress.parseNumericAddress(str), i);
        }

        public Builder addDnsServer(InetAddress inetAddress) {
            if (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Bad address");
            }
            if (this.mConfig.dnsServers == null) {
                this.mConfig.dnsServers = new ArrayList();
            }
            this.mConfig.dnsServers.add(inetAddress.getHostAddress());
            return this;
        }

        public Builder addDnsServer(String str) {
            return addDnsServer(InetAddress.parseNumericAddress(str));
        }

        public Builder addSearchDomain(String str) {
            if (this.mConfig.searchDomains == null) {
                this.mConfig.searchDomains = new ArrayList();
            }
            this.mConfig.searchDomains.add(str);
            return this;
        }

        public Builder allowFamily(int i) {
            if (i == OsConstants.AF_INET) {
                this.mConfig.allowIPv4 = true;
            } else if (i == OsConstants.AF_INET6) {
                this.mConfig.allowIPv6 = true;
            } else {
                throw new IllegalArgumentException(i + " is neither " + OsConstants.AF_INET + " nor " + OsConstants.AF_INET6);
            }
            return this;
        }

        private void verifyApp(String str) throws PackageManager.NameNotFoundException {
            try {
                IPackageManager.Stub.asInterface(ServiceManager.getService("package")).getApplicationInfo(str, 0, UserHandle.getCallingUserId());
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }

        public Builder addAllowedApplication(String str) throws PackageManager.NameNotFoundException {
            if (this.mConfig.disallowedApplications != null) {
                throw new UnsupportedOperationException("addDisallowedApplication already called");
            }
            verifyApp(str);
            if (this.mConfig.allowedApplications == null) {
                this.mConfig.allowedApplications = new ArrayList();
            }
            this.mConfig.allowedApplications.add(str);
            return this;
        }

        public Builder addDisallowedApplication(String str) throws PackageManager.NameNotFoundException {
            if (this.mConfig.allowedApplications != null) {
                throw new UnsupportedOperationException("addAllowedApplication already called");
            }
            verifyApp(str);
            if (this.mConfig.disallowedApplications == null) {
                this.mConfig.disallowedApplications = new ArrayList();
            }
            this.mConfig.disallowedApplications.add(str);
            return this;
        }

        public Builder allowBypass() {
            this.mConfig.allowBypass = true;
            return this;
        }

        public Builder setBlocking(boolean z) {
            this.mConfig.blocking = z;
            return this;
        }

        public Builder setUnderlyingNetworks(Network[] networkArr) {
            this.mConfig.underlyingNetworks = networkArr != null ? (Network[]) networkArr.clone() : null;
            return this;
        }

        public ParcelFileDescriptor establish() {
            this.mConfig.addresses = this.mAddresses;
            this.mConfig.routes = this.mRoutes;
            try {
                return VpnService.getService().establishVpn(this.mConfig);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
