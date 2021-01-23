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

import java.util.function.Consumer;

import dsatool.gui.GUIUtil;
import dsatool.ui.GraphicTableCell;
import dsatool.ui.IntegerSpinnerTableCell;
import dsatool.util.ErrorLogger;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class AttackTable {
	@FXML
	private VBox box;
	@FXML
	private TableColumn<Attack, String> attackNameColumn;
	@FXML
	private TableColumn<Attack, String> attackTPColumn;
	@FXML
	private TableColumn<Attack, Integer> attackATColumn;
	@FXML
	private TableColumn<Attack, Integer> attackPAColumn;
	@FXML
	private TableColumn<Attack, String> attackDistanceColumn;
	@FXML
	private TableColumn<Attack, String> attackNotesColumn;
	@FXML
	private TableView<Attack> attacksTable;
	@FXML
	private TextField newAttackField;
	@FXML
	private Button attackAddButton;

	private final JSONListener updateListener = o -> updateAttacks();

	private JSONObject character;

	public AttackTable(final BooleanExpression isEditable, final DoubleExpression width, final boolean needsStart) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("AttackTable.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		attacksTable.editableProperty().bind(isEditable);
		newAttackField.editableProperty().bind(isEditable);
		attackAddButton.disableProperty().bind(isEditable.not());

		initAttacks(width, needsStart);
	}

	@FXML
	private void addAttack() {
		final JSONObject attacks = character.getObj("Angriffe");
		final JSONObject newAttack = new JSONObject(attacks);
		newAttack.put("Trefferpunkte", new JSONObject(newAttack));
		String name = newAttackField.getText();
		name = "".equals(name) ? "Attacke" : name;
		if (attacks.containsKey(name)) {
			for (int i = 2; i < 100; ++i) {
				if (!attacks.containsKey(name + i)) {
					name = name + i;
					break;
				}
			}
		}
		attacks.put(name, newAttack);
		attacks.notifyListeners(null);
	}

	public Node getControl() {
		return box;
	}

	private void initAttacks(final DoubleExpression width, final boolean needsStart) {
		attacksTable.prefWidthProperty().bind(width);
		GUIUtil.autosizeTable(attacksTable);
		GUIUtil.cellValueFactories(attacksTable, "name", "tp", "at", "pa", "dk", "notes");

		attackATColumn.setCellFactory(o -> new IntegerSpinnerTableCell<>(0, 99));
		attackATColumn.setOnEditCommit(t -> t.getRowValue().setAt(t.getNewValue()));
		attackPAColumn.setCellFactory(o -> new IntegerSpinnerTableCell<>(0, 99));
		attackPAColumn.setOnEditCommit(t -> t.getRowValue().setPa(t.getNewValue()));

		attackNameColumn.setCellFactory(o -> {
			final TableCell<Attack, String> cell = new GraphicTableCell<>(false) {
				@Override
				protected void createGraphic() {
					final TextField t = new TextField();
					createGraphic(t, t::getText, t::setText);
				}
			};
			return cell;
		});
		attackNameColumn.setOnEditCommit(event -> {
			final Attack attack = event.getRowValue();
			attack.setName(event.getNewValue());
		});

		attackNotesColumn.setCellFactory(o -> {
			final TableCell<Attack, String> cell = new GraphicTableCell<>(false) {
				@Override
				protected void createGraphic() {
					final TextField t = new TextField();
					createGraphic(t, t::getText, t::setText);
				}
			};
			return cell;
		});
		attackNotesColumn.setOnEditCommit(event -> {
			final String note = event.getNewValue();
			final Attack attack = event.getRowValue();
			attack.setNotes(note);
		});

		attacksTable.setRowFactory(tableView -> {
			final TableRow<Attack> row = new TableRow<>();

			final ContextMenu contextMenu = new ContextMenu();

			final Consumer<Object> edit = obj -> {
				final Attack attack = row.getItem();
				final Window window = box.getScene().getWindow();
				if (attack != null && !"".equals(attack.getName())) {
					new AttackEditor(window, attack, needsStart);
				}
			};

			row.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					edit.accept(null);
				}
			});

			final MenuItem editItem = new MenuItem("Bearbeiten");
			contextMenu.getItems().add(editItem);
			editItem.setOnAction(event -> edit.accept(null));

			final MenuItem deleteItem = new MenuItem("Löschen");
			contextMenu.getItems().add(deleteItem);
			deleteItem.setOnAction(o -> {
				final Attack attack = row.getItem();
				if (!"".equals(attack.getName())) {
					final String name = attack.getName();
					final JSONObject attacks = character.getObj("Angriffe");
					attacks.removeKey(name);
					attacks.notifyListeners(null);
				}
			});

			row.setContextMenu(contextMenu);

			return row;
		});
	}

	public void setCharacter(final JSONObject newCharacter) {
		if (character != null) {
			character.getObj("Angriffe").removeListener(updateListener);
		}
		character = newCharacter;
		newCharacter.getObj("Angriffe").addListener(updateListener);
		updateAttacks();
	}

	private void updateAttacks() {
		attacksTable.getItems().clear();

		final JSONObject attacks = character.getObj("Angriffe");
		for (final String attack : attacks.keySet()) {
			attacksTable.getItems().add(new Attack(attack, attacks.getObj(attack)));
		}
	}
}
