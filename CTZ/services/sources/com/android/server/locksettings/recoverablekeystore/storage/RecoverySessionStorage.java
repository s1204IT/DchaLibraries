package com.android.server.locksettings.recoverablekeystore.storage;

import android.util.SparseArray;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;
import javax.security.auth.Destroyable;

public class RecoverySessionStorage implements Destroyable {
    private final SparseArray<ArrayList<Entry>> mSessionsByUid = new SparseArray<>();

    public Entry get(int i, String str) {
        ArrayList<Entry> arrayList = this.mSessionsByUid.get(i);
        if (arrayList == null) {
            return null;
        }
        for (Entry entry : arrayList) {
            if (str.equals(entry.mSessionId)) {
                return entry;
            }
        }
        return null;
    }

    public void add(int i, Entry entry) {
        if (this.mSessionsByUid.get(i) == null) {
            this.mSessionsByUid.put(i, new ArrayList<>());
        }
        this.mSessionsByUid.get(i).add(entry);
    }

    public void remove(int i, final String str) {
        if (this.mSessionsByUid.get(i) == null) {
            return;
        }
        this.mSessionsByUid.get(i).removeIf(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((RecoverySessionStorage.Entry) obj).mSessionId.equals(str);
            }
        });
    }

    public void remove(int i) {
        ArrayList<Entry> arrayList = this.mSessionsByUid.get(i);
        if (arrayList == null) {
            return;
        }
        Iterator<Entry> it = arrayList.iterator();
        while (it.hasNext()) {
            it.next().destroy();
        }
        this.mSessionsByUid.remove(i);
    }

    public int size() {
        int size = this.mSessionsByUid.size();
        int size2 = 0;
        for (int i = 0; i < size; i++) {
            size2 += this.mSessionsByUid.valueAt(i).size();
        }
        return size2;
    }

    @Override
    public void destroy() {
        int size = this.mSessionsByUid.size();
        for (int i = 0; i < size; i++) {
            Iterator<Entry> it = this.mSessionsByUid.valueAt(i).iterator();
            while (it.hasNext()) {
                it.next().destroy();
            }
        }
    }

    public static class Entry implements Destroyable {
        private final byte[] mKeyClaimant;
        private final byte[] mLskfHash;
        private final String mSessionId;
        private final byte[] mVaultParams;

        public Entry(String str, byte[] bArr, byte[] bArr2, byte[] bArr3) {
            this.mLskfHash = bArr;
            this.mSessionId = str;
            this.mKeyClaimant = bArr2;
            this.mVaultParams = bArr3;
        }

        public byte[] getLskfHash() {
            return this.mLskfHash;
        }

        public byte[] getKeyClaimant() {
            return this.mKeyClaimant;
        }

        public byte[] getVaultParams() {
            return this.mVaultParams;
        }

        @Override
        public void destroy() {
            Arrays.fill(this.mLskfHash, (byte) 0);
            Arrays.fill(this.mKeyClaimant, (byte) 0);
        }
    }
}
