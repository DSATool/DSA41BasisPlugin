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

import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class Spell extends Talent {
	private JSONObject actualRepresentation;
	private final StringProperty complexity;
	private final StringProperty representation;
	private final BooleanProperty primarySpell;

	public Spell(String name, JSONObject spell, JSONObject actual, JSONObject actualGroup, String representation) {
		super(name, null, spell, actual, actualGroup);

		this.representation = new SimpleStringProperty(representation);
		complexity = new SimpleStringProperty(DSAUtil.getEnhancementGroupString(spell.getIntOrDefault("Komplexität", 1)));

		actualRepresentation = actual == null ? null : actual.getObjOrDefault(representation, null);

		primarySpell = new SimpleBooleanProperty(actualRepresentation == null ? false : actualRepresentation.getBoolOrDefault("Hauszauber", false));
		primaryTalent = new SimpleBooleanProperty(actualRepresentation == null ? false : actualRepresentation.getBoolOrDefault("Leittalent", false));

		ses = new SimpleIntegerProperty(actualRepresentation == null ? 0 : actualRepresentation.getIntOrDefault("SEs", 0));
		value = new SimpleIntegerProperty(actualRepresentation == null || !actualRepresentation.getBoolOrDefault("aktiviert", true) ? Integer.MIN_VALUE
				: actualRepresentation.getIntOrDefault("ZfW", 0));
	}

	public final ReadOnlyStringProperty complexityProperty() {
		return complexity;
	}

	public final String getComplexity() {
		return complexity.get();
	}

	@Override
	public int getEnhancementCost(JSONObject hero, int targetZfW) {
		return HeroUtil.getSpellComplexity(hero, name.get(), representation.get(), targetZfW);
	}

	public final String getRepresentation() {
		return representation.get();
	}

	private void insertRepresentation(boolean activated) {
		actualRepresentation = new JSONObject(actual);
		if (!activated) {
			actualRepresentation.put("aktiviert", false);
		}
		actual.put(representation.get(), actualRepresentation);
	}

	@Override
	protected void insertTalent(boolean activated) {
		actual = new JSONObject(actualGroup);
		actualGroup.put(getName(), actual);
	}

	public final boolean isPrimarySpell() {
		return primarySpell.get();
	}

	public final BooleanProperty primarySpellProperty() {
		return primarySpell;
	}

	public final ReadOnlyStringProperty representationProperty() {
		return representation;
	}

	public final void setPrimarySpell(boolean primary) {
		if (actual == null) {
			insertTalent(false);
		}
		if (actualRepresentation == null) {
			insertRepresentation(false);
		}
		if (primary) {
			actualRepresentation.put("Hauszauber", true);
		} else {
			actualRepresentation.removeKey("Hauszauber");
		}
		primarySpell.set(primary);
		actual.notifyListeners(null);
	}

	@Override
	public void setPrimaryTalent(boolean primary) {
		if (actual == null) {
			insertTalent(false);
		}
		if (actualRepresentation == null) {
			insertRepresentation(false);
		}
		if (primary) {
			actualRepresentation.put("Leittalent", true);
		} else {
			actualRepresentation.removeKey("Leittalent");
		}
		primaryTalent.set(primary);
		actual.notifyListeners(null);
	}

	@Override
	public void setSes(int ses) {
		if (actual == null) {
			insertTalent(false);
		}
		if (actualRepresentation == null) {
			insertRepresentation(false);
		}
		if (ses == 0) {
			actualRepresentation.removeKey("SEs");
		} else {
			actualRepresentation.put("SEs", ses);
		}
		this.ses.set(ses);
		actual.notifyListeners(null);
	}

	@Override
	public void setValue(int value) {
		if (actual == null) {
			insertTalent(true);
		}
		if (actualRepresentation == null) {
			insertRepresentation(true);
		}
		if (value == Integer.MIN_VALUE) {
			actualRepresentation.removeKey("ZfW");
			actualRepresentation.put("aktiviert", false);
		} else {
			actualRepresentation.put("ZfW", value);
			actualRepresentation.removeKey("aktiviert");
		}
		this.value.set(value);
		actual.notifyListeners(null);
	}
}
