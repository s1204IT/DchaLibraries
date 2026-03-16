package jp.co.omronsoft.iwnnime.ml.controlpanel;

public class SubControlPanelStandard extends ControlPanelStandard {
    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
