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
import jsonant.value.JSONObject;

public class Valuable extends InventoryItem {
	private final DoubleProperty value = new SimpleDoubleProperty();

	public Valuable(final JSONObject item, final JSONObject baseItem) {
		super(item, baseItem);

		recompute();
	}

	public final double getValue() {
		return value.get();
	}

	@Override
	public final void recompute() {
		super.recompute();

		value.set(item.getDoubleOrDefault("Wert", baseItem.getDoubleOrDefault("Wert", 0.0)));
	}

	public final void setValue(final double value) {
		item.put("Wert", value);
		item.notifyListeners(null);
	}

	public final DoubleProperty valueProperty() {
		return value;
	}
}
