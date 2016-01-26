/*
 * Copyright (C) 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fathom.x509;

import com.google.common.base.Strings;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Utility class to generate X509 certificates, keystores, and truststores.
 *
 * @author James Moger
 */
public class X509Utils {

    public static final String SERVER_KEY_STORE = "serverKeyStore.jks";

    public static final String SERVER_TRUST_STORE = "serverTrustStore.jks";

    public static final String CERTS = "certs";

    public static final String CA_KEY_STORE = "certs/caKeyStore.p12";

    public static final String CA_REVOCATION_LIST = "certs/caRevocationList.crl";

    public static final String CA_CN = "Fathom Certificate Authority";

    public static final String CA_ALIAS = CA_CN;

    private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    private static final int KEY_LENGTH = 2048;

    private static final String KEY_ALGORITHM = "RSA";

    private static final String SIGNING_ALGORITHM = "SHA512withRSA";

    public static final boolean unlimitedStrength;

    private static final Logger logger = LoggerFactory.getLogger(X509Utils.class);

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // check for JCE Unlimited Strength
        int maxKeyLen = 0;
        try {
            maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
        } catch (NoSuchAlgorithmException e) {
        }

        unlimitedStrength = maxKeyLen > 128;
        if (unlimitedStrength) {
            logger.info("Using JCE Unlimited Strength Jurisdiction Policy files");
        } else {
            logger.info("Using JCE Standard Encryption Policy files, encryption key lengths will be limited");
        }
    }

    public static enum RevocationReason {
        // https://en.wikipedia.org/wiki/Revocation_list
        unspecified, keyCompromise, caCompromise, affiliationChanged, superseded,
        cessationOfOperation, certificateHold, unused, removeFromCRL, privilegeWithdrawn,
        ACompromise;

        public static RevocationReason[] reasons = {
                unspecified, keyCompromise, caCompromise,
                affiliationChanged, superseded, cessationOfOperation,
                privilegeWithdrawn};

        @Override
        public String toString() {
            return name() + " (" + ordinal() + ")";
        }
    }

    public interface X509Log {
        void log(String message);
    }

    public static class X509Metadata {

        // map for distinguished name OIDs
        public final Map<String, String> oids;

        // CN in distingiushed name
        public final String commonName;

        // password for store
        public final String password;

        // password hint for README in bundle
        public String passwordHint;

        // E or EMAILADDRESS in distinguished name
        public String emailAddress;

        // start date of generated certificate
        public Date notBefore;

        // expiration date of generated certificate
        public Date notAfter;

        // hostname of server for which certificate is generated
        public String serverHostname;

        // displayname of user for README in bundle
        public String userDisplayname;

        // serialnumber of generated or read certificate
        public String serialNumber;

        public X509Metadata(String cn, String pwd) {
            this(cn, pwd, 1);
        }

        public X509Metadata(String cn, String pwd, int validYears) {
            if (Strings.isNullOrEmpty(cn)) {
                throw new RuntimeException("Common name required!");
            }
            if (Strings.isNullOrEmpty(pwd)) {
                throw new RuntimeException("Password required!");
            }

            commonName = cn;
            password = pwd;
            Calendar c = Calendar.getInstance(TimeZone.getDefault());
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            notBefore = c.getTime();
            c.add(Calendar.YEAR, validYears);
            c.add(Calendar.DATE, 1);
            notAfter = c.getTime();
            oids = new HashMap<String, String>();
        }

        public X509Metadata clone(String commonName, String password) {
            X509Metadata clone = new X509Metadata(commonName, password);
            clone.emailAddress = emailAddress;
            clone.notBefore = notBefore;
            clone.notAfter = notAfter;
            clone.oids.putAll(oids);
            clone.passwordHint = passwordHint;
            clone.serverHostname = serverHostname;
            clone.userDisplayname = userDisplayname;
            return clone;
        }

        public String getOID(String oid, String defaultValue) {
            if (oids.containsKey(oid)) {
                return oids.get(oid);
            }
            return defaultValue;
        }

        public void setOID(String oid, String value) {
            if (Strings.isNullOrEmpty(value)) {
                oids.remove(oid);
            } else {
                oids.put(oid, value);
            }
        }
    }

    /**
     * Prepare all the certificates and stores necessary for a Fathom server.
     *
     * @param metadata
     * @param folder
     */
    public static void prepareX509Infrastructure(X509Metadata metadata, File folder) {
        prepareX509Infrastructure(metadata, folder, message -> {
        });
    }

    /**
     * Prepare all the certificates and stores necessary for a Fathom server.
     *
     * @param metadata
     * @param folder
     * @param x509log
     */
    public static void prepareX509Infrastructure(X509Metadata metadata, File folder, X509Log x509log) {
        // make the specified folder, if necessary
        folder.mkdirs();

        // Fathom CA certificate
        File caKeyStore = new File(folder, CA_KEY_STORE);
        if (!caKeyStore.exists()) {
            logger.info(MessageFormat.format("Generating {0} ({1})", CA_CN, caKeyStore.getAbsolutePath()));
            X509Certificate caCert = newCertificateAuthority(metadata, caKeyStore, x509log);
            saveCertificate(caCert, new File(caKeyStore.getParentFile(), "ca.cer"));
        }

        // Fathom CRL
        File caRevocationList = new File(folder, CA_REVOCATION_LIST);
        if (!caRevocationList.exists()) {
            logger.info(MessageFormat.format("Generating {0} CRL ({1})", CA_CN, caRevocationList.getAbsolutePath()));
            newCertificateRevocationList(caRevocationList, caKeyStore, metadata.password);
            x509log.log("new certificate revocation list created");
        }

        // create web SSL certificate signed by CA
        File serverKeyStore = new File(folder, SERVER_KEY_STORE);
        if (!serverKeyStore.exists()) {
            logger.info(MessageFormat.format("Generating SSL certificate for {0} signed by {1} ({2})", metadata.commonName, CA_CN, serverKeyStore.getAbsolutePath()));
            PrivateKey caPrivateKey = getPrivateKey(CA_ALIAS, caKeyStore, metadata.password);
            X509Certificate caCert = getCertificate(CA_ALIAS, caKeyStore, metadata.password);
            newSSLCertificate(metadata, caPrivateKey, caCert, serverKeyStore, x509log);
        }

        // server certificate trust store holds trusted public certificates
        File serverTrustStore = new File(folder, X509Utils.SERVER_TRUST_STORE);
        if (!serverTrustStore.exists()) {
            logger.info(MessageFormat.format("Importing {0} into trust store ({1})", CA_ALIAS, serverTrustStore.getAbsolutePath()));
            X509Certificate caCert = getCertificate(CA_ALIAS, caKeyStore, metadata.password);
            addTrustedCertificate(CA_ALIAS, caCert, serverTrustStore, metadata.password);
        }
    }

    /**
     * Open a keystore.  Store type is determined by file extension of name. If
     * undetermined, JKS is assumed.  The keystore does not need to exist.
     *
     * @param storeFile
     * @param storePassword
     * @return a KeyStore
     */
    public static KeyStore openKeyStore(File storeFile, String storePassword) {
        String lc = storeFile.getName().toLowerCase();
        String type = "JKS";
        String provider = null;
        if (lc.endsWith(".p12") || lc.endsWith(".pfx")) {
            type = "PKCS12";
            provider = BC;
        }

        try {
            KeyStore store;
            if (provider == null) {
                store = KeyStore.getInstance(type);
            } else {
                store = KeyStore.getInstance(type, provider);
            }
            if (storeFile.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(storeFile);
                    store.load(fis, storePassword.toCharArray());
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            } else {
                store.load(null);
            }
            return store;
        } catch (Exception e) {
            throw new RuntimeException("Could not open keystore " + storeFile, e);
        }
    }

    /**
     * Saves the keystore to the specified file.
     *
     * @param targetStoreFile
     * @param store
     * @param password
     */
    public static void saveKeyStore(File targetStoreFile, KeyStore store, String password) {
        File folder = targetStoreFile.getAbsoluteFile().getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File tmpFile = new File(folder, Long.toHexString(System.currentTimeMillis()) + ".tmp");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpFile);
            store.store(fos, password.toCharArray());
            fos.flush();
            fos.close();
            if (targetStoreFile.exists()) {
                targetStoreFile.delete();
            }
            tmpFile.renameTo(targetStoreFile);
        } catch (IOException e) {
            String message = e.getMessage().toLowerCase();
            if (message.contains("illegal key size")) {
                throw new RuntimeException("Illegal Key Size! You might consider installing the JCE Unlimited Strength Jurisdiction Policy files for your JVM.");
            } else {
                throw new RuntimeException("Could not save keystore " + targetStoreFile, e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not save keystore " + targetStoreFile, e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }

            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Retrieves the X509 certificate with the specified alias from the certificate
     * store.
     *
     * @param alias
     * @param storeFile
     * @param storePassword
     * @return the certificate
     */
    public static X509Certificate getCertificate(String alias, File storeFile, String storePassword) {
        try {
            KeyStore store = openKeyStore(storeFile, storePassword);
            X509Certificate caCert = (X509Certificate) store.getCertificate(alias);
            return caCert;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the private key for the specified alias from the certificate
     * store.
     *
     * @param alias
     * @param storeFile
     * @param storePassword
     * @return the private key
     */
    public static PrivateKey getPrivateKey(String alias, File storeFile, String storePassword) {
        try {
            KeyStore store = openKeyStore(storeFile, storePassword);
            PrivateKey key = (PrivateKey) store.getKey(alias, storePassword.toCharArray());
            return key;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the certificate to the file system.  If the destination filename
     * ends with the pem extension, the certificate is written in the PEM format,
     * otherwise the certificate is written in the DER format.
     *
     * @param cert
     * @param targetFile
     */
    public static void saveCertificate(X509Certificate cert, File targetFile) {
        File folder = targetFile.getAbsoluteFile().getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File tmpFile = new File(folder, Long.toHexString(System.currentTimeMillis()) + ".tmp");
        try {
            boolean asPem = targetFile.getName().toLowerCase().endsWith(".pem");
            if (asPem) {
                // PEM encoded X509
                PEMWriter pemWriter = null;
                try {
                    pemWriter = new PEMWriter(new FileWriter(tmpFile));
                    pemWriter.writeObject(cert);
                    pemWriter.flush();
                } finally {
                    if (pemWriter != null) {
                        pemWriter.close();
                    }
                }
            } else {
                // DER encoded X509
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(tmpFile);
                    fos.write(cert.getEncoded());
                    fos.flush();
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            }

            // rename tmp file to target
            if (targetFile.exists()) {
                targetFile.delete();
            }
            tmpFile.renameTo(targetFile);
        } catch (Exception e) {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            throw new RuntimeException("Failed to save certificate " + cert.getSubjectX500Principal().getName(), e);
        }
    }

    /**
     * Generate a new keypair.
     *
     * @return a keypair
     * @throws Exception
     */
    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC);
        kpGen.initialize(KEY_LENGTH, new SecureRandom());
        return kpGen.generateKeyPair();
    }

    /**
     * Builds a distinguished name from the X509Metadata.
     *
     * @return a DN
     */
    private static X500Name buildDistinguishedName(X509Metadata metadata) {
        X500NameBuilder dnBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        setOID(dnBuilder, metadata, "C", null);
        setOID(dnBuilder, metadata, "ST", null);
        setOID(dnBuilder, metadata, "L", null);
        setOID(dnBuilder, metadata, "O", "Fathom");
        setOID(dnBuilder, metadata, "OU", "Fathom");
        setOID(dnBuilder, metadata, "E", metadata.emailAddress);
        setOID(dnBuilder, metadata, "CN", metadata.commonName);
        X500Name dn = dnBuilder.build();
        return dn;
    }

    private static void setOID(X500NameBuilder dnBuilder, X509Metadata metadata,
                               String oid, String defaultValue) {

        String value = null;
        if (metadata.oids != null && metadata.oids.containsKey(oid)) {
            value = metadata.oids.get(oid);
        }
        if (Strings.isNullOrEmpty(value)) {
            value = defaultValue;
        }

        if (!Strings.isNullOrEmpty(value)) {
            try {
                Field field = BCStyle.class.getField(oid);
                ASN1ObjectIdentifier objectId = (ASN1ObjectIdentifier) field.get(null);
                dnBuilder.addRDN(objectId, value);
            } catch (Exception e) {
                logger.error(MessageFormat.format("Failed to set OID \"{0}\"!", oid), e);
            }
        }
    }

    /**
     * Creates a new SSL certificate signed by the CA private key and stored in
     * keyStore.
     *
     * @param sslMetadata
     * @param caPrivateKey
     * @param caCert
     * @param targetStoreFile
     * @param x509log
     */
    public static X509Certificate newSSLCertificate(X509Metadata sslMetadata, PrivateKey caPrivateKey, X509Certificate caCert, File targetStoreFile, X509Log x509log) {
        try {
            KeyPair pair = newKeyPair();

            X500Name webDN = buildDistinguishedName(sslMetadata);
            X500Name issuerDN = new X500Name(PrincipalUtil.getIssuerX509Principal(caCert).getName());

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuerDN,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    sslMetadata.notBefore,
                    sslMetadata.notAfter,
                    webDN,
                    pair.getPublic());

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            certBuilder.addExtension(X509Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(pair.getPublic()));
            certBuilder.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(false));
            certBuilder.addExtension(X509Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCert.getPublicKey()));

            // support alternateSubjectNames for SSL certificates
            List<GeneralName> altNames = new ArrayList<GeneralName>();
            if (isIpAddress(sslMetadata.commonName)) {
                altNames.add(new GeneralName(GeneralName.iPAddress, sslMetadata.commonName));
            }
            if (altNames.size() > 0) {
                GeneralNames subjectAltName = new GeneralNames(altNames.toArray(new GeneralName[altNames.size()]));
                certBuilder.addExtension(X509Extension.subjectAlternativeName, false, subjectAltName);
            }

            ContentSigner caSigner = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                    .setProvider(BC).build(caPrivateKey);
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
                    .getCertificate(certBuilder.build(caSigner));

            cert.checkValidity(new Date());
            cert.verify(caCert.getPublicKey());

            // Save to keystore
            KeyStore serverStore = openKeyStore(targetStoreFile, sslMetadata.password);
            serverStore.setKeyEntry(sslMetadata.commonName, pair.getPrivate(), sslMetadata.password.toCharArray(),
                    new Certificate[]{cert, caCert});
            saveKeyStore(targetStoreFile, serverStore, sslMetadata.password);

            x509log.log(MessageFormat.format("New SSL certificate {0,number,0} [{1}]", cert.getSerialNumber(), cert.getSubjectDN().getName()));

            // update serial number in metadata object
            sslMetadata.serialNumber = cert.getSerialNumber().toString();

            return cert;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to generate SSL certificate!", t);
        }
    }

    /**
     * Creates a new certificate authority PKCS#12 store.  This function will
     * destroy any existing CA store.
     *
     * @param metadata
     * @param storeFile
     * @param x509log
     * @return
     */
    public static X509Certificate newCertificateAuthority(X509Metadata metadata, File storeFile, X509Log x509log) {
        try {
            KeyPair caPair = newKeyPair();

            ContentSigner caSigner = new JcaContentSignerBuilder(SIGNING_ALGORITHM).setProvider(BC).build(caPair.getPrivate());

            // clone metadata
            X509Metadata caMetadata = metadata.clone(CA_CN, metadata.password);
            X500Name issuerDN = buildDistinguishedName(caMetadata);

            // Generate self-signed certificate
            X509v3CertificateBuilder caBuilder = new JcaX509v3CertificateBuilder(
                    issuerDN,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    caMetadata.notBefore,
                    caMetadata.notAfter,
                    issuerDN,
                    caPair.getPublic());

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            caBuilder.addExtension(X509Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(caPair.getPublic()));
            caBuilder.addExtension(X509Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caPair.getPublic()));
            caBuilder.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(true));
            caBuilder.addExtension(X509Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));

            JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider(BC);
            X509Certificate cert = converter.getCertificate(caBuilder.build(caSigner));

            // confirm the validity of the CA certificate
            cert.checkValidity(new Date());
            cert.verify(cert.getPublicKey());

            // Delete existing keystore
            if (storeFile.exists()) {
                storeFile.delete();
            }

            // Save private key and certificate to new keystore
            KeyStore store = openKeyStore(storeFile, caMetadata.password);
            store.setKeyEntry(CA_ALIAS, caPair.getPrivate(), caMetadata.password.toCharArray(),
                    new Certificate[]{cert});
            saveKeyStore(storeFile, store, caMetadata.password);

            x509log.log(MessageFormat.format("New CA certificate {0,number,0} [{1}]", cert.getSerialNumber(), cert.getIssuerDN().getName()));

            // update serial number in metadata object
            caMetadata.serialNumber = cert.getSerialNumber().toString();

            return cert;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to generate Fathom CA certificate!", t);
        }
    }

    /**
     * Creates a new certificate revocation list (CRL).  This function will
     * destroy any existing CRL file.
     *
     * @param caRevocationList
     * @param caKeystoreFile
     * @param caKeystorePassword
     * @return
     */
    public static void newCertificateRevocationList(File caRevocationList, File caKeystoreFile, String caKeystorePassword) {
        try {
            // read the Fathom CA key and certificate
            KeyStore store = openKeyStore(caKeystoreFile, caKeystorePassword);
            PrivateKey caPrivateKey = (PrivateKey) store.getKey(CA_ALIAS, caKeystorePassword.toCharArray());
            X509Certificate caCert = (X509Certificate) store.getCertificate(CA_ALIAS);

            X500Name issuerDN = new X500Name(PrincipalUtil.getIssuerX509Principal(caCert).getName());
            X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(issuerDN, new Date());

            // build and sign CRL with CA private key
            ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).setProvider(BC).build(caPrivateKey);
            X509CRLHolder crl = crlBuilder.build(signer);

            File tmpFile = new File(caRevocationList.getParentFile(), Long.toHexString(System.currentTimeMillis()) + ".tmp");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(tmpFile);
                fos.write(crl.getEncoded());
                fos.flush();
                fos.close();
                if (caRevocationList.exists()) {
                    caRevocationList.delete();
                }
                tmpFile.renameTo(caRevocationList);
            } finally {
                if (fos != null) {
                    fos.close();
                }
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new certificate revocation list " + caRevocationList, e);
        }
    }

    /**
     * Imports a certificate into the trust store.
     *
     * @param alias
     * @param cert
     * @param storeFile
     * @param storePassword
     */
    public static void addTrustedCertificate(String alias, X509Certificate cert, File storeFile, String storePassword) {
        try {
            KeyStore store = openKeyStore(storeFile, storePassword);
            store.setCertificateEntry(alias, cert);
            saveKeyStore(storeFile, store, storePassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import certificate into trust store " + storeFile, e);
        }
    }

    /**
     * Creates a new client certificate PKCS#12 and PEM store.  Any existing
     * stores are destroyed.
     *
     * @param clientMetadata a container for dynamic parameters needed for generation
     * @param caPrivateKey
     * @param caCert
     * @param targetFolder
     * @return
     */
    public static X509Certificate newClientCertificate(X509Metadata clientMetadata,
                                                       PrivateKey caPrivateKey, X509Certificate caCert, File targetFolder) {
        try {
            KeyPair pair = newKeyPair();

            X500Name userDN = buildDistinguishedName(clientMetadata);
            X500Name issuerDN = new X500Name(PrincipalUtil.getIssuerX509Principal(caCert).getName());

            // create a new certificate signed by the Fathom CA certificate
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuerDN,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    clientMetadata.notBefore,
                    clientMetadata.notAfter,
                    userDN,
                    pair.getPublic());

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            certBuilder.addExtension(X509Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(pair.getPublic()));
            certBuilder.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(false));
            certBuilder.addExtension(X509Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCert.getPublicKey()));
            certBuilder.addExtension(X509Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.digitalSignature));
            if (!Strings.isNullOrEmpty(clientMetadata.emailAddress)) {
                GeneralNames subjectAltName = new GeneralNames(
                        new GeneralName(GeneralName.rfc822Name, clientMetadata.emailAddress));
                certBuilder.addExtension(X509Extension.subjectAlternativeName, false, subjectAltName);
            }

            ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).setProvider(BC).build(caPrivateKey);

            X509Certificate userCert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certBuilder.build(signer));
            PKCS12BagAttributeCarrier bagAttr = (PKCS12BagAttributeCarrier) pair.getPrivate();
            bagAttr.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId,
                    extUtils.createSubjectKeyIdentifier(pair.getPublic()));

            // confirm the validity of the user certificate
            userCert.checkValidity();
            userCert.verify(caCert.getPublicKey());
            userCert.getIssuerDN().equals(caCert.getSubjectDN());

            // verify user certificate chain
            verifyChain(userCert, caCert);

            targetFolder.mkdirs();

            // save certificate, stamped with unique name
            String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String id = date;
            File certFile = new File(targetFolder, id + ".cer");
            int count = 0;
            while (certFile.exists()) {
                id = date + "_" + Character.toString((char) (0x61 + count));
                certFile = new File(targetFolder, id + ".cer");
                count++;
            }

            // save user private key, user certificate and CA certificate to a PKCS#12 store
            File p12File = new File(targetFolder, clientMetadata.commonName + ".p12");
            if (p12File.exists()) {
                p12File.delete();
            }
            KeyStore userStore = openKeyStore(p12File, clientMetadata.password);
            userStore.setKeyEntry(MessageFormat.format("Fathom ({0}) {1} {2}", clientMetadata.serverHostname, clientMetadata.userDisplayname, id), pair.getPrivate(), null, new Certificate[]{userCert});
            userStore.setCertificateEntry(MessageFormat.format("Fathom ({0}) Certificate Authority", clientMetadata.serverHostname), caCert);
            saveKeyStore(p12File, userStore, clientMetadata.password);

            // save user private key, user certificate, and CA certificate to a PEM store
            File pemFile = new File(targetFolder, clientMetadata.commonName + ".pem");
            if (pemFile.exists()) {
                pemFile.delete();
            }
            PEMWriter pemWriter = new PEMWriter(new FileWriter(pemFile));
            pemWriter.writeObject(pair.getPrivate(), "DES-EDE3-CBC", clientMetadata.password.toCharArray(), new SecureRandom());
            pemWriter.writeObject(userCert);
            pemWriter.writeObject(caCert);
            pemWriter.flush();
            pemWriter.close();

            // save certificate after successfully creating the key stores
            saveCertificate(userCert, certFile);

            // update serial number in metadata object
            clientMetadata.serialNumber = userCert.getSerialNumber().toString();

            return userCert;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to generate client certificate!", t);
        }
    }

    /**
     * Verifies a certificate's chain to ensure that it will function properly.
     *
     * @param testCert
     * @param additionalCerts
     * @return
     */
    public static PKIXCertPathBuilderResult verifyChain(X509Certificate testCert, X509Certificate... additionalCerts) {
        try {
            // Check for self-signed certificate
            if (isSelfSigned(testCert)) {
                throw new RuntimeException("The certificate is self-signed.  Nothing to verify.");
            }

            // Prepare a set of all certificates
            // chain builder must have all certs, including cert to validate
            // http://stackoverflow.com/a/10788392
            Set<X509Certificate> certs = new HashSet<X509Certificate>();
            certs.add(testCert);
            certs.addAll(Arrays.asList(additionalCerts));

            // Attempt to build the certification chain and verify it
            // Create the selector that specifies the starting certificate
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(testCert);

            // Create the trust anchors (set of root CA certificates)
            Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
            for (X509Certificate cert : additionalCerts) {
                if (isSelfSigned(cert)) {
                    trustAnchors.add(new TrustAnchor(cert, null));
                }
            }

            // Configure the PKIX certificate builder
            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);
            pkixParams.setRevocationEnabled(false);
            pkixParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certs), BC));

            // Build and verify the certification chain
            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", BC);
            PKIXCertPathBuilderResult verifiedCertChain = (PKIXCertPathBuilderResult) builder.build(pkixParams);

            // The chain is built and verified
            return verifiedCertChain;
        } catch (CertPathBuilderException e) {
            throw new RuntimeException("Error building certification path: " + testCert.getSubjectX500Principal(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying the certificate: " + testCert.getSubjectX500Principal(), e);
        }
    }

    /**
     * Checks whether given X.509 certificate is self-signed.
     *
     * @param cert
     * @return true if the certificate is self-signed
     */
    public static boolean isSelfSigned(X509Certificate cert) {
        try {
            cert.verify(cert.getPublicKey());
            return true;
        } catch (SignatureException e) {
            return false;
        } catch (InvalidKeyException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Metadata getMetadata(X509Certificate cert) {
        // manually split DN into OID components
        // this is instead of parsing with LdapName which:
        // (1) I don't trust the order of values
        // (2) it filters out values like EMAILADDRESS
        String dn = cert.getSubjectDN().getName();
        Map<String, String> oids = new HashMap<String, String>();
        for (String kvp : dn.split(",")) {
            String[] val = kvp.trim().split("=");
            String oid = val[0].toUpperCase().trim();
            String data = val[1].trim();
            oids.put(oid, data);
        }

        X509Metadata metadata = new X509Metadata(oids.get("CN"), "whocares");
        metadata.oids.putAll(oids);
        metadata.serialNumber = cert.getSerialNumber().toString();
        metadata.notAfter = cert.getNotAfter();
        metadata.notBefore = cert.getNotBefore();
        metadata.emailAddress = metadata.getOID("E", null);
        if (metadata.emailAddress == null) {
            metadata.emailAddress = metadata.getOID("EMAILADDRESS", null);
        }
        return metadata;
    }

    public static boolean isIpAddress(String address) {
        if (Strings.isNullOrEmpty(address)) {
            return false;
        }
        String[] fields = address.split("\\.");
        if (fields.length == 4) {
            // IPV4
            for (String field : fields) {
                try {
                    int value = Integer.parseInt(field);
                    if (value < 0 || value > 255) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }
        // TODO IPV6?
        return false;
    }
}
