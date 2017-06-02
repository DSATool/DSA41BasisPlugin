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
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class Spell extends Talent {
	private JSONObject actualSpell;
	private final StringProperty complexity;
	private final StringProperty representation;
	private final BooleanProperty primarySpell;

	public Spell(final String name, final JSONObject spell, final JSONObject actualRepresentation, final JSONObject actualSpell, final JSONObject actualGroup,
			final String representation) {
		super(name, null, spell, actualRepresentation, actualGroup);

		this.actualSpell = actualSpell;

		this.representation = new SimpleStringProperty(representation);
		complexity = new SimpleStringProperty(DSAUtil.getEnhancementGroupString(spell.getIntOrDefault("Komplexität", 1)));

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
	public int getEnhancementCost(final JSONObject hero, final int targetZfW) {
		return HeroUtil.getSpellComplexity(hero, name.get(), representation.get(), targetZfW);
	}

	public final String getRepresentation() {
		return representation.get();
	}

	private void insertSpell() {
		if (actualSpell == null) {
			actualSpell = new JSONObject(actualGroup);
		}
		actualGroup.put(name.get(), actualSpell);
	}

	@Override
	public void insertTalent(final boolean activated) {
		if (actualSpell == null) {
			insertSpell();
		}
		if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
			final JSONArray choiceTalent = actualSpell.getArr(representation.get());
			if (actual == null) {
				actual = new JSONObject(choiceTalent);
			}
			if (!choiceTalent.contains(actual)) {
				choiceTalent.add(actual);
			}
		} else {
			if (actual == null) {
				actual = new JSONObject(actual);
			}
			actualSpell.put(representation.get(), actual);
		}
		if (!activated) {
			actual.put("aktiviert", false);
		}
		actual.notifyListeners(null);
	}

	public final boolean isPrimarySpell() {
		return primarySpell.get();
	}

	public final BooleanProperty primarySpellProperty() {
		return primarySpell;
	}

	@Override
	public void removeTalent() {
		if (actualSpell != null) {
			if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
				actualSpell.getArr(representation.get()).remove(actual);
				if (actualSpell.getArr(representation.get()).size() == 0) {
					actualSpell.removeKey(representation.get());
				}
			} else {
				actualSpell.remove(actual);
			}
			if (actualSpell.size() == 0) {
				actualGroup.remove(actualSpell);
			}
			actualSpell.notifyListeners(null);
		}
	}

	public final ReadOnlyStringProperty representationProperty() {
		return representation;
	}

	public final void setPrimarySpell(final boolean primary) {
		if (actualSpell == null) {
			insertSpell();
		}
		if (actual == null) {
			insertTalent(false);
		}
		if (primary) {
			actual.put("Hauszauber", true);
		} else {
			actual.removeKey("Hauszauber");
		}
		primarySpell.set(primary);
		actualSpell.notifyListeners(null);
	}

	@Override
	public void setPrimaryTalent(final boolean primary) {
		if (actualSpell == null) {
			insertSpell();
		}
		if (actual == null) {
			insertTalent(false);
		}
		if (primary) {
			actual.put("Leittalent", true);
		} else {
			actual.removeKey("Leittalent");
		}
		primaryTalent.set(primary);
		actualSpell.notifyListeners(null);
	}

	@Override
	public void setSes(final int ses) {
		if (actualSpell == null) {
			insertSpell();
		}
		if (actual == null) {
			insertTalent(false);
		}
		if (ses == 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", ses);
		}
		this.ses.set(ses);
		actualSpell.notifyListeners(null);
	}

	@Override
	public void setValue(final int value) {
		if (actualSpell == null) {
			insertSpell();
		}
		if (actual == null) {
			insertTalent(false);
		}
		if (value == Integer.MIN_VALUE) {
			actual.removeKey("ZfW");
			actual.put("aktiviert", false);
		} else {
			actual.put("ZfW", value);
			actual.removeKey("aktiviert");
		}
		this.value.set(value);
		actualSpell.notifyListeners(null);
	}

	@Override
	public void setVariant(final String variant) {
		if (actualSpell == null) {
			insertSpell();
		}
		if (actual == null) {
			insertTalent(false);
		}
		if (talent.containsKey("Auswahl")) {
			actual.put("Auswahl", variant);
		} else if (talent.containsKey("Freitext")) {
			actual.put("Freitext", variant);
		}
		this.variant.set(variant);
		displayName.set(name.get() + ": " + variant);
		actualSpell.notifyListeners(null);
	}
}
