package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.BatteryInfo;
import com.android.settingslib.graph.UsageView;
import com.android.systemui.BatteryMeterDrawable;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BatteryController;
import java.text.NumberFormat;

public class BatteryTile extends QSTile<QSTile.State> implements BatteryController.BatteryStateChangeCallback {
    private final BatteryController mBatteryController;
    private final BatteryDetail mBatteryDetail;
    private boolean mCharging;
    private boolean mDetailShown;
    private int mLevel;
    private boolean mPluggedIn;
    private boolean mPowerSave;

    public BatteryTile(QSTile.Host host) {
        super(host);
        this.mBatteryDetail = new BatteryDetail(this, null);
        this.mBatteryController = host.getBatteryController();
    }

    @Override
    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    @Override
    public QSTile.DetailAdapter getDetailAdapter() {
        return this.mBatteryDetail;
    }

    @Override
    public int getMetricsCategory() {
        return 261;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            this.mBatteryController.addStateChangedCallback(this);
        } else {
            this.mBatteryController.removeStateChangedCallback(this);
        }
    }

    @Override
    public void setDetailListening(boolean listening) {
        super.setDetailListening(listening);
        if (listening) {
            return;
        }
        this.mBatteryDetail.mCurrentView = null;
    }

    @Override
    public Intent getLongClickIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return new Intent("android.intent.action.POWER_USAGE_SUMMARY");
    }

    @Override
    protected void handleClick() {
        showDetail(true);
    }

    @Override
    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.battery);
    }

    @Override
    protected void handleUpdateState(QSTile.State state, Object arg) {
        int level = arg != null ? ((Integer) arg).intValue() : this.mLevel;
        String percentage = NumberFormat.getPercentInstance().format(((double) level) / 100.0d);
        state.icon = new QSTile.Icon() {
            @Override
            public Drawable getDrawable(Context context) {
                BatteryMeterDrawable drawable = new BatteryMeterDrawable(context, new Handler(Looper.getMainLooper()), context.getColor(R.color.batterymeter_frame_color));
                drawable.onBatteryLevelChanged(BatteryTile.this.mLevel, BatteryTile.this.mPluggedIn, BatteryTile.this.mCharging);
                drawable.onPowerSaveChanged(BatteryTile.this.mPowerSave);
                return drawable;
            }

            @Override
            public int getPadding() {
                return BatteryTile.this.mHost.getContext().getResources().getDimensionPixelSize(R.dimen.qs_battery_padding);
            }
        };
        state.label = percentage;
        state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_battery, percentage) + "," + (this.mPowerSave ? this.mContext.getString(R.string.battery_saver_notification_title) : this.mCharging ? this.mContext.getString(R.string.expanded_header_battery_charging) : "") + "," + this.mContext.getString(R.string.accessibility_battery_details);
        String name = Button.class.getName();
        state.expandedAccessibilityClassName = name;
        state.minimalAccessibilityClassName = name;
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        this.mLevel = level;
        this.mPluggedIn = pluggedIn;
        this.mCharging = charging;
        refreshState(Integer.valueOf(level));
        if (!this.mDetailShown) {
            return;
        }
        this.mBatteryDetail.postBindView();
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        this.mPowerSave = isPowerSave;
        refreshState(null);
        if (!this.mDetailShown) {
            return;
        }
        this.mBatteryDetail.postBindView();
    }

    private final class BatteryDetail implements QSTile.DetailAdapter, View.OnClickListener, View.OnAttachStateChangeListener {
        private View mCurrentView;
        private final BatteryMeterDrawable mDrawable;
        private final BroadcastReceiver mReceiver;

        BatteryDetail(BatteryTile this$0, BatteryDetail batteryDetail) {
            this();
        }

        private BatteryDetail() {
            this.mDrawable = new BatteryMeterDrawable(BatteryTile.this.mHost.getContext(), new Handler(), BatteryTile.this.mHost.getContext().getColor(R.color.batterymeter_frame_color));
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    BatteryDetail.this.postBindView();
                }
            };
        }

        @Override
        public CharSequence getTitle() {
            return BatteryTile.this.mContext.getString(R.string.battery_panel_title, Integer.valueOf(BatteryTile.this.mLevel));
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(BatteryTile.this.mContext).inflate(R.layout.battery_detail, parent, false);
            }
            this.mCurrentView = convertView;
            this.mCurrentView.addOnAttachStateChangeListener(this);
            bindView();
            return convertView;
        }

        public void postBindView() {
            if (this.mCurrentView == null) {
                return;
            }
            this.mCurrentView.post(new Runnable() {
                @Override
                public void run() {
                    BatteryDetail.this.bindView();
                }
            });
        }

        public void bindView() {
            if (this.mCurrentView == null) {
                return;
            }
            this.mDrawable.onBatteryLevelChanged(100, false, false);
            this.mDrawable.onPowerSaveChanged(true);
            this.mDrawable.disableShowPercent();
            ((ImageView) this.mCurrentView.findViewById(android.R.id.icon)).setImageDrawable(this.mDrawable);
            Checkable checkbox = (Checkable) this.mCurrentView.findViewById(android.R.id.toggle);
            checkbox.setChecked(BatteryTile.this.mPowerSave);
            BatteryInfo.getBatteryInfo(BatteryTile.this.mContext, new BatteryInfo.Callback() {
                @Override
                public void onBatteryInfoLoaded(BatteryInfo info) {
                    if (BatteryDetail.this.mCurrentView == null) {
                        return;
                    }
                    BatteryDetail.this.bindBatteryInfo(info);
                }
            });
            TextView batterySaverTitle = (TextView) this.mCurrentView.findViewById(android.R.id.title);
            TextView batterySaverSummary = (TextView) this.mCurrentView.findViewById(android.R.id.summary);
            if (BatteryTile.this.mCharging) {
                this.mCurrentView.findViewById(R.id.switch_container).setAlpha(0.7f);
                batterySaverTitle.setTextSize(2, 14.0f);
                batterySaverTitle.setText(R.string.battery_detail_charging_summary);
                this.mCurrentView.findViewById(android.R.id.toggle).setVisibility(8);
                this.mCurrentView.findViewById(R.id.switch_container).setClickable(false);
                return;
            }
            this.mCurrentView.findViewById(R.id.switch_container).setAlpha(1.0f);
            batterySaverTitle.setTextSize(2, 16.0f);
            batterySaverTitle.setText(R.string.battery_detail_switch_title);
            batterySaverSummary.setText(R.string.battery_detail_switch_summary);
            this.mCurrentView.findViewById(android.R.id.toggle).setVisibility(0);
            this.mCurrentView.findViewById(R.id.switch_container).setClickable(true);
            this.mCurrentView.findViewById(R.id.switch_container).setOnClickListener(this);
        }

        public void bindBatteryInfo(BatteryInfo info) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(info.batteryPercentString, new RelativeSizeSpan(2.6f), 17);
            if (info.remainingLabel != null) {
                if (BatteryTile.this.mContext.getResources().getBoolean(R.bool.quick_settings_wide)) {
                    builder.append(' ');
                } else {
                    builder.append('\n');
                }
                builder.append((CharSequence) info.remainingLabel);
            }
            ((TextView) this.mCurrentView.findViewById(R.id.charge_and_estimation)).setText(builder);
            info.bindHistory((UsageView) this.mCurrentView.findViewById(R.id.battery_usage), new BatteryInfo.BatteryDataParser[0]);
        }

        @Override
        public void onClick(View v) {
            BatteryTile.this.mBatteryController.setPowerSaveMode(!BatteryTile.this.mPowerSave);
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return new Intent("android.intent.action.POWER_USAGE_SUMMARY");
        }

        @Override
        public void setToggleState(boolean state) {
        }

        @Override
        public int getMetricsCategory() {
            return 274;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            if (BatteryTile.this.mDetailShown) {
                return;
            }
            BatteryTile.this.mDetailShown = true;
            v.getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.TIME_TICK"));
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            if (!BatteryTile.this.mDetailShown) {
                return;
            }
            BatteryTile.this.mDetailShown = false;
            v.getContext().unregisterReceiver(this.mReceiver);
        }
    }
}
