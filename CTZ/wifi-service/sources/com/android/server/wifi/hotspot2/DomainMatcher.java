package com.android.server.wifi.hotspot2;

import android.text.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DomainMatcher {
    public static final int MATCH_NONE = 0;
    public static final int MATCH_PRIMARY = 1;
    public static final int MATCH_SECONDARY = 2;
    private final Label mRoot = new Label(0);

    private static class Label {
        private int mMatch;
        private final Map<String, Label> mSubDomains = new HashMap();

        Label(int i) {
            this.mMatch = i;
        }

        public void addDomain(Iterator<String> it, int i) {
            String next = it.next();
            Label label = this.mSubDomains.get(next);
            if (label == null) {
                label = new Label(0);
                this.mSubDomains.put(next, label);
            }
            if (it.hasNext()) {
                label.addDomain(it, i);
            } else {
                label.mMatch = i;
            }
        }

        public Label getSubLabel(String str) {
            return this.mSubDomains.get(str);
        }

        public int getMatch() {
            return this.mMatch;
        }

        private void toString(StringBuilder sb) {
            if (this.mSubDomains != null) {
                sb.append(".{");
                for (Map.Entry<String, Label> entry : this.mSubDomains.entrySet()) {
                    sb.append(entry.getKey());
                    entry.getValue().toString(sb);
                }
                sb.append('}');
                return;
            }
            sb.append('=');
            sb.append(this.mMatch);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }
    }

    public DomainMatcher(String str, List<String> list) {
        if (list != null) {
            for (String str2 : list) {
                if (!TextUtils.isEmpty(str2)) {
                    this.mRoot.addDomain(Utils.splitDomain(str2).iterator(), 2);
                }
            }
        }
        if (!TextUtils.isEmpty(str)) {
            this.mRoot.addDomain(Utils.splitDomain(str).iterator(), 1);
        }
    }

    public int isSubDomain(String str) {
        int match = 0;
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        List<String> listSplitDomain = Utils.splitDomain(str);
        Label subLabel = this.mRoot;
        Iterator<String> it = listSplitDomain.iterator();
        while (it.hasNext() && (subLabel = subLabel.getSubLabel(it.next())) != null && (subLabel.getMatch() == 0 || (match = subLabel.getMatch()) != 1)) {
        }
        return match;
    }

    public static boolean arg2SubdomainOfArg1(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return false;
        }
        List<String> listSplitDomain = Utils.splitDomain(str);
        List<String> listSplitDomain2 = Utils.splitDomain(str2);
        if (listSplitDomain2.size() < listSplitDomain.size()) {
            return false;
        }
        Iterator<String> it = listSplitDomain.iterator();
        Iterator<String> it2 = listSplitDomain2.iterator();
        while (it.hasNext()) {
            if (!TextUtils.equals(it.next(), it2.next())) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "Domain matcher " + this.mRoot;
    }
}
