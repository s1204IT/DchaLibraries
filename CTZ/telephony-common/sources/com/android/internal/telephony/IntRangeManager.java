package com.android.internal.telephony;

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

        IntRange(int i, int i2, String str) {
            this.mStartId = i;
            this.mEndId = i2;
            this.mClients = new ArrayList<>(4);
            this.mClients.add(IntRangeManager.this.new ClientRange(i, i2, str));
        }

        IntRange(ClientRange clientRange) {
            this.mStartId = clientRange.mStartId;
            this.mEndId = clientRange.mEndId;
            this.mClients = new ArrayList<>(4);
            this.mClients.add(clientRange);
        }

        IntRange(IntRange intRange, int i) {
            this.mStartId = intRange.mStartId;
            this.mEndId = intRange.mEndId;
            this.mClients = new ArrayList<>(intRange.mClients.size());
            for (int i2 = 0; i2 < i; i2++) {
                this.mClients.add(intRange.mClients.get(i2));
            }
        }

        void insert(ClientRange clientRange) {
            int size = this.mClients.size();
            int i = -1;
            for (int i2 = 0; i2 < size; i2++) {
                ClientRange clientRange2 = this.mClients.get(i2);
                if (clientRange.mStartId <= clientRange2.mStartId) {
                    if (!clientRange.equals(clientRange2)) {
                        if (clientRange.mStartId == clientRange2.mStartId && clientRange.mEndId > clientRange2.mEndId) {
                            i = i2 + 1;
                            if (i >= size) {
                                break;
                            }
                        } else {
                            this.mClients.add(i2, clientRange);
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
            if (i != -1 && i < size) {
                this.mClients.add(i, clientRange);
            } else {
                this.mClients.add(clientRange);
            }
        }
    }

    private class ClientRange {
        final String mClient;
        final int mEndId;
        final int mStartId;

        ClientRange(int i, int i2, String str) {
            this.mStartId = i;
            this.mEndId = i2;
            this.mClient = str;
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof ClientRange)) {
                return false;
            }
            ClientRange clientRange = (ClientRange) obj;
            return this.mStartId == clientRange.mStartId && this.mEndId == clientRange.mEndId && this.mClient.equals(clientRange.mClient);
        }

        public int hashCode() {
            return (((this.mStartId * 31) + this.mEndId) * 31) + this.mClient.hashCode();
        }
    }

    protected IntRangeManager() {
    }

    public synchronized boolean enableRange(int i, int i2, String str) {
        int i3;
        IntRange intRange;
        int size = this.mRanges.size();
        if (size == 0) {
            if (!tryAddRanges(i, i2, true)) {
                return false;
            }
            this.mRanges.add(new IntRange(i, i2, str));
            return true;
        }
        for (int i4 = 0; i4 < size; i4++) {
            IntRange intRange2 = this.mRanges.get(i4);
            if (i >= intRange2.mStartId && i2 <= intRange2.mEndId) {
                intRange2.insert(new ClientRange(i, i2, str));
                return true;
            }
            if (i - 1 == intRange2.mEndId) {
                int i5 = i4 + 1;
                if (i5 < size) {
                    intRange = this.mRanges.get(i5);
                    if (intRange.mStartId - 1 <= i2) {
                        i3 = i2 <= intRange.mEndId ? intRange.mStartId - 1 : i2;
                    } else {
                        i3 = i2;
                        intRange = null;
                    }
                }
                if (!tryAddRanges(i, i3, true)) {
                    return false;
                }
                intRange2.mEndId = i2;
                intRange2.insert(new ClientRange(i, i2, str));
                if (intRange != null) {
                    if (intRange2.mEndId < intRange.mEndId) {
                        intRange2.mEndId = intRange.mEndId;
                    }
                    intRange2.mClients.addAll(intRange.mClients);
                    this.mRanges.remove(intRange);
                }
                return true;
            }
            if (i < intRange2.mStartId) {
                int i6 = i2 + 1;
                if (i6 < intRange2.mStartId) {
                    if (!tryAddRanges(i, i2, true)) {
                        return false;
                    }
                    this.mRanges.add(i4, new IntRange(i, i2, str));
                    return true;
                }
                if (i2 <= intRange2.mEndId) {
                    if (!tryAddRanges(i, intRange2.mStartId - 1, true)) {
                        return false;
                    }
                    intRange2.mStartId = i;
                    intRange2.mClients.add(0, new ClientRange(i, i2, str));
                    return true;
                }
                int i7 = i4 + 1;
                for (int i8 = i7; i8 < size; i8++) {
                    IntRange intRange3 = this.mRanges.get(i8);
                    if (i6 < intRange3.mStartId) {
                        if (!tryAddRanges(i, i2, true)) {
                            return false;
                        }
                        intRange2.mStartId = i;
                        intRange2.mEndId = i2;
                        intRange2.mClients.add(0, new ClientRange(i, i2, str));
                        for (int i9 = i7; i9 < i8; i9++) {
                            IntRange intRange4 = this.mRanges.get(i7);
                            intRange2.mClients.addAll(intRange4.mClients);
                            this.mRanges.remove(intRange4);
                        }
                        return true;
                    }
                    if (i2 <= intRange3.mEndId) {
                        if (!tryAddRanges(i, intRange3.mStartId - 1, true)) {
                            return false;
                        }
                        intRange2.mStartId = i;
                        intRange2.mEndId = intRange3.mEndId;
                        intRange2.mClients.add(0, new ClientRange(i, i2, str));
                        for (int i10 = i7; i10 <= i8; i10++) {
                            IntRange intRange5 = this.mRanges.get(i7);
                            intRange2.mClients.addAll(intRange5.mClients);
                            this.mRanges.remove(intRange5);
                        }
                        return true;
                    }
                }
                if (!tryAddRanges(i, i2, true)) {
                    return false;
                }
                intRange2.mStartId = i;
                intRange2.mEndId = i2;
                intRange2.mClients.add(0, new ClientRange(i, i2, str));
                for (int i11 = i7; i11 < size; i11++) {
                    IntRange intRange6 = this.mRanges.get(i7);
                    intRange2.mClients.addAll(intRange6.mClients);
                    this.mRanges.remove(intRange6);
                }
                return true;
            }
            if (i + 1 <= intRange2.mEndId) {
                if (i2 <= intRange2.mEndId) {
                    intRange2.insert(new ClientRange(i, i2, str));
                    return true;
                }
                int i12 = i4 + 1;
                int i13 = i4;
                for (int i14 = i12; i14 < size && i2 + 1 >= this.mRanges.get(i14).mStartId; i14++) {
                    i13 = i14;
                }
                if (i13 == i4) {
                    if (!tryAddRanges(intRange2.mEndId + 1, i2, true)) {
                        return false;
                    }
                    intRange2.mEndId = i2;
                    intRange2.insert(new ClientRange(i, i2, str));
                    return true;
                }
                IntRange intRange7 = this.mRanges.get(i13);
                if (!tryAddRanges(intRange2.mEndId + 1, i2 <= intRange7.mEndId ? intRange7.mStartId - 1 : i2, true)) {
                    return false;
                }
                intRange2.mEndId = i2 <= intRange7.mEndId ? intRange7.mEndId : i2;
                intRange2.insert(new ClientRange(i, i2, str));
                for (int i15 = i12; i15 <= i13; i15++) {
                    IntRange intRange8 = this.mRanges.get(i12);
                    intRange2.mClients.addAll(intRange8.mClients);
                    this.mRanges.remove(intRange8);
                }
                return true;
            }
        }
        if (!tryAddRanges(i, i2, true)) {
            return false;
        }
        this.mRanges.add(new IntRange(i, i2, str));
        return true;
    }

    public synchronized boolean disableRange(int i, int i2, String str) {
        boolean z;
        int size = this.mRanges.size();
        for (int i3 = 0; i3 < size; i3++) {
            IntRange intRange = this.mRanges.get(i3);
            if (i < intRange.mStartId) {
                return false;
            }
            if (i2 <= intRange.mEndId) {
                ArrayList<ClientRange> arrayList = intRange.mClients;
                int size2 = arrayList.size();
                if (size2 == 1) {
                    ClientRange clientRange = arrayList.get(0);
                    if (clientRange.mStartId != i || clientRange.mEndId != i2 || !clientRange.mClient.equals(str)) {
                        return false;
                    }
                    this.mRanges.remove(i3);
                    if (updateRanges()) {
                        return true;
                    }
                    this.mRanges.add(i3, intRange);
                    return false;
                }
                int i4 = Integer.MIN_VALUE;
                for (int i5 = 0; i5 < size2; i5++) {
                    ClientRange clientRange2 = arrayList.get(i5);
                    if (clientRange2.mStartId == i && clientRange2.mEndId == i2 && clientRange2.mClient.equals(str)) {
                        if (i5 == size2 - 1) {
                            if (intRange.mEndId == i4) {
                                arrayList.remove(i5);
                                return true;
                            }
                            arrayList.remove(i5);
                            intRange.mEndId = i4;
                            if (updateRanges()) {
                                return true;
                            }
                            arrayList.add(i5, clientRange2);
                            intRange.mEndId = clientRange2.mEndId;
                            return false;
                        }
                        IntRange intRange2 = new IntRange(intRange, i5);
                        if (i5 == 0) {
                            int i6 = arrayList.get(1).mStartId;
                            if (i6 == intRange.mStartId) {
                                z = false;
                            } else {
                                intRange2.mStartId = i6;
                                z = true;
                            }
                            i4 = arrayList.get(1).mEndId;
                        } else {
                            z = false;
                        }
                        ArrayList arrayList2 = new ArrayList();
                        for (int i7 = i5 + 1; i7 < size2; i7++) {
                            ClientRange clientRange3 = arrayList.get(i7);
                            if (clientRange3.mStartId > i4 + 1) {
                                intRange2.mEndId = i4;
                                arrayList2.add(intRange2);
                                intRange2 = new IntRange(clientRange3);
                                z = true;
                            } else {
                                if (intRange2.mEndId < clientRange3.mEndId) {
                                    intRange2.mEndId = clientRange3.mEndId;
                                }
                                intRange2.mClients.add(clientRange3);
                            }
                            if (clientRange3.mEndId > i4) {
                                i4 = clientRange3.mEndId;
                            }
                        }
                        if (i4 < i2) {
                            intRange2.mEndId = i4;
                            z = true;
                        }
                        arrayList2.add(intRange2);
                        this.mRanges.remove(i3);
                        this.mRanges.addAll(i3, arrayList2);
                        if (!z || updateRanges()) {
                            return true;
                        }
                        this.mRanges.removeAll(arrayList2);
                        this.mRanges.add(i3, intRange);
                        return false;
                    }
                    if (clientRange2.mEndId > i4) {
                        i4 = clientRange2.mEndId;
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

    protected boolean tryAddRanges(int i, int i2, boolean z) {
        startUpdate();
        populateAllRanges();
        addRange(i, i2, z);
        return finishUpdate();
    }

    public boolean isEmpty() {
        return this.mRanges.isEmpty();
    }

    private void populateAllRanges() {
        for (IntRange intRange : this.mRanges) {
            addRange(intRange.mStartId, intRange.mEndId, true);
        }
    }

    private void populateAllClientRanges() {
        int size = this.mRanges.size();
        for (int i = 0; i < size; i++) {
            IntRange intRange = this.mRanges.get(i);
            int size2 = intRange.mClients.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ClientRange clientRange = intRange.mClients.get(i2);
                addRange(clientRange.mStartId, clientRange.mEndId, true);
            }
        }
    }
}
