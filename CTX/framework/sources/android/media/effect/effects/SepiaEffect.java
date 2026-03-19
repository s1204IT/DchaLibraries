package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.SepiaFilter;
import android.media.effect.EffectContext;
import android.media.effect.SingleFilterEffect;

public class SepiaEffect extends SingleFilterEffect {
    public SepiaEffect(EffectContext effectContext, String str) {
        super(effectContext, str, SepiaFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
