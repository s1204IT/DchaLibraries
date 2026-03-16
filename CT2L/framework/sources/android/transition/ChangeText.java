package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.net.ProxyInfo;
import android.text.Spanned;
import android.transition.Transition;
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

    public ChangeText setChangeBehavior(int changeBehavior) {
        if (changeBehavior >= 0 && changeBehavior <= 3) {
            this.mChangeBehavior = changeBehavior;
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
            TextView textview = (TextView) transitionValues.view;
            transitionValues.values.put(PROPNAME_TEXT, textview.getText());
            if (textview instanceof EditText) {
                transitionValues.values.put(PROPNAME_TEXT_SELECTION_START, Integer.valueOf(textview.getSelectionStart()));
                transitionValues.values.put(PROPNAME_TEXT_SELECTION_END, Integer.valueOf(textview.getSelectionEnd()));
            }
            if (this.mChangeBehavior > 0) {
                transitionValues.values.put(PROPNAME_TEXT_COLOR, Integer.valueOf(textview.getCurrentTextColor()));
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
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        final int endSelectionEnd;
        final int endSelectionStart;
        final int startSelectionEnd;
        final int startSelectionStart;
        final int endColor;
        Animator anim;
        if (startValues == null || endValues == null || !(startValues.view instanceof TextView) || !(endValues.view instanceof TextView)) {
            return null;
        }
        final TextView view = (TextView) endValues.view;
        Map<String, Object> startVals = startValues.values;
        Map<String, Object> endVals = endValues.values;
        final CharSequence startText = startVals.get(PROPNAME_TEXT) != null ? (CharSequence) startVals.get(PROPNAME_TEXT) : ProxyInfo.LOCAL_EXCL_LIST;
        final CharSequence endText = endVals.get(PROPNAME_TEXT) != null ? (CharSequence) endVals.get(PROPNAME_TEXT) : ProxyInfo.LOCAL_EXCL_LIST;
        if (view instanceof EditText) {
            startSelectionStart = startVals.get(PROPNAME_TEXT_SELECTION_START) != null ? ((Integer) startVals.get(PROPNAME_TEXT_SELECTION_START)).intValue() : -1;
            startSelectionEnd = startVals.get(PROPNAME_TEXT_SELECTION_END) != null ? ((Integer) startVals.get(PROPNAME_TEXT_SELECTION_END)).intValue() : startSelectionStart;
            endSelectionStart = endVals.get(PROPNAME_TEXT_SELECTION_START) != null ? ((Integer) endVals.get(PROPNAME_TEXT_SELECTION_START)).intValue() : -1;
            endSelectionEnd = endVals.get(PROPNAME_TEXT_SELECTION_END) != null ? ((Integer) endVals.get(PROPNAME_TEXT_SELECTION_END)).intValue() : endSelectionStart;
        } else {
            endSelectionEnd = -1;
            endSelectionStart = -1;
            startSelectionEnd = -1;
            startSelectionStart = -1;
        }
        if (!startText.equals(endText)) {
            if (this.mChangeBehavior != 2) {
                view.setText(startText);
                if (view instanceof EditText) {
                    setSelection((EditText) view, startSelectionStart, startSelectionEnd);
                }
            }
            if (this.mChangeBehavior == 0) {
                endColor = 0;
                anim = ValueAnimator.ofFloat(0.0f, 1.0f);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (startText.equals(view.getText())) {
                            view.setText(endText);
                            if (view instanceof EditText) {
                                ChangeText.this.setSelection((EditText) view, endSelectionStart, endSelectionEnd);
                            }
                        }
                    }
                });
            } else {
                final int startColor = ((Integer) startVals.get(PROPNAME_TEXT_COLOR)).intValue();
                endColor = ((Integer) endVals.get(PROPNAME_TEXT_COLOR)).intValue();
                ValueAnimator outAnim = null;
                ValueAnimator inAnim = null;
                if (this.mChangeBehavior == 3 || this.mChangeBehavior == 1) {
                    outAnim = ValueAnimator.ofInt(255, 0);
                    outAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            int currAlpha = ((Integer) animation.getAnimatedValue()).intValue();
                            view.setTextColor((currAlpha << 24) | (startColor & Spanned.SPAN_PRIORITY) | (startColor & 65280) | (startColor & 255));
                        }
                    });
                    outAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (startText.equals(view.getText())) {
                                view.setText(endText);
                                if (view instanceof EditText) {
                                    ChangeText.this.setSelection((EditText) view, endSelectionStart, endSelectionEnd);
                                }
                            }
                            view.setTextColor(endColor);
                        }
                    });
                }
                if (this.mChangeBehavior == 3 || this.mChangeBehavior == 2) {
                    inAnim = ValueAnimator.ofInt(0, 255);
                    inAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            int currAlpha = ((Integer) animation.getAnimatedValue()).intValue();
                            view.setTextColor((currAlpha << 24) | (Color.red(endColor) << 16) | (Color.green(endColor) << 8) | Color.red(endColor));
                        }
                    });
                    inAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            view.setTextColor(endColor);
                        }
                    });
                }
                if (outAnim != null && inAnim != null) {
                    anim = new AnimatorSet();
                    ((AnimatorSet) anim).playSequentially(outAnim, inAnim);
                } else if (outAnim != null) {
                    anim = outAnim;
                } else {
                    anim = inAnim;
                }
            }
            final CharSequence charSequence = endText;
            final int i = endSelectionStart;
            final int i2 = endSelectionEnd;
            final int i3 = endColor;
            final CharSequence charSequence2 = startText;
            Transition.TransitionListener transitionListener = new Transition.TransitionListenerAdapter() {
                int mPausedColor = 0;

                @Override
                public void onTransitionPause(Transition transition) {
                    if (ChangeText.this.mChangeBehavior != 2) {
                        view.setText(charSequence);
                        if (view instanceof EditText) {
                            ChangeText.this.setSelection((EditText) view, i, i2);
                        }
                    }
                    if (ChangeText.this.mChangeBehavior > 0) {
                        this.mPausedColor = view.getCurrentTextColor();
                        view.setTextColor(i3);
                    }
                }

                @Override
                public void onTransitionResume(Transition transition) {
                    if (ChangeText.this.mChangeBehavior != 2) {
                        view.setText(charSequence2);
                        if (view instanceof EditText) {
                            ChangeText.this.setSelection((EditText) view, startSelectionStart, startSelectionEnd);
                        }
                    }
                    if (ChangeText.this.mChangeBehavior > 0) {
                        view.setTextColor(this.mPausedColor);
                    }
                }
            };
            addListener(transitionListener);
            return anim;
        }
        return null;
    }

    private void setSelection(EditText editText, int start, int end) {
        if (start >= 0 && end >= 0) {
            editText.setSelection(start, end);
        }
    }
}
