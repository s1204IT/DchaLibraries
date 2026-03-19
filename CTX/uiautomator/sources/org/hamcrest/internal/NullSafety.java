package org.hamcrest.internal;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNull;

public class NullSafety {
    public static <E> List<Matcher<? super E>> nullSafe(Matcher<? super E>[] matcherArr) {
        ArrayList arrayList = new ArrayList(matcherArr.length);
        for (Matcher<? super E> matcherNullValue : matcherArr) {
            if (matcherNullValue == null) {
                matcherNullValue = IsNull.nullValue();
            }
            arrayList.add(matcherNullValue);
        }
        return arrayList;
    }
}
