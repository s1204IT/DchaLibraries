package com.android.providers.contacts;

import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionContext {
    private HashSet<Long> mChangedRawContacts;
    private HashSet<Long> mDirtyRawContacts;
    private final boolean mForProfile;
    private HashMap<Long, Long> mInsertedRawContactsAccounts;
    private HashSet<Long> mStaleSearchIndexContacts;
    private HashSet<Long> mStaleSearchIndexRawContacts;
    private HashSet<Long> mUpdatedRawContacts;
    private HashMap<Long, Object> mUpdatedSyncStates;

    public TransactionContext(boolean forProfile) {
        this.mForProfile = forProfile;
    }

    public void rawContactInserted(long rawContactId, long accountId) {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = Maps.newHashMap();
        }
        this.mInsertedRawContactsAccounts.put(Long.valueOf(rawContactId), Long.valueOf(accountId));
        markRawContactChangedOrDeletedOrInserted(rawContactId);
    }

    public void rawContactUpdated(long rawContactId) {
        if (this.mUpdatedRawContacts == null) {
            this.mUpdatedRawContacts = Sets.newHashSet();
        }
        this.mUpdatedRawContacts.add(Long.valueOf(rawContactId));
    }

    public void markRawContactDirtyAndChanged(long rawContactId, boolean isSyncAdapter) {
        if (!isSyncAdapter) {
            if (this.mDirtyRawContacts == null) {
                this.mDirtyRawContacts = Sets.newHashSet();
            }
            this.mDirtyRawContacts.add(Long.valueOf(rawContactId));
        }
        markRawContactChangedOrDeletedOrInserted(rawContactId);
    }

    public void markRawContactChangedOrDeletedOrInserted(long rawContactId) {
        if (this.mChangedRawContacts == null) {
            this.mChangedRawContacts = Sets.newHashSet();
        }
        this.mChangedRawContacts.add(Long.valueOf(rawContactId));
    }

    public void syncStateUpdated(long rowId, Object data) {
        if (this.mUpdatedSyncStates == null) {
            this.mUpdatedSyncStates = Maps.newHashMap();
        }
        this.mUpdatedSyncStates.put(Long.valueOf(rowId), data);
    }

    public void invalidateSearchIndexForRawContact(long rawContactId) {
        if (this.mStaleSearchIndexRawContacts == null) {
            this.mStaleSearchIndexRawContacts = Sets.newHashSet();
        }
        this.mStaleSearchIndexRawContacts.add(Long.valueOf(rawContactId));
    }

    public void invalidateSearchIndexForContact(long contactId) {
        if (this.mStaleSearchIndexContacts == null) {
            this.mStaleSearchIndexContacts = Sets.newHashSet();
        }
        this.mStaleSearchIndexContacts.add(Long.valueOf(contactId));
    }

    public Set<Long> getInsertedRawContactIds() {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = Maps.newHashMap();
        }
        return this.mInsertedRawContactsAccounts.keySet();
    }

    public Set<Long> getUpdatedRawContactIds() {
        if (this.mUpdatedRawContacts == null) {
            this.mUpdatedRawContacts = Sets.newHashSet();
        }
        return this.mUpdatedRawContacts;
    }

    public Set<Long> getDirtyRawContactIds() {
        if (this.mDirtyRawContacts == null) {
            this.mDirtyRawContacts = Sets.newHashSet();
        }
        return this.mDirtyRawContacts;
    }

    public Set<Long> getChangedRawContactIds() {
        if (this.mChangedRawContacts == null) {
            this.mChangedRawContacts = Sets.newHashSet();
        }
        return this.mChangedRawContacts;
    }

    public Set<Long> getStaleSearchIndexRawContactIds() {
        if (this.mStaleSearchIndexRawContacts == null) {
            this.mStaleSearchIndexRawContacts = Sets.newHashSet();
        }
        return this.mStaleSearchIndexRawContacts;
    }

    public Set<Long> getStaleSearchIndexContactIds() {
        if (this.mStaleSearchIndexContacts == null) {
            this.mStaleSearchIndexContacts = Sets.newHashSet();
        }
        return this.mStaleSearchIndexContacts;
    }

    public Set<Map.Entry<Long, Object>> getUpdatedSyncStates() {
        if (this.mUpdatedSyncStates == null) {
            this.mUpdatedSyncStates = Maps.newHashMap();
        }
        return this.mUpdatedSyncStates.entrySet();
    }

    public Long getAccountIdOrNullForRawContact(long rawContactId) {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = Maps.newHashMap();
        }
        return this.mInsertedRawContactsAccounts.get(Long.valueOf(rawContactId));
    }

    public boolean isNewRawContact(long rawContactId) {
        if (this.mInsertedRawContactsAccounts == null) {
            this.mInsertedRawContactsAccounts = Maps.newHashMap();
        }
        return this.mInsertedRawContactsAccounts.containsKey(Long.valueOf(rawContactId));
    }

    public void clearExceptSearchIndexUpdates() {
        this.mInsertedRawContactsAccounts = null;
        this.mUpdatedRawContacts = null;
        this.mUpdatedSyncStates = null;
        this.mDirtyRawContacts = null;
        this.mChangedRawContacts = null;
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
