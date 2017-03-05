package com.github.unafraid.signer.gui.controllers;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.unafraid.signer.DocumentSigner;
import com.github.unafraid.signer.server.NetworkManager;
import com.github.unafraid.signer.utils.Dialogs;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

/**
 * @author UnAfraid
 */
public class MainController implements Initializable
{
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
	
	private static final Map<String, String> drivers = new LinkedHashMap<>();
	
	static
	{
		final String windir = System.getenv("windir");
		final String programFiles = System.getenv("ProgramFiles");
		
		if ((windir != null) && (windir.length() > 0) && (programFiles != null) && (programFiles.length() > 0))
		{
			drivers.put("Charismatics(32 bit)", windir + "\\System32\\cmP11.dll");
			drivers.put("Charismatics(64 bit)", windir + "\\SysWOW64\\cmP1164.dll");
			drivers.put("Bit4id", windir + "\\System32\\bit4ipki.dll");
			drivers.put("CryptoVision(32 bit)", windir + "\\System32\\cvP11.dll");
			drivers.put("CryptoVision(64 bit)", windir + "\\SysWOW64\\cvP11.dll");
			drivers.put("Siemens CardOS", windir + "\\System32\\siecap11.dll");
			drivers.put("SafeNet/Datakey", windir + "\\system32\\dkck201.dll");
			drivers.put("ActivCard Gold", windir + "\\System32\\acpkcs.dll");
			drivers.put("Setec SetWeb", programFiles + "\\SetWeb\\settoki.dll");
			drivers.put("Gemplus", programFiles + "\\GemPlus\\GemSafe Libraries\\BIN\\gclib.dll");
			drivers.put("Utimaco SafeGuard", windir + "\\system32\\pkcs201n.dll");
		}
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		refreshStartStopButton();
		for (String path : drivers.values())
		{
			if (Files.exists(Paths.get(path)))
			{
				middlewarePath.setText(path);
				break;
			}
		}
	}
	
	private void refreshStartStopButton()
	{
		if (!middlewarePath.getText().isEmpty())
		{
			startStopServerButton.setDisable(false);
		}
	}
	
	@FXML
	public void onMiddlewareSelected(ActionEvent event)
	{
		final FileChooser chooser = new FileChooser();
		chooser.setInitialDirectory(new File("C:/Windows/System32/"));
		chooser.setInitialFileName("bit4ipki.dll");
		chooser.setTitle("Select your middleware's library");
		chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("*.dll", "*.so"));
		
		final File file = chooser.showOpenDialog(null);
		if (file == null)
		{
			return;
		}
		
		middlewarePath.setText(file.getAbsolutePath().replaceAll("\\\\", "/"));
		refreshStartStopButton();
	}
	
	@FXML
	public void onStartStopServerButton(ActionEvent event)
	{
		final Object started = startStopServerButton.getProperties().get("started");
		
		if (started != Boolean.TRUE)
		{
			if (pinCodeField.getText().isEmpty())
			{
				Dialogs.showDialog(Alert.AlertType.ERROR, "Input error", "No pin code", "Please enter your PIN code!");
				return;
			}
			
			startStopServerButton.setText("Verifying your Smart card...");
			
			try
			{
				DocumentSigner.init(middlewarePath.getText(), pinCodeField.getText());
			}
			catch (Exception e)
			{
				Dialogs.showExceptionDialog(Alert.AlertType.ERROR, "Smart card error", e.getMessage(), e);
				return;
			}
			finally
			{
				startStopServerButton.setText("Start");
			}
			
			try
			{
				NetworkManager.getInstance().start();
			}
			catch (Exception e)
			{
				LOGGER.warn(e.getMessage(), e);
			}
			
			middlewareLabel.setDisable(true);
			middlewarePath.setDisable(true);
			pinCodeLabel.setDisable(true);
			pinCodeField.setDisable(true);
			selectMiddlewareButton.setDisable(true);
			startStopServerButton.setText("Stop");
			startStopServerButton.getProperties().put("started", Boolean.TRUE);
		}
		else
		{
			try
			{
				NetworkManager.getInstance().stop();
			}
			catch (Exception e)
			{
				LOGGER.warn(e.getMessage(), e);
			}
			
			middlewareLabel.setDisable(false);
			middlewarePath.setDisable(false);
			pinCodeLabel.setDisable(false);
			pinCodeField.setDisable(false);
			selectMiddlewareButton.setDisable(false);
			startStopServerButton.setText("Start server");
			startStopServerButton.getProperties().remove("started", Boolean.TRUE);
		}
	}
	
	@FXML
	public void onApplicationExitRequest(ActionEvent event)
	{
		Platform.exit();
	}
	
	@FXML
	public void onAboutRequest(ActionEvent event)
	{
	}
}
