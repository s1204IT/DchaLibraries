package android.ext.services.notification;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class ChannelImpressions implements Parcelable {
    private float mDismissToViewRatioLimit;
    private int mDismissals;
    private int mStreak;
    private int mStreakLimit;
    private int mViews;
    private static final boolean DEBUG = Log.isLoggable("ExtAssistant.CI", 3);
    public static final Parcelable.Creator<ChannelImpressions> CREATOR = new Parcelable.Creator<ChannelImpressions>() {
        @Override
        public ChannelImpressions createFromParcel(Parcel parcel) {
            return new ChannelImpressions(parcel);
        }

        @Override
        public ChannelImpressions[] newArray(int i) {
            return new ChannelImpressions[i];
        }
    };

    public ChannelImpressions() {
        this.mDismissals = 0;
        this.mViews = 0;
        this.mStreak = 0;
        this.mDismissToViewRatioLimit = 0.8f;
        this.mStreakLimit = 2;
    }

    protected ChannelImpressions(Parcel parcel) {
        this.mDismissals = 0;
        this.mViews = 0;
        this.mStreak = 0;
        this.mDismissals = parcel.readInt();
        this.mViews = parcel.readInt();
        this.mStreak = parcel.readInt();
        this.mDismissToViewRatioLimit = parcel.readFloat();
        this.mStreakLimit = parcel.readInt();
    }

    public int getStreak() {
        return this.mStreak;
    }

    public int getDismissals() {
        return this.mDismissals;
    }

    public int getViews() {
        return this.mViews;
    }

    public void incrementDismissals() {
        this.mDismissals++;
        this.mStreak++;
    }

    void updateThresholds(float f, int i) {
        this.mDismissToViewRatioLimit = f;
        this.mStreakLimit = i;
    }

    @VisibleForTesting
    float getDismissToViewRatioLimit() {
        return this.mDismissToViewRatioLimit;
    }

    @VisibleForTesting
    int getStreakLimit() {
        return this.mStreakLimit;
    }

    public void append(ChannelImpressions channelImpressions) {
        if (channelImpressions != null) {
            this.mViews += channelImpressions.getViews();
            this.mStreak += channelImpressions.getStreak();
            this.mDismissals += channelImpressions.getDismissals();
        }
    }

    public void incrementViews() {
        this.mViews++;
    }

    public void resetStreak() {
        this.mStreak = 0;
    }

    public boolean shouldTriggerBlock() {
        if (getViews() == 0) {
            return false;
        }
        if (DEBUG) {
            Log.d("ExtAssistant.CI", "should trigger? " + getDismissals() + " " + getViews() + " " + getStreak());
        }
        return ((float) getDismissals()) / ((float) getViews()) > this.mDismissToViewRatioLimit && getStreak() > this.mStreakLimit;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mDismissals);
        parcel.writeInt(this.mViews);
        parcel.writeInt(this.mStreak);
        parcel.writeFloat(this.mDismissToViewRatioLimit);
        parcel.writeInt(this.mStreakLimit);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChannelImpressions channelImpressions = (ChannelImpressions) obj;
        if (this.mDismissals == channelImpressions.mDismissals && this.mViews == channelImpressions.mViews && this.mStreak == channelImpressions.mStreak) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((this.mDismissals * 31) + this.mViews)) + this.mStreak;
    }

    public String toString() {
        return "ChannelImpressions{mDismissals=" + this.mDismissals + ", mViews=" + this.mViews + ", mStreak=" + this.mStreak + ", thresholds=(" + this.mDismissToViewRatioLimit + "," + this.mStreakLimit + ")}";
    }

    protected void populateFromXml(XmlPullParser xmlPullParser) {
        this.mDismissals = safeInt(xmlPullParser, "dismisses", 0);
        this.mStreak = safeInt(xmlPullParser, "streak", 0);
        this.mViews = safeInt(xmlPullParser, "views", 0);
    }

    protected void writeXml(XmlSerializer xmlSerializer) throws IOException {
        if (this.mDismissals != 0) {
            xmlSerializer.attribute(null, "dismisses", String.valueOf(this.mDismissals));
        }
        if (this.mStreak != 0) {
            xmlSerializer.attribute(null, "streak", String.valueOf(this.mStreak));
        }
        if (this.mViews != 0) {
            xmlSerializer.attribute(null, "views", String.valueOf(this.mViews));
        }
    }

    private static int safeInt(XmlPullParser xmlPullParser, String str, int i) {
        return tryParseInt(xmlPullParser.getAttributeValue(null, str), i);
    }

    private static int tryParseInt(String str, int i) {
        if (TextUtils.isEmpty(str)) {
            return i;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return i;
        }
    }
}
