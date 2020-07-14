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

public class DerivedValue {

	protected JSONObject actual;
	protected final IntegerProperty current = new SimpleIntegerProperty();
	protected final IntegerProperty manualModifier = new SimpleIntegerProperty();
	private final StringProperty name;
	protected IntegerProperty ses;

	public DerivedValue(final String name, final JSONObject derivation, final JSONObject hero) {
		this.name = new SimpleStringProperty(name);

		final JSONObject attributes = hero.getObj("Eigenschaften");
		final JSONObject basicValues = hero.getObj("Basiswerte");

		actual = basicValues.getObj(name);

		if (actual == null) {
			actual = new JSONObject(basicValues);
			basicValues.put(name, actual);
		}

		attributes.addListener(o -> recalculate(derivation, hero));
		actual.addListener(o -> recalculate(derivation, hero));

		recalculateBase(derivation, hero);

		ses = new SimpleIntegerProperty(actual == null ? 0 : actual.getIntOrDefault("SEs", 0));
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

	public final int getModifier() {
		return actual.getIntOrDefault("Modifikator", 0);
	}

	public final String getName() {
		return name.get();
	}

	public final int getSes() {
		return ses.get();
	}

	public final IntegerProperty manualModifierProperty() {
		return manualModifier;
	}

	public final ReadOnlyStringProperty nameProperty() {
		return name;
	}

	protected void recalculate(final JSONObject derivation, final JSONObject hero) {
		recalculateBase(derivation, hero);
	}

	private void recalculateBase(final JSONObject derivation, final JSONObject hero) {
		final int value = HeroUtil.deriveValue(derivation, hero, actual, false);
		manualModifier.set(actual.getIntOrDefault("Modifikator:Manuell", 0));
		current.bind(manualModifier.add(value));
	}

	public final IntegerProperty sesProperty() {
		return ses;
	}

	public final void setManualModifier(final int modifier) {
		if (modifier == 0) {
			actual.removeKey("Modifikator:Manuell");
		} else {
			actual.put("Modifikator:Manuell", modifier);
		}
		manualModifier.set(modifier);
		actual.notifyListeners(null);
	}

	public final void setModifier(final int modifier) {
		if (modifier == 0) {
			actual.removeKey("Modifikator");
		} else {
			actual.put("Modifikator", modifier);
		}
		actual.notifyListeners(null);
	}

	public void setSes(final int ses) {
		if (actual == null) return;
		if (ses == 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", ses);
		}
		actual.notifyListeners(null);
		this.ses.set(ses);
	}
}
