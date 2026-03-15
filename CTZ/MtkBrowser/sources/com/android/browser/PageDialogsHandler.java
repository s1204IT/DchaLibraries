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

    private void addError(LayoutInflater layoutInflater, LinearLayout linearLayout, int i) {
        TextView textView = (TextView) layoutInflater.inflate(2130968625, (ViewGroup) linearLayout, false);
        textView.setText(i);
        linearLayout.addView(textView);
    }

    private AlertDialog.Builder createSslCertificateDialog(SslCertificate sslCertificate, SslError sslError) {
        int i;
        View viewInflateCertificateView = sslCertificate.inflateCertificateView(this.mContext);
        LinearLayout linearLayout = (LinearLayout) viewInflateCertificateView.findViewById(android.R.id.input_hour);
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(this.mContext);
        if (sslError == null) {
            ((TextView) ((LinearLayout) layoutInflaterFrom.inflate(2130968624, linearLayout)).findViewById(2131558517)).setText(android.R.string.mediasize_iso_b3);
            i = 2130837548;
        } else {
            if (sslError.hasError(3)) {
                addError(layoutInflaterFrom, linearLayout, 2131492974);
            }
            if (sslError.hasError(2)) {
                addError(layoutInflaterFrom, linearLayout, 2131492975);
            }
            if (sslError.hasError(1)) {
                addError(layoutInflaterFrom, linearLayout, 2131492976);
            }
            if (sslError.hasError(0)) {
                addError(layoutInflaterFrom, linearLayout, 2131492977);
            }
            if (sslError.hasError(4)) {
                addError(layoutInflaterFrom, linearLayout, 2131492978);
            }
            if (sslError.hasError(5)) {
                addError(layoutInflaterFrom, linearLayout, 2131492979);
            }
            if (linearLayout.getChildCount() == 0) {
                addError(layoutInflaterFrom, linearLayout, 2131492980);
            }
            i = 2130837547;
        }
        return new AlertDialog.Builder(this.mContext).setTitle(android.R.string.mediasize_iso_b2).setIcon(i).setView(viewInflateCertificateView);
    }

    private void showSSLCertificate(Tab tab) {
        SslCertificate certificate = tab.getWebView().getCertificate();
        if (certificate == null) {
            return;
        }
        this.mSSLCertificateView = tab;
        this.mSSLCertificateDialog = createSslCertificateDialog(certificate, tab.getSslCertificateError()).setPositiveButton(2131492964, new DialogInterface.OnClickListener(this, tab) {
            final PageDialogsHandler this$0;
            final Tab val$tab;

            {
                this.this$0 = this;
                this.val$tab = tab;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.this$0.mSSLCertificateDialog = null;
                this.this$0.mSSLCertificateView = null;
                this.this$0.showPageInfo(this.val$tab, false, null);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener(this, tab) {
            final PageDialogsHandler this$0;
            final Tab val$tab;

            {
                this.this$0 = this;
                this.val$tab = tab;
            }

            @Override
            public void onCancel(DialogInterface dialogInterface) {
                this.this$0.mSSLCertificateDialog = null;
                this.this$0.mSSLCertificateView = null;
                this.this$0.showPageInfo(this.val$tab, false, null);
            }
        }).show();
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
        if (this.mPopupWindowAttemptDialog != null) {
            this.mPopupWindowAttemptDialog.dismiss();
            this.mPopupWindowAttemptDialog = null;
            this.mPopupWindowAttemptView = null;
            this.mPopupWindowAttemptIsDialog = false;
            this.mPopupWindowAttemptMessage = null;
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
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
        if (this.mPopupWindowAttemptDialog != null) {
            this.mPopupWindowAttemptDialog.dismiss();
            showPopupWindowAttempt(this.mPopupWindowAttemptView, this.mPopupWindowAttemptIsDialog, this.mPopupWindowAttemptMessage);
        }
    }

    public void setHttpAuthUsernamePassword(String str, String str2, String str3, String str4) {
        WebView currentTopWebView = this.mController.getCurrentTopWebView();
        if (currentTopWebView != null) {
            currentTopWebView.setHttpAuthUsernamePassword(str, str2, str3, str4);
        }
    }

    void showHttpAuthentication(Tab tab, HttpAuthHandler httpAuthHandler, String str, String str2) {
        this.mHttpAuthenticationDialog = new HttpAuthenticationDialog(this.mContext, str, str2);
        this.mHttpAuthenticationHandler = httpAuthHandler;
        this.mHttpAuthenticationDialog.setOkListener(new HttpAuthenticationDialog.OkListener(this, httpAuthHandler) {
            final PageDialogsHandler this$0;
            final HttpAuthHandler val$handler;

            {
                this.this$0 = this;
                this.val$handler = httpAuthHandler;
            }

            @Override
            public void onOk(String str3, String str4, String str5, String str6) {
                this.this$0.setHttpAuthUsernamePassword(str3, str4, str5, str6);
                this.val$handler.proceed(str5, str6);
                this.this$0.mHttpAuthenticationDialog = null;
            }
        });
        this.mHttpAuthenticationDialog.setCancelListener(new HttpAuthenticationDialog.CancelListener(this, httpAuthHandler, tab) {
            final PageDialogsHandler this$0;
            final HttpAuthHandler val$handler;
            final Tab val$tab;

            {
                this.this$0 = this;
                this.val$handler = httpAuthHandler;
                this.val$tab = tab;
            }

            @Override
            public void onCancel() {
                this.val$handler.cancel();
                this.this$0.mController.onUpdatedSecurityState(this.val$tab);
                this.this$0.mHttpAuthenticationDialog = null;
            }
        });
        this.mHttpAuthenticationDialog.show();
    }

    void showPageInfo(Tab tab, boolean z, String str) {
        if (tab == null) {
            return;
        }
        View viewInflate = LayoutInflater.from(this.mContext).inflate(2130968612, (ViewGroup) null);
        WebView webView = tab.getWebView();
        String url = z ? str : tab.getUrl();
        String title = tab.getTitle();
        String str2 = url == null ? "" : url;
        if (title == null) {
            title = "";
        }
        ((TextView) viewInflate.findViewById(2131558456)).setText(str2);
        ((TextView) viewInflate.findViewById(2131558407)).setText(title);
        this.mPageInfoView = tab;
        this.mPageInfoFromShowSSLCertificateOnError = z;
        this.mUrlCertificateOnError = str;
        AlertDialog.Builder onCancelListener = new AlertDialog.Builder(this.mContext).setTitle(2131492966).setIcon(android.R.drawable.ic_dialog_info).setView(viewInflate).setPositiveButton(2131492964, new DialogInterface.OnClickListener(this, z) {
            final PageDialogsHandler this$0;
            final boolean val$fromShowSSLCertificateOnError;

            {
                this.this$0 = this;
                this.val$fromShowSSLCertificateOnError = z;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.this$0.mPageInfoDialog = null;
                this.this$0.mPageInfoView = null;
                if (this.val$fromShowSSLCertificateOnError) {
                    this.this$0.showSSLCertificateOnError(this.this$0.mSSLCertificateOnErrorView, this.this$0.mSSLCertificateOnErrorHandler, this.this$0.mSSLCertificateOnErrorError);
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener(this, z) {
            final PageDialogsHandler this$0;
            final boolean val$fromShowSSLCertificateOnError;

            {
                this.this$0 = this;
                this.val$fromShowSSLCertificateOnError = z;
            }

            @Override
            public void onCancel(DialogInterface dialogInterface) {
                this.this$0.mPageInfoDialog = null;
                this.this$0.mPageInfoView = null;
                if (this.val$fromShowSSLCertificateOnError) {
                    this.this$0.showSSLCertificateOnError(this.this$0.mSSLCertificateOnErrorView, this.this$0.mSSLCertificateOnErrorHandler, this.this$0.mSSLCertificateOnErrorError);
                }
            }
        });
        if (z || (webView != null && webView.getCertificate() != null)) {
            onCancelListener.setNeutralButton(2131492972, new DialogInterface.OnClickListener(this, z, tab) {
                final PageDialogsHandler this$0;
                final boolean val$fromShowSSLCertificateOnError;
                final Tab val$tab;

                {
                    this.this$0 = this;
                    this.val$fromShowSSLCertificateOnError = z;
                    this.val$tab = tab;
                }

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    this.this$0.mPageInfoDialog = null;
                    this.this$0.mPageInfoView = null;
                    if (this.val$fromShowSSLCertificateOnError) {
                        this.this$0.showSSLCertificateOnError(this.this$0.mSSLCertificateOnErrorView, this.this$0.mSSLCertificateOnErrorHandler, this.this$0.mSSLCertificateOnErrorError);
                    } else {
                        this.this$0.showSSLCertificate(this.val$tab);
                    }
                }
            });
        }
        this.mPageInfoDialog = onCancelListener.show();
    }

    void showPopupWindowAttempt(Tab tab, boolean z, Message message) {
        this.mPopupWindowAttemptView = tab;
        this.mPopupWindowAttemptIsDialog = z;
        this.mPopupWindowAttemptMessage = message;
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener(this, message, z, tab) {
            final PageDialogsHandler this$0;
            final boolean val$dialog;
            final Message val$resultMsg;
            final Tab val$tab;

            {
                this.this$0 = this;
                this.val$resultMsg = message;
                this.val$dialog = z;
                this.val$tab = tab;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.this$0.mPopupWindowAttemptDialog = null;
                this.this$0.mPopupWindowAttemptView = null;
                this.this$0.mPopupWindowAttemptIsDialog = false;
                this.this$0.mPopupWindowAttemptMessage = null;
                WebView.WebViewTransport webViewTransport = (WebView.WebViewTransport) this.val$resultMsg.obj;
                if (this.val$dialog) {
                    this.val$tab.createSubWindow();
                    this.this$0.mController.attachSubWindow(this.val$tab);
                    webViewTransport.setWebView(this.val$tab.getSubWebView());
                    this.val$tab.PopupWindowShown(false);
                } else {
                    webViewTransport.setWebView(this.this$0.mController.openTab((String) null, this.val$tab, true, true).getWebView());
                }
                this.val$resultMsg.sendToTarget();
            }
        };
        this.mPopupWindowAttemptDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(2131493211).setPositiveButton(2131493212, onClickListener).setNegativeButton(2131493213, new DialogInterface.OnClickListener(this, message, tab) {
            final PageDialogsHandler this$0;
            final Message val$resultMsg;
            final Tab val$tab;

            {
                this.this$0 = this;
                this.val$resultMsg = message;
                this.val$tab = tab;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.this$0.mPopupWindowAttemptDialog = null;
                this.this$0.mPopupWindowAttemptView = null;
                this.this$0.mPopupWindowAttemptIsDialog = false;
                this.this$0.mPopupWindowAttemptMessage = null;
                this.val$resultMsg.sendToTarget();
                this.val$tab.PopupWindowShown(false);
            }
        }).setCancelable(false).create();
        this.mPopupWindowAttemptDialog.show();
        if (z) {
            tab.PopupWindowShown(true);
        }
    }

    void showSSLCertificateOnError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
        SslCertificate certificate;
        if (sslError == null || (certificate = sslError.getCertificate()) == null) {
            return;
        }
        this.mSSLCertificateOnErrorHandler = sslErrorHandler;
        this.mSSLCertificateOnErrorView = webView;
        this.mSSLCertificateOnErrorError = sslError;
        this.mSSLCertificateOnErrorDialog = createSslCertificateDialog(certificate, sslError).setPositiveButton(2131492964, new DialogInterface.OnClickListener(this, webView, sslErrorHandler, sslError) {
            final PageDialogsHandler this$0;
            final SslError val$error;
            final SslErrorHandler val$handler;
            final WebView val$view;

            {
                this.this$0 = this;
                this.val$view = webView;
                this.val$handler = sslErrorHandler;
                this.val$error = sslError;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.this$0.mSSLCertificateOnErrorDialog = null;
                this.this$0.mSSLCertificateOnErrorView = null;
                this.this$0.mSSLCertificateOnErrorHandler = null;
                this.this$0.mSSLCertificateOnErrorError = null;
                ((BrowserWebView) this.val$view).getWebViewClient().onReceivedSslError(this.val$view, this.val$handler, this.val$error);
            }
        }).setNeutralButton(2131492967, new DialogInterface.OnClickListener(this, webView, sslError) {
            final PageDialogsHandler this$0;
            final SslError val$error;
            final WebView val$view;

            {
                this.this$0 = this;
                this.val$view = webView;
                this.val$error = sslError;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                this.this$0.mSSLCertificateOnErrorDialog = null;
                this.this$0.showPageInfo(this.this$0.mController.getTabControl().getTabFromView(this.val$view), true, this.val$error.getUrl());
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener(this, webView, sslErrorHandler, sslError) {
            final PageDialogsHandler this$0;
            final SslError val$error;
            final SslErrorHandler val$handler;
            final WebView val$view;

            {
                this.this$0 = this;
                this.val$view = webView;
                this.val$handler = sslErrorHandler;
                this.val$error = sslError;
            }

            @Override
            public void onCancel(DialogInterface dialogInterface) {
                this.this$0.mSSLCertificateOnErrorDialog = null;
                this.this$0.mSSLCertificateOnErrorView = null;
                this.this$0.mSSLCertificateOnErrorHandler = null;
                this.this$0.mSSLCertificateOnErrorError = null;
                ((BrowserWebView) this.val$view).getWebViewClient().onReceivedSslError(this.val$view, this.val$handler, this.val$error);
            }
        }).show();
    }
}
