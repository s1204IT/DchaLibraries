package android.view.inputmethod;

import android.os.Bundle;
import android.os.FileObserver;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Printer;
import java.util.Arrays;

public class EditorInfo implements InputType, Parcelable {
    public static final Parcelable.Creator<EditorInfo> CREATOR = new Parcelable.Creator<EditorInfo>() {
        @Override
        public EditorInfo createFromParcel(Parcel parcel) {
            EditorInfo editorInfo = new EditorInfo();
            editorInfo.inputType = parcel.readInt();
            editorInfo.imeOptions = parcel.readInt();
            editorInfo.privateImeOptions = parcel.readString();
            editorInfo.actionLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            editorInfo.actionId = parcel.readInt();
            editorInfo.initialSelStart = parcel.readInt();
            editorInfo.initialSelEnd = parcel.readInt();
            editorInfo.initialCapsMode = parcel.readInt();
            editorInfo.hintText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            editorInfo.label = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            editorInfo.packageName = parcel.readString();
            editorInfo.fieldId = parcel.readInt();
            editorInfo.fieldName = parcel.readString();
            editorInfo.extras = parcel.readBundle();
            LocaleList localeListCreateFromParcel = LocaleList.CREATOR.createFromParcel(parcel);
            if (localeListCreateFromParcel.isEmpty()) {
                localeListCreateFromParcel = null;
            }
            editorInfo.hintLocales = localeListCreateFromParcel;
            editorInfo.contentMimeTypes = parcel.readStringArray();
            return editorInfo;
        }

        @Override
        public EditorInfo[] newArray(int i) {
            return new EditorInfo[i];
        }
    };
    public static final int IME_ACTION_DONE = 6;
    public static final int IME_ACTION_GO = 2;
    public static final int IME_ACTION_NEXT = 5;
    public static final int IME_ACTION_NONE = 1;
    public static final int IME_ACTION_PREVIOUS = 7;
    public static final int IME_ACTION_SEARCH = 3;
    public static final int IME_ACTION_SEND = 4;
    public static final int IME_ACTION_UNSPECIFIED = 0;
    public static final int IME_FLAG_FORCE_ASCII = Integer.MIN_VALUE;
    public static final int IME_FLAG_NAVIGATE_NEXT = 134217728;
    public static final int IME_FLAG_NAVIGATE_PREVIOUS = 67108864;
    public static final int IME_FLAG_NO_ACCESSORY_ACTION = 536870912;
    public static final int IME_FLAG_NO_ENTER_ACTION = 1073741824;
    public static final int IME_FLAG_NO_EXTRACT_UI = 268435456;
    public static final int IME_FLAG_NO_FULLSCREEN = 33554432;
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 16777216;
    public static final int IME_MASK_ACTION = 255;
    public static final int IME_NULL = 0;
    public Bundle extras;
    public int fieldId;
    public String fieldName;
    public CharSequence hintText;
    public CharSequence label;
    public String packageName;
    public int inputType = 0;
    public int imeOptions = 0;
    public String privateImeOptions = null;
    public CharSequence actionLabel = null;
    public int actionId = 0;
    public int initialSelStart = -1;
    public int initialSelEnd = -1;
    public int initialCapsMode = 0;
    public LocaleList hintLocales = null;
    public String[] contentMimeTypes = null;

    public final void makeCompatible(int i) {
        if (i < 11) {
            int i2 = this.inputType & FileObserver.ALL_EVENTS;
            if (i2 == 2 || i2 == 18) {
                this.inputType = (this.inputType & InputType.TYPE_MASK_FLAGS) | 2;
            } else if (i2 == 209) {
                this.inputType = 33 | (this.inputType & InputType.TYPE_MASK_FLAGS);
            } else if (i2 == 225) {
                this.inputType = 129 | (this.inputType & InputType.TYPE_MASK_FLAGS);
            }
        }
    }

    public void dump(Printer printer, String str) {
        printer.println(str + "inputType=0x" + Integer.toHexString(this.inputType) + " imeOptions=0x" + Integer.toHexString(this.imeOptions) + " privateImeOptions=" + this.privateImeOptions);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("actionLabel=");
        sb.append((Object) this.actionLabel);
        sb.append(" actionId=");
        sb.append(this.actionId);
        printer.println(sb.toString());
        printer.println(str + "initialSelStart=" + this.initialSelStart + " initialSelEnd=" + this.initialSelEnd + " initialCapsMode=0x" + Integer.toHexString(this.initialCapsMode));
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str);
        sb2.append("hintText=");
        sb2.append((Object) this.hintText);
        sb2.append(" label=");
        sb2.append((Object) this.label);
        printer.println(sb2.toString());
        printer.println(str + "packageName=" + this.packageName + " fieldId=" + this.fieldId + " fieldName=" + this.fieldName);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(str);
        sb3.append("extras=");
        sb3.append(this.extras);
        printer.println(sb3.toString());
        printer.println(str + "hintLocales=" + this.hintLocales);
        printer.println(str + "contentMimeTypes=" + Arrays.toString(this.contentMimeTypes));
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.inputType);
        parcel.writeInt(this.imeOptions);
        parcel.writeString(this.privateImeOptions);
        TextUtils.writeToParcel(this.actionLabel, parcel, i);
        parcel.writeInt(this.actionId);
        parcel.writeInt(this.initialSelStart);
        parcel.writeInt(this.initialSelEnd);
        parcel.writeInt(this.initialCapsMode);
        TextUtils.writeToParcel(this.hintText, parcel, i);
        TextUtils.writeToParcel(this.label, parcel, i);
        parcel.writeString(this.packageName);
        parcel.writeInt(this.fieldId);
        parcel.writeString(this.fieldName);
        parcel.writeBundle(this.extras);
        if (this.hintLocales != null) {
            this.hintLocales.writeToParcel(parcel, i);
        } else {
            LocaleList.getEmptyLocaleList().writeToParcel(parcel, i);
        }
        parcel.writeStringArray(this.contentMimeTypes);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
