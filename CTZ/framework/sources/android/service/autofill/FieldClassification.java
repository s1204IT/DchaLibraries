package android.service.autofill;

import android.os.Parcel;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class FieldClassification {
    private final ArrayList<Match> mMatches;

    public FieldClassification(ArrayList<Match> arrayList) {
        this.mMatches = (ArrayList) Preconditions.checkNotNull(arrayList);
        Collections.sort(this.mMatches, new Comparator<Match>() {
            @Override
            public int compare(Match match, Match match2) {
                if (match.mScore > match2.mScore) {
                    return -1;
                }
                return match.mScore < match2.mScore ? 1 : 0;
            }
        });
    }

    public List<Match> getMatches() {
        return this.mMatches;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        return "FieldClassification: " + this.mMatches;
    }

    private void writeToParcel(Parcel parcel) {
        parcel.writeInt(this.mMatches.size());
        for (int i = 0; i < this.mMatches.size(); i++) {
            this.mMatches.get(i).writeToParcel(parcel);
        }
    }

    private static FieldClassification readFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < i; i2++) {
            arrayList.add(i2, Match.readFromParcel(parcel));
        }
        return new FieldClassification(arrayList);
    }

    static FieldClassification[] readArrayFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        FieldClassification[] fieldClassificationArr = new FieldClassification[i];
        for (int i2 = 0; i2 < i; i2++) {
            fieldClassificationArr[i2] = readFromParcel(parcel);
        }
        return fieldClassificationArr;
    }

    static void writeArrayToParcel(Parcel parcel, FieldClassification[] fieldClassificationArr) {
        parcel.writeInt(fieldClassificationArr.length);
        for (FieldClassification fieldClassification : fieldClassificationArr) {
            fieldClassification.writeToParcel(parcel);
        }
    }

    public static final class Match {
        private final String mCategoryId;
        private final float mScore;

        public Match(String str, float f) {
            this.mCategoryId = (String) Preconditions.checkNotNull(str);
            this.mScore = f;
        }

        public String getCategoryId() {
            return this.mCategoryId;
        }

        public float getScore() {
            return this.mScore;
        }

        public String toString() {
            if (!Helper.sDebug) {
                return super.toString();
            }
            StringBuilder sb = new StringBuilder("Match: categoryId=");
            Helper.appendRedacted(sb, this.mCategoryId);
            sb.append(", score=");
            sb.append(this.mScore);
            return sb.toString();
        }

        private void writeToParcel(Parcel parcel) {
            parcel.writeString(this.mCategoryId);
            parcel.writeFloat(this.mScore);
        }

        private static Match readFromParcel(Parcel parcel) {
            return new Match(parcel.readString(), parcel.readFloat());
        }
    }
}
