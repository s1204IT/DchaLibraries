package android.print;

import android.os.Parcel;
import android.os.Parcelable;
import android.print.PrintAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PrinterCapabilitiesInfo implements Parcelable {
    public static final int DEFAULT_UNDEFINED = -1;
    private static final int PROPERTY_COLOR_MODE = 2;
    private static final int PROPERTY_COUNT = 3;
    private static final int PROPERTY_MEDIA_SIZE = 0;
    private static final int PROPERTY_RESOLUTION = 1;
    private int mColorModes;
    private final int[] mDefaults;
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
        public PrinterCapabilitiesInfo[] newArray(int size) {
            return new PrinterCapabilitiesInfo[size];
        }
    };

    public PrinterCapabilitiesInfo() {
        this.mMinMargins = DEFAULT_MARGINS;
        this.mDefaults = new int[3];
        Arrays.fill(this.mDefaults, -1);
    }

    public PrinterCapabilitiesInfo(PrinterCapabilitiesInfo prototype) {
        this.mMinMargins = DEFAULT_MARGINS;
        this.mDefaults = new int[3];
        copyFrom(prototype);
    }

    public void copyFrom(PrinterCapabilitiesInfo other) {
        if (this != other) {
            this.mMinMargins = other.mMinMargins;
            if (other.mMediaSizes != null) {
                if (this.mMediaSizes != null) {
                    this.mMediaSizes.clear();
                    this.mMediaSizes.addAll(other.mMediaSizes);
                } else {
                    this.mMediaSizes = new ArrayList(other.mMediaSizes);
                }
            } else {
                this.mMediaSizes = null;
            }
            if (other.mResolutions != null) {
                if (this.mResolutions != null) {
                    this.mResolutions.clear();
                    this.mResolutions.addAll(other.mResolutions);
                } else {
                    this.mResolutions = new ArrayList(other.mResolutions);
                }
            } else {
                this.mResolutions = null;
            }
            this.mColorModes = other.mColorModes;
            int defaultCount = other.mDefaults.length;
            for (int i = 0; i < defaultCount; i++) {
                this.mDefaults[i] = other.mDefaults[i];
            }
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

    public PrintAttributes getDefaults() {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setMinMargins(this.mMinMargins);
        int mediaSizeIndex = this.mDefaults[0];
        if (mediaSizeIndex >= 0) {
            builder.setMediaSize(this.mMediaSizes.get(mediaSizeIndex));
        }
        int resolutionIndex = this.mDefaults[1];
        if (resolutionIndex >= 0) {
            builder.setResolution(this.mResolutions.get(resolutionIndex));
        }
        int colorMode = this.mDefaults[2];
        if (colorMode > 0) {
            builder.setColorMode(colorMode);
        }
        return builder.build();
    }

    private PrinterCapabilitiesInfo(Parcel parcel) {
        this.mMinMargins = DEFAULT_MARGINS;
        this.mDefaults = new int[3];
        this.mMinMargins = readMargins(parcel);
        readMediaSizes(parcel);
        readResolutions(parcel);
        this.mColorModes = parcel.readInt();
        readDefaults(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        writeMargins(this.mMinMargins, parcel);
        writeMediaSizes(parcel);
        writeResolutions(parcel);
        parcel.writeInt(this.mColorModes);
        writeDefaults(parcel);
    }

    public int hashCode() {
        int result = (this.mMinMargins == null ? 0 : this.mMinMargins.hashCode()) + 31;
        return (((((((result * 31) + (this.mMediaSizes == null ? 0 : this.mMediaSizes.hashCode())) * 31) + (this.mResolutions != null ? this.mResolutions.hashCode() : 0)) * 31) + this.mColorModes) * 31) + Arrays.hashCode(this.mDefaults);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            PrinterCapabilitiesInfo other = (PrinterCapabilitiesInfo) obj;
            if (this.mMinMargins == null) {
                if (other.mMinMargins != null) {
                    return false;
                }
            } else if (!this.mMinMargins.equals(other.mMinMargins)) {
                return false;
            }
            if (this.mMediaSizes == null) {
                if (other.mMediaSizes != null) {
                    return false;
                }
            } else if (!this.mMediaSizes.equals(other.mMediaSizes)) {
                return false;
            }
            if (this.mResolutions == null) {
                if (other.mResolutions != null) {
                    return false;
                }
            } else if (!this.mResolutions.equals(other.mResolutions)) {
                return false;
            }
            return this.mColorModes == other.mColorModes && Arrays.equals(this.mDefaults, other.mDefaults);
        }
        return false;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrinterInfo{");
        builder.append("minMargins=").append(this.mMinMargins);
        builder.append(", mediaSizes=").append(this.mMediaSizes);
        builder.append(", resolutions=").append(this.mResolutions);
        builder.append(", colorModes=").append(colorModesToString());
        builder.append("\"}");
        return builder.toString();
    }

    private String colorModesToString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        int colorModes = this.mColorModes;
        while (colorModes != 0) {
            int colorMode = 1 << Integer.numberOfTrailingZeros(colorModes);
            colorModes &= colorMode ^ (-1);
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(PrintAttributes.colorModeToString(colorMode));
        }
        builder.append(']');
        return builder.toString();
    }

    private void writeMediaSizes(Parcel parcel) {
        if (this.mMediaSizes == null) {
            parcel.writeInt(0);
            return;
        }
        int mediaSizeCount = this.mMediaSizes.size();
        parcel.writeInt(mediaSizeCount);
        for (int i = 0; i < mediaSizeCount; i++) {
            this.mMediaSizes.get(i).writeToParcel(parcel);
        }
    }

    private void readMediaSizes(Parcel parcel) {
        int mediaSizeCount = parcel.readInt();
        if (mediaSizeCount > 0 && this.mMediaSizes == null) {
            this.mMediaSizes = new ArrayList();
        }
        for (int i = 0; i < mediaSizeCount; i++) {
            this.mMediaSizes.add(PrintAttributes.MediaSize.createFromParcel(parcel));
        }
    }

    private void writeResolutions(Parcel parcel) {
        if (this.mResolutions == null) {
            parcel.writeInt(0);
            return;
        }
        int resolutionCount = this.mResolutions.size();
        parcel.writeInt(resolutionCount);
        for (int i = 0; i < resolutionCount; i++) {
            this.mResolutions.get(i).writeToParcel(parcel);
        }
    }

    private void readResolutions(Parcel parcel) {
        int resolutionCount = parcel.readInt();
        if (resolutionCount > 0 && this.mResolutions == null) {
            this.mResolutions = new ArrayList();
        }
        for (int i = 0; i < resolutionCount; i++) {
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
        int defaultCount = parcel.readInt();
        for (int i = 0; i < defaultCount; i++) {
            this.mDefaults[i] = parcel.readInt();
        }
    }

    private void writeDefaults(Parcel parcel) {
        int defaultCount = this.mDefaults.length;
        parcel.writeInt(defaultCount);
        for (int i = 0; i < defaultCount; i++) {
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

        public Builder addMediaSize(PrintAttributes.MediaSize mediaSize, boolean isDefault) {
            if (this.mPrototype.mMediaSizes == null) {
                this.mPrototype.mMediaSizes = new ArrayList();
            }
            int insertionIndex = this.mPrototype.mMediaSizes.size();
            this.mPrototype.mMediaSizes.add(mediaSize);
            if (isDefault) {
                throwIfDefaultAlreadySpecified(0);
                this.mPrototype.mDefaults[0] = insertionIndex;
            }
            return this;
        }

        public Builder addResolution(PrintAttributes.Resolution resolution, boolean isDefault) {
            if (this.mPrototype.mResolutions == null) {
                this.mPrototype.mResolutions = new ArrayList();
            }
            int insertionIndex = this.mPrototype.mResolutions.size();
            this.mPrototype.mResolutions.add(resolution);
            if (isDefault) {
                throwIfDefaultAlreadySpecified(1);
                this.mPrototype.mDefaults[1] = insertionIndex;
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

        public Builder setColorModes(int colorModes, int defaultColorMode) {
            int currentModes = colorModes;
            while (currentModes > 0) {
                int currentMode = 1 << Integer.numberOfTrailingZeros(currentModes);
                currentModes &= currentMode ^ (-1);
                PrintAttributes.enforceValidColorMode(currentMode);
            }
            if ((colorModes & defaultColorMode) == 0) {
                throw new IllegalArgumentException("Default color mode not in color modes.");
            }
            PrintAttributes.enforceValidColorMode(colorModes);
            this.mPrototype.mColorModes = colorModes;
            this.mPrototype.mDefaults[2] = defaultColorMode;
            return this;
        }

        public PrinterCapabilitiesInfo build() {
            if (this.mPrototype.mMediaSizes != null && !this.mPrototype.mMediaSizes.isEmpty()) {
                if (this.mPrototype.mDefaults[0] != -1) {
                    if (this.mPrototype.mResolutions != null && !this.mPrototype.mResolutions.isEmpty()) {
                        if (this.mPrototype.mDefaults[1] != -1) {
                            if (this.mPrototype.mColorModes != 0) {
                                if (this.mPrototype.mDefaults[2] != -1) {
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

        private void throwIfDefaultAlreadySpecified(int propertyIndex) {
            if (this.mPrototype.mDefaults[propertyIndex] != -1) {
                throw new IllegalArgumentException("Default already specified.");
            }
        }
    }
}
