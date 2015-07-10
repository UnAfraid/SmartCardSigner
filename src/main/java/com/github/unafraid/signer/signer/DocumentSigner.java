package com.github.unafraid.signer.signer;

import com.github.unafraid.signer.signer.model.DocumentSignException;
import com.github.unafraid.signer.signer.model.PrivateKeyAndCertChain;
import com.github.unafraid.signer.signer.model.SignedDocument;
import sun.security.pkcs11.SunPKCS11;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Svetlin Nakov on 10.7.2005 year..
 */
public class DocumentSigner {
    private static final String PKCS11_KEYSTORE_TYPE = "PKCS11";
    private static final String X509_CERTIFICATE_TYPE = "X.509";
    private static final String CERTIFICATION_CHAIN_ENCODING = "PkiPath";
    private static final String DIGITAL_SIGNATURE_ALGORITHM_NAME = "SHA1withRSA";

    public SignedDocument sign(byte[] data, String libraryPath, String pin) throws DocumentSignException {
        if (libraryPath == null || !new File(libraryPath).isFile()) {
            throw new DocumentSignException("It is mandatory to choose a PCKS#11 native implementation library for for smart card (.dll or .so file)!");
        }

        // Load the keystore from the smart card using the specified PIN code
        final KeyStore userKeyStore;
        try {
            userKeyStore = loadKeyStoreFromSmartCard(libraryPath, pin);
        } catch (Exception e) {
            throw new DocumentSignException("Can not read the keystore from the smart card.\n" +
                    "Possible reasons:\n" +
                    " - The smart card reader in not connected.\n" +
                    " - The smart card is not inserted.\n" +
                    " - The PKCS#11 implementation library is invalid.\n" +
                    " - The PIN for the smart card is incorrect.\n" +
                    "Problem details: " + e.getMessage(), e);
        }

        // Get the private key and its certification chain from the keystore
        final PrivateKeyAndCertChain privateKeyAndCertChain;
        try {
            privateKeyAndCertChain = getPrivateKeyAndCertChain(userKeyStore);
        } catch (GeneralSecurityException e) {
            throw new DocumentSignException("Can not extract the private key and certificate from the smart card. Reason: " + e.getMessage(), e);
        }

        // Check if the private key is available
        final PrivateKey privateKey = privateKeyAndCertChain.mPrivateKey;
        if (privateKey == null) {
            throw new DocumentSignException("Can not find the private key on the smart card.");
        }

        // Check if X.509 certification chain is available
        final Certificate[] certChain = privateKeyAndCertChain.mCertificationChain;
        if (certChain == null) {
            throw new DocumentSignException("Can not find the certificate on the smart card.");
        }

        // Save X.509 certification chain in the result encoded in Base64
        final String certificationChain;
        try {
            certificationChain = encodeX509CertChainToBase64(certChain);
        } catch (CertificateException e) {
            throw new DocumentSignException("Invalid certificate on the smart card.");
        }

        // Calculate the digital signature of the file, encode it in Base64 and save it in the result
        final String signature;
        try {
            byte[] digitalSignature = signDocument(data, privateKey);
            signature = new String(Base64.getEncoder().encode(digitalSignature));
        } catch (GeneralSecurityException e) {
            throw new DocumentSignException("File signing failed.\nProblem details: " + e.getMessage(), e);
        }

        // Create the result object
        return new SignedDocument(certificationChain, signature);
    }

    /**
     * Loads the keystore from the smart card using its PKCS#11 implementation
     * library and the Sun PKCS#11 security provider. The PIN code for accessing
     * the smart card is required.
     */
    private KeyStore loadKeyStoreFromSmartCard(String aPKCS11LibraryFileName, String aSmartCardPIN) throws GeneralSecurityException, IOException {
        // First configure the Sun PKCS#11 provider. It requires a stream (or file)
        // containing the configuration parameters - "name" and "library".
        final byte[] pkcs11ConfigBytes = ("name = SmartCard\n" + "library = " + aPKCS11LibraryFileName).getBytes();

        // Instantiate the provider dynamically with Java reflection
        try (ByteArrayInputStream confStream = new ByteArrayInputStream(pkcs11ConfigBytes)) {
            Security.addProvider(new SunPKCS11(confStream));
        } catch (Exception e) {
            throw new KeyStoreException("Can initialize Sun PKCS#11 security provider. Reason: " + e.getCause().getMessage());
        }

        // Read the keystore form the smart card
        final char[] pin = aSmartCardPIN.toCharArray();
        final KeyStore keyStore = KeyStore.getInstance(PKCS11_KEYSTORE_TYPE);
        keyStore.load(null, pin);
        return keyStore;
    }

    /**
     * @return private key and certification chain corresponding to it, extracted from
     * given keystore. The keystore is considered to have only one entry that contains
     * both certification chain and its corresponding private key. If the keystore has
     * no entries, an exception is thrown.
     */
    private PrivateKeyAndCertChain getPrivateKeyAndCertChain(KeyStore aKeyStore) throws GeneralSecurityException {
        final Enumeration aliasesEnum = aKeyStore.aliases();
        if (aliasesEnum.hasMoreElements()) {
            final String alias = (String) aliasesEnum.nextElement();
            final Certificate[] certificationChain = aKeyStore.getCertificateChain(alias);
            final PrivateKey privateKey = (PrivateKey) aKeyStore.getKey(alias, null);
            final PrivateKeyAndCertChain result = new PrivateKeyAndCertChain();
            result.mPrivateKey = privateKey;
            result.mCertificationChain = certificationChain;
            return result;
        } else {
            throw new KeyStoreException("The keystore is empty!");
        }
    }

    /**
     * @return Base64-encoded ASN.1 DER representation of given X.509 certification
     * chain.
     */
    private String encodeX509CertChainToBase64(Certificate[] aCertificationChain) throws CertificateException {
        final List<Certificate> certList = Arrays.asList(aCertificationChain);
        final CertificateFactory certFactory = CertificateFactory.getInstance(X509_CERTIFICATE_TYPE);
        final CertPath certPath = certFactory.generateCertPath(certList);
        final byte[] certPathEncoded = certPath.getEncoded(CERTIFICATION_CHAIN_ENCODING);
        return new String(Base64.getEncoder().encode(certPathEncoded));
    }

    /**
     * Signs given document with a given private key.
     */
    private byte[] signDocument(byte[] aDocument, PrivateKey aPrivateKey) throws GeneralSecurityException {
        final Signature signatureAlgorithm = Signature.getInstance(DIGITAL_SIGNATURE_ALGORITHM_NAME);
        signatureAlgorithm.initSign(aPrivateKey);
        signatureAlgorithm.update(aDocument);
        return signatureAlgorithm.sign();
    }
}
