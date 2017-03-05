package com.github.unafraid.signer.gui.controllers;

import java.lang.reflect.Field;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.DocumentSigner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import sun.security.pkcs11.wrapper.CK_TOKEN_INFO;

/**
 * @author Venci
 */
public class SignController implements Initializable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SignController.class);
	
	static final String[] KEY_USAGES =
	{
		"Digital Signature",
		"Non Repudiation",
		"Key Encipherment",
		"Data Encipherment",
		"Key Agreement",
		"Key Cert Sign",
		"cRLSign",
		"Encipher Only",
		"Decipher Only"
	};
	
	@FXML
	private Text headerText;
	
	@FXML
	private TextArea signData;
	
	@FXML
	private ComboBox<CertificateHolder> certList;
	
	@FXML
	private TextArea certDetails;
	
	@FXML
	private PasswordField pinBox;
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		try
		{
			final KeyStore store = DocumentSigner.getKeystore();
			final Enumeration<String> aliases = store.aliases();
			while (aliases.hasMoreElements())
			{
				String alias = aliases.nextElement();
				try
				{
					final X509Certificate cert = (X509Certificate) store.getCertificate(alias);
					certList.getItems().add(new CertificateHolder(store, alias, cert));
				}
				catch (Exception e)
				{
					LOGGER.warn(e.getMessage(), e);
				}
			}
			
			if (!certList.getItems().isEmpty())
			{
				final CertificateHolder holder = certList.getItems().get(0);
				certList.getSelectionModel().select(0);
				certDetails.setText(holder.getDescription());
			}
		}
		catch (Exception e)
		{
			LOGGER.warn(e.getMessage(), e);
		}
	}
	
	public String getPinCode()
	{
		return pinBox.getText();
	}
	
	static final <T> T getObject(Class<?> sourceClass, Object sourceInstance, final String fieldName, Class<T> targetClass)
	{
		try
		{
			final Field field = sourceClass.getDeclaredField(fieldName);
			
			// Mark down if field was accessible
			final boolean isAccessible = field.isAccessible();
			
			// Enforce accessible to retrieve the object associated with this field
			if (!isAccessible)
			{
				field.setAccessible(true);
			}
			
			// Get the object
			final Object fieldObject = field.get(sourceInstance);
			
			// Restore the original accessible state.
			field.setAccessible(isAccessible);
			
			// Make sure the object is the one we expect to be
			if (targetClass.isInstance(fieldObject))
			{
				return targetClass.cast(fieldObject);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Error while retrieving object of {}#{}", sourceInstance.getClass().getName(), fieldName, e);
		}
		return null;
	}
	
	public void setDomainName(String domainName)
	{
		headerText.setText(headerText.getText().replace("%s", domainName));
	}
	
	public void setContentToSign(String contentToSign)
	{
		signData.setText(contentToSign);
	}
	
	@FXML
	private void onCertificateChanged(ActionEvent event)
	{
		final CertificateHolder holder = certList.getSelectionModel().getSelectedItem();
		if (holder != null)
		{
			certDetails.setText(holder.getDescription());
		}
	}
	
	@FXML
	private void onCancel(ActionEvent event)
	{
		certDetails.getScene().setUserData("CANCEL");
		certDetails.getScene().getWindow().hide();
	}
	
	@FXML
	private void onOk(ActionEvent event)
	{
		certDetails.getScene().setUserData("OK");
		certDetails.getScene().getWindow().hide();
	}
	
	static class CertificateHolder
	{
		final KeyStore _keyStore;
		final String _alias;
		final X509Certificate _certificate;
		
		CertificateHolder(KeyStore store, String alias, X509Certificate certificate)
		{
			_keyStore = store;
			_alias = alias;
			_certificate = certificate;
		}
		
		public X509Certificate getCertificate()
		{
			return _certificate;
		}
		
		@Override
		public String toString()
		{
			return getTokenLabel() + ":" + _alias;
		}
		
		private String getTokenLabel()
		{
			final Object keyStoreSpi = getObject(_keyStore.getClass(), _keyStore, "keyStoreSpi", Object.class);
			final Object token = getObject(keyStoreSpi.getClass(), keyStoreSpi, "token", Object.class);
			final CK_TOKEN_INFO info = getObject(token.getClass(), token, "tokenInfo", CK_TOKEN_INFO.class);
			return new String(info.label).trim();
		}
		
		public String getDescription()
		{
			try
			{
				final X509Certificate cert = (X509Certificate) _keyStore.getCertificate(_alias);
				final String issuerName = cert.getIssuerDN().getName();
				final String serialNumber = Integer.toHexString(cert.getSerialNumber().intValue()).toUpperCase();
				final String subjectName = cert.getSubjectDN().getName();
				final Date notBefore = cert.getNotBefore();
				final Date notAfter = cert.getNotAfter();
				final String keyUsage = parseKeyUsage(cert.getKeyUsage());
				final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
				final StringJoiner sj = new StringJoiner(System.lineSeparator());
				final Map<String, String> commonNames = parseCommonName(subjectName);
				sj.add("Issued to: " + subjectName);
				sj.add("  Serial Number: " + serialNumber);
				sj.add("  Valid from " + formatter.format(notBefore) + " to " + formatter.format(notAfter));
				sj.add("  Certificate Key Usage: " + keyUsage);
				if (commonNames.containsKey("EMAILADDRESS"))
				{
					sj.add("  Email: " + commonNames.get("EMAILADDRESS"));
				}
				sj.add("Issued by: " + issuerName);
				sj.add("Stored in: " + getTokenLabel());
				return sj.toString();
			}
			catch (Exception e)
			{
				return "Error: " + e.getMessage();
			}
		}
		
		private static String parseKeyUsage(boolean[] keyUsage)
		{
			final StringJoiner sj = new StringJoiner(", ");
			for (int i = 0; i < keyUsage.length; i++)
			{
				if (keyUsage[i])
				{
					sj.add(KEY_USAGES[i]);
				}
			}
			return sj.toString();
		}
		
		private static Map<String, String> parseCommonName(String subjectName)
		{
			final Map<String, String> values = new HashMap<>();
			final String[] data = subjectName.split(", ");
			for (String entry : data)
			{
				final String[] keyValuePair = entry.split("=");
				values.put(keyValuePair[0], keyValuePair[1]);
			}
			return values;
		}
	}
}
