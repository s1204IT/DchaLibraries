package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PasswordMetrics implements Parcelable {
    private static final int CHAR_DIGIT = 2;
    private static final int CHAR_LOWER_CASE = 0;
    private static final int CHAR_SYMBOL = 3;
    private static final int CHAR_UPPER_CASE = 1;
    public static final Parcelable.Creator<PasswordMetrics> CREATOR = new Parcelable.Creator<PasswordMetrics>() {
        @Override
        public PasswordMetrics createFromParcel(Parcel parcel) {
            return new PasswordMetrics(parcel);
        }

        @Override
        public PasswordMetrics[] newArray(int i) {
            return new PasswordMetrics[i];
        }
    };
    public static final int MAX_ALLOWED_SEQUENCE = 3;
    public int length;
    public int letters;
    public int lowerCase;
    public int nonLetter;
    public int numeric;
    public int quality;
    public int symbols;
    public int upperCase;

    @Retention(RetentionPolicy.SOURCE)
    private @interface CharacterCatagory {
    }

    public PasswordMetrics() {
        this.quality = 0;
        this.length = 0;
        this.letters = 0;
        this.upperCase = 0;
        this.lowerCase = 0;
        this.numeric = 0;
        this.symbols = 0;
        this.nonLetter = 0;
    }

    public PasswordMetrics(int i, int i2) {
        this.quality = 0;
        this.length = 0;
        this.letters = 0;
        this.upperCase = 0;
        this.lowerCase = 0;
        this.numeric = 0;
        this.symbols = 0;
        this.nonLetter = 0;
        this.quality = i;
        this.length = i2;
    }

    public PasswordMetrics(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this(i, i2);
        this.letters = i3;
        this.upperCase = i4;
        this.lowerCase = i5;
        this.numeric = i6;
        this.symbols = i7;
        this.nonLetter = i8;
    }

    private PasswordMetrics(Parcel parcel) {
        this.quality = 0;
        this.length = 0;
        this.letters = 0;
        this.upperCase = 0;
        this.lowerCase = 0;
        this.numeric = 0;
        this.symbols = 0;
        this.nonLetter = 0;
        this.quality = parcel.readInt();
        this.length = parcel.readInt();
        this.letters = parcel.readInt();
        this.upperCase = parcel.readInt();
        this.lowerCase = parcel.readInt();
        this.numeric = parcel.readInt();
        this.symbols = parcel.readInt();
        this.nonLetter = parcel.readInt();
    }

    public boolean isDefault() {
        return this.quality == 0 && this.length == 0 && this.letters == 0 && this.upperCase == 0 && this.lowerCase == 0 && this.numeric == 0 && this.symbols == 0 && this.nonLetter == 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.quality);
        parcel.writeInt(this.length);
        parcel.writeInt(this.letters);
        parcel.writeInt(this.upperCase);
        parcel.writeInt(this.lowerCase);
        parcel.writeInt(this.numeric);
        parcel.writeInt(this.symbols);
        parcel.writeInt(this.nonLetter);
    }

    public static PasswordMetrics computeForPassword(String str) {
        int i;
        int i2;
        int length = str.length();
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        for (int i9 = 0; i9 < length; i9++) {
            switch (categoryChar(str.charAt(i9))) {
                case 0:
                    i3++;
                    i5++;
                    break;
                case 1:
                    i3++;
                    i4++;
                    break;
                case 2:
                    i6++;
                    i8++;
                    break;
                case 3:
                    i7++;
                    i8++;
                    break;
            }
        }
        boolean z = true;
        boolean z2 = i6 > 0;
        if (i3 + i7 <= 0) {
            z = false;
        }
        if (z && z2) {
            i2 = 327680;
        } else if (z) {
            i2 = 262144;
        } else if (z2) {
            if (maxLengthSequence(str) > 3) {
                i2 = 131072;
            } else {
                i2 = 196608;
            }
        } else {
            i = 0;
            return new PasswordMetrics(i, length, i3, i4, i5, i6, i7, i8);
        }
        i = i2;
        return new PasswordMetrics(i, length, i3, i4, i5, i6, i7, i8);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PasswordMetrics)) {
            return false;
        }
        PasswordMetrics passwordMetrics = (PasswordMetrics) obj;
        return this.quality == passwordMetrics.quality && this.length == passwordMetrics.length && this.letters == passwordMetrics.letters && this.upperCase == passwordMetrics.upperCase && this.lowerCase == passwordMetrics.lowerCase && this.numeric == passwordMetrics.numeric && this.symbols == passwordMetrics.symbols && this.nonLetter == passwordMetrics.nonLetter;
    }

    public static int maxLengthSequence(String str) {
        if (str.length() == 0) {
            return 0;
        }
        char cCharAt = str.charAt(0);
        int iMax = 0;
        int i = 0;
        boolean z = false;
        int i2 = 0;
        int iCategoryChar = categoryChar(cCharAt);
        char c = cCharAt;
        int i3 = 1;
        while (i3 < str.length()) {
            char cCharAt2 = str.charAt(i3);
            int iCategoryChar2 = categoryChar(cCharAt2);
            int i4 = cCharAt2 - c;
            if (iCategoryChar2 != iCategoryChar || Math.abs(i4) > maxDiffCategory(iCategoryChar)) {
                int iMax2 = Math.max(iMax, i3 - i);
                i = i3;
                z = false;
                iMax = iMax2;
                iCategoryChar = iCategoryChar2;
            } else {
                if (z && i4 != i2) {
                    iMax = Math.max(iMax, i3 - i);
                    i = i3 - 1;
                }
                i2 = i4;
                z = true;
            }
            i3++;
            c = cCharAt2;
        }
        return Math.max(iMax, str.length() - i);
    }

    private static int categoryChar(char c) {
        if ('a' <= c && c <= 'z') {
            return 0;
        }
        if ('A' > c || c > 'Z') {
            return ('0' > c || c > '9') ? 3 : 2;
        }
        return 1;
    }

    private static int maxDiffCategory(int i) {
        switch (i) {
            case 0:
            case 1:
                return 1;
            case 2:
                return 10;
            default:
                return 0;
        }
    }
}
