package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.DrawableContainer;
import android.util.AttributeSet;
import android.util.StateSet;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class StateListDrawable extends DrawableContainer {
    private static final boolean DEBUG = false;
    private static final String TAG = "StateListDrawable";
    private boolean mMutated;
    private StateListState mStateListState;

    StateListDrawable(StateListState state, Resources res, StateListDrawable stateListDrawable) {
        this(state, res);
    }

    public StateListDrawable() {
        this(null, null);
    }

    public void addState(int[] stateSet, Drawable drawable) {
        if (drawable == null) {
            return;
        }
        this.mStateListState.addStateSet(stateSet, drawable);
        onStateChange(getState());
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean changed = super.onStateChange(stateSet);
        int idx = this.mStateListState.indexOfStateSet(stateSet);
        if (idx < 0) {
            idx = this.mStateListState.indexOfStateSet(StateSet.WILD_CARD);
        }
        if (selectDrawable(idx)) {
            return true;
        }
        return changed;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.StateListDrawable);
        super.inflateWithAttributes(r, parser, a, 1);
        updateStateFromTypedArray(a);
        updateDensity(r);
        a.recycle();
        inflateChildElements(r, parser, attrs, theme);
        onStateChange(getState());
    }

    private void updateStateFromTypedArray(TypedArray a) {
        StateListState state = this.mStateListState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mVariablePadding = a.getBoolean(2, state.mVariablePadding);
        state.mConstantSize = a.getBoolean(3, state.mConstantSize);
        state.mEnterFadeDuration = a.getInt(4, state.mEnterFadeDuration);
        state.mExitFadeDuration = a.getInt(5, state.mExitFadeDuration);
        state.mDither = a.getBoolean(0, state.mDither);
        state.mAutoMirrored = a.getBoolean(6, state.mAutoMirrored);
    }

    private void inflateChildElements(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        int type;
        StateListState state = this.mStateListState;
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int type2 = parser.next();
            if (type2 == 1) {
                return;
            }
            int depth = parser.getDepth();
            if (depth < innerDepth && type2 == 3) {
                return;
            }
            if (type2 == 2 && depth <= innerDepth && parser.getName().equals("item")) {
                TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.StateListDrawableItem);
                Drawable dr = a.getDrawable(0);
                a.recycle();
                int[] states = extractStateSet(attrs);
                if (dr == null) {
                    do {
                        type = parser.next();
                    } while (type == 4);
                    if (type != 2) {
                        throw new XmlPullParserException(parser.getPositionDescription() + ": <item> tag requires a 'drawable' attribute or child tag defining a drawable");
                    }
                    dr = Drawable.createFromXmlInner(r, parser, attrs, theme);
                }
                state.addStateSet(states, dr);
            }
        }
    }

    int[] extractStateSet(AttributeSet attrs) {
        int j;
        int numAttrs = attrs.getAttributeCount();
        int[] states = new int[numAttrs];
        int i = 0;
        int j2 = 0;
        while (i < numAttrs) {
            int stateResId = attrs.getAttributeNameResource(i);
            switch (stateResId) {
                case 0:
                    j = j2;
                    break;
                case android.R.attr.id:
                case android.R.attr.drawable:
                    j = j2;
                    break;
                default:
                    j = j2 + 1;
                    if (!attrs.getAttributeBooleanValue(i, false)) {
                        stateResId = -stateResId;
                    }
                    states[j2] = stateResId;
                    break;
            }
            i++;
            j2 = j;
        }
        return StateSet.trimStateSet(states, j2);
    }

    StateListState getStateListState() {
        return this.mStateListState;
    }

    public int getStateCount() {
        return this.mStateListState.getChildCount();
    }

    public int[] getStateSet(int index) {
        return this.mStateListState.mStateSets[index];
    }

    public Drawable getStateDrawable(int index) {
        return this.mStateListState.getChild(index);
    }

    public int getStateDrawableIndex(int[] stateSet) {
        return this.mStateListState.indexOfStateSet(stateSet);
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mStateListState.mutate();
            this.mMutated = true;
        }
        return this;
    }

    @Override
    StateListState cloneConstantState() {
        return new StateListState(this.mStateListState, this, null);
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mMutated = false;
    }

    static class StateListState extends DrawableContainer.DrawableContainerState {
        int[][] mStateSets;
        int[] mThemeAttrs;

        StateListState(StateListState orig, StateListDrawable owner, Resources res) {
            super(orig, owner, res);
            if (orig != null) {
                this.mThemeAttrs = orig.mThemeAttrs;
                this.mStateSets = orig.mStateSets;
            } else {
                this.mThemeAttrs = null;
                this.mStateSets = new int[getCapacity()][];
            }
        }

        void mutate() {
            this.mThemeAttrs = this.mThemeAttrs != null ? (int[]) this.mThemeAttrs.clone() : null;
            int[][] stateSets = new int[this.mStateSets.length][];
            for (int i = this.mStateSets.length - 1; i >= 0; i--) {
                stateSets[i] = this.mStateSets[i] != null ? (int[]) this.mStateSets[i].clone() : null;
            }
            this.mStateSets = stateSets;
        }

        int addStateSet(int[] stateSet, Drawable drawable) {
            int pos = addChild(drawable);
            this.mStateSets[pos] = stateSet;
            return pos;
        }

        int indexOfStateSet(int[] stateSet) {
            int[][] stateSets = this.mStateSets;
            int N = getChildCount();
            for (int i = 0; i < N; i++) {
                if (StateSet.stateSetMatches(stateSets[i], stateSet)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public Drawable newDrawable() {
            return new StateListDrawable(this, null, 0 == true ? 1 : 0);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new StateListDrawable(this, res, null);
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs == null) {
                return super.canApplyTheme();
            }
            return true;
        }

        @Override
        public void growArray(int oldSize, int newSize) {
            super.growArray(oldSize, newSize);
            int[][] newStateSets = new int[newSize][];
            System.arraycopy(this.mStateSets, 0, newStateSets, 0, oldSize);
            this.mStateSets = newStateSets;
        }
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        onStateChange(getState());
    }

    @Override
    protected void setConstantState(DrawableContainer.DrawableContainerState state) {
        super.setConstantState(state);
        if (!(state instanceof StateListState)) {
            return;
        }
        this.mStateListState = (StateListState) state;
    }

    private StateListDrawable(StateListState state, Resources res) {
        StateListState newState = new StateListState(state, this, res);
        setConstantState(newState);
        onStateChange(getState());
    }

    StateListDrawable(StateListState state) {
        if (state == null) {
            return;
        }
        setConstantState(state);
    }
}
