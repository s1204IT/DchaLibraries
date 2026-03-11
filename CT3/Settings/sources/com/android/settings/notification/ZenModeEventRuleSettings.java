package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CalendarContract;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZenModeEventRuleSettings extends ZenModeRuleSettingsBase {
    private static final Comparator<CalendarInfo> CALENDAR_NAME = new Comparator<CalendarInfo>() {
        @Override
        public int compare(CalendarInfo lhs, CalendarInfo rhs) {
            return lhs.name.compareTo(rhs.name);
        }
    };
    private DropDownPreference mCalendar;
    private List<CalendarInfo> mCalendars;
    private boolean mCreate;
    private ZenModeConfig.EventInfo mEvent;
    private DropDownPreference mReply;

    public static class CalendarInfo {
        public String name;
        public int userId;
    }

    @Override
    protected boolean setRule(AutomaticZenRule rule) {
        this.mEvent = rule != null ? ZenModeConfig.tryParseEventConditionId(rule.getConditionId()) : null;
        return this.mEvent != null;
    }

    @Override
    protected String getZenModeDependency() {
        return null;
    }

    @Override
    protected int getEnabledToastText() {
        return R.string.zen_event_rule_enabled_toast;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        if (!this.mCreate) {
            reloadCalendar();
        }
        this.mCreate = false;
    }

    private void reloadCalendar() {
        this.mCalendars = getCalendars(this.mContext);
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.zen_mode_event_rule_calendar_any));
        values.add(key(0, null));
        String eventCalendar = this.mEvent != null ? this.mEvent.calendar : null;
        boolean found = false;
        for (CalendarInfo calendar : this.mCalendars) {
            entries.add(calendar.name);
            values.add(key(calendar));
            if (eventCalendar != null && eventCalendar.equals(calendar.name)) {
                found = true;
            }
        }
        if (eventCalendar != null && !found) {
            entries.add(eventCalendar);
            values.add(key(this.mEvent.userId, eventCalendar));
        }
        this.mCalendar.setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
        this.mCalendar.setEntryValues((CharSequence[]) values.toArray(new CharSequence[values.size()]));
    }

    @Override
    protected void onCreateInternal() {
        this.mCreate = true;
        addPreferencesFromResource(R.xml.zen_mode_event_rule_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mCalendar = (DropDownPreference) root.findPreference("calendar");
        this.mCalendar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String calendarKey = (String) newValue;
                if (calendarKey.equals(ZenModeEventRuleSettings.key(ZenModeEventRuleSettings.this.mEvent))) {
                    return false;
                }
                int i = calendarKey.indexOf(58);
                ZenModeEventRuleSettings.this.mEvent.userId = Integer.parseInt(calendarKey.substring(0, i));
                ZenModeEventRuleSettings.this.mEvent.calendar = calendarKey.substring(i + 1);
                if (ZenModeEventRuleSettings.this.mEvent.calendar.isEmpty()) {
                    ZenModeEventRuleSettings.this.mEvent.calendar = null;
                }
                ZenModeEventRuleSettings.this.updateRule(ZenModeConfig.toEventConditionId(ZenModeEventRuleSettings.this.mEvent));
                return true;
            }
        });
        this.mReply = (DropDownPreference) root.findPreference("reply");
        this.mReply.setEntries(new CharSequence[]{getString(R.string.zen_mode_event_rule_reply_any_except_no), getString(R.string.zen_mode_event_rule_reply_yes_or_maybe), getString(R.string.zen_mode_event_rule_reply_yes)});
        this.mReply.setEntryValues(new CharSequence[]{Integer.toString(0), Integer.toString(1), Integer.toString(2)});
        this.mReply.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int reply = Integer.parseInt((String) newValue);
                if (reply == ZenModeEventRuleSettings.this.mEvent.reply) {
                    return false;
                }
                ZenModeEventRuleSettings.this.mEvent.reply = reply;
                ZenModeEventRuleSettings.this.updateRule(ZenModeConfig.toEventConditionId(ZenModeEventRuleSettings.this.mEvent));
                return true;
            }
        });
        reloadCalendar();
        updateControlsInternal();
    }

    @Override
    protected void updateControlsInternal() {
        this.mCalendar.setValue(key(this.mEvent));
        this.mReply.setValue(Integer.toString(this.mEvent.reply));
    }

    @Override
    protected int getMetricsCategory() {
        return 146;
    }

    private static List<CalendarInfo> getCalendars(Context context) {
        List<CalendarInfo> calendars = new ArrayList<>();
        for (UserHandle user : UserManager.get(context).getUserProfiles()) {
            Context userContext = getContextForUser(context, user);
            if (userContext != null) {
                addCalendars(userContext, calendars);
            }
        }
        Collections.sort(calendars, CALENDAR_NAME);
        return calendars;
    }

    private static Context getContextForUser(Context context, UserHandle user) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static void addCalendars(Context context, List<CalendarInfo> outCalendars) {
        String[] projection = {"_id", "calendar_displayName", "(account_name=ownerAccount) AS \"primary\""};
        Cursor cursor = null;
        try {
            Cursor cursor2 = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection, "\"primary\" = 1", null, null);
            if (cursor2 != null) {
                while (cursor2.moveToNext()) {
                    CalendarInfo ci = new CalendarInfo();
                    ci.name = cursor2.getString(1);
                    ci.userId = context.getUserId();
                    outCalendars.add(ci);
                }
                if (cursor2 == null) {
                    return;
                }
                cursor2.close();
                return;
            }
            if (cursor2 == null) {
                return;
            }
            cursor2.close();
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    private static String key(CalendarInfo calendar) {
        return key(calendar.userId, calendar.name);
    }

    public static String key(ZenModeConfig.EventInfo event) {
        return key(event.userId, event.calendar);
    }

    private static String key(int userId, String calendar) {
        StringBuilder sbAppend = new StringBuilder().append(ZenModeConfig.EventInfo.resolveUserId(userId)).append(":");
        if (calendar == null) {
            calendar = "";
        }
        return sbAppend.append(calendar).toString();
    }
}
