package com.android.documentsui.selection;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Selection implements Parcelable, Iterable<String> {
    public static final Parcelable.ClassLoaderCreator<Selection> CREATOR = new Parcelable.ClassLoaderCreator<Selection>() {
        @Override
        public Selection createFromParcel(Parcel parcel) {
            return createFromParcel(parcel, (ClassLoader) null);
        }

        @Override
        public Selection createFromParcel(Parcel parcel, ClassLoader classLoader) {
            ArrayList arrayList = new ArrayList();
            parcel.readStringList(arrayList);
            return new Selection(new HashSet(arrayList));
        }

        @Override
        public Selection[] newArray(int i) {
            return new Selection[i];
        }
    };
    final Set<String> mProvisionalSelection;
    final Set<String> mSelection;

    public Selection() {
        this.mSelection = new HashSet();
        this.mProvisionalSelection = new HashSet();
    }

    private Selection(Set<String> set) {
        this.mSelection = set;
        this.mProvisionalSelection = new HashSet();
    }

    public boolean contains(String str) {
        return this.mSelection.contains(str) || this.mProvisionalSelection.contains(str);
    }

    @Override
    public Iterator<String> iterator() {
        return this.mSelection.iterator();
    }

    public int size() {
        return this.mSelection.size() + this.mProvisionalSelection.size();
    }

    public boolean isEmpty() {
        return this.mSelection.isEmpty() && this.mProvisionalSelection.isEmpty();
    }

    Map<String, Boolean> setProvisionalSelection(Set<String> set) {
        HashMap map = new HashMap();
        for (String str : this.mProvisionalSelection) {
            if (!set.contains(str) && !this.mSelection.contains(str)) {
                map.put(str, false);
            }
        }
        for (String str2 : this.mSelection) {
            if (!set.contains(str2)) {
                map.put(str2, false);
            }
        }
        for (String str3 : set) {
            if (!this.mSelection.contains(str3) && !this.mProvisionalSelection.contains(str3)) {
                map.put(str3, true);
            }
        }
        for (Map.Entry entry : map.entrySet()) {
            String str4 = (String) entry.getKey();
            if (((Boolean) entry.getValue()).booleanValue()) {
                this.mProvisionalSelection.add(str4);
            } else {
                this.mProvisionalSelection.remove(str4);
            }
        }
        return map;
    }

    protected void mergeProvisionalSelection() {
        this.mSelection.addAll(this.mProvisionalSelection);
        this.mProvisionalSelection.clear();
    }

    void clearProvisionalSelection() {
        this.mProvisionalSelection.clear();
    }

    boolean add(String str) {
        if (this.mSelection.contains(str)) {
            return false;
        }
        this.mSelection.add(str);
        return true;
    }

    boolean remove(String str) {
        if (!this.mSelection.contains(str)) {
            return false;
        }
        this.mSelection.remove(str);
        return true;
    }

    void clear() {
        this.mSelection.clear();
    }

    void intersect(Collection<String> collection) {
        Preconditions.checkArgument(collection != null);
        this.mSelection.retainAll(collection);
        this.mProvisionalSelection.retainAll(collection);
    }

    void copyFrom(Selection selection) {
        this.mSelection.clear();
        this.mSelection.addAll(selection.mSelection);
        this.mProvisionalSelection.clear();
        this.mProvisionalSelection.addAll(selection.mProvisionalSelection);
    }

    public String toString() {
        if (size() <= 0) {
            return "size=0, items=[]";
        }
        StringBuilder sb = new StringBuilder(size() * 28);
        sb.append("Selection{");
        sb.append("primary{size=" + this.mSelection.size());
        sb.append(", entries=" + this.mSelection);
        sb.append("}, provisional{size=" + this.mProvisionalSelection.size());
        sb.append(", entries=" + this.mProvisionalSelection);
        sb.append("}}");
        return sb.toString();
    }

    public int hashCode() {
        return this.mSelection.hashCode() ^ this.mProvisionalSelection.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Selection) {
            return equals((Selection) obj);
        }
        return false;
    }

    private boolean equals(Selection selection) {
        return this.mSelection.equals(selection.mSelection) && this.mProvisionalSelection.equals(selection.mProvisionalSelection);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringList(new ArrayList(this.mSelection));
    }
}
