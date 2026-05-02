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
package dsa41basis.hero;

import dsa41basis.util.HeroUtil;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import jsonant.event.JSONListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class MetaTalent extends Talent {

	protected final DoubleProperty preciseValue;

	private final JSONListener recalculateListener;

	protected MetaTalent(final String name, final JSONObject talentGroup, final JSONObject talent, final JSONObject hero) {
		super(name, talentGroup, talent, null, null);
		preciseValue = new SimpleDoubleProperty(0);
		recalculateListener = _ -> recalculate(hero);
		hero.getObj("Eigenschaften").addListener(recalculateListener);
		hero.getObj("Talente").addListener(recalculateListener);
		recalculate(hero);
		value.bind(preciseValue.add(0.5));
	}

	@Override
	public double getPreciseValue() {
		return preciseValue.get();
	}

	public ReadOnlyDoubleProperty preciseValueProperty() {
		return preciseValue;
	}

	private void recalculate(final JSONObject hero) {
		final JSONArray calculation = talent.getArr("Berechnung");
		int numTalents = calculation.size();

		if (talent.containsKey("Berechnung:Auswahl")) {
			numTalents += 1;
		}

		double taw = 0;
		int min = Integer.MAX_VALUE;
		int current;
		for (int i = 0; i < calculation.size(); ++i) {
			final String currentName = calculation.getString(i);
			final JSONObject currentTalent = HeroUtil.findTalent(currentName)._1;
			if (currentTalent == null) {
				final JSONObject attribute = hero.getObj("Eigenschaften").getObj(currentName);
				current = HeroUtil.getCurrentValue(attribute, false);
			} else {
				final JSONObject actualTalent = (JSONObject) HeroUtil.findActualTalent(hero, calculation.getString(i))._1;
				if (actualTalent == null && !currentTalent.getBoolOrDefault("Basis", false)
						|| actualTalent != null && actualTalent.getIntOrDefault("TaW", currentTalent.getBoolOrDefault("Basis", false) ? 0 : -1) < 0) {
					taw = Double.NEGATIVE_INFINITY;
					break;
				}
				current = actualTalent != null ? actualTalent.getIntOrDefault("TaW", 0) : 0;
				min = Math.min(min, current);
			}
			taw += current;
		}
		if (talent.containsKey("Berechnung:Auswahl")) {
			final JSONArray choice = talent.getArr("Berechnung:Auswahl");
			int choiceTaw = -1;
			for (int i = 0; i < choice.size(); ++i) {
				final String currentName = choice.getString(i);
				final JSONObject currentTalent = HeroUtil.findTalent(currentName)._1;
				if (currentTalent == null) {
					choiceTaw = Math.max(choiceTaw, HeroUtil.getCurrentValue(hero.getObj("Eigenschaften").getObj(currentName), false));
				} else {
					final JSONValue actualTalent = HeroUtil.findActualTalent(hero, choice.getString(i))._1;
					if (currentTalent.containsKey("Auswahl") || currentTalent.containsKey("Freitext")) {
						int max = -1;
						for (int j = 0; j < actualTalent.size(); ++j) {
							max = Math.max(max, ((JSONArray) actualTalent).getObj(j).getIntOrDefault("TaW", -1));
						}
						choiceTaw = Math.max(choiceTaw, Math.max(max, currentTalent.getBoolOrDefault("Basis", false) ? 0 : -1));
					} else {
						if (actualTalent != null) {
							choiceTaw = Math.max(choiceTaw,
									((JSONObject) actualTalent).getIntOrDefault("TaW", currentTalent.getBoolOrDefault("Basis", false) ? 0 : -1));
						} else if (choiceTaw == -1 && currentTalent.getBoolOrDefault("Basis", false)) {
							choiceTaw = 0;
						}
					}
				}
			}
			if (choiceTaw < 0) {
				taw = Double.NEGATIVE_INFINITY;
			} else {
				min = Math.min(min, choiceTaw);
				taw += choiceTaw;
			}
		}

		final double divisor = talent.getDoubleOrDefault("Divisor", (double) numTalents);

		taw = taw != Double.NEGATIVE_INFINITY ? Math.min(taw / divisor, 2 * min) : Double.NEGATIVE_INFINITY;

		preciseValue.set(taw);
	}

}
