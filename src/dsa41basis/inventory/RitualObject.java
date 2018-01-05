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
package dsa41basis.inventory;

import java.util.ArrayList;
import java.util.List;

import dsa41basis.util.DSAUtil;
import dsatool.resources.ResourceManager;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class RitualObject extends InventoryItem {

	private static final List<String> types = new ArrayList<>();

	static {
		final JSONObject ritualGroups = ResourceManager.getResource("data/Ritualgruppen");
		DSAUtil.foreach(group -> group.getString("Ritualobjekt") != null, (name, group) -> {
			types.add(group.getString("Ritualobjekt"));
		}, ritualGroups);
	}

	private final StringProperty type;
	private final IntegerProperty volume;

	private final StringProperty material;

	private final String ritualGroupName;

	public RitualObject(final JSONObject item, final JSONObject baseItem, final String ritualGroupName) {
		super(item, baseItem);

		this.ritualGroupName = ritualGroupName;
		final JSONObject ritualGroup = ResourceManager.getResource("data/Ritualgruppen").getObj(ritualGroupName);

		volume = new SimpleIntegerProperty(item.getIntOrDefault("Volumen", baseItem.getIntOrDefault("Volumen", ritualGroup.getIntOrDefault("Volumen", 0))));
		material = new SimpleStringProperty(item.getStringOrDefault("Material", baseItem.getStringOrDefault("Material", "")));
		type = new SimpleStringProperty(ritualGroup.getStringOrDefault("Ritualobjekt", "Bannschwert"));

		recompute();
	}

	public String getMaterial() {
		return material.get();
	}

	public String getRitualGroupName() {
		return ritualGroupName;
	}

	public List<Ritual> getRituals() {
		final JSONObject rituals = item.getObjOrDefault("Rituale", baseItem.getObj("Rituale"));
		final List<Ritual> result = new ArrayList<>();
		final JSONObject ritualGroup = ResourceManager.getResource("data/Rituale").getObj(ritualGroupName);
		for (final String ritualName : rituals.keySet()) {
			final JSONObject ritual = ritualGroup.getObjOrDefault(ritualName, null);
			if (ritual != null && ritual.containsKey("Mehrfach") && !"Anzahl".equals(ritual.getString("Mehrfach"))) {
				final JSONArray choices = rituals.getArr(ritualName);
				for (int i = 0; i < choices.size(); ++i) {
					result.add(new Ritual(ritualGroupName, ritualName, choices.getObj(i)));
				}
			} else {
				result.add(new Ritual(ritualGroupName, ritualName, rituals.getObj(ritualName)));
			}
		}
		return result;
	}

	public String getType() {
		return type.get();
	}

	public List<String> getTypes() {
		final List<String> result = new ArrayList<>();
		final JSONArray categories = baseItem.getArr("Kategorien");
		final JSONObject ritualGroups = ResourceManager.getResource("data/Ritualgruppen");
		DSAUtil.foreach(group -> group.getString("Ritualobjekt") != null, (name, group) -> {
			final String ritualObject = group.getString("Ritualobjekt");
			if (categories.contains(ritualObject)) {
				result.add(ritualObject);
			}
		}, ritualGroups);
		if (categories.contains("Bannschwert")) {
			result.add("Bannschwert");
		}
		return result;
	}

	public int getVolume() {
		return volume.get();
	}

	public StringProperty materialProperty() {
		return material;
	}

	public void setMaterial(final String material) {
		if ("".equals(material)) return;
		this.material.set(material);
		item.put("Material", material);
		item.notifyListeners(null);
	}

	public void setRituals(final List<Ritual> rituals) {
		final JSONObject actualRituals = item.getObjOrDefault("Rituale", baseItem.getObj("Rituale"));
		actualRituals.clear();

		final JSONObject ritualGroup = ResourceManager.getResource("data/Rituale").getObj(ritualGroupName);

		for (final Ritual actual : rituals) {
			final String ritualName = actual.getName();
			final JSONObject ritual = ritualGroup.getObjOrDefault(ritualName, null);
			if (ritual != null && ritual.containsKey("Mehrfach")) {
				if ("Anzahl".equals(ritual.getString("Mehrfach"))) {
					final JSONObject newRitual = new JSONObject(actualRituals);
					newRitual.put("Anzahl", (Integer) actual.getChoice());
					actualRituals.put(actual.getName(), newRitual);
				} else {
					final JSONArray choices = actualRituals.getArr(ritualName);
					final JSONObject newRitual = new JSONObject(choices);
					newRitual.put("Auswahl", (String) actual.getChoice());
					choices.add(newRitual);
				}
			} else {
				actualRituals.put(actual.getName(), new JSONObject(actualRituals));
			}
		}
	}

	public void setTypes(final List<String> types) {
		final JSONArray categories = baseItem.getArr("Kategorien");
		for (final String type : RitualObject.types) {
			categories.remove(type);
		}
		for (final String type : types) {
			categories.add(type);
		}
	}

	public void setVolume(final int volume) {
		if (volume == 0) return;
		this.volume.set(volume);
		item.put("Volumen", volume);
		item.notifyListeners(null);
	}

	public ReadOnlyStringProperty typeProperty() {
		return type;
	}

	public IntegerProperty volumeProperty() {
		return volume;
	}
}
