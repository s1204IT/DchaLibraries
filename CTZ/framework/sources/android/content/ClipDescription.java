package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ClipDescription implements Parcelable {
    public static final Parcelable.Creator<ClipDescription> CREATOR = new Parcelable.Creator<ClipDescription>() {
        @Override
        public ClipDescription createFromParcel(Parcel parcel) {
            return new ClipDescription(parcel);
        }

        @Override
        public ClipDescription[] newArray(int i) {
            return new ClipDescription[i];
        }
    };
    public static final String EXTRA_TARGET_COMPONENT_NAME = "android.content.extra.TARGET_COMPONENT_NAME";
    public static final String EXTRA_USER_SERIAL_NUMBER = "android.content.extra.USER_SERIAL_NUMBER";
    public static final String MIMETYPE_TEXT_HTML = "text/html";
    public static final String MIMETYPE_TEXT_INTENT = "text/vnd.android.intent";
    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
    public static final String MIMETYPE_TEXT_URILIST = "text/uri-list";
    private PersistableBundle mExtras;
    final CharSequence mLabel;
    private final ArrayList<String> mMimeTypes;
    private long mTimeStamp;

    public ClipDescription(CharSequence charSequence, String[] strArr) {
        if (strArr == null) {
            throw new NullPointerException("mimeTypes is null");
        }
        this.mLabel = charSequence;
        this.mMimeTypes = new ArrayList<>(Arrays.asList(strArr));
    }

    public ClipDescription(ClipDescription clipDescription) {
        this.mLabel = clipDescription.mLabel;
        this.mMimeTypes = new ArrayList<>(clipDescription.mMimeTypes);
        this.mTimeStamp = clipDescription.mTimeStamp;
    }

    public static boolean compareMimeTypes(String str, String str2) {
        int length = str2.length();
        if (length == 3 && str2.equals("*/*")) {
            return true;
        }
        int iIndexOf = str2.indexOf(47);
        if (iIndexOf > 0) {
            if (length == iIndexOf + 2) {
                int i = iIndexOf + 1;
                if (str2.charAt(i) == '*') {
                    if (str2.regionMatches(0, str, 0, i)) {
                        return true;
                    }
                } else if (str2.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setTimestamp(long j) {
        this.mTimeStamp = j;
    }

    public long getTimestamp() {
        return this.mTimeStamp;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public boolean hasMimeType(String str) {
        int size = this.mMimeTypes.size();
        for (int i = 0; i < size; i++) {
            if (compareMimeTypes(this.mMimeTypes.get(i), str)) {
                return true;
            }
        }
        return false;
    }

    public String[] filterMimeTypes(String str) {
        int size = this.mMimeTypes.size();
        ArrayList arrayList = null;
        for (int i = 0; i < size; i++) {
            if (compareMimeTypes(this.mMimeTypes.get(i), str)) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(this.mMimeTypes.get(i));
            }
        }
        if (arrayList == null) {
            return null;
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        return strArr;
    }

    public int getMimeTypeCount() {
        return this.mMimeTypes.size();
    }

    public String getMimeType(int i) {
        return this.mMimeTypes.get(i);
    }

    void addMimeTypes(String[] strArr) {
        for (int i = 0; i != strArr.length; i++) {
            String str = strArr[i];
            if (!this.mMimeTypes.contains(str)) {
                this.mMimeTypes.add(str);
            }
        }
    }

    public PersistableBundle getExtras() {
        return this.mExtras;
    }

    public void setExtras(PersistableBundle persistableBundle) {
        this.mExtras = new PersistableBundle(persistableBundle);
    }

    public void validate() {
        if (this.mMimeTypes == null) {
            throw new NullPointerException("null mime types");
        }
        int size = this.mMimeTypes.size();
        if (size <= 0) {
            throw new IllegalArgumentException("must have at least 1 mime type");
        }
        for (int i = 0; i < size; i++) {
            if (this.mMimeTypes.get(i) == null) {
                throw new NullPointerException("mime type at " + i + " is null");
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ClipDescription { ");
        toShortString(sb);
        sb.append(" }");
        return sb.toString();
    }

    public boolean toShortString(StringBuilder sb) {
        boolean z = !toShortStringTypesOnly(sb);
        if (this.mLabel != null) {
            if (!z) {
                sb.append(' ');
            }
            sb.append('\"');
            sb.append(this.mLabel);
            sb.append('\"');
            z = false;
        }
        if (this.mExtras != null) {
            if (!z) {
                sb.append(' ');
            }
            sb.append(this.mExtras.toString());
            z = false;
        }
        if (this.mTimeStamp > 0) {
            if (!z) {
                sb.append(' ');
            }
            sb.append('<');
            sb.append(TimeUtils.logTimeOfDay(this.mTimeStamp));
            sb.append('>');
            z = false;
        }
        return !z;
    }

    public boolean toShortStringTypesOnly(StringBuilder sb) {
        int size = this.mMimeTypes.size();
        int i = 0;
        boolean z = true;
        while (i < size) {
            if (!z) {
                sb.append(' ');
            }
            sb.append(this.mMimeTypes.get(i));
            i++;
            z = false;
        }
        return !z;
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        int size = this.mMimeTypes.size();
        for (int i = 0; i < size; i++) {
            protoOutputStream.write(2237677961217L, this.mMimeTypes.get(i));
        }
        if (this.mLabel != null) {
            protoOutputStream.write(1138166333442L, this.mLabel.toString());
        }
        if (this.mExtras != null) {
            this.mExtras.writeToProto(protoOutputStream, 1146756268035L);
        }
        if (this.mTimeStamp > 0) {
            protoOutputStream.write(1112396529668L, this.mTimeStamp);
        }
        protoOutputStream.end(jStart);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        TextUtils.writeToParcel(this.mLabel, parcel, i);
        parcel.writeStringList(this.mMimeTypes);
        parcel.writePersistableBundle(this.mExtras);
        parcel.writeLong(this.mTimeStamp);
    }

    ClipDescription(Parcel parcel) {
        this.mLabel = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mMimeTypes = parcel.createStringArrayList();
        this.mExtras = parcel.readPersistableBundle();
        this.mTimeStamp = parcel.readLong();
    }
}
