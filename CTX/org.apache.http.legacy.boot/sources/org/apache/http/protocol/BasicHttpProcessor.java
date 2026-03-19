package org.apache.http.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;

@Deprecated
public final class BasicHttpProcessor implements HttpProcessor, HttpRequestInterceptorList, HttpResponseInterceptorList, Cloneable {
    protected List requestInterceptors = null;
    protected List responseInterceptors = null;

    @Override
    public void addRequestInterceptor(HttpRequestInterceptor httpRequestInterceptor) {
        if (httpRequestInterceptor == null) {
            return;
        }
        if (this.requestInterceptors == null) {
            this.requestInterceptors = new ArrayList();
        }
        this.requestInterceptors.add(httpRequestInterceptor);
    }

    @Override
    public void addRequestInterceptor(HttpRequestInterceptor httpRequestInterceptor, int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(i));
        }
        if (httpRequestInterceptor == null) {
            return;
        }
        if (this.requestInterceptors == null) {
            if (i > 0) {
                throw new IndexOutOfBoundsException(String.valueOf(i));
            }
            this.requestInterceptors = new ArrayList();
        }
        this.requestInterceptors.add(i, httpRequestInterceptor);
    }

    @Override
    public void addResponseInterceptor(HttpResponseInterceptor httpResponseInterceptor, int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(i));
        }
        if (httpResponseInterceptor == null) {
            return;
        }
        if (this.responseInterceptors == null) {
            if (i > 0) {
                throw new IndexOutOfBoundsException(String.valueOf(i));
            }
            this.responseInterceptors = new ArrayList();
        }
        this.responseInterceptors.add(i, httpResponseInterceptor);
    }

    @Override
    public void removeRequestInterceptorByClass(Class cls) {
        if (this.requestInterceptors == null) {
            return;
        }
        Iterator it = this.requestInterceptors.iterator();
        while (it.hasNext()) {
            if (it.next().getClass().equals(cls)) {
                it.remove();
            }
        }
    }

    @Override
    public void removeResponseInterceptorByClass(Class cls) {
        if (this.responseInterceptors == null) {
            return;
        }
        Iterator it = this.responseInterceptors.iterator();
        while (it.hasNext()) {
            if (it.next().getClass().equals(cls)) {
                it.remove();
            }
        }
    }

    public final void addInterceptor(HttpRequestInterceptor httpRequestInterceptor) {
        addRequestInterceptor(httpRequestInterceptor);
    }

    public final void addInterceptor(HttpRequestInterceptor httpRequestInterceptor, int i) {
        addRequestInterceptor(httpRequestInterceptor, i);
    }

    @Override
    public int getRequestInterceptorCount() {
        if (this.requestInterceptors == null) {
            return 0;
        }
        return this.requestInterceptors.size();
    }

    @Override
    public HttpRequestInterceptor getRequestInterceptor(int i) {
        if (this.requestInterceptors == null || i < 0 || i >= this.requestInterceptors.size()) {
            return null;
        }
        return (HttpRequestInterceptor) this.requestInterceptors.get(i);
    }

    @Override
    public void clearRequestInterceptors() {
        this.requestInterceptors = null;
    }

    @Override
    public void addResponseInterceptor(HttpResponseInterceptor httpResponseInterceptor) {
        if (httpResponseInterceptor == null) {
            return;
        }
        if (this.responseInterceptors == null) {
            this.responseInterceptors = new ArrayList();
        }
        this.responseInterceptors.add(httpResponseInterceptor);
    }

    public final void addInterceptor(HttpResponseInterceptor httpResponseInterceptor) {
        addResponseInterceptor(httpResponseInterceptor);
    }

    public final void addInterceptor(HttpResponseInterceptor httpResponseInterceptor, int i) {
        addResponseInterceptor(httpResponseInterceptor, i);
    }

    @Override
    public int getResponseInterceptorCount() {
        if (this.responseInterceptors == null) {
            return 0;
        }
        return this.responseInterceptors.size();
    }

    @Override
    public HttpResponseInterceptor getResponseInterceptor(int i) {
        if (this.responseInterceptors == null || i < 0 || i >= this.responseInterceptors.size()) {
            return null;
        }
        return (HttpResponseInterceptor) this.responseInterceptors.get(i);
    }

    @Override
    public void clearResponseInterceptors() {
        this.responseInterceptors = null;
    }

    @Override
    public void setInterceptors(List list) {
        if (list == null) {
            throw new IllegalArgumentException("List must not be null.");
        }
        if (this.requestInterceptors != null) {
            this.requestInterceptors.clear();
        }
        if (this.responseInterceptors != null) {
            this.responseInterceptors.clear();
        }
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (obj instanceof HttpRequestInterceptor) {
                addInterceptor((HttpRequestInterceptor) obj);
            }
            if (obj instanceof HttpResponseInterceptor) {
                addInterceptor((HttpResponseInterceptor) obj);
            }
        }
    }

    public void clearInterceptors() {
        clearRequestInterceptors();
        clearResponseInterceptors();
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        if (this.requestInterceptors != null) {
            for (int i = 0; i < this.requestInterceptors.size(); i++) {
                ((HttpRequestInterceptor) this.requestInterceptors.get(i)).process(httpRequest, httpContext);
            }
        }
    }

    @Override
    public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        if (this.responseInterceptors != null) {
            for (int i = 0; i < this.responseInterceptors.size(); i++) {
                ((HttpResponseInterceptor) this.responseInterceptors.get(i)).process(httpResponse, httpContext);
            }
        }
    }

    protected void copyInterceptors(BasicHttpProcessor basicHttpProcessor) {
        if (this.requestInterceptors != null) {
            basicHttpProcessor.requestInterceptors = new ArrayList(this.requestInterceptors);
        }
        if (this.responseInterceptors != null) {
            basicHttpProcessor.responseInterceptors = new ArrayList(this.responseInterceptors);
        }
    }

    public BasicHttpProcessor copy() {
        BasicHttpProcessor basicHttpProcessor = new BasicHttpProcessor();
        copyInterceptors(basicHttpProcessor);
        return basicHttpProcessor;
    }

    public Object clone() throws CloneNotSupportedException {
        BasicHttpProcessor basicHttpProcessor = (BasicHttpProcessor) super.clone();
        copyInterceptors(basicHttpProcessor);
        return basicHttpProcessor;
    }
}
