package android.content;

import android.net.Uri;
import com.android.internal.telephony.PhoneConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UriMatcher {
    private static final int EXACT = 0;
    public static final int NO_MATCH = -1;
    private static final int NUMBER = 1;
    static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");
    private static final int TEXT = 2;
    private ArrayList<UriMatcher> mChildren;
    private int mCode;
    private String mText;
    private int mWhich;

    public UriMatcher(int code) {
        this.mCode = code;
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

    public void addURI(String authority, String path, int code) {
        if (code < 0) {
            throw new IllegalArgumentException("code " + code + " is invalid: it must be positive");
        }
        String[] tokens = null;
        if (path != null) {
            String newPath = path;
            if (path.length() > 0 && path.charAt(0) == '/') {
                newPath = path.substring(1);
            }
            tokens = PATH_SPLIT_PATTERN.split(newPath);
        }
        int numTokens = tokens != null ? tokens.length : 0;
        UriMatcher node = this;
        int i = -1;
        while (i < numTokens) {
            String token = i < 0 ? authority : tokens[i];
            ArrayList<UriMatcher> children = node.mChildren;
            int numChildren = children.size();
            int j = 0;
            while (true) {
                if (j >= numChildren) {
                    break;
                }
                UriMatcher child = children.get(j);
                if (!token.equals(child.mText)) {
                    j++;
                } else {
                    node = child;
                    break;
                }
            }
            if (j == numChildren) {
                UriMatcher child2 = new UriMatcher();
                if (token.equals("#")) {
                    child2.mWhich = 1;
                } else if (token.equals(PhoneConstants.APN_TYPE_ALL)) {
                    child2.mWhich = 2;
                } else {
                    child2.mWhich = 0;
                }
                child2.mText = token;
                node.mChildren.add(child2);
                node = child2;
            }
            i++;
        }
        node.mCode = code;
    }

    public int match(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        int li = pathSegments.size();
        UriMatcher node = this;
        if (li == 0 && uri.getAuthority() == null) {
            return this.mCode;
        }
        int i = -1;
        while (i < li) {
            String u = i < 0 ? uri.getAuthority() : pathSegments.get(i);
            ArrayList<UriMatcher> list = node.mChildren;
            if (list != null) {
                node = null;
                int lj = list.size();
                for (int j = 0; j < lj; j++) {
                    UriMatcher n = list.get(j);
                    switch (n.mWhich) {
                        case 0:
                            if (n.mText.equals(u)) {
                                node = n;
                            }
                            break;
                        case 1:
                            int lk = u.length();
                            int k = 0;
                            while (true) {
                                if (k < lk) {
                                    char c = u.charAt(k);
                                    if (c >= '0' && c <= '9') {
                                        k++;
                                    }
                                } else {
                                    node = n;
                                }
                                break;
                            }
                            break;
                        case 2:
                            node = n;
                            break;
                    }
                    if (node != null) {
                        if (node == null) {
                            i++;
                        } else {
                            return -1;
                        }
                    }
                }
                if (node == null) {
                }
            } else {
                return node.mCode;
            }
        }
        return node.mCode;
    }
}
