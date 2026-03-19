package com.android.server.net;

import android.net.LinkProperties;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class DnsServerRepository {
    public static final int NUM_CURRENT_SERVERS = 3;
    public static final int NUM_SERVERS = 12;
    public static final String TAG = "DnsServerRepository";
    private Set<InetAddress> mCurrentServers = new HashSet();
    private ArrayList<DnsServerEntry> mAllServers = new ArrayList<>(12);
    private HashMap<InetAddress, DnsServerEntry> mIndex = new HashMap<>(12);

    public synchronized void setDnsServersOn(LinkProperties linkProperties) {
        linkProperties.setDnsServers(this.mCurrentServers);
    }

    public synchronized boolean addServers(long j, String[] strArr) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j2 = (1000 * j) + jCurrentTimeMillis;
        for (String str : strArr) {
            try {
                InetAddress numericAddress = InetAddress.parseNumericAddress(str);
                if (!updateExistingEntry(numericAddress, j2) && j2 > jCurrentTimeMillis) {
                    DnsServerEntry dnsServerEntry = new DnsServerEntry(numericAddress, j2);
                    this.mAllServers.add(dnsServerEntry);
                    this.mIndex.put(numericAddress, dnsServerEntry);
                }
            } catch (IllegalArgumentException e) {
            }
        }
        Collections.sort(this.mAllServers);
        return updateCurrentServers();
    }

    private synchronized boolean updateExistingEntry(InetAddress inetAddress, long j) {
        DnsServerEntry dnsServerEntry = this.mIndex.get(inetAddress);
        if (dnsServerEntry != null) {
            dnsServerEntry.expiry = j;
            return true;
        }
        return false;
    }

    private synchronized boolean updateCurrentServers() {
        boolean zAdd;
        long jCurrentTimeMillis = System.currentTimeMillis();
        zAdd = false;
        for (int size = this.mAllServers.size() - 1; size >= 0 && (size >= 12 || this.mAllServers.get(size).expiry < jCurrentTimeMillis); size--) {
            DnsServerEntry dnsServerEntryRemove = this.mAllServers.remove(size);
            this.mIndex.remove(dnsServerEntryRemove.address);
            zAdd |= this.mCurrentServers.remove(dnsServerEntryRemove.address);
        }
        for (DnsServerEntry dnsServerEntry : this.mAllServers) {
            if (this.mCurrentServers.size() >= 3) {
                break;
            }
            zAdd |= this.mCurrentServers.add(dnsServerEntry.address);
        }
        return zAdd;
    }
}
