package android.print;

import android.os.Parcel;
import android.os.Parcelable;
import android.print.PrintAttributes;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public final class PrinterCapabilitiesInfo implements Parcelable {
    public static final int DEFAULT_UNDEFINED = -1;
    private static final int PROPERTY_COLOR_MODE = 2;
    private static final int PROPERTY_COUNT = 4;
    private static final int PROPERTY_DUPLEX_MODE = 3;
    private static final int PROPERTY_MEDIA_SIZE = 0;
    private static final int PROPERTY_RESOLUTION = 1;
    private int mColorModes;
    private final int[] mDefaults;
    private int mDuplexModes;
    private List<PrintAttributes.MediaSize> mMediaSizes;
    private PrintAttributes.Margins mMinMargins;
    private List<PrintAttributes.Resolution> mResolutions;
    private static final PrintAttributes.Margins DEFAULT_MARGINS = new PrintAttributes.Margins(0, 0, 0, 0);
    public static final Parcelable.Creator<PrinterCapabilitiesInfo> CREATOR = new Parcelable.Creator<PrinterCapabilitiesInfo>() {
        @Override
        public PrinterCapabilitiesInfo createFromParcel(Parcel parcel) {
            return new PrinterCapabilitiesInfo(parcel);
        }

        @Override
        public PrinterCapabilitiesInfo[] newArray(int i) {
            return new PrinterCapabilitiesInfo[i];
        }
    };

    public PrinterCapabilitiesInfo() {
        this.mMinMargins = DEFAULT_MARGINS;
        this.mDefaults = new int[4];
        Arrays.fill(this.mDefaults, -1);
    }

    public PrinterCapabilitiesInfo(PrinterCapabilitiesInfo printerCapabilitiesInfo) {
        this.mMinMargins = DEFAULT_MARGINS;
        this.mDefaults = new int[4];
        copyFrom(printerCapabilitiesInfo);
    }

    public void copyFrom(PrinterCapabilitiesInfo printerCapabilitiesInfo) {
        if (this == printerCapabilitiesInfo) {
            return;
        }
        this.mMinMargins = printerCapabilitiesInfo.mMinMargins;
        if (printerCapabilitiesInfo.mMediaSizes != null) {
            if (this.mMediaSizes != null) {
                this.mMediaSizes.clear();
                this.mMediaSizes.addAll(printerCapabilitiesInfo.mMediaSizes);
            } else {
                this.mMediaSizes = new ArrayList(printerCapabilitiesInfo.mMediaSizes);
            }
        } else {
            this.mMediaSizes = null;
        }
        if (printerCapabilitiesInfo.mResolutions != null) {
            if (this.mResolutions != null) {
                this.mResolutions.clear();
                this.mResolutions.addAll(printerCapabilitiesInfo.mResolutions);
            } else {
                this.mResolutions = new ArrayList(printerCapabilitiesInfo.mResolutions);
            }
        } else {
            this.mResolutions = null;
        }
        this.mColorModes = printerCapabilitiesInfo.mColorModes;
        this.mDuplexModes = printerCapabilitiesInfo.mDuplexModes;
        int length = printerCapabilitiesInfo.mDefaults.length;
        for (int i = 0; i < length; i++) {
            this.mDefaults[i] = printerCapabilitiesInfo.mDefaults[i];
        }
    }

    public List<PrintAttributes.MediaSize> getMediaSizes() {
        return Collections.unmodifiableList(this.mMediaSizes);
    }

    public List<PrintAttributes.Resolution> getResolutions() {
        return Collections.unmodifiableList(this.mResolutions);
    }

    public PrintAttributes.Margins getMinMargins() {
        return this.mMinMargins;
    }

    public int getColorModes() {
        return this.mColorModes;
    }

    public int getDuplexModes() {
        return this.mDuplexModes;
    }

    public PrintAttributes getDefaults() {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setMinMargins(this.mMinMargins);
        int i = this.mDefaults[0];
        if (i >= 0) {
            builder.setMediaSize(this.mMediaSizes.get(i));
        }
        int i2 = this.mDefaults[1];
        if (i2 >= 0) {
            builder.setResolution(this.mResolutions.get(i2));
        }
        int i3 = this.mDefaults[2];
        if (i3 > 0) {
            builder.setColorMode(i3);
        }
        int i4 = this.mDefaults[3];
        if (i4 > 0) {
            builder.setDuplexMode(i4);
        }
        return builder.build();
    }

    private static void enforceValidMask(int i, IntConsumer intConsumer) {
        while (i > 0) {
            int iNumberOfTrailingZeros = 1 << Integer.numberOfTrailingZeros(i);
            i &= ~iNumberOfTrailingZeros;
            intConsumer.accept(iNumberOfTrailingZeros);
        }
    }

    private PrinterCapabilitiesInfo(Parcel parcel) {
        this.mMinMargins = DEFAULT_MARGINS;
        this.mDefaults = new int[4];
        this.mMinMargins = (PrintAttributes.Margins) Preconditions.checkNotNull(readMargins(parcel));
        readMediaSizes(parcel);
        readResolutions(parcel);
        this.mColorModes = parcel.readInt();
        enforceValidMask(this.mColorModes, new IntConsumer() {
            @Override
            public final void accept(int i) {
                PrintAttributes.enforceValidColorMode(i);
            }
        });
        this.mDuplexModes = parcel.readInt();
        enforceValidMask(this.mDuplexModes, new IntConsumer() {
            @Override
            public final void accept(int i) {
                PrintAttributes.enforceValidDuplexMode(i);
            }
        });
        readDefaults(parcel);
        Preconditions.checkArgument(this.mMediaSizes.size() > this.mDefaults[0]);
        Preconditions.checkArgument(this.mResolutions.size() > this.mDefaults[1]);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeMargins(this.mMinMargins, parcel);
        writeMediaSizes(parcel);
        writeResolutions(parcel);
        parcel.writeInt(this.mColorModes);
        parcel.writeInt(this.mDuplexModes);
        writeDefaults(parcel);
    }

    public int hashCode() {
        return (31 * ((((((((((this.mMinMargins == null ? 0 : this.mMinMargins.hashCode()) + 31) * 31) + (this.mMediaSizes == null ? 0 : this.mMediaSizes.hashCode())) * 31) + (this.mResolutions != null ? this.mResolutions.hashCode() : 0)) * 31) + this.mColorModes) * 31) + this.mDuplexModes)) + Arrays.hashCode(this.mDefaults);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrinterCapabilitiesInfo printerCapabilitiesInfo = (PrinterCapabilitiesInfo) obj;
        if (this.mMinMargins == null) {
            if (printerCapabilitiesInfo.mMinMargins != null) {
                return false;
            }
        } else if (!this.mMinMargins.equals(printerCapabilitiesInfo.mMinMargins)) {
            return false;
        }
        if (this.mMediaSizes == null) {
            if (printerCapabilitiesInfo.mMediaSizes != null) {
                return false;
            }
        } else if (!this.mMediaSizes.equals(printerCapabilitiesInfo.mMediaSizes)) {
            return false;
        }
        if (this.mResolutions == null) {
            if (printerCapabilitiesInfo.mResolutions != null) {
                return false;
            }
        } else if (!this.mResolutions.equals(printerCapabilitiesInfo.mResolutions)) {
            return false;
        }
        if (this.mColorModes == printerCapabilitiesInfo.mColorModes && this.mDuplexModes == printerCapabilitiesInfo.mDuplexModes && Arrays.equals(this.mDefaults, printerCapabilitiesInfo.mDefaults)) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "PrinterInfo{minMargins=" + this.mMinMargins + ", mediaSizes=" + this.mMediaSizes + ", resolutions=" + this.mResolutions + ", colorModes=" + colorModesToString() + ", duplexModes=" + duplexModesToString() + "\"}";
    }

    private String colorModesToString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = this.mColorModes;
        while (i != 0) {
            int iNumberOfTrailingZeros = 1 << Integer.numberOfTrailingZeros(i);
            i &= ~iNumberOfTrailingZeros;
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(PrintAttributes.colorModeToString(iNumberOfTrailingZeros));
        }
        sb.append(']');
        return sb.toString();
    }

    private String duplexModesToString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = this.mDuplexModes;
        while (i != 0) {
            int iNumberOfTrailingZeros = 1 << Integer.numberOfTrailingZeros(i);
            i &= ~iNumberOfTrailingZeros;
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(PrintAttributes.duplexModeToString(iNumberOfTrailingZeros));
        }
        sb.append(']');
        return sb.toString();
    }

    private void writeMediaSizes(Parcel parcel) {
        if (this.mMediaSizes == null) {
            parcel.writeInt(0);
            return;
        }
        int size = this.mMediaSizes.size();
        parcel.writeInt(size);
        for (int i = 0; i < size; i++) {
            this.mMediaSizes.get(i).writeToParcel(parcel);
        }
    }

    private void readMediaSizes(Parcel parcel) {
        int i = parcel.readInt();
        if (i > 0 && this.mMediaSizes == null) {
            this.mMediaSizes = new ArrayList();
        }
        for (int i2 = 0; i2 < i; i2++) {
            this.mMediaSizes.add(PrintAttributes.MediaSize.createFromParcel(parcel));
        }
    }

    private void writeResolutions(Parcel parcel) {
        if (this.mResolutions == null) {
            parcel.writeInt(0);
            return;
        }
        int size = this.mResolutions.size();
        parcel.writeInt(size);
        for (int i = 0; i < size; i++) {
            this.mResolutions.get(i).writeToParcel(parcel);
        }
    }

    private void readResolutions(Parcel parcel) {
        int i = parcel.readInt();
        if (i > 0 && this.mResolutions == null) {
            this.mResolutions = new ArrayList();
        }
        for (int i2 = 0; i2 < i; i2++) {
            this.mResolutions.add(PrintAttributes.Resolution.createFromParcel(parcel));
        }
    }

    private void writeMargins(PrintAttributes.Margins margins, Parcel parcel) {
        if (margins == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            margins.writeToParcel(parcel);
        }
    }

    private PrintAttributes.Margins readMargins(Parcel parcel) {
        if (parcel.readInt() == 1) {
            return PrintAttributes.Margins.createFromParcel(parcel);
        }
        return null;
    }

    private void readDefaults(Parcel parcel) {
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.mDefaults[i2] = parcel.readInt();
        }
    }

    private void writeDefaults(Parcel parcel) {
        int length = this.mDefaults.length;
        parcel.writeInt(length);
        for (int i = 0; i < length; i++) {
            parcel.writeInt(this.mDefaults[i]);
        }
    }

    public static final class Builder {
        private final PrinterCapabilitiesInfo mPrototype;

        public Builder(PrinterId printerId) {
            if (printerId == null) {
                throw new IllegalArgumentException("printerId cannot be null.");
            }
            this.mPrototype = new PrinterCapabilitiesInfo();
        }

        public Builder addMediaSize(PrintAttributes.MediaSize mediaSize, boolean z) {
            if (this.mPrototype.mMediaSizes == null) {
                this.mPrototype.mMediaSizes = new ArrayList();
            }
            int size = this.mPrototype.mMediaSizes.size();
            this.mPrototype.mMediaSizes.add(mediaSize);
            if (z) {
                throwIfDefaultAlreadySpecified(0);
                this.mPrototype.mDefaults[0] = size;
            }
            return this;
        }

        public Builder addResolution(PrintAttributes.Resolution resolution, boolean z) {
            if (this.mPrototype.mResolutions == null) {
                this.mPrototype.mResolutions = new ArrayList();
            }
            int size = this.mPrototype.mResolutions.size();
            this.mPrototype.mResolutions.add(resolution);
            if (z) {
                throwIfDefaultAlreadySpecified(1);
                this.mPrototype.mDefaults[1] = size;
            }
            return this;
        }

        public Builder setMinMargins(PrintAttributes.Margins margins) {
            if (margins != null) {
                this.mPrototype.mMinMargins = margins;
                return this;
            }
            throw new IllegalArgumentException("margins cannot be null");
        }

        public Builder setColorModes(int i, int i2) {
            PrinterCapabilitiesInfo.enforceValidMask(i, new IntConsumer() {
                @Override
                public final void accept(int i3) {
                    PrintAttributes.enforceValidColorMode(i3);
                }
            });
            PrintAttributes.enforceValidColorMode(i2);
            this.mPrototype.mColorModes = i;
            this.mPrototype.mDefaults[2] = i2;
            return this;
        }

        public Builder setDuplexModes(int i, int i2) {
            PrinterCapabilitiesInfo.enforceValidMask(i, new IntConsumer() {
                @Override
                public final void accept(int i3) {
                    PrintAttributes.enforceValidDuplexMode(i3);
                }
            });
            PrintAttributes.enforceValidDuplexMode(i2);
            this.mPrototype.mDuplexModes = i;
            this.mPrototype.mDefaults[3] = i2;
            return this;
        }

        public PrinterCapabilitiesInfo build() {
            if (this.mPrototype.mMediaSizes != null && !this.mPrototype.mMediaSizes.isEmpty()) {
                if (this.mPrototype.mDefaults[0] != -1) {
                    if (this.mPrototype.mResolutions != null && !this.mPrototype.mResolutions.isEmpty()) {
                        if (this.mPrototype.mDefaults[1] != -1) {
                            if (this.mPrototype.mColorModes != 0) {
                                if (this.mPrototype.mDefaults[2] != -1) {
                                    if (this.mPrototype.mDuplexModes == 0) {
                                        setDuplexModes(1, 1);
                                    }
                                    if (this.mPrototype.mMinMargins == null) {
                                        throw new IllegalArgumentException("margins cannot be null");
                                    }
                                    return this.mPrototype;
                                }
                                throw new IllegalStateException("No default color mode specified.");
                            }
                            throw new IllegalStateException("No color mode specified.");
                        }
                        throw new IllegalStateException("No default resolution specified.");
                    }
                    throw new IllegalStateException("No resolution specified.");
                }
                throw new IllegalStateException("No default media size specified.");
            }
            throw new IllegalStateException("No media size specified.");
        }

        private void throwIfDefaultAlreadySpecified(int i) {
            if (this.mPrototype.mDefaults[i] != -1) {
                throw new IllegalArgumentException("Default already specified.");
            }
        }
    }
}
