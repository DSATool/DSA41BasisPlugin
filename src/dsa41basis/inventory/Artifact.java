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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class Artifact extends InventoryItem {
	public static String[] types = new String[] { "Applicatus", "Arcanovi (einmalig)", "Arcanovi (aufladbar)", "Arcanovi (semipermanent)", "Matrixgeber",
			"Zaubertalisman", "Infinitum" };

	private final StringProperty type = new SimpleStringProperty();
	private final IntegerProperty loadNum = new SimpleIntegerProperty();
	private final StringProperty loadFreq = new SimpleStringProperty();
	private final StringProperty stability = new SimpleStringProperty();
	private final IntegerProperty asp = new SimpleIntegerProperty();
	private final IntegerProperty pAsp = new SimpleIntegerProperty();
	private final StringProperty triggerType = new SimpleStringProperty();
	private final IntegerProperty triggerActions = new SimpleIntegerProperty();
	private final StringProperty triggerDesc = new SimpleStringProperty();
	private final ObjectProperty<JSONArray> spells = new SimpleObjectProperty<>();

	public Artifact(final JSONObject item, final JSONObject baseItem) {
		super(item, baseItem);

		recompute();
	}

	public final ReadOnlyIntegerProperty aspProperty() {
		return asp;
	}

	public final int getAsp() {
		return asp.get();
	}

	public final String getLoadFreq() {
		return loadFreq.get();
	}

	public final int getLoadNum() {
		return loadNum.get();
	}

	public final int getPAsp() {
		return pAsp.get();
	}

	public final JSONArray getSpells() {
		return spells.get();
	}

	public final String getStability() {
		return stability.get();
	}

	public final int getTriggerActions() {
		return triggerActions.get();
	}

	public final String getTriggerDesc() {
		return triggerDesc.get();
	}

	public final String getTriggerType() {
		return triggerType.get();
	}

	public final String getType() {
		return type.get();
	}

	public final ReadOnlyStringProperty loadFreqProperty() {
		return loadFreq;
	}

	public final ReadOnlyIntegerProperty loadNumProperty() {
		return loadNum;
	}

	public final ReadOnlyIntegerProperty pAspProperty() {
		return pAsp;
	}

	@Override
	public final void recompute() {
		super.recompute();

		type.set(item.getStringOrDefault("Typ", baseItem.getStringOrDefault("Typ", "Applicatus")));
		final JSONObject loads = item.getObjOrDefault("Ladungen", baseItem.getObj("Ladungen"));
		loadNum.set(loads.getIntOrDefault("Additiv", loads.getIntOrDefault("Multiplikativ", 1)));
		loadFreq.set(loads.getStringOrDefault("Grundmenge", "Jahr"));
		stability.set(item.getStringOrDefault("Stabilität", "labil"));
		final JSONObject energy = item.getObjOrDefault("Astralenergie", baseItem.getObj("Astralenergie"));
		asp.set(energy.getIntOrDefault("Additiv", 0));
		pAsp.set(energy.getObj("Permanent").getIntOrDefault("Additiv", 0));
		final JSONObject trigger = item.getObjOrDefault("Auslöser", baseItem.getObj("Auslöser"));
		triggerType.set(trigger.getStringOrDefault("Typ", "Berührung"));
		triggerActions.set(trigger.getBoolOrDefault("Reaktion", false) ? 0 : trigger.getIntOrDefault("Aktionen", 0));
		triggerDesc.set(trigger.getStringOrDefault("Beschreibung", ""));
		spells.set(item.getArrOrDefault("Wirkende Sprüche", baseItem.getArr("Wirkende Sprüche")));
	}

	public final void setAsp(final int asp, final int pAsp) {
		final JSONObject energy = item.getObjOrDefault("Astralenergie", baseItem.getObj("Astralenergie"));
		energy.put("Additiv", asp);
		energy.getObj("Permanent").put("Additiv", pAsp);
		this.asp.set(asp);
		this.pAsp.set(pAsp);
		item.notifyListeners(null);
	}

	public final void setLoads(final int loadNum, final String loadFreq) {
		if (loadNum != Integer.MIN_VALUE) {
			final JSONObject loads = item.getObjOrDefault("Ladungen", baseItem.getObj("Ladungen"));
			if (loadFreq != null) {
				loads.put("Multiplikativ", loadNum);
				loads.put("Grundmenge", loadFreq);
				loads.removeKey("Additiv");
			} else {
				loads.put("Additiv", loadNum);
				loads.removeKey("Multiplikativ");
				loads.removeKey("Grundmenge");
			}
		} else {
			item.removeKey("Ladungen");
			baseItem.removeKey("Ladungen");
		}
		this.loadNum.set(loadNum);
		this.loadFreq.set(loadFreq);
		item.notifyListeners(null);
	}

	public final void setSpells(final JSONArray spells) {
		item.put("Wirkende Sprüche", spells);
		this.spells.set(spells);
		item.notifyListeners(null);
	}

	public final void setStability(final String stability) {
		if (stability != null) {
			item.put("Stabilität", stability);
		} else {
			item.removeKey("Stabilität");
		}
		this.stability.set(stability);
		item.notifyListeners(null);
	}

	public final void setTrigger(final String triggerType, final int triggerActions, final String triggerDesc) {
		final JSONObject trigger = item.getObjOrDefault("Auslöser", baseItem.getObj("Auslöser"));
		trigger.put("Typ", triggerType);
		if (triggerActions == 0) {
			trigger.put("Reaktion", true);
			trigger.removeKey("Aktionen");
		} else {
			trigger.put("Aktionen", triggerActions);
			trigger.removeKey("Reaktion");
		}
		trigger.put("Beschreibung", triggerDesc);
		this.triggerType.set(triggerType);
		this.triggerActions.set(triggerActions);
		this.triggerDesc.set(triggerDesc);
		item.notifyListeners(null);
	}

	public final void setType(final String type) {
		item.put("Typ", type);
		this.type.set(type);
		item.notifyListeners(null);
	}

	public final ReadOnlyObjectProperty<JSONArray> spellsProperty() {
		return spells;
	}

	public final StringProperty stabilityProperty() {
		return stability;
	}

	public final ReadOnlyIntegerProperty triggerActionsProperty() {
		return triggerActions;
	}

	public final ReadOnlyStringProperty triggerDescProperty() {
		return triggerDesc;
	}

	public final ReadOnlyStringProperty triggerTypeProperty() {
		return triggerType;
	}

	public final StringProperty typeProperty() {
		return type;
	}
}
