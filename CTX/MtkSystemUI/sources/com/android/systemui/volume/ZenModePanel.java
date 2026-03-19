package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.Interaction;
import com.android.systemui.volume.SegmentedButtons;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

public class ZenModePanel extends FrameLayout {
    private boolean mAttached;
    private int mAttachedZen;
    private View mAutoRule;
    private TextView mAutoTitle;
    private int mBucketIndex;
    private Callback mCallback;
    private final ConfigurableTexts mConfigurableTexts;
    private final Context mContext;
    private ZenModeController mController;
    private ViewGroup mEdit;
    private View mEmpty;
    private ImageView mEmptyIcon;
    private TextView mEmptyText;
    private Condition mExitCondition;
    private boolean mExpanded;
    private final Uri mForeverId;
    private final H mHandler;
    private boolean mHidden;
    protected final LayoutInflater mInflater;
    private final Interaction.Callback mInteractionCallback;
    private final ZenPrefs mPrefs;
    private Condition mSessionExitCondition;
    private int mSessionZen;
    private int mState;
    private String mTag;
    private final TransitionHelper mTransitionHelper;
    private boolean mVoiceCapable;
    private TextView mZenAlarmWarning;
    protected SegmentedButtons mZenButtons;
    protected final SegmentedButtons.Callback mZenButtonsCallback;
    private final ZenModeController.Callback mZenCallback;
    protected LinearLayout mZenConditions;
    private View mZenIntroduction;
    private View mZenIntroductionConfirm;
    private TextView mZenIntroductionCustomize;
    private TextView mZenIntroductionMessage;
    protected int mZenModeButtonLayoutId;
    protected int mZenModeConditionLayoutId;
    private RadioGroup mZenRadioGroup;
    private LinearLayout mZenRadioGroupContent;
    private static final boolean DEBUG = Log.isLoggable("ZenModePanel", 3);
    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    private static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);
    public static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    public static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");

    public interface Callback {
        void onExpanded(boolean z);

        void onInteraction();

        void onPrioritySettings();
    }

    public ZenModePanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHandler = new H();
        this.mTransitionHelper = new TransitionHelper();
        this.mTag = "ZenModePanel/" + Integer.toHexString(System.identityHashCode(this));
        this.mBucketIndex = -1;
        this.mState = 0;
        this.mZenCallback = new ZenModeController.Callback() {
            @Override
            public void onManualRuleChanged(ZenModeConfig.ZenRule zenRule) {
                ZenModePanel.this.mHandler.obtainMessage(2, zenRule).sendToTarget();
            }
        };
        this.mZenButtonsCallback = new SegmentedButtons.Callback() {
            @Override
            public void onSelected(Object obj, boolean z) {
                if (obj != null && ZenModePanel.this.mZenButtons.isShown() && ZenModePanel.this.isAttachedToWindow()) {
                    final int iIntValue = ((Integer) obj).intValue();
                    if (z) {
                        MetricsLogger.action(ZenModePanel.this.mContext, 165, iIntValue);
                    }
                    if (ZenModePanel.DEBUG) {
                        Log.d(ZenModePanel.this.mTag, "mZenButtonsCallback selected=" + iIntValue);
                    }
                    final Uri realConditionId = ZenModePanel.this.getRealConditionId(ZenModePanel.this.mSessionExitCondition);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            ZenModePanel.this.mController.setZen(iIntValue, realConditionId, "ZenModePanel.selectZen");
                            if (iIntValue != 0) {
                                Prefs.putInt(ZenModePanel.this.mContext, "DndFavoriteZen", iIntValue);
                            }
                        }
                    });
                }
            }

            @Override
            public void onInteraction() {
                ZenModePanel.this.fireInteraction();
            }
        };
        this.mInteractionCallback = new Interaction.Callback() {
            @Override
            public void onInteraction() {
                ZenModePanel.this.fireInteraction();
            }
        };
        this.mContext = context;
        this.mPrefs = new ZenPrefs();
        this.mInflater = LayoutInflater.from(this.mContext);
        this.mForeverId = Condition.newId(this.mContext).appendPath("forever").build();
        this.mConfigurableTexts = new ConfigurableTexts(this.mContext);
        this.mVoiceCapable = Util.isVoiceCapable(this.mContext);
        this.mZenModeConditionLayoutId = R.layout.zen_mode_condition;
        this.mZenModeButtonLayoutId = R.layout.zen_mode_button;
        if (DEBUG) {
            Log.d(this.mTag, "new ZenModePanel");
        }
    }

    protected void createZenButtons() {
        this.mZenButtons = (SegmentedButtons) findViewById(R.id.zen_buttons);
        this.mZenButtons.addButton(R.string.interruption_level_none_twoline, R.string.interruption_level_none_with_warning, 2);
        this.mZenButtons.addButton(R.string.interruption_level_alarms_twoline, R.string.interruption_level_alarms, 3);
        this.mZenButtons.addButton(R.string.interruption_level_priority_twoline, R.string.interruption_level_priority, 1);
        this.mZenButtons.setCallback(this.mZenButtonsCallback);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        createZenButtons();
        this.mZenIntroduction = findViewById(R.id.zen_introduction);
        this.mZenIntroductionMessage = (TextView) findViewById(R.id.zen_introduction_message);
        this.mZenIntroductionConfirm = findViewById(R.id.zen_introduction_confirm);
        this.mZenIntroductionConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.confirmZenIntroduction();
            }
        });
        this.mZenIntroductionCustomize = (TextView) findViewById(R.id.zen_introduction_customize);
        this.mZenIntroductionCustomize.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ZenModePanel.lambda$onFinishInflate$1(this.f$0, view);
            }
        });
        this.mConfigurableTexts.add(this.mZenIntroductionCustomize, R.string.zen_priority_customize_button);
        this.mZenConditions = (LinearLayout) findViewById(R.id.zen_conditions);
        this.mZenAlarmWarning = (TextView) findViewById(R.id.zen_alarm_warning);
        this.mZenRadioGroup = (RadioGroup) findViewById(R.id.zen_radio_buttons);
        this.mZenRadioGroupContent = (LinearLayout) findViewById(R.id.zen_radio_buttons_content);
        this.mEdit = (ViewGroup) findViewById(R.id.edit_container);
        this.mEmpty = findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(4);
        this.mEmptyText = (TextView) this.mEmpty.findViewById(android.R.id.title);
        this.mEmptyIcon = (ImageView) this.mEmpty.findViewById(android.R.id.icon);
        this.mAutoRule = findViewById(R.id.auto_rule);
        this.mAutoTitle = (TextView) this.mAutoRule.findViewById(android.R.id.title);
        this.mAutoRule.setVisibility(4);
    }

    public static void lambda$onFinishInflate$1(ZenModePanel zenModePanel, View view) {
        zenModePanel.confirmZenIntroduction();
        if (zenModePanel.mCallback != null) {
            zenModePanel.mCallback.onPrioritySettings();
        }
    }

    public void setEmptyState(final int i, final int i2) {
        this.mEmptyIcon.post(new Runnable() {
            @Override
            public final void run() {
                ZenModePanel.lambda$setEmptyState$2(this.f$0, i, i2);
            }
        });
    }

    public static void lambda$setEmptyState$2(ZenModePanel zenModePanel, int i, int i2) {
        zenModePanel.mEmptyIcon.setImageResource(i);
        zenModePanel.mEmptyText.setText(i2);
    }

    public void setAutoText(final CharSequence charSequence) {
        this.mAutoTitle.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mAutoTitle.setText(charSequence);
            }
        });
    }

    public void setState(int i) {
        if (this.mState == i) {
            return;
        }
        transitionFrom(getView(this.mState), getView(i));
        this.mState = i;
    }

    private void transitionFrom(final View view, final View view2) {
        view.post(new Runnable() {
            @Override
            public final void run() {
                ZenModePanel.lambda$transitionFrom$5(view2, view);
            }
        });
    }

    static void lambda$transitionFrom$5(View view, final View view2) {
        view.setAlpha(0.0f);
        view.setVisibility(0);
        view.bringToFront();
        view.animate().cancel();
        view.animate().alpha(1.0f).setDuration(300L).withEndAction(new Runnable() {
            @Override
            public final void run() {
                view2.setVisibility(4);
            }
        }).start();
    }

    private View getView(int i) {
        switch (i) {
            case 1:
                return this.mAutoRule;
            case 2:
                return this.mEmpty;
            default:
                return this.mEdit;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mConfigurableTexts.update();
        if (this.mZenButtons != null) {
            this.mZenButtons.update();
        }
    }

    private void confirmZenIntroduction() {
        String strPrefKeyForConfirmation = prefKeyForConfirmation(getSelectedZen(0));
        if (strPrefKeyForConfirmation == null) {
            return;
        }
        if (DEBUG) {
            Log.d("ZenModePanel", "confirmZenIntroduction " + strPrefKeyForConfirmation);
        }
        Prefs.putBoolean(this.mContext, strPrefKeyForConfirmation, true);
        this.mHandler.sendEmptyMessage(3);
    }

    private static String prefKeyForConfirmation(int i) {
        switch (i) {
            case 1:
                return "DndConfirmedPriorityIntroduction";
            case 2:
                return "DndConfirmedSilenceIntroduction";
            case 3:
                return "DndConfirmedAlarmIntroduction";
            default:
                return null;
        }
    }

    private void onAttach() {
        setExpanded(true);
        this.mAttachedZen = this.mController.getZen();
        ZenModeConfig.ZenRule manualRule = this.mController.getManualRule();
        this.mExitCondition = manualRule != null ? manualRule.condition : null;
        if (DEBUG) {
            Log.d(this.mTag, "onAttach " + this.mAttachedZen + " " + manualRule);
        }
        handleUpdateManualRule(manualRule);
        this.mZenButtons.setSelectedValue(Integer.valueOf(this.mAttachedZen), false);
        this.mSessionZen = this.mAttachedZen;
        this.mTransitionHelper.clear();
        this.mController.addCallback(this.mZenCallback);
        setSessionExitCondition(copy(this.mExitCondition));
        updateWidgets();
        setAttached(true);
    }

    private void onDetach() {
        if (DEBUG) {
            Log.d(this.mTag, "onDetach");
        }
        setExpanded(false);
        checkForAttachedZenChange();
        setAttached(false);
        this.mAttachedZen = -1;
        this.mSessionZen = -1;
        this.mController.removeCallback(this.mZenCallback);
        setSessionExitCondition(null);
        this.mTransitionHelper.clear();
    }

    @VisibleForTesting
    void setAttached(boolean z) {
        this.mAttached = z;
    }

    @Override
    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        if (z == this.mAttached) {
            return;
        }
        if (z) {
            onAttach();
        } else {
            onDetach();
        }
    }

    private void setSessionExitCondition(Condition condition) {
        if (Objects.equals(condition, this.mSessionExitCondition)) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "mSessionExitCondition=" + getConditionId(condition));
        }
        this.mSessionExitCondition = condition;
    }

    private void checkForAttachedZenChange() {
        int selectedZen = getSelectedZen(-1);
        if (DEBUG) {
            Log.d(this.mTag, "selectedZen=" + selectedZen);
        }
        if (selectedZen != this.mAttachedZen) {
            if (DEBUG) {
                Log.d(this.mTag, "attachedZen: " + this.mAttachedZen + " -> " + selectedZen);
            }
            if (selectedZen == 2) {
                this.mPrefs.trackNoneSelected();
            }
        }
    }

    private void setExpanded(boolean z) {
        if (z == this.mExpanded) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "setExpanded " + z);
        }
        this.mExpanded = z;
        updateWidgets();
        fireExpanded();
    }

    protected void addZenConditions(int i) {
        for (int i2 = 0; i2 < i; i2++) {
            View viewInflate = this.mInflater.inflate(this.mZenModeButtonLayoutId, this.mEdit, false);
            viewInflate.setId(i2);
            this.mZenRadioGroup.addView(viewInflate);
            View viewInflate2 = this.mInflater.inflate(this.mZenModeConditionLayoutId, this.mEdit, false);
            viewInflate2.setId(i2 + i);
            this.mZenRadioGroupContent.addView(viewInflate2);
        }
    }

    public void init(ZenModeController zenModeController) {
        this.mController = zenModeController;
        addZenConditions(3);
        this.mSessionZen = getSelectedZen(-1);
        handleUpdateManualRule(this.mController.getManualRule());
        if (DEBUG) {
            Log.d(this.mTag, "init mExitCondition=" + this.mExitCondition);
        }
        hideAllConditions();
    }

    private void setExitCondition(Condition condition) {
        if (Objects.equals(this.mExitCondition, condition)) {
            return;
        }
        this.mExitCondition = condition;
        if (DEBUG) {
            Log.d(this.mTag, "mExitCondition=" + getConditionId(this.mExitCondition));
        }
        updateWidgets();
    }

    private static Uri getConditionId(Condition condition) {
        if (condition != null) {
            return condition.id;
        }
        return null;
    }

    private Uri getRealConditionId(Condition condition) {
        if (isForever(condition)) {
            return null;
        }
        return getConditionId(condition);
    }

    private static Condition copy(Condition condition) {
        if (condition == null) {
            return null;
        }
        return condition.copy();
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @VisibleForTesting
    void handleUpdateManualRule(ZenModeConfig.ZenRule zenRule) {
        Condition conditionCreateCondition;
        handleUpdateZen(zenRule != null ? zenRule.zenMode : 0);
        if (zenRule == null) {
            conditionCreateCondition = null;
        } else {
            conditionCreateCondition = zenRule.condition != null ? zenRule.condition : createCondition(zenRule.conditionId);
        }
        handleUpdateConditions(conditionCreateCondition);
        setExitCondition(conditionCreateCondition);
    }

    private Condition createCondition(Uri uri) {
        if (ZenModeConfig.isValidCountdownToAlarmConditionId(uri)) {
            return ZenModeConfig.toNextAlarmCondition(this.mContext, ZenModeConfig.tryParseCountdownConditionId(uri), ActivityManager.getCurrentUser());
        }
        if (ZenModeConfig.isValidCountdownConditionId(uri)) {
            long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(uri);
            return ZenModeConfig.toTimeCondition(this.mContext, jTryParseCountdownConditionId, (int) (((jTryParseCountdownConditionId - System.currentTimeMillis()) + 30000) / 60000), ActivityManager.getCurrentUser(), false);
        }
        return forever();
    }

    private void handleUpdateZen(int i) {
        if (this.mSessionZen != -1 && this.mSessionZen != i) {
            this.mSessionZen = i;
        }
        this.mZenButtons.setSelectedValue(Integer.valueOf(i), false);
        updateWidgets();
    }

    @VisibleForTesting
    int getSelectedZen(int i) {
        Object selectedValue = this.mZenButtons.getSelectedValue();
        return selectedValue != null ? ((Integer) selectedValue).intValue() : i;
    }

    private void updateWidgets() {
        int i;
        if (this.mTransitionHelper.isTransitioning()) {
            this.mTransitionHelper.pendingUpdateWidgets();
            return;
        }
        int selectedZen = getSelectedZen(0);
        boolean z = true;
        boolean z2 = selectedZen == 1;
        boolean z3 = selectedZen == 2;
        boolean z4 = selectedZen == 3;
        if ((!z2 || this.mPrefs.mConfirmedPriorityIntroduction) && ((!z3 || this.mPrefs.mConfirmedSilenceIntroduction) && (!z4 || this.mPrefs.mConfirmedAlarmIntroduction))) {
            z = false;
        }
        this.mZenButtons.setVisibility(this.mHidden ? 8 : 0);
        this.mZenIntroduction.setVisibility(z ? 0 : 8);
        if (z) {
            if (z2) {
                i = R.string.zen_priority_introduction;
            } else if (z4) {
                i = R.string.zen_alarms_introduction;
            } else if (this.mVoiceCapable) {
                i = R.string.zen_silence_introduction_voice;
            } else {
                i = R.string.zen_silence_introduction;
            }
            this.mConfigurableTexts.add(this.mZenIntroductionMessage, i);
            this.mConfigurableTexts.update();
            this.mZenIntroductionCustomize.setVisibility(z2 ? 0 : 8);
        }
        String strComputeAlarmWarningText = computeAlarmWarningText(z3);
        this.mZenAlarmWarning.setVisibility(strComputeAlarmWarningText == null ? 8 : 0);
        this.mZenAlarmWarning.setText(strComputeAlarmWarningText);
    }

    private String computeAlarmWarningText(boolean z) {
        int i;
        if (!z) {
            return null;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        long nextAlarm = this.mController.getNextAlarm();
        if (nextAlarm < jCurrentTimeMillis) {
            return null;
        }
        if (this.mSessionExitCondition == null || isForever(this.mSessionExitCondition)) {
            i = R.string.zen_alarm_warning_indef;
        } else {
            long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(this.mSessionExitCondition.id);
            if (jTryParseCountdownConditionId > jCurrentTimeMillis && nextAlarm < jTryParseCountdownConditionId) {
                i = R.string.zen_alarm_warning;
            } else {
                i = 0;
            }
        }
        if (i == 0) {
            return null;
        }
        boolean z2 = nextAlarm - jCurrentTimeMillis < 86400000;
        boolean zIs24HourFormat = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
        return getResources().getString(i, getResources().getString(z2 ? R.string.alarm_template : R.string.alarm_template_far, DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), z2 ? zIs24HourFormat ? "Hm" : "hma" : zIs24HourFormat ? "EEEHm" : "EEEhma"), nextAlarm)));
    }

    @VisibleForTesting
    void handleUpdateConditions(Condition condition) {
        if (this.mTransitionHelper.isTransitioning()) {
            return;
        }
        bind(forever(), this.mZenRadioGroupContent.getChildAt(0), 0);
        if (condition == null) {
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isForever(condition)) {
            getConditionTagAt(0).rb.setChecked(true);
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isAlarm(condition)) {
            bindGenericCountdown();
            bindNextAlarm(condition);
            getConditionTagAt(2).rb.setChecked(true);
        } else if (isCountdown(condition)) {
            bindNextAlarm(getTimeUntilNextAlarmCondition());
            bind(condition, this.mZenRadioGroupContent.getChildAt(1), 1);
            getConditionTagAt(1).rb.setChecked(true);
        } else {
            Slog.wtf("ZenModePanel", "Invalid manual condition: " + condition);
        }
        this.mZenConditions.setVisibility(this.mSessionZen == 0 ? 8 : 0);
    }

    private void bindGenericCountdown() {
        this.mBucketIndex = DEFAULT_BUCKET_INDEX;
        Condition timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        if (!this.mAttached || getConditionTagAt(1).condition == null) {
            bind(timeCondition, this.mZenRadioGroupContent.getChildAt(1), 1);
        }
    }

    private void bindNextAlarm(Condition condition) {
        View childAt = this.mZenRadioGroupContent.getChildAt(2);
        ConditionTag conditionTag = (ConditionTag) childAt.getTag();
        if (condition != null && (!this.mAttached || conditionTag == null || conditionTag.condition == null)) {
            bind(condition, childAt, 2);
        }
        ConditionTag conditionTag2 = (ConditionTag) childAt.getTag();
        int i = 0;
        boolean z = (conditionTag2 == null || conditionTag2.condition == null) ? false : true;
        this.mZenRadioGroup.getChildAt(2).setVisibility(z ? 0 : 4);
        if (!z) {
            i = 4;
        }
        childAt.setVisibility(i);
    }

    private Condition forever() {
        return new Condition(this.mForeverId, foreverSummary(this.mContext), "", "", 0, 1, 0);
    }

    private static String foreverSummary(Context context) {
        return context.getString(android.R.string.oneMonthDurationPast);
    }

    private Condition getTimeUntilNextAlarmCondition() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        setToMidnight(gregorianCalendar);
        gregorianCalendar.add(5, 6);
        long nextAlarm = this.mController.getNextAlarm();
        if (nextAlarm > 0) {
            GregorianCalendar gregorianCalendar2 = new GregorianCalendar();
            gregorianCalendar2.setTimeInMillis(nextAlarm);
            setToMidnight(gregorianCalendar2);
            if (gregorianCalendar.compareTo((Calendar) gregorianCalendar2) >= 0) {
                return ZenModeConfig.toNextAlarmCondition(this.mContext, nextAlarm, ActivityManager.getCurrentUser());
            }
            return null;
        }
        return null;
    }

    private void setToMidnight(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }

    @VisibleForTesting
    ConditionTag getConditionTagAt(int i) {
        return (ConditionTag) this.mZenRadioGroupContent.getChildAt(i).getTag();
    }

    @VisibleForTesting
    int getVisibleConditions() {
        int childCount = this.mZenRadioGroupContent.getChildCount();
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            i += this.mZenRadioGroupContent.getChildAt(i2).getVisibility() == 0 ? 1 : 0;
        }
        return i;
    }

    private void hideAllConditions() {
        int childCount = this.mZenRadioGroupContent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mZenRadioGroupContent.getChildAt(i).setVisibility(8);
        }
    }

    private static boolean isAlarm(Condition condition) {
        return condition != null && ZenModeConfig.isValidCountdownToAlarmConditionId(condition.id);
    }

    private static boolean isCountdown(Condition condition) {
        return condition != null && ZenModeConfig.isValidCountdownConditionId(condition.id);
    }

    private boolean isForever(Condition condition) {
        return condition != null && this.mForeverId.equals(condition.id);
    }

    private void bind(Condition condition, final View view, final int i) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        boolean z = condition.state == 1;
        final ConditionTag conditionTag = view.getTag() != null ? (ConditionTag) view.getTag() : new ConditionTag();
        view.setTag(conditionTag);
        boolean z2 = conditionTag.rb == null;
        if (conditionTag.rb == null) {
            conditionTag.rb = (RadioButton) this.mZenRadioGroup.getChildAt(i);
        }
        conditionTag.condition = condition;
        final Uri conditionId = getConditionId(conditionTag.condition);
        if (DEBUG) {
            Log.d(this.mTag, "bind i=" + this.mZenRadioGroupContent.indexOfChild(view) + " first=" + z2 + " condition=" + conditionId);
        }
        conditionTag.rb.setEnabled(z);
        conditionTag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z3) {
                if (ZenModePanel.this.mExpanded && z3) {
                    conditionTag.rb.setChecked(true);
                    if (ZenModePanel.DEBUG) {
                        Log.d(ZenModePanel.this.mTag, "onCheckedChanged " + conditionId);
                    }
                    MetricsLogger.action(ZenModePanel.this.mContext, 164);
                    ZenModePanel.this.select(conditionTag.condition);
                    ZenModePanel.this.announceConditionSelection(conditionTag);
                }
            }
        });
        if (conditionTag.lines == null) {
            conditionTag.lines = view.findViewById(android.R.id.content);
        }
        if (conditionTag.line1 == null) {
            conditionTag.line1 = (TextView) view.findViewById(android.R.id.text1);
            this.mConfigurableTexts.add(conditionTag.line1);
        }
        if (conditionTag.line2 == null) {
            conditionTag.line2 = (TextView) view.findViewById(android.R.id.text2);
            this.mConfigurableTexts.add(conditionTag.line2);
        }
        String str = !TextUtils.isEmpty(condition.line1) ? condition.line1 : condition.summary;
        String str2 = condition.line2;
        conditionTag.line1.setText(str);
        if (TextUtils.isEmpty(str2)) {
            conditionTag.line2.setVisibility(8);
        } else {
            conditionTag.line2.setVisibility(0);
            conditionTag.line2.setText(str2);
        }
        conditionTag.lines.setEnabled(z);
        conditionTag.lines.setAlpha(z ? 1.0f : 0.4f);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.button1);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ZenModePanel.this.onClickTimeButton(view, conditionTag, false, i);
            }
        });
        ImageView imageView2 = (ImageView) view.findViewById(android.R.id.button2);
        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                ZenModePanel.this.onClickTimeButton(view, conditionTag, true, i);
            }
        });
        conditionTag.lines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                conditionTag.rb.setChecked(true);
            }
        });
        long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        if (i != 2 && jTryParseCountdownConditionId > 0) {
            imageView.setVisibility(0);
            imageView2.setVisibility(0);
            if (this.mBucketIndex > -1) {
                imageView.setEnabled(this.mBucketIndex > 0);
                imageView2.setEnabled(this.mBucketIndex < MINUTE_BUCKETS.length - 1);
            } else {
                imageView.setEnabled(jTryParseCountdownConditionId - System.currentTimeMillis() > ((long) (MIN_BUCKET_MINUTES * 60000)));
                imageView2.setEnabled(!Objects.equals(condition.summary, ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser()).summary));
            }
            imageView.setAlpha(imageView.isEnabled() ? 1.0f : 0.5f);
            imageView2.setAlpha(imageView2.isEnabled() ? 1.0f : 0.5f);
        } else {
            imageView.setVisibility(8);
            imageView2.setVisibility(8);
        }
        if (z2) {
            Interaction.register(conditionTag.rb, this.mInteractionCallback);
            Interaction.register(conditionTag.lines, this.mInteractionCallback);
            Interaction.register(imageView, this.mInteractionCallback);
            Interaction.register(imageView2, this.mInteractionCallback);
        }
        view.setVisibility(0);
    }

    private void announceConditionSelection(ConditionTag conditionTag) {
        String string;
        switch (getSelectedZen(0)) {
            case 1:
                string = this.mContext.getString(R.string.interruption_level_priority);
                break;
            case 2:
                string = this.mContext.getString(R.string.interruption_level_none);
                break;
            case 3:
                string = this.mContext.getString(R.string.interruption_level_alarms);
                break;
            default:
                return;
        }
        announceForAccessibility(this.mContext.getString(R.string.zen_mode_and_condition, string, conditionTag.line1.getText()));
    }

    private void onClickTimeButton(View view, ConditionTag conditionTag, boolean z, int i) {
        Condition timeCondition;
        int i2;
        MetricsLogger.action(this.mContext, 163, z);
        int length = MINUTE_BUCKETS.length;
        if (this.mBucketIndex == -1) {
            long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(getConditionId(conditionTag.condition));
            long jCurrentTimeMillis = System.currentTimeMillis();
            for (int i3 = 0; i3 < length; i3++) {
                if (!z) {
                    i2 = (length - 1) - i3;
                } else {
                    i2 = i3;
                }
                int i4 = MINUTE_BUCKETS[i2];
                long j = jCurrentTimeMillis + ((long) (60000 * i4));
                if ((z && j > jTryParseCountdownConditionId) || (!z && j < jTryParseCountdownConditionId)) {
                    this.mBucketIndex = i2;
                    timeCondition = ZenModeConfig.toTimeCondition(this.mContext, j, i4, ActivityManager.getCurrentUser(), false);
                    break;
                }
            }
            timeCondition = null;
            if (timeCondition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            this.mBucketIndex = Math.max(0, Math.min(length - 1, this.mBucketIndex + (z ? 1 : -1)));
            timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        bind(timeCondition, view, i);
        conditionTag.rb.setChecked(true);
        select(timeCondition);
        announceConditionSelection(conditionTag);
    }

    private void select(Condition condition) {
        if (DEBUG) {
            Log.d(this.mTag, "select " + condition);
        }
        if (this.mSessionZen == -1 || this.mSessionZen == 0) {
            if (DEBUG) {
                Log.d(this.mTag, "Ignoring condition selection outside of manual zen");
                return;
            }
            return;
        }
        final Uri realConditionId = getRealConditionId(condition);
        if (this.mController != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    ZenModePanel.this.mController.setZen(ZenModePanel.this.mSessionZen, realConditionId, "ZenModePanel.selectCondition");
                }
            });
        }
        setExitCondition(condition);
        if (realConditionId == null) {
            this.mPrefs.setMinuteIndex(-1);
        } else if ((isAlarm(condition) || isCountdown(condition)) && this.mBucketIndex != -1) {
            this.mPrefs.setMinuteIndex(this.mBucketIndex);
        }
        setSessionExitCondition(copy(condition));
    }

    private void fireInteraction() {
        if (this.mCallback != null) {
            this.mCallback.onInteraction();
        }
    }

    private void fireExpanded() {
        if (this.mCallback != null) {
            this.mCallback.onExpanded(this.mExpanded);
        }
    }

    private final class H extends Handler {
        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 2:
                    ZenModePanel.this.handleUpdateManualRule((ZenModeConfig.ZenRule) message.obj);
                    break;
                case 3:
                    ZenModePanel.this.updateWidgets();
                    break;
            }
        }
    }

    @VisibleForTesting
    static class ConditionTag {
        Condition condition;
        TextView line1;
        TextView line2;
        View lines;
        RadioButton rb;

        ConditionTag() {
        }
    }

    private final class ZenPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
        private boolean mConfirmedAlarmIntroduction;
        private boolean mConfirmedPriorityIntroduction;
        private boolean mConfirmedSilenceIntroduction;
        private int mMinuteIndex;
        private final int mNoneDangerousThreshold;
        private int mNoneSelected;

        private ZenPrefs() {
            this.mNoneDangerousThreshold = ZenModePanel.this.mContext.getResources().getInteger(R.integer.zen_mode_alarm_warning_threshold);
            Prefs.registerListener(ZenModePanel.this.mContext, this);
            updateMinuteIndex();
            updateNoneSelected();
            updateConfirmedPriorityIntroduction();
            updateConfirmedSilenceIntroduction();
            updateConfirmedAlarmIntroduction();
        }

        public void trackNoneSelected() {
            this.mNoneSelected = clampNoneSelected(this.mNoneSelected + 1);
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Setting none selected: " + this.mNoneSelected + " threshold=" + this.mNoneDangerousThreshold);
            }
            Prefs.putInt(ZenModePanel.this.mContext, "DndNoneSelected", this.mNoneSelected);
        }

        public void setMinuteIndex(int i) {
            int iClampIndex = clampIndex(i);
            if (iClampIndex == this.mMinuteIndex) {
                return;
            }
            this.mMinuteIndex = clampIndex(iClampIndex);
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Setting favorite minute index: " + this.mMinuteIndex);
            }
            Prefs.putInt(ZenModePanel.this.mContext, "DndCountdownMinuteIndex", this.mMinuteIndex);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            updateMinuteIndex();
            updateNoneSelected();
            updateConfirmedPriorityIntroduction();
            updateConfirmedSilenceIntroduction();
            updateConfirmedAlarmIntroduction();
        }

        private void updateMinuteIndex() {
            this.mMinuteIndex = clampIndex(Prefs.getInt(ZenModePanel.this.mContext, "DndCountdownMinuteIndex", ZenModePanel.DEFAULT_BUCKET_INDEX));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Favorite minute index: " + this.mMinuteIndex);
            }
        }

        private int clampIndex(int i) {
            return MathUtils.constrain(i, -1, ZenModePanel.MINUTE_BUCKETS.length - 1);
        }

        private void updateNoneSelected() {
            this.mNoneSelected = clampNoneSelected(Prefs.getInt(ZenModePanel.this.mContext, "DndNoneSelected", 0));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "None selected: " + this.mNoneSelected);
            }
        }

        private int clampNoneSelected(int i) {
            return MathUtils.constrain(i, 0, Integer.MAX_VALUE);
        }

        private void updateConfirmedPriorityIntroduction() {
            boolean z = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedPriorityIntroduction", false);
            if (z == this.mConfirmedPriorityIntroduction) {
                return;
            }
            this.mConfirmedPriorityIntroduction = z;
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Confirmed priority introduction: " + this.mConfirmedPriorityIntroduction);
            }
        }

        private void updateConfirmedSilenceIntroduction() {
            boolean z = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedSilenceIntroduction", false);
            if (z == this.mConfirmedSilenceIntroduction) {
                return;
            }
            this.mConfirmedSilenceIntroduction = z;
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Confirmed silence introduction: " + this.mConfirmedSilenceIntroduction);
            }
        }

        private void updateConfirmedAlarmIntroduction() {
            boolean z = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedAlarmIntroduction", false);
            if (z == this.mConfirmedAlarmIntroduction) {
                return;
            }
            this.mConfirmedAlarmIntroduction = z;
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Confirmed alarm introduction: " + this.mConfirmedAlarmIntroduction);
            }
        }
    }

    private final class TransitionHelper implements LayoutTransition.TransitionListener, Runnable {
        private boolean mPendingUpdateWidgets;
        private boolean mTransitioning;
        private final ArraySet<View> mTransitioningViews;

        private TransitionHelper() {
            this.mTransitioningViews = new ArraySet<>();
        }

        public void clear() {
            this.mTransitioningViews.clear();
            this.mPendingUpdateWidgets = false;
        }

        public void pendingUpdateWidgets() {
            this.mPendingUpdateWidgets = true;
        }

        public boolean isTransitioning() {
            return !this.mTransitioningViews.isEmpty();
        }

        @Override
        public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            this.mTransitioningViews.add(view);
            updateTransitioning();
        }

        @Override
        public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            this.mTransitioningViews.remove(view);
            updateTransitioning();
        }

        @Override
        public void run() {
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "TransitionHelper run mPendingUpdateWidgets=" + this.mPendingUpdateWidgets);
            }
            if (this.mPendingUpdateWidgets) {
                ZenModePanel.this.updateWidgets();
            }
            this.mPendingUpdateWidgets = false;
        }

        private void updateTransitioning() {
            boolean zIsTransitioning = isTransitioning();
            if (this.mTransitioning == zIsTransitioning) {
                return;
            }
            this.mTransitioning = zIsTransitioning;
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "TransitionHelper mTransitioning=" + this.mTransitioning);
            }
            if (!this.mTransitioning) {
                if (this.mPendingUpdateWidgets) {
                    ZenModePanel.this.mHandler.post(this);
                } else {
                    this.mPendingUpdateWidgets = false;
                }
            }
        }
    }
}
