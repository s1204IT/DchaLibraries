package android.app.admin;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class SystemUpdatePolicy implements Parcelable {

    @SystemApi
    public static final Parcelable.Creator<SystemUpdatePolicy> CREATOR = new Parcelable.Creator<SystemUpdatePolicy>() {
        @Override
        public SystemUpdatePolicy createFromParcel(Parcel parcel) {
            SystemUpdatePolicy systemUpdatePolicy = new SystemUpdatePolicy();
            systemUpdatePolicy.mPolicyType = parcel.readInt();
            systemUpdatePolicy.mMaintenanceWindowStart = parcel.readInt();
            systemUpdatePolicy.mMaintenanceWindowEnd = parcel.readInt();
            int i = parcel.readInt();
            systemUpdatePolicy.mFreezePeriods.ensureCapacity(i);
            for (int i2 = 0; i2 < i; i2++) {
                systemUpdatePolicy.mFreezePeriods.add(new FreezePeriod(MonthDay.of(parcel.readInt(), parcel.readInt()), MonthDay.of(parcel.readInt(), parcel.readInt())));
            }
            return systemUpdatePolicy;
        }

        @Override
        public SystemUpdatePolicy[] newArray(int i) {
            return new SystemUpdatePolicy[i];
        }
    };
    static final int FREEZE_PERIOD_MAX_LENGTH = 90;
    static final int FREEZE_PERIOD_MIN_SEPARATION = 60;
    private static final String KEY_FREEZE_END = "end";
    private static final String KEY_FREEZE_START = "start";
    private static final String KEY_FREEZE_TAG = "freeze";
    private static final String KEY_INSTALL_WINDOW_END = "install_window_end";
    private static final String KEY_INSTALL_WINDOW_START = "install_window_start";
    private static final String KEY_POLICY_TYPE = "policy_type";
    private static final String TAG = "SystemUpdatePolicy";
    public static final int TYPE_INSTALL_AUTOMATIC = 1;
    public static final int TYPE_INSTALL_WINDOWED = 2;

    @SystemApi
    public static final int TYPE_PAUSE = 4;
    public static final int TYPE_POSTPONE = 3;
    private static final int TYPE_UNKNOWN = -1;
    private static final int WINDOW_BOUNDARY = 1440;
    private final ArrayList<FreezePeriod> mFreezePeriods;
    private int mMaintenanceWindowEnd;
    private int mMaintenanceWindowStart;
    private int mPolicyType;

    @Retention(RetentionPolicy.SOURCE)
    @interface SystemUpdatePolicyType {
    }

    public static final class ValidationFailedException extends IllegalArgumentException implements Parcelable {
        public static final Parcelable.Creator<ValidationFailedException> CREATOR = new Parcelable.Creator<ValidationFailedException>() {
            @Override
            public ValidationFailedException createFromParcel(Parcel parcel) {
                return new ValidationFailedException(parcel.readInt(), parcel.readString());
            }

            @Override
            public ValidationFailedException[] newArray(int i) {
                return new ValidationFailedException[i];
            }
        };
        public static final int ERROR_COMBINED_FREEZE_PERIOD_TOO_CLOSE = 6;
        public static final int ERROR_COMBINED_FREEZE_PERIOD_TOO_LONG = 5;
        public static final int ERROR_DUPLICATE_OR_OVERLAP = 2;
        public static final int ERROR_NEW_FREEZE_PERIOD_TOO_CLOSE = 4;
        public static final int ERROR_NEW_FREEZE_PERIOD_TOO_LONG = 3;
        public static final int ERROR_NONE = 0;
        public static final int ERROR_UNKNOWN = 1;
        private final int mErrorCode;

        @Retention(RetentionPolicy.SOURCE)
        @interface ValidationFailureType {
        }

        private ValidationFailedException(int i, String str) {
            super(str);
            this.mErrorCode = i;
        }

        public int getErrorCode() {
            return this.mErrorCode;
        }

        public static ValidationFailedException duplicateOrOverlapPeriods() {
            return new ValidationFailedException(2, "Found duplicate or overlapping periods");
        }

        public static ValidationFailedException freezePeriodTooLong(String str) {
            return new ValidationFailedException(3, str);
        }

        public static ValidationFailedException freezePeriodTooClose(String str) {
            return new ValidationFailedException(4, str);
        }

        public static ValidationFailedException combinedPeriodTooLong(String str) {
            return new ValidationFailedException(5, str);
        }

        public static ValidationFailedException combinedPeriodTooClose(String str) {
            return new ValidationFailedException(6, str);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mErrorCode);
            parcel.writeString(getMessage());
        }
    }

    private SystemUpdatePolicy() {
        this.mPolicyType = -1;
        this.mFreezePeriods = new ArrayList<>();
    }

    public static SystemUpdatePolicy createAutomaticInstallPolicy() {
        SystemUpdatePolicy systemUpdatePolicy = new SystemUpdatePolicy();
        systemUpdatePolicy.mPolicyType = 1;
        return systemUpdatePolicy;
    }

    public static SystemUpdatePolicy createWindowedInstallPolicy(int i, int i2) {
        if (i < 0 || i >= 1440 || i2 < 0 || i2 >= 1440) {
            throw new IllegalArgumentException("startTime and endTime must be inside [0, 1440)");
        }
        SystemUpdatePolicy systemUpdatePolicy = new SystemUpdatePolicy();
        systemUpdatePolicy.mPolicyType = 2;
        systemUpdatePolicy.mMaintenanceWindowStart = i;
        systemUpdatePolicy.mMaintenanceWindowEnd = i2;
        return systemUpdatePolicy;
    }

    public static SystemUpdatePolicy createPostponeInstallPolicy() {
        SystemUpdatePolicy systemUpdatePolicy = new SystemUpdatePolicy();
        systemUpdatePolicy.mPolicyType = 3;
        return systemUpdatePolicy;
    }

    public int getPolicyType() {
        return this.mPolicyType;
    }

    public int getInstallWindowStart() {
        if (this.mPolicyType == 2) {
            return this.mMaintenanceWindowStart;
        }
        return -1;
    }

    public int getInstallWindowEnd() {
        if (this.mPolicyType == 2) {
            return this.mMaintenanceWindowEnd;
        }
        return -1;
    }

    public boolean isValid() {
        try {
            validateType();
            validateFreezePeriods();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void validateType() {
        if (this.mPolicyType == 1 || this.mPolicyType == 3) {
            return;
        }
        if (this.mPolicyType == 2) {
            if (this.mMaintenanceWindowStart < 0 || this.mMaintenanceWindowStart >= 1440 || this.mMaintenanceWindowEnd < 0 || this.mMaintenanceWindowEnd >= 1440) {
                throw new IllegalArgumentException("Invalid maintenance window");
            }
            return;
        }
        throw new IllegalArgumentException("Invalid system update policy type.");
    }

    public SystemUpdatePolicy setFreezePeriods(List<FreezePeriod> list) {
        FreezePeriod.validatePeriods(list);
        this.mFreezePeriods.clear();
        this.mFreezePeriods.addAll(list);
        return this;
    }

    public List<FreezePeriod> getFreezePeriods() {
        return Collections.unmodifiableList(this.mFreezePeriods);
    }

    public Pair<LocalDate, LocalDate> getCurrentFreezePeriod(LocalDate localDate) {
        for (FreezePeriod freezePeriod : this.mFreezePeriods) {
            if (freezePeriod.contains(localDate)) {
                return freezePeriod.toCurrentOrFutureRealDates(localDate);
            }
        }
        return null;
    }

    private long timeUntilNextFreezePeriod(long j) {
        LocalDate localDate;
        FreezePeriod next;
        List<FreezePeriod> listCanonicalizePeriods = FreezePeriod.canonicalizePeriods(this.mFreezePeriods);
        LocalDate localDateMillisToDate = millisToDate(j);
        Iterator<FreezePeriod> it = listCanonicalizePeriods.iterator();
        do {
            if (it.hasNext()) {
                next = it.next();
                if (next.after(localDateMillisToDate)) {
                    localDate = next.toCurrentOrFutureRealDates(localDateMillisToDate).first;
                }
            } else {
                localDate = null;
            }
            if (localDate == null) {
                localDate = listCanonicalizePeriods.get(0).toCurrentOrFutureRealDates(localDateMillisToDate).first;
            }
            return dateToMillis(localDate) - j;
        } while (!next.contains(localDateMillisToDate));
        throw new IllegalArgumentException("Given date is inside a freeze period");
    }

    public void validateFreezePeriods() {
        FreezePeriod.validatePeriods(this.mFreezePeriods);
    }

    public void validateAgainstPreviousFreezePeriod(LocalDate localDate, LocalDate localDate2, LocalDate localDate3) {
        FreezePeriod.validateAgainstPreviousFreezePeriod(this.mFreezePeriods, localDate, localDate2, localDate3);
    }

    @SystemApi
    public static class InstallationOption {
        private long mEffectiveTime;
        private final int mType;

        @Retention(RetentionPolicy.SOURCE)
        @interface InstallationOptionType {
        }

        InstallationOption(int i, long j) {
            this.mType = i;
            this.mEffectiveTime = j;
        }

        public int getType() {
            return this.mType;
        }

        public long getEffectiveTime() {
            return this.mEffectiveTime;
        }

        protected void limitEffectiveTime(long j) {
            this.mEffectiveTime = Long.min(this.mEffectiveTime, j);
        }
    }

    @SystemApi
    public InstallationOption getInstallationOptionAt(long j) {
        Pair<LocalDate, LocalDate> currentFreezePeriod = getCurrentFreezePeriod(millisToDate(j));
        if (currentFreezePeriod != null) {
            return new InstallationOption(4, dateToMillis(roundUpLeapDay(currentFreezePeriod.second).plusDays(1L)) - j);
        }
        InstallationOption installationOptionRegardlessFreezeAt = getInstallationOptionRegardlessFreezeAt(j);
        if (this.mFreezePeriods.size() > 0) {
            installationOptionRegardlessFreezeAt.limitEffectiveTime(timeUntilNextFreezePeriod(j));
        }
        return installationOptionRegardlessFreezeAt;
    }

    private InstallationOption getInstallationOptionRegardlessFreezeAt(long j) {
        if (this.mPolicyType == 1 || this.mPolicyType == 3) {
            return new InstallationOption(this.mPolicyType, Long.MAX_VALUE);
        }
        if (this.mPolicyType == 2) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(j);
            long millis = TimeUnit.HOURS.toMillis(calendar.get(11)) + TimeUnit.MINUTES.toMillis(calendar.get(12)) + TimeUnit.SECONDS.toMillis(calendar.get(13)) + ((long) calendar.get(14));
            long millis2 = TimeUnit.MINUTES.toMillis(this.mMaintenanceWindowStart);
            long millis3 = TimeUnit.MINUTES.toMillis(this.mMaintenanceWindowEnd);
            long millis4 = TimeUnit.DAYS.toMillis(1L);
            if ((millis2 <= millis && millis <= millis3) || (millis2 > millis3 && (millis2 <= millis || millis <= millis3))) {
                return new InstallationOption(1, ((millis3 - millis) + millis4) % millis4);
            }
            return new InstallationOption(4, ((millis2 - millis) + millis4) % millis4);
        }
        throw new RuntimeException("Unknown policy type");
    }

    private static LocalDate roundUpLeapDay(LocalDate localDate) {
        if (localDate.isLeapYear() && localDate.getMonthValue() == 2 && localDate.getDayOfMonth() == 28) {
            return localDate.plusDays(1L);
        }
        return localDate;
    }

    private static LocalDate millisToDate(long j) {
        return Instant.ofEpochMilli(j).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static long dateToMillis(LocalDate localDate) {
        return LocalDateTime.of(localDate, LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public String toString() {
        return String.format("SystemUpdatePolicy (type: %d, windowStart: %d, windowEnd: %d, freezes: [%s])", Integer.valueOf(this.mPolicyType), Integer.valueOf(this.mMaintenanceWindowStart), Integer.valueOf(this.mMaintenanceWindowEnd), this.mFreezePeriods.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((FreezePeriod) obj).toString();
            }
        }).collect(Collectors.joining(",")));
    }

    @Override
    @SystemApi
    public int describeContents() {
        return 0;
    }

    @Override
    @SystemApi
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPolicyType);
        parcel.writeInt(this.mMaintenanceWindowStart);
        parcel.writeInt(this.mMaintenanceWindowEnd);
        int size = this.mFreezePeriods.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            FreezePeriod freezePeriod = this.mFreezePeriods.get(i2);
            parcel.writeInt(freezePeriod.getStart().getMonthValue());
            parcel.writeInt(freezePeriod.getStart().getDayOfMonth());
            parcel.writeInt(freezePeriod.getEnd().getMonthValue());
            parcel.writeInt(freezePeriod.getEnd().getDayOfMonth());
        }
    }

    public static SystemUpdatePolicy restoreFromXml(XmlPullParser xmlPullParser) {
        try {
            SystemUpdatePolicy systemUpdatePolicy = new SystemUpdatePolicy();
            String attributeValue = xmlPullParser.getAttributeValue(null, KEY_POLICY_TYPE);
            if (attributeValue != null) {
                systemUpdatePolicy.mPolicyType = Integer.parseInt(attributeValue);
                String attributeValue2 = xmlPullParser.getAttributeValue(null, KEY_INSTALL_WINDOW_START);
                if (attributeValue2 != null) {
                    systemUpdatePolicy.mMaintenanceWindowStart = Integer.parseInt(attributeValue2);
                }
                String attributeValue3 = xmlPullParser.getAttributeValue(null, KEY_INSTALL_WINDOW_END);
                if (attributeValue3 != null) {
                    systemUpdatePolicy.mMaintenanceWindowEnd = Integer.parseInt(attributeValue3);
                }
                int depth = xmlPullParser.getDepth();
                while (true) {
                    int next = xmlPullParser.next();
                    if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                        break;
                    }
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals(KEY_FREEZE_TAG)) {
                        systemUpdatePolicy.mFreezePeriods.add(new FreezePeriod(MonthDay.parse(xmlPullParser.getAttributeValue(null, "start")), MonthDay.parse(xmlPullParser.getAttributeValue(null, "end"))));
                    }
                }
                return systemUpdatePolicy;
            }
        } catch (IOException | NumberFormatException | XmlPullParserException e) {
            Log.w(TAG, "Load xml failed", e);
        }
        return null;
    }

    public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.attribute(null, KEY_POLICY_TYPE, Integer.toString(this.mPolicyType));
        xmlSerializer.attribute(null, KEY_INSTALL_WINDOW_START, Integer.toString(this.mMaintenanceWindowStart));
        xmlSerializer.attribute(null, KEY_INSTALL_WINDOW_END, Integer.toString(this.mMaintenanceWindowEnd));
        for (int i = 0; i < this.mFreezePeriods.size(); i++) {
            FreezePeriod freezePeriod = this.mFreezePeriods.get(i);
            xmlSerializer.startTag(null, KEY_FREEZE_TAG);
            xmlSerializer.attribute(null, "start", freezePeriod.getStart().toString());
            xmlSerializer.attribute(null, "end", freezePeriod.getEnd().toString());
            xmlSerializer.endTag(null, KEY_FREEZE_TAG);
        }
    }
}
