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

import dsa41basis.hero.Buyable;
import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
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

public class EnergyEnhancementDialog {
	@FXML
	private VBox root;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Label nameLabel;
	@FXML
	private Label enhanceLabel;
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

	public EnergyEnhancementDialog(final Window window, final Buyable energy, final JSONObject character, final int initialTarget) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("EnhancementDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final Stage stage = GUIUtil.setupStage(root, 200, 215, "Zukauf", window, true);

		enhanceLabel.setText(" zukaufen:");

		target.valueProperty().addListener((_, _, _) -> ap.getValueFactory().setValue(getCalculatedAP(energy, character)));
		ses.valueProperty().addListener((_, _, _) -> ap.getValueFactory().setValue(getCalculatedAP(energy, character)));

		okButton.setOnAction(_ -> {
			final int usedSes = Math.min(ses.getValue(), target.getValue() - energy.getValue());
			final JSONArray history = character.getArr("Historie");
			final JSONObject historyEntry = new JSONObject(history);
			historyEntry.put("Typ", "Basiswert");
			historyEntry.put("Basiswert", energy.getName());
			historyEntry.put("Von", energy.getBought());
			historyEntry.put("Auf", target.getValue() - energy.getValue() + energy.getBought());
			if (usedSes > 0) {
				historyEntry.put("SEs", usedSes);
			}
			historyEntry.put("AP", ap.getValue());
			final LocalDate currentDate = LocalDate.now();
			historyEntry.put("Datum", currentDate.toString());
			history.add(historyEntry);

			final JSONObject bio = character.getObj("Biografie");
			bio.put("Abenteuerpunkte-Guthaben", bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0) - ap.getValue());
			energy.setBought(target.getValue() - energy.getValue() + energy.getBought());
			energy.setSes(Math.max(energy.getSes() - usedSes, 0));

			stage.close();
		});

		cancelButton.setOnAction(_ -> stage.close());

		nameLabel.setText(energy.getName());
		startLabel.setText(Integer.toString(energy.getValue()));
		maxLabel.setText(
				energy.getMaximum(character) == Integer.MAX_VALUE ? "—"
						: Integer.toString(energy.getValue() - energy.getBought() + energy.getMaximum(character)));
		((IntegerSpinnerValueFactory) target.getValueFactory()).setMin(energy.getValue());
		((IntegerSpinnerValueFactory) target.getValueFactory()).setMax(energy.getValue() - energy.getBought() + energy.getMaximum(character));
		target.getValueFactory().setValue(initialTarget);
		ses.getValueFactory().setValue(energy.getSes());

		stage.show();
	}

	private int getCalculatedAP(final Buyable energy, final JSONObject hero) {
		final int SELevel = energy.getBought() + Math.min(target.getValue() - energy.getValue(), ses.getValue());
		final int targetLevel = target.getValue() - energy.getValue() + energy.getBought();
		return DSAUtil.getEnhancementCost(energy.getEnhancementComplexity(hero, SELevel) - 1, energy.getBought(), SELevel)
				+ DSAUtil.getEnhancementCost(energy.getEnhancementComplexity(hero, targetLevel), SELevel, targetLevel);
	}
}
