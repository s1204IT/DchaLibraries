package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.DuotoneFilter;
import android.media.effect.EffectContext;
import android.media.effect.SingleFilterEffect;

public class DuotoneEffect extends SingleFilterEffect {
    public DuotoneEffect(EffectContext effectContext, String str) {
        super(effectContext, str, DuotoneFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
