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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class InventoryItem {
	protected final JSONObject item;
	protected final JSONObject baseItem;
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty notes = new SimpleStringProperty();
	private final StringProperty itemType = new SimpleStringProperty();
	protected final DoubleProperty weight = new SimpleDoubleProperty();

	public InventoryItem(final JSONObject item, final JSONObject baseItem) {
		this.item = item;
		this.baseItem = baseItem;
		baseItem.addListener(o -> recompute());
		recomputeBase();
	}

	public JSONObject getItem() {
		return baseItem;
	}

	public final String getItemType() {
		return itemType.get();
	}

	public final String getName() {
		return name.get();
	}

	public final String getNotes() {
		return notes.get();
	}

	public double getWeight() {
		return weight.get();
	}

	public final StringProperty itemTypeProperty() {
		return itemType;
	}

	public final StringProperty nameProperty() {
		return name;
	}

	public final StringProperty notesProperty() {
		return notes;
	}

	public void recompute() {
		recomputeBase();
	}

	private void recomputeBase() {
		name.set(item.getStringOrDefault("Name", baseItem.getStringOrDefault("Name", "")));
		notes.set(item.getStringOrDefault("Anmerkungen", baseItem.getStringOrDefault("Anmerkungen", "")));
		weight.set(item.getDoubleOrDefault("Gewicht", baseItem.getDoubleOrDefault("Gewicht", 0.0)));
		itemType.set(item.getStringOrDefault("Typ", baseItem.getStringOrDefault("Typ", "")));
	}

	public final void setItemType(final String type) {
		item.put("Typ", type);
		item.notifyListeners(null);
	}

	public final void setName(final String name) {
		baseItem.put("Name", name);
		baseItem.notifyListeners(null);
	}

	public final void setNotes(final String notes) {
		if (notes == null || "".equals(notes)) {
			item.removeKey("Anmerkungen");
		} else {
			item.put("Anmerkungen", notes);
		}
		item.notifyListeners(null);
	}

	public void setWeight(final double weight) {
		item.removeKey("Gewicht");
		if (weight != 0) {
			baseItem.put("Gewicht", weight);
		} else {
			baseItem.removeKey("Gewicht");
		}
		item.notifyListeners(null);
	}

	public final DoubleProperty weightProperty() {
		return weight;
	}
}
