package com.android.internal.widget.multiwaveview;

import android.animation.TimeInterpolator;

class Ease {
    private static final float DOMAIN = 1.0f;
    private static final float DURATION = 1.0f;
    private static final float START = 0.0f;

    Ease() {
    }

    static class Linear {
        public static final TimeInterpolator easeNone = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return input;
            }
        };

        Linear() {
        }
    }

    static class Cubic {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return (1.0f * input2 * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = (input / 1.0f) - 1.0f;
                return (((input2 * input2 * input2) + 1.0f) * 1.0f) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 0.5f;
                if (input2 < 1.0f) {
                    return (0.5f * input2 * input2 * input2) + 0.0f;
                }
                float input3 = input2 - 2.0f;
                return (((input3 * input3 * input3) + 2.0f) * 0.5f) + 0.0f;
            }
        };

        Cubic() {
        }
    }

    static class Quad {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return (1.0f * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return ((-1.0f) * input2 * (input2 - 2.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 0.5f;
                if (input2 < 1.0f) {
                    return (0.5f * input2 * input2) + 0.0f;
                }
                float input3 = input2 - 1.0f;
                return ((-0.5f) * (((input3 - 2.0f) * input3) - 1.0f)) + 0.0f;
            }
        };

        Quad() {
        }
    }

    static class Quart {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return (1.0f * input2 * input2 * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = (input / 1.0f) - 1.0f;
                return ((-1.0f) * ((((input2 * input2) * input2) * input2) - 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 0.5f;
                if (input2 < 1.0f) {
                    return (0.5f * input2 * input2 * input2 * input2) + 0.0f;
                }
                float input3 = input2 - 2.0f;
                return ((-0.5f) * ((((input3 * input3) * input3) * input3) - 2.0f)) + 0.0f;
            }
        };

        Quart() {
        }
    }

    static class Quint {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return (1.0f * input2 * input2 * input2 * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = (input / 1.0f) - 1.0f;
                return (((input2 * input2 * input2 * input2 * input2) + 1.0f) * 1.0f) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float input2 = input / 0.5f;
                if (input2 < 1.0f) {
                    return (0.5f * input2 * input2 * input2 * input2 * input2) + 0.0f;
                }
                float input3 = input2 - 2.0f;
                return (((input3 * input3 * input3 * input3 * input3) + 2.0f) * 0.5f) + 0.0f;
            }
        };

        Quint() {
        }
    }

    static class Sine {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return ((-1.0f) * ((float) Math.cos(((double) (input / 1.0f)) * 1.5707963267948966d))) + 1.0f + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return (((float) Math.sin(((double) (input / 1.0f)) * 1.5707963267948966d)) * 1.0f) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return ((-0.5f) * (((float) Math.cos((3.141592653589793d * ((double) input)) / 1.0d)) - 1.0f)) + 0.0f;
            }
        };

        Sine() {
        }
    }
}
