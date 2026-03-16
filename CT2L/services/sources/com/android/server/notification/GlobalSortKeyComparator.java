package com.android.server.notification;

import java.util.Comparator;

public class GlobalSortKeyComparator implements Comparator<NotificationRecord> {
    @Override
    public int compare(NotificationRecord left, NotificationRecord right) {
        if (left.getGlobalSortKey() == null) {
            throw new IllegalStateException("Missing left global sort key: " + left);
        }
        if (right.getGlobalSortKey() == null) {
            throw new IllegalStateException("Missing right global sort key: " + right);
        }
        return left.getGlobalSortKey().compareTo(right.getGlobalSortKey());
    }
}
