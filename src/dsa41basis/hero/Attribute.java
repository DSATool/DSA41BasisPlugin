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
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class Attribute implements Enhanceable {

	protected final JSONObject actual;
	private final IntegerProperty current;
	private final IntegerProperty modifier;
	private final IntegerProperty manualModifier;
	private final StringProperty name;
	private final IntegerProperty value;
	protected IntegerProperty ses;

	private final JSONListener refreshListener = o -> refreshValue();

	public Attribute(final String name, final JSONObject actual) {
		this.actual = actual;
		this.name = new SimpleStringProperty(name);

		value = new SimpleIntegerProperty(actual.getIntOrDefault("Wert", 0));

		modifier = new SimpleIntegerProperty(actual.getIntOrDefault("Modifikator", 0));

		manualModifier = new SimpleIntegerProperty(actual.getIntOrDefault("Modifikator:Manuell", 0));

		ses = new SimpleIntegerProperty(actual.getIntOrDefault("SEs", 0));

		actual.addLocalListener(refreshListener);

		current = new SimpleIntegerProperty();
		refreshValue();
	}

	public final ReadOnlyIntegerProperty currentProperty() {
		return current;
	}

	@Override
	public JSONObject getActual() {
		return actual;
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

	@Override
	public final IntegerProperty sesProperty() {
		return ses;
	}

	public final void setManualModifier(final int modifier) {
		if (manualModifier.get() != modifier) {
			if (modifier == 0) {
				actual.removeKey("Modifikator:Manuell");
			} else {
				actual.put("Modifikator:Manuell", modifier);
			}
			manualModifier.set(modifier);
			actual.notifyListeners(null);
		}
	}

	public final void setModifier(final int modifier) {
		if (this.modifier.get() != modifier) {
			if (modifier == 0) {
				actual.removeKey("Modifikator");
			} else {
				actual.put("Modifikator", modifier);
			}
			this.modifier.set(modifier);
			actual.notifyListeners(null);
		}
	}

	public final void setStart(final int start) {
		if (actual.getIntOrDefault("Start", 0) != start) {
			if (start == 0) {
				actual.removeKey("Start");
			} else {
				actual.put("Start", start);
			}
			actual.notifyListeners(null);
		}
	}

	public final void setValue(final int value) {
		if (this.value.get() != value) {
			actual.put("Wert", value);
			this.value.set(value);
			actual.notifyListeners(null);
		}
	}

	public final IntegerProperty valueProperty() {
		return value;
	}
}
