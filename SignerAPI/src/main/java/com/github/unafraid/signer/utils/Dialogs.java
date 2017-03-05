package com.github.unafraid.signer.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.StageStyle;

/**
 * Created by Ugur, UnAfraid on 7-4-2015.
 */
public class Dialogs
{
	public static void showDialog(AlertType alertType, String title, String header, String context)
	{
		final Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(context);
		alert.initStyle(StageStyle.UTILITY);
		alert.show();
	}
	
	public static void showExceptionDialog(AlertType alertType, String title, String header, Throwable throwable)
	{
		final Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(throwable.getMessage());
		
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		
		final Label label = new Label("The exception stacktrace was:");
		
		final TextArea textArea = new TextArea(sw.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);
		
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		
		final GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);
		
		alert.getDialogPane().setExpandableContent(expContent);
		alert.show();
	}
	
	public static <T> Optional<T> showChoiceDialog(String title, String header, String content, Collection<T> choices, T defaultOption)
	{
		final ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultOption, choices);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(content);
		return dialog.showAndWait();
	}
	
	public static Optional<String> showInputDialog(String title, String header, String content, String defaultOption)
	{
		final TextInputDialog dialog = new TextInputDialog(defaultOption);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(content);
		return dialog.showAndWait();
	}
	
	public static Optional<ButtonType> showButtonDialog(String title, String header, String content, List<ButtonType> buttons)
	{
		final Alert dialog = new Alert(AlertType.CONFIRMATION);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(content);
		dialog.getButtonTypes().setAll(buttons);
		return dialog.showAndWait();
	}
	
	public static boolean showConfirmationDialog(String title, String header, String content)
	{
		final Alert dialog = new Alert(AlertType.CONFIRMATION);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(content);
		
		final Optional<ButtonType> result = dialog.showAndWait();
		return result.isPresent() && (result.get() == ButtonType.OK);
	}
}