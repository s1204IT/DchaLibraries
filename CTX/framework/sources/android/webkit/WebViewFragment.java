package android.webkit;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@Deprecated
public class WebViewFragment extends Fragment {
    private boolean mIsWebViewAvailable;
    private WebView mWebView;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (this.mWebView != null) {
            this.mWebView.destroy();
        }
        this.mWebView = new WebView(getContext());
        this.mIsWebViewAvailable = true;
        return this.mWebView;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mWebView.onPause();
    }

    @Override
    public void onResume() {
        this.mWebView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        this.mIsWebViewAvailable = false;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (this.mWebView != null) {
            this.mWebView.destroy();
            this.mWebView = null;
        }
        super.onDestroy();
    }

    public WebView getWebView() {
        if (this.mIsWebViewAvailable) {
            return this.mWebView;
        }
        return null;
    }
}
