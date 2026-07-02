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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import dsa41basis.util.DSAUtil;
import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class MagicAnimalBindingDialog {
	@FXML
	private VBox root;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;
	@FXML
	private ReactiveSpinner<Integer> ap;
	@FXML
	private TextField name;
	@FXML
	private ComboBox<String> species;
	@FXML
	private CheckBox mighty;
	@FXML
	private Label attributePointsLabel;
	@FXML
	private VBox attributesBox;
	@FXML
	private ReactiveSpinner<Integer> lep;
	@FXML
	private ReactiveSpinner<Integer> asp;
	@FXML
	private ReactiveSpinner<Integer> aup;
	@FXML
	private Label lepMax;
	@FXML
	private Label aspMax;
	@FXML
	private Label aupMax;

	private final JSONObject animalTypes;

	private final IntegerProperty attributePoints = new SimpleIntegerProperty(20);

	public MagicAnimalBindingDialog(final Window window, final JSONObject animal, final JSONObject hero, final boolean allowCancellation) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("MagicAnimalBindingDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");

		final Stage stage = GUIUtil.setupStage(root, 275, 290 + 27 * attributes.size(), "Vertrautentier binden", window, true);

		attributePointsLabel.textProperty().bind(attributePoints.asString());

		final Map<String, Tuple<ReactiveSpinner<Integer>, Label>> attributeInputs = new LinkedHashMap<>();

		animalTypes = ResourceManager.getResource("data/Vertrautentierarten");

		DSAUtil.foreach(_ -> true, (key, attribute) -> {
			final HBox box = new HBox(10);
			attributesBox.getChildren().add(box);
			final Label nameLabel = new Label(attribute.getString("Name"));
			nameLabel.setAlignment(Pos.CENTER_LEFT);
			nameLabel.setMaxWidth(Double.POSITIVE_INFINITY);
			nameLabel.setMaxHeight(Double.POSITIVE_INFINITY);
			HBox.setHgrow(nameLabel, Priority.ALWAYS);
			final ReactiveSpinner<Integer> spinner = new ReactiveSpinner<>(0, 999);
			spinner.setEditable(true);
			spinner.setPrefWidth(50);
			spinner.valueProperty().addListener((_, oldV, newV) -> {
				final int min = ((IntegerSpinnerValueFactory) spinner.getValueFactory()).getMin();
				final int max = getAttributeMaximum(key);
				final int previousAP = (oldV - min) * 2 + (oldV > max ? (oldV - max) * 3 : 0);
				final int newAP = (newV - min) * 2 + (newV > max ? (newV - max) * 3 : 0);
				ap.getValueFactory().setValue(ap.getValue() - previousAP + newAP);
				attributePoints.set(attributePoints.get() + oldV - newV);
			});
			final Label maxLabel = new Label();
			maxLabel.setAlignment(Pos.CENTER_RIGHT);
			maxLabel.setMaxHeight(Double.POSITIVE_INFINITY);
			attributeInputs.put(key, new Tuple<>(spinner, maxLabel));
			box.getChildren().addAll(nameLabel, maxLabel, spinner);
		}, attributes);

		lep.valueProperty().addListener((_, oldV, newV) -> ap.getValueFactory().setValue(ap.getValue() + (newV - oldV) * 5));
		asp.valueProperty().addListener((_, oldV, newV) -> ap.getValueFactory().setValue(ap.getValue() + (newV - oldV) * 5));
		aup.valueProperty().addListener((_, oldV, newV) -> ap.getValueFactory().setValue(ap.getValue() + (newV - oldV) * 2));

		species.valueProperty().addListener((_, _, newV) -> {
			final JSONObject basicValues = animalTypes.getObj(newV).getObj("Basiswerte");
			setEnergyBase(basicValues, "Lebensenergie", lep, lepMax);
			setEnergyBase(basicValues, "Astralenergie", asp, aspMax);
			setEnergyBase(basicValues, "Ausdauer", aup, aupMax);
			setAttributeRanges(attributeInputs, true);
		});

		species.getItems().setAll(animalTypes.keySet());
		species.getSelectionModel().select(animalTypes.keySet().iterator().next());

		mighty.selectedProperty().addListener((_, _, _) -> setAttributeRanges(attributeInputs, false));

		final JSONObject actualBasicValues = animal.getObj("Basiswerte");

		final JSONObject ritualKnowledge = actualBasicValues.getObj("Ritualkenntnis (Vertrautenmagie)");
		ritualKnowledge.put("TaW", 3);

		final JSONObject skills = animal.getObj("Fertigkeiten");
		skills.put("Zwiegespräch", new JSONObject(skills));

		okButton.disableProperty().bind(attributePoints.lessThan(0));

		okButton.setOnAction(_ -> {
			final JSONObject bio = animal.getObj("Biografie");
			bio.put("Name", name.getText().isBlank() ? "Vertrautentier" : name.getText());
			bio.put("Tierart", species.getValue());
			bio.put("Abenteuerpunkte-Kosten", ap.getValue());

			final JSONObject actualAttributes = animal.getObj("Eigenschaften");
			for (final Entry<String, Tuple<ReactiveSpinner<Integer>, Label>> inputs : attributeInputs.entrySet()) {
				final JSONObject attribute = actualAttributes.getObj(inputs.getKey());
				final int value = inputs.getValue()._1.getValue();
				attribute.put("Wert", value);
				attribute.put("Start", value);
			}

			final JSONObject basicValues = animalTypes.getObj(species.getValue()).getObj("Basiswerte");
			for (final String basicValue : basicValues.keySet()) {
				switch (basicValue) {
					case "Lebensenergie":
						final JSONObject actualLeP = actualBasicValues.getObj("Lebensenergie");
						final int lepValue = lep.getValue();
						actualLeP.put("Wert", lepValue);
						actualLeP.put("Start", lepValue);
						break;
					case "Astralenergie":
						final JSONObject actualAsP = actualBasicValues.getObj("Astralenergie");
						final int aspValue = lep.getValue();
						actualAsP.put("Wert", aspValue);
						actualAsP.put("Start", aspValue);
						break;
					case "Ausdauer":
						final JSONObject actualAuP = actualBasicValues.getObj("Ausdauer");
						actualAuP.put("Wert", aup.getValue());
						break;
					case "Geschwindigkeit":
						final JSONObject actualSpeed = basicValues.getObj("Geschwindigkeit").clone(actualBasicValues);
						actualBasicValues.put("Geschwindigkeit", actualSpeed);
						if (actualSpeed.containsKey("Wert")) {
							actualSpeed.put("Start", actualSpeed.getDouble("Wert"));
						}
						if (actualSpeed.containsKey("Boden")) {
							actualSpeed.put("Boden:Start", actualSpeed.getDouble("Boden"));
						}
						if (actualSpeed.containsKey("Luft")) {
							actualSpeed.put("Luft:Start", actualSpeed.getDouble("Luft"));
						}
						break;
					case "Magieresistenz":
						final JSONObject actualMR = basicValues.getObj("Geschwindigkeit").clone(actualBasicValues);
						actualBasicValues.put("Geschwindigkeit", actualMR);
						if (actualMR.containsKey("Wert")) {
							actualMR.put("Start", actualMR.getDouble("Wert"));
						}
						if (actualMR.containsKey("Geist")) {
							actualMR.put("Geist:Start", actualMR.getDouble("Geist"));
						}
						if (actualMR.containsKey("Körper")) {
							actualMR.put("Körper:Start", actualMR.getDouble("Körper"));
						}
						break;
					default:
						actualBasicValues.put(basicValue, basicValues.getObj(basicValue).clone(actualBasicValues));
						break;
				}
			}

			final JSONObject rkw = new JSONObject(actualBasicValues);
			rkw.put("TaW", 3);
			actualBasicValues.put("Ritualkenntnis (Vertrautenmagie)", rkw);

			final JSONObject attacks = animalTypes.getObj(species.getValue()).getObj("Angriffe");
			if (attacks.size() > 0) {
				final JSONObject actualAttacks = animal.getObj("Angriffe");
				for (final String attack : attacks.keySet()) {
					actualAttacks.put(attack, attacks.getObj(attack).clone(actualAttacks));
				}
			}

			if ("Kröte".equals(species.getValue())) {
				skills.put("Krötenschlag", new JSONObject(skills));
			}

			final JSONArray animals = (JSONArray) animal.getParent();
			animals.add(animal);
			animals.notifyListeners(null);

			stage.close();
		});

		if (allowCancellation) {
			cancelButton.setOnAction(_ -> stage.close());
		} else {
			cancelButton.setVisible(false);
			cancelButton.setManaged(false);
		}

		stage.showAndWait();
	}

	private int getAttributeMaximum(final String attribute) {
		return animalTypes.getObj(species.getValue()).getObj("Eigenschaften").getObj(attribute).getIntOrDefault("Maximum", 1);
	}

	private void setAttributeRanges(final Map<String, Tuple<ReactiveSpinner<Integer>, Label>> attributeInputs,
			final boolean setCurrentValue) {
		final JSONObject animalType = animalTypes.getObj(species.getValue());
		final JSONObject animalAttributes = animalType.getObj("Eigenschaften");
		int additionalAP = 0;
		for (final Entry<String, Tuple<ReactiveSpinner<Integer>, Label>> inputs : attributeInputs.entrySet()) {
			final String name = inputs.getKey();
			final JSONObject attribute = animalAttributes.getObj(name);
			final IntegerSpinnerValueFactory value = (IntegerSpinnerValueFactory) inputs.getValue()._1.getValueFactory();
			final int min = attribute.getIntOrDefault("Minimum", 1);
			final int max = getAttributeMaximum(name);
			final int absoluteMax = (int) Math.round(max * (mighty.isSelected() ? 1.5 : 1));
			value.setMin(min);
			value.setMax(absoluteMax);
			if (setCurrentValue) {
				value.setValue(min);
			} else {
				additionalAP += (value.getValue() - min) * 2;
			}
			inputs.getValue()._2.setText(Integer.toString(max) + (mighty.isSelected() ? " (" + absoluteMax + ")" : ""));
		}
		attributePoints.set((mighty.isSelected() ? 40 : 20) - additionalAP / 2);
		if (!setCurrentValue) {
			additionalAP += (lep.getValue() - ((IntegerSpinnerValueFactory) lep.getValueFactory()).getMin()) * 5;
			additionalAP += (asp.getValue() - ((IntegerSpinnerValueFactory) asp.getValueFactory()).getMin()) * 5;
			additionalAP += (aup.getValue() - ((IntegerSpinnerValueFactory) aup.getValueFactory()).getMin()) * 2;
		}

		ap.getValueFactory().setValue((mighty.isSelected() ? 120 : animalType.getIntOrDefault("Bindungskosten", 80)) + additionalAP);
	}

	private void setEnergyBase(final JSONObject basicValues, final String energy, final ReactiveSpinner<Integer> spinner, final Label maxLabel) {
		final int base = basicValues.getObj("Lebensenergie").getIntOrDefault("Wert", 1);
		final IntegerSpinnerValueFactory value = (IntegerSpinnerValueFactory) spinner.getValueFactory();
		value.setMin(base);
		value.setMax(base + 3);
		value.setValue(base);
		maxLabel.setText(Integer.toString(base + 3));
	}

}
