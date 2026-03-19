package android.icu.impl.number;

import java.math.BigDecimal;

public class MultiplierImpl implements MicroPropsGenerator {
    final BigDecimal bigDecimalMultiplier;
    final int magnitudeMultiplier;
    final MicroPropsGenerator parent;

    public MultiplierImpl(int i) {
        this.magnitudeMultiplier = i;
        this.bigDecimalMultiplier = null;
        this.parent = null;
    }

    public MultiplierImpl(BigDecimal bigDecimal) {
        this.magnitudeMultiplier = 0;
        this.bigDecimalMultiplier = bigDecimal;
        this.parent = null;
    }

    private MultiplierImpl(MultiplierImpl multiplierImpl, MicroPropsGenerator microPropsGenerator) {
        this.magnitudeMultiplier = multiplierImpl.magnitudeMultiplier;
        this.bigDecimalMultiplier = multiplierImpl.bigDecimalMultiplier;
        this.parent = microPropsGenerator;
    }

    public MicroPropsGenerator copyAndChain(MicroPropsGenerator microPropsGenerator) {
        return new MultiplierImpl(this, microPropsGenerator);
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity decimalQuantity) {
        MicroProps microPropsProcessQuantity = this.parent.processQuantity(decimalQuantity);
        decimalQuantity.adjustMagnitude(this.magnitudeMultiplier);
        if (this.bigDecimalMultiplier != null) {
            decimalQuantity.multiplyBy(this.bigDecimalMultiplier);
        }
        return microPropsProcessQuantity;
    }
}
