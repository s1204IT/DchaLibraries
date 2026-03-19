package android.net.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;

public class InterfaceSet {
    public final Set<String> ifnames;

    public InterfaceSet(String... strArr) {
        HashSet hashSet = new HashSet();
        for (String str : strArr) {
            if (str != null) {
                hashSet.add(str);
            }
        }
        this.ifnames = Collections.unmodifiableSet(hashSet);
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
        Iterator<String> it = this.ifnames.iterator();
        while (it.hasNext()) {
            stringJoiner.add(it.next());
        }
        return stringJoiner.toString();
    }

    public boolean equals(Object obj) {
        return obj != 0 && (obj instanceof InterfaceSet) && this.ifnames.equals(obj.ifnames);
    }
}
