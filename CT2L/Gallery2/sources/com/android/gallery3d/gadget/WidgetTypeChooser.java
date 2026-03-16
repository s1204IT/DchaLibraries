package com.android.gallery3d.gadget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import com.android.gallery3d.R;

public class WidgetTypeChooser extends Activity {
    private RadioGroup.OnCheckedChangeListener mListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Intent data = new Intent().putExtra("widget-type", checkedId);
            WidgetTypeChooser.this.setResult(-1, data);
            WidgetTypeChooser.this.finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.widget_type);
        setContentView(R.layout.choose_widget_type);
        RadioGroup rg = (RadioGroup) findViewById(R.id.widget_type);
        rg.setOnCheckedChangeListener(this.mListener);
        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WidgetTypeChooser.this.setResult(0);
                WidgetTypeChooser.this.finish();
            }
        });
    }
}
