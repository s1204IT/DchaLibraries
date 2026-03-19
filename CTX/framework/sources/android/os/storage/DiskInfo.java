package android.os.storage;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DebugUtils;
import com.android.internal.R;
import com.android.internal.app.DumpHeapActivity;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import java.io.CharArrayWriter;
import java.util.Objects;

public class DiskInfo implements Parcelable {
    public static final String ACTION_DISK_SCANNED = "android.os.storage.action.DISK_SCANNED";
    public static final Parcelable.Creator<DiskInfo> CREATOR = new Parcelable.Creator<DiskInfo>() {
        @Override
        public DiskInfo createFromParcel(Parcel parcel) {
            return new DiskInfo(parcel);
        }

        @Override
        public DiskInfo[] newArray(int i) {
            return new DiskInfo[i];
        }
    };
    public static final String EXTRA_DISK_ID = "android.os.storage.extra.DISK_ID";
    public static final String EXTRA_VOLUME_COUNT = "android.os.storage.extra.VOLUME_COUNT";
    public static final int FLAG_ADOPTABLE = 1;
    public static final int FLAG_DEFAULT_PRIMARY = 2;
    public static final int FLAG_SD = 4;
    public static final int FLAG_USB = 8;
    public final int flags;
    public final String id;
    public String label;
    public long size;
    public String sysPath;
    public int volumeCount;

    public DiskInfo(String str, int i) {
        this.id = (String) Preconditions.checkNotNull(str);
        this.flags = i;
    }

    public DiskInfo(Parcel parcel) {
        this.id = parcel.readString();
        this.flags = parcel.readInt();
        this.size = parcel.readLong();
        this.label = parcel.readString();
        this.volumeCount = parcel.readInt();
        this.sysPath = parcel.readString();
    }

    public String getId() {
        return this.id;
    }

    private boolean isInteresting(String str) {
        return (TextUtils.isEmpty(str) || str.equalsIgnoreCase("ata") || str.toLowerCase().contains("generic") || str.toLowerCase().startsWith(Context.USB_SERVICE) || str.toLowerCase().startsWith("multiple")) ? false : true;
    }

    public String getDescription() {
        Resources system = Resources.getSystem();
        if ((this.flags & 4) != 0) {
            if (isInteresting(this.label)) {
                return system.getString(R.string.storage_sd_card_label, this.label);
            }
            return system.getString(R.string.storage_sd_card);
        }
        if ((this.flags & 8) != 0) {
            if (isInteresting(this.label)) {
                return system.getString(R.string.storage_usb_drive_label, this.label);
            }
            return system.getString(R.string.storage_usb_drive);
        }
        return null;
    }

    public String getShortDescription() {
        Resources system = Resources.getSystem();
        if (isSd()) {
            return system.getString(R.string.storage_sd_card);
        }
        if (isUsb()) {
            return system.getString(R.string.storage_usb_drive);
        }
        return null;
    }

    public boolean isAdoptable() {
        return (this.flags & 1) != 0;
    }

    public boolean isDefaultPrimary() {
        return (this.flags & 2) != 0;
    }

    public boolean isSd() {
        return (this.flags & 4) != 0;
    }

    public boolean isUsb() {
        return (this.flags & 8) != 0;
    }

    public String toString() {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        dump(new IndentingPrintWriter(charArrayWriter, "    ", 80));
        return charArrayWriter.toString();
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("DiskInfo{" + this.id + "}:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.printPair("flags", DebugUtils.flagsToString(getClass(), "FLAG_", this.flags));
        indentingPrintWriter.printPair(DumpHeapActivity.KEY_SIZE, Long.valueOf(this.size));
        indentingPrintWriter.printPair("label", this.label);
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("sysPath", this.sysPath);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
    }

    public DiskInfo m26clone() {
        Parcel parcelObtain = Parcel.obtain();
        try {
            writeToParcel(parcelObtain, 0);
            parcelObtain.setDataPosition(0);
            return CREATOR.createFromParcel(parcelObtain);
        } finally {
            parcelObtain.recycle();
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof DiskInfo) {
            return Objects.equals(this.id, ((DiskInfo) obj).id);
        }
        return false;
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.id);
        parcel.writeInt(this.flags);
        parcel.writeLong(this.size);
        parcel.writeString(this.label);
        parcel.writeInt(this.volumeCount);
        parcel.writeString(this.sysPath);
    }
}
