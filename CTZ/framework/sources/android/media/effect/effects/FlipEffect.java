package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.FlipFilter;
import android.media.effect.EffectContext;
import android.media.effect.SingleFilterEffect;

public class FlipEffect extends SingleFilterEffect {
    public FlipEffect(EffectContext effectContext, String str) {
        super(effectContext, str, FlipFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
