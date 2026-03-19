package com.android.providers.contacts;

import android.util.ArrayMap;
import android.util.ArraySet;
import java.util.Map;
import java.util.Set;

public class TransactionContext {
    private ArraySet<Long> mBackupIdChangedRawContacts;
    private ArraySet<Long> mChangedRawContacts;
    private ArraySet<Long> mDirtyRawContacts;
    private final boolean mForProfile;
    private ArrayMap<Long, Long> mInsertedRawContactsAccounts;
    private ArraySet<Long> mMetadataDirtyRawContacts;
    private ArraySet<Long> mStaleSearchIndexContacts;
    private ArraySet<Long> mStaleSearchIndexRawContacts;
    private ArraySet<Long> mUpdatedRawContacts;
    private ArrayMap<Long, Object> mUpdatedSyncStates;

    public TransactionContext(boolean z) {
        this.mForProfile = z;
    }

    public void rawContactInserted(long j, long j2) {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = new ArrayMap<>();
        }
        this.mInsertedRawContactsAccounts.put(Long.valueOf(j), Long.valueOf(j2));
        markRawContactChangedOrDeletedOrInserted(j);
    }

    public void rawContactUpdated(long j) {
        if (this.mUpdatedRawContacts == null) {
            this.mUpdatedRawContacts = new ArraySet<>();
        }
        this.mUpdatedRawContacts.add(Long.valueOf(j));
    }

    public void markRawContactDirtyAndChanged(long j, boolean z) {
        if (!z) {
            if (this.mDirtyRawContacts == null) {
                this.mDirtyRawContacts = new ArraySet<>();
            }
            this.mDirtyRawContacts.add(Long.valueOf(j));
        }
        markRawContactChangedOrDeletedOrInserted(j);
    }

    public void markRawContactMetadataDirty(long j, boolean z) {
        if (!z) {
            if (this.mMetadataDirtyRawContacts == null) {
                this.mMetadataDirtyRawContacts = new ArraySet<>();
            }
            this.mMetadataDirtyRawContacts.add(Long.valueOf(j));
        }
    }

    public void markBackupIdChangedRawContact(long j) {
        if (this.mBackupIdChangedRawContacts == null) {
            this.mBackupIdChangedRawContacts = new ArraySet<>();
        }
        this.mBackupIdChangedRawContacts.add(Long.valueOf(j));
    }

    public void markRawContactChangedOrDeletedOrInserted(long j) {
        if (this.mChangedRawContacts == null) {
            this.mChangedRawContacts = new ArraySet<>();
        }
        this.mChangedRawContacts.add(Long.valueOf(j));
    }

    public void syncStateUpdated(long j, Object obj) {
        if (this.mUpdatedSyncStates == null) {
            this.mUpdatedSyncStates = new ArrayMap<>();
        }
        this.mUpdatedSyncStates.put(Long.valueOf(j), obj);
    }

    public void invalidateSearchIndexForRawContact(long j) {
        if (this.mStaleSearchIndexRawContacts == null) {
            this.mStaleSearchIndexRawContacts = new ArraySet<>();
        }
        this.mStaleSearchIndexRawContacts.add(Long.valueOf(j));
    }

    public void invalidateSearchIndexForContact(long j) {
        if (this.mStaleSearchIndexContacts == null) {
            this.mStaleSearchIndexContacts = new ArraySet<>();
        }
        this.mStaleSearchIndexContacts.add(Long.valueOf(j));
    }

    public Set<Long> getInsertedRawContactIds() {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = new ArrayMap<>();
        }
        return this.mInsertedRawContactsAccounts.keySet();
    }

    public Set<Long> getUpdatedRawContactIds() {
        if (this.mUpdatedRawContacts == null) {
            this.mUpdatedRawContacts = new ArraySet<>();
        }
        return this.mUpdatedRawContacts;
    }

    public Set<Long> getDirtyRawContactIds() {
        if (this.mDirtyRawContacts == null) {
            this.mDirtyRawContacts = new ArraySet<>();
        }
        return this.mDirtyRawContacts;
    }

    public Set<Long> getMetadataDirtyRawContactIds() {
        if (this.mMetadataDirtyRawContacts == null) {
            this.mMetadataDirtyRawContacts = new ArraySet<>();
        }
        return this.mMetadataDirtyRawContacts;
    }

    public Set<Long> getBackupIdChangedRawContacts() {
        if (this.mBackupIdChangedRawContacts == null) {
            this.mBackupIdChangedRawContacts = new ArraySet<>();
        }
        return this.mBackupIdChangedRawContacts;
    }

    public Set<Long> getChangedRawContactIds() {
        if (this.mChangedRawContacts == null) {
            this.mChangedRawContacts = new ArraySet<>();
        }
        return this.mChangedRawContacts;
    }

    public Set<Long> getStaleSearchIndexRawContactIds() {
        if (this.mStaleSearchIndexRawContacts == null) {
            this.mStaleSearchIndexRawContacts = new ArraySet<>();
        }
        return this.mStaleSearchIndexRawContacts;
    }

    public Set<Long> getStaleSearchIndexContactIds() {
        if (this.mStaleSearchIndexContacts == null) {
            this.mStaleSearchIndexContacts = new ArraySet<>();
        }
        return this.mStaleSearchIndexContacts;
    }

    public Set<Map.Entry<Long, Object>> getUpdatedSyncStates() {
        if (this.mUpdatedSyncStates == null) {
            this.mUpdatedSyncStates = new ArrayMap<>();
        }
        return this.mUpdatedSyncStates.entrySet();
    }

    public Long getAccountIdOrNullForRawContact(long j) {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = new ArrayMap<>();
        }
        return this.mInsertedRawContactsAccounts.get(Long.valueOf(j));
    }

    public boolean isNewRawContact(long j) {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = new ArrayMap<>();
        }
        return this.mInsertedRawContactsAccounts.containsKey(Long.valueOf(j));
    }

    public void clearExceptSearchIndexUpdates() {
        this.mInsertedRawContactsAccounts = null;
        this.mUpdatedRawContacts = null;
        this.mUpdatedSyncStates = null;
        this.mDirtyRawContacts = null;
        this.mMetadataDirtyRawContacts = null;
        this.mChangedRawContacts = null;
        this.mBackupIdChangedRawContacts = null;
    }

    public void clearSearchIndexUpdates() {
        this.mStaleSearchIndexRawContacts = null;
        this.mStaleSearchIndexContacts = null;
    }

    public void clearAll() {
        clearExceptSearchIndexUpdates();
        clearSearchIndexUpdates();
    }
}
