package com.android.providers.contacts.aggregation.util;

import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RawContactMatchingCandidates {
    private List<MatchScore> mBestMatches;
    private Set<Long> mRawContactIds;
    private Map<Long, Long> mRawContactToAccount;
    private Map<Long, Long> mRawContactToContact;

    public RawContactMatchingCandidates(List<MatchScore> list) {
        this.mRawContactIds = null;
        this.mRawContactToContact = null;
        this.mRawContactToAccount = null;
        Preconditions.checkNotNull(list);
        this.mBestMatches = list;
    }

    public RawContactMatchingCandidates() {
        this.mRawContactIds = null;
        this.mRawContactToContact = null;
        this.mRawContactToAccount = null;
        this.mBestMatches = new ArrayList();
    }

    public int getCount() {
        return this.mBestMatches.size();
    }

    public void add(MatchScore matchScore) {
        this.mBestMatches.add(matchScore);
        if (this.mRawContactIds != null) {
            this.mRawContactIds.add(Long.valueOf(matchScore.getRawContactId()));
        }
        if (this.mRawContactToAccount != null) {
            this.mRawContactToAccount.put(Long.valueOf(matchScore.getRawContactId()), Long.valueOf(matchScore.getAccountId()));
        }
        if (this.mRawContactToContact != null) {
            this.mRawContactToContact.put(Long.valueOf(matchScore.getRawContactId()), Long.valueOf(matchScore.getContactId()));
        }
    }

    public Set<Long> getRawContactIdSet() {
        if (this.mRawContactIds == null) {
            createRawContactIdSet();
        }
        return this.mRawContactIds;
    }

    public Map<Long, Long> getRawContactToAccount() {
        if (this.mRawContactToAccount == null) {
            createRawContactToAccountMap();
        }
        return this.mRawContactToAccount;
    }

    public Long getContactId(Long l) {
        if (this.mRawContactToContact == null) {
            createRawContactToContactMap();
        }
        return this.mRawContactToContact.get(l);
    }

    private void createRawContactToContactMap() {
        this.mRawContactToContact = new ArrayMap();
        for (int i = 0; i < this.mBestMatches.size(); i++) {
            this.mRawContactToContact.put(Long.valueOf(this.mBestMatches.get(i).getRawContactId()), Long.valueOf(this.mBestMatches.get(i).getContactId()));
        }
    }

    private void createRawContactToAccountMap() {
        this.mRawContactToAccount = new ArrayMap();
        for (int i = 0; i < this.mBestMatches.size(); i++) {
            this.mRawContactToAccount.put(Long.valueOf(this.mBestMatches.get(i).getRawContactId()), Long.valueOf(this.mBestMatches.get(i).getAccountId()));
        }
    }

    private void createRawContactIdSet() {
        this.mRawContactIds = new ArraySet();
        for (int i = 0; i < this.mBestMatches.size(); i++) {
            this.mRawContactIds.add(Long.valueOf(this.mBestMatches.get(i).getRawContactId()));
        }
    }
}
