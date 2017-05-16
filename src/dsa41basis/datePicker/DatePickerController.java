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
package dsa41basis.datePicker;

import java.time.LocalDate;

import dsa41basis.util.DSAUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.util.ErrorLogger;
import dsatool.util.ReactiveSpinner;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class DatePickerController implements JSONListener {
	@FXML
	private HBox box;
	@FXML
	private ReactiveSpinner<Integer> day;
	@FXML
	private ComboBox<String> month;
	private JSONObject time = null;
	private final Tooltip tooltip = new Tooltip();

	@FXML
	private ReactiveSpinner<Integer> year;

	public DatePickerController() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("DatePicker.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		tooltip.setContentDisplay(ContentDisplay.RIGHT);

		Tooltip.install(box, tooltip);
		Tooltip.install(day, tooltip);
		Tooltip.install(month, tooltip);
		Tooltip.install(year, tooltip);
		load();
	}

	public Node getControl() {
		return box;
	}

	public void load() {
		if (time != null) {
			time.removeListener(this);
		}

		final JSONObject general = ResourceManager.getResource("data/Allgemein");
		if (!general.containsKey("Zeit")) {
			time = new JSONObject(general);
			general.put("Zeit", time);
			setToCurrentDate();
		}
		notifyChanged(null);
		day.setEditable(true);

		day.valueProperty().addListener((ChangeListener<Integer>) (observable, oldValue, newValue) -> {
			time.put("Tag", newValue);
			updateTooltip();
			time.notifyListeners(this);
		});
		month.getSelectionModel().selectedIndexProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
			time.put("Monat", newValue.intValue() + 1);
			updateTooltip();
			time.notifyListeners(this);
		});
		year.valueProperty().addListener((ChangeListener<Integer>) (observable, oldValue, newValue) -> {
			time.put("Jahr", newValue);
			updateTooltip();
			time.notifyListeners(this);
		});

		time.addListener(this);
	}

	@Override
	public void notifyChanged(final JSONValue changed) {
		time = ResourceManager.getResource("data/Allgemein").getObj("Zeit");

		day.getValueFactory().setValue(time.getIntOrDefault("Tag", 1));
		month.setItems(FXCollections.observableArrayList(DSAUtil.months));
		month.getSelectionModel().select(time.getIntOrDefault("Monat", 1) - 1);
		year.getValueFactory().setValue(time.getIntOrDefault("Jahr", 1000));

		updateTooltip();
	}

	@FXML
	private void setToCurrentDate() {
		final LocalDate currentDate = LocalDate.now();
		final String dateSetting = Settings.getSettingStringOrDefault("Jahreswechsel", "Allgemein", "Jahreswechsel");

		int currentYear = currentDate.getYear() - 977;
		int days = currentDate.getDayOfYear();

		if (currentDate.isLeapYear() && days >= 60) {
			days -= 1;
		}

		switch (dateSetting) {
		case "Jahreszeiten":
			days -= 181;
			break;
		case "Astronomisch":
			days -= 171;
			break;
		default:
			break;
		}

		if (days <= 0) {
			days += 365;
			currentYear -= 1;
		}

		time.put("Tag", days % 30);
		time.put("Monat", days / 30 + 1);
		time.put("Jahr", currentYear);
		notifyChanged(null);
	}

	private void updateTooltip() {
		final int monthDayBase = DSAUtil.getMonthDayBase(day.getValue(), month.getSelectionModel().getSelectedIndex(), year.getValue());

		tooltip.setText(DSAUtil.weekdays[(monthDayBase % 7 + 7) % 7]);

		String lunarPhase = "";
		Color color = Color.WHITE;
		double rotation = 0;
		if (monthDayBase < 8) {
			lunarPhase = "\uE3A8"; // waxing moon
		} else if (monthDayBase < 15) {
			lunarPhase = "\uE3A6"; // full moon
		} else if (monthDayBase < 22) {
			lunarPhase = "\uE3A8"; // waning moon
			rotation = 180;
		} else {
			lunarPhase = "\uE3A6"; // new moon
			color = Color.DIMGREY;
		}
		final Label lunarPhaseLabel = new Label(lunarPhase);
		lunarPhaseLabel.setRotate(rotation);
		lunarPhaseLabel.setTextFill(color);
		lunarPhaseLabel.getStyleClass().add("icon-font");
		tooltip.setGraphic(lunarPhaseLabel);
	}
}
