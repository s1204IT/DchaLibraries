package com.android.settings.notification;

import android.animation.LayoutTransition;
import android.app.INotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class ZenModeConditionSelection extends RadioGroup {
    private Condition mCondition;
    private final List<Condition> mConditions;
    private final Context mContext;
    private final H mHandler;
    private final IConditionListener mListener;
    private final INotificationManager mNoMan;

    public ZenModeConditionSelection(Context context) {
        super(context);
        this.mHandler = new H();
        this.mListener = new IConditionListener.Stub() {
            public void onConditionsReceived(Condition[] conditions) {
                if (conditions != null && conditions.length != 0) {
                    ZenModeConditionSelection.this.mHandler.obtainMessage(1, conditions).sendToTarget();
                }
            }
        };
        this.mContext = context;
        this.mConditions = new ArrayList();
        setLayoutTransition(new LayoutTransition());
        int p = this.mContext.getResources().getDimensionPixelSize(R.dimen.content_margin_left);
        setPadding(p, p, p, 0);
        this.mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        RadioButton b = newRadioButton(null);
        b.setText(this.mContext.getString(android.R.string.network_partial_connectivity_detailed));
        b.setChecked(true);
        for (int i = ZenModeConfig.MINUTE_BUCKETS.length - 1; i >= 0; i--) {
            handleCondition(ZenModeConfig.toTimeCondition(this.mContext, ZenModeConfig.MINUTE_BUCKETS[i], UserHandle.myUserId()));
        }
    }

    private RadioButton newRadioButton(Condition condition) {
        final RadioButton button = new RadioButton(this.mContext);
        button.setTag(condition);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ZenModeConditionSelection.this.setCondition((Condition) button.getTag());
                }
            }
        });
        addView(button);
        return button;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestZenModeConditions(1);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        requestZenModeConditions(0);
    }

    protected void requestZenModeConditions(int relevance) {
        Log.d("ZenModeConditionSelection", "requestZenModeConditions " + Condition.relevanceToString(relevance));
        try {
            this.mNoMan.requestZenModeConditions(this.mListener, relevance);
        } catch (RemoteException e) {
        }
    }

    protected void handleConditions(Condition[] conditions) {
        for (Condition c : conditions) {
            handleCondition(c);
        }
    }

    protected void handleCondition(Condition c) {
        if (!this.mConditions.contains(c)) {
            RadioButton v = (RadioButton) findViewWithTag(c.id);
            if ((c.state == 1 || c.state == 2) && v == null) {
                v = newRadioButton(c);
            }
            if (v != null) {
                v.setText(!TextUtils.isEmpty(c.line1) ? c.line1 : c.summary);
                v.setEnabled(c.state == 1);
            }
            this.mConditions.add(c);
        }
    }

    protected void setCondition(Condition c) {
        Log.d("ZenModeConditionSelection", "setCondition " + c);
        this.mCondition = c;
    }

    public void confirmCondition() {
        Log.d("ZenModeConditionSelection", "confirmCondition " + this.mCondition);
        try {
            this.mNoMan.setZenModeCondition(this.mCondition);
        } catch (RemoteException e) {
        }
    }

    private final class H extends Handler {
        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ZenModeConditionSelection.this.handleConditions((Condition[]) msg.obj);
            }
        }
    }
}
