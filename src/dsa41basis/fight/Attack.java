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

import dsa41basis.util.HeroUtil;
import dsatool.util.Tuple3;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class Attack {
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty tp = new SimpleStringProperty();
	private final IntegerProperty tpMod = new SimpleIntegerProperty();
	private final IntegerProperty at = new SimpleIntegerProperty();
	private final IntegerProperty pa = new SimpleIntegerProperty();
	private final IntegerProperty atMod = new SimpleIntegerProperty();
	private final IntegerProperty paMod = new SimpleIntegerProperty();
	private final IntegerProperty atStart = new SimpleIntegerProperty();
	private final IntegerProperty paStart = new SimpleIntegerProperty();
	private final StringProperty dk = new SimpleStringProperty();
	private final StringProperty notes = new SimpleStringProperty();

	private final JSONObject actual;

	public Attack(final String name, final JSONObject actual) {
		this.actual = actual;

		this.name.set(name);
		tp.set(HeroUtil.getTPString(null, actual, actual));
		atMod.set(actual.getObj("Trefferpunkte").getIntOrDefault("Modifikator", 0));
		at.set(actual.getIntOrDefault("Attackewert", Integer.MIN_VALUE));
		pa.set(actual.getIntOrDefault("Paradewert", Integer.MIN_VALUE));
		atMod.set(actual.getIntOrDefault("Attackewert:Modifikator", 0));
		paMod.set(actual.getIntOrDefault("Paradewert:Modifikator", 0));
		atMod.set(actual.getIntOrDefault("Attackewert:Start", 0));
		paMod.set(actual.getIntOrDefault("Paradewert:Start", 0));
		dk.set(String.join("", actual.getArr("Distanzklassen").getStrings()));
		notes.set(actual.getStringOrDefault("Anmerkungen", ""));
	}

	public IntegerProperty atModProperty() {
		return atMod;
	}

	public IntegerProperty atProperty() {
		return at;
	}

	public IntegerProperty atStartProperty() {
		return atStart;
	}

	public final ReadOnlyStringProperty dkProperty() {
		return dk;
	}

	public int getAt() {
		return at.get();
	}

	public int getAtMod() {
		return atMod.get();
	}

	public int getAtStart() {
		return atStart.get();
	}

	public final String getDk() {
		return dk.get();
	}

	public String getName() {
		return name.get();
	}

	public String getNotes() {
		return notes.get();
	}

	public int getPa() {
		return pa.get();
	}

	public int getPaMod() {
		return paMod.get();
	}

	public int getPaStart() {
		return paStart.get();
	}

	public final String getTp() {
		return tp.get();
	}

	public int getTpMod() {
		return tpMod.get();
	}

	public final Tuple3<Integer, Integer, Integer> getTpRaw() {
		final JSONObject tpValues = actual.getObj("Trefferpunkte");
		return new Tuple3<>(tpValues.getIntOrDefault("Würfel:Anzahl", 1), tpValues.getIntOrDefault("Wüfel:Typ", 6),
				tpValues.getIntOrDefault("Trefferpunkte", 0));
	}

	public StringProperty nameProperty() {
		return name;
	}

	public StringProperty notesProperty() {
		return notes;
	}

	public IntegerProperty paModProperty() {
		return paMod;
	}

	public IntegerProperty paProperty() {
		return pa;
	}

	public IntegerProperty paStartProperty() {
		return paStart;
	}

	public void setAt(final int at) {
		actual.put("Attackewert", at);
		this.at.set(at);
		actual.notifyListeners(null);
	}

	public void setAtMod(final int atMod) {
		if (atMod == 0) {
			actual.removeKey("Attackewert:Modifikator");
		} else {
			actual.put("Attackewert:Modifikator", atMod);
		}
		this.atMod.set(atMod);
		actual.notifyListeners(null);
	}

	public void setAtStart(final int atStart) {
		if (atStart == 0) {
			actual.removeKey("Attackewert:Start");
		} else {
			actual.put("Attackewert:Start", atStart);
		}
		this.atStart.set(atStart);
		actual.notifyListeners(null);
	}

	public final void setDK(final String dk) {
		final JSONArray distanceClasses = actual.getArr("Distanzklassen");
		distanceClasses.clear();
		for (final Character c : dk.toCharArray()) {
			distanceClasses.add(c.toString());
		}
		this.dk.set(dk);
		distanceClasses.notifyListeners(null);
	}

	public void setName(final String name) {
		if ("".equals(name)) return;
		((JSONObject) actual.getParent()).remove(actual);
		((JSONObject) actual.getParent()).put(name, actual);
		this.name.set(name);
		actual.notifyListeners(null);
	}

	public void setNotes(final String notes) {
		if ("".equals(notes)) {
			actual.removeKey("Anmerkungen");
		} else {
			actual.put("Anmerkungen", notes);
		}
		this.notes.set(notes);
		actual.notifyListeners(null);
	}

	public void setPa(final int pa) {
		actual.put("Paradewert", pa);
		this.pa.set(pa);
		actual.notifyListeners(null);
	}

	public void setPaMod(final int paMod) {
		if (paMod == 0) {
			actual.removeKey("Paradewert:Modifikator");
		} else {
			actual.put("Paradewert:Modifikator", paMod);
		}
		this.paMod.set(paMod);
		actual.notifyListeners(null);
	}

	public void setPaStart(final int paStart) {
		if (paStart == 0) {
			actual.removeKey("Paradewert:Start");
		} else {
			actual.put("Paradewert:Start", paStart);
		}
		this.paStart.set(paStart);
		actual.notifyListeners(null);
	}

	public final void setTp(final int diceType, final int numDice, final int tp, final boolean reducedWoundThreshold, final boolean staminaDamage,
			final int tpMod) {
		final JSONObject tpValues = actual.getObj("Trefferpunkte");

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
		tpValues.put("Modifikator", tpMod);
		this.tp.set(HeroUtil.getTPString(null, actual, actual));
		this.tpMod.set(tpMod);
		tpValues.notifyListeners(null);
	}

	public IntegerProperty tpModProperty() {
		return tpMod;
	}

	public final ReadOnlyStringProperty tpProperty() {
		return tp;
	}
}