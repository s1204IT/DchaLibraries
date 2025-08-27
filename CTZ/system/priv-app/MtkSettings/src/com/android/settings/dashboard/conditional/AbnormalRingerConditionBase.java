package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import com.android.settings.R;

/* loaded from: classes.dex */
public abstract class AbnormalRingerConditionBase extends Condition {
    protected final AudioManager mAudioManager;
    private final IntentFilter mFilter;
    private final RingerModeChangeReceiver mReceiver;

    AbnormalRingerConditionBase(ConditionManager conditionManager) {
        super(conditionManager);
        this.mAudioManager = (AudioManager) this.mManager.getContext().getSystemService("audio");
        this.mReceiver = new RingerModeChangeReceiver(this);
        this.mFilter = new IntentFilter("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        conditionManager.getContext().registerReceiver(this.mReceiver, this.mFilter);
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public CharSequence[] getActions() {
        return new CharSequence[]{this.mManager.getContext().getText(R.string.condition_device_muted_action_turn_on_sound)};
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onPrimaryClick() {
        this.mManager.getContext().startActivity(new Intent("android.settings.SOUND_SETTINGS").addFlags(268435456));
    }

    @Override // com.android.settings.dashboard.conditional.Condition
    public void onActionClick(int i) {
        this.mAudioManager.setRingerModeInternal(2);
        this.mAudioManager.setStreamVolume(2, 1, 0);
        refreshState();
    }

    static class RingerModeChangeReceiver extends BroadcastReceiver {
        private final AbnormalRingerConditionBase mCondition;

        public RingerModeChangeReceiver(AbnormalRingerConditionBase abnormalRingerConditionBase) {
            this.mCondition = abnormalRingerConditionBase;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(intent.getAction())) {
                this.mCondition.refreshState();
            }
        }
    }
}
