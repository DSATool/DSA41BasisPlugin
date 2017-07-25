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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

@SuppressWarnings("unchecked")
public class HeldenSoftwareXMLHeroLoader implements FileLoader {

	private static Map<String, String> goddesses;
	private static Map<String, String> proConReplacements;
	private static Map<String, String> ritualKnowledge;
	private static Map<String, String> repReplacements;
	private static Map<String, String> raceReplacements;
	private static Map<String, String> cultureReplacements;
	private static Map<String, String> professionReplacements;
	private static Map<String, String> talentReplacements;
	private static Map<String, String> spellReplacements;
	private static Map<String, String> groupReplacements;
	private static Map<String, String> skillReplacements;
	private static Map<String, String> representations;

	static {
		groupReplacements = new HashMap<>();
		groupReplacements.put("Nahkampf", "Nahkampftalente");
		groupReplacements.put("Fernkampf", "Fernkampftalente");
		groupReplacements.put("Kampf", "Kampftalente");
		groupReplacements.put("Körperlich", "Körperliche Talente");
		groupReplacements.put("Gesellschaft", "Gesellschaftliche Talente");
		groupReplacements.put("Natur", "Natur-Talente");
		groupReplacements.put("Wissen", "Wissenstalente");
		groupReplacements.put("Handwerk", "Handwerkstalente");
	}

	static {
		talentReplacements = new HashMap<>();
		talentReplacements.put("Zweihandhiebwaffen", "Zweihand-Hiebwaffen");
		talentReplacements.put("Sich verstecken", "Sich Verstecken");
		talentReplacements.put("Stimmen imitieren", "Stimmen Imitieren");
		talentReplacements.put("Sich verkleiden", "Sich Verkleiden");
		talentReplacements.put("Fallen stellen", "Fallenstellen");
		talentReplacements.put("Götter und Kulte", "Götter/Kulte");
		talentReplacements.put("Geografie", "Geographie");
		talentReplacements.put("Sagen und Legenden", "Sagen/Legenden");
		talentReplacements.put("Boote fahren", "Boote Fahren");
		talentReplacements.put("Fahrzeug lenken", "Fahrzeug Lenken");
		talentReplacements.put("Kartografie", "Kartographie");
		talentReplacements.put("Schlösser knacken", "Schlösser Knacken");
		talentReplacements.put("Schnaps brennen", "Schnaps Brennen");
		talentReplacements.put("Stoffe färben", "Stoffe Färben");
		talentReplacements.put("Sprachen kennen Alt-Imperial/Aureliani", "Aureliani");
		talentReplacements.put("Sprachen kennen Urtulamidya", "Ur-Tulamidya");
		talentReplacements.put("Lesen/Schreiben Altes Kemi", "Altes Kemi (Schrift)");
		talentReplacements.put("Lesen/Schreiben Angram", "Angram (Schrift)");
		talentReplacements.put("Lesen/Schreiben Gimaril-Glyphen", "Gimaril");
		talentReplacements.put("Lesen/Schreiben Gjalskisch", "Gjalskisch (Schrift)");
		talentReplacements.put("Lesen/Schreiben Hjaldingsche Runen", "Hjaldingsche Runen");
		talentReplacements.put("Lesen/Schreiben (Alt-)Imperiale Zeichen", "Imperiale Zeichen");
		talentReplacements.put("Lesen/Schreiben Isdira/Asdharia", "Isdira (Schrift)");
		talentReplacements.put("Lesen/Schreiben Rogolan", "Rogolan (Schrift)");
		talentReplacements.put("Lesen/Schreiben Trollische Raumbilderschrift", "Raumbilderschrift");
		talentReplacements.put("Lesen/Schreiben Tulamidya", "Tulamidya (Schrift)");
		talentReplacements.put("Lesen/Schreiben Urtulamidya", "Ur-Tulamidya (Schrift)");
		talentReplacements.put("Lesen/Schreiben Zhayad", "Zhayad (Schrift)");
	}

	static {
		spellReplacements = new HashMap<>();
		spellReplacements.put("Analys Arkanstruktur", "Analys Arcanstruktur");
		spellReplacements.put("Aquafaxius Wasserstrahl", "Aquafaxius");
		spellReplacements.put("Archofaxius Erzstrahl", "Archofaxius");
		spellReplacements.put("Brenne toter Stoff!", "Brenne, toter Stoff!");
		spellReplacements.put("Frigifaxius Eisstrahl", "Frigifaxius");
		spellReplacements.put("Frigisphaero Eisball", "Frigisphaero");
		spellReplacements.put("Humofaxius Humusstrahl", "Humofaxius");
		spellReplacements.put("Motoricus", "Motoricus Geisteshand");
		spellReplacements.put("Orcanofaxius Luftstrahl", "Orcanofaxius");
		spellReplacements.put("Orconosphaero Orkanball", "Orcanosphaero");
		spellReplacements.put("Orkanwand", "Wand aus Luft");
		spellReplacements.put("Reptilea Natternest", "Reptilea Natternnest");
		spellReplacements.put("Respondami", "Respondami Wahrheitszwang");
		spellReplacements.put("Skelettarius", "Skelettarius Totenherr");
		spellReplacements.put("Silentium", "Silentium Schweigekreis");
		spellReplacements.put("Umbraporta Schattentüre", "Umbraporta Schattentür");
		spellReplacements.put("Weiße Mähn und goldener Huf", "Weiße Mähn' und gold'ner Huf");
	}

	static {
		representations = new HashMap<>();
		representations.put("Achaz", "Ach");
		representations.put("Borbaradianer", "Bor");
		representations.put("Druide", "Dru");
		representations.put("Elf", "Elf");
		representations.put("Hexe", "Hex");
		representations.put("Schelm", "Sch");
		representations.put("Scharlatan", "Srl");
		representations.put("Magiedilletant", "ÜNB");
	}

	static {
		proConReplacements = new HashMap<>();
		proConReplacements.put("Begabung für [Merkmal]", "Begabung für Merkmal");
		proConReplacements.put("Begabung für [Ritual]", "Begabung für Ritual");
		proConReplacements.put("Begabung für [Talent]", "Begabung für Talent");
		proConReplacements.put("Begabung für [Talentgruppe]", "Begabung für Talentgruppe");
		proConReplacements.put("Begabung für [Zauber]", "Begabung für Zauber");
		proConReplacements.put("Geweiht [Angrosch]", "Geweiht");
		proConReplacements.put("Geweiht [Gravesh]", "Geweiht");
		proConReplacements.put("Geweiht [H'Ranga]", "Geweiht");
		proConReplacements.put("Geweiht [nicht-alveranische Gottheit]", "Geweiht");
		proConReplacements.put("Geweiht [zwölfgöttliche Kirche]", "Geweiht");
		proConReplacements.put("Gutaussehend", "Gut Aussehend");
		proConReplacements.put("Herausragende Eigenschaft: Mut", "Herausragender Mut");
		proConReplacements.put("Herausragende Eigenschaft: Klugheit", "Herausragende Klugheit");
		proConReplacements.put("Herausragende Eigenschaft: Intuition", "Herausragende Intuition");
		proConReplacements.put("Herausragende Eigenschaft: Charisma", "Herausragendes Charisma");
		proConReplacements.put("Herausragende Eigenschaft: Gewandtheit", "Herausragende Gewandtheit");
		proConReplacements.put("Herausragende Eigenschaft: Fingerfertigkeit", "Herausragende Fingerfertigkeit");
		proConReplacements.put("Herausragende Eigenschaft: Konstitution", "Herausragende Konstitution");
		proConReplacements.put("Herausragende Eigenschaft: Körperkraft", "Herausragende Körperkraft");
		proConReplacements.put("Tierempathie (alle)", "Tierempathie");
		proConReplacements.put("Tierempathie (speziell)", "Tierempathie (einzelne Tierart)");
		proConReplacements.put("Madas Fluch (stark)", "Madas Fluch");
		proConReplacements.put("Miserable Eigenschaft: Mut", "Miserabler Mut");
		proConReplacements.put("Miserable Eigenschaft: Klugheit", "Miserable Klugheit");
		proConReplacements.put("Miserable Eigenschaft: Intuition", "Miserable Intuition");
		proConReplacements.put("Miserable Eigenschaft: Charisma", "Miserables Charisma");
		proConReplacements.put("Miserable Eigenschaft: Gewandtheit", "Miserable Gewandtheit");
		proConReplacements.put("Miserable Eigenschaft: Fingerfertigkeit", "Miserable Fingerfertigkeit");
		proConReplacements.put("Miserable Eigenschaft: Konstitution", "Miserable Konstitution");
		proConReplacements.put("Miserable Eigenschaft: Körperkraft", "Miserable Körperkraft");
		proConReplacements.put("Schneller alternd", "Schneller Alternd");
		proConReplacements.put("Unfähigkeit für [Merkmal]", "Unfähigkeit für Merkmal");
		proConReplacements.put("Unfähigkeit für [Talent]", "Unfähigkeit für Talent");
		proConReplacements.put("Unfähigkeit für [Talentgruppe]", "Unfähigkeit für Talentgruppe");
	}

	static {
		skillReplacements = new HashMap<>();
		skillReplacements.put("Beiß auf Granit", "Beiß auf Granit!");
		skillReplacements.put("Beute", "Beute!");
		skillReplacements.put("Pech an den Hals", "Pech an den Hals wünschen");
		skillReplacements.put("Lied des Trostes (Firnelfische Variante)", "Firnelfisches Lied des Trostes");
		skillReplacements.put("Wasserbann", "Geodischer Wasserbann");
		skillReplacements.put("Fackel", "Ewige Flamme");
		skillReplacements.put("Seil", "Seil des Adepten");
		skillReplacements.put("Stabverlängerung", "Doppeltes Maß");
		skillReplacements.put("Ruf des Krieges", "Der Ruf des Krieges");
		skillReplacements.put("Schutz Rastullahs", "Der Schutz Rastullahs");
		skillReplacements.put("Glyphe der Elementaren Attraktion", "Glyphe der elementaren Attraktion");
		skillReplacements.put("Glyphe der Elementaren Bannung", "Glyphe der elementaren Bannung");
		skillReplacements.put("Satinavs Siegel", "Zusatzzeichen Satinavs Siegel");
		skillReplacements.put("Schutzsiegel", "Zusatzzeichen Schutzsiegel");
		skillReplacements.put("Arngrims Höhle", "Arngrimms Höhle");
	}

	static {
		goddesses = new HashMap<>();
		goddesses.put("Greif", "Praios");
		goddesses.put("Schwert", "Rondra");
		goddesses.put("Delphin", "Efferd");
		goddesses.put("Gans", "Travia");
		goddesses.put("Rabe", "Boron");
		goddesses.put("Schlange", "Hesinde");
		goddesses.put("Eisbär", "Firun");
		goddesses.put("Eidechse", "Tsa");
		goddesses.put("Fuchs", "Phex");
		goddesses.put("Storch", "Peraine");
		goddesses.put("Hammer/Amboss", "Ingerimm");
		goddesses.put("Stute", "Rahja");
		goddesses.put("Sternenleere", "Namenloser");
	}

	static {
		repReplacements = new HashMap<>();
		repReplacements.put("Achaz", "Kristallomantisch");
		repReplacements.put("Borbaradianer", "Borbaradianisch");
		repReplacements.put("Druide", "Druidisch");
		repReplacements.put("Elf", "Elfisch");
		repReplacements.put("Geode", "Geodisch");
		repReplacements.put("Hexe", "Hexisch");
		repReplacements.put("Magier", "Gildenmagisch");
		repReplacements.put("Scharlatan", "Scharlatanisch");
		repReplacements.put("Schelm", "Schelmisch");
	}

	static {
		ritualKnowledge = new HashMap<>();
		ritualKnowledge.put("Derwisch", "Derwische");
		ritualKnowledge.put("Druide", "Druidisch");
		ritualKnowledge.put("Geode", "Geodisch (Herren der Erde)");
		ritualKnowledge.put("Gildenmagie", "Gildenmagisch");
		ritualKnowledge.put("Hexe", "Hexisch");
		ritualKnowledge.put("Kristallomantie", "Kristallomantisch");
		ritualKnowledge.put("Scharlatan", "Scharlatanisch");
	}

	static {
		raceReplacements = new HashMap<>();
		raceReplacements.put("Mittellaender", "Mittelländer");
		raceReplacements.put("Gjalsker", "Gjalskerländer");
		raceReplacements.put("Halbelfe firnelfischer Abstammung", "Halbelf firnelfischer Abstammung");
		raceReplacements.put("Halbelfe nivesischer Abstammung", "Halbelf nivesischer Abstammung");
		raceReplacements.put("Halbelfe thorwalscher Abstammung", "Halbelf thorwalscher Abstammung");
		raceReplacements.put("in elfischer Kultur aufgewachsen", "bei Elfen großgezogen");
		raceReplacements.put("Brillantzwerge", "Brillantzwerg");
		raceReplacements.put("Erz-/Hügelzwerge", "Erzzwerg");
		raceReplacements.put("Ambosszwerge", "Ambosszwerg");
		raceReplacements.put("Wilde Zwerge", "Wilder Zwerg");
	}

	static {
		cultureReplacements = new HashMap<>();
		cultureReplacements.put("AndergastNostria", "Andergast und Nostria");
		cultureReplacements.put("ArchaischeAchaz", "Archaische Achaz");
		cultureReplacements.put("AuelfenSippe", "Auelfische Sippe");
		cultureReplacements.put("Dschungelstaemme", "Dschungelstämme");
		cultureReplacements.put("ElfischeSiedlung", "Elfische Siedlung");
		cultureReplacements.put("FestumerGhetto", "Festumer Ghetto");
		cultureReplacements.put("FirnelfenSippe", "Firnelfische Sippe");
		cultureReplacements.put("Garetien", "Mittelländische Städte");
		cultureReplacements.put("Gjalskerlaender", "Gjalskerland");
		cultureReplacements.put("Huegelzwerge", "Hügelzwerge");
		cultureReplacements.put("Mittelreich", "Mittelländische Landbevölkerung");
		cultureReplacements.put("Nivesenstaemme", "Nivesenstämme");
		cultureReplacements.put("Novadis", "Novadi");
		cultureReplacements.put("NuanaaeLi", "Nuanaä-Lie");
		cultureReplacements.put("StammesAchaz", "Stammes-Achaz");
		cultureReplacements.put("SteppenelfenSippe", "Steppenelfische Sippe");
		cultureReplacements.put("Suedaventurien", "Südaventurien");
		cultureReplacements.put("Svellttal", "Svellttal und Nordlande");
		cultureReplacements.put("SvellttalOkkupanten", "Svellttal-Besatzer");
		cultureReplacements.put("TulamidischeStadtstaaten", "Tulamidische Stadtstaaten");
		cultureReplacements.put("VerloreneStaemme", "Verlorene Stämme");
		cultureReplacements.put("WaldelfenSippe", "Waldelfische Sippe");
		cultureReplacements.put("WaldinselUtulus", "Waldinsel-Utulus");
		cultureReplacements.put("Kannemünde / Mhanerhaven", "Kannemünde/Mhanerhaven");
		cultureReplacements.put("Flüchtlinge aus borbaradianisch besetzten Städten", "Flüchtlinge aus borbaradianisch besetzten Gebieten");
		cultureReplacements.put("Maraskanische Exilanten in Festum", "Maraskanische Exilanten");
		cultureReplacements.put("Weiden/Greifenfurt", "Regionen Weiden und Greifenfurt");
		cultureReplacements.put("Kasimit", "Kasimiten");
		cultureReplacements.put("Maraskanische Exilanten in Khunchom", "Maraskanische Exilanten");
		cultureReplacements.put("Wüstenoase (Männer)", "Wüstenoase");
		cultureReplacements.put("Wüstenoase (Frauen)", "Wüstenoase");
		cultureReplacements.put("Sippe (UdW)", "Sippe");
		cultureReplacements.put("Ottajasko (UdW)", "Ottajasko");
		cultureReplacements.put("Söldnerottajasko Hammerfaust in Vinay/Brabak (UdW)", "Söldnerottajasko Hammerfaust in Vinay/Brabak");
		cultureReplacements.put("Söldnerottajasko Hammerfaust in Askja/Regenwald (UdW)", "Söldnerottajasko Hammerfaust in Askja/Regenwald");
		cultureReplacements.put("Söldnerottajasko Bannerträger in Drolsash/Drol (UdW)", "Söldnerottajasko Bannerträger in Drôlsash/Drôl");
		cultureReplacements.put("Söldnerottajasko Drachen von Llanka, heimatlos (UdW)", "Söldnerottajasko Drachen von Llanka, heimatlos");
		cultureReplacements.put("Sippe aus dem Binnenland (UdW)", "Sippe|Binnenland");
		cultureReplacements.put("Küstengebiet", "Küstengebiete");
		cultureReplacements.put("Stadtstaat Al'Anfa", "Al'Anfa");
		cultureReplacements.put("Maraskanische Exilanten in Al'Anfa", "Maraskanische Exilanten");
		cultureReplacements.put("Stadtstaat Brabak", "Brabak");
		cultureReplacements.put("Stadtstaat Charypso", "Charypso");
		cultureReplacements.put("Stadtstaat Chorhop", "Chorhop");
		cultureReplacements.put("Stadtstaat Hôt-Alem", "Hôt-Alem");
		cultureReplacements.put("Stadtstaat Khefu", "Khefu");
		cultureReplacements.put("Stadtstaat Mengbilla", "Mengbilla");
		cultureReplacements.put("Stadtstaat Mirham", "Mirham");
		cultureReplacements.put("Stadtstaat Sylla", "Sylla");
		cultureReplacements.put("Kolonialhafen", "Kolonialhäfen");
		cultureReplacements.put("Norbardensippe in Thorwal (UdW)", "Norbardensippe in Thorwal");
		cultureReplacements.put("Südliche Mittellande (Almada, Garetien)", "Südliche Mittellande");
		cultureReplacements.put("Großstadt (Lowangen, Punin)", "Großstadt");
		cultureReplacements.put("Firnelfisch beeinflusste Siedlung (Olport, Keamonmund)", "Firnelfisch beeinflusste Siedlung");
		cultureReplacements.put("Waldelfisch beeinflusste Siedlung (Donnerbach, Gerasim, Kvirasim)", "Waldelfisch beeinflusste Siedlung");
		cultureReplacements.put("Ergoch (Sklaven)", "Ergoch");
		cultureReplacements.put("Grishik (Bauern)", "Grishik");
		cultureReplacements.put("Drasdech (Handwerker)", "Drasdech");
		cultureReplacements.put("Khurkach (Krieger und Jäger)", "Khurkach");
		cultureReplacements.put("Okwach (Elite-Krieger, Priester, Schamanen)", "Okwach");
	}

	static {
		professionReplacements = new HashMap<>();
		professionReplacements.put("Alchemist", "Alchimist");
		professionReplacements.put("ArchaischerHandwerkerDerFerkinas", "Handwerker|Archaische Handwerker|Archaische Handwerker der Ferkinas");
		professionReplacements.put("ArchaischerHandwerkerDesSuedens", "Handwerker|Archaische Handwerker|Archaische Handwerker Südaventuriens und der Achaz");
		professionReplacements.put("Bordmagus", "Bordzauberer|Halbzauberer");
		professionReplacements.put("FaehnrichFusskaemper", "Fähnrich|Fähnrich der Fußkämpfer");
		professionReplacements.put("FaehnrichKavallerie", "Fähnrich|Fähnrich der Kavallerie");
		professionReplacements.put("FaehnrichSee", "Fähnrich|Fähnrich zur See");
		professionReplacements.put("Fernhaendler", "Fernhändler");
		professionReplacements.put("Grenzjaeger", "Grenzjäger");
		professionReplacements.put("Grosswildjaeger", "Großwildjäger");
		professionReplacements.put("Haendler", "Händler");
		professionReplacements.put("Hoefling", "Höfling");
		professionReplacements.put("Hofkuenstler", "Hofkünstler");
		professionReplacements.put("Jaeger", "Jäger");
		professionReplacements.put("Jahrmarktskaempfer", "Jahrmarktskämpfer");
		professionReplacements.put("Kaempfer", "Kämpfer");
		professionReplacements.put("KaempferUdw", "Soldat");
		professionReplacements.put("Karawanenfuehrer", "Karawanenführer");
		professionReplacements.put("Legendensaenger", "Legendensänger");
		professionReplacements.put("Lehrmeister", "Magier");
		professionReplacements.put("Rattenfaenger", "Rattenfänger");
		professionReplacements.put("Soeldner", "Söldner");
		professionReplacements.put("Stabsfaehnrich", "Fähnrich|Stabsfähnrich");
		professionReplacements.put("Strassenraeuber", "Straßenräuber");
		professionReplacements.put("Tageloehner", "Tagelöhner");
		professionReplacements.put("Tierbaendiger", "Tierbändiger");
		professionReplacements.put("Tierkrieger", "Durro-Dûn");
		professionReplacements.put("Wildnislaeufer", "Wildnisläufer");
		professionReplacements.put("WindUndWettermagus", "Bordzauberer");
		professionReplacements.put("Zaubertaenzer", "Tänzer");
		professionReplacements.put("AlAnfa", "Al'Anfa");
		professionReplacements.put("Gareth oder Arivor", "Gareth");
		professionReplacements.put("Stabsfähnrich aus Wehrheim", "Wehrheim");
		professionReplacements.put("Stabsfähnrich aus Vinsalt", "Vinsalt");
		professionReplacements.put("städtischer Bogenschütze (UdW)", "Stadtgardist|Städtischer Bogenschütze");
		professionReplacements.put("Akademiegardist/Tempelgardist/Ehrengardist", "Akademiegardist");
		professionReplacements.put("Kämpferschule Rekkerskola in Enqui (UdW)", "Gardist in Enqui");
		professionReplacements.put("Festung der Tapferen zu Baburin", "Krieger aus Baburin");
		professionReplacements.put("Akademie 'Schwert und Schild' zu Baliho", "Krieger aus Baliho");
		professionReplacements.put("Herzögliche Kriegerakademie zu Elenvina", "Krieger aus Elenvina");
		professionReplacements.put("Haus der Hohen Kriegskunst derer vom Berg in Eslamsgrund", "Krieger aus Eslamsgrund");
		professionReplacements.put("Institut der Hohen Schule der Reiterei zu Gareth", "Krieger aus Gareth");
		professionReplacements.put("Ruadas Ehr in Havena", "Krieger aus Havena");
		professionReplacements.put("Kriegerakademie 'Mutter Rondra' auf Hylailos", "Krieger aus Hylailos");
		professionReplacements.put("Kriegerschule Rabenschnabel zu Mengbilla", "Krieger aus Mengbilla");
		professionReplacements.put("Die Rondragefällige und Theaterritterliche Kriegerschule der Bornländischen Lande zu Neersand", "Krieger aus Neersand");
		professionReplacements.put("Freie Kämpferschule der Trutzburg zu Prem", "Krieger aus Prem");
		professionReplacements.put("Freie Kämpferschule der Trutzburg zu Prem (UdW)", "Krieger aus Prem");
		professionReplacements.put("Königliches Kriegerseminar zu Punin", "Krieger aus Punin");
		professionReplacements.put("Kriegerschule Feuerlilie in Rommilys", "Krieger aus Rommilys");
		professionReplacements.put("Gänseritter", "Krieger aus Rommilys|Gänseritter");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal", "Krieger aus Thorwal");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal/ Hetja Fotskari (UdW)", "Krieger aus Thorwal|Hetja der Fotskari");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal/ Hetja Riddari (UdW)", "Krieger aus Thorwal|Hetja der Riddari");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal/ Hetja Mangskari (UdW)", "Krieger aus Thorwal|Hetja der Mangskari");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal/ Hetja Herverkmader (UdW)", "Krieger aus Thorwal|Hetja der Herverkmader");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal/ Hetja Bogskari (UdW)", "Krieger aus Thorwal|Hetja der Bogskari");
		professionReplacements.put("Kämpferschule Ugdalfskronir in Thorwal/ Hetja Sjahskari (UdW)", "Krieger aus Thorwal|Hetja der Sjahskari");
		professionReplacements.put("Akademie der Kriegs- und Lebenskunst zu Vinsalt", "Krieger aus Vinsalt");
		professionReplacements.put("Rondras Schwertkunst in Winhall", "Krieger aus Winhall");
		professionReplacements.put("Knappe aus Andergast/Nostria (UdW)",
				"Ritter alten Schlags|Knappe des traditionellen Ritters|Knappe aus den Streitenden Königreichen");
		professionReplacements.put("Adersin", "Schwertgeselle nach Adersin");
		professionReplacements.put("Uinin", "Schwertgeselle nach Uinin");
		professionReplacements.put("Essalio Fedorino", "Schwertgeselle nach Fedorino");
		professionReplacements.put("Rafim al-Halan", "Schwertgeselle nach al-Halan");
		professionReplacements.put("Rekker der Hjalskari (Fotskari) (UdW)", "Rekker der Fotskari");
		professionReplacements.put("Rekker der Hjalskari (Riddari) (UdW)", "Rekker der Riddari");
		professionReplacements.put("Rekker der Hjalskari (Mangskari) (UdW)", "Rekker der Mangskari");
		professionReplacements.put("Rekker der Hjalskari (Herverkmader) (UdW)", "Rekker der Fotskari|Rekker der Herverkmader");
		professionReplacements.put("Rekker der Hjalskari (Bogskari) (UdW)", "Rekker der Bogskari");
		professionReplacements.put("Rekker der Hjalskari (Sjahskari) (UdW)", "Rekker der Sjahskari");
		professionReplacements.put("Kämpfer aus einer Ottajasko (UdW)", "Kämpfer einer Ottajasko");
		professionReplacements.put("Kämpfer aus einer Sippe (UdW)", "Kämpfer einer Sippe");
		professionReplacements.put("Kämpfer/Robbenjäger aus einer Ottajasko (UdW)", "Kämpfer einer Ottajasko|Robbenjäger");
		professionReplacements.put("Kämpfer/Seefahrer aus einer Ottajasko (UdW)", "Kämpfer einer Ottajasko|Robbenjäger");
		professionReplacements.put("Kämpfer/Robbenjäger aus einer Sippe (UdW)", "Kämpfer einer Sippe|Seefahrer");
		professionReplacements.put("Kämpfer/Seefahrer aus einer Sippe (UdW)", "Kämpfer einer Sippe|Seefahrer");
		professionReplacements.put("Leichtes Fußvolk (Anderthalbhänder)", "Leichtes Fußvolk|Anderthalbhänder");
		professionReplacements.put("Premer Seesöldner (UdW)", "Premer Seesöldner");
		professionReplacements.put("Tempelwache Achaz", "Achaz-Stammeskrieger|Tempelwache");
		professionReplacements.put("Stafettenläufer", "Zwergischer Stafettenläufer");
		professionReplacements.put("Bergungs-, Schwamm- oder Korallentaucher", "Bergungstaucher");
		professionReplacements.put("Robbenjäger (UdW)", "Robbenjäger");
		professionReplacements.put("Fallensteller (UdW)", "Robbenjäger|Fallensteller");
		professionReplacements.put("Goldsucher oder Prospektor", "Prospektor");
		professionReplacements.put("Walfänger/Haijäger", "Walfänger");
		professionReplacements.put("Skalde aus der Runajasko (UdW)", "Skalde aus der Runajasko");
		professionReplacements.put("Akrobat/Tänzer", "Akrobat");
		professionReplacements.put("Taugenichts/Stutzer", "Stutzer");
		professionReplacements.put("Taugenichts/Dilettant", "Dilettant");
		professionReplacements.put("Erzieher", "Erzieher der Achaz");
		professionReplacements.put("Baumeister/Deichmeister", "Baumeister");
		professionReplacements.put("Hüttenkundiger/Bronzegießer/Eisengießer", "Hüttenkundiger");
		professionReplacements.put("traditioneller Schiffbauer (UdW)", "Traditioneller Schiffbauer");
		professionReplacements.put("Philosoph/Metaphysiker", "Philosoph");
		professionReplacements.put("Völkerkundler/Sagenkundler", "Völkerkundler");
		professionReplacements.put("zwergischer Bastler", "Bastler");
		professionReplacements.put("zwergischer, dörflicher Bastler", "Bastler|dörflicher Handwerker");
		professionReplacements.put("Sesh'shem", "Sesh'shemet");
		professionReplacements.put("Schauermann", "Schauerleute");
		professionReplacements.put("Quacksalber/Zahnreißer", "Quacksalber");
		professionReplacements.put("Brutfleger", "Brutpfleger der Achaz");
		professionReplacements.put("Bund des Roten Salamanders (Andergast)", "Bund des Roten Salamanders|Andergast");
		professionReplacements.put("Bund des Roten Salamanders (Andergast, magiebegabt)", "Magiebegabter Alchimist|Bund des Roten Salamanders|Andergast");
		professionReplacements.put("Bund des Roten Salamanders (Brabak)", "Bund des Roten Salamanders|Brabak");
		professionReplacements.put("Bund des Roten Salamanders (Brabak, magiebegabt)", "Magiebegabter Alchimist|Bund des Roten Salamanders|Brabak");
		professionReplacements.put("Bund des Roten Salamanders (Fasar)", "Bund des Roten Salamanders|Fasar");
		professionReplacements.put("Bund des Roten Salamanders (Fasar, magiebegabt)", "Magiebegabter Alchimist|Bund des Roten Salamanders|Fasar");
		professionReplacements.put("Bund des Roten Salamanders (Festum)", "Bund des Roten Salamanders|Festum");
		professionReplacements.put("Bund des Roten Salamanders (Festum, magiebegabt)", "Magiebegabter Alchimist|Bund des Roten Salamanders|Festum");
		professionReplacements.put("Gilde der Alchimisten zu Mengbilla (magiebegabt)", "Magiebegabter Alchimist|Gilde der Alchimisten zu Mengbilla");
		professionReplacements.put("Alchimistische Fakultät der Universität von Al'Anfa (magiebegabt)",
				"Magiebegabter Alchimist|Alchimistische Fakultät der Universität von Al'Anfa");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Arithmetik)",
				"Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Arithmetik");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Arithmetik, magiebegabt)",
				"Magiebegabter Alchimist|Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Arithmetik");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Geometrie)",
				"Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Geometrie");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Geometrie, magiebegabt)",
				"Magiebegabter Alchimist|Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Geometrie");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Musiklehre)",
				"Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Musiklehre");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Musiklehre, magiebegabt)",
				"Magiebegabter Alchimist|Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Musiklehre");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Astronomie)",
				"Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Astronomie");
		professionReplacements.put("Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis (Astronomie, magiebegabt)",
				"Magiebegabter Alchimist|Fakultät der Alchimie der Herzog-Eolan-Universität zu Methumis|Quadrivium Astronomie");
		professionReplacements.put("Spagyrischer Zweig der Halle des Lebens zu Norburg (magiebegabt)",
				"Magiebegabter Alchimist|Spagyrischer Zweig der Halle des Lebens zu Norburg");
		professionReplacements.put("Kammerjaeger", "Kammerjäger");
		professionReplacements.put("Kammerjaeger (magiebegabt)", "Magiebegabter Alchimist|Kammerjäger");
		professionReplacements.put("Alchimist aus Unau (magiebegabt)", "Magiebegabter Alchimist|Alchimist aus Unau");
		professionReplacements.put("Konzilsdruide (Eiselementarist)", "Konzilsdruide|Eiselementarist");
		professionReplacements.put("Konzilsdruide (Erzelementarist)", "Konzilsdruide|Erzelementarist");
		professionReplacements.put("Konzilsdruide (Feuerelementarist)", "Konzilsdruide|Feuerelementarist");
		professionReplacements.put("Konzilsdruide (Humuselementarist)", "Konzilsdruide|Humuselementarist");
		professionReplacements.put("Konzilsdruide (Luftelementarist)", "Konzilsdruide|Luftelementarist");
		professionReplacements.put("Konzilsdruide (Wasserelementarist)", "Konzilsdruide|Wasserelementarist");
		professionReplacements.put("Verschwiegene Schwestern", "Verschwiegene Schwester");
		professionReplacements.put("Schwestern des Wissens", "Schwester des Wissens");
		professionReplacements.put("Universität von Al'Anfa - Leibmagier-Zweig", "Universität von Al'Anfa|Leibmagier-Zweig");
		professionReplacements.put("Universität von Al'Anfa - Seekriegs-Zweig", "Universität von Al'Anfa|Seekriegs-Zweig");
		professionReplacements.put("Akademie der Geistesreisen zu Belhanka", "Akademie der Geistreisen zu Belhanka");
		professionReplacements.put("Seminar der elfischen Verständigung zu Donnerbach",
				"Seminar der Elfischen Verständigung und natürlichen Heilung zu Donnerbach|Verständigungszweig");
		professionReplacements.put("Seminar der natürlichen Heilung zu Donnerbach",
				"Seminar der Elfischen Verständigung und natürlichen Heilung zu Donnerbach|Heilungszweig");
		professionReplacements.put("Konzil der Elemente zu Drakonia - Eiselementarist", "Konzil der Elemente zu Drakonia|Eiselementarist");
		professionReplacements.put("Konzil der Elemente zu Drakonia - Erzelementarist", "Konzil der Elemente zu Drakonia|Erzelementarist");
		professionReplacements.put("Konzil der Elemente zu Drakonia - Feuerelementarist", "Konzil der Elemente zu Drakonia|Feuerelementarist");
		professionReplacements.put("Konzil der Elemente zu Drakonia - Humuselementarist", "Konzil der Elemente zu Drakonia|Humuselementarist");
		professionReplacements.put("Konzil der Elemente zu Drakonia - Luftelementarist", "Konzil der Elemente zu Drakonia|Luftelementarist");
		professionReplacements.put("Konzil der Elemente zu Drakonia - Wasserelementarist", "Konzil der Elemente zu Drakonia|Wasserelementarist");
		professionReplacements.put("Akademie der geistigen Kraft zu Fasar: Gemäßigter Zweig", "Akademie der geistigen Kraft zu Fasar|Gemäßigter Zweig");
		professionReplacements.put("Akademie der geistigen Kraft zu Fasar: Harter Zweig", "Akademie der geistigen Kraft zu Fasar|Harter Zweig");
		professionReplacements.put("Sulman Al-Nassori", "Drachenei-Akademie zu Khunchom|Magier der Sulman Al-Nassori");
		professionReplacements.put("Runenzauberer (UdW)", "Runenzauberer");
		professionReplacements.put("Arcanes Institut Punin - Metamagie, Beschwörungen, Herbeirufung",
				"Akademie der hohen Magie & arcanes Institut zu Punin|Punin I");
		professionReplacements.put("Arcanes Institut Punin - Metamagie, magische Hellsicht, Kraftlinien, Artefakte",
				"Akademie der hohen Magie & arcanes Institut zu Punin|Punin II");
		professionReplacements.put("Pentagramm-Akademie zu Rashdul (Elementarer Zweig)", "Pentagramm-Akademie zu Rashdul|Elementarer Zweig");
		professionReplacements.put("Pentagramm-Akademie zu Rashdul (dämonischer Zweig)", "Pentagramm-Akademie zu Rashdul|Dämonischer Zweig");
		professionReplacements.put("Stoerrebrandt-Kolleg zu Riva (Magischer Berater)", "Stoerrebrandt-Kolleg zu Riva|Magischer Berater");
		professionReplacements.put("Stoerrebrandt-Kolleg zu Riva (Magischer Leibwächter)", "Stoerrebrandt-Kolleg zu Riva|Magischer Leibwächter");
		professionReplacements.put("Informations-Institut zu Rommilys", "Informations-Institut Rommilys");
		professionReplacements.put("Schule der vierfachen Verwandlung zu Sinoda", "Schule der Vierfachen Verwandlung zu Sinoda");
		professionReplacements.put("Kreis der Einfühlung", "Kreis der Einfühlung in den nördlichen Salamandersteinen");
		professionReplacements.put("Tulamidische Sharisad (magisch)", "Tulamidische Sharisad|Zaubertänzer");
		professionReplacements.put("Novadische Sharisad (magisch)", "Novadische Sharisad|Zaubertänzer");
		professionReplacements.put("Zahorischer Hazaqi (magisch)", "Zahorischer Hazaqi|Zaubertänzer");
		professionReplacements.put("Aranischer Majuna (magisch)", "Aranischer Majuna|Zaubertänzer");
		professionReplacements.put("Waldmenschen", "Stammeskrieger|Waldmenschen-Stammeskrieger");
		professionReplacements.put("Fjarninger", "Stammeskrieger|Stammeskrieger der Fjarninger");
		professionReplacements.put("Gjalskerländer", "Stammeskrieger|Stammeskrieger der Gjalskerländer");
		professionReplacements.put("Ferkina", "Stammeskrieger|Ferkina-Krieger");
		professionReplacements.put("Trollzacker", "Stammeskrieger|Stammeskrieger der Trollzacker");
		professionReplacements.put("Goblin", "Stammeskrieger|Goblin-Stammeskrieger");
		professionReplacements.put("Ork", "Stammeskrieger|Orkischer Stammeskrieger");
		professionReplacements.put("Achaz", "Stammeskrieger|Achaz-Stammeskrieger");
		professionReplacements.put("Brobim", "Stammeskrieger|Brobim-Stammeskrieger");
		professionReplacements.put("Beni Dervez", "Stammeskrieger der Beni Dervez");
		professionReplacements.put("Tarisharim", "Hadjinim|Tarisharim");
		professionReplacements.put("Al'Drakorhim", "Hadjinim|Al'Drakorhim");
		professionReplacements.put("Beni Uchakâni", "Hadjinim|Beni Uchakâni");
		professionReplacements.put("Praios", "Geweihter des Praios");
		professionReplacements.put("Praios (Hüterorden)", "Geweihter des Praios|Ordensgeweihter vom Hüterorden");
		professionReplacements.put("Praios (Bannstrahler)", "Bannstrahler|Geweihtes Mitglied");
		professionReplacements.put("Bannstrahler (ungeweiht)", "Bannstrahler|Nicht geweihtes Mitglied");
		professionReplacements.put("Rondra", "Geweihter der Rondra");
		professionReplacements.put("Rondra, Rhodenstein", "Geweihter der Rondra|Ordensgeweihter vom Rhodenstein");
		professionReplacements.put("Rondra, Amazone", "Rondrageweihte Amazone");
		professionReplacements.put("Efferd, Noviziat im Binnenland", "Geweihter des Efferd|Noviziat im Binnenland");
		professionReplacements.put("Efferd, Noviziat am Rand der Khôm", "Geweihter des Efferd|Noviziat am Rand der Khôm");
		professionReplacements.put("Efferd, Noviziat an der Küste", "Geweihter des Efferd|Noviziat an der Küste");
		professionReplacements.put("Travia", "Geweihter der Travia");
		professionReplacements.put("Travia, Badilakaner", "Geweihter der Travia|Geweihter Badilakaner");
		professionReplacements.put("Travia aus Thorwal (UdW)", "Geweihter der Travia|Thorwal");
		professionReplacements.put("Boron, Puniner Ritus", "Geweihter des Boron|Puniner Ritus");
		professionReplacements.put("Boron, Al'Anfaner Ritus", "Geweihter des Boron|Al'Anfaner Ritus");
		professionReplacements.put("Boron (Golgarit)", "Ordenskrieger der Golgariten|Geweihtes Mitglied");
		professionReplacements.put("Golgarit (ungeweiht)", "Ordenskrieger der Golgariten|Nicht geweihtes Mitglied");
		professionReplacements.put("Orden des schwarzen Raben (Rabengarde) zu Land", "Krieger vom Orden des Schwarzen Raben|Rabengarde zu Land");
		professionReplacements.put("Orden des schwarzen Raben (Rabengarde) zur See", "Krieger vom Orden des Schwarzen Raben|Rabengarde zur See");
		professionReplacements.put("Hesinde, Pastori", "Geweihter der Hesinde|Pastori");
		professionReplacements.put("Hesinde, Satori", "Geweihter der Hesinde|Satori");
		professionReplacements.put("Hesinde, Freidenker", "Geweihter der Hesinde|Freidenker");
		professionReplacements.put("Hesinde, Draconiter", "Geweihter der Hesinde|Draconiter, sakraler Zweig");
		professionReplacements.put("Firun, Waldläufer", "Geweihter des Firun|Waldläufer");
		professionReplacements.put("Firun, Hüter der Jagd", "Geweihter des Firun|Hüter der Jagd");
		professionReplacements.put("Firun aus Thorwal (UdW)", "Geweihter des Firun|Waldläufer|Thorwal");
		professionReplacements.put("Tsa", "Geweihter der Tsa");
		professionReplacements.put("Tsa, Koboldfreund", "Geweihter der Tsa|Koboldfreund");
		professionReplacements.put("Tsa, Freiheitskämpfer", "Geweihter der Tsa|Freiheitskämpfer");
		professionReplacements.put("Phex", "Geweihter des Phex");
		professionReplacements.put("Phex, Beutelschneider", "Geweihter des Phex|Beutelschneider");
		professionReplacements.put("Phex, Fassadenkletterer", "Geweihter des Phex|Fassadenkletterer");
		professionReplacements.put("Phex, Betrüger", "Geweihter des Phex|Betrüger");
		professionReplacements.put("Phex, Intrigant", "Geweihter des Phex|Intrigant");
		professionReplacements.put("Phex, Händler", "Geweihter des Phex|Händler");
		professionReplacements.put("Phex, Hehler", "Geweihter des Phex|Hehler");
		professionReplacements.put("Peraine: Noviziat in einem städtischen Tempel", "Geweihter der Peraine|Noviziat in einem städtischen Tempel");
		professionReplacements.put("Peraine: Noviziat auf dem Land", "Geweihter der Peraine|Noviziat auf dem Land");
		professionReplacements.put("Peraine, Therbûnit", "Geweihter der Peraine|Peraine-Geweihter der Therbûniten");
		professionReplacements.put("Ingerimm, Traditioneller Kult", "Geweihter des Ingerimm|Traditioneller Kult");
		professionReplacements.put("Ingerimm, Ingra-Kult des Nordens", "Geweihter des Ingerimm|Ingra-Kult des Nordens");
		professionReplacements.put("Rahja", "Geweihter der Rahja");
		professionReplacements.put("Rahja, Güldenländischer Ritus", "Geweihter der Rahja|Güldenländischer Ritus");
		professionReplacements.put("Rahja, Mhanadistanischer Ritus", "Geweihter der Rahja|Mhanadistanischer Ritus");
		professionReplacements.put("Rahja, Tempel des Nordens", "Geweihter der Rahja|Tempel des Nordens");
		professionReplacements.put("Rahja aus Andergast/Nostria (UdW)", "Geweihter der Rahja|Streitende Königreiche");
		professionReplacements.put("Rahja aus Teshkal (Andergast) (UdW)", "Geweihter der Rahja|Streitende Königreiche|Teschkaler Rahjani");
		professionReplacements.put("Kavalier Rahjas", "Rahja-Kavalier");
		professionReplacements.put("Prediger vom Bund des wahren Glaubens", "Prediger vom Bund des Wahren Glaubens");
		professionReplacements.put("Aves", "Geweihter des Aves");
		professionReplacements.put("Nandus", "Geweihter des Nandus");
		professionReplacements.put("Nandus (Marktschreiber)", "Geweihter des Nandus|Marktschreiber");
		professionReplacements.put("Nandus (Volkslehrer)", "Geweihter des Nandus|Volkslehrer");
		professionReplacements.put("Nandus (Rechtshelfer)", "Geweihter des Nandus|Rechtshelfer");
		professionReplacements.put("Ifirn", "Geweihter der Ifirn");
		professionReplacements.put("Ifirn aus Thorwal (UdW)", "Geweihter der Ifirn|Thorwal");
		professionReplacements.put("Swafnir", "Geweihter des Swafnir");
		professionReplacements.put("Swafnir, Hirte der Walwütigen", "Geweihter des Swafnir|Hirte der Walwütigen");
		professionReplacements.put("Angrosch, Hüter der Esse", "Geweihter des Angrosch|Hüter der Esse");
		professionReplacements.put("Angrosch, Hüter der Tradition", "Geweihter des Angrosch|Hüter der Tradition");
		professionReplacements.put("Angrosch, Hüter der Wacht", "Geweihter des Angrosch|Hüter der Wacht");
		professionReplacements.put("Priester von Rur und Gror", "Priester von Rur und Gror|Tempelpriester von Rur und Gror");
		professionReplacements.put("Geheimer Priester von Rur und Gror",
				"Priester von Rur und Gror|Tempelpriester von Rur und Gror|Geheimer Priester auf Schwarz-Maraskan");
		professionReplacements.put("Wanderpriester von Rur und Gror", "Priester von Rur und Gror|Wanderpriester von Rur und Gror");
		professionReplacements.put("Geheimer Wanderpriester von Rur und Gror",
				"Priester von Rur und Gror|Wanderpriester von Rur und Gror|Geheimer Priester auf Schwarz-Maraskan");
		professionReplacements.put("Medizinmann (Dschungelstämme)", "Medizinmann|Dschungelstämme");
		professionReplacements.put("Medizinmann (Verlorene Stämme)", "Medizinmann|Verlorene Stämme");
		professionReplacements.put("Medizinmann (Utulus)", "Medizinmann|Waldinsel-Utulus");
		professionReplacements.put("Medizinmann (Miniwatu)", "Medizinmann|Miniwatu");
		professionReplacements.put("Medizinmann (Tocamuyac)", "Medizinmann|Tocamuyac");
		professionReplacements.put("Medizinmann (Darna)", "Medizinmann|Darna");
		professionReplacements.put("Kasknuk (Nivesen-Schamane)", "Kasknuk");
		professionReplacements.put("Nuranshar", "Nuranshâr");
		professionReplacements.put("Nuranshâr (Mheresh)", "Nuranshâr|Mherech");
		professionReplacements.put("Nuranshâr (Shai'aian)", "Nuranshâr|Shai'aian");
		professionReplacements.put("Nuranshâr (Thalusien)", "Nuranshâr|Thalusien");
		professionReplacements.put("Skuldrun: Heiler", "Skuldrun|Heiler");
		professionReplacements.put("Skuldrun: Mammut-Seher", "Skuldrun|Mammut-Seher");
		professionReplacements.put("Skuldrun: Zauberschmied", "Skuldrun|Zauberschmied");
		professionReplacements.put("Geweihter Gravesh-Priester", "Gravesh-Priester|Geweihter Gravesh-Priester");
		professionReplacements.put("Stammes-Schamanin der Goblins", "Goblin-Schamanin|Stammes-Schamanin");
		professionReplacements.put("Festumer Schamanin der Goblins", "Goblin-Schamanin|Festumer Schamanin");
		professionReplacements.put("Kammerjaegerin", "Kammerjäger");
		professionReplacements.put("Kammerjaegerin (magiebegabt)", "Magiebegabter Alchimist|Kammerjäger");
		professionReplacements.put("Erzählerin", "Erzähler");
		professionReplacements.put("Skaldin", "Skalde");
		professionReplacements.put("Skaldin aus der Runajasko (UdW)", "Skalde aus der Runajasko");
		professionReplacements.put("Erntehelferin", "Erntehelfer");
		professionReplacements.put("Feldsklavin", "Feldsklave");
		professionReplacements.put("Freibäuerin", "Freibauer");
		professionReplacements.put("Gärtnerin", "Gärtner");
		professionReplacements.put("Gutsfrau", "Gutsherr");
		professionReplacements.put("Leibeigene", "Leibeigener");
		professionReplacements.put("Magd", "Knecht");
		professionReplacements.put("Müllerin", "Müller");
		professionReplacements.put("Pflanzerin", "Pflanzer");
		professionReplacements.put("Viehzüchterin", "Viehzüchter");
		professionReplacements.put("Winzerin", "Winzer");
		professionReplacements.put("Pilzzüchterin", "Pilzzüchter");
		professionReplacements.put("Schachtfegerin", "Schachtfeger");
		professionReplacements.put("Botenläuferin", "Botenläufer");
		professionReplacements.put("Botenreiterin", "Botenreiter");
		professionReplacements.put("Erzieherin", "Erzieher der Achaz");
		professionReplacements.put("Hausdienerin", "Hausdiener");
		professionReplacements.put("Hausmagd", "Hausknecht");
		professionReplacements.put("Haussklavin", "Haussklave");
		professionReplacements.put("Haussklavin aus Al'Anfa", "Haussklave aus Al'Anfa");
		professionReplacements.put("Kutscherin", "Kutscher");
		professionReplacements.put("Zofe", "Leibdiener");
		professionReplacements.put("Haindruidin", "Haindruide");
		professionReplacements.put("Hüterin der Macht", "Hüter der Macht");
		professionReplacements.put("Konzilsdruidin (Eiselementarist)", "Konzilsdruide|Eiselementarist");
		professionReplacements.put("Konzilsdruidin (Erzelementaristin)", "Konzilsdruide|Erzelementarist");
		professionReplacements.put("Konzilsdruidin (Feuerelementaristin)", "Konzilsdruide|Feuerelementarist");
		professionReplacements.put("Konzilsdruidin (Humuselementaristin)", "Konzilsdruide|Humuselementarist");
		professionReplacements.put("Konzilsdruidin (Luftelementaristin)", "Konzilsdruide|Luftelementarist");
		professionReplacements.put("Konzilsdruidin (Wasserelementaristin)", "Konzilsdruide|Wasserelementarist");
		professionReplacements.put("Mehrerin der Macht", "Mehrer der Macht");
		professionReplacements.put("Sumupriesterin", "Sumupriester");
		professionReplacements.put("Apothekaria", "Apothekarius");
		professionReplacements.put("Baumeisterin/Deichmeisterin", "Baumeister");
		professionReplacements.put("Druckerin", "Drucker");
		professionReplacements.put("Hüttenkundige/Bronzegießerin/Eisengießerin", "Hüttenkundiger");
		professionReplacements.put("Mechanika", "Mechanikus");
		professionReplacements.put("Schiffbauerin", "Schiffbauer");
		professionReplacements.put("traditionelle Schiffbauerin (UdW)", "Traditioneller Schiffbauer");
		professionReplacements.put("Tresorbauerin", "Tresorbauer");
		professionReplacements.put("Uhrmacherin", "Uhrmacher");
		professionReplacements.put("Grabräuberin", "Grabräuber");
		professionReplacements.put("Auelfische Kämpferin", "Auelfischer Kämpfer");
		professionReplacements.put("Firnelfische Kämpferin", "Firnelfischer Kämpfer");
		professionReplacements.put("Steppenelfische Kämpferin", "Steppenelfischer Kämpfer");
		professionReplacements.put("Waldelfische Kämpferin", "Waldelfischer Kämpfer");
		professionReplacements.put("Bergungs-, Schwamm- oder Korallentaucherin", "Bergungstaucher");
		professionReplacements.put("Harpunierin", "Harpunier");
		professionReplacements.put("Perlenfischerin", "Perlenfischer");
		professionReplacements.put("Seefischerin", "Seefischer");
		professionReplacements.put("Unterwasserjägerin", "Unterwasserjäger");
		professionReplacements.put("Akademiegardistin/Tempelgardistin/Ehrengardistin", "Akademiegardist");
		professionReplacements.put("Aranische Sippenkriegerin", "Aranischer Sippenkrieger");
		professionReplacements.put("Schließerin", "Schließer");
		professionReplacements.put("städtische Bogenschützin (UdW)", "Stadtgardist|Städtischer Bogenschütze");
		professionReplacements.put("Straßenwächterin", "Straßenwächter");
		professionReplacements.put("Tempelgardistin der Stadt des Schweigens", "Akademiegardist|Tempelgardist der Stadt des Schweigens");
		professionReplacements.put("Akrobatin/Tänzerin", "Akrobat");
		professionReplacements.put("Dompteuse", "Dompteur");
		professionReplacements.put("Musika", "Musikus");
		professionReplacements.put("Possenreißerin", "Possenreißer");
		professionReplacements.put("Schauspielerin", "Schauspieler");
		professionReplacements.put("Schlangenbeschwörerin", "Schlangenbeschwörer");
		professionReplacements.put("Vagantin", "Vagant");
		professionReplacements.put("Anatomin", "Anatom");
		professionReplacements.put("Historikerin", "Historiker");
		professionReplacements.put("Mathematica", "Mathematicus");
		professionReplacements.put("Medica", "Medicus");
		professionReplacements.put("Philosophin/Metaphysikerin", "Philosoph");
		professionReplacements.put("Rechtsgelehrte", "Rechtsgelehrter");
		professionReplacements.put("Sprachenkundlerin", "Sprachenkunder");
		professionReplacements.put("Völkerkundlerin/Sagenkundlerin", "Völkerkundler");
		professionReplacements.put("Zahlenmystikerin", "Zahlenmystiker");
		professionReplacements.put("Dienerin Sumus", "Diener Sumus");
		professionReplacements.put("Herrin der Erde", "Herr der Erde");
		professionReplacements.put("Angrosch, Hüterin der Esse", "Geweihter des Angrosch|Hüter der Esse");
		professionReplacements.put("Angrosch, Hüterin der Tradition", "Geweihter des Angrosch|Hüter der Tradition");
		professionReplacements.put("Angrosch, Hüterin der Wacht", "Geweihter des Angrosch|Hüter der Wacht");
		professionReplacements.put("Boron (Golgaritin)", "Ordenskrieger der Golgariten|Geweihtes Mitglied");
		professionReplacements.put("Firun, Waldläuferin", "Geweihter des Firun|Waldläufer");
		professionReplacements.put("Firun, Hüterin der Jagd", "Geweihter des Firun|Hüter der Jagd");
		professionReplacements.put("Priesterin von Rur und Gror", "Priester von Rur und Gror|Tempelpriester von Rur und Gror");
		professionReplacements.put("Geheime Priesterin von Rur und Gror",
				"Priester von Rur und Gror|Tempelpriester von Rur und Gror|Geheimer Priester auf Schwarz-Maraskan");
		professionReplacements.put("Wanderpriesterin von Rur und Gror", "Priester von Rur und Gror|Wanderpriester von Rur und Gror");
		professionReplacements.put("Geheime Wanderpriesterin von Rur und Gror",
				"Priester von Rur und Gror|Wanderpriester von Rur und Gror|Geheimer Priester auf Schwarz-Maraskan");
		professionReplacements.put("Hesinde, Freidenkerin", "Geweihter der Hesinde|Freidenker");
		professionReplacements.put("Hesinde, Draconiterin", "Geweihter der Hesinde|Draconiter, sakraler Zweig");
		professionReplacements.put("Nandus (Marktschreiberin)", "Geweihter des Nandus|Marktschreiber");
		professionReplacements.put("Nandus (Volkslehrerin)", "Geweihter des Nandus|Volkslehrer");
		professionReplacements.put("Nandus (Rechtshelferin)", "Geweihter des Nandus|Rechtshelfer");
		professionReplacements.put("Phex, Beutelschneiderin", "Geweihter des Phex|Beutelschneider");
		professionReplacements.put("Phex, Fassadenklettererin", "Geweihter des Phex|Fassadenkletterer");
		professionReplacements.put("Phex, Betrügerin", "Geweihter des Phex|Betrüger");
		professionReplacements.put("Phex, Intrigantin", "Geweihter des Phex|Intrigant");
		professionReplacements.put("Phex, Händlerin", "Geweihter des Phex|Händler");
		professionReplacements.put("Phex, Hehlerin", "Geweihter des Phex|Hehler");
		professionReplacements.put("Praios (Bannstrahlerin)", "Bannstrahler|Geweihtes Mitglied");
		professionReplacements.put("Predigerin vom Bund des wahren Glaubens", "Prediger vom Bund des Wahren Glaubens");
		professionReplacements.put("Priesterin der H'Szint", "Priester der H'Szint");
		professionReplacements.put("Priesterin der Zsahh", "Priester der Zsahh");
		professionReplacements.put("Swafnir, Hirtin der Walwütigen", "Geweihter des Swafnir|Hirte der Walwütigen");
		professionReplacements.put("Travia, Badilakanerin", "Geweihter der Travia|Geweihter Badilakaner");
		professionReplacements.put("Tsa, Koboldfreundin", "Geweihter der Tsa|Koboldfreund");
		professionReplacements.put("Tsa, Freiheitskämpferin", "Geweihter der Tsa|Freiheitskämpfer");
		professionReplacements.put("Fasarer Gladiatorin", "Fasarer Gladiator");
		professionReplacements.put("Schaukämpferin", "Schaukämpfer");
		professionReplacements.put("Kopfgeldjägerin", "Kopfgeldjäger");
		professionReplacements.put("Sklavenjägerin", "Sklavenjäger");
		professionReplacements.put("Fahrende Händlerin", "Fahrender Händler");
		professionReplacements.put("Geldwechslerin", "Geldwechsler");
		professionReplacements.put("Großhändlerin", "Großhändler");
		professionReplacements.put("Hausiererin", "Hausierer");
		professionReplacements.put("Hehlerin", "Hehler");
		professionReplacements.put("Krämerin", "Krämer");
		professionReplacements.put("Tauschhändlerin", "Tauschhändler");
		professionReplacements.put("Hirtin", "Hirte");
		professionReplacements.put("Kleintierzüchterin", "Kleintierzüchter");
		professionReplacements.put("Nivesische Karenhirtin", "Nivesischer Karenhirte");
		professionReplacements.put("Rinderhirtin", "Rinderhirte");
		professionReplacements.put("Schäferin", "Schäfer");
		professionReplacements.put("Viehdiebin", "Viehdieb");
		professionReplacements.put("Wasserbüffelhirtin", "Wasserbüffelhirte");
		professionReplacements.put("Bildhauerin", "Bildhauer");
		professionReplacements.put("Darstellerin", "Darsteller");
		professionReplacements.put("Hofmusica", "Hofmusicus");
		professionReplacements.put("Kalligrahpin", "Kalligraph");
		professionReplacements.put("Malerin", "Maler");
		professionReplacements.put("Tanzlehrerin", "Tanzlehrer");
		professionReplacements.put("Fallenstellerin", "Fallensteller");
		professionReplacements.put("Fallenstellerin (UdW)", "Robbenjäger|Fallensteller");
		professionReplacements.put("Robbenjägerin (UdW)", "Robbenjäger");
		professionReplacements.put("Stammesjägerin", "Stammesjäger");
		professionReplacements.put("Wildhüterin", "Wildhüter");
		professionReplacements.put("Kopfgeldjägerin", "Kopfgeldjäger");
		professionReplacements.put("Kopfgeldjägerin", "Kopfgeldjäger");
		professionReplacements.put("Kämpferin aus einer Ottajasko (UdW)", "Kämpfer einer Ottajasko");
		professionReplacements.put("Kämpferin aus einer Sippe (UdW)", "Kämpfer einer Sippe");
		professionReplacements.put("Kämpferin/Robbenjägerin aus einer Ottajasko (UdW)", "Kämpfer einer Ottajasko|Robbenjäger");
		professionReplacements.put("Kämpferin/Seefahrerin aus einer Ottajasko (UdW)", "Kämpfer einer Ottajasko|Robbenjäger");
		professionReplacements.put("Kämpferin/Robbenjägerin aus einer Sippe (UdW)", "Kämpfer einer Sippe|Seefahrer");
		professionReplacements.put("Kämpferin/Seefahrerin aus einer Sippe (UdW)", "Kämpfer einer Sippe|Seefahrer");
		professionReplacements.put("Karawanenführerin", "Karawanenführer");
		professionReplacements.put("Salzgängerin", "Salzgänger");
		professionReplacements.put("Standard-Kriegerin", "Standard-Krieger");
		professionReplacements.put("Kurtisane", "Gesellschafter");
		professionReplacements.put("Hure", "Lustknabe");
		professionReplacements.put("Unterhändlerin", "Unterhändler");
		professionReplacements.put("Bannstrahlerin (ungeweiht)", "Bannstrahler|Nicht geweihtes Mitglied");
		professionReplacements.put("Gänseritterin", "Krieger aus Rommilys|Gänseritter");
		professionReplacements.put("Golgaritin (ungeweiht)", "Ordenskrieger der Golgariten|Nicht geweihtes Mitglied");
		professionReplacements.put("Kavaliera Rahjas", "Rahja-Kavalier");
		professionReplacements.put("Säbeltänzerin", "Säbeltänzer");
		professionReplacements.put("Goldsucherin oder Prospektorin", "Prospektor");
		professionReplacements.put("Kräutersammlerin", "Kräutersammler");
		professionReplacements.put("Sammlerin", "Sammler");
		professionReplacements.put("Knappin", "Knappe");
		professionReplacements.put("Knappin aus Andergast/Nostria (UdW)",
				"Ritter alten Schlags|Knappe des traditionellen Ritters|Knappe aus den Streitenden Königreichen");
		professionReplacements.put("Knappin des traditionellen Ritters", "Ritter alten Schlags|Knappe des traditionellen Ritters");
		professionReplacements.put("Ritterin alten Schlags", "Ritter alten Schlags");
		professionReplacements.put("Branach-Dûn", "Brenoch-Dûn");
		professionReplacements.put("Medizinfrau (Dschungelstämme)", "Medizinmann|Dschungelstämme");
		professionReplacements.put("Medizinfrau (Verlorene Stämme)", "Medizinmann|Verlorene Stämme");
		professionReplacements.put("Medizinfrau (Utulus)", "Medizinmann|Waldinsel-Utulus");
		professionReplacements.put("Medizinfrau (Miniwatu)", "Medizinmann|Miniwatu");
		professionReplacements.put("Medizinfrau (Tocamuyac)", "Medizinmann|Tocamuyac");
		professionReplacements.put("Medizinfrau (Darna)", "Medizinmann|Darna");
		professionReplacements.put("Kaskju (Nivesen-Schamanin)", "Kasknuk");
		professionReplacements.put("Skuldrun: Heilerin", "Skuldrun|Heiler");
		professionReplacements.put("Skuldrun: Mammut-Seherin", "Skuldrun|Mammut-Seher");
		professionReplacements.put("Skuldrun: Zauberschmiedin", "Skuldrun|Zauberschmied");
		professionReplacements.put("Gravesh-Priesterin", "Gravesh-Priester");
		professionReplacements.put("Geweihte Gravesh-Priesterin", "Gravesh-Priester|Geweihter Gravesh-Priester");
		professionReplacements.put("Rikai-Priesterin", "Rikai-Priester");
		professionReplacements.put("Tairach-Priesterin", "Tairach-Priester");
		professionReplacements.put("Schamanin der Achaz", "Schamane der Achaz");
		professionReplacements.put("Hofscharlatanin", "Hofscharlatan");
		professionReplacements.put("Jahrmarktszauberin", "Jahrmarktszauberer");
		professionReplacements.put("Magische Quacksalberin", "Magischer Quacksalber");
		professionReplacements.put("Scharlatanische Seherin", "Scharlatanischer Seher");
		professionReplacements.put("Theaterzauberin", "Theaterzauberer");
		professionReplacements.put("Trickbetrügerin", "Trickbetrüger");
		professionReplacements.put("Hofnarrin", "Hofnarr");
		professionReplacements.put("Possenreißerin", "Possenreißer");
		professionReplacements.put("Schöpferin", "Schöpfer");
		professionReplacements.put("Vagabundin", "Vagabund");
		professionReplacements.put("Visionärin", "Visionär");
		professionReplacements.put("Fährfrau", "Fährmann");
		professionReplacements.put("Flößerin", "Flößer");
		professionReplacements.put("Flusspiratin", "Flusspirat");
		professionReplacements.put("Flussschifferin", "Flussschiffer");
		professionReplacements.put("Lotsin", "Lotse");
		professionReplacements.put("Zöllnerin", "Zöllner");
		professionReplacements.put("Amtsschreiberin", "Amtsschreiber");
		professionReplacements.put("Kontoristin", "Kontorist");
		professionReplacements.put("Kopistin", "Kopist");
		professionReplacements.put("Schreiberin", "Schreiber");
		professionReplacements.put("Pamphletistin", "Pamphletist");
		professionReplacements.put("Erzzwergischer Schwertgesellin", "Erzzwergischer Schwertgeselle");
		professionReplacements.put("Matrosin", "Matrose");
		professionReplacements.put("Navigatorin", "Navigator");
		professionReplacements.put("Piratin", "Pirat");
		professionReplacements.put("Robbenjägerin", "Robbenjäger");
		professionReplacements.put("Walfängerin/Haijägerin", "Walfänger");
		professionReplacements.put("Artilleristin", "Artillerist");
		professionReplacements.put("Aufgesessene Schützin", "Aufgesessener Schütze");
		professionReplacements.put("Berittene Schützin", "Berittener Schütze");
		professionReplacements.put("Sappeurin", "Sappeur");
		professionReplacements.put("Schützin", "Schütze");
		professionReplacements.put("Seeartilleristin", "Seeartillerist");
		professionReplacements.put("Seesoldatin", "Seesoldat");
		professionReplacements.put("Streitwagenlenkerin", "Streitwagenlenker");
		professionReplacements.put("Leibwächterin", "Leibwächter");
		professionReplacements.put("Premer Seesöldnerin (UdW)", "Premer Seesöldner");
		professionReplacements.put("Schlachtreiterin", "Schlachtreiter");
		professionReplacements.put("Sklaven-Aufseherin", "Sklaven-Aufseher");
		professionReplacements.put("Geheimagentin", "Geheimagent");
		professionReplacements.put("Informantin", "Informant");
		professionReplacements.put("Nanduriatin", "Nanduriat");
		professionReplacements.put("Novadische Wüstenkriegerin", "Novadischer Wüstenkrieger");
		professionReplacements.put("Achmad'sunni", "Novadischer Wüstenkrieger|Achmad'sunni");
		professionReplacements.put("Banditin", "Bandit");
		professionReplacements.put("Freischärlerin", "Freischärler");
		professionReplacements.put("Kutschenräuberin", "Kutschenräuber");
		professionReplacements.put("Thalusische Wegelagerin", "Thalusischer Wegelagerer");
		professionReplacements.put("Wegelagerin", "Wegelagerer");
		professionReplacements.put("Hochstaplerin", "Hochstapler");
		professionReplacements.put("Schieberin", "Schieber");
		professionReplacements.put("Spielerin", "Spieler");
		professionReplacements.put("Zuhälterin", "Zuhälter");
		professionReplacements.put("Bauhelferin", "Bauhelfer");
		professionReplacements.put("Holzfällerin", "Holzfäller");
		professionReplacements.put("Köhlerin", "Köhler");
		professionReplacements.put("Lastenträgerin", "Lastenträger");
		professionReplacements.put("Palmschneiderin", "Palmschneider");
		professionReplacements.put("Schauerfrau", "Schauerleute");
		professionReplacements.put("Zahorische Hazaqi", "Zahorischer Hazaqi");
		professionReplacements.put("Zahorische Hazaqi (magisch)", "Zahorischer Hazaqi|Zaubertänzer");
		professionReplacements.put("Aranische Majuna", "Aranischer Majuna");
		professionReplacements.put("Aranische Majuna (magisch)", "Aranischer Majuna|Zaubertänzer");
		professionReplacements.put("Taugenichts/Dilletantin", "Dilletant");
		professionReplacements.put("Taugenichts/Stutzerin", "Stutzer");
		professionReplacements.put("Falknerin", "Falkner");
		professionReplacements.put("Hundeführerin", "Hundeführer");
		professionReplacements.put("Tierbändigerin", "Tierbändiger");
		professionReplacements.put("Zureiterin", "Zureiter");
		professionReplacements.put("Auenläuferin", "Auenläufer");
		professionReplacements.put("Schneeläuferin", "Schneeläufer");
		professionReplacements.put("Steppenreiterin", "Steppenreiter");
		professionReplacements.put("Wipflelläuferin", "Wipfelläufer");
		professionReplacements.put("Brutpflegerin", "Brutpfleger");
		professionReplacements.put("Feldscherin", "Feldscher");
		professionReplacements.put("Quacksalberin/Zahnreißerin", "Quacksalber");
		professionReplacements.put("Wundärztin", "Wundarzt");
		professionReplacements.put("Beschützerin", "Beschützer");
	}

	private int at, pa;

	private JSONObject hero;

	private XMLStreamReader parser;

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

	private JSONObject enterSkill(final String name, final String choice, final String text, final boolean applyEffect, final boolean cheaperSkill) {
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

	private String getLastPart(String str, final Map<String, String> replacements) {
		final int separator = str.lastIndexOf('.');
		str = str.substring(separator + 1);
		str = replacements.getOrDefault(str, str);
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
			} else if (Arrays.asList("at", "pa").contains(name)) {
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
		final String replaced = getLastPart(get("name"), cultureReplacements);
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
				for (final String variant : cultureReplacements.getOrDefault(name, name).split("\\|")) {
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
						new Tuple3<>("parade", () -> "at", () -> get("value")));
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

		final JSONObject bio = new JSONObject(hero);
		hero.put("Biografie", bio);

		bio.put("Vorname", firstName);
		if (endFirstName != name.length()) {
			bio.put("Nachname", name.substring(endFirstName + 1));
		}

		apply("held", new Tuple<>("basis", () -> parseBasis()), new Tuple<>("eigenschaften", () -> parseAttributes()), new Tuple<>("vt", () -> parseProsCons()),
				new Tuple<>("sf", () -> parseSkills()), new Tuple<>("talentliste", () -> parseTalents()), new Tuple<>("zauberliste", () -> parseSpells()),
				new Tuple<>("kampf", () -> parseFight()), new Tuple<>("gegenstände", () -> parseInventory()), new Tuple<>("geldboerse", () -> parseMoney()));

		cleanupCheaperSkills();

		ResourceManager.moveResource(hero, "characters/" + firstName);
		return hero;
	}

	private void parseHeroes(final List<JSONObject> heroes) {
		apply("helden", new Tuple<>("held", () -> heroes.add(parseHero())));
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
						variants.add("Grangor");
					} else if ("Geode".equals(finalName) && "Brobim".equals(name)) {
						variants.add("Brobim-Geode");
					} else if ("Tänzer".equals(finalName) && "Gaukler".equals(name)) {
						variants.add("Gaukler-Tänzer");
					} else {
						for (final String variant : professionReplacements.getOrDefault(name, name).split("\\|")) {
							variants.add(variant);
						}
					}
				}));

				if ("Stammeskrieger".equals(professionName) || "Geweihter".equals(professionName) || "Ordenskrieger".equals(professionName)
						|| "Schamane".equals(professionName)) {
					final String variant = variants.remove(0);
					final String replacedVariant = professionReplacements.getOrDefault(variant, variant);
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
							ErrorLogger.log("Konnte Sonderfertigkeit " + skillName + " nicht finden");
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
						for (final String variant : professionReplacements.getOrDefault(name, name).split("\\|")) {
							proVariants.add(variant);
						}
					}
				}));

				if ("Stammeskrieger".equals(proName) || "Geweihter".equals(proName) || "Ordenskrieger".equals(proName) || "Schamane".equals(proName)) {
					final String variant = proVariants.remove(0);
					final String replacedVariant = professionReplacements.getOrDefault(variant, variant);
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
								if (proMods.size() == 0 && !proName.equals(name) || proMods.size() > 0 && !proMods.getString(proMods.size() - 1).equals(name)) {
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
								ErrorLogger.log("Konnte Sonderfertigkeit " + skillName + " nicht finden");
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
							ErrorLogger.log("Konnte Sonderfertigkeit " + skillName + " nicht finden");
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
			name = proConReplacements.getOrDefault(name, name);
			String choice = get("value");
			String text = get("value");
			String value = get("value");

			if (Arrays.asList("Breitgefächerte Bildung", "Veteran").contains(name))
				return;
			else if (Arrays.asList("Astrale Regeneration", "Schnelle Heilung", "Wesen der Nacht", "Fluch der Finsternis", "Madas Fluch", "Schlafstörungen")
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
					switch (text) {
					case "tierische Gifte":
					case "mineralische Gifte":
					case "pflanzliche Gifte":
					case "alchimistische Gifte":
					case "Atemgifte":
					case "Einnahmegifte":
					case "Kontaktgifte":
					case "Blut-/Waffengifte":
						name = prefix + text;
						break;
					case "alle Gifte":
						name = "Allgemeine " + prefix + "Gifte";
						break;
					default:
						name = prefix + "Gift";
						break;
					}
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
				switch (get("value")) {
				case "Sicht":
					name = "Herausragende Sicht";
					break;
				case "Geruchssinn":
					name = "Herausragender Geruchssinn";
					break;
				case "Gehör":
					name = "Herausragendes Gehör";
					break;
				case "Tastsinn":
					name = "Herausragender Tastsinn";
					break;
				}
			} else if ("Wolfskind".equals(name)) {
				if (!"intuitiv".equals(get("value"))) {
					name += ", freiwillige Verwandlung";
				}
			} else if ("Zusätzliche Gliedmaßen".equals(name)) {
				name = "Zusätzliche Gliedmaße (" + get("value") + ")";
			} else if (name.startsWith("Angst") || name.startsWith("Vorurteile") || name.startsWith("Weltfremd")) {
				if (name.startsWith("Angst") && name.charAt(10) != '(') {
					text = name.substring(10);
					value = get("value");
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
				switch (get("value")) {
				case "Sicht":
					name = "Kurzsichtig";
					break;
				case "Geruchssinn":
					name = "Eingeschränkter Geruchssinn";
					break;
				case "Gehör":
					name = "Schwerhörig";
					break;
				case "Tastsinn":
					name = "Eingeschränkter Tastsinn";
					break;
				}
			} else if ("Moralkodex".equals(name)) {
				value = "1";
			}
			choice = replaceTalent(choice);
			choice = spellReplacements.getOrDefault(choice, choice);
			choice = replaceSkill(choice);
			choice = groupReplacements.getOrDefault(choice, choice);
			if (pros.containsKey(name)) {
				final JSONObject pro = pros.getObj(name);
				if (pro.containsKey("Freitext") || pro.containsKey("Auswahl")) {
					final JSONArray actualPro = actualPros.getArr(name);
					final JSONObject currentPro = new JSONObject(actualPro);
					actualPro.add(currentPro);
					if (pro.containsKey("Auswahl")) {
						currentPro.put("Auswahl", choice);
					}
					if (pro.containsKey("Freitext")) {
						currentPro.put("Freitext", text);
					}
					if (pro.getBoolOrDefault("Abgestuft", false)) {
						currentPro.put("Stufe", Integer.parseInt(value));
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
						currentCon.put("Auswahl", choice);
					}
					if (con.containsKey("Freitext")) {
						currentCon.put("Freitext", text);
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
					if (Arrays.asList("Behäbig", "Kleinwüchsig", "Lahm", "Zwergenwuchs").contains(name)) {
						HeroUtil.applyEffect(hero, name, con, currentCon);
					}
				}
			}
		}));
	}

	private void parseRace() {
		final JSONObject bio = hero.getObj("Biografie");
		final String replaced = getLastPart(get("name"), raceReplacements);
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
				for (final String variant : raceReplacements.getOrDefault(name, name).split("\\|")) {
					variants.add(variant);
				}
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

		apply("rasse", new Tuple<>("groesse", () -> {
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
				choice = spellReplacements.getOrDefault(choice, choice);
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
				choice = repReplacements.getOrDefault(choice, choice);
				name = "Repräsentation";
			} else if (name.startsWith("Ritualkenntnis")) {
				choice = name.substring(16);
				choice = choice.startsWith("Zaubertänzer") ? "Zaubertänze" : ritualKnowledge.getOrDefault(choice, choice);
				name = "Ritualkenntnis";
			} else if (name.startsWith("Liturgiekenntnis")) {
				choice = name.substring(18, name.length() - 1);
				name = "Liturgiekenntnis";
			} else if ("Kulturkunde".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("kultur", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, value, "", false, false);
				}
				return;
			} else if ("Ortskenntnis".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("auswahl", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, "", value, false, false);
				}
				return;
			} else if ("Berufsgeheimnis".equals(name)) {
				apply("sonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill("Berufsgeheimnis", "", values.getOrDefault("2", ""), false, false);
				}));
				return;
			} else if ("Scharfschütze".equals(name) || "Meisterschütze".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("talent", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, value, "", false, false);
				}
				return;
			} else if ("Schnellladen".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("talent", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill("Schnellladen (" + value + ")", "", "", false, false);
				}
				return;
			} else if ("Rüstungsgewöhnung I".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("gegenstand", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, "", value, false, false);
				}
				return;
			} else if ("Waffenmeister".equals(name)) {
				apply("sonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					final JSONObject skill = enterSkill("Waffenmeister", values.getOrDefault("1", ""), values.getOrDefault("0", ""), false, false);
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
			} else if ("Akoluth".equals(name)) {
				final Set<String> values = extract("sonderfertigkeit", new Tuple3<>("auswahl", () -> get("name"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, value, "", false, false);
				}
				return;
			}

			final boolean applyEffect = Arrays.asList("Kampfgespür", "Kampfreflexe").contains(name);

			enterSkill(name, choice, text, applyEffect, false);
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
				choice = spellReplacements.getOrDefault(choice, choice);
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
				choice = repReplacements.getOrDefault(choice, choice);
				name = "Repräsentation";
			} else if (name.startsWith("Ritualkenntnis")) {
				choice = name.substring(16);
				choice = choice.startsWith("Zaubertänzer") ? "Zaubertänze" : ritualKnowledge.getOrDefault(choice, choice);
				name = "Ritualkenntnis";
			} else if (name.startsWith("Liturgiekenntnis")) {
				choice = name.substring(18, name.length() - 1);
				name = "Liturgiekenntnis";
			} else if ("Kulturkunde".equals(name) || "Scharfschütze".equals(name) || "Meisterschütze".equals(name) || "Akoluth".equals(name)) {
				final Set<String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("auswahl", () -> get("auswahl"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, value, "", false, true);
				}
				return;
			} else if ("Ortskenntnis".equals(name) || "Rüstungsgewöhnung I".equals(name)) {
				final Set<String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("auswahl", () -> get("auswahl"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill(name, "", value, false, true);
				}
				return;
			} else if ("Berufsgeheimnis".equals(name)) {
				apply("verbilligtesonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill("Berufsgeheimnis", "", values.getOrDefault("2", ""), false, true);
				}));
				return;
			} else if ("Schnellladen".equals(name)) {
				final Set<String> values = extract("verbilligtesonderfertigkeit", new Tuple3<>("auswahl", () -> get("auswahl"), () -> null)).keySet();
				for (final String value : values) {
					enterSkill("Schnellladen (" + value + ")", "", "", false, true);
				}
				return;
			} else if ("Waffenmeister".equals(name)) {
				apply("verbilligtesonderfertigkeit", new Tuple<>("auswahl", () -> {
					final Map<String, String> values = extract("auswahl", new Tuple3<>("wahl", () -> get("position"), () -> get("value")));
					enterSkill("Waffenmeister", values.getOrDefault("1", ""), values.getOrDefault("0", ""), false, true);
				}));
				return;
			}

			enterSkill(name, choice, text, false, true);
		}));
	}

	private void parseSpells() {
		final JSONObject spells = hero.getObj("Zauber");

		apply("zauberliste", new Tuple<>("zauber", () -> {
			String name = get("name");
			name = spellReplacements.getOrDefault(name, name);
			final String variant = get("variante");
			if ("Dämonenbann".equals(name)) {
				name = variant + "bann";
			}
			final JSONObject spell = HeroUtil.findTalent(name)._1;
			if (spell != null) {
				final JSONObject actualSpell = spells.getObj(name);
				final String rep = representations.getOrDefault(get("repraesentation"), "Mag");
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
		in = skillReplacements.getOrDefault(in, in);
		return in;
	}

	private String replaceTalent(String in) {
		if (in == null) return null;
		in = talentReplacements.getOrDefault(in, in);
		if (in.startsWith("Sprachen kennen") || in.startsWith("Lesen/Schreiben")) {
			in = in.substring(16);
		} else if (in.startsWith("Heilkunde")) {
			in = "Heilkunde " + in.substring(11);
		} else if (in.startsWith("Ritualkenntnis")) {
			in = in.substring(16);
			in = in.startsWith("Zaubertänzer") ? "Zaubertänze" : ritualKnowledge.getOrDefault(in, in);
		} else if (in.startsWith("Liturgiekenntnis")) {
			in = in.substring(18, in.length() - 1);
		}

		return in;
	}

}
