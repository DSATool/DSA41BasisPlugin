/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dsa41basis.ui.hero;

import java.time.LocalDate;

import dsa41basis.hero.Raisable;
import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class AttributeEnhancementDialog {
	@FXML
	private VBox root;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Label nameLabel;
	@FXML
	private Label startLabel;
	@FXML
	private Label maxLabel;
	@FXML
	private ReactiveSpinner<Integer> target;
	@FXML
	private ReactiveSpinner<Integer> ses;
	@FXML
	private ReactiveSpinner<Integer> ap;

	final Stage stage;

	public AttributeEnhancementDialog(final Window window, final Raisable attribute, final JSONObject character, final int initialTarget) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("EnhancementDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
		final boolean isMiserable = attributes.containsKey(attribute.getName())
				? character.getObj("Nachteile").containsKey(attributes.getObj(attribute.getName()).getString("Miserable Eigenschaft")) : false;

		stage = GUIUtil.setupStage(root, 250, 215, "Eigenschaft steigern", window, true);

		target.valueProperty().addListener((_, _, _) -> ap.getValueFactory().setValue(getCalculatedAP(attribute, character, isMiserable)));
		ses.valueProperty().addListener((_, _, _) -> ap.getValueFactory().setValue(getCalculatedAP(attribute, character, isMiserable)));

		okButton.setOnAction(_ -> {
			final int usedSes = Math.min(target.getValue() - attribute.getValue(), ses.getValue());
			final JSONArray history = character.getArr("Historie");
			final JSONObject historyEntry = new JSONObject(history);
			historyEntry.put("Typ", "Eigenschaft");
			historyEntry.put("Eigenschaft", attribute.getName());
			historyEntry.put("Von", attribute.getValue());
			historyEntry.put("Auf", target.getValue());
			if (usedSes > 0) {
				historyEntry.put("SEs", usedSes);
			}
			historyEntry.put("AP", ap.getValue());
			final LocalDate currentDate = LocalDate.now();
			historyEntry.put("Datum", currentDate.toString());
			history.add(historyEntry);
			history.notifyListeners(null);

			final JSONObject bio = character.getObj("Biografie");
			bio.put("Abenteuerpunkte-Guthaben", bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) - ap.getValue());
			bio.notifyListeners(null);

			attribute.setValue(target.getValue());
			attribute.setSes(Math.max(attribute.getSes() - usedSes, 0));

			stage.close();
		});

		cancelButton.setOnAction(_ -> stage.close());

		final String longName = attributes.containsKey(attribute.getName()) ? attributes.getObj(attribute.getName()).getString("Name") : attribute.getName();

		nameLabel.setText(longName);
		startLabel.setText(Integer.toString(attribute.getValue()));
		maxLabel.setText(attribute.getMaximum(character) == Integer.MAX_VALUE ? "—" : Integer.toString(attribute.getMaximum(character)));
		((IntegerSpinnerValueFactory) target.getValueFactory()).setMin(attribute.getValue());
		((IntegerSpinnerValueFactory) target.getValueFactory()).setMax(attribute.getMaximum(character));
		target.getValueFactory().setValue(initialTarget);
		ses.getValueFactory().setValue(attribute.getSes());

		stage.show();
	}

	private int getCalculatedAP(final Raisable attribute, final JSONObject character, final boolean isMiserable) {
		final int value = attribute.getValue();
		final int SELevel = value + Math.min(target.getValue() - value, ses.getValue());
		return (DSAUtil.getEnhancementCost(attribute.getEnhancementComplexity(character, SELevel) - 1, value, SELevel)
				+ DSAUtil.getEnhancementCost(attribute.getEnhancementComplexity(character, target.getValue()), SELevel, target.getValue()))
				* (isMiserable ? 2 : 1);
	}

	public Stage getStage() {
		return stage;
	}
}
