package com.google.common.collect;

import java.lang.reflect.Array;

final class Platform {
    static <T> T[] newArray(T[] tArr, int i) {
        return (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i));
    }

    static MapMaker tryWeakKeys(MapMaker mapMaker) {
        return mapMaker.weakKeys();
    }
}
