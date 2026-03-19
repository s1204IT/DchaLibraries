package mf.org.apache.xerces.impl.xs.traversers;

import java.util.Hashtable;

class LargeContainer extends Container {
    Hashtable items;

    LargeContainer(int size) {
        this.items = new Hashtable((size * 2) + 1);
        this.values = new OneAttr[size];
    }

    @Override
    void put(String key, OneAttr value) {
        this.items.put(key, value);
        OneAttr[] oneAttrArr = this.values;
        int i = this.pos;
        this.pos = i + 1;
        oneAttrArr[i] = value;
    }

    @Override
    OneAttr get(String key) {
        OneAttr ret = (OneAttr) this.items.get(key);
        return ret;
    }
}
