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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.When;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.paint.Color;
import jsonant.value.JSONObject;

public class Energy extends DerivedValue implements Buyable {

	public static Color COLOR_LEP = Color.RED;
	public static Color COLOR_AUP = Color.DODGERBLUE;
	public static Color COLOR_ASP = Color.VIOLET.saturate();
	public static Color COLOR_KAP = Color.YELLOW.darker();
	public static Color COLOR_MR = Color.WHITE;

	protected final IntegerProperty bought = new SimpleIntegerProperty();
	protected final DoubleProperty currentPercentage = new SimpleDoubleProperty();
	protected final IntegerProperty value = new SimpleIntegerProperty();
	protected final IntegerProperty permanent = new SimpleIntegerProperty();
	protected final IntegerProperty maximum = new SimpleIntegerProperty();
	protected IntegerProperty ses;

	protected final int enhancementCost;

	public Energy(final String name, final JSONObject derivation, final JSONObject hero) {
		super(name, derivation, hero);

		final When cond = Bindings.when(value.isEqualTo(0));
		currentPercentage.bind(cond.then(0).otherwise(current.divide(cond.then(1.0).otherwise(value))));

		enhancementCost = derivation.getIntOrDefault("Zukauf", 8);

		recalculate(derivation, hero);

		ses = new SimpleIntegerProperty(actual == null ? 0 : actual.getIntOrDefault("SEs", 0));
	}

	@Override
	public final IntegerProperty boughtProperty() {
		return bought;
	}

	public final ReadOnlyDoubleProperty currentPercentageProperty() {
		return currentPercentage;
	}

	@Override
	public final int getBought() {
		return bought.get();
	}

	public final double getCurrentPercentage() {
		return currentPercentage.get();
	}

	@Override
	public int getEnhancementComplexity(final JSONObject hero, final int targetLevel) {
		return enhancementCost;
	}

	@Override
	public final int getMaximum(final JSONObject hero) {
		return maximum.get();
	}

	public final int getPermanent() {
		return permanent.get();
	}

	@Override
	public final int getValue() {
		return value.get();
	}

	public final ReadOnlyIntegerProperty maximumProperty() {
		return maximum;
	}

	public final IntegerProperty permanentProperty() {
		return permanent;
	}

	@Override
	protected void recalculate(final JSONObject derivation, final JSONObject hero) {
		super.recalculate(derivation, hero);

		if (derivation.getIntOrDefault("Zukauf", -1) != -1) {
			maximum.set(HeroUtil.deriveValue(derivation.getObj("Zukauf:Maximum"), hero, null, false));
			bought.set(actual.getIntOrDefault("Kauf", 0));
		} else {
			bought.set(Integer.MIN_VALUE);
		}

		final int value = HeroUtil.deriveValue(derivation, hero, actual, false);
		permanent.set(actual.getIntOrDefault("Permanent", 0));
		this.value.set(value);
		current.bind(this.value.add(manualModifier));
	}

	@Override
	public final IntegerProperty sesProperty() {
		return ses;
	}

	@Override
	public final void setBought(final int bought) {
		if (this.bought.get() != bought) {
			if (bought == 0) {
				actual.removeKey("Kauf");
			} else {
				actual.put("Kauf", bought);
			}
			this.bought.set(bought);
			actual.notifyListeners(null);
		}
	}

	public final void setPermanent(final int permanent) {
		if (this.permanent.get() != permanent) {
			if (permanent == 0) {
				actual.removeKey("Permanent");
			} else {
				actual.put("Permanent", permanent);
			}
			this.permanent.set(permanent);
			actual.notifyListeners(null);
		}
	}

	public final ReadOnlyIntegerProperty valueProperty() {
		return value;
	}
}
