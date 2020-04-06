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
package dsa41basis.serialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

@SuppressWarnings("unchecked")
public class HeldenSoftwareXMLHeroLoader implements FileLoader {

	private static JSONObject replacements = ResourceManager.getResource("data/HeldenSoftwareXML");
	private static JSONObject proConReplacements = replacements.getObj("Vorteile/Nachteile");
	private static JSONObject ritualKnowledge = replacements.getObj("Ritualkenntnis");
	private static JSONObject representationReplacements = replacements.getObj("Repräsentationen");
	private static JSONObject raceReplacements = replacements.getObj("Rassen");
	private static JSONObject cultureReplacements = replacements.getObj("Kulturen");
	private static JSONObject professionReplacements = replacements.getObj("Professionen");
	private static JSONObject talentReplacements = replacements.getObj("Talente");
	private static JSONObject spellReplacements = replacements.getObj("Zauber");
	private static JSONObject groupReplacements = replacements.getObj("Talentgruppen");
	private static JSONObject skillReplacements = replacements.getObj("Sonderfertigkeiten");
	private static JSONObject shortRepReplacements = replacements.getObj("Repräsentationen:Abkürzungen");

	private int at, pa;

	private JSONObject hero;

	private XMLStreamReader parser;

	private String boronCult = null;

	private void apply(final String end, final Tuple<String, Runnable>... extractors) {
		try {
			while (parser.hasNext()) {
				if (parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
					for (final Tuple<String, Runnable> extractor : extractors) {
						if (extractor._1.equals(parser.getLocalName())) {
							extractor._2.run();
							int level = 0;
							if (parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
								parser.next();
							}
							do {
								if (parser.getEventType() == XMLStreamConstants.START_ELEMENT && extractor._1.equals(parser.getLocalName())) {
									++level;
								} else if (parser.getEventType() == XMLStreamConstants.END_ELEMENT && extractor._1.equals(parser.getLocalName())) {
									if (level == 0) {
										break;
									} else {
										--level;
									}
								}
								parser.next();
							} while (parser.hasNext());
							break;
						}
					}
				} else if (parser.getEventType() == XMLStreamConstants.END_ELEMENT && end.equals(parser.getLocalName())) {
					break;
				}
				parser.next();
			}
		} catch (final XMLStreamException e) {
			ErrorLogger.logError(e);
		}
	}

	private String ask(final String title, final String text, final String... choices) {
		final Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(text);

		final Map<ButtonType, String> results = new LinkedHashMap<>();

		for (int i = 0; i < choices.length; i += 2) {
			results.put(new ButtonType(choices[i]), choices[i + 1]);
		}

		alert.getButtonTypes().setAll(results.keySet());

		final Optional<ButtonType> result = alert.showAndWait();

		if (result.isPresent())
			return results.get(result.get());
		else
			return choices[1];
	}

	private void cleanupCheaperSkills() {
		final JSONObject actualSkills = hero.getObj("Sonderfertigkeiten");
		final JSONObject cheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");

		final List<String> toRemove = new ArrayList<>();
		for (final String name : cheaperSkills.keySet()) {
			if (!actualSkills.containsKey(name) || name.startsWith("temporary")) {
				continue;
			}
			final JSONObject skill = HeroUtil.findSkill(name);
			final boolean hasChoice = skill.containsKey("Auswahl");
			final boolean hasText = skill.containsKey("Freitext");
			if (hasChoice || hasText) {
				final JSONArray cheaper = cheaperSkills.getArr(name);
				final JSONArray actual = actualSkills.getArr(name);
				for (int i = 0; i < cheaper.size(); ++i) {
					final JSONObject cheaperSkill = cheaper.getObj(i);
					if (hasChoice && !cheaperSkill.containsKey("Auswahl")) {
						continue;
					}
					if (hasText && !cheaperSkill.containsKey("Freitext")) {
						continue;
					}
					for (int j = 0; j < actual.size(); ++j) {
						final JSONObject actualSkill = actual.getObj(j);
						if (hasChoice && !cheaperSkill.getString("Auswahl").equals(actualSkill.getString("Auswahl"))) {
							continue;
						}
						if (hasText && !cheaperSkill.getString("Freitext").equals(actualSkill.getString("Freitext"))) {
							continue;
						}
						actual.removeAt(j);
						break;
					}
				}
			} else {
				toRemove.add(name);
			}
		}
		for (final String name : toRemove) {
			cheaperSkills.removeKey(name);
		}
	}

	private JSONObject enterSkill(final String name, final int value, final String choice, final String text, final boolean applyEffect,
			final boolean cheaperSkill) {
		final JSONObject skills = hero.getObj(cheaperSkill ? "Verbilligte Sonderfertigkeiten" : "Sonderfertigkeiten");
		final JSONObject skill = HeroUtil.findSkill(name);
		JSONObject currentSkill = null;
		if (skill != null) {
			if (skill.containsKey("Freitext") || skill.containsKey("Auswahl")) {
				final JSONArray actualSkill = skills.getArr(name);
				if (cheaperSkill) {
					for (int i = 0; i < actualSkill.size(); ++i) {
						currentSkill = actualSkill.getObj(i);
						if (skill.containsKey("Auswahl") && (choice == null || !choice.equals(currentSkill.getString("Auswahl")))) {
							continue;
						}
						if (skill.containsKey("Freitext") && (text == null || !text.equals(currentSkill.getString("Freitext")))) {
							continue;
						}
						currentSkill.put("Verbilligungen", currentSkill.getIntOrDefault("Verbilligungen", 1) + 1);
						return currentSkill;
					}
				}
				currentSkill = new JSONObject(actualSkill);
				actualSkill.add(currentSkill);
				if (skill.getBoolOrDefault("Abgestuft", false)) {
					currentSkill.put("Stufe", value);
				}
				if (skill.containsKey("Auswahl")) {
					currentSkill.put("Auswahl", choice != null ? choice : "");
				}
				if (skill.containsKey("Freitext")) {
					currentSkill.put("Freitext", text != null ? text : "");
				}
				if (applyEffect) {
					HeroUtil.applyEffect(hero, name, skill, currentSkill);
				}
			} else {
				if (cheaperSkill && skills.containsKey(name)) {
					currentSkill = skills.getObj(name);
					currentSkill.put("Verbilligungen", currentSkill.getIntOrDefault("Verbilligungen", 1) + 1);
				} else {
					currentSkill = new JSONObject(skills);
					skills.put(name, currentSkill);
					if (applyEffect) {
						HeroUtil.applyEffect(hero, name, skill, currentSkill);
					}
				}
			}
		}
		return currentSkill;
	}

	private Map<String, String> extract(final String end, final Tuple3<String, Supplier<String>, Supplier<String>>... extractors) {
		final Map<String, String> result = new HashMap<>();

		final Tuple<String, Runnable>[] funs = new Tuple[extractors.length];
		int i = 0;
		for (final Tuple3<String, Supplier<String>, Supplier<String>> extractor : extractors) {
			funs[i] = new Tuple<>(extractor._1, () -> result.put(extractor._2.get(), extractor._3.get()));
			++i;
		}
		apply(end, funs);

		return result;
	}

	private String get(final String attributeName) {
		return parser.getAttributeValue("", attributeName);
	}

	@Override
	public List<String> getExtensions() {
		return Collections.singletonList("*.xml");
	}

	private String getLastPart(String str, final JSONObject replacements) {
		final int separator = str.lastIndexOf('.');
		str = str.substring(separator + 1);
		str = replacements.getStringOrDefault(str, str);
		return str;
	}

	@Override
	public String getName() {
		return "HeldenSoftware.xml";
	}

	@Override
	public List<JSONObject> loadFile(final File file) {
		try (InputStream in = new FileInputStream(file)) {
			final XMLInputFactory factory = XMLInputFactory.newInstance();

			parser = factory.createXMLStreamReader(in);

			final List<JSONObject> heroes = new LinkedList<>();

			apply("", new Tuple<>("helden", () -> parseHeroes(heroes)));

			return heroes;
		} catch (final Exception e) {
			ErrorLogger.logError(e);
			return null;
		}
	}

	private void parseAttributes() {
		final JSONObject actualAttributes = hero.getObj("Eigenschaften");
		final JSONObject actualBasicValues = hero.getObj("Basiswerte");

		final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");

		final JSONObject speed = new JSONObject(actualBasicValues);
		speed.put("Wert", 8);
		actualBasicValues.put("Geschwindigkeit", speed);

		apply("eigenschaften", new Tuple<>("eigenschaft", () -> {
			final String name = get("name");
			final int value = Integer.parseInt(get("value"));
			final int mod = Integer.parseInt(get("mod"));
			if (Arrays
					.asList("Initiative-Basis", "Wundschwelle", "Sozialstatus", "Lebensenergie", "Ausdauer", "Astralenergie", "Karmaenergie", "Magieresistenz")
					.contains(name)) {
				final JSONObject basicValue = new JSONObject(actualBasicValues);
				actualBasicValues.put(name, basicValue);
				if ("Sozialstatus".equals(name)) {
					basicValue.put("Wert", value);
				} else if ("Karmaenergie".equals(name)) {
					basicValue.put("Permanent", mod);
				} else {
					basicValue.put("Modifikator", mod);
					basicValue.put("Kauf", value);
				}
			} else if (Set.of("at", "pa").contains(name)) {
				if ("at".equals(name)) {
					at = value;
				} else {
					pa = value;
				}
			} else {
				for (final String attributeName : attributes.keySet()) {
					if (name.equals(attributes.getObj(attributeName).getString("Name"))) {
						final JSONObject attribute = new JSONObject(actualAttributes);
						actualAttributes.put(attributeName, attribute);
						attribute.put("Wert", value + mod);
						attribute.put("Start", Integer.parseInt(get("startwert")) + mod);
						attribute.put("Modifikator", mod);
						break;
					}
				}
			}
		}));
	}

	private void parseBasis() {
		final JSONObject bio = hero.getObj("Biografie");

		apply("basis", new Tuple<>("geschlecht", () -> bio.put("Geschlecht", get("name"))), new Tuple<>("rasse", () -> parseRace()),
				new Tuple<>("kultur", () -> parseCulture()), new Tuple<>("ausbildungen", () -> parseProfession()),
				new Tuple<>("abenteuerpunkte", () -> bio.put("Abenteuerpunkte", Integer.parseInt(get("value")))),
				new Tuple<>("freieabenteuerpunkte", () -> bio.put("Abenteuerpunkte-Guthaben", Integer.parseInt(get("value")))));
	}

	private void parseCulture() {
		final JSONObject bio = hero.getObj("Biografie");
		String replaced = getLastPart(get("name"), cultureReplacements);
		if ("AndergastNostria".equals(replaced)) {
			replaced = ask("Variante auswählen", "Variante für Kultur Andergast und Nostria auswählen", "Andergast", "Andergast", "Nostria", "Nostria");
		}
		final String[] split = replaced.split("\\|");
		final String cultureName = split[0];
		bio.put("Kultur", cultureName);

		String ml = get("sprache");
		String sl = get("zweitsprache");
		String mlWriting = get("schrift");

		JSONObject culture = ResourceManager.getResource("data/Kulturen").getObjOrDefault(cultureName, null);

		final List<String> variants = new ArrayList<>();
		for (int i = 1; i < split.length; ++i) {
			variants.add(split[i]);
		}
		apply("kultur", new Tuple<>("variante", () -> {
			final String name = get("name");
			if ("Horasreich".equals(cultureName) && "Städte mit wichtigem Tempel/Pilgerstätte".equals(name)) {
				variants.add("Städte mit wichtigem Tempel/mit Pilgerstätte");
			} else if ("Novadi".equals(cultureName) && "Männer oder Achmad'Sunni".equals(name)) {
				variants.add("männlich".equals(bio.getStringOrDefault("Geschlecht", "männlich")) ? "Männer" : "Achmad'Sunni");
			} else if ("Dschungelstämme".equals(cultureName) && "keine Spezialisierung".equals(name)) {
				variants.add("Dschungelstämme");
			} else if ("Südaventurien".equals(cultureName) && "keine Spezialisierung".equals(name)) {
				variants.add("Südaventurien");
			} else if ("Goblinstämme".equals(cultureName) && "Schneegoblins".equals(name)) {
				variants.add("männlich".equals(bio.getStringOrDefault("Geschlecht", "männlich")) ? "Männer" : "Frauen");
				variants.add("Schneegoblins");
			} else if ("Stammes-Achaz".equals(cultureName) && "beliebiger Stamm".equals(name)) {
				variants.add("Stammes-Achaz");
			} else {
				for (final String variant : cultureReplacements.getStringOrDefault(name, name).split("\\|")) {
					variants.add(variant);
				}
			}
		}));

		final JSONArray mods = new JSONArray(bio);

		while (!variants.isEmpty() && culture != null && culture.containsKey("Varianten")) {
			boolean found = false;
			for (int i = 0; i < variants.size(); ++i) {
				if (culture.containsKey("Varianten")) {
					final String name = variants.get(i);
					if (culture.getObj("Varianten").containsKey(name)) {
						found = true;
						culture = culture.getObj("Varianten").getObj(name);
						if (mods.size() == 0 && !cultureName.equals(name) || mods.size() > 0 && !mods.getString(mods.size() - 1).equals(name)) {
							mods.add(name);
						}
						variants.remove(i);
						i--;
					}
				} else {
					break;
				}
			}
			if (!found) {
				if (!variants.isEmpty() && culture.containsKey("Varianten")) {
					final String name = variants.remove(0);
					for (final String variantName : culture.getObj("Varianten").keySet()) {
						final JSONObject variant = culture.getObj("Varianten").getObj(variantName);
						if (variant.containsKey("Varianten") && variant.getObj("Varianten").containsKey(name)) {
							if (found) {
								found = false;
								mods.removeAt(mods.size() - 1);
								mods.removeAt(mods.size() - 1);
								break;
							} else {
								found = true;
								mods.add(variantName);
								mods.add(name);
							}
						}
					}
					if (found) {
						culture = culture.getObj("Varianten").getObj(mods.getString(mods.size() - 2)).getObj("Varianten")
								.getObj(mods.getString(mods.size() - 1));
					}
				}
				break;
			}
		}

		if (mods.size() > 0) {
			bio.put("Kultur:Modifikation", mods);
		}

		if (ml != null) {
			ml = replaceTalent(ml);
			hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(ml).put("Muttersprache", true);
		} else if (culture != null) {
			JSONArray mlChoices = null;
			while (culture != null && mlChoices == null) {
				mlChoices = culture.getObj("Sprachen").getArrOrDefault("Muttersprache", null);
				culture = (JSONObject) culture.getParent();
				if (culture != null) {
					culture = (JSONObject) culture.getParent();
				}
			}
			if (mlChoices != null) {
				for (int i = 0; i < mlChoices.size(); ++i) {
					final JSONArray mlChoice = mlChoices.getArr(i);
					if (mlChoice.size() == 1) {
						ml = mlChoice.getString(0);
						hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(ml).put("Muttersprache", true);
					}
				}
			}
		}

		if (sl != null) {
			sl = replaceTalent(sl);
			hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(sl).put("Zweitsprache", true);
		} else if (culture != null) {
			JSONArray slChoices = null;
			while (culture != null && slChoices == null) {
				slChoices = culture.getObj("Sprachen").getArrOrDefault("Zweitsprache", null);
				culture = (JSONObject) culture.getParent();
				if (culture != null) {
					culture = (JSONObject) culture.getParent();
				}
			}
			if (slChoices != null) {
				for (int i = 0; i < slChoices.size(); ++i) {
					final JSONArray slChoice = slChoices.getArr(i);
					if (slChoice.size() == 1) {
						hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(slChoice.getString(0)).put("Zweitsprache", true);
					}
				}
			}
		}

		if (mlWriting != null) {
			mlWriting = replaceTalent(mlWriting);
			hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(mlWriting).put("Muttersprache", true);
		} else if (culture != null) {
			JSONObject mlWritings = null;
			while (culture != null && mlWritings == null) {
				mlWritings = culture.getObj("Sprachen").getObjOrDefault("Muttersprache:Schrift", null);
				culture = (JSONObject) culture.getParent();
				if (culture != null) {
					culture = (JSONObject) culture.getParent();
				}
			}
			if (mlWritings != null && mlWritings.containsKey(ml)) {
				hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(mlWritings.getString(ml)).put("Muttersprache", true);
			}
		}
	}

	private void parseFight() {
		final JSONObject actualTalents = hero.getObj("Talente");

		apply("kampf", new Tuple<>("kampfwerte", () -> {
			final String name = replaceTalent(parser.getAttributeValue("", "name"));
			final String group = HeroUtil.findTalent(name)._2;
			if (group != null) {
				final JSONObject talent = actualTalents.getObj(group).getObj(name);
				final Map<String, String> values = extract("kampfwerte", new Tuple3<>("attacke", () -> "at", () -> get("value")),
						new Tuple3<>("parade", () -> "pa", () -> get("value")));
				final int actualAt = Integer.parseInt(values.getOrDefault("at", "0")) - at;
				final int actualPa = Integer.parseInt(values.getOrDefault("pa", "0")) - pa;
				final int taw = talent.getIntOrDefault("TaW", 0);
				if (actualAt + actualPa == taw) {
					talent.put("AT", actualAt);
					talent.put("PA", actualPa);
				} else {
					talent.put("AT", (taw + 1) / 2);
					talent.put("PA", taw / 2);
				}
			}
		}));
	}

	private JSONObject parseHero() {
		final String name = get("name");
		int endFirstName = name.indexOf(' ');
		if (endFirstName == -1) {
			endFirstName = name.length();
		}
		final String firstName = name.substring(0, endFirstName);

		hero = new JSONObject(null);

		// Ensure nice order of entries
		hero.put("Spieler", hero.getStringOrDefault("Spieler", ""));
		hero.getObj("Biografie");
		hero.getObj("Eigenschaften");
		hero.getObj("Basiswerte");
		hero.getObj("Vorteile");
		hero.getObj("Nachteile");
		hero.getObj("Sonderfertigkeiten");
		hero.getObj("Verbilligte Sonderfertigkeiten");
		hero.getObj("Talente");

		final JSONObject bio = new JSONObject(hero);
		hero.put("Biografie", bio);

		bio.put("Vorname", firstName);
		if (endFirstName != name.length()) {
			bio.put("Nachname", name.substring(endFirstName + 1));
		}

		apply("held", new Tuple<>("basis", () -> parseBasis()), new Tuple<>("eigenschaften", () -> parseAttributes()), new Tuple<>("vt", () -> parseProsCons()),
				new Tuple<>("sf", () -> parseSkills()), new Tuple<>("ereignisse", () -> parseHistory()), new Tuple<>("talentliste", () -> parseTalents()),
				new Tuple<>("zauberliste", () -> parseSpells()),
				new Tuple<>("kampf", () -> parseFight()), new Tuple<>("gegenstände", () -> parseInventory()), new Tuple<>("geldboerse", () -> parseMoney()));

		cleanupCheaperSkills();

		ResourceManager.moveResource(hero, "characters/" + firstName);
		return hero;
	}

	private void parseHeroes(final List<JSONObject> heroes) {
		apply("helden", new Tuple<>("held", () -> heroes.add(parseHero())));
	}

	private void parseHistory() {
		final JSONArray history = hero.getArr("Historie");

		final int[] ap = new int[] { 0 };

		apply("ereignisse", new Tuple<>("ereignis", () -> {
			try {

				final String type = get("text");

				JSONObject historyEntry = null;
				final LocalDate entryDate = LocalDate.from(Instant.ofEpochMilli(Long.parseLong(get("time"))).atZone(ZoneId.systemDefault()));

				switch (type) {
					case "Talent steigern":
					case "Nahkampftalent steigern":
						String talentName = replaceTalent(get("obj"));

						historyEntry = new JSONObject(history);

						historyEntry.put("Typ", "Talent");

						if (talentName.startsWith("Kraftschub")) {
							historyEntry.put("Auswahl", talentName.substring(12, talentName.length() - 1));
							talentName = "Kräfteschub";
						} else if (talentName.startsWith("Talentschub")) {
							historyEntry.put("Auswahl", talentName.substring(13, talentName.length() - 1));
							talentName = "Talentschub";
						}

						historyEntry.put("Talent", talentName);

						final String oldV = get("Alt");
						final int oldIndex = oldV.indexOf(';');
						historyEntry.put("Von", Integer.parseInt(oldIndex < 0 ? oldV : oldV.substring(0, oldIndex)));
						final String newV = get("Neu");
						final int newIndex = newV.indexOf(';');
						historyEntry.put("Auf", Integer.parseInt(newIndex < 0 ? newV : newV.substring(0, newIndex)));

						String talentMethod = get("Info");
						if (talentMethod == null) {
							historyEntry = null;
							break;
						}
						if (talentMethod.startsWith("SE")) {
							historyEntry.put("SEs", 1);
							talentMethod = talentMethod.substring(4);
						}
						if ("Freie Steigerung".equals(talentMethod)) {
							historyEntry.put("Methode", "Gegenseitiges Lehren");
							historyEntry.put("AP", 0);
						} else {
							historyEntry.put("Methode", talentMethod);
							final String talentAp = get("Abenteuerpunkte");
							historyEntry.put("AP", talentAp != null ? -Integer.parseInt(talentAp) : 0);
						}

						final JSONObject lastPossibleTalent = history.size() == 0 ? null : history.getObj(history.size() - 1);
						if (lastPossibleTalent != null && "Talent".equals(lastPossibleTalent.getString("Typ"))
								&& entryDate.equals(LocalDate.parse(lastPossibleTalent.getString("Datum")))
								&& lastPossibleTalent.getString("Talent").equals(talentName)
								&& (!lastPossibleTalent.containsKey("Auswahl")
										|| lastPossibleTalent.getString("Auswahl").equals(historyEntry.getString("Auswahl")))
								&& (!lastPossibleTalent.containsKey("Freitext")
										|| lastPossibleTalent.getString("Freitext").equals(historyEntry.getString("Freitext")))
								&& lastPossibleTalent.getString("Methode").equals(historyEntry.getString("Methode"))
								&& lastPossibleTalent.getInt("Auf") == historyEntry.getInt("Von")) {
							lastPossibleTalent.put("Auf", historyEntry.getInt("Auf"));
							if (historyEntry.containsKey("SEs")) {
								lastPossibleTalent.put("SEs", lastPossibleTalent.getIntOrDefault("SEs", 0) + historyEntry.getInt("SEs"));
							}
							lastPossibleTalent.put("AP", lastPossibleTalent.getInt("AP") + historyEntry.getInt("AP"));
							historyEntry = null;
						}

						break;
					case "Talent aktivieren":
						String activatedTalentName = replaceTalent(get("obj"));

						historyEntry = new JSONObject(history);

						historyEntry.put("Typ", "Talent");
						if (activatedTalentName.startsWith("Kraftschub")) {
							historyEntry.put("Auswahl", activatedTalentName.substring(12, activatedTalentName.length() - 1));
							activatedTalentName = "Kräfteschub";
						} else if (activatedTalentName.startsWith("Talentschub")) {
							historyEntry.put("Auswahl", activatedTalentName.substring(13, activatedTalentName.length() - 1));
							activatedTalentName = "Talentschub";
						}

						historyEntry.put("Talent", activatedTalentName);

						historyEntry.put("Auf", 0);

						if ("SE".equals(get("Info"))) {
							historyEntry.put("SEs", 1);
						}

						historyEntry.put("Methode", "Gegenseitiges Lehren");
						final String activatedTalentAp = get("Abenteuerpunkte");
						historyEntry.put("AP", activatedTalentAp != null ? -Integer.parseInt(activatedTalentAp) : 0);

						break;
					case "Zauber steigern":
						historyEntry = new JSONObject(history);

						final String spellName = get("obj");
						String spell = spellName.substring(0, spellName.indexOf('[') - 1);
						spell = spellReplacements.getStringOrDefault(spell, spell);
						if ("Dämonenbann".equals(spell)) {
							spell = spellName.substring(spellName.lastIndexOf('[') + 1, spellName.lastIndexOf(']')) + "bann";
						} else if ("Adlerschwinge Wolfsgestalt".equals(spell)) {
							historyEntry.put("Freitext", spellName.substring(spellName.lastIndexOf('[') + 1, spellName.lastIndexOf(']')));
						}

						historyEntry.put("Typ", "Zauber");
						historyEntry.put("Zauber", spell);
						historyEntry.put("Repräsentation",
								shortRepReplacements.getStringOrDefault(spellName.substring(spellName.indexOf('[') + 1, spellName.indexOf(']')), "Mag"));

						historyEntry.put("Von", Integer.parseInt(get("Alt")));
						historyEntry.put("Auf", Integer.parseInt(get("Neu")));

						String spellMethod = get("Info");
						if (spellMethod == null) {
							historyEntry = null;
							break;
						}
						if (spellMethod.startsWith("SE")) {
							historyEntry.put("SEs", 1);
							spellMethod = spellMethod.substring(4);
						}
						if ("Freie Steigerung".equals(spellMethod)) {
							historyEntry.put("Methode", "Gegenseitiges Lehren");
							historyEntry.put("AP", 0);
						} else {
							historyEntry.put("Methode", spellMethod);
							final String spellAp = get("Abenteuerpunkte");
							historyEntry.put("AP", spellAp != null ? -Integer.parseInt(spellAp) : 0);
						}

						final JSONObject lastPossibleSpell = history.size() == 0 ? null : history.getObj(history.size() - 1);
						if (lastPossibleSpell != null && "Zauber".equals(lastPossibleSpell.getString("Typ"))
								&& entryDate.equals(LocalDate.parse(lastPossibleSpell.getString("Datum")))
								&& lastPossibleSpell.getString("Zauber").equals(spell)
								&& lastPossibleSpell.getString("Repräsentation").equals(historyEntry.getString("Repräsentation"))
								&& (!lastPossibleSpell.containsKey("Auswahl")
										|| lastPossibleSpell.getString("Auswahl").equals(historyEntry.getString("Auswahl")))
								&& (!lastPossibleSpell.containsKey("Freitext")
										|| lastPossibleSpell.getString("Freitext").equals(historyEntry.getString("Freitext")))
								&& lastPossibleSpell.getString("Methode").equals(historyEntry.getString("Methode"))
								&& lastPossibleSpell.getInt("Auf") == historyEntry.getInt("Von")) {
							lastPossibleSpell.put("Auf", historyEntry.getInt("Auf"));
							if (historyEntry.containsKey("SEs")) {
								lastPossibleSpell.put("SEs", lastPossibleSpell.getIntOrDefault("SEs", 0) + historyEntry.getInt("SEs"));
							}
							lastPossibleSpell.put("AP", lastPossibleSpell.getInt("AP") + historyEntry.getInt("AP"));
							historyEntry = null;
						}

						break;
					case "Zauber aktivieren":
						historyEntry = new JSONObject(history);

						final String activatedSpellName = get("obj");
						String activatedSpell = activatedSpellName.substring(0, activatedSpellName.indexOf('[') - 1);
						activatedSpell = spellReplacements.getStringOrDefault(activatedSpell, activatedSpell);
						if ("Dämonenbann".equals(activatedSpell)) {
							activatedSpell = activatedSpellName.substring(activatedSpellName.lastIndexOf('[') + 1, activatedSpellName.lastIndexOf(']'))
									+ "bann";
						} else if ("Adlerschwinge Wolfsgestalt".equals(activatedSpell)) {
							historyEntry.put("Freitext",
									activatedSpellName.substring(activatedSpellName.lastIndexOf('[') + 1, activatedSpellName.lastIndexOf(']')));
						}

						historyEntry.put("Typ", "Zauber");
						historyEntry.put("Zauber", activatedSpell);
						historyEntry.put("Repräsentation",
								shortRepReplacements.getStringOrDefault(
										activatedSpellName.substring(activatedSpellName.indexOf('[') + 1, activatedSpellName.indexOf(']')),
										"Mag"));

						historyEntry.put("Auf", 0);

						if ("SE".equals(get("Info"))) {
							historyEntry.put("SEs", 1);
						}

						historyEntry.put("Methode", "Gegenseitiges Lehren");
						final String activatedSpellAp = get("Abenteuerpunkte");
						historyEntry.put("AP", activatedSpellAp != null ? -Integer.parseInt(activatedSpellAp) : 0);

						break;
					case "Eigenschaft steigern":
						final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
						String attributeName = get("obj");

						if (Set.of("Sozialstatus", "Attacke", "Parade", "Fernkampf-Basis").contains(attributeName)) {
							break;
						}

						historyEntry = new JSONObject(history);

						String entryType = "Basiswert";
						for (final String attribute : attributes.keySet()) {
							if (attributes.getObj(attribute).getString("Name").equals(attributeName)) {
								attributeName = attribute;
								entryType = "Eigenschaft";
								break;
							}
						}

						historyEntry.put("Typ", entryType);
						historyEntry.put(entryType, attributeName);

						historyEntry.put("Von", Integer.parseInt(get("Alt")));
						historyEntry.put("Auf", Integer.parseInt(get("Neu")));

						if ("SE".equals(get("Info"))) {
							historyEntry.put("SEs", 1);
						}
						final String attributeAp = get("Abenteuerpunkte");
						historyEntry.put("AP", attributeAp != null ? -Integer.parseInt(attributeAp) : 0);

						final JSONObject lastPossibleAttribute = history.size() == 0 ? null : history.getObj(history.size() - 1);
						if (lastPossibleAttribute != null && entryType.equals(lastPossibleAttribute.getString("Typ"))
								&& entryDate.equals(LocalDate.parse(lastPossibleAttribute.getString("Datum")))
								&& lastPossibleAttribute.getString(entryType).equals(attributeName)
								&& lastPossibleAttribute.getInt("Auf") == historyEntry.getInt("Von")) {
							lastPossibleAttribute.put("Auf", historyEntry.getInt("Auf"));
							if (historyEntry.containsKey("SEs")) {
								lastPossibleAttribute.put("SEs", lastPossibleAttribute.getIntOrDefault("SEs", 0) + historyEntry.getInt("SEs"));
							}
							lastPossibleAttribute.put("AP", lastPossibleAttribute.getInt("AP") + historyEntry.getInt("AP"));
							historyEntry = null;
						}

						break;
					case "Sonderfertigkeit hinzugefügt":
						historyEntry = new JSONObject(history);
						historyEntry.put("Typ", "Sonderfertigkeit");
						String skillName = replaceSkill(get("obj"));

						if (skillName.startsWith("Talentspezialisierung")) {
							final String talent = replaceTalent(skillName.substring(22, skillName.indexOf('(') - 1));
							historyEntry.put("Auswahl", talent);
							final String specialization = skillName.substring(skillName.indexOf('(') + 1, skillName.lastIndexOf(')'));
							historyEntry.put("Freitext", specialization);
							final JSONObject talents = ResourceManager.getResource("data/Talente");
							if (talents.getObj("Nahkampftalente").containsKey(talent) || talents.getObj("Fernkampftalente").containsKey(talent)) {
								skillName = "Waffenspezialisierung";
							} else {
								skillName = "Talentspezialisierung";
							}
						} else if (skillName.startsWith("Zauberspezialisierung")) {
							String spellChoice = skillName.substring(22, skillName.indexOf('[') - 1);
							spellChoice = spellReplacements.getStringOrDefault(spellChoice, spellChoice);
							if ("Dämonenbann".equals(spellChoice)) {
								spellChoice = skillName.substring(skillName.lastIndexOf('[') + 1, skillName.lastIndexOf(']')) + "bann";
							}
							historyEntry.put("Auswahl", spellChoice);
							final String specialization = skillName.substring(skillName.indexOf('(') + 1, skillName.lastIndexOf(')'));
							historyEntry.put("Freitext", specialization);
							skillName = "Zauberspezialisierung";
						} else if (skillName.startsWith("Merkmalskenntnis")) {
							String trait = skillName.substring(18);
							if ("Elementar".equals(trait)) {
								trait = "Elementar (gesamt)";
							} else if ("Dämonisch".equals(trait)) {
								trait = "Dämonisch (gesamt)";
							}
							historyEntry.put("Auswahl", trait);
							skillName = "Merkmalskenntnis";
						} else if (skillName.startsWith("Repräsentation")) {
							String rep = skillName.substring(16);
							rep = representationReplacements.getStringOrDefault(rep, rep);
							historyEntry.put("Auswahl", rep);
							skillName = "Repräsentation";
						} else if (skillName.startsWith("Ritualkenntnis")) {
							String choice = skillName.substring(16);
							choice = ritualKnowledge.getStringOrDefault(choice, choice);
							historyEntry.put("Auswahl", choice);
							skillName = "Ritualkenntnis";
						} else if (skillName.startsWith("Liturgiekenntnis")) {
							String goddess = skillName.substring(18, skillName.length() - 1);
							if ("Boron".equals(goddess)) {
								goddess = selectBoronCult();
							}
							historyEntry.put("Auswahl", goddess);
							skillName = "Liturgiekenntnis";
						} else if (skillName.startsWith("Kulturkunde")) {
							final String culture = skillName.substring(13, skillName.length() - 1);
							historyEntry.put("Auswahl", culture);
							skillName = "Kulturkunde";
						} else if (skillName.startsWith("Ortskenntnis")) {
							final String location = skillName.substring(14, skillName.length() - 1);
							historyEntry.put("Freitext", location);
							skillName = "Ortskenntnis";
						} else if (skillName.startsWith("Berufsgeheimnis")) {
							final String text = skillName.substring(skillName.lastIndexOf(';') + 2, skillName.length() - 1);
							historyEntry.put("Freitext", text);
							skillName = "Berufsgeheimnis";
						} else if (skillName.startsWith("Scharfschütze")) {
							final String talent = skillName.substring(15, skillName.length() - 1);
							historyEntry.put("Auswahl", talent);
							skillName = "Scharfschütze";
						} else if (skillName.startsWith("Meisterschütze")) {
							final String talent = skillName.substring(16, skillName.length() - 1);
							historyEntry.put("Auswahl", talent);
							skillName = "Meisterschütze";
						} else if (skillName.startsWith("Rüstungsgewöhnung I") && !skillName.startsWith("Rüstungsgewöhnung II")) {
							final String armor = skillName.substring(21, skillName.length() - 1);
							historyEntry.put("Freitext", armor);
							skillName = "Rüstungsgewöhnung I";
						} else if (skillName.startsWith("Waffenmeister") && !"Waffenmeister (Schild)".equals(skillName)) {
							final int talentIndex = skillName.indexOf(';') + 1;
							final String talent = skillName.substring(talentIndex + 1, skillName.indexOf(';', talentIndex));
							historyEntry.put("Auswahl", talent);
							final String weapon = skillName.substring(15, talentIndex - 1);
							historyEntry.put("Freitext", weapon);
							skillName = "Waffenmeister";
						} else if (skillName.startsWith("Wahrer Name")) {
							final int detailsIndex = skillName.indexOf('(');
							final String quality = skillName.substring(detailsIndex + 2, skillName.indexOf(' ', detailsIndex));
							historyEntry.put("Stufe", Integer.valueOf(quality));
							final String being = skillName.substring(skillName.lastIndexOf(' ') + 1, skillName.indexOf(')', detailsIndex));
							historyEntry.put("Freitext", being);
							skillName = "Wahre Namen";
						} else if (skillName.startsWith("Akoluth")) {
							String goddess = skillName.substring(9, skillName.length() - 1);
							if ("Boron".equals(goddess)) {
								goddess = selectBoronCult();
							}
							historyEntry.put("Auswahl", goddess);
							skillName = "Akoluth";
						}

						final JSONObject skill = HeroUtil.findSkill(skillName);
						if (skill != null) {
							if (skill.containsKey("Auswahl") && !historyEntry.containsKey("Auswahl")) {
								final int choiceIndex = skillName.indexOf('(');
								historyEntry.put("Auswahl", skillName.substring(choiceIndex + 1, skillName.length() - 1));
								skillName = skillName.substring(0, choiceIndex - 1);
							} else if (skill.containsKey("Freitext") && !historyEntry.containsKey("Freitext")) {
								final int textIndex = skillName.indexOf('(');
								historyEntry.put("Freitext", skillName.substring(textIndex + 1, skillName.length() - 1));
								skillName = skillName.substring(0, textIndex - 1);
							}
						}

						historyEntry.put("Sonderfertigkeit", skillName);
						final String skillAp = get("Abenteuerpunkte");
						historyEntry.put("AP", skillAp != null ? -Integer.parseInt(skillAp) : 0);
						break;
					case "Abenteuerpunkte":
						if (get("obj") == null || "gesamt".equals(get("obj"))) {
							historyEntry = new JSONObject(history);
							historyEntry.put("Typ", "Abenteuerpunkte");
							historyEntry.put("Von", Integer.parseInt(get("Alt")));
							final int newAP = Integer.parseInt(get("Neu"));
							historyEntry.put("Auf", newAP);
							ap[0] = newAP;
						}
						break;
					case "Ereignis eingeben":
						final String event = get("obj");
						if ("Abenteuerpunkte (Hinzugewinn)".equals(event) && ap[0] != 0) {
							historyEntry = new JSONObject(history);
							historyEntry.put("Typ", "Abenteuerpunkte");
							historyEntry.put("Von", ap[0]);
							ap[0] += Integer.parseInt(get("Abenteuerpunkte"));
							historyEntry.put("Auf", ap[0]);
						}
						break;
				}

				if (historyEntry != null) {
					historyEntry.put("Datum", entryDate.toString());
					history.add(historyEntry);
				}
			} catch (final Exception e) {
				ErrorLogger.logError(e);
			}
		}));
	}

	private void parseInventory() {
		final JSONObject equipment = ResourceManager.getResource("data/Ausruestung");
		final JSONArray inventory = hero.getObj("Besitz").getArr("Ausrüstung");

		apply("gegenstände", new Tuple<>("gegenstand", () -> {
			final String name = parser.getAttributeValue("", "name");
			final JSONObject item = equipment.containsKey(name) ? equipment.getObj(name).clone(inventory) : new JSONObject(inventory);

			item.put("Name", name);
			inventory.add(item);

			apply("gegenstand", new Tuple<>("modallgemein", () -> {
				final Map<String, String> values = extract("modallgemein", new Tuple3<>("name", () -> "Name", () -> get("value")),
						new Tuple3<>("gewicht", () -> "Gewicht", () -> get("value")));
				item.put("Name", values.getOrDefault("Name", name));
				if (values.containsKey("Gewicht")) {
					item.put("Gewicht", Double.parseDouble(values.get("Gewicht")) / 40);
				}
			}), new Tuple<>("Nahkampfwaffe", () -> {
				if (item.containsKey("Waffentypen") && item.getArr("Waffentypen").size() > 1) {
					final Set<String> values = extract("Nahkampfwaffe", new Tuple3<>("talente", () -> get("kampftalent"), () -> null)).keySet();
					if (!values.isEmpty()) {
						item.put("Waffentyp:Primär", values.iterator().next());
					}
				}
			}), new Tuple<>("Fernkampfwaffe", () -> {
				if (item.containsKey("Waffentypen") && item.getArr("Waffentypen").size() > 1) {
					final Set<String> values = extract("Fernkampfwaffe", new Tuple3<>("talente", () -> get("kampftalent"), () -> null)).keySet();
					if (!values.isEmpty()) {
						item.put("Waffentyp:Primär", values.iterator().next());
					}
				}
			}));
		}));
	}

	private void parseMoney() {
		final JSONObject money = hero.getObj("Besitz").getObj("Geld");

		final Map<String, String> values = extract("geldboerse", new Tuple3<>("muenze", () -> get("name"), () -> get("anzahl")));
		for (String coin : values.keySet()) {
			final int amount = Integer.parseInt(values.get(coin));
			if ("Dukat".equals(coin)) {
				coin = "Dukaten";
			}
			money.put(coin, amount);
		}
	}

	private void parseProfession() {
		final JSONObject bio = hero.getObj("Biografie");
		final JSONObject pros = hero.getObj("Vorteile");

		apply("ausbildungen", new Tuple<>("ausbildung", () -> {
			switch (get("art")) {
				case "Hauptprofession":
					final String replaced = getLastPart(get("name"), professionReplacements);
					final String[] split = replaced.split("\\|");
					String professionName = split[0];
					bio.put("Profession", professionName);

					String tl = get("zweitsprache");

					JSONObject profession = ResourceManager.getResource("data/Professionen").getObj(professionName);

					final List<String> variants = new ArrayList<>();
					for (int i = 1; i < split.length; ++i) {
						variants.add(split[i]);
					}

					final String finalName = professionName;
					apply("ausbildung", new Tuple<>("variante", () -> {
						final String name = get("name");
						if ("Fähnrich zur See".equals(finalName) && "Fähnrich zur See".equals(name)) {
							variants.add(ask("Variante auswählen", "Variante für Profession Fähnrich zur See auswählen", "Grangor", "Grangor", "Methumis",
									"Methumis", "Perricum", "Perricum"));
						} else if ("Geode".equals(finalName) && "Brobim".equals(name)) {
							variants.add("Brobim-Geode");
						} else if ("Tänzer".equals(finalName) && "Gaukler".equals(name)) {
							variants.add("Gaukler-Tänzer");
						} else {
							for (final String variant : professionReplacements.getStringOrDefault(name, name).split("\\|")) {
								final String var = switch (variant) {
									case "Gareth oder Arivor" -> ask("Variante auswählen", "Variante für Profession Fähnrich der Kavallerie auswählen",
											"Gareth", "Gareth", "Arivor", "Arivor");
									case "Bergungs-, Schwamm- oder Korallentaucher" -> ask("Variante auswählen", "Variante für Profession Fischer auswählen",
											"Bergungstaucher", "Bergungstaucher", "Schwammtaucher", "Schwammtaucher", "Korallentaucher", "Korallentaucher");
									case "Goldsucher oder Prospektor" -> ask("Variante auswählen", "Variante für Profession Prospektor auswählen", "Prospektor",
											"Prospektor", "Goldsucher", "Goldsucher");
									case "Walfänger/Haijäger" -> ask("Variante auswählen", "Variante für Profession Seefahrer auswählen", "Walfänger",
											"Wahlfänger", "Haijäger", "Haijäger");
									case "Baumeister/Deichmeister" -> ask("Variante auswählen", "Variante für Profession Edelhandwerker auswählen",
											"Baumeister", "Baumeister", "Deichmeister", "Deichmeister");
									case "Hüttenkundiger/Bronzegießer/Eisengießer" -> ask("Variante auswählen",
											"Variante für Profession Edelhandwerker auswählen", "Hüttenkundiger", "Hüttenkundiger", "Bronzegießer",
											"Bronzegießer", "Eisengießer", "Eisengießer");
									case "Philosoph/Metaphysiker" -> ask("Variante auswählen", "Variante für Profession Gelehrter auswählen", "Philosoph",
											"Philosoph", "Metaphysiker", "Metaphysiker");
									case "Völkerkundler/Sagenkundler" -> ask("Variante auswählen", "Variante für Profession Gelehrter auswählen",
											"Völkerkundler", "Völkerkundler", "Sagenkundler", "Sagenkundler");
									case "Quacksalber/Zahnreißer" -> ask("Variante auswählen", "Variante für Profession Wundarzt auswählen", "Quacksalber",
											"Quacksalber", "Zahnreißer", "Zahnreißer");
									default -> variant;
								};
								variants.add(var);
							}
						}
					}));

					if ("Stammeskrieger".equals(professionName) || "Geweihter".equals(professionName) || "Ordenskrieger".equals(professionName)
							|| "Schamane".equals(professionName)) {
						final String variant = variants.remove(0);
						final String replacedVariant = professionReplacements.getStringOrDefault(variant, variant);
						final String[] splitVariant = replacedVariant.split("\\|");
						professionName = splitVariant[0];
						bio.put("Profession", professionName);

						profession = ResourceManager.getResource("data/Professionen").getObj(professionName);
						for (int i = 1; i < split.length; ++i) {
							variants.add(split[i]);
						}
					}

					final JSONArray mods = new JSONArray(bio);

					while (!variants.isEmpty() && profession != null && profession.containsKey("Varianten")) {
						boolean found = false;
						for (int i = 0; i < variants.size(); ++i) {
							if (profession.containsKey("Varianten")) {
								final String name = variants.get(i);
								if (profession.getObj("Varianten").containsKey(name)) {
									found = true;
									profession = profession.getObj("Varianten").getObj(name);
									if (mods.size() == 0 && !professionName.equals(name) || mods.size() > 0 && !mods.getString(mods.size() - 1).equals(name)) {
										mods.add(name);
									}
									variants.remove(i);
									i--;
								}
							} else {
								break;
							}
						}
						if (!found) {
							if (!variants.isEmpty() && profession.containsKey("Varianten")) {
								final String name = variants.remove(0);
								for (final String variantName : profession.getObj("Varianten").keySet()) {
									final JSONObject variant = profession.getObj("Varianten").getObj(variantName);
									if (variant.containsKey("Varianten") && variant.getObj("Varianten").containsKey(name)) {
										if (found) {
											found = false;
											mods.removeAt(mods.size() - 1);
											mods.removeAt(mods.size() - 1);
											break;
										} else {
											found = true;
											mods.add(variantName);
											mods.add(name);
										}
									}
								}
								if (found) {
									profession = profession.getObj("Varianten").getObj(mods.getString(mods.size() - 2)).getObj("Varianten")
											.getObj(mods.getString(mods.size() - 1));
								}
							}
							break;
						}
					}

					if (mods.size() > 0) {
						bio.put("Profession:Modifikation", mods);
					}

					if (tl != null) {
						tl = replaceTalent(tl);
						hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(tl).put("Lehrsprache", true);
					} else if (profession != null) {
						JSONArray tlChoices = null;
						JSONObject prof = profession;
						while (prof != null && tlChoices == null) {
							tlChoices = prof.getObj("Sprachen").getArrOrDefault("Lehrsprache", null);
							prof = (JSONObject) prof.getParent();
							if (prof != null) {
								prof = (JSONObject) prof.getParent();
							}
						}
						if (tlChoices != null) {
							for (int i = 0; i < tlChoices.size(); ++i) {
								final JSONArray slChoice = tlChoices.getArr(i);
								if (slChoice.size() == 1) {
									hero.getObj("Talente").getObj("Sprachen und Schriften").getObj(slChoice.getString(0)).put("Lehrsprache", true);
								}
							}
						}
					}

					JSONObject cheaperSkills = null;
					JSONObject prof = profession;
					while (prof != null && cheaperSkills == null) {
						cheaperSkills = prof.getObjOrDefault("Verbilligte Sonderfertigkeiten", null);
						prof = (JSONObject) prof.getParent();
						if (prof != null) {
							prof = (JSONObject) prof.getParent();
						}
					}
					if (cheaperSkills != null) {
						final JSONObject actualCheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
						for (final String skillName : cheaperSkills.keySet()) {
							if ("Wahl".equals(skillName)) {
								continue;
							}
							final JSONObject skill = HeroUtil.findSkill(skillName);
							if (skill == null) {
								continue;
							}
							if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
								final JSONArray cheaperSkill = cheaperSkills.getArr(skillName);
								for (int i = 0; i < cheaperSkill.size(); ++i) {
									final JSONArray actualSkill = actualCheaperSkills.getArr(skillName);
									actualSkill.add(cheaperSkill.getObj(i).clone(actualSkill));
								}
							} else {
								actualCheaperSkills.put(skillName, cheaperSkills.getObj(skillName).clone(actualCheaperSkills));
							}
						}
					}

					break;
				case "BGB":
				case "Veteran":
					final JSONObject pro = new JSONObject(pros);
					final boolean isBGB = "BGB".equals(get("art"));
					pros.put(isBGB ? "Breitgefächerte Bildung" : "Veteran", pro);

					final String replacedProName = getLastPart(get("name"), professionReplacements);
					final String[] splitProName = replacedProName.split("\\|");
					String proName = splitProName[0];
					if (isBGB) {
						pro.put("Profession", proName);
					}

					JSONObject proProf = ResourceManager.getResource("data/Professionen").getObj(proName);

					final List<String> proVariants = new ArrayList<>();
					for (int i = 1; i < splitProName.length; ++i) {
						proVariants.add(splitProName[i]);
					}

					final String finalProName = proName;
					apply("ausbildung", new Tuple<>("variante", () -> {
						final String name = get("name");
						if ("Fähnrich zur See".equals(finalProName) && "Fähnrich zur See".equals(name)) {
							proVariants.add("Grangor");
						} else if ("Geode".equals(finalProName) && "Brobim".equals(name)) {
							proVariants.add("Brobim-Geode");
						} else if ("Tänzer".equals(finalProName) && "Gaukler".equals(name)) {
							proVariants.add("Gaukler-Tänzer");
						} else {
							for (final String variant : professionReplacements.getStringOrDefault(name, name).split("\\|")) {
								proVariants.add(variant);
							}
						}
					}));

					if ("Stammeskrieger".equals(proName) || "Geweihter".equals(proName) || "Ordenskrieger".equals(proName) || "Schamane".equals(proName)) {
						final String variant = proVariants.remove(0);
						final String replacedVariant = professionReplacements.getStringOrDefault(variant, variant);
						final String[] splitVariant = replacedVariant.split("\\|");
						proName = splitVariant[0];

						if (isBGB) {
							pro.put("Profession", proName);
						}

						proProf = ResourceManager.getResource("data/Professionen").getObj(proName);
						for (int i = 1; i < splitProName.length; ++i) {
							proVariants.add(splitProName[i]);
						}
					}

					final JSONArray proMods = new JSONArray(bio);

					while (!proVariants.isEmpty() && proProf != null && proProf.containsKey("Varianten")) {
						boolean found = false;
						for (int i = 0; i < proVariants.size(); ++i) {
							if (proProf.containsKey("Varianten")) {
								final String name = proVariants.get(i);
								if (proProf.getObj("Varianten").containsKey(name)) {
									found = true;
									proProf = proProf.getObj("Varianten").getObj(name);
									if (proMods.size() == 0 && !proName.equals(name)
											|| proMods.size() > 0 && !proMods.getString(proMods.size() - 1).equals(name)) {
										proMods.add(name);
									}
									proVariants.remove(i);
									i--;
								}
							} else {
								break;
							}
						}
						if (!found) {
							if (!proVariants.isEmpty() && proProf.containsKey("Varianten")) {
								final String name = proVariants.remove(0);
								for (final String variantName : proProf.getObj("Varianten").keySet()) {
									final JSONObject variant = proProf.getObj("Varianten").getObj(variantName);
									if (variant.containsKey("Varianten") && variant.getObj("Varianten").containsKey(name)) {
										if (found) {
											found = false;
											proMods.removeAt(proMods.size() - 1);
											proMods.removeAt(proMods.size() - 1);
											break;
										} else {
											found = true;
											proMods.add(variantName);
											proMods.add(name);
										}
									}
								}
								if (found) {
									proProf = proProf.getObj("Varianten").getObj(proMods.getString(proMods.size() - 2)).getObj("Varianten")
											.getObj(proMods.getString(proMods.size() - 1));
								}
							}
							break;
						}
					}

					if (proMods.size() > 0) {
						pro.put("Profession:Modifikation", proMods);
					}

					if (isBGB) {
						JSONObject cheaperSkillsPro = null;
						JSONObject profCheaperSkills = proProf;
						while (profCheaperSkills != null && cheaperSkillsPro == null) {
							cheaperSkillsPro = profCheaperSkills.getObjOrDefault("Verbilligte Sonderfertigkeiten", null);
							profCheaperSkills = (JSONObject) profCheaperSkills.getParent();
							if (profCheaperSkills != null) {
								profCheaperSkills = (JSONObject) profCheaperSkills.getParent();
							}
						}
						if (cheaperSkillsPro != null) {
							final JSONObject actualCheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
							for (final String skillName : cheaperSkillsPro.keySet()) {
								if ("Wahl".equals(skillName)) {
									continue;
								}
								final JSONObject skill = HeroUtil.findSkill(skillName);
								if (skill == null) {
									continue;
								}
								if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
									final JSONArray cheaperSkill = cheaperSkillsPro.getArr(skillName);
									for (int i = 0; i < cheaperSkill.size(); ++i) {
										final JSONArray actualSkill = actualCheaperSkills.getArr(skillName);
										actualSkill.add(cheaperSkill.getObj(i).clone(actualSkill));
									}
								} else {
									actualCheaperSkills.put(skillName, cheaperSkillsPro.getObj(skillName).clone(actualCheaperSkills));
								}
							}
						}
					}

					JSONObject skillsPro = null;
					JSONObject profSkills = proProf;
					while (profSkills != null && skillsPro == null) {
						skillsPro = profSkills.getObjOrDefault("Sonderfertigkeiten", null);
						profSkills = (JSONObject) profSkills.getParent();
						if (profSkills != null) {
							profSkills = (JSONObject) profSkills.getParent();
						}
					}
					if (skillsPro != null) {
						final JSONObject actualCheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
						for (final String skillName : skillsPro.keySet()) {
							if ("Wahl".equals(skillName)) {
								continue;
							}
							final JSONObject skill = HeroUtil.findSkill(skillName);
							if (skill == null) {
								continue;
							}
							if (skill.containsKey("Auswahl") || skill.containsKey("Freitext")) {
								final JSONArray cheaperSkill = skillsPro.getArr(skillName);
								for (int i = 0; i < cheaperSkill.size(); ++i) {
									final JSONArray actualSkill = actualCheaperSkills.getArr(skillName);
									actualSkill.add(cheaperSkill.getObj(i).clone(actualSkill));
								}
							} else {
								actualCheaperSkills.put(skillName, skillsPro.getObj(skillName).clone(actualCheaperSkills));
							}
						}
					}

					break;
			}
		}));
	}

	private void parseProsCons() {
		final JSONObject actualPros = hero.getObj("Vorteile");
		final JSONObject actualCons = hero.getObj("Nachteile");

		final JSONObject pros = ResourceManager.getResource("data/Vorteile");
		final JSONObject cons = ResourceManager.getResource("data/Nachteile");

		apply("vt", new Tuple<>("vorteil", () -> {
			String name = get("name");
			name = proConReplacements.getStringOrDefault(name, name);
			String choice = get("value");
			String text = get("value");
			String value = get("value");

			if (Set.of("Breitgefächerte Bildung", "Veteran").contains(name))
				return;
			else if (Set.of("Astrale Regeneration", "Schnelle Heilung", "Wesen der Nacht", "Fluch der Finsternis", "Madas Fluch", "Schlafstörungen")
					.contains(name)) {
				name += " " + "III".substring(0, Integer.parseInt(get("value")));
			} else if (name.startsWith("Resistenz") || name.startsWith("Immunität")) {
				if (name.endsWith("Gift")) {
					String prefix = "";
					if (name.startsWith("Immunität")) {
						prefix = "Immunität gegen ";
					} else {
						prefix = "Resistenz gegen ";
					}
					name = switch (text) {
						case "tierische Gifte", "mineralische Gifte", "pflanzliche Gifte", "alchimistische Gifte", "Atemgifte", "Einnahmegifte", "Kontaktgifte", "Blut-/Waffengifte" -> prefix
								+ text;
						case "alle Gifte" -> "Allgemeine " + prefix + "Gifte";
						default -> prefix + "Gift";
					};
				} else {
					if (name.startsWith("Immunität")) {
						if ("alle Krankheiten".equals(text)) {
							name = "Immunität gegen Krankheiten";
						} else {
							name = "Immunität gegen Krankheit";
						}
					} else {
						name = "Resistenz gegen Krankheiten";
					}
				}
			} else if ("Herausragender Sinn".equals(name)) {
				name = switch (get("value")) {
					case "Sicht" -> "Herausragende Sicht";
					case "Geruchssinn" -> "Herausragender Geruchssinn";
					case "Gehör" -> "Herausragendes Gehör";
					case "Tastsinn" -> "Herausragender Tastsinn";
					default -> null;
				};
			} else if ("Wolfskind".equals(name)) {
				if (!"intuitiv".equals(get("value"))) {
					name += ", freiwillige Verwandlung";
				}
			} else if ("Zusätzliche Gliedmaßen".equals(name)) {
				name = "Zusätzliche Gliedmaße (" + get("value") + ")";
			} else if (name.startsWith("Angst") || name.startsWith("Vorurteile") || name.startsWith("Weltfremd")) {
				if (name.startsWith("Angst") && name.charAt(10) != '(') {
					text = name.substring(10);
				} else {
					final Map<String, String> values = extract("vorteil", new Tuple3<>("auswahl", () -> get("position"), () -> get("value")));
					text = values.getOrDefault("1", "");
					value = values.getOrDefault("0", "1");
				}

				if (name.startsWith("Vorurteile")) {
					name = "Vorurteile";
				} else if (name.startsWith("Weltfremd")) {
					name = "Weltfremd";
				} else if (!"Angst vor (häufiger Auslöser)".equals(name)) {
					name = "Angst vor";
				}
			} else if ("Eingeschränkter Sinn".equals(name)) {
				name = switch (get("value")) {
					case "Sicht" -> "Kurzsichtig";
					case "Geruchssinn" -> "Eingeschränkter Geruchssinn";
					case "Gehör" -> "Schwerhörig";
					case "Tastsinn" -> "Eingeschränkter Tastsinn";
					default -> null;
				};
			} else if ("Moralkodex".equals(name)) {
				value = "1";
			} else if ("Sucht".equals(name)) {
				final int valueStart = value.indexOf('(');
				text = value.substring(0, valueStart - 1);
				if ("Alkohol".equals(text)) {
					name = "Sucht (hohe Verfügbarkeit)";
				}
				value = value.substring(valueStart + 1, value.length() - 1);
			}
			choice = replaceTalent(choice);
			choice = spellReplacements.getStringOrDefault(choice, choice);
			choice = replaceSkill(choice);
			choice = groupReplacements.getStringOrDefault(choice, choice);
			choice = choice == null ? null : switch (choice) {
				case "Elementar", "Dämonisch" -> choice + " (gesamt)";
				default -> choice;
			};
			if (pros.containsKey(name)) {
				final JSONObject pro = pros.getObj(name);
				if (pro.containsKey("Freitext") || pro.containsKey("Auswahl")) {
					final JSONArray actualPro = actualPros.getArr(name);
					final JSONObject currentPro = new JSONObject(actualPro);
					actualPro.add(currentPro);
					if (pro.containsKey("Auswahl")) {
						currentPro.put("Auswahl", choice != null && !choice.equals(value) ? choice : pro.getString("Auswahl"));
					}
					if (pro.containsKey("Freitext")) {
						currentPro.put("Freitext", text != null && !text.equals(value) && !text.equals(choice) ? text : pro.getString("Freitext"));
					}
					if (pro.getBoolOrDefault("Abgestuft", false)) {
						if ("Besonderer Besitz".equals(name)) {
							if (value.startsWith("Stufe ")) {
								currentPro.put("Stufe", Integer.parseInt(value.substring(6)));
							}
						} else {
							currentPro.put("Stufe", Integer.parseInt(value));
						}
					}
				} else {
					final JSONObject currentPro = new JSONObject(actualPros);
					actualPros.put(name, currentPro);
					if (pro.getBoolOrDefault("Abgestuft", false)) {
						currentPro.put("Stufe", Integer.parseInt(value));
					}
					if ("Flink".equals(name)) {
						HeroUtil.applyEffect(hero, name, pro, currentPro);
					}
				}
			} else if (cons.containsKey(name)) {
				final JSONObject con = cons.getObj(name);
				if (con.containsKey("Freitext") || con.containsKey("Auswahl")) {
					final JSONArray actualCon = actualCons.getArr(name);
					final JSONObject currentCon = new JSONObject(actualCon);
					actualCon.add(currentCon);
					if (con.containsKey("Auswahl")) {
						currentCon.put("Auswahl", choice != null && !choice.equals(value) ? choice : con.getString("Auswahl"));
					}
					if (con.containsKey("Freitext")) {
						currentCon.put("Freitext", text != null && !text.equals(value) && !text.equals(choice) ? text : con.getString("Freitext"));
					}
					if (con.getBoolOrDefault("Abgestuft", false)) {
						currentCon.put("Stufe", Integer.parseInt(value));
					}
				} else {
					final JSONObject currentCon = new JSONObject(actualCons);
					actualCons.put(name, currentCon);
					if (con.getBoolOrDefault("Abgestuft", false)) {
						currentCon.put("Stufe", Integer.parseInt(value));
					}
					if (Set.of("Behäbig", "Kleinwüchsig", "Lahm", "Zwergenwuchs").contains(name)) {
						HeroUtil.applyEffect(hero, name, con, currentCon);
					}
				}
			}
		}));
	}

	private void parseRace() {
		final JSONObject bio = hero.getObj("Biografie");
		String replaced = getLastPart(get("name"), raceReplacements);
		if ("Erz-/Hügelzwerge".equals(replaced)) {
			replaced = ask("Variante auswählen", "Variante für Rasse Zwerg auswählen", "Erzzwerg", "Erzzwerg", "Hügelzwerg", "Hügelzwerg");
		}
		final String[] split = replaced.split("\\|");
		final String raceName = split[0];
		bio.put("Rasse", raceName);

		JSONObject race = ResourceManager.getResource("data/Rassen").getObj(raceName);

		final List<String> variants = new ArrayList<>();
		for (int i = 1; i < split.length; ++i) {
			variants.add(split[i]);
		}
		apply("rasse", new Tuple<>("variante", () -> {
			final String name = get("name");
			if ("Ork".equals(raceName) && "keine Variante".equals(name)) {
				variants.add("Korogai");
			} else {
				for (final String variant : raceReplacements.getStringOrDefault(name, name).split("\\|")) {
					variants.add(variant);
				}
			}
		}), new Tuple<>("groesse", () -> {
			bio.put("Größe", Integer.parseInt(get("value")));
			bio.put("Gewicht", Integer.parseInt(get("gewicht")));
		}), new Tuple<>("aussehen", () -> {
			bio.put("Augenfarbe", get("augenfarbe"));
			bio.put("Haarfarbe", get("haarfarbe"));
			bio.put("Geburtstag", Integer.parseInt(get("gbtag")));
			bio.put("Geburtsmonat", Integer.parseInt(get("gbmonat")));
			bio.put("Geburtsjahr", Integer.parseInt(get("gbjahr")));
		}), new Tuple<>("variante", () -> {
			final String mod = get("name");
			if (!raceName.equals(mod)) {
				bio.getArr("Rasse:Modifikation").add(mod);
			}
		}));

		final JSONArray mods = new JSONArray(bio);

		while (!variants.isEmpty() && race != null && race.containsKey("Varianten")) {
			boolean found = false;
			for (int i = 0; i < variants.size(); ++i) {
				if (race.containsKey("Varianten")) {
					final String name = variants.get(i);
					if (race.getObj("Varianten").containsKey(name)) {
						found = true;
						race = race.getObj("Varianten").getObj(name);
						if (mods.size() == 0 && !raceName.equals(name) || mods.size() > 0 && !mods.getString(mods.size() - 1).equals(name)) {
							mods.add(name);
						}
						variants.remove(i);
						i--;
					}
				} else {
					break;
				}
			}
			if (!found) {
				if (!variants.isEmpty() && race.containsKey("Varianten")) {
					final String name = variants.remove(0);
					for (final String variantName : race.getObj("Varianten").keySet()) {
						final JSONObject variant = race.getObj("Varianten").getObj(variantName);
						if (variant.containsKey("Varianten") && variant.getObj("Varianten").containsKey(name)) {
							if (found) {
								found = false;
								mods.removeAt(mods.size() - 1);
								mods.removeAt(mods.size() - 1);
								break;
							} else {
								found = true;
								mods.add(variantName);
								mods.add(name);
							}
						}
					}
					if (found) {
						race = race.getObj("Varianten").getObj(mods.getString(mods.size() - 2)).getObj("Varianten").getObj(mods.getString(mods.size() - 1));
					}
				}
				break;
			}
		}

		if (mods.size() > 0) {
			bio.put("Rasse:Modifikation", mods);
		}
	}

	private void parseSkills() {
		final JSONObject talents = ResourceManager.getResource("data/Talente");

		apply("sf", new Tuple<>("sonderfertigkeit", () -> {
			String name = get("name");
			name = replaceSkill(name);
			String choice = get("value");
			String text = get("value");
			if (name.startsWith("Talentspezialisierung")) {
				final Map<String, String> values = extract("sonderfertigkeit", new Tuple3<>("talent", () -> "Talent", () -> get("name")),
						new Tuple3<>("spezialisierung", () -> "Spez", () -> get("name")));
				choice = values.get("Talent");
				choice = replaceTalent(choice);
				text = values.get("Spez");
				if (talents.getObj("Nahkampftalente").containsKey(choice) || talents.getObj("Fernkampftalente").containsKey(choice)) {
					name = "Waffenspezialisierung";
				} else {
					name = "Talentspezialisierung";
				}
			} else if (name.startsWith("Zauberspezialisierung")) {
				name = "Zauberspezialisierung";
				final Map<String, String> values = extract("sonderfertigkeit",
						new Tuple3<>("zauber", () -> "Zauber", () -> get("name") + "|" + get("variante")),
						new Tuple3<>("spezialisierung", () -> "Spez", () -> get("name")));
				final String[] parts = values.get("Zauber").split("\\|");
				choice = parts[0];
				choice = spellReplacements.getStringOrDefault(choice, choice);
				if ("Dämonenbann".equals(choice)) {
					choice = parts[1] + "bann";
				}
				text = values.get("Spez");
			} else if ("Elementarharmonisierte Aura".equals(name)) {
				final Map<String, String> value = extract("sonderfertigkeit", new Tuple3<>("auswahl", () -> "Auswahl", () -> get("name")));
				name = replaceSkill("Elementarharmonisierte Aura (" + value.get("Auswahl") + ")");
			} else if (name.startsWith("Merkmalskenntnis")) {
				choice = name.substring(18);
				if ("Elementar".equals(choice)) {
					choice = "Elementar (gesamt)";
				} else if ("Dämonisch".equals(choice)) {
					choice = "Dämonisch (gesamt)";
				}
				name = "Merkmalskenntnis";
			} else if (name.startsWith("Repräsentation")) {
				choice = name.substring(16);
				choice = representationReplacements.getStringOrDefault(choice, choice);
				name = "Repräsentation";
			} else if (name.startsWith("Ritualkenntnis")) {
				choice = name.substring(16);
				choice = choice.startsWith("Zaubertänzer") ? "Zaubertänze" : ritualKnowledge.getStringOrDefault(choice, choice);
				name = "Ritualkenntnis";
			} else if (name.startsWith("Liturgiekenntnis")) {
				choice = name.substring(18, name.length() - 1);
				if ("Boron".equals(choice)) {
					choice = selectBoronCult();
				}
				name = "Liturgiekenntnis";
			} else if ("Kulturkunde".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("kultur", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, value, "", false, false);
				}
				return;
			} else if ("Ortskenntnis".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("auswahl", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, "", value, false, false);
				}
				return;
			} else if ("Berufsgeheimnis".equals(name)) {
				apply("sonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill("Berufsgeheimnis", 0, "", values.getOrDefault("2", ""), false, false);
				}));
				return;
			} else if ("Scharfschütze".equals(name) || "Meisterschütze".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("talent", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, value, "", false, false);
				}
				return;
			} else if ("Schnellladen".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("talent", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill("Schnellladen (" + value + ")", 0, "", "", false, false);
				}
				return;
			} else if ("Rüstungsgewöhnung I".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("gegenstand", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, "", value, false, false);
				}
				return;
			} else if ("Waffenmeister".equals(name)) {
				apply("sonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					final JSONObject skill = enterSkill("Waffenmeister", 0, values.getOrDefault("1", ""), values.getOrDefault("0", ""), false, false);
					for (int i = 3; i < 99; ++i) {
						if (!values.containsKey(Integer.toString(i))) {
							break;
						}
						final String value = values.get(Integer.toString(i));
						if (value.startsWith("INI-Bonus:")) {
							skill.put("Initiative:Modifikator", Integer.parseInt(value.substring(11)));
						} else if ("TP/KK: -1/-1".equals(value)) {
							final JSONObject tpkk = new JSONObject(skill);
							tpkk.put("Schwellenwert", -1);
							tpkk.put("Schadensschritte", -1);
							skill.put("Trefferpunkte/Körperkraft", tpkk);
						} else if (value.startsWith("AT:")) {
							final JSONObject wm = skill.getObj("Waffenmodifikatoren");
							wm.put("Attackemodifikator", Integer.parseInt(value.substring(4)));
						} else if (value.startsWith("PA:")) {
							final JSONObject wm = skill.getObj("Waffenmodifikatoren");
							wm.put("Parademodifikator", Integer.parseInt(value.substring(4)));
						} else if (value.startsWith("Reichweite FK:")) {
							skill.put("Reichweite", Integer.parseInt(value.substring(16, 17)));
						} else if ("Ladezeit halbieren".equals(value)) {
							skill.put("Ladezeit", true);
						} else if (value.startsWith("Probenerleichterung:")) {
							final int separator = value.lastIndexOf(';');
							final JSONObject maneuver = skill.getObj("Manöver:Erleichterung");
							maneuver.put(value.substring(21, separator), Integer.parseInt(value.substring(separator + 3)));
						} else if (value.startsWith("Gezielte Schüsse")) {
							final JSONObject maneuver = skill.getObj("Manöver:Erleichterung");
							maneuver.put("Gezielter Schuss", Integer.parseInt(value.substring(18)));
						} else if (value.startsWith("Erlaubtes Manöver:")) {
							final JSONArray maneuver = skill.getArr("Manöver:Zusätzlich");
							maneuver.add(value.substring(19));
						} else if (value.startsWith("Besondere Vorteile")) {
							final JSONObject maneuver = skill.getObj("Vorteile");
							maneuver.put(value.substring(24), Integer.parseInt(value.substring(20, 21)));
						} else if (value.contains("zusätzliche Waffe:")) {
							final int separator = value.indexOf(';');
							final JSONArray weapons = skill.getArr("Waffen");
							weapons.add(value.substring(22, separator));
						}
					}
				}));
				return;
			} else if (name.startsWith("Wahrer Name")) {
				final String skillName = "Wahre Namen";

				apply("sonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill(skillName, Integer.valueOf(values.getOrDefault("0", "")), "", values.getOrDefault("2", ""), false, false);
				}));
				return;
			} else if ("Akoluth".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("auswahl", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, value, "", false, false);
				}
				return;
			}

			final boolean applyEffect = Set.of("Kampfgespür", "Kampfreflexe").contains(name);

			enterSkill(name, 0, choice, text, applyEffect, false);
		}), new Tuple<>("verbilligtesonderfertigkeit", () -> {
			String name = get("name");
			name = replaceSkill(name);
			String choice = get("value");
			String text = get("value");
			if (name.startsWith("Talentspezialisierung")) {
				final Map<String, String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("talent", () -> "Talent", () -> get("name")),
						new Tuple3<>("spezialisierung", () -> "Spez", () -> get("name")));
				choice = values.get("Talent");
				choice = replaceTalent(choice);
				text = values.get("Spez");

				if (talents.getObj("Nahkampftalente").containsKey(choice) || talents.getObj("Fernkampftalente").containsKey(choice)) {
					name = "Waffenspezialisierung";
				} else {
					name = "Talentspezialisierung";
				}
			} else if (name.startsWith("Zauberspezialisierung")) {
				name = "Zauberspezialisierung";
				final Map<String, String> values = extract("sonderfertigkeit",
						new Tuple3<>("zauber", () -> "Zauber", () -> get("name") + "|" + get("variante")),
						new Tuple3<>("spezialisierung", () -> "Spez", () -> get("name")));
				final String[] parts = values.get("Zauber").split("\\|");
				choice = parts[0];
				choice = spellReplacements.getStringOrDefault(choice, choice);
				if ("Dämonenbann".equals(choice)) {
					choice = parts[1] + "bann";
				}
				text = values.get("Spez");
			} else if (name.startsWith("Merkmalskenntnis")) {
				choice = name.substring(18);
				if ("Elementar".equals(choice)) {
					choice = "Elementar (gesamt)";
				} else if ("Dämonisch".equals(choice)) {
					choice = "Dämonisch (gesamt)";
				}
				name = "Merkmalskenntnis";
			} else if (name.startsWith("Repräsentation")) {
				choice = name.substring(16);
				choice = representationReplacements.getStringOrDefault(choice, choice);
				name = "Repräsentation";
			} else if (name.startsWith("Ritualkenntnis")) {
				choice = name.substring(16);
				choice = choice.startsWith("Zaubertänzer") ? "Zaubertänze" : ritualKnowledge.getStringOrDefault(choice, choice);
				name = "Ritualkenntnis";
			} else if (name.startsWith("Liturgiekenntnis")) {
				choice = name.substring(18, name.length() - 1);
				if ("Boron".equals(choice)) {
					choice = selectBoronCult();
				}
				name = "Liturgiekenntnis";
			} else if ("Kulturkunde".equals(name) || "Scharfschütze".equals(name) || "Meisterschütze".equals(name) || "Akoluth".equals(name)) {
				final Set<String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("auswahl", () -> get("auswahl"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, value, "", false, true);
				}
				return;
			} else if ("Ortskenntnis".equals(name) || "Rüstungsgewöhnung I".equals(name)) {
				final Set<String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("auswahl", () -> get("auswahl"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, 0, "", value, false, true);
				}
				return;
			} else if ("Berufsgeheimnis".equals(name)) {
				apply("verbilligtesonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill("Berufsgeheimnis", 0, "", values.getOrDefault("2", ""), false, true);
				}));
				return;
			} else if ("Schnellladen".equals(name)) {
				final Set<String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("auswahl", () -> get("auswahl"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill("Schnellladen (" + value + ")", 0, "", "", false, true);
				}
				return;
			} else if ("Waffenmeister".equals(name)) {
				apply("verbilligtesonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill("Waffenmeister", 0, values.getOrDefault("1", ""), values.getOrDefault("0", ""), false, true);
				}));
				return;
			}

			enterSkill(name, 0, choice, text, false, true);
		}));
	}

	private void parseSpells() {
		final JSONObject spells = hero.getObj("Zauber");

		apply("zauberliste", new Tuple<>("zauber", () -> {
			String name = get("name");
			name = spellReplacements.getStringOrDefault(name, name);
			final String variant = get("variante");
			if ("Dämonenbann".equals(name)) {
				name = variant + "bann";
			}
			final JSONObject spell = HeroUtil.findTalent(name)._1;
			if (spell != null) {
				final JSONObject actualSpell = spells.getObj(name);
				final String rep = shortRepReplacements.getStringOrDefault(get("repraesentation"), "Mag");
				final JSONObject actualRepresentation;
				if (spell.containsKey("Auswahl") || spell.containsKey("Freitext")) {
					final JSONArray choiceSpell = actualSpell.getArr(rep);
					actualRepresentation = new JSONObject(choiceSpell);
					choiceSpell.add(actualRepresentation);
				} else {
					actualRepresentation = actualSpell.getObj(rep);
				}
				actualRepresentation.put("ZfW", Integer.parseInt(get("value")));
				if ("true".equals(get("hauszauber"))) {
					actualRepresentation.put("Hauszauber", true);
				}
				if ("true".equals(get("leittalent"))) {
					actualRepresentation.put("Leittalent", true);
				}
				if ("true".equals(get("se"))) {
					actualRepresentation.put("SEs", 1);
				}
				if (variant != null) {
					if (spell.containsKey("Auswahl")) {
						actualRepresentation.put("Auswahl", variant);
					} else if (spell.containsKey("Freitext")) {
						actualRepresentation.put("Freitext", variant);
					}
				}
			}
		}));
	}

	private void parseTalents() {
		final JSONObject actualTalents = hero.getObj("Talente");

		apply("talentliste", new Tuple<>("talent", () -> {
			String name = get("name");
			name = replaceTalent(name);
			String variant = get("variante");
			if (name.startsWith("Kraftschub")) {
				variant = name.substring(12, name.length() - 1);
				name = "Kräfteschub";
			} else if (name.startsWith("Talentschub")) {
				variant = name.substring(13, name.length() - 1);
				name = "Talentschub";
			}
			final Tuple<JSONObject, String> talentAndGroup = HeroUtil.findTalent(name);
			final JSONObject talent = talentAndGroup._1;
			final String group = talentAndGroup._2;
			if (group != null) {
				final JSONObject actualTalent;
				if (talent.containsKey("Auswahl") || talent.containsKey("Freitext")) {
					final JSONArray choiceTalent = actualTalents.getObj(group).getArr(name);
					actualTalent = new JSONObject(choiceTalent);
					choiceTalent.add(actualTalent);
				} else {
					actualTalent = actualTalents.getObj(group).getObj(name);
				}
				final int taw = Integer.parseInt(get("value"));
				actualTalent.put("TaW", taw);
				if ("Fernkampftalente".equals(group) || talent.getBoolOrDefault("NurAT", false)) {
					actualTalent.put("AT", taw);
				}
				if ("true".equals(get("leittalent"))) {
					actualTalent.put("Leittalent", true);
				}
				if ("true".equals(get("se"))) {
					actualTalent.put("SEs", 1);
				}
				if (variant != null) {
					if (talent.containsKey("Auswahl")) {
						actualTalent.put("Auswahl", variant);
					} else if (talent.containsKey("Freitext")) {
						actualTalent.put("Freitext", variant);
					}
				}
			}
		}));
	}

	private String replaceSkill(String in) {
		if (in == null) return null;
		if (in.startsWith("Runen:")) {
			in = in.substring(7);
		} else if (in.startsWith("Ritual:")) {
			in = in.substring(8);
		} else if (in.startsWith("Liturgie:")) {
			in = in.substring(10);
			if (in.contains("(")) {
				final int start = in.indexOf('(');
				final int end = in.indexOf(')');
				if (end - start > 4) {
					in = in.substring(start + 1, end);
				}
			}
		} else if (in.startsWith("Elfenlied")) {
			in = in.substring(11);
		} else if (in.startsWith("Hexenfluch")) {
			in = in.substring(12);
		} else if (in.startsWith("Stabzauber")) {
			in = in.substring(12);
			if ("Bindung".equals(in)) {
				in = "Bindung des Stabes";
			}
		} else if (in.startsWith("Zaubertanz")) {
			in = in.substring(12);
			final int beginRealName = in.lastIndexOf('(');
			if (beginRealName != -1) {
				in = in.substring(beginRealName + 1, in.length() - 1);
			}
		} else if (in.startsWith("Hexenritual")) {
			in = in.substring(13);
		} else if (in.startsWith("Kugelzauber")) {
			in = in.substring(13);
			if ("Bindung".equals(in)) {
				in = "Bindung der Kugel";
			}
		} else if (in.startsWith("Gabe des Odûn") || in.startsWith("Schalenzauber") || in.startsWith("Trommelzauber") || in.startsWith("Zauberzeichen:")) {
			in = in.substring(15);
		} else if (in.startsWith("Schuppenbeutel") || in.startsWith("Zibilja-Ritual")) {
			in = in.substring(16);
		} else if (in.startsWith("Schlangenring-Zauber")) {
			in = in.substring(22);
		} else if (in.startsWith("Waffenloser Kampfstil")) {
			in = in.substring(23);
		} else if (in.startsWith("Druidisches Dolchritual")) {
			in = in.substring(25);
			if (!"Opferdolch".equals(in)) {
				in = in + " des Dolches";
			}
		} else if (in.startsWith("Kristallomantisches Ritual")) {
			in = in.substring(28);
		} else if (in.startsWith("Druidisches Herrschaftsritual")) {
			in = in.substring(31);
		} else if (in.startsWith("Spätweihe")) {
			in = "Spätweihe";
		}
		in = skillReplacements.getStringOrDefault(in, in);
		return in;
	}

	private String replaceTalent(String in) {
		if (in == null) return null;
		in = talentReplacements.getStringOrDefault(in, in);
		if (in.startsWith("Sprachen kennen") || in.startsWith("Lesen/Schreiben")) {
			in = in.substring(16);
		} else if (in.startsWith("Heilkunde")) {
			in = "Heilkunde " + in.substring(11);
		} else if (in.startsWith("Ritualkenntnis")) {
			in = in.substring(16);
			in = in.startsWith("Zaubertänzer") ? "Zaubertänze" : ritualKnowledge.getStringOrDefault(in, in);
		} else if (in.startsWith("Liturgiekenntnis")) {
			in = in.substring(18, in.length() - 1);
			if ("Boron".equals(in)) {
				in = selectBoronCult();
			}
		}

		return in;
	}

	private String selectBoronCult() {
		if (boronCult == null) {
			boronCult = ask("Ritus auswählen", "Boron-Ritus auswählen:", "Puniner Ritus", "Boron (Punin)", "Al'Anfaner Ritus", "Boron (Al'Anfa)");
		}
		return boronCult;
	}

}
