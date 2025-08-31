/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.certificate.certextensions.standard;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.internal.CertificateValidity;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.certextensions.CertificateExtensionException;
import org.cesecore.certificates.certificate.certextensions.CustomCertificateExtension;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.endentity.EndEntityInformation;

import com.keyfactor.util.CertTools;

/**
 * Class for standard X509 certificate extension. 
 * See rfc5280 or later for spec of this extension.
 */
public class AuthorityKeyIdentifier extends StandardCertificateExtension implements CustomCertificateExtension {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(AuthorityKeyIdentifier.class);

    public AuthorityKeyIdentifier() {
        super.setDisplayName("AuthorityKeyIdentifier");
        
    }

    @Override
    public void init(final CertificateProfile certProf) {
        super.setOID(Extension.authorityKeyIdentifier.getId());
        super.setCriticalFlag(certProf.getAuthorityKeyIdentifierCritical());
    }

    @Override
    public ASN1Encodable getValue(final EndEntityInformation subject, final CA ca, final CertificateProfile certProfile, final PublicKey userPublicKey,
            final PublicKey caPublicKey, CertificateValidity val) throws CertificateExtensionException {
        org.bouncycastle.asn1.x509.AuthorityKeyIdentifier ret = null;
        // Default value is that we calculate it from scratch!
        // (If this is a root CA we must calculate the AuthorityKeyIdentifier from scratch)
        // (If the CA signing this cert does not have a SubjectKeyIdentifier we must calculate the AuthorityKeyIdentifier from scratch)
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(caPublicKey.getEncoded());
        X509ExtensionUtils x509ExtensionUtils = new BcX509ExtensionUtils();
        final boolean isRootCA = (certProfile.getType() == CertificateConstants.CERTTYPE_ROOTCA);
        // If it is a Root CA, AKI and SKI are the same, and if we have said to use truncated SKI, the AKi should be the same
        // We have a real corner case here. If you have a root CA with method 1 KeyID, then you renew it changing to method 2 keyID
        // The renewed cert will have method 2 keyID, but if you create a link certificate the link certificate should have the AKI of the
        // old certificate, i.e. method 1. This does not happen here as we have no way of knowing if it's a link certificate we create here.
        if (isRootCA && certProfile.getUseTruncatedSubjectKeyIdentifier()) {
            // Just because there is no x509ExtensionUtils.createTruncatedAuthorityKeyIdentifier
            final SubjectKeyIdentifier ski = x509ExtensionUtils.createTruncatedSubjectKeyIdentifier(spki);            
            ret = new org.bouncycastle.asn1.x509.AuthorityKeyIdentifier(ski.getKeyIdentifier());
        } else {
            // "Normal" key identifier 
            ret = x509ExtensionUtils.createAuthorityKeyIdentifier(spki);
        }
        // If we have a CA-certificate (i.e. this is not a Root CA), we must take the authority key identifier from
        // the CA-certificates SubjectKeyIdentifier if it exists. If we don't do that we will get the wrong identifier if the
        // CA does not follow RFC3280 (guess if MS-CA follows RFC3280?)
        final X509Certificate cacert = getCACertificate(ca, caPublicKey);
        if ((cacert != null) && (!isRootCA)) {
            byte[] akibytes;
            akibytes = CertTools.getSubjectKeyId(cacert);
            if (akibytes != null) {
                ret = new org.bouncycastle.asn1.x509.AuthorityKeyIdentifier(akibytes);
                if (log.isDebugEnabled()) {
                    log.debug("Using AuthorityKeyIdentifier from CA-certificates SubjectKeyIdentifier.");
                }
            }
        }
        return ret;
    }

    private X509Certificate getCACertificate(final CA ca, final PublicKey caPublicKey) {
        final List<Certificate> rolloverChain = ca.getRolloverCertificateChain();
        if (rolloverChain != null && rolloverChain.get(0).getPublicKey().equals(caPublicKey)) {
            return (X509Certificate) rolloverChain.get(0);
        } else {
            return (X509Certificate) ca.getCACertificate();
        }
    }

    @Override
    public Map<String, String[]> getAvailableProperties() {
        return Collections.emptyMap();
    }

    @Override
    public byte[] getValueEncoded(EndEntityInformation userData, CA ca, CertificateProfile certProfile, PublicKey userPublicKey,
            PublicKey caPublicKey, CertificateValidity val, String oid) throws CertificateExtensionException {
        return super.getValueEncoded(userData, ca, certProfile, userPublicKey, caPublicKey, val);
    }
}
