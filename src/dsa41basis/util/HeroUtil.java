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
package dsa41basis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class HeroUtil {

	public static final Set<String> scaleColors = new HashSet<>();
	public static final Set<String> hairColors = new HashSet<>();
	public static final Set<String> eyeColors = new HashSet<>();
	public static final Set<String> skinColors = new HashSet<>();

	static {
		final JSONObject races = ResourceManager.getResource("data/Rassen");

		DSAUtil.foreach(o -> true, (name, race) -> {
			if (race.containsKey("Schuppenfarbe")) {
				scaleColors.addAll(race.getObj("Schuppenfarbe").keySet());
			}
			if (race.containsKey("Haarfarbe")) {
				hairColors.addAll(race.getObj("Haarfarbe").keySet());
			}
			if (race.containsKey("Augenfarbe")) {
				eyeColors.addAll(race.getObj("Augenfarbe").keySet());
			}
			if (race.containsKey("Hautfarbe")) {
				final JSONArray colors = race.getArr("Hautfarbe");
				for (int i = 0; i < colors.size(); ++i) {
					skinColors.add(colors.getString(i));
				}
			}
		}, races);
	}

	public static void addMoney(final JSONObject hero, final int kreutzer) {
		final JSONObject money = hero.getObj("Besitz").getObj("Geld");
		int current = kreutzer;
		for (final String unit : new String[] { "Kreuzer", "Heller", "Silbertaler" }) {
			int newValue = money.getIntOrDefault(unit, 0) + current;
			if (newValue < 0) {
				current = -(int) Math.ceil(-newValue / 10.0);
				newValue -= current * 10;
			} else if (newValue >= 10) {
				current = (int) Math.floor(newValue / 10.0);
				newValue -= current * 10;
			} else {
				current = 0;
			}
			money.put(unit, newValue);
		}
		money.put("Dukaten", money.getIntOrDefault("Dukaten", 0) + current);
		money.notifyListeners(null);
	}

	public static void applyEffect(final JSONObject hero, final String effectorName, final JSONObject effector, final JSONObject actual) {
		final JSONObject effect = effector.getObjOrDefault("Effekte", null);
		if (hero == null || effect == null) return;

		if (effect.containsKey("Eigenschaften")) {
			final JSONObject attributes = hero.getObj("Eigenschaften");
			final JSONObject attributeChanges = effect.getObj("Eigenschaften");
			for (final String attributeName : attributeChanges.keySet()) {
				final JSONObject attribute = attributes.getObj(attributeName);
				attribute.put("Wert", attribute.getIntOrDefault("Wert", 0) + attributeChanges.getInt(attributeName) * actual.getIntOrDefault("Stufe", 1));
				attribute.put("Start", attribute.getIntOrDefault("Start", 0) + attributeChanges.getInt(attributeName) * actual.getIntOrDefault("Stufe", 1));
				attribute.notifyListeners(null);
			}
		}

		if (effect.containsKey("Basiswerte")) {
			final JSONObject actualBasicValues = hero.getObj("Basiswerte");
			final JSONObject basicValueChanges = effect.getObj("Basiswerte");
			for (final String basicValueName : basicValueChanges.keySet()) {
				final JSONObject basicValue = actualBasicValues.getObj(basicValueName);
				if ("Karmaenergie".equals(basicValueName)) {
					basicValue.put("Permanent",
							basicValue.getIntOrDefault("Permanent", 0) + basicValueChanges.getInt(basicValueName) * actual.getIntOrDefault("Stufe", 1));
				} else {
					basicValue.put("Modifikator",
							basicValue.getIntOrDefault("Modifikator", 0) + basicValueChanges.getInt(basicValueName) * actual.getIntOrDefault("Stufe", 1));
				}
				basicValue.notifyListeners(null);
			}
		}

		if (effect.containsKey("Vorteile/Nachteile/Sonderfertigkeiten")) {
			final JSONObject proConSkillChanges = effect.getObj("Vorteile/Nachteile/Sonderfertigkeiten");
			for (final String proConSkillName : proConSkillChanges.keySet()) {
				final Tuple<JSONObject, String> res = HeroUtil.findProConOrSkill(proConSkillName);
				final JSONObject proConSkill = res._1;
				final JSONObject target = hero.getObj(res._2);
				if (proConSkill == null) {
					continue;
				}
				if (proConSkill.containsKey("Auswahl") || proConSkill.containsKey("Freitext")) {
					final JSONArray currentProConSkillChanges = proConSkillChanges.getArr(proConSkillName);
					final JSONArray actualProConSkills = target.getArr(proConSkillName);
					for (int i = 0; i < currentProConSkillChanges.size(); ++i) {
						final JSONObject currentProConSkillChange = currentProConSkillChanges.getObj(i).clone(null);
						if ("Auswahl".equals(currentProConSkillChange.getString("Auswahl"))) {
							currentProConSkillChange.put("Auswahl", actual.getString("Auswahl"));
						}
						if ("Freitext".equals(currentProConSkillChange.getString("Freitext"))) {
							currentProConSkillChange.put("Freitext", actual.getString("Freitext"));
						}
						boolean match = false;
						int j;
						for (j = 0; j < actualProConSkills.size(); ++j) {
							final JSONObject actualProConSkill = actualProConSkills.getObj(j);
							if (proConSkill.containsKey("Auswahl")
									&& !actualProConSkill.getStringOrDefault("Auswahl", "").equals(currentProConSkillChange.getString("Auswahl"))) {
								continue;
							}
							if (proConSkill.containsKey("Freitext")
									&& !actualProConSkill.getStringOrDefault("Freitext", "").equals(currentProConSkillChange.getString("Freitext"))) {
								continue;
							}
							match = true;
							if (proConSkill.getBoolOrDefault("Abgestuft", false)) {
								actualProConSkill.put("Stufe",
										actualProConSkill.getIntOrDefault("Stufe", 0) + currentProConSkillChange.getIntOrDefault("Stufe", 0));
								applyEffect(hero, proConSkillName, proConSkill, currentProConSkillChange);
								actualProConSkill.notifyListeners(null);
							}
							break;
						}
						if (!match) {
							actualProConSkills.add(currentProConSkillChange.clone(actualProConSkills));
							if (!proConSkill.getBoolOrDefault("Abgestuft", false)) {
								actualProConSkills.getObj(j).put("AutomatischDurch", effectorName);
							}
							applyEffect(hero, proConSkillName, proConSkill, currentProConSkillChange);
							actualProConSkills.getObj(j).notifyListeners(null);
						}
					}
				} else {
					if (proConSkill.getBoolOrDefault("Abgestuft", false)) {
						target.getObj(proConSkillName).put("Stufe", target.getObj(proConSkillName).getIntOrDefault("Stufe", 0)
								+ proConSkillChanges.getObj(proConSkillName).getIntOrDefault("Stufe", 0));
						applyEffect(hero, proConSkillName, proConSkill, proConSkillChanges.getObj(proConSkillName));
						target.getObj(proConSkillName).notifyListeners(null);
					} else if (!target.containsKey(proConSkillName)) {
						target.put(proConSkillName, proConSkillChanges.getObj(proConSkillName).clone(target));
						target.getObj(proConSkillName).put("AutomatischDurch", effectorName);
						applyEffect(hero, proConSkillName, proConSkill, proConSkillChanges.getObj(proConSkillName));
						target.getObj(proConSkillName).notifyListeners(null);
					}
				}
			}
		}

		if (effect.containsKey("Talente")) {
			final JSONObject talentChanges = effect.getObj("Talente");
			for (final String talentName : talentChanges.keySet()) {
				final String modifiedName = "Auswahl".equals(talentName) ? actual.getString("Auswahl") : talentName;
				final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(modifiedName);
				final JSONObject talent = talentAndGroup._1;
				final String groupName = talentAndGroup._2;
				if (groupName != null) {
					final String targetValue = "Zauber".equals(groupName) ? "ZfW" : "TaW";
					final JSONObject actualGroup = "Zauber".equals(groupName) ? hero.getObj("Zauber") : hero.getObj("Talente").getObj(groupName);
					if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
						final JSONArray actualTalent;
						if ("Zauber".equals(groupName)) {
							final JSONObject actualSpell = actualGroup.getObj(modifiedName);
							if (actualSpell.size() == 0) {
								actualTalent = actualSpell.getArr("ÜNB");
							} else {
								actualTalent = actualSpell.getArr(actualSpell.keySet().iterator().next());
							}
						} else {
							actualTalent = actualGroup.getArr(modifiedName);
						}
						final JSONObject modifications = talentChanges.getObj(talentName);
						for (String variantName : modifications.keySet()) {
							final int change = modifications.getInt(variantName);
							if ("Auswahl".equals(variantName)) {
								variantName = actual.getString("Auswahl");
							} else if ("Freitext".equals(variantName)) {
								variantName = actual.getString("Freitext");
							}
							JSONObject actualVariant = null;
							for (int i = 0; i < actualTalent.size(); ++i) {
								final JSONObject variant = actualTalent.getObj(i);
								if (talent.containsKey("Auswahl") && variantName.equals(variant.getString("Auswahl"))
										|| talent.containsKey("Freitext") && variantName.equals(variant.getString("Freitext"))) {
									actualVariant = variant;
									break;
								}
							}
							if (actualVariant == null) {
								actualVariant = new JSONObject(actualTalent);
								actualVariant.put(talent.containsKey("Auswahl") ? "Auswahl" : "Freitext", variantName);
								actualTalent.add(actualVariant);
							}
							if (!actualVariant.containsKey(targetValue) || !actualVariant.getBoolOrDefault("aktiviert", true)) {
								actualVariant.put("AutomatischDurch", effectorName);
							}
							actualVariant.put(targetValue, actualVariant.getIntOrDefault(targetValue, 0) + change * actual.getIntOrDefault("Stufe", 1));
							actualVariant.removeKey("aktiviert");
							actualVariant.notifyListeners(null);
						}
					} else {
						final JSONObject actualTalent;
						if ("Zauber".equals(groupName)) {
							final JSONObject actualSpell = actualGroup.getObj(modifiedName);
							if (actualSpell.size() == 0) {
								actualTalent = actualSpell.getObj("ÜNB");
							} else {
								actualTalent = actualSpell.getObj(actualSpell.keySet().iterator().next());
							}
						} else {
							actualTalent = actualGroup.getObj(modifiedName);
						}
						final int change = talentChanges.getInt(talentName);
						if (actualTalent.size() == 0 || !actualTalent.getBoolOrDefault("aktiviert", true)) {
							actualTalent.put("AutomatischDurch", effectorName);
						}
						actualTalent.put(targetValue, actualTalent.getIntOrDefault(targetValue, 0) + change * actual.getIntOrDefault("Stufe", 1));
						actualTalent.removeKey("aktiviert");
						actualTalent.notifyListeners(null);
					}
				}
			}
		}
	}

	public static int deriveValue(final JSONObject derivation, final JSONObject hero, final JSONObject actual, final boolean includeManualMods) {
		int additional = 0;
		if (actual != null) {
			additional += actual.getIntOrDefault("Kauf", 0) + actual.getIntOrDefault("Permanent", 0) + actual.getIntOrDefault("Modifikator", 0)
					+ (includeManualMods ? actual.getIntOrDefault("Modifikator:Manuell", 0) : 0) + actual.getIntOrDefault("Wert", 0);
		}
		return (int) Math.round(deriveValueRaw(derivation, hero)) + additional;
	}

	public static double deriveValueRaw(final JSONObject derivation, final JSONObject hero) {
		int value = 0;

		final JSONObject attributes = hero.getObj("Eigenschaften");
		final JSONArray derivationAttributes = derivation.getArrOrDefault("Eigenschaften", new JSONArray(null));
		for (int i = 0; i < derivationAttributes.size(); ++i) {
			value += getCurrentValue(attributes.getObj(derivationAttributes.getString(i)), false);
		}

		final JSONObject derivedValues = ResourceManager.getResource("data/Basiswerte");
		final JSONObject basicValues = hero.getObj("Basiswerte");
		final JSONArray derivationBasicValues = derivation.getArrOrDefault("Basiswerte", new JSONArray(null));
		for (int i = 0; i < derivationBasicValues.size(); ++i) {
			final String derivedName = derivationBasicValues.getString(i);
			value += deriveValue(derivedValues.getObj(derivedName), hero, basicValues.getObj(derivedName), false);
		}

		if (derivation == derivedValues.getObj("Astralenergie")) {
			if (attributes.getParent() instanceof JSONObject) {
				if (hero.containsKey("Sonderfertigkeiten") && hero.getObj("Sonderfertigkeiten").containsKey("Gefäß der Sterne")) {
					value += getCurrentValue(attributes.getObj("CH"), false);
				}
			}
		} else if (derivation == derivedValues.getObj("Geschwindigkeit")) {
			final int GE = attributes.getObj("GE").getIntOrDefault("Wert", 1);
			final int geModifier = GE > 15 ? 1 : GE < 11 ? -1 : 0;
			value += geModifier;
		}

		return value * derivation.getDoubleOrDefault("Multiplikator", 1.0);
	}

	public static Tuple<JSONValue, JSONObject> findActualTalent(final JSONObject hero, final String talentName) {
		if (hero != null) {
			final JSONObject actualTalentGroups = hero.getObj("Talente");
			for (final String talentGroupName : actualTalentGroups.keySet()) {
				final JSONObject talentGroup = actualTalentGroups.getObj(talentGroupName);
				if (talentGroup.containsKey(talentName)) return new Tuple<>((JSONValue) talentGroup.getUnsafe(talentName), talentGroup);
			}
			final JSONObject spells = hero.getObjOrDefault("Zauber", null);
			if (spells != null && spells.containsKey(talentName)) return new Tuple<>(spells.getObj(talentName), spells);
		}
		return new Tuple<>(null, null);
	}

	public static Tuple<JSONObject, String> findProConOrSkill(final String name) {
		if (name != null && !name.isEmpty()) {
			final JSONObject pros = ResourceManager.getResource("data/Vorteile");
			if (pros.containsKey(name)) return new Tuple<>(pros.getObj(name), "Vorteile");
			final JSONObject cons = ResourceManager.getResource("data/Nachteile");
			if (cons.containsKey(name)) return new Tuple<>(cons.getObj(name), "Nachteile");
			final JSONObject skill = findSkill(name);
			if (skill != null) return new Tuple<>(skill, "Sonderfertigkeiten");
			ErrorLogger.log("Konnte Vorteil/Nachteil/Sonderfertigkeit nicht finden: " + name);
		}
		return new Tuple<>(null, null);
	}

	public static JSONObject findSkill(final String skillName) {
		if (skillName != null && !skillName.isEmpty()) {
			final JSONObject specialSkills = ResourceManager.getResource("data/Sonderfertigkeiten");
			for (final String skillGroup : specialSkills.keySet()) {
				final JSONObject group = specialSkills.getObj(skillGroup);
				if (group.containsKey(skillName)) return group.getObj(skillName);
			}
			final JSONObject rituals = ResourceManager.getResource("data/Rituale");
			for (final String skillGroup : rituals.keySet()) {
				final JSONObject group = rituals.getObj(skillGroup);
				if (group.containsKey(skillName)) return group.getObj(skillName);
			}
			final JSONObject liturgies = ResourceManager.getResource("data/Liturgien");
			if (liturgies.containsKey(skillName)) return liturgies.getObj(skillName);
			final JSONObject shamanistic = ResourceManager.getResource("data/Schamanenrituale");
			if (shamanistic.containsKey(skillName)) return shamanistic.getObj(skillName);
			ErrorLogger.log("Konnte Sonderfertigkeit nicht finden: " + skillName);
		}
		return null;
	}

	public static Tuple<JSONObject, String> findTalent(final String talentName) {
		if (talentName != null && !talentName.isEmpty()) {
			final JSONObject talentGroups = ResourceManager.getResource("data/Talente");
			for (final String talentGroupName : talentGroups.keySet()) {
				final JSONObject talentGroup = talentGroups.getObj(talentGroupName);
				if (talentGroup.containsKey(talentName)) return new Tuple<>(talentGroup.getObj(talentName), talentGroupName);
			}
			final JSONObject spells = ResourceManager.getResource("data/Zauber");
			if (spells.containsKey(talentName)) return new Tuple<>(spells.getObj(talentName), "Zauber");
			ErrorLogger.log("Konnte Talent nicht finden: " + talentName);
		}
		return new Tuple<>(null, null);
	}

	public static void foreachInventoryItem(final boolean isAnimal, final Predicate<JSONObject> filter, final BiConsumer<JSONObject, Boolean> function,
			final JSONObject inventory) {
		DSAUtil.foreach(filter, item -> {
			function.accept(item, isAnimal);
			foreachInventoryItem(isAnimal, filter, function, item);
		}, inventory.getArr("Ausrüstung"));
	}

	public static void foreachInventoryItem(final JSONObject hero, final Predicate<JSONObject> filter, final BiConsumer<JSONObject, Boolean> function) {
		foreachInventoryItem(false, filter, function, hero.getObj("Besitz"));
		DSAUtil.foreach(animal -> true, animal -> {
			foreachInventoryItem(true, filter, function, animal);
		}, hero.getArr("Tiere"));
	}

	public static String getAntiElement(final String element) {
		switch (element) {
		case "Eis":
			return "Humus";
		case "Erz":
			return "Luft";
		case "Feuer":
			return "Wasser";
		case "Humus":
			return "Eis";
		case "Luft":
			return "Erz";
		case "Wasser":
			return "Feuer";
		}
		return null;
	}

	public static Integer getAT(final JSONObject hero, JSONObject weapon, final String type, final boolean closeCombat, final boolean wrongHand,
			final boolean includeManualMods) {
		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey(closeCombat ? "Nahkampfwaffe" : "Fernkampfwaffe")) {
			weapon = weapon.getObj(closeCombat ? "Nahkampfwaffe" : "Fernkampfwaffe");
		}

		final JSONObject talent = ResourceManager.getResource("data/Talente").getObj(closeCombat ? "Nahkampftalente" : "Fernkampftalente").getObjOrDefault(type,
				null);

		if (talent == null) return null;

		final JSONObject actualTalent = hero.getObj("Talente").getObj(closeCombat ? "Nahkampftalente" : "Fernkampftalente").getObjOrDefault(type, null);
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");

		final boolean hasSpecialisation = weapon != null
				&& HeroUtil.getSpecialisation(skills.getArr("Waffenspezialisierung"), type,
						weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ"))) != null;
		final JSONObject weaponModifiers = weapon == null ? null : weapon.getObjOrDefault("Waffenmodifikatoren", baseWeapon.getObj("Waffenmodifikatoren"));

		final JSONObject weaponMastery = weapon == null ? null : HeroUtil.getSpecialisation(skills.getArr("Waffenmeister"), type,
				weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
		final int masteryAT = weaponMastery != null ? weaponMastery.getObj("Waffenmodifikatoren").getIntOrDefault("Attackemodifikator", 0) : 0;

		return deriveValue(ResourceManager.getResource("data/Basiswerte").getObj(closeCombat ? "Attacke-Basis" : "Fernkampf-Basis"), hero,
				hero.getObj("Basiswerte"), includeManualMods)
				+ (closeCombat && weaponModifiers != null ? weaponModifiers.getIntOrDefault("Attackemodifikator", 0) : 0)
				+ (actualTalent != null ? actualTalent.getIntOrDefault("AT", 0) : 0)
				+ (hasSpecialisation ? !closeCombat || talent.getBoolOrDefault("NurAT", false) ? 2 : 1 : 0) + masteryAT
				- Math.max(talent.getIntOrDefault("BEMultiplikativ", 1) * getBE(hero) + talent.getIntOrDefault("BEAdditiv", 0), 0)
						/ (!closeCombat || talent.getBoolOrDefault("NurAT", false) ? 1 : 2);
	}

	public static int getBE(final JSONObject hero) {
		double BE = getBERaw(hero);
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		if (skills.containsKey("Rüstungsgewöhnung III")) {
			BE -= 2;
		} else if (skills.containsKey("Rüstungsgewöhnung II")) {
			BE -= 1;
		} else if (skills.containsKey("Rüstungsgewöhnung I")) {
			final JSONArray items = hero.getObj("Besitz").getArr("Ausrüstung");
			boolean BEReduced = false;
			final JSONArray armorAdaption = skills.getArr("Rüstungsgewöhnung I");
			for (int i = 0; i < armorAdaption.size() && !BEReduced; ++i) {
				for (int j = 0; j < items.size(); ++j) {
					final JSONObject item = items.getObj(j);
					final JSONArray categories = item.getArr("Kategorien");
					if (categories.contains("Rüstung")) {
						JSONObject armor = item;
						if (item.containsKey("Rüstung")) {
							armor = item.getObj("Rüstung");
						}
						if (armorAdaption.getObj(i).getString("Freitext").equals(armor.getStringOrDefault("Typ", item.getString("Typ")))) {
							BE -= 1;
							BEReduced = true;
							break;
						}
					}
				}
			}
		}
		return (int) Math.max(0, Math.round(BE));
	}

	public static double getBERaw(final JSONObject hero) {
		final String armorSetting = Settings.getSettingStringOrDefault("Zonenrüstung", "Kampf", "Rüstungsart");

		final JSONArray items = hero.getObj("Besitz").getArr("Ausrüstung");
		double BE = 0;
		for (int i = 0; i < items.size(); ++i) {
			final JSONObject item = items.getObj(i);
			final JSONArray categories = item.getArr("Kategorien");
			if (categories.contains("Rüstung")) {
				JSONObject armor = item;
				if (item.containsKey("Rüstung")) {
					armor = item.getObj("Rüstung");
				}
				if ("Gesamtrüstung".equals(armorSetting)) {
					BE += armor.getIntOrDefault("Gesamtbehinderung", item.getIntOrDefault("Gesamtbehinderung", 0));
				} else {
					BE += armor.getDoubleOrDefault("Behinderung", item.getDoubleOrDefault("Behinderung",
							armor.getIntOrDefault("Gesamtbehinderung", item.getIntOrDefault("Gesamtbehinderung", 0)).doubleValue()));
				}
			}
		}
		return BE;
	}

	public static Set<String> getChoices(final JSONObject hero, final String choice, final String other) {
		final Set<String> choices = new LinkedHashSet<>();
		if (choice == null) return choices;
		switch (choice) {
		case "Merkmal":
			choices.addAll(ResourceManager.getResource("data/Merkmale").keySet());
			break;
		case "Ritual":
			final JSONObject rituals = ResourceManager.getResource("data/Rituale");
			for (final String group : rituals.keySet()) {
				choices.addAll(rituals.getObj(group).keySet());
			}
			break;
		case "Talentgruppe":
			choices.add("Kampftalente");
			choices.addAll(ResourceManager.getResource("data/Talentgruppen").keySet());
			choices.removeAll(Arrays.asList("Gaben", "Ritualkenntnis", "Liturgiekenntnis"));
			break;
		case "Talent":
			final JSONObject talents = ResourceManager.getResource("data/Talente");
			for (final String talentgroup : talents.keySet()) {
				choices.addAll(talents.getObj(talentgroup).keySet());
			}
			break;
		case "Zauber":
			choices.addAll(ResourceManager.getResource("data/Zauber").keySet());
			break;
		case "Körperliche Eigenschaft":
			final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
			for (final String attribute : attributes.keySet()) {
				if (attributes.getObj(attribute).getStringOrDefault("Eigenschaft", "geistig").equals("körperlich")) {
					choices.add(attributes.getObj(attribute).getString("Name"));
				}
			}
			break;
		case "Eigenschaft":
			final JSONObject attributes2 = ResourceManager.getResource("data/Eigenschaften");
			for (final String attribute : attributes2.keySet()) {
				choices.add(attributes2.getObj(attribute).getString("Name"));
			}
			break;
		case "Schlechte Eigenschaft":
			if (hero != null) {
				final JSONObject cons = ResourceManager.getResource("data/Nachteile");
				for (final String con : hero.getObj("Nachteile").keySet()) {
					if (cons.getObj(con).getBoolOrDefault("Schlechte Eigenschaft", false)) {
						choices.add(con);
					}
				}
			}
			break;
		case "Gottheit":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Liturgiekenntnis").keySet());
			break;
		case "Erzdämon":
			choices.addAll(ResourceManager.getResource("data/Erzdaemonen").keySet());
			choices.add("Aphasmayra");
			break;
		case "Kultur":
			final JSONObject cultures = ResourceManager.getResource("data/Kulturen");
			for (final String cultureName : cultures.keySet()) {
				final JSONObject culture = cultures.getObj(cultureName);
				if (culture.containsKey("Kulturkunde")) {
					choices.add(culture.getString("Kulturkunde"));
				}
				if (culture.containsKey("Varianten")) {
					final JSONObject variants = culture.getObj("Varianten");
					for (final String variantName : variants.keySet()) {
						final JSONObject variant = variants.getObj(variantName);
						if (variant.containsKey("Kulturkunde")) {
							choices.add(variant.getString("Kulturkunde"));
						}
					}
				}
			}
			Collections.addAll(choices, new String[] { "Schwarze Lande", "Trolle", "Grolme" });
			break;
		case "Fernkampftalent":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Fernkampftalente").keySet());
			break;
		case "Kampftalent":
			final JSONObject talents2 = ResourceManager.getResource("data/Talente");
			choices.addAll(talents2.getObj("Nahkampftalente").keySet());
			choices.addAll(talents2.getObj("Fernkampftalente").keySet());
			break;
		case "Profession":
			choices.addAll(ResourceManager.getResource("data/Professionen").keySet());
			break;
		case "Profession:Variante":
			if (hero != null) {
				final JSONObject variants = ResourceManager.getResource("data/Professionen").getObj(hero.getObj("Biografie").getString("Profession"))
						.getObj("Varianten");
				choices.addAll(getVariantStrings(variants));
			}
			break;
		case "Repräsentation":
			final JSONObject representations = ResourceManager.getResource("data/Repraesentationen");
			for (final String representation : representations.keySet()) {
				choices.add(representations.getObj(representation).getString("Name"));
			}
			break;
		case "Ritualkenntnis":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Ritualkenntnis").keySet());
			break;
		case "Kirche":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Liturgiekenntnis").keySet());
			choices.add("Bund des wahren Glaubens");
			break;
		case "Spezialisierung":
			final JSONObject talent = HeroUtil.findTalent(other)._1;
			if (talent != null && talent.containsKey("Spezialisierungen")) {
				final JSONArray specializations = talent.getArr("Spezialisierungen");
				for (int i = 0; i < specializations.size(); ++i) {
					choices.add(specializations.getString(i));
				}
			}
			final JSONObject spell = ResourceManager.getResource("data/Zauber").getObjOrDefault(other, null);
			if (spell != null) {
				if (spell.containsKey("Spontane Modifikationen")) {
					final JSONArray spoMos = spell.getArr("Spontane Modifikationen");
					for (int i = 0; i < spoMos.size(); ++i) {
						choices.add(spoMos.getString(i));
					}
				}
				if (spell.containsKey("Varianten")) {
					final JSONArray variants = spell.getArr("Varianten");
					for (int i = 0; i < variants.size(); ++i) {
						choices.add(variants.getString(i));
					}
				}
			}
			break;
		case "Waffe":
			final JSONObject weaponItems = ResourceManager.getResource("data/Ausruestung");
			for (final String item : weaponItems.keySet()) {
				if (weaponItems.getObj(item).getArr("Waffentypen").contains(other)) {
					choices.add(weaponItems.getObj(item).getString("Typ"));
				}
			}
			break;
		case "Rüstung":
			final JSONObject armorItems = ResourceManager.getResource("data/Ausruestung");
			for (final String item : armorItems.keySet()) {
				if (armorItems.getObj(item).getArr("Kategorien").contains("Rüstung")) {
					choices.add(armorItems.getObj(item).getString("Typ"));
				}
			}
			break;
		default:
			choices.add(choice);
			break;
		}

		return choices;
	}

	public static int getCurrentValue(final JSONObject actual, final boolean includeManualMod) {
		return actual.getIntOrDefault("Wert", 0) + (includeManualMod ? actual.getIntOrDefault("Modifikator:Manuell", 0) : 0);
	}

	public static int getDistance(final JSONObject hero, JSONObject weapon, final String type, final String distance) {
		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Fernkampfwaffe")) {
			weapon = weapon.getObj("Fernkampfwaffe");
		}
		final JSONObject weaponMastery = hero == null || weapon == null ? null
				: HeroUtil.getSpecialisation(hero.getObj("Sonderfertigkeiten").getArr("Waffenmeister"), type,
						weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));

		final JSONObject weaponDistances = weapon.getObjOrDefault("Reichweiten", baseWeapon.getObj("Reichweiten"));

		final int dist = weaponDistances.getIntOrDefault(distance, Integer.MIN_VALUE);
		if (dist == Integer.MIN_VALUE) return Integer.MIN_VALUE;
		return (int) Math.round(dist * (1 + (weaponMastery != null ? weaponMastery.getIntOrDefault("Reichweite", 0) * 0.1 : 0)));
	}

	public static int getLoadTime(final JSONObject hero, JSONObject weapon, final String type) {
		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Fernkampfwaffe")) {
			weapon = weapon.getObj("Fernkampfwaffe");
		}

		double loadTime = weapon.getIntOrDefault("Ladedauer", baseWeapon.getIntOrDefault("Ladedauer", 0));

		if (hero != null) {
			if ("Bogen".equals(type)) {
				if (hero.getObj("Sonderfertigkeiten").containsKey("Schnellladen (Bogen)")) {
					loadTime -= 1;
				}
				loadTime = Math.max(loadTime, 1);
			} else if ("Armbrust".equals(type)) {
				if (hero.getObj("Sonderfertigkeiten").containsKey("Schnellladen (Armbrust)")) {
					loadTime *= 0.75;
				}

				final JSONObject weaponMastery = weapon == null ? null
						: HeroUtil.getSpecialisation(hero.getObj("Sonderfertigkeiten").getArr("Waffenmeister"), type,
								weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
				if (weaponMastery != null && weaponMastery.getBoolOrDefault("Ladezeit", false)) {
					loadTime *= 0.5;
				}
			}
		}

		return (int) Math.round(loadTime);
	}

	public static Integer getPA(final JSONObject hero, JSONObject weapon, final String type, final boolean wrongHand, final boolean includeManualMods) {
		final JSONObject talent = ResourceManager.getResource("data/Talente").getObj("Nahkampftalente").getObjOrDefault(type, null);

		if (talent == null) return null;

		final boolean ATonly = talent.getBoolOrDefault("NurAT", false);

		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Nahkampfwaffe")) {
			weapon = weapon.getObj("Nahkampfwaffe");
		}

		final JSONObject actualTalent = hero.getObj("Talente").getObj("Nahkampftalente").getObjOrDefault(type, null);
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");

		final boolean hasSpecialisation = weapon != null
				&& HeroUtil.getSpecialisation(skills.getArr("Waffenspezialisierung"), type,
						weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ"))) != null;
		final JSONObject weaponModifiers = weapon == null ? null : weapon.getObjOrDefault("Waffenmodifikatoren", baseWeapon.getObj("Waffenmodifikatoren"));

		final JSONObject weaponMastery = weapon == null ? null : HeroUtil.getSpecialisation(skills.getArr("Waffenmeister"), type,
				weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
		final int masteryPA = weaponMastery != null ? weaponMastery.getObj("Waffenmodifikatoren").getIntOrDefault("Parademodifikator", 0) : 0;

		if (ATonly) return null;

		return deriveValue(ResourceManager.getResource("data/Basiswerte").getObj("Parade-Basis"), hero, hero.getObj("Basiswerte"), includeManualMods)
				+ (weaponModifiers == null ? 0 : weaponModifiers.getIntOrDefault("Parademodifikator", 0))
				+ (actualTalent != null ? actualTalent.getIntOrDefault("PA", 0) : 0) + (hasSpecialisation ? 1 : 0) + masteryPA
				- Math.max(talent.getIntOrDefault("BEMultiplikativ", 1) * getBE(hero) + talent.getIntOrDefault("BEAdditiv", 0) + (ATonly ? 0 : 1), 0)
						/ (ATonly ? 1 : 2);
	}

	public static JSONObject getSpecialisation(final JSONArray specialisations, final String talent, final String specialisation) {
		if (specialisations == null || specialisation == null) return null;
		for (int i = 0; i < specialisations.size(); ++i) {
			final JSONObject skill = specialisations.getObj(i);
			if (talent.equals(skill.getString("Auswahl"))) {
				if (specialisation.equals(skill.getStringOrDefault("Freitext", ""))) return skill;
				if (skill.containsKey("Waffen")) {
					final JSONArray weapons = skill.getArr("Waffen");
					for (int j = 0; j < weapons.size(); ++j) {
						if (specialisation.equals(weapons.getString(j))) return skill;
					}
				}
			}
		}
		return null;
	}

	public static int getSpellBaseComplexity(final String spellName, final String representation) {
		final JSONObject baseSpell = ResourceManager.getResource("data/Zauber").getObj(spellName);
		final JSONObject spell = baseSpell.getObj("Repräsentationen").getObjOrDefault(representation, baseSpell);

		return spell.getIntOrDefault("Komplexität", baseSpell.getIntOrDefault("Komplexität", 1));
	}

	public static int getSpellComplexity(final JSONObject hero, final String spellName, final String representation, final int targetZfW) {
		final JSONObject baseSpell = ResourceManager.getResource("data/Zauber").getObj(spellName);
		final JSONObject spell = baseSpell.getObj("Repräsentationen").getObjOrDefault(representation, baseSpell);

		int complexity = getSpellBaseComplexity(spellName, representation);

		final JSONObject pros = hero.getObj("Vorteile");
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final JSONObject cons = hero.getObj("Nachteile");

		final JSONArray actualRepresentations = skills.getArr("Repräsentation");

		boolean hasRepresentation = false;

		if ("ÜNB".equals(representation)) {
			complexity = 6;
			hasRepresentation = true;
		}

		boolean hasGuildMagic = false;
		boolean hasCharlatan = false;
		final String representationName = ResourceManager.getResource("data/Repraesentationen").getObj(representation).getStringOrDefault("Name", "");
		for (int i = 0; i < actualRepresentations.size(); ++i) {
			final String actualRepresentation = actualRepresentations.getObj(i).getString("Auswahl");
			if (representationName.equals(actualRepresentation)) {
				hasRepresentation = true;
				break;
			} else if ("Gildenmagisch".equals(actualRepresentation)) {
				hasGuildMagic = true;
			} else if ("Scharlatanisch".equals(actualRepresentation)) {
				hasCharlatan = true;
			}
		}
		if (!hasRepresentation) {
			if (hasGuildMagic && ("Srl".equals(representation) || "Bor".equals(representation))) {
				++complexity;
				hasRepresentation = true;
			} else if (hasCharlatan) {
				if ("Mag".equals(representation)) {
					++complexity;
					hasRepresentation = true;
				} else if ("Sch".equals(representation)) {
					complexity += 2;
					hasRepresentation = true;
				}
			}
		}

		if (!hasRepresentation) {
			if ("Sch".equals(representation) || actualRepresentations.size() == 1 && "Schelm".equals(actualRepresentations.getObj(0).getString("Auswahl"))) {
				complexity += 3;
			} else {
				complexity += 2;
			}
		}

		if (pros.containsKey("Begabung für Zauber")) {
			final JSONArray pro = pros.getArr("Begabung für Zauber");
			for (int i = 0; i < pro.size(); ++i) {
				if (spellName.equals(pro.getObj(i).getString("Auswahl"))) {
					--complexity;
					break;
				}
			}
		}

		final JSONArray traits = spell.getArrOrDefault("Merkmale", baseSpell.getArr("Merkmale"));
		boolean isAntiElement = false;

		if (pros.containsKey("Begabung für Merkmal")) {
			final JSONArray pro = pros.getArr("Begabung für Merkmal");
			for (int i = 0; i < pro.size(); ++i) {
				final String actualPro = pro.getObj(i).getString("Auswahl");
				boolean elementary = false;
				boolean demonical = false;
				String element = null;
				if ("Elementar (gesamt)".equals(actualPro)) {
					elementary = true;
				} else if ("Dämonisch (gesamt)".equals(actualPro)) {
					demonical = true;
				} else if (actualPro.startsWith("Elementar")) {
					element = actualPro.substring(11, actualPro.length() - 1);
				}
				for (int j = 0; j < traits.size(); ++j) {
					final String trait = traits.getString(j);
					if (trait.equals(actualPro) || elementary && trait.startsWith("Elementar") || demonical && trait.startsWith("Dämonisch")) {
						--complexity;
						break;
					} else if (element != null && trait.startsWith("Elementar")) {
						if (getAntiElement(element).equals(trait.substring(11, trait.length() - 1))) {
							++complexity;
							isAntiElement = true;
							break;
						}
					}
				}
			}
		}

		if (cons.containsKey("Unfähigkeit für Merkmal")) {
			final JSONArray con = cons.getArr("Unfähigkeit für Merkmal");
			for (int i = 0; i < con.size(); ++i) {
				final String actualCon = con.getObj(i).getString("Auswahl");
				boolean elementary = false;
				boolean demonical = false;
				if ("Elementar (gesamt)".equals(actualCon)) {
					elementary = true;
				} else if ("Dämonisch (gesamt)".equals(actualCon)) {
					demonical = true;
				}
				for (int j = 0; j < traits.size(); ++j) {
					final String trait = traits.getString(j);
					if (trait.equals(actualCon) || elementary && trait.startsWith("Elementar") || demonical && trait.startsWith("Dämonisch")) {
						++complexity;
						break;
					}
				}
			}
		}

		if (skills.containsKey("Merkmalskenntnis")) {
			final JSONArray skill = skills.getArr("Merkmalskenntnis");
			for (int i = 0; i < skill.size(); ++i) {
				final String actualSkill = skill.getObj(i).getString("Auswahl");
				boolean elementary = false;
				boolean demonical = false;
				String element = null;
				if ("Elementar (gesamt)".equals(actualSkill)) {
					elementary = true;
				} else if ("Dämonisch (gesamt)".equals(actualSkill)) {
					demonical = true;
				} else if (actualSkill.startsWith("Elementar")) {
					element = actualSkill.substring(11, actualSkill.length() - 1);
				}
				for (int j = 0; j < traits.size(); ++j) {
					final String trait = traits.getString(j);
					if (trait.equals(actualSkill) || elementary && trait.startsWith("Elementar") || demonical && trait.startsWith("Dämonisch")) {
						--complexity;
						break;
					} else if (element != null && trait.startsWith("Elementar")) {
						final String antiElement = getAntiElement(element);
						if (antiElement.equals(trait.substring(11, trait.length() - 1))) {
							for (int k = 0; k < skill.size(); ++k) {
								final String choice = skill.getObj(k).getString("Auswahl");
								if (antiElement.equals(choice)) {
									if (pros.containsKey("Elementarharmonisierte Aura (" + element + '/' + antiElement + ')')
											|| pros.containsKey("Elementarharmonisierte Aura (" + antiElement + '/' + element + ')')) {
										--complexity;
									} else {
										++complexity;
									}
								}
							}
							isAntiElement = true;
							++complexity;
							break;
						}
					}
				}
			}
		}

		final JSONObject actualSpell = hero.getObj("Zauber").getObjOrDefault(spellName, null);
		if (actualSpell != null) {
			final JSONObject actualRepresentation = actualSpell.getObj(representation);
			if (actualRepresentation != null && actualRepresentation.getBoolOrDefault("Hauszauber", false)) {
				--complexity;
			}
		}

		if (!isAntiElement && (spell.containsKey("Verwandte Sprüche") || baseSpell.containsKey("Verwandte Sprüche"))) {
			final JSONArray similarSpells = spell.getArrOrDefault("Verwandte Sprüche", baseSpell.getArr("Verwandte Sprüche"));
			similarSpells: for (int i = 0; i < similarSpells.size(); ++i) {
				final JSONObject similar = hero.getObj("Zauber").getObjOrDefault(similarSpells.getString(i), null);
				if (similar != null) {
					for (final String currentRep : similar.keySet()) {
						if (!"Temporär".equals(currentRep) && similar.getObj(currentRep).getIntOrDefault("ZfW", 0) >= targetZfW) {
							--complexity;
							break similarSpells;
						}
					}
				}
			}
		}

		if (cons.containsKey("Elfische Weltsicht")) {
			++complexity;
			if ("Elf".equals(representation) && spell.getObj("Verbreitung").getIntOrDefault("Elf", 0) > 4) {
				--complexity;
			} else if (actualSpell != null && actualSpell.containsKey(representation)
					&& actualSpell.getObj(representation).getBoolOrDefault("Leittalent", false)) {
				--complexity;
			}
		}

		return Math.max(complexity, 0);
	}

	public static int getTalentBaseComplexity(final String talentName) {
		final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(talentName);
		final JSONObject talent = talentAndGroup._1;
		JSONObject talentGroup = ResourceManager.getResource("data/Talentgruppen").getObj(talentAndGroup._2);

		if ("Sprachen und Schriften".equals(talentAndGroup._2)) {
			talentGroup = talentGroup.getObj(talent.getBoolOrDefault("Schrift", false) ? "Schriften" : "Sprachen");
		}

		final int complexity = talentGroup != null ? talentGroup.getIntOrDefault("Steigerung", 0) : 0;
		return complexity + talent.getIntOrDefault("Steigerung", 0);
	}

	public static int getTalentComplexity(final JSONObject hero, final String talentName) {
		int complexity = getTalentBaseComplexity(talentName);

		final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(talentName);
		final JSONObject talent = talentAndGroup._1;

		if ("Ritualkenntnis".equals(talentAndGroup._2)) {
			final JSONObject representations = ResourceManager.getResource("data/Repraesentationen");
			boolean foundRep = false;
			for (final String rep : representations.keySet()) {
				final JSONObject representation = representations.getObj(rep);
				if (talentName.equals(representation.getString("Name"))) {
					foundRep = true;
					break;
				}
			}
			if (foundRep) {
				final JSONObject skills = hero.getObj("Sonderfertigkeiten");
				if (skills.containsKey("Repräsentation")) {
					final JSONArray actualRepresentations = skills.getArr("Repräsentation");
					boolean foundActual = false;
					for (int i = 0; i < actualRepresentations.size(); ++i) {
						final String actualRepresentation = actualRepresentations.getObj(i).getString("Auswahl");
						if (talentName.equals(actualRepresentation)) {
							foundActual = true;
							break;
						}
					}
					if (!foundActual) {
						complexity += 2;
					}
				}
			}
		} else if ("Sprachen und Schriften".equals(talentAndGroup._2)) {
			final JSONObject actualTalent = (JSONObject) HeroUtil.findActualTalent(hero, talentName)._1;

			if (actualTalent != null && (actualTalent.getBoolOrDefault("Muttersprache", false) || actualTalent.getBoolOrDefault("Zweitsprache", false)
					|| actualTalent.getBoolOrDefault("Lehrsprache", false))) {
				--complexity;
			} else {
				final JSONArray languageGroups = talent.getArrOrDefault("Sprachfamilien", null);
				if (languageGroups != null) {
					final boolean[] found = { false };
					DSAUtil.foreach(language -> language.getBoolOrDefault("Muttersprache", false), (name, language) -> {
						final JSONArray groups = HeroUtil.findTalent(name)._1.getArrOrDefault("Sprachfamilien", null);
						if (groups != null) {
							for (int i = 0; i < groups.size(); ++i) {
								if (languageGroups.contains(groups.getString(i))) {
									found[0] = true;
									return false;
								}
							}
						}
						return true;
					}, hero.getObj("Talente").getObj("Sprachen und Schriften"));
					if (found[0]) {
						--complexity;
					}
				}
			}
		}

		boolean hasReduction = false;

		final JSONObject pros = hero.getObj("Vorteile");
		final JSONObject cons = hero.getObj("Nachteile");

		if (pros.containsKey("Begabung für Talent")) {
			final JSONArray pro = pros.getArr("Begabung für Talent");
			for (int i = 0; i < pro.size(); ++i) {
				final String actualPro = pro.getObj(i).getString("Auswahl");
				if (talentName.equals(actualPro)) {
					--complexity;
					hasReduction = true;
					break;
				}
			}
		}

		if (!hasReduction && pros.containsKey("Begabung für Talentgruppe")) {
			final JSONArray pro = pros.getArr("Begabung für Talentgruppe");
			for (int i = 0; i < pro.size(); ++i) {
				final String actualPro = pro.getObj(i).getString("Auswahl");
				if (talentAndGroup._2.equals(actualPro)) {
					--complexity;
					break;
				}
			}
		}

		if (cons.containsKey("Unfähigkeit für Talent")) {
			final JSONArray con = cons.getArr("Unfähigkeit für Talent");
			for (int i = 0; i < con.size(); ++i) {
				final String actualCon = con.getObj(i).getString("Auswahl");
				if (talentName.equals(actualCon)) {
					++complexity;
					break;
				}
			}
		}

		if (cons.containsKey("Unfähigkeit für Talentgruppe")) {
			final JSONArray con = cons.getArr("Unfähigkeit für Talentgruppe");
			for (int i = 0; i < con.size(); ++i) {
				final String actualCon = con.getObj(i).getString("Auswahl");
				if (talentAndGroup._2.equals(actualCon)) {
					++complexity;
					break;
				}
			}
		}

		if (cons.containsKey("Elfische Weltsicht")) {
			++complexity;
			final JSONValue actualTalent = HeroUtil.findActualTalent(hero, talentName)._1;
			if (talent.getBoolOrDefault("Leittalent", false)) {
				--complexity;
			} else if (actualTalent instanceof JSONArray) {
				for (int i = 0; i < actualTalent.size(); ++i) {
					if (((JSONArray) actualTalent).getObj(i).getBoolOrDefault("Leittalent", false)) {
						--complexity;
						break;
					}
				}
			} else if (actualTalent instanceof JSONObject && ((JSONObject) actualTalent).getBoolOrDefault("Leittalent", false)) {
				--complexity;
			}
		}

		return complexity;
	}

	public static String getTPString(final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon) {
		final JSONObject TPValues = weapon.getObjOrDefault("Trefferpunkte", baseWeapon.getObjOrDefault("Trefferpunkte", null));
		if (TPValues == null) return "";

		final StringBuilder TPString = new StringBuilder();
		TPString.append(TPValues.getIntOrDefault("Würfel:Anzahl", 1));
		TPString.append('W');
		TPString.append(TPValues.getIntOrDefault("Würfel:Typ", 6));
		int TPAdditive = TPValues.getIntOrDefault("Trefferpunkte", 0);

		if (hero != null && (weapon.containsKey("Trefferpunkte/Körperkraft") || baseWeapon.containsKey("Trefferpunkte/Körperkraft"))) {
			final JSONObject TPKKValues = weapon.getObjOrDefault("Trefferpunkte/Körperkraft", baseWeapon.getObj("Trefferpunkte/Körperkraft"));
			final int threshold = TPKKValues.getIntOrDefault("Schwellenwert", Integer.MIN_VALUE);
			final int KK = HeroUtil.getCurrentValue(hero.getObj("Eigenschaften").getObj("KK"), true);
			final int steps = TPKKValues.getIntOrDefault("Schadensschritte", Integer.MIN_VALUE);
			if (threshold != Integer.MIN_VALUE && steps != Integer.MIN_VALUE) {
				final int TPKKModifier = (KK - threshold) / steps;
				TPAdditive += TPKKModifier;
			}
		}

		if (TPAdditive != 0) {
			if (TPAdditive > 0) {
				TPString.append('+');
			}
			TPString.append(TPAdditive);
		}

		if (TPValues.getBoolOrDefault("Reduzierte Wundschwelle", false)) {
			TPString.append('*');
		}
		if (TPValues.getBoolOrDefault("Ausdauerschaden", false)) {
			TPString.append("(A)");
		}
		return TPString.toString();
	}

	private static List<String> getVariantStrings(final JSONObject variants) {
		final List<String> result = new ArrayList<>();
		final List<String> combinable = new ArrayList<>();
		for (final String variantName : variants.keySet()) {
			final JSONObject variant = variants.getObj(variantName);
			if (variant.getBoolOrDefault("kombinierbar", false)) {
				final List<String> newCombinations = new ArrayList<>(combinable.size());
				for (final String variantString : combinable) {
					newCombinations.add(variantString + ", " + variantName);
				}
				combinable.add(variantName);
				combinable.addAll(newCombinations);
			} else {
				final List<String> newVariants = getVariantStrings(variant.getObj("Varianten"));
				for (final String variantString : newVariants) {
					if (!variantString.equals(variantName) && !variantString.startsWith(variantName + ",")) {
						result.add(variantName + ", " + variantString);
					} else {
						result.add(variantString);
					}
				}
				if (newVariants.isEmpty()) {
					result.add(variantName);
				}
			}
		}
		final List<String> newCombinations = new ArrayList<>(combinable.size());
		for (final String variantString : result) {
			for (final String combination : combinable) {
				newCombinations.add(variantString + ", " + combination);
			}
		}
		result.addAll(newCombinations);
		return result;
	}

	public static int getZoneRS(final JSONObject hero, final String zone) {
		final String armorSetting = Settings.getSettingStringOrDefault("Zonenrüstung", "Kampf", "Rüstungsart");

		final JSONArray armorList = hero.getObj("Besitz").getArr("Ausrüstung");
		int RS = 0;
		for (int i = 0; i < armorList.size(); ++i) {
			final JSONObject item = armorList.getObj(i);
			final JSONArray categories = item.getArr("Kategorien");
			if (categories != null && categories.contains("Rüstung")) {
				JSONObject armor = item;
				if (item.containsKey("Rüstung")) {
					armor = item.getObj("Rüstung");
				}
				final JSONObject zoneRS = armor.getObjOrDefault("Rüstungsschutz", item.getObj("Rüstungsschutz"));
				if ("Gesamtrüstung".equals(armorSetting) || zoneRS == null) {
					RS += armor.getIntOrDefault("Gesamtrüstungsschutz", item.getIntOrDefault("Gesamtrüstungsschutz", 0));
				} else {
					RS += zoneRS.getIntOrDefault(zone, 0);
				}
			}
		}

		final JSONObject pros = hero.getObj("Vorteile");
		if (pros.containsKey("Natürlicher Rüstungsschutz")) {
			RS += pros.getObj("Natürlicher Rüstungsschutz").getIntOrDefault("Stufe", 0);
		}

		return RS;
	}

	public static Integer interpretSpellRoll(final JSONObject hero, final String talentName, final String representation, final String specialization,
			final String[] challenge,
			final Tuple3<Integer, Integer, Integer> roll, final int modification) {
		final JSONObject spells = ResourceManager.getResource("data/Zauber");
		final JSONObject actualSpells = hero.getObjOrDefault("Zauber", null);
		if (actualSpells == null) return null;
		final JSONObject actualSpell = actualSpells.getObjOrDefault(talentName, null);
		if (actualSpell == null) return null;
		final JSONObject actualRepresentation = actualSpell.getObjOrDefault(representation, null);
		final JSONObject talent = spells.getObjOrDefault(talentName, null);
		if (talent == null || actualRepresentation == null) return null;
		int taw = actualRepresentation.getIntOrDefault("ZfW", 0);
		if (specialization != null) {
			final JSONObject specialSkills = hero.getObj("Sonderfertigkeiten");
			if (specialSkills.containsKey("Zauberspezialisierung")) {
				final JSONArray talentSpecializations = specialSkills.getArr("Zauberspezialisierung");
				for (int i = 0; i < talentSpecializations.size(); ++i) {
					final JSONObject talentSpecialization = talentSpecializations.getObj(i);
					if (talentName.equals(talentSpecialization.getString("Auswahl")) && specialization.equals(talentSpecialization.getString("Freitext"))) {
						taw += 2;
						break;
					}
				}
			}
		}
		return interpretTalentRoll(hero, talent, spells, challenge, taw, roll, modification);
	}

	private static Integer interpretTalentRoll(final JSONObject hero, final JSONObject talent, final JSONObject talentGroup, final String[] challenge,
			final int taw,
			final Tuple3<Integer, Integer, Integer> roll, final int modification) {
		final int modifiedTaw = taw + modification;
		int result = modifiedTaw;
		final JSONObject attributes = hero.getObj("Eigenschaften");
		int ones = 0;
		for (int i = 0; i < 3; ++i) {
			final String attributeName = challenge[i];
			final JSONObject attribute = attributes.getObj(attributeName);
			final int currentRoll = (Integer) roll.get(i + 1);
			if (currentRoll == 1) {
				++ones;
			}
			final int currentResult = getCurrentValue(attribute, true) - currentRoll + (modifiedTaw < 0 ? modifiedTaw : 0);
			if (currentResult < 0) {
				result += currentResult;
			}
		}
		if (ones >= 2) return Math.max(1, taw);
		return Math.min(result, Math.max(0, taw));
	}

	public static Integer interpretTalentRoll(final JSONObject hero, final String talentName, final String specialization, final String[] challenge,
			final Tuple3<Integer, Integer, Integer> roll, final int modification) {
		final JSONValue actualTalent = findActualTalent(hero, talentName)._1;
		final Tuple<JSONObject, String> talentAndGroup = findTalent(talentName);
		final JSONObject talent = talentAndGroup._1;
		final JSONObject talentGroup = ResourceManager.getResource("data/Talente").getObj(talentAndGroup._2);
		if (talent == null || actualTalent == null && !talent.getBoolOrDefault("Basis", false)) return null;
		int taw = 0;
		if (actualTalent instanceof JSONArray) {
			for (int i = 0; i < actualTalent.size(); ++i) {
				taw = Math.max(taw, ((JSONArray) actualTalent).getObj(i).getIntOrDefault("TaW", 0));
			}
		} else if (actualTalent instanceof JSONObject) {
			taw = ((JSONObject) actualTalent).getIntOrDefault("TaW", 0);
		}
		if (specialization != null && actualTalent != null) {
			final JSONObject specialSkills = hero.getObj("Sonderfertigkeiten");
			if (specialSkills.containsKey("Talentspezialisierung")) {
				final JSONArray talentSpecializations = specialSkills.getArr("Talentspezialisierung");
				for (int i = 0; i < talentSpecializations.size(); ++i) {
					final JSONObject talentSpecialization = talentSpecializations.getObj(i);
					if (talentName.equals(talentSpecialization.getString("Auswahl")) && specialization.equals(talentSpecialization.getString("Freitext"))) {
						taw += 2;
						break;
					}
				}
			}
		}
		return interpretTalentRoll(hero, talent, talentGroup, challenge, taw, roll, modification);
	}

	public static boolean isClerical(final JSONObject hero, final boolean includeAcolytes) {
		if (hero == null) return false;
		final JSONObject pros = hero.getObj("Vorteile");
		if (pros.containsKey("Geweiht")) return true;
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		if (skills.containsKey("Spätweihe")) return true;
		if (skills.containsKey("Kontakt zum Großen Geist")) return true;
		if (includeAcolytes && skills.containsKey("Akoluth")) return true;
		return false;
	}

	public static boolean isMagical(final JSONObject hero) {
		if (hero == null) return false;
		final JSONObject pros = hero.getObj("Vorteile");
		if (pros.containsKey("Vollzauberer") || pros.containsKey("Halbzauberer") || pros.containsKey("Viertelzauberer")) return true;
		return false;
	}

	public static boolean isNoble(final JSONObject hero) {
		if (hero == null) return false;
		final JSONObject pros = hero.getObj("Vorteile");
		if (pros.containsKey("Adlige Abstammung") || pros.containsKey("Adliges Erbe") || pros.containsKey("Amtsadel")) return true;
		return false;
	}

	public static int randomSize(final JSONObject calculation, final boolean small) {
		if (calculation == null) return 0;
		int size = calculation.getIntOrDefault("Basis", 0);
		final JSONArray random = calculation.getArr("Würfel");
		for (int i = 0; i < random.size(); ++i) {
			final JSONObject dice = random.getObj(i);
			final int num = dice.getIntOrDefault("Würfel:Anzahl", 1);
			final int type = dice.getIntOrDefault("Würfel:Typ", 6);
			for (int j = 0; j < num; ++j) {
				size += DSAUtil.diceRoll(type);
			}
		}
		if (small) {
			while (size > 160) {
				size -= size / 10;
			}
		}
		return size;
	}

	public static int randomWeight(final JSONObject calculation, final int size, final double deviation, final boolean obese) {
		if (calculation == null) return 0;
		int weight = size + calculation.getIntOrDefault("Modifikator", 0);
		weight += weight * DSAUtil.random.nextGaussian() / 3 * deviation;
		if (obese) {
			weight += weight / 2;
		}
		return weight;
	}

	public static void unapplyEffect(final JSONObject hero, final String effectorName, final JSONObject effector, final JSONObject actual) {
		final JSONObject effect = effector.getObjOrDefault("Effekte", null);
		if (hero == null || effect == null) return;

		if (effect.containsKey("Eigenschaften")) {
			final JSONObject attributes = hero.getObj("Eigenschaften");
			final JSONObject attributeChanges = effect.getObj("Eigenschaften");
			for (final String attributeName : attributeChanges.keySet()) {
				final JSONObject attribute = attributes.getObj(attributeName);
				attribute.put("Wert", attribute.getIntOrDefault("Wert", 0) - attributeChanges.getInt(attributeName) * actual.getIntOrDefault("Stufe", 1));
				attribute.put("Start", attribute.getIntOrDefault("Start", 0) - attributeChanges.getInt(attributeName) * actual.getIntOrDefault("Stufe", 1));
				attribute.notifyListeners(null);
			}
		}

		if (effect.containsKey("Basiswerte")) {
			final JSONObject actualBasicValues = hero.getObj("Basiswerte");
			final JSONObject basicValueChanges = effect.getObj("Basiswerte");
			for (final String basicValueName : basicValueChanges.keySet()) {
				final JSONObject basicValue = actualBasicValues.getObj(basicValueName);
				if ("Karmaenergie".equals(basicValueName)) {
					basicValue.put("Permanent",
							basicValue.getIntOrDefault("Permanent", 0) - basicValueChanges.getInt(basicValueName) * actual.getIntOrDefault("Stufe", 1));
				} else {
					basicValue.put("Modifikator",
							basicValue.getIntOrDefault("Modifikator", 0) - basicValueChanges.getInt(basicValueName) * actual.getIntOrDefault("Stufe", 1));
				}
				basicValue.notifyListeners(null);
			}
		}

		if (effect.containsKey("Vorteile/Nachteile/Sonderfertigkeiten")) {
			final JSONObject proConSkillChanges = effect.getObj("Vorteile/Nachteile/Sonderfertigkeiten");
			for (final String proConSkillName : proConSkillChanges.keySet()) {
				final Tuple<JSONObject, String> res = HeroUtil.findProConOrSkill(proConSkillName);
				final JSONObject proConSkill = res._1;
				final JSONObject target = hero.getObj(res._2);
				if (proConSkill == null) {
					continue;
				}
				if (proConSkill.containsKey("Auswahl") || proConSkill.containsKey("Freitext")) {
					final JSONArray currentProConSkillChanges = proConSkillChanges.getArr(proConSkillName);
					final JSONArray actualProConSkills = target.getArr(proConSkillName);
					for (int i = 0; i < currentProConSkillChanges.size(); ++i) {
						final JSONObject currentProConSkillChange = currentProConSkillChanges.getObj(i).clone(null);
						if ("Auswahl".equals(currentProConSkillChange.getString("Auswahl"))) {
							currentProConSkillChange.put("Auswahl", actual.getString("Auswahl"));
						}
						if ("Freitext".equals(currentProConSkillChange.getString("Freitext"))) {
							currentProConSkillChange.put("Freitext", actual.getString("Freitext"));
						}
						for (int j = 0; j < actualProConSkills.size(); ++j) {
							final JSONObject actualProConSkill = actualProConSkills.getObj(j);
							if (proConSkill.containsKey("Auswahl")
									&& !actualProConSkill.getStringOrDefault("Auswahl", "").equals(currentProConSkillChange.getString("Auswahl"))) {
								continue;
							}
							if (proConSkill.containsKey("Freitext")
									&& !actualProConSkill.getStringOrDefault("Freitext", "").equals(currentProConSkillChange.getString("Freitext"))) {
								continue;
							}
							if (proConSkill.getBoolOrDefault("Abgestuft", false)) {
								actualProConSkill.put("Stufe",
										actualProConSkill.getIntOrDefault("Stufe", 0) - currentProConSkillChange.getIntOrDefault("Stufe", 0));
								actualProConSkill.notifyListeners(null);
								if (actualProConSkill.getInt("Stufe") == 0) {
									actualProConSkills.remove(actualProConSkill);
								}
								unapplyEffect(hero, proConSkillName, proConSkill, currentProConSkillChange);
							} else if (actualProConSkill.getStringOrDefault("AutomatischDurch", "").equals(effectorName)) {
								actualProConSkills.remove(actualProConSkill);
								unapplyEffect(hero, proConSkillName, proConSkill, currentProConSkillChange);
							}
							break;
						}
					}
				} else {
					final JSONObject actualProConSkill = target.getObj(proConSkillName);
					if (proConSkill.getBoolOrDefault("Abgestuft", false)) {
						actualProConSkill.put("Stufe",
								actualProConSkill.getIntOrDefault("Stufe", 0) - proConSkillChanges.getObj(proConSkillName).getIntOrDefault("Stufe", 0));
						actualProConSkill.notifyListeners(null);
						if (actualProConSkill.getInt("Stufe") == 0) {
							target.removeKey(proConSkillName);
						}
						unapplyEffect(hero, proConSkillName, proConSkill, proConSkillChanges.getObj(proConSkillName));
					} else if (actualProConSkill.getStringOrDefault("AutomatischDurch", "").equals(effectorName)) {
						target.removeKey(proConSkillName);
						unapplyEffect(hero, proConSkillName, proConSkill, proConSkillChanges.getObj(proConSkillName));
					}
				}
				target.notifyListeners(null);
			}
		}

		if (effect.containsKey("Talente")) {
			final JSONObject talentChanges = effect.getObj("Talente");
			for (final String talentName : talentChanges.keySet()) {
				final String modifiedName = "Auswahl".equals(talentName) ? actual.getString("Auswahl") : talentName;
				final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(modifiedName);
				final JSONObject talent = talentAndGroup._1;
				final String groupName = talentAndGroup._2;
				if (groupName != null) {
					final String targetValue = "Zauber".equals(groupName) ? "ZfW" : "TaW";
					final JSONObject actualGroup = "Zauber".equals(groupName) ? hero.getObj("Zauber") : hero.getObj("Talente").getObj(groupName);
					if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
						final JSONArray actualTalent;
						if ("Zauber".equals(groupName)) {
							final JSONObject actualSpell = actualGroup.getObj(modifiedName);
							if (actualSpell.size() == 0) {
								actualTalent = actualSpell.getArr("ÜNB");
							} else {
								actualTalent = actualSpell.getArr(actualSpell.keySet().iterator().next());
							}
						} else {
							actualTalent = actualGroup.getArr(modifiedName);
						}
						final JSONObject modifications = talentChanges.getObj(talentName);
						for (String variantName : modifications.keySet()) {
							final int change = modifications.getInt(variantName);
							if ("Auswahl".equals(variantName)) {
								variantName = actual.getString("Auswahl");
							} else if ("Freitext".equals(variantName)) {
								variantName = actual.getString("Freitext");
							}
							JSONObject actualVariant = null;
							for (int i = 0; i < actualTalent.size(); ++i) {
								final JSONObject variant = actualTalent.getObj(i);
								if (talent.containsKey("Auswahl") && variantName.equals(variant.getString("Auswahl"))
										|| talent.containsKey("Freitext") && variantName.equals(variant.getString("Freitext"))) {
									actualVariant = variant;
									break;
								}
							}
							if (actualVariant == null) {
								actualVariant = new JSONObject(actualTalent);
								actualVariant.put(talent.containsKey("Auswahl") ? "Auswahl" : "Freitext", variantName);
								actualTalent.add(actualVariant);
							}
							actualVariant.put(targetValue,
									actualVariant.getIntOrDefault(targetValue, 0) - change * actual.getIntOrDefault("Stufe", 1));
							if (actualVariant.getInt(targetValue) == 0 && actualVariant.getStringOrDefault("AutomatischDurch", "").equals(effectorName)) {
								actualTalent.remove(actualVariant);
								if (actualTalent.size() == 0) {
									actualTalent.getParent().remove(actualTalent);
								}
							}
							actualVariant.notifyListeners(null);
						}
					} else {
						final JSONObject actualTalent;
						if ("Zauber".equals(groupName)) {
							final JSONObject actualSpell = actualGroup.getObj(modifiedName);
							if (actualSpell.size() == 0) {
								actualTalent = actualSpell.getObj("ÜNB");
							} else {
								actualTalent = actualSpell.getObj(actualSpell.keySet().iterator().next());
							}
						} else {
							actualTalent = actualGroup.getObj(modifiedName);
						}
						final int change = talentChanges.getInt(talentName);
						actualTalent.put(targetValue,
								actualTalent.getIntOrDefault(targetValue, 0) - change * actual.getIntOrDefault("Stufe", 1));
						if (actualTalent.getInt(targetValue) == 0 && actualTalent.getStringOrDefault("AutomatischDurch", "").equals(effectorName)) {
							actualTalent.getParent().remove(actualTalent);
						}
						actualTalent.notifyListeners(null);
					}
				}
			}
		}
	}
}
