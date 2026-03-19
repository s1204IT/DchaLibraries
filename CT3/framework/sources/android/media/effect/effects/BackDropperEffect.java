package android.media.effect.effects;

import android.filterfw.core.Filter;
import android.filterfw.core.OneShotScheduler;
import android.filterpacks.videoproc.BackDropperFilter;
import android.media.effect.EffectContext;
import android.media.effect.EffectUpdateListener;
import android.media.effect.FilterGraphEffect;
import android.provider.MediaStore;

public class BackDropperEffect extends FilterGraphEffect {
    private static final String mGraphDefinition = "@import android.filterpacks.base;\n@import android.filterpacks.videoproc;\n@import android.filterpacks.videosrc;\n\n@filter GLTextureSource foreground {\n  texId = 0;\n  width = 0;\n  height = 0;\n  repeatFrame = true;\n}\n\n@filter MediaSource background {\n  sourceUrl = \"no_file_specified\";\n  waitForNewFrame = false;\n  sourceIsUrl = true;\n}\n\n@filter BackDropperFilter replacer {\n  autowbToggle = 1;\n}\n\n@filter GLTextureTarget output {\n  texId = 0;\n}\n\n@connect foreground[frame]  => replacer[video];\n@connect background[video]  => replacer[background];\n@connect replacer[video]    => output[frame];\n";
    private EffectUpdateListener mEffectListener;
    private BackDropperFilter.LearningDoneListener mLearningListener;

    public BackDropperEffect(EffectContext context, String name) {
        super(context, name, mGraphDefinition, "foreground", MediaStore.EXTRA_OUTPUT, OneShotScheduler.class);
        this.mEffectListener = null;
        this.mLearningListener = new BackDropperFilter.LearningDoneListener() {
            @Override
            public void onLearningDone(BackDropperFilter filter) {
                if (BackDropperEffect.this.mEffectListener == null) {
                    return;
                }
                BackDropperEffect.this.mEffectListener.onEffectUpdated(BackDropperEffect.this, null);
            }
        };
        Filter replacer = this.mGraph.getFilter("replacer");
        replacer.setInputValue("learningDoneListener", this.mLearningListener);
    }

    @Override
    public void setParameter(String parameterKey, Object value) {
        if (parameterKey.equals("source")) {
            Filter background = this.mGraph.getFilter("background");
            background.setInputValue("sourceUrl", value);
        } else {
            if (!parameterKey.equals("context")) {
                return;
            }
            Filter background2 = this.mGraph.getFilter("background");
            background2.setInputValue("context", value);
        }
    }

    @Override
    public void setUpdateListener(EffectUpdateListener listener) {
        this.mEffectListener = listener;
    }
}
