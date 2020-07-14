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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dsa41basis.serialization.FileLoader;
import dsa41basis.serialization.Loaders;
import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class HeroSelector {
	@FXML
	protected ListView<String> list;
	@FXML
	private BorderPane pane;
	@FXML
	private HBox buttons;

	protected List<JSONObject> heroes;

	protected final List<HeroController> controllers = new ArrayList<>();

	public HeroSelector(final boolean allowCreate) {
		this(allowCreate, false);
	}

	public HeroSelector(final boolean allowCreate, final boolean fixFirst) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			pane = fxmlLoader.load(HeroSelector.class.getResource("HeroSelector.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		if (!allowCreate) {
			buttons.getChildren().remove(0);
		}

		list.setCellFactory(list -> {
			final ListCell<String> cell = new TextFieldListCell<>();
			final ContextMenu cellMenu = new ContextMenu();

			final MenuItem create = new MenuItem("Neuer Held");
			create.setOnAction(event -> addNewHero());

			final MenuItem remove = new MenuItem("Löschen");
			remove.setOnAction(event -> removeHero(cell.getIndex()));

			final MenuItem load = new MenuItem("Importieren");
			load.setOnAction(event -> loadHero());

			final MenuItem save = new MenuItem("Exportieren");
			save.setOnAction(event -> saveHero(cell.getIndex()));

			if (allowCreate) {
				cellMenu.getItems().add(create);
			}
			cellMenu.getItems().add(remove);
			cellMenu.getItems().add(load);
			cellMenu.getItems().add(save);

			cell.setContextMenu(cellMenu);

			BooleanBinding isChar = cell.emptyProperty().not();
			if (fixFirst) {
				isChar = isChar.and(cell.indexProperty().isNotEqualTo(0));
			}

			remove.visibleProperty().bind(isChar);
			save.visibleProperty().bind(isChar);

			return cell;
		});
	}

	private void addHero(final JSONObject hero) {
		final JSONObject bio = hero.getObj("Biografie");
		list.getItems().add(bio.getString("Vorname"));
		bio.addLocalListener(o -> {
			list.getItems().set(heroes.indexOf(hero), bio.getString("Vorname"));
		});
	}

	@FXML
	private void addNewHero() {
		final JSONObject hero = ResourceManager.getNewResource("characters/Neuer_Held");
		list.getSelectionModel().clearAndSelect(list.getItems().size() - 1);
		final JSONObject bio = hero.getObj("Biografie");
		bio.put("Vorname", "Neuer Held");
		bio.put("Nachname", "");
		bio.put("Rasse", "");
		bio.put("Kultur", "");
		bio.put("Profession", "");
		bio.put("Geburtstag", 0);
		bio.put("Geburtsmonat", 0);
		bio.put("Geburtsjahr", 0);
		bio.put("Geschlecht", "männlich");
		bio.put("Größe", 175);
		bio.put("Gewicht", 75);
		bio.put("Augenfarbe", "braun");
		bio.put("Haarfarbe", "braun");
		bio.put("Hautfarbe", "weiß");
		bio.put("Gottheiten", "Zwölfgötter");
		bio.put("Abenteuerpunkte", 0);
		bio.put("Abenteuerpunkte-Guthaben", 0);
		hero.put("Biografie", bio);
		bio.notifyListeners(null);
		final JSONObject actualAttributes = new JSONObject(hero);
		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
		for (final String attribute : attributes.keySet()) {
			actualAttributes.put(attribute, new JSONObject(actualAttributes));
		}
		hero.put("Eigenschaften", actualAttributes);
		final JSONObject derivedValues = new JSONObject(hero);
		for (final String derivedValue : new String[] { "Attacke-Basis", "Parade-Basis", "Fernkampf-Basis", "Initiative-Basis", "Wundschwelle",
				"Sozialstatus" }) {
			derivedValues.put(derivedValue, new JSONObject(derivedValues));
		}
		hero.put("Basiswerte", derivedValues);
		hero.put("Vorteile", new JSONObject(hero));
		hero.put("Nachteile", new JSONObject(hero));
		hero.put("Sonderfertigkeiten", new JSONObject(hero));
		hero.put("Verbilligte Sonderfertigkeiten", new JSONObject(hero));
		final JSONObject actualTalents = new JSONObject(hero);
		final JSONObject talents = ResourceManager.getResource("data/Talente");
		for (final String talentGroupName : talents.keySet()) {
			final JSONObject talentGroup = talents.getObj(talentGroupName);
			final JSONObject actualTalentGroup = new JSONObject(actualTalents);
			for (final String talent : talentGroup.keySet()) {
				if (talentGroup.getObj(talent).getBoolOrDefault("Basis", false)) {
					actualTalentGroup.put(talent, new JSONObject(actualTalentGroup));
				}
			}
			actualTalents.put(talentGroupName, actualTalentGroup);
		}
		hero.put("Talente", actualTalents);
		final JSONObject inventory = new JSONObject(hero);
		inventory.put("Geld", new JSONObject(inventory));
		inventory.put("Ausrüstung", new JSONArray(inventory));
		hero.put("Besitz", inventory);
	}

	public Node getRoot() {
		return pane;
	}

	public void load() {
		final MultipleSelectionModel<String> listModel = list.getSelectionModel();
		listModel.selectedIndexProperty().addListener((o, oldV, newV) -> {
			if (oldV.intValue() != newV.intValue() && newV.intValue() > -1) {
				setHero(newV.intValue());
			}
		});

		reload();

		ResourceManager.addPathListener("characters/", (discard) -> {
			if (!discard) {
				reload();
			}
		});
	}

	@FXML
	private void loadHero() {
		final FileChooser dialog = new FileChooser();

		final Map<ExtensionFilter, FileLoader> filters = new HashMap<>();

		dialog.setTitle("Datei öffnen");
		for (final FileLoader heroLoader : Loaders.heroLoaders) {
			final ExtensionFilter filter = new ExtensionFilter(heroLoader.getName(), heroLoader.getExtensions());
			filters.put(filter, heroLoader);
			dialog.getExtensionFilters().addAll(filter);
		}

		final File file = dialog.showOpenDialog(null);
		if (file != null) {
			final List<JSONObject> newHeroes = filters.get(dialog.getSelectedExtensionFilter()).loadFile(file);
			if (!newHeroes.isEmpty()) {
				list.getSelectionModel().select(newHeroes.get(0).getObj("Biografie").getString("Vorname"));
			}
		}
	}

	/**
	 * Reloads the data if it has changed
	 */
	protected void reload() {
		final MultipleSelectionModel<String> listModel = list.getSelectionModel();
		final int selected = listModel.getSelectedIndex();
		final JSONObject selectedHero = selected < 0 ? null : heroes.get(selected);
		heroes = ResourceManager.getAllResources("characters/");
		list.getItems().clear();
		for (final JSONObject hero : heroes) {
			addHero(hero);
		}
		if (heroes.size() == 0) {
			addNewHero();
		}
		final int index = heroes.indexOf(selectedHero);
		if (listModel.getSelectedIndex() != index || listModel.getSelectedIndex() == -1) {
			listModel.clearAndSelect(Math.max(0, index));
		}
	}

	@FXML
	private void removeHero() {
		final int index = list.getSelectionModel().getSelectedIndex();
		removeHero(index);
		list.getSelectionModel().clearAndSelect(Math.min(list.getItems().size() - 1, index));
	}

	private void removeHero(final int index) {
		if (index > -1) {
			final JSONObject hero = heroes.get(index);

			final Alert deleteConfirmation = new Alert(AlertType.CONFIRMATION);
			deleteConfirmation.setTitle("Held löschen?");
			deleteConfirmation.setHeaderText("Held " + hero.getObj("Biografie").getString("Vorname") + " löschen?");
			deleteConfirmation.setContentText("Der Held kann danach nicht wiederhergestellt werden!");
			deleteConfirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

			final Optional<ButtonType> result = deleteConfirmation.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				ResourceManager.deleteResource(hero);
			}
		}
	}

	@FXML
	private void saveHero() {
		final int index = list.getSelectionModel().getSelectedIndex();
		if (index > -1) {
			saveHero(index);
		}
	}

	private void saveHero(final int index) {
		final JSONObject hero = heroes.get(index);

		final FileChooser dialog = new FileChooser();

		dialog.setTitle("Datei speichern");
		dialog.setInitialFileName(hero.getObj("Biografie").getString("Vorname") + ".json");
		dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.json", "*.json"));

		final File file = dialog.showSaveDialog(null);
		if (file != null) {
			ResourceManager.saveResource(hero, file.getAbsolutePath());
		}
	}

	public void setContent(final Node content) {
		pane.setCenter(content);
	}

	protected void setHero(final int index) {
		for (final HeroController controller : controllers) {
			try {
				controller.setHero(heroes.get(index));
			} catch (final Exception e) {
				ErrorLogger.logError(e);
			}
		}
	}
}
