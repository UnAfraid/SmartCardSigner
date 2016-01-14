package com.github.unafraid.signer.signer.model;

/**
 * Created by Svetlin Nakov on 10.7.2005 г..
 * Data structure that holds a pair of Base64-encoded
 * certification chain and digital signature.
 */
public class SignedDocument {
    private String _certificationChain = null;
    private String _signature = null;
    private String _signedData = null;

    public SignedDocument(String certificationChain, String signature, String signedData) {
        _certificationChain = certificationChain;
        _signature = signature;
        _signedData = signedData;
    }

    public String getCertificationChain() {
        return _certificationChain;
    }

    public String getSignature() {
        return _signature;
    }

    public String getSignedData() {
        return _signedData;
    }
}