package android.content;

import android.net.Uri;
import com.android.internal.telephony.PhoneConstants;
import java.util.ArrayList;
import java.util.List;

public class UriMatcher {
    private static final int EXACT = 0;
    public static final int NO_MATCH = -1;
    private static final int NUMBER = 1;
    private static final int TEXT = 2;
    private ArrayList<UriMatcher> mChildren;
    private int mCode;
    private String mText;
    private int mWhich;

    public UriMatcher(int i) {
        this.mCode = i;
        this.mWhich = -1;
        this.mChildren = new ArrayList<>();
        this.mText = null;
    }

    private UriMatcher() {
        this.mCode = -1;
        this.mWhich = -1;
        this.mChildren = new ArrayList<>();
        this.mText = null;
    }

    public void addURI(String str, String str2, int i) {
        String str3;
        if (i < 0) {
            throw new IllegalArgumentException("code " + i + " is invalid: it must be positive");
        }
        String[] strArrSplit = null;
        if (str2 != null) {
            if (str2.length() > 1 && str2.charAt(0) == '/') {
                str2 = str2.substring(1);
            }
            strArrSplit = str2.split("/");
        }
        int length = strArrSplit != null ? strArrSplit.length : 0;
        UriMatcher uriMatcher = this;
        for (int i2 = -1; i2 < length; i2++) {
            if (i2 >= 0) {
                str3 = strArrSplit[i2];
            } else {
                str3 = str;
            }
            ArrayList<UriMatcher> arrayList = uriMatcher.mChildren;
            int size = arrayList.size();
            int i3 = 0;
            while (true) {
                if (i3 >= size) {
                    break;
                }
                UriMatcher uriMatcher2 = arrayList.get(i3);
                if (!str3.equals(uriMatcher2.mText)) {
                    i3++;
                } else {
                    uriMatcher = uriMatcher2;
                    break;
                }
            }
            if (i3 == size) {
                UriMatcher uriMatcher3 = new UriMatcher();
                if (str3.equals("#")) {
                    uriMatcher3.mWhich = 1;
                } else if (str3.equals(PhoneConstants.APN_TYPE_ALL)) {
                    uriMatcher3.mWhich = 2;
                } else {
                    uriMatcher3.mWhich = 0;
                }
                uriMatcher3.mText = str3;
                uriMatcher.mChildren.add(uriMatcher3);
                uriMatcher = uriMatcher3;
            }
        }
        uriMatcher.mCode = i;
    }

    public int match(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        int size = pathSegments.size();
        if (size == 0 && uri.getAuthority() == null) {
            return this.mCode;
        }
        UriMatcher uriMatcher = this;
        int i = -1;
        while (i < size) {
            String authority = i < 0 ? uri.getAuthority() : pathSegments.get(i);
            ArrayList<UriMatcher> arrayList = uriMatcher.mChildren;
            if (arrayList != null) {
                int size2 = arrayList.size();
                UriMatcher uriMatcher2 = null;
                for (int i2 = 0; i2 < size2; i2++) {
                    UriMatcher uriMatcher3 = arrayList.get(i2);
                    switch (uriMatcher3.mWhich) {
                        case 0:
                            if (uriMatcher3.mText.equals(authority)) {
                                uriMatcher2 = uriMatcher3;
                            }
                            break;
                        case 1:
                            int length = authority.length();
                            for (int i3 = 0; i3 < length; i3++) {
                                char cCharAt = authority.charAt(i3);
                                if (cCharAt < '0' || cCharAt > '9') {
                                }
                                break;
                            }
                            uriMatcher2 = uriMatcher3;
                            break;
                    }
                    if (uriMatcher2 != null) {
                        uriMatcher = uriMatcher2;
                        if (uriMatcher != null) {
                            return -1;
                        }
                        i++;
                    }
                }
                uriMatcher = uriMatcher2;
                if (uriMatcher != null) {
                }
            } else {
                return uriMatcher.mCode;
            }
        }
        return uriMatcher.mCode;
    }
}
