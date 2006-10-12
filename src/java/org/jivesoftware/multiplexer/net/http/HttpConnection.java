/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.multiplexer.net.http;

import org.jivesoftware.multiplexer.Connection;
import org.mortbay.util.ajax.Continuation;


/**
 *
 */
public class HttpConnection {
    private Connection.CompressionPolicy compressionPolicy;
    private long requestId;
    private String body;
    private HttpSession session;
    private Continuation continuation;
    private boolean isClosed;

    public HttpConnection(long requestID) {
        this.requestId = requestID;
    }

    public boolean validate() {
        return false;
    }

    /**
     * The connection should be closed without delivering a stanza to the requestor.
     */
    public void close() {
        if(isClosed) {
            return;
        }

        try {
            deliverBody(null);
        }
        catch (HttpConnectionClosedException e) {
            /* Shouldn't happen */
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isSecure() {
        return false;
    }

    public void deliverBody(String body) throws HttpConnectionClosedException {
        // We only want to use this function once so we will close it when the body is delivered.
        if(isClosed) {
            throw new HttpConnectionClosedException("The http connection is no longer " +
                    "available to deliver content");
        }
        else {
            isClosed = true;
        }

        if (continuation != null) {
            continuation.setObject(body);
            continuation.resume();
        }
        else {
            this.body = body;
        }
    }

    /**
     * A call that will cause a wait, or in the case of Jetty the thread to be freed, if there
     * is no deliverable currently available. Once the deliverable becomes available it is returned.
     *
     * @return the deliverable to send to the client
     * @throws HttpBindTimeoutException to indicate that the maximum wait time requested by the
     * client
     * has been surpassed and an empty response should be returned.
     */
    public String getDeliverable() throws HttpBindTimeoutException {
        if (body == null && continuation != null) {
            body = waitForDeliverable();
        }
        else if (body == null && continuation == null) {
            throw new IllegalStateException("Continuation not set, cannot wait for deliverable.");
        }
        return body;
    }

    private String waitForDeliverable() throws HttpBindTimeoutException {
        if(continuation.suspend(session.getWait() * 1000)) {
            String deliverable = (String) continuation.getObject();
            // This will occur when the hold attribute of a session has been exceded.
            if(deliverable == null) {
                throw new HttpBindTimeoutException();
            }
            return deliverable;
        }
        throw new HttpBindTimeoutException("Request " + requestId + " exceded response time from " +
                "server of " + session.getWait() + " seconds.");
    }

    public boolean isCompressed() {
        return false;
    }

    public Connection.CompressionPolicy getCompressionPolicy() {
        return compressionPolicy;
    }

    public void setCompressionPolicy(Connection.CompressionPolicy compressionPolicy) {
        this.compressionPolicy = compressionPolicy;
    }

    public long getRequestId() {
        return requestId;
    }

    /**
     * Set the session that this connection belongs to.
     *
     * @param session the session that this connection belongs to.
     */
    void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * Returns the session that this connection belongs to.
     *
     * @return the session that this connection belongs to.
     */
    public HttpSession getSession() {
        return session;
    }

    void setContinuation(Continuation continuation) {
        this.continuation = continuation;
    }
}
