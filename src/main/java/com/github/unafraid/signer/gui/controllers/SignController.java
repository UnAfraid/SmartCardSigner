package com.github.unafraid.signer.gui.controllers;

import com.github.unafraid.signer.signer.DocumentSigner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * Created by Venci on 12.9.2015 ã..
 */
public class SignController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Text headerText;

    @FXML
    private TextArea signData;

    @FXML
    private ComboBox certList;

    @FXML
    private TextArea certDetails;

    @FXML
    private TextField pinBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ?\_(?)_/?
/*
        try {
            final DocumentSigner signer = new DocumentSigner();
            final KeyStore store = signer.getKeystore(middlewarePath.getText());
            store.load(null, pin.getText().toCharArray());
            final Enumeration aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                Object alias = aliases.nextElement();
                try {
                    final X509Certificate cert = (X509Certificate) store.getCertificate(alias.toString());
                    certList.getItems().add(new CertificateHolder(cert));
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
        */
    }

    public void setDomainName(String domainName) {
        headerText.setText(headerText.getText().replace("%s", domainName));
    }

    public void setContentToSign(String contentToSign) {
        signData.setText(contentToSign);
    }

    @FXML
    private void onCertificateChanged(ActionEvent event) {
        //
    }

    @FXML
    private void onCancel(ActionEvent event) {
        certDetails.getScene().setUserData("OK");
        certDetails.getScene().getWindow().hide();
    }

    @FXML
    private void onOk(ActionEvent event) {
        certDetails.getScene().setUserData("OK");
        certDetails.getScene().getWindow().hide();
    }

    static class CertificateHolder {
        final X509Certificate _certificate;

        CertificateHolder(X509Certificate certificate) {
            _certificate = certificate;
        }

        public X509Certificate getCertificate() {
            return _certificate;
        }

        @Override
        public String toString() {
            return _certificate.getSubjectDN().getName();
        }
    }
}
