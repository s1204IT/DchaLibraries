package android.telecom;

import android.util.ArrayMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Phone {
    private AudioState mAudioState;
    private final InCallAdapter mInCallAdapter;
    private final Map<String, Call> mCallByTelecomCallId = new ArrayMap();
    private final List<Call> mCalls = new CopyOnWriteArrayList();
    private final List<Call> mUnmodifiableCalls = Collections.unmodifiableList(this.mCalls);
    private final List<Listener> mListeners = new CopyOnWriteArrayList();
    private boolean mCanAddCall = true;

    public static abstract class Listener {
        public void onAudioStateChanged(Phone phone, AudioState audioState) {
        }

        public void onBringToForeground(Phone phone, boolean showDialpad) {
        }

        public void onCallAdded(Phone phone, Call call) {
        }

        public void onCallRemoved(Phone phone, Call call) {
        }

        public void onCanAddCallChanged(Phone phone, boolean canAddCall) {
        }
    }

    Phone(InCallAdapter adapter) {
        this.mInCallAdapter = adapter;
    }

    final void internalAddCall(ParcelableCall parcelableCall) {
        Call call = new Call(this, parcelableCall.getId(), this.mInCallAdapter);
        this.mCallByTelecomCallId.put(parcelableCall.getId(), call);
        this.mCalls.add(call);
        checkCallTree(parcelableCall);
        call.internalUpdate(parcelableCall, this.mCallByTelecomCallId);
        fireCallAdded(call);
    }

    final void internalRemoveCall(Call call) {
        this.mCallByTelecomCallId.remove(call.internalGetCallId());
        this.mCalls.remove(call);
        fireCallRemoved(call);
    }

    final void internalUpdateCall(ParcelableCall parcelableCall) {
        Call call = this.mCallByTelecomCallId.get(parcelableCall.getId());
        if (call != null) {
            checkCallTree(parcelableCall);
            call.internalUpdate(parcelableCall, this.mCallByTelecomCallId);
        }
    }

    final void internalSetPostDialWait(String telecomId, String remaining) {
        Call call = this.mCallByTelecomCallId.get(telecomId);
        if (call != null) {
            call.internalSetPostDialWait(remaining);
        }
    }

    final void internalAudioStateChanged(AudioState audioState) {
        if (!Objects.equals(this.mAudioState, audioState)) {
            this.mAudioState = audioState;
            fireAudioStateChanged(audioState);
        }
    }

    final Call internalGetCallByTelecomId(String telecomId) {
        return this.mCallByTelecomCallId.get(telecomId);
    }

    final void internalBringToForeground(boolean showDialpad) {
        fireBringToForeground(showDialpad);
    }

    final void internalSetCanAddCall(boolean canAddCall) {
        if (this.mCanAddCall != canAddCall) {
            this.mCanAddCall = canAddCall;
            fireCanAddCallChanged(canAddCall);
        }
    }

    final void destroy() {
        for (Call call : this.mCalls) {
            if (call.getState() != 7) {
                call.internalSetDisconnected();
            }
        }
    }

    public final void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(Listener listener) {
        if (listener != null) {
            this.mListeners.remove(listener);
        }
    }

    public final List<Call> getCalls() {
        return this.mUnmodifiableCalls;
    }

    public final boolean canAddCall() {
        return this.mCanAddCall;
    }

    public final void setMuted(boolean state) {
        this.mInCallAdapter.mute(state);
    }

    public final boolean extraVolume(boolean state) {
        return this.mInCallAdapter.extraVolume(state);
    }

    public final boolean record() {
        return this.mInCallAdapter.record();
    }

    public final void setAudioRoute(int route) {
        this.mInCallAdapter.setAudioRoute(route);
    }

    public final void setProximitySensorOn() {
        this.mInCallAdapter.turnProximitySensorOn();
    }

    public final void setProximitySensorOff(boolean screenOnImmediately) {
        this.mInCallAdapter.turnProximitySensorOff(screenOnImmediately);
    }

    public final AudioState getAudioState() {
        return this.mAudioState;
    }

    private void fireCallAdded(Call call) {
        for (Listener listener : this.mListeners) {
            listener.onCallAdded(this, call);
        }
    }

    private void fireCallRemoved(Call call) {
        for (Listener listener : this.mListeners) {
            listener.onCallRemoved(this, call);
        }
    }

    private void fireAudioStateChanged(AudioState audioState) {
        for (Listener listener : this.mListeners) {
            listener.onAudioStateChanged(this, audioState);
        }
    }

    private void fireBringToForeground(boolean showDialpad) {
        for (Listener listener : this.mListeners) {
            listener.onBringToForeground(this, showDialpad);
        }
    }

    private void fireCanAddCallChanged(boolean canAddCall) {
        for (Listener listener : this.mListeners) {
            listener.onCanAddCallChanged(this, canAddCall);
        }
    }

    private void checkCallTree(ParcelableCall parcelableCall) {
        if (parcelableCall.getParentCallId() != null && !this.mCallByTelecomCallId.containsKey(parcelableCall.getParentCallId())) {
            Log.wtf(this, "ParcelableCall %s has nonexistent parent %s", parcelableCall.getId(), parcelableCall.getParentCallId());
        }
        if (parcelableCall.getChildCallIds() != null) {
            for (int i = 0; i < parcelableCall.getChildCallIds().size(); i++) {
                if (!this.mCallByTelecomCallId.containsKey(parcelableCall.getChildCallIds().get(i))) {
                    Log.wtf(this, "ParcelableCall %s has nonexistent child %s", parcelableCall.getId(), parcelableCall.getChildCallIds().get(i));
                }
            }
        }
    }
}
