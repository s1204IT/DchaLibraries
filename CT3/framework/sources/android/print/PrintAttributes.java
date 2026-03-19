package android.print;

import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothInputDevice;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.Map;

public final class PrintAttributes implements Parcelable {
    public static final int COLOR_MODE_COLOR = 2;
    public static final int COLOR_MODE_MONOCHROME = 1;
    public static final Parcelable.Creator<PrintAttributes> CREATOR = new Parcelable.Creator<PrintAttributes>() {
        @Override
        public PrintAttributes createFromParcel(Parcel parcel) {
            return new PrintAttributes(parcel, null);
        }

        @Override
        public PrintAttributes[] newArray(int size) {
            return new PrintAttributes[size];
        }
    };
    public static final int DUPLEX_MODE_LONG_EDGE = 2;
    public static final int DUPLEX_MODE_NONE = 1;
    public static final int DUPLEX_MODE_SHORT_EDGE = 4;
    private static final int VALID_COLOR_MODES = 3;
    private static final int VALID_DUPLEX_MODES = 7;
    private int mColorMode;
    private int mDuplexMode;
    private MediaSize mMediaSize;
    private Margins mMinMargins;
    private Resolution mResolution;

    PrintAttributes(Parcel parcel, PrintAttributes printAttributes) {
        this(parcel);
    }

    PrintAttributes() {
    }

    private PrintAttributes(Parcel parcel) {
        this.mMediaSize = parcel.readInt() == 1 ? MediaSize.createFromParcel(parcel) : null;
        this.mResolution = parcel.readInt() == 1 ? Resolution.createFromParcel(parcel) : null;
        this.mMinMargins = parcel.readInt() == 1 ? Margins.createFromParcel(parcel) : null;
        this.mColorMode = parcel.readInt();
        if (this.mColorMode != 0) {
            enforceValidColorMode(this.mColorMode);
        }
        this.mDuplexMode = parcel.readInt();
        if (this.mDuplexMode == 0) {
            return;
        }
        enforceValidDuplexMode(this.mDuplexMode);
    }

    public MediaSize getMediaSize() {
        return this.mMediaSize;
    }

    public void setMediaSize(MediaSize mediaSize) {
        this.mMediaSize = mediaSize;
    }

    public Resolution getResolution() {
        return this.mResolution;
    }

    public void setResolution(Resolution resolution) {
        this.mResolution = resolution;
    }

    public Margins getMinMargins() {
        return this.mMinMargins;
    }

    public void setMinMargins(Margins margins) {
        this.mMinMargins = margins;
    }

    public int getColorMode() {
        return this.mColorMode;
    }

    public void setColorMode(int colorMode) {
        enforceValidColorMode(colorMode);
        this.mColorMode = colorMode;
    }

    public boolean isPortrait() {
        return this.mMediaSize.isPortrait();
    }

    public int getDuplexMode() {
        return this.mDuplexMode;
    }

    public void setDuplexMode(int duplexMode) {
        enforceValidDuplexMode(duplexMode);
        this.mDuplexMode = duplexMode;
    }

    public PrintAttributes asPortrait() {
        if (isPortrait()) {
            return this;
        }
        PrintAttributes attributes = new PrintAttributes();
        attributes.setMediaSize(getMediaSize().asPortrait());
        Resolution oldResolution = getResolution();
        Resolution newResolution = new Resolution(oldResolution.getId(), oldResolution.getLabel(), oldResolution.getVerticalDpi(), oldResolution.getHorizontalDpi());
        attributes.setResolution(newResolution);
        attributes.setMinMargins(getMinMargins());
        attributes.setColorMode(getColorMode());
        attributes.setDuplexMode(getDuplexMode());
        return attributes;
    }

    public PrintAttributes asLandscape() {
        if (!isPortrait()) {
            return this;
        }
        PrintAttributes attributes = new PrintAttributes();
        attributes.setMediaSize(getMediaSize().asLandscape());
        Resolution oldResolution = getResolution();
        Resolution newResolution = new Resolution(oldResolution.getId(), oldResolution.getLabel(), oldResolution.getVerticalDpi(), oldResolution.getHorizontalDpi());
        attributes.setResolution(newResolution);
        attributes.setMinMargins(getMinMargins());
        attributes.setColorMode(getColorMode());
        attributes.setDuplexMode(getDuplexMode());
        return attributes;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        if (this.mMediaSize != null) {
            parcel.writeInt(1);
            this.mMediaSize.writeToParcel(parcel);
        } else {
            parcel.writeInt(0);
        }
        if (this.mResolution != null) {
            parcel.writeInt(1);
            this.mResolution.writeToParcel(parcel);
        } else {
            parcel.writeInt(0);
        }
        if (this.mMinMargins != null) {
            parcel.writeInt(1);
            this.mMinMargins.writeToParcel(parcel);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mColorMode);
        parcel.writeInt(this.mDuplexMode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int hashCode() {
        int result = this.mColorMode + 31;
        return (((((((result * 31) + this.mDuplexMode) * 31) + (this.mMinMargins == null ? 0 : this.mMinMargins.hashCode())) * 31) + (this.mMediaSize == null ? 0 : this.mMediaSize.hashCode())) * 31) + (this.mResolution != null ? this.mResolution.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrintAttributes other = (PrintAttributes) obj;
        if (this.mColorMode != other.mColorMode || this.mDuplexMode != other.mDuplexMode) {
            return false;
        }
        if (this.mMinMargins == null) {
            if (other.mMinMargins != null) {
                return false;
            }
        } else if (!this.mMinMargins.equals(other.mMinMargins)) {
            return false;
        }
        if (this.mMediaSize == null) {
            if (other.mMediaSize != null) {
                return false;
            }
        } else if (!this.mMediaSize.equals(other.mMediaSize)) {
            return false;
        }
        if (this.mResolution == null) {
            if (other.mResolution != null) {
                return false;
            }
        } else if (!this.mResolution.equals(other.mResolution)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrintAttributes{");
        builder.append("mediaSize: ").append(this.mMediaSize);
        if (this.mMediaSize != null) {
            builder.append(", orientation: ").append(this.mMediaSize.isPortrait() ? Camera.Parameters.SCENE_MODE_PORTRAIT : Camera.Parameters.SCENE_MODE_LANDSCAPE);
        } else {
            builder.append(", orientation: ").append("null");
        }
        builder.append(", resolution: ").append(this.mResolution);
        builder.append(", minMargins: ").append(this.mMinMargins);
        builder.append(", colorMode: ").append(colorModeToString(this.mColorMode));
        builder.append(", duplexMode: ").append(duplexModeToString(this.mDuplexMode));
        builder.append("}");
        return builder.toString();
    }

    public void clear() {
        this.mMediaSize = null;
        this.mResolution = null;
        this.mMinMargins = null;
        this.mColorMode = 0;
        this.mDuplexMode = 0;
    }

    public void copyFrom(PrintAttributes other) {
        this.mMediaSize = other.mMediaSize;
        this.mResolution = other.mResolution;
        this.mMinMargins = other.mMinMargins;
        this.mColorMode = other.mColorMode;
        this.mDuplexMode = other.mDuplexMode;
    }

    public static final class MediaSize {
        private static final String LOG_TAG = "MediaSize";
        private final int mHeightMils;
        private final String mId;
        public final String mLabel;
        public final int mLabelResId;
        public final String mPackageName;
        private final int mWidthMils;
        private static final Map<String, MediaSize> sIdToMediaSizeMap = new ArrayMap();
        public static final MediaSize UNKNOWN_PORTRAIT = new MediaSize("UNKNOWN_PORTRAIT", ZenModeConfig.SYSTEM_AUTHORITY, 17040767, 1, Integer.MAX_VALUE);
        public static final MediaSize UNKNOWN_LANDSCAPE = new MediaSize("UNKNOWN_LANDSCAPE", ZenModeConfig.SYSTEM_AUTHORITY, 17040768, Integer.MAX_VALUE, 1);
        public static final MediaSize ISO_A0 = new MediaSize("ISO_A0", ZenModeConfig.SYSTEM_AUTHORITY, 17040686, 33110, 46810);
        public static final MediaSize ISO_A1 = new MediaSize("ISO_A1", ZenModeConfig.SYSTEM_AUTHORITY, 17040687, 23390, 33110);
        public static final MediaSize ISO_A2 = new MediaSize("ISO_A2", ZenModeConfig.SYSTEM_AUTHORITY, 17040688, 16540, 23390);
        public static final MediaSize ISO_A3 = new MediaSize("ISO_A3", ZenModeConfig.SYSTEM_AUTHORITY, 17040689, 11690, 16540);
        public static final MediaSize ISO_A4 = new MediaSize("ISO_A4", ZenModeConfig.SYSTEM_AUTHORITY, 17040690, 8270, 11690);
        public static final MediaSize ISO_A5 = new MediaSize("ISO_A5", ZenModeConfig.SYSTEM_AUTHORITY, 17040691, 5830, 8270);
        public static final MediaSize ISO_A6 = new MediaSize("ISO_A6", ZenModeConfig.SYSTEM_AUTHORITY, 17040692, 4130, 5830);
        public static final MediaSize ISO_A7 = new MediaSize("ISO_A7", ZenModeConfig.SYSTEM_AUTHORITY, 17040693, 2910, 4130);
        public static final MediaSize ISO_A8 = new MediaSize("ISO_A8", ZenModeConfig.SYSTEM_AUTHORITY, 17040694, 2050, 2910);
        public static final MediaSize ISO_A9 = new MediaSize("ISO_A9", ZenModeConfig.SYSTEM_AUTHORITY, 17040695, 1460, 2050);
        public static final MediaSize ISO_A10 = new MediaSize("ISO_A10", ZenModeConfig.SYSTEM_AUTHORITY, 17040696, AudioFormat.CHANNEL_OUT_7POINT1, 1460);
        public static final MediaSize ISO_B0 = new MediaSize("ISO_B0", ZenModeConfig.SYSTEM_AUTHORITY, 17040697, 39370, 55670);
        public static final MediaSize ISO_B1 = new MediaSize("ISO_B1", ZenModeConfig.SYSTEM_AUTHORITY, 17040698, 27830, 39370);
        public static final MediaSize ISO_B2 = new MediaSize("ISO_B2", ZenModeConfig.SYSTEM_AUTHORITY, 17040699, 19690, 27830);
        public static final MediaSize ISO_B3 = new MediaSize("ISO_B3", ZenModeConfig.SYSTEM_AUTHORITY, 17040700, 13900, 19690);
        public static final MediaSize ISO_B4 = new MediaSize("ISO_B4", ZenModeConfig.SYSTEM_AUTHORITY, 17040701, 9840, 13900);
        public static final MediaSize ISO_B5 = new MediaSize("ISO_B5", ZenModeConfig.SYSTEM_AUTHORITY, 17040702, 6930, 9840);
        public static final MediaSize ISO_B6 = new MediaSize("ISO_B6", ZenModeConfig.SYSTEM_AUTHORITY, 17040703, 4920, 6930);
        public static final MediaSize ISO_B7 = new MediaSize("ISO_B7", ZenModeConfig.SYSTEM_AUTHORITY, 17040704, 3460, 4920);
        public static final MediaSize ISO_B8 = new MediaSize("ISO_B8", ZenModeConfig.SYSTEM_AUTHORITY, 17040705, 2440, 3460);
        public static final MediaSize ISO_B9 = new MediaSize("ISO_B9", ZenModeConfig.SYSTEM_AUTHORITY, 17040706, 1730, 2440);
        public static final MediaSize ISO_B10 = new MediaSize("ISO_B10", ZenModeConfig.SYSTEM_AUTHORITY, 17040707, 1220, 1730);
        public static final MediaSize ISO_C0 = new MediaSize("ISO_C0", ZenModeConfig.SYSTEM_AUTHORITY, 17040708, 36100, 51060);
        public static final MediaSize ISO_C1 = new MediaSize("ISO_C1", ZenModeConfig.SYSTEM_AUTHORITY, 17040709, 25510, 36100);
        public static final MediaSize ISO_C2 = new MediaSize("ISO_C2", ZenModeConfig.SYSTEM_AUTHORITY, 17040710, 18030, 25510);
        public static final MediaSize ISO_C3 = new MediaSize("ISO_C3", ZenModeConfig.SYSTEM_AUTHORITY, 17040711, 12760, 18030);
        public static final MediaSize ISO_C4 = new MediaSize("ISO_C4", ZenModeConfig.SYSTEM_AUTHORITY, 17040712, 9020, 12760);
        public static final MediaSize ISO_C5 = new MediaSize("ISO_C5", ZenModeConfig.SYSTEM_AUTHORITY, 17040713, 6380, 9020);
        public static final MediaSize ISO_C6 = new MediaSize("ISO_C6", ZenModeConfig.SYSTEM_AUTHORITY, 17040714, 4490, 6380);
        public static final MediaSize ISO_C7 = new MediaSize("ISO_C7", ZenModeConfig.SYSTEM_AUTHORITY, 17040715, 3190, 4490);
        public static final MediaSize ISO_C8 = new MediaSize("ISO_C8", ZenModeConfig.SYSTEM_AUTHORITY, 17040716, 2240, 3190);
        public static final MediaSize ISO_C9 = new MediaSize("ISO_C9", ZenModeConfig.SYSTEM_AUTHORITY, 17040717, 1570, 2240);
        public static final MediaSize ISO_C10 = new MediaSize("ISO_C10", ZenModeConfig.SYSTEM_AUTHORITY, 17040718, MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE, 1570);
        public static final MediaSize NA_LETTER = new MediaSize("NA_LETTER", ZenModeConfig.SYSTEM_AUTHORITY, 17040719, 8500, 11000);
        public static final MediaSize NA_GOVT_LETTER = new MediaSize("NA_GOVT_LETTER", ZenModeConfig.SYSTEM_AUTHORITY, 17040720, 8000, 10500);
        public static final MediaSize NA_LEGAL = new MediaSize("NA_LEGAL", ZenModeConfig.SYSTEM_AUTHORITY, 17040721, 8500, 14000);
        public static final MediaSize NA_JUNIOR_LEGAL = new MediaSize("NA_JUNIOR_LEGAL", ZenModeConfig.SYSTEM_AUTHORITY, 17040722, 8000, BluetoothInputDevice.INPUT_DISCONNECT_FAILED_NOT_CONNECTED);
        public static final MediaSize NA_LEDGER = new MediaSize("NA_LEDGER", ZenModeConfig.SYSTEM_AUTHORITY, 17040723, 17000, 11000);
        public static final MediaSize NA_TABLOID = new MediaSize("NA_TABLOID", ZenModeConfig.SYSTEM_AUTHORITY, 17040724, 11000, 17000);
        public static final MediaSize NA_INDEX_3X5 = new MediaSize("NA_INDEX_3X5", ZenModeConfig.SYSTEM_AUTHORITY, 17040725, 3000, BluetoothInputDevice.INPUT_DISCONNECT_FAILED_NOT_CONNECTED);
        public static final MediaSize NA_INDEX_4X6 = new MediaSize("NA_INDEX_4X6", ZenModeConfig.SYSTEM_AUTHORITY, 17040726, AudioFormat.SAMPLE_RATE_HZ_MIN, BluetoothHealth.HEALTH_OPERATION_SUCCESS);
        public static final MediaSize NA_INDEX_5X8 = new MediaSize("NA_INDEX_5X8", ZenModeConfig.SYSTEM_AUTHORITY, 17040727, BluetoothInputDevice.INPUT_DISCONNECT_FAILED_NOT_CONNECTED, 8000);
        public static final MediaSize NA_MONARCH = new MediaSize("NA_MONARCH", ZenModeConfig.SYSTEM_AUTHORITY, 17040728, 7250, 10500);
        public static final MediaSize NA_QUARTO = new MediaSize("NA_QUARTO", ZenModeConfig.SYSTEM_AUTHORITY, 17040729, 8000, 10000);
        public static final MediaSize NA_FOOLSCAP = new MediaSize("NA_FOOLSCAP", ZenModeConfig.SYSTEM_AUTHORITY, 17040730, 8000, 13000);
        public static final MediaSize ROC_8K = new MediaSize("ROC_8K", ZenModeConfig.SYSTEM_AUTHORITY, 17040731, 10629, 15354);
        public static final MediaSize ROC_16K = new MediaSize("ROC_16K", ZenModeConfig.SYSTEM_AUTHORITY, 17040732, 7677, 10629);
        public static final MediaSize PRC_1 = new MediaSize("PRC_1", ZenModeConfig.SYSTEM_AUTHORITY, 17040733, 4015, 6496);
        public static final MediaSize PRC_2 = new MediaSize("PRC_2", ZenModeConfig.SYSTEM_AUTHORITY, 17040734, 4015, 6929);
        public static final MediaSize PRC_3 = new MediaSize("PRC_3", ZenModeConfig.SYSTEM_AUTHORITY, 17040735, 4921, 6929);
        public static final MediaSize PRC_4 = new MediaSize("PRC_4", ZenModeConfig.SYSTEM_AUTHORITY, 17040736, 4330, 8189);
        public static final MediaSize PRC_5 = new MediaSize("PRC_5", ZenModeConfig.SYSTEM_AUTHORITY, 17040737, 4330, 8661);
        public static final MediaSize PRC_6 = new MediaSize("PRC_6", ZenModeConfig.SYSTEM_AUTHORITY, 17040738, 4724, 12599);
        public static final MediaSize PRC_7 = new MediaSize("PRC_7", ZenModeConfig.SYSTEM_AUTHORITY, 17040739, 6299, 9055);
        public static final MediaSize PRC_8 = new MediaSize("PRC_8", ZenModeConfig.SYSTEM_AUTHORITY, 17040740, 4724, 12165);
        public static final MediaSize PRC_9 = new MediaSize("PRC_9", ZenModeConfig.SYSTEM_AUTHORITY, 17040741, 9016, 12756);
        public static final MediaSize PRC_10 = new MediaSize("PRC_10", ZenModeConfig.SYSTEM_AUTHORITY, 17040742, 12756, 18032);
        public static final MediaSize PRC_16K = new MediaSize("PRC_16K", ZenModeConfig.SYSTEM_AUTHORITY, 17040743, 5749, 8465);
        public static final MediaSize OM_PA_KAI = new MediaSize("OM_PA_KAI", ZenModeConfig.SYSTEM_AUTHORITY, 17040744, 10512, 15315);
        public static final MediaSize OM_DAI_PA_KAI = new MediaSize("OM_DAI_PA_KAI", ZenModeConfig.SYSTEM_AUTHORITY, 17040745, 10827, 15551);
        public static final MediaSize OM_JUURO_KU_KAI = new MediaSize("OM_JUURO_KU_KAI", ZenModeConfig.SYSTEM_AUTHORITY, 17040746, 7796, 10827);
        public static final MediaSize JIS_B10 = new MediaSize("JIS_B10", ZenModeConfig.SYSTEM_AUTHORITY, 17040747, 1259, 1772);
        public static final MediaSize JIS_B9 = new MediaSize("JIS_B9", ZenModeConfig.SYSTEM_AUTHORITY, 17040748, 1772, 2520);
        public static final MediaSize JIS_B8 = new MediaSize("JIS_B8", ZenModeConfig.SYSTEM_AUTHORITY, 17040749, 2520, 3583);
        public static final MediaSize JIS_B7 = new MediaSize("JIS_B7", ZenModeConfig.SYSTEM_AUTHORITY, 17040750, 3583, 5049);
        public static final MediaSize JIS_B6 = new MediaSize("JIS_B6", ZenModeConfig.SYSTEM_AUTHORITY, 17040751, 5049, 7165);
        public static final MediaSize JIS_B5 = new MediaSize("JIS_B5", ZenModeConfig.SYSTEM_AUTHORITY, 17040752, 7165, 10118);
        public static final MediaSize JIS_B4 = new MediaSize("JIS_B4", ZenModeConfig.SYSTEM_AUTHORITY, 17040753, 10118, 14331);
        public static final MediaSize JIS_B3 = new MediaSize("JIS_B3", ZenModeConfig.SYSTEM_AUTHORITY, 17040754, 14331, 20276);
        public static final MediaSize JIS_B2 = new MediaSize("JIS_B2", ZenModeConfig.SYSTEM_AUTHORITY, 17040755, 20276, 28661);
        public static final MediaSize JIS_B1 = new MediaSize("JIS_B1", ZenModeConfig.SYSTEM_AUTHORITY, 17040756, 28661, 40551);
        public static final MediaSize JIS_B0 = new MediaSize("JIS_B0", ZenModeConfig.SYSTEM_AUTHORITY, 17040757, 40551, 57323);
        public static final MediaSize JIS_EXEC = new MediaSize("JIS_EXEC", ZenModeConfig.SYSTEM_AUTHORITY, 17040758, 8504, 12992);
        public static final MediaSize JPN_CHOU4 = new MediaSize("JPN_CHOU4", ZenModeConfig.SYSTEM_AUTHORITY, 17040759, 3543, 8071);
        public static final MediaSize JPN_CHOU3 = new MediaSize("JPN_CHOU3", ZenModeConfig.SYSTEM_AUTHORITY, 17040760, 4724, 9252);
        public static final MediaSize JPN_CHOU2 = new MediaSize("JPN_CHOU2", ZenModeConfig.SYSTEM_AUTHORITY, 17040761, 4374, 5748);
        public static final MediaSize JPN_HAGAKI = new MediaSize("JPN_HAGAKI", ZenModeConfig.SYSTEM_AUTHORITY, 17040762, 3937, 5827);
        public static final MediaSize JPN_OUFUKU = new MediaSize("JPN_OUFUKU", ZenModeConfig.SYSTEM_AUTHORITY, 17040763, 5827, 7874);
        public static final MediaSize JPN_KAHU = new MediaSize("JPN_KAHU", ZenModeConfig.SYSTEM_AUTHORITY, 17040764, 9449, 12681);
        public static final MediaSize JPN_KAKU2 = new MediaSize("JPN_KAKU2", ZenModeConfig.SYSTEM_AUTHORITY, 17040765, 9449, 13071);
        public static final MediaSize JPN_YOU4 = new MediaSize("JPN_YOU4", ZenModeConfig.SYSTEM_AUTHORITY, 17040766, 4134, 9252);

        public MediaSize(String id, String packageName, int labelResId, int widthMils, int heightMils) {
            this(id, null, packageName, widthMils, heightMils, labelResId);
            sIdToMediaSizeMap.put(this.mId, this);
        }

        public MediaSize(String id, String label, int widthMils, int heightMils) {
            this(id, label, null, widthMils, heightMils, 0);
        }

        public static ArraySet<MediaSize> getAllPredefinedSizes() {
            ArraySet<MediaSize> definedMediaSizes = new ArraySet<>(sIdToMediaSizeMap.values());
            definedMediaSizes.remove(UNKNOWN_PORTRAIT);
            definedMediaSizes.remove(UNKNOWN_LANDSCAPE);
            return definedMediaSizes;
        }

        public MediaSize(String id, String label, String packageName, int widthMils, int heightMils, int labelResId) {
            this.mPackageName = packageName;
            this.mId = (String) Preconditions.checkStringNotEmpty(id, "id cannot be empty.");
            this.mLabelResId = labelResId;
            this.mWidthMils = Preconditions.checkArgumentPositive(widthMils, "widthMils cannot be less than or equal to zero.");
            this.mHeightMils = Preconditions.checkArgumentPositive(heightMils, "heightMils cannot be less than or equal to zero.");
            this.mLabel = label;
            Preconditions.checkArgument((!TextUtils.isEmpty(label)) != (!TextUtils.isEmpty(packageName) && labelResId != 0), "label cannot be empty.");
        }

        public String getId() {
            return this.mId;
        }

        public String getLabel(PackageManager packageManager) {
            if (!TextUtils.isEmpty(this.mPackageName) && this.mLabelResId > 0) {
                try {
                    return packageManager.getResourcesForApplication(this.mPackageName).getString(this.mLabelResId);
                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                    Log.w(LOG_TAG, "Could not load resouce" + this.mLabelResId + " from package " + this.mPackageName);
                }
            }
            return this.mLabel;
        }

        public int getWidthMils() {
            return this.mWidthMils;
        }

        public int getHeightMils() {
            return this.mHeightMils;
        }

        public boolean isPortrait() {
            return this.mHeightMils >= this.mWidthMils;
        }

        public MediaSize asPortrait() {
            if (isPortrait()) {
                return this;
            }
            return new MediaSize(this.mId, this.mLabel, this.mPackageName, Math.min(this.mWidthMils, this.mHeightMils), Math.max(this.mWidthMils, this.mHeightMils), this.mLabelResId);
        }

        public MediaSize asLandscape() {
            if (!isPortrait()) {
                return this;
            }
            return new MediaSize(this.mId, this.mLabel, this.mPackageName, Math.max(this.mWidthMils, this.mHeightMils), Math.min(this.mWidthMils, this.mHeightMils), this.mLabelResId);
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeString(this.mId);
            parcel.writeString(this.mLabel);
            parcel.writeString(this.mPackageName);
            parcel.writeInt(this.mWidthMils);
            parcel.writeInt(this.mHeightMils);
            parcel.writeInt(this.mLabelResId);
        }

        static MediaSize createFromParcel(Parcel parcel) {
            return new MediaSize(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        public int hashCode() {
            int result = this.mWidthMils + 31;
            return (result * 31) + this.mHeightMils;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MediaSize other = (MediaSize) obj;
            return this.mWidthMils == other.mWidthMils && this.mHeightMils == other.mHeightMils;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MediaSize{");
            builder.append("id: ").append(this.mId);
            builder.append(", label: ").append(this.mLabel);
            builder.append(", packageName: ").append(this.mPackageName);
            builder.append(", heightMils: ").append(this.mHeightMils);
            builder.append(", widthMils: ").append(this.mWidthMils);
            builder.append(", labelResId: ").append(this.mLabelResId);
            builder.append("}");
            return builder.toString();
        }

        public static MediaSize getStandardMediaSizeById(String id) {
            return sIdToMediaSizeMap.get(id);
        }
    }

    public static final class Resolution {
        private final int mHorizontalDpi;
        private final String mId;
        private final String mLabel;
        private final int mVerticalDpi;

        public Resolution(String id, String label, int horizontalDpi, int verticalDpi) {
            if (TextUtils.isEmpty(id)) {
                throw new IllegalArgumentException("id cannot be empty.");
            }
            if (TextUtils.isEmpty(label)) {
                throw new IllegalArgumentException("label cannot be empty.");
            }
            if (horizontalDpi <= 0) {
                throw new IllegalArgumentException("horizontalDpi cannot be less than or equal to zero.");
            }
            if (verticalDpi <= 0) {
                throw new IllegalArgumentException("verticalDpi cannot be less than or equal to zero.");
            }
            this.mId = id;
            this.mLabel = label;
            this.mHorizontalDpi = horizontalDpi;
            this.mVerticalDpi = verticalDpi;
        }

        public String getId() {
            return this.mId;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public int getHorizontalDpi() {
            return this.mHorizontalDpi;
        }

        public int getVerticalDpi() {
            return this.mVerticalDpi;
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeString(this.mId);
            parcel.writeString(this.mLabel);
            parcel.writeInt(this.mHorizontalDpi);
            parcel.writeInt(this.mVerticalDpi);
        }

        static Resolution createFromParcel(Parcel parcel) {
            return new Resolution(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt());
        }

        public int hashCode() {
            int result = this.mHorizontalDpi + 31;
            return (result * 31) + this.mVerticalDpi;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Resolution other = (Resolution) obj;
            return this.mHorizontalDpi == other.mHorizontalDpi && this.mVerticalDpi == other.mVerticalDpi;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Resolution{");
            builder.append("id: ").append(this.mId);
            builder.append(", label: ").append(this.mLabel);
            builder.append(", horizontalDpi: ").append(this.mHorizontalDpi);
            builder.append(", verticalDpi: ").append(this.mVerticalDpi);
            builder.append("}");
            return builder.toString();
        }
    }

    public static final class Margins {
        public static final Margins NO_MARGINS = new Margins(0, 0, 0, 0);
        private final int mBottomMils;
        private final int mLeftMils;
        private final int mRightMils;
        private final int mTopMils;

        public Margins(int leftMils, int topMils, int rightMils, int bottomMils) {
            this.mTopMils = topMils;
            this.mLeftMils = leftMils;
            this.mRightMils = rightMils;
            this.mBottomMils = bottomMils;
        }

        public int getLeftMils() {
            return this.mLeftMils;
        }

        public int getTopMils() {
            return this.mTopMils;
        }

        public int getRightMils() {
            return this.mRightMils;
        }

        public int getBottomMils() {
            return this.mBottomMils;
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeInt(this.mLeftMils);
            parcel.writeInt(this.mTopMils);
            parcel.writeInt(this.mRightMils);
            parcel.writeInt(this.mBottomMils);
        }

        static Margins createFromParcel(Parcel parcel) {
            return new Margins(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        public int hashCode() {
            int result = this.mBottomMils + 31;
            return (((((result * 31) + this.mLeftMils) * 31) + this.mRightMils) * 31) + this.mTopMils;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Margins other = (Margins) obj;
            return this.mBottomMils == other.mBottomMils && this.mLeftMils == other.mLeftMils && this.mRightMils == other.mRightMils && this.mTopMils == other.mTopMils;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Margins{");
            builder.append("leftMils: ").append(this.mLeftMils);
            builder.append(", topMils: ").append(this.mTopMils);
            builder.append(", rightMils: ").append(this.mRightMils);
            builder.append(", bottomMils: ").append(this.mBottomMils);
            builder.append("}");
            return builder.toString();
        }
    }

    static String colorModeToString(int colorMode) {
        switch (colorMode) {
            case 1:
                return "COLOR_MODE_MONOCHROME";
            case 2:
                return "COLOR_MODE_COLOR";
            default:
                return "COLOR_MODE_UNKNOWN";
        }
    }

    static String duplexModeToString(int duplexMode) {
        switch (duplexMode) {
            case 1:
                return "DUPLEX_MODE_NONE";
            case 2:
                return "DUPLEX_MODE_LONG_EDGE";
            case 3:
            default:
                return "DUPLEX_MODE_UNKNOWN";
            case 4:
                return "DUPLEX_MODE_SHORT_EDGE";
        }
    }

    static void enforceValidColorMode(int colorMode) {
        if ((colorMode & 3) != 0 && Integer.bitCount(colorMode) == 1) {
        } else {
            throw new IllegalArgumentException("invalid color mode: " + colorMode);
        }
    }

    static void enforceValidDuplexMode(int duplexMode) {
        if ((duplexMode & 7) != 0 && Integer.bitCount(duplexMode) == 1) {
        } else {
            throw new IllegalArgumentException("invalid duplex mode: " + duplexMode);
        }
    }

    public static final class Builder {
        private final PrintAttributes mAttributes = new PrintAttributes();

        public Builder setMediaSize(MediaSize mediaSize) {
            this.mAttributes.setMediaSize(mediaSize);
            return this;
        }

        public Builder setResolution(Resolution resolution) {
            this.mAttributes.setResolution(resolution);
            return this;
        }

        public Builder setMinMargins(Margins margins) {
            this.mAttributes.setMinMargins(margins);
            return this;
        }

        public Builder setColorMode(int colorMode) {
            this.mAttributes.setColorMode(colorMode);
            return this;
        }

        public Builder setDuplexMode(int duplexMode) {
            this.mAttributes.setDuplexMode(duplexMode);
            return this;
        }

        public PrintAttributes build() {
            return this.mAttributes;
        }
    }
}
