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
package dsa41basis.fight;

import dsa41basis.inventory.InventoryItem;
import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.Tuple;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class DefensiveWeapon extends InventoryItem {
	private final IntegerProperty at = new SimpleIntegerProperty();
	private final IntegerProperty bf = new SimpleIntegerProperty();
	private final IntegerProperty ini = new SimpleIntegerProperty();
	private final IntegerProperty pa = new SimpleIntegerProperty();
	private final StringProperty wm = new SimpleStringProperty();

	private final JSONObject hero;
	private final boolean shield;

	public DefensiveWeapon(final boolean shield, final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon) {
		super(weapon, baseWeapon);

		this.hero = hero;
		this.shield = shield;

		final JSONObject weaponModifiers = weapon.getObjOrDefault("Waffenmodifikatoren", baseWeapon.getObj("Waffenmodifikatoren"));

		if (hero != null) {
			hero.getObj("Eigenschaften").addLocalListener(o -> recomputePa(hero, shield, weaponModifiers));
		}

		recompute();
	}

	public final ReadOnlyIntegerProperty atProperty() {
		return at;
	}

	public final IntegerProperty bfProperty() {
		return bf;
	}

	public final int getAt() {
		return at.get();
	}

	public final int getBf() {
		return bf.get();
	}

	public final int getIni() {
		return ini.get();
	}

	public final int getPa() {
		return pa.get();
	}

	public final String getWM() {
		return wm.get();
	}

	public Tuple<Integer, Integer> getWMraw() {
		final JSONObject weaponModifiers = item.getObjOrDefault("Waffenmodifikatoren", baseItem.getObj("Waffenmodifikatoren"));
		return new Tuple<>(weaponModifiers.getIntOrDefault("Attackemodifikator", 0), weaponModifiers.getIntOrDefault("Parademodifikator", 0));
	}

	public final IntegerProperty iniProperty() {
		return ini;
	}

	public final ReadOnlyIntegerProperty paProperty() {
		return pa;
	}

	@Override
	public final void recompute() {
		super.recompute();

		final JSONObject weaponModifiers = item.getObjOrDefault("Waffenmodifikatoren", baseItem.getObj("Waffenmodifikatoren"));

		at.set(weaponModifiers.getIntOrDefault("Attackemodifikator", 0));

		pa.set(recomputePa(hero, shield, weaponModifiers));

		ini.set(item.getIntOrDefault("Initiative:Modifikator", baseItem.getIntOrDefault("Initiative:Modifikator", 0)));

		final Integer BF = item.getIntOrDefault("Bruchfaktor", baseItem.getInt("Bruchfaktor"));
		bf.set(BF != null ? BF : Integer.MIN_VALUE);

		wm.set(weaponModifiers.getIntOrDefault("Attackemodifikator", 0).toString() + "/" + weaponModifiers.getIntOrDefault("Parademodifikator", 0));

		weight.set(item.getDoubleOrDefault("Gewicht", baseItem.getDoubleOrDefault("Gewicht", 0.0)) * 40);
	}

	private int recomputePa(final JSONObject hero, final boolean shield, final JSONObject weaponModifiers) {
		if (hero == null) return 0;

		final JSONObject skills = hero.getObj("Sonderfertigkeiten");

		int PA = weaponModifiers.getIntOrDefault("Parademodifikator", 0);

		if (shield) {
			final int PABase = HeroUtil.deriveValue(ResourceManager.getResource("data/Basiswerte").getObj("Parade-Basis"), hero.getObj("Eigenschaften"),
					hero.getObj("Basiswerte").getObj("Parade-Basis"), true);
			PA += PABase;

			if (skills.containsKey("Linkhand")) {
				PA += 1;
			}
			if (skills.containsKey("Schildkampf I")) {
				PA += 2;
			}
			if (skills.containsKey("Schildkampf II")) {
				PA += 2;
			}

			return PA;
		} else {
			if (skills.containsKey("Linkhand")) {
				PA -= 4;
				if (skills.containsKey("Parierwaffen I")) {
					PA += 3;
				}
				if (skills.containsKey("Parierwaffen II")) {
					PA += 3;
				}

				return PA;
			} else
				return Integer.MIN_VALUE;
		}
	}

	public final void setBf(final int bf) {
		if (bf != Integer.MIN_VALUE) {
			baseItem.put("Bruchfaktor", bf);
		} else {
			item.removeKey("Bruchfaktor");
			baseItem.removeKey("Bruchfaktor");
		}
		item.notifyListeners(null);
	}

	public final void setIni(final int ini) {
		item.put("Initiative:Modifikator", ini);
		item.notifyListeners(null);
	}

	@Override
	public final void setWeight(final double weight) {
		super.setWeight(weight / 40);
	}

	public final void setWM(final int atMod, final int paMod) {
		final JSONObject wm = item.getObj("Waffenmodifikatoren");
		wm.put("Attackemodifikator", atMod);
		wm.put("Parademodifikatior", paMod);
		wm.notifyListeners(null);
	}

	public final ReadOnlyStringProperty wmProperty() {
		return wm;
	}
}
