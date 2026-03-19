package com.android.providers.contacts.aggregation.util;

import android.util.ArrayMap;
import android.util.Log;
import com.android.providers.contacts.util.Hex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContactMatcher {
    private static int[] sMinScore = new int[25];
    private static int[] sMaxScore = new int[25];
    private final ArrayMap<Long, MatchScore> mScores = new ArrayMap<>();
    private final ArrayList<MatchScore> mScoreList = new ArrayList<>();
    private int mScoreCount = 0;
    private final NameDistance mNameDistanceConservative = new NameDistance();
    private final NameDistance mNameDistanceApproximate = new NameDistance(30);

    static {
        setScoreRange(0, 0, 99, 99);
        setScoreRange(1, 1, 90, 90);
        setScoreRange(2, 2, 50, 80);
        setScoreRange(2, 4, 30, 60);
        setScoreRange(2, 3, 50, 60);
        setScoreRange(4, 4, 50, 60);
        setScoreRange(4, 2, 50, 60);
        setScoreRange(4, 3, 50, 60);
        setScoreRange(3, 3, 50, 60);
        setScoreRange(3, 2, 50, 60);
        setScoreRange(3, 4, 50, 60);
    }

    private static void setScoreRange(int i, int i2, int i3, int i4) {
        int i5 = (i2 * 5) + i;
        sMinScore[i5] = i3;
        sMaxScore[i5] = i4;
    }

    private static int getMinScore(int i, int i2) {
        return sMinScore[(i2 * 5) + i];
    }

    private static int getMaxScore(int i, int i2) {
        return sMaxScore[(i2 * 5) + i];
    }

    private MatchScore getMatchingScore(long j) {
        MatchScore matchScore = this.mScores.get(Long.valueOf(j));
        if (matchScore == null) {
            if (this.mScoreList.size() > this.mScoreCount) {
                matchScore = this.mScoreList.get(this.mScoreCount);
                matchScore.reset(j);
            } else {
                matchScore = new MatchScore(j);
                this.mScoreList.add(matchScore);
            }
            this.mScoreCount++;
            this.mScores.put(Long.valueOf(j), matchScore);
        }
        return matchScore;
    }

    public void matchIdentity(long j) {
        updatePrimaryScore(j, 100);
    }

    public void matchName(long j, int i, String str, int i2, String str2, int i3) {
        int minScore;
        float f;
        int maxScore = getMaxScore(i, i2);
        if (maxScore == 0) {
            return;
        }
        if (str.equals(str2)) {
            updatePrimaryScore(j, maxScore);
            return;
        }
        if (i3 == 0 || (minScore = getMinScore(i, i2)) == maxScore) {
            return;
        }
        try {
            boolean z = true;
            float distance = (i3 == 1 ? this.mNameDistanceConservative : this.mNameDistanceApproximate).getDistance(Hex.decodeHex(str), Hex.decodeHex(str2));
            int i4 = 0;
            if (i != 4 && i2 != 4) {
                z = false;
            }
            if (z) {
                f = 0.95f;
            } else {
                f = 0.82f;
            }
            if (distance > f) {
                i4 = (int) (minScore + ((maxScore - minScore) * (1.0f - distance)));
            }
            updatePrimaryScore(j, i4);
        } catch (RuntimeException e) {
            Log.e("ContactMatcher", "Failed to decode normalized name.  Skipping.", e);
        }
    }

    public void updateScoreWithPhoneNumberMatch(long j) {
        updateSecondaryScore(j, 71);
    }

    public void updateScoreWithEmailMatch(long j) {
        updateSecondaryScore(j, 71);
    }

    public void updateScoreWithNicknameMatch(long j) {
        updateSecondaryScore(j, 71);
    }

    private void updatePrimaryScore(long j, int i) {
        getMatchingScore(j).updatePrimaryScore(i);
    }

    private void updateSecondaryScore(long j, int i) {
        getMatchingScore(j).updateSecondaryScore(i);
    }

    public void keepIn(long j) {
        getMatchingScore(j).keepIn();
    }

    public void keepOut(long j) {
        getMatchingScore(j).keepOut();
    }

    public void clear() {
        this.mScores.clear();
        this.mScoreCount = 0;
    }

    public List<Long> prepareSecondaryMatchCandidates(int i) {
        ArrayList arrayList = null;
        for (int i2 = 0; i2 < this.mScoreCount; i2++) {
            MatchScore matchScore = this.mScoreList.get(i2);
            if (!matchScore.isKeepOut()) {
                if (matchScore.getSecondaryScore() >= i) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(Long.valueOf(matchScore.getContactId()));
                }
                matchScore.setPrimaryScore(-1);
            }
        }
        return arrayList;
    }

    public long pickBestMatch(int i, boolean z) {
        int i2 = 0;
        long contactId = -1;
        for (int i3 = 0; i3 < this.mScoreCount; i3++) {
            MatchScore matchScore = this.mScoreList.get(i3);
            if (!matchScore.isKeepOut()) {
                if (matchScore.isKeepIn()) {
                    return matchScore.getContactId();
                }
                int primaryScore = matchScore.getPrimaryScore();
                if (primaryScore == -1) {
                    primaryScore = matchScore.getSecondaryScore();
                }
                if (primaryScore < i) {
                    continue;
                } else {
                    if (contactId != -1 && !z) {
                        return -2L;
                    }
                    if (primaryScore > i2 || (primaryScore == i2 && contactId > matchScore.getContactId())) {
                        contactId = matchScore.getContactId();
                        i2 = primaryScore;
                    }
                }
            }
        }
        return contactId;
    }

    public List<MatchScore> pickBestMatches(int i) {
        int i2 = i * 1000;
        List<MatchScore> listSubList = this.mScoreList.subList(0, this.mScoreCount);
        Collections.sort(listSubList);
        int i3 = 0;
        for (int i4 = 0; i4 < this.mScoreCount && listSubList.get(i4).getScore() >= i2; i4++) {
            i3++;
        }
        return listSubList.subList(0, i3);
    }

    public String toString() {
        return this.mScoreList.subList(0, this.mScoreCount).toString();
    }
}
