package android.media.effect;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterFactory;
import android.filterfw.core.FilterFunction;
import android.filterfw.core.Frame;

public class SingleFilterEffect extends FilterEffect {
    protected FilterFunction mFunction;
    protected String mInputName;
    protected String mOutputName;

    public SingleFilterEffect(EffectContext effectContext, String str, Class cls, String str2, String str3, Object... objArr) {
        super(effectContext, str);
        this.mInputName = str2;
        this.mOutputName = str3;
        Filter filterCreateFilterByClass = FilterFactory.sharedFactory().createFilterByClass(cls, cls.getSimpleName());
        filterCreateFilterByClass.initWithAssignmentList(objArr);
        this.mFunction = new FilterFunction(getFilterContext(), filterCreateFilterByClass);
    }

    @Override
    public void apply(int i, int i2, int i3, int i4) {
        beginGLEffect();
        Frame frameFrameFromTexture = frameFromTexture(i, i2, i3);
        Frame frameFrameFromTexture2 = frameFromTexture(i4, i2, i3);
        Frame frameExecuteWithArgList = this.mFunction.executeWithArgList(this.mInputName, frameFrameFromTexture);
        frameFrameFromTexture2.setDataFromFrame(frameExecuteWithArgList);
        frameFrameFromTexture.release();
        frameFrameFromTexture2.release();
        frameExecuteWithArgList.release();
        endGLEffect();
    }

    @Override
    public void setParameter(String str, Object obj) {
        this.mFunction.setInputValue(str, obj);
    }

    @Override
    public void release() {
        this.mFunction.tearDown();
        this.mFunction = null;
    }
}
