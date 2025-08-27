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
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class ZenModeEventRuleSettings extends ZenModeRuleSettingsBase {
    private static final Comparator<CalendarInfo> CALENDAR_NAME = new Comparator<CalendarInfo>() { // from class: com.android.settings.notification.ZenModeEventRuleSettings.3
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(CalendarInfo calendarInfo, CalendarInfo calendarInfo2) {
            return calendarInfo.name.compareTo(calendarInfo2.name);
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

    @Override // com.android.settings.notification.ZenModeRuleSettingsBase
    protected boolean setRule(AutomaticZenRule automaticZenRule) {
        this.mEvent = automaticZenRule != null ? ZenModeConfig.tryParseEventConditionId(automaticZenRule.getConditionId()) : null;
        return this.mEvent != null;
    }

    @Override // com.android.settings.notification.ZenModeRuleSettingsBase, com.android.settings.notification.ZenModeSettingsBase, com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
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

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_event_rule_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mHeader = new ZenAutomaticRuleHeaderPreferenceController(context, this, getLifecycle());
        this.mSwitch = new ZenAutomaticRuleSwitchPreferenceController(context, this, getLifecycle());
        arrayList.add(this.mHeader);
        arrayList.add(this.mSwitch);
        return arrayList;
    }

    private void reloadCalendar() {
        this.mCalendars = getCalendars(this.mContext);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList.add(getString(R.string.zen_mode_event_rule_calendar_any));
        boolean z = false;
        arrayList2.add(key(0, null));
        String str = this.mEvent != null ? this.mEvent.calendar : null;
        for (CalendarInfo calendarInfo : this.mCalendars) {
            arrayList.add(calendarInfo.name);
            arrayList2.add(key(calendarInfo));
            if (str != null && str.equals(calendarInfo.name)) {
                z = true;
            }
        }
        if (str != null && !z) {
            arrayList.add(str);
            arrayList2.add(key(this.mEvent.userId, str));
        }
        this.mCalendar.setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
        this.mCalendar.setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[arrayList2.size()]));
    }

    @Override // com.android.settings.notification.ZenModeRuleSettingsBase
    protected void onCreateInternal() {
        this.mCreate = true;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mCalendar = (DropDownPreference) preferenceScreen.findPreference("calendar");
        this.mCalendar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.notification.ZenModeEventRuleSettings.1
            @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) {
                String str = (String) obj;
                if (str.equals(ZenModeEventRuleSettings.key(ZenModeEventRuleSettings.this.mEvent))) {
                    return false;
                }
                int iIndexOf = str.indexOf(58);
                ZenModeEventRuleSettings.this.mEvent.userId = Integer.parseInt(str.substring(0, iIndexOf));
                ZenModeEventRuleSettings.this.mEvent.calendar = str.substring(iIndexOf + 1);
                if (ZenModeEventRuleSettings.this.mEvent.calendar.isEmpty()) {
                    ZenModeEventRuleSettings.this.mEvent.calendar = null;
                }
                ZenModeEventRuleSettings.this.updateRule(ZenModeConfig.toEventConditionId(ZenModeEventRuleSettings.this.mEvent));
                return true;
            }
        });
        this.mReply = (DropDownPreference) preferenceScreen.findPreference("reply");
        this.mReply.setEntries(new CharSequence[]{getString(R.string.zen_mode_event_rule_reply_any_except_no), getString(R.string.zen_mode_event_rule_reply_yes_or_maybe), getString(R.string.zen_mode_event_rule_reply_yes)});
        this.mReply.setEntryValues(new CharSequence[]{Integer.toString(0), Integer.toString(1), Integer.toString(2)});
        this.mReply.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.notification.ZenModeEventRuleSettings.2
            @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
                int i = Integer.parseInt((String) obj);
                if (i == ZenModeEventRuleSettings.this.mEvent.reply) {
                    return false;
                }
                ZenModeEventRuleSettings.this.mEvent.reply = i;
                ZenModeEventRuleSettings.this.updateRule(ZenModeConfig.toEventConditionId(ZenModeEventRuleSettings.this.mEvent));
                return true;
            }
        });
        reloadCalendar();
        updateControlsInternal();
    }

    @Override // com.android.settings.notification.ZenModeRuleSettingsBase
    protected void updateControlsInternal() {
        this.mCalendar.setValue(key(this.mEvent));
        this.mReply.setValue(Integer.toString(this.mEvent.reply));
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 146;
    }

    private static List<CalendarInfo> getCalendars(Context context) throws Throwable {
        ArrayList arrayList = new ArrayList();
        Iterator<UserHandle> it = UserManager.get(context).getUserProfiles().iterator();
        while (it.hasNext()) {
            Context contextForUser = getContextForUser(context, it.next());
            if (contextForUser != null) {
                addCalendars(contextForUser, arrayList);
            }
        }
        Collections.sort(arrayList, CALENDAR_NAME);
        return arrayList;
    }

    private static Context getContextForUser(Context context, UserHandle userHandle) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static void addCalendars(Context context, List<CalendarInfo> list) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, new String[]{"_id", "calendar_displayName", "(account_name=ownerAccount) AS \"primary\""}, "\"primary\" = 1", null, null);
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    try {
                        CalendarInfo calendarInfo = new CalendarInfo();
                        calendarInfo.name = cursorQuery.getString(1);
                        calendarInfo.userId = context.getUserId();
                        list.add(calendarInfo);
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            if (cursorQuery == null) {
                return;
            }
            cursorQuery.close();
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private static String key(CalendarInfo calendarInfo) {
        return key(calendarInfo.userId, calendarInfo.name);
    }

    private static String key(ZenModeConfig.EventInfo eventInfo) {
        return key(eventInfo.userId, eventInfo.calendar);
    }

    private static String key(int i, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(ZenModeConfig.EventInfo.resolveUserId(i));
        sb.append(":");
        if (str == null) {
            str = "";
        }
        sb.append(str);
        return sb.toString();
    }
}
