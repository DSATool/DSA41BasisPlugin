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

import dsatool.resources.ResourceManager;
import dsatool.util.Tuple;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class RequirementsUtil {

	private static boolean checkRKPVariants(final JSONObject variants, final JSONArray source) {
		if (variants.containsKey("Muss")) {
			final JSONArray requiredVariants = variants.getArr("Muss");
			for (int i = 0; i < requiredVariants.size(); ++i) {
				if (!source.contains(requiredVariants.getString(i))) return false;
			}
		}
		if (variants.containsKey("Wahl")) {
			final JSONArray variantChoiceGroups = variants.getArr("Wahl");
			for (int i = 0; i < variantChoiceGroups.size(); ++i) {
				final JSONArray choiceGroup = variantChoiceGroups.getArr(i);
				boolean match = false;
				for (int j = 0; j < choiceGroup.size(); ++j) {
					if (source.contains(choiceGroup.getString(i))) {
						match = true;
					}
				}
				if (!match) return false;
			}
		}
		if (variants.containsKey("Nicht")) {
			final JSONArray prohibitedVariants = variants.getArr("Nicht");
			for (int i = 0; i < prohibitedVariants.size(); ++i) {
				if (source.contains(prohibitedVariants.getString(i))) return false;
			}
		}

		return true;
	}

	private static boolean fulfillsRKPRequirement(final JSONObject hero, final String category, final JSONObject rkps, final boolean isProfession) {
		if (rkps.containsKey("Muss")) {
			final JSONObject must = rkps.getObj("Muss");
			for (final String rkp : must.keySet()) {
				if (!hasRKP(hero, category, rkp, must.getObj(rkp), isProfession)) return false;
			}
		}
		if (rkps.containsKey("Wahl")) {
			final JSONObject choices = rkps.getObj("Wahl");
			boolean match = false;
			for (final String rkp : choices.keySet()) {
				if (hasRKP(hero, category, rkp, choices.getObj(rkp), isProfession)) {
					match = true;
				}
			}
			if (!match) return false;
		}
		if (rkps.containsKey("Nicht")) {
			final JSONObject not = rkps.getObj("Nicht");
			for (final String rkp : not.keySet()) {
				if (hasRKP(hero, category, rkp, not.getObj(rkp), isProfession)) return false;
			}
		}

		return true;
	}

	private static boolean hasChoice(final JSONArray skill, final int requiredLevel, String requiredChoice, final String choice, final String text,
			final boolean matchText, final boolean negate, final boolean strictMatching) {
		boolean found = false;

		if ("Auswahl".equals(requiredChoice)) {
			requiredChoice = choice;
		} else if ("Freitext".equals(requiredChoice)) {
			requiredChoice = text;
		}
		if (requiredChoice == null && strictMatching) return false;

		for (int i = 0; i < skill.size() && !found; ++i) {
			final JSONObject actualSkill = skill.getObj(i);
			if (requiredChoice == null || requiredChoice.equals(actualSkill.getString(matchText ? "Freitext" : "Auswahl")) != negate) {
				found = actualSkill.getIntOrDefault("Stufe", 0) >= requiredLevel;
			}
		}

		return found;
	}

	private static boolean hasProConSkill(final JSONObject hero, final String name, final JSONObject requiredSkill, final String choice, final String text,
			final boolean strictMatching) {
		final JSONObject pros = hero.getObj("Vorteile");
		final JSONObject cons = hero.getObj("Nachteile");
		final JSONObject actualSkills = hero.getObj("Sonderfertigkeiten");

		JSONValue skill = null;
		if (pros.containsKey(name)) {
			skill = (JSONValue) pros.getUnsafe(name);
		} else if (cons.containsKey(name)) {
			skill = (JSONValue) cons.getUnsafe(name);
		} else if (actualSkills.containsKey(name)) {
			skill = (JSONValue) actualSkills.getUnsafe(name);
		} else
			return false;

		final int requiredLevel = requiredSkill.getIntOrDefault("Stufe", 0);

		for (int kind = 0; kind < 2; ++kind) {
			if (requiredSkill.containsKey(kind == 0 ? "Auswahl" : "Freitext")) {
				final JSONArray arr = (JSONArray) skill;

				final JSONObject skillChoice = requiredSkill.getObj(kind == 0 ? "Auswahl" : "Freitext");
				if (skillChoice.containsKey("Muss")) {
					if (!hasChoice(arr, requiredLevel, skillChoice.getString("Muss"), choice, text, kind != 0, false, strictMatching)) return false;
				}
				if (skillChoice.containsKey("Wahl")) {
					final JSONArray choiceChoice = skillChoice.getArr("Wahl");
					boolean found = false;
					for (int i = 0; i < choiceChoice.size() && !found; ++i) {
						if (hasChoice(arr, requiredLevel, choiceChoice.getString(i), choice, text, kind != 0, false, strictMatching)) {
							found = true;
						}
					}
					if (!found) return false;
				}
				if (skillChoice.containsKey("Nicht")) {
					if (!hasChoice(arr, requiredLevel, skillChoice.getString("Nicht"), choice, text, kind != 0, true, strictMatching)) return false;
				}
			}
		}

		if (!requiredSkill.containsKey("Auswahl") && !requiredSkill.containsKey("Freitext")) {
			if (skill instanceof JSONObject) {
				if (((JSONObject) skill).getIntOrDefault("Stufe", 0) < requiredLevel) return false;
			} else {
				if (!hasChoice((JSONArray) skill, requiredLevel, "Auswahl", null, null, false, false, false)) return false;
			}
		}

		return true;
	}

	private static boolean hasRKP(final JSONObject hero, final String category, final String name, final JSONObject rkp, final boolean isProfession) {
		final JSONObject biography = hero.getObj("Biografie");
		final JSONObject pros = hero.getObj("Vorteile");
		if (name.equals(biography.getString(category))) {
			if (rkp.containsKey("Varianten")) {
				if (checkRKPVariants(rkp.getObj("Varianten"), biography.getArr(category + ":Varianten"))) return true;
				if (isProfession && pros.containsKey("Veteran")) {
					if (checkRKPVariants(rkp.getObj("Varianten"), pros.getObj("Veteran").getArr(category + ":Varianten"))) return true;
				}
			} else
				return true;
		} else if (isProfession && pros.containsKey("Breitgefächerte Bildung")) {
			final JSONObject bgb = pros.getObj("Breitgefächerte Bildung");
			if (name.equals(bgb.getString("Profession"))) {
				if (rkp.containsKey("Varianten")) {
					if (checkRKPVariants(rkp.getObj("Varianten"), bgb.getArr(category + ":Varianten"))) return true;
				} else
					return true;
			}
		}
		return false;
	}

	public static boolean isRequirementFulfilled(final JSONObject hero, final JSONObject requirements, final String choice, final String text,
			final boolean includeManualMods) {
		if (requirements == null) return true;

		if (requirements.containsKey("Wahl")) {
			final JSONArray choices = requirements.getArr("Wahl");
			for (int i = 0; i < choices.size(); ++i) {
				boolean fulfilled = false;
				final JSONArray currentChoices = choices.getArr(i);
				for (int j = 0; j < currentChoices.size(); ++j) {
					if (isRequirementFulfilled(hero, currentChoices.getObj(i), choice, text, includeManualMods)) {
						fulfilled = true;
						break;
					}
				}
				if (!fulfilled) return false;
			}
		}

		if (requirements.containsKey("Auswahl")) {
			final JSONArray choices = requirements.getArr("Auswahl");
			boolean found = false;
			for (int i = 0; i < choices.size(); ++i) {
				if (choices.getString(i).equals(choice)) {
					found = true;
					break;
				}
			}
			if (!found) return false;
		}

		if (requirements.containsKey("Freitext")) {
			final JSONArray choices = requirements.getArr("Freitext");
			boolean found = false;
			for (int i = 0; i < choices.size(); ++i) {
				if (choices.getString(i).equals(text)) {
					found = true;
					break;
				}
			}
			if (!found) return false;
		}

		final JSONObject attributes = hero.getObj("Eigenschaften");
		if (requirements.containsKey("Eigenschaften")) {
			final JSONObject requiredAttributes = requirements.getObj("Eigenschaften");
			for (final String attribute : requiredAttributes.keySet()) {
				if (HeroUtil.getCurrentValue(attributes.getObj(attribute), includeManualMods) < requiredAttributes.getInt(attribute)) return false;
			}
		}

		if (requirements.containsKey("Basiswerte")) {
			final JSONObject basicValues = ResourceManager.getResource("data/Basiswerte");
			final JSONObject actualValues = hero.getObj("Basiswerte");
			final JSONObject requiredValues = requirements.getObj("Basiswerte");
			for (final String basicValue : requiredValues.keySet()) {
				final int requiredValue = requiredValues.getInt(basicValue);
				if (requiredValue < 0) {
					if (HeroUtil.deriveValue(basicValues.getObj(basicValue), hero, actualValues.getObj(basicValue), includeManualMods) > -requiredValue)
						return false;
				} else if (HeroUtil.deriveValue(basicValues.getObj(basicValue), hero, actualValues.getObj(basicValue), includeManualMods) < requiredValue)
					return false;
			}
		}

		final JSONObject biography = hero.getObj("Biografie");

		if (requirements.containsKey("Geschlecht")) {
			if (!requirements.getString("Geschlecht").equals(biography.getString("Geschlecht"))) return false;
		}

		if (requirements.containsKey("Rassen")) {
			if (!fulfillsRKPRequirement(hero, "Rasse", requirements.getObj("Rassen"), false)) return false;
		}

		if (requirements.containsKey("Kulturen")) {
			if (!fulfillsRKPRequirement(hero, "Kultur", requirements.getObj("Kulturen"), false)) return false;
		}

		if (requirements.containsKey("Professionen")) {
			if (!fulfillsRKPRequirement(hero, "Profession", requirements.getObj("Professionen"), true)) return false;
		}

		if (requirements.containsKey("Talente")) {
			final JSONObject talents = requirements.getObj("Talente");
			if (talents.containsKey("Muss")) {
				final JSONObject must = talents.getObj("Muss");
				for (String talent : must.keySet()) {
					final int value = must.getInt(talent);
					if ("Auswahl".equals(talent)) {
						talent = choice;
					} else if ("Freitext".equals(talent)) {
						talent = text;
					}
					if (!isTalentRequirementFulfilled(hero, talent, value)) return false;
				}
			}
			if (talents.containsKey("Wahl")) {
				final JSONArray choices = talents.getArr("Wahl");
				for (int i = 0; i < choices.size(); ++i) {
					final JSONObject curChoice = choices.getObj(i);
					boolean match = false;
					for (String talent : curChoice.keySet()) {
						final int value = curChoice.getInt(talent);
						if ("Auswahl".equals(talent)) {
							talent = choice;
						} else if ("Freitext".equals(talent)) {
							talent = text;
						}
						if (isTalentRequirementFulfilled(hero, talent, value)) {
							match = true;
						}
					}
					if (!match) return false;
				}
			}
		}

		final JSONObject actualSkills = hero.getObj("Sonderfertigkeiten");

		if (requirements.containsKey("Sonderfertigkeiten AP")) {
			final JSONObject skills = ResourceManager.getResource("data/Sonderfertigkeiten");
			final JSONObject rituals = ResourceManager.getResource("data/Rituale");
			final JSONObject ap = requirements.getObj("Sonderfertigkeiten AP");
			if (ap.containsKey("Muss")) {
				final JSONObject must = ap.getObj("Muss");
				for (final String groupName : must.keySet()) {
					int requiredAP = must.getInt(groupName);
					JSONObject group = null;
					if (skills.containsKey(groupName)) {
						group = skills.getObj(groupName);
					} else if (rituals.containsKey(groupName)) {
						group = rituals.getObj(groupName);
					} else if ("Liturgien".equals(groupName)) {
						group = ResourceManager.getResource("data/Liturgien");
					} else if ("Schamenenrituale".equals(groupName)) {
						group = ResourceManager.getResource("data/Schamanenrituale");
					} else
						return false;
					for (final String skillName : group.keySet()) {
						if (actualSkills.containsKey(skillName)) {
							final JSONObject skill = group.getObj(skillName);
							if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
								requiredAP -= skill.getIntOrDefault("Kosten", 0) * actualSkills.getArr(skillName).size();
							} else {
								requiredAP -= skill.getIntOrDefault("Kosten", 0);
							}
						}
					}
					if (requiredAP > 0) return false;
				}
			}
			if (ap.containsKey("Wahl")) {
				final JSONArray choices = ap.getArr("Wahl");
				for (int i = 0; i < choices.size(); ++i) {
					final JSONObject curChoice = choices.getObj(i);
					boolean match = false;
					for (final String groupName : curChoice.keySet()) {
						int requiredAP = curChoice.getInt(groupName);
						JSONObject group = null;
						if (skills.containsKey(groupName)) {
							group = skills.getObj(groupName);
						} else if (rituals.containsKey(groupName)) {
							group = rituals.getObj(groupName);
						} else if ("Liturgien".equals(groupName)) {
							group = ResourceManager.getResource("data/Liturgien");
						} else if ("Schamenenrituale".equals(groupName)) {
							group = ResourceManager.getResource("data/Schamanenrituale");
						} else {
							continue;
						}
						for (final String skillName : group.keySet()) {
							if (actualSkills.containsKey(skillName)) {
								final JSONObject skill = group.getObj(skillName);
								if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
									requiredAP -= skill.getIntOrDefault("Kosten", 0) * actualSkills.getArr(skillName).size();
								} else {
									requiredAP -= skill.getIntOrDefault("Kosten", 0);
								}
							}
						}
						if (requiredAP <= 0) {
							match = true;
							break;
						}
					}
					if (!match) return false;
				}
			}
		}

		if (requirements.containsKey("Vorteile/Nachteile/Sonderfertigkeiten")) {
			final JSONObject skills = requirements.getObj("Vorteile/Nachteile/Sonderfertigkeiten");
			if (skills.containsKey("Muss")) {
				final JSONObject must = skills.getObj("Muss");
				for (final String name : must.keySet()) {
					final JSONObject skill = must.getObj(name);
					if (!"Waffenspezialisierung".equals(name) || !"Auswahl".equals(skill.getObj("Auswahl").getStringOrDefault("Muss", null))) {
						if (!hasProConSkill(hero, name, skill, choice, text, false))
							return false;
					} else {
						final JSONObject talent = HeroUtil.findTalent(choice)._1;
						if (talent != null && talent.getArrOrDefault("Spezialisierungen", new JSONArray(null)).size() != 0)
							if (!hasProConSkill(hero, name, skill, choice, text, false)) return false;
					}
				}
			}
			if (skills.containsKey("Wahl")) {
				final JSONArray choices = skills.getArr("Wahl");
				for (int i = 0; i < choices.size(); ++i) {
					final JSONObject curChoice = choices.getObj(i);
					boolean found = false;
					for (final String name : curChoice.keySet()) {
						final JSONObject skill = curChoice.getObj(name);
						if (hasProConSkill(hero, name, skill, choice, text, false)) {
							found = true;
							break;
						}
					}
					if (!found) return false;
				}
			}
			if (skills.containsKey("Nicht")) {
				final JSONObject must = skills.getObj("Nicht");
				for (final String name : must.keySet()) {
					final JSONObject skill = must.getObj(name);
					if (hasProConSkill(hero, name, skill, choice, text, true)) return false;
				}
			}
		}

		return true;
	}

	private static boolean isTalentRequirementFulfilled(final JSONObject hero, final String talentName, final int value) {
		if (talentName == null)
			return true;
		else if ("Lesen/Schreiben".equals(talentName)) {
			final JSONObject languages = hero.getObj("Talente").getObj("Sprachen und Schriften");
			for (final String language : languages.keySet()) {
				if (!HeroUtil.findTalent(language)._1.getBoolOrDefault("Schrift", false)) {
					continue;
				}
				final JSONObject actual = languages.getObj(language);
				if (value < 0) {
					if (actual == null || actual.getIntOrDefault("TaW", 0) > -value) return false;
				} else if (actual != null && actual.getIntOrDefault("TaW", 0) >= value && actual.getBoolOrDefault("aktiviert", true)) {}
				return true;
			}
			return value < 0;
		} else {
			final JSONObject talent = HeroUtil.findTalent(talentName)._1;
			final Tuple<JSONValue, JSONObject> res = HeroUtil.findActualTalent(hero, talentName);
			final JSONValue actual = res._1;
			if (actual == null) return value < 0;
			if (res._2 != null && res._2 == hero.getObjOrDefault("Zauber", null)) {
				for (final String rep : ((JSONObject) actual).keySet()) {
					if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
						final JSONArray actualRep = ((JSONObject) actual).getArr(rep);
						for (int i = 0; i < actualRep.size(); ++i) {
							if (value < 0) {
								if (actualRep.getObj(i).getIntOrDefault("ZfW", 0) > -value) return false;
							} else if (actualRep.getObj(i).getIntOrDefault("ZfW", 0) >= value
									&& actualRep.getObj(i).getBoolOrDefault("aktiviert", true))
								return true;
						}
					} else {
						if (value < 0) {
							if (((JSONObject) actual).getObj(rep).getIntOrDefault("ZfW", 0) > -value) return false;
						} else if (((JSONObject) actual).getObj(rep).getIntOrDefault("ZfW", 0) >= value
								&& ((JSONObject) actual).getObj(rep).getBoolOrDefault("aktiviert", true))
							return true;
					}
				}
				return value < 0;
			} else {
				if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
					for (int i = 0; i < actual.size(); ++i) {
						if (value < 0) {
							if (((JSONArray) actual).getObj(i).getIntOrDefault("ZfW", 0) > -value) return false;
						} else if (((JSONArray) actual).getObj(i).getIntOrDefault("ZfW", 0) >= value
								&& ((JSONArray) actual).getObj(i).getBoolOrDefault("aktiviert", true))
							return true;
					}
					return value < 0;
				} else {
					if (value < 0)
						return ((JSONObject) actual).getIntOrDefault("TaW", 0) <= -value;
					else
						return ((JSONObject) actual).getIntOrDefault("TaW", 0) >= value && ((JSONObject) actual).getBoolOrDefault("aktiviert", true);
				}
			}
		}
	}

	private RequirementsUtil() {}

}
