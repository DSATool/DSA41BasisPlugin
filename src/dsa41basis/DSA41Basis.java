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
package dsa41basis;

import java.util.List;

import dsa41basis.datePicker.DatePickerController;
import dsa41basis.util.ResourceSanitizer;
import dsatool.credits.Credits;
import dsatool.gui.Main;
import dsatool.plugins.Plugin;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.settings.StringChoiceSetting;
import dsatool.util.Util;

/**
 * Basic plugin for the DSA 4.1 rules
 *
 * @author Dominik Helm
 */
public class DSA41Basis extends Plugin {
	private DatePickerController datePicker = null;

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "DSA41Basis";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#initialize()
	 */
	@Override
	public void initialize() {
		getNotifications = true;

		ResourceManager.setDiscriminatingAttribute("Bücher");
		ResourceManager.setPriorities(Settings.getSettingArray("Allgemein", "Bücher").getStrings());

		ResourceManager.addResourceSanitizer(ResourceSanitizer.heroSanitizer);

		Settings.addSetting(new StringChoiceSetting("Jahreswechsel", "Jahreswechsel", List.of("Jahreswechsel", "Jahreszeiten", "Astronomisch"),
				"Allgemein", "Jahreswechsel"));
		Settings.addSetting(new StringChoiceSetting("Rüstungsart", "Zonenrüstung", List.of("Zonenrüstung", "Gesamtrüstung", "Zonengesamtrüstung"),
				"Kampf", "Rüstungsart"));

		Credits.credits.add(0,
				new Credits(
						"DAS SCHWARZE AUGE, AVENTURIEN, DERE, MYRANOR, THARUN, UTHURIA und RIESLAND sind eingetragene Marken der Significant Fantasy Medienrechte GbR. Ohne vorherige schriftliche Genehmigung der Ulisses Medien und Spiel Distribution GmbH ist eine Verwendung der genannten Markenzeichen nicht gestattet.",
						null, null, "http://www.ulisses-spiele.de/", Util.getAppDir() + "/resources/logos/Fanprojekt.png"));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#load()
	 */
	@Override
	public void load() {
		if (datePicker == null) {
			datePicker = new DatePickerController();
			Main.statusBar.getRightItems().add(datePicker.getControl());
		} else {
			datePicker.load();
		}
	}
}
