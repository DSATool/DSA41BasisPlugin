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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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

public class Talent implements Enhanceable {
	private static Map<JSONObject, Talent> talentCache = new IdentityHashMap<>();

	public static Talent getTalent(final String name, final JSONObject talentGroup, final JSONObject talent, final JSONObject actual,
			final JSONObject actualGroup) {
		if (talentCache.containsKey(actual))
			return talentCache.get(actual);
		final String key = ((JSONObject) talentGroup.getParent()).keyOf(talentGroup);
		final Talent newTalent = switch (key) {
			case "Nahkampftalente", "Fernkampftalente" -> new FightTalent(name, talentGroup, talent, actual, actualGroup);
			case "Körperliche Talente" -> new PhysicalTalent(name, talentGroup, talent, actual, actualGroup);
			case "Sprachen", "Schriften" -> new LanguageTalent(name, talentGroup, talent, actual, actualGroup);
			default -> new Talent(name, talentGroup, talent, actual, actualGroup);
		};
		if (actual != null) {
			talentCache.put(actual, newTalent);
		}
		return newTalent;
	}

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

	protected Talent(final String name, final JSONObject talentGroup, final JSONObject talent, final JSONObject actual, final JSONObject actualGroup) {
		this.name = new SimpleStringProperty(name);
		this.talent = talent;
		this.actual = actual;
		this.actualGroup = actualGroup;
		final String primaryAttribute = talent.getStringOrDefault("Leiteigenschaft", "IN");
		challenge = talent.getArrOrDefault("Probe", talentGroup == null ? null
				: talentGroup.getArrOrDefault("Probe", new JSONArray(List.of(primaryAttribute, primaryAttribute, primaryAttribute), null)));
		attributes = new SimpleStringProperty(DSAUtil.getChallengeString(challenge));
		final boolean autoPrimaryTalent = talent.getBoolOrDefault("Leittalent", false);
		primaryTalent = new SimpleBooleanProperty(actual == null ? autoPrimaryTalent : actual.getBoolOrDefault("Leittalent", autoPrimaryTalent));
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

	public boolean exists() {
		return actual != null;
	}

	@Override
	public JSONObject getActual() {
		if (actual == null) {
			insertTalent(true);
		}
		return actual;
	}

	public final String getAttributes() {
		return attributes.get();
	}

	public final String getDisplayName() {
		return displayName.get();
	}

	public int getEnhancementComplexity(final JSONObject hero, final int targetTaW) {
		return HeroUtil.getTalentComplexity(hero, name.get());
	}

	public int getMaximum(final JSONObject hero) {
		final JSONObject attributes = hero.getObj("Eigenschaften");
		int max = 0;
		for (int i = 0; i < 3; ++i) {
			max = Math.max(max, HeroUtil.getCurrentValue(attributes.getObj(challenge.getString(i)), false));
		}
		final JSONArray aptitude = hero.getObj("Vorteile").getArrOrDefault("Begabung für Talent", null);
		if (aptitude != null) {
			for (int i = 0; i < aptitude.size(); ++i) {
				if (name.get().equals(aptitude.getObj(i).getStringOrDefault("Auswahl", null))) return max + 5;
			}
		}
		final JSONArray aptitudes = hero.getObj("Vorteile").getArrOrDefault("Begabung für Talentgruppe", null);
		if (aptitudes != null) {
			for (int i = 0; i < aptitudes.size(); ++i) {
				if (HeroUtil.findTalent(name.get())._2.equals(aptitudes.getObj(i).getStringOrDefault("Auswahl", null))) return max + 5;
			}
		}
		return max + 3;
	}

	public final String getName() {
		return name.get();
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
		boolean inserted = false;
		if (actualGroup != null && (talent.containsKey("Auswahl") || talent.containsKey("Freitext"))) {
			final JSONArray choiceTalent = actualGroup.getArr(name.get());
			if (actual == null) {
				actual = new JSONObject(choiceTalent);
				talentCache.put(actual, this);
			}
			if (!choiceTalent.contains(actual)) {
				choiceTalent.add(actual);
				inserted = true;
			}
		} else {
			if (actual == null) {
				actual = new JSONObject(actualGroup);
				talentCache.put(actual, this);
				inserted = true;
			}
			if (actualGroup != null && !actualGroup.containsKey(name.get())) {
				actualGroup.put(name.get(), actual);
				inserted = true;
			}
		}
		if (inserted) {
			if (!activated) {
				actual.put("aktiviert", false);
			}
			actual.notifyListeners(null);
		}
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
				final JSONArray actualTalent = actualGroup.getArr(name.get());
				actualTalent.remove(actual);
				if (actualTalent.size() == 0) {
					actualGroup.removeKey(name.get());
				}
			} else {
				actualGroup.removeKey(name.get());
			}
			talentCache.remove(actual);
			if (actual != null) {
				actualGroup.notifyListeners(null);
			}
			actual = null;
		}
	}

	@Override
	public final IntegerProperty sesProperty() {
		return ses;
	}

	public void setPrimaryTalent(final boolean primary) {
		if (primaryTalent.get() != primary) {
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
	}

	@Override
	public void setSes(final int ses) {
		if (actual == null) {
			insertTalent(false);
		}

		Enhanceable.super.setSes(ses);

		if (ses == 0 && !actual.getBoolOrDefault("aktiviert", true) && !actual.getBoolOrDefault("Leittalent", false)) {
			removeTalent();
		}
	}

	public void setValue(final int value) {
		if (this.value.get() != value) {
			if (actual == null) {
				insertTalent(true);
			}
			final JSONObject actualTalent = actual;
			if (value == Integer.MIN_VALUE) {
				if (ses.get() == 0 && !actual.getBoolOrDefault("Leittalent", false)) {
					removeTalent();
				} else {
					actual.removeKey("TaW");
					actual.put("aktiviert", false);
				}
			} else {
				actual.put("TaW", value);
				actual.removeKey("aktiviert");
			}
			this.value.set(value);
			actualTalent.notifyListeners(null);
		}
	}

	public void setVariant(final String variant) {
		if (!this.variant.get().equals(variant)) {
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
	}

	public IntegerProperty valueProperty() {
		return value;
	}
}
