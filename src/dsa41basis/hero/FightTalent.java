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
package dsa41basis.hero;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class FightTalent extends Talent {
	private final IntegerProperty at;
	private final BooleanProperty attackOnly;
	private final StringProperty be;
	private final IntegerProperty pa;

	protected FightTalent(final String name, final JSONObject talentGroup, final JSONObject talent, final JSONObject actual, final JSONObject actualGroup) {
		super(name, talentGroup, talent, actual, actualGroup);

		attackOnly = new SimpleBooleanProperty(talent.getBoolOrDefault("NurAT", false) || talent.getBoolOrDefault("FK", false));

		if (attackOnly.get()) {
			pa = new SimpleIntegerProperty(Integer.MIN_VALUE);
		} else {
			pa = new SimpleIntegerProperty(actual == null ? 0 : actual.getIntOrDefault("PA", 0));
		}

		at = new SimpleIntegerProperty(actual == null ? 0 : actual.getIntOrDefault("AT", attackOnly.get() ? value.get() : 0));

		final int beAdd = talent.getIntOrDefault("BEAdditiv", 0);
		final int beMul = talent.getIntOrDefault("BEMultiplikativ", 0);
		final StringBuilder beString = new StringBuilder(8);

		if (beAdd != 0) {
			beString.append("BE");
			if (beAdd > 0) {
				beString.append('+');
			}
			beString.append(beAdd);
		} else if (beMul != 0) {
			beString.append("BE");
			if (beMul != 1) {
				beString.append('x');
				beString.append(beMul);
			}
		} else {
			beString.append('-');
		}

		be = new SimpleStringProperty(beString.toString());
	}

	public final IntegerProperty atProperty() {
		return at;
	}

	public final ReadOnlyBooleanProperty attackOnlyProperty() {
		return attackOnly;
	}

	public final ReadOnlyStringProperty beProperty() {
		return be;
	}

	public final int getAt() {
		return at.get();
	}

	public final boolean getAttackOnly() {
		return attackOnly.get();
	}

	public final String getBe() {
		return be.get();
	}

	public final int getPa() {
		return pa.get();
	}

	public final IntegerProperty paProperty() {
		return pa;
	}

	public final void setAt(final int at) {
		if (this.at.get() != at) {
			if (!attackOnly.get()) {
				pa.set(value.get() - at);
				actual.put("PA", pa.get());
			}
			actual.put("AT", at);
			this.at.set(at);
			actual.notifyListeners(null);
		}
	}

	public final void setPa(final int pa) {
		if (attackOnly.get()) return;
		if (this.pa.get() != pa) {
			at.set(value.get() - pa);
			actual.put("AT", at.get());
			actual.put("PA", pa);
			this.pa.set(pa);
			actual.notifyListeners(null);
		}
	}

	@Override
	public final void setValue(final int value) {
		if (this.value.get() != value) {
			if (actual == null) {
				insertTalent(true);
			}

			if (attackOnly.get()) {
				setAt(value);
			} else {

				int at = this.at.get();
				int pa = this.pa.get();

				final int diff = at - pa;

				at = (value - diff + (diff < 0 ? 0 : 1)) / 2 + diff;
				pa = value - at;

				if (at - pa > 5) {
					at -= 1;
				} else if (at - pa < -5) {
					at += 1;
				}

				if (value == 0) {
					at = 0;
				} else if (value > 0) {
					at = Math.min(value, Math.max(0, at));
				} else {
					at = Math.max(value, Math.min(at, 0));
				}

				setAt(at);
			}
		}

		super.setValue(value);
	}
}
