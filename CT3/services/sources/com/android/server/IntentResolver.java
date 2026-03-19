package com.android.server;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.FastImmutableArraySet;
import android.util.LogPrinter;
import android.util.MutableInt;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import com.android.internal.util.FastPrintWriter;
import com.android.server.content.SyncOperation;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class IntentResolver<F extends IntentFilter, R> {
    private static final boolean DEBUG = false;
    private static final String TAG = "IntentResolver";
    private static final boolean localLOGV = false;
    private static final boolean localVerificationLOGV = false;
    private static final Comparator mResolvePrioritySorter = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            int q1 = ((IntentFilter) o1).getPriority();
            int q2 = ((IntentFilter) o2).getPriority();
            if (q1 > q2) {
                return -1;
            }
            return q1 < q2 ? 1 : 0;
        }
    };
    private final ArraySet<F> mFilters = new ArraySet<>();
    private final ArrayMap<String, F[]> mTypeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mBaseTypeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mWildTypeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mSchemeToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mActionToFilter = new ArrayMap<>();
    private final ArrayMap<String, F[]> mTypedActionToFilter = new ArrayMap<>();

    protected abstract boolean isPackageForFilter(String str, F f);

    protected abstract F[] newArray(int i);

    public void addFilter(F f) {
        this.mFilters.add(f);
        int numS = register_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = register_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            register_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT == 0) {
            return;
        }
        register_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
    }

    private boolean filterEquals(IntentFilter f1, IntentFilter f2) {
        int s1 = f1.countActions();
        int s2 = f2.countActions();
        if (s1 != s2) {
            return false;
        }
        for (int i = 0; i < s1; i++) {
            if (!f2.hasAction(f1.getAction(i))) {
                return false;
            }
        }
        int s12 = f1.countCategories();
        int s22 = f2.countCategories();
        if (s12 != s22) {
            return false;
        }
        for (int i2 = 0; i2 < s12; i2++) {
            if (!f2.hasCategory(f1.getCategory(i2))) {
                return false;
            }
        }
        int s13 = f1.countDataTypes();
        int s23 = f2.countDataTypes();
        if (s13 != s23) {
            return false;
        }
        for (int i3 = 0; i3 < s13; i3++) {
            if (!f2.hasExactDataType(f1.getDataType(i3))) {
                return false;
            }
        }
        int s14 = f1.countDataSchemes();
        int s24 = f2.countDataSchemes();
        if (s14 != s24) {
            return false;
        }
        for (int i4 = 0; i4 < s14; i4++) {
            if (!f2.hasDataScheme(f1.getDataScheme(i4))) {
                return false;
            }
        }
        int s15 = f1.countDataAuthorities();
        int s25 = f2.countDataAuthorities();
        if (s15 != s25) {
            return false;
        }
        for (int i5 = 0; i5 < s15; i5++) {
            if (!f2.hasDataAuthority(f1.getDataAuthority(i5))) {
                return false;
            }
        }
        int s16 = f1.countDataPaths();
        int s26 = f2.countDataPaths();
        if (s16 != s26) {
            return false;
        }
        for (int i6 = 0; i6 < s16; i6++) {
            if (!f2.hasDataPath(f1.getDataPath(i6))) {
                return false;
            }
        }
        int s17 = f1.countDataSchemeSpecificParts();
        int s27 = f2.countDataSchemeSpecificParts();
        if (s17 != s27) {
            return false;
        }
        for (int i7 = 0; i7 < s17; i7++) {
            if (!f2.hasDataSchemeSpecificPart(f1.getDataSchemeSpecificPart(i7))) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<F> collectFilters(F[] array, IntentFilter matching) {
        F cur;
        ArrayList<F> res = null;
        if (array != null) {
            for (int i = 0; i < array.length && (cur = array[i]) != null; i++) {
                if (filterEquals(cur, matching)) {
                    if (res == null) {
                        res = new ArrayList<>();
                    }
                    res.add(cur);
                }
            }
        }
        return res;
    }

    public ArrayList<F> findFilters(IntentFilter intentFilter) {
        if (intentFilter.countDataSchemes() == 1) {
            return collectFilters(this.mSchemeToFilter.get(intentFilter.getDataScheme(0)), intentFilter);
        }
        if (intentFilter.countDataTypes() != 0 && intentFilter.countActions() == 1) {
            return collectFilters(this.mTypedActionToFilter.get(intentFilter.getAction(0)), intentFilter);
        }
        if (intentFilter.countDataTypes() == 0 && intentFilter.countDataSchemes() == 0 && intentFilter.countActions() == 1) {
            return collectFilters(this.mActionToFilter.get(intentFilter.getAction(0)), intentFilter);
        }
        ArrayList arrayList = (ArrayList<F>) null;
        for (F f : this.mFilters) {
            if (filterEquals(f, intentFilter)) {
                if (arrayList == null) {
                    arrayList = (ArrayList<F>) new ArrayList();
                }
                arrayList.add(f);
            }
        }
        return (ArrayList<F>) arrayList;
    }

    public void removeFilter(F f) {
        removeFilterInternal(f);
        this.mFilters.remove(f);
    }

    void removeFilterInternal(F f) {
        int numS = unregister_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int numT = unregister_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            unregister_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (numT == 0) {
            return;
        }
        unregister_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
    }

    boolean dumpMap(PrintWriter out, String titlePrefix, String title, String prefix, ArrayMap<String, F[]> map, String packageName, boolean printFilter, boolean collapseDuplicates) {
        String eprefix = prefix + "  ";
        String fprefix = prefix + "    ";
        ArrayMap<Object, MutableInt> found = new ArrayMap<>();
        boolean printedSomething = false;
        Printer printer = null;
        for (int mapi = 0; mapi < map.size(); mapi++) {
            F[] a = map.valueAt(mapi);
            boolean printedHeader = false;
            if (collapseDuplicates && !printFilter) {
                found.clear();
                for (F filter : a) {
                    if (filter == null) {
                        break;
                    }
                    if (packageName == null || isPackageForFilter(packageName, filter)) {
                        Object label = filterToLabel(filter);
                        int index = found.indexOfKey(label);
                        if (index < 0) {
                            found.put(label, new MutableInt(1));
                        } else {
                            found.valueAt(index).value++;
                        }
                    }
                }
                for (int i = 0; i < found.size(); i++) {
                    if (title != null) {
                        out.print(titlePrefix);
                        out.println(title);
                        title = null;
                    }
                    if (!printedHeader) {
                        out.print(eprefix);
                        out.print(map.keyAt(mapi));
                        out.println(":");
                        printedHeader = true;
                    }
                    printedSomething = true;
                    dumpFilterLabel(out, fprefix, found.keyAt(i), found.valueAt(i).value);
                }
            } else {
                for (F filter2 : a) {
                    if (filter2 != null) {
                        if (packageName == null || isPackageForFilter(packageName, filter2)) {
                            if (title != null) {
                                out.print(titlePrefix);
                                out.println(title);
                                title = null;
                            }
                            if (!printedHeader) {
                                out.print(eprefix);
                                out.print(map.keyAt(mapi));
                                out.println(":");
                                printedHeader = true;
                            }
                            printedSomething = true;
                            dumpFilter(out, fprefix, filter2);
                            if (printFilter) {
                                if (printer == null) {
                                    printer = new PrintWriterPrinter(out);
                                }
                                filter2.dump(printer, fprefix + "  ");
                            }
                        }
                    }
                }
            }
        }
        return printedSomething;
    }

    public boolean dump(PrintWriter out, String title, String prefix, String packageName, boolean printFilter, boolean collapseDuplicates) {
        String innerPrefix = prefix + "  ";
        String sepPrefix = "\n" + prefix;
        String curPrefix = title + "\n" + prefix;
        if (dumpMap(out, curPrefix, "Full MIME Types:", innerPrefix, this.mTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Base MIME Types:", innerPrefix, this.mBaseTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Wild MIME Types:", innerPrefix, this.mWildTypeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Schemes:", innerPrefix, this.mSchemeToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "Non-Data Actions:", innerPrefix, this.mActionToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        if (dumpMap(out, curPrefix, "MIME Typed Actions:", innerPrefix, this.mTypedActionToFilter, packageName, printFilter, collapseDuplicates)) {
            curPrefix = sepPrefix;
        }
        return curPrefix == sepPrefix;
    }

    private class IteratorWrapper implements Iterator<F> {
        private F mCur;
        private final Iterator<F> mI;

        IteratorWrapper(Iterator<F> it) {
            this.mI = it;
        }

        @Override
        public boolean hasNext() {
            return this.mI.hasNext();
        }

        @Override
        public F next() {
            F next = this.mI.next();
            this.mCur = next;
            return next;
        }

        @Override
        public void remove() {
            if (this.mCur != null) {
                IntentResolver.this.removeFilterInternal(this.mCur);
            }
            this.mI.remove();
        }
    }

    public Iterator<F> filterIterator() {
        return new IteratorWrapper(this.mFilters.iterator());
    }

    public Set<F> filterSet() {
        return Collections.unmodifiableSet(this.mFilters);
    }

    public List<R> queryIntentFromList(Intent intent, String resolvedType, boolean defaultOnly, ArrayList<F[]> listCut, int userId) {
        ArrayList<R> resultList = new ArrayList<>();
        boolean debug = (intent.getFlags() & 8) != 0;
        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        String scheme = intent.getScheme();
        int N = listCut.size();
        for (int i = 0; i < N; i++) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, listCut.get(i), resultList, userId);
        }
        sortResults(resultList);
        return resultList;
    }

    public List<R> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
        int slashpos;
        String scheme = intent.getScheme();
        ArrayList<R> finalList = new ArrayList<>();
        boolean debug = (intent.getFlags() & 8) != 0;
        if (debug) {
            Slog.v(TAG, "Resolving type=" + resolvedType + " scheme=" + scheme + " defaultOnly=" + defaultOnly + " userId=" + userId + " of " + intent);
        }
        F[] firstTypeCut = null;
        F[] secondTypeCut = null;
        F[] thirdTypeCut = null;
        F[] schemeCut = null;
        if (resolvedType != null && (slashpos = resolvedType.indexOf(47)) > 0) {
            String baseType = resolvedType.substring(0, slashpos);
            if (!baseType.equals("*")) {
                if (resolvedType.length() == slashpos + 2 && resolvedType.charAt(slashpos + 1) == '*') {
                    firstTypeCut = this.mBaseTypeToFilter.get(baseType);
                    if (debug) {
                        Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut));
                    }
                    secondTypeCut = this.mWildTypeToFilter.get(baseType);
                    if (debug) {
                        Slog.v(TAG, "Second type cut: " + Arrays.toString(secondTypeCut));
                    }
                } else {
                    firstTypeCut = this.mTypeToFilter.get(resolvedType);
                    if (debug) {
                        Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut));
                    }
                    secondTypeCut = this.mWildTypeToFilter.get(baseType);
                    if (debug) {
                        Slog.v(TAG, "Second type cut: " + Arrays.toString(secondTypeCut));
                    }
                }
                thirdTypeCut = this.mWildTypeToFilter.get("*");
                if (debug) {
                    Slog.v(TAG, "Third type cut: " + Arrays.toString(thirdTypeCut));
                }
            } else if (intent.getAction() != null) {
                firstTypeCut = this.mTypedActionToFilter.get(intent.getAction());
                if (debug) {
                    Slog.v(TAG, "Typed Action list: " + Arrays.toString(firstTypeCut));
                }
            }
        }
        if (scheme != null) {
            F[] schemeCut2 = this.mSchemeToFilter.get(scheme);
            schemeCut = schemeCut2;
            if (debug) {
                Slog.v(TAG, "Scheme list: " + Arrays.toString(schemeCut));
            }
        }
        if (resolvedType == null && scheme == null && intent.getAction() != null) {
            firstTypeCut = this.mActionToFilter.get(intent.getAction());
            if (debug) {
                Slog.v(TAG, "Action list: " + Arrays.toString(firstTypeCut));
            }
        }
        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        if (firstTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, firstTypeCut, finalList, userId);
        }
        if (secondTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, secondTypeCut, finalList, userId);
        }
        if (thirdTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, thirdTypeCut, finalList, userId);
        }
        if (schemeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme, schemeCut, finalList, userId);
        }
        sortResults(finalList);
        if (debug) {
            Slog.v(TAG, "Final result list:");
            for (int i = 0; i < finalList.size(); i++) {
                Slog.v(TAG, "  " + finalList.get(i));
            }
        }
        return finalList;
    }

    protected boolean allowFilterResult(F filter, List<R> dest) {
        return true;
    }

    protected boolean isFilterStopped(F filter, int userId) {
        return false;
    }

    protected boolean isFilterVerified(F filter) {
        return filter.isVerified();
    }

    protected R newResult(F f, int match, int userId) {
        return f;
    }

    protected void sortResults(List<R> results) {
        Collections.sort(results, mResolvePrioritySorter);
    }

    protected void dumpFilter(PrintWriter out, String prefix, F filter) {
        out.print(prefix);
        out.println(filter);
    }

    protected Object filterToLabel(F filter) {
        return "IntentFilter";
    }

    protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
        out.print(prefix);
        out.print(label);
        out.print(": ");
        out.println(count);
    }

    private final void addFilter(ArrayMap<String, F[]> arrayMap, String name, F filter) {
        IntentFilter[] intentFilterArr = (IntentFilter[]) arrayMap.get(name);
        if (intentFilterArr == null) {
            IntentFilter[] intentFilterArrNewArray = newArray(2);
            arrayMap.put(name, intentFilterArrNewArray);
            intentFilterArrNewArray[0] = filter;
            return;
        }
        int N = intentFilterArr.length;
        int i = N;
        while (i > 0 && intentFilterArr[i - 1] == null) {
            i--;
        }
        if (i < N) {
            intentFilterArr[i] = filter;
            return;
        }
        IntentFilter[] intentFilterArrNewArray2 = newArray((N * 3) / 2);
        System.arraycopy(intentFilterArr, 0, intentFilterArrNewArray2, 0, N);
        intentFilterArrNewArray2[N] = filter;
        arrayMap.put(name, intentFilterArrNewArray2);
    }

    private final int register_mime_types(F filter, String prefix) {
        Iterator<String> i = filter.typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }
            addFilter(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                addFilter(this.mBaseTypeToFilter, baseName, filter);
            } else {
                addFilter(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int unregister_mime_types(F filter, String prefix) {
        Iterator<String> i = filter.typesIterator();
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            String baseName = name;
            int slashpos = name.indexOf(47);
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }
            remove_all_objects(this.mTypeToFilter, name, filter);
            if (slashpos > 0) {
                remove_all_objects(this.mBaseTypeToFilter, baseName, filter);
            } else {
                remove_all_objects(this.mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int register_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            addFilter(dest, name, filter);
        }
        return num;
    }

    private final int unregister_intent_filter(F filter, Iterator<String> i, ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }
        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            remove_all_objects(dest, name, filter);
        }
        return num;
    }

    private final void remove_all_objects(ArrayMap<String, F[]> arrayMap, String name, Object object) {
        IntentFilter[] intentFilterArr = (IntentFilter[]) arrayMap.get(name);
        if (intentFilterArr == null) {
            return;
        }
        int LAST = intentFilterArr.length - 1;
        while (LAST >= 0 && intentFilterArr[LAST] == null) {
            LAST--;
        }
        for (int idx = LAST; idx >= 0; idx--) {
            if (intentFilterArr[idx] == object) {
                int remain = LAST - idx;
                if (remain > 0) {
                    System.arraycopy(intentFilterArr, idx + 1, intentFilterArr, idx, remain);
                }
                intentFilterArr[LAST] = null;
                LAST--;
            }
        }
        if (LAST < 0) {
            arrayMap.remove(name);
        } else {
            if (LAST >= intentFilterArr.length / 2) {
                return;
            }
            IntentFilter[] intentFilterArrNewArray = newArray(LAST + 2);
            System.arraycopy(intentFilterArr, 0, intentFilterArrNewArray, 0, LAST + 1);
            arrayMap.put(name, intentFilterArrNewArray);
        }
    }

    private static FastImmutableArraySet<String> getFastIntentCategories(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories == null) {
            return null;
        }
        return new FastImmutableArraySet<>((String[]) categories.toArray(new String[categories.size()]));
    }

    private void buildResolveList(Intent intent, FastImmutableArraySet<String> categories, boolean debug, boolean defaultOnly, String resolvedType, String scheme, F[] src, List<R> dest, int userId) {
        Printer logPrinter;
        FastPrintWriter fastPrintWriter;
        String reason;
        String action = intent.getAction();
        Uri data = intent.getData();
        String packageName = intent.getPackage();
        boolean excludingStopped = intent.isExcludingStopped();
        if (debug) {
            logPrinter = new LogPrinter(2, TAG, 3);
            fastPrintWriter = new FastPrintWriter(logPrinter);
        } else {
            logPrinter = null;
            fastPrintWriter = null;
        }
        int N = src != null ? src.length : 0;
        boolean hasNonDefaults = false;
        for (int i = 0; i < N; i++) {
            F filter = src[i];
            if (filter == null) {
                if (debug || !hasNonDefaults) {
                }
                if (dest.size() == 0) {
                    Slog.v(TAG, "resolveIntent failed: found match, but none with CATEGORY_DEFAULT");
                    return;
                } else {
                    if (dest.size() > 1) {
                        Slog.v(TAG, "resolveIntent: multiple matches, only some with CATEGORY_DEFAULT");
                        return;
                    }
                    return;
                }
            }
            if (debug) {
                Slog.v(TAG, "Matching against filter " + filter);
            }
            if (excludingStopped && isFilterStopped(filter, userId)) {
                if (debug) {
                    Slog.v(TAG, "  Filter's target is stopped; skipping");
                }
            } else if (packageName == null || isPackageForFilter(packageName, filter)) {
                if (filter.getAutoVerify() && debug) {
                    Slog.v(TAG, "  Filter verified: " + isFilterVerified(filter));
                    int authorities = filter.countDataAuthorities();
                    for (int z = 0; z < authorities; z++) {
                        Slog.v(TAG, "   " + filter.getDataAuthority(z).getHost());
                    }
                }
                if (allowFilterResult(filter, dest)) {
                    int match = filter.match(action, resolvedType, scheme, data, categories, TAG);
                    if (match >= 0) {
                        if (debug) {
                            Slog.v(TAG, "  Filter matched!  match=0x" + Integer.toHexString(match) + " hasDefault=" + filter.hasCategory("android.intent.category.DEFAULT"));
                        }
                        if (!defaultOnly || filter.hasCategory("android.intent.category.DEFAULT")) {
                            R oneResult = newResult(filter, match, userId);
                            if (oneResult != null) {
                                dest.add(oneResult);
                                if (debug) {
                                    dumpFilter(fastPrintWriter, "    ", filter);
                                    fastPrintWriter.flush();
                                    filter.dump(logPrinter, "    ");
                                }
                            }
                        } else {
                            hasNonDefaults = true;
                        }
                    } else if (debug) {
                        switch (match) {
                            case SyncOperation.REASON_PERIODIC:
                                reason = "category";
                                break;
                            case -3:
                                reason = "action";
                                break;
                            case -2:
                                reason = "data";
                                break;
                            case -1:
                                reason = DatabaseHelper.SoundModelContract.KEY_TYPE;
                                break;
                            default:
                                reason = "unknown reason";
                                break;
                        }
                        Slog.v(TAG, "  Filter did not match: " + reason);
                    }
                } else if (debug) {
                    Slog.v(TAG, "  Filter's target already added");
                }
            } else if (debug) {
                Slog.v(TAG, "  Filter is not from package " + packageName + "; skipping");
            }
        }
        if (debug) {
        }
    }
}
