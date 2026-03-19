package android.icu.impl;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class SortedSetRelation {
    public static final int A = 6;
    public static final int ADDALL = 7;
    public static final int ANY = 7;
    public static final int A_AND_B = 2;
    public static final int A_NOT_B = 4;
    public static final int B = 3;
    public static final int B_NOT_A = 1;
    public static final int B_REMOVEALL = 1;
    public static final int COMPLEMENTALL = 5;
    public static final int CONTAINS = 6;
    public static final int DISJOINT = 5;
    public static final int EQUALS = 2;
    public static final int ISCONTAINED = 3;
    public static final int NONE = 0;
    public static final int NO_A = 1;
    public static final int NO_B = 4;
    public static final int REMOVEALL = 4;
    public static final int RETAINALL = 2;

    public static <T extends Comparable<? super T>> boolean hasRelation(SortedSet<T> a, int allow, SortedSet<T> b) {
        if (allow < 0 || allow > 7) {
            throw new IllegalArgumentException("Relation " + allow + " out of range");
        }
        boolean anb = (allow & 4) != 0;
        boolean ab = (allow & 2) != 0;
        boolean bna = (allow & 1) != 0;
        switch (allow) {
            case 2:
                if (a.size() != b.size()) {
                    return false;
                }
                break;
            case 3:
                if (a.size() > b.size()) {
                    return false;
                }
                break;
            case 6:
                if (a.size() < b.size()) {
                    return false;
                }
                break;
        }
        if (a.size() == 0) {
            if (b.size() == 0) {
                return true;
            }
            return bna;
        }
        if (b.size() == 0) {
            return anb;
        }
        Iterator<T> it = a.iterator();
        Iterator<T> it2 = b.iterator();
        T aa = it.next();
        T bb = it2.next();
        while (true) {
            int comp = aa.compareTo(bb);
            if (comp == 0) {
                if (!ab) {
                    return false;
                }
                if (!it.hasNext()) {
                    if (it2.hasNext()) {
                        return bna;
                    }
                    return true;
                }
                if (!it2.hasNext()) {
                    return anb;
                }
                aa = it.next();
                bb = it2.next();
            } else if (comp < 0) {
                if (!anb) {
                    return false;
                }
                if (!it.hasNext()) {
                    return bna;
                }
                aa = it.next();
            } else {
                if (!bna) {
                    return false;
                }
                if (!it2.hasNext()) {
                    return anb;
                }
                bb = it2.next();
            }
        }
    }

    public static <T extends Comparable<? super T>> SortedSet<? extends T> doOperation(SortedSet<T> sortedSet, int relation, SortedSet<T> b) {
        switch (relation) {
            case 0:
                sortedSet.clear();
                return sortedSet;
            case 1:
                TreeSet<? extends T> temp = new TreeSet<>((SortedSet<? extends T>) b);
                temp.removeAll(sortedSet);
                sortedSet.clear();
                sortedSet.addAll(temp);
                return sortedSet;
            case 2:
                sortedSet.retainAll(b);
                return sortedSet;
            case 3:
                sortedSet.clear();
                sortedSet.addAll(b);
                return sortedSet;
            case 4:
                sortedSet.removeAll(b);
                return sortedSet;
            case 5:
                TreeSet<? extends T> temp2 = new TreeSet<>((SortedSet<? extends T>) b);
                temp2.removeAll(sortedSet);
                sortedSet.removeAll(b);
                sortedSet.addAll(temp2);
                return sortedSet;
            case 6:
                return sortedSet;
            case 7:
                sortedSet.addAll(b);
                return sortedSet;
            default:
                throw new IllegalArgumentException("Relation " + relation + " out of range");
        }
    }
}
