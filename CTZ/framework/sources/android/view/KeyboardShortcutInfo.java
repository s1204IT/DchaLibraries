package android.view;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public final class KeyboardShortcutInfo implements Parcelable {
    public static final Parcelable.Creator<KeyboardShortcutInfo> CREATOR = new Parcelable.Creator<KeyboardShortcutInfo>() {
        @Override
        public KeyboardShortcutInfo createFromParcel(Parcel parcel) {
            return new KeyboardShortcutInfo(parcel);
        }

        @Override
        public KeyboardShortcutInfo[] newArray(int i) {
            return new KeyboardShortcutInfo[i];
        }
    };
    private final char mBaseCharacter;
    private final Icon mIcon;
    private final int mKeycode;
    private final CharSequence mLabel;
    private final int mModifiers;

    public KeyboardShortcutInfo(CharSequence charSequence, Icon icon, int i, int i2) {
        this.mLabel = charSequence;
        this.mIcon = icon;
        boolean z = false;
        this.mBaseCharacter = (char) 0;
        if (i >= 0 && i <= KeyEvent.getMaxKeyCode()) {
            z = true;
        }
        Preconditions.checkArgument(z);
        this.mKeycode = i;
        this.mModifiers = i2;
    }

    public KeyboardShortcutInfo(CharSequence charSequence, int i, int i2) {
        this(charSequence, null, i, i2);
    }

    public KeyboardShortcutInfo(CharSequence charSequence, char c, int i) {
        this.mLabel = charSequence;
        Preconditions.checkArgument(c != 0);
        this.mBaseCharacter = c;
        this.mKeycode = 0;
        this.mModifiers = i;
        this.mIcon = null;
    }

    private KeyboardShortcutInfo(Parcel parcel) {
        this.mLabel = parcel.readCharSequence();
        this.mIcon = (Icon) parcel.readParcelable(null);
        this.mBaseCharacter = (char) parcel.readInt();
        this.mKeycode = parcel.readInt();
        this.mModifiers = parcel.readInt();
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public int getKeycode() {
        return this.mKeycode;
    }

    public char getBaseCharacter() {
        return this.mBaseCharacter;
    }

    public int getModifiers() {
        return this.mModifiers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeCharSequence(this.mLabel);
        parcel.writeParcelable(this.mIcon, 0);
        parcel.writeInt(this.mBaseCharacter);
        parcel.writeInt(this.mKeycode);
        parcel.writeInt(this.mModifiers);
    }
}
