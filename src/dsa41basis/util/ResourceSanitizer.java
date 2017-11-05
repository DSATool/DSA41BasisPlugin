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

	public static final Function<JSONObject, JSONObject> historySanitizer = object -> {
		JSONObject result = object;
		if (object.containsKey("Steigerungshistorie")) {
			result = new JSONObject(null);
			result.put("Historie", object.getArr("Steigerungshistorie"));
			result.addAll(object, false);
		}
		return result;
	};

	public static final Function<JSONObject, JSONObject> heroSanitizer = object -> {
		JSONObject result = object;
		if (object.containsKey("Spieler")) {
			result = new JSONObject(null);
			result.put("Spieler", object.getStringOrDefault("Spieler", ""));
			result.put("Biografie", sortBiography(result, object.getObj("Biografie")));
			result.put("Eigenschaften", sortByResource(result, object.getObj("Eigenschaften"), ResourceManager.getResource("data/Eigenschaften")));
			result.put("Basiswerte", sortByResource(result, object.getObj("Basiswerte"), ResourceManager.getResource("data/Basiswerte")));
			result.put("Vorteile", sortProConSkills(result, object.getObj("Vorteile")));
			result.put("Nachteile", sortProConSkills(result, object.getObj("Nachteile")));
			result.put("Sonderfertigkeiten", sortProConSkills(result, object.getObj("Sonderfertigkeiten")));
			result.put("Verbilligte Sonderfertigkeiten", sortProConSkills(result, object.getObj("Verbilligte Sonderfertigkeiten")));
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

	private static JSONObject sortBiography(final JSONObject hero, final JSONObject object) {
		final JSONObject result = new JSONObject(hero);
		result.put("Vorname", object.getStringOrDefault("Vorname", ""));
		result.put("Nachname", object.getStringOrDefault("Nachname", ""));
		result.put("Geburtstag", object.getIntOrDefault("Geburtstag", 1));
		result.put("Geburtsmonat", object.getIntOrDefault("Geburtsmonat", 1));
		result.put("Geburtsjahr", object.getIntOrDefault("Geburtsjahr", 1000));
		result.put("Augenfarbe", object.getStringOrDefault("Augenfarbe", ""));
		if (object.containsKey("Schuppenfarbe 1")) {
			result.put("Schuppenfarbe 1", object.getStringOrDefault("Schuppenfarbe 1", ""));
			result.put("Schuppenfarbe 2", object.getStringOrDefault("Schuppenfarbe 2", ""));
		} else {
			result.put("Haarfarbe", object.getStringOrDefault("Haarfarbe", ""));
			result.put("Hautfarbe", object.getStringOrDefault("Hautfarbe", ""));
		}
		result.put("Rasse", object.getStringOrDefault("Rasse", ""));
		if (object.containsKey("Rasse:Modifikation")) {
			result.put("Rasse:Modifikation", object.getArr("Rasse:Modifikation").clone(result));
		}
		result.put("Kultur", object.getStringOrDefault("Kultur", ""));
		if (object.containsKey("Kultur:Modifikation")) {
			result.put("Kultur:Modifikation", object.getArr("Kultur:Modifikation").clone(result));
		}
		result.put("Profession", object.getStringOrDefault("Profession", ""));
		if (object.containsKey("Profession:Modifikation")) {
			result.put("Profession:Modifikation", object.getArr("Profession:Modifikation").clone(result));
		}
		result.put("Abenteuerpunkte", object.getIntOrDefault("Abenteuerpunkte", 0));
		result.put("Abenteuerpunkte-Guthaben", object.getIntOrDefault("Abenteuerpunkte-Guthaben", 0));
		result.addAll(object, false);
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

	private static JSONObject sortInventory(final JSONObject hero, final JSONObject object) {
		final JSONObject result = new JSONObject(hero);
		result.put("Geld", object.getObj("Geld").clone(result));
		result.put("Ausrüstung", object.getArr("Ausrüstung").clone(result));
		if (object.containsKey("Tiere")) {
			result.put("Tiere", object.getArr("Tiere").clone(result));
		}
		result.addAll(object, false);
		return result;
	}

	private static JSONObject sortProConSkills(final JSONObject hero, final JSONObject object) {
		final Set<String> actual = new TreeSet<>((s1, s2) -> comparator.compare(s1, s2));
		actual.addAll(object.keySet());
		final JSONObject result = new JSONObject(hero);
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
					newArr.add(choice);
				}
			}
		}
		return result;
	}

	private static JSONObject sortSpells(final JSONObject hero, final JSONObject object) {
		final Set<String> actual = new TreeSet<>((s1, s2) -> comparator.compare(s1, s2));
		actual.addAll(object.keySet());
		final JSONObject result = new JSONObject(hero);
		for (final String key : actual) {
			result.put(key, sortProConSkills(hero, object.getObj(key)));
		}
		return result;
	}

	private static JSONObject sortTalents(final JSONObject hero, final JSONObject object) {
		final JSONObject result = new JSONObject(hero);
		final JSONObject talentGroups = ResourceManager.getResource("data/Talentgruppen");
		for (final String key : talentGroups.keySet()) {
			if (object.containsKey(key)) {
				result.put(key, sortProConSkills(hero, object.getObj(key)));
			}
		}
		result.addAll(object, false);
		return result;
	}
}
