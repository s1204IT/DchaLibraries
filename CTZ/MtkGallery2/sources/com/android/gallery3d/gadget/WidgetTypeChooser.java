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
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            WidgetTypeChooser.this.setResult(-1, new Intent().putExtra("widget-type", i));
            WidgetTypeChooser.this.finish();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(R.string.widget_type);
        setContentView(R.layout.choose_widget_type);
        ((RadioGroup) findViewById(R.id.widget_type)).setOnCheckedChangeListener(this.mListener);
        ((Button) findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WidgetTypeChooser.this.setResult(0);
                WidgetTypeChooser.this.finish();
            }
        });
    }
}
