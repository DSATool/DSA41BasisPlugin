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

import dsatool.resources.ResourceManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class Ritual {

	private final StringProperty name;
	private final ObjectProperty<Object> choice;

	private final JSONObject actual;

	public Ritual(final String ritualGroup, final String name, final JSONObject actual) {
		this.actual = actual;

		this.name = new SimpleStringProperty(name);

		final JSONObject ritual = ResourceManager.getResource("data/Rituale").getObj(ritualGroup).getObjOrDefault(name, null);

		if (ritual != null && ritual.containsKey("Mehrfach")) {
			if ("Anzahl".equals(ritual.getString("Mehrfach"))) {
				choice = new SimpleObjectProperty<>(actual.getIntOrDefault("Anzahl", 1));
			} else {
				choice = new SimpleObjectProperty<>(actual.getStringOrDefault("Auswahl", ritual.getString("Mehrfach")));
			}
		} else {
			choice = new SimpleObjectProperty<>(null);
		}
	}

	public ReadOnlyObjectProperty<Object> choiceProperty() {
		return choice;
	}

	public Object getChoice() {
		return choice.get();
	}

	public String getName() {
		return name.get();
	}

	public ReadOnlyStringProperty nameProperty() {
		return name;
	}

	public void setChoice(final Object choice) {
		this.choice.set(choice);
		if (choice instanceof Integer) {
			actual.put("Anzahl", (Integer) choice);
		} else if (choice instanceof String) {
			actual.put("Auswahl", (String) choice);
		}
		actual.notifyListeners(null);
	}
}
