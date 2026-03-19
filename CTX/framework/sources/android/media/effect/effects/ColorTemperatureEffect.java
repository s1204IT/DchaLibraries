package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.ColorTemperatureFilter;
import android.media.effect.EffectContext;
import android.media.effect.SingleFilterEffect;

public class ColorTemperatureEffect extends SingleFilterEffect {
    public ColorTemperatureEffect(EffectContext effectContext, String str) {
        super(effectContext, str, ColorTemperatureFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
