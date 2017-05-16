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
package dsa41basis.hero;

import dsa41basis.util.HeroUtil;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class Attribute {

	protected final JSONObject actual;
	private final IntegerProperty current;
	private final IntegerProperty modifier;
	private final IntegerProperty manualModifier;
	private final StringProperty name;
	private final IntegerProperty value;
	protected IntegerProperty ses;

	public Attribute(String name, JSONObject actual) {
		this.actual = actual;
		this.name = new SimpleStringProperty(name);

		value = new SimpleIntegerProperty(actual.getIntOrDefault("Wert", 0));

		modifier = new SimpleIntegerProperty(actual.getIntOrDefault("Modifikator", 0));

		manualModifier = new SimpleIntegerProperty(actual.getIntOrDefault("Modifikator:Manuell", 0));

		ses = new SimpleIntegerProperty(actual == null ? 0 : actual.getIntOrDefault("SEs", 0));

		actual.addLocalListener(o -> refreshValue());

		current = new SimpleIntegerProperty();
		refreshValue();
	}

	public final ReadOnlyIntegerProperty currentProperty() {
		return current;
	}

	public final int getCurrent() {
		return current.get();
	}

	public final int getManualModifier() {
		return manualModifier.get();
	}

	public int getMaximum() {
		return (int) Math.round(getStart() * 1.5);
	}

	public final int getModifier() {
		return modifier.get();
	}

	public final String getName() {
		return name.get();
	}

	public final int getSes() {
		return ses.get();
	}

	public final int getStart() {
		return actual.getIntOrDefault("Start", value.get());
	}

	public final int getValue() {
		return value.get();
	}

	public final IntegerProperty manualModifierProperty() {
		return manualModifier;
	}

	public final IntegerProperty modifierProperty() {
		return modifier;
	}

	public final ReadOnlyStringProperty nameProperty() {
		return name;
	}

	private void refreshValue() {
		current.set(HeroUtil.getCurrentValue(actual, true));
	}

	public final IntegerProperty sesProperty() {
		return ses;
	}

	public final void setManualModifier(int modifier) {
		if (modifier == 0) {
			actual.removeKey("Modifikator:Manuell");
		} else {
			actual.put("Modifikator:Manuell", modifier);
		}
		actual.notifyListeners(null);
		manualModifier.set(modifier);
	}

	public final void setModifier(int modifier) {
		if (modifier == 0) {
			actual.removeKey("Modifikator");
		} else {
			actual.put("Modifikator", modifier);
		}
		actual.notifyListeners(null);
		this.modifier.set(modifier);
	}

	public void setSes(int ses) {
		if (actual == null) return;
		if (ses == 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", ses);
		}
		actual.notifyListeners(null);
		this.ses.set(ses);
	}

	public final void setStart(int start) {
		if (start == 0) {
			actual.removeKey("Start");
		} else {
			actual.put("Start", start);
		}
		actual.notifyListeners(null);
	}

	public final void setValue(int value) {
		actual.put("Wert", value);
		actual.notifyListeners(null);
		this.value.set(value);
	}

	public final IntegerProperty valueProperty() {
		return value;
	}
}
