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
import java.util.Collections;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.internal.CertificateValidity;
import org.cesecore.certificates.certificate.certextensions.CertificateExtensionException;
import org.cesecore.certificates.certificate.certextensions.CustomCertificateExtension;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.endentity.EndEntityInformation;

/** 
 * Class for standard X509 certificate extension. 
 * See rfc5280 or later for spec of this extension.      
 */
public class SubjectKeyIdentifier extends StandardCertificateExtension implements CustomCertificateExtension {

    private static final long serialVersionUID = 1L;

    public SubjectKeyIdentifier() {
        super.setDisplayName("SubjectKeyIdentifier");
        
    }
    
    @Override
    public void init(final CertificateProfile certProf) {
        super.setOID(Extension.subjectKeyIdentifier.getId());
        super.setCriticalFlag(certProf.getSubjectKeyIdentifierCritical());
    }
    
    @Override
    public ASN1Encodable getValue(final EndEntityInformation subject, final CA ca, final CertificateProfile certProfile,
            final PublicKey userPublicKey, final PublicKey caPublicKey, CertificateValidity val) throws CertificateExtensionException {
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(userPublicKey.getEncoded());
        X509ExtensionUtils x509ExtensionUtils = new BcX509ExtensionUtils();
        if (certProfile.getUseTruncatedSubjectKeyIdentifier()) {
            return x509ExtensionUtils.createTruncatedSubjectKeyIdentifier(spki);            
        } else {
            return x509ExtensionUtils.createSubjectKeyIdentifier(spki);
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
