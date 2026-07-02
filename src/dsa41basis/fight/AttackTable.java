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

import dsa41basis.hero.Raisable;
import dsa41basis.ui.hero.AttributeEnhancementDialog;
import dsatool.gui.GUIUtil;
import dsatool.ui.GraphicTableCell;
import dsatool.ui.IntegerSpinnerTableCell;
import dsatool.util.ErrorLogger;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.IntegerProperty;
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

	private final JSONListener updateListener = _ -> updateAttacks();

	private JSONObject character;

	public AttackTable(final JSONObject character, final BooleanExpression isEditable, final DoubleExpression width, final boolean needsStart,
			final boolean needsEnhancement) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("AttackTable.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		newAttackField.editableProperty().bind(isEditable);
		attackAddButton.disableProperty().bind(isEditable.not());

		initAttacks(character, isEditable, width, needsStart, needsEnhancement);
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

	private void initAttacks(final JSONObject character, final BooleanExpression isEditable, final DoubleExpression width, final boolean needsStart,
			final boolean needsEnhancement) {
		attacksTable.prefWidthProperty().bind(width);
		GUIUtil.autosizeTable(attacksTable);
		GUIUtil.cellValueFactories(attacksTable, "name", "tp", "at", "pa", "dk", "notes");

		attackATColumn.setCellFactory(_ -> new IntegerSpinnerTableCell<>(0, 99));
		attackATColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				if (isEditable.get()) {
					t.getRowValue().setAt(t.getNewValue());
				} else if (!t.getNewValue().equals(t.getOldValue())) {
					showEnhancementDialog(t.getRowValue(), t.getNewValue(), false);
				}
			}
		});
		attackATColumn.editableProperty().bind(Bindings.createBooleanBinding(() -> needsEnhancement || isEditable.get(), isEditable));

		attackPAColumn.setCellFactory(_ -> new IntegerSpinnerTableCell<>(0, 99));
		attackPAColumn.setOnEditCommit(t -> {
			if (t.getRowValue() != null) {
				if (isEditable.get()) {
					t.getRowValue().setPa(t.getNewValue());
				} else if (!t.getNewValue().equals(t.getOldValue())) {
					showEnhancementDialog(t.getRowValue(), t.getNewValue(), true);
				}
			}
		});
		attackPAColumn.editableProperty().bind(Bindings.createBooleanBinding(() -> needsEnhancement || isEditable.get(), isEditable));

		attackNameColumn.setCellFactory(_ -> {
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
		attackNameColumn.editableProperty().bind(isEditable);

		attackNotesColumn.setCellFactory(_ -> {
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
		attackNotesColumn.editableProperty().bind(isEditable);

		attacksTable.setRowFactory(_ -> {
			final TableRow<Attack> row = new TableRow<>();

			final ContextMenu contextMenu = new ContextMenu();

			final Consumer<Object> edit = _ -> {
				final Attack attack = row.getItem();
				final Window window = box.getScene().getWindow();
				if (attack != null && !"".equals(attack.getName())) {
					new AttackEditor(window, attack, needsStart);
				}
			};

			row.setOnMouseClicked(event -> {
				if (MouseButton.PRIMARY.equals(event.getButton()) && event.getClickCount() == 2) {
					edit.accept(null);
				}
			});

			if (needsEnhancement) {
				final MenuItem attackEnhanceItem = new MenuItem("Attacke steigern");
				contextMenu.getItems().add(attackEnhanceItem);
				attackEnhanceItem.setOnAction(_ -> {
					final Attack attack = row.getItem();
					showEnhancementDialog(attack, attack.getAt() + 1, false);
				});

				final MenuItem defenseEnhanceItem = new MenuItem("Parade steigern");
				contextMenu.getItems().add(defenseEnhanceItem);
				defenseEnhanceItem.setOnAction(_ -> {
					final Attack attack = row.getItem();
					showEnhancementDialog(attack, attack.getPa() + 1, true);
				});
			}

			final MenuItem editItem = new MenuItem("Bearbeiten");
			contextMenu.getItems().add(editItem);
			editItem.setOnAction(_ -> edit.accept(null));

			final MenuItem deleteItem = new MenuItem("Löschen");
			contextMenu.getItems().add(deleteItem);
			deleteItem.setOnAction(_ -> {
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

	private void showEnhancementDialog(final Attack attack, final int initialTarget, final boolean isDefense) {
		final Raisable dummyRaisable = new Raisable() {

			@Override
			public JSONObject getActual() {
				return attack.getActual();
			}

			@Override
			public int getEnhancementComplexity(final JSONObject hero, final int targetLevel) {
				return 6;
			}

			@Override
			public int getMaximum(final JSONObject hero) {
				return isDefense ? attack.getPaMaximum() : attack.getAtMaximum();
			}

			@Override
			public String getName() {
				return attack.getName() + (isDefense ? ":Parade" : ":Attacke");
			}

			@Override
			public int getValue() {
				return isDefense ? attack.getPa() : attack.getAt();
			}

			@Override
			public IntegerProperty sesProperty() {
				return null;
			}

			@Override
			public void setValue(final int value) {
				if (isDefense) {
					attack.setPa(value);
				} else {
					attack.setAt(value);
				}
			}
		};

		new AttributeEnhancementDialog(box.getScene().getWindow(), dummyRaisable, character, initialTarget);
	}

	private void updateAttacks() {
		attacksTable.getItems().clear();

		final JSONObject attacks = character.getObj("Angriffe");
		for (final String attack : attacks.keySet()) {
			attacksTable.getItems().add(new Attack(attack, attacks.getObj(attack)));
		}
	}
}
