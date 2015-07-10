package com.github.unafraid.signer.signer.model;

/**
 * Created by Svetlin Nakov on 10.7.2005 г..
 * Exception class used for document signing errors.
 */
public class DocumentSignException extends Exception {
    public DocumentSignException(String aMessage) {
        super(aMessage);
    }

    public DocumentSignException(String aMessage, Throwable aCause) {
        super(aMessage, aCause);
    }
}