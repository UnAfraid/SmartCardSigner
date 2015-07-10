package com.github.unafraid.signer.signer.model;

/**
 * Created by Svetlin Nakov on 10.7.2005 г..
 * Data structure that holds a pair of Base64-encoded
 * certification chain and digital signature.
 */
public class SignedDocument {
    private String _certificationChain = null;
    private String _signature = null;

    public SignedDocument(String certificationChain, String signature) {
        _certificationChain = certificationChain;
        _signature = signature;
    }

    public String getCertificationChain() {
        return _certificationChain;
    }

    public String getSignature() {
        return _signature;
    }
}