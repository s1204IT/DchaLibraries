package com.android.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.browser.HttpAuthenticationDialog;

public class PageDialogsHandler {
    private Context mContext;
    private Controller mController;
    private HttpAuthenticationDialog mHttpAuthenticationDialog;
    private HttpAuthHandler mHttpAuthenticationHandler;
    private AlertDialog mPageInfoDialog;
    private boolean mPageInfoFromShowSSLCertificateOnError;
    private Tab mPageInfoView;
    private AlertDialog mPopupWindowAttemptDialog;
    private boolean mPopupWindowAttemptIsDialog;
    private Message mPopupWindowAttemptMessage;
    private Tab mPopupWindowAttemptView;
    private AlertDialog mSSLCertificateDialog;
    private AlertDialog mSSLCertificateOnErrorDialog;
    private SslError mSSLCertificateOnErrorError;
    private SslErrorHandler mSSLCertificateOnErrorHandler;
    private WebView mSSLCertificateOnErrorView;
    private Tab mSSLCertificateView;
    private String mUrlCertificateOnError;

    public PageDialogsHandler(Context context, Controller controller) {
        this.mContext = context;
        this.mController = controller;
    }

    public void onConfigurationChanged(Configuration config) {
        if (this.mPageInfoDialog != null) {
            this.mPageInfoDialog.dismiss();
            showPageInfo(this.mPageInfoView, this.mPageInfoFromShowSSLCertificateOnError, this.mUrlCertificateOnError);
        }
        if (this.mSSLCertificateDialog != null) {
            this.mSSLCertificateDialog.dismiss();
            showSSLCertificate(this.mSSLCertificateView);
        }
        if (this.mSSLCertificateOnErrorDialog != null) {
            this.mSSLCertificateOnErrorDialog.dismiss();
            showSSLCertificateOnError(this.mSSLCertificateOnErrorView, this.mSSLCertificateOnErrorHandler, this.mSSLCertificateOnErrorError);
        }
        if (this.mHttpAuthenticationDialog != null) {
            this.mHttpAuthenticationDialog.reshow();
        }
        if (this.mPopupWindowAttemptDialog == null) {
            return;
        }
        this.mPopupWindowAttemptDialog.dismiss();
        showPopupWindowAttempt(this.mPopupWindowAttemptView, this.mPopupWindowAttemptIsDialog, this.mPopupWindowAttemptMessage);
    }

    void showHttpAuthentication(final Tab tab, final HttpAuthHandler handler, String host, String realm) {
        this.mHttpAuthenticationDialog = new HttpAuthenticationDialog(this.mContext, host, realm);
        this.mHttpAuthenticationHandler = handler;
        this.mHttpAuthenticationDialog.setOkListener(new HttpAuthenticationDialog.OkListener() {
            @Override
            public void onOk(String host2, String realm2, String username, String password) {
                PageDialogsHandler.this.setHttpAuthUsernamePassword(host2, realm2, username, password);
                handler.proceed(username, password);
                PageDialogsHandler.this.mHttpAuthenticationDialog = null;
            }
        });
        this.mHttpAuthenticationDialog.setCancelListener(new HttpAuthenticationDialog.CancelListener() {
            @Override
            public void onCancel() {
                handler.cancel();
                PageDialogsHandler.this.mController.onUpdatedSecurityState(tab);
                PageDialogsHandler.this.mHttpAuthenticationDialog = null;
            }
        });
        this.mHttpAuthenticationDialog.show();
    }

    public void setHttpAuthUsernamePassword(String host, String realm, String username, String password) {
        WebView w = this.mController.getCurrentTopWebView();
        if (w == null) {
            return;
        }
        w.setHttpAuthUsernamePassword(host, realm, username, password);
    }

    void showPageInfo(final Tab tab, final boolean fromShowSSLCertificateOnError, String urlCertificateOnError) {
        if (tab == null) {
            return;
        }
        LayoutInflater factory = LayoutInflater.from(this.mContext);
        View pageInfoView = factory.inflate(R.layout.page_info, (ViewGroup) null);
        WebView view = tab.getWebView();
        String url = fromShowSSLCertificateOnError ? urlCertificateOnError : tab.getUrl();
        String title = tab.getTitle();
        if (url == null) {
            url = "";
        }
        if (title == null) {
            title = "";
        }
        ((TextView) pageInfoView.findViewById(R.id.address)).setText(url);
        ((TextView) pageInfoView.findViewById(R.id.title)).setText(title);
        this.mPageInfoView = tab;
        this.mPageInfoFromShowSSLCertificateOnError = fromShowSSLCertificateOnError;
        this.mUrlCertificateOnError = urlCertificateOnError;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this.mContext).setTitle(R.string.page_info).setIcon(android.R.drawable.ic_dialog_info).setView(pageInfoView).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                PageDialogsHandler.this.mPageInfoDialog = null;
                PageDialogsHandler.this.mPageInfoView = null;
                if (!fromShowSSLCertificateOnError) {
                    return;
                }
                PageDialogsHandler.this.showSSLCertificateOnError(PageDialogsHandler.this.mSSLCertificateOnErrorView, PageDialogsHandler.this.mSSLCertificateOnErrorHandler, PageDialogsHandler.this.mSSLCertificateOnErrorError);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                PageDialogsHandler.this.mPageInfoDialog = null;
                PageDialogsHandler.this.mPageInfoView = null;
                if (!fromShowSSLCertificateOnError) {
                    return;
                }
                PageDialogsHandler.this.showSSLCertificateOnError(PageDialogsHandler.this.mSSLCertificateOnErrorView, PageDialogsHandler.this.mSSLCertificateOnErrorHandler, PageDialogsHandler.this.mSSLCertificateOnErrorError);
            }
        });
        if (fromShowSSLCertificateOnError || (view != null && view.getCertificate() != null)) {
            alertDialogBuilder.setNeutralButton(R.string.view_certificate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    PageDialogsHandler.this.mPageInfoDialog = null;
                    PageDialogsHandler.this.mPageInfoView = null;
                    if (fromShowSSLCertificateOnError) {
                        PageDialogsHandler.this.showSSLCertificateOnError(PageDialogsHandler.this.mSSLCertificateOnErrorView, PageDialogsHandler.this.mSSLCertificateOnErrorHandler, PageDialogsHandler.this.mSSLCertificateOnErrorError);
                    } else {
                        PageDialogsHandler.this.showSSLCertificate(tab);
                    }
                }
            });
        }
        this.mPageInfoDialog = alertDialogBuilder.show();
    }

    public void showSSLCertificate(final Tab tab) {
        SslCertificate cert = tab.getWebView().getCertificate();
        if (cert == null) {
            return;
        }
        this.mSSLCertificateView = tab;
        this.mSSLCertificateDialog = createSslCertificateDialog(cert, tab.getSslCertificateError()).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                PageDialogsHandler.this.mSSLCertificateDialog = null;
                PageDialogsHandler.this.mSSLCertificateView = null;
                PageDialogsHandler.this.showPageInfo(tab, false, null);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                PageDialogsHandler.this.mSSLCertificateDialog = null;
                PageDialogsHandler.this.mSSLCertificateView = null;
                PageDialogsHandler.this.showPageInfo(tab, false, null);
            }
        }).show();
    }

    void showSSLCertificateOnError(final WebView view, final SslErrorHandler handler, final SslError error) {
        SslCertificate cert;
        if (error == null || (cert = error.getCertificate()) == null) {
            return;
        }
        this.mSSLCertificateOnErrorHandler = handler;
        this.mSSLCertificateOnErrorView = view;
        this.mSSLCertificateOnErrorError = error;
        this.mSSLCertificateOnErrorDialog = createSslCertificateDialog(cert, error).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                PageDialogsHandler.this.mSSLCertificateOnErrorDialog = null;
                PageDialogsHandler.this.mSSLCertificateOnErrorView = null;
                PageDialogsHandler.this.mSSLCertificateOnErrorHandler = null;
                PageDialogsHandler.this.mSSLCertificateOnErrorError = null;
                ((BrowserWebView) view).getWebViewClient().onReceivedSslError(view, handler, error);
            }
        }).setNeutralButton(R.string.page_info_view, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                PageDialogsHandler.this.mSSLCertificateOnErrorDialog = null;
                PageDialogsHandler.this.showPageInfo(PageDialogsHandler.this.mController.getTabControl().getTabFromView(view), true, error.getUrl());
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                PageDialogsHandler.this.mSSLCertificateOnErrorDialog = null;
                PageDialogsHandler.this.mSSLCertificateOnErrorView = null;
                PageDialogsHandler.this.mSSLCertificateOnErrorHandler = null;
                PageDialogsHandler.this.mSSLCertificateOnErrorError = null;
                ((BrowserWebView) view).getWebViewClient().onReceivedSslError(view, handler, error);
            }
        }).show();
    }

    private AlertDialog.Builder createSslCertificateDialog(SslCertificate certificate, SslError error) {
        int iconId;
        View certificateView = certificate.inflateCertificateView(this.mContext);
        LinearLayout placeholder = (LinearLayout) certificateView.findViewById(android.R.id.multi-select);
        LayoutInflater factory = LayoutInflater.from(this.mContext);
        if (error == null) {
            iconId = R.drawable.ic_dialog_browser_certificate_secure;
            LinearLayout table = (LinearLayout) factory.inflate(R.layout.ssl_success, placeholder);
            TextView successString = (TextView) table.findViewById(R.id.success);
            successString.setText(android.R.string.ime_action_search);
        } else {
            iconId = R.drawable.ic_dialog_browser_certificate_partially_secure;
            if (error.hasError(3)) {
                addError(factory, placeholder, R.string.ssl_untrusted);
            }
            if (error.hasError(2)) {
                addError(factory, placeholder, R.string.ssl_mismatch);
            }
            if (error.hasError(1)) {
                addError(factory, placeholder, R.string.ssl_expired);
            }
            if (error.hasError(0)) {
                addError(factory, placeholder, R.string.ssl_not_yet_valid);
            }
            if (error.hasError(4)) {
                addError(factory, placeholder, R.string.ssl_date_invalid);
            }
            if (error.hasError(5)) {
                addError(factory, placeholder, R.string.ssl_invalid);
            }
            if (placeholder.getChildCount() == 0) {
                addError(factory, placeholder, R.string.ssl_unknown);
            }
        }
        return new AlertDialog.Builder(this.mContext).setTitle(android.R.string.ime_action_previous).setIcon(iconId).setView(certificateView);
    }

    private void addError(LayoutInflater inflater, LinearLayout parent, int error) {
        TextView textView = (TextView) inflater.inflate(R.layout.ssl_warning, (ViewGroup) parent, false);
        textView.setText(error);
        parent.addView(textView);
    }

    void showPopupWindowAttempt(final Tab tab, final boolean dialog, final Message resultMsg) {
        this.mPopupWindowAttemptView = tab;
        this.mPopupWindowAttemptIsDialog = dialog;
        this.mPopupWindowAttemptMessage = resultMsg;
        DialogInterface.OnClickListener allowListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                PageDialogsHandler.this.mPopupWindowAttemptDialog = null;
                PageDialogsHandler.this.mPopupWindowAttemptView = null;
                PageDialogsHandler.this.mPopupWindowAttemptIsDialog = false;
                PageDialogsHandler.this.mPopupWindowAttemptMessage = null;
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                if (dialog) {
                    tab.createSubWindow();
                    PageDialogsHandler.this.mController.attachSubWindow(tab);
                    transport.setWebView(tab.getSubWebView());
                    tab.PopupWindowShown(false);
                } else {
                    Tab newTab = PageDialogsHandler.this.mController.openTab((String) null, tab, true, true);
                    transport.setWebView(newTab.getWebView());
                }
                resultMsg.sendToTarget();
            }
        };
        DialogInterface.OnClickListener blockListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                PageDialogsHandler.this.mPopupWindowAttemptDialog = null;
                PageDialogsHandler.this.mPopupWindowAttemptView = null;
                PageDialogsHandler.this.mPopupWindowAttemptIsDialog = false;
                PageDialogsHandler.this.mPopupWindowAttemptMessage = null;
                resultMsg.sendToTarget();
                tab.PopupWindowShown(false);
            }
        };
        this.mPopupWindowAttemptDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.popup_window_attempt).setPositiveButton(R.string.allow, allowListener).setNegativeButton(R.string.block, blockListener).setCancelable(false).create();
        this.mPopupWindowAttemptDialog.show();
        if (dialog) {
            tab.PopupWindowShown(true);
        }
    }

    void destroyDialogs() {
        if (this.mPageInfoDialog != null) {
            this.mPageInfoDialog.dismiss();
            this.mPageInfoDialog = null;
            this.mPageInfoView = null;
        }
        if (this.mSSLCertificateDialog != null) {
            this.mSSLCertificateDialog.dismiss();
            this.mSSLCertificateDialog = null;
            this.mSSLCertificateView = null;
        }
        if (this.mSSLCertificateOnErrorDialog != null) {
            this.mSSLCertificateOnErrorDialog.dismiss();
            ((BrowserWebView) this.mSSLCertificateOnErrorView).getWebViewClient().onReceivedSslError(this.mSSLCertificateOnErrorView, this.mSSLCertificateOnErrorHandler, this.mSSLCertificateOnErrorError);
            this.mSSLCertificateOnErrorDialog = null;
            this.mSSLCertificateOnErrorView = null;
            this.mSSLCertificateOnErrorHandler = null;
            this.mSSLCertificateOnErrorError = null;
        }
        if (this.mHttpAuthenticationDialog != null) {
            this.mHttpAuthenticationHandler.cancel();
            this.mHttpAuthenticationDialog = null;
            this.mHttpAuthenticationHandler = null;
        }
        if (this.mPopupWindowAttemptDialog == null) {
            return;
        }
        this.mPopupWindowAttemptDialog.dismiss();
        this.mPopupWindowAttemptDialog = null;
        this.mPopupWindowAttemptView = null;
        this.mPopupWindowAttemptIsDialog = false;
        this.mPopupWindowAttemptMessage = null;
    }
}
