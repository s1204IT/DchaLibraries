package com.android.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
            if (!(obj instanceof Attendee)) {
                return false;
            }
            Attendee other = (Attendee) obj;
            return TextUtils.equals(this.mEmail, other.mEmail);
        }

        public Attendee(String name, String email) {
            this(name, email, 0, null, null);
        }

        public Attendee(String name, String email, int status, String identity, String idNamespace) {
            this.mName = name;
            this.mEmail = email;
            this.mStatus = status;
            this.mIdentity = identity;
            this.mIdNamespace = idNamespace;
        }
    }

    public static class ReminderEntry implements Serializable, Comparable<ReminderEntry> {
        private final int mMethod;
        private final int mMinutes;

        public static ReminderEntry valueOf(int minutes, int method) {
            return new ReminderEntry(minutes, method);
        }

        public static ReminderEntry valueOf(int minutes) {
            return valueOf(minutes, 0);
        }

        private ReminderEntry(int minutes, int method) {
            this.mMinutes = minutes;
            this.mMethod = method;
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
            ReminderEntry re = (ReminderEntry) obj;
            if (re.mMinutes == this.mMinutes) {
                return re.mMethod == this.mMethod || (re.mMethod == 0 && this.mMethod == 1) || (re.mMethod == 1 && this.mMethod == 0);
            }
            return false;
        }

        public String toString() {
            return "ReminderEntry min=" + this.mMinutes + " meth=" + this.mMethod;
        }

        @Override
        public int compareTo(ReminderEntry re) {
            if (re.mMinutes != this.mMinutes) {
                return re.mMinutes - this.mMinutes;
            }
            if (re.mMethod != this.mMethod) {
                return this.mMethod - re.mMethod;
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
        this.mReminders = new ArrayList<>();
        this.mDefaultReminders = new ArrayList<>();
        this.mAttendeesList = new LinkedHashMap<>();
        this.mTimezone = TimeZone.getDefault().getID();
    }

    public CalendarEventModel(Context context) {
        this();
        this.mTimezone = Utils.getTimeZone(context, null);
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        String defaultReminder = prefs.getString("preferences_default_reminder", "-1");
        int defaultReminderMins = Integer.parseInt(defaultReminder);
        if (defaultReminderMins != -1) {
            this.mHasAlarm = true;
            this.mReminders.add(ReminderEntry.valueOf(defaultReminderMins));
            this.mDefaultReminders.add(ReminderEntry.valueOf(defaultReminderMins));
        }
    }

    public CalendarEventModel(Context context, Intent intent) {
        this(context);
        if (intent != null) {
            String title = intent.getStringExtra("title");
            if (title != null) {
                this.mTitle = title;
            }
            String location = intent.getStringExtra("eventLocation");
            if (location != null) {
                this.mLocation = location;
            }
            String description = intent.getStringExtra("description");
            if (description != null) {
                this.mDescription = description;
            }
            int availability = intent.getIntExtra("availability", -1);
            if (availability != -1) {
                this.mAvailability = availability;
            }
            int accessLevel = intent.getIntExtra("accessLevel", -1);
            if (accessLevel != -1) {
                this.mAccessLevel = accessLevel > 0 ? accessLevel - 1 : accessLevel;
            }
            String rrule = intent.getStringExtra("rrule");
            if (!TextUtils.isEmpty(rrule)) {
                this.mRrule = rrule;
            }
            String emails = intent.getStringExtra("android.intent.extra.EMAIL");
            if (!TextUtils.isEmpty(emails)) {
                String[] emailArray = emails.split("[ ,;]");
                for (String email : emailArray) {
                    if (!TextUtils.isEmpty(email) && email.contains("@")) {
                        String email2 = email.trim();
                        if (!this.mAttendeesList.containsKey(email2)) {
                            this.mAttendeesList.put(email2, new Attendee("", email2));
                        }
                    }
                }
            }
        }
    }

    public boolean isValid() {
        return (this.mCalendarId == -1 || TextUtils.isEmpty(this.mOwnerAccount)) ? false : true;
    }

    public boolean isEmpty() {
        if (this.mTitle != null && this.mTitle.trim().length() > 0) {
            return false;
        }
        if (this.mLocation == null || this.mLocation.trim().length() <= 0) {
            return this.mDescription == null || this.mDescription.trim().length() <= 0;
        }
        return false;
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

    public void addAttendees(String attendees, Rfc822Validator validator) {
        LinkedHashSet<Rfc822Token> addresses = EditEventHelper.getAddressesFromList(attendees, validator);
        synchronized (this) {
            for (Rfc822Token address : addresses) {
                Attendee attendee = new Attendee(address.getName(), address.getAddress());
                if (TextUtils.isEmpty(attendee.mName)) {
                    attendee.mName = attendee.mEmail;
                }
                addAttendee(attendee);
            }
        }
    }

    public String getAttendeesString() {
        StringBuilder b = new StringBuilder();
        for (Attendee attendee : this.mAttendeesList.values()) {
            String name = attendee.mName;
            String email = attendee.mEmail;
            String status = Integer.toString(attendee.mStatus);
            b.append("name:").append(name);
            b.append(" email:").append(email);
            b.append(" status:").append(status);
        }
        return b.toString();
    }

    public int hashCode() {
        int result = (this.mAllDay ? 1231 : 1237) + 31;
        return (((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((((result * 31) + (this.mAttendeesList == null ? 0 : getAttendeesString().hashCode())) * 31) + ((int) (this.mCalendarId ^ (this.mCalendarId >>> 32)))) * 31) + (this.mDescription == null ? 0 : this.mDescription.hashCode())) * 31) + (this.mDuration == null ? 0 : this.mDuration.hashCode())) * 31) + ((int) (this.mEnd ^ (this.mEnd >>> 32)))) * 31) + (this.mGuestsCanInviteOthers ? 1231 : 1237)) * 31) + (this.mGuestsCanModify ? 1231 : 1237)) * 31) + (this.mGuestsCanSeeGuests ? 1231 : 1237)) * 31) + (this.mOrganizerCanRespond ? 1231 : 1237)) * 31) + (this.mModelUpdatedWithEventCursor ? 1231 : 1237)) * 31) + this.mCalendarAccessLevel) * 31) + (this.mHasAlarm ? 1231 : 1237)) * 31) + (this.mHasAttendeeData ? 1231 : 1237)) * 31) + ((int) (this.mId ^ (this.mId >>> 32)))) * 31) + (this.mIsFirstEventInSeries ? 1231 : 1237)) * 31) + (this.mIsOrganizer ? 1231 : 1237)) * 31) + (this.mLocation == null ? 0 : this.mLocation.hashCode())) * 31) + (this.mOrganizer == null ? 0 : this.mOrganizer.hashCode())) * 31) + (this.mOriginalAllDay == null ? 0 : this.mOriginalAllDay.hashCode())) * 31) + ((int) (this.mOriginalEnd ^ (this.mOriginalEnd >>> 32)))) * 31) + (this.mOriginalSyncId == null ? 0 : this.mOriginalSyncId.hashCode())) * 31) + ((int) (this.mOriginalId ^ (this.mOriginalEnd >>> 32)))) * 31) + ((int) (this.mOriginalStart ^ (this.mOriginalStart >>> 32)))) * 31) + (this.mOriginalTime == null ? 0 : this.mOriginalTime.hashCode())) * 31) + (this.mOwnerAccount == null ? 0 : this.mOwnerAccount.hashCode())) * 31) + (this.mReminders == null ? 0 : this.mReminders.hashCode())) * 31) + (this.mRrule == null ? 0 : this.mRrule.hashCode())) * 31) + this.mSelfAttendeeStatus) * 31) + this.mOwnerAttendeeId) * 31) + ((int) (this.mStart ^ (this.mStart >>> 32)))) * 31) + (this.mSyncAccount == null ? 0 : this.mSyncAccount.hashCode())) * 31) + (this.mSyncAccountType == null ? 0 : this.mSyncAccountType.hashCode())) * 31) + (this.mSyncId == null ? 0 : this.mSyncId.hashCode())) * 31) + (this.mTimezone == null ? 0 : this.mTimezone.hashCode())) * 31) + (this.mTimezone2 == null ? 0 : this.mTimezone2.hashCode())) * 31) + (this.mTitle == null ? 0 : this.mTitle.hashCode())) * 31) + this.mAvailability) * 31) + (this.mUri != null ? this.mUri.hashCode() : 0)) * 31) + this.mAccessLevel) * 31) + this.mEventStatus;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof CalendarEventModel)) {
            CalendarEventModel other = (CalendarEventModel) obj;
            if (!checkOriginalModelFields(other)) {
                return false;
            }
            if (this.mLocation == null) {
                if (other.mLocation != null) {
                    return false;
                }
            } else if (!this.mLocation.equals(other.mLocation)) {
                return false;
            }
            if (this.mTitle == null) {
                if (other.mTitle != null) {
                    return false;
                }
            } else if (!this.mTitle.equals(other.mTitle)) {
                return false;
            }
            if (this.mDescription == null) {
                if (other.mDescription != null) {
                    return false;
                }
            } else if (!this.mDescription.equals(other.mDescription)) {
                return false;
            }
            if (this.mDuration == null) {
                if (other.mDuration != null) {
                    return false;
                }
            } else if (!this.mDuration.equals(other.mDuration)) {
                return false;
            }
            if (this.mEnd == other.mEnd && this.mIsFirstEventInSeries == other.mIsFirstEventInSeries && this.mOriginalEnd == other.mOriginalEnd && this.mOriginalStart == other.mOriginalStart && this.mStart == other.mStart && this.mOriginalId == other.mOriginalId) {
                if (this.mOriginalSyncId == null) {
                    if (other.mOriginalSyncId != null) {
                        return false;
                    }
                } else if (!this.mOriginalSyncId.equals(other.mOriginalSyncId)) {
                    return false;
                }
                return this.mRrule == null ? other.mRrule == null : this.mRrule.equals(other.mRrule);
            }
            return false;
        }
        return false;
    }

    public boolean isUnchanged(CalendarEventModel originalModel) {
        if (this == originalModel) {
            return true;
        }
        if (originalModel == null || !checkOriginalModelFields(originalModel)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mLocation)) {
            if (!TextUtils.isEmpty(originalModel.mLocation)) {
                return false;
            }
        } else if (!this.mLocation.equals(originalModel.mLocation)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mTitle)) {
            if (!TextUtils.isEmpty(originalModel.mTitle)) {
                return false;
            }
        } else if (!this.mTitle.equals(originalModel.mTitle)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mDescription)) {
            if (!TextUtils.isEmpty(originalModel.mDescription)) {
                return false;
            }
        } else if (!this.mDescription.equals(originalModel.mDescription)) {
            return false;
        }
        if (TextUtils.isEmpty(this.mDuration)) {
            if (!TextUtils.isEmpty(originalModel.mDuration)) {
                return false;
            }
        } else if (!this.mDuration.equals(originalModel.mDuration)) {
            return false;
        }
        if (this.mEnd != this.mOriginalEnd || this.mStart != this.mOriginalStart) {
            return false;
        }
        if (this.mOriginalId != originalModel.mOriginalId && this.mOriginalId != originalModel.mId) {
            return false;
        }
        if (TextUtils.isEmpty(this.mRrule)) {
            if (!TextUtils.isEmpty(originalModel.mRrule)) {
                boolean syncIdNotReferenced = this.mOriginalSyncId == null || !this.mOriginalSyncId.equals(originalModel.mSyncId);
                boolean localIdNotReferenced = this.mOriginalId == -1 || this.mOriginalId != originalModel.mId;
                if (syncIdNotReferenced && localIdNotReferenced) {
                    return false;
                }
            }
        } else if (!this.mRrule.equals(originalModel.mRrule)) {
            return false;
        }
        return true;
    }

    protected boolean checkOriginalModelFields(CalendarEventModel originalModel) {
        if (this.mAllDay != originalModel.mAllDay) {
            return false;
        }
        if (this.mAttendeesList == null) {
            if (originalModel.mAttendeesList != null) {
                return false;
            }
        } else if (!this.mAttendeesList.equals(originalModel.mAttendeesList)) {
            return false;
        }
        if (this.mCalendarId != originalModel.mCalendarId || this.mCalendarColor != originalModel.mCalendarColor || this.mCalendarColorInitialized != originalModel.mCalendarColorInitialized || this.mGuestsCanInviteOthers != originalModel.mGuestsCanInviteOthers || this.mGuestsCanModify != originalModel.mGuestsCanModify || this.mGuestsCanSeeGuests != originalModel.mGuestsCanSeeGuests || this.mOrganizerCanRespond != originalModel.mOrganizerCanRespond || this.mCalendarAccessLevel != originalModel.mCalendarAccessLevel || this.mModelUpdatedWithEventCursor != originalModel.mModelUpdatedWithEventCursor || this.mHasAlarm != originalModel.mHasAlarm || this.mHasAttendeeData != originalModel.mHasAttendeeData || this.mId != originalModel.mId || this.mIsOrganizer != originalModel.mIsOrganizer) {
            return false;
        }
        if (this.mOrganizer == null) {
            if (originalModel.mOrganizer != null) {
                return false;
            }
        } else if (!this.mOrganizer.equals(originalModel.mOrganizer)) {
            return false;
        }
        if (this.mOriginalAllDay == null) {
            if (originalModel.mOriginalAllDay != null) {
                return false;
            }
        } else if (!this.mOriginalAllDay.equals(originalModel.mOriginalAllDay)) {
            return false;
        }
        if (this.mOriginalTime == null) {
            if (originalModel.mOriginalTime != null) {
                return false;
            }
        } else if (!this.mOriginalTime.equals(originalModel.mOriginalTime)) {
            return false;
        }
        if (this.mOwnerAccount == null) {
            if (originalModel.mOwnerAccount != null) {
                return false;
            }
        } else if (!this.mOwnerAccount.equals(originalModel.mOwnerAccount)) {
            return false;
        }
        if (this.mReminders == null) {
            if (originalModel.mReminders != null) {
                return false;
            }
        } else if (!this.mReminders.equals(originalModel.mReminders)) {
            return false;
        }
        if (this.mSelfAttendeeStatus != originalModel.mSelfAttendeeStatus || this.mOwnerAttendeeId != originalModel.mOwnerAttendeeId) {
            return false;
        }
        if (this.mSyncAccount == null) {
            if (originalModel.mSyncAccount != null) {
                return false;
            }
        } else if (!this.mSyncAccount.equals(originalModel.mSyncAccount)) {
            return false;
        }
        if (this.mSyncAccountType == null) {
            if (originalModel.mSyncAccountType != null) {
                return false;
            }
        } else if (!this.mSyncAccountType.equals(originalModel.mSyncAccountType)) {
            return false;
        }
        if (this.mSyncId == null) {
            if (originalModel.mSyncId != null) {
                return false;
            }
        } else if (!this.mSyncId.equals(originalModel.mSyncId)) {
            return false;
        }
        if (this.mTimezone == null) {
            if (originalModel.mTimezone != null) {
                return false;
            }
        } else if (!this.mTimezone.equals(originalModel.mTimezone)) {
            return false;
        }
        if (this.mTimezone2 == null) {
            if (originalModel.mTimezone2 != null) {
                return false;
            }
        } else if (!this.mTimezone2.equals(originalModel.mTimezone2)) {
            return false;
        }
        if (this.mAvailability != originalModel.mAvailability) {
            return false;
        }
        if (this.mUri == null) {
            if (originalModel.mUri != null) {
                return false;
            }
        } else if (!this.mUri.equals(originalModel.mUri)) {
            return false;
        }
        return this.mAccessLevel == originalModel.mAccessLevel && this.mEventStatus == originalModel.mEventStatus && this.mEventColor == originalModel.mEventColor && this.mEventColorInitialized == originalModel.mEventColorInitialized;
    }

    public boolean normalizeReminders() {
        if (this.mReminders.size() > 1) {
            Collections.sort(this.mReminders);
            ReminderEntry prev = this.mReminders.get(this.mReminders.size() - 1);
            for (int i = this.mReminders.size() - 2; i >= 0; i--) {
                ReminderEntry cur = this.mReminders.get(i);
                if (prev.equals(cur)) {
                    this.mReminders.remove(i + 1);
                }
                prev = cur;
            }
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

    public void setCalendarColor(int color) {
        this.mCalendarColor = color;
        this.mCalendarColorInitialized = true;
    }

    public void setEventColor(int color) {
        this.mEventColor = color;
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
