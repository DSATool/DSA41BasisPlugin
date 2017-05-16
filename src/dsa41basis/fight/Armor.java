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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONObject;

public class Armor extends InventoryItem {

	private final IntegerProperty back = new SimpleIntegerProperty();
	private final IntegerProperty belly = new SimpleIntegerProperty();
	private final IntegerProperty breast = new SimpleIntegerProperty();
	private final IntegerProperty head = new SimpleIntegerProperty();
	private final IntegerProperty larm = new SimpleIntegerProperty();
	private final IntegerProperty lleg = new SimpleIntegerProperty();
	private final IntegerProperty rarm = new SimpleIntegerProperty();
	private final IntegerProperty rleg = new SimpleIntegerProperty();
	private final DoubleProperty totalbe = new SimpleDoubleProperty();
	private final DoubleProperty totalrs = new SimpleDoubleProperty();
	private final DoubleProperty zonebe = new SimpleDoubleProperty();
	private final DoubleProperty zoners = new SimpleDoubleProperty();
	private final BooleanProperty additionalArmor = new SimpleBooleanProperty();

	public Armor(final JSONObject armor, final JSONObject baseObject) {
		super(armor, baseObject);

		recompute();
	}

	public final BooleanProperty additionalArmorProperty() {
		return additionalArmor;
	}

	public final IntegerProperty backProperty() {
		return back;
	}

	public final IntegerProperty bellyProperty() {
		return belly;
	}

	public final IntegerProperty breastProperty() {
		return breast;
	}

	public final int getBack() {
		return back.get();
	}

	public final int getBelly() {
		return belly.get();
	}

	public final int getBreast() {
		return breast.get();
	}

	public final int getHead() {
		return head.get();
	}

	public final int getLarm() {
		return larm.get();
	}

	public final int getLleg() {
		return lleg.get();
	}

	public final int getRarm() {
		return rarm.get();
	}

	public final int getRleg() {
		return rleg.get();
	}

	public final double getTotalbe() {
		return totalbe.get();
	}

	public final double getTotalrs() {
		return totalrs.get();
	}

	public final double getZonebe() {
		return zonebe.get();
	}

	public final double getZoners() {
		return zoners.get();
	}

	public final IntegerProperty headProperty() {
		return head;
	}

	public final boolean isAdditionalArmor() {
		return additionalArmor.get();
	}

	public final IntegerProperty larmProperty() {
		return larm;
	}

	public final IntegerProperty llegProperty() {
		return lleg;
	}

	public final IntegerProperty rarmProperty() {
		return rarm;
	}

	@Override
	public final void recompute() {
		super.recompute();

		if (item.containsKey("Rüstungsschutz") || baseItem.containsKey("Rüstungsschutz")) {
			final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
			final int[] zoneValues = new int[8];
			int i = 0;
			for (final String zone : new String[] { "Kopf", "Brust", "Rücken", "Bauch", "Linker Arm", "Rechter Arm", "Linkes Bein", "Rechtes Bein" }) {
				zoneValues[i] = zones.getInt(zone);
				++i;
			}
			head.set(zoneValues[0]);
			breast.set(zoneValues[1]);
			back.set(zoneValues[2]);
			belly.set(zoneValues[3]);
			larm.set(zoneValues[4]);
			rarm.set(zoneValues[5]);
			lleg.set(zoneValues[6]);
			rleg.set(zoneValues[7]);
			zoners.set((zoneValues[0] * 2 + zoneValues[1] * 4 + zoneValues[2] * 4 + zoneValues[3] * 4 + zoneValues[4] + zoneValues[5] + zoneValues[6] * 2
					+ zoneValues[7] * 2) / 20.0);
		} else {
			final Integer RS = item.getIntOrDefault("Gesamtrüstungsschutz", baseItem.getIntOrDefault("Gesamtrüstungsschutz", 0));
			head.set(RS);
			breast.set(RS);
			back.set(RS);
			belly.set(RS);
			larm.set(RS);
			rarm.set(RS);
			lleg.set(RS);
			rleg.set(RS);
			zoners.set(RS);
		}

		if (item.containsKey("Behinderung") || baseItem.containsKey("Behinderung")) {
			zonebe.set(item.getDoubleOrDefault("Behinderung", baseItem.getDoubleOrDefault("Behinderung", 0.0)));
		} else {
			zonebe.set(item.getIntOrDefault("Gesamtbehinderung", baseItem.getIntOrDefault("Gesamtbehinderung", 0)));
		}

		totalrs.set(item.getIntOrDefault("Gesamtrüstungsschutz", baseItem.getIntOrDefault("Gesamtrüstungsschutz", 0)));
		totalbe.set(item.getIntOrDefault("Gesamtbehinderung", baseItem.getIntOrDefault("Gesamtbehinderung", 0)));

		additionalArmor.set(item.getBoolOrDefault("Zusatzrüstung", baseItem.getBoolOrDefault("Zusatzrüstung", false)));
	}

	public final IntegerProperty rlegProperty() {
		return rleg;
	}

	public final void setAdditionalArmor(final boolean additionalArmor) {
		if (additionalArmor) {
			item.put("Zusatzrüstung", true);
		} else {
			item.removeKey("Zusatzrüstung");
			baseItem.removeKey("Zusatzrüstung");
		}
		item.notifyListeners(null);
	}

	public final void setBack(final int back) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Rücken", back);
		item.notifyListeners(null);
	}

	public final void setBelly(final int belly) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Bauch", belly);
		item.notifyListeners(null);
	}

	public final void setBreast(final int breast) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Brust", breast);
		item.notifyListeners(null);
	}

	public final void setHead(final int head) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Kopf", head);
		item.notifyListeners(null);
	}

	public final void setLarm(final int larm) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Linker Arm", larm);
		item.notifyListeners(null);
	}

	public final void setLleg(final int lleg) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Linkes Bein", lleg);
		item.notifyListeners(null);
	}

	public final void setRarm(final int rarm) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Rechter Arm", rarm);
		item.notifyListeners(null);
	}

	public final void setRleg(final int rleg) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Rechtes Bein", rleg);
		item.notifyListeners(null);
	}

	public final void setRs(final int head, final int breast, final int back, final int belly, final int larm, final int rarm, final int lleg, final int rleg) {
		final JSONObject zones = item.getObjOrDefault("Rüstungsschutz", baseItem.getObj("Rüstungsschutz"));
		zones.put("Kopf", head);
		zones.put("Brust", breast);
		zones.put("Rücken", back);
		zones.put("Bauch", belly);
		zones.put("Linker Arm", larm);
		zones.put("Rechter Arm", rarm);
		zones.put("Linkes Bein", lleg);
		zones.put("Rechtes Bein", rleg);
		item.notifyListeners(null);
	}

	public final void setTotalbe(final double totalbe) {
		item.put("Gesamtbehinderung", totalbe);
		item.notifyListeners(null);
	}

	public final void setTotalrs(final double totalrs) {
		item.put("Gesamtrüstungsschutz", totalrs);
		item.notifyListeners(null);
	}

	public final void setZonebe(final double zonebe) {
		item.put("Behinderung", zonebe);
		item.notifyListeners(null);
	}

	public final void setZoners(final double zoners) {
		item.put("Rüstungsschutz", zoners);
		item.notifyListeners(null);
	}

	public final DoubleProperty totalbeProperty() {
		return totalbe;
	}

	public final DoubleProperty totalrsProperty() {
		return totalrs;
	}

	public final DoubleProperty zonebeProperty() {
		return zonebe;
	}

	public final DoubleProperty zonersProperty() {
		return zoners;
	}

}
