package com.github.unafraid.signer.gui.controllers;

import com.github.unafraid.signer.server.ServerNetworkManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by UnAfraid on 11.7.2015 ã..
 */
public class MainController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Label middlewareLabel;

    @FXML
    private TextField middlewarePath;

    @FXML
    private Button selectMiddlewareButton;

    @FXML
    private Button startStopServerButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshStartStopButton();
    }

    private void refreshStartStopButton() {
        if (!middlewarePath.getText().isEmpty()) {
            startStopServerButton.setDisable(false);
        }
    }

    @FXML
    public void onMiddlewareSelected(ActionEvent event) {
        final FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File("C:/Windows/System32/"));
        chooser.setInitialFileName("bit4ipki.dll");
        chooser.setTitle("Select your middleware's library");
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("*.dll", "*.so"));

        final File file = chooser.showOpenDialog(null);

        if (file == null) {
            return;
        }

        middlewarePath.setText(file.getAbsolutePath().replaceAll("\\\\", "/"));
        refreshStartStopButton();
    }

    @FXML
    public void onStartStopServerButton(ActionEvent event) {
        final Object started = startStopServerButton.getProperties().get("started");

        if (started != Boolean.TRUE) {
            try {
                ServerNetworkManager.getInstance().start();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }

            middlewareLabel.setDisable(true);
            middlewarePath.setDisable(true);
            selectMiddlewareButton.setDisable(true);
            startStopServerButton.setText("Stop");
            startStopServerButton.getProperties().put("started", Boolean.TRUE);
        } else {
            try {
                ServerNetworkManager.getInstance().stop();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }

            middlewareLabel.setDisable(false);
            middlewarePath.setDisable(false);
            selectMiddlewareButton.setDisable(false);
            startStopServerButton.setText("Start server");
            startStopServerButton.getProperties().remove("started", Boolean.TRUE);
        }
    }
/*
    @FXML
    public void verifyPin(ActionEvent event) {
        certificateSelector.getItems().clear();
        if (!pin.getText().isEmpty()) {
            try {
                final DocumentSigner signer = new DocumentSigner();
                final KeyStore store = signer.getKeystore(middlewarePath.getText());
                store.load(null, pin.getText().toCharArray());
                final Enumeration aliases = store.aliases();
                while (aliases.hasMoreElements()) {
                    Object alias = aliases.nextElement();
                    try {
                        final X509Certificate cert = (X509Certificate) store.getCertificate(alias.toString());
                        certificateSelector.getItems().add(new CertificateHolder(cert));
                    } catch (Exception e) {
                        LOGGER.warn(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        if (!certificateSelector.getItems().isEmpty()) {
            Preferences.userRoot().put(ServerManager.MIDLWARE_PATH, middlewarePath.getText());
            System.setProperty(ServerManager.CARD_PIN, pin.getText());
            startButton.setDisable(false);
        }
    }

    @FXML
    public void selectCertificate(ActionEvent event) {
        final CertificateHolder holder = certificateSelector.getSelectionModel().getSelectedItem();
        if (holder != null) {
            certificateDescr.setText(holder.getCertificate().toString());
        }
    }
*/
    @FXML
    public void onApplicationExitRequest(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void onAboutRequest(ActionEvent event) {

    }
/*
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
*/
}
