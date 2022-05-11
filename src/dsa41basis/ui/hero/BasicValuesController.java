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

import java.util.Optional;

import dsa41basis.fight.ArmorEditor;
import dsa41basis.util.HeroUtil;
import dsatool.resources.Settings;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import jsonant.event.JSONListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class BasicValuesController implements JSONListener {

	public enum CharacterType {
		NORMAL, ANIMAL, HORSE, MAGIC_ANIMAL
	}

	@FXML
	private GridPane grid;
	@FXML
	private Node boughtLabel;
	@FXML
	private ReactiveSpinner<Integer> iniBase;
	@FXML
	private ReactiveSpinner<Integer> iniDiceNum;
	@FXML
	private ReactiveSpinner<Integer> iniDiceType;
	@FXML
	private ReactiveSpinner<Integer> iniMod;
	@FXML
	private ReactiveSpinner<Integer> rs;
	@FXML
	private Button zoneRS;
	@FXML
	private ComboBox<String> mrChoice;
	@FXML
	private ReactiveSpinner<Integer> mr;
	@FXML
	private Node mrBox;
	@FXML
	private ReactiveSpinner<Integer> mrMind;
	@FXML
	private ReactiveSpinner<Integer> mrBody;
	@FXML
	private ReactiveSpinner<Integer> mrMod;
	@FXML
	private Node mrModBox;
	@FXML
	private ReactiveSpinner<Integer> mrMindMod;
	@FXML
	private ReactiveSpinner<Integer> mrBodyMod;
	@FXML
	private ReactiveSpinner<Integer> mrBought;
	@FXML
	private Node mrBoughtBox;
	@FXML
	private ReactiveSpinner<Integer> mrMindBought;
	@FXML
	private ReactiveSpinner<Integer> mrBodyBought;
	@FXML
	private ComboBox<String> speedChoice;
	@FXML
	private ReactiveSpinner<Double> speed;
	@FXML
	private Node speedBox;
	@FXML
	private ReactiveSpinner<Double> speedGround;
	@FXML
	private ReactiveSpinner<Double> speedAir;
	@FXML
	private ReactiveSpinner<Double> speedMod;
	@FXML
	private Node speedModBox;
	@FXML
	private ReactiveSpinner<Double> speedGroundMod;
	@FXML
	private ReactiveSpinner<Double> speedAirMod;
	@FXML
	private ReactiveSpinner<Double> speedBought;
	@FXML
	private Node speedBoughtBox;
	@FXML
	private ReactiveSpinner<Double> speedGroundBought;
	@FXML
	private ReactiveSpinner<Double> speedAirBought;
	@FXML
	private Node horseSpeedBox;
	@FXML
	private ReactiveSpinner<Integer> speedWalk;
	@FXML
	private ReactiveSpinner<Integer> speedTrot;
	@FXML
	private ReactiveSpinner<Integer> speedGallop;
	@FXML
	private Node horseSpeedModBox;
	@FXML
	private ReactiveSpinner<Integer> speedWalkMod;
	@FXML
	private ReactiveSpinner<Integer> speedTrotMod;
	@FXML
	private ReactiveSpinner<Integer> speedGallopMod;
	@FXML
	private Label staminaLabel;
	@FXML
	private Node staminaBox;
	@FXML
	private Node staminaModBox;
	@FXML
	private ReactiveSpinner<Integer> staminaTrot;
	@FXML
	private ReactiveSpinner<Integer> staminaGallop;
	@FXML
	private ReactiveSpinner<Integer> staminaTrotMod;
	@FXML
	private ReactiveSpinner<Integer> staminaGallopMod;
	@FXML
	private Node feedBox;
	@FXML
	private ReactiveSpinner<Integer> feedBase;
	@FXML
	private ReactiveSpinner<Integer> feedLight;
	@FXML
	private ReactiveSpinner<Integer> feedMedium;
	@FXML
	private ReactiveSpinner<Integer> feedHeavy;
	@FXML
	private Label tkLabel;
	@FXML
	private Node tkBox;
	@FXML
	private Node tkModBox;
	@FXML
	private ReactiveSpinner<Double> tkFactor;
	@FXML
	private ReactiveSpinner<Double> tkMod;
	@FXML
	private Label tkValue;
	@FXML
	private Label zkLabel;
	@FXML
	private Node zkBox;
	@FXML
	private Node zkModBox;
	@FXML
	private ReactiveSpinner<Double> zkFactor;
	@FXML
	private ReactiveSpinner<Double> zkMod;
	@FXML
	private Label zkValue;
	@FXML
	private Node apBox;
	@FXML
	private ReactiveSpinner<Integer> ap;
	@FXML
	private ReactiveSpinner<Integer> freeAp;
	@FXML
	private ReactiveSpinner<Integer> rkw;

	private JSONObject character;
	private final CharacterType type;

	private ChangeListener<? super Integer> rsListener;

	public BasicValuesController(final BooleanExpression disabled, final CharacterType type, final boolean needsTKZK) {
		this.type = type;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("BasicValues.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final ObservableList<Node> controls = grid.getChildren();

		if (type == CharacterType.HORSE) {
			controls.remove(mr);
			mrChoice.setDisable(true);
			speedChoice.setDisable(true);
			controls.remove(speed);
			controls.remove(speedBox);
			controls.remove(speedMod);
			controls.remove(speedModBox);
		} else {
			controls.remove(horseSpeedBox);
			controls.remove(horseSpeedModBox);
			controls.remove(staminaLabel);
			controls.remove(staminaBox);
			controls.remove(staminaModBox);
			controls.remove(feedBox);
		}

		if (type != CharacterType.MAGIC_ANIMAL) {
			controls.remove(boughtLabel);
			controls.remove(speedBought);
			controls.remove(speedBoughtBox);
			controls.remove(mrMod);
			controls.remove(mrModBox);
			controls.remove(mrBought);
			controls.remove(mrBoughtBox);
			controls.remove(apBox);
		}

		if (!needsTKZK) {
			controls.remove(tkLabel);
			controls.remove(tkBox);
			controls.remove(tkModBox);
			controls.remove(zkLabel);
			controls.remove(zkBox);
			controls.remove(zkModBox);
		}

		iniBase.disableProperty().bind(disabled);
		iniDiceNum.disableProperty().bind(disabled);
		iniDiceType.disableProperty().bind(disabled);
		iniMod.disableProperty().bind(disabled);
		rs.disableProperty().bind(disabled);
		zoneRS.disableProperty().bind(disabled);
		zoneRS.setVisible(type == CharacterType.NORMAL && "Zonenrüstung".equals(Settings.getSettingStringOrDefault("Zonenrüstung", "Kampf", "Rüstungsart")));
		mrChoice.disableProperty().bind(disabled.or(new SimpleBooleanProperty(type == CharacterType.HORSE)));
		mr.disableProperty().bind(disabled);
		mrMind.disableProperty().bind(disabled);
		mrBody.disableProperty().bind(disabled);
		mrBought.disableProperty().bind(disabled);
		mrMindBought.disableProperty().bind(disabled);
		mrBodyBought.disableProperty().bind(disabled);
		mrMod.disableProperty().bind(disabled);
		mrMindMod.disableProperty().bind(disabled);
		mrBodyMod.disableProperty().bind(disabled);
		speedChoice.disableProperty().bind(disabled.or(new SimpleBooleanProperty(type == CharacterType.HORSE)));
		speed.disableProperty().bind(disabled);
		speedGround.disableProperty().bind(disabled);
		speedAir.disableProperty().bind(disabled);
		speedBought.disableProperty().bind(disabled);
		speedGroundBought.disableProperty().bind(disabled);
		speedAirBought.disableProperty().bind(disabled);
		speedMod.disableProperty().bind(disabled);
		speedGroundMod.disableProperty().bind(disabled);
		speedAirMod.disableProperty().bind(disabled);
		speedWalk.disableProperty().bind(disabled);
		speedTrot.disableProperty().bind(disabled);
		speedGallop.disableProperty().bind(disabled);
		speedWalkMod.disableProperty().bind(disabled);
		speedTrotMod.disableProperty().bind(disabled);
		speedGallopMod.disableProperty().bind(disabled);
		staminaTrot.disableProperty().bind(disabled);
		staminaGallop.disableProperty().bind(disabled);
		staminaTrotMod.disableProperty().bind(disabled);
		staminaGallopMod.disableProperty().bind(disabled);
		feedBase.disableProperty().bind(disabled);
		feedLight.disableProperty().bind(disabled);
		feedMedium.disableProperty().bind(disabled);
		feedHeavy.disableProperty().bind(disabled);
		tkFactor.disableProperty().bind(disabled);
		tkMod.disableProperty().bind(disabled);
		zkFactor.disableProperty().bind(disabled);
		zkMod.disableProperty().bind(disabled);
		freeAp.disableProperty().bind(disabled);
		rkw.disableProperty().bind(disabled);

		init();
	}

	private ChangeListener<Double> doubleListener(final String value, final String key) {
		return (o, oldV, newV) -> {
			final JSONObject actualValue = character.getObj("Basiswerte").getObj(value);
			actualValue.put(key, newV);
			actualValue.notifyListeners(this);
		};
	}

	private JSONObject getArmorItem() {
		final JSONArray armorList = character.getObj("Besitz").getArr("Ausrüstung");
		JSONObject armor = null;
		for (int i = 0; i < armorList.size(); ++i) {
			final JSONObject item = armorList.getObj(i);
			if ("Rüstung".equals(item.getString("Name"))) {
				armor = item;
				break;
			}
		}
		if (armor == null) {
			armor = new JSONObject(armorList);
			armorList.add(armor);
			armor.put("Name", "Rüstung");
			armor.getArr("Kategorien").add("Rüstung");
		}
		return armor;
	}

	public Node getControl() {
		return grid;
	}

	private void init() {
		iniBase.valueProperty().addListener(listener("Initiative-Basis", "Wert"));
		iniDiceNum.valueProperty().addListener(listener("Initiative", "Würfel:Anzahl"));
		iniDiceType.valueProperty().addListener(listener("Initiative", "Würfel:Typ"));
		iniMod.valueProperty().addListener(listener("Initiative-Basis", "Modifikator"));

		rsListener = (o, oldV, newV) -> {
			if (type == CharacterType.NORMAL) {
				final JSONObject armor = getArmorItem();
				if (armor.containsKey("Rüstungsschutz")) {
					final Alert deleteConfirmation = new Alert(AlertType.CONFIRMATION);
					deleteConfirmation.setTitle("Zonenrüstungswerte löschen?");
					deleteConfirmation.setHeaderText("Zonenrüstungswerte löschen?");
					deleteConfirmation.setContentText("Die Werte können danach nicht wiederhergestellt werden!");
					deleteConfirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

					final Optional<ButtonType> result = deleteConfirmation.showAndWait();
					if (!result.isPresent() || !result.get().equals(ButtonType.YES)) return;
				}
				armor.removeKey("Rüstungsschutz");
				armor.put("Gesamtrüstungsschutz", newV);
			} else {
				final JSONObject actualValue = character.getObj("Vorteile").getObj("Natürlicher Rüstungsschutz");
				actualValue.put("Stufe", newV);
				actualValue.notifyListeners(this);
			}
		};

		rs.valueProperty().addListener(rsListener);
		zoneRS.setOnAction(e -> new ArmorEditor(grid.getScene().getWindow(), getArmorItem()));

		mrChoice.setItems(FXCollections.observableArrayList("Magieresistenz", "MR (Geist/Körper)"));

		final BooleanBinding isSingleMR = mrChoice.getSelectionModel().selectedItemProperty().isEqualTo("Magieresistenz");

		mr.managedProperty().bind(isSingleMR);
		mr.visibleProperty().bind(isSingleMR);
		mrBox.managedProperty().bind(isSingleMR.not());
		mrBox.visibleProperty().bind(isSingleMR.not());

		if (type == CharacterType.MAGIC_ANIMAL) {
			mrMod.managedProperty().bind(isSingleMR);
			mrMod.visibleProperty().bind(isSingleMR);
			mrModBox.managedProperty().bind(isSingleMR.not());
			mrModBox.visibleProperty().bind(isSingleMR.not());
			mrBought.managedProperty().bind(isSingleMR);
			mrBought.visibleProperty().bind(isSingleMR);
			mrBoughtBox.managedProperty().bind(isSingleMR.not());
			mrBoughtBox.visibleProperty().bind(isSingleMR.not());
		}

		isSingleMR.addListener((o, oldV, newV) -> {
			final JSONObject actualMr = character.getObj("Basiswerte").getObj("Magieresistenz");
			if (newV) {
				actualMr.removeKey("Geist");
			} else {
				actualMr.put("Geist", actualMr.getIntOrDefault("Wert", 0));
			}
		});

		mr.valueProperty().addListener(listener("Magieresistenz", "Wert"));
		mrMind.valueProperty().addListener(listener("Magieresistenz", "Geist"));
		mrBody.valueProperty().addListener(listener("Magieresistenz", "Körper"));
		if (type == CharacterType.MAGIC_ANIMAL) {
			mrMod.valueProperty().addListener(listener("Magieresistenz", "Modifikator"));
			mrMindMod.valueProperty().addListener(listener("Magieresistenz", "Geist:Modifikator"));
			mrBodyMod.valueProperty().addListener(listener("Magieresistenz", "Körper:Modifikator"));
			mrBought.valueProperty().addListener(listener("Magieresistenz", "Kauf"));
			mrMindBought.valueProperty().addListener(listener("Magieresistenz", "Geist:Kauf"));
			mrBodyBought.valueProperty().addListener(listener("Magieresistenz", "Körper:Kauf"));

			ap.valueProperty().addListener((o, oldV, newV) -> freeAp.getValueFactory().setValue(freeAp.getValue() + newV - oldV));
			freeAp.valueProperty().addListener((o, oldV, newV) -> {
				final JSONObject biography = character.getObj("Biografie");
				biography.put("Abenteuerpunkte", ap.getValue());
				biography.put("Abenteuerpunkte-Guthaben", freeAp.getValue());
			});
			rkw.valueProperty().addListener((o, oldV, newV) -> {
				if (oldV == null || newV == null || oldV.equals(newV)) return;
				final JSONObject ritualKnowledge = character.getObj("Basiswerte").getObj("Ritualkenntnis (Vertrautenmagie)");
				ritualKnowledge.put("TaW", newV);
				ritualKnowledge.notifyListeners(null);
			});
		}

		if (type == CharacterType.HORSE) {
			speedChoice.setItems(FXCollections.observableArrayList("GS (Schritt/Trab/Galopp)"));
			speedChoice.getSelectionModel().select(0);

			speedWalk.valueProperty().addListener(listener("Geschwindigkeit", "Schritt"));
			speedTrot.valueProperty().addListener(listener("Geschwindigkeit", "Trab"));
			speedGallop.valueProperty().addListener(listener("Geschwindigkeit", "Galopp"));
			speedWalkMod.valueProperty().addListener(listener("Geschwindigkeit", "Schritt:Modifikator"));
			speedTrotMod.valueProperty().addListener(listener("Geschwindigkeit", "Trab:Modifikator"));
			speedGallopMod.valueProperty().addListener(listener("Geschwindigkeit", "Galopp:Modifikator"));

			staminaTrot.valueProperty().addListener(listener("Ausdauer", "Trab"));
			staminaGallop.valueProperty().addListener(listener("Ausdauer", "Galopp"));
			staminaTrotMod.valueProperty().addListener(listener("Ausdauer", "Trab:Modifikator"));
			staminaGallopMod.valueProperty().addListener(listener("Ausdauer", "Galopp:Modifikator"));

			feedBase.valueProperty().addListener(listener("Futterbedarf", "Erhaltung"));
			feedLight.valueProperty().addListener(listener("Futterbedarf", "Leicht"));
			feedMedium.valueProperty().addListener(listener("Futterbedarf", "Mittel"));
			feedHeavy.valueProperty().addListener(listener("Futterbedarf", "Schwer"));

			tkFactor.valueProperty().addListener(listenerDouble("Tragkraft", "Wert"));
			tkFactor.valueProperty().addListener((o, oldV, newV) -> updateTkZk());
			tkMod.valueProperty().addListener(listenerDouble("Tragkraft", "Modifikator"));
			zkFactor.valueProperty().addListener(listenerDouble("Zugkraft", "Wert"));
			zkFactor.valueProperty().addListener((o, oldV, newV) -> updateTkZk());
			zkMod.valueProperty().addListener(listenerDouble("Zugkraft", "Modifikator"));
		} else {
			speedChoice.setItems(FXCollections.observableArrayList("Geschwindigkeit", "GS (Boden/Luft)"));

			final BooleanBinding isSingleSpeed = speedChoice.getSelectionModel().selectedItemProperty().isEqualTo("Geschwindigkeit");
			speed.managedProperty().bind(isSingleSpeed);
			speed.visibleProperty().bind(isSingleSpeed);
			speedBox.managedProperty().bind(isSingleSpeed.not());
			speedBox.visibleProperty().bind(isSingleSpeed.not());
			speedMod.managedProperty().bind(isSingleSpeed);
			speedMod.visibleProperty().bind(isSingleSpeed);
			speedModBox.managedProperty().bind(isSingleSpeed.not());
			speedModBox.visibleProperty().bind(isSingleSpeed.not());
			speedBought.managedProperty().bind(isSingleSpeed);
			speedBought.visibleProperty().bind(isSingleSpeed);
			speedBoughtBox.managedProperty().bind(isSingleSpeed.not());
			speedBoughtBox.visibleProperty().bind(isSingleSpeed.not());

			isSingleSpeed.addListener((o, oldV, newV) -> {
				final JSONObject actualSpeed = character.getObj("Basiswerte").getObj("Geschwindigkeit");
				if (newV) {
					actualSpeed.removeKey("Boden");
				} else {
					actualSpeed.put("Boden", actualSpeed.getDoubleOrDefault("Wert", 0.0));
				}
			});

			speed.valueProperty().addListener(doubleListener("Geschwindigkeit", "Wert"));
			speedGround.valueProperty().addListener(doubleListener("Geschwindigkeit", "Boden"));
			speedAir.valueProperty().addListener(doubleListener("Geschwindigkeit", "Luft"));
			speedMod.valueProperty().addListener(doubleListener("Geschwindigkeit", "Modifikator"));
			speedGroundMod.valueProperty().addListener(doubleListener("Geschwindigkeit", "Boden:Modifikator"));
			speedAirMod.valueProperty().addListener(doubleListener("Geschwindigkeit", "Luft:Modifikator"));

			if (type == CharacterType.MAGIC_ANIMAL) {
				speedBought.valueProperty().addListener(doubleListener("Geschwindigkeit", "Kauf"));
				speedGroundBought.valueProperty().addListener(doubleListener("Geschwindigkeit", "Boden:Kauf"));
				speedAirBought.valueProperty().addListener(doubleListener("Geschwindigkeit", "Luft:Kauf"));
			}
		}
	}

	private ChangeListener<Integer> listener(final String value, final String key) {
		return (o, oldV, newV) -> {
			final JSONObject actualValue = character.getObj("Basiswerte").getObj(value);
			actualValue.put(key, newV);
			actualValue.notifyListeners(this);
		};
	}

	private ChangeListener<Double> listenerDouble(final String value, final String key) {
		return (o, oldV, newV) -> {
			final JSONObject actualValue = character.getObj("Basiswerte").getObj(value);
			final int intV = (int) (double) newV;
			if (intV == newV) {
				actualValue.put(key, intV);
			} else {
				actualValue.put(key, newV);
			}
			actualValue.notifyListeners(this);
		};
	}

	@Override
	public void notifyChanged(final JSONValue changed) {
		setCharacter(character);
	}

	public void setCharacter(final JSONObject character) {
		if (this.character != null) {
			this.character.getObj("Eigenschaften").getObj("KK").removeListener(this);
			this.character.getObj("Basiswerte").removeListener(this);
		}
		this.character = character;

		final JSONObject baseValues = character.getObj("Basiswerte");
		baseValues.addListener(this);

		final JSONObject iniBaseValues = baseValues.getObj("Initiative-Basis");
		final JSONObject ini = baseValues.getObj("Initiative");
		iniBase.getValueFactory().setValue(iniBaseValues.getIntOrDefault("Wert", 0));
		iniDiceNum.getValueFactory().setValue(ini.getIntOrDefault("Würfel:Anzahl", 1));
		iniDiceType.getValueFactory().setValue(ini.getIntOrDefault("Würfel:Typ", 6));
		iniMod.getValueFactory().setValue(iniBaseValues.getIntOrDefault("Modifikator", 0));

		if (type == CharacterType.NORMAL) {
			final JSONObject armor = getArmorItem();
			if (armor.containsKey("Gesamtrüstungsschutz")) {
				rs.getValueFactory().setValue(armor.getInt("Gesamtrüstungsschutz"));
			} else {
				rs.valueProperty().removeListener(rsListener);
				rs.getValueFactory().setValue(0);
				rs.valueProperty().addListener(rsListener);
			}
		} else {
			final JSONObject actualValue = character.getObj("Vorteile").getObj("Natürlicher Rüstungsschutz");
			rs.getValueFactory().setValue(actualValue.getIntOrDefault("Stufe", 0));
		}

		final JSONObject actualMr = baseValues.getObj("Magieresistenz");
		mrChoice.getSelectionModel().select(actualMr.containsKey("Geist") || type == CharacterType.HORSE ? 1 : 0);
		mr.getValueFactory().setValue(actualMr.getIntOrDefault("Wert", 0));
		mrMind.getValueFactory().setValue(actualMr.getIntOrDefault("Geist", 0));
		mrBody.getValueFactory().setValue(actualMr.getIntOrDefault("Körper", 0));
		if (type == CharacterType.MAGIC_ANIMAL) {
			mrMod.getValueFactory().setValue(actualMr.getIntOrDefault("Modifikator", 0));
			mrMindMod.getValueFactory().setValue(actualMr.getIntOrDefault("Geist:Modifikator", 0));
			mrBodyMod.getValueFactory().setValue(actualMr.getIntOrDefault("Körper:Modifikator", 0));
			mrBought.getValueFactory().setValue(actualMr.getIntOrDefault("Kauf", 0));
			mrMindBought.getValueFactory().setValue(actualMr.getIntOrDefault("Geist:Kauf", 0));
			mrBodyBought.getValueFactory().setValue(actualMr.getIntOrDefault("Körper:Kauf", 0));

			final JSONObject biography = character.getObj("Biografie");
			ap.getValueFactory().setValue(biography.getIntOrDefault("Abenteuerpunkte", 0));
			freeAp.getValueFactory().setValue(biography.getIntOrDefault("Abenteuerpunkte-Guthaben", 0));
			rkw.getValueFactory().setValue(baseValues.getObj("Ritualkenntnis (Vertrautenmagie)").getIntOrDefault("TaW", 3));
		}

		if (type == CharacterType.HORSE) {
			speedChoice.setItems(FXCollections.observableArrayList("GS (Schritt/Trab/Galopp)"));
			speedChoice.getSelectionModel().select(0);
			final JSONObject speed = baseValues.getObj("Geschwindigkeit");
			speedWalk.getValueFactory().setValue(speed.getIntOrDefault("Schritt", 0));
			speedTrot.getValueFactory().setValue(speed.getIntOrDefault("Trab", 0));
			speedGallop.getValueFactory().setValue(speed.getIntOrDefault("Galopp", 0));
			speedWalkMod.getValueFactory().setValue(speed.getIntOrDefault("Schritt:Modifikator", 0));
			speedTrotMod.getValueFactory().setValue(speed.getIntOrDefault("Trab:Modifikator", 0));
			speedGallopMod.getValueFactory().setValue(speed.getIntOrDefault("Galopp:Modifikator", 0));

			final JSONObject stamina = baseValues.getObj("Ausdauer");
			staminaTrot.getValueFactory().setValue(stamina.getIntOrDefault("Trab", 0));
			staminaGallop.getValueFactory().setValue(stamina.getIntOrDefault("Galopp", 0));
			staminaTrotMod.getValueFactory().setValue(stamina.getIntOrDefault("Trab:Modifikator", 0));
			staminaGallopMod.getValueFactory().setValue(stamina.getIntOrDefault("Galopp:Modifikator", 0));

			final JSONObject feed = baseValues.getObj("Futterbedarf");
			feedBase.getValueFactory().setValue(feed.getIntOrDefault("Erhaltung", 0));
			feedLight.getValueFactory().setValue(feed.getIntOrDefault("Leicht", 0));
			feedMedium.getValueFactory().setValue(feed.getIntOrDefault("Mittel", 0));
			feedHeavy.getValueFactory().setValue(feed.getIntOrDefault("Schwer", 0));

			final JSONObject tk = baseValues.getObj("Tragkraft");
			tkFactor.getValueFactory().setValue(tk.getDoubleOrDefault("Wert", 1.0));
			tkMod.getValueFactory().setValue(tk.getDoubleOrDefault("Modifikator", 0.0));

			final JSONObject zk = baseValues.getObj("Zugkraft");
			zkFactor.getValueFactory().setValue(zk.getDoubleOrDefault("Wert", 1.0));
			zkMod.getValueFactory().setValue(zk.getDoubleOrDefault("Modifikator", 0.0));

			final JSONObject strength = character.getObj("Eigenschaften").getObj("KK");
			strength.addListener(this);
			updateTkZk();
		} else {
			final JSONObject actualSpeed = baseValues.getObj("Geschwindigkeit");
			speedChoice.getSelectionModel().select(actualSpeed.containsKey("Boden") ? 1 : 0);

			speed.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Wert", 0.0));
			speedGround.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Boden", 0.0));
			speedAir.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Luft", 0.0));
			speedMod.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Modifikator", 0.0));
			speedGroundMod.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Boden:Modifikator", 0.0));
			speedAirMod.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Luft:Modifikator", 0.0));

			if (type == CharacterType.MAGIC_ANIMAL) {
				speedBought.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Kauf", 0.0));
				speedGroundBought.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Boden:Kauf", 0.0));
				speedAirBought.getValueFactory().setValue(actualSpeed.getDoubleOrDefault("Luft:Kauf", 0.0));
			}
		}
	}

	private void updateTkZk() {
		final int strength = HeroUtil.getCurrentValue(character.getObj("Eigenschaften").getObj("KK"), true);
		tkValue.setText(Integer.toString((int) Math.round(tkFactor.getValue() * strength)));
		zkValue.setText(Integer.toString((int) Math.round(zkFactor.getValue() * strength)));
	}
}
