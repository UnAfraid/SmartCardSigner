package com.github.unafraid.signer.signer;

import com.github.unafraid.signer.signer.model.DocumentSignException;
import com.github.unafraid.signer.signer.model.PrivateKeyAndCertChain;
import com.github.unafraid.signer.signer.model.SignedDocument;
import com.github.unafraid.signer.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.pkcs.*;
import sun.security.pkcs11.SunPKCS11;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

/**
 * @author Svetlin Nakov, UnAfraid, Venci
 */
public class DocumentSigner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSigner.class);
    private static KeyStore KEY_STORE;
    private static String MIDDLEWARE_PATH;
    private static final String PKCS11_KEYSTORE_TYPE = "PKCS11";
    private static final String X509_CERTIFICATE_TYPE = "X.509";
    private static final String CERTIFICATION_CHAIN_ENCODING = "PkiPath";
    private static final String DIGITAL_SIGNATURE_ALGORITHM_NAME = "SHA1withRSA";
    protected static volatile MessageDigest md = null;

    public static synchronized void init(String middlewarePath, String pinCode) throws Exception {
        MIDDLEWARE_PATH = middlewarePath;
        KEY_STORE = getKeystore();
        KEY_STORE.load(null, pinCode.toCharArray());
        final Enumeration<String> aliases = KEY_STORE.aliases();
        if (!aliases.hasMoreElements()) {
            throw new Error("No certificates available!");
        }
    }

    public static SignedDocument sign(byte[] data) throws DocumentSignException {
        if (MIDDLEWARE_PATH == null || !new File(MIDDLEWARE_PATH).isFile()) {
            throw new DocumentSignException(
                    "It is mandatory to choose a PCKS#11 native implementation library for for smart card (.dll or .so file)!");
        }

        final byte[] hash = sha1(data);

        // Load the keystore from the smart card using the specified PIN code
        final KeyStore userKeyStore;
        try {
            userKeyStore = getKeystore();
        } catch (Exception e) {
            throw new DocumentSignException(
                    "Can not read the keystore from the smart card.\n" + "Possible reasons:\n"
                            + " - The smart card reader in not connected.\n" + " - The smart card is not inserted.\n"
                            + " - The PKCS#11 implementation library is invalid.\n"
                            + " - The PIN for the smart card is incorrect.\n" + "Problem details: " + e.getMessage(),
                    e);
        }

        // Get the private key and its certification chain from the keystore
        final PrivateKeyAndCertChain privateKeyAndCertChain;
        try {
            privateKeyAndCertChain = getPrivateKeyAndCertChain(userKeyStore);
        } catch (GeneralSecurityException e) {
            throw new DocumentSignException(
                    "Can not extract the private key and certificate from the smart card. Reason: " + e.getMessage(),
                    e);
        }

        // Check if the private key is available
        final PrivateKey privateKey = privateKeyAndCertChain.mPrivateKey;
        if (privateKey == null) {
            throw new DocumentSignException("Can not find the private key on the smart card.");
        }

        // Check if X.509 certificate is available
        final X509Certificate cert = privateKeyAndCertChain.mCertificate;
        if (cert == null) {
            throw new DocumentSignException("Can not find the certificate on the smart card.");
        }

        // Check if X.509 certification chain is available
        final X509Certificate[] certChain = privateKeyAndCertChain.mCertificationChain;
        if (certChain == null) {
            throw new DocumentSignException("Can not find the certification chain on the smart card.");
        }

        // Save X.509 certification chain in the result encoded in Base64
        final String certificationChain;
        try {
            certificationChain = encodeX509CertChainToBase64(certChain);
        } catch (CertificateException e) {
            throw new DocumentSignException("Invalid certificate on the smart card.");
        }

        // Generate crypto.signText()-compatible string
        String signedData;
        String signature = null;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024)) {
            try {
                AlgorithmId digestAlgorithmId = new AlgorithmId(AlgorithmId.SHA_oid);
                AlgorithmId signAlgorithmId = new AlgorithmId(AlgorithmId.RSAEncryption_oid);

                // @formatter:off
                final PKCS9Attributes authenticatedAttributes = new PKCS9Attributes(new PKCS9Attribute[] {
					new PKCS9Attribute(PKCS9Attribute.CONTENT_TYPE_OID, ContentInfo.DATA_OID),
					new PKCS9Attribute(PKCS9Attribute.MESSAGE_DIGEST_OID, hash),
					new PKCS9Attribute(PKCS9Attribute.SIGNING_TIME_OID, new Date()),
				});

				// Calculate the digital signature of the file, encode it in Base64 and
				// save it in the result
				byte[] digitalSignature;
				try {
					digitalSignature = signDocument(authenticatedAttributes.getDerEncoding(), privateKey);
					signature = new String(Base64.getEncoder().encode(digitalSignature));
				} catch (GeneralSecurityException e) {
					throw new DocumentSignException("File signing failed.\nProblem details: " + e.getMessage(), e);
				}

				final PKCS7 p7 = new PKCS7(
					new AlgorithmId[] { 
						digestAlgorithmId 
					},
					new ContentInfo(ContentInfo.DATA_OID, null),
                    certChain,
					new SignerInfo[] { 
						new SignerInfo(
							X500Name.asX500Name(cert.getIssuerX500Principal()),
							cert.getSerialNumber(), 
							digestAlgorithmId, 
							authenticatedAttributes, 
							signAlgorithmId,
							digitalSignature, 
							null
						)
					}
				);
				// @formatter:on
                p7.encodeSignedData(out);
            } catch (IOException e) {
                // who cares
            }

            signedData = new String(Base64.getEncoder().encode(out.toByteArray()));
        } catch (Exception e) {
            LOGGER.warn("Failed to sign text: {} ", new String(data), e);
            throw new DocumentSignException("Couldn't sign text!", e);
        }

        // Create the result object
        return new SignedDocument(certificationChain, signature, signedData);
    }

    /**
     * Loads the keystore from the smart card using its PKCS#11 implementation
     * library and the Sun PKCS#11 security provider. The PIN code for accessing
     * the smart card is required.
     */
    public static KeyStore getKeystore() throws GeneralSecurityException, IOException {
        if (KEY_STORE != null) {
            return KEY_STORE;
        }
        // First configure the Sun PKCS#11 provider. It requires a stream (or
        // file)
        // containing the configuration parameters - "name" and "library".
        final String config = String.format(IOUtils.streamToByteArray(DocumentSigner.class.getResourceAsStream("/keystore.conf")), MIDDLEWARE_PATH);

        // Instantiate the provider dynamically with Java reflection
        try (ByteArrayInputStream confStream = new ByteArrayInputStream(config.getBytes())) {
            Security.addProvider(new SunPKCS11(confStream));
        } catch (Exception e) {
            throw new KeyStoreException("Can initialize Sun PKCS#11 security provider. Reason: " + e.getCause().getMessage());
        }

        // Read the keystore form the smart card
        return KEY_STORE = KeyStore.getInstance(PKCS11_KEYSTORE_TYPE);
    }

    /**
     * @return private key and certification chain corresponding to it,
     * extracted from given keystore. The keystore is considered to have
     * only one entry that contains both certification chain and its
     * corresponding private key. If the keystore has no entries, an
     * exception is thrown.
     */
    private static PrivateKeyAndCertChain getPrivateKeyAndCertChain(KeyStore aKeyStore)
            throws GeneralSecurityException {
        final Enumeration<String> aliasesEnum = aKeyStore.aliases();
        final CertificateFactory certFactory = CertificateFactory.getInstance(X509_CERTIFICATE_TYPE);
        if (aliasesEnum.hasMoreElements()) {
            final String alias = aliasesEnum.nextElement();
            final X509Certificate certificate = (X509Certificate) aKeyStore.getCertificate(alias);
            final Certificate[] certificationChain = aKeyStore.getCertificateChain(alias);
            final X509Certificate[] x509Certificates = new X509Certificate[certificationChain.length];
            for (int i = 0; i < certificationChain.length; i++) {
                x509Certificates[i] = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certificationChain[i].getEncoded()));
            }
            final PrivateKey privateKey = (PrivateKey) aKeyStore.getKey(alias, null);
            final PrivateKeyAndCertChain result = new PrivateKeyAndCertChain();
            result.mPrivateKey = privateKey;
            result.mCertificate = certificate;
            result.mCertificationChain = x509Certificates;
            return result;
        } else {
            throw new KeyStoreException("The keystore is empty!");
        }
    }

    /**
     * @return Base64-encoded ASN.1 DER representation of given X.509
     * certification chain.
     */
    private static String encodeX509CertChainToBase64(Certificate[] aCertificationChain) throws CertificateException {
        final List<Certificate> certList = Arrays.asList(aCertificationChain);
        final CertificateFactory certFactory = CertificateFactory.getInstance(X509_CERTIFICATE_TYPE);
        final CertPath certPath = certFactory.generateCertPath(certList);
        final byte[] certPathEncoded = certPath.getEncoded(CERTIFICATION_CHAIN_ENCODING);
        return new String(Base64.getEncoder().encode(certPathEncoded));
    }

    /**
     * Signs given document with a given private key.
     */
    private static byte[] signDocument(byte[] aDocument, PrivateKey aPrivateKey) throws GeneralSecurityException {
        final Signature signatureAlgorithm = Signature.getInstance(DIGITAL_SIGNATURE_ALGORITHM_NAME);
        signatureAlgorithm.initSign(aPrivateKey);
        signatureAlgorithm.update(aDocument);
        return signatureAlgorithm.sign();
    }

    protected static byte[] sha1(byte[] input) {
        if (md == null) {
            synchronized (DocumentSigner.class) {
                if (md == null) {
                    try {
                        md = MessageDigest.getInstance("SHA-1");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }

        if (md != null) {
            return md.digest(input);
        }

        return null;
    }
}
