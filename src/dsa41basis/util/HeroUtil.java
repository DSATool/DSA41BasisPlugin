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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class HeroUtil {
	public static final JSONObject infight = new JSONObject(null);
	public static final JSONObject brawling;
	public static final JSONObject wrestling;

	static {
		final JSONObject TP = new JSONObject(infight);
		TP.put("W6", 1);
		TP.put("Trefferpunkte", 0);
		TP.put("Ausdauerschaden", true);
		infight.put("Trefferpunkte", TP);
		final JSONObject TPKK = new JSONObject(infight);
		TPKK.put("Schwellenwert", 10);
		TPKK.put("Schadensschritte", 3);
		infight.put("Trefferpunkte/Körperkraft", TPKK);
		final JSONObject weaponModifiers = new JSONObject(infight);
		infight.put("Waffenmodifikatoren", weaponModifiers);
		final JSONArray distanceClasses = new JSONArray(infight);
		distanceClasses.add("H");
		infight.put("Distanzklassen", distanceClasses);
		infight.put("Typ", "Waffenlos");
		brawling = infight.clone(null);
		wrestling = infight.clone(null);
		infight.put("Zweihändig", true);
		final JSONArray weaponTypes = new JSONArray(infight);
		weaponTypes.add("Raufen");
		weaponTypes.add("Ringen");
		infight.put("Waffentypen", weaponTypes);
		infight.put("Name", "Waffenlos");
		final JSONArray weaponTypesBrawling = new JSONArray(brawling);
		weaponTypesBrawling.add("Raufen");
		brawling.put("Waffentypen", weaponTypesBrawling);
		brawling.put("Name", "Raufen");
		final JSONArray weaponTypesWresting = new JSONArray(wrestling);
		weaponTypesWresting.add("Ringen");
		wrestling.put("Waffentypen", weaponTypesWresting);
		wrestling.put("Name", "Ringen");
	}

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

	private static void addProfessionModifierString(final StringBuilder professionString, JSONObject profession, final JSONArray professionModifiers,
			final boolean female) {
		final String[] modifiers = professionModifiers.getStrings().toArray(new String[] {});
		if (female) {
			boolean first = true;
			int i = 0;
			for (; i < modifiers.length; ++i) {
				if (first) {
					first = false;
				} else {
					professionString.append(", ");
				}

				final String modifierName = modifiers[i];
				if (profession.getObj("Varianten").containsKey(modifierName)) {
					profession = profession.getObj("Varianten").getObj(modifierName);
					professionString.append(profession.getStringOrDefault("Weiblich", modifierName));
				} else {
					break;
				}
			}
			for (; i < modifiers.length; ++i) {
				final String modifierName = modifiers[i];
				JSONObject parent = profession.getObjOrDefault("Varianten", null);
				while (parent != null && parent.getParent() != null && !parent.containsKey(modifierName)) {
					parent = (JSONObject) parent.getParent().getParent();
				}
				if (parent != null) {
					professionString.append(parent.getObj("Varianten").getObj(modifierName).getStringOrDefault("Weiblich", modifierName));
				}
			}
		} else {
			professionString.append(String.join(", ", modifiers));
		}
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
						final Object modificationsData = talentChanges.getUnsafe(talentName);
						if (modificationsData instanceof final JSONObject modifications) {
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

								applyTalentEffect(actualVariant, actual, targetValue, effectorName, change);
							}
						} else {
							final JSONObject actualVariant = new JSONObject(actualTalent);
							actualVariant.put(talent.containsKey("Auswahl") ? "Auswahl" : "Freitext",
									talent.getString(talent.containsKey("Auswahl") ? "Auswahl" : "Freitext"));
							actualTalent.add(actualVariant);

							applyTalentEffect(actualVariant, actual, targetValue, effectorName, talentChanges.getInt(talentName));
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
						applyTalentEffect(actualTalent, actual, targetValue, effectorName, change);
					}
				}
			}
		}
	}

	private static void applyTalentEffect(final JSONObject actualTalent, final JSONObject actual, final String targetValue, final String effectorName,
			final int change) {
		if (!actualTalent.containsKey(targetValue) || !actualTalent.getBoolOrDefault("aktiviert", true)) {
			actualTalent.put("AutomatischDurch", effectorName);
		}
		actualTalent.put(targetValue, actualTalent.getIntOrDefault(targetValue, 0) + change * actual.getIntOrDefault("Stufe", 1));
		actualTalent.removeKey("aktiviert");
		actualTalent.notifyListeners(null);
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

		if (derivation == derivedValues.getObj("Astralenergie") && hero.getObj("Sonderfertigkeiten").containsKey("Gefäß der Sterne")) {
			value += getCurrentValue(attributes.getObj("CH"), false);
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

			final String groupName = findTalent(talentName)._2;
			if ("Zauber".equals(groupName))
				return new Tuple<>(null, hero.getObj("Zauber"));
			else if (groupName != null) return new Tuple<>(null, actualTalentGroups.getObj(groupName));
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

	public static void foreachInventoryItem(final boolean isExtraInventory, final Predicate<JSONObject> filter, final BiConsumer<JSONObject, Boolean> function,
			final JSONArray inventory) {
		DSAUtil.foreach(filter, item -> {
			function.accept(item, isExtraInventory);
		}, inventory);
	}

	public static void foreachInventoryItem(final JSONObject hero, final Predicate<JSONObject> filter, final BiConsumer<JSONObject, Boolean> function) {
		foreachInventoryItem(false, filter, function, hero.getObj("Besitz").getArr("Ausrüstung"));
		DSAUtil.foreach(inventory -> true, inventory -> {
			foreachInventoryItem(true, filter, function, inventory.getArr("Ausrüstung"));
		}, hero.getObj("Besitz").getArr("Inventare"));
		DSAUtil.foreach(animal -> true, animal -> {
			foreachInventoryItem(true, filter, function, animal.getArr("Ausrüstung"));
			DSAUtil.foreach(inventory -> true, inventory -> {
				foreachInventoryItem(true, filter, function, inventory.getArr("Ausrüstung"));
			}, animal.getArr("Inventare"));
		}, hero.getArr("Tiere"));
	}

	public static String getAntiElement(final String element) {
		return switch (element) {
			case "Eis" -> "Humus";
			case "Erz" -> "Luft";
			case "Feuer" -> "Wasser";
			case "Humus" -> "Eis";
			case "Luft" -> "Erz";
			case "Wasser" -> "Feuer";
			default -> null;
		};
	}

	public static Integer getAT(final JSONObject hero, JSONObject weapon, final String type, final boolean closeCombat, final boolean wrongHand,
			final boolean includeManualMods) {
		JSONObject secondaryWeapon = null;
		if (closeCombat) {
			final JSONObject baseWeapon = weapon;
			if (weapon != null && weapon.containsKey("Nahkampfwaffe")) {
				weapon = weapon.getObj("Nahkampfwaffe");
			}
			if (!weapon.getBoolOrDefault("Zweihändig", weapon.getBoolOrDefault("Zweihändig", false))) {
				secondaryWeapon = weapon.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false)) ? getMainWeapon(hero)
						: getSecondaryWeapon(hero);
			}
		}
		return getAT(hero, weapon, type, closeCombat, wrongHand, secondaryWeapon, includeManualMods);
	}

	public static Integer getAT(final JSONObject hero, final JSONObject weapon, final String type, final boolean closeCombat, final boolean wrongHand,
			final JSONObject secondaryWeapon, final boolean includeManualMods) {
		return getAT(hero, weapon, type, closeCombat, wrongHand, secondaryWeapon, getDefaultArmor(hero), includeManualMods);
	}

	public static Integer getAT(final JSONObject hero, JSONObject weapon, final String type, final boolean closeCombat, final boolean wrongHand,
			JSONObject secondaryWeapon, final JSONObject armorSet, final boolean includeManualMods) {
		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey(closeCombat ? "Nahkampfwaffe" : "Fernkampfwaffe")) {
			weapon = weapon.getObj(closeCombat ? "Nahkampfwaffe" : "Fernkampfwaffe");
		}

		final JSONObject baseValueDerivation = ResourceManager.getResource("data/Basiswerte").getObj(closeCombat ? "Attacke-Basis" : "Fernkampf-Basis");
		final int baseValue = deriveValue(baseValueDerivation, hero, hero.getObj("Basiswerte"), includeManualMods);

		final JSONObject talents = ResourceManager.getResource("data/Talente").getObj(closeCombat ? "Nahkampftalente" : "Fernkampftalente");
		final JSONObject talent = talents.getObjOrDefault(type, null);

		if (talent == null) return null;

		final JSONObject actualTalent = hero.getObj("Talente").getObj(closeCombat ? "Nahkampftalente" : "Fernkampftalente").getObjOrDefault(type, null);
		final int fromTalent = actualTalent != null ? actualTalent.getIntOrDefault("AT", 0) : 0;

		final JSONObject actualSkills = hero.getObj("Sonderfertigkeiten");

		final boolean hasSpecialisation = weapon != null
				&& HeroUtil.getSpecialisation(actualSkills.getArrOrDefault("Waffenspezialisierung", null), type,
						weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ"))) != null;
		int specialisation = hasSpecialisation ? !closeCombat || talent.getBoolOrDefault("NurAT", false) ? 2 : 1 : 0;
		if (Set.of("Raufen", "Ringen").contains(type)) {
			specialisation += getInfightSpecialisationCount(actualSkills, type);
		}

		final JSONObject weaponModifiers = weapon == null ? null : weapon.getObjOrDefault("Waffenmodifikatoren", baseWeapon.getObj("Waffenmodifikatoren"));
		final int weaponModifier = weaponModifiers != null ? weaponModifiers.getIntOrDefault("Attackemodifikator", 0) : 0;

		final JSONObject weaponMastery = weapon == null ? null : HeroUtil.getSpecialisation(actualSkills.getArrOrDefault("Waffenmeister", null), type,
				weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
		final int masteryAT = weaponMastery != null ? weaponMastery.getObj("Waffenmodifikatoren").getIntOrDefault("Attackemodifikator", 0) : 0;

		final int be = getEffectiveBE(hero, talent, getBE(hero, armorSet)) / (!closeCombat || talent.getBoolOrDefault("NurAT", false) ? 1 : 2);

		final int TPKKModifier = Math.min(getTPKKModifier(hero, weapon, baseWeapon), 0);

		int secondHandModifier = 0;

		if (weapon.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false))) {
			if (!hero.getObj("Vorteile").containsKey("Beidhändig") && !actualSkills.containsKey("Beidhändiger Kampf II")) {
				if (actualSkills.containsKey("Beidhändiger Kampf I")) {
					secondHandModifier = -3;
				} else if (actualSkills.containsKey("Linkand")) {
					secondHandModifier = -6;
				} else {
					secondHandModifier = -9;
				}
			}
		}

		int secondWeaponModifier = 0;

		if (secondaryWeapon != null) {
			JSONObject secondaryBase = null;
			boolean isCloseCombatWeapon = false;
			final JSONValue parent = secondaryWeapon.getParent();
			if (parent instanceof final JSONObject parentObject) {
				if ("Nahkampfwaffe".equals(parentObject.keyOf(secondaryWeapon))) {
					secondaryBase = parentObject;
					isCloseCombatWeapon = true;
				} else if ("Schild".equals(parentObject.keyOf(secondaryWeapon)) || "Parierwaffe".equals(parentObject.keyOf(secondaryWeapon))) {
					secondaryBase = parentObject;
				}
			}
			if (secondaryBase == null) {
				secondaryBase = secondaryWeapon;
				final JSONArray categories = secondaryWeapon.getArr("Kategorien");
				if (secondaryWeapon.containsKey("Nahkampfwaffe") && !categories.contains("Schild") && !categories.contains("Parierwaffe")) {
					secondaryWeapon = secondaryWeapon.getObj("Nahkampfwaffe");
					isCloseCombatWeapon = true;
				} else if (secondaryWeapon.containsKey("Schild") && !categories.contains("Nahkampfwaffe") && !categories.contains("Parierwaffe")) {
					secondaryWeapon = secondaryWeapon.getObj("Schild");
				} else if (secondaryWeapon.containsKey("Parierwaffe") && !categories.contains("Nahkampfwaffe") && !categories.contains("Schild")) {
					secondaryWeapon = secondaryWeapon.getObj("Parierwaffe");
				} else {
					if (categories.contains("Nahkampfwaffe") && !secondaryWeapon.containsKey("Nahkampfwaffe")) {
						isCloseCombatWeapon = true;
					}
				}
			}
			if (isCloseCombatWeapon) {
				final String mainType = weapon.getStringOrDefault("Typ", baseWeapon.getStringOrDefault("Typ", ""));
				if (!mainType.equals(secondaryWeapon.getStringOrDefault("Typ", secondaryBase.getString("Typ")))) {
					if (type.equals(secondaryWeapon.getStringOrDefault("Waffentyp:Primär", secondaryBase.getString("Waffentyp:Primär")))) {
						secondWeaponModifier = -1;
					} else {
						secondWeaponModifier = -2;
					}
				}
			} else {
				secondWeaponModifier = secondaryWeapon.getObjOrDefault("Waffenmodifikatoren", secondaryBase.getObj("Waffenmodifikatoren"))
						.getIntOrDefault("Attackemodifikator", 0);
			}
		}

		return baseValue + fromTalent + weaponModifier + specialisation + masteryAT - be + TPKKModifier + secondHandModifier + secondWeaponModifier;
	}

	public static int getBE(final JSONObject hero) {
		return getBE(hero, getDefaultArmor(hero));
	}

	public static int getBE(final JSONObject hero, final JSONObject armorSet) {
		final double[] BE = { getBERaw(hero, armorSet) };
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		if (skills.containsKey("Rüstungsgewöhnung III")) {
			BE[0] -= 2;
		} else if (skills.containsKey("Rüstungsgewöhnung II") || skills.containsKey("Rüstungsgewöhnung I") && hasReducedBE(hero, armorSet)) {
			BE[0] -= 1;
		}
		return (int) Math.max(0, Math.round(BE[0]));
	}

	public static double getBERaw(final JSONObject hero) {
		return getBERaw(hero, getDefaultArmor(hero));
	}

	public static double getBERaw(final JSONObject hero, final JSONObject armorSet) {
		final String armorSetting = Settings.getSettingStringOrDefault("Zonenrüstung", "Kampf", "Rüstungsart");
		final String armorSetName = armorSet != null ? armorSet.getStringOrDefault("Name", null) : null;

		final double[] BE = { 0 };

		foreachInventoryItem(hero, item -> item.containsKey("Kategorien") && item.getArr("Kategorien").contains("Rüstung"), (item, extraInventory) -> {
			JSONObject armor = item;
			if (item.containsKey("Rüstung")) {
				armor = item.getObj("Rüstung");
			}
			final JSONArray armorSets = armor.getArrOrDefault("Rüstungskombinationen", item.getArrOrDefault("Rüstungskombinationen", new JSONArray(null)));
			if (armorSetName == null && armorSets.size() == 0 || armorSetName != null && armorSets.contains(armorSetName)) {
				if ("Gesamtrüstung".equals(armorSetting)) {
					BE[0] += armor.getIntOrDefault("Gesamtbehinderung", item.getIntOrDefault("Gesamtbehinderung", 0));
				} else {
					BE[0] += armor.getDoubleOrDefault("Behinderung", item.getDoubleOrDefault("Behinderung",
							armor.getIntOrDefault("Gesamtbehinderung", item.getIntOrDefault("Gesamtbehinderung", 0)).doubleValue()));
				}
			}
		});

		return BE[0];
	}

	public static String getChallengeValuesString(final JSONObject hero, final JSONArray challenge, final boolean includeValues) {
		if (challenge == null || challenge.size() < 3) return "—";

		final StringBuilder attributesString = new StringBuilder(8);

		if (hero != null && includeValues) {
			final JSONObject attributes = hero.getObj("Eigenschaften");
			attributesString.append(Integer.toString(getCurrentValue(attributes.getObj(challenge.getString(0)), false)));
			attributesString.append('/');
			attributesString.append(Integer.toString(getCurrentValue(attributes.getObj(challenge.getString(1)), false)));
			attributesString.append('/');
			attributesString.append(Integer.toString(getCurrentValue(attributes.getObj(challenge.getString(2)), false)));
		} else {
			attributesString.append("      /      /      ");
		}

		for (int i = 3; i < challenge.size(); ++i) {
			final String mod = challenge.getUnsafe(i).toString();
			if ('-' != mod.charAt(0)) {
				attributesString.append('+');
			}
			attributesString.append(mod);
		}

		return attributesString.toString();
	}

	public static Set<String> getChoices(final JSONObject hero, final String choice, final String other) {
		final Set<String> choices = new LinkedHashSet<>();
		if (choice == null) return choices;

		final JSONObject allChoiceData = ResourceManager.getResource("data/Auswahl");
		if (allChoiceData.containsKey(choice)) {
			final JSONObject choiceData = allChoiceData.getObj(choice);
			if (choiceData.containsKey("Zusätzlich:Start")) {
				choices.addAll(choiceData.getArr("Zusätzlich:Start").getStrings());
			}
			if (choiceData.containsKey("Pfade")) {
				final JSONArray paths = choiceData.getArr("Pfade");
				paths: for (int i = 0; i < paths.size(); ++i) {
					final JSONArray path = paths.getArr(i);
					List<JSONObject> data = new ArrayList<>();
					data.add(ResourceManager.getResource("data/" + path.getString(0)));
					for (int j = 1; j < path.size(); ++j) {
						final String pathElement = path.getString(j);
						if ("*".equals(pathElement)) {
							data = data.stream().flatMap(element -> element.keySet().stream().map(key -> element.getObj(key))).collect(Collectors.toList());
						} else if (pathElement.startsWith("/")) {
							choices.addAll(data.stream().map(element -> element.getString(pathElement.substring(1))).collect(Collectors.toList()));
							continue paths;
						} else {
							data.replaceAll(element -> element.getObj(pathElement));
						}
					}
					for (final JSONObject element : data) {
						choices.addAll(element.keySet());
					}
				}
			}
			if (choiceData.containsKey("Zusätzlich:Ende")) {
				choices.addAll(choiceData.getArr("Zusätzlich:Ende").getStrings());
			}
			if (choiceData.containsKey("Entfernen")) {
				choices.removeAll(choiceData.getArr("Entfernen").getStrings());
			}
		} else {
			switch (choice) {
				case "Talent":
					final JSONObject talents = ResourceManager.getResource("data/Talente");
					for (final String talentgroup : talents.keySet()) {
						if (!"Meta-Talente".equals(talentgroup)) {
							choices.addAll(talents.getObj(talentgroup).keySet());
						}
					}
					break;
				case "Übernatürliche Begabung":
					final JSONObject spells = ResourceManager.getResource("data/Zauber");
					for (final String spellName : spells.keySet()) {
						if (spells.getObj(spellName).getObj("Repräsentationen").containsKey("ÜNB")) {
							choices.add(spellName);
						}
					}
					break;
				case "Körperliche Eigenschaft":
					final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
					for (final String attribute : attributes.keySet()) {
						if (attributes.getObj(attribute).getStringOrDefault("Eigenschaft", "geistig").equals("körperlich")) {
							choices.add(attributes.getObj(attribute).getString("Name"));
						}
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
				case "Profession:Variante":
					if (hero != null) {
						final JSONObject variants = ResourceManager.getResource("data/Professionen").getObj(hero.getObj("Biografie").getString("Profession"))
								.getObj("Varianten");
						choices.addAll(getVariantStrings(variants));
					}
					break;
				case "Profession:Variante:BGB":
					if (hero != null && other != null) {
						final JSONObject variants = ResourceManager.getResource("data/Professionen").getObj(other).getObj("Varianten");
						choices.addAll(getVariantStrings(variants));
					}
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
					final JSONObject choiceTalent = HeroUtil.findTalent(other)._1;
					final JSONArray specializations = choiceTalent != null ? choiceTalent.getArrOrDefault("Spezialisierungen", null) : null;
					if (specializations != null) {
						choices.addAll(specializations.getStrings());
					} else {
						final JSONObject weaponItems = ResourceManager.getResource("data/Ausruestung");
						for (final String itemName : weaponItems.keySet()) {
							final JSONObject item = weaponItems.getObj(itemName);
							if (item.getArr("Waffentypen").contains(other) && !item.getBoolOrDefault("Improvisiert", false)) {
								choices.add(item.getString("Typ"));
							}
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
		}

		return choices;
	}

	public static int getCurrentValue(final JSONObject actual, final boolean includeManualMod) {
		return actual.getIntOrDefault("Wert", 0) + (includeManualMod ? actual.getIntOrDefault("Modifikator:Manuell", 0) : 0);
	}

	public static JSONObject getDefaultArmor(final JSONObject hero) {
		final JSONArray armorSets = hero.getObj("Kampf").getArrOrDefault("Rüstungskombinationen", null);
		if (armorSets != null) {
			for (int i = 0; i < armorSets.size(); ++i) {
				if (armorSets.getObj(i).getBoolOrDefault("Standardrüstung", false)) return armorSets.getObj(i);
			}
		}
		return null;
	}

	public static Integer getDefensiveWeaponAT(final JSONObject hero, final JSONObject weapon, final JSONObject mainWeapon, final boolean includeManualMods) {
		return getDefensiveWeaponAT(hero, weapon, mainWeapon, getDefaultArmor(hero), includeManualMods);
	}

	public static Integer getDefensiveWeaponAT(final JSONObject hero, JSONObject weapon, final JSONObject mainWeapon, final JSONObject armorSet,
			final boolean includeManualMods) {

		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		if (!skills.containsKey("Tod von Links")) return null;

		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Parierwaffe")) {
			weapon = weapon.getObj("Parierwaffe");
		}

		final JSONArray types = weapon.getArrOrDefault("Waffentypen", baseWeapon.getArrOrDefault("Waffentypen", new JSONArray(null)));
		if (types.size() == 0) return null;
		final String type = types.getString(0);
		final JSONObject actualTalent = hero.getObj("Talente").getObj("Nahkampftalente").getObjOrDefault(type, null);
		final int be = getEffectiveBE(hero, actualTalent, getBE(hero, armorSet)) / 2;

		if (be > 4) return null;

		return getAT(hero, weapon, type, true, true, mainWeapon, armorSet, includeManualMods);
	}

	public static int getDefensiveWeaponPA(final JSONObject hero, final JSONObject defensiveWeapon, final boolean includeManualMods) {
		return getDefensiveWeaponPA(hero, defensiveWeapon, getMainWeapon(hero), includeManualMods);
	}

	public static Integer getDefensiveWeaponPA(final JSONObject hero, JSONObject defensiveWeapon, JSONObject mainWeapon, final boolean includeManualMods) {
		final JSONObject baseDefensiveWeapon = defensiveWeapon;
		if (defensiveWeapon != null && defensiveWeapon.containsKey("Parierwaffe")) {
			defensiveWeapon = defensiveWeapon.getObj("Parierwaffe");
		}

		final JSONObject weaponModifiers = defensiveWeapon == null ? null
				: defensiveWeapon.getObjOrDefault("Waffenmodifikatoren", baseDefensiveWeapon.getObj("Waffenmodifikatoren"));
		int PA = weaponModifiers == null ? 0 : weaponModifiers.getIntOrDefault("Parademodifikator", 0);

		if (mainWeapon != null) {
			final JSONObject baseWeapon = mainWeapon;
			if (mainWeapon != null && mainWeapon.containsKey("Nahkampfwaffe")) {
				mainWeapon = mainWeapon.getObj("Nahkampfwaffe");
			}
			String type = mainWeapon.getStringOrDefault("Waffentyp:Primär", baseWeapon.getStringOrDefault("Waffentyp:Primär", null));
			if (type == null) {
				type = mainWeapon.getArrOrDefault("Waffentypen", baseWeapon.getArr("Waffentypen")).getString(0);
			}
			final Integer mainPA = getPA(hero, mainWeapon, type, false, null, includeManualMods);
			if (mainPA == null) return null;
			PA += mainPA;
		}

		final JSONObject skills = hero.getObj("Sonderfertigkeiten");

		if (skills.containsKey("Linkhand")) {
			PA -= 4;
			if (skills.containsKey("Parierwaffen I")) {
				PA += 3;
			}
			if (skills.containsKey("Parierwaffen II")) {
				PA += 3;
			}

			return PA;
		} else
			return Integer.MIN_VALUE;
	}

	public static int getDistance(final JSONObject hero, JSONObject weapon, final String type, final String distance) {
		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Fernkampfwaffe")) {
			weapon = weapon.getObj("Fernkampfwaffe");
		}
		final JSONObject weaponMastery = hero == null ? null : HeroUtil.getSpecialisation(
				hero.getObj("Sonderfertigkeiten").getArrOrDefault("Waffenmeister", null), type, weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));

		final JSONObject weaponDistances = weapon.getObjOrDefault("Reichweiten", baseWeapon.getObj("Reichweiten"));

		final int dist = weaponDistances.getIntOrDefault(distance, Integer.MIN_VALUE);
		if (dist == Integer.MIN_VALUE) return Integer.MIN_VALUE;
		return (int) Math.round(dist * (1 + (weaponMastery != null ? weaponMastery.getIntOrDefault("Reichweite", 0) * 0.1 : 0)));
	}

	private static int getEffectiveBE(final JSONObject hero, final JSONObject talent, final int BE) {
		return talent.getIntOrDefault("BEMultiplikativ", 0) * BE + Math.max(BE + talent.getIntOrDefault("BEAdditiv", Integer.MIN_VALUE), 0);
	}

	public static int getEvasion(final JSONObject hero) {
		int result = 0;

		final int PABase = HeroUtil.deriveValue(ResourceManager.getResource("data/Basiswerte").getObj("Parade-Basis"), hero,
				hero.getObj("Basiswerte").getObj("Parade-Basis"), true);
		result = PABase;

		final int BE = HeroUtil.getBE(hero);
		result -= BE;

		final JSONObject specialSkills = hero.getObj("Sonderfertigkeiten");
		if (specialSkills.containsKey("Ausweichen I")) {
			result += 3;
		}
		if (specialSkills.containsKey("Ausweichen II")) {
			result += 3;
		}
		if (specialSkills.containsKey("Ausweichen III")) {
			result += 3;
		}

		final JSONObject acrobaticsTalent = hero.getObj("Talente").getObj("Körperliche Talente").getObjOrDefault("Akrobatik", null);
		if (acrobaticsTalent != null) {
			final int acrobaticsValue = acrobaticsTalent.getIntOrDefault("TaW", 0);
			if (acrobaticsValue > 11) {
				result += (acrobaticsValue - 9) / 3;
			}
		}

		if (hero.getObj("Vorteile").containsKey("Flink")) {
			result += hero.getObj("Vorteile").getObj("Flink").getIntOrDefault("Stufe", 1);
		}

		if (hero.getObj("Nachteile").containsKey("Behäbig")) {
			result -= 1;
		}

		return result;
	}

	private static int getInfightSpecialisationCount(final JSONObject actualSkills, final String talent) {
		final JSONObject skills = ResourceManager.getResource("data/Sonderfertigkeiten").getObj("Waffenlose Kampftechniken");
		int result = 0;

		for (final String skillName : skills.keySet()) {
			if (actualSkills.containsKey(skillName)) {
				final String specialisationTalent = skills.getObj(skillName).getString("Spezialisierung");
				if (talent.equals(specialisationTalent)
						|| "Auswahl".equals(specialisationTalent) && talent.equals(actualSkills.getObj(skillName).getString("Auswahl"))) {
					result += 1;
				}
			}
		}

		return Math.min(result, 2);
	}

	public static String getItemNotes(final JSONObject item, final JSONObject baseItem) {
		String defaultNotes = " ";
		if (item.containsKey("Bannschwert") && item.getObj("Bannschwert").getObj("Rituale").containsKey("Bannschwert")) {
			defaultNotes = "Bannschwert";
			if (item.getObj("Bannschwert").getObj("Rituale").containsKey("Apport")) {
				defaultNotes += ", Apport";
			}
		}

		return item.getStringOrDefault("Anmerkungen", baseItem.getStringOrDefault("Anmerkungen", defaultNotes));
	}

	public static int getLoadTime(final JSONObject hero, JSONObject weapon, final String type) {
		final JSONObject baseWeapon = weapon;
		if (weapon.containsKey("Fernkampfwaffe")) {
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
						: HeroUtil.getSpecialisation(hero.getObj("Sonderfertigkeiten").getArrOrDefault("Waffenmeister", null), type,
								weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
				if (weaponMastery != null && weaponMastery.getBoolOrDefault("Ladezeit", false)) {
					loadTime *= 0.5;
				}
			}
		}

		return (int) Math.round(loadTime);
	}

	public static JSONObject getMainWeapon(final JSONObject hero) {
		final JSONObject[] mainWeapon = { null };

		foreachInventoryItem(hero, item -> mainWeapon[0] == null && item.containsKey("Kategorien") && item.getArr("Kategorien").contains("Nahkampfwaffe"),
				(item, extraInventory) -> {
					final JSONObject baseWeapon = item;
					if (item != null && item.containsKey("Nahkampfwaffe")) {
						item = item.getObj("Nahkampfwaffe");
					}

					if (item.getBoolOrDefault("Hauptwaffe", baseWeapon.getBoolOrDefault("Hauptwaffe", false))
							&& !item.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false))) {
						mainWeapon[0] = baseWeapon;
					}
				});

		return mainWeapon[0];
	}

	public static Integer getPA(final JSONObject hero, JSONObject weapon, final String type, final boolean wrongHand, final boolean includeManualMods) {
		JSONObject secondaryWeapon = null;
		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Nahkampfwaffe")) {
			weapon = weapon.getObj("Nahkampfwaffe");
		}
		if (!weapon.getBoolOrDefault("Zweihändig", weapon.getBoolOrDefault("Zweihändig", false))) {
			secondaryWeapon = weapon.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false)) ? getMainWeapon(hero)
					: getSecondaryWeapon(hero);
		}
		return getPA(hero, weapon, type, wrongHand, secondaryWeapon, includeManualMods);
	}

	public static Integer getPA(final JSONObject hero, final JSONObject weapon, final String type, final boolean wrongHand, final JSONObject secondaryWeapon,
			final boolean includeManualMods) {
		return getPA(hero, weapon, type, wrongHand, secondaryWeapon, getDefaultArmor(hero), includeManualMods);
	}

	public static Integer getPA(final JSONObject hero, JSONObject weapon, final String type, final boolean wrongHand, JSONObject secondaryWeapon,
			final JSONObject armorSet, final boolean includeManualMods) {
		final JSONObject talent = ResourceManager.getResource("data/Talente").getObj("Nahkampftalente").getObjOrDefault(type, null);

		if (talent == null) return null;

		final boolean ATonly = talent.getBoolOrDefault("NurAT", false);
		if (ATonly) return null;

		final JSONObject baseWeapon = weapon;
		if (weapon != null && weapon.containsKey("Nahkampfwaffe")) {
			weapon = weapon.getObj("Nahkampfwaffe");
		}

		int secondWeaponModifier = 0;

		if (secondaryWeapon != null) {
			JSONObject secondaryBase = null;
			boolean isCloseCombatWeapon = false;
			boolean isShield = false;
			boolean isDefensiveWeapon = false;

			final JSONValue parent = secondaryWeapon.getParent();
			if (parent instanceof final JSONObject parentObject) {
				if ("Nahkampfwaffe".equals(parentObject.keyOf(secondaryWeapon))) {
					secondaryBase = parentObject;
					isCloseCombatWeapon = true;
				} else if ("Schild".equals(parentObject.keyOf(secondaryWeapon))) {
					secondaryBase = parentObject;
					isShield = true;
				} else if ("Parierwaffe".equals(parentObject.keyOf(secondaryWeapon))) {
					secondaryBase = parentObject;
					isDefensiveWeapon = true;
				}
			}
			if (secondaryBase == null) {
				secondaryBase = secondaryWeapon;
				final JSONArray categories = secondaryWeapon.getArr("Kategorien");
				if (secondaryWeapon.containsKey("Nahkampfwaffe") && !categories.contains("Schild") && !categories.contains("Parierwaffe")) {
					secondaryWeapon = secondaryWeapon.getObj("Nahkampfwaffe");
					isCloseCombatWeapon = true;
				} else if (secondaryWeapon.containsKey("Schild") && !categories.contains("Nahkampfwaffe") && !categories.contains("Parierwaffe")) {
					secondaryWeapon = secondaryWeapon.getObj("Schild");
					isShield = true;
				} else if (secondaryWeapon.containsKey("Parierwaffe") && !categories.contains("Nahkampfwaffe") && !categories.contains("Schild")) {
					secondaryWeapon = secondaryWeapon.getObj("Parierwaffe");
					isDefensiveWeapon = true;
				} else {
					if (categories.contains("Nahkampfwaffe") && !secondaryWeapon.containsKey("Nahkampfwaffe")) {
						isCloseCombatWeapon = true;
					} else if (categories.contains("Schild") && !secondaryWeapon.containsKey("Schild")) {
						isShield = true;
					} else if (categories.contains("Parierwaffe") && !secondaryWeapon.containsKey("Parierwaffe")) {
						isDefensiveWeapon = true;
					}
				}
			}
			if (isCloseCombatWeapon) {
				final String mainType = weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ"));
				if (!mainType.equals(secondaryWeapon.getStringOrDefault("Typ", secondaryBase.getString("Typ")))) {
					if (type.equals(secondaryWeapon.getStringOrDefault("Waffentyp:Primär", secondaryBase.getString("Waffentyp:Primär")))) {
						secondWeaponModifier = -1;
					} else {
						secondWeaponModifier = -2;
					}
				}
			} else if (isShield)
				return getShieldPA(hero, secondaryBase, baseWeapon, includeManualMods);
			else if (isDefensiveWeapon) {
				secondWeaponModifier = getDefensiveWeaponPA(hero, secondaryBase, null, includeManualMods);
			}
		}

		final JSONObject baseValueDerivation = ResourceManager.getResource("data/Basiswerte").getObj("Parade-Basis");
		final int baseValue = deriveValue(baseValueDerivation, hero, hero.getObj("Basiswerte"), includeManualMods);

		final JSONObject actualTalent = hero.getObj("Talente").getObj("Nahkampftalente").getObjOrDefault(type, null);
		final int fromTalent = actualTalent != null ? actualTalent.getIntOrDefault("PA", 0) : 0;

		final JSONObject actualSkills = hero.getObj("Sonderfertigkeiten");

		final boolean hasSpecialisation = weapon != null
				&& HeroUtil.getSpecialisation(actualSkills.getArrOrDefault("Waffenspezialisierung", null), type,
						weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ"))) != null;
		int specialisation = hasSpecialisation ? 1 : 0;
		if (Set.of("Raufen", "Ringen").contains(type)) {
			specialisation += getInfightSpecialisationCount(actualSkills, type);
		}

		final JSONObject weaponModifiers = weapon == null ? null : weapon.getObjOrDefault("Waffenmodifikatoren", baseWeapon.getObj("Waffenmodifikatoren"));
		final int weaponModifier = weaponModifiers == null ? 0 : weaponModifiers.getIntOrDefault("Parademodifikator", 0);

		final JSONObject weaponMastery = weapon == null ? null : HeroUtil.getSpecialisation(actualSkills.getArrOrDefault("Waffenmeister", null), type,
				weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
		final int masteryPA = weaponMastery != null ? weaponMastery.getObj("Waffenmodifikatoren").getIntOrDefault("Parademodifikator", 0) : 0;

		final int be = (getEffectiveBE(hero, talent, getBE(hero, armorSet)) + 1) / 2;

		final int TPKKModifier = Math.min(getTPKKModifier(hero, weapon, baseWeapon), 0);

		int secondHandModifier = 0;

		if (weapon.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false))) {
			if (!hero.getObj("Vorteile").containsKey("Beidhändig") && !actualSkills.containsKey("Beidhändiger Kampf II")) {
				if (actualSkills.containsKey("Beidhändiger Kampf I")) {
					secondHandModifier = -3;
				} else if (actualSkills.containsKey("Linkand")) {
					secondHandModifier = -6;
				} else {
					secondHandModifier = -9;
				}
			}
		}

		return baseValue + fromTalent + weaponModifier + specialisation + masteryPA - be + TPKKModifier + secondHandModifier + secondWeaponModifier;
	}

	public static String getProfessionString(final JSONObject hero, final JSONObject bio, final JSONObject professions, final boolean withVeteranBGB) {
		final boolean female = "weiblich".equals(bio.getString("Geschlecht"));
		final StringBuilder professionString = new StringBuilder();
		final String professionName = bio.getStringOrDefault("Profession", "");
		JSONObject profession = null;
		if (female) {
			profession = professions.getObj(professionName);
			professionString.append(profession.getStringOrDefault("Weiblich", professionName));
		} else {
			professionString.append(professionName);
		}
		if (bio.containsKey("Profession:Modifikation")) {
			final JSONArray professionModifiers = bio.getArr("Profession:Modifikation");
			professionString.append(" (");
			HeroUtil.addProfessionModifierString(professionString, profession, professionModifiers, female);
			professionString.append(")");
		}

		if (withVeteranBGB) {
			final String veteranBGBString = HeroUtil.getVeteranBGBString(hero, bio, professions);
			if (!veteranBGBString.isEmpty()) {
				professionString.append(' ').append(veteranBGBString);
			}
		}

		return professionString.toString();
	}

	public static JSONObject getSecondaryWeapon(final JSONObject hero) {
		final JSONObject[] secondaryWeapon = { null };

		foreachInventoryItem(hero, item -> secondaryWeapon[0] == null && item.containsKey("Kategorien"),
				(item, extraInventory) -> {
					if (item.getArr("Kategorien").contains("Nahkampfwaffe")) {
						final JSONObject baseWeapon = item;
						if (item != null && item.containsKey("Nahkampfwaffe")) {
							item = item.getObj("Nahkampfwaffe");
						}

						if (item.getBoolOrDefault("Hauptwaffe", baseWeapon.getBoolOrDefault("Hauptwaffe", false))
								&& item.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false))) {
							secondaryWeapon[0] = item;
						}
					}
					if (item.getArr("Kategorien").contains("Schild")) {
						final JSONObject baseWeapon = item;
						if (item != null && item.containsKey("Schild")) {
							item = item.getObj("Schild");
						}

						if (item.getBoolOrDefault("Seitenwaffe", baseWeapon.getBoolOrDefault("Seitenwaffe", false))) {
							secondaryWeapon[0] = item;
						}
					}
					if (item.getArr("Kategorien").contains("Parierwaffe")) {
						final JSONObject baseWeapon = item;
						if (item != null && item.containsKey("Parierwaffe")) {
							item = item.getObj("Parierwaffe");
						}

						if (item.getBoolOrDefault("Seitenwaffe", baseWeapon.getBoolOrDefault("Seitenwaffe", false))) {
							secondaryWeapon[0] = item;
						}
					}
				});

		return secondaryWeapon[0];
	}

	public static Integer getShieldAT(final JSONObject hero, final JSONObject shield, final boolean includeManualMods) {
		return getShieldAT(hero, shield, getDefaultArmor(hero), includeManualMods);
	}

	public static Integer getShieldAT(final JSONObject hero, JSONObject shield, final JSONObject armorSet, final boolean includeManualMods) {

		final JSONObject skills = hero.getObj("Sonderfertigkeiten");

		if (!skills.containsKey("Schildkampf I")) return null;

		final JSONObject baseValueDerivation = ResourceManager.getResource("data/Basiswerte").getObj("Attacke-Basis");
		final int baseValue = deriveValue(baseValueDerivation, hero, hero.getObj("Basiswerte"), includeManualMods);

		final JSONObject talent = ResourceManager.getResource("data/Talente").getObj("Nahkampftalente").getObjOrDefault("Raufen", null);
		if (talent == null) return null;

		final JSONObject actualTalent = hero.getObj("Talente").getObj("Nahkampftalente").getObjOrDefault("Raufen", null);
		final int fromTalent = actualTalent != null ? actualTalent.getIntOrDefault("AT", 0) : 0;

		final JSONObject baseShield = shield;
		if (shield != null && shield.containsKey("Schild")) {
			shield = shield.getObj("Schild");
		}

		final JSONObject weaponModifiers = shield == null ? null : shield.getObjOrDefault("Waffenmodifikatoren", baseShield.getObj("Waffenmodifikatoren"));
		final int weaponModifier = weaponModifiers != null ? weaponModifiers.getIntOrDefault("Attackemodifikator", 0) : 0;

		final int be = getEffectiveBE(hero, talent, getBE(hero, armorSet)) / 2;

		final int TPKKModifier = Math.min(getTPKKModifierRaw(hero, 13, 3), 0);

		final int shieldModifier = skills.containsKey("Schildkampf II") || skills.containsKey("Knaufschlag") || skills.containsKey("Schmutzige Tricks") ? 0
				: -3;

		return baseValue + fromTalent + weaponModifier - be + TPKKModifier - shieldModifier;
	}

	public static int getShieldPA(final JSONObject hero, final JSONObject shield, final boolean includeManualMods) {
		return getShieldPA(hero, shield, getMainWeapon(hero), includeManualMods);
	}

	public static int getShieldPA(final JSONObject hero, JSONObject shield, JSONObject mainWeapon, final boolean includeManualMods) {

		final JSONObject baseValueDerivation = ResourceManager.getResource("data/Basiswerte").getObj("Parade-Basis");
		final int baseValue = deriveValue(baseValueDerivation, hero, hero.getObj("Basiswerte"), includeManualMods);

		final JSONObject baseShield = shield;
		if (shield != null && shield.containsKey("Schild")) {
			shield = shield.getObj("Schild");
		}

		final JSONObject weaponModifiers = shield == null ? null : shield.getObjOrDefault("Waffenmodifikatoren", baseShield.getObj("Waffenmodifikatoren"));
		final int weaponModifier = weaponModifiers == null ? 0 : weaponModifiers.getIntOrDefault("Parademodifikator", 0);

		int PA = baseValue + weaponModifier;

		final JSONObject skills = hero.getObj("Sonderfertigkeiten");

		if (skills.containsKey("Linkhand")) {
			PA += 1;
		}
		if (skills.containsKey("Schildkampf I")) {
			PA += 2;
		}
		if (skills.containsKey("Schildkampf II")) {
			PA += 2;
		}

		if (mainWeapon != null) {
			final JSONObject baseWeapon = mainWeapon;
			if (mainWeapon != null && mainWeapon.containsKey("Nahkampfwaffe")) {
				mainWeapon = mainWeapon.getObj("Nahkampfwaffe");
			}
			String type = mainWeapon.getStringOrDefault("Waffentyp:Primär", baseWeapon.getStringOrDefault("Waffentyp:Primär", null));
			if (type == null) {
				type = mainWeapon.getArrOrDefault("Waffentypen", baseWeapon.getArr("Waffentypen")).getString(0);
			}
			final Integer mainWeaponPA = getPA(hero, mainWeapon, type, false, null, includeManualMods);
			if (mainWeaponPA != null) {
				if (mainWeaponPA >= 21) {
					PA += 3;
				} else if (mainWeaponPA >= 18) {
					PA += 2;
				} else if (mainWeaponPA >= 15) {
					PA += 1;
				}
			}
		}

		return PA;
	}

	public static String getShieldTPString(final JSONObject hero, JSONObject shield) {
		final JSONObject baseShield = shield;
		if (shield != null && shield.containsKey("Schild")) {
			shield = shield.getObj("Schild");
		}

		return getTPString(shield, baseShield, "1W6+1(A)", getTPKKModifierRaw(hero, 13, 3));
	}

	public static JSONObject getSpecialisation(final JSONArray specialisations, final String talent, final String specialisation) {
		if (specialisations == null || talent == null || specialisation == null) return null;
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

		final JSONArray actualRepresentations = skills.getArrOrDefault("Repräsentation", new JSONArray(null));

		boolean hasGuildMagic = false;
		boolean hasCharlatan = false;
		if ("ÜNB".equals(representation)) {
			complexity = 6;
		} else {
			boolean hasRepresentation = false;
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
				} else if (hasCharlatan && "Mag".equals(representation)) {
					++complexity;
				} else if ("Sch".equals(representation) && !hasCharlatan) {
					complexity += 3;
				} else {
					complexity += 2;
				}
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

		boolean hasDifferentRepBonus = false;

		final JSONObject actualSpell = hero.getObj("Zauber").getObjOrDefault(spellName, null);
		if (actualSpell != null) {
			final JSONObject actualRepresentation = actualSpell.getObj(representation);
			if (actualRepresentation != null && actualRepresentation.getBoolOrDefault("Hauszauber", false)) {
				--complexity;
			}
			for (final String currentRep : actualSpell.keySet()) {
				if (currentRep.equals(representation)) {
					continue;
				}
				if (!"Temporär".equals(currentRep) && actualSpell.getObj(currentRep).getIntOrDefault("ZfW", 0) >= targetZfW) {
					--complexity;
					hasDifferentRepBonus = true;
					break;
				}
			}
		}

		if ((spell.containsKey("Verwandte Sprüche") || baseSpell.containsKey("Verwandte Sprüche")) && !hasDifferentRepBonus && !isAntiElement) {
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
			} else if (actualTalent instanceof final JSONArray arr) {
				for (int i = 0; i < actualTalent.size(); ++i) {
					if (arr.getObj(i).getBoolOrDefault("Leittalent", false)) {
						--complexity;
						break;
					}
				}
			} else if (actualTalent instanceof final JSONObject obj && obj.getBoolOrDefault("Leittalent", false)) {
				--complexity;
			}
		}

		return complexity;
	}

	private static int getTPKKModifier(final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon) {
		if (hero == null || !(weapon.containsKey("Trefferpunkte/Körperkraft") || baseWeapon.containsKey("Trefferpunkte/Körperkraft")))
			return 0;

		final JSONObject TPKKValues = weapon.getObjOrDefault("Trefferpunkte/Körperkraft", baseWeapon.getObj("Trefferpunkte/Körperkraft"));
		final int threshold = TPKKValues.getIntOrDefault("Schwellenwert", Integer.MIN_VALUE);
		final int steps = TPKKValues.getIntOrDefault("Schadensschritte", Integer.MIN_VALUE);
		if (steps != Integer.MIN_VALUE && steps != 0) {
			int modifier = getTPKKModifierRaw(hero, threshold, steps);

			if (weapon.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false))) {
				if (!hero.getObj("Vorteile").containsKey("Beidhändig") && !hero.getObj("Sonderfertigkeiten").containsKey("Beidhändiger Kampf I")) {
					modifier = Math.min(modifier, 0);
				}
			}

			return modifier;
		} else
			return 0;
	}

	public static int getTPKKModifierRaw(final JSONObject hero, final int threshold, final int steps) {
		final int KK = HeroUtil.getCurrentValue(hero.getObj("Eigenschaften").getObj("KK"), true);
		if (steps != Integer.MIN_VALUE && steps != 0) {
			final int modifier = (KK - threshold) / steps;
			return modifier;
		} else
			return 0;
	}

	public static String getTPString(final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon) {
		return getTPString(weapon, baseWeapon, "", getTPKKModifier(hero, weapon, baseWeapon));
	}

	private static String getTPString(final JSONObject weapon, final JSONObject baseWeapon, final String defaultTP, final int TPKKModifier) {
		final JSONObject TPValues = weapon.getObjOrDefault("Trefferpunkte", baseWeapon.getObjOrDefault("Trefferpunkte", null));
		if (TPValues == null) return defaultTP;

		final StringBuilder TPString = DSAUtil.getRollString(TPValues, TPValues.getIntOrDefault("Trefferpunkte", 0) + TPKKModifier);

		if (TPValues.getBoolOrDefault("Geweihter Schaden", false)) {
			TPString.append("g");
		}
		if (TPValues.getBoolOrDefault("Magischer Schaden", false)) {
			TPString.append("m");
		}
		if (TPValues.getBoolOrDefault("Kälteschaden", false)) {
			TPString.append("k");
		}
		if (TPValues.getBoolOrDefault("Ausdauerschaden", false)) {
			TPString.append("(A)");
		}
		if (TPValues.getBoolOrDefault("Reduzierte Wundschwelle", false)) {
			TPString.append('*');
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

	public static String getVeteranBGBString(final JSONObject hero, final JSONObject bio, final JSONObject professions) {
		final StringBuilder professionString = new StringBuilder();
		final JSONObject pros = hero.getObj("Vorteile");
		final boolean female = "weiblich".equals(bio.getString("Geschlecht"));
		if (pros != null) {
			if (pros.containsKey("Veteran")) {
				final JSONObject veteran = pros.getObj("Veteran");
				professionString.append("Veteran");
				if (veteran.containsKey("Profession:Modifikation")) {
					final JSONArray veteranMod = veteran.getArr("Profession:Modifikation");
					if (veteranMod.size() > 0) {
						professionString.append(' ');
						addProfessionModifierString(professionString, professions.getObj(bio.getStringOrDefault("Profession", "")), veteranMod, female);
					}
				}
			}
			if (pros.containsKey("Breitgefächerte Bildung")) {
				final JSONObject bgb = pros.getObj("Breitgefächerte Bildung");
				if (professionString.length() > 0) {
					professionString.append(' ');
				}
				professionString.append("BGB ");
				final String bgbProfessionName = bgb.getString("Profession");
				JSONObject bgbProfession = null;
				if (female) {
					bgbProfession = professions.getObj(bgbProfessionName);
					professionString.append(bgbProfession.getStringOrDefault("Weiblich", bgbProfessionName));
				} else {
					professionString.append(bgbProfessionName);
				}
				if (bgb.containsKey("Profession:Modifikation")) {
					final JSONArray bgbMod = bgb.getArr("Profession:Modifikation");
					if (bgbMod.size() > 0) {
						professionString.append(" (");
						addProfessionModifierString(professionString, bgbProfession, bgbMod, female);
						professionString.append(")");
					}
				}
			}
		}
		return professionString.toString();
	}

	public static String getWeaponNotes(final JSONObject weapon, final JSONObject baseWeapon, final String type, final JSONObject hero) {
		final StringBuilder notes = new StringBuilder(weapon.getStringOrDefault("Anmerkungen", baseWeapon.getStringOrDefault("Anmerkungen", "")));
		boolean first = notes.isEmpty();

		if (List.of("Raufen", "Ringen").contains(type) && hero != null) {
			final JSONObject anatomy = hero.getObj("Talente").getObj("Wissenstalente").getObjOrDefault("Anatomie", null);
			if (anatomy != null && anatomy.getIntOrDefault("TaW", 0) >= 10) {
				if (first) {
					first = false;
				} else {
					notes.append(", ");
				}
				notes.append("+1TP");
				if (weapon.getObjOrDefault("Trefferpunkte", baseWeapon.getObj("Trefferpunkte")).getBoolOrDefault("Ausdauerschaden", false)) {
					notes.append("(A)");
				}
				notes.append(" gg. Menschen");
			}
		}

		if (weapon.getBoolOrDefault("Zweithand", baseWeapon.getBoolOrDefault("Zweithand", false))) {
			if (first) {
				first = false;
			} else {
				notes.append(", ");
			}
			if (hero.getObj("Vorteile").containsKey("Linkshänder")) {
				notes.append("Rechte Hand");
			} else {
				notes.append("Linke Hand");
			}
		}

		if (baseWeapon.containsKey("Bannschwert") && baseWeapon.getObj("Bannschwert").getObj("Rituale").containsKey("Bannschwert")) {
			if (first) {
				first = false;
			} else {
				notes.append(", ");
			}
			notes.append("Bannschwert");
			if (baseWeapon.getObj("Bannschwert").getObj("Rituale").containsKey("Apport")) {
				notes.append(", Apport");
			}
		}

		final JSONObject weaponMastery = HeroUtil.getSpecialisation(hero.getObj("Sonderfertigkeiten").getArrOrDefault("Waffenmeister", null),
				type, weapon.getStringOrDefault("Typ", baseWeapon.getString("Typ")));
		if (weaponMastery != null) {
			final JSONObject easierManeuvers = weaponMastery.getObjOrDefault("Manöver:Erleichterung", null);
			final JSONArray additionalManeuvers = weaponMastery.getArrOrDefault("Manöver:Zusätzlich", null);
			final JSONObject pros = weaponMastery.getObjOrDefault("Vorteile", null);
			if (easierManeuvers != null) {
				for (final String maneuver : easierManeuvers.keySet()) {
					if (first) {
						first = false;
					} else {
						notes.append(", ");
					}
					notes.append(maneuver);
					notes.append('-');
					notes.append(easierManeuvers.getInt(maneuver));
				}
			}
			if (additionalManeuvers != null) {
				for (final String maneuver : additionalManeuvers.getStrings()) {
					if (first) {
						first = false;
					} else {
						notes.append(", ");
					}
					notes.append(maneuver);
				}
			}
			if (pros != null) {
				for (final String pro : pros.keySet()) {
					if (first) {
						first = false;
					} else {
						notes.append(", ");
					}
					notes.append(pro);
				}
			}
		}

		return notes.toString();
	}

	public static int getZoneRS(final JSONObject hero, final String zone) {
		return getZoneRS(hero, zone, getDefaultArmor(hero));
	}

	public static int getZoneRS(final JSONObject hero, final String zone, final JSONObject armorSet) {
		final String armorSetting = Settings.getSettingStringOrDefault("Zonenrüstung", "Kampf", "Rüstungsart");
		final String armorSetName = armorSet != null ? armorSet.getStringOrDefault("Name", null) : null;

		final double[] RS = { 0 };

		foreachInventoryItem(hero, item -> item.containsKey("Kategorien") && item.getArr("Kategorien").contains("Rüstung"), (item, extraInventory) -> {
			JSONObject armor = item;
			if (item.containsKey("Rüstung")) {
				armor = item.getObj("Rüstung");
			}
			final JSONArray armorSets = armor.getArrOrDefault("Rüstungskombinationen", item.getArrOrDefault("Rüstungskombinationen", new JSONArray(null)));
			if (armorSetName == null && armorSets.size() == 0 || armorSetName != null && armorSets.contains(armorSetName)) {
				final JSONObject zoneRS = armor.getObjOrDefault("Rüstungsschutz", item.getObjOrDefault("Rüstungsschutz", null));
				if ("Gesamtrüstung".equals(armorSetting) || zoneRS == null && !(armor.containsKey("Rüstungsschutz") || item.containsKey("Rüstungsschutz"))) {
					RS[0] += armor.getIntOrDefault("Gesamtrüstungsschutz", item.getIntOrDefault("Gesamtrüstungsschutz", 0));
				} else if (zoneRS == null) {
					RS[0] += armor.getDoubleOrDefault("Rüstungsschutz", item.getDoubleOrDefault("Rüstungsschutz", 0.0));
				} else {
					RS[0] += zoneRS.getIntOrDefault(zone, 0);
				}
			}
		});

		final JSONObject pros = hero.getObj("Vorteile");
		if (pros.containsKey("Natürlicher Rüstungsschutz")) {
			RS[0] += pros.getObj("Natürlicher Rüstungsschutz").getIntOrDefault("Stufe", 0);
		}

		return (int) Math.round(RS[0]);
	}

	public static boolean hasReducedBE(final JSONObject hero, final JSONObject armorSet) {
		final boolean[] BEReduced = { false };
		final JSONObject skills = hero.getObj("Sonderfertigkeiten");
		final String armorSetName = armorSet != null ? armorSet.getStringOrDefault("Name", null) : null;
		final JSONArray armorAdaption = skills.getArr("Rüstungsgewöhnung I");
		for (int i = 0; i < armorAdaption.size() && !BEReduced[0]; ++i) {
			final String adaptation = armorAdaption.getObj(i).getString("Freitext");
			foreachInventoryItem(hero, item -> !BEReduced[0] && item.containsKey("Kategorien") && item.getArr("Kategorien").contains("Rüstung"),
					(item, extraInventory) -> {
						JSONObject armor = item;
						if (item.containsKey("Rüstung")) {
							armor = item.getObj("Rüstung");
						}
						final JSONArray armorSets = armor.getArrOrDefault("Rüstungskombinationen",
								item.getArrOrDefault("Rüstungskombinationen", new JSONArray(null)));
						if (armorSetName == null && armorSets.size() == 0 || armorSetName != null && armorSets.contains(armorSetName)) {
							if (adaptation.equals(armor.getStringOrDefault("Typ", item.getString("Typ")))) {
								BEReduced[0] = true;
							}
						}
					});
		}
		return BEReduced[0];
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
			final int taw, final Tuple3<Integer, Integer, Integer> roll, final int modification) {
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
		if (actualTalent instanceof final JSONArray arr) {
			for (int i = 0; i < actualTalent.size(); ++i) {
				taw = Math.max(taw, arr.getObj(i).getIntOrDefault("TaW", 0));
			}
		} else {
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
		if (pros.containsKey("Vollzauberer") || pros.containsKey("Halbzauberer") || pros.containsKey("Viertelzauberer")
				|| pros.containsKey("Viertelzauberer (unentdeckt)"))
			return true;
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
						final Object modificationsData = talentChanges.getUnsafe(talentName);
						if (modificationsData instanceof final JSONObject modifications) {
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

								unapplyTalentEffect(actualVariant, actual, targetValue, effectorName, change, true);
							}
						} else {
							final JSONObject actualVariant = new JSONObject(actualTalent);
							actualVariant.put(talent.containsKey("Auswahl") ? "Auswahl" : "Freitext",
									talent.getString(talent.containsKey("Auswahl") ? "Auswahl" : "Freitext"));
							actualTalent.add(actualVariant);

							unapplyTalentEffect(actualVariant, actual, targetValue, effectorName, talentChanges.getInt(talentName), true);
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
						unapplyTalentEffect(actualTalent, actual, targetValue, effectorName, change, false);
					}
				}
			}
		}
	}

	private static void unapplyTalentEffect(final JSONObject actualTalent, final JSONObject actual, final String targetValue, final String effectorName,
			final int change, final boolean isVariant) {
		actualTalent.put(targetValue, actualTalent.getIntOrDefault(targetValue, 0) - change * actual.getIntOrDefault("Stufe", 1));
		if (actualTalent.getInt(targetValue) == 0 && actualTalent.getStringOrDefault("AutomatischDurch", "").equals(effectorName)) {
			actualTalent.getParent().remove(actualTalent);
			if (isVariant) {
				if (actualTalent.getParent().size() == 0) {
					actualTalent.getParent().getParent().remove(actualTalent.getParent());
				}
			}
		}
		actualTalent.notifyListeners(null);
	}

	private HeroUtil() {}
}
