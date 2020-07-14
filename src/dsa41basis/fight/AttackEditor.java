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
package dsa41basis.fight;

import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple3;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class AttackEditor {
	@FXML
	private VBox root;
	@FXML
	private TextField name;
	@FXML
	private ReactiveSpinner<Integer> tpNumDice;
	@FXML
	private ReactiveSpinner<Integer> tpTypeDice;
	@FXML
	private ReactiveSpinner<Integer> tpAdditional;
	@FXML
	private CheckBox tpStamina;
	@FXML
	private CheckBox tpWound;
	@FXML
	private ReactiveSpinner<Integer> tpMod;
	@FXML
	private ReactiveSpinner<Integer> atValue;
	@FXML
	private ReactiveSpinner<Integer> atMod;
	@FXML
	private Node atStartBox;
	@FXML
	private ReactiveSpinner<Integer> atStart;
	@FXML
	private ReactiveSpinner<Integer> paValue;
	@FXML
	private ReactiveSpinner<Integer> paMod;
	@FXML
	private Node paStartBox;
	@FXML
	private ReactiveSpinner<Integer> paStart;
	@FXML
	private CheckBox dkH;
	@FXML
	private CheckBox dkN;
	@FXML
	private CheckBox dkS;
	@FXML
	private CheckBox dkP;
	@FXML
	private TextField notes;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	public AttackEditor(final Window window, final Attack attack, final boolean needsStart) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("AttackEditor.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final Stage stage = new Stage();
		stage.setTitle("Bearbeiten");
		stage.setScene(new Scene(root, 330, 215));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setResizable(false);
		stage.initOwner(window);

		if (!needsStart) {
			atStartBox.setManaged(false);
			atStartBox.setVisible(false);
			paStartBox.setManaged(false);
			paStartBox.setVisible(false);
		}

		name.setText(attack.getName());
		final Tuple3<Integer, Integer, Integer> tpValues = attack.getTpRaw();
		tpNumDice.getValueFactory().setValue(tpValues._1);
		tpTypeDice.getValueFactory().setValue(tpValues._2);
		tpAdditional.getValueFactory().setValue(tpValues._3);
		final String tp = attack.getTp();
		tpStamina.setSelected(tp.contains("A"));
		tpWound.setSelected(tp.contains("*"));
		tpMod.getValueFactory().setValue(attack.getTpMod());
		atValue.getValueFactory().setValue(attack.getAt());
		atMod.getValueFactory().setValue(attack.getAtMod());
		atStart.getValueFactory().setValue(attack.getAtStart());
		paValue.getValueFactory().setValue(attack.getPa());
		paMod.getValueFactory().setValue(attack.getPaMod());
		paStart.getValueFactory().setValue(attack.getPaStart());
		final String dk = attack.getDk();
		dkH.setSelected(dk.contains("H"));
		dkN.setSelected(dk.contains("N"));
		dkS.setSelected(dk.contains("S"));
		dkP.setSelected(dk.contains("P"));
		notes.setText(attack.getNotes());

		okButton.setOnAction(event -> {
			attack.setName(name.getText());
			attack.setTp(tpTypeDice.getValue(), tpNumDice.getValue(), tpAdditional.getValue(), tpWound.isSelected(), tpStamina.isSelected(), tpMod.getValue());
			attack.setAt(atValue.getValue());
			attack.setAtMod(atMod.getValue());
			attack.setAtStart(needsStart ? atStart.getValue() : 0);
			attack.setPa(paValue.getValue());
			attack.setPaMod(paMod.getValue());
			attack.setPaStart(needsStart ? paStart.getValue() : 0);
			attack.setDK((dkH.isSelected() ? "H" : "") + (dkN.isSelected() ? "N" : "") + (dkS.isSelected() ? "S" : "") + (dkP.isSelected() ? "P" : ""));
			attack.setNotes(notes.getText());
			stage.close();
		});

		cancelButton.setOnAction(event -> stage.close());

		stage.show();
	}
}
