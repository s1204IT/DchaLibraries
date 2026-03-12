package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.Interaction;
import com.android.systemui.volume.SegmentedButtons;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;

public class ZenModePanel extends LinearLayout {
    private static final boolean DEBUG = Log.isLoggable("ZenModePanel", 3);
    private static final int DEFAULT_BUCKET_INDEX;
    private static final int MAX_BUCKET_MINUTES;
    private static final int[] MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES;
    public static final Intent ZEN_SETTINGS;
    private boolean mAttached;
    private int mAttachedZen;
    private int mBucketIndex;
    private Callback mCallback;
    private Condition[] mConditions;
    private final Context mContext;
    private ZenModeController mController;
    private final boolean mCountdownConditionSupported;
    private Condition mExitCondition;
    private String mExitConditionText;
    private boolean mExpanded;
    private final int mFirstConditionIndex;
    private final Uri mForeverId;
    private final H mHandler;
    private boolean mHidden;
    private final IconPulser mIconPulser;
    private final LayoutInflater mInflater;
    private final Interaction.Callback mInteractionCallback;
    private final Interpolator mInterpolator;
    private final int mMaxConditions;
    private final int mMaxOptionalConditions;
    private View mMoreSettings;
    private final Prefs mPrefs;
    private boolean mRequestingConditions;
    private Condition mSessionExitCondition;
    private int mSessionZen;
    private final int mSubheadColor;
    private final int mSubheadWarningColor;
    private String mTag;
    private Condition mTimeCondition;
    private final TransitionHelper mTransitionHelper;
    private SegmentedButtons mZenButtons;
    private final SegmentedButtons.Callback mZenButtonsCallback;
    private final ZenModeController.Callback mZenCallback;
    private LinearLayout mZenConditions;
    private View mZenSubhead;
    private TextView mZenSubheadCollapsed;
    private TextView mZenSubheadExpanded;

    public interface Callback {
        void onExpanded(boolean z);

        void onInteraction();

        void onMoreSettings();
    }

    static {
        MINUTE_BUCKETS = DEBUG ? new int[]{0, 1, 2, 5, 15, 30, 45, 60, 120, 180, 240, 480} : ZenModeConfig.MINUTE_BUCKETS;
        MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
        MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
        DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);
        ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    }

    public ZenModePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new H();
        this.mTransitionHelper = new TransitionHelper();
        this.mTag = "ZenModePanel/" + Integer.toHexString(System.identityHashCode(this));
        this.mBucketIndex = -1;
        this.mZenCallback = new ZenModeController.Callback() {
            @Override
            public void onZenChanged(int zen) {
                ZenModePanel.this.mHandler.obtainMessage(3, zen, 0).sendToTarget();
            }

            @Override
            public void onConditionsChanged(Condition[] conditions) {
                ZenModePanel.this.mHandler.obtainMessage(1, conditions).sendToTarget();
            }

            @Override
            public void onExitConditionChanged(Condition exitCondition) {
                ZenModePanel.this.mHandler.obtainMessage(2, exitCondition).sendToTarget();
            }
        };
        this.mZenButtonsCallback = new SegmentedButtons.Callback() {
            @Override
            public void onSelected(final Object value) {
                if (value != null && ZenModePanel.this.mZenButtons.isShown()) {
                    if (ZenModePanel.DEBUG) {
                        Log.d(ZenModePanel.this.mTag, "mZenButtonsCallback selected=" + value);
                    }
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            ZenModePanel.this.mController.setZen(((Integer) value).intValue());
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
        this.mPrefs = new Prefs();
        this.mInflater = LayoutInflater.from(this.mContext.getApplicationContext());
        this.mIconPulser = new IconPulser(this.mContext);
        Resources res = this.mContext.getResources();
        this.mSubheadWarningColor = res.getColor(R.color.system_warning_color);
        this.mSubheadColor = res.getColor(R.color.qs_subhead);
        this.mInterpolator = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_slow_in);
        this.mCountdownConditionSupported = NotificationManager.from(this.mContext).isSystemConditionProviderEnabled("countdown");
        int countdownDelta = this.mCountdownConditionSupported ? 1 : 0;
        this.mFirstConditionIndex = countdownDelta + 1;
        int minConditions = countdownDelta + 1;
        this.mMaxConditions = MathUtils.constrain(res.getInteger(R.integer.zen_mode_max_conditions), minConditions, 100);
        this.mMaxOptionalConditions = this.mMaxConditions - minConditions;
        this.mForeverId = Condition.newId(this.mContext).appendPath("forever").build();
        if (DEBUG) {
            Log.d(this.mTag, "new ZenModePanel");
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ZenModePanel state:");
        pw.print("  mCountdownConditionSupported=");
        pw.println(this.mCountdownConditionSupported);
        pw.print("  mMaxConditions=");
        pw.println(this.mMaxConditions);
        pw.print("  mRequestingConditions=");
        pw.println(this.mRequestingConditions);
        pw.print("  mAttached=");
        pw.println(this.mAttached);
        pw.print("  mHidden=");
        pw.println(this.mHidden);
        pw.print("  mExpanded=");
        pw.println(this.mExpanded);
        pw.print("  mSessionZen=");
        pw.println(this.mSessionZen);
        pw.print("  mAttachedZen=");
        pw.println(this.mAttachedZen);
        this.mTransitionHelper.dump(fd, pw, args);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mZenButtons = (SegmentedButtons) findViewById(R.id.zen_buttons);
        this.mZenButtons.addButton(R.string.interruption_level_none, R.drawable.ic_zen_none, 2);
        this.mZenButtons.addButton(R.string.interruption_level_priority, R.drawable.ic_zen_important, 1);
        this.mZenButtons.addButton(R.string.interruption_level_all, R.drawable.ic_zen_all, 0);
        this.mZenButtons.setCallback(this.mZenButtonsCallback);
        ViewGroup zenButtonsContainer = (ViewGroup) findViewById(R.id.zen_buttons_container);
        zenButtonsContainer.setLayoutTransition(newLayoutTransition(null));
        this.mZenSubhead = findViewById(R.id.zen_subhead);
        this.mZenSubheadCollapsed = (TextView) findViewById(R.id.zen_subhead_collapsed);
        this.mZenSubheadCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.setExpanded(true);
            }
        });
        Interaction.register(this.mZenSubheadCollapsed, this.mInteractionCallback);
        this.mZenSubheadExpanded = (TextView) findViewById(R.id.zen_subhead_expanded);
        Interaction.register(this.mZenSubheadExpanded, this.mInteractionCallback);
        this.mMoreSettings = findViewById(R.id.zen_more_settings);
        this.mMoreSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.fireMoreSettings();
            }
        });
        Interaction.register(this.mMoreSettings, this.mInteractionCallback);
        this.mZenConditions = (LinearLayout) findViewById(R.id.zen_conditions);
        for (int i = 0; i < this.mMaxConditions; i++) {
            this.mZenConditions.addView(this.mInflater.inflate(R.layout.zen_mode_condition, (ViewGroup) this, false));
        }
        setLayoutTransition(newLayoutTransition(this.mTransitionHelper));
    }

    private LayoutTransition newLayoutTransition(LayoutTransition.TransitionListener listener) {
        LayoutTransition transition = new LayoutTransition();
        transition.disableTransitionType(3);
        transition.disableTransitionType(1);
        transition.disableTransitionType(2);
        transition.setInterpolator(0, this.mInterpolator);
        if (listener != null) {
            transition.addTransitionListener(listener);
        }
        return transition;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
        this.mAttached = true;
        this.mAttachedZen = getSelectedZen(-1);
        this.mSessionZen = this.mAttachedZen;
        this.mTransitionHelper.clear();
        setSessionExitCondition(copy(this.mExitCondition));
        refreshExitConditionText();
        updateWidgets();
        setRequestingConditions(this.mHidden ? false : true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        checkForAttachedZenChange();
        this.mAttached = false;
        this.mAttachedZen = -1;
        this.mSessionZen = -1;
        setSessionExitCondition(null);
        setExpanded(false);
        setRequestingConditions(false);
        this.mTransitionHelper.clear();
    }

    private void setSessionExitCondition(Condition condition) {
        if (!Objects.equals(condition, this.mSessionExitCondition)) {
            if (DEBUG) {
                Log.d(this.mTag, "mSessionExitCondition=" + getConditionId(condition));
            }
            this.mSessionExitCondition = condition;
        }
    }

    public void setHidden(boolean hidden) {
        if (this.mHidden != hidden) {
            if (DEBUG) {
                Log.d(this.mTag, "hidden=" + hidden);
            }
            this.mHidden = hidden;
            setRequestingConditions(this.mAttached && !this.mHidden);
            updateWidgets();
        }
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

    public void setExpanded(boolean expanded) {
        if (expanded != this.mExpanded) {
            this.mExpanded = expanded;
            if (this.mExpanded) {
                ensureSelection();
            }
            updateWidgets();
            fireExpanded();
        }
    }

    private void setRequestingConditions(final boolean requesting) {
        if (this.mRequestingConditions != requesting) {
            if (DEBUG) {
                Log.d(this.mTag, "setRequestingConditions " + requesting);
            }
            this.mRequestingConditions = requesting;
            if (this.mController != null) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        ZenModePanel.this.mController.requestConditions(requesting);
                    }
                });
            }
            if (this.mRequestingConditions) {
                this.mTimeCondition = parseExistingTimeCondition(this.mExitCondition);
                if (this.mTimeCondition != null) {
                    this.mBucketIndex = -1;
                } else {
                    this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                    this.mTimeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
                }
                if (DEBUG) {
                    Log.d(this.mTag, "Initial bucket index: " + this.mBucketIndex);
                }
                this.mConditions = null;
                handleUpdateConditions();
                return;
            }
            hideAllConditions();
        }
    }

    public void init(ZenModeController controller) {
        this.mController = controller;
        setExitCondition(this.mController.getExitCondition());
        refreshExitConditionText();
        this.mSessionZen = getSelectedZen(-1);
        handleUpdateZen(this.mController.getZen());
        if (DEBUG) {
            Log.d(this.mTag, "init mExitCondition=" + this.mExitCondition);
        }
        hideAllConditions();
        this.mController.addCallback(this.mZenCallback);
    }

    public void updateLocale() {
        this.mZenButtons.updateLocale();
    }

    private void setExitCondition(Condition exitCondition) {
        if (!Objects.equals(this.mExitCondition, exitCondition)) {
            this.mExitCondition = exitCondition;
            if (DEBUG) {
                Log.d(this.mTag, "mExitCondition=" + getConditionId(this.mExitCondition));
            }
            refreshExitConditionText();
            updateWidgets();
        }
    }

    private static Uri getConditionId(Condition condition) {
        if (condition != null) {
            return condition.id;
        }
        return null;
    }

    private static boolean sameConditionId(Condition lhs, Condition rhs) {
        return lhs == null ? rhs == null : rhs != null && lhs.id.equals(rhs.id);
    }

    private static Condition copy(Condition condition) {
        if (condition == null) {
            return null;
        }
        return condition.copy();
    }

    private void refreshExitConditionText() {
        if (this.mExitCondition == null) {
            this.mExitConditionText = foreverSummary();
        } else if (isCountdown(this.mExitCondition)) {
            Condition condition = parseExistingTimeCondition(this.mExitCondition);
            this.mExitConditionText = condition != null ? condition.summary : foreverSummary();
        } else {
            this.mExitConditionText = this.mExitCondition.summary;
        }
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void showSilentHint() {
        if (DEBUG) {
            Log.d(this.mTag, "showSilentHint");
        }
        if (this.mZenButtons != null && this.mZenButtons.getChildCount() != 0) {
            View noneButton = this.mZenButtons.getChildAt(0);
            this.mIconPulser.start(noneButton);
        }
    }

    public void handleUpdateZen(int zen) {
        if (this.mSessionZen != -1 && this.mSessionZen != zen) {
            setExpanded(zen != 0);
            this.mSessionZen = zen;
        }
        this.mZenButtons.setSelectedValue(Integer.valueOf(zen));
        updateWidgets();
        handleUpdateConditions();
        if (this.mExpanded) {
            Condition selected = getSelectedCondition();
            if (!Objects.equals(this.mExitCondition, selected)) {
                select(selected);
            }
        }
    }

    private Condition getSelectedCondition() {
        int N = getVisibleConditions();
        for (int i = 0; i < N; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && tag.rb.isChecked()) {
                return tag.condition;
            }
        }
        return null;
    }

    private int getSelectedZen(int defValue) {
        Object zen = this.mZenButtons.getSelectedValue();
        if (zen == null) {
            return defValue;
        }
        int defValue2 = ((Integer) zen).intValue();
        return defValue2;
    }

    public void updateWidgets() {
        if (this.mTransitionHelper.isTransitioning()) {
            this.mTransitionHelper.pendingUpdateWidgets();
            return;
        }
        int zen = getSelectedZen(0);
        boolean zenOff = zen == 0;
        boolean zenImportant = zen == 1;
        boolean zenNone = zen == 2;
        boolean expanded = !this.mHidden && this.mExpanded;
        if (BenesseExtension.getDchaState() != 0) {
            this.mZenButtons.setVisibility(8);
        } else {
            this.mZenButtons.setVisibility(this.mHidden ? 8 : 0);
        }
        this.mZenSubhead.setVisibility((this.mHidden || zenOff) ? 8 : 0);
        this.mZenSubheadExpanded.setVisibility(expanded ? 0 : 8);
        this.mZenSubheadCollapsed.setVisibility(!expanded ? 0 : 8);
        this.mMoreSettings.setVisibility((zenImportant && expanded) ? 0 : 8);
        this.mZenConditions.setVisibility((zenOff || !expanded) ? 8 : 0);
        if (zenNone) {
            this.mZenSubheadExpanded.setText(R.string.zen_no_interruptions_with_warning);
            this.mZenSubheadCollapsed.setText(this.mExitConditionText);
        } else if (zenImportant) {
            this.mZenSubheadExpanded.setText(R.string.zen_important_interruptions);
            this.mZenSubheadCollapsed.setText(this.mExitConditionText);
        }
        this.mZenSubheadExpanded.setTextColor((zenNone && this.mPrefs.isNoneDangerous()) ? this.mSubheadWarningColor : this.mSubheadColor);
    }

    private Condition parseExistingTimeCondition(Condition condition) {
        if (condition == null) {
            return null;
        }
        long time = ZenModeConfig.tryParseCountdownConditionId(condition.id);
        if (time == 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        long span = time - now;
        if (span <= 0 || span > MAX_BUCKET_MINUTES * 60000) {
            return null;
        }
        return ZenModeConfig.toTimeCondition(this.mContext, time, Math.round(span / 60000.0f), now, ActivityManager.getCurrentUser());
    }

    public void handleUpdateConditions(Condition[] conditions) {
        Condition[] conditions2 = trimConditions(conditions);
        if (Arrays.equals(conditions2, this.mConditions)) {
            int count = this.mConditions == null ? 0 : this.mConditions.length;
            if (DEBUG) {
                Log.d(this.mTag, "handleUpdateConditions unchanged conditionCount=" + count);
                return;
            }
            return;
        }
        this.mConditions = conditions2;
        handleUpdateConditions();
    }

    private Condition[] trimConditions(Condition[] conditions) {
        if (conditions == null || conditions.length <= this.mMaxOptionalConditions) {
            return conditions;
        }
        int found = -1;
        int i = 0;
        while (true) {
            if (i >= conditions.length) {
                break;
            }
            Condition c = conditions[i];
            if (this.mSessionExitCondition == null || !sameConditionId(this.mSessionExitCondition, c)) {
                i++;
            } else {
                found = i;
                break;
            }
        }
        Condition[] rt = (Condition[]) Arrays.copyOf(conditions, this.mMaxOptionalConditions);
        if (found >= this.mMaxOptionalConditions) {
            rt[this.mMaxOptionalConditions - 1] = conditions[found];
            return rt;
        }
        return rt;
    }

    public void handleUpdateConditions() {
        if (this.mTransitionHelper.isTransitioning()) {
            this.mTransitionHelper.pendingUpdateConditions();
            return;
        }
        int conditionCount = this.mConditions == null ? 0 : this.mConditions.length;
        if (DEBUG) {
            Log.d(this.mTag, "handleUpdateConditions conditionCount=" + conditionCount);
        }
        bind(forever(), this.mZenConditions.getChildAt(0));
        if (this.mCountdownConditionSupported && this.mTimeCondition != null) {
            bind(this.mTimeCondition, this.mZenConditions.getChildAt(1));
        }
        for (int i = 0; i < conditionCount; i++) {
            bind(this.mConditions[i], this.mZenConditions.getChildAt(this.mFirstConditionIndex + i));
        }
        for (int i2 = this.mZenConditions.getChildCount() - 1; i2 > this.mFirstConditionIndex + conditionCount; i2--) {
            this.mZenConditions.getChildAt(i2).setVisibility(8);
        }
        if (this.mExpanded) {
            ensureSelection();
        }
    }

    private Condition forever() {
        return new Condition(this.mForeverId, foreverSummary(), "", "", 0, 1, 0);
    }

    private String foreverSummary() {
        return this.mContext.getString(android.R.string.network_partial_connectivity_detailed);
    }

    public ConditionTag getConditionTagAt(int index) {
        return (ConditionTag) this.mZenConditions.getChildAt(index).getTag();
    }

    public int getVisibleConditions() {
        int rt = 0;
        int N = this.mZenConditions.getChildCount();
        for (int i = 0; i < N; i++) {
            rt += this.mZenConditions.getChildAt(i).getVisibility() == 0 ? 1 : 0;
        }
        return rt;
    }

    private void hideAllConditions() {
        int N = this.mZenConditions.getChildCount();
        for (int i = 0; i < N; i++) {
            this.mZenConditions.getChildAt(i).setVisibility(8);
        }
    }

    private void ensureSelection() {
        int visibleConditions = getVisibleConditions();
        if (visibleConditions != 0) {
            for (int i = 0; i < visibleConditions; i++) {
                ConditionTag tag = getConditionTagAt(i);
                if (tag != null && tag.rb.isChecked()) {
                    if (DEBUG) {
                        Log.d(this.mTag, "Not selecting a default, checked=" + tag.condition);
                        return;
                    }
                    return;
                }
            }
            ConditionTag foreverTag = getConditionTagAt(0);
            if (foreverTag != null) {
                if (DEBUG) {
                    Log.d(this.mTag, "Selecting a default");
                }
                int favoriteIndex = this.mPrefs.getMinuteIndex();
                if (favoriteIndex == -1 || !this.mCountdownConditionSupported) {
                    foreverTag.rb.setChecked(true);
                    return;
                }
                this.mTimeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[favoriteIndex], ActivityManager.getCurrentUser());
                this.mBucketIndex = favoriteIndex;
                bind(this.mTimeCondition, this.mZenConditions.getChildAt(1));
                getConditionTagAt(1).rb.setChecked(true);
            }
        }
    }

    public void handleExitConditionChanged(Condition exitCondition) {
        setExitCondition(exitCondition);
        if (DEBUG) {
            Log.d(this.mTag, "handleExitConditionChanged " + this.mExitCondition);
        }
        int N = getVisibleConditions();
        for (int i = 0; i < N; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && sameConditionId(tag.condition, this.mExitCondition)) {
                bind(exitCondition, this.mZenConditions.getChildAt(i));
            }
        }
    }

    private boolean isCountdown(Condition c) {
        return c != null && ZenModeConfig.isValidCountdownConditionId(c.id);
    }

    private boolean isForever(Condition c) {
        return c != null && this.mForeverId.equals(c.id);
    }

    private void bind(Condition condition, final View row) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        boolean enabled = condition.state == 1;
        final ConditionTag tag = row.getTag() != null ? (ConditionTag) row.getTag() : new ConditionTag();
        row.setTag(tag);
        boolean first = tag.rb == null;
        if (tag.rb == null) {
            tag.rb = (RadioButton) row.findViewById(android.R.id.checkbox);
        }
        tag.condition = condition;
        final Uri conditionId = getConditionId(tag.condition);
        if (DEBUG) {
            Log.d(this.mTag, "bind i=" + this.mZenConditions.indexOfChild(row) + " first=" + first + " condition=" + conditionId);
        }
        tag.rb.setEnabled(enabled);
        boolean checked = !(this.mSessionExitCondition == null && this.mAttachedZen == 0) && (sameConditionId(this.mSessionExitCondition, tag.condition) || (isCountdown(this.mSessionExitCondition) && isCountdown(tag.condition)));
        if (checked != tag.rb.isChecked()) {
            if (DEBUG) {
                Log.d(this.mTag, "bind checked=" + checked + " condition=" + conditionId);
            }
            tag.rb.setChecked(checked);
        }
        tag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (ZenModePanel.this.mExpanded && isChecked) {
                    if (ZenModePanel.DEBUG) {
                        Log.d(ZenModePanel.this.mTag, "onCheckedChanged " + conditionId);
                    }
                    int N = ZenModePanel.this.getVisibleConditions();
                    for (int i = 0; i < N; i++) {
                        ConditionTag childTag = ZenModePanel.this.getConditionTagAt(i);
                        if (childTag != null && childTag != tag) {
                            childTag.rb.setChecked(false);
                        }
                    }
                    ZenModePanel.this.select(tag.condition);
                    ZenModePanel.this.announceConditionSelection(tag);
                }
            }
        });
        if (tag.lines == null) {
            tag.lines = row.findViewById(android.R.id.content);
        }
        if (tag.line1 == null) {
            tag.line1 = (TextView) row.findViewById(android.R.id.text1);
        }
        if (tag.line2 == null) {
            tag.line2 = (TextView) row.findViewById(android.R.id.text2);
        }
        String line1 = !TextUtils.isEmpty(condition.line1) ? condition.line1 : condition.summary;
        String line2 = condition.line2;
        tag.line1.setText(line1);
        if (TextUtils.isEmpty(line2)) {
            tag.line2.setVisibility(8);
        } else {
            tag.line2.setVisibility(0);
            tag.line2.setText(line2);
        }
        tag.lines.setEnabled(enabled);
        tag.lines.setAlpha(enabled ? 1.0f : 0.4f);
        ImageView button1 = (ImageView) row.findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.onClickTimeButton(row, tag, false);
            }
        });
        ImageView button2 = (ImageView) row.findViewById(android.R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.onClickTimeButton(row, tag, true);
            }
        });
        tag.lines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tag.rb.setChecked(true);
            }
        });
        long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        if (time > 0) {
            button1.setVisibility(0);
            button2.setVisibility(0);
            if (this.mBucketIndex > -1) {
                button1.setEnabled(this.mBucketIndex > 0);
                button2.setEnabled(this.mBucketIndex < MINUTE_BUCKETS.length + (-1));
            } else {
                long span = time - System.currentTimeMillis();
                button1.setEnabled(span > ((long) (MIN_BUCKET_MINUTES * 60000)));
                Condition maxCondition = ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser());
                button2.setEnabled(!Objects.equals(condition.summary, maxCondition.summary));
            }
            button1.setAlpha(button1.isEnabled() ? 1.0f : 0.5f);
            button2.setAlpha(button2.isEnabled() ? 1.0f : 0.5f);
        } else {
            button1.setVisibility(8);
            button2.setVisibility(8);
        }
        if (first) {
            Interaction.register(tag.rb, this.mInteractionCallback);
            Interaction.register(tag.lines, this.mInteractionCallback);
            Interaction.register(button1, this.mInteractionCallback);
            Interaction.register(button2, this.mInteractionCallback);
        }
        row.setVisibility(0);
    }

    public void announceConditionSelection(ConditionTag tag) {
        String modeText;
        int zen = getSelectedZen(0);
        switch (zen) {
            case 1:
                modeText = this.mContext.getString(R.string.zen_important_interruptions);
                break;
            case 2:
                modeText = this.mContext.getString(R.string.zen_no_interruptions);
                break;
            default:
                return;
        }
        announceForAccessibility(this.mContext.getString(R.string.zen_mode_and_condition, modeText, tag.line1.getText()));
    }

    public void onClickTimeButton(View row, ConditionTag tag, boolean up) {
        Condition newCondition = null;
        int N = MINUTE_BUCKETS.length;
        if (this.mBucketIndex == -1) {
            Uri conditionId = getConditionId(tag.condition);
            long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
            long now = System.currentTimeMillis();
            for (int i = 0; i < N; i++) {
                int j = up ? i : (N - 1) - i;
                int bucketMinutes = MINUTE_BUCKETS[j];
                long bucketTime = now + ((long) (60000 * bucketMinutes));
                if ((up && bucketTime > time) || (!up && bucketTime < time)) {
                    this.mBucketIndex = j;
                    newCondition = ZenModeConfig.toTimeCondition(this.mContext, bucketTime, bucketMinutes, now, ActivityManager.getCurrentUser());
                    break;
                }
            }
            if (newCondition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            this.mBucketIndex = Math.max(0, Math.min(N - 1, (up ? 1 : -1) + this.mBucketIndex));
            newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        this.mTimeCondition = newCondition;
        bind(this.mTimeCondition, row);
        tag.rb.setChecked(true);
        select(this.mTimeCondition);
        announceConditionSelection(tag);
    }

    public void select(final Condition condition) {
        if (DEBUG) {
            Log.d(this.mTag, "select " + condition);
        }
        final boolean isForever = isForever(condition);
        if (this.mController != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    ZenModePanel.this.mController.setExitCondition(isForever ? null : condition);
                }
            });
        }
        setExitCondition(condition);
        if (isForever) {
            this.mPrefs.setMinuteIndex(-1);
        } else if (isCountdown(condition) && this.mBucketIndex != -1) {
            this.mPrefs.setMinuteIndex(this.mBucketIndex);
        }
        setSessionExitCondition(copy(condition));
    }

    public void fireMoreSettings() {
        if (this.mCallback != null) {
            this.mCallback.onMoreSettings();
        }
    }

    public void fireInteraction() {
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
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ZenModePanel.this.handleUpdateConditions((Condition[]) msg.obj);
            } else if (msg.what == 2) {
                ZenModePanel.this.handleExitConditionChanged((Condition) msg.obj);
            } else if (msg.what == 3) {
                ZenModePanel.this.handleUpdateZen(msg.arg1);
            }
        }
    }

    private static class ConditionTag {
        Condition condition;
        TextView line1;
        TextView line2;
        View lines;
        RadioButton rb;

        private ConditionTag() {
        }
    }

    private final class Prefs implements SharedPreferences.OnSharedPreferenceChangeListener {
        private int mMinuteIndex;
        private final int mNoneDangerousThreshold;
        private int mNoneSelected;

        private Prefs() {
            this.mNoneDangerousThreshold = ZenModePanel.this.mContext.getResources().getInteger(R.integer.zen_mode_alarm_warning_threshold);
            prefs().registerOnSharedPreferenceChangeListener(this);
            updateMinuteIndex();
            updateNoneSelected();
        }

        public boolean isNoneDangerous() {
            return this.mNoneSelected < this.mNoneDangerousThreshold;
        }

        public void trackNoneSelected() {
            this.mNoneSelected = clampNoneSelected(this.mNoneSelected + 1);
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Setting none selected: " + this.mNoneSelected + " threshold=" + this.mNoneDangerousThreshold);
            }
            prefs().edit().putInt("noneSelected", this.mNoneSelected).apply();
        }

        public int getMinuteIndex() {
            return this.mMinuteIndex;
        }

        public void setMinuteIndex(int minuteIndex) {
            int minuteIndex2 = clampIndex(minuteIndex);
            if (minuteIndex2 != this.mMinuteIndex) {
                this.mMinuteIndex = clampIndex(minuteIndex2);
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "Setting favorite minute index: " + this.mMinuteIndex);
                }
                prefs().edit().putInt("minuteIndex", this.mMinuteIndex).apply();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updateMinuteIndex();
            updateNoneSelected();
        }

        private SharedPreferences prefs() {
            return ZenModePanel.this.mContext.getSharedPreferences(ZenModePanel.this.mContext.getPackageName(), 0);
        }

        private void updateMinuteIndex() {
            this.mMinuteIndex = clampIndex(prefs().getInt("minuteIndex", ZenModePanel.DEFAULT_BUCKET_INDEX));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Favorite minute index: " + this.mMinuteIndex);
            }
        }

        private int clampIndex(int index) {
            return MathUtils.constrain(index, -1, ZenModePanel.MINUTE_BUCKETS.length - 1);
        }

        private void updateNoneSelected() {
            this.mNoneSelected = clampNoneSelected(prefs().getInt("noneSelected", 0));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "None selected: " + this.mNoneSelected);
            }
        }

        private int clampNoneSelected(int noneSelected) {
            return MathUtils.constrain(noneSelected, 0, Integer.MAX_VALUE);
        }
    }

    private final class TransitionHelper implements LayoutTransition.TransitionListener, Runnable {
        private boolean mPendingUpdateConditions;
        private boolean mPendingUpdateWidgets;
        private boolean mTransitioning;
        private final ArraySet<View> mTransitioningViews;

        private TransitionHelper() {
            this.mTransitioningViews = new ArraySet<>();
        }

        public void clear() {
            this.mTransitioningViews.clear();
            this.mPendingUpdateWidgets = false;
            this.mPendingUpdateConditions = false;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("  TransitionHelper state:");
            pw.print("    mPendingUpdateConditions=");
            pw.println(this.mPendingUpdateConditions);
            pw.print("    mPendingUpdateWidgets=");
            pw.println(this.mPendingUpdateWidgets);
            pw.print("    mTransitioning=");
            pw.println(this.mTransitioning);
            pw.print("    mTransitioningViews=");
            pw.println(this.mTransitioningViews);
        }

        public void pendingUpdateConditions() {
            this.mPendingUpdateConditions = true;
        }

        public void pendingUpdateWidgets() {
            this.mPendingUpdateWidgets = true;
        }

        public boolean isTransitioning() {
            return !this.mTransitioningViews.isEmpty();
        }

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            this.mTransitioningViews.add(view);
            updateTransitioning();
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            this.mTransitioningViews.remove(view);
            updateTransitioning();
        }

        @Override
        public void run() {
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "TransitionHelper run mPendingUpdateWidgets=" + this.mPendingUpdateWidgets + " mPendingUpdateConditions=" + this.mPendingUpdateConditions);
            }
            if (this.mPendingUpdateWidgets) {
                ZenModePanel.this.updateWidgets();
            }
            if (this.mPendingUpdateConditions) {
                ZenModePanel.this.handleUpdateConditions();
            }
            this.mPendingUpdateConditions = false;
            this.mPendingUpdateWidgets = false;
        }

        private void updateTransitioning() {
            boolean transitioning = isTransitioning();
            if (this.mTransitioning != transitioning) {
                this.mTransitioning = transitioning;
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "TransitionHelper mTransitioning=" + this.mTransitioning);
                }
                if (!this.mTransitioning) {
                    if (this.mPendingUpdateConditions || this.mPendingUpdateWidgets) {
                        ZenModePanel.this.mHandler.post(this);
                    } else {
                        this.mPendingUpdateWidgets = false;
                        this.mPendingUpdateConditions = false;
                    }
                }
            }
        }
    }
}
