package com.android.launcher3.model;

import android.content.Context;
import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import java.text.Collator;
import java.util.Comparator;

public class AppNameComparator {
    private final AbstractUserComparator<ItemInfo> mAppInfoComparator;
    private final Collator mCollator = Collator.getInstance();
    private final Comparator<String> mSectionNameComparator = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return AppNameComparator.this.compareTitles(o1, o2);
        }
    };

    public AppNameComparator(Context context) {
        this.mAppInfoComparator = new AbstractUserComparator<ItemInfo>(context) {
            @Override
            public final int compare(ItemInfo a, ItemInfo b) {
                int result = AppNameComparator.this.compareTitles(a.title.toString(), b.title.toString());
                if (result == 0 && (a instanceof AppInfo) && (b instanceof AppInfo)) {
                    AppInfo aAppInfo = (AppInfo) a;
                    AppInfo bAppInfo = (AppInfo) b;
                    result = aAppInfo.componentName.compareTo(bAppInfo.componentName);
                    if (result == 0) {
                        return super.compare(a, b);
                    }
                }
                return result;
            }
        };
    }

    public Comparator<ItemInfo> getAppInfoComparator() {
        return this.mAppInfoComparator;
    }

    public Comparator<String> getSectionNameComparator() {
        return this.mSectionNameComparator;
    }

    int compareTitles(String titleA, String titleB) {
        boolean zIsLetterOrDigit;
        boolean zIsLetterOrDigit2;
        if (titleA.length() <= 0) {
            zIsLetterOrDigit = false;
        } else {
            zIsLetterOrDigit = Character.isLetterOrDigit(titleA.codePointAt(0));
        }
        if (titleB.length() <= 0) {
            zIsLetterOrDigit2 = false;
        } else {
            zIsLetterOrDigit2 = Character.isLetterOrDigit(titleB.codePointAt(0));
        }
        if (zIsLetterOrDigit && !zIsLetterOrDigit2) {
            return -1;
        }
        if (!zIsLetterOrDigit && zIsLetterOrDigit2) {
            return 1;
        }
        return this.mCollator.compare(titleA, titleB);
    }
}
