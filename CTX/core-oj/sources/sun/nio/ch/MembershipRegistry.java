package sun.nio.ch;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.MembershipKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class MembershipRegistry {
    private Map<InetAddress, List<MembershipKeyImpl>> groups = null;

    MembershipRegistry() {
    }

    MembershipKey checkMembership(InetAddress inetAddress, NetworkInterface networkInterface, InetAddress inetAddress2) {
        List<MembershipKeyImpl> list;
        if (this.groups != null && (list = this.groups.get(inetAddress)) != null) {
            for (MembershipKeyImpl membershipKeyImpl : list) {
                if (membershipKeyImpl.networkInterface().equals(networkInterface)) {
                    if (inetAddress2 == null) {
                        if (membershipKeyImpl.sourceAddress() == null) {
                            return membershipKeyImpl;
                        }
                        throw new IllegalStateException("Already a member to receive all packets");
                    }
                    if (membershipKeyImpl.sourceAddress() == null) {
                        throw new IllegalStateException("Already have source-specific membership");
                    }
                    if (inetAddress2.equals(membershipKeyImpl.sourceAddress())) {
                        return membershipKeyImpl;
                    }
                }
            }
            return null;
        }
        return null;
    }

    void add(MembershipKeyImpl membershipKeyImpl) {
        List<MembershipKeyImpl> linkedList;
        InetAddress inetAddressGroup = membershipKeyImpl.group();
        if (this.groups == null) {
            this.groups = new HashMap();
            linkedList = null;
        } else {
            linkedList = this.groups.get(inetAddressGroup);
        }
        if (linkedList == null) {
            linkedList = new LinkedList<>();
            this.groups.put(inetAddressGroup, linkedList);
        }
        linkedList.add(membershipKeyImpl);
    }

    void remove(MembershipKeyImpl membershipKeyImpl) {
        InetAddress inetAddressGroup = membershipKeyImpl.group();
        List<MembershipKeyImpl> list = this.groups.get(inetAddressGroup);
        if (list != null) {
            Iterator<MembershipKeyImpl> it = list.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                } else if (it.next() == membershipKeyImpl) {
                    it.remove();
                    break;
                }
            }
            if (list.isEmpty()) {
                this.groups.remove(inetAddressGroup);
            }
        }
    }

    void invalidateAll() {
        if (this.groups != null) {
            Iterator<InetAddress> it = this.groups.keySet().iterator();
            while (it.hasNext()) {
                Iterator<MembershipKeyImpl> it2 = this.groups.get(it.next()).iterator();
                while (it2.hasNext()) {
                    it2.next().invalidate();
                }
            }
        }
    }
}
