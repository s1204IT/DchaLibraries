package com.google.common.collect;

import java.lang.reflect.Array;

class Platform {
    static <T> T[] clone(T[] tArr) {
        return (T[]) ((Object[]) tArr.clone());
    }

    static <T> T[] newArray(T[] tArr, int i) {
        return (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i));
    }

    static MapMaker tryWeakKeys(MapMaker mapMaker) {
        return mapMaker.weakKeys();
    }
}
