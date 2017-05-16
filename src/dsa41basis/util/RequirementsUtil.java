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

	private static boolean checkRKPVariants(JSONObject variants, JSONArray source) {
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

	private static boolean fulfillsRKPRequirement(JSONObject hero, String category, JSONObject rkps, boolean isProfession) {
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

	private static boolean hasRKP(JSONObject hero, String category, String name, JSONObject rkp, boolean isProfession) {
		final JSONObject biography = hero.getObj("Biografie");
		final JSONObject pros = hero.getObj("Vorteile");
		if (biography.getString(category).equals(name)) {
			if (rkp.containsKey("Varianten")) {
				if (checkRKPVariants(rkp.getObj("Varianten"), biography.getArr(category + ":Varianten"))) return true;
				if (isProfession && pros.containsKey("Veteran")) {
					if (checkRKPVariants(rkp.getObj("Varianten"), pros.getObj("Veteran").getArr(category + ":Varianten"))) return true;
				}
			} else
				return true;
		} else if (isProfession && pros.containsKey("Breitgefächerte Bildung")) {
			final JSONObject bgb = pros.getObj("Breitgefächerte Bildung");
			if (bgb.getString("Profession").equals(name)) {
				if (rkp.containsKey("Varianten")) {
					if (checkRKPVariants(rkp.getObj("Varianten"), bgb.getArr(category + ":Varianten"))) return true;
				} else
					return true;
			}
		}
		return false;
	}

	public static boolean isRequirementFulfilled(JSONObject hero, JSONObject requirements, String choice, String freeText) {
		if (requirements == null) return true;

		if (requirements.containsKey("Wahl")) {
			final JSONArray choices = requirements.getArr("Wahl");
			for (int i = 0; i < choices.size(); ++i) {
				boolean fulfilled = false;
				final JSONArray currentChoices = choices.getArr(i);
				for (int j = 0; j < currentChoices.size(); ++j) {
					if (isRequirementFulfilled(hero, currentChoices.getObj(i), choice, freeText)) {
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
				if (choices.getString(i).equals(freeText)) {
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
				if (HeroUtil.getCurrentValue(attributes.getObj(attribute), false) < requiredAttributes.getInt(attribute)) return false;
			}
		}

		if (requirements.containsKey("Basiswerte")) {
			final JSONObject basicValues = ResourceManager.getResource("data/Basiswerte");
			final JSONObject actualValues = hero.getObj("Basiswerte");
			final JSONObject requiredValues = requirements.getObj("Basiswerte");
			for (final String basicValue : requiredValues.keySet()) {
				final int requiredValue = requiredValues.getInt(basicValue);
				if (requiredValue < 0) {
					if (HeroUtil.deriveValue(basicValues.getObj(basicValue), attributes, actualValues.getObj(basicValue), false) > -requiredValue) return false;
				} else if (HeroUtil.deriveValue(basicValues.getObj(basicValue), attributes, actualValues.getObj(basicValue), false) < requiredValue)
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
				for (final String talent : must.keySet()) {
					if (!isTalentRequirementFulfilled(hero, talent, must.getInt(talent))) return false;
				}
			}
			if (talents.containsKey("Wahl")) {
				final JSONArray choices = talents.getArr("Wahl");
				for (int i = 0; i < choices.size(); ++i) {
					final JSONObject curChoice = choices.getObj(i);
					boolean match = false;
					for (final String talent : curChoice.keySet()) {
						if (isTalentRequirementFulfilled(hero, talent, curChoice.getInt(talent))) {
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
			final JSONObject pros = hero.getObj("Vorteile");
			final JSONObject cons = hero.getObj("Nachteile");

			final JSONObject skills = requirements.getObj("Vorteile/Nachteile/Sonderfertigkeiten");
			if (skills.containsKey("Muss")) {
				final JSONObject must = skills.getObj("Muss");
				for (final String requiredSkillName : must.keySet()) {
					final JSONObject requiredSkill = must.getObj(requiredSkillName);
					JSONValue skill = null;
					if (pros.containsKey(requiredSkillName)) {
						skill = (JSONValue) pros.getUnsafe(requiredSkillName);
					} else if (cons.containsKey(requiredSkillName)) {
						skill = (JSONValue) cons.getUnsafe(requiredSkillName);
					} else if (actualSkills.containsKey(requiredSkillName)) {
						skill = (JSONValue) actualSkills.getUnsafe(requiredSkillName);
					} else
						return false;
					if (requiredSkill.containsKey("Auswahl")) {
						final JSONObject skillChoice = requiredSkill.getObj("Auswahl");
						if (skillChoice.containsKey("Muss")) {
							String choiceMust = skillChoice.getString("Muss");
							boolean found = false;
							if (choice != null && "Auswahl".equals(choiceMust)) {
								choiceMust = choice;
							} else if (freeText != null && "Freitext".equals(choiceMust)) {
								choiceMust = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (choiceMust.equals(actualSkill.getString("Auswahl"))) {
									found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
											: true;
									break;
								}
							}
							if (!found) return false;
						}
						if (skillChoice.containsKey("Wahl")) {
							final JSONArray choiceChoice = skillChoice.getArr("Wahl");
							boolean found = false;
							for (int i = 0; i < choiceChoice.size(); ++i) {
								String curChoiceChoice = choiceChoice.getString(i);
								if (choice != null && "Auswahl".equals(curChoiceChoice)) {
									curChoiceChoice = choice;
								} else if (freeText != null && "Freitext".equals(curChoiceChoice)) {
									curChoiceChoice = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (curChoiceChoice.equals(actualSkill.getString("Auswahl"))) {
										found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
												: true;
										break;
									}
								}
								if (found) {
									break;
								}
							}
							if (!found) return false;
						}
						if (skillChoice.containsKey("Nicht")) {
							String choiceNot = skillChoice.getString("Nicht");
							boolean found = false;
							if (choice != null && "Auswahl".equals(choiceNot)) {
								choiceNot = choice;
							} else if (freeText != null && "Freitext".equals(choiceNot)) {
								choiceNot = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (!choiceNot.equals(actualSkill.getString("Auswahl"))) {
									found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
											: true;
									if (found) {
										break;
									}
								}
							}
							if (!found) return false;
						}
					}
					if (requiredSkill.containsKey("Freitext")) {
						final JSONObject skillText = requiredSkill.getObj("Freitext");
						if (skillText.containsKey("Muss")) {
							String textMust = skillText.getString("Muss");
							boolean found = false;
							if (choice != null && "Auswahl".equals(textMust)) {
								textMust = choice;
							} else if (freeText != null && "Freitext".equals(textMust)) {
								textMust = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (textMust.equals(actualSkill.getString("Freitext"))) {
									found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
											: true;
									break;
								}
							}
							if (!found) return false;
						}
						if (skillText.containsKey("Wahl")) {
							final JSONArray textChoice = skillText.getArr("Wahl");
							boolean found = false;
							for (int i = 0; i < textChoice.size(); ++i) {
								String curTextChoice = textChoice.getString(i);
								if (choice != null && "Auswahl".equals(curTextChoice)) {
									curTextChoice = choice;
								} else if (freeText != null && "Freitext".equals(curTextChoice)) {
									curTextChoice = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (curTextChoice.equals(actualSkill.getString("Freitext"))) {
										found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
												: true;
										break;
									}
								}
								if (found) {
									break;
								}
							}
							if (!found) return false;
						}
						if (skill != null && skillText.containsKey("Nicht")) {
							String textNot = skillText.getString("Nicht");
							boolean found = false;
							if (choice != null && "Auswahl".equals(textNot)) {
								textNot = choice;
							} else if (freeText != null && "Freitext".equals(textNot)) {
								textNot = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (!textNot.equals(actualSkill.getString("Freitext"))) {
									found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
											: true;
									if (found) {
										break;
									}
								}
							}
							if (!found) return false;
						}
					}
					if (requiredSkill.containsKey("Stufe") && !requiredSkill.containsKey("Auswahl") && !requiredSkill.containsKey("Freitext")) {
						if (((JSONObject) skill).getIntOrDefault("Stufe", 0) < requiredSkill.getInt("Stufe")) return false;
					}
				}
			}
			if (skills.containsKey("Wahl")) {
				final JSONArray choices = skills.getArr("Wahl");
				for (int i = 0; i < choices.size(); ++i) {
					final JSONObject curChoice = choices.getObj(i);
					boolean match = false;
					for (final String requiredSkillName : curChoice.keySet()) {
						final JSONObject requiredSkill = curChoice.getObj(requiredSkillName);
						JSONValue skill = null;
						if (pros.containsKey(requiredSkillName)) {
							skill = (JSONValue) pros.getUnsafe(requiredSkillName);
						} else if (cons.containsKey(requiredSkillName)) {
							skill = (JSONValue) cons.getUnsafe(requiredSkillName);
						} else if (actualSkills.containsKey(requiredSkillName)) {
							skill = (JSONValue) actualSkills.getUnsafe(requiredSkillName);
						} else {
							continue;
						}
						if (requiredSkill.containsKey("Auswahl")) {
							final JSONObject skillChoice = requiredSkill.getObj("Auswahl");
							if (skillChoice.containsKey("Muss")) {
								String choiceMust = skillChoice.getString("Muss");
								boolean found = false;
								if (choice != null && "Auswahl".equals(choiceMust)) {
									choiceMust = choice;
								} else if (freeText != null && "Freitext".equals(choiceMust)) {
									choiceMust = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (choiceMust.equals(actualSkill.getString("Auswahl"))) {
										found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
												: true;
										break;
									}
								}
								if (!found) {
									continue;
								}
							}
							if (skillChoice.containsKey("Wahl")) {
								final JSONArray choiceChoice = skillChoice.getArr("Wahl");
								boolean found = false;
								for (int j = 0; j < choiceChoice.size(); ++j) {
									String curChoiceChoice = choiceChoice.getString(j);
									if (choice != null && "Auswahl".equals(curChoiceChoice)) {
										curChoiceChoice = choice;
									} else if (freeText != null && "Freitext".equals(curChoiceChoice)) {
										curChoiceChoice = freeText;
									}
									for (int k = 0; k < skill.size(); ++k) {
										final JSONObject actualSkill = ((JSONArray) skill).getObj(k);
										if (curChoiceChoice.equals(actualSkill.getString("Auswahl"))) {
											found = requiredSkill.containsKey("Stufe")
													? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe") : true;
											break;
										}
									}
									if (found) {
										break;
									}
								}
								if (!found) {
									continue;
								}
							}
							if (skillChoice.containsKey("Nicht")) {
								String choiceNot = skillChoice.getString("Nicht");
								boolean found = false;
								if (choice != null && "Auswahl".equals(choiceNot)) {
									choiceNot = choice;
								} else if (freeText != null && "Freitext".equals(choiceNot)) {
									choiceNot = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (!choiceNot.equals(actualSkill.getString("Auswahl"))) {
										found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
												: true;
										if (found) {
											break;
										}
									}
								}
								if (!found) {
									continue;
								}
							}
						}
						if (requiredSkill.containsKey("Freitext")) {
							final JSONObject skillText = requiredSkill.getObj("Freitext");
							if (skillText.containsKey("Muss")) {
								String textMust = skillText.getString("Muss");
								boolean found = false;
								if (choice != null && "Auswahl".equals(textMust)) {
									textMust = choice;
								} else if (freeText != null && "Freitext".equals(textMust)) {
									textMust = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (textMust.equals(actualSkill.getString("Freitext"))) {
										found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
												: true;
										break;
									}
								}
								if (!found) {
									continue;
								}
							}
							if (skillText.containsKey("Wahl")) {
								final JSONArray textChoice = skillText.getArr("Wahl");
								boolean found = false;
								for (int j = 0; j < textChoice.size(); ++j) {
									String curTextChoice = textChoice.getString(j);
									if (choice != null && "Auswahl".equals(curTextChoice)) {
										curTextChoice = choice;
									} else if (freeText != null && "Freitext".equals(curTextChoice)) {
										curTextChoice = freeText;
									}
									for (int k = 0; k < skill.size(); ++k) {
										final JSONObject actualSkill = ((JSONArray) skill).getObj(k);
										if (curTextChoice.equals(actualSkill.getString("Freitext"))) {
											found = requiredSkill.containsKey("Stufe")
													? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe") : true;
											break;
										}
									}
									if (found) {
										break;
									}
								}
								if (!found) {
									continue;
								}
							}
							if (skill != null && skillText.containsKey("Nicht")) {
								String textNot = skillText.getString("Nicht");
								boolean found = false;
								if (choice != null && "Auswahl".equals(textNot)) {
									textNot = choice;
								} else if (freeText != null && "Freitext".equals(textNot)) {
									textNot = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (!textNot.equals(actualSkill.getString("Freitext"))) {
										found = requiredSkill.containsKey("Stufe") ? actualSkill.getIntOrDefault("Stufe", 0) >= requiredSkill.getInt("Stufe")
												: true;
										if (found) {
											break;
										}
									}
								}
								if (!found) {
									continue;
								}
							}
						}
						match = true;
						break;
					}
					if (!match) return false;
				}
			}
			if (skills.containsKey("Nicht")) {
				final JSONObject not = skills.getObj("Nicht");
				for (final String prohibitedSkillName : not.keySet()) {
					final JSONObject prohibitedSkill = not.getObj(prohibitedSkillName);
					final boolean choiceOrText = prohibitedSkill.containsKey("Auswahl") || prohibitedSkill.containsKey("Freitext");
					JSONValue skill = null;
					if (pros.containsKey(prohibitedSkillName)) {
						skill = (JSONValue) pros.getUnsafe(prohibitedSkillName);
					} else if (cons.containsKey(prohibitedSkillName)) {
						skill = (JSONValue) cons.getUnsafe(prohibitedSkillName);
					} else if (actualSkills.containsKey(prohibitedSkillName)) {
						skill = (JSONValue) actualSkills.getUnsafe(prohibitedSkillName);
					}
					if (!choiceOrText && skill != null) return prohibitedSkill.containsKey("Stufe")
							? ((JSONObject) skill).getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe") : false;
					if (skill != null & prohibitedSkill.containsKey("Auswahl")) {
						final JSONObject skillChoice = prohibitedSkill.getObj("Auswahl");
						if (skillChoice.containsKey("Muss")) {
							String choiceMust = skillChoice.getString("Muss");
							if (choice != null && "Auswahl".equals(choiceMust)) {
								choiceMust = choice;
							} else if (freeText != null && "Freitext".equals(choiceMust)) {
								choiceMust = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (choiceMust.equals(actualSkill.getString("Auswahl")) && (!prohibitedSkill.containsKey("Stufe")
										|| actualSkill.getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe")))
									return false;
							}
						}
						if (skillChoice.containsKey("Wahl")) {
							final JSONArray choiceChoice = skillChoice.getArr("Wahl");
							for (int i = 0; i < choiceChoice.size(); ++i) {
								String curChoiceChoice = choiceChoice.getString(i);
								if (choice != null && "Auswahl".equals(curChoiceChoice)) {
									curChoiceChoice = choice;
								} else if (freeText != null && "Freitext".equals(curChoiceChoice)) {
									curChoiceChoice = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (curChoiceChoice.equals(actualSkill.getString("Auswahl")) && (!prohibitedSkill.containsKey("Stufe")
											|| actualSkill.getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe")))
										return false;
								}
							}
						}
						if (skillChoice.containsKey("Nicht")) {
							String choiceNot = skillChoice.getString("Nicht");
							if (choice != null && "Auswahl".equals(choiceNot)) {
								choiceNot = choice;
							} else if (freeText != null && "Freitext".equals(choiceNot)) {
								choiceNot = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (!choiceNot.equals(actualSkill.getString("Auswahl")) && (!prohibitedSkill.containsKey("Stufe")
										|| actualSkill.getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe")))
									return false;
							}
						}
					}
					if (skill != null && prohibitedSkill.containsKey("Freitext")) {
						final JSONObject skillText = prohibitedSkill.getObj("Freitext");
						if (skillText.containsKey("Muss")) {
							String textMust = skillText.getString("Muss");
							if (choice != null && "Auswahl".equals(textMust)) {
								textMust = choice;
							} else if (freeText != null && "Freitext".equals(textMust)) {
								textMust = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (textMust.equals(actualSkill.getString("Freitext")) && (!prohibitedSkill.containsKey("Stufe")
										|| actualSkill.getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe")))
									return false;
							}
						}
						if (skillText.containsKey("Wahl")) {
							final JSONArray textChoice = skillText.getArr("Wahl");
							for (int i = 0; i < textChoice.size(); ++i) {
								String curTextChoice = textChoice.getString(i);
								if (choice != null && "Auswahl".equals(curTextChoice)) {
									curTextChoice = choice;
								} else if (freeText != null && "Freitext".equals(curTextChoice)) {
									curTextChoice = freeText;
								}
								for (int j = 0; j < skill.size(); ++j) {
									final JSONObject actualSkill = ((JSONArray) skill).getObj(j);
									if (curTextChoice.equals(actualSkill.getString("Freitext")) && (!prohibitedSkill.containsKey("Stufe")
											|| actualSkill.getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe")))
										return false;
								}
							}
						}
						if (skillText.containsKey("Nicht")) {
							String textNot = skillText.getString("Nicht");
							if (choice != null && "Auswahl".equals(textNot)) {
								textNot = choice;
							} else if (freeText != null && "Freitext".equals(textNot)) {
								textNot = freeText;
							}
							for (int i = 0; i < skill.size(); ++i) {
								final JSONObject actualSkill = ((JSONArray) skill).getObj(i);
								if (!textNot.equals(actualSkill.getString("Freitext")) && (!prohibitedSkill.containsKey("Stufe")
										|| actualSkill.getIntOrDefault("Stufe", 0) >= prohibitedSkill.getInt("Stufe")))
									return false;
							}
						}
					}
				}
			}
		}

		return true;
	}

	private static boolean isTalentRequirementFulfilled(JSONObject hero, String talent, int value) {
		if ("Lesen/Schreiben".equals(talent)) {
			final JSONObject languages = hero.getObj("Talente").getObj("Sprachen und Schriften");
			for (final String language : languages.keySet()) {
				if (!HeroUtil.findTalent(language)._1.getBoolOrDefault("Schrift", false)) {
					continue;
				}
				final JSONObject actual = languages.getObj(language);
				if (value < 0) {
					if (actual == null || actual.getIntOrDefault("TaW", 0) > -value) return false;
				} else if (actual != null && actual.getIntOrDefault("TaW", 0) >= value && actual.getBoolOrDefault("aktiviert", true)) return true;
				return true;
			}
			return value < 0;
		} else {
			final Tuple<JSONObject, JSONObject> res = HeroUtil.findActualTalent(hero, talent);
			final JSONObject actual = res._1;
			if (res._2 != null && res._2 == hero.getObjOrDefault("Zauber", null)) {
				if (actual == null) return value < 0;
				for (final String rep : actual.keySet()) {
					if (value < 0) {
						if (actual.getObj(rep).getIntOrDefault("ZfW", 0) > -value) return false;
					} else if (actual.getObj(rep).getIntOrDefault("ZfW", 0) >= value && actual.getObj(rep).getBoolOrDefault("aktiviert", true)) return true;
				}
				return value < 0;
			} else {
				if (value < 0)
					return actual == null || actual.getIntOrDefault("TaW", 0) <= -value || !actual.getBoolOrDefault("aktiviert", true);
				else
					return actual != null && actual.getIntOrDefault("TaW", 0) >= value && actual.getBoolOrDefault("aktiviert", true);
			}
		}
	}

}
