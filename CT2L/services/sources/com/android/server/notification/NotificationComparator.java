package com.android.server.notification;

import java.util.Comparator;

public class NotificationComparator implements Comparator<NotificationRecord> {
    @Override
    public int compare(NotificationRecord left, NotificationRecord right) {
        int leftPackagePriority = left.getPackagePriority();
        int rightPackagePriority = right.getPackagePriority();
        if (leftPackagePriority != rightPackagePriority) {
            return Integer.compare(leftPackagePriority, rightPackagePriority) * (-1);
        }
        int leftScore = left.sbn.getScore();
        int rightScore = right.sbn.getScore();
        if (leftScore != rightScore) {
            return Integer.compare(leftScore, rightScore) * (-1);
        }
        float leftPeople = left.getContactAffinity();
        float rightPeople = right.getContactAffinity();
        if (leftPeople != rightPeople) {
            return Float.compare(leftPeople, rightPeople) * (-1);
        }
        return Long.compare(left.getRankingTimeMs(), right.getRankingTimeMs()) * (-1);
    }
}
