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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import dsa41basis.hero.Spell;
import dsa41basis.hero.Talent;
import dsatool.resources.ResourceManager;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import dsatool.util.Util;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class DSAUtil {

	public enum Units {
		NONE, TIME, RANGE
	}

	public static final Random random = new Random();

	public static final String[] months = { "Praios", "Rondra", "Efferd", "Travia", "Boron", "Hesinde", "Firun", "Tsa", "Phex", "Peraine", "Ingerimm", "Rahja",
			"Namenloser" };

	public static final String[] weekdays = { "Erdtag", "Markttag", "Praiostag", "Rohalstag", "Feuertag", "Wassertag", "Windstag" };

	private static Map<String, Tuple<String, String>> timeUnits = new HashMap<>();

	private static Map<String, Tuple<String, String>> rangeUnits = new HashMap<>();

	static {
		timeUnits.put("Sekunde", new Tuple<>("Sek.", "Sek."));
		timeUnits.put("Aktion", new Tuple<>("Akt.", "Akt."));
		timeUnits.put("Kampfrunde", new Tuple<>("KR", "KR"));
		timeUnits.put("Minute", new Tuple<>("Min.", "Min."));
		timeUnits.put("Spielrunde", new Tuple<>("SR", "SR"));
		timeUnits.put("Stunde", new Tuple<>("h", "h"));
		timeUnits.put("Zeiteinheit", new Tuple<>("ZE", "ZE"));
		timeUnits.put("Tag", new Tuple<>("Tag", "Tage"));
		timeUnits.put("Woche", new Tuple<>("Woche", "Wochen"));
		timeUnits.put("Monat", new Tuple<>("Monat", "Monate"));
		timeUnits.put("Jahr", new Tuple<>("Jahr", "Jahre"));
		rangeUnits.put("Spann", new Tuple<>("Sp", "Sp"));
		rangeUnits.put("Schritt", new Tuple<>("S", "S"));
		rangeUnits.put("Meile", new Tuple<>("M", "M"));
	}

	public static int diceRoll(final int dice) {
		return random.nextInt(dice) + 1;
	}

	public static void foreach(final Predicate<JSONObject> filter, final BiConsumer<String, JSONObject> function, final JSONObject... collections) {
		foreach(filter, (k, o) -> {
			function.accept(k, o);
			return true;
		}, collections);
	}

	public static void foreach(final Predicate<JSONObject> filter, final BiFunction<String, JSONObject, Boolean> function, final JSONObject... collections) {
		for (final JSONObject collection : collections) {
			if (collection == null) {
				continue;
			}
			for (final String key : collection.keySet()) {
				final JSONObject object = collection.getObj(key);
				if (object != null && filter.test(object)) {
					if (!function.apply(key, object)) {
						break;
					}
				}
			}
		}
	}

	public static void foreach(final Predicate<JSONObject> filter, final Consumer<JSONObject> function, final JSONArray... collections) {
		foreach(filter, o -> {
			function.accept(o);
			return true;
		}, collections);
	}

	public static void foreach(final Predicate<JSONObject> filter, final Function<JSONObject, Boolean> function, final JSONArray... collections) {
		for (final JSONArray collection : collections) {
			if (collection == null) {
				continue;
			}
			for (int i = 0; i < collection.size(); ++i) {
				final JSONObject object = collection.getObj(i);
				if (object != null && filter.test(object)) {
					if (!function.apply(object)) {
						break;
					}
				}
			}
		}
	}

	public static String getBEString(final JSONObject talent) {
		final int beAdd = talent.getIntOrDefault("BEAdditiv", 0);
		final int beMul = talent.getIntOrDefault("BEMultiplikativ", 0);

		final StringBuilder beString = new StringBuilder(8);

		if (beAdd != 0) {
			beString.append("BE");
			if (beAdd > 0) {
				beString.append('+');
			}
			beString.append(beAdd);
		} else if (beMul != 0) {
			beString.append("BE");
			if (beMul != 1) {
				beString.append('x');
				beString.append(beMul);
			}
		} else {
			beString.append('-');
		}
		return beString.toString();
	}

	public static String getChallengeString(final JSONArray challenge) {
		if (challenge == null || challenge.size() < 3) return "—";
		final StringBuilder attributesString = new StringBuilder(8);
		attributesString.append(challenge.getString(0));
		attributesString.append('/');
		attributesString.append(challenge.getString(1));
		attributesString.append('/');
		attributesString.append(challenge.getString(2));
		for (int i = 3; i < challenge.size(); ++i) {
			final String mod = challenge.getUnsafe(i).toString();
			if ('-' != mod.charAt(0)) {
				attributesString.append('+');
			}
			attributesString.append(mod);
		}
		return attributesString.toString();
	}

	public static int getDaysBetween(final int oldDay, final int oldMonth, final int oldYear, final int newDay, final int newMonth, final int newYear) {
		return (newYear - oldYear) * 365 + (newMonth - oldMonth) * 30 + newDay - oldDay;
	}

	public static int getEnhancementCost(final int enhancement, int targetLevel, final boolean charGen) {
		final JSONObject costs = ResourceManager.getResource("data/Steigerungskosten");
		if (targetLevel <= 0) {
			if (charGen) return costs.getObj(getEnhancementGroupString(enhancement)).getIntOrDefault("Faktor", 1);
			targetLevel = 0;
		} else if (targetLevel > 31) {
			targetLevel = 31;
		}
		return costs.getObj(getEnhancementGroupString(enhancement)).getArr("Kosten").getInt(targetLevel);
	}

	public static int getEnhancementCost(final int enhancement, final int startLevel, final int targetLevel) {
		int result = 0;
		for (int i = startLevel + 1; i <= targetLevel; ++i) {
			result += getEnhancementCost(enhancement, i, false);
		}
		return result;
	}

	public static int getEnhancementCost(final Talent talent, final JSONObject hero, final String method, final int startLevel,
			final int targetLevel, final boolean charGen) {
		int modifier = "Lehrmeister".equals(method) ? -1 : "Selbststudium".equals(method) ? 1 : 0;
		final String talentGroup = HeroUtil.findTalent(talent.getName())._2;
		int charGenModifier = 0;
		int maxLevel = 10;
		if (charGen) {
			final JSONObject pros = hero.getObj("Vorteile");
			if (pros.containsKey("Breitgefächerte Bildung") || pros.containsKey("Veteran")) {
				maxLevel = 15;
			}
			if (pros.containsKey("Akademische Ausbildung (Gelehrter)") || pros.containsKey("Akademische Ausbildung (Magier)")) {
				if (Set.of("Wissenstalente", "Sprachen und Schriften").contains(talentGroup)) {
					--charGenModifier;

					final JSONArray proGroup = pros.getArrOrDefault("Begabung für Talentgruppe", null);
					final JSONArray proSingle = pros.getArrOrDefault("Begabung für Talent", null);
					if (proGroup != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							final String choice = pro.getString("Auswahl");
							if (talentGroup.equals(choice)) {
								++charGenModifier;
								break;
							}
						}
					}
					if (charGenModifier == -1 && proSingle != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							if (talent.getName().equals(pro.getString("Auswahl"))) {
								++charGenModifier;
								break;
							}
						}
					}
				}
			}
			if (pros.containsKey("Akademische Ausbildung (Krieger)")) {
				if (Set.of("Nahkampftalente", "Fernkampftalente").contains(talentGroup)) {
					charGenModifier -= 2;

					final JSONArray proGroup = pros.getArrOrDefault("Begabung für Talentgruppe", null);
					final JSONArray proSingle = pros.getArrOrDefault("Begabung für Talent", null);
					if (proGroup != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							final String choice = pro.getString("Auswahl");
							if (talentGroup.equals(choice) || "Kampftalente".equals(choice)) {
								++charGenModifier;
								break;
							}
						}
					}
					if (charGenModifier == -2 && proSingle != null) {
						for (int i = 0; i < proGroup.size(); ++i) {
							final JSONObject pro = proGroup.getObj(i);
							if (talent.getName().equals(pro.getString("Auswahl"))) {
								++charGenModifier;
								break;
							}
						}
					}
				}
			}
			if ("Sprachen und Schriften".equals(talentGroup) && (talent.getActual() == null || !talent.getActual().getBoolOrDefault("Muttersprache", false)
					&& !talent.getActual().getBoolOrDefault("Zweitsprache", false) && !talent.getActual().getBoolOrDefault("Lehrsprache", false))) {
				modifier -= 1;
			}
		}
		modifier += charGenModifier;
		final double multiplier = hero.getObj("Vorteile").containsKey("Eidetisches Gedächtnis")
				&& (talent instanceof Spell || Set.of("Wissenstalente", "Sprachen und Schriften").contains(talentGroup)) ? 0.5
						: hero.getObj("Vorteile").containsKey("Eidetisches Gedächtnis")
								&& (talent instanceof Spell || "Sprachen und Schriften".equals(talentGroup)) ? 0.75 : 1.0;
		int result = 0;
		for (int i = startLevel + 1; i <= targetLevel; ++i) {
			if (charGen && i == maxLevel) {
				modifier -= charGenModifier;
			}
			final int localModifier = i > 10 && "Selbststudium".equals(method) ? 1 : 0;
			result += Math.round(getEnhancementCost(talent.getEnhancementComplexity(hero, i) + modifier + localModifier, i, charGen) * multiplier);
		}
		return result;
	}

	public static String getEnhancementGroupString(final int enhancement) {
		if (enhancement <= 0)
			return "A+";
		else if (enhancement >= 8) return "H";
		return Character.toString((char) ('@' + enhancement));
	}

	private static String getModificationString(final JSONObject modification, final Map<String, Tuple<String, String>> units, final boolean signed) {
		if (modification == null) return "—";
		if (modification.containsKey("Text")) return modification.getString("Text");

		if (modification.getBoolOrDefault("Selbst", false)) return "Selbst";
		if (modification.getBoolOrDefault("Berührung", false)) return "Ber.";
		if (modification.getBoolOrDefault("Sicht", false)) return "Sicht";
		if (modification.getBoolOrDefault("Fern", false)) return "Fern";
		if (modification.getBoolOrDefault("Horizont", false)) return "Horiz.";

		final StringBuilder result = new StringBuilder();

		boolean permanent = false;
		if (modification.containsKey("Permanent") && modification.getUnsafe("Permanent") instanceof Boolean) {
			permanent = modification.getBoolOrDefault("Permanent", false);
		}
		final boolean immediately = modification.getBoolOrDefault("Augenblicklich", false);
		final boolean atWill = modification.getBoolOrDefault("Beliebig", false);
		final boolean solstice = modification.getBoolOrDefault("Sonnenwende", false);

		if (permanent || immediately || atWill || solstice) {
			if (permanent) {
				result.append("permanent");
			}
			if (immediately) {
				result.append("sofort");
			}
			if (atWill) {
				result.append("beliebig");
			}
			if (solstice) {
				result.append("Sonnenw.");
			}
			if (modification.getBoolOrDefault("Aufrechterhalten", false)) {
				result.append(" (A)");
			}
			return result.toString();
		}

		boolean singleUnit = true;

		if (modification.containsKey("Summanden")) {
			final JSONArray summands = modification.getArr("Summanden");
			for (int i = 0; i < summands.size(); ++i) {
				final String summand = getModificationString(summands.getObj(i), units, signed);
				result.append((i == 0 || summand.startsWith("-") || signed ? "" : "+") + summand);
			}
		} else {
			final int factor = modification.getIntOrDefault("Multiplikativ", 1);
			int divisor = modification.getIntOrDefault("Divisor", 1);
			final int summand = modification.getIntOrDefault("Additiv", Integer.MIN_VALUE);

			if (summand > 0) {
				if (signed) {
					result.append('+');
				}
				result.append(summand);
				if (summand != 1) {
					singleUnit = false;
				}
			}

			final boolean base = modification.containsKey("Basis");
			final boolean portion = modification.containsKey("Grundmenge");
			if (base || portion) {
				if (summand > 0) {
					singleUnit = false;
				}
				if (summand > 0 || signed && (factor > 0 || divisor > 0)) {
					result.append('+');
				} else if (divisor < 0) {
					result.append('-');
					divisor *= -1;
				}
				if (portion || factor != 1 && factor != -1) {
					singleUnit = false;
					result.append(factor);
					result.append(base ? 'x' : "/");
				} else if (factor == -1) {
					singleUnit = false;
					result.append('-');
				} else if (factor == 1) {
					singleUnit = false;
				}
				result.append(modification.getString(base ? "Basis" : "Grundmenge"));
				if (divisor != 1) {
					result.append('/');
					result.append(divisor);
				}
				if (modification.getBoolOrDefault("Quadrat", false)) {
					result.append('²');
				}
			}

			if (summand <= 0 && summand != Integer.MIN_VALUE) {
				result.append(Util.getSignedIntegerString(summand));
				singleUnit = false;
			}
		}

		if (modification.containsKey("Minimum")) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("min. ");
			if (signed) {
				result.append(Util.getSignedIntegerString(modification.getInt("Minimum")));
			} else {
				result.append(Integer.toString(modification.getInt("Minimum")));
			}
		}

		if (modification.containsKey("Maximum")) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("max. ");
			result.append(Util.getSignedIntegerString(modification.getInt("Maximum")));
		}

		if (modification.containsKey("Einheit")) {
			result.append(' ');

			final String unit = modification.getString("Einheit");

			final Tuple<String, String> suffixes = units.getOrDefault(unit, new Tuple<>(unit, unit));

			if (singleUnit) {
				result.append(suffixes._1);
			} else {
				result.append(suffixes._2);
			}
		}

		if (modification.getBoolOrDefault("Aufrechterhalten", false)) {
			result.append(" (A)");
		}

		if (modification.containsKey("Permanent") && modification.getUnsafe("Permanent") instanceof JSONObject) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append(getModificationString(modification.getObj("Permanent"), units, signed));
			result.append("p");
		}

		if (result.length() == 0) {
			result.append('—');
		}

		return result.toString();
	}

	public static String getModificationString(final JSONObject modification, final Units units, final boolean signed) {
		Map<String, Tuple<String, String>> unitMap = null;
		switch (units) {
		case TIME:
			unitMap = timeUnits;
			break;
		case RANGE:
			unitMap = rangeUnits;
			break;
		case NONE:
		default:
			unitMap = new HashMap<>();
			break;
		}
		return getModificationString(modification, unitMap, signed);
	}

	/**
	 * Returns the monthday base for calculation of weekday and lunar phase as
	 * of Geographia Aventurica, p.253
	 *
	 * @param day
	 *            The day of the month 1..30
	 * @param month
	 *            The month of the year 0..12 (starting with Praios)
	 * @param year
	 *            The year in BF
	 * @return The monthday base
	 */
	public static int getMonthDayBase(final int day, final int month, final int year) {
		return (year - 993 + 2 * month + day) % 28;
	}

	public static String getRandomName(final JSONArray generator, final boolean male, final boolean middleClass, final boolean noble) {
		final StringBuilder result = new StringBuilder();
		final Map<Integer, Boolean> choices = new HashMap<>();
		final JSONArray[] consonantic = new JSONArray[2];
		for (int i = 0; i < generator.size(); ++i) {
			final JSONObject part = generator.getObj(i);
			if (part.containsKey("männlich") && male ^ part.getBool("männlich")) {
				continue;
			}
			if (part.containsKey("bürgerlich") && (middleClass || noble) ^ part.getBool("bürgerlich")) {
				continue;
			}
			if (part.containsKey("adlig") && noble ^ part.getBool("adlig")) {
				continue;
			}
			if (part.containsKey("Wahrscheinlichkeit")) {
				final JSONArray choice = part.getArr("Wahrscheinlichkeit");
				final Integer choiceNum = choice.getInt(0);
				final Boolean taken = choices.getOrDefault(choiceNum, false);
				final boolean take = random.nextDouble() < (taken ? choice.getDouble(2) : choice.getDouble(1));
				choices.put(choiceNum, taken || take);
				if (!take) {
					continue;
				}
			}
			if (part.containsKey("Text")) {
				result.append(part.getString("Text"));
			} else if (part.containsKey("konsonantisch")) {
				consonantic[part.getBool("konsonantisch") ? 0 : 1] = part.getArr("Auswahl");
			} else if (part.containsKey("Auswahl")) {
				final JSONArray choice = part.getArr("Auswahl");
				final String chosen = choice.getString(random.nextInt(choice.size()));
				if (consonantic[0] != null && consonantic[1] != null) {
					switch (Character.toLowerCase(chosen.charAt(0))) {
					case 'a':
					case 'e':
					case 'i':
					case 'o':
					case 'u':
					case 'y':
						result.append(consonantic[1].getString(random.nextInt(consonantic[1].size())));
						break;
					default:
						result.append(consonantic[0].getString(random.nextInt(consonantic[0].size())));
						break;
					}
					consonantic[0] = null;
					consonantic[1] = null;
				}
				result.append(chosen);
			} else if (part.containsKey("Wiederholung")) {
				final JSONArray repetition = part.getArr("Wiederholung");
				final int numRepetitions = random.nextInt(repetition.getInt(1) - repetition.getInt(0) + 1) + repetition.getInt(0);
				for (int j = 0; j < numRepetitions; ++j) {
					result.append(repetition.getString(2));
					final int repeatedPart = random.nextInt(repetition.getInt(4) - repetition.getInt(3) + 1) + repetition.getInt(3);
					final JSONArray choice = generator.getObj(i - repeatedPart).getArr("Auswahl");
					result.append(choice.getString(random.nextInt(choice.size())));
				}
			}
		}
		return result.toString().trim();
	}

	public static String getRepresentationAbbreviation(final String representation) {
		final JSONObject representations = ResourceManager.getResource("data/Repraesentationen");
		for (final String abbreviation : representations.keySet()) {
			if (representation.equals(representations.getObj(abbreviation).getString("Name"))) return abbreviation;
		}
		return null;
	}

	public static String printProOrCon(final JSONObject actual, final String proOrConName, final JSONObject proOrCon, final boolean displayLevel) {
		final StringBuilder result = new StringBuilder(proOrConName.replace(' ', '\u00A0'));

		if (proOrCon != null && (proOrCon.containsKey("Auswahl") || proOrCon.containsKey("Freitext"))) {
			result.append("\u00A0(");
			if (proOrCon.containsKey("Auswahl")) {
				result.append(actual.getString("Auswahl"));
				if (proOrCon.containsKey("Freitext")) {
					result.append(", ");
				}
			}
			if (proOrCon.containsKey("Freitext")) {
				result.append(actual.getString("Freitext"));
			}
			result.append(')');
		}

		if ("Breitgefächerte Bildung".equals(proOrConName)) {
			result.append("\u00A0(");
			result.append(actual.getString("Profession").replace(' ', '\u00A0'));
			final JSONArray variants = actual.getArr("Profession:Modifikation");
			if (variants.size() > 0) {
				result.append("\u00A0(");
				result.append(String.join(",\u00A0", variants.getStrings()).replace(' ', '\u00A0'));
				result.append(')');
			}
			result.append(')');
		} else if ("Veteran".equals(proOrConName)) {
			final JSONArray variants = actual.getArr("Profession:Modifikation");
			if (variants.size() > 0) {
				result.append("\u00A0(");
				result.append(String.join(",\u00A0", variants.getStrings()).replace(' ', '\u00A0'));
				result.append(')');
			}
		}

		if (displayLevel && proOrCon != null && proOrCon.getBoolOrDefault("Abgestuft", false)) {
			result.append('\u00A0');
			result.append(actual.getIntOrDefault("Stufe", 0));
		}

		return result.toString();
	}

	public static Tuple3<Integer, Integer, Integer> talentRoll() {
		return new Tuple3<>(diceRoll(20), diceRoll(20), diceRoll(20));
	}

	private DSAUtil() {}
}
