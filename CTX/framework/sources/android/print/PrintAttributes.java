package android.print;

import android.bluetooth.BluetoothHealth;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public final class PrintAttributes implements Parcelable {
    public static final int COLOR_MODE_COLOR = 2;
    public static final int COLOR_MODE_MONOCHROME = 1;
    public static final Parcelable.Creator<PrintAttributes> CREATOR = new Parcelable.Creator<PrintAttributes>() {
        @Override
        public PrintAttributes createFromParcel(Parcel parcel) {
            return new PrintAttributes(parcel);
        }

        @Override
        public PrintAttributes[] newArray(int i) {
            return new PrintAttributes[i];
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

    @Retention(RetentionPolicy.SOURCE)
    @interface ColorMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface DuplexMode {
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
        if (this.mDuplexMode != 0) {
            enforceValidDuplexMode(this.mDuplexMode);
        }
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

    public void setColorMode(int i) {
        enforceValidColorMode(i);
        this.mColorMode = i;
    }

    public boolean isPortrait() {
        return this.mMediaSize.isPortrait();
    }

    public int getDuplexMode() {
        return this.mDuplexMode;
    }

    public void setDuplexMode(int i) {
        enforceValidDuplexMode(i);
        this.mDuplexMode = i;
    }

    public PrintAttributes asPortrait() {
        if (isPortrait()) {
            return this;
        }
        PrintAttributes printAttributes = new PrintAttributes();
        printAttributes.setMediaSize(getMediaSize().asPortrait());
        Resolution resolution = getResolution();
        printAttributes.setResolution(new Resolution(resolution.getId(), resolution.getLabel(), resolution.getVerticalDpi(), resolution.getHorizontalDpi()));
        printAttributes.setMinMargins(getMinMargins());
        printAttributes.setColorMode(getColorMode());
        printAttributes.setDuplexMode(getDuplexMode());
        return printAttributes;
    }

    public PrintAttributes asLandscape() {
        if (!isPortrait()) {
            return this;
        }
        PrintAttributes printAttributes = new PrintAttributes();
        printAttributes.setMediaSize(getMediaSize().asLandscape());
        Resolution resolution = getResolution();
        printAttributes.setResolution(new Resolution(resolution.getId(), resolution.getLabel(), resolution.getVerticalDpi(), resolution.getHorizontalDpi()));
        printAttributes.setMinMargins(getMinMargins());
        printAttributes.setColorMode(getColorMode());
        printAttributes.setDuplexMode(getDuplexMode());
        return printAttributes;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
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
        return (31 * (((((((this.mColorMode + 31) * 31) + this.mDuplexMode) * 31) + (this.mMinMargins == null ? 0 : this.mMinMargins.hashCode())) * 31) + (this.mMediaSize == null ? 0 : this.mMediaSize.hashCode()))) + (this.mResolution != null ? this.mResolution.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrintAttributes printAttributes = (PrintAttributes) obj;
        if (this.mColorMode != printAttributes.mColorMode || this.mDuplexMode != printAttributes.mDuplexMode) {
            return false;
        }
        if (this.mMinMargins == null) {
            if (printAttributes.mMinMargins != null) {
                return false;
            }
        } else if (!this.mMinMargins.equals(printAttributes.mMinMargins)) {
            return false;
        }
        if (this.mMediaSize == null) {
            if (printAttributes.mMediaSize != null) {
                return false;
            }
        } else if (!this.mMediaSize.equals(printAttributes.mMediaSize)) {
            return false;
        }
        if (this.mResolution == null) {
            if (printAttributes.mResolution != null) {
                return false;
            }
        } else if (!this.mResolution.equals(printAttributes.mResolution)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PrintAttributes{");
        sb.append("mediaSize: ");
        sb.append(this.mMediaSize);
        if (this.mMediaSize != null) {
            sb.append(", orientation: ");
            sb.append(this.mMediaSize.isPortrait() ? Camera.Parameters.SCENE_MODE_PORTRAIT : Camera.Parameters.SCENE_MODE_LANDSCAPE);
        } else {
            sb.append(", orientation: ");
            sb.append("null");
        }
        sb.append(", resolution: ");
        sb.append(this.mResolution);
        sb.append(", minMargins: ");
        sb.append(this.mMinMargins);
        sb.append(", colorMode: ");
        sb.append(colorModeToString(this.mColorMode));
        sb.append(", duplexMode: ");
        sb.append(duplexModeToString(this.mDuplexMode));
        sb.append("}");
        return sb.toString();
    }

    public void clear() {
        this.mMediaSize = null;
        this.mResolution = null;
        this.mMinMargins = null;
        this.mColorMode = 0;
        this.mDuplexMode = 0;
    }

    public void copyFrom(PrintAttributes printAttributes) {
        this.mMediaSize = printAttributes.mMediaSize;
        this.mResolution = printAttributes.mResolution;
        this.mMinMargins = printAttributes.mMinMargins;
        this.mColorMode = printAttributes.mColorMode;
        this.mDuplexMode = printAttributes.mDuplexMode;
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
        public static final MediaSize UNKNOWN_PORTRAIT = new MediaSize("UNKNOWN_PORTRAIT", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_unknown_portrait, 1, Integer.MAX_VALUE);
        public static final MediaSize UNKNOWN_LANDSCAPE = new MediaSize("UNKNOWN_LANDSCAPE", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_unknown_landscape, Integer.MAX_VALUE, 1);
        public static final MediaSize ISO_A0 = new MediaSize("ISO_A0", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a0, 33110, 46810);
        public static final MediaSize ISO_A1 = new MediaSize("ISO_A1", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a1, 23390, 33110);
        public static final MediaSize ISO_A2 = new MediaSize("ISO_A2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a2, 16540, 23390);
        public static final MediaSize ISO_A3 = new MediaSize("ISO_A3", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a3, 11690, 16540);
        public static final MediaSize ISO_A4 = new MediaSize("ISO_A4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a4, 8270, 11690);
        public static final MediaSize ISO_A5 = new MediaSize("ISO_A5", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a5, 5830, 8270);
        public static final MediaSize ISO_A6 = new MediaSize("ISO_A6", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a6, 4130, 5830);
        public static final MediaSize ISO_A7 = new MediaSize("ISO_A7", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a7, 2910, 4130);
        public static final MediaSize ISO_A8 = new MediaSize("ISO_A8", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a8, 2050, 2910);
        public static final MediaSize ISO_A9 = new MediaSize("ISO_A9", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a9, 1460, 2050);
        public static final MediaSize ISO_A10 = new MediaSize("ISO_A10", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_a10, 1020, 1460);
        public static final MediaSize ISO_B0 = new MediaSize("ISO_B0", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b0, 39370, 55670);
        public static final MediaSize ISO_B1 = new MediaSize("ISO_B1", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b1, 27830, 39370);
        public static final MediaSize ISO_B2 = new MediaSize("ISO_B2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b2, 19690, 27830);
        public static final MediaSize ISO_B3 = new MediaSize("ISO_B3", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b3, 13900, 19690);
        public static final MediaSize ISO_B4 = new MediaSize("ISO_B4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b4, 9840, 13900);
        public static final MediaSize ISO_B5 = new MediaSize("ISO_B5", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b5, 6930, 9840);
        public static final MediaSize ISO_B6 = new MediaSize("ISO_B6", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b6, 4920, 6930);
        public static final MediaSize ISO_B7 = new MediaSize("ISO_B7", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b7, 3460, 4920);
        public static final MediaSize ISO_B8 = new MediaSize("ISO_B8", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b8, 2440, 3460);
        public static final MediaSize ISO_B9 = new MediaSize("ISO_B9", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b9, 1730, 2440);
        public static final MediaSize ISO_B10 = new MediaSize("ISO_B10", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_b10, 1220, 1730);
        public static final MediaSize ISO_C0 = new MediaSize("ISO_C0", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c0, 36100, 51060);
        public static final MediaSize ISO_C1 = new MediaSize("ISO_C1", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c1, 25510, 36100);
        public static final MediaSize ISO_C2 = new MediaSize("ISO_C2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c2, 18030, 25510);
        public static final MediaSize ISO_C3 = new MediaSize("ISO_C3", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c3, 12760, 18030);
        public static final MediaSize ISO_C4 = new MediaSize("ISO_C4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c4, 9020, 12760);
        public static final MediaSize ISO_C5 = new MediaSize("ISO_C5", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c5, 6380, 9020);
        public static final MediaSize ISO_C6 = new MediaSize("ISO_C6", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c6, 4490, 6380);
        public static final MediaSize ISO_C7 = new MediaSize("ISO_C7", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c7, 3190, 4490);
        public static final MediaSize ISO_C8 = new MediaSize("ISO_C8", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c8, 2240, 3190);
        public static final MediaSize ISO_C9 = new MediaSize("ISO_C9", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c9, 1570, 2240);
        public static final MediaSize ISO_C10 = new MediaSize("ISO_C10", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_iso_c10, 1100, 1570);
        public static final MediaSize NA_LETTER = new MediaSize("NA_LETTER", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_letter, 8500, 11000);
        public static final MediaSize NA_GOVT_LETTER = new MediaSize("NA_GOVT_LETTER", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_gvrnmt_letter, 8000, 10500);
        public static final MediaSize NA_LEGAL = new MediaSize("NA_LEGAL", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_legal, 8500, 14000);
        public static final MediaSize NA_JUNIOR_LEGAL = new MediaSize("NA_JUNIOR_LEGAL", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_junior_legal, 8000, 5000);
        public static final MediaSize NA_LEDGER = new MediaSize("NA_LEDGER", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_ledger, 17000, 11000);
        public static final MediaSize NA_TABLOID = new MediaSize("NA_TABLOID", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_tabloid, 11000, 17000);
        public static final MediaSize NA_INDEX_3X5 = new MediaSize("NA_INDEX_3X5", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_index_3x5, 3000, 5000);
        public static final MediaSize NA_INDEX_4X6 = new MediaSize("NA_INDEX_4X6", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_index_4x6, 4000, BluetoothHealth.HEALTH_OPERATION_SUCCESS);
        public static final MediaSize NA_INDEX_5X8 = new MediaSize("NA_INDEX_5X8", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_index_5x8, 5000, 8000);
        public static final MediaSize NA_MONARCH = new MediaSize("NA_MONARCH", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_monarch, 7250, 10500);
        public static final MediaSize NA_QUARTO = new MediaSize("NA_QUARTO", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_quarto, 8000, 10000);
        public static final MediaSize NA_FOOLSCAP = new MediaSize("NA_FOOLSCAP", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_na_foolscap, 8000, 13000);
        public static final MediaSize ROC_8K = new MediaSize("ROC_8K", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_roc_8k, 10629, 15354);
        public static final MediaSize ROC_16K = new MediaSize("ROC_16K", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_roc_16k, 7677, 10629);
        public static final MediaSize PRC_1 = new MediaSize("PRC_1", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_1, 4015, 6496);
        public static final MediaSize PRC_2 = new MediaSize("PRC_2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_2, 4015, 6929);
        public static final MediaSize PRC_3 = new MediaSize("PRC_3", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_3, 4921, 6929);
        public static final MediaSize PRC_4 = new MediaSize("PRC_4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_4, 4330, 8189);
        public static final MediaSize PRC_5 = new MediaSize("PRC_5", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_5, 4330, 8661);
        public static final MediaSize PRC_6 = new MediaSize("PRC_6", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_6, 4724, 12599);
        public static final MediaSize PRC_7 = new MediaSize("PRC_7", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_7, 6299, 9055);
        public static final MediaSize PRC_8 = new MediaSize("PRC_8", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_8, 4724, 12165);
        public static final MediaSize PRC_9 = new MediaSize("PRC_9", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_9, 9016, 12756);
        public static final MediaSize PRC_10 = new MediaSize("PRC_10", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_10, 12756, 18032);
        public static final MediaSize PRC_16K = new MediaSize("PRC_16K", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_prc_16k, 5749, 8465);
        public static final MediaSize OM_PA_KAI = new MediaSize("OM_PA_KAI", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_om_pa_kai, 10512, 15315);
        public static final MediaSize OM_DAI_PA_KAI = new MediaSize("OM_DAI_PA_KAI", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_om_dai_pa_kai, 10827, 15551);
        public static final MediaSize OM_JUURO_KU_KAI = new MediaSize("OM_JUURO_KU_KAI", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_chinese_om_jurro_ku_kai, 7796, 10827);
        public static final MediaSize JIS_B10 = new MediaSize("JIS_B10", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b10, MetricsProto.MetricsEvent.NOTIFICATION_ZEN_MODE_TOGGLE_ON_FOREVER, 1772);
        public static final MediaSize JIS_B9 = new MediaSize("JIS_B9", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b9, 1772, 2520);
        public static final MediaSize JIS_B8 = new MediaSize("JIS_B8", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b8, 2520, 3583);
        public static final MediaSize JIS_B7 = new MediaSize("JIS_B7", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b7, 3583, 5049);
        public static final MediaSize JIS_B6 = new MediaSize("JIS_B6", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b6, 5049, 7165);
        public static final MediaSize JIS_B5 = new MediaSize("JIS_B5", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b5, 7165, 10118);
        public static final MediaSize JIS_B4 = new MediaSize("JIS_B4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b4, 10118, 14331);
        public static final MediaSize JIS_B3 = new MediaSize("JIS_B3", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b3, 14331, 20276);
        public static final MediaSize JIS_B2 = new MediaSize("JIS_B2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b2, 20276, 28661);
        public static final MediaSize JIS_B1 = new MediaSize("JIS_B1", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b1, 28661, 40551);
        public static final MediaSize JIS_B0 = new MediaSize("JIS_B0", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_b0, 40551, 57323);
        public static final MediaSize JIS_EXEC = new MediaSize("JIS_EXEC", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_jis_exec, 8504, 12992);
        public static final MediaSize JPN_CHOU4 = new MediaSize("JPN_CHOU4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_chou4, 3543, 8071);
        public static final MediaSize JPN_CHOU3 = new MediaSize("JPN_CHOU3", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_chou3, 4724, 9252);
        public static final MediaSize JPN_CHOU2 = new MediaSize("JPN_CHOU2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_chou2, SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY, 5748);
        public static final MediaSize JPN_HAGAKI = new MediaSize("JPN_HAGAKI", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_hagaki, 3937, 5827);
        public static final MediaSize JPN_OUFUKU = new MediaSize("JPN_OUFUKU", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_oufuku, 5827, 7874);
        public static final MediaSize JPN_KAHU = new MediaSize("JPN_KAHU", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_kahu, 9449, 12681);
        public static final MediaSize JPN_KAKU2 = new MediaSize("JPN_KAKU2", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_kaku2, 9449, 13071);
        public static final MediaSize JPN_YOU4 = new MediaSize("JPN_YOU4", ZenModeConfig.SYSTEM_AUTHORITY, R.string.mediasize_japanese_you4, 4134, 9252);

        public MediaSize(String str, String str2, int i, int i2, int i3) {
            this(str, null, str2, i2, i3, i);
            sIdToMediaSizeMap.put(this.mId, this);
        }

        public MediaSize(String str, String str2, int i, int i2) {
            this(str, str2, null, i, i2, 0);
        }

        public static ArraySet<MediaSize> getAllPredefinedSizes() {
            ArraySet<MediaSize> arraySet = new ArraySet<>(sIdToMediaSizeMap.values());
            arraySet.remove(UNKNOWN_PORTRAIT);
            arraySet.remove(UNKNOWN_LANDSCAPE);
            return arraySet;
        }

        public MediaSize(String str, String str2, String str3, int i, int i2, int i3) {
            this.mPackageName = str3;
            this.mId = (String) Preconditions.checkStringNotEmpty(str, "id cannot be empty.");
            this.mLabelResId = i3;
            this.mWidthMils = Preconditions.checkArgumentPositive(i, "widthMils cannot be less than or equal to zero.");
            this.mHeightMils = Preconditions.checkArgumentPositive(i2, "heightMils cannot be less than or equal to zero.");
            this.mLabel = str2;
            Preconditions.checkArgument((TextUtils.isEmpty(str2) ^ true) != (!TextUtils.isEmpty(str3) && i3 != 0), "label cannot be empty.");
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
            return (31 * (this.mWidthMils + 31)) + this.mHeightMils;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MediaSize mediaSize = (MediaSize) obj;
            if (this.mWidthMils == mediaSize.mWidthMils && this.mHeightMils == mediaSize.mHeightMils) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "MediaSize{id: " + this.mId + ", label: " + this.mLabel + ", packageName: " + this.mPackageName + ", heightMils: " + this.mHeightMils + ", widthMils: " + this.mWidthMils + ", labelResId: " + this.mLabelResId + "}";
        }

        public static MediaSize getStandardMediaSizeById(String str) {
            return sIdToMediaSizeMap.get(str);
        }
    }

    public static final class Resolution {
        private final int mHorizontalDpi;
        private final String mId;
        private final String mLabel;
        private final int mVerticalDpi;

        public Resolution(String str, String str2, int i, int i2) {
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException("id cannot be empty.");
            }
            if (TextUtils.isEmpty(str2)) {
                throw new IllegalArgumentException("label cannot be empty.");
            }
            if (i <= 0) {
                throw new IllegalArgumentException("horizontalDpi cannot be less than or equal to zero.");
            }
            if (i2 <= 0) {
                throw new IllegalArgumentException("verticalDpi cannot be less than or equal to zero.");
            }
            this.mId = str;
            this.mLabel = str2;
            this.mHorizontalDpi = i;
            this.mVerticalDpi = i2;
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
            return (31 * (this.mHorizontalDpi + 31)) + this.mVerticalDpi;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Resolution resolution = (Resolution) obj;
            if (this.mHorizontalDpi == resolution.mHorizontalDpi && this.mVerticalDpi == resolution.mVerticalDpi) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "Resolution{id: " + this.mId + ", label: " + this.mLabel + ", horizontalDpi: " + this.mHorizontalDpi + ", verticalDpi: " + this.mVerticalDpi + "}";
        }
    }

    public static final class Margins {
        public static final Margins NO_MARGINS = new Margins(0, 0, 0, 0);
        private final int mBottomMils;
        private final int mLeftMils;
        private final int mRightMils;
        private final int mTopMils;

        public Margins(int i, int i2, int i3, int i4) {
            this.mTopMils = i2;
            this.mLeftMils = i;
            this.mRightMils = i3;
            this.mBottomMils = i4;
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
            return (31 * (((((this.mBottomMils + 31) * 31) + this.mLeftMils) * 31) + this.mRightMils)) + this.mTopMils;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Margins margins = (Margins) obj;
            if (this.mBottomMils == margins.mBottomMils && this.mLeftMils == margins.mLeftMils && this.mRightMils == margins.mRightMils && this.mTopMils == margins.mTopMils) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "Margins{leftMils: " + this.mLeftMils + ", topMils: " + this.mTopMils + ", rightMils: " + this.mRightMils + ", bottomMils: " + this.mBottomMils + "}";
        }
    }

    static String colorModeToString(int i) {
        switch (i) {
            case 1:
                return "COLOR_MODE_MONOCHROME";
            case 2:
                return "COLOR_MODE_COLOR";
            default:
                return "COLOR_MODE_UNKNOWN";
        }
    }

    static String duplexModeToString(int i) {
        if (i != 4) {
            switch (i) {
                case 1:
                    return "DUPLEX_MODE_NONE";
                case 2:
                    return "DUPLEX_MODE_LONG_EDGE";
                default:
                    return "DUPLEX_MODE_UNKNOWN";
            }
        }
        return "DUPLEX_MODE_SHORT_EDGE";
    }

    static void enforceValidColorMode(int i) {
        if ((i & 3) == 0 || Integer.bitCount(i) != 1) {
            throw new IllegalArgumentException("invalid color mode: " + i);
        }
    }

    static void enforceValidDuplexMode(int i) {
        if ((i & 7) == 0 || Integer.bitCount(i) != 1) {
            throw new IllegalArgumentException("invalid duplex mode: " + i);
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

        public Builder setColorMode(int i) {
            this.mAttributes.setColorMode(i);
            return this;
        }

        public Builder setDuplexMode(int i) {
            this.mAttributes.setDuplexMode(i);
            return this;
        }

        public PrintAttributes build() {
            return this.mAttributes;
        }
    }
}
