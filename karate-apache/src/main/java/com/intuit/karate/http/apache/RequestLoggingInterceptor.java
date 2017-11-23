/*
 * The MIT License
 *
 * Copyright 2017 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.http.apache;

import com.intuit.karate.FileUtils;
import com.intuit.karate.ScriptContext;
import com.intuit.karate.http.HttpRequestActual;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;

/**
 *
 * @author pthomas3
 */
public class RequestLoggingInterceptor implements HttpRequestInterceptor {

    private final ScriptContext context;
    private final AtomicInteger counter;
    
    public RequestLoggingInterceptor(AtomicInteger counter, ScriptContext context) {
        this.context = context;
        this.counter = counter;
    }      

    @Override
    public void process(HttpRequest request, HttpContext httpContext) throws HttpException, IOException {
        HttpRequestActual actual = new HttpRequestActual();
        int id = counter.incrementAndGet();
        String uri = (String) httpContext.getAttribute(ApacheHttpClient.URI_CONTEXT_KEY);
        String method = request.getRequestLine().getMethod();
        actual.setUri(uri);        
        actual.setMethod(method);
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(id).append(" > ").append(method).append(' ').append(uri).append('\n');
        LoggingUtils.logHeaders(sb, id, '>', request, actual);
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
            HttpEntity entity = entityRequest.getEntity();
            if (LoggingUtils.isPrintable(entity)) {
                LoggingEntityWrapper wrapper = new LoggingEntityWrapper(entity); // todo optimize, preserve if stream
                if (context.logger.isDebugEnabled()) {
                    String buffer = FileUtils.toString(wrapper.getContent());
                    sb.append(buffer).append('\n');
                }
                actual.setBody(wrapper.getBytes());
                entityRequest.setEntity(wrapper);
            }
        }
        context.setLastRequest(actual);
        if (context.logger.isDebugEnabled()) {
            context.logger.debug(sb.toString());
        }
    }

}
