package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class KeyboardShortcutGroup implements Parcelable {
    public static final Parcelable.Creator<KeyboardShortcutGroup> CREATOR = new Parcelable.Creator<KeyboardShortcutGroup>() {
        @Override
        public KeyboardShortcutGroup createFromParcel(Parcel parcel) {
            return new KeyboardShortcutGroup(parcel);
        }

        @Override
        public KeyboardShortcutGroup[] newArray(int i) {
            return new KeyboardShortcutGroup[i];
        }
    };
    private final List<KeyboardShortcutInfo> mItems;
    private final CharSequence mLabel;
    private boolean mSystemGroup;

    public KeyboardShortcutGroup(CharSequence charSequence, List<KeyboardShortcutInfo> list) {
        this.mLabel = charSequence;
        this.mItems = new ArrayList((Collection) Preconditions.checkNotNull(list));
    }

    public KeyboardShortcutGroup(CharSequence charSequence) {
        this(charSequence, (List<KeyboardShortcutInfo>) Collections.emptyList());
    }

    public KeyboardShortcutGroup(CharSequence charSequence, List<KeyboardShortcutInfo> list, boolean z) {
        this.mLabel = charSequence;
        this.mItems = new ArrayList((Collection) Preconditions.checkNotNull(list));
        this.mSystemGroup = z;
    }

    public KeyboardShortcutGroup(CharSequence charSequence, boolean z) {
        this(charSequence, Collections.emptyList(), z);
    }

    private KeyboardShortcutGroup(Parcel parcel) {
        this.mItems = new ArrayList();
        this.mLabel = parcel.readCharSequence();
        parcel.readTypedList(this.mItems, KeyboardShortcutInfo.CREATOR);
        this.mSystemGroup = parcel.readInt() == 1;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public List<KeyboardShortcutInfo> getItems() {
        return this.mItems;
    }

    public boolean isSystemGroup() {
        return this.mSystemGroup;
    }

    public void addItem(KeyboardShortcutInfo keyboardShortcutInfo) {
        this.mItems.add(keyboardShortcutInfo);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeCharSequence(this.mLabel);
        parcel.writeTypedList(this.mItems);
        parcel.writeInt(this.mSystemGroup ? 1 : 0);
    }
}
