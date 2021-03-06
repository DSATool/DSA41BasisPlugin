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

import java.util.Set;

import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class ProOrCon {

	public enum ChoiceOrTextEnum {
		CHOICE, NONE, TEXT
	}

	protected final JSONObject actual;
	protected final IntegerProperty cost = new SimpleIntegerProperty();
	protected final IntegerProperty numCheaper = new SimpleIntegerProperty();
	protected final StringProperty description = new SimpleStringProperty("");
	protected final ChoiceOrTextEnum first;
	protected final JSONObject hero;
	protected final StringProperty name;
	protected final StringProperty displayName;
	protected final JSONObject proOrCon;
	protected final ChoiceOrTextEnum second;
	protected final IntegerProperty value = new SimpleIntegerProperty(Integer.MIN_VALUE);
	protected final StringProperty variant = new SimpleStringProperty("");
	protected final BooleanProperty valid = new SimpleBooleanProperty(true);

	protected final int min;
	protected final int max;
	protected final int step;

	private final boolean stepwise;

	public ProOrCon(final String name, final JSONObject hero, final JSONObject proOrCon, final JSONObject actual) {
		this.hero = hero;
		this.proOrCon = proOrCon;
		this.actual = actual;
		this.name = new SimpleStringProperty(name);
		displayName = new SimpleStringProperty(name);

		final boolean hasChoice = proOrCon.containsKey("Auswahl") || "Auswahl".equals(proOrCon.getString("Spezialisierung"));
		final String text = proOrCon.getString("Freitext");
		stepwise = proOrCon.getBoolOrDefault("Abgestuft", false);
		final JSONObject profession = hero != null ? ResourceManager.getResource("data/Professionen").getObj(hero.getObj("Biografie").getString("Profession"))
				: null;
		final boolean hasBGB = "Breitgefächerte Bildung".equals(name);
		boolean hasBGBVariant = false;
		if (hasBGB) {
			final JSONObject bgbprofession = ResourceManager.getResource("data/Professionen").getObjOrDefault(actual.getString("Profession"), null);
			if (bgbprofession != null && bgbprofession.containsKey("Varianten")) {
				hasBGBVariant = true;
			}
		}
		final boolean hasVeteranVariant = "Veteran".equals(name) && profession != null && profession.containsKey("Varianten");

		if (hasBGB) {
			first = ChoiceOrTextEnum.CHOICE;
			description.set(actual.getStringOrDefault("Profession", ResourceManager.getResource("data/Professionen").keySet().iterator().next()));
			actual.put("Profession", description.get());
		} else if (hasVeteranVariant) {
			first = ChoiceOrTextEnum.TEXT;
			final JSONArray variants = actual.getArrOrDefault("Profession:Modifikation", null);
			if (variants != null) {
				description.set(String.join(", ", variants.getStrings()));
			}
		} else if (hasChoice) {
			first = ChoiceOrTextEnum.CHOICE;
			if (actual.containsKey("Auswahl")) {
				description.set(actual.getString("Auswahl"));
			} else {
				final Set<String> choices = getFirstChoiceItems(false);
				if (!choices.isEmpty()) {
					description.set(choices.iterator().next());
					for (final String currentChoice : choices) {
						updateValid();
						if (valid.get()) {
							break;
						}
						description.set(currentChoice);
					}
					updateValid();
					if (!valid.get()) {
						description.set(choices.iterator().next());
					}
					actual.put("Auswahl", description.get());
				}
			}
		} else if (text != null) {
			first = ChoiceOrTextEnum.TEXT;
			if (actual.containsKey("Freitext")) {
				description.set(actual.getString("Freitext"));
			} else {
				final Set<String> choices = getSecondChoiceItems(false);
				if (!choices.isEmpty()) {
					description.set(choices.iterator().next());
					for (final String currentChoice : choices) {
						updateValid();
						if (valid.get()) {
							break;
						}
						description.set(currentChoice);
					}
					updateValid();
					if (!valid.get()) {
						description.set(choices.iterator().next());
					}
					actual.put("Freitext", description.get());
				}
			}
		} else {
			first = ChoiceOrTextEnum.NONE;
		}
		description.addListener((o, oldV, newV) -> updateValid());

		if (hasBGBVariant) {
			second = ChoiceOrTextEnum.TEXT;
			final JSONArray variants = actual.getArrOrDefault("Profession:Modifikation", null);
			if (variants != null) {
				variant.set(String.join(", ", variants.getStrings()));
			}
		} else if (hasBGB) {
			second = ChoiceOrTextEnum.TEXT;
		} else if (hasChoice && text != null) {
			second = ChoiceOrTextEnum.TEXT;
			if (actual.containsKey("Freitext")) {
				variant.set(actual.getString("Freitext"));
			} else {
				final Set<String> choices = getSecondChoiceItems(false);
				if (!choices.isEmpty()) {
					setVariant(choices.iterator().next(), true);
					for (final String currentChoice : choices) {
						updateValid();
						if (valid.get()) {
							break;
						}
						setVariant(currentChoice, true);
					}
					updateValid();
					if (!valid.get()) {
						variant.set(choices.iterator().next());
					}
					actual.put("Freitext", variant.get());
				}
			}
		} else {
			second = ChoiceOrTextEnum.NONE;
		}
		variant.addListener((o, oldV, newV) -> updateValid());

		final Tuple3<Integer, Integer, Integer> bounds = calculateBounds(proOrCon);
		min = bounds._1;
		max = bounds._2;
		step = bounds._3;

		if (stepwise) {
			value.set(actual.getIntOrDefault("Stufe", min));
			actual.put("Stufe", value.get());
		}

		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));

		if (hero != null && proOrCon.containsKey("Gottheiten")) {
			final JSONObject liturgies = ResourceManager.getResource("data/Liturgien");
			final JSONObject liturgyKnowledges = hero.getObj("Talente").getObj("Liturgiekenntnis");
			if (!liturgyKnowledges.keySet().isEmpty()) {
				final String deity = liturgyKnowledges.keySet().iterator().next();
				final JSONObject deities = liturgies.getObj(name).getObjOrDefault("Gottheiten", null);
				if (deities != null && deities.containsKey(deity)) {
					displayName.set(deities.getObj(deity).getStringOrDefault("Name", name));
				}
			}
		}

		updateValid();
		if (hero != null) {
			hero.addListener(o -> updateValid());
		}
	}

	private Tuple3<Integer, Integer, Integer> calculateBounds(final JSONObject proOrCon) {
		int step = 1;
		final double cost = proOrCon.getDoubleOrDefault("Kosten", 1.0);
		if (Math.rint(cost) != cost) {
			for (int i = 1; i < 11; ++i) {
				final double quotient = i / cost;
				if (Math.rint(quotient) == quotient) {
					step = (int) quotient;
					break;
				}
			}
		}

		int min = proOrCon.getIntOrDefault("Stufe:Minimum", 1);
		for (int i = 0; i < step; ++i) {
			final double quotient = (min + i) / (double) step;
			if (Math.rint(quotient) == quotient) {
				min = min + i;
				break;
			}
		}

		final int max = proOrCon.getIntOrDefault("Stufe:Maximum", 9999);

		return new Tuple3<>(min, max, step);
	}

	public final IntegerProperty costProperty() {
		return cost;
	}

	public final StringProperty descriptionProperty() {
		return description;
	}

	public final ReadOnlyStringProperty displayNameProperty() {
		return displayName;
	}

	public ChoiceOrTextEnum firstChoiceOrText() {
		return first;
	}

	public JSONObject getActual() {
		return actual;
	}

	public double getBaseCost() {
		double baseCost = 1.0;
		if (proOrCon.containsKey("Kosten")) {
			baseCost = proOrCon.getDouble("Kosten");
		} else if (hero != null) {
			if (name.get().endsWith("spezialisierung") && description.get() != null && !"".equals(description.get())) {
				final String talentName = description.get();
				final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(talentName);
				int complexity = Integer.MAX_VALUE;
				if ("Zauber".equals(talentAndGroup._2)) {
					final JSONArray representations = hero.getObj("Sonderfertigkeiten").getArrOrDefault("Repräsentation", null);
					if (representations != null) {
						for (final JSONObject representation : representations.getObjs()) {
							final String abbreviation = DSAUtil.getRepresentationAbbreviation(representation.getString("Auswahl"));
							complexity = Math.min(HeroUtil.getSpellComplexity(hero, talentName, abbreviation, Integer.MAX_VALUE), complexity);
						}
					}
				} else {
					complexity = HeroUtil.getTalentComplexity(hero, talentName);
				}
				final JSONObject costs = ResourceManager.getResource("data/Steigerungskosten");
				baseCost = 20 * costs.getObj(DSAUtil.getEnhancementGroupString(complexity)).getIntOrDefault("Faktor", 1);
				final JSONArray specializations = hero.getObj("Sonderfertigkeiten").getArrOrDefault(name.get(), null);
				if (specializations != null) {
					int count = 0;
					for (final JSONObject specialization : specializations.getObjs()) {
						if (talentName.equals(specialization.getString("Auswahl"))) {
							++count;
						}
					}
					baseCost *= count + 1;
				}
			} else if (proOrCon.getParent() == ResourceManager.getResource("data/Liturgien")) {
				final JSONObject liturgies = ResourceManager.getResource("data/Liturgien");
				int level = proOrCon.getIntOrDefault("Grad", 1);
				final JSONObject liturgyKnowledges = hero.getObj("Talente").getObj("Liturgiekenntnis");
				if (!liturgyKnowledges.keySet().isEmpty()) {
					final String deity = liturgyKnowledges.keySet().iterator().next();
					final JSONObject deities = liturgies.getObj(name.get()).getObjOrDefault("Gottheiten", null);
					if (deities != null && deities.containsKey(deity)) {
						level = deities.getObj(deity).getIntOrDefault("Grad", level);
					}
				}
				final JSONObject levels = ResourceManager.getResource("data/Liturgiegrade");
				baseCost = levels.getObj("Grad " + level).getDoubleOrDefault("Kosten", level * 50.0);
			}
		}

		if (proOrCon.containsKey("Kosten:Voraussetzungen")) {
			final JSONArray requirements = proOrCon.getArr("Kosten:Voraussetzungen");
			for (int i = 0; i < requirements.size(); ++i) {
				final JSONObject requirement = requirements.getObj(i);
				final String choice = first == ChoiceOrTextEnum.CHOICE ? description.get() : "";
				final String text = first == ChoiceOrTextEnum.TEXT ? description.get() : second == ChoiceOrTextEnum.TEXT ? variant.get() : "";
				if (hero != null
						&& RequirementsUtil.isRequirementFulfilled(hero, requirement, choice.isEmpty() ? null : choice, text.isEmpty() ? null : text, false)) {
					baseCost *= requirement.getDoubleOrDefault("Multiplikativ", 1.0);
					baseCost /= requirement.getDoubleOrDefault("Divisor", 1.0);
				}
			}
		}
		return baseCost;
	}

	public final int getCost() {
		return cost.get();
	}

	public final String getDescription() {
		return description.get();
	}

	public final String getDisplayName() {
		return displayName.get();
	}

	public Set<String> getFirstChoiceItems(final boolean onlyUnused) {
		final String choice = "Breitgefächerte Bildung".equals(name.get()) ? "Profession"
				: "Veteran".equals(name.get()) ? "Profession:Variante" : "Auswahl".equals(proOrCon.getString("Spezialisierung")) ? "Waffenloses Kampftalent"
						: proOrCon.getStringOrDefault("Auswahl", proOrCon.getString("Freitext"));
		final Set<String> choices = HeroUtil.getChoices(hero, choice, null);

		if ("Talentspezialisierung".equals(name.get())) {
			final JSONObject talents = ResourceManager.getResource("data/Talente");
			choices.removeAll(talents.getObj("Nahkampftalente").keySet());
			choices.removeAll(talents.getObj("Fernkampftalente").keySet());
		}

		if (proOrCon.containsKey("Voraussetzungen")) {
			final JSONObject requirements = proOrCon.getObj("Voraussetzungen");
			if (requirements.containsKey("Auswahl")) {
				final JSONArray possible = requirements.getArr("Auswahl");
				choices.removeIf(name -> !possible.contains(name));
			} else if (!proOrCon.containsKey("Auswahl") && requirements.containsKey("Freitext")) {
				final JSONArray possible = requirements.getArr("Freitext");
				choices.removeIf(name -> !possible.contains(name));
			}
		}

		if (onlyUnused && hero != null && second == ChoiceOrTextEnum.NONE) {
			final JSONObject pros = hero.getObj("Vorteile");
			final JSONObject cons = hero.getObj("Nachteile");
			final JSONObject skills = hero.getObj("Sonderfertigkeiten");

			JSONArray used = null;
			if (pros.containsKey(getName())) {
				used = pros.getArr(getName());
			} else if (cons.containsKey(getName())) {
				used = cons.getArr(getName());
			} else if (skills.containsKey(getName())) {
				used = skills.getArr(getName());
			}

			if (used != null) {
				for (int i = 0; i < used.size(); ++i) {
					final JSONObject current = used.getObj(i);
					if (first == ChoiceOrTextEnum.CHOICE) {
						choices.remove(current.getString("Auswahl"));
					} else {
						choices.remove(current.getString("Freitext"));
					}
				}
			}
		}

		if (choices.isEmpty()) {
			choices.add("");
		}

		return choices;
	}

	public int getMaxValue() {
		return max;
	}

	public int getMinValue() {
		return min;
	}

	public final String getName() {
		return name.get();
	}

	public final int getNumCheaper() {
		return numCheaper.get();
	}

	public JSONObject getProOrCon() {
		return proOrCon;
	}

	public Set<String> getSecondChoiceItems(final boolean onlyUnused) {
		final String choice = "Breitgefächerte Bildung".equals(name.get()) ? "Profession:Variante:BGB" : proOrCon.getString("Freitext");
		final Set<String> choices = HeroUtil.getChoices(hero, choice, description != null ? description.get() : null);
		if (choice == null) return choices;

		if (proOrCon.containsKey("Voraussetzungen")) {
			final JSONObject requirements = proOrCon.getObj("Voraussetzungen");
			if (requirements.containsKey("Freitext")) {
				final JSONArray possible = requirements.getArr("Freitext");
				choices.removeIf(name -> !possible.contains(name));
			}
		}

		if (onlyUnused && hero != null) {
			final JSONObject pros = hero.getObj("Vorteile");
			final JSONObject cons = hero.getObj("Nachteile");
			final JSONObject skills = hero.getObj("Sonderfertigkeiten");

			JSONArray used = null;
			if (pros.containsKey(getName())) {
				used = pros.getArr(getName());
			} else if (cons.containsKey(getName())) {
				used = cons.getArr(getName());
			} else if (skills.containsKey(getName())) {
				used = skills.getArr(getName());
			}

			if (used != null) {
				for (int i = 0; i < used.size(); ++i) {
					final JSONObject current = used.getObj(i);
					if (!getDescription().equals(current.getString("Auswahl"))) {
						continue;
					}
					choices.remove(current.getString("Freitext"));
				}
			}
		}

		if (choices.isEmpty()) {
			choices.add("");
		}

		return choices;
	}

	public int getStep() {
		return step;
	}

	public final boolean getValid() {
		return valid.get();
	}

	public final int getValue() {
		return value.get();
	}

	public final String getVariant() {
		return variant.get();
	}

	public final ReadOnlyStringProperty nameProperty() {
		return name;
	}

	public final IntegerProperty numCheaperProperty() {
		return numCheaper;
	}

	public void remove() {
		JSONObject group;
		if (proOrCon.containsKey("Auswahl") || proOrCon.containsKey("Freitext")) {
			final JSONArray actualCon = (JSONArray) actual.getParent();
			actualCon.remove(actual);
			group = (JSONObject) actualCon.getParent();
			if (actualCon.size() == 0) {
				group.removeKey(name.get());
				group.notifyListeners(null);
			} else {
				actualCon.notifyListeners(null);
			}
		} else {
			group = (JSONObject) actual.getParent();
			group.removeKey(name.get());
			group.notifyListeners(null);
		}
	}

	public ChoiceOrTextEnum secondChoiceOrText() {
		return second;
	}

	public final void setCost(final int cost) {
		actual.put("Kosten", cost);
		this.cost.set(cost);
		actual.notifyListeners(null);
	}

	public void setDescription(final String description, final boolean applyEffect) {
		if (applyEffect) {
			HeroUtil.unapplyEffect(hero, name.get(), proOrCon, actual);
		}
		if (proOrCon.containsKey("Auswahl") || "Auswahl".equals(proOrCon.getString("Spezialisierung"))) {
			actual.put("Auswahl", description);
		} else if (proOrCon.containsKey("Freitext")) {
			actual.put("Freitext", description);
		} else if ("Breitgefächerte Bildung".equals(name.get())) {
			actual.put("Profession", description);
			variant.set("");
		} else if ("Veteran".equals(name.get())) {
			final JSONArray variants = new JSONArray(actual);
			final String[] variantStrings = description.trim().split(", ");
			for (final String variantName : variantStrings) {
				variants.add(variantName);
			}
			actual.put("Profession:Modifikation", variants);
		}
		if (applyEffect) {
			HeroUtil.applyEffect(hero, name.get(), proOrCon, actual);
		}
		this.description.set(description);
		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));
		actual.notifyListeners(null);
	}

	public void setNumCheaper(final int numCheaper) {
		JSONObject cheaper = null;
		final boolean isSkill = actual.getParent() == hero.getObj("Sonderfertigkeiten");
		if (isSkill) {
			final JSONObject cheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
			if (proOrCon.containsKey("Auswahl") || proOrCon.containsKey("Freitext")) {
				final JSONArray cheaperSkill = cheaperSkills.getArr(name.get());
				for (int i = 0; i < cheaperSkill.size(); ++i) {
					final JSONObject variant = cheaperSkill.getObj(i);
					if (proOrCon.containsKey("Auswahl") && !variant.getStringOrDefault("Auswahl", "").equals(getDescription())) {
						continue;
					}
					if (proOrCon.containsKey("Freitext")
							&& !variant.getStringOrDefault("Freitext", "").equals(proOrCon.containsKey("Auswahl") ? getDescription() : getVariant())) {
						continue;
					}
					cheaper = variant;
				}
				if (cheaper == null) {
					cheaper = new JSONObject(cheaperSkill);
					if (proOrCon.containsKey("Auswahl")) {
						cheaper.put("Auswahl", getDescription());
					}
					if (proOrCon.containsKey("Freitext")) {
						cheaper.put("Freitext", proOrCon.containsKey("Auswahl") ? getDescription() : getVariant());
					}
					cheaperSkill.add(cheaper);
				}
			} else {
				cheaperSkills.getObj(name.get());
			}
		} else {
			cheaper = actual;
		}
		if (numCheaper > 1) {
			cheaper.put(isSkill ? "Verbilligungen" : "SEs", numCheaper);
		} else if (numCheaper == 1) {
			cheaper.removeKey(isSkill ? "Verbilligungen" : "SEs");
		} else {
			if (isSkill) {
				cheaper.getParent().remove(cheaper);
			} else {
				cheaper.removeKey("SEs");
			}
		}
		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));
		this.numCheaper.set(numCheaper);
		actual.notifyListeners(null);
	}

	public void setValue(final int value) {
		HeroUtil.unapplyEffect(hero, name.get(), proOrCon, actual);
		actual.put("Stufe", value);
		HeroUtil.applyEffect(hero, name.get(), proOrCon, actual);
		updateCost(value, actual.getString("Auswahl"), actual.getString("Freitext"));
		this.value.set(value);
		actual.notifyListeners(null);
	}

	public void setVariant(final String variant, final boolean applyEffect) {
		if (applyEffect) {
			HeroUtil.unapplyEffect(hero, name.get(), proOrCon, actual);
		}
		if (proOrCon.containsKey("Auswahl") && proOrCon.containsKey("Freitext")) {
			actual.put("Freitext", variant);
		} else if ("Breitgefächerte Bildung".equals(name.get())) {
			final JSONArray variants = new JSONArray(actual);
			final String[] variantStrings = variant.trim().split(", ");
			for (final String variantName : variantStrings) {
				variants.add(variantName);
			}
			actual.put("Profession:Modifikation", variants);
		}
		if (applyEffect) {
			HeroUtil.applyEffect(hero, name.get(), proOrCon, actual);
		}
		this.variant.set(variant);
		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));
		actual.notifyListeners(null);
	}

	protected void updateCost(final int value, final String choice, final String text) {
		cost.set((int) Math.round(getBaseCost() * (stepwise ? value : 1)));
		if (actual.containsKey("Kosten")) {
			cost.set(actual.getDouble("Kosten").intValue());
		} else if (hero != null) {
			final JSONObject cheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
			if (cheaperSkills.containsKey(name.get())) {
				if (proOrCon.containsKey("Auswahl") || proOrCon.containsKey("Freitext")) {
					final JSONArray cheaperSkill = cheaperSkills.getArr(name.get());
					for (int i = 0; i < cheaperSkill.size(); ++i) {
						final JSONObject variant = cheaperSkill.getObj(i);
						if (proOrCon.containsKey("Auswahl") && !variant.getStringOrDefault("Auswahl", "").equals(choice)) {
							continue;
						}
						if (proOrCon.containsKey("Freitext") && !variant.getStringOrDefault("Freitext", "").equals(text)) {
							continue;
						}
						numCheaper.set(variant.getIntOrDefault("Verbilligungen", 1));
						cost.set((int) Math.round(cost.get() / Math.pow(2, numCheaper.get())));
					}
				} else {
					final JSONObject cheaperSkill = cheaperSkills.getObj(name.get());
					numCheaper.set(cheaperSkill.getIntOrDefault("Verbilligungen", 1));
					cost.set((int) Math.round(cost.get() / Math.pow(2, numCheaper.get())));
				}
			} else if (hero.getObj("Nachteile").containsKey("Elfische Weltsicht")) {
				final JSONObject pros = ResourceManager.getResource("data/Vorteile");
				final JSONObject cons = ResourceManager.getResource("data/Nachteile");
				if (!proOrCon.getBoolOrDefault("Leitsonderfertigkeit", false) && !pros.containsKey(name.get()) && !cons.containsKey(name.get())) {
					cost.set(cost.get() + (cost.get() + 1) / 2);
				}
			}
		}
	}

	protected void updateValid() {
		if (hero == null || !proOrCon.containsKey("Voraussetzungen")) return;
		final String choice = first == ChoiceOrTextEnum.CHOICE ? description.get() : "";
		final String text = first == ChoiceOrTextEnum.TEXT ? description.get() : second == ChoiceOrTextEnum.TEXT ? variant.get() : "";
		valid.set(RequirementsUtil.isRequirementFulfilled(hero, proOrCon.getObj("Voraussetzungen"), choice == null || choice.isEmpty() ? null : choice,
				text == null || text.isEmpty() ? null : text, true));
	}

	public final ReadOnlyBooleanProperty validProperty() {
		return valid;
	}

	public final IntegerProperty valueProperty() {
		return value;
	}

	public final StringProperty variantProperty() {
		return variant;
	}
}
