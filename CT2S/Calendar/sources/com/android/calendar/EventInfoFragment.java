package com.android.calendar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.DeleteEventHelper;
import com.android.calendar.alerts.QuickResponseActivity;
import com.android.calendar.event.AttendeesView;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.event.EditEventHelper;
import com.android.calendar.event.EventColorPickerDialog;
import com.android.calendar.event.EventViewUtils;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.colorpicker.ColorPickerSwatch;
import com.android.colorpicker.HsvColorComparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventInfoFragment extends DialogFragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, CalendarController.EventHandler, DeleteEventHelper.DeleteNotifyListener, ColorPickerSwatch.OnColorSelectedListener {
    static final String[] CALENDARS_PROJECTION;
    static final String[] COLORS_PROJECTION;
    private static int DIALOG_TOP_MARGIN;
    private static final String[] REMINDERS_PROJECTION;
    private static int mCustomAppIconSize;
    private static int mDialogHeight;
    private static int mDialogWidth;
    private static float mScale;
    private Button emailAttendeesButton;
    ArrayList<CalendarEventModel.Attendee> mAcceptedAttendees;
    private Activity mActivity;
    private boolean mAllDay;
    private ObjectAnimator mAnimateAlpha;
    private int mAttendeeResponseFromIntent;
    private Cursor mAttendeesCursor;
    private String mCalendarAllowedReminders;
    private int mCalendarColor;
    private boolean mCalendarColorInitialized;
    private String mCalendarOwnerAccount;
    private long mCalendarOwnerAttendeeId;
    private Cursor mCalendarsCursor;
    private boolean mCanModifyCalendar;
    private boolean mCanModifyEvent;
    ArrayList<String> mCcEmails;
    private EventColorPickerDialog mColorPickerDialog;
    private int[] mColors;
    private Context mContext;
    private CalendarController mController;
    private int mCurrentColor;
    private boolean mCurrentColorInitialized;
    private int mCurrentColorKey;
    private int mCurrentQuery;
    ArrayList<CalendarEventModel.Attendee> mDeclinedAttendees;
    private int mDefaultReminderMinutes;
    private boolean mDeleteDialogVisible;
    private DeleteEventHelper mDeleteHelper;
    private ExpandableTextView mDesc;
    private boolean mDismissOnResume;
    private SparseIntArray mDisplayColorKeyMap;
    private EditResponseHelper mEditResponseHelper;
    private long mEndMillis;
    private View mErrorMsgView;
    private Cursor mEventCursor;
    private boolean mEventDeletionStarted;
    private long mEventId;
    private String mEventOrganizerDisplayName;
    private String mEventOrganizerEmail;
    private QueryHandler mHandler;
    private boolean mHasAlarm;
    private boolean mHasAttendeeData;
    private View mHeadlines;
    private boolean mIsBusyFreeCalendar;
    private boolean mIsDialog;
    private boolean mIsOrganizer;
    private boolean mIsPaused;
    private boolean mIsRepeating;
    private boolean mIsTabletConfig;
    private final Runnable mLoadingMsgAlphaUpdater;
    private long mLoadingMsgStartTime;
    private View mLoadingMsgView;
    private AttendeesView mLongAttendees;
    private int mMaxReminders;
    private Menu mMenu;
    private int mMinTop;
    private boolean mNoCrossFade;
    ArrayList<CalendarEventModel.Attendee> mNoResponseAttendees;
    private int mNumOfAttendees;
    private int mOriginalAttendeeResponse;
    private int mOriginalColor;
    private boolean mOriginalColorInitialized;
    public ArrayList<CalendarEventModel.ReminderEntry> mOriginalReminders;
    private boolean mOwnerCanRespond;
    private AdapterView.OnItemSelectedListener mReminderChangeListener;
    private ArrayList<String> mReminderMethodLabels;
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMinuteLabels;
    private ArrayList<Integer> mReminderMinuteValues;
    private final ArrayList<LinearLayout> mReminderViews;
    public ArrayList<CalendarEventModel.ReminderEntry> mReminders;
    private Cursor mRemindersCursor;
    private RadioGroup mResponseRadioGroup;
    private ScrollView mScrollView;
    private long mStartMillis;
    private String mSyncAccountName;
    private final Runnable mTZUpdater;
    ArrayList<CalendarEventModel.Attendee> mTentativeAttendees;
    private int mTentativeUserSetResponse;
    private TextView mTitle;
    ArrayList<String> mToEmails;
    public ArrayList<CalendarEventModel.ReminderEntry> mUnsupportedReminders;
    private Uri mUri;
    private boolean mUserModifiedReminders;
    private int mUserSetResponse;
    private View mView;
    private TextView mWhenDateTime;
    private TextView mWhere;
    private int mWhichEvents;
    private int mWindowStyle;
    private int mX;
    private int mY;
    private final Runnable onDeleteRunnable;
    private static final String[] EVENT_PROJECTION = {"_id", "title", "rrule", "allDay", "calendar_id", "dtstart", "_sync_id", "eventTimezone", "description", "eventLocation", "calendar_access_level", "calendar_color", "eventColor", "hasAttendeeData", "organizer", "hasAlarm", "maxReminders", "allowedReminders", "customAppPackage", "customAppUri", "dtend", "duration", "original_sync_id"};
    private static final String[] ATTENDEES_PROJECTION = {"_id", "attendeeName", "attendeeEmail", "attendeeRelationship", "attendeeStatus", "attendeeIdentity", "attendeeIdNamespace"};

    static int access$3476(EventInfoFragment x0, int x1) {
        int i = x0.mCurrentQuery | x1;
        x0.mCurrentQuery = i;
        return i;
    }

    static {
        if (!Utils.isJellybeanOrLater()) {
            EVENT_PROJECTION[18] = "_id";
            EVENT_PROJECTION[19] = "_id";
            ATTENDEES_PROJECTION[5] = "_id";
            ATTENDEES_PROJECTION[6] = "_id";
        }
        REMINDERS_PROJECTION = new String[]{"_id", "minutes", "method"};
        CALENDARS_PROJECTION = new String[]{"_id", "calendar_displayName", "ownerAccount", "canOrganizerRespond", "account_name", "account_type"};
        COLORS_PROJECTION = new String[]{"_id", "color", "color_index"};
        mScale = 0.0f;
        mCustomAppIconSize = 32;
        mDialogWidth = 500;
        mDialogHeight = 600;
        DIALOG_TOP_MARGIN = 8;
    }

    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            View button;
            Activity activity = EventInfoFragment.this.getActivity();
            if (activity != null && !activity.isFinishing()) {
                switch (token) {
                    case 1:
                        EventInfoFragment.this.mEventCursor = Utils.matrixCursorFromCursor(cursor);
                        if (!EventInfoFragment.this.initEventCursor()) {
                            EventInfoFragment.this.displayEventNotFound();
                            return;
                        }
                        if (!EventInfoFragment.this.mCalendarColorInitialized) {
                            EventInfoFragment.this.mCalendarColor = Utils.getDisplayColorFromColor(EventInfoFragment.this.mEventCursor.getInt(11));
                            EventInfoFragment.this.mCalendarColorInitialized = true;
                        }
                        if (!EventInfoFragment.this.mOriginalColorInitialized) {
                            EventInfoFragment.this.mOriginalColor = EventInfoFragment.this.mEventCursor.isNull(12) ? EventInfoFragment.this.mCalendarColor : Utils.getDisplayColorFromColor(EventInfoFragment.this.mEventCursor.getInt(12));
                            EventInfoFragment.this.mOriginalColorInitialized = true;
                        }
                        if (!EventInfoFragment.this.mCurrentColorInitialized) {
                            EventInfoFragment.this.mCurrentColor = EventInfoFragment.this.mOriginalColor;
                            EventInfoFragment.this.mCurrentColorInitialized = true;
                        }
                        EventInfoFragment.this.updateEvent(EventInfoFragment.this.mView);
                        EventInfoFragment.this.prepareReminders();
                        Uri uri = CalendarContract.Calendars.CONTENT_URI;
                        String[] args = {Long.toString(EventInfoFragment.this.mEventCursor.getLong(4))};
                        startQuery(2, null, uri, EventInfoFragment.CALENDARS_PROJECTION, "_id=?", args, null);
                        break;
                        break;
                    case 2:
                        EventInfoFragment.this.mCalendarsCursor = Utils.matrixCursorFromCursor(cursor);
                        EventInfoFragment.this.updateCalendar(EventInfoFragment.this.mView);
                        EventInfoFragment.this.updateTitle();
                        String[] args2 = {EventInfoFragment.this.mCalendarsCursor.getString(4), EventInfoFragment.this.mCalendarsCursor.getString(5)};
                        Uri uri2 = CalendarContract.Colors.CONTENT_URI;
                        startQuery(64, null, uri2, EventInfoFragment.COLORS_PROJECTION, "account_name=? AND account_type=? AND color_type=1", args2, null);
                        if (EventInfoFragment.this.mIsBusyFreeCalendar) {
                            EventInfoFragment.this.sendAccessibilityEventIfQueryDone(4);
                        } else {
                            String[] args3 = {Long.toString(EventInfoFragment.this.mEventId)};
                            Uri uri3 = CalendarContract.Attendees.CONTENT_URI;
                            startQuery(4, null, uri3, EventInfoFragment.ATTENDEES_PROJECTION, "event_id=?", args3, "attendeeName ASC, attendeeEmail ASC");
                        }
                        if (!EventInfoFragment.this.mHasAlarm) {
                            EventInfoFragment.this.sendAccessibilityEventIfQueryDone(16);
                        } else {
                            String[] args4 = {Long.toString(EventInfoFragment.this.mEventId)};
                            Uri uri4 = CalendarContract.Reminders.CONTENT_URI;
                            startQuery(16, null, uri4, EventInfoFragment.REMINDERS_PROJECTION, "event_id=?", args4, null);
                        }
                        break;
                    case 4:
                        EventInfoFragment.this.mAttendeesCursor = Utils.matrixCursorFromCursor(cursor);
                        EventInfoFragment.this.initAttendeesCursor(EventInfoFragment.this.mView);
                        EventInfoFragment.this.updateResponse(EventInfoFragment.this.mView);
                        break;
                    case 8:
                        SpannableStringBuilder sb = new SpannableStringBuilder();
                        String calendarName = EventInfoFragment.this.mCalendarsCursor.getString(1);
                        sb.append((CharSequence) calendarName);
                        String email = EventInfoFragment.this.mCalendarsCursor.getString(2);
                        if (cursor.getCount() > 1 && !calendarName.equalsIgnoreCase(email) && Utils.isValidEmail(email)) {
                            sb.append(" (").append((CharSequence) email).append((CharSequence) ")");
                        }
                        EventInfoFragment.this.setVisibilityCommon(EventInfoFragment.this.mView, R.id.calendar_container, 0);
                        EventInfoFragment.this.setTextCommon(EventInfoFragment.this.mView, R.id.calendar_name, sb);
                        break;
                    case 16:
                        EventInfoFragment.this.mRemindersCursor = Utils.matrixCursorFromCursor(cursor);
                        EventInfoFragment.this.initReminders(EventInfoFragment.this.mView, EventInfoFragment.this.mRemindersCursor);
                        break;
                    case 32:
                        if (cursor.getCount() > 1) {
                            String displayName = EventInfoFragment.this.mCalendarsCursor.getString(1);
                            EventInfoFragment.this.mHandler.startQuery(8, null, CalendarContract.Calendars.CONTENT_URI, EventInfoFragment.CALENDARS_PROJECTION, "calendar_displayName=?", new String[]{displayName}, null);
                        } else {
                            EventInfoFragment.this.setVisibilityCommon(EventInfoFragment.this.mView, R.id.calendar_container, 8);
                            EventInfoFragment.access$3476(EventInfoFragment.this, 8);
                        }
                        break;
                    case 64:
                        ArrayList<Integer> colors = new ArrayList<>();
                        if (cursor.moveToFirst()) {
                            do {
                                int colorKey = cursor.getInt(2);
                                int rawColor = cursor.getInt(1);
                                int displayColor = Utils.getDisplayColorFromColor(rawColor);
                                EventInfoFragment.this.mDisplayColorKeyMap.put(displayColor, colorKey);
                                colors.add(Integer.valueOf(displayColor));
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                        Integer[] sortedColors = new Integer[colors.size()];
                        Arrays.sort(colors.toArray(sortedColors), new HsvColorComparator());
                        EventInfoFragment.this.mColors = new int[sortedColors.length];
                        for (int i = 0; i < sortedColors.length; i++) {
                            EventInfoFragment.this.mColors[i] = sortedColors[i].intValue();
                            float[] hsv = new float[3];
                            Color.colorToHSV(EventInfoFragment.this.mColors[i], hsv);
                        }
                        if (EventInfoFragment.this.mCanModifyCalendar && (button = EventInfoFragment.this.mView.findViewById(R.id.change_color)) != null && EventInfoFragment.this.mColors.length > 0) {
                            button.setEnabled(true);
                            button.setVisibility(0);
                        }
                        EventInfoFragment.this.updateMenu();
                        break;
                }
                cursor.close();
                EventInfoFragment.this.sendAccessibilityEventIfQueryDone(token);
                if (EventInfoFragment.this.mCurrentQuery == 127) {
                    if (EventInfoFragment.this.mLoadingMsgView.getAlpha() == 1.0f) {
                        long timeDiff = 600 - (System.currentTimeMillis() - EventInfoFragment.this.mLoadingMsgStartTime);
                        if (timeDiff > 0) {
                            EventInfoFragment.this.mAnimateAlpha.setStartDelay(timeDiff);
                        }
                    }
                    if (EventInfoFragment.this.mAnimateAlpha.isRunning() || EventInfoFragment.this.mAnimateAlpha.isStarted() || EventInfoFragment.this.mNoCrossFade) {
                        EventInfoFragment.this.mScrollView.setAlpha(1.0f);
                        EventInfoFragment.this.mLoadingMsgView.setVisibility(8);
                        return;
                    } else {
                        EventInfoFragment.this.mAnimateAlpha.start();
                        return;
                    }
                }
                return;
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void sendAccessibilityEventIfQueryDone(int token) {
        this.mCurrentQuery |= token;
        if (this.mCurrentQuery == 127) {
            sendAccessibilityEvent();
        }
    }

    public EventInfoFragment(Context context, Uri uri, long startMillis, long endMillis, int attendeeResponse, boolean isDialog, int windowStyle, ArrayList<CalendarEventModel.ReminderEntry> reminders) {
        this.mWindowStyle = 1;
        this.mCurrentQuery = 0;
        this.mEventOrganizerDisplayName = "";
        this.mCalendarOwnerAttendeeId = -1L;
        this.mDeleteDialogVisible = false;
        this.mAttendeeResponseFromIntent = 0;
        this.mUserSetResponse = 0;
        this.mWhichEvents = -1;
        this.mTentativeUserSetResponse = 0;
        this.mEventDeletionStarted = false;
        this.mMenu = null;
        this.mDisplayColorKeyMap = new SparseIntArray();
        this.mOriginalColor = -1;
        this.mOriginalColorInitialized = false;
        this.mCalendarColor = -1;
        this.mCalendarColorInitialized = false;
        this.mCurrentColor = -1;
        this.mCurrentColorInitialized = false;
        this.mCurrentColorKey = -1;
        this.mNoCrossFade = false;
        this.mAcceptedAttendees = new ArrayList<>();
        this.mDeclinedAttendees = new ArrayList<>();
        this.mTentativeAttendees = new ArrayList<>();
        this.mNoResponseAttendees = new ArrayList<>();
        this.mToEmails = new ArrayList<>();
        this.mCcEmails = new ArrayList<>();
        this.mReminderViews = new ArrayList<>(0);
        this.mOriginalReminders = new ArrayList<>();
        this.mUnsupportedReminders = new ArrayList<>();
        this.mUserModifiedReminders = false;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                EventInfoFragment.this.updateEvent(EventInfoFragment.this.mView);
            }
        };
        this.mLoadingMsgAlphaUpdater = new Runnable() {
            @Override
            public void run() {
                if (!EventInfoFragment.this.mAnimateAlpha.isRunning() && EventInfoFragment.this.mScrollView.getAlpha() == 0.0f) {
                    EventInfoFragment.this.mLoadingMsgStartTime = System.currentTimeMillis();
                    EventInfoFragment.this.mLoadingMsgView.setAlpha(1.0f);
                }
            }
        };
        this.mIsDialog = false;
        this.mIsPaused = true;
        this.mDismissOnResume = false;
        this.mX = -1;
        this.mY = -1;
        this.onDeleteRunnable = new Runnable() {
            @Override
            public void run() {
                if (EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mDismissOnResume = true;
                } else if (EventInfoFragment.this.isVisible()) {
                    EventInfoFragment.this.dismiss();
                }
            }
        };
        Resources r = context.getResources();
        if (mScale == 0.0f) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1.0f) {
                mCustomAppIconSize = (int) (mCustomAppIconSize * mScale);
                if (isDialog) {
                    DIALOG_TOP_MARGIN = (int) (DIALOG_TOP_MARGIN * mScale);
                }
            }
        }
        if (isDialog) {
            setDialogSize(r);
        }
        this.mIsDialog = isDialog;
        setStyle(1, 0);
        this.mUri = uri;
        this.mStartMillis = startMillis;
        this.mEndMillis = endMillis;
        this.mAttendeeResponseFromIntent = attendeeResponse;
        this.mWindowStyle = windowStyle;
        this.mReminders = reminders;
    }

    public EventInfoFragment() {
        this.mWindowStyle = 1;
        this.mCurrentQuery = 0;
        this.mEventOrganizerDisplayName = "";
        this.mCalendarOwnerAttendeeId = -1L;
        this.mDeleteDialogVisible = false;
        this.mAttendeeResponseFromIntent = 0;
        this.mUserSetResponse = 0;
        this.mWhichEvents = -1;
        this.mTentativeUserSetResponse = 0;
        this.mEventDeletionStarted = false;
        this.mMenu = null;
        this.mDisplayColorKeyMap = new SparseIntArray();
        this.mOriginalColor = -1;
        this.mOriginalColorInitialized = false;
        this.mCalendarColor = -1;
        this.mCalendarColorInitialized = false;
        this.mCurrentColor = -1;
        this.mCurrentColorInitialized = false;
        this.mCurrentColorKey = -1;
        this.mNoCrossFade = false;
        this.mAcceptedAttendees = new ArrayList<>();
        this.mDeclinedAttendees = new ArrayList<>();
        this.mTentativeAttendees = new ArrayList<>();
        this.mNoResponseAttendees = new ArrayList<>();
        this.mToEmails = new ArrayList<>();
        this.mCcEmails = new ArrayList<>();
        this.mReminderViews = new ArrayList<>(0);
        this.mOriginalReminders = new ArrayList<>();
        this.mUnsupportedReminders = new ArrayList<>();
        this.mUserModifiedReminders = false;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                EventInfoFragment.this.updateEvent(EventInfoFragment.this.mView);
            }
        };
        this.mLoadingMsgAlphaUpdater = new Runnable() {
            @Override
            public void run() {
                if (!EventInfoFragment.this.mAnimateAlpha.isRunning() && EventInfoFragment.this.mScrollView.getAlpha() == 0.0f) {
                    EventInfoFragment.this.mLoadingMsgStartTime = System.currentTimeMillis();
                    EventInfoFragment.this.mLoadingMsgView.setAlpha(1.0f);
                }
            }
        };
        this.mIsDialog = false;
        this.mIsPaused = true;
        this.mDismissOnResume = false;
        this.mX = -1;
        this.mY = -1;
        this.onDeleteRunnable = new Runnable() {
            @Override
            public void run() {
                if (EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mDismissOnResume = true;
                } else if (EventInfoFragment.this.isVisible()) {
                    EventInfoFragment.this.dismiss();
                }
            }
        };
    }

    public EventInfoFragment(Context context, long eventId, long startMillis, long endMillis, int attendeeResponse, boolean isDialog, int windowStyle, ArrayList<CalendarEventModel.ReminderEntry> reminders) {
        this(context, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), startMillis, endMillis, attendeeResponse, isDialog, windowStyle, reminders);
        this.mEventId = eventId;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mReminderChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer prevValue = (Integer) parent.getTag();
                if (prevValue == null || prevValue.intValue() != position) {
                    parent.setTag(Integer.valueOf(position));
                    EventInfoFragment.this.mUserModifiedReminders = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        if (savedInstanceState != null) {
            this.mIsDialog = savedInstanceState.getBoolean("key_fragment_is_dialog", false);
            this.mWindowStyle = savedInstanceState.getInt("key_window_style", 1);
        }
        if (this.mIsDialog) {
            applyDialogParams();
        }
        Activity activity = getActivity();
        this.mContext = activity;
        this.mColorPickerDialog = (EventColorPickerDialog) activity.getFragmentManager().findFragmentByTag("EventColorPickerDialog");
        if (this.mColorPickerDialog != null) {
            this.mColorPickerDialog.setOnColorSelectedListener(this);
        }
    }

    private void applyDialogParams() {
        Dialog dialog = getDialog();
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        window.addFlags(2);
        WindowManager.LayoutParams a = window.getAttributes();
        a.dimAmount = 0.4f;
        a.width = mDialogWidth;
        a.height = mDialogHeight;
        if (this.mX != -1 || this.mY != -1) {
            a.x = this.mX - (mDialogWidth / 2);
            a.y = this.mY - (mDialogHeight / 2);
            if (a.y < this.mMinTop) {
                a.y = this.mMinTop + DIALOG_TOP_MARGIN;
            }
            a.gravity = 51;
        }
        window.setAttributes(a);
    }

    public void setDialogParams(int x, int y, int minTop) {
        this.mX = x;
        this.mY = y;
        this.mMinTop = minTop;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (this.mTentativeUserSetResponse == 0) {
            int response = getResponseFromButtonId(checkedId);
            if (!this.mIsRepeating) {
                this.mUserSetResponse = response;
            } else if (checkedId == findButtonIdForResponse(this.mOriginalAttendeeResponse)) {
                this.mUserSetResponse = response;
            } else {
                this.mTentativeUserSetResponse = response;
                this.mEditResponseHelper.showDialog(this.mWhichEvents);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mController.deregisterEventHandler(Integer.valueOf(R.layout.event_info));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        this.mIsTabletConfig = Utils.getConfigBool(this.mActivity, R.bool.tablet_config);
        this.mController = CalendarController.getInstance(this.mActivity);
        this.mController.registerEventHandler(R.layout.event_info, this);
        this.mEditResponseHelper = new EditResponseHelper(activity);
        this.mEditResponseHelper.setDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (EventInfoFragment.this.mEditResponseHelper.getWhichEvents() != -1) {
                    EventInfoFragment.this.mUserSetResponse = EventInfoFragment.this.mTentativeUserSetResponse;
                    EventInfoFragment.this.mWhichEvents = EventInfoFragment.this.mEditResponseHelper.getWhichEvents();
                } else {
                    int oldResponse = EventInfoFragment.this.mUserSetResponse != 0 ? EventInfoFragment.this.mUserSetResponse : EventInfoFragment.this.mOriginalAttendeeResponse;
                    int buttonToCheck = EventInfoFragment.findButtonIdForResponse(oldResponse);
                    if (EventInfoFragment.this.mResponseRadioGroup != null) {
                        EventInfoFragment.this.mResponseRadioGroup.check(buttonToCheck);
                    }
                    if (buttonToCheck == -1) {
                        EventInfoFragment.this.mEditResponseHelper.setWhichEvents(-1);
                    }
                }
                if (!EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mTentativeUserSetResponse = 0;
                }
            }
        });
        if (this.mAttendeeResponseFromIntent != 0) {
            this.mEditResponseHelper.setWhichEvents(1);
            this.mWhichEvents = this.mEditResponseHelper.getWhichEvents();
        }
        this.mHandler = new QueryHandler(activity);
        if (!this.mIsDialog) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.mIsDialog = savedInstanceState.getBoolean("key_fragment_is_dialog", false);
            this.mWindowStyle = savedInstanceState.getInt("key_window_style", 1);
            this.mDeleteDialogVisible = savedInstanceState.getBoolean("key_delete_dialog_visible", false);
            this.mCalendarColor = savedInstanceState.getInt("key_calendar_color");
            this.mCalendarColorInitialized = savedInstanceState.getBoolean("key_calendar_color_init");
            this.mOriginalColor = savedInstanceState.getInt("key_original_color");
            this.mOriginalColorInitialized = savedInstanceState.getBoolean("key_original_color_init");
            this.mCurrentColor = savedInstanceState.getInt("key_current_color");
            this.mCurrentColorInitialized = savedInstanceState.getBoolean("key_current_color_init");
            this.mCurrentColorKey = savedInstanceState.getInt("key_current_color_key");
            this.mTentativeUserSetResponse = savedInstanceState.getInt("key_tentative_user_response", 0);
            if (this.mTentativeUserSetResponse != 0 && this.mEditResponseHelper != null) {
                this.mEditResponseHelper.setWhichEvents(savedInstanceState.getInt("key_response_which_events", -1));
            }
            this.mUserSetResponse = savedInstanceState.getInt("key_user_set_attendee_response", 0);
            if (this.mUserSetResponse != 0) {
                this.mWhichEvents = savedInstanceState.getInt("key_response_which_events", -1);
            }
            this.mReminders = Utils.readRemindersFromBundle(savedInstanceState);
        }
        if (this.mWindowStyle == 1) {
            this.mView = inflater.inflate(R.layout.event_info_dialog, container, false);
        } else {
            this.mView = inflater.inflate(R.layout.event_info, container, false);
        }
        this.mScrollView = (ScrollView) this.mView.findViewById(R.id.event_info_scroll_view);
        this.mLoadingMsgView = this.mView.findViewById(R.id.event_info_loading_msg);
        this.mErrorMsgView = this.mView.findViewById(R.id.event_info_error_msg);
        this.mTitle = (TextView) this.mView.findViewById(R.id.title);
        this.mWhenDateTime = (TextView) this.mView.findViewById(R.id.when_datetime);
        this.mWhere = (TextView) this.mView.findViewById(R.id.where);
        this.mDesc = (ExpandableTextView) this.mView.findViewById(R.id.description);
        this.mHeadlines = this.mView.findViewById(R.id.event_info_headline);
        this.mLongAttendees = (AttendeesView) this.mView.findViewById(R.id.long_attendee_list);
        this.mResponseRadioGroup = (RadioGroup) this.mView.findViewById(R.id.response_value);
        if (this.mUri == null) {
            this.mEventId = savedInstanceState.getLong("key_event_id");
            this.mUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
            this.mStartMillis = savedInstanceState.getLong("key_start_millis");
            this.mEndMillis = savedInstanceState.getLong("key_end_millis");
        }
        this.mAnimateAlpha = ObjectAnimator.ofFloat(this.mScrollView, "Alpha", 0.0f, 1.0f);
        this.mAnimateAlpha.setDuration(300L);
        this.mAnimateAlpha.addListener(new AnimatorListenerAdapter() {
            int defLayerType;

            @Override
            public void onAnimationStart(Animator animation) {
                this.defLayerType = EventInfoFragment.this.mScrollView.getLayerType();
                EventInfoFragment.this.mScrollView.setLayerType(2, null);
                EventInfoFragment.this.mLoadingMsgView.removeCallbacks(EventInfoFragment.this.mLoadingMsgAlphaUpdater);
                EventInfoFragment.this.mLoadingMsgView.setVisibility(8);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                EventInfoFragment.this.mScrollView.setLayerType(this.defLayerType, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                EventInfoFragment.this.mScrollView.setLayerType(this.defLayerType, null);
                EventInfoFragment.this.mNoCrossFade = true;
            }
        });
        this.mLoadingMsgView.setAlpha(0.0f);
        this.mScrollView.setAlpha(0.0f);
        this.mErrorMsgView.setVisibility(4);
        this.mLoadingMsgView.postDelayed(this.mLoadingMsgAlphaUpdater, 600L);
        this.mHandler.startQuery(1, null, this.mUri, EVENT_PROJECTION, null, null, null);
        View b = this.mView.findViewById(R.id.delete);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EventInfoFragment.this.mCanModifyCalendar) {
                    EventInfoFragment.this.mDeleteHelper = new DeleteEventHelper(EventInfoFragment.this.mContext, EventInfoFragment.this.mActivity, (EventInfoFragment.this.mIsDialog || EventInfoFragment.this.mIsTabletConfig) ? false : true);
                    EventInfoFragment.this.mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                    EventInfoFragment.this.mDeleteHelper.setOnDismissListener(EventInfoFragment.this.createDeleteOnDismissListener());
                    EventInfoFragment.this.mDeleteDialogVisible = true;
                    EventInfoFragment.this.mDeleteHelper.delete(EventInfoFragment.this.mStartMillis, EventInfoFragment.this.mEndMillis, EventInfoFragment.this.mEventId, -1, EventInfoFragment.this.onDeleteRunnable);
                }
            }
        });
        View b2 = this.mView.findViewById(R.id.change_color);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EventInfoFragment.this.mCanModifyCalendar) {
                    EventInfoFragment.this.showEventColorPickerDialog();
                }
            }
        });
        if ((!this.mIsDialog && !this.mIsTabletConfig) || this.mWindowStyle == 0) {
            this.mView.findViewById(R.id.event_info_buttons_container).setVisibility(8);
        }
        this.emailAttendeesButton = (Button) this.mView.findViewById(R.id.email_attendees_button);
        if (this.emailAttendeesButton != null) {
            this.emailAttendeesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EventInfoFragment.this.emailAttendees();
                }
            });
        }
        View reminderAddButton = this.mView.findViewById(R.id.reminder_add);
        View.OnClickListener addReminderOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventInfoFragment.this.addReminder();
                EventInfoFragment.this.mUserModifiedReminders = true;
            }
        };
        reminderAddButton.setOnClickListener(addReminderOnClickListener);
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this.mActivity);
        String defaultReminderString = prefs.getString("preferences_default_reminder", "-1");
        this.mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);
        prepareReminders();
        return this.mView;
    }

    private void updateTitle() {
        Resources res = getActivity().getResources();
        if (this.mCanModifyCalendar && !this.mIsOrganizer) {
            getActivity().setTitle(res.getString(R.string.event_info_title_invite));
        } else {
            getActivity().setTitle(res.getString(R.string.event_info_title));
        }
    }

    private boolean initEventCursor() {
        boolean z = false;
        if (this.mEventCursor == null || this.mEventCursor.getCount() == 0) {
            return false;
        }
        this.mEventCursor.moveToFirst();
        this.mEventId = this.mEventCursor.getInt(0);
        String rRule = this.mEventCursor.getString(2);
        this.mIsRepeating = !TextUtils.isEmpty(rRule);
        if (this.mEventCursor.getInt(15) == 1) {
            z = true;
        } else if (this.mReminders != null && this.mReminders.size() > 0) {
            z = true;
        }
        this.mHasAlarm = z;
        this.mMaxReminders = this.mEventCursor.getInt(16);
        this.mCalendarAllowedReminders = this.mEventCursor.getString(17);
        return true;
    }

    private void initAttendeesCursor(View view) {
        this.mOriginalAttendeeResponse = 0;
        this.mCalendarOwnerAttendeeId = -1L;
        this.mNumOfAttendees = 0;
        if (this.mAttendeesCursor != null) {
            this.mNumOfAttendees = this.mAttendeesCursor.getCount();
            if (this.mAttendeesCursor.moveToFirst()) {
                this.mAcceptedAttendees.clear();
                this.mDeclinedAttendees.clear();
                this.mTentativeAttendees.clear();
                this.mNoResponseAttendees.clear();
                do {
                    int status = this.mAttendeesCursor.getInt(4);
                    String name = this.mAttendeesCursor.getString(1);
                    String email = this.mAttendeesCursor.getString(2);
                    if (this.mAttendeesCursor.getInt(3) == 2 && !TextUtils.isEmpty(name)) {
                        this.mEventOrganizerDisplayName = name;
                        if (!this.mIsOrganizer) {
                            setVisibilityCommon(view, R.id.organizer_container, 0);
                            setTextCommon(view, R.id.organizer, this.mEventOrganizerDisplayName);
                        }
                    }
                    if (this.mCalendarOwnerAttendeeId == -1 && this.mCalendarOwnerAccount.equalsIgnoreCase(email)) {
                        this.mCalendarOwnerAttendeeId = this.mAttendeesCursor.getInt(0);
                        this.mOriginalAttendeeResponse = this.mAttendeesCursor.getInt(4);
                    } else {
                        String identity = null;
                        String idNamespace = null;
                        if (Utils.isJellybeanOrLater()) {
                            identity = this.mAttendeesCursor.getString(5);
                            idNamespace = this.mAttendeesCursor.getString(6);
                        }
                        switch (status) {
                            case 1:
                                this.mAcceptedAttendees.add(new CalendarEventModel.Attendee(name, email, 1, identity, idNamespace));
                                break;
                            case 2:
                                this.mDeclinedAttendees.add(new CalendarEventModel.Attendee(name, email, 2, identity, idNamespace));
                                break;
                            case 3:
                            default:
                                this.mNoResponseAttendees.add(new CalendarEventModel.Attendee(name, email, 0, identity, idNamespace));
                                break;
                            case 4:
                                this.mTentativeAttendees.add(new CalendarEventModel.Attendee(name, email, 4, identity, idNamespace));
                                break;
                        }
                    }
                } while (this.mAttendeesCursor.moveToNext());
                this.mAttendeesCursor.moveToFirst();
                updateAttendees(view);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        int response;
        super.onSaveInstanceState(outState);
        outState.putLong("key_event_id", this.mEventId);
        outState.putLong("key_start_millis", this.mStartMillis);
        outState.putLong("key_end_millis", this.mEndMillis);
        outState.putBoolean("key_fragment_is_dialog", this.mIsDialog);
        outState.putInt("key_window_style", this.mWindowStyle);
        outState.putBoolean("key_delete_dialog_visible", this.mDeleteDialogVisible);
        outState.putInt("key_calendar_color", this.mCalendarColor);
        outState.putBoolean("key_calendar_color_init", this.mCalendarColorInitialized);
        outState.putInt("key_original_color", this.mOriginalColor);
        outState.putBoolean("key_original_color_init", this.mOriginalColorInitialized);
        outState.putInt("key_current_color", this.mCurrentColor);
        outState.putBoolean("key_current_color_init", this.mCurrentColorInitialized);
        outState.putInt("key_current_color_key", this.mCurrentColorKey);
        outState.putInt("key_tentative_user_response", this.mTentativeUserSetResponse);
        if (this.mTentativeUserSetResponse != 0 && this.mEditResponseHelper != null) {
            outState.putInt("key_response_which_events", this.mEditResponseHelper.getWhichEvents());
        }
        if (this.mAttendeeResponseFromIntent != 0) {
            response = this.mAttendeeResponseFromIntent;
        } else {
            response = this.mOriginalAttendeeResponse;
        }
        outState.putInt("key_attendee_response", response);
        if (this.mUserSetResponse != 0) {
            int response2 = this.mUserSetResponse;
            outState.putInt("key_user_set_attendee_response", response2);
            outState.putInt("key_response_which_events", this.mWhichEvents);
        }
        this.mReminders = EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues);
        int numReminders = this.mReminders.size();
        ArrayList<Integer> reminderMinutes = new ArrayList<>(numReminders);
        ArrayList<Integer> reminderMethods = new ArrayList<>(numReminders);
        for (CalendarEventModel.ReminderEntry reminder : this.mReminders) {
            reminderMinutes.add(Integer.valueOf(reminder.getMinutes()));
            reminderMethods.add(Integer.valueOf(reminder.getMethod()));
        }
        outState.putIntegerArrayList("key_reminder_minutes", reminderMinutes);
        outState.putIntegerArrayList("key_reminder_methods", reminderMethods);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if ((!this.mIsDialog && !this.mIsTabletConfig) || this.mWindowStyle == 0) {
            inflater.inflate(R.menu.event_info_title_bar, menu);
            this.mMenu = menu;
            updateMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mIsDialog) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == 16908332) {
            Utils.returnToCalendarHome(this.mContext);
            this.mActivity.finish();
            return true;
        }
        if (itemId == R.id.info_action_edit) {
            doEdit();
            this.mActivity.finish();
        } else if (itemId == R.id.info_action_delete) {
            this.mDeleteHelper = new DeleteEventHelper(this.mActivity, this.mActivity, true);
            this.mDeleteHelper.setDeleteNotificationListener(this);
            this.mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            this.mDeleteDialogVisible = true;
            this.mDeleteHelper.delete(this.mStartMillis, this.mEndMillis, this.mEventId, -1, this.onDeleteRunnable);
        } else if (itemId == R.id.info_action_change_color) {
            showEventColorPickerDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEventColorPickerDialog() {
        if (this.mColorPickerDialog == null) {
            this.mColorPickerDialog = EventColorPickerDialog.newInstance(this.mColors, this.mCurrentColor, this.mCalendarColor, this.mIsTabletConfig);
            this.mColorPickerDialog.setOnColorSelectedListener(this);
        }
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.executePendingTransactions();
        if (!this.mColorPickerDialog.isAdded()) {
            this.mColorPickerDialog.show(fragmentManager, "EventColorPickerDialog");
        }
    }

    private boolean saveEventColor() {
        if (this.mCurrentColor == this.mOriginalColor) {
            return false;
        }
        ContentValues values = new ContentValues();
        if (this.mCurrentColor != this.mCalendarColor) {
            values.put("eventColor_index", Integer.valueOf(this.mCurrentColorKey));
        } else {
            values.put("eventColor_index", "");
        }
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
        this.mHandler.startUpdate(this.mHandler.getNextToken(), null, uri, values, null, null, 0L);
        return true;
    }

    @Override
    public void onStop() {
        Activity act = getActivity();
        if (!this.mEventDeletionStarted && act != null && !act.isChangingConfigurations()) {
            boolean responseSaved = saveResponse();
            boolean eventColorSaved = saveEventColor();
            if (saveReminders() || responseSaved || eventColorSaved) {
                Toast.makeText(getActivity(), R.string.saving_event, 0).show();
            }
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (this.mEventCursor != null) {
            this.mEventCursor.close();
        }
        if (this.mCalendarsCursor != null) {
            this.mCalendarsCursor.close();
        }
        if (this.mAttendeesCursor != null) {
            this.mAttendeesCursor.close();
        }
        super.onDestroy();
    }

    private boolean saveResponse() {
        int status;
        if (this.mAttendeesCursor == null || this.mEventCursor == null || (status = getResponseFromButtonId(this.mResponseRadioGroup.getCheckedRadioButtonId())) == 0 || status == this.mOriginalAttendeeResponse || this.mCalendarOwnerAttendeeId == -1) {
            return false;
        }
        if (!this.mIsRepeating) {
            updateResponse(this.mEventId, this.mCalendarOwnerAttendeeId, status);
            this.mOriginalAttendeeResponse = status;
            return true;
        }
        switch (this.mWhichEvents) {
            case -1:
                break;
            case 0:
                createExceptionResponse(this.mEventId, status);
                this.mOriginalAttendeeResponse = status;
                break;
            case 1:
                updateResponse(this.mEventId, this.mCalendarOwnerAttendeeId, status);
                this.mOriginalAttendeeResponse = status;
                break;
            default:
                Log.e("EventInfoFragment", "Unexpected choice for updating invitation response");
                break;
        }
        return false;
    }

    private void updateResponse(long eventId, long attendeeId, int status) {
        ContentValues values = new ContentValues();
        if (!TextUtils.isEmpty(this.mCalendarOwnerAccount)) {
            values.put("attendeeEmail", this.mCalendarOwnerAccount);
        }
        values.put("attendeeStatus", Integer.valueOf(status));
        values.put("event_id", Long.valueOf(eventId));
        Uri uri = ContentUris.withAppendedId(CalendarContract.Attendees.CONTENT_URI, attendeeId);
        this.mHandler.startUpdate(this.mHandler.getNextToken(), null, uri, values, null, null, 0L);
    }

    private void createExceptionResponse(long eventId, int status) {
        ContentValues values = new ContentValues();
        values.put("originalInstanceTime", Long.valueOf(this.mStartMillis));
        values.put("selfAttendeeStatus", Integer.valueOf(status));
        values.put("eventStatus", (Integer) 1);
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        Uri exceptionUri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_EXCEPTION_URI, String.valueOf(eventId));
        ops.add(ContentProviderOperation.newInsert(exceptionUri).withValues(values).build());
        this.mHandler.startBatch(this.mHandler.getNextToken(), null, "com.android.calendar", ops, 0L);
    }

    public static int getResponseFromButtonId(int buttonId) {
        if (buttonId == R.id.response_yes) {
            return 1;
        }
        if (buttonId == R.id.response_maybe) {
            return 4;
        }
        if (buttonId == R.id.response_no) {
            return 2;
        }
        return 0;
    }

    public static int findButtonIdForResponse(int response) {
        switch (response) {
            case 1:
                return R.id.response_yes;
            case 2:
                return R.id.response_no;
            case 3:
            default:
                return -1;
            case 4:
                return R.id.response_maybe;
        }
    }

    private void doEdit() {
        Context c = getActivity();
        if (c != null) {
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
            Intent intent = new Intent("android.intent.action.EDIT", uri);
            intent.setClass(this.mActivity, EditEventActivity.class);
            intent.putExtra("beginTime", this.mStartMillis);
            intent.putExtra("endTime", this.mEndMillis);
            intent.putExtra("allDay", this.mAllDay);
            intent.putExtra("event_color", this.mCurrentColor);
            intent.putExtra("reminders", EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues));
            intent.putExtra("editMode", true);
            startActivity(intent);
        }
    }

    private void displayEventNotFound() {
        this.mErrorMsgView.setVisibility(0);
        this.mScrollView.setVisibility(8);
        this.mLoadingMsgView.setVisibility(8);
    }

    private void updateEvent(View view) {
        Context context;
        if (this.mEventCursor != null && view != null && (context = view.getContext()) != null) {
            String eventName = this.mEventCursor.getString(1);
            if (eventName == null || eventName.length() == 0) {
                eventName = getActivity().getString(R.string.no_title_label);
            }
            if (this.mStartMillis == 0 && this.mEndMillis == 0) {
                this.mStartMillis = this.mEventCursor.getLong(5);
                this.mEndMillis = this.mEventCursor.getLong(20);
                if (this.mEndMillis == 0) {
                    String duration = this.mEventCursor.getString(21);
                    if (!TextUtils.isEmpty(duration)) {
                        try {
                            Duration d = new Duration();
                            d.parse(duration);
                            long endMillis = this.mStartMillis + d.getMillis();
                            if (endMillis >= this.mStartMillis) {
                                this.mEndMillis = endMillis;
                            } else {
                                Log.d("EventInfoFragment", "Invalid duration string: " + duration);
                            }
                        } catch (DateException e) {
                            Log.d("EventInfoFragment", "Error parsing duration string " + duration, e);
                        }
                    }
                    if (this.mEndMillis == 0) {
                        this.mEndMillis = this.mStartMillis;
                    }
                }
            }
            this.mAllDay = this.mEventCursor.getInt(3) != 0;
            String location = this.mEventCursor.getString(9);
            String description = this.mEventCursor.getString(8);
            String rRule = this.mEventCursor.getString(2);
            String eventTimezone = this.mEventCursor.getString(7);
            this.mHeadlines.setBackgroundColor(this.mCurrentColor);
            if (eventName != null) {
                setTextCommon(view, R.id.title, eventName);
            }
            String localTimezone = Utils.getTimeZone(this.mActivity, this.mTZUpdater);
            Resources resources = context.getResources();
            String displayedDatetime = Utils.getDisplayedDatetime(this.mStartMillis, this.mEndMillis, System.currentTimeMillis(), localTimezone, this.mAllDay, context);
            String displayedTimezone = null;
            if (!this.mAllDay) {
                displayedTimezone = Utils.getDisplayedTimezone(this.mStartMillis, localTimezone, eventTimezone);
            }
            if (displayedTimezone == null) {
                setTextCommon(view, R.id.when_datetime, displayedDatetime);
            } else {
                int timezoneIndex = displayedDatetime.length();
                String displayedDatetime2 = displayedDatetime + "  " + displayedTimezone;
                SpannableStringBuilder sb = new SpannableStringBuilder(displayedDatetime2);
                ForegroundColorSpan transparentColorSpan = new ForegroundColorSpan(resources.getColor(R.color.event_info_headline_transparent_color));
                sb.setSpan(transparentColorSpan, timezoneIndex, displayedDatetime2.length(), 18);
                setTextCommon(view, R.id.when_datetime, sb);
            }
            CharSequence repeatString = null;
            if (!TextUtils.isEmpty(rRule)) {
                EventRecurrence eventRecurrence = new EventRecurrence();
                eventRecurrence.parse(rRule);
                Time date = new Time(localTimezone);
                date.set(this.mStartMillis);
                if (this.mAllDay) {
                    date.timezone = "UTC";
                }
                eventRecurrence.setStartDate(date);
                repeatString = EventRecurrenceFormatter.getRepeatString(this.mContext, resources, eventRecurrence, true);
            }
            if (repeatString == null) {
                view.findViewById(R.id.when_repeat).setVisibility(8);
            } else {
                setTextCommon(view, R.id.when_repeat, repeatString);
            }
            if (location == null || location.trim().length() == 0) {
                setVisibilityCommon(view, R.id.where, 8);
            } else {
                TextView textView = this.mWhere;
                if (textView != null) {
                    textView.setAutoLinkMask(0);
                    textView.setText(location.trim());
                    try {
                        textView.setText(Utils.extendedLinkify(textView.getText().toString(), true));
                        MovementMethod mm = textView.getMovementMethod();
                        if ((mm == null || !(mm instanceof LinkMovementMethod)) && textView.getLinksClickable()) {
                            textView.setMovementMethod(LinkMovementMethod.getInstance());
                        }
                    } catch (Exception ex) {
                        Log.e("EventInfoFragment", "Linkification failed", ex);
                    }
                    textView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            try {
                                return v.onTouchEvent(event);
                            } catch (ActivityNotFoundException e2) {
                                return true;
                            }
                        }
                    });
                }
            }
            if (description != null && description.length() != 0) {
                this.mDesc.setText(description);
            }
            if (Utils.isJellybeanOrLater()) {
                updateCustomAppButton();
            }
        }
    }

    private void updateCustomAppButton() {
        PackageManager pm;
        Button launchButton = (Button) this.mView.findViewById(R.id.launch_custom_app_button);
        if (launchButton != null) {
            String customAppPackage = this.mEventCursor.getString(18);
            String customAppUri = this.mEventCursor.getString(19);
            if (!TextUtils.isEmpty(customAppPackage) && !TextUtils.isEmpty(customAppUri) && (pm = this.mContext.getPackageManager()) != null) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(customAppPackage, 0);
                    if (info != null) {
                        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
                        final Intent intent = new Intent("android.provider.calendar.action.HANDLE_CUSTOM_EVENT", uri);
                        intent.setPackage(customAppPackage);
                        intent.putExtra("customAppUri", customAppUri);
                        intent.putExtra("beginTime", this.mStartMillis);
                        if (pm.resolveActivity(intent, 0) != null) {
                            Drawable icon = pm.getApplicationIcon(info);
                            if (icon != null) {
                                Drawable[] d = launchButton.getCompoundDrawables();
                                icon.setBounds(0, 0, mCustomAppIconSize, mCustomAppIconSize);
                                launchButton.setCompoundDrawables(icon, d[1], d[2], d[3]);
                            }
                            CharSequence label = pm.getApplicationLabel(info);
                            if (label != null && label.length() != 0) {
                                launchButton.setText(label);
                            }
                            launchButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        EventInfoFragment.this.startActivityForResult(intent, 0);
                                    } catch (ActivityNotFoundException e) {
                                        EventInfoFragment.this.setVisibilityCommon(EventInfoFragment.this.mView, R.id.launch_custom_app_container, 8);
                                    }
                                }
                            });
                            setVisibilityCommon(this.mView, R.id.launch_custom_app_container, 0);
                            return;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        setVisibilityCommon(this.mView, R.id.launch_custom_app_container, 8);
    }

    private void sendAccessibilityEvent() {
        int id;
        AccessibilityManager am = (AccessibilityManager) getActivity().getSystemService("accessibility");
        if (am.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(8);
            event.setClassName(EventInfoFragment.class.getName());
            event.setPackageName(getActivity().getPackageName());
            List<CharSequence> text = event.getText();
            addFieldToAccessibilityEvent(text, this.mTitle, null);
            addFieldToAccessibilityEvent(text, this.mWhenDateTime, null);
            addFieldToAccessibilityEvent(text, this.mWhere, null);
            addFieldToAccessibilityEvent(text, null, this.mDesc);
            if (this.mResponseRadioGroup.getVisibility() == 0 && (id = this.mResponseRadioGroup.getCheckedRadioButtonId()) != -1) {
                text.add(((TextView) getView().findViewById(R.id.response_label)).getText());
                text.add(((Object) ((RadioButton) this.mResponseRadioGroup.findViewById(id)).getText()) + ". ");
            }
            am.sendAccessibilityEvent(event);
        }
    }

    private void addFieldToAccessibilityEvent(List<CharSequence> text, TextView tv, ExpandableTextView etv) {
        CharSequence cs;
        if (tv != null) {
            cs = tv.getText();
        } else if (etv != null) {
            cs = etv.getText();
        } else {
            return;
        }
        if (!TextUtils.isEmpty(cs)) {
            CharSequence cs2 = cs.toString().trim();
            if (cs2.length() > 0) {
                text.add(cs2);
                text.add(". ");
            }
        }
    }

    private void updateCalendar(View view) {
        View button;
        View button2;
        this.mCalendarOwnerAccount = "";
        if (this.mCalendarsCursor != null && this.mEventCursor != null) {
            this.mCalendarsCursor.moveToFirst();
            String tempAccount = this.mCalendarsCursor.getString(2);
            if (tempAccount == null) {
                tempAccount = "";
            }
            this.mCalendarOwnerAccount = tempAccount;
            this.mOwnerCanRespond = this.mCalendarsCursor.getInt(3) != 0;
            this.mSyncAccountName = this.mCalendarsCursor.getString(4);
            this.mHandler.startQuery(32, null, CalendarContract.Calendars.CONTENT_URI, CALENDARS_PROJECTION, "visible=?", new String[]{"1"}, null);
            this.mEventOrganizerEmail = this.mEventCursor.getString(14);
            this.mIsOrganizer = this.mCalendarOwnerAccount.equalsIgnoreCase(this.mEventOrganizerEmail);
            if (!TextUtils.isEmpty(this.mEventOrganizerEmail) && !this.mEventOrganizerEmail.endsWith("calendar.google.com")) {
                this.mEventOrganizerDisplayName = this.mEventOrganizerEmail;
            }
            if (!this.mIsOrganizer && !TextUtils.isEmpty(this.mEventOrganizerDisplayName)) {
                setTextCommon(view, R.id.organizer, this.mEventOrganizerDisplayName);
                setVisibilityCommon(view, R.id.organizer_container, 0);
            } else {
                setVisibilityCommon(view, R.id.organizer_container, 8);
            }
            this.mHasAttendeeData = this.mEventCursor.getInt(13) != 0;
            this.mCanModifyCalendar = this.mEventCursor.getInt(10) >= 500;
            this.mCanModifyEvent = this.mCanModifyCalendar && this.mIsOrganizer;
            this.mIsBusyFreeCalendar = this.mEventCursor.getInt(10) == 100;
            if (!this.mIsBusyFreeCalendar) {
                View b = this.mView.findViewById(R.id.edit);
                b.setEnabled(true);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventInfoFragment.this.doEdit();
                        if (!EventInfoFragment.this.mIsDialog) {
                            if (!EventInfoFragment.this.mIsTabletConfig) {
                                EventInfoFragment.this.getActivity().finish();
                                return;
                            }
                            return;
                        }
                        EventInfoFragment.this.dismiss();
                    }
                });
            }
            if (this.mCanModifyCalendar && (button2 = this.mView.findViewById(R.id.delete)) != null) {
                button2.setEnabled(true);
                button2.setVisibility(0);
            }
            if (this.mCanModifyEvent && (button = this.mView.findViewById(R.id.edit)) != null) {
                button.setEnabled(true);
                button.setVisibility(0);
            }
            if (((!this.mIsDialog && !this.mIsTabletConfig) || this.mWindowStyle == 0) && this.mMenu != null) {
                this.mActivity.invalidateOptionsMenu();
                return;
            }
            return;
        }
        setVisibilityCommon(view, R.id.calendar, 8);
        sendAccessibilityEventIfQueryDone(8);
    }

    private void updateMenu() {
        if (this.mMenu != null) {
            MenuItem delete = this.mMenu.findItem(R.id.info_action_delete);
            MenuItem edit = this.mMenu.findItem(R.id.info_action_edit);
            MenuItem changeColor = this.mMenu.findItem(R.id.info_action_change_color);
            if (delete != null) {
                delete.setVisible(this.mCanModifyCalendar);
                delete.setEnabled(this.mCanModifyCalendar);
            }
            if (edit != null) {
                edit.setVisible(this.mCanModifyEvent);
                edit.setEnabled(this.mCanModifyEvent);
            }
            if (changeColor != null && this.mColors != null && this.mColors.length > 0) {
                changeColor.setVisible(this.mCanModifyCalendar);
                changeColor.setEnabled(this.mCanModifyCalendar);
            }
        }
    }

    private void updateAttendees(View view) {
        if (this.mAcceptedAttendees.size() + this.mDeclinedAttendees.size() + this.mTentativeAttendees.size() + this.mNoResponseAttendees.size() > 0) {
            this.mLongAttendees.clearAttendees();
            this.mLongAttendees.addAttendees(this.mAcceptedAttendees);
            this.mLongAttendees.addAttendees(this.mDeclinedAttendees);
            this.mLongAttendees.addAttendees(this.mTentativeAttendees);
            this.mLongAttendees.addAttendees(this.mNoResponseAttendees);
            this.mLongAttendees.setEnabled(false);
            this.mLongAttendees.setVisibility(0);
        } else {
            this.mLongAttendees.setVisibility(8);
        }
        if (hasEmailableAttendees()) {
            setVisibilityCommon(this.mView, R.id.email_attendees_container, 0);
            if (this.emailAttendeesButton != null) {
                this.emailAttendeesButton.setText(R.string.email_guests_label);
                return;
            }
            return;
        }
        if (hasEmailableOrganizer()) {
            setVisibilityCommon(this.mView, R.id.email_attendees_container, 0);
            if (this.emailAttendeesButton != null) {
                this.emailAttendeesButton.setText(R.string.email_organizer_label);
                return;
            }
            return;
        }
        setVisibilityCommon(this.mView, R.id.email_attendees_container, 8);
    }

    private boolean hasEmailableAttendees() {
        for (CalendarEventModel.Attendee attendee : this.mAcceptedAttendees) {
            if (Utils.isEmailableFrom(attendee.mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        for (CalendarEventModel.Attendee attendee2 : this.mTentativeAttendees) {
            if (Utils.isEmailableFrom(attendee2.mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        for (CalendarEventModel.Attendee attendee3 : this.mNoResponseAttendees) {
            if (Utils.isEmailableFrom(attendee3.mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        for (CalendarEventModel.Attendee attendee4 : this.mDeclinedAttendees) {
            if (Utils.isEmailableFrom(attendee4.mEmail, this.mSyncAccountName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEmailableOrganizer() {
        return this.mEventOrganizerEmail != null && Utils.isEmailableFrom(this.mEventOrganizerEmail, this.mSyncAccountName);
    }

    public void initReminders(View view, Cursor cursor) {
        ArrayList<CalendarEventModel.ReminderEntry> reminders;
        this.mOriginalReminders.clear();
        this.mUnsupportedReminders.clear();
        while (cursor.moveToNext()) {
            int minutes = cursor.getInt(1);
            int method = cursor.getInt(2);
            if (method != 0 && !this.mReminderMethodValues.contains(Integer.valueOf(method))) {
                this.mUnsupportedReminders.add(CalendarEventModel.ReminderEntry.valueOf(minutes, method));
            } else {
                this.mOriginalReminders.add(CalendarEventModel.ReminderEntry.valueOf(minutes, method));
            }
        }
        Collections.sort(this.mOriginalReminders);
        if (!this.mUserModifiedReminders) {
            LinearLayout parent = (LinearLayout) this.mScrollView.findViewById(R.id.reminder_items_container);
            if (parent != null) {
                parent.removeAllViews();
            }
            if (this.mReminderViews != null) {
                this.mReminderViews.clear();
            }
            if (this.mHasAlarm) {
                if (this.mReminders != null) {
                    reminders = this.mReminders;
                } else {
                    reminders = this.mOriginalReminders;
                }
                for (CalendarEventModel.ReminderEntry re : reminders) {
                    EventViewUtils.addMinutesToList(this.mActivity, this.mReminderMinuteValues, this.mReminderMinuteLabels, re.getMinutes());
                }
                for (CalendarEventModel.ReminderEntry re2 : reminders) {
                    EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderViews, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, re2, Integer.MAX_VALUE, this.mReminderChangeListener);
                }
                EventViewUtils.updateAddReminderButton(this.mView, this.mReminderViews, this.mMaxReminders);
            }
        }
    }

    void updateResponse(View view) {
        int response;
        if (!this.mCanModifyCalendar || ((this.mHasAttendeeData && this.mIsOrganizer && this.mNumOfAttendees <= 1) || (this.mIsOrganizer && !this.mOwnerCanRespond))) {
            setVisibilityCommon(view, R.id.response_container, 8);
            return;
        }
        setVisibilityCommon(view, R.id.response_container, 0);
        if (this.mTentativeUserSetResponse != 0) {
            response = this.mTentativeUserSetResponse;
        } else if (this.mUserSetResponse != 0) {
            response = this.mUserSetResponse;
        } else if (this.mAttendeeResponseFromIntent != 0) {
            response = this.mAttendeeResponseFromIntent;
        } else {
            response = this.mOriginalAttendeeResponse;
        }
        int buttonToCheck = findButtonIdForResponse(response);
        this.mResponseRadioGroup.check(buttonToCheck);
        this.mResponseRadioGroup.setOnCheckedChangeListener(this);
    }

    private void setTextCommon(View view, int id, CharSequence text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView != null) {
            textView.setText(text);
        }
    }

    private void setVisibilityCommon(View view, int id, int visibility) {
        View v = view.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    @Override
    public void onPause() {
        this.mIsPaused = true;
        this.mHandler.removeCallbacks(this.onDeleteRunnable);
        super.onPause();
        if (this.mDeleteDialogVisible && this.mDeleteHelper != null) {
            this.mDeleteHelper.dismissAlertDialog();
            this.mDeleteHelper = null;
        }
        if (this.mTentativeUserSetResponse != 0 && this.mEditResponseHelper != null) {
            this.mEditResponseHelper.dismissAlertDialog();
        }
    }

    @Override
    public void onResume() {
        boolean z = false;
        super.onResume();
        if (this.mIsDialog) {
            setDialogSize(getActivity().getResources());
            applyDialogParams();
        }
        this.mIsPaused = false;
        if (this.mDismissOnResume) {
            this.mHandler.post(this.onDeleteRunnable);
        }
        if (this.mDeleteDialogVisible) {
            Context context = this.mContext;
            Activity activity = this.mActivity;
            if (!this.mIsDialog && !this.mIsTabletConfig) {
                z = true;
            }
            this.mDeleteHelper = new DeleteEventHelper(context, activity, z);
            this.mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            this.mDeleteHelper.delete(this.mStartMillis, this.mEndMillis, this.mEventId, -1, this.onDeleteRunnable);
            return;
        }
        if (this.mTentativeUserSetResponse != 0) {
            int buttonId = findButtonIdForResponse(this.mTentativeUserSetResponse);
            this.mResponseRadioGroup.check(buttonId);
            this.mEditResponseHelper.showDialog(this.mEditResponseHelper.getWhichEvents());
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return 128L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        reloadEvents();
    }

    public void reloadEvents() {
        if (this.mHandler != null) {
            this.mHandler.startQuery(1, null, this.mUri, EVENT_PROJECTION, null, null, null);
        }
    }

    @Override
    public void onClick(View view) {
        LinearLayout reminderItem = (LinearLayout) view.getParent();
        LinearLayout parent = (LinearLayout) reminderItem.getParent();
        parent.removeView(reminderItem);
        this.mReminderViews.remove(reminderItem);
        this.mUserModifiedReminders = true;
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderViews, this.mMaxReminders);
    }

    private void addReminder() {
        if (this.mDefaultReminderMinutes == -1) {
            EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderViews, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, CalendarEventModel.ReminderEntry.valueOf(10), this.mMaxReminders, this.mReminderChangeListener);
        } else {
            EventViewUtils.addReminder(this.mActivity, this.mScrollView, this, this.mReminderViews, this.mReminderMinuteValues, this.mReminderMinuteLabels, this.mReminderMethodValues, this.mReminderMethodLabels, CalendarEventModel.ReminderEntry.valueOf(this.mDefaultReminderMinutes), this.mMaxReminders, this.mReminderChangeListener);
        }
        EventViewUtils.updateAddReminderButton(this.mView, this.mReminderViews, this.mMaxReminders);
    }

    private synchronized void prepareReminders() {
        if (this.mReminderMinuteValues == null || this.mReminderMinuteLabels == null || this.mReminderMethodValues == null || this.mReminderMethodLabels == null || this.mCalendarAllowedReminders != null) {
            Resources r = this.mActivity.getResources();
            this.mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);
            this.mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);
            this.mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);
            this.mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);
            if (this.mCalendarAllowedReminders != null) {
                EventViewUtils.reduceMethodList(this.mReminderMethodValues, this.mReminderMethodLabels, this.mCalendarAllowedReminders);
            }
            if (this.mView != null) {
                this.mView.invalidate();
            }
        }
    }

    private boolean saveReminders() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(3);
        this.mReminders = EventViewUtils.reminderItemsToReminders(this.mReminderViews, this.mReminderMinuteValues, this.mReminderMethodValues);
        this.mOriginalReminders.addAll(this.mUnsupportedReminders);
        Collections.sort(this.mOriginalReminders);
        this.mReminders.addAll(this.mUnsupportedReminders);
        Collections.sort(this.mReminders);
        boolean changed = EditEventHelper.saveReminders(ops, this.mEventId, this.mReminders, this.mOriginalReminders, false);
        if (!changed) {
            return false;
        }
        AsyncQueryService service = new AsyncQueryService(getActivity());
        service.startBatch(0, null, CalendarContract.Calendars.CONTENT_URI.getAuthority(), ops, 0L);
        this.mOriginalReminders = this.mReminders;
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, this.mEventId);
        int len = this.mReminders.size();
        boolean hasAlarm = len > 0;
        if (hasAlarm != this.mHasAlarm) {
            ContentValues values = new ContentValues();
            values.put("hasAlarm", Integer.valueOf(hasAlarm ? 1 : 0));
            service.startUpdate(0, null, uri, values, null, null, 0L);
        }
        return true;
    }

    private void emailAttendees() {
        Intent i = new Intent(getActivity(), (Class<?>) QuickResponseActivity.class);
        i.putExtra("eventId", this.mEventId);
        i.addFlags(268435456);
        startActivity(i);
    }

    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<>(size);
        for (int i : vals) {
            list.add(Integer.valueOf(i));
        }
        return list;
    }

    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<>(Arrays.asList(labels));
        return list;
    }

    @Override
    public void onDeleteStarted() {
        this.mEventDeletionStarted = true;
    }

    private DialogInterface.OnDismissListener createDeleteOnDismissListener() {
        return new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!EventInfoFragment.this.mIsPaused) {
                    EventInfoFragment.this.mDeleteDialogVisible = false;
                }
            }
        };
    }

    public long getEventId() {
        return this.mEventId;
    }

    public long getStartMillis() {
        return this.mStartMillis;
    }

    public long getEndMillis() {
        return this.mEndMillis;
    }

    private void setDialogSize(Resources r) {
        mDialogWidth = (int) r.getDimension(R.dimen.event_info_dialog_width);
        mDialogHeight = (int) r.getDimension(R.dimen.event_info_dialog_height);
    }

    @Override
    public void onColorSelected(int color) {
        this.mCurrentColor = color;
        this.mCurrentColorKey = this.mDisplayColorKeyMap.get(color);
        this.mHeadlines.setBackgroundColor(color);
    }
}
