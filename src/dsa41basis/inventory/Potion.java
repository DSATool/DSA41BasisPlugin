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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class Potion extends InventoryItem {
	private final StringProperty quality = new SimpleStringProperty();
	private final StringProperty effect = new SimpleStringProperty();
	private final IntegerProperty amount = new SimpleIntegerProperty();

	public Potion(final JSONObject item, final JSONObject baseItem) {
		super(item, baseItem);

		recompute();
	}

	public final IntegerProperty amountProperty() {
		return amount;
	}

	public final StringProperty effectProperty() {
		return effect;
	}

	public final int getAmount() {
		return amount.get();
	}

	public final String getEffect() {
		return effect.get();
	}

	public final String getQuality() {
		return quality.get();
	}

	public final StringProperty qualityProperty() {
		return quality;
	}

	@Override
	public final void recompute() {
		super.recompute();

		quality.set(item.getStringOrDefault("Qualität", baseItem.getStringOrDefault("Qualität", "")));
		effect.set(item.getStringOrDefault("Wirkung", baseItem.getStringOrDefault("Wirkung", "")));
		amount.set(item.getIntOrDefault("Anzahl", baseItem.getIntOrDefault("Anzahl", 1)));
	}

	public final void setAmount(final int amount) {
		item.put("Anzahl", amount);
		item.notifyListeners(null);
	}

	public final void setEffect(final String effect) {
		item.put("Wirkung", effect);
		item.notifyListeners(null);
	}

	public final void setQuality(final String quality) {
		item.put("Qualität", quality);
		item.notifyListeners(null);
	}
}
