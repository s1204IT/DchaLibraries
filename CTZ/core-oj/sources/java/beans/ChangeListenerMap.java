package java.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract class ChangeListenerMap<L extends EventListener> {
    private Map<String, L[]> map;

    public abstract L extract(L l);

    protected abstract L[] newArray(int i);

    protected abstract L newProxy(String str, L l);

    ChangeListenerMap() {
    }

    public final synchronized void add(String str, L l) {
        int length;
        if (this.map == null) {
            this.map = new HashMap();
        }
        L[] lArr = this.map.get(str);
        if (lArr != null) {
            length = lArr.length;
        } else {
            length = 0;
        }
        EventListener[] eventListenerArrNewArray = newArray(length + 1);
        eventListenerArrNewArray[length] = l;
        if (lArr != null) {
            System.arraycopy(lArr, 0, eventListenerArrNewArray, 0, length);
        }
        this.map.put(str, eventListenerArrNewArray);
    }

    public final synchronized void remove(String str, L l) {
        L[] lArr;
        if (this.map != null && (lArr = this.map.get(str)) != null) {
            int i = 0;
            while (true) {
                if (i >= lArr.length) {
                    break;
                }
                if (!l.equals(lArr[i])) {
                    i++;
                } else {
                    int length = lArr.length - 1;
                    if (length > 0) {
                        EventListener[] eventListenerArrNewArray = newArray(length);
                        System.arraycopy(lArr, 0, eventListenerArrNewArray, 0, i);
                        System.arraycopy(lArr, i + 1, eventListenerArrNewArray, i, length - i);
                        this.map.put(str, eventListenerArrNewArray);
                    } else {
                        this.map.remove(str);
                        if (this.map.isEmpty()) {
                            this.map = null;
                        }
                    }
                }
            }
        }
    }

    public final synchronized L[] get(String str) {
        L[] lArr;
        if (this.map != null) {
            lArr = this.map.get(str);
        } else {
            lArr = null;
        }
        return lArr;
    }

    public final void set(String str, L[] lArr) {
        if (lArr != null) {
            if (this.map == null) {
                this.map = new HashMap();
            }
            this.map.put(str, lArr);
        } else if (this.map != null) {
            this.map.remove(str);
            if (this.map.isEmpty()) {
                this.map = null;
            }
        }
    }

    public final synchronized L[] getListeners() {
        if (this.map == null) {
            return (L[]) newArray(0);
        }
        ArrayList arrayList = new ArrayList();
        L[] lArr = this.map.get(null);
        if (lArr != null) {
            for (L l : lArr) {
                arrayList.add(l);
            }
        }
        for (Map.Entry<String, L[]> entry : this.map.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                for (L l2 : entry.getValue()) {
                    arrayList.add(newProxy(key, l2));
                }
            }
        }
        return (L[]) ((EventListener[]) arrayList.toArray(newArray(arrayList.size())));
    }

    public final L[] getListeners(String str) {
        EventListener[] eventListenerArr;
        if (str != null && (eventListenerArr = get(str)) != null) {
            return (L[]) ((EventListener[]) eventListenerArr.clone());
        }
        return (L[]) newArray(0);
    }

    public final synchronized boolean hasListeners(String str) {
        boolean z = false;
        if (this.map == null) {
            return false;
        }
        if (this.map.get(null) != null) {
            z = true;
        } else if (str != null) {
            if (this.map.get(str) != null) {
            }
        }
        return z;
    }

    public final Set<Map.Entry<String, L[]>> getEntries() {
        if (this.map != null) {
            return this.map.entrySet();
        }
        return Collections.emptySet();
    }
}
