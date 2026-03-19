package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Map;

public class ChangeText extends Transition {
    public static final int CHANGE_BEHAVIOR_IN = 2;
    public static final int CHANGE_BEHAVIOR_KEEP = 0;
    public static final int CHANGE_BEHAVIOR_OUT = 1;
    public static final int CHANGE_BEHAVIOR_OUT_IN = 3;
    private static final String LOG_TAG = "TextChange";
    private static final String PROPNAME_TEXT_COLOR = "android:textchange:textColor";
    private int mChangeBehavior = 0;
    private static final String PROPNAME_TEXT = "android:textchange:text";
    private static final String PROPNAME_TEXT_SELECTION_START = "android:textchange:textSelectionStart";
    private static final String PROPNAME_TEXT_SELECTION_END = "android:textchange:textSelectionEnd";
    private static final String[] sTransitionProperties = {PROPNAME_TEXT, PROPNAME_TEXT_SELECTION_START, PROPNAME_TEXT_SELECTION_END};

    public ChangeText setChangeBehavior(int i) {
        if (i >= 0 && i <= 3) {
            this.mChangeBehavior = i;
        }
        return this;
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public int getChangeBehavior() {
        return this.mChangeBehavior;
    }

    private void captureValues(TransitionValues transitionValues) {
        if (transitionValues.view instanceof TextView) {
            TextView textView = (TextView) transitionValues.view;
            transitionValues.values.put(PROPNAME_TEXT, textView.getText());
            if (textView instanceof EditText) {
                transitionValues.values.put(PROPNAME_TEXT_SELECTION_START, Integer.valueOf(textView.getSelectionStart()));
                transitionValues.values.put(PROPNAME_TEXT_SELECTION_END, Integer.valueOf(textView.getSelectionEnd()));
            }
            if (this.mChangeBehavior > 0) {
                transitionValues.values.put(PROPNAME_TEXT_COLOR, Integer.valueOf(textView.getCurrentTextColor()));
            }
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public Animator createAnimator(ViewGroup viewGroup, TransitionValues transitionValues, TransitionValues transitionValues2) {
        int i;
        int i2;
        int i3;
        int iIntValue;
        int i4;
        char c;
        int i5;
        int i6;
        final int i7;
        ValueAnimator valueAnimator;
        int i8;
        final int i9;
        Animator animator;
        final int i10;
        if (transitionValues == null || transitionValues2 == null || !(transitionValues.view instanceof TextView) || !(transitionValues2.view instanceof TextView)) {
            return null;
        }
        final TextView textView = (TextView) transitionValues2.view;
        Map<String, Object> map = transitionValues.values;
        Map<String, Object> map2 = transitionValues2.values;
        final CharSequence charSequence = map.get(PROPNAME_TEXT) != null ? (CharSequence) map.get(PROPNAME_TEXT) : "";
        final CharSequence charSequence2 = map2.get(PROPNAME_TEXT) != null ? (CharSequence) map2.get(PROPNAME_TEXT) : "";
        boolean z = textView instanceof EditText;
        if (z) {
            int iIntValue2 = map.get(PROPNAME_TEXT_SELECTION_START) != null ? ((Integer) map.get(PROPNAME_TEXT_SELECTION_START)).intValue() : -1;
            int iIntValue3 = map.get(PROPNAME_TEXT_SELECTION_END) != null ? ((Integer) map.get(PROPNAME_TEXT_SELECTION_END)).intValue() : iIntValue2;
            int iIntValue4 = map2.get(PROPNAME_TEXT_SELECTION_START) != null ? ((Integer) map2.get(PROPNAME_TEXT_SELECTION_START)).intValue() : -1;
            i3 = iIntValue4;
            i = iIntValue2;
            i2 = iIntValue3;
            iIntValue = map2.get(PROPNAME_TEXT_SELECTION_END) != null ? ((Integer) map2.get(PROPNAME_TEXT_SELECTION_END)).intValue() : iIntValue4;
        } else {
            i = -1;
            i2 = -1;
            i3 = -1;
            iIntValue = -1;
        }
        if (charSequence.equals(charSequence2)) {
            return null;
        }
        if (this.mChangeBehavior != 2) {
            textView.setText(charSequence);
            if (z) {
                setSelection((EditText) textView, i, i2);
            }
        }
        if (this.mChangeBehavior == 0) {
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            final int i11 = i3;
            final int i12 = iIntValue;
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator2) {
                    if (charSequence.equals(textView.getText())) {
                        textView.setText(charSequence2);
                        if (textView instanceof EditText) {
                            ChangeText.this.setSelection((EditText) textView, i11, i12);
                        }
                    }
                }
            });
            animator = valueAnimatorOfFloat;
            i5 = i;
            i4 = i2;
            i10 = 0;
        } else {
            final int iIntValue5 = ((Integer) map.get(PROPNAME_TEXT_COLOR)).intValue();
            int iIntValue6 = ((Integer) map2.get(PROPNAME_TEXT_COLOR)).intValue();
            if (this.mChangeBehavior == 3 || this.mChangeBehavior == 1) {
                ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(Color.alpha(iIntValue5), 0);
                valueAnimatorOfInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                        textView.setTextColor((((Integer) valueAnimator2.getAnimatedValue()).intValue() << 24) | (iIntValue5 & 16777215));
                    }
                });
                i4 = i2;
                c = 1;
                i5 = i;
                i6 = 3;
                final int i13 = i3;
                i7 = iIntValue6;
                final int i14 = iIntValue;
                valueAnimatorOfInt.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator2) {
                        if (charSequence.equals(textView.getText())) {
                            textView.setText(charSequence2);
                            if (textView instanceof EditText) {
                                ChangeText.this.setSelection((EditText) textView, i13, i14);
                            }
                        }
                        textView.setTextColor(i7);
                    }
                });
                valueAnimator = valueAnimatorOfInt;
            } else {
                c = 1;
                i7 = iIntValue6;
                i5 = i;
                i4 = i2;
                valueAnimator = null;
                i6 = 3;
            }
            if (this.mChangeBehavior != i6) {
                i8 = 2;
                if (this.mChangeBehavior != 2) {
                    i9 = i7;
                    animator = null;
                }
                if (valueAnimator == null && animator != null) {
                    AnimatorSet animatorSet = new AnimatorSet();
                    Animator[] animatorArr = new Animator[i8];
                    animatorArr[0] = valueAnimator;
                    animatorArr[c] = animator;
                    animatorSet.playSequentially(animatorArr);
                    animator = animatorSet;
                } else if (valueAnimator == null) {
                    i10 = i9;
                    animator = valueAnimator;
                }
                i10 = i9;
            } else {
                i8 = 2;
            }
            int[] iArr = new int[i8];
            iArr[0] = 0;
            i9 = i7;
            iArr[c] = Color.alpha(i9);
            ValueAnimator valueAnimatorOfInt2 = ValueAnimator.ofInt(iArr);
            valueAnimatorOfInt2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    textView.setTextColor((((Integer) valueAnimator2.getAnimatedValue()).intValue() << 24) | (i9 & 16777215));
                }
            });
            valueAnimatorOfInt2.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animator2) {
                    textView.setTextColor(i9);
                }
            });
            animator = valueAnimatorOfInt2;
            if (valueAnimator == null) {
                if (valueAnimator == null) {
                    i10 = i9;
                }
            }
        }
        final int i15 = i3;
        final int i16 = iIntValue;
        final int i17 = i5;
        final int i18 = i4;
        addListener(new TransitionListenerAdapter() {
            int mPausedColor = 0;

            @Override
            public void onTransitionPause(Transition transition) {
                if (ChangeText.this.mChangeBehavior != 2) {
                    textView.setText(charSequence2);
                    if (textView instanceof EditText) {
                        ChangeText.this.setSelection((EditText) textView, i15, i16);
                    }
                }
                if (ChangeText.this.mChangeBehavior > 0) {
                    this.mPausedColor = textView.getCurrentTextColor();
                    textView.setTextColor(i10);
                }
            }

            @Override
            public void onTransitionResume(Transition transition) {
                if (ChangeText.this.mChangeBehavior != 2) {
                    textView.setText(charSequence);
                    if (textView instanceof EditText) {
                        ChangeText.this.setSelection((EditText) textView, i17, i18);
                    }
                }
                if (ChangeText.this.mChangeBehavior > 0) {
                    textView.setTextColor(this.mPausedColor);
                }
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                transition.removeListener(this);
            }
        });
        return animator;
    }

    private void setSelection(EditText editText, int i, int i2) {
        if (i >= 0 && i2 >= 0) {
            editText.setSelection(i, i2);
        }
    }
}
