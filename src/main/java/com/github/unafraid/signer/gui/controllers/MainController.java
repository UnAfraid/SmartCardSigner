package com.github.unafraid.signer.gui.controllers;

import com.github.unafraid.signer.server.ServerNetworkManager;
import com.github.unafraid.signer.signer.DocumentSigner;
import com.github.unafraid.signer.signer.SignerConfig;
import com.github.unafraid.signer.utils.Dialogs;
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
 * Created by UnAfraid on 11.7.2015 �..
 */
public class MainController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Label middlewareLabel;

    @FXML
    private TextField middlewarePath;

    @FXML
    private Label pinCodeLabel;

    @FXML
    private PasswordField pinCodeField;

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
        SignerConfig.MIDDLEWARE_PATH = middlewarePath.getText();
        SignerConfig.PIN_CODE = pinCodeField.getText();
        refreshStartStopButton();
    }

    @FXML
    public void onStartStopServerButton(ActionEvent event) {
        final Object started = startStopServerButton.getProperties().get("started");

        if (started != Boolean.TRUE) {
            if (pinCodeField.getText().isEmpty()) {
                Dialogs.showDialog(Alert.AlertType.ERROR, "Input error", "No pin code", "Please enter your PIN code!");
                return;
            }

            SignerConfig.MIDDLEWARE_PATH = middlewarePath.getText();
            SignerConfig.PIN_CODE = pinCodeField.getText();

            startStopServerButton.setText("Verifying your Smart card...");

            try {
                DocumentSigner.init(middlewarePath.getText(), pinCodeField.getText());
            } catch (Exception e) {
                Dialogs.showExceptionDialog(Alert.AlertType.ERROR, "Smart card error", e.getMessage(), e);
                return;
            } finally {
                startStopServerButton.setText("Start");
            }

            try {
                ServerNetworkManager.getInstance().start();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }

            middlewareLabel.setDisable(true);
            middlewarePath.setDisable(true);
            pinCodeLabel.setDisable(true);
            pinCodeField.setDisable(true);
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
            pinCodeLabel.setDisable(false);
            pinCodeField.setDisable(false);
            selectMiddlewareButton.setDisable(false);
            startStopServerButton.setText("Start server");
            startStopServerButton.getProperties().remove("started", Boolean.TRUE);
            SignerConfig.MIDDLEWARE_PATH = middlewarePath.getText();
            SignerConfig.PIN_CODE = pinCodeField.getText();
        }
    }

    @FXML
    public void onApplicationExitRequest(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void onAboutRequest(ActionEvent event) {
    }
}
