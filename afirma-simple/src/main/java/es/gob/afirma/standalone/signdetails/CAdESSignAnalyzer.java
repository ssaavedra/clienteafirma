package es.gob.afirma.standalone.signdetails;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.esf.SignaturePolicyId;
import org.spongycastle.asn1.esf.SignerAttribute;
import org.spongycastle.asn1.esf.SignerLocation;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationStore;
import org.spongycastle.util.Store;

import es.gob.afirma.core.AOInvalidFormatException;
import es.gob.afirma.core.misc.MimeHelper;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.core.signers.AdESPolicy;
import es.gob.afirma.core.util.tree.AOTreeModel;
import es.gob.afirma.signers.cades.CAdESAttributes;
import es.gob.afirma.signvalidation.CertHolderBySignerIdSelector;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.crypto.CompleteSignInfo;

public class CAdESSignAnalyzer implements SignAnalyzer {

	static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private List <SignDetails> signDetailsList;
	AOTreeModel signersTree;
	private CMSSignedData cmsSignedData;
	private Properties oidMimetypeProp;

	private static final String CADES = "CAdES"; //$NON-NLS-1$s

	public CAdESSignAnalyzer(final byte [] data) throws Exception {
    	try {
    		this.signDetailsList = new ArrayList<SignDetails>();
            final CompleteSignInfo signInfo = SignAnalyzer.getSignInfo(data);
            this.signersTree = signInfo.getSignsTree();
    		loadOidMimetypeProperties();
    		createSignDetails(data);
    	}
    	catch (final Exception e) {
    		throw new AOInvalidFormatException("No se ha podido cargar el documento XML de firmas", e); //$NON-NLS-1$
    	}
	}

	@Override
	public AOTreeModel getSignersTree() {
		return this.signersTree;
	}

	@Override
	public List<SignDetails> getAllSignDetails() {
		return this.signDetailsList;
	}

	@Override
	public String getSignFormat() {
		return CADES;
	}

	@Override
	public String getDataLocation() {
        if (this.cmsSignedData.getSignedContent() != null && this.cmsSignedData.getSignedContent().getContent() != null) {
        	return SimpleAfirmaMessages.getString("ValidationInfoDialog.21"); //$NON-NLS-1$
        }
        return SimpleAfirmaMessages.getString("ValidationInfoDialog.22"); //$NON-NLS-1$
	}

	private void createSignDetails(final byte [] data) throws Exception {

		this.cmsSignedData = new CMSSignedData(data);

		for (final Object si : this.cmsSignedData.getSignerInfos().getSigners()) {
			final SignerInformation signer = (SignerInformation) si;
			buildSignDetails(signer);
		}
	}

	private void buildSignDetails(final SignerInformation signer) throws Exception {

		final SignDetails cadesSignDetails = new SignDetails();
		final ASN1ObjectIdentifier contentType = signer.getContentType();

		// Perfil de firma
		final String signProfile = SignatureFormatDetectorPadesCades.resolveASN1Format(this.cmsSignedData, signer);
		cadesSignDetails.setSignProfile(signProfile);

		// Mimetype
		if (contentType != null) {
			final String mimeType = (String) this.oidMimetypeProp.get(signer.getContentType().getId());
			cadesSignDetails.getDataObjectFormats().add(new DataObjectFormat(mimeType));
		}

		// Algoritmo
		final String algorithm = AOSignConstants.getDigestAlgorithmName(signer.getDigestAlgorithmID().getAlgorithm().toString());
		cadesSignDetails.setAlgorithm(algorithm);

		// Politica
		analyzePolicy(signer, cadesSignDetails);

		// Roles
		Attribute roleAttr = signer.getSignedAttributes().get(PKCSObjectIdentifiers.id_aa_ets_signerAttr);
		if (roleAttr == null) {
			roleAttr = signer.getSignedAttributes().get(new ASN1ObjectIdentifier(CAdESAttributes.OID_id_aa_ets_signerAttrV2));
		}
		if (roleAttr != null) {
			final SignerAttribute signerAttribute = SignerAttribute.getInstance(roleAttr.getAttrValues().getObjectAt(0));
			final Object [] objAttributes = signerAttribute.getValues();
			final org.spongycastle.asn1.x509.Attribute [] rolesAttr =  (org.spongycastle.asn1.x509.Attribute[]) objAttributes[0];
			for (int i = 0 ; i < rolesAttr.length ; i++) {
				final String role = rolesAttr[i].getAttrValues().getObjectAt(0).toString();
				cadesSignDetails.getMetadata().put("claimedRole" + i, role); //$NON-NLS-1$
			}
		}

		// Localizacion de la firma
		final Attribute locationAttr = signer.getSignedAttributes().get(PKCSObjectIdentifiers.id_aa_ets_signerLocation);
		if (locationAttr != null) {
			final SignerLocation signerLocation = SignerLocation.getInstance(locationAttr.getAttrValues().getObjectAt(0));
			if (signerLocation.getCountry() != null) {
				final String country = signerLocation.getCountry().toString();
				cadesSignDetails.getMetadata().put(SimpleAfirmaMessages.getString("ValidationInfoDialog.9"), country); //$NON-NLS-1$
			}
			if (signerLocation.getPostal() != null && signerLocation.getPostal().length > 0) {
				final String postalCode = signerLocation.getPostal()[0].toString();
				cadesSignDetails.getMetadata().put(SimpleAfirmaMessages.getString("ValidationInfoDialog.8"), postalCode); //$NON-NLS-1$
			}
			if (signerLocation.getLocality() != null) {
				final String locality = signerLocation.getLocality().toString();
				cadesSignDetails.getMetadata().put(SimpleAfirmaMessages.getString("ValidationInfoDialog.23"), locality); //$NON-NLS-1$
			}
		}

		// Certificado de firma
		final Store<X509CertificateHolder> certsStore = this.cmsSignedData.getCertificates();

		final CertificateFactory certFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$

		final Iterator<X509CertificateHolder> certIt = certsStore
				.getMatches(new CertHolderBySignerIdSelector(signer.getSID())).iterator();

		final X509Certificate x509Cert = (X509Certificate) certFactory
				.generateCertificate(new ByteArrayInputStream(certIt.next().getEncoded()));

		final CertificateDetails certDetails = new CertificateDetails(x509Cert);
		cadesSignDetails.getSigners().add(certDetails);

		this.signDetailsList.add(cadesSignDetails);

		//Tratamos tambien las contrafirmas de la firma en caso de que tenga
		final SignerInformationStore counterSignatures = signer.getCounterSignatures();
		if (counterSignatures != null && counterSignatures.size() > 0) {
			for (final SignerInformation si : (List<SignerInformation>) counterSignatures.getSigners()) {
				buildSignDetails(si);
			}
		}

	}

    /** Carga el fichero de propiedades que relaciona OID de formato con su MimeType
     * correspondiente.
     * @throws IOException Cuando hay errores en la carga del fichero de propiedades. */
    private void loadOidMimetypeProperties() throws IOException {
        this.oidMimetypeProp = new Properties();
        try (
    		final InputStream isProp = MimeHelper.class.getClassLoader().getResourceAsStream(
				"resources/mimetypes_oids.properties" //$NON-NLS-1$
			);
		) {
	        if (isProp == null) {
	        	throw new IOException(
        			"No se ha encontrado el fichero de recursos para la relacion entre OID y MimeType" //$NON-NLS-1$
    			);
	        }
	        this.oidMimetypeProp.load(isProp);
        }
    }

    private static void analyzePolicy(final SignerInformation si, final SignDetails signDetails) {
		final Attribute policyAttr = si.getSignedAttributes().get(PKCSObjectIdentifiers.id_aa_ets_sigPolicyId);
		if (policyAttr != null && policyAttr.getAttrValues() != null && policyAttr.getAttrValues().size() > 0) {
			final SignaturePolicyId sigPolId = SignaturePolicyId.getInstance(policyAttr.getAttrValues().getObjectAt(0));
			final String polId = sigPolId.getSigPolicyId().toString();
			if (polId.equals(SignDetails.POLICY_CADES_AGE_1_9.getPolicyIdentifier().substring(8))) {
				signDetails.setPolicy(new SignaturePolicy(SimpleAfirmaMessages.getString("PreferencesPanel.73"), SignDetails.POLICY_CADES_AGE_1_9)); //$NON-NLS-1$
			} else if (polId.equals(SignDetails.POLICY_CADES_AGE_1_8.getPolicyIdentifier().substring(8))) {
				signDetails.setPolicy(new SignaturePolicy(SimpleAfirmaMessages.getString("PreferencesPanel.25"), SignDetails.POLICY_CADES_AGE_1_8)); //$NON-NLS-1$
			} else {
				final String identifierHash = sigPolId.getSigPolicyHash().getHashValue().toString();
				final String identifierHashAlgorithm = sigPolId.getSigPolicyHash().getHashAlgorithm().getAlgorithm().toString();
				final String qualifier = sigPolId.getSigPolicyQualifiers().getInfoAt(0).getSigQualifier().toString();
				final AdESPolicy newPolicy = new AdESPolicy(polId, identifierHash.substring(1), identifierHashAlgorithm, qualifier);
				signDetails.setPolicy(new SignaturePolicy("", newPolicy)); //$NON-NLS-1$
			}
		}
    }

}
