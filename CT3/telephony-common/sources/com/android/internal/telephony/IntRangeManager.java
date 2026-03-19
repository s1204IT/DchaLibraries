package com.android.internal.telephony;

import android.telephony.Rlog;
import java.util.ArrayList;

public abstract class IntRangeManager {
    private static final int INITIAL_CLIENTS_ARRAY_SIZE = 4;
    private ArrayList<IntRange> mRanges = new ArrayList<>();

    protected abstract void addRange(int i, int i2, boolean z);

    protected abstract boolean finishUpdate();

    protected abstract void startUpdate();

    private class IntRange {
        final ArrayList<ClientRange> mClients;
        int mEndId;
        int mStartId;

        IntRange(int startId, int endId, String client) {
            this.mStartId = startId;
            this.mEndId = endId;
            this.mClients = new ArrayList<>(4);
            this.mClients.add(IntRangeManager.this.new ClientRange(startId, endId, client));
        }

        IntRange(ClientRange clientRange) {
            this.mStartId = clientRange.mStartId;
            this.mEndId = clientRange.mEndId;
            this.mClients = new ArrayList<>(4);
            this.mClients.add(clientRange);
        }

        IntRange(IntRange intRange, int numElements) {
            this.mStartId = intRange.mStartId;
            this.mEndId = intRange.mEndId;
            this.mClients = new ArrayList<>(intRange.mClients.size());
            for (int i = 0; i < numElements; i++) {
                this.mClients.add(intRange.mClients.get(i));
            }
        }

        void insert(ClientRange range) {
            int len = this.mClients.size();
            int insert = -1;
            for (int i = 0; i < len; i++) {
                ClientRange nextRange = this.mClients.get(i);
                if (range.mStartId <= nextRange.mStartId) {
                    if (!range.equals(nextRange)) {
                        if (range.mStartId == nextRange.mStartId && range.mEndId > nextRange.mEndId) {
                            insert = i + 1;
                            if (insert >= len) {
                                break;
                            }
                        } else {
                            this.mClients.add(i, range);
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
            if (insert != -1 && insert < len) {
                this.mClients.add(insert, range);
            } else {
                this.mClients.add(range);
            }
        }
    }

    private class ClientRange {
        final String mClient;
        final int mEndId;
        final int mStartId;

        ClientRange(int startId, int endId, String client) {
            this.mStartId = startId;
            this.mEndId = endId;
            this.mClient = client;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof ClientRange)) {
                return false;
            }
            ClientRange other = (ClientRange) o;
            if (this.mStartId == other.mStartId && this.mEndId == other.mEndId) {
                return this.mClient.equals(other.mClient);
            }
            return false;
        }

        public int hashCode() {
            return (((this.mStartId * 31) + this.mEndId) * 31) + this.mClient.hashCode();
        }
    }

    protected IntRangeManager() {
    }

    public synchronized boolean enableRange(int startId, int endId, String client) {
        int len = this.mRanges.size();
        if (len == 0) {
            if (tryAddRanges(startId, endId, true)) {
                this.mRanges.add(new IntRange(startId, endId, client));
                return true;
            }
            return false;
        }
        for (int startIndex = 0; startIndex < len; startIndex++) {
            IntRange range = this.mRanges.get(startIndex);
            if (startId >= range.mStartId && endId <= range.mEndId) {
                range.insert(new ClientRange(startId, endId, client));
                return true;
            }
            if (startId - 1 == range.mEndId) {
                int newRangeEndId = endId;
                IntRange nextRange = null;
                if (startIndex + 1 < len) {
                    IntRange nextRange2 = this.mRanges.get(startIndex + 1);
                    nextRange = nextRange2;
                    if (nextRange.mStartId - 1 <= endId) {
                        if (endId <= nextRange.mEndId) {
                            newRangeEndId = nextRange.mStartId - 1;
                        }
                    } else {
                        nextRange = null;
                    }
                }
                if (tryAddRanges(startId, newRangeEndId, true)) {
                    range.mEndId = endId;
                    range.insert(new ClientRange(startId, endId, client));
                    if (nextRange != null) {
                        if (range.mEndId < nextRange.mEndId) {
                            range.mEndId = nextRange.mEndId;
                        }
                        range.mClients.addAll(nextRange.mClients);
                        this.mRanges.remove(nextRange);
                    }
                    return true;
                }
                return false;
            }
            if (startId < range.mStartId) {
                if (endId + 1 < range.mStartId) {
                    if (tryAddRanges(startId, endId, true)) {
                        this.mRanges.add(startIndex, new IntRange(startId, endId, client));
                        return true;
                    }
                    return false;
                }
                if (endId <= range.mEndId) {
                    if (tryAddRanges(startId, range.mStartId - 1, true)) {
                        range.mStartId = startId;
                        range.mClients.add(0, new ClientRange(startId, endId, client));
                        return true;
                    }
                    return false;
                }
                for (int endIndex = startIndex + 1; endIndex < len; endIndex++) {
                    IntRange endRange = this.mRanges.get(endIndex);
                    if (endId + 1 < endRange.mStartId) {
                        if (tryAddRanges(startId, endId, true)) {
                            range.mStartId = startId;
                            range.mEndId = endId;
                            range.mClients.add(0, new ClientRange(startId, endId, client));
                            int joinIndex = startIndex + 1;
                            for (int i = joinIndex; i < endIndex; i++) {
                                IntRange joinRange = this.mRanges.get(joinIndex);
                                range.mClients.addAll(joinRange.mClients);
                                this.mRanges.remove(joinRange);
                            }
                            return true;
                        }
                        return false;
                    }
                    if (endId <= endRange.mEndId) {
                        if (tryAddRanges(startId, endRange.mStartId - 1, true)) {
                            range.mStartId = startId;
                            range.mEndId = endRange.mEndId;
                            range.mClients.add(0, new ClientRange(startId, endId, client));
                            int joinIndex2 = startIndex + 1;
                            for (int i2 = joinIndex2; i2 <= endIndex; i2++) {
                                IntRange joinRange2 = this.mRanges.get(joinIndex2);
                                range.mClients.addAll(joinRange2.mClients);
                                this.mRanges.remove(joinRange2);
                            }
                            return true;
                        }
                        return false;
                    }
                }
                if (tryAddRanges(startId, endId, true)) {
                    range.mStartId = startId;
                    range.mEndId = endId;
                    range.mClients.add(0, new ClientRange(startId, endId, client));
                    int joinIndex3 = startIndex + 1;
                    for (int i3 = joinIndex3; i3 < len; i3++) {
                        IntRange joinRange3 = this.mRanges.get(joinIndex3);
                        range.mClients.addAll(joinRange3.mClients);
                        this.mRanges.remove(joinRange3);
                    }
                    return true;
                }
                return false;
            }
            if (startId + 1 <= range.mEndId) {
                if (endId <= range.mEndId) {
                    range.insert(new ClientRange(startId, endId, client));
                    return true;
                }
                int endIndex2 = startIndex;
                for (int testIndex = startIndex + 1; testIndex < len; testIndex++) {
                    IntRange testRange = this.mRanges.get(testIndex);
                    if (endId + 1 < testRange.mStartId) {
                        break;
                    }
                    endIndex2 = testIndex;
                }
                if (endIndex2 == startIndex) {
                    if (tryAddRanges(range.mEndId + 1, endId, true)) {
                        range.mEndId = endId;
                        range.insert(new ClientRange(startId, endId, client));
                        return true;
                    }
                    return false;
                }
                IntRange endRange2 = this.mRanges.get(endIndex2);
                if (tryAddRanges(range.mEndId + 1, endId <= endRange2.mEndId ? endRange2.mStartId - 1 : endId, true)) {
                    range.mEndId = endId <= endRange2.mEndId ? endRange2.mEndId : endId;
                    range.insert(new ClientRange(startId, endId, client));
                    int joinIndex4 = startIndex + 1;
                    for (int i4 = joinIndex4; i4 <= endIndex2; i4++) {
                        IntRange joinRange4 = this.mRanges.get(joinIndex4);
                        range.mClients.addAll(joinRange4.mClients);
                        this.mRanges.remove(joinRange4);
                    }
                    return true;
                }
                return false;
            }
        }
        if (tryAddRanges(startId, endId, true)) {
            this.mRanges.add(new IntRange(startId, endId, client));
            return true;
        }
        return false;
    }

    public synchronized boolean disableRange(int startId, int endId, String client) {
        int len = this.mRanges.size();
        for (int i = 0; i < len; i++) {
            IntRange range = this.mRanges.get(i);
            if (startId < range.mStartId) {
                return false;
            }
            if (endId <= range.mEndId) {
                ArrayList<ClientRange> clients = range.mClients;
                int crLength = clients.size();
                if (crLength == 1) {
                    ClientRange cr = clients.get(0);
                    if (cr.mStartId == startId && cr.mEndId == endId && cr.mClient.equals(client)) {
                        this.mRanges.remove(i);
                        if (updateRanges()) {
                            return true;
                        }
                        this.mRanges.add(i, range);
                        return false;
                    }
                    return false;
                }
                int largestEndId = Integer.MIN_VALUE;
                boolean updateStarted = false;
                for (int crIndex = 0; crIndex < crLength; crIndex++) {
                    ClientRange cr2 = clients.get(crIndex);
                    if (cr2.mStartId == startId && cr2.mEndId == endId && cr2.mClient.equals(client)) {
                        if (crIndex == crLength - 1) {
                            if (range.mEndId == largestEndId) {
                                clients.remove(crIndex);
                                return true;
                            }
                            clients.remove(crIndex);
                            range.mEndId = largestEndId;
                            if (updateRanges()) {
                                return true;
                            }
                            clients.add(crIndex, cr2);
                            range.mEndId = cr2.mEndId;
                            return false;
                        }
                        IntRange rangeCopy = new IntRange(range, crIndex);
                        if (crIndex == 0) {
                            int nextStartId = clients.get(1).mStartId;
                            if (nextStartId != range.mStartId) {
                                updateStarted = true;
                                rangeCopy.mStartId = nextStartId;
                            }
                            largestEndId = clients.get(1).mEndId;
                        }
                        ArrayList<IntRange> newRanges = new ArrayList<>();
                        IntRange currentRange = rangeCopy;
                        for (int nextIndex = crIndex + 1; nextIndex < crLength; nextIndex++) {
                            ClientRange nextCr = clients.get(nextIndex);
                            if (nextCr.mStartId > largestEndId + 1) {
                                updateStarted = true;
                                currentRange.mEndId = largestEndId;
                                newRanges.add(currentRange);
                                currentRange = new IntRange(nextCr);
                            } else {
                                if (currentRange.mEndId < nextCr.mEndId) {
                                    currentRange.mEndId = nextCr.mEndId;
                                }
                                currentRange.mClients.add(nextCr);
                            }
                            if (nextCr.mEndId > largestEndId) {
                                largestEndId = nextCr.mEndId;
                            }
                        }
                        if (largestEndId < endId) {
                            updateStarted = true;
                            currentRange.mEndId = largestEndId;
                        }
                        newRanges.add(currentRange);
                        this.mRanges.remove(i);
                        this.mRanges.addAll(i, newRanges);
                        if (updateStarted && !updateRanges()) {
                            this.mRanges.removeAll(newRanges);
                            this.mRanges.add(i, range);
                            return false;
                        }
                        return true;
                    }
                    if (cr2.mEndId > largestEndId) {
                        largestEndId = cr2.mEndId;
                    }
                }
            }
        }
        return false;
    }

    public boolean updateRanges() {
        startUpdate();
        populateAllRanges();
        return finishUpdate();
    }

    protected boolean tryAddRanges(int startId, int endId, boolean selected) {
        startUpdate();
        populateAllRanges();
        addRange(startId, endId, selected);
        return finishUpdate();
    }

    public boolean isEmpty() {
        return this.mRanges.isEmpty();
    }

    private void populateAllRanges() {
        for (IntRange currRange : this.mRanges) {
            addRange(currRange.mStartId, currRange.mEndId, true);
        }
    }

    private void populateAllClientRanges() {
        int len = this.mRanges.size();
        for (int i = 0; i < len; i++) {
            IntRange range = this.mRanges.get(i);
            int clientLen = range.mClients.size();
            for (int j = 0; j < clientLen; j++) {
                ClientRange nextRange = range.mClients.get(j);
                addRange(nextRange.mStartId, nextRange.mEndId, true);
            }
        }
    }

    public void clearAllRanges() {
        Rlog.d("IntRangeManager", "clear all the ranges");
        this.mRanges.clear();
    }
}
