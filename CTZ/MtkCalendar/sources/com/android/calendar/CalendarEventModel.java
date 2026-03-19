package com.android.calendar;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import com.android.calendar.event.EditEventHelper;
import com.android.calendar.event.EventColorCache;
import com.android.common.Rfc822Validator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.TimeZone;

public class CalendarEventModel implements Serializable {
    public int mAccessLevel;
    public String mAccountType;
    public boolean mAllDay;
    public LinkedHashMap<String, Attendee> mAttendeesList;
    public int mAvailability;
    public int mCalendarAccessLevel;
    public String mCalendarAccountName;
    public String mCalendarAccountType;
    public String mCalendarAllowedAttendeeTypes;
    public String mCalendarAllowedAvailability;
    public String mCalendarAllowedReminders;
    private int mCalendarColor;
    private boolean mCalendarColorInitialized;
    public String mCalendarDisplayName;
    public long mCalendarId;
    public int mCalendarMaxReminders;
    public ArrayList<ReminderEntry> mDefaultReminders;
    public String mDescription;
    public String mDuration;
    public long mEnd;
    private int mEventColor;
    public EventColorCache mEventColorCache;
    private boolean mEventColorInitialized;
    public int mEventStatus;
    public boolean mGuestsCanInviteOthers;
    public boolean mGuestsCanModify;
    public boolean mGuestsCanSeeGuests;
    public boolean mHasAlarm;
    public boolean mHasAttendeeData;
    public long mId;
    public boolean mIsFirstEventInSeries;
    public boolean mIsLunar;
    public boolean mIsOrganizer;
    public String mLocation;
    public boolean mModelUpdatedWithEventCursor;
    public String mOrganizer;
    public boolean mOrganizerCanRespond;
    public String mOrganizerDisplayName;
    public Boolean mOriginalAllDay;
    public long mOriginalEnd;
    public long mOriginalId;
    public long mOriginalStart;
    public String mOriginalSyncId;
    public Long mOriginalTime;
    public String mOwnerAccount;
    public int mOwnerAttendeeId;
    public ArrayList<ReminderEntry> mReminders;
    public String mRrule;
    public int mSelfAttendeeStatus;
    public long mStart;
    public String mSyncAccount;
    public String mSyncAccountType;
    public String mSyncId;
    public String mTimezone;
    public String mTimezone2;
    public String mTitle;
    public String mUri;

    public static class Attendee implements Serializable {
        public String mEmail;
        public String mIdNamespace;
        public String mIdentity;
        public String mName;
        public int mStatus;

        public int hashCode() {
            if (this.mEmail == null) {
                return 0;
            }
            return this.mEmail.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return (obj instanceof Attendee) && TextUtils.equals(this.mEmail, ((Attendee) obj).mEmail);
        }

        public Attendee(String str, String str2) {
            this(str, str2, 0, null, null);
        }

        public Attendee(String str, String str2, int i, String str3, String str4) {
            this.mName = str;
            this.mEmail = str2;
            this.mStatus = i;
            this.mIdentity = str3;
            this.mIdNamespace = str4;
        }

        public String getRecipientAddress() {
            int iIndexOf;
            if (TextUtils.isEmpty(this.mEmail)) {
                return "";
            }
            String strSubstring = this.mName;
            if ((TextUtils.isEmpty(strSubstring) || this.mEmail.equals(strSubstring)) && (iIndexOf = this.mEmail.indexOf("@")) != -1) {
                strSubstring = this.mEmail.substring(0, iIndexOf);
            }
            return strSubstring + " <" + this.mEmail + ">";
        }
    }

    public static class ReminderEntry implements Serializable, Comparable<ReminderEntry> {
        private final int mMethod;
        private final int mMinutes;

        public static ReminderEntry valueOf(int i, int i2) {
            return new ReminderEntry(i, i2);
        }

        public static ReminderEntry valueOf(int i) {
            return valueOf(i, 0);
        }

        private ReminderEntry(int i, int i2) {
            this.mMinutes = i;
            this.mMethod = i2;
        }

        public int hashCode() {
            return (this.mMinutes * 10) + this.mMethod;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReminderEntry)) {
                return false;
            }
            ReminderEntry reminderEntry = (ReminderEntry) obj;
            if (reminderEntry.mMinutes != this.mMinutes) {
                return false;
            }
            if (reminderEntry.mMethod != this.mMethod) {
                if (reminderEntry.mMethod == 0 && this.mMethod == 1) {
                    return true;
                }
                return reminderEntry.mMethod == 1 && this.mMethod == 0;
            }
            return true;
        }

        public String toString() {
            return "ReminderEntry min=" + this.mMinutes + " meth=" + this.mMethod;
        }

        @Override
        public int compareTo(ReminderEntry reminderEntry) {
            if (reminderEntry.mMinutes != this.mMinutes) {
                return reminderEntry.mMinutes - this.mMinutes;
            }
            if (reminderEntry.mMethod != this.mMethod) {
                return this.mMethod - reminderEntry.mMethod;
            }
            return 0;
        }

        public int getMinutes() {
            return this.mMinutes;
        }

        public int getMethod() {
            return this.mMethod;
        }
    }

    public CalendarEventModel() {
        this.mUri = null;
        this.mId = -1L;
        this.mCalendarId = -1L;
        this.mCalendarDisplayName = "";
        this.mCalendarColor = -1;
        this.mCalendarColorInitialized = false;
        this.mSyncId = null;
        this.mSyncAccount = null;
        this.mSyncAccountType = null;
        this.mEventColor = -1;
        this.mEventColorInitialized = false;
        this.mOwnerAccount = null;
        this.mTitle = null;
        this.mLocation = null;
        this.mDescription = null;
        this.mRrule = null;
        this.mOrganizer = null;
        this.mOrganizerDisplayName = null;
        this.mIsOrganizer = true;
        this.mIsFirstEventInSeries = true;
        this.mOriginalStart = -1L;
        this.mStart = -1L;
        this.mOriginalEnd = -1L;
        this.mEnd = -1L;
        this.mDuration = null;
        this.mTimezone = null;
        this.mTimezone2 = null;
        this.mAllDay = false;
        this.mHasAlarm = false;
        this.mAvailability = 0;
        this.mHasAttendeeData = true;
        this.mSelfAttendeeStatus = -1;
        this.mOwnerAttendeeId = -1;
        this.mOriginalSyncId = null;
        this.mOriginalId = -1L;
        this.mOriginalTime = null;
        this.mOriginalAllDay = null;
        this.mGuestsCanModify = false;
        this.mGuestsCanInviteOthers = false;
        this.mGuestsCanSeeGuests = false;
        this.mOrganizerCanRespond = false;
        this.mCalendarAccessLevel = 500;
        this.mEventStatus = 1;
        this.mAccessLevel = 0;
        this.mIsLunar = false;
        this.mAccountType = "";
        this.mReminders = new ArrayList<>();
        this.mDefaultReminders = new ArrayList<>();
        this.mAttendeesList = new LinkedHashMap<>();
        this.mTimezone = TimeZone.getDefault().getID();
    }

    public CalendarEventModel(Context context) {
        this();
        this.mTimezone = Utils.getTimeZone(context, null);
        int i = Integer.parseInt(GeneralPreferences.getSharedPreferences(context).getString("preferences_default_reminder", "-1"));
        if (i != -1) {
            this.mHasAlarm = true;
            this.mReminders.add(ReminderEntry.valueOf(i));
            this.mDefaultReminders.add(ReminderEntry.valueOf(i));
        }
    }

    public CalendarEventModel(Context context, Intent intent) {
        this(context);
        if (intent == null) {
            return;
        }
        String stringExtra = intent.getStringExtra("title");
        if (stringExtra != null) {
            this.mTitle = stringExtra;
        }
        String stringExtra2 = intent.getStringExtra("eventLocation");
        if (stringExtra2 != null) {
            this.mLocation = stringExtra2;
        }
        String stringExtra3 = intent.getStringExtra("description");
        if (stringExtra3 != null) {
            this.mDescription = stringExtra3;
        }
        int intExtra = intent.getIntExtra("availability", -1);
        if (intExtra != -1) {
            this.mAvailability = intExtra;
        }
        int intExtra2 = intent.getIntExtra("accessLevel", -1);
        if (intExtra2 != -1) {
            this.mAccessLevel = intExtra2 > 0 ? intExtra2 - 1 : intExtra2;
        }
        String stringExtra4 = intent.getStringExtra("rrule");
        if (!TextUtils.isEmpty(stringExtra4)) {
            this.mRrule = stringExtra4;
        }
        String stringExtra5 = intent.getStringExtra("android.intent.extra.EMAIL");
        if (!TextUtils.isEmpty(stringExtra5)) {
            for (String str : stringExtra5.split("[ ,;]")) {
                if (!TextUtils.isEmpty(str) && str.contains("@")) {
                    String strTrim = str.trim();
                    if (!this.mAttendeesList.containsKey(strTrim)) {
                        this.mAttendeesList.put(strTrim, new Attendee("", strTrim));
                    }
                }
            }
        }
    }

    public boolean isValid() {
        return (this.mCalendarId == -1 || TextUtils.isEmpty(this.mOwnerAccount)) ? false : true;
    }

    public void clear() {
        this.mUri = null;
        this.mId = -1L;
        this.mCalendarId = -1L;
        this.mCalendarColor = -1;
        this.mCalendarColorInitialized = false;
        this.mEventColorCache = null;
        this.mEventColor = -1;
        this.mEventColorInitialized = false;
        this.mSyncId = null;
        this.mSyncAccount = null;
        this.mSyncAccountType = null;
        this.mOwnerAccount = null;
        this.mTitle = null;
        this.mLocation = null;
        this.mDescription = null;
        this.mRrule = null;
        this.mOrganizer = null;
        this.mOrganizerDisplayName = null;
        this.mIsOrganizer = true;
        this.mIsFirstEventInSeries = true;
        this.mOriginalStart = -1L;
        this.mStart = -1L;
        this.mOriginalEnd = -1L;
        this.mEnd = -1L;
        this.mDuration = null;
        this.mTimezone = null;
        this.mTimezone2 = null;
        this.mAllDay = false;
        this.mHasAlarm = false;
        this.mHasAttendeeData = true;
        this.mSelfAttendeeStatus = -1;
        this.mOwnerAttendeeId = -1;
        this.mOriginalId = -1L;
        this.mOriginalSyncId = null;
        this.mOriginalTime = null;
        this.mOriginalAllDay = null;
        this.mGuestsCanModify = false;
        this.mGuestsCanInviteOthers = false;
        this.mGuestsCanSeeGuests = false;
        this.mAccessLevel = 0;
        this.mEventStatus = 1;
        this.mOrganizerCanRespond = false;
        this.mCalendarAccessLevel = 500;
        this.mModelUpdatedWithEventCursor = false;
        this.mCalendarAllowedReminders = null;
        this.mCalendarAllowedAttendeeTypes = null;
        this.mCalendarAllowedAvailability = null;
        this.mReminders = new ArrayList<>();
        this.mAttendeesList.clear();
    }

    public void addAttendee(Attendee attendee) {
        this.mAttendeesList.put(attendee.mEmail, attendee);
    }

    public void addAttendees(String str, Rfc822Validator rfc822Validator) {
        LinkedHashSet<Rfc822Token> addressesFromList = EditEventHelper.getAddressesFromList(str, rfc822Validator);
        synchronized (this) {
            for (Rfc822Token rfc822Token : addressesFromList) {
                Attendee attendee = new Attendee(rfc822Token.getName(), rfc822Token.getAddress());
                if (TextUtils.isEmpty(attendee.mName)) {
                    attendee.mName = attendee.mEmail;
                }
                addAttendee(attendee);
            }
        }
    }

    public String getAttendeesString() {
        StringBuilder sb = new StringBuilder();
        for (Attendee attendee : this.mAttendeesList.values()) {
            String str = attendee.mName;
            String str2 = attendee.mEmail;
            String string = Integer.toString(attendee.mStatus);
            sb.append("name:");
            sb.append(str);
            sb.append(" email:");
            sb.append(str2);
            sb.append(" status:");
            sb.append(string);
        }
        return sb.toString();
    }

    public int hashCode() {
        return (31 * ((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((this.mAllDay ? 1231 : 1237) + 31) * 31) + (this.mAttendeesList == null ? 0 : getAttendeesString().hashCode())) * 31) + ((int) (this.mCalendarId ^ (this.mCalendarId >>> 32)))) * 31) + (this.mDescription == null ? 0 : this.mDescription.hashCode())) * 31) + (this.mDuration == null ? 0 : this.mDuration.hashCode())) * 31) + ((int) (this.mEnd ^ (this.mEnd >>> 32)))) * 31) + (this.mGuestsCanInviteOthers ? 1231 : 1237)) * 31) + (this.mGuestsCanModify ? 1231 : 1237)) * 31) + (this.mGuestsCanSeeGuests ? 1231 : 1237)) * 31) + (this.mOrganizerCanRespond ? 1231 : 1237)) * 31) + (this.mModelUpdatedWithEventCursor ? 1231 : 1237)) * 31) + this.mCalendarAccessLevel) * 31) + (this.mHasAlarm ? 1231 : 1237)) * 31) + (this.mHasAttendeeData ? 1231 : 1237)) * 31) + ((int) (this.mId ^ (this.mId >>> 32)))) * 31) + (this.mIsFirstEventInSeries ? 1231 : 1237)) * 31) + (this.mIsOrganizer ? 1231 : 1237)) * 31) + (this.mLocation == null ? 0 : this.mLocation.hashCode())) * 31) + (this.mOrganizer == null ? 0 : this.mOrganizer.hashCode())) * 31) + (this.mOriginalAllDay == null ? 0 : this.mOriginalAllDay.hashCode())) * 31) + ((int) (this.mOriginalEnd ^ (this.mOriginalEnd >>> 32)))) * 31) + (this.mOriginalSyncId == null ? 0 : this.mOriginalSyncId.hashCode())) * 31) + ((int) (this.mOriginalId ^ (this.mOriginalEnd >>> 32)))) * 31) + ((int) (this.mOriginalStart ^ (this.mOriginalStart >>> 32)))) * 31) + (this.mOriginalTime == null ? 0 : this.mOriginalTime.hashCode())) * 31) + (this.mOwnerAccount == null ? 0 : this.mOwnerAccount.hashCode())) * 31) + (this.mReminders == null ? 0 : this.mReminders.hashCode())) * 31) + (this.mRrule == null ? 0 : this.mRrule.hashCode())) * 31) + this.mSelfAttendeeStatus) * 31) + this.mOwnerAttendeeId) * 31) + ((int) (this.mStart ^ (this.mStart >>> 32)))) * 31) + (this.mSyncAccount == null ? 0 : this.mSyncAccount.hashCode())) * 31) + (this.mSyncAccountType == null ? 0 : this.mSyncAccountType.hashCode())) * 31) + (this.mSyncId == null ? 0 : this.mSyncId.hashCode())) * 31) + (this.mTimezone == null ? 0 : this.mTimezone.hashCode())) * 31) + (this.mTimezone2 == null ? 0 : this.mTimezone2.hashCode())) * 31) + (this.mTitle == null ? 0 : this.mTitle.hashCode())) * 31) + this.mAvailability) * 31) + (this.mUri != null ? this.mUri.hashCode() : 0)) * 31) + this.mAccessLevel)) + this.mEventStatus;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == 0 || !(obj instanceof CalendarEventModel) || !checkOriginalModelFields(obj)) {
            return false;
        }
        if (this.mLocation == null) {
            if (obj.mLocation != null) {
                return false;
            }
        } else if (!this.mLocation.equals(obj.mLocation)) {
            return false;
        }
        if (this.mTitle == null) {
            if (obj.mTitle != null) {
                return false;
            }
        } else if (!this.mTitle.equals(obj.mTitle)) {
            return false;
        }
        if (this.mDescription == null) {
            if (obj.mDescription != null) {
                return false;
            }
        } else if (!this.mDescription.equals(obj.mDescription)) {
            return false;
        }
        if (this.mDuration == null) {
            if (obj.mDuration != null) {
                return false;
            }
        } else if (!this.mDuration.equals(obj.mDuration)) {
            return false;
        }
        if (this.mEnd != obj.mEnd || this.mIsFirstEventInSeries != obj.mIsFirstEventInSeries || this.mOriginalEnd != obj.mOriginalEnd || this.mOriginalStart != obj.mOriginalStart || this.mStart != obj.mStart || this.mOriginalId != obj.mOriginalId) {
            return false;
        }
        if (this.mOriginalSyncId == null) {
            if (obj.mOriginalSyncId != null) {
                return false;
            }
        } else if (!this.mOriginalSyncId.equals(obj.mOriginalSyncId)) {
            return false;
        }
        if (this.mRrule == null) {
            if (obj.mRrule != null) {
                return false;
            }
        } else if (!this.mRrule.equals(obj.mRrule)) {
            return false;
        }
        return true;
    }

    public boolean isUnchanged(CalendarEventModel calendarEventModel) {
        if (this == calendarEventModel) {
            return true;
        }
        if (calendarEventModel == null || !checkOriginalModelFields(calendarEventModel)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mLocation)) {
            if (!TextUtils.isEmpty(calendarEventModel.mLocation)) {
                return false;
            }
        } else if (!this.mLocation.equals(calendarEventModel.mLocation)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mTitle)) {
            if (!TextUtils.isEmpty(calendarEventModel.mTitle)) {
                return false;
            }
        } else if (!this.mTitle.equals(calendarEventModel.mTitle)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mDescription)) {
            if (!TextUtils.isEmpty(calendarEventModel.mDescription)) {
                return false;
            }
        } else if (!this.mDescription.equals(calendarEventModel.mDescription)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mDuration)) {
            if (!TextUtils.isEmpty(calendarEventModel.mDuration)) {
                return false;
            }
        } else if (!this.mDuration.equals(calendarEventModel.mDuration)) {
            return false;
        }
        if (this.mEnd != this.mOriginalEnd || this.mStart != this.mOriginalStart) {
            return false;
        }
        if (this.mOriginalId != calendarEventModel.mOriginalId && this.mOriginalId != calendarEventModel.mId) {
            return false;
        }
        if (TextUtils.isEmpty(this.mRrule)) {
            if (!TextUtils.isEmpty(calendarEventModel.mRrule)) {
                boolean z = this.mOriginalSyncId == null || !this.mOriginalSyncId.equals(calendarEventModel.mSyncId);
                boolean z2 = this.mOriginalId == -1 || this.mOriginalId != calendarEventModel.mId;
                if (z && z2) {
                    return false;
                }
            }
        } else if (!this.mRrule.equals(calendarEventModel.mRrule)) {
            return false;
        }
        return true;
    }

    protected boolean checkOriginalModelFields(CalendarEventModel calendarEventModel) {
        if (this.mAllDay != calendarEventModel.mAllDay) {
            return false;
        }
        if (this.mAttendeesList == null) {
            if (calendarEventModel.mAttendeesList != null) {
                return false;
            }
        } else if (!this.mAttendeesList.equals(calendarEventModel.mAttendeesList)) {
            return false;
        }
        if (this.mCalendarId != calendarEventModel.mCalendarId || this.mCalendarColor != calendarEventModel.mCalendarColor || this.mCalendarColorInitialized != calendarEventModel.mCalendarColorInitialized || this.mGuestsCanInviteOthers != calendarEventModel.mGuestsCanInviteOthers || this.mGuestsCanModify != calendarEventModel.mGuestsCanModify || this.mGuestsCanSeeGuests != calendarEventModel.mGuestsCanSeeGuests || this.mOrganizerCanRespond != calendarEventModel.mOrganizerCanRespond || this.mCalendarAccessLevel != calendarEventModel.mCalendarAccessLevel || this.mModelUpdatedWithEventCursor != calendarEventModel.mModelUpdatedWithEventCursor || this.mHasAlarm != calendarEventModel.mHasAlarm || this.mHasAttendeeData != calendarEventModel.mHasAttendeeData || this.mId != calendarEventModel.mId || this.mIsOrganizer != calendarEventModel.mIsOrganizer) {
            return false;
        }
        if (this.mOrganizer == null) {
            if (calendarEventModel.mOrganizer != null) {
                return false;
            }
        } else if (!this.mOrganizer.equals(calendarEventModel.mOrganizer)) {
            return false;
        }
        if (this.mOriginalAllDay == null) {
            if (calendarEventModel.mOriginalAllDay != null) {
                return false;
            }
        } else if (!this.mOriginalAllDay.equals(calendarEventModel.mOriginalAllDay)) {
            return false;
        }
        if (this.mOriginalTime == null) {
            if (calendarEventModel.mOriginalTime != null) {
                return false;
            }
        } else if (!this.mOriginalTime.equals(calendarEventModel.mOriginalTime)) {
            return false;
        }
        if (this.mOwnerAccount == null) {
            if (calendarEventModel.mOwnerAccount != null) {
                return false;
            }
        } else if (!this.mOwnerAccount.equals(calendarEventModel.mOwnerAccount)) {
            return false;
        }
        if (this.mReminders == null) {
            if (calendarEventModel.mReminders != null) {
                return false;
            }
        } else if (!this.mReminders.equals(calendarEventModel.mReminders)) {
            return false;
        }
        if (this.mSelfAttendeeStatus != calendarEventModel.mSelfAttendeeStatus || this.mOwnerAttendeeId != calendarEventModel.mOwnerAttendeeId) {
            return false;
        }
        if (this.mSyncAccount == null) {
            if (calendarEventModel.mSyncAccount != null) {
                return false;
            }
        } else if (!this.mSyncAccount.equals(calendarEventModel.mSyncAccount)) {
            return false;
        }
        if (this.mSyncAccountType == null) {
            if (calendarEventModel.mSyncAccountType != null) {
                return false;
            }
        } else if (!this.mSyncAccountType.equals(calendarEventModel.mSyncAccountType)) {
            return false;
        }
        if (this.mSyncId == null) {
            if (calendarEventModel.mSyncId != null) {
                return false;
            }
        } else if (!this.mSyncId.equals(calendarEventModel.mSyncId)) {
            return false;
        }
        if (this.mTimezone == null) {
            if (calendarEventModel.mTimezone != null) {
                return false;
            }
        } else if (!this.mTimezone.equals(calendarEventModel.mTimezone)) {
            return false;
        }
        if (this.mTimezone2 == null) {
            if (calendarEventModel.mTimezone2 != null) {
                return false;
            }
        } else if (!this.mTimezone2.equals(calendarEventModel.mTimezone2)) {
            return false;
        }
        if (this.mAvailability != calendarEventModel.mAvailability) {
            return false;
        }
        if (this.mUri == null) {
            if (calendarEventModel.mUri != null) {
                return false;
            }
        } else if (!this.mUri.equals(calendarEventModel.mUri)) {
            return false;
        }
        return this.mAccessLevel == calendarEventModel.mAccessLevel && this.mEventStatus == calendarEventModel.mEventStatus && this.mEventColor == calendarEventModel.mEventColor && this.mEventColorInitialized == calendarEventModel.mEventColorInitialized && this.mIsLunar == calendarEventModel.mIsLunar;
    }

    public boolean normalizeReminders() {
        if (this.mReminders.size() <= 1) {
            return true;
        }
        Collections.sort(this.mReminders);
        ReminderEntry reminderEntry = this.mReminders.get(this.mReminders.size() - 1);
        int size = this.mReminders.size() - 2;
        while (size >= 0) {
            ReminderEntry reminderEntry2 = this.mReminders.get(size);
            if (reminderEntry.equals(reminderEntry2)) {
                this.mReminders.remove(size + 1);
            }
            size--;
            reminderEntry = reminderEntry2;
        }
        return true;
    }

    public boolean isCalendarColorInitialized() {
        return this.mCalendarColorInitialized;
    }

    public boolean isEventColorInitialized() {
        return this.mEventColorInitialized;
    }

    public int getCalendarColor() {
        return this.mCalendarColor;
    }

    public int getEventColor() {
        return this.mEventColor;
    }

    public void setCalendarColor(int i) {
        this.mCalendarColor = i;
        this.mCalendarColorInitialized = true;
    }

    public void setEventColor(int i) {
        this.mEventColor = i;
        this.mEventColorInitialized = true;
    }

    public int[] getCalendarEventColors() {
        if (this.mEventColorCache != null) {
            return this.mEventColorCache.getColorArray(this.mCalendarAccountName, this.mCalendarAccountType);
        }
        return null;
    }

    public int getEventColorKey() {
        if (this.mEventColorCache != null) {
            return this.mEventColorCache.getColorKey(this.mCalendarAccountName, this.mCalendarAccountType, this.mEventColor);
        }
        return -1;
    }
}
