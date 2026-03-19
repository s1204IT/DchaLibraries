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

    BasicPeriodFormatterFactory(PeriodFormatterDataService periodFormatterDataService) {
        this.ds = periodFormatterDataService;
    }

    public static BasicPeriodFormatterFactory getDefault() {
        return (BasicPeriodFormatterFactory) BasicPeriodFormatterService.getInstance().newPeriodFormatterFactory();
    }

    @Override
    public PeriodFormatterFactory setLocale(String str) {
        this.data = null;
        this.localeName = str;
        return this;
    }

    @Override
    public PeriodFormatterFactory setDisplayLimit(boolean z) {
        updateCustomizations().displayLimit = z;
        return this;
    }

    public boolean getDisplayLimit() {
        return this.customizations.displayLimit;
    }

    @Override
    public PeriodFormatterFactory setDisplayPastFuture(boolean z) {
        updateCustomizations().displayDirection = z;
        return this;
    }

    public boolean getDisplayPastFuture() {
        return this.customizations.displayDirection;
    }

    @Override
    public PeriodFormatterFactory setSeparatorVariant(int i) {
        updateCustomizations().separatorVariant = (byte) i;
        return this;
    }

    public int getSeparatorVariant() {
        return this.customizations.separatorVariant;
    }

    @Override
    public PeriodFormatterFactory setUnitVariant(int i) {
        updateCustomizations().unitVariant = (byte) i;
        return this;
    }

    public int getUnitVariant() {
        return this.customizations.unitVariant;
    }

    @Override
    public PeriodFormatterFactory setCountVariant(int i) {
        updateCustomizations().countVariant = (byte) i;
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

    PeriodFormatterData getData(String str) {
        return this.ds.get(str);
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
            Customizations customizations = new Customizations();
            customizations.displayLimit = this.displayLimit;
            customizations.displayDirection = this.displayDirection;
            customizations.separatorVariant = this.separatorVariant;
            customizations.unitVariant = this.unitVariant;
            customizations.countVariant = this.countVariant;
            return customizations;
        }
    }
}
