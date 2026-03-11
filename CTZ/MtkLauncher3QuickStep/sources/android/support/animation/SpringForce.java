package android.support.animation;

import android.support.animation.DynamicAnimation;
import android.support.annotation.FloatRange;
import android.support.annotation.RestrictTo;

public final class SpringForce implements Force {
    public static final float DAMPING_RATIO_HIGH_BOUNCY = 0.2f;
    public static final float DAMPING_RATIO_LOW_BOUNCY = 0.75f;
    public static final float DAMPING_RATIO_MEDIUM_BOUNCY = 0.5f;
    public static final float DAMPING_RATIO_NO_BOUNCY = 1.0f;
    public static final float STIFFNESS_HIGH = 10000.0f;
    public static final float STIFFNESS_LOW = 200.0f;
    public static final float STIFFNESS_MEDIUM = 1500.0f;
    public static final float STIFFNESS_VERY_LOW = 50.0f;
    private static final double UNSET = Double.MAX_VALUE;
    private static final double VELOCITY_THRESHOLD_MULTIPLIER = 62.5d;
    private double mDampedFreq;
    double mDampingRatio;
    private double mFinalPosition;
    private double mGammaMinus;
    private double mGammaPlus;
    private boolean mInitialized;
    private final DynamicAnimation.MassState mMassState;
    double mNaturalFreq;
    private double mValueThreshold;
    private double mVelocityThreshold;

    public SpringForce() {
        this.mNaturalFreq = Math.sqrt(1500.0d);
        this.mDampingRatio = 0.5d;
        this.mInitialized = false;
        this.mFinalPosition = UNSET;
        this.mMassState = new DynamicAnimation.MassState();
    }

    public SpringForce(float finalPosition) {
        this.mNaturalFreq = Math.sqrt(1500.0d);
        this.mDampingRatio = 0.5d;
        this.mInitialized = false;
        this.mFinalPosition = UNSET;
        this.mMassState = new DynamicAnimation.MassState();
        this.mFinalPosition = finalPosition;
    }

    public SpringForce setStiffness(@FloatRange(from = 0.0d, fromInclusive = false) float stiffness) {
        if (stiffness <= 0.0f) {
            throw new IllegalArgumentException("Spring stiffness constant must be positive.");
        }
        this.mNaturalFreq = Math.sqrt(stiffness);
        this.mInitialized = false;
        return this;
    }

    public float getStiffness() {
        return (float) (this.mNaturalFreq * this.mNaturalFreq);
    }

    public SpringForce setDampingRatio(@FloatRange(from = 0.0d) float dampingRatio) {
        if (dampingRatio < 0.0f) {
            throw new IllegalArgumentException("Damping ratio must be non-negative");
        }
        this.mDampingRatio = dampingRatio;
        this.mInitialized = false;
        return this;
    }

    public float getDampingRatio() {
        return (float) this.mDampingRatio;
    }

    public SpringForce setFinalPosition(float finalPosition) {
        this.mFinalPosition = finalPosition;
        return this;
    }

    public float getFinalPosition() {
        return (float) this.mFinalPosition;
    }

    @Override
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public float getAcceleration(float lastDisplacement, float lastVelocity) {
        float lastDisplacement2 = lastDisplacement - getFinalPosition();
        double k = this.mNaturalFreq * this.mNaturalFreq;
        double c = 2.0d * this.mNaturalFreq * this.mDampingRatio;
        return (float) (((-k) * ((double) lastDisplacement2)) - (((double) lastVelocity) * c));
    }

    @Override
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public boolean isAtEquilibrium(float value, float velocity) {
        if (Math.abs(velocity) < this.mVelocityThreshold && Math.abs(value - getFinalPosition()) < this.mValueThreshold) {
            return true;
        }
        return false;
    }

    private void init() {
        if (this.mInitialized) {
            return;
        }
        if (this.mFinalPosition == UNSET) {
            throw new IllegalStateException("Error: Final position of the spring must be set before the animation starts");
        }
        if (this.mDampingRatio > 1.0d) {
            this.mGammaPlus = ((-this.mDampingRatio) * this.mNaturalFreq) + (this.mNaturalFreq * Math.sqrt((this.mDampingRatio * this.mDampingRatio) - 1.0d));
            this.mGammaMinus = ((-this.mDampingRatio) * this.mNaturalFreq) - (this.mNaturalFreq * Math.sqrt((this.mDampingRatio * this.mDampingRatio) - 1.0d));
        } else if (this.mDampingRatio >= 0.0d && this.mDampingRatio < 1.0d) {
            this.mDampedFreq = this.mNaturalFreq * Math.sqrt(1.0d - (this.mDampingRatio * this.mDampingRatio));
        }
        this.mInitialized = true;
    }

    DynamicAnimation.MassState updateValues(double lastDisplacement, double lastVelocity, long timeElapsed) {
        double currentVelocity;
        double displacement;
        init();
        double deltaT = timeElapsed / 1000.0d;
        double lastDisplacement2 = lastDisplacement - this.mFinalPosition;
        if (this.mDampingRatio > 1.0d) {
            double coeffA = lastDisplacement2 - (((this.mGammaMinus * lastDisplacement2) - lastVelocity) / (this.mGammaMinus - this.mGammaPlus));
            double coeffB = ((this.mGammaMinus * lastDisplacement2) - lastVelocity) / (this.mGammaMinus - this.mGammaPlus);
            displacement = (Math.pow(2.718281828459045d, this.mGammaMinus * deltaT) * coeffA) + (Math.pow(2.718281828459045d, this.mGammaPlus * deltaT) * coeffB);
            currentVelocity = (this.mGammaMinus * coeffA * Math.pow(2.718281828459045d, this.mGammaMinus * deltaT)) + (this.mGammaPlus * coeffB * Math.pow(2.718281828459045d, this.mGammaPlus * deltaT));
        } else if (this.mDampingRatio == 1.0d) {
            double coeffB2 = lastVelocity + (this.mNaturalFreq * lastDisplacement2);
            double displacement2 = ((coeffB2 * deltaT) + lastDisplacement2) * Math.pow(2.718281828459045d, (-this.mNaturalFreq) * deltaT);
            currentVelocity = (((coeffB2 * deltaT) + lastDisplacement2) * Math.pow(2.718281828459045d, (-this.mNaturalFreq) * deltaT) * (-this.mNaturalFreq)) + (Math.pow(2.718281828459045d, (-this.mNaturalFreq) * deltaT) * coeffB2);
            displacement = displacement2;
        } else {
            double sinCoeff = (1.0d / this.mDampedFreq) * ((this.mDampingRatio * this.mNaturalFreq * lastDisplacement2) + lastVelocity);
            double dPow = Math.pow(2.718281828459045d, (-this.mDampingRatio) * this.mNaturalFreq * deltaT);
            double dCos = Math.cos(this.mDampedFreq * deltaT) * lastDisplacement2;
            double lastDisplacement3 = this.mDampedFreq;
            double displacement3 = dPow * (dCos + (Math.sin(lastDisplacement3 * deltaT) * sinCoeff));
            double d = (-this.mNaturalFreq) * displacement3 * this.mDampingRatio;
            double dPow2 = Math.pow(2.718281828459045d, (-this.mDampingRatio) * this.mNaturalFreq * deltaT);
            double d2 = (-this.mDampedFreq) * lastDisplacement2;
            double cosCoeff = this.mDampedFreq;
            currentVelocity = d + (dPow2 * ((d2 * Math.sin(cosCoeff * deltaT)) + (this.mDampedFreq * sinCoeff * Math.cos(this.mDampedFreq * deltaT))));
            displacement = displacement3;
        }
        this.mMassState.mValue = (float) (this.mFinalPosition + displacement);
        this.mMassState.mVelocity = (float) currentVelocity;
        return this.mMassState;
    }

    void setValueThreshold(double threshold) {
        this.mValueThreshold = Math.abs(threshold);
        this.mVelocityThreshold = this.mValueThreshold * VELOCITY_THRESHOLD_MULTIPLIER;
    }
}
