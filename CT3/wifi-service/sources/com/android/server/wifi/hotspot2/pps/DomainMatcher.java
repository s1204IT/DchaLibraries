package com.android.server.wifi.hotspot2.pps;

import com.android.server.wifi.hotspot2.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DomainMatcher {
    private static final String[] TestDomains = {"garbage.apple.com", "apple.com", "com", "jan.android.google.com.", "jan.android.google.com", "android.google.com", "google.com", "jan.android.google.net.", "jan.android.google.net", "android.google.net", "google.net", "net.", "."};
    private final Label mRoot = new Label(Match.None, null);

    public enum Match {
        None,
        Primary,
        Secondary;

        public static Match[] valuesCustom() {
            return values();
        }
    }

    private static class Label {
        private final Match mMatch;
        private final Map<String, Label> mSubDomains;

        Label(Match match, Label label) {
            this(match);
        }

        private Label(Match match) {
            this.mMatch = match;
            this.mSubDomains = match == Match.None ? new HashMap() : null;
        }

        private void addDomain(Iterator<String> labels, Match match) {
            String labelName = labels.next();
            if (labels.hasNext()) {
                Label subLabel = new Label(Match.None);
                this.mSubDomains.put(labelName, subLabel);
                subLabel.addDomain(labels, match);
                return;
            }
            this.mSubDomains.put(labelName, new Label(match));
        }

        private Label getSubLabel(String labelString) {
            return this.mSubDomains.get(labelString);
        }

        public Match getMatch() {
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
            sb.append('=').append(this.mMatch);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }
    }

    public DomainMatcher(List<String> primary, List<List<String>> secondary) {
        for (List<String> secondaryLabel : secondary) {
            this.mRoot.addDomain(secondaryLabel.iterator(), Match.Secondary);
        }
        this.mRoot.addDomain(primary.iterator(), Match.Primary);
    }

    public Match isSubDomain(List<String> domain) {
        Label label = this.mRoot;
        for (String labelString : domain) {
            label = label.getSubLabel(labelString);
            if (label == null) {
                return Match.None;
            }
            if (label.getMatch() != Match.None) {
                return label.getMatch();
            }
        }
        return Match.None;
    }

    public static boolean arg2SubdomainOfArg1(List<String> arg1, List<String> arg2) {
        if (arg2.size() < arg1.size()) {
            return false;
        }
        Iterator<String> l1 = arg1.iterator();
        Iterator<String> l2 = arg2.iterator();
        while (l1.hasNext()) {
            if (!l1.next().equals(l2.next())) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "Domain matcher " + this.mRoot;
    }

    public static void main(String[] args) {
        DomainMatcher dm1 = new DomainMatcher(Utils.splitDomain("android.google.com"), Collections.emptyList());
        for (String domain : TestDomains) {
            System.out.println(domain + ": " + dm1.isSubDomain(Utils.splitDomain(domain)));
        }
        List<List<String>> secondaries = new ArrayList<>();
        secondaries.add(Utils.splitDomain("apple.com"));
        secondaries.add(Utils.splitDomain("net"));
        DomainMatcher dm2 = new DomainMatcher(Utils.splitDomain("android.google.com"), secondaries);
        for (String domain2 : TestDomains) {
            System.out.println(domain2 + ": " + dm2.isSubDomain(Utils.splitDomain(domain2)));
        }
    }
}
