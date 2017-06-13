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

import java.util.Arrays;
import java.util.Set;

import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class Talent {

	protected final StringProperty name;
	protected final StringProperty displayName;
	protected JSONObject actual;
	protected JSONObject actualGroup;
	protected IntegerProperty ses;
	protected IntegerProperty value;
	protected final StringProperty attributes;
	protected BooleanProperty primaryTalent;
	protected final JSONArray challenge;
	protected JSONObject talent;
	protected final StringProperty variant;

	public Talent(final String name, final JSONObject talentGroup, final JSONObject talent, final JSONObject actual, final JSONObject actualGroup) {
		this.name = new SimpleStringProperty(name);
		this.talent = talent;
		this.actual = actual;
		this.actualGroup = actualGroup;
		final String primaryAttribute = talent.getStringOrDefault("Leiteigenschaft", "IN");
		challenge = talent.getArrOrDefault("Probe", talentGroup == null ? null
				: talentGroup.getArrOrDefault("Probe", new JSONArray(Arrays.asList(primaryAttribute, primaryAttribute, primaryAttribute), null)));
		attributes = new SimpleStringProperty(DSAUtil.getChallengeString(challenge));
		primaryTalent = new SimpleBooleanProperty(actual == null ? false : actual.getBoolOrDefault("Leittalent", false));
		ses = new SimpleIntegerProperty(actual == null ? 0 : actual.getIntOrDefault("SEs", 0));
		value = new SimpleIntegerProperty(actual == null || !actual.getBoolOrDefault("aktiviert", true)
				? talent.getBoolOrDefault("Basis", false) ? 0 : Integer.MIN_VALUE : actual.getIntOrDefault("TaW", 0));

		if (talent.containsKey("Auswahl")) {
			final Set<String> choices = HeroUtil.getChoices(null, talent.getString("Auswahl"), null);
			variant = new SimpleStringProperty(actual != null ? actual.getStringOrDefault("Auswahl", choices.isEmpty() ? "" : choices.iterator().next()) : "");
		} else if (talent.containsKey("Freitext")) {
			final Set<String> choices = HeroUtil.getChoices(null, talent.getString("Freitext"), null);
			variant = new SimpleStringProperty(actual != null ? actual.getStringOrDefault("Freitext", choices.isEmpty() ? "" : choices.iterator().next()) : "");
		} else {
			variant = new SimpleStringProperty("");
		}

		if ("".equals(variant.get())) {
			displayName = new SimpleStringProperty(name);
		} else {
			displayName = new SimpleStringProperty(name + ": " + variant.get());
		}
	}

	public final ReadOnlyStringProperty attributesProperty() {
		return attributes;
	}

	public JSONObject getActual() {
		return actual;
	}

	public final String getAttributes() {
		return attributes.get();
	}

	public final String getDisplayName() {
		return displayName.get();
	}

	public int getEnhancementCost(final JSONObject hero, final int targetTaW) {
		return HeroUtil.getTalentComplexity(hero, name.get());
	}

	public int getMaximum(final JSONObject hero) {
		final JSONObject attributes = hero.getObj("Eigenschaften");
		int max = 0;
		for (int i = 0; i < 3; ++i) {
			final int attribute = HeroUtil.getCurrentValue(attributes.getObj(challenge.getString(i)), false);
			if (attribute + 3 > max) {
				max = attribute + 3;
			}
		}
		return max;
	}

	public final String getName() {
		return name.get();
	}

	public final int getSes() {
		return ses.get();
	}

	public JSONObject getTalent() {
		return talent;
	}

	public int getValue() {
		return value.get();
	}

	public final String getVariant() {
		return variant.get();
	}

	public void insertTalent(final boolean activated) {
		if (actualGroup != null && (talent.containsKey("Auswahl") || talent.containsKey("Freitext"))) {
			final JSONArray choiceTalent = actualGroup.getArr(name.get());
			if (actual == null) {
				actual = new JSONObject(choiceTalent);
			}
			if (!choiceTalent.contains(actual)) {
				choiceTalent.add(actual);
			}
		} else {
			if (actual == null) {
				actual = new JSONObject(actualGroup);
			}
			if (actualGroup != null) {
				actualGroup.put(name.get(), actual);
			}
		}
		if (!activated) {
			actual.put("aktiviert", false);
		}
		actual.notifyListeners(null);
	}

	public final boolean isPrimaryTalent() {
		return primaryTalent.get();
	}

	public final ReadOnlyStringProperty nameProperty() {
		return name;
	}

	public final BooleanProperty primaryTalentProperty() {
		return primaryTalent;
	}

	public void removeTalent() {
		if (actualGroup != null) {
			if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
				actualGroup.getArr(name.get()).remove(actual);
			} else {
				actualGroup.removeKey(name.get());
			}
			actualGroup.notifyListeners(null);
		}
	}

	public final IntegerProperty sesProperty() {
		return ses;
	}

	public void setPrimaryTalent(final boolean primary) {
		if (actual == null) {
			insertTalent(false);
		}
		if (primary) {
			actual.put("Leittalent", true);
		} else {
			actual.removeKey("Leittalent");
		}
		primaryTalent.set(primary);
		actual.notifyListeners(null);
	}

	public void setSes(final int ses) {
		if (actual == null) {
			insertTalent(false);
		}
		if (ses == 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", ses);
		}
		this.ses.set(ses);
		actual.notifyListeners(null);
	}

	public void setValue(final int value) {
		if (actual == null) {
			insertTalent(true);
		}
		if (value == Integer.MIN_VALUE) {
			actual.removeKey("TaW");
			actual.put("aktiviert", false);
		} else {
			actual.put("TaW", value);
			actual.removeKey("aktiviert");
		}
		this.value.set(value);
		actual.notifyListeners(null);
	}

	public void setVariant(final String variant) {
		if (actual == null) {
			insertTalent(true);
		}
		if (talent.containsKey("Auswahl")) {
			actual.put("Auswahl", variant);
		} else if (talent.containsKey("Freitext")) {
			actual.put("Freitext", variant);
		}
		this.variant.set(variant);
		displayName.set(name.get() + ": " + variant);
		actual.notifyListeners(null);
	}

	public IntegerProperty valueProperty() {
		return value;
	}
}
