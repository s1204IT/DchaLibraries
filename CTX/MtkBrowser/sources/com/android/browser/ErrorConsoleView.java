package com.android.browser;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

class ErrorConsoleView extends LinearLayout {
    private TextView mConsoleHeader;
    private int mCurrentShowState;
    private ErrorConsoleListView mErrorList;
    private Vector<ConsoleMessage> mErrorMessageCache;
    private Button mEvalButton;
    private EditText mEvalEditText;
    private LinearLayout mEvalJsViewGroup;
    private boolean mSetupComplete;
    private WebView mWebView;

    static class AnonymousClass3 {
        static final int[] $SwitchMap$android$webkit$ConsoleMessage$MessageLevel = new int[ConsoleMessage.MessageLevel.values().length];

        static {
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.ERROR.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.WARNING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.TIP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static class ErrorConsoleListView extends ListView {
        private ErrorConsoleMessageList mConsoleMessages;

        private static class ErrorConsoleMessageList extends BaseAdapter implements ListAdapter {
            private LayoutInflater mInflater;
            private Vector<ConsoleMessage> mMessages = new Vector<>();

            public ErrorConsoleMessageList(Context context) {
                this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            }

            public void add(ConsoleMessage consoleMessage) {
                this.mMessages.add(consoleMessage);
                notifyDataSetChanged();
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            public void clear() {
                this.mMessages.clear();
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                return this.mMessages.size();
            }

            @Override
            public Object getItem(int i) {
                return this.mMessages.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                ConsoleMessage consoleMessage = this.mMessages.get(i);
                if (consoleMessage == null) {
                    return null;
                }
                if (view == null) {
                    view = this.mInflater.inflate(android.R.layout.two_line_list_item, viewGroup, false);
                }
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
                textView.setText(consoleMessage.sourceId() + ":" + consoleMessage.lineNumber());
                textView.setTextColor(-1);
                textView2.setText(consoleMessage.message());
                switch (AnonymousClass3.$SwitchMap$android$webkit$ConsoleMessage$MessageLevel[consoleMessage.messageLevel().ordinal()]) {
                    case 1:
                        textView2.setTextColor(-65536);
                        break;
                    case 2:
                        textView2.setTextColor(Color.rgb(255, 192, 0));
                        break;
                    case 3:
                        textView2.setTextColor(-16776961);
                        break;
                    default:
                        textView2.setTextColor(-3355444);
                        break;
                }
                return view;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public boolean isEnabled(int i) {
                return false;
            }
        }

        public ErrorConsoleListView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mConsoleMessages = new ErrorConsoleMessageList(context);
            setAdapter((ListAdapter) this.mConsoleMessages);
        }

        public void addErrorMessage(ConsoleMessage consoleMessage) {
            this.mConsoleMessages.add(consoleMessage);
            setSelection(this.mConsoleMessages.getCount());
        }

        public void clearErrorMessages() {
            this.mConsoleMessages.clear();
        }
    }

    public ErrorConsoleView(Context context) {
        super(context);
        this.mCurrentShowState = 2;
        this.mSetupComplete = false;
    }

    private void commonSetupIfNeeded() {
        if (this.mSetupComplete) {
            return;
        }
        ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(2130968598, this);
        this.mConsoleHeader = (TextView) findViewById(2131558473);
        this.mErrorList = (ErrorConsoleListView) findViewById(2131558474);
        this.mEvalJsViewGroup = (LinearLayout) findViewById(2131558475);
        this.mEvalEditText = (EditText) findViewById(2131558476);
        this.mEvalButton = (Button) findViewById(2131558477);
        this.mEvalButton.setOnClickListener(new View.OnClickListener(this) {
            final ErrorConsoleView this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                if (this.this$0.mWebView != null) {
                    HashMap map = new HashMap();
                    map.put(Browser.HEADER, Browser.UAPROF);
                    this.this$0.mWebView.loadUrl("javascript:" + ((Object) this.this$0.mEvalEditText.getText()), map);
                }
                this.this$0.mEvalEditText.setText("");
            }
        });
        this.mConsoleHeader.setOnClickListener(new View.OnClickListener(this) {
            final ErrorConsoleView this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(View view) {
                if (this.this$0.mCurrentShowState == 0) {
                    this.this$0.showConsole(1);
                } else {
                    this.this$0.showConsole(0);
                }
            }
        });
        if (this.mErrorMessageCache != null) {
            Iterator<ConsoleMessage> it = this.mErrorMessageCache.iterator();
            while (it.hasNext()) {
                this.mErrorList.addErrorMessage(it.next());
            }
            this.mErrorMessageCache.clear();
        }
        this.mSetupComplete = true;
    }

    public void addErrorMessage(ConsoleMessage consoleMessage) {
        if (this.mSetupComplete) {
            this.mErrorList.addErrorMessage(consoleMessage);
            return;
        }
        if (this.mErrorMessageCache == null) {
            this.mErrorMessageCache = new Vector<>();
        }
        this.mErrorMessageCache.add(consoleMessage);
    }

    public void clearErrorMessages() {
        if (this.mSetupComplete) {
            this.mErrorList.clearErrorMessages();
        } else if (this.mErrorMessageCache != null) {
            this.mErrorMessageCache.clear();
        }
    }

    public int getShowState() {
        if (this.mSetupComplete) {
            return this.mCurrentShowState;
        }
        return 2;
    }

    public int numberOfErrors() {
        if (this.mSetupComplete) {
            return this.mErrorList.getCount();
        }
        if (this.mErrorMessageCache == null) {
            return 0;
        }
        return this.mErrorMessageCache.size();
    }

    public void setWebView(WebView webView) {
        this.mWebView = webView;
    }

    public void showConsole(int i) {
        commonSetupIfNeeded();
        switch (i) {
            case 0:
                this.mConsoleHeader.setVisibility(0);
                this.mConsoleHeader.setText(2131493260);
                this.mErrorList.setVisibility(8);
                this.mEvalJsViewGroup.setVisibility(8);
                break;
            case 1:
                this.mConsoleHeader.setVisibility(0);
                this.mConsoleHeader.setText(2131493261);
                this.mErrorList.setVisibility(0);
                this.mEvalJsViewGroup.setVisibility(0);
                break;
            case 2:
                this.mConsoleHeader.setVisibility(8);
                this.mErrorList.setVisibility(8);
                this.mEvalJsViewGroup.setVisibility(8);
                break;
        }
        this.mCurrentShowState = i;
    }
}
