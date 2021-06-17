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

import java.util.Arrays;

import dsa41basis.fight.WithAttack;
import dsa41basis.fight.WithDefense;
import dsa41basis.hero.Attribute;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONObject;

public class SingleRollDialog {

	public enum Type {
		ATTRIBUTE("Eigenschaftsprobe"), ATTACK("Attacke"), DEFENSE("Parade");

		public String title;

		Type(final String title) {
			this.title = title;
		}
	}

	@FXML
	private VBox root;
	@FXML
	private Button okButton;
	@FXML
	private ReactiveSpinner<Integer> dice1;
	@FXML
	private ReactiveSpinner<Integer> dice2;
	@FXML
	private ReactiveSpinner<Integer> mod;
	@FXML
	private Label result;
	@FXML
	private Node zoneBox;
	@FXML
	private ReactiveSpinner<Integer> zoneDice;
	@FXML
	private Label zoneLabel;
	@FXML
	private CheckBox back;
	@FXML
	private Node tpBox;
	@FXML
	private ReactiveSpinner<Integer> tp;
	@FXML
	private Label tpModLabel;
	@FXML
	private Label tpMod;
	@FXML
	private Label tpResult;
	@FXML
	private Node tpModifiersBox;
	@FXML
	private CheckBox staminaDamage;
	@FXML
	private CheckBox reducedWoundThreshold;
	@FXML
	private CheckBox unarmed;
	@FXML
	private Node woundBox;
	@FXML
	private Label wounds;

	private final Type type;
	private final JSONObject hero;
	private final Object target;

	public SingleRollDialog(final Window window, final Type type, final JSONObject hero, final Object target) {
		this.type = type;
		this.hero = hero;
		this.target = target;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("SingleRollDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final Stage stage = new Stage();
		stage.setTitle(type.title);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setResizable(false);
		stage.initOwner(window);

		dice1.getValueFactory().setValue(10); // Won't get an update of the listener for newV == 1 otherwise

		dice1.valueProperty().addListener((o, oldV, newV) -> {
			final boolean isSpecial = newV == 1 || newV == 20;
			if (isSpecial) {
				dice2.getValueFactory().setValue(DSAUtil.diceRoll(20));
			}
			dice2.setVisible(isSpecial);
			updateInterpretation();
		});

		dice1.getValueFactory().setValue(DSAUtil.diceRoll(20));

		dice2.valueProperty().addListener((o, oldV, newV) -> updateInterpretation());
		mod.valueProperty().addListener((o, oldV, newV) -> updateInterpretation());
		zoneDice.valueProperty().addListener((o, oldV, newV) -> updateInterpretation());
		back.selectedProperty().addListener((o, oldV, newV) -> updateInterpretation());
		tp.valueProperty().addListener((o, oldV, newV) -> updateInterpretation());
		staminaDamage.selectedProperty().addListener((o, oldV, newV) -> updateInterpretation());
		reducedWoundThreshold.selectedProperty().addListener((o, oldV, newV) -> updateInterpretation());
		unarmed.selectedProperty().addListener((o, oldV, newV) -> updateInterpretation());

		okButton.setOnAction(e -> stage.close());
		okButton.setDefaultButton(true);

		final boolean needsZones = type != Type.ATTRIBUTE
				&& !"Gesamtrüstung".equals(Settings.getSettingStringOrDefault("Zonenrüstung", "Kampf", "Rüstungsart"));
		zoneBox.setVisible(needsZones);
		zoneBox.setManaged(needsZones);

		if (type == Type.ATTRIBUTE) {
			stage.setScene(new Scene(root, 210, 103));

			tpBox.setVisible(false);
			tpBox.setManaged(false);
		} else {
			zoneDice.getValueFactory().setValue(DSAUtil.diceRoll(20));

			tpBox.setVisible(true);
			if (type == Type.DEFENSE) {
				stage.setScene(new Scene(root, 400, 191 - (needsZones ? 0 : 19)));

				tpModLabel.setText("-");
			} else {
				stage.setScene(new Scene(root, 245, 164 - (needsZones ? 0 : 19)));

				final WithAttack weapon = (WithAttack) target;
				final Tuple3<Integer, Integer, Integer> tpRaw = weapon.getTpRaw();

				int tpRoll = 0;
				for (int i = 0; i < tpRaw._1; ++i) {
					tpRoll += DSAUtil.diceRoll(tpRaw._2);
				}
				tp.getValueFactory().setValue(tpRoll);

				String tpModString = tpRaw._3.toString();
				final Tuple<Boolean, Boolean> tpMods = weapon.getTPModifiers();
				if (tpMods._1) {
					tpModString += "(A)";
				}
				if (tpMods._2) {
					tpModString += '*';
				}
				tpMod.setText(tpModString);
			}
		}

		if (type != Type.DEFENSE) {
			tpModifiersBox.setVisible(false);
			tpModifiersBox.setManaged(false);
			woundBox.setVisible(false);
			woundBox.setManaged(false);
		}

		stage.show();
	}

	private String getZone(final int roll) {
		final JSONObject zones = ResourceManager.getResource("data/Wunden").getObj("Zonenwunden");
		for (final String zone : zones.keySet()) {
			if (zones.getObj(zone).getObj("Zufall").getArr("Werte").contains(roll)) {
				final boolean needsBack = Arrays.asList("Brust", "Bauch").contains(zone);
				back.setVisible(needsBack);
				back.setManaged(needsBack);
				if ("Beine".equals(zone))
					return roll % 2 == 0 ? "Rechtes Bein" : "Linkes Bein";
				return zone;
			}
		}
		return "";
	}

	private int interpret(final int roll) {
		final int toCompare = switch (type) {
			case ATTRIBUTE -> ((Attribute) target).getCurrent();
			case ATTACK -> ((WithAttack) target).getAt();
			case DEFENSE -> ((WithDefense) target).getPa();
		};

		return toCompare - roll - mod.getValue();
	}

	private void updateInterpretation() {
		result.setStyle("");

		int interpretation;
		String resultText;

		final int value = dice1.getValue();
		if (value == 1) {
			result.setTextFill(Color.GREEN);
			interpretation = interpret(dice2.getValue());
			if (interpretation >= 0) {
				resultText = "glücklich bestätigt";
				result.setStyle("-fx-font-weight: bold");
			} else {
				resultText = "glücklich";
			}
		} else if (value == 20) {
			result.setTextFill(Color.RED);
			interpretation = interpret(dice2.getValue());
			if (interpretation < 0) {
				resultText = "Patzer";
				result.setStyle("-fx-font-weight: bold");
			} else {
				resultText = "Patzer abgewendet";
			}
		} else {
			interpretation = interpret(dice1.getValue());
			if (interpretation >= 0) {
				result.setTextFill(Color.GREEN);
				resultText = "Erfolg";
			} else {
				result.setTextFill(Color.RED);
				resultText = "Misserfolg";
			}
		}

		result.setText(resultText + " (" + interpretation + ")");

		if (type != Type.ATTRIBUTE) {
			final String zone = getZone(zoneDice.getValue());
			zoneLabel.setText(zone);

			if (type == Type.DEFENSE) {
				final String armorZone = switch (zone) {
					case "Schwertarm" -> "Rechter Arm";
					case "Schildarm" -> "Linker Arm";
					case "Brust", "Bauch" -> back.isSelected() ? "Rücken" : zone;
					default -> zone;
				};

				final int zoneRS = HeroUtil.getZoneRS(hero, armorZone);
				final int sp = Math.max(tp.getValue() - zoneRS, 0);

				final double woundThreshold = HeroUtil.deriveValueRaw(ResourceManager.getResource("data/Basiswerte").getObj("Wundschwelle"), hero);
				final int woundModifier = hero.getObj("Basiswerte").getObj("Wundschwelle").getIntOrDefault("Modifikator", 0) + (unarmed.isSelected() ? 2 : 0)
						- (reducedWoundThreshold.isSelected() ? 2 : 0);

				final int actualSP = staminaDamage.isSelected() ? (sp + (unarmed.isSelected() ? 0 : 1)) / 2 : sp;

				for (int i = 3; i > 0; --i) {
					if (actualSP > (int) Math.round(i * woundThreshold + woundModifier)) {
						wounds.setText(i + " Wunde" + (i > 1 ? "n" : ""));
						break;
					}
					wounds.setText("keine Wunden");
				}

				final String tpResultText = Integer.toString(sp) + (staminaDamage.isSelected() ? " (" + actualSP + "SP)" : "");

				tpResult.setText(tpResultText);
				tpMod.setText(Integer.toString(zoneRS));
			} else {
				tpResult.setText(Integer.toString(tp.getValue() + ((WithAttack) target).getTpRaw()._3));
			}
		}
	}
}
