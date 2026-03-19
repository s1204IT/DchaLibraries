package android.content.pm;

import android.os.Parcel;
import java.util.ArrayList;
import java.util.HashMap;

public class PackageParserCacheHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "PackageParserCacheHelper";

    private PackageParserCacheHelper() {
    }

    public static class ReadHelper extends Parcel.ReadWriteHelper {
        private final Parcel mParcel;
        private final ArrayList<String> mStrings = new ArrayList<>();

        public ReadHelper(Parcel parcel) {
            this.mParcel = parcel;
        }

        public void startAndInstall() {
            this.mStrings.clear();
            int i = this.mParcel.readInt();
            int iDataPosition = this.mParcel.dataPosition();
            this.mParcel.setDataPosition(i);
            this.mParcel.readStringList(this.mStrings);
            this.mParcel.setDataPosition(iDataPosition);
            this.mParcel.setReadWriteHelper(this);
        }

        @Override
        public String readString(Parcel parcel) {
            return this.mStrings.get(parcel.readInt());
        }
    }

    public static class WriteHelper extends Parcel.ReadWriteHelper {
        private final Parcel mParcel;
        private final int mStartPos;
        private final ArrayList<String> mStrings = new ArrayList<>();
        private final HashMap<String, Integer> mIndexes = new HashMap<>();

        public WriteHelper(Parcel parcel) {
            this.mParcel = parcel;
            this.mStartPos = parcel.dataPosition();
            this.mParcel.writeInt(0);
            this.mParcel.setReadWriteHelper(this);
        }

        @Override
        public void writeString(Parcel parcel, String str) {
            Integer num = this.mIndexes.get(str);
            if (num != null) {
                parcel.writeInt(num.intValue());
                return;
            }
            int size = this.mStrings.size();
            this.mIndexes.put(str, Integer.valueOf(size));
            this.mStrings.add(str);
            parcel.writeInt(size);
        }

        public void finishAndUninstall() {
            this.mParcel.setReadWriteHelper(null);
            int iDataPosition = this.mParcel.dataPosition();
            this.mParcel.writeStringList(this.mStrings);
            this.mParcel.setDataPosition(this.mStartPos);
            this.mParcel.writeInt(iDataPosition);
            this.mParcel.setDataPosition(this.mParcel.dataSize());
        }
    }
}
