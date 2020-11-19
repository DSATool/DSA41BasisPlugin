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

import java.util.ArrayList;
import java.util.List;

import dsa41basis.inventory.InventoryItem;
import dsa41basis.util.HeroUtil;
import dsatool.util.Tuple;
import dsatool.util.Tuple3;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public abstract class OffensiveWeapon extends InventoryItem implements WithAttack {

	protected final JSONObject hero;

	protected final IntegerProperty ebe = new SimpleIntegerProperty();
	private final ObjectProperty<ObservableList<String>> talents;
	protected final IntegerProperty at = new SimpleIntegerProperty();
	protected final StringProperty tp = new SimpleStringProperty();
	protected StringProperty type;

	private final JSONObject combatTalents;

	public OffensiveWeapon(final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon, final JSONObject combatTalents,
			final JSONObject actualTalents) {
		super(weapon, baseWeapon);

		this.hero = hero;
		this.combatTalents = combatTalents;

		if (hero != null) {
			hero.getObj("Eigenschaften").addListener(o -> recomputeAtPa());
			hero.getObj("Sonderfertigkeiten").addLocalListener(o -> recomputeAtPa());
			hero.getObj("Besitz").getArr("Ausrüstung").addListener(o -> recomputeAtPa());
			hero.getObj("Eigenschaften").addListener(o -> tp.set(HeroUtil.getTPString(hero, weapon, baseWeapon)));
		}

		final JSONArray weaponTypes = weapon.getArrOrDefault("Waffentypen", baseWeapon.getArr("Waffentypen"));
		talents = new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));
		if (actualTalents != null) {
			for (int i = 0; i < weaponTypes.size(); ++i) {
				talents.get().add(weaponTypes.getString(i));
			}
		}
		type = new SimpleStringProperty(weaponTypes.size() == 0 ? "" : weaponTypes.getString(0));

		final String primaryType = weapon.getStringOrDefault("Waffentyp:Primär", baseWeapon.getString("Waffentyp:Primär"));
		if (weaponTypes.size() > 1 && primaryType != null) {
			type = new SimpleStringProperty(primaryType);
		}

		type.addListener(o -> recomputeAtPa());
	}

	public final ReadOnlyIntegerProperty atProperty() {
		return at;
	}

	public final ReadOnlyIntegerProperty ebeProperty() {
		return ebe;
	}

	@Override
	public final int getAt() {
		return at.get();
	}

	public final int getEbe() {
		return ebe.get();
	}

	public List<String> getTalents() {
		return talents.get();
	}

	@Override
	public final String getTp() {
		return tp.get();
	}

	@Override
	public Tuple<Boolean, Boolean> getTPModifiers() {
		final JSONObject TPValues = item.getObjOrDefault("Trefferpunkte", baseItem.getObjOrDefault("Trefferpunkte", null));
		return new Tuple<>(TPValues.getBoolOrDefault("Ausdauerschaden", false), TPValues.getBoolOrDefault("Reduzierte Wundschwelle", false));
	}

	@Override
	public final Tuple3<Integer, Integer, Integer> getTpRaw() {
		final JSONObject tpValues = item.getObj("Trefferpunkte");
		return new Tuple3<>(tpValues.getIntOrDefault("Würfel:Anzahl", 1), tpValues.getIntOrDefault("Wüfel:Typ", 6),
				tpValues.getIntOrDefault("Trefferpunkte", 0));
	}

	public final String getType() {
		return type.get();
	}

	@Override
	public double getWeight() {
		return weight.get() * 40;
	}

	@Override
	public void recompute() {
		super.recompute();

		ebe.set(combatTalents != null &&
				combatTalents.getObjOrDefault(type.get(), null) != null ? combatTalents.getObj(type.get()).getIntOrDefault("BEAdditiv", 0) : 0);
		tp.set(HeroUtil.getTPString(hero, item, baseItem));

		final JSONArray weaponTypes = item.getArrOrDefault("Waffentypen", baseItem.getArr("Waffentypen"));
		talents.get().clear();
		if (weaponTypes != null) {
			for (int i = 0; i < weaponTypes.size(); ++i) {
				talents.get().add(weaponTypes.getString(i));
			}
		}
	}

	abstract protected void recomputeAtPa();

	public final void setTalents(final List<String> talents) {
		final JSONArray weaponTypes = item.getArrOrDefault("Waffentypen", baseItem.getArr("Waffentypen"));
		weaponTypes.clear();
		for (final String talent : talents) {
			weaponTypes.add(talent);
		}
		item.notifyListeners(null);
	}

	public final void setTp(final int diceType, final int numDice, final int tp, final boolean reducedWoundThreshold, final boolean staminaDamage) {
		final JSONObject tpValues = item.getObj("Trefferpunkte");

		tpValues.put("Würfel:Typ", diceType);
		tpValues.put("Würfel:Anzahl", numDice);
		tpValues.put("Trefferpunkte", tp);
		if (reducedWoundThreshold) {
			tpValues.put("Reduzierte Wundschwelle", true);
		} else {
			tpValues.removeKey("Reduzierte Wundschwelle");
		}
		if (staminaDamage) {
			tpValues.put("Ausdauerschaden", true);
		} else {
			tpValues.removeKey("Ausdauerschaden");
		}
		tpValues.notifyListeners(null);
	}

	public final void setType(final String type) {
		item.put("Waffentyp:Primär", type);
		this.type.set(type);
	}

	@Override
	public final void setWeight(final double weight) {
		super.setWeight(weight / 40);
	}

	public ObjectProperty<ObservableList<String>> talentsProperty() {
		return talents;
	}

	public final ReadOnlyStringProperty tpProperty() {
		return tp;
	}

	public final StringProperty typeProperty() {
		return type;
	}
}
