package com.github.unafraid.signer.signer.model;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @author Svetlin Nakov on 10.7.2005 г..
 * Data structure that holds a pair of private key and
 * certification chain corresponding to this private key.
 */
public class PrivateKeyAndCertChain {
    public PrivateKey mPrivateKey;
    public X509Certificate mCertificate;
    public Certificate[] mCertificationChain;
}
