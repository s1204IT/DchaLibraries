package android.icu.impl.duration;

import android.icu.impl.duration.impl.PeriodFormatterData;
import android.icu.impl.duration.impl.PeriodFormatterDataService;
import java.util.Locale;

public class BasicPeriodFormatterFactory implements PeriodFormatterFactory {
    private boolean customizationsInUse;
    private PeriodFormatterData data;
    private final PeriodFormatterDataService ds;
    private Customizations customizations = new Customizations();
    private String localeName = Locale.getDefault().toString();

    BasicPeriodFormatterFactory(PeriodFormatterDataService ds) {
        this.ds = ds;
    }

    public static BasicPeriodFormatterFactory getDefault() {
        return (BasicPeriodFormatterFactory) BasicPeriodFormatterService.getInstance().newPeriodFormatterFactory();
    }

    @Override
    public PeriodFormatterFactory setLocale(String localeName) {
        this.data = null;
        this.localeName = localeName;
        return this;
    }

    @Override
    public PeriodFormatterFactory setDisplayLimit(boolean display) {
        updateCustomizations().displayLimit = display;
        return this;
    }

    public boolean getDisplayLimit() {
        return this.customizations.displayLimit;
    }

    @Override
    public PeriodFormatterFactory setDisplayPastFuture(boolean display) {
        updateCustomizations().displayDirection = display;
        return this;
    }

    public boolean getDisplayPastFuture() {
        return this.customizations.displayDirection;
    }

    @Override
    public PeriodFormatterFactory setSeparatorVariant(int variant) {
        updateCustomizations().separatorVariant = (byte) variant;
        return this;
    }

    public int getSeparatorVariant() {
        return this.customizations.separatorVariant;
    }

    @Override
    public PeriodFormatterFactory setUnitVariant(int variant) {
        updateCustomizations().unitVariant = (byte) variant;
        return this;
    }

    public int getUnitVariant() {
        return this.customizations.unitVariant;
    }

    @Override
    public PeriodFormatterFactory setCountVariant(int variant) {
        updateCustomizations().countVariant = (byte) variant;
        return this;
    }

    public int getCountVariant() {
        return this.customizations.countVariant;
    }

    @Override
    public PeriodFormatter getFormatter() {
        this.customizationsInUse = true;
        return new BasicPeriodFormatter(this, this.localeName, getData(), this.customizations);
    }

    private Customizations updateCustomizations() {
        if (this.customizationsInUse) {
            this.customizations = this.customizations.copy();
            this.customizationsInUse = false;
        }
        return this.customizations;
    }

    PeriodFormatterData getData() {
        if (this.data == null) {
            this.data = this.ds.get(this.localeName);
        }
        return this.data;
    }

    PeriodFormatterData getData(String locName) {
        return this.ds.get(locName);
    }

    static class Customizations {
        boolean displayLimit = true;
        boolean displayDirection = true;
        byte separatorVariant = 2;
        byte unitVariant = 0;
        byte countVariant = 0;

        Customizations() {
        }

        public Customizations copy() {
            Customizations result = new Customizations();
            result.displayLimit = this.displayLimit;
            result.displayDirection = this.displayDirection;
            result.separatorVariant = this.separatorVariant;
            result.unitVariant = this.unitVariant;
            result.countVariant = this.countVariant;
            return result;
        }
    }
}
