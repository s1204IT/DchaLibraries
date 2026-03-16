package com.android.providers.contacts.aggregation.util;

import android.util.Log;
import com.android.providers.contacts.util.Hex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ContactMatcher {
    private static int[] sMinScore = new int[25];
    private static int[] sMaxScore = new int[25];
    private final HashMap<Long, MatchScore> mScores = new HashMap<>();
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

    private static void setScoreRange(int candidateNameType, int nameType, int scoreFrom, int scoreTo) {
        int index = (nameType * 5) + candidateNameType;
        sMinScore[index] = scoreFrom;
        sMaxScore[index] = scoreTo;
    }

    private static int getMinScore(int candidateNameType, int nameType) {
        int index = (nameType * 5) + candidateNameType;
        return sMinScore[index];
    }

    private static int getMaxScore(int candidateNameType, int nameType) {
        int index = (nameType * 5) + candidateNameType;
        return sMaxScore[index];
    }

    public static class MatchScore implements Comparable<MatchScore> {
        private long mContactId;
        private boolean mKeepIn;
        private boolean mKeepOut;
        private int mMatchCount;
        private int mPrimaryScore;
        private int mSecondaryScore;

        public MatchScore(long contactId) {
            this.mContactId = contactId;
        }

        public void reset(long contactId) {
            this.mContactId = contactId;
            this.mKeepIn = false;
            this.mKeepOut = false;
            this.mPrimaryScore = 0;
            this.mSecondaryScore = 0;
            this.mMatchCount = 0;
        }

        public long getContactId() {
            return this.mContactId;
        }

        public void updatePrimaryScore(int score) {
            if (score > this.mPrimaryScore) {
                this.mPrimaryScore = score;
            }
            this.mMatchCount++;
        }

        public void updateSecondaryScore(int score) {
            if (score > this.mSecondaryScore) {
                this.mSecondaryScore = score;
            }
            this.mMatchCount++;
        }

        public void keepIn() {
            this.mKeepIn = true;
        }

        public void keepOut() {
            this.mKeepOut = true;
        }

        public int getScore() {
            if (this.mKeepOut) {
                return 0;
            }
            if (this.mKeepIn) {
                return 100;
            }
            int score = this.mPrimaryScore > this.mSecondaryScore ? this.mPrimaryScore : this.mSecondaryScore;
            return (score * 1000) + this.mMatchCount;
        }

        @Override
        public int compareTo(MatchScore another) {
            return another.getScore() - getScore();
        }

        public String toString() {
            return this.mContactId + ": " + this.mPrimaryScore + "/" + this.mSecondaryScore + "(" + this.mMatchCount + ")";
        }
    }

    private MatchScore getMatchingScore(long contactId) {
        MatchScore matchingScore = this.mScores.get(Long.valueOf(contactId));
        if (matchingScore == null) {
            if (this.mScoreList.size() > this.mScoreCount) {
                matchingScore = this.mScoreList.get(this.mScoreCount);
                matchingScore.reset(contactId);
            } else {
                matchingScore = new MatchScore(contactId);
                this.mScoreList.add(matchingScore);
            }
            this.mScoreCount++;
            this.mScores.put(Long.valueOf(contactId), matchingScore);
        }
        return matchingScore;
    }

    public void matchIdentity(long contactId) {
        updatePrimaryScore(contactId, 100);
    }

    public void matchName(long contactId, int candidateNameType, String candidateName, int nameType, String name, int algorithm) {
        int minScore;
        int score;
        int maxScore = getMaxScore(candidateNameType, nameType);
        if (maxScore != 0) {
            if (candidateName.equals(name)) {
                updatePrimaryScore(contactId, maxScore);
                return;
            }
            if (algorithm != 0 && (minScore = getMinScore(candidateNameType, nameType)) != maxScore) {
                try {
                    byte[] decodedCandidateName = Hex.decodeHex(candidateName);
                    byte[] decodedName = Hex.decodeHex(name);
                    NameDistance nameDistance = algorithm == 1 ? this.mNameDistanceConservative : this.mNameDistanceApproximate;
                    float distance = nameDistance.getDistance(decodedCandidateName, decodedName);
                    boolean emailBased = candidateNameType == 4 || nameType == 4;
                    float threshold = emailBased ? 0.95f : 0.82f;
                    if (distance > threshold) {
                        score = (int) (minScore + ((maxScore - minScore) * (1.0f - distance)));
                    } else {
                        score = 0;
                    }
                    updatePrimaryScore(contactId, score);
                } catch (RuntimeException e) {
                    Log.e("ContactMatcher", "Failed to decode normalized name.  Skipping.", e);
                }
            }
        }
    }

    public void updateScoreWithPhoneNumberMatch(long contactId) {
        updateSecondaryScore(contactId, 71);
    }

    public void updateScoreWithEmailMatch(long contactId) {
        updateSecondaryScore(contactId, 71);
    }

    public void updateScoreWithNicknameMatch(long contactId) {
        updateSecondaryScore(contactId, 71);
    }

    private void updatePrimaryScore(long contactId, int score) {
        getMatchingScore(contactId).updatePrimaryScore(score);
    }

    private void updateSecondaryScore(long contactId, int score) {
        getMatchingScore(contactId).updateSecondaryScore(score);
    }

    public void keepIn(long contactId) {
        getMatchingScore(contactId).keepIn();
    }

    public void keepOut(long contactId) {
        getMatchingScore(contactId).keepOut();
    }

    public void clear() {
        this.mScores.clear();
        this.mScoreCount = 0;
    }

    public List<Long> prepareSecondaryMatchCandidates(int threshold) {
        ArrayList<Long> contactIds = null;
        for (int i = 0; i < this.mScoreCount; i++) {
            MatchScore score = this.mScoreList.get(i);
            if (!score.mKeepOut) {
                int s = score.mSecondaryScore;
                if (s >= threshold) {
                    if (contactIds == null) {
                        contactIds = new ArrayList<>();
                    }
                    contactIds.add(Long.valueOf(score.mContactId));
                }
                score.mPrimaryScore = -1;
            }
        }
        return contactIds;
    }

    public long pickBestMatch(int threshold, boolean allowMultipleMatches) {
        long contactId = -1;
        int maxScore = 0;
        for (int i = 0; i < this.mScoreCount; i++) {
            MatchScore score = this.mScoreList.get(i);
            if (!score.mKeepOut) {
                if (score.mKeepIn) {
                    long contactId2 = score.mContactId;
                    return contactId2;
                }
                int s = score.mPrimaryScore;
                if (s == -1) {
                    s = score.mSecondaryScore;
                }
                if (s < threshold) {
                    continue;
                } else {
                    if (contactId != -1 && !allowMultipleMatches) {
                        return -2L;
                    }
                    if (s > maxScore || (s == maxScore && contactId > score.mContactId)) {
                        contactId = score.mContactId;
                        maxScore = s;
                    }
                }
            }
        }
        return contactId;
    }

    public List<MatchScore> pickBestMatches(int threshold) {
        int scaledThreshold = threshold * 1000;
        List<MatchScore> matches = this.mScoreList.subList(0, this.mScoreCount);
        Collections.sort(matches);
        int count = 0;
        for (int i = 0; i < this.mScoreCount; i++) {
            MatchScore matchScore = matches.get(i);
            if (matchScore.getScore() < scaledThreshold) {
                break;
            }
            count++;
        }
        return matches.subList(0, count);
    }

    public String toString() {
        return this.mScoreList.subList(0, this.mScoreCount).toString();
    }
}
