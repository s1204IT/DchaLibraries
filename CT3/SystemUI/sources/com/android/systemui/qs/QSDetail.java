package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.phone.BaseStatusBarHeader;
import com.android.systemui.statusbar.phone.QSTileHost;

public class QSDetail extends LinearLayout {
    private QSDetailClipper mClipper;
    private boolean mClosingDetail;
    private QSTile.DetailAdapter mDetailAdapter;
    private ViewGroup mDetailContent;
    private TextView mDetailDoneButton;
    private TextView mDetailSettingsButton;
    private final SparseArray<View> mDetailViews;
    private boolean mFullyExpanded;
    private BaseStatusBarHeader mHeader;
    private final AnimatorListenerAdapter mHideGridContentWhenDone;
    private QSTileHost mHost;
    private int mOpenX;
    private int mOpenY;
    private View mQsDetailHeader;
    private View mQsDetailHeaderBack;
    private ImageView mQsDetailHeaderProgress;
    private Switch mQsDetailHeaderSwitch;
    private TextView mQsDetailHeaderTitle;
    private QSPanel mQsPanel;
    private final QSPanel.Callback mQsPanelCallback;
    private boolean mScanState;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone;
    private boolean mTriggeredExpand;

    public QSDetail(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDetailViews = new SparseArray<>();
        this.mQsPanelCallback = new QSPanel.Callback() {
            @Override
            public void onToggleStateChanged(final boolean state) {
                QSDetail.this.post(new Runnable() {
                    @Override
                    public void run() {
                        QSDetail.this.handleToggleStateChanged(state);
                    }
                });
            }

            @Override
            public void onShowingDetail(final QSTile.DetailAdapter detail, final int x, final int y) {
                QSDetail.this.post(new Runnable() {
                    @Override
                    public void run() {
                        QSDetail.this.handleShowingDetail(detail, x, y);
                    }
                });
            }

            @Override
            public void onScanStateChanged(final boolean state) {
                QSDetail.this.post(new Runnable() {
                    @Override
                    public void run() {
                        QSDetail.this.handleScanStateChanged(state);
                    }
                });
            }
        };
        this.mHideGridContentWhenDone = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                animation.removeListener(this);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (QSDetail.this.mDetailAdapter == null) {
                    return;
                }
                QSDetail.this.mQsPanel.setGridContentVisibility(false);
                QSDetail.this.mHeader.setVisibility(4);
            }
        };
        this.mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                QSDetail.this.mDetailContent.removeAllViews();
                QSDetail.this.setVisibility(4);
                QSDetail.this.mClosingDetail = false;
            }
        };
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mDetailDoneButton, R.dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(this.mDetailSettingsButton, R.dimen.qs_detail_button_text_size);
        for (int i = 0; i < this.mDetailViews.size(); i++) {
            this.mDetailViews.valueAt(i).dispatchConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDetailContent = (ViewGroup) findViewById(android.R.id.content);
        this.mDetailSettingsButton = (TextView) findViewById(android.R.id.button2);
        this.mDetailDoneButton = (TextView) findViewById(android.R.id.button1);
        this.mQsDetailHeader = findViewById(R.id.qs_detail_header);
        this.mQsDetailHeaderBack = this.mQsDetailHeader.findViewById(android.R.id.accessibilitySystemActionBack);
        this.mQsDetailHeaderTitle = (TextView) this.mQsDetailHeader.findViewById(android.R.id.title);
        this.mQsDetailHeaderSwitch = (Switch) this.mQsDetailHeader.findViewById(android.R.id.toggle);
        this.mQsDetailHeaderProgress = (ImageView) findViewById(R.id.qs_detail_header_progress);
        updateDetailText();
        this.mClipper = new QSDetailClipper(this);
        View.OnClickListener doneListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QSDetail.this.announceForAccessibility(QSDetail.this.mContext.getString(R.string.accessibility_desc_quick_settings));
                QSDetail.this.mQsPanel.closeDetail();
            }
        };
        this.mQsDetailHeaderBack.setOnClickListener(doneListener);
        this.mDetailDoneButton.setOnClickListener(doneListener);
    }

    public void setQsPanel(QSPanel panel, BaseStatusBarHeader header) {
        this.mQsPanel = panel;
        this.mHeader = header;
        this.mHeader.setCallback(this.mQsPanelCallback);
        this.mQsPanel.setCallback(this.mQsPanelCallback);
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
    }

    public boolean isShowingDetail() {
        return this.mDetailAdapter != null;
    }

    public void setFullyExpanded(boolean fullyExpanded) {
        this.mFullyExpanded = fullyExpanded;
    }

    public void setExpanded(boolean qsExpanded) {
        if (qsExpanded) {
            return;
        }
        this.mTriggeredExpand = false;
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(R.string.quick_settings_done);
        this.mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
    }

    public boolean isClosingDetail() {
        return this.mClosingDetail;
    }

    public void handleShowingDetail(final QSTile.DetailAdapter adapter, int x, int y) {
        Animator.AnimatorListener listener;
        boolean showingDetail = adapter != null;
        setClickable(showingDetail);
        if (showingDetail) {
            this.mQsDetailHeaderTitle.setText(adapter.getTitle());
            Boolean toggleState = adapter.getToggleState();
            if (toggleState == null) {
                this.mQsDetailHeaderSwitch.setVisibility(4);
                this.mQsDetailHeader.setClickable(false);
            } else {
                this.mQsDetailHeaderSwitch.setVisibility(0);
                this.mQsDetailHeaderSwitch.setChecked(toggleState.booleanValue());
                this.mQsDetailHeader.setClickable(true);
                this.mQsDetailHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean checked = !QSDetail.this.mQsDetailHeaderSwitch.isChecked();
                        QSDetail.this.mQsDetailHeaderSwitch.setChecked(checked);
                        adapter.setToggleState(checked);
                    }
                });
            }
            if (!this.mFullyExpanded) {
                this.mTriggeredExpand = true;
                this.mHost.animateToggleQSExpansion();
            } else {
                this.mTriggeredExpand = false;
            }
            this.mOpenX = x;
            this.mOpenY = y;
        } else {
            x = this.mOpenX;
            y = this.mOpenY;
            if (this.mTriggeredExpand) {
                this.mHost.animateToggleQSExpansion();
                this.mTriggeredExpand = false;
            }
        }
        boolean visibleDiff = (this.mDetailAdapter != null) != (adapter != null);
        if (visibleDiff || this.mDetailAdapter != adapter) {
            if (adapter != null) {
                int viewCacheIndex = adapter.getMetricsCategory();
                View detailView = adapter.createDetailView(this.mContext, this.mDetailViews.get(viewCacheIndex), this.mDetailContent);
                if (detailView == null) {
                    throw new IllegalStateException("Must return detail view");
                }
                final Intent settingsIntent = adapter.getSettingsIntent();
                this.mDetailSettingsButton.setVisibility(settingsIntent != null ? 0 : 8);
                this.mDetailSettingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QSDetail.this.mHost.startActivityDismissingKeyguard(settingsIntent);
                    }
                });
                this.mDetailContent.removeAllViews();
                this.mDetailContent.addView(detailView);
                this.mDetailViews.put(viewCacheIndex, detailView);
                MetricsLogger.visible(this.mContext, adapter.getMetricsCategory());
                announceForAccessibility(this.mContext.getString(R.string.accessibility_quick_settings_detail, adapter.getTitle()));
                this.mDetailAdapter = adapter;
                listener = this.mHideGridContentWhenDone;
                setVisibility(0);
            } else {
                if (this.mDetailAdapter != null) {
                    MetricsLogger.hidden(this.mContext, this.mDetailAdapter.getMetricsCategory());
                }
                this.mClosingDetail = true;
                this.mDetailAdapter = null;
                listener = this.mTeardownDetailWhenDone;
                this.mHeader.setVisibility(0);
                this.mQsPanel.setGridContentVisibility(true);
                this.mQsPanelCallback.onScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            if (!visibleDiff) {
                return;
            }
            if (this.mFullyExpanded || this.mDetailAdapter != null) {
                setAlpha(1.0f);
                this.mClipper.animateCircularClip(x, y, this.mDetailAdapter != null, listener);
            } else {
                animate().alpha(0.0f).setDuration(300L).setListener(listener).start();
            }
        }
    }

    public void handleToggleStateChanged(boolean state) {
        this.mQsDetailHeaderSwitch.setChecked(state);
    }

    public void handleScanStateChanged(boolean state) {
        if (this.mScanState == state) {
            return;
        }
        this.mScanState = state;
        Animatable anim = (Animatable) this.mQsDetailHeaderProgress.getDrawable();
        if (state) {
            this.mQsDetailHeaderProgress.animate().alpha(1.0f);
            anim.start();
        } else {
            this.mQsDetailHeaderProgress.animate().alpha(0.0f);
            anim.stop();
        }
    }
}
