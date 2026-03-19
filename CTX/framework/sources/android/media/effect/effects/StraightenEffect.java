package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.StraightenFilter;
import android.media.effect.EffectContext;
import android.media.effect.SingleFilterEffect;

public class StraightenEffect extends SingleFilterEffect {
    public StraightenEffect(EffectContext effectContext, String str) {
        super(effectContext, str, StraightenFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
