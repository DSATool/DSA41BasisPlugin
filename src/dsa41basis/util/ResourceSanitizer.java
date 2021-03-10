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

import java.text.Collator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import dsatool.resources.ResourceManager;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class ResourceSanitizer {

	public static final Function<JSONObject, JSONObject> animalSanitizer = object -> {
		if (object.containsKey("Tiere")) {
			final JSONArray animals = object.getArr("Tiere");
			for (final JSONObject animal : animals.getObjs()) {
				final JSONObject basicValues = animal.getObj("Basiswerte");
				if (basicValues.containsKey("Rüstungsschutz")) {
					final JSONObject pros = animal.getObj("Vorteile");
					final JSONObject proRS = new JSONObject(pros);
					proRS.put("Stufe", basicValues.getObj("Rüstungsschutz").getIntOrDefault("Wert", 0));
					pros.put("Natürlicher Rüstungsschutz", proRS);
					basicValues.removeKey("Rüstungsschutz");
				}
				final JSONObject ini = basicValues.getObj("Initiative");
				if (ini.containsKey("Basis")) {
					final JSONObject iniBase = basicValues.getObj("Initiative-Basis");
					final int mod = ini.getIntOrDefault("Modifikator", 0);
					iniBase.put("Wert", ini.getInt("Basis") - mod);
					if (mod != 0) {
						iniBase.put("Modifikator", mod);
					}
					ini.removeKey("Basis");
					ini.removeKey("Modifikator");
				}
			}
		}
		return object;
	};

	public static final Function<JSONObject, JSONObject> heroSanitizer = object -> {
		JSONObject result = object;
		if (object.containsKey("Spieler")) {
			result = new JSONObject(null);
			result.put("Spieler", object.getStringOrDefault("Spieler", ""));
			result.put("Biografie", sortBiography(result, object.getObj("Biografie")));
			result.put("Eigenschaften", sortByResource(result, object.getObj("Eigenschaften"), ResourceManager.getResource("data/Eigenschaften")));
			result.put("Basiswerte", sortByResource(result, object.getObj("Basiswerte"), ResourceManager.getResource("data/Basiswerte")));
			result.put("Vorteile", sortActual(result, object.getObj("Vorteile")));
			result.put("Nachteile", sortActual(result, object.getObj("Nachteile")));
			result.put("Sonderfertigkeiten", sortActual(result, object.getObj("Sonderfertigkeiten")));
			result.put("Verbilligte Sonderfertigkeiten", sortActual(result, object.getObj("Verbilligte Sonderfertigkeiten")));
			result.put("Talente", sortTalents(result, object.getObj("Talente")));
			if (object.containsKey("Zauber")) {
				result.put("Zauber", sortSpells(result, object.getObj("Zauber")));
			}
			result.put("Besitz", sortInventory(result, object.getObj("Besitz")));
			result.put("Historie", object.getArr("Historie").clone(result));
			result.addAll(object, false);
		}

		return result;
	};

	private static final Collator comparator = Collator.getInstance(Locale.GERMANY);

	private static JSONObject sortActual(final JSONObject parent, final JSONObject object) {
		final Set<String> actual = new TreeSet<>(comparator);
		actual.addAll(object.keySet());
		final JSONObject result = new JSONObject(parent);
		for (final String key : actual) {
			final Object value = object.getUnsafe(key);
			if (value instanceof JSONObject) {
				result.put(key, ((JSONObject) value).clone(result));
			} else if (value instanceof JSONArray) {
				final JSONArray newArr = new JSONArray(result);
				result.put(key, newArr);
				final Set<JSONObject> choices = new TreeSet<>((o1, o2) -> {
					final String choice1 = o1.getString("Auswahl");
					final String choice2 = o2.getString("Auswahl");
					if (choice1 != null && choice2 != null) {
						final int choice = comparator.compare(choice1, choice2);
						if (choice != 0) return choice;
					}
					final String text1 = o1.getString("Freitext");
					final String text2 = o2.getString("Freitext");
					if (text1 != null && text2 != null) return comparator.compare(text1, text2);
					return 0;
				});
				choices.addAll(((JSONArray) value).getObjs());
				for (final JSONObject choice : choices) {
					newArr.add(choice.clone(newArr));
				}
			}
		}
		return result;
	}

	private static JSONObject sortBiography(final JSONObject hero, final JSONObject bio) {
		final JSONObject result = new JSONObject(hero);
		result.put("Vorname", bio.getStringOrDefault("Vorname", ""));
		result.put("Nachname", bio.getStringOrDefault("Nachname", ""));
		result.put("Geburtstag", bio.getIntOrDefault("Geburtstag", 1));
		result.put("Geburtsmonat", bio.getIntOrDefault("Geburtsmonat", 1));
		result.put("Geburtsjahr", bio.getIntOrDefault("Geburtsjahr", 1000));
		result.put("Augenfarbe", bio.getStringOrDefault("Augenfarbe", ""));
		if (bio.containsKey("Schuppenfarbe 1")) {
			result.put("Schuppenfarbe 1", bio.getStringOrDefault("Schuppenfarbe 1", ""));
			result.put("Schuppenfarbe 2", bio.getStringOrDefault("Schuppenfarbe 2", ""));
		} else {
			result.put("Haarfarbe", bio.getStringOrDefault("Haarfarbe", ""));
			result.put("Hautfarbe", bio.getStringOrDefault("Hautfarbe", ""));
		}
		result.put("Rasse", bio.getStringOrDefault("Rasse", ""));
		if (bio.containsKey("Rasse:Modifikation")) {
			result.put("Rasse:Modifikation", bio.getArr("Rasse:Modifikation").clone(result));
		}
		result.put("Kultur", bio.getStringOrDefault("Kultur", ""));
		if (bio.containsKey("Kultur:Modifikation")) {
			result.put("Kultur:Modifikation", bio.getArr("Kultur:Modifikation").clone(result));
		}
		result.put("Profession", bio.getStringOrDefault("Profession", ""));
		if (bio.containsKey("Profession:Modifikation")) {
			result.put("Profession:Modifikation", bio.getArr("Profession:Modifikation").clone(result));
		}
		result.put("Abenteuerpunkte", bio.getIntOrDefault("Abenteuerpunkte", 0));
		result.put("Abenteuerpunkte-Guthaben", bio.getIntOrDefault("Abenteuerpunkte-Guthaben", 0));
		result.addAll(bio, false);
		return result;
	}

	private static JSONObject sortByResource(final JSONObject hero, final JSONObject object, final JSONObject resource) {
		final JSONObject result = new JSONObject(hero);
		for (final String key : resource.keySet()) {
			if (object.containsKey(key)) {
				result.put(key, object.getObj(key).clone(result));
			}
		}
		result.addAll(object, false);
		return result;
	}

	private static JSONObject sortInventory(final JSONObject hero, final JSONObject inventory) {
		final JSONObject result = new JSONObject(hero);
		result.put("Geld", inventory.getObj("Geld").clone(result));
		result.put("Ausrüstung", inventory.getArr("Ausrüstung").clone(result));
		if (inventory.containsKey("Tiere")) {
			result.put("Tiere", inventory.getArr("Tiere").clone(result));
		}
		result.addAll(inventory, false);
		return result;
	}

	private static JSONObject sortSpells(final JSONObject hero, final JSONObject spells) {
		final Set<String> actual = new TreeSet<>(comparator);
		actual.addAll(spells.keySet());
		final JSONObject result = new JSONObject(hero);
		for (final String key : actual) {
			result.put(key, sortActual(result, spells.getObj(key)));
		}
		return result;
	}

	private static JSONObject sortTalents(final JSONObject hero, final JSONObject talents) {
		final JSONObject result = new JSONObject(hero);
		final JSONObject talentGroups = ResourceManager.getResource("data/Talentgruppen");
		for (final String key : talentGroups.keySet()) {
			if (talents.containsKey(key)) {
				result.put(key, sortActual(result, talents.getObj(key)));
			}
		}
		result.addAll(talents, false);
		return result;
	}

	private ResourceSanitizer() {}
}
