package android.net;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class LinkProperties implements Parcelable {
    public static final Parcelable.Creator<LinkProperties> CREATOR = new Parcelable.Creator<LinkProperties>() {
        @Override
        public LinkProperties createFromParcel(Parcel parcel) {
            LinkProperties linkProperties = new LinkProperties();
            String string = parcel.readString();
            if (string != null) {
                linkProperties.setInterfaceName(string);
            }
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                linkProperties.addLinkAddress((LinkAddress) parcel.readParcelable(null));
            }
            int i3 = parcel.readInt();
            for (int i4 = 0; i4 < i3; i4++) {
                try {
                    linkProperties.addDnsServer(InetAddress.getByAddress(parcel.createByteArray()));
                } catch (UnknownHostException e) {
                }
            }
            int i5 = parcel.readInt();
            for (int i6 = 0; i6 < i5; i6++) {
                try {
                    linkProperties.addValidatedPrivateDnsServer(InetAddress.getByAddress(parcel.createByteArray()));
                } catch (UnknownHostException e2) {
                }
            }
            linkProperties.setUsePrivateDns(parcel.readBoolean());
            linkProperties.setPrivateDnsServerName(parcel.readString());
            linkProperties.setDomains(parcel.readString());
            linkProperties.setMtu(parcel.readInt());
            linkProperties.setTcpBufferSizes(parcel.readString());
            int i7 = parcel.readInt();
            for (int i8 = 0; i8 < i7; i8++) {
                linkProperties.addRoute((RouteInfo) parcel.readParcelable(null));
            }
            if (parcel.readByte() == 1) {
                linkProperties.setHttpProxy((ProxyInfo) parcel.readParcelable(null));
            }
            ArrayList arrayList = new ArrayList();
            parcel.readList(arrayList, LinkProperties.class.getClassLoader());
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                linkProperties.addStackedLink((LinkProperties) it.next());
            }
            return linkProperties;
        }

        @Override
        public LinkProperties[] newArray(int i) {
            return new LinkProperties[i];
        }
    };
    private static final int MAX_MTU = 10000;
    private static final int MIN_MTU = 68;
    private static final int MIN_MTU_V6 = 1280;
    private String mDomains;
    private ProxyInfo mHttpProxy;
    private String mIfaceName;
    private int mMtu;
    private String mPrivateDnsServerName;
    private String mTcpBufferSizes;
    private boolean mUsePrivateDns;
    private ArrayList<LinkAddress> mLinkAddresses = new ArrayList<>();
    private ArrayList<InetAddress> mDnses = new ArrayList<>();
    private ArrayList<InetAddress> mValidatedPrivateDnses = new ArrayList<>();
    private ArrayList<RouteInfo> mRoutes = new ArrayList<>();
    private Hashtable<String, LinkProperties> mStackedLinks = new Hashtable<>();

    public enum ProvisioningChange {
        STILL_NOT_PROVISIONED,
        LOST_PROVISIONING,
        GAINED_PROVISIONING,
        STILL_PROVISIONED
    }

    public static class CompareResult<T> {
        public final List<T> removed = new ArrayList();
        public final List<T> added = new ArrayList();

        public CompareResult() {
        }

        public CompareResult(Collection<T> collection, Collection<T> collection2) {
            if (collection != null) {
                this.removed.addAll(collection);
            }
            if (collection2 != null) {
                for (T t : collection2) {
                    if (!this.removed.remove(t)) {
                        this.added.add(t);
                    }
                }
            }
        }

        public String toString() {
            String str = "removed=[";
            Iterator<T> it = this.removed.iterator();
            while (it.hasNext()) {
                str = str + it.next().toString() + ",";
            }
            String str2 = str + "] added=[";
            Iterator<T> it2 = this.added.iterator();
            while (it2.hasNext()) {
                str2 = str2 + it2.next().toString() + ",";
            }
            return str2 + "]";
        }
    }

    public static ProvisioningChange compareProvisioning(LinkProperties linkProperties, LinkProperties linkProperties2) {
        if (linkProperties.isProvisioned() && linkProperties2.isProvisioned()) {
            if ((linkProperties.isIPv4Provisioned() && !linkProperties2.isIPv4Provisioned()) || (linkProperties.isIPv6Provisioned() && !linkProperties2.isIPv6Provisioned())) {
                return ProvisioningChange.LOST_PROVISIONING;
            }
            return ProvisioningChange.STILL_PROVISIONED;
        }
        if (linkProperties.isProvisioned() && !linkProperties2.isProvisioned()) {
            return ProvisioningChange.LOST_PROVISIONING;
        }
        if (!linkProperties.isProvisioned() && linkProperties2.isProvisioned()) {
            return ProvisioningChange.GAINED_PROVISIONING;
        }
        return ProvisioningChange.STILL_NOT_PROVISIONED;
    }

    public LinkProperties() {
    }

    public LinkProperties(LinkProperties linkProperties) {
        if (linkProperties != null) {
            this.mIfaceName = linkProperties.getInterfaceName();
            Iterator<LinkAddress> it = linkProperties.getLinkAddresses().iterator();
            while (it.hasNext()) {
                this.mLinkAddresses.add(it.next());
            }
            Iterator<InetAddress> it2 = linkProperties.getDnsServers().iterator();
            while (it2.hasNext()) {
                this.mDnses.add(it2.next());
            }
            Iterator<InetAddress> it3 = linkProperties.getValidatedPrivateDnsServers().iterator();
            while (it3.hasNext()) {
                this.mValidatedPrivateDnses.add(it3.next());
            }
            this.mUsePrivateDns = linkProperties.mUsePrivateDns;
            this.mPrivateDnsServerName = linkProperties.mPrivateDnsServerName;
            this.mDomains = linkProperties.getDomains();
            Iterator<RouteInfo> it4 = linkProperties.getRoutes().iterator();
            while (it4.hasNext()) {
                this.mRoutes.add(it4.next());
            }
            this.mHttpProxy = linkProperties.getHttpProxy() == null ? null : new ProxyInfo(linkProperties.getHttpProxy());
            Iterator<LinkProperties> it5 = linkProperties.mStackedLinks.values().iterator();
            while (it5.hasNext()) {
                addStackedLink(it5.next());
            }
            setMtu(linkProperties.getMtu());
            this.mTcpBufferSizes = linkProperties.mTcpBufferSizes;
        }
    }

    public void setInterfaceName(String str) {
        this.mIfaceName = str;
        ArrayList<RouteInfo> arrayList = new ArrayList<>(this.mRoutes.size());
        Iterator<RouteInfo> it = this.mRoutes.iterator();
        while (it.hasNext()) {
            arrayList.add(routeWithInterface(it.next()));
        }
        this.mRoutes = arrayList;
    }

    public String getInterfaceName() {
        return this.mIfaceName;
    }

    public List<String> getAllInterfaceNames() {
        ArrayList arrayList = new ArrayList(this.mStackedLinks.size() + 1);
        if (this.mIfaceName != null) {
            arrayList.add(new String(this.mIfaceName));
        }
        Iterator<LinkProperties> it = this.mStackedLinks.values().iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next().getAllInterfaceNames());
        }
        return arrayList;
    }

    public List<InetAddress> getAddresses() {
        ArrayList arrayList = new ArrayList();
        Iterator<LinkAddress> it = this.mLinkAddresses.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getAddress());
        }
        return Collections.unmodifiableList(arrayList);
    }

    public List<InetAddress> getAllAddresses() {
        ArrayList arrayList = new ArrayList();
        Iterator<LinkAddress> it = this.mLinkAddresses.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getAddress());
        }
        Iterator<LinkProperties> it2 = this.mStackedLinks.values().iterator();
        while (it2.hasNext()) {
            arrayList.addAll(it2.next().getAllAddresses());
        }
        return arrayList;
    }

    private int findLinkAddressIndex(LinkAddress linkAddress) {
        for (int i = 0; i < this.mLinkAddresses.size(); i++) {
            if (this.mLinkAddresses.get(i).isSameAddressAs(linkAddress)) {
                return i;
            }
        }
        return -1;
    }

    public boolean addLinkAddress(LinkAddress linkAddress) {
        if (linkAddress == null) {
            return false;
        }
        int iFindLinkAddressIndex = findLinkAddressIndex(linkAddress);
        if (iFindLinkAddressIndex < 0) {
            this.mLinkAddresses.add(linkAddress);
            return true;
        }
        if (this.mLinkAddresses.get(iFindLinkAddressIndex).equals(linkAddress)) {
            return false;
        }
        this.mLinkAddresses.set(iFindLinkAddressIndex, linkAddress);
        return true;
    }

    public boolean removeLinkAddress(LinkAddress linkAddress) {
        int iFindLinkAddressIndex = findLinkAddressIndex(linkAddress);
        if (iFindLinkAddressIndex >= 0) {
            this.mLinkAddresses.remove(iFindLinkAddressIndex);
            return true;
        }
        return false;
    }

    public List<LinkAddress> getLinkAddresses() {
        return Collections.unmodifiableList(this.mLinkAddresses);
    }

    public List<LinkAddress> getAllLinkAddresses() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mLinkAddresses);
        Iterator<LinkProperties> it = this.mStackedLinks.values().iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next().getAllLinkAddresses());
        }
        return arrayList;
    }

    public void setLinkAddresses(Collection<LinkAddress> collection) {
        this.mLinkAddresses.clear();
        Iterator<LinkAddress> it = collection.iterator();
        while (it.hasNext()) {
            addLinkAddress(it.next());
        }
    }

    public boolean addDnsServer(InetAddress inetAddress) {
        if (inetAddress != null && !this.mDnses.contains(inetAddress)) {
            this.mDnses.add(inetAddress);
            return true;
        }
        return false;
    }

    public boolean removeDnsServer(InetAddress inetAddress) {
        if (inetAddress != null) {
            return this.mDnses.remove(inetAddress);
        }
        return false;
    }

    public void setDnsServers(Collection<InetAddress> collection) {
        this.mDnses.clear();
        Iterator<InetAddress> it = collection.iterator();
        while (it.hasNext()) {
            addDnsServer(it.next());
        }
    }

    public List<InetAddress> getDnsServers() {
        return Collections.unmodifiableList(this.mDnses);
    }

    public void setUsePrivateDns(boolean z) {
        this.mUsePrivateDns = z;
    }

    public boolean isPrivateDnsActive() {
        return this.mUsePrivateDns;
    }

    public void setPrivateDnsServerName(String str) {
        this.mPrivateDnsServerName = str;
    }

    public String getPrivateDnsServerName() {
        return this.mPrivateDnsServerName;
    }

    public boolean addValidatedPrivateDnsServer(InetAddress inetAddress) {
        if (inetAddress != null && !this.mValidatedPrivateDnses.contains(inetAddress)) {
            this.mValidatedPrivateDnses.add(inetAddress);
            return true;
        }
        return false;
    }

    public boolean removeValidatedPrivateDnsServer(InetAddress inetAddress) {
        if (inetAddress != null) {
            return this.mValidatedPrivateDnses.remove(inetAddress);
        }
        return false;
    }

    public void setValidatedPrivateDnsServers(Collection<InetAddress> collection) {
        this.mValidatedPrivateDnses.clear();
        Iterator<InetAddress> it = collection.iterator();
        while (it.hasNext()) {
            addValidatedPrivateDnsServer(it.next());
        }
    }

    public List<InetAddress> getValidatedPrivateDnsServers() {
        return Collections.unmodifiableList(this.mValidatedPrivateDnses);
    }

    public void setDomains(String str) {
        this.mDomains = str;
    }

    public String getDomains() {
        return this.mDomains;
    }

    public void setMtu(int i) {
        this.mMtu = i;
    }

    public int getMtu() {
        return this.mMtu;
    }

    public void setTcpBufferSizes(String str) {
        this.mTcpBufferSizes = str;
    }

    public String getTcpBufferSizes() {
        return this.mTcpBufferSizes;
    }

    private RouteInfo routeWithInterface(RouteInfo routeInfo) {
        return new RouteInfo(routeInfo.getDestination(), routeInfo.getGateway(), this.mIfaceName, routeInfo.getType());
    }

    public boolean addRoute(RouteInfo routeInfo) {
        if (routeInfo != null) {
            String str = routeInfo.getInterface();
            if (str != null && !str.equals(this.mIfaceName)) {
                throw new IllegalArgumentException("Route added with non-matching interface: " + str + " vs. " + this.mIfaceName);
            }
            RouteInfo routeInfoRouteWithInterface = routeWithInterface(routeInfo);
            if (!this.mRoutes.contains(routeInfoRouteWithInterface)) {
                this.mRoutes.add(routeInfoRouteWithInterface);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean removeRoute(RouteInfo routeInfo) {
        return routeInfo != null && Objects.equals(this.mIfaceName, routeInfo.getInterface()) && this.mRoutes.remove(routeInfo);
    }

    public List<RouteInfo> getRoutes() {
        return Collections.unmodifiableList(this.mRoutes);
    }

    public void ensureDirectlyConnectedRoutes() {
        Iterator<LinkAddress> it = this.mLinkAddresses.iterator();
        while (it.hasNext()) {
            addRoute(new RouteInfo(it.next(), (InetAddress) null, this.mIfaceName));
        }
    }

    public List<RouteInfo> getAllRoutes() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mRoutes);
        Iterator<LinkProperties> it = this.mStackedLinks.values().iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next().getAllRoutes());
        }
        return arrayList;
    }

    public void setHttpProxy(ProxyInfo proxyInfo) {
        this.mHttpProxy = proxyInfo;
    }

    public ProxyInfo getHttpProxy() {
        return this.mHttpProxy;
    }

    public boolean addStackedLink(LinkProperties linkProperties) {
        if (linkProperties != null && linkProperties.getInterfaceName() != null) {
            this.mStackedLinks.put(linkProperties.getInterfaceName(), linkProperties);
            return true;
        }
        return false;
    }

    public boolean removeStackedLink(String str) {
        return (str == null || this.mStackedLinks.remove(str) == null) ? false : true;
    }

    public List<LinkProperties> getStackedLinks() {
        if (this.mStackedLinks.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<LinkProperties> it = this.mStackedLinks.values().iterator();
        while (it.hasNext()) {
            arrayList.add(new LinkProperties(it.next()));
        }
        return Collections.unmodifiableList(arrayList);
    }

    public void clear() {
        this.mIfaceName = null;
        this.mLinkAddresses.clear();
        this.mDnses.clear();
        this.mUsePrivateDns = false;
        this.mPrivateDnsServerName = null;
        this.mDomains = null;
        this.mRoutes.clear();
        this.mHttpProxy = null;
        this.mStackedLinks.clear();
        this.mMtu = 0;
        this.mTcpBufferSizes = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        String str;
        String str2;
        if (this.mIfaceName == null) {
            str = "";
        } else {
            str = "InterfaceName: " + this.mIfaceName + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
        }
        String str3 = "LinkAddresses: [";
        Iterator<LinkAddress> it = this.mLinkAddresses.iterator();
        while (it.hasNext()) {
            str3 = str3 + it.next().toString() + ",";
        }
        String str4 = str3 + "] ";
        String str5 = "DnsAddresses: [";
        Iterator<InetAddress> it2 = this.mDnses.iterator();
        while (it2.hasNext()) {
            str5 = str5 + it2.next().getHostAddress() + ",";
        }
        String str6 = str5 + "] ";
        String str7 = "UsePrivateDns: " + this.mUsePrivateDns + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
        String str8 = "PrivateDnsServerName: " + this.mPrivateDnsServerName + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
        if (!this.mValidatedPrivateDnses.isEmpty()) {
            String str9 = "ValidatedPrivateDnsAddresses: [";
            Iterator<InetAddress> it3 = this.mValidatedPrivateDnses.iterator();
            while (it3.hasNext()) {
                str9 = str9 + it3.next().getHostAddress() + ",";
            }
            String str10 = str9 + "] ";
        }
        String str11 = "Domains: " + this.mDomains;
        String str12 = " MTU: " + this.mMtu;
        String str13 = "";
        if (this.mTcpBufferSizes != null) {
            str13 = " TcpBufferSizes: " + this.mTcpBufferSizes;
        }
        String str14 = " Routes: [";
        Iterator<RouteInfo> it4 = this.mRoutes.iterator();
        while (it4.hasNext()) {
            str14 = str14 + it4.next().toString() + ",";
        }
        String str15 = str14 + "] ";
        if (this.mHttpProxy == null) {
            str2 = "";
        } else {
            str2 = " HttpProxy: " + this.mHttpProxy.toString() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
        }
        String str16 = "";
        if (this.mStackedLinks.values().size() > 0) {
            String str17 = " Stacked: [";
            Iterator<LinkProperties> it5 = this.mStackedLinks.values().iterator();
            while (it5.hasNext()) {
                str17 = str17 + " [" + it5.next().toString() + " ],";
            }
            str16 = str17 + "] ";
        }
        return "{" + str + str4 + str15 + str6 + str7 + str8 + str11 + str12 + str13 + str2 + str16 + "}";
    }

    public boolean hasIPv4Address() {
        Iterator<LinkAddress> it = this.mLinkAddresses.iterator();
        while (it.hasNext()) {
            if (it.next().getAddress() instanceof Inet4Address) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIPv4AddressOnInterface(String str) {
        return (Objects.equals(str, this.mIfaceName) && hasIPv4Address()) || (str != null && this.mStackedLinks.containsKey(str) && this.mStackedLinks.get(str).hasIPv4Address());
    }

    public boolean hasGlobalIPv6Address() {
        for (LinkAddress linkAddress : this.mLinkAddresses) {
            if ((linkAddress.getAddress() instanceof Inet6Address) && linkAddress.isGlobalPreferred()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIPv4DefaultRoute() {
        Iterator<RouteInfo> it = this.mRoutes.iterator();
        while (it.hasNext()) {
            if (it.next().isIPv4Default()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIPv6DefaultRoute() {
        Iterator<RouteInfo> it = this.mRoutes.iterator();
        while (it.hasNext()) {
            if (it.next().isIPv6Default()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIPv4DnsServer() {
        Iterator<InetAddress> it = this.mDnses.iterator();
        while (it.hasNext()) {
            if (it.next() instanceof Inet4Address) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIPv6DnsServer() {
        Iterator<InetAddress> it = this.mDnses.iterator();
        while (it.hasNext()) {
            if (it.next() instanceof Inet6Address) {
                return true;
            }
        }
        return false;
    }

    public boolean isIPv4Provisioned() {
        return hasIPv4Address() && hasIPv4DefaultRoute() && hasIPv4DnsServer();
    }

    public boolean isIPv6Provisioned() {
        return hasGlobalIPv6Address() && hasIPv6DefaultRoute() && hasIPv6DnsServer();
    }

    public boolean isProvisioned() {
        return isIPv4Provisioned() || isIPv6Provisioned();
    }

    public boolean isReachable(InetAddress inetAddress) {
        RouteInfo routeInfoSelectBestRoute = RouteInfo.selectBestRoute(getAllRoutes(), inetAddress);
        if (routeInfoSelectBestRoute == null) {
            return false;
        }
        if (inetAddress instanceof Inet4Address) {
            return hasIPv4AddressOnInterface(routeInfoSelectBestRoute.getInterface());
        }
        if (inetAddress instanceof Inet6Address) {
            return inetAddress.isLinkLocalAddress() ? ((Inet6Address) inetAddress).getScopeId() != 0 : !routeInfoSelectBestRoute.hasGateway() || hasGlobalIPv6Address();
        }
        return false;
    }

    public boolean isIdenticalInterfaceName(LinkProperties linkProperties) {
        return TextUtils.equals(getInterfaceName(), linkProperties.getInterfaceName());
    }

    public boolean isIdenticalAddresses(LinkProperties linkProperties) {
        List<InetAddress> addresses = linkProperties.getAddresses();
        List<InetAddress> addresses2 = getAddresses();
        if (addresses2.size() == addresses.size()) {
            return addresses2.containsAll(addresses);
        }
        return false;
    }

    public boolean isIdenticalDnses(LinkProperties linkProperties) {
        List<InetAddress> dnsServers = linkProperties.getDnsServers();
        String domains = linkProperties.getDomains();
        if (this.mDomains == null) {
            if (domains != null) {
                return false;
            }
        } else if (!this.mDomains.equals(domains)) {
            return false;
        }
        if (this.mDnses.size() == dnsServers.size()) {
            return this.mDnses.containsAll(dnsServers);
        }
        return false;
    }

    public boolean isIdenticalPrivateDns(LinkProperties linkProperties) {
        return isPrivateDnsActive() == linkProperties.isPrivateDnsActive() && TextUtils.equals(getPrivateDnsServerName(), linkProperties.getPrivateDnsServerName());
    }

    public boolean isIdenticalValidatedPrivateDnses(LinkProperties linkProperties) {
        List<InetAddress> validatedPrivateDnsServers = linkProperties.getValidatedPrivateDnsServers();
        if (this.mValidatedPrivateDnses.size() == validatedPrivateDnsServers.size()) {
            return this.mValidatedPrivateDnses.containsAll(validatedPrivateDnsServers);
        }
        return false;
    }

    public boolean isIdenticalRoutes(LinkProperties linkProperties) {
        List<RouteInfo> routes = linkProperties.getRoutes();
        if (this.mRoutes.size() == routes.size()) {
            return this.mRoutes.containsAll(routes);
        }
        return false;
    }

    public boolean isIdenticalHttpProxy(LinkProperties linkProperties) {
        if (getHttpProxy() == null) {
            return linkProperties.getHttpProxy() == null;
        }
        return getHttpProxy().equals(linkProperties.getHttpProxy());
    }

    public boolean isIdenticalStackedLinks(LinkProperties linkProperties) {
        if (!this.mStackedLinks.keySet().equals(linkProperties.mStackedLinks.keySet())) {
            return false;
        }
        for (LinkProperties linkProperties2 : this.mStackedLinks.values()) {
            if (!linkProperties2.equals(linkProperties.mStackedLinks.get(linkProperties2.getInterfaceName()))) {
                return false;
            }
        }
        return true;
    }

    public boolean isIdenticalMtu(LinkProperties linkProperties) {
        return getMtu() == linkProperties.getMtu();
    }

    public boolean isIdenticalTcpBufferSizes(LinkProperties linkProperties) {
        return Objects.equals(this.mTcpBufferSizes, linkProperties.mTcpBufferSizes);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LinkProperties)) {
            return false;
        }
        LinkProperties linkProperties = (LinkProperties) obj;
        return isIdenticalInterfaceName(linkProperties) && isIdenticalAddresses(linkProperties) && isIdenticalDnses(linkProperties) && isIdenticalPrivateDns(linkProperties) && isIdenticalValidatedPrivateDnses(linkProperties) && isIdenticalRoutes(linkProperties) && isIdenticalHttpProxy(linkProperties) && isIdenticalStackedLinks(linkProperties) && isIdenticalMtu(linkProperties) && isIdenticalTcpBufferSizes(linkProperties);
    }

    public CompareResult<LinkAddress> compareAddresses(LinkProperties linkProperties) {
        return new CompareResult<>(this.mLinkAddresses, linkProperties != null ? linkProperties.getLinkAddresses() : null);
    }

    public CompareResult<InetAddress> compareDnses(LinkProperties linkProperties) {
        return new CompareResult<>(this.mDnses, linkProperties != null ? linkProperties.getDnsServers() : null);
    }

    public CompareResult<InetAddress> compareValidatedPrivateDnses(LinkProperties linkProperties) {
        return new CompareResult<>(this.mValidatedPrivateDnses, linkProperties != null ? linkProperties.getValidatedPrivateDnsServers() : null);
    }

    public CompareResult<RouteInfo> compareAllRoutes(LinkProperties linkProperties) {
        return new CompareResult<>(getAllRoutes(), linkProperties != null ? linkProperties.getAllRoutes() : null);
    }

    public CompareResult<String> compareAllInterfaceNames(LinkProperties linkProperties) {
        return new CompareResult<>(getAllInterfaceNames(), linkProperties != null ? linkProperties.getAllInterfaceNames() : null);
    }

    public int hashCode() {
        int iHashCode;
        if (this.mIfaceName == null) {
            iHashCode = 0;
        } else {
            iHashCode = this.mIfaceName.hashCode() + (this.mLinkAddresses.size() * 31) + (this.mDnses.size() * 37) + (this.mValidatedPrivateDnses.size() * 61) + (this.mDomains == null ? 0 : this.mDomains.hashCode()) + (this.mRoutes.size() * 41) + (this.mHttpProxy == null ? 0 : this.mHttpProxy.hashCode()) + (this.mStackedLinks.hashCode() * 47);
        }
        return iHashCode + (this.mMtu * 51) + (this.mTcpBufferSizes == null ? 0 : this.mTcpBufferSizes.hashCode()) + (this.mUsePrivateDns ? 57 : 0) + (this.mPrivateDnsServerName != null ? this.mPrivateDnsServerName.hashCode() : 0);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getInterfaceName());
        parcel.writeInt(this.mLinkAddresses.size());
        Iterator<LinkAddress> it = this.mLinkAddresses.iterator();
        while (it.hasNext()) {
            parcel.writeParcelable(it.next(), i);
        }
        parcel.writeInt(this.mDnses.size());
        Iterator<InetAddress> it2 = this.mDnses.iterator();
        while (it2.hasNext()) {
            parcel.writeByteArray(it2.next().getAddress());
        }
        parcel.writeInt(this.mValidatedPrivateDnses.size());
        Iterator<InetAddress> it3 = this.mValidatedPrivateDnses.iterator();
        while (it3.hasNext()) {
            parcel.writeByteArray(it3.next().getAddress());
        }
        parcel.writeBoolean(this.mUsePrivateDns);
        parcel.writeString(this.mPrivateDnsServerName);
        parcel.writeString(this.mDomains);
        parcel.writeInt(this.mMtu);
        parcel.writeString(this.mTcpBufferSizes);
        parcel.writeInt(this.mRoutes.size());
        Iterator<RouteInfo> it4 = this.mRoutes.iterator();
        while (it4.hasNext()) {
            parcel.writeParcelable(it4.next(), i);
        }
        if (this.mHttpProxy != null) {
            parcel.writeByte((byte) 1);
            parcel.writeParcelable(this.mHttpProxy, i);
        } else {
            parcel.writeByte((byte) 0);
        }
        parcel.writeList(new ArrayList(this.mStackedLinks.values()));
    }

    public static boolean isValidMtu(int i, boolean z) {
        if (z) {
            if (i >= 1280 && i <= 10000) {
                return true;
            }
            return false;
        }
        if (i >= 68 && i <= 10000) {
            return true;
        }
        return false;
    }
}
