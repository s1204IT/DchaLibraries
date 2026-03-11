package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.service.notification.ZenModeConfig;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.Objects;

public class ZenFooter extends LinearLayout {
    private static final String TAG = Util.logTag(ZenFooter.class);
    private ZenModeConfig mConfig;
    private final Context mContext;
    private ZenModeController mController;
    private TextView mEndNowButton;
    private ImageView mIcon;
    private final SpTexts mSpTexts;
    private TextView mSummaryLine1;
    private TextView mSummaryLine2;
    private int mZen;
    private final ZenModeController.Callback mZenCallback;

    public ZenFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mZen = -1;
        this.mZenCallback = new ZenModeController.Callback() {
            @Override
            public void onZenChanged(int zen) {
                ZenFooter.this.setZen(zen);
            }

            @Override
            public void onConfigChanged(ZenModeConfig config) {
                ZenFooter.this.setConfig(config);
            }
        };
        this.mContext = context;
        this.mSpTexts = new SpTexts(this.mContext);
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(new ValueAnimator().getDuration() / 2);
        setLayoutTransition(layoutTransition);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(R.id.volume_zen_icon);
        this.mSummaryLine1 = (TextView) findViewById(R.id.volume_zen_summary_line_1);
        this.mSummaryLine2 = (TextView) findViewById(R.id.volume_zen_summary_line_2);
        this.mEndNowButton = (TextView) findViewById(R.id.volume_zen_end_now);
        this.mSpTexts.add(this.mSummaryLine1);
        this.mSpTexts.add(this.mSummaryLine2);
        this.mSpTexts.add(this.mEndNowButton);
    }

    public void init(final ZenModeController controller) {
        this.mEndNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.setZen(0, null, ZenFooter.TAG);
            }
        });
        this.mZen = controller.getZen();
        this.mConfig = controller.getConfig();
        this.mController = controller;
        this.mController.addCallback(this.mZenCallback);
        update();
    }

    public void cleanup() {
        this.mController.removeCallback(this.mZenCallback);
    }

    public void setZen(int zen) {
        if (this.mZen == zen) {
            return;
        }
        this.mZen = zen;
        update();
    }

    public void setConfig(ZenModeConfig config) {
        if (Objects.equals(this.mConfig, config)) {
            return;
        }
        this.mConfig = config;
        update();
    }

    private boolean isZenPriority() {
        return this.mZen == 1;
    }

    private boolean isZenAlarms() {
        return this.mZen == 3;
    }

    private boolean isZenNone() {
        return this.mZen == 2;
    }

    public void update() {
        String line1;
        boolean isForever;
        this.mIcon.setImageResource(isZenNone() ? R.drawable.ic_dnd_total_silence : R.drawable.ic_dnd);
        if (isZenPriority()) {
            line1 = this.mContext.getString(R.string.interruption_level_priority);
        } else if (isZenAlarms()) {
            line1 = this.mContext.getString(R.string.interruption_level_alarms);
        } else {
            line1 = isZenNone() ? this.mContext.getString(R.string.interruption_level_none) : null;
        }
        Util.setText(this.mSummaryLine1, line1);
        if (this.mConfig == null || this.mConfig.manualRule == null) {
            isForever = false;
        } else {
            isForever = this.mConfig.manualRule.conditionId == null;
        }
        CharSequence line2 = isForever ? this.mContext.getString(android.R.string.lockscreen_instructions_when_pattern_enabled) : ZenModeConfig.getConditionSummary(this.mContext, this.mConfig, this.mController.getCurrentUser(), true);
        Util.setText(this.mSummaryLine2, line2);
    }

    public void onConfigurationChanged() {
        Util.setText(this.mEndNowButton, this.mContext.getString(R.string.volume_zen_end_now));
        this.mSpTexts.update();
    }
}
