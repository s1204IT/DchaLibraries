package com.android.gallery3d.filtershow.editors;

import android.content.Context;
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
import com.mediatek.gallery3d.util.Log;
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

    protected ParametricEditor(int i) {
        super(i);
        this.LOGTAG = "ParametricEditor";
    }

    protected ParametricEditor(int i, int i2, int i3) {
        super(i);
        this.LOGTAG = "ParametricEditor";
        this.mLayoutID = i2;
        this.mViewID = i3;
    }

    @Override
    public String calculateUserMessage(Context context, String str, Object obj) {
        if (useCompact(context) & (this.mShowParameter == SHOW_VALUE_INT)) {
            if (getLocalRepresentation() instanceof FilterBasicRepresentation) {
                return " " + str.toUpperCase() + " " + ((FilterBasicRepresentation) getLocalRepresentation()).getStateRepresentation();
            }
            return " " + str.toUpperCase() + " " + obj;
        }
        return " " + str.toUpperCase();
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
            FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) getLocalRepresentation();
            if (this.mControl == null) {
                Log.w("ParametricEditor", "<reflectCurrentFilter> null pointer for mControl.");
            } else {
                this.mControl.setPrameter(filterBasicRepresentation);
            }
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
    public void setUtilityPanelUI(View view, View view2) {
        this.mActionButton = view;
        this.mEditControl = view2;
        Parameter parameterToEdit = getParameterToEdit(getLocalRepresentation());
        if (parameterToEdit != null) {
            control(parameterToEdit, view2);
            return;
        }
        this.mSeekBar = new SeekBar(view2.getContext());
        this.mSeekBar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        ((LinearLayout) view2).addView(this.mSeekBar);
        this.mSeekBar.setOnSeekBarChangeListener(this);
    }

    protected void control(Parameter parameter, View view) {
        String parameterType = parameter.getParameterType();
        Class cls = (useCompact(view.getContext()) ? portraitMap : landscapeMap).get(parameterType);
        if (cls != null) {
            try {
                this.mControl = (Control) cls.newInstance();
                parameter.setController(this.mControl);
                this.mControl.setUp((ViewGroup) view, parameter, this);
                return;
            } catch (Exception e) {
                Log.e("ParametricEditor", "Error in loading Control ", e);
                return;
            }
        }
        Log.e("ParametricEditor", "Unable to find class for " + parameterType);
        for (String str : portraitMap.keySet()) {
            Log.e("ParametricEditor", "for " + str + " use " + portraitMap.get(str));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
