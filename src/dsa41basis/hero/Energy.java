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

import dsa41basis.util.HeroUtil;
import javafx.beans.binding.When;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jsonant.value.JSONObject;

public class Energy extends DerivedValue {

	protected final IntegerProperty bought;
	protected final DoubleProperty currentPercentage = new SimpleDoubleProperty();
	protected final IntegerProperty max = new SimpleIntegerProperty();
	protected final IntegerProperty permanent;
	protected final IntegerProperty buyableMaximum = new SimpleIntegerProperty();

	protected final int enhancementCost;

	public Energy(String name, JSONObject derivation, JSONObject attributes, JSONObject basicValues) {
		super(name, derivation, attributes, basicValues);

		bought = new SimpleIntegerProperty(actual.getIntOrDefault("Kauf", 0));
		permanent = new SimpleIntegerProperty(actual.getIntOrDefault("Permanent", 0));

		current.bind(max.add(manualModifier));

		final When cond = new When(max.isEqualTo(0));
		currentPercentage.bind(cond.then(0).otherwise(current.divide(cond.then(1.0).otherwise(max))));

		enhancementCost = derivation.getIntOrDefault("Zukauf", 8);

		recalculate(derivation, attributes);
	}

	public final IntegerProperty boughtProperty() {
		return bought;
	}

	public final ReadOnlyIntegerProperty buyableMaximumProperty() {
		return buyableMaximum;
	}

	public final ReadOnlyDoubleProperty currentPercentageProperty() {
		return currentPercentage;
	}

	public final int getBought() {
		return bought.get();
	}

	public final int getBuyableMaximum() {
		return buyableMaximum.get();
	}

	public final double getCurrentPercentage() {
		return currentPercentage.get();
	}

	public int getEnhancementCost() {
		return enhancementCost;
	}

	public final int getMax() {
		return max.get();
	}

	public final int getPermanent() {
		return permanent.get();
	}

	public final ReadOnlyIntegerProperty maxProperty() {
		return max;
	}

	public final IntegerProperty permanentProperty() {
		return permanent;
	}

	@Override
	protected void recalculate(JSONObject derivation, JSONObject attributes) {
		super.recalculate(derivation, attributes);

		if (derivation.getIntOrDefault("Zukauf", -1) != -1) {
			buyableMaximum.set(HeroUtil.deriveValue(derivation.getObj("Zukauf:Maximum"), attributes, null, false));
			bought.set(actual.getIntOrDefault("Kauf", 0));
		} else {
			bought.set(Integer.MIN_VALUE);
		}

		final int value = HeroUtil.deriveValue(derivation, attributes, actual, false);
		permanent.set(actual.getIntOrDefault("Permanent", 0));
		max.set(value);
		current.bind(max.add(manualModifier));
	}

	public final void setBought(int bought) {
		if (bought == 0) {
			actual.removeKey("Kauf");
		} else {
			actual.put("Kauf", bought);
		}
		actual.notifyListeners(null);
	}

	public final void setPermanent(int permanent) {
		if (permanent == 0) {
			actual.removeKey("Permanent");
		} else {
			actual.put("Permanent", permanent);
		}
		actual.notifyListeners(null);
	}
}
