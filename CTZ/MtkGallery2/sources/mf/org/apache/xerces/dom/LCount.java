package mf.org.apache.xerces.dom;

import java.util.Hashtable;

class LCount {
    static Hashtable lCounts = new Hashtable();
    public int defaults;
    public int captures = 0;
    public int bubbles = 0;
    public int total = 0;

    LCount() {
    }

    static LCount lookup(String evtName) {
        LCount lc = (LCount) lCounts.get(evtName);
        if (lc == null) {
            Hashtable hashtable = lCounts;
            LCount lc2 = new LCount();
            hashtable.put(evtName, lc2);
            return lc2;
        }
        return lc;
    }
}
