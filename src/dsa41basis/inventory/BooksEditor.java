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
package dsa41basis.inventory;

import java.util.HashSet;
import java.util.Set;

import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.ui.IntegerSpinnerTableCell;
import dsatool.ui.ReactiveComboBox;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONObject;

public class BooksEditor {
	@FXML
	private VBox root;
	@FXML
	private TableView<Tuple<String, Integer>> booksTable;
	@FXML
	private TableColumn<Tuple<String, Integer>, Integer> pageColumn;
	@FXML
	private ReactiveComboBox<String> bookList;
	@FXML
	private Button addBook;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	private InventoryItem item;
	private boolean isDefault;
	private final Set<String> chosen = new HashSet<>();

	public BooksEditor(final Window window, final InventoryItem item) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("BooksEditor.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final Stage stage = new Stage();
		stage.setTitle("Buchreferenzen");
		stage.setScene(new Scene(root, 290, 85));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setResizable(false);
		stage.initOwner(window);

		booksTable.heightProperty().addListener((o, oldV, newV) -> stage.setHeight(stage.getHeight() + newV.doubleValue() - oldV.doubleValue()));

		GUIUtil.autosizeTable(booksTable);
		GUIUtil.cellValueFactories(booksTable, "_1", "_2");

		pageColumn.setCellFactory(c -> new IntegerSpinnerTableCell<>(1, 999));
		pageColumn.setOnEditCommit(e -> {
			isDefault = false;
			booksTable.getItems().set(e.getTablePosition().getRow(), new Tuple<>(e.getRowValue()._1, e.getNewValue()));
		});

		booksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		booksTable.setRowFactory(table -> {
			final TableRow<Tuple<String, Integer>> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem deleteItem = new MenuItem("Entfernen");
			deleteItem.setOnAction(event -> {
				final Tuple<String, Integer> book = row.getItem();
				final String name = book._1;
				chosen.remove(name);
				booksTable.getItems().remove(book);
				initList();
			});
			contextMenu.getItems().add(deleteItem);
			row.contextMenuProperty().bind(Bindings.when(row.itemProperty().isNotNull()).then(contextMenu).otherwise((ContextMenu) null));

			GUIUtil.dragDropReorder(row, moved -> isDefault = false, booksTable);

			return row;
		});

		addBook.disableProperty().bind(Bindings.isEmpty(bookList.getItems()));

		okButton.setOnAction(event -> {
			final JSONObject actual = item.getItem();
			if (isDefault) {
				if (actual.containsKey("Regelwerke")) {
					actual.removeKey("Regelwerke");
					actual.notifyListeners(null);
				}
			} else {
				final JSONObject newBooks = new JSONObject(actual);
				for (final Tuple<String, Integer> book : booksTable.getItems()) {
					newBooks.put(book._1, book._2);
				}
				actual.put("Regelwerke", newBooks);
				actual.notifyListeners(null);
			}
			stage.close();
		});

		cancelButton.setOnAction(event -> stage.close());

		okButton.setDefaultButton(true);
		cancelButton.setCancelButton(true);

		this.item = item;

		isDefault = !item.getItem().containsKey("Regelwerke") && !item.getBaseItem().containsKey("Regelwerke");

		stage.show();

		fillTable();

		initList();
	}

	@FXML
	private void addBook() {
		final String selected = bookList.getSelectionModel().getSelectedItem();
		chosen.add(selected);
		bookList.getItems().remove(selected);
		booksTable.getItems().add(new Tuple<>(selected, 1));
		isDefault = false;
	}

	private void fillTable() {
		booksTable.getItems().clear();

		final JSONObject actual = item.getItem();
		final JSONObject baseItem = item.getBaseItem();

		JSONObject books;
		if (isDefault) {
			final JSONObject equipment = ResourceManager.getResource("data/Ausruestung");
			final String type = item.getItemType();
			books = ResourceManager.getDiscrimination(equipment.getObj(type.isEmpty() ? item.getName() : type));
		} else {
			books = actual.getObjOrDefault("Regelwerke", baseItem.getObj("Regelwerke"));
		}

		if (books != null) {
			for (final String book : books.keySet()) {
				booksTable.getItems().add(new Tuple<>(book, books.getInt(book)));
				chosen.add(book);
			}
		}
	}

	private void initList() {
		final String selected = bookList.getSelectionModel().getSelectedItem();
		bookList.getItems().clear();
		for (final String book : Settings.getSettingArray("Allgemein", "Bücher").getStrings()) {
			if (!chosen.contains(book)) {
				bookList.getItems().add(book);
			}
		}
		if (!bookList.getItems().isEmpty()) {
			if (selected != null) {
				bookList.getSelectionModel().select(selected);
			} else {
				bookList.getSelectionModel().select(0);
			}
		}
	}

	@FXML
	private void reset() {
		isDefault = true;
	}
}
