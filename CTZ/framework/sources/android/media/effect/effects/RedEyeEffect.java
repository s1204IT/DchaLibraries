package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.RedEyeFilter;
import android.media.effect.EffectContext;
import android.media.effect.SingleFilterEffect;

public class RedEyeEffect extends SingleFilterEffect {
    public RedEyeEffect(EffectContext effectContext, String str) {
        super(effectContext, str, RedEyeFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
