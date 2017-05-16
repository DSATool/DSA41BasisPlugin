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
import jsonant.value.JSONObject;

public class Clothing extends InventoryItem {
	private final IntegerProperty ks = new SimpleIntegerProperty();

	public Clothing(final JSONObject item, final JSONObject baseItem) {
		super(item, baseItem);

		recompute();
	}

	public final int getKs() {
		return ks.get();
	}

	public final IntegerProperty ksProperty() {
		return ks;
	}

	@Override
	public final void recompute() {
		super.recompute();

		ks.set(item.getIntOrDefault("Kälteschutz", baseItem.getIntOrDefault("Kälteschutz", 0)));
	}

	public final void setKs(final int ks) {
		item.put("Kälteschutz", ks);
		item.notifyListeners(null);
	}
}
