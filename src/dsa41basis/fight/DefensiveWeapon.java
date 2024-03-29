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
import dsatool.util.Tuple;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class DefensiveWeapon extends InventoryItem implements WithDefense {
	private final IntegerProperty at = new SimpleIntegerProperty();
	private final IntegerProperty bf = new SimpleIntegerProperty();
	private final IntegerProperty ini = new SimpleIntegerProperty();
	private final IntegerProperty pa = new SimpleIntegerProperty();
	private final StringProperty wm = new SimpleStringProperty();
	private final BooleanProperty mainWeapon = new SimpleBooleanProperty();

	private final JSONObject hero;
	private final boolean shield;

	private final JSONListener recomputeListener;

	public DefensiveWeapon(final boolean shield, final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon) {
		super(weapon, baseWeapon);

		this.hero = hero;
		this.shield = shield;

		final JSONObject weaponModifiers = weapon.getObjOrDefault("Waffenmodifikatoren", baseWeapon.getObj("Waffenmodifikatoren"));

		recomputeListener = o -> recomputePa(hero, shield, weaponModifiers);
		if (hero != null) {
			hero.getObj("Eigenschaften").addLocalListener(recomputeListener);
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

	@Override
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

	public final boolean isMainWeapon() {
		return mainWeapon.get();
	}

	public final ReadOnlyBooleanProperty mainWeaponProperty() {
		return mainWeapon;
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

		mainWeapon.set(item.getBoolOrDefault("Seitenwaffe", baseItem.getBoolOrDefault("Seitenwaffe", false)));
	}

	private int recomputePa(final JSONObject hero, final boolean shield, final JSONObject weaponModifiers) {
		if (hero == null) return 0;

		if (shield)
			return HeroUtil.getShieldPA(hero, baseItem, true);
		else
			return HeroUtil.getDefensiveWeaponPA(hero, baseItem, true);
	}

	public final void setBf(final int bf) {
		item.removeKey("Bruchfaktor");
		if (bf != Integer.MIN_VALUE) {
			baseItem.put("Bruchfaktor", bf);
		} else {
			baseItem.removeKey("Bruchfaktor");
		}
		item.notifyListeners(null);
	}

	public final void setIni(final int ini) {
		item.put("Initiative:Modifikator", ini);
		item.notifyListeners(null);
	}

	public final void setMainWeapon(final boolean mainWeapon) {
		if (mainWeapon) {
			item.put("Seitenwaffe", true);
		} else {
			item.removeKey("Seitenwaffe");
			baseItem.removeKey("Seitenwaffe");
		}
		item.notifyListeners(null);
	}

	@Override
	public final void setWeight(final double weight) {
		super.setWeight(weight / 40);
	}

	public final void setWM(final int atMod, final int paMod) {
		final JSONObject wm = item.getObj("Waffenmodifikatoren");
		wm.put("Attackemodifikator", atMod);
		wm.put("Parademodifikator", paMod);
		wm.notifyListeners(null);
	}

	public final ReadOnlyStringProperty wmProperty() {
		return wm;
	}
}
