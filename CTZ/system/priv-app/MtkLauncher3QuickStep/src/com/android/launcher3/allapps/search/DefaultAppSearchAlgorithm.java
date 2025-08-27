package com.android.launcher3.allapps.search;

import android.os.Handler;
import com.android.launcher3.AppInfo;
import com.android.launcher3.allapps.search.AllAppsSearchBarController;
import com.android.launcher3.util.ComponentKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultAppSearchAlgorithm implements SearchAlgorithm {
    private final List<AppInfo> mApps;
    protected final Handler mResultHandler = new Handler();

    public DefaultAppSearchAlgorithm(List<AppInfo> list) {
        this.mApps = list;
    }

    @Override // com.android.launcher3.allapps.search.SearchAlgorithm
    public void cancel(boolean z) {
        if (z) {
            this.mResultHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override // com.android.launcher3.allapps.search.SearchAlgorithm
    public void doSearch(final String str, final AllAppsSearchBarController.Callbacks callbacks) {
        final ArrayList<ComponentKey> titleMatchResult = getTitleMatchResult(str);
        this.mResultHandler.post(new Runnable() { // from class: com.android.launcher3.allapps.search.DefaultAppSearchAlgorithm.1
            @Override // java.lang.Runnable
            public void run() {
                callbacks.onSearchResult(str, titleMatchResult);
            }
        });
    }

    private ArrayList<ComponentKey> getTitleMatchResult(String str) {
        String lowerCase = str.toLowerCase();
        ArrayList<ComponentKey> arrayList = new ArrayList<>();
        StringMatcher stringMatcher = StringMatcher.getInstance();
        for (AppInfo appInfo : this.mApps) {
            if (matches(appInfo, lowerCase, stringMatcher)) {
                arrayList.add(appInfo.toComponentKey());
            }
        }
        return arrayList;
    }

    public static boolean matches(AppInfo appInfo, String str, StringMatcher stringMatcher) {
        int type;
        int length = str.length();
        String string = appInfo.title.toString();
        int length2 = string.length();
        if (length2 < length || length <= 0) {
            return false;
        }
        int i = length2 - length;
        int i2 = 0;
        int type2 = Character.getType(string.codePointAt(0));
        int i3 = 0;
        while (i3 <= i) {
            if (i3 < length2 - 1) {
                type = Character.getType(string.codePointAt(i3 + 1));
            } else {
                type = 0;
            }
            if (!isBreak(type2, i2, type) || !stringMatcher.matches(str, string.substring(i3, i3 + length))) {
                i3++;
                i2 = type2;
                type2 = type;
            } else {
                return true;
            }
        }
        return false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:31:0x0033  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static boolean isBreak(int i, int i2, int i3) {
        if (i2 != 0) {
            switch (i2) {
                case 12:
                case 13:
                case 14:
                    break;
                default:
                    if (i != 20) {
                        switch (i) {
                            case 1:
                                if (i3 == 1) {
                                }
                                if (i2 != 1) {
                                    break;
                                }
                                break;
                            case 2:
                                if (i2 <= 5 && i2 > 0) {
                                    break;
                                }
                                break;
                            case 3:
                                if (i2 != 1) {
                                }
                                break;
                            default:
                                switch (i) {
                                    case 9:
                                    case 10:
                                    case 11:
                                        if (i2 == 9 || i2 == 10 || i2 == 11) {
                                            break;
                                        }
                                        break;
                                    default:
                                        switch (i) {
                                        }
                                }
                        }
                    }
                    break;
            }
            return false;
        }
        return true;
    }

    public static class StringMatcher {
        private static final char MAX_UNICODE = 65535;
        private final Collator mCollator = Collator.getInstance();

        StringMatcher() {
            this.mCollator.setStrength(0);
            this.mCollator.setDecomposition(1);
        }

        public boolean matches(String str, String str2) {
            switch (this.mCollator.compare(str, str2)) {
                case -1:
                    Collator collator = this.mCollator;
                    StringBuilder sb = new StringBuilder();
                    sb.append(str);
                    sb.append(MAX_UNICODE);
                    return collator.compare(sb.toString(), str2) > -1;
                case 0:
                    return true;
                default:
                    return false;
            }
        }

        public static StringMatcher getInstance() {
            return new StringMatcher();
        }
    }
}
