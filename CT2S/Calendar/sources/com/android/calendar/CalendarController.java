package com.android.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsActivity;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

public class CalendarController {
    private static WeakHashMap<Context, WeakReference<CalendarController>> instances = new WeakHashMap<>();
    private final Context mContext;
    private int mDetailViewType;
    private Pair<Integer, EventHandler> mFirstEventHandler;
    private Pair<Integer, EventHandler> mToBeAddedFirstEventHandler;
    private final LinkedHashMap<Integer, EventHandler> eventHandlers = new LinkedHashMap<>(5);
    private final LinkedList<Integer> mToBeRemovedEventHandlers = new LinkedList<>();
    private final LinkedHashMap<Integer, EventHandler> mToBeAddedEventHandlers = new LinkedHashMap<>();
    private volatile int mDispatchInProgressCounter = 0;
    private final WeakHashMap<Object, Long> filters = new WeakHashMap<>(1);
    private int mViewType = -1;
    private int mPreviousViewType = -1;
    private long mEventId = -1;
    private final Time mTime = new Time();
    private long mDateFlags = 0;
    private final Runnable mUpdateTimezone = new Runnable() {
        @Override
        public void run() {
            CalendarController.this.mTime.switchTimezone(Utils.getTimeZone(CalendarController.this.mContext, this));
        }
    };

    public interface EventHandler {
        long getSupportedEventTypes();

        void handleEvent(EventInfo eventInfo);
    }

    public static class EventInfo {
        public long calendarId;
        public ComponentName componentName;
        public Time endTime;
        public String eventTitle;
        public long eventType;
        public long extraLong;
        public long id;
        public String query;
        public Time selectedTime;
        public Time startTime;
        public int viewType;
        public int x;
        public int y;

        public boolean isAllDay() {
            if (this.eventType == 2) {
                return (this.extraLong & 256) != 0;
            }
            Log.wtf("CalendarController", "illegal call to isAllDay , wrong event type " + this.eventType);
            return false;
        }

        public int getResponse() {
            if (this.eventType != 2) {
                Log.wtf("CalendarController", "illegal call to getResponse , wrong event type " + this.eventType);
                return 0;
            }
            int response = (int) (this.extraLong & 255);
            switch (response) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                case 5:
                case 6:
                case 7:
                default:
                    Log.wtf("CalendarController", "Unknown attendee response " + response);
                    break;
                case 4:
                    break;
                case 8:
                    break;
            }
            return 0;
        }

        public static long buildViewExtraLong(int response, boolean allDay) {
            long extra = allDay ? 256L : 0L;
            switch (response) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                default:
                    Log.wtf("CalendarController", "Unknown attendee response " + response);
                    break;
                case 4:
                    break;
            }
            return extra | 1;
        }
    }

    public static CalendarController getInstance(Context context) throws Throwable {
        CalendarController controller;
        CalendarController controller2;
        synchronized (instances) {
            try {
                WeakReference<CalendarController> weakController = instances.get(context);
                if (weakController == null) {
                    controller = null;
                } else {
                    CalendarController controller3 = weakController.get();
                    controller = controller3;
                }
                if (controller == null) {
                    try {
                        controller2 = new CalendarController(context);
                        instances.put(context, new WeakReference<>(controller2));
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } else {
                    controller2 = controller;
                }
                return controller2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public static void removeInstance(Context context) {
        instances.remove(context);
    }

    private CalendarController(Context context) {
        this.mDetailViewType = -1;
        this.mContext = context;
        this.mUpdateTimezone.run();
        this.mTime.setToNow();
        this.mDetailViewType = Utils.getSharedPreference(this.mContext, "preferred_detailedView", 2);
    }

    public void sendEventRelatedEvent(Object sender, long eventType, long eventId, long startMillis, long endMillis, int x, int y, long selectedMillis) {
        sendEventRelatedEventWithExtra(sender, eventType, eventId, startMillis, endMillis, x, y, EventInfo.buildViewExtraLong(0, false), selectedMillis);
    }

    public void sendEventRelatedEventWithExtra(Object sender, long eventType, long eventId, long startMillis, long endMillis, int x, int y, long extraLong, long selectedMillis) {
        sendEventRelatedEventWithExtraWithTitleWithCalendarId(sender, eventType, eventId, startMillis, endMillis, x, y, extraLong, selectedMillis, null, -1L);
    }

    public void sendEventRelatedEventWithExtraWithTitleWithCalendarId(Object sender, long eventType, long eventId, long startMillis, long endMillis, int x, int y, long extraLong, long selectedMillis, String title, long calendarId) {
        EventInfo info = new EventInfo();
        info.eventType = eventType;
        if (eventType == 8 || eventType == 4) {
            info.viewType = 0;
        }
        info.id = eventId;
        info.startTime = new Time(Utils.getTimeZone(this.mContext, this.mUpdateTimezone));
        info.startTime.set(startMillis);
        if (selectedMillis != -1) {
            info.selectedTime = new Time(Utils.getTimeZone(this.mContext, this.mUpdateTimezone));
            info.selectedTime.set(selectedMillis);
        } else {
            info.selectedTime = info.startTime;
        }
        info.endTime = new Time(Utils.getTimeZone(this.mContext, this.mUpdateTimezone));
        info.endTime.set(endMillis);
        info.x = x;
        info.y = y;
        info.extraLong = extraLong;
        info.eventTitle = title;
        info.calendarId = calendarId;
        sendEvent(sender, info);
    }

    public void sendEvent(Object sender, long eventType, Time start, Time end, long eventId, int viewType) {
        sendEvent(sender, eventType, start, end, start, eventId, viewType, 2L, null, null);
    }

    public void sendEvent(Object sender, long eventType, Time start, Time end, long eventId, int viewType, long extraLong, String query, ComponentName componentName) {
        sendEvent(sender, eventType, start, end, start, eventId, viewType, extraLong, query, componentName);
    }

    public void sendEvent(Object sender, long eventType, Time start, Time end, Time selected, long eventId, int viewType, long extraLong, String query, ComponentName componentName) {
        EventInfo info = new EventInfo();
        info.eventType = eventType;
        info.startTime = start;
        info.selectedTime = selected;
        info.endTime = end;
        info.id = eventId;
        info.viewType = viewType;
        info.query = query;
        info.componentName = componentName;
        info.extraLong = extraLong;
        sendEvent(sender, info);
    }

    public void sendEvent(Object sender, EventInfo event) {
        EventHandler handler;
        Long filteredTypes = this.filters.get(sender);
        if (filteredTypes == null || (filteredTypes.longValue() & event.eventType) == 0) {
            this.mPreviousViewType = this.mViewType;
            if (event.viewType == -1) {
                event.viewType = this.mDetailViewType;
                this.mViewType = this.mDetailViewType;
            } else if (event.viewType == 0) {
                event.viewType = this.mViewType;
            } else if (event.viewType != 5) {
                this.mViewType = event.viewType;
                if (event.viewType == 1 || event.viewType == 2 || (Utils.getAllowWeekForDetailView() && event.viewType == 3)) {
                    this.mDetailViewType = this.mViewType;
                }
            }
            long startMillis = 0;
            if (event.startTime != null) {
                startMillis = event.startTime.toMillis(false);
            }
            if (event.selectedTime != null && event.selectedTime.toMillis(false) != 0) {
                this.mTime.set(event.selectedTime);
            } else {
                if (startMillis != 0) {
                    long mtimeMillis = this.mTime.toMillis(false);
                    if (mtimeMillis < startMillis || (event.endTime != null && mtimeMillis > event.endTime.toMillis(false))) {
                        this.mTime.set(event.startTime);
                    }
                }
                event.selectedTime = this.mTime;
            }
            if (event.eventType == 1024) {
                this.mDateFlags = event.extraLong;
            }
            if (startMillis == 0) {
                event.startTime = this.mTime;
            }
            if ((event.eventType & 13) != 0) {
                if (event.id > 0) {
                    this.mEventId = event.id;
                } else {
                    this.mEventId = -1L;
                }
            }
            boolean handled = false;
            synchronized (this) {
                this.mDispatchInProgressCounter++;
                if (this.mFirstEventHandler != null && (handler = (EventHandler) this.mFirstEventHandler.second) != null && (handler.getSupportedEventTypes() & event.eventType) != 0 && !this.mToBeRemovedEventHandlers.contains(this.mFirstEventHandler.first)) {
                    handler.handleEvent(event);
                    handled = true;
                }
                for (Map.Entry<Integer, EventHandler> entry : this.eventHandlers.entrySet()) {
                    int key = entry.getKey().intValue();
                    if (this.mFirstEventHandler == null || key != ((Integer) this.mFirstEventHandler.first).intValue()) {
                        EventHandler eventHandler = entry.getValue();
                        if (eventHandler != null && (eventHandler.getSupportedEventTypes() & event.eventType) != 0 && !this.mToBeRemovedEventHandlers.contains(Integer.valueOf(key))) {
                            eventHandler.handleEvent(event);
                            handled = true;
                        }
                    }
                }
                this.mDispatchInProgressCounter--;
                if (this.mDispatchInProgressCounter == 0) {
                    if (this.mToBeRemovedEventHandlers.size() > 0) {
                        for (Integer zombie : this.mToBeRemovedEventHandlers) {
                            this.eventHandlers.remove(zombie);
                            if (this.mFirstEventHandler != null && zombie.equals(this.mFirstEventHandler.first)) {
                                this.mFirstEventHandler = null;
                            }
                        }
                        this.mToBeRemovedEventHandlers.clear();
                    }
                    if (this.mToBeAddedFirstEventHandler != null) {
                        this.mFirstEventHandler = this.mToBeAddedFirstEventHandler;
                        this.mToBeAddedFirstEventHandler = null;
                    }
                    if (this.mToBeAddedEventHandlers.size() > 0) {
                        for (Map.Entry<Integer, EventHandler> food : this.mToBeAddedEventHandlers.entrySet()) {
                            this.eventHandlers.put(food.getKey(), food.getValue());
                        }
                    }
                }
            }
            if (handled) {
                return;
            }
            if (event.eventType == 64) {
                launchSettings();
                return;
            }
            if (event.eventType == 2048) {
                launchSelectVisibleCalendars();
                return;
            }
            long endTime = event.endTime == null ? -1L : event.endTime.toMillis(false);
            if (event.eventType == 1) {
                launchCreateEvent(event.startTime.toMillis(false), endTime, event.extraLong == 16, event.eventTitle, event.calendarId);
                return;
            }
            if (event.eventType == 2) {
                launchViewEvent(event.id, event.startTime.toMillis(false), endTime, event.getResponse());
                return;
            }
            if (event.eventType == 8) {
                launchEditEvent(event.id, event.startTime.toMillis(false), endTime, true);
                return;
            }
            if (event.eventType == 4) {
                launchEditEvent(event.id, event.startTime.toMillis(false), endTime, false);
            } else if (event.eventType == 16) {
                launchDeleteEvent(event.id, event.startTime.toMillis(false), endTime);
            } else if (event.eventType == 256) {
                launchSearch(event.id, event.query, event.componentName);
            }
        }
    }

    public void registerEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeAddedEventHandlers.put(Integer.valueOf(key), eventHandler);
            } else {
                this.eventHandlers.put(Integer.valueOf(key), eventHandler);
            }
        }
    }

    public void registerFirstEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            registerEventHandler(key, eventHandler);
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeAddedFirstEventHandler = new Pair<>(Integer.valueOf(key), eventHandler);
            } else {
                this.mFirstEventHandler = new Pair<>(Integer.valueOf(key), eventHandler);
            }
        }
    }

    public void deregisterEventHandler(Integer key) {
        synchronized (this) {
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeRemovedEventHandlers.add(key);
            } else {
                this.eventHandlers.remove(key);
                if (this.mFirstEventHandler != null && this.mFirstEventHandler.first == key) {
                    this.mFirstEventHandler = null;
                }
            }
        }
    }

    public void deregisterAllEventHandlers() {
        synchronized (this) {
            if (this.mDispatchInProgressCounter > 0) {
                this.mToBeRemovedEventHandlers.addAll(this.eventHandlers.keySet());
            } else {
                this.eventHandlers.clear();
                this.mFirstEventHandler = null;
            }
        }
    }

    public long getTime() {
        return this.mTime.toMillis(false);
    }

    public long getDateFlags() {
        return this.mDateFlags;
    }

    public void setTime(long millisTime) {
        this.mTime.set(millisTime);
    }

    public long getEventId() {
        return this.mEventId;
    }

    public int getViewType() {
        return this.mViewType;
    }

    public int getPreviousViewType() {
        return this.mPreviousViewType;
    }

    private void launchSelectVisibleCalendars() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, SelectVisibleCalendarsActivity.class);
        intent.setFlags(537001984);
        this.mContext.startActivity(intent);
    }

    private void launchSettings() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, CalendarSettingsActivity.class);
        intent.setFlags(537001984);
        this.mContext.startActivity(intent);
    }

    private void launchCreateEvent(long startMillis, long endMillis, boolean allDayEvent, String title, long calendarId) {
        Intent intent = generateCreateEventIntent(startMillis, endMillis, allDayEvent, title, calendarId);
        this.mEventId = -1L;
        this.mContext.startActivity(intent);
    }

    public Intent generateCreateEventIntent(long startMillis, long endMillis, boolean allDayEvent, String title, long calendarId) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this.mContext, EditEventActivity.class);
        intent.putExtra("beginTime", startMillis);
        intent.putExtra("endTime", endMillis);
        intent.putExtra("allDay", allDayEvent);
        intent.putExtra("calendar_id", calendarId);
        intent.putExtra("title", title);
        return intent;
    }

    public void launchViewEvent(long eventId, long startMillis, long endMillis, int response) {
        Intent intent = new Intent("android.intent.action.VIEW");
        Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        intent.setData(eventUri);
        intent.setClass(this.mContext, AllInOneActivity.class);
        intent.putExtra("beginTime", startMillis);
        intent.putExtra("endTime", endMillis);
        intent.putExtra("attendeeStatus", response);
        intent.setFlags(67108864);
        this.mContext.startActivity(intent);
    }

    private void launchEditEvent(long eventId, long startMillis, long endMillis, boolean edit) {
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        Intent intent = new Intent("android.intent.action.EDIT", uri);
        intent.putExtra("beginTime", startMillis);
        intent.putExtra("endTime", endMillis);
        intent.setClass(this.mContext, EditEventActivity.class);
        intent.putExtra("editMode", edit);
        this.mEventId = eventId;
        this.mContext.startActivity(intent);
    }

    private void launchDeleteEvent(long eventId, long startMillis, long endMillis) {
        launchDeleteEventAndFinish(null, eventId, startMillis, endMillis, -1);
    }

    private void launchDeleteEventAndFinish(Activity parentActivity, long eventId, long startMillis, long endMillis, int deleteWhich) {
        DeleteEventHelper deleteEventHelper = new DeleteEventHelper(this.mContext, parentActivity, parentActivity != null);
        deleteEventHelper.delete(startMillis, endMillis, eventId, deleteWhich);
    }

    private void launchSearch(long eventId, String query, ComponentName componentName) {
        SearchManager searchManager = (SearchManager) this.mContext.getSystemService("search");
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(componentName);
        Intent intent = new Intent("android.intent.action.SEARCH");
        intent.putExtra("query", query);
        intent.setComponent(searchableInfo.getSearchActivity());
        intent.addFlags(536870912);
        this.mContext.startActivity(intent);
    }

    public void refreshCalendars() {
        Account[] accounts = AccountManager.get(this.mContext).getAccounts();
        Log.d("CalendarController", "Refreshing " + accounts.length + " accounts");
        String authority = CalendarContract.Calendars.CONTENT_URI.getAuthority();
        for (int i = 0; i < accounts.length; i++) {
            if (Log.isLoggable("CalendarController", 3)) {
                Log.d("CalendarController", "Refreshing calendars for: " + accounts[i]);
            }
            Bundle extras = new Bundle();
            extras.putBoolean("force", true);
            ContentResolver.requestSync(accounts[i], authority, extras);
        }
    }

    public void setViewType(int viewType) {
        this.mViewType = viewType;
    }

    public void setEventId(long eventId) {
        this.mEventId = eventId;
    }
}
