package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.ActionSlider;
import com.android.gallery3d.filtershow.controller.BasicSlider;
import com.android.gallery3d.filtershow.controller.ColorChooser;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterBrightness;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.android.gallery3d.filtershow.controller.ParameterHue;
import com.android.gallery3d.filtershow.controller.ParameterOpacity;
import com.android.gallery3d.filtershow.controller.ParameterSaturation;
import com.android.gallery3d.filtershow.controller.SliderBrightness;
import com.android.gallery3d.filtershow.controller.SliderHue;
import com.android.gallery3d.filtershow.controller.SliderOpacity;
import com.android.gallery3d.filtershow.controller.SliderSaturation;
import com.android.gallery3d.filtershow.controller.StyleChooser;
import com.android.gallery3d.filtershow.controller.TitledSlider;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import java.util.HashMap;

public class ParametricEditor extends Editor {
    private final String LOGTAG;
    View mActionButton;
    protected Control mControl;
    View mEditControl;
    private int mLayoutID;
    private int mViewID;
    public static int ID = R.id.editorParametric;
    static HashMap<String, Class> portraitMap = new HashMap<>();
    static HashMap<String, Class> landscapeMap = new HashMap<>();

    static {
        portraitMap.put(ParameterSaturation.sParameterType, SliderSaturation.class);
        landscapeMap.put(ParameterSaturation.sParameterType, SliderSaturation.class);
        portraitMap.put(ParameterHue.sParameterType, SliderHue.class);
        landscapeMap.put(ParameterHue.sParameterType, SliderHue.class);
        portraitMap.put(ParameterOpacity.sParameterType, SliderOpacity.class);
        landscapeMap.put(ParameterOpacity.sParameterType, SliderOpacity.class);
        portraitMap.put(ParameterBrightness.sParameterType, SliderBrightness.class);
        landscapeMap.put(ParameterBrightness.sParameterType, SliderBrightness.class);
        portraitMap.put(ParameterColor.sParameterType, ColorChooser.class);
        landscapeMap.put(ParameterColor.sParameterType, ColorChooser.class);
        portraitMap.put("ParameterInteger", BasicSlider.class);
        landscapeMap.put("ParameterInteger", TitledSlider.class);
        portraitMap.put("ParameterActionAndInt", ActionSlider.class);
        landscapeMap.put("ParameterActionAndInt", ActionSlider.class);
        portraitMap.put("ParameterStyles", StyleChooser.class);
        landscapeMap.put("ParameterStyles", StyleChooser.class);
    }

    protected ParametricEditor(int id) {
        super(id);
        this.LOGTAG = "ParametricEditor";
    }

    protected ParametricEditor(int id, int layoutID, int viewID) {
        super(id);
        this.LOGTAG = "ParametricEditor";
        this.mLayoutID = layoutID;
        this.mViewID = viewID;
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        if (!((this.mShowParameter == SHOW_VALUE_INT) & useCompact(context))) {
            String apply = " " + effectName.toUpperCase();
            return apply;
        }
        if (!(getLocalRepresentation() instanceof FilterBasicRepresentation)) {
            String apply2 = " " + effectName.toUpperCase() + " " + parameterValue;
            return apply2;
        }
        FilterBasicRepresentation interval = (FilterBasicRepresentation) getLocalRepresentation();
        String apply3 = " " + effectName.toUpperCase() + " " + interval.getStateRepresentation();
        return apply3;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        unpack(this.mViewID, this.mLayoutID);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterBasicRepresentation)) {
            FilterBasicRepresentation interval = (FilterBasicRepresentation) getLocalRepresentation();
            this.mControl.setPrameter(interval);
        }
    }

    protected static boolean useCompact(Context context) {
        return context.getResources().getConfiguration().orientation == 1;
    }

    protected Parameter getParameterToEdit(FilterRepresentation filterRepresentation) {
        if (this instanceof Parameter) {
            return (Parameter) this;
        }
        if (filterRepresentation instanceof Parameter) {
            return (Parameter) filterRepresentation;
        }
        return null;
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        this.mActionButton = actionButton;
        this.mEditControl = editControl;
        FilterRepresentation rep = getLocalRepresentation();
        Parameter param = getParameterToEdit(rep);
        if (param != null) {
            control(param, editControl);
            return;
        }
        this.mSeekBar = new SeekBar(editControl.getContext());
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        this.mSeekBar.setLayoutParams(lp);
        ((LinearLayout) editControl).addView(this.mSeekBar);
        this.mSeekBar.setOnSeekBarChangeListener(this);
    }

    protected void control(Parameter p, View editControl) {
        String pType = p.getParameterType();
        Context context = editControl.getContext();
        Class c = (useCompact(context) ? portraitMap : landscapeMap).get(pType);
        if (c != null) {
            try {
                this.mControl = (Control) c.newInstance();
                p.setController(this.mControl);
                this.mControl.setUp((ViewGroup) editControl, p, this);
                return;
            } catch (Exception e) {
                Log.e("ParametricEditor", "Error in loading Control ", e);
                return;
            }
        }
        Log.e("ParametricEditor", "Unable to find class for " + pType);
        for (String string : portraitMap.keySet()) {
            Log.e("ParametricEditor", "for " + string + " use " + portraitMap.get(string));
        }
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }
}
