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
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FastPrintWriter;
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
        public int compare(Object obj, Object obj2) {
            int priority = ((IntentFilter) obj).getPriority();
            int priority2 = ((IntentFilter) obj2).getPriority();
            if (priority > priority2) {
                return -1;
            }
            return priority < priority2 ? 1 : 0;
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
        int iRegister_intent_filter = register_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int iRegister_mime_types = register_mime_types(f, "      Type: ");
        if (iRegister_intent_filter == 0 && iRegister_mime_types == 0) {
            register_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (iRegister_mime_types != 0) {
            register_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    public static boolean filterEquals(IntentFilter intentFilter, IntentFilter intentFilter2) {
        int iCountActions = intentFilter.countActions();
        if (iCountActions != intentFilter2.countActions()) {
            return false;
        }
        for (int i = 0; i < iCountActions; i++) {
            if (!intentFilter2.hasAction(intentFilter.getAction(i))) {
                return false;
            }
        }
        int iCountCategories = intentFilter.countCategories();
        if (iCountCategories != intentFilter2.countCategories()) {
            return false;
        }
        for (int i2 = 0; i2 < iCountCategories; i2++) {
            if (!intentFilter2.hasCategory(intentFilter.getCategory(i2))) {
                return false;
            }
        }
        int iCountDataTypes = intentFilter.countDataTypes();
        if (iCountDataTypes != intentFilter2.countDataTypes()) {
            return false;
        }
        for (int i3 = 0; i3 < iCountDataTypes; i3++) {
            if (!intentFilter2.hasExactDataType(intentFilter.getDataType(i3))) {
                return false;
            }
        }
        int iCountDataSchemes = intentFilter.countDataSchemes();
        if (iCountDataSchemes != intentFilter2.countDataSchemes()) {
            return false;
        }
        for (int i4 = 0; i4 < iCountDataSchemes; i4++) {
            if (!intentFilter2.hasDataScheme(intentFilter.getDataScheme(i4))) {
                return false;
            }
        }
        int iCountDataAuthorities = intentFilter.countDataAuthorities();
        if (iCountDataAuthorities != intentFilter2.countDataAuthorities()) {
            return false;
        }
        for (int i5 = 0; i5 < iCountDataAuthorities; i5++) {
            if (!intentFilter2.hasDataAuthority(intentFilter.getDataAuthority(i5))) {
                return false;
            }
        }
        int iCountDataPaths = intentFilter.countDataPaths();
        if (iCountDataPaths != intentFilter2.countDataPaths()) {
            return false;
        }
        for (int i6 = 0; i6 < iCountDataPaths; i6++) {
            if (!intentFilter2.hasDataPath(intentFilter.getDataPath(i6))) {
                return false;
            }
        }
        int iCountDataSchemeSpecificParts = intentFilter.countDataSchemeSpecificParts();
        if (iCountDataSchemeSpecificParts != intentFilter2.countDataSchemeSpecificParts()) {
            return false;
        }
        for (int i7 = 0; i7 < iCountDataSchemeSpecificParts; i7++) {
            if (!intentFilter2.hasDataSchemeSpecificPart(intentFilter.getDataSchemeSpecificPart(i7))) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<F> collectFilters(F[] fArr, IntentFilter intentFilter) {
        F f;
        ArrayList<F> arrayList = null;
        if (fArr != null) {
            for (int i = 0; i < fArr.length && (f = fArr[i]) != null; i++) {
                if (filterEquals(f, intentFilter)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    arrayList.add(f);
                }
            }
        }
        return arrayList;
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
        ArrayList<F> arrayList = null;
        for (F f : this.mFilters) {
            if (filterEquals(f, intentFilter)) {
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                }
                arrayList.add(f);
            }
        }
        return arrayList;
    }

    public void removeFilter(F f) {
        removeFilterInternal(f);
        this.mFilters.remove(f);
    }

    void removeFilterInternal(F f) {
        int iUnregister_intent_filter = unregister_intent_filter(f, f.schemesIterator(), this.mSchemeToFilter, "      Scheme: ");
        int iUnregister_mime_types = unregister_mime_types(f, "      Type: ");
        if (iUnregister_intent_filter == 0 && iUnregister_mime_types == 0) {
            unregister_intent_filter(f, f.actionsIterator(), this.mActionToFilter, "      Action: ");
        }
        if (iUnregister_mime_types != 0) {
            unregister_intent_filter(f, f.actionsIterator(), this.mTypedActionToFilter, "      TypedAction: ");
        }
    }

    boolean dumpMap(PrintWriter printWriter, String str, String str2, String str3, ArrayMap<String, F[]> arrayMap, String str4, boolean z, boolean z2) {
        PrintWriterPrinter printWriterPrinter;
        String str5;
        PrintWriterPrinter printWriterPrinter2;
        String str6;
        PrintWriterPrinter printWriterPrinter3;
        IntentResolver<F, R> intentResolver = this;
        PrintWriter printWriter2 = printWriter;
        String str7 = str3 + "  ";
        String str8 = str3 + "    ";
        ArrayMap arrayMap2 = new ArrayMap();
        String str9 = str2;
        int i = 0;
        boolean z3 = false;
        PrintWriterPrinter printWriterPrinter4 = null;
        while (i < arrayMap.size()) {
            F[] fArrValueAt = arrayMap.valueAt(i);
            int length = fArrValueAt.length;
            if (z2 && !z) {
                arrayMap2.clear();
                int i2 = 0;
                while (i2 < length) {
                    F f = fArrValueAt[i2];
                    if (f == null) {
                        break;
                    }
                    if (str4 != null && !intentResolver.isPackageForFilter(str4, f)) {
                        str6 = str9;
                        printWriterPrinter3 = printWriterPrinter4;
                    } else {
                        Object objFilterToLabel = intentResolver.filterToLabel(f);
                        str6 = str9;
                        int iIndexOfKey = arrayMap2.indexOfKey(objFilterToLabel);
                        if (iIndexOfKey < 0) {
                            printWriterPrinter3 = printWriterPrinter4;
                            arrayMap2.put(objFilterToLabel, new MutableInt(1));
                        } else {
                            printWriterPrinter3 = printWriterPrinter4;
                            ((MutableInt) arrayMap2.valueAt(iIndexOfKey)).value++;
                        }
                    }
                    i2++;
                    str9 = str6;
                    printWriterPrinter4 = printWriterPrinter3;
                }
                printWriterPrinter = printWriterPrinter4;
                str5 = str9;
                int i3 = 0;
                boolean z4 = false;
                while (i3 < arrayMap2.size()) {
                    if (str5 != null) {
                        printWriter.print(str);
                        printWriter2.println(str5);
                        str5 = null;
                    }
                    if (!z4) {
                        printWriter2.print(str7);
                        printWriter2.print(arrayMap.keyAt(i));
                        printWriter2.println(":");
                        z4 = true;
                    }
                    intentResolver.dumpFilterLabel(printWriter2, str8, arrayMap2.keyAt(i3), ((MutableInt) arrayMap2.valueAt(i3)).value);
                    i3++;
                    z3 = true;
                }
            } else {
                printWriterPrinter = printWriterPrinter4;
                str5 = str9;
                int i4 = 0;
                boolean z5 = false;
                while (i4 < length) {
                    F f2 = fArrValueAt[i4];
                    if (f2 != null) {
                        if (str4 == null || intentResolver.isPackageForFilter(str4, f2)) {
                            if (str5 != null) {
                                printWriter.print(str);
                                printWriter2.println(str5);
                                str5 = null;
                            }
                            if (!z5) {
                                printWriter2.print(str7);
                                printWriter2.print(arrayMap.keyAt(i));
                                printWriter2.println(":");
                                z5 = true;
                            }
                            intentResolver.dumpFilter(printWriter2, str8, f2);
                            if (z) {
                                if (printWriterPrinter == null) {
                                    printWriterPrinter2 = new PrintWriterPrinter(printWriter2);
                                } else {
                                    printWriterPrinter2 = printWriterPrinter;
                                }
                                f2.dump(printWriterPrinter2, str8 + "  ");
                                printWriterPrinter = printWriterPrinter2;
                            }
                            z3 = true;
                        }
                        i4++;
                        intentResolver = this;
                        printWriter2 = printWriter;
                    }
                }
            }
            str9 = str5;
            printWriterPrinter4 = printWriterPrinter;
            i++;
            intentResolver = this;
            printWriter2 = printWriter;
        }
        return z3;
    }

    void writeProtoMap(ProtoOutputStream protoOutputStream, long j, ArrayMap<String, F[]> arrayMap) {
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1138166333441L, arrayMap.keyAt(i));
            for (F f : arrayMap.valueAt(i)) {
                if (f != null) {
                    protoOutputStream.write(2237677961218L, f.toString());
                }
            }
            protoOutputStream.end(jStart);
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        writeProtoMap(protoOutputStream, 2246267895809L, this.mTypeToFilter);
        writeProtoMap(protoOutputStream, 2246267895810L, this.mBaseTypeToFilter);
        writeProtoMap(protoOutputStream, 2246267895811L, this.mWildTypeToFilter);
        writeProtoMap(protoOutputStream, 2246267895812L, this.mSchemeToFilter);
        writeProtoMap(protoOutputStream, 2246267895813L, this.mActionToFilter);
        writeProtoMap(protoOutputStream, 2246267895814L, this.mTypedActionToFilter);
        protoOutputStream.end(jStart);
    }

    public boolean dump(PrintWriter printWriter, String str, String str2, String str3, boolean z, boolean z2) {
        String str4 = str2 + "  ";
        String str5 = "\n" + str2;
        String str6 = str + "\n" + str2;
        if (dumpMap(printWriter, str6, "Full MIME Types:", str4, this.mTypeToFilter, str3, z, z2)) {
            str6 = str5;
        }
        if (dumpMap(printWriter, str6, "Base MIME Types:", str4, this.mBaseTypeToFilter, str3, z, z2)) {
            str6 = str5;
        }
        if (dumpMap(printWriter, str6, "Wild MIME Types:", str4, this.mWildTypeToFilter, str3, z, z2)) {
            str6 = str5;
        }
        if (dumpMap(printWriter, str6, "Schemes:", str4, this.mSchemeToFilter, str3, z, z2)) {
            str6 = str5;
        }
        if (dumpMap(printWriter, str6, "Non-Data Actions:", str4, this.mActionToFilter, str3, z, z2)) {
            str6 = str5;
        }
        if (dumpMap(printWriter, str6, "MIME Typed Actions:", str4, this.mTypedActionToFilter, str3, z, z2)) {
            str6 = str5;
        }
        return str6 == str5;
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

    public List<R> queryIntentFromList(Intent intent, String str, boolean z, ArrayList<F[]> arrayList, int i) {
        ArrayList arrayList2 = new ArrayList();
        boolean z2 = (intent.getFlags() & 8) != 0;
        FastImmutableArraySet<String> fastIntentCategories = getFastIntentCategories(intent);
        String scheme = intent.getScheme();
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            buildResolveList(intent, fastIntentCategories, z2, z, str, scheme, arrayList.get(i2), arrayList2, i);
        }
        filterResults(arrayList2);
        sortResults(arrayList2);
        return arrayList2;
    }

    public List<R> queryIntent(Intent intent, String str, boolean z, int i) {
        Intent intent2;
        boolean z2;
        F[] fArr;
        F[] fArr2;
        F[] fArr3;
        F[] fArr4;
        int iIndexOf;
        F[] fArr5;
        String scheme = intent.getScheme();
        ArrayList arrayList = new ArrayList();
        boolean z3 = (intent.getFlags() & 8) != 0;
        if (z3) {
            StringBuilder sb = new StringBuilder();
            sb.append("Resolving type=");
            sb.append(str);
            sb.append(" scheme=");
            sb.append(scheme);
            sb.append(" defaultOnly=");
            z2 = z;
            sb.append(z2);
            sb.append(" userId=");
            sb.append(i);
            sb.append(" of ");
            intent2 = intent;
            sb.append(intent2);
            Slog.v(TAG, sb.toString());
        } else {
            intent2 = intent;
            z2 = z;
        }
        F[] fArr6 = null;
        if (str == null || (iIndexOf = str.indexOf(47)) <= 0) {
            fArr = null;
            fArr2 = null;
            fArr3 = fArr2;
        } else {
            String strSubstring = str.substring(0, iIndexOf);
            if (!strSubstring.equals("*")) {
                if (str.length() != iIndexOf + 2 || str.charAt(iIndexOf + 1) != '*') {
                    fArr = this.mTypeToFilter.get(str);
                    if (z3) {
                        Slog.v(TAG, "First type cut: " + Arrays.toString(fArr));
                    }
                    fArr5 = this.mWildTypeToFilter.get(strSubstring);
                    if (z3) {
                        Slog.v(TAG, "Second type cut: " + Arrays.toString(fArr5));
                    }
                } else {
                    fArr = this.mBaseTypeToFilter.get(strSubstring);
                    if (z3) {
                        Slog.v(TAG, "First type cut: " + Arrays.toString(fArr));
                    }
                    fArr5 = this.mWildTypeToFilter.get(strSubstring);
                    if (z3) {
                        Slog.v(TAG, "Second type cut: " + Arrays.toString(fArr5));
                    }
                }
                F[] fArr7 = this.mWildTypeToFilter.get("*");
                if (z3) {
                    Slog.v(TAG, "Third type cut: " + Arrays.toString(fArr7));
                }
                fArr2 = fArr5;
                fArr3 = fArr7;
            } else {
                if (intent.getAction() != null) {
                    fArr = this.mTypedActionToFilter.get(intent.getAction());
                    if (z3) {
                        Slog.v(TAG, "Typed Action list: " + Arrays.toString(fArr));
                    }
                    fArr2 = null;
                }
                fArr3 = fArr2;
            }
        }
        if (scheme != null) {
            fArr6 = this.mSchemeToFilter.get(scheme);
            if (z3) {
                Slog.v(TAG, "Scheme list: " + Arrays.toString(fArr6));
            }
        }
        F[] fArr8 = fArr6;
        if (str == null && scheme == null && intent.getAction() != null) {
            F[] fArr9 = this.mActionToFilter.get(intent.getAction());
            if (z3) {
                Slog.v(TAG, "Action list: " + Arrays.toString(fArr9));
            }
            fArr4 = fArr9;
        } else {
            fArr4 = fArr;
        }
        FastImmutableArraySet<String> fastIntentCategories = getFastIntentCategories(intent);
        if (fArr4 != null) {
            buildResolveList(intent2, fastIntentCategories, z3, z2, str, scheme, fArr4, arrayList, i);
        }
        if (fArr2 != null) {
            buildResolveList(intent, fastIntentCategories, z3, z, str, scheme, fArr2, arrayList, i);
        }
        if (fArr3 != null) {
            buildResolveList(intent, fastIntentCategories, z3, z, str, scheme, fArr3, arrayList, i);
        }
        if (fArr8 != null) {
            buildResolveList(intent, fastIntentCategories, z3, z, str, scheme, fArr8, arrayList, i);
        }
        filterResults(arrayList);
        sortResults(arrayList);
        if (z3) {
            Slog.v(TAG, "Final result list:");
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                Slog.v(TAG, "  " + arrayList.get(i2));
            }
        }
        return arrayList;
    }

    protected boolean allowFilterResult(F f, List<R> list) {
        return true;
    }

    protected boolean isFilterStopped(F f, int i) {
        return false;
    }

    protected boolean isFilterVerified(F f) {
        return f.isVerified();
    }

    protected R newResult(F f, int i, int i2) {
        return f;
    }

    protected void sortResults(List<R> list) {
        Collections.sort(list, mResolvePrioritySorter);
    }

    protected void filterResults(List<R> list) {
    }

    protected void dumpFilter(PrintWriter printWriter, String str, F f) {
        printWriter.print(str);
        printWriter.println(f);
    }

    protected Object filterToLabel(F f) {
        return "IntentFilter";
    }

    protected void dumpFilterLabel(PrintWriter printWriter, String str, Object obj, int i) {
        printWriter.print(str);
        printWriter.print(obj);
        printWriter.print(": ");
        printWriter.println(i);
    }

    private final void addFilter(ArrayMap<String, F[]> arrayMap, String str, F f) {
        IntentFilter[] intentFilterArr = (IntentFilter[]) arrayMap.get(str);
        if (intentFilterArr == null) {
            IntentFilter[] intentFilterArrNewArray = newArray(2);
            arrayMap.put(str, intentFilterArrNewArray);
            intentFilterArrNewArray[0] = f;
            return;
        }
        int length = intentFilterArr.length;
        int i = length;
        while (i > 0 && intentFilterArr[i - 1] == null) {
            i--;
        }
        if (i >= length) {
            IntentFilter[] intentFilterArrNewArray2 = newArray((length * 3) / 2);
            System.arraycopy(intentFilterArr, 0, intentFilterArrNewArray2, 0, length);
            intentFilterArrNewArray2[length] = f;
            arrayMap.put(str, intentFilterArrNewArray2);
            return;
        }
        intentFilterArr[i] = f;
    }

    private final int register_mime_types(F f, String str) {
        String strIntern;
        Iterator<String> itTypesIterator = f.typesIterator();
        if (itTypesIterator == null) {
            return 0;
        }
        int i = 0;
        while (itTypesIterator.hasNext()) {
            String next = itTypesIterator.next();
            i++;
            int iIndexOf = next.indexOf(47);
            if (iIndexOf > 0) {
                strIntern = next.substring(0, iIndexOf).intern();
            } else {
                strIntern = next;
                next = next + "/*";
            }
            addFilter(this.mTypeToFilter, next, f);
            if (iIndexOf > 0) {
                addFilter(this.mBaseTypeToFilter, strIntern, f);
            } else {
                addFilter(this.mWildTypeToFilter, strIntern, f);
            }
        }
        return i;
    }

    private final int unregister_mime_types(F f, String str) {
        String strIntern;
        Iterator<String> itTypesIterator = f.typesIterator();
        if (itTypesIterator == null) {
            return 0;
        }
        int i = 0;
        while (itTypesIterator.hasNext()) {
            String next = itTypesIterator.next();
            i++;
            int iIndexOf = next.indexOf(47);
            if (iIndexOf > 0) {
                strIntern = next.substring(0, iIndexOf).intern();
            } else {
                strIntern = next;
                next = next + "/*";
            }
            remove_all_objects(this.mTypeToFilter, next, f);
            if (iIndexOf > 0) {
                remove_all_objects(this.mBaseTypeToFilter, strIntern, f);
            } else {
                remove_all_objects(this.mWildTypeToFilter, strIntern, f);
            }
        }
        return i;
    }

    private final int register_intent_filter(F f, Iterator<String> it, ArrayMap<String, F[]> arrayMap, String str) {
        int i = 0;
        if (it == null) {
            return 0;
        }
        while (it.hasNext()) {
            i++;
            addFilter(arrayMap, it.next(), f);
        }
        return i;
    }

    private final int unregister_intent_filter(F f, Iterator<String> it, ArrayMap<String, F[]> arrayMap, String str) {
        int i = 0;
        if (it == null) {
            return 0;
        }
        while (it.hasNext()) {
            i++;
            remove_all_objects(arrayMap, it.next(), f);
        }
        return i;
    }

    private final void remove_all_objects(ArrayMap<String, F[]> arrayMap, String str, Object obj) {
        IntentFilter[] intentFilterArr = (IntentFilter[]) arrayMap.get(str);
        if (intentFilterArr != null) {
            int length = intentFilterArr.length - 1;
            while (length >= 0 && intentFilterArr[length] == null) {
                length--;
            }
            int i = length;
            while (length >= 0) {
                if (intentFilterArr[length] == obj) {
                    int i2 = i - length;
                    if (i2 > 0) {
                        System.arraycopy(intentFilterArr, length + 1, intentFilterArr, length, i2);
                    }
                    intentFilterArr[i] = null;
                    i--;
                }
                length--;
            }
            if (i < 0) {
                arrayMap.remove(str);
            } else if (i < intentFilterArr.length / 2) {
                IntentFilter[] intentFilterArrNewArray = newArray(i + 2);
                System.arraycopy(intentFilterArr, 0, intentFilterArrNewArray, 0, i + 1);
                arrayMap.put(str, intentFilterArrNewArray);
            }
        }
    }

    private static FastImmutableArraySet<String> getFastIntentCategories(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories == null) {
            return null;
        }
        return new FastImmutableArraySet<>((String[]) categories.toArray(new String[categories.size()]));
    }

    private void buildResolveList(Intent intent, FastImmutableArraySet<String> fastImmutableArraySet, boolean z, boolean z2, String str, String str2, F[] fArr, List<R> list, int i) {
        LogPrinter logPrinter;
        FastPrintWriter fastPrintWriter;
        String str3;
        int i2;
        int i3;
        Uri uri;
        LogPrinter logPrinter2;
        String str4;
        FastPrintWriter fastPrintWriter2;
        String str5;
        F[] fArr2 = fArr;
        String action = intent.getAction();
        Uri data = intent.getData();
        String str6 = intent.getPackage();
        boolean zIsExcludingStopped = intent.isExcludingStopped();
        if (z) {
            LogPrinter logPrinter3 = new LogPrinter(2, TAG, 3);
            logPrinter = logPrinter3;
            fastPrintWriter = new FastPrintWriter(logPrinter3);
        } else {
            logPrinter = null;
            fastPrintWriter = null;
        }
        int length = fArr2 != null ? fArr2.length : 0;
        int i4 = 0;
        boolean z3 = false;
        while (i4 < length) {
            F f = fArr2[i4];
            if (f == null) {
                if (z || !z3) {
                }
                if (list.size() == 0) {
                    Slog.v(TAG, "resolveIntent failed: found match, but none with CATEGORY_DEFAULT");
                    return;
                } else {
                    if (list.size() > 1) {
                        Slog.v(TAG, "resolveIntent: multiple matches, only some with CATEGORY_DEFAULT");
                        return;
                    }
                    return;
                }
            }
            if (z) {
                Slog.v(TAG, "Matching against filter " + f);
            }
            if (zIsExcludingStopped && isFilterStopped(f, i)) {
                if (z) {
                    Slog.v(TAG, "  Filter's target is stopped; skipping");
                }
            } else if (str6 == null || isPackageForFilter(str6, f)) {
                if (f.getAutoVerify() && z) {
                    Slog.v(TAG, "  Filter verified: " + isFilterVerified(f));
                    int iCountDataAuthorities = f.countDataAuthorities();
                    int i5 = 0;
                    while (i5 < iCountDataAuthorities) {
                        Slog.v(TAG, "   " + f.getDataAuthority(i5).getHost());
                        i5++;
                        iCountDataAuthorities = iCountDataAuthorities;
                        i4 = i4;
                    }
                }
                int i6 = i4;
                if (allowFilterResult(f, list)) {
                    str3 = action;
                    i2 = i6;
                    i3 = length;
                    Uri uri2 = data;
                    uri = data;
                    logPrinter2 = logPrinter;
                    str4 = str6;
                    fastPrintWriter2 = fastPrintWriter;
                    int iMatch = f.match(action, str, str2, uri2, fastImmutableArraySet, TAG);
                    if (iMatch >= 0) {
                        if (z) {
                            Slog.v(TAG, "  Filter matched!  match=0x" + Integer.toHexString(iMatch) + " hasDefault=" + f.hasCategory("android.intent.category.DEFAULT"));
                        }
                        if (!z2 || f.hasCategory("android.intent.category.DEFAULT")) {
                            R rNewResult = newResult(f, iMatch, i);
                            if (rNewResult != null) {
                                list.add(rNewResult);
                                if (z) {
                                    dumpFilter(fastPrintWriter2, "    ", f);
                                    fastPrintWriter2.flush();
                                    f.dump(logPrinter2, "    ");
                                }
                            }
                        } else {
                            z3 = true;
                        }
                    } else if (z) {
                        switch (iMatch) {
                            case -4:
                                str5 = "category";
                                break;
                            case -3:
                                str5 = "action";
                                break;
                            case -2:
                                str5 = "data";
                                break;
                            case -1:
                                str5 = DatabaseHelper.SoundModelContract.KEY_TYPE;
                                break;
                            default:
                                str5 = "unknown reason";
                                break;
                        }
                        Slog.v(TAG, "  Filter did not match: " + str5);
                    }
                    i4 = i2 + 1;
                    logPrinter = logPrinter2;
                    fastPrintWriter = fastPrintWriter2;
                    length = i3;
                    action = str3;
                    data = uri;
                    str6 = str4;
                    fArr2 = fArr;
                } else {
                    if (z) {
                        Slog.v(TAG, "  Filter's target already added");
                    }
                    i3 = length;
                    str3 = action;
                    uri = data;
                    str4 = str6;
                    i2 = i6;
                    logPrinter2 = logPrinter;
                    fastPrintWriter2 = fastPrintWriter;
                    i4 = i2 + 1;
                    logPrinter = logPrinter2;
                    fastPrintWriter = fastPrintWriter2;
                    length = i3;
                    action = str3;
                    data = uri;
                    str6 = str4;
                    fArr2 = fArr;
                }
            } else if (z) {
                Slog.v(TAG, "  Filter is not from package " + str6 + "; skipping");
            }
            i2 = i4;
            i3 = length;
            str3 = action;
            uri = data;
            str4 = str6;
            logPrinter2 = logPrinter;
            fastPrintWriter2 = fastPrintWriter;
            i4 = i2 + 1;
            logPrinter = logPrinter2;
            fastPrintWriter = fastPrintWriter2;
            length = i3;
            action = str3;
            data = uri;
            str6 = str4;
            fArr2 = fArr;
        }
        if (z) {
        }
    }
}
