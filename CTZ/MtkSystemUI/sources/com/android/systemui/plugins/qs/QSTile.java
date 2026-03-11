package com.android.systemui.plugins.qs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import com.android.systemui.plugins.annotations.Dependencies;
import com.android.systemui.plugins.annotations.DependsOn;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import java.util.Objects;
import java.util.function.Supplier;

@Dependencies({@DependsOn(target = QSIconView.class), @DependsOn(target = DetailAdapter.class), @DependsOn(target = Callback.class), @DependsOn(target = Icon.class), @DependsOn(target = State.class)})
@ProvidesInterface(version = 1)
public interface QSTile {
    public static final int VERSION = 1;

    @ProvidesInterface(version = 1)
    public interface Callback {
        public static final int VERSION = 1;

        void onAnnouncementRequested(CharSequence charSequence);

        void onScanStateChanged(boolean z);

        void onShowDetail(boolean z);

        void onStateChanged(State state);

        void onToggleStateChanged(boolean z);
    }

    void addCallback(Callback callback);

    void clearState();

    void click();

    QSIconView createTileView(Context context);

    void destroy();

    DetailAdapter getDetailAdapter();

    int getMetricsCategory();

    State getState();

    CharSequence getTileLabel();

    String getTileSpec();

    boolean isAvailable();

    void longClick();

    void refreshState();

    void removeCallback(Callback callback);

    void removeCallbacks();

    void secondaryClick();

    void setDetailListening(boolean z);

    void setListening(Object obj, boolean z);

    void setTileSpec(String str);

    void userSwitch(int i);

    default LogMaker populate(LogMaker logMaker) {
        return logMaker;
    }

    @ProvidesInterface(version = 1)
    public static abstract class Icon {
        public static final int VERSION = 1;

        public abstract Drawable getDrawable(Context context);

        public Drawable getInvisibleDrawable(Context context) {
            return getDrawable(context);
        }

        public int hashCode() {
            return Icon.class.hashCode();
        }

        public int getPadding() {
            return 0;
        }
    }

    @ProvidesInterface(version = 1)
    public static class State {
        public static final int VERSION = 1;
        public CharSequence contentDescription;
        public boolean disabledByPolicy;
        public CharSequence dualLabelContentDescription;
        public String expandedAccessibilityClassName;
        public Icon icon;
        public Supplier<Icon> iconSupplier;
        public CharSequence label;
        public CharSequence secondaryLabel;
        public SlashState slash;
        public int state = 2;
        public boolean dualTarget = false;
        public boolean isTransient = false;

        public boolean copyTo(State state) {
            if (state == null) {
                throw new IllegalArgumentException();
            }
            if (!state.getClass().equals(getClass())) {
                throw new IllegalArgumentException();
            }
            boolean z = (Objects.equals(state.icon, this.icon) && Objects.equals(state.iconSupplier, this.iconSupplier) && Objects.equals(state.label, this.label) && Objects.equals(state.secondaryLabel, this.secondaryLabel) && Objects.equals(state.contentDescription, this.contentDescription) && Objects.equals(state.dualLabelContentDescription, this.dualLabelContentDescription) && Objects.equals(state.expandedAccessibilityClassName, this.expandedAccessibilityClassName) && Objects.equals(Boolean.valueOf(state.disabledByPolicy), Boolean.valueOf(this.disabledByPolicy)) && Objects.equals(Integer.valueOf(state.state), Integer.valueOf(this.state)) && Objects.equals(Boolean.valueOf(state.isTransient), Boolean.valueOf(this.isTransient)) && Objects.equals(Boolean.valueOf(state.dualTarget), Boolean.valueOf(this.dualTarget)) && Objects.equals(state.slash, this.slash)) ? false : true;
            state.icon = this.icon;
            state.iconSupplier = this.iconSupplier;
            state.label = this.label;
            state.secondaryLabel = this.secondaryLabel;
            state.contentDescription = this.contentDescription;
            state.dualLabelContentDescription = this.dualLabelContentDescription;
            state.expandedAccessibilityClassName = this.expandedAccessibilityClassName;
            state.disabledByPolicy = this.disabledByPolicy;
            state.state = this.state;
            state.dualTarget = this.dualTarget;
            state.isTransient = this.isTransient;
            state.slash = this.slash != null ? this.slash.copy() : null;
            return z;
        }

        public String toString() {
            return toStringBuilder().toString();
        }

        protected StringBuilder toStringBuilder() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append('[');
            sb.append(",icon=");
            sb.append(this.icon);
            sb.append(",iconSupplier=");
            sb.append(this.iconSupplier);
            sb.append(",label=");
            sb.append(this.label);
            sb.append(",secondaryLabel=");
            sb.append(this.secondaryLabel);
            sb.append(",contentDescription=");
            sb.append(this.contentDescription);
            sb.append(",dualLabelContentDescription=");
            sb.append(this.dualLabelContentDescription);
            sb.append(",expandedAccessibilityClassName=");
            sb.append(this.expandedAccessibilityClassName);
            sb.append(",disabledByPolicy=");
            sb.append(this.disabledByPolicy);
            sb.append(",dualTarget=");
            sb.append(this.dualTarget);
            sb.append(",isTransient=");
            sb.append(this.isTransient);
            sb.append(",state=");
            sb.append(this.state);
            sb.append(",slash=\"");
            sb.append(this.slash);
            sb.append("\"");
            sb.append(']');
            return sb;
        }

        public State copy() {
            State state = new State();
            copyTo(state);
            return state;
        }
    }

    @ProvidesInterface(version = 1)
    public static class BooleanState extends State {
        public static final int VERSION = 1;
        public boolean value;

        @Override
        public boolean copyTo(State state) {
            BooleanState booleanState = (BooleanState) state;
            boolean z = super.copyTo(state) || booleanState.value != this.value;
            booleanState.value = this.value;
            return z;
        }

        @Override
        protected StringBuilder toStringBuilder() {
            StringBuilder stringBuilder = super.toStringBuilder();
            stringBuilder.insert(stringBuilder.length() - 1, ",value=" + this.value);
            return stringBuilder;
        }

        @Override
        public State copy() {
            BooleanState booleanState = new BooleanState();
            copyTo(booleanState);
            return booleanState;
        }
    }

    @ProvidesInterface(version = 1)
    public static final class SignalState extends BooleanState {
        public static final int VERSION = 1;
        public boolean activityIn;
        public boolean activityOut;
        public boolean isOverlayIconWide;
        public int overlayIconId;

        @Override
        public boolean copyTo(State state) {
            SignalState signalState = (SignalState) state;
            boolean z = (signalState.activityIn == this.activityIn && signalState.activityOut == this.activityOut && signalState.isOverlayIconWide == this.isOverlayIconWide && signalState.overlayIconId == this.overlayIconId) ? false : true;
            signalState.activityIn = this.activityIn;
            signalState.activityOut = this.activityOut;
            signalState.isOverlayIconWide = this.isOverlayIconWide;
            signalState.overlayIconId = this.overlayIconId;
            return super.copyTo(state) || z;
        }

        @Override
        protected StringBuilder toStringBuilder() {
            StringBuilder stringBuilder = super.toStringBuilder();
            stringBuilder.insert(stringBuilder.length() - 1, ",activityIn=" + this.activityIn);
            stringBuilder.insert(stringBuilder.length() + (-1), ",activityOut=" + this.activityOut);
            return stringBuilder;
        }

        @Override
        public State copy() {
            SignalState signalState = new SignalState();
            copyTo(signalState);
            return signalState;
        }
    }

    @ProvidesInterface(version = 1)
    public static class AirplaneBooleanState extends BooleanState {
        public static final int VERSION = 1;
        public boolean isAirplaneMode;

        @Override
        public boolean copyTo(State state) {
            AirplaneBooleanState airplaneBooleanState = (AirplaneBooleanState) state;
            boolean z = super.copyTo(state) || airplaneBooleanState.isAirplaneMode != this.isAirplaneMode;
            airplaneBooleanState.isAirplaneMode = this.isAirplaneMode;
            return z;
        }

        @Override
        public State copy() {
            AirplaneBooleanState airplaneBooleanState = new AirplaneBooleanState();
            copyTo(airplaneBooleanState);
            return airplaneBooleanState;
        }
    }

    @ProvidesInterface(version = 2)
    public static class SlashState {
        public static final int VERSION = 2;
        public boolean isSlashed;
        public float rotation;

        public String toString() {
            return "isSlashed=" + this.isSlashed + ",rotation=" + this.rotation;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            try {
                if (((SlashState) obj).rotation == this.rotation) {
                    return ((SlashState) obj).isSlashed == this.isSlashed;
                }
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        public SlashState copy() {
            SlashState slashState = new SlashState();
            slashState.rotation = this.rotation;
            slashState.isSlashed = this.isSlashed;
            return slashState;
        }
    }
}
