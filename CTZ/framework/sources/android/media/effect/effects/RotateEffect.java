package android.media.effect.effects;

import android.app.slice.SliceItem;
import android.filterpacks.imageproc.RotateFilter;
import android.media.effect.EffectContext;
import android.media.effect.SizeChangeEffect;

public class RotateEffect extends SizeChangeEffect {
    public RotateEffect(EffectContext effectContext, String str) {
        super(effectContext, str, RotateFilter.class, SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE, new Object[0]);
    }
}
