package com.android.documentsui.base;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.documentsui.sorting.SortModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class State implements Parcelable {
    public static final Parcelable.ClassLoaderCreator<State> CREATOR = new Parcelable.ClassLoaderCreator<State>() {
        @Override
        public State createFromParcel(Parcel parcel) {
            return createFromParcel(parcel, (ClassLoader) null);
        }

        @Override
        public State createFromParcel(Parcel parcel, ClassLoader classLoader) {
            State state = new State();
            state.action = parcel.readInt();
            state.acceptMimes = parcel.readStringArray();
            state.allowMultiple = parcel.readInt() != 0;
            state.localOnly = parcel.readInt() != 0;
            state.showDeviceStorageOption = parcel.readInt() != 0;
            state.showAdvanced = parcel.readInt() != 0;
            DurableUtils.readFromParcel(parcel, state.stack);
            parcel.readMap(state.dirConfigs, classLoader);
            parcel.readList(state.excludedAuthorities, classLoader);
            state.openableOnly = parcel.readInt() != 0;
            state.sortModel = (SortModel) parcel.readParcelable(classLoader);
            return state;
        }

        @Override
        public State[] newArray(int i) {
            return new State[i];
        }
    };
    public String[] acceptMimes;
    public int action;
    public boolean allowMultiple;
    public boolean directoryCopy;
    public boolean localOnly;
    public boolean openableOnly;
    public boolean showAdvanced;
    public boolean showDeviceStorageOption;
    public SortModel sortModel;
    public int derivedMode = 2;
    public boolean debugMode = false;
    public int copyOperationSubType = -1;
    public final DocumentStack stack = new DocumentStack();
    public HashMap<String, SparseArray<Parcelable>> dirConfigs = new HashMap<>();
    public List<String> excludedAuthorities = new ArrayList();
    public List<String> userAssignedAuthority = new ArrayList();

    public void initAcceptMimes(Intent intent, String str) {
        if (intent.hasExtra("android.intent.extra.MIME_TYPES")) {
            this.acceptMimes = intent.getStringArrayExtra("android.intent.extra.MIME_TYPES");
        } else {
            this.acceptMimes = new String[]{str};
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.action);
        parcel.writeStringArray(this.acceptMimes);
        parcel.writeInt(this.allowMultiple ? 1 : 0);
        parcel.writeInt(this.localOnly ? 1 : 0);
        parcel.writeInt(this.showDeviceStorageOption ? 1 : 0);
        parcel.writeInt(this.showAdvanced ? 1 : 0);
        DurableUtils.writeToParcel(parcel, this.stack);
        parcel.writeMap(this.dirConfigs);
        parcel.writeList(this.excludedAuthorities);
        parcel.writeInt(this.openableOnly ? 1 : 0);
        parcel.writeParcelable(this.sortModel, 0);
    }

    public String toString() {
        return "State{action=" + this.action + ", acceptMimes=" + Arrays.toString(this.acceptMimes) + ", allowMultiple=" + this.allowMultiple + ", localOnly=" + this.localOnly + ", showDeviceStorageOption=" + this.showDeviceStorageOption + ", showAdvanced=" + this.showAdvanced + ", stack=" + this.stack + ", dirConfigs=" + this.dirConfigs + ", excludedAuthorities=" + this.excludedAuthorities + ", openableOnly=" + this.openableOnly + ", sortModel=" + this.sortModel + "}";
    }
}
