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
import dsatool.util.Tuple5;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class RangedWeapon extends OffensiveWeapon {

	private final StringProperty ammunition = new SimpleStringProperty();
	private final StringProperty ammunitionType = new SimpleStringProperty();
	private final IntegerProperty at = new SimpleIntegerProperty();
	private final StringProperty distance = new SimpleStringProperty();
	private final StringProperty distancetp = new SimpleStringProperty();
	private final IntegerProperty load = new SimpleIntegerProperty();
	private final DoubleProperty bulletweight = new SimpleDoubleProperty();

	public RangedWeapon(final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon, final JSONObject rangedCombatTalents,
			final JSONObject actualTalents) {
		super(hero, weapon, baseWeapon, rangedCombatTalents, actualTalents);
		recompute();
	}

	public final ReadOnlyStringProperty ammunitionProperty() {
		return ammunition;
	}

	public final StringProperty ammunitionTypeProperty() {
		return ammunitionType;
	}

	public final ReadOnlyIntegerProperty atProperty() {
		return at;
	}

	public final DoubleProperty bulletweightProperty() {
		return bulletweight;
	}

	private int computeAt() {
		return hero != null ? HeroUtil.getAT(hero, item, type.get(), false, false, true) : 0;
	}

	private String computeDistanceString() {
		final StringBuilder distancesString = new StringBuilder();
		boolean isFirstDistance = true;
		for (final String distance : new String[] { "Sehr Nah", "Nah", "Mittel", "Weit", "Extrem Weit" }) {
			if (isFirstDistance) {
				isFirstDistance = false;
			} else {
				distancesString.append('/');
			}
			final int dist = HeroUtil.getDistance(hero, item, type.get(), distance);
			if (dist == Integer.MIN_VALUE) {
				distancesString.append("—");
			} else {
				distancesString.append(dist);
			}
		}
		return distancesString.toString();
	}

	private String computeDistanceTPs() {
		final JSONObject distanceTPs = item.getObjOrDefault("Trefferpunkte/Entfernung", baseItem.getObj("Trefferpunkte/Entfernung"));
		final StringBuilder distanceTPsString = new StringBuilder();
		boolean isFirstDistance = true;
		for (final String distance : new String[] { "Sehr Nah", "Nah", "Mittel", "Weit", "Extrem Weit" }) {
			if (isFirstDistance) {
				isFirstDistance = false;
			} else {
				distanceTPsString.append('/');
			}
			final int distanceModifier = distanceTPs == null ? 0 : distanceTPs.getIntOrDefault(distance, Integer.MIN_VALUE);
			if (distanceModifier > 0) {
				distanceTPsString.append('+');
			}
			if (distanceModifier == Integer.MIN_VALUE) {
				distanceTPsString.append("—");
			} else {
				distanceTPsString.append(distanceModifier);
			}
		}
		return distanceTPsString.toString();
	}

	public final ReadOnlyStringProperty distanceProperty() {
		return distance;
	}

	public final ReadOnlyStringProperty distancetpProperty() {
		return distancetp;
	}

	public final String getAmmunition() {
		return ammunition.get();
	}

	public final int getAmmunitionMax() {
		if (baseItem.containsKey("Munition"))
			return item.getObjOrDefault("Munition", baseItem.getObj("Munition")).getIntOrDefault("Gesamt", 1);
		else
			return item.getObj("Munition").getIntOrDefault("Gesamt", 1);
	}

	public final String getAmmunitionType() {
		return ammunitionType.get();
	}

	public JSONObject getAmmunitionTypes() {
		if (baseItem.containsKey("Munition"))
			return item.getObjOrDefault("Munition", baseItem.getObj("Munition"));
		else
			return item.getObj("Munition");
	}

	public final int getAt() {
		return at.get();
	}

	public final double getBulletweight() {
		return bulletweight.get();
	}

	public final String getDistance() {
		return distance.get();
	}

	public Tuple5<Integer, Integer, Integer, Integer, Integer> getDistanceRaw() {
		final JSONObject distances = item.getObjOrDefault("Reichweiten", baseItem.getObj("Reichweiten"));
		return new Tuple5<>(distances.getIntOrDefault("Sehr Nah", 0), distances.getIntOrDefault("Nah", 0), distances.getIntOrDefault("Mittel", 0),
				distances.getIntOrDefault("Weit", 0), distances.getIntOrDefault("Extrem Weit", 0));
	}

	public final String getDistancetp() {
		return distancetp.get();
	}

	public Tuple5<Integer, Integer, Integer, Integer, Integer> getDistancetpRaw() {
		final JSONObject distances = item.getObjOrDefault("Trefferpunkte/Entfernung", baseItem.getObj("Trefferpunkte/Entfernung"));
		return new Tuple5<>(distances.getIntOrDefault("Sehr Nah", 0), distances.getIntOrDefault("Nah", 0), distances.getIntOrDefault("Mittel", 0),
				distances.getIntOrDefault("Weit", 0), distances.getIntOrDefault("Extrem Weit", 0));
	}

	public final int getLoad() {
		return load.get();
	}

	public final ReadOnlyIntegerProperty loadProperty() {
		return load;
	}

	@Override
	public final void recompute() {
		super.recompute();

		at.set(computeAt());
		load.set(HeroUtil.getLoadTime(hero, item, type.get()));
		distance.set(computeDistanceString());
		distancetp.set(computeDistanceTPs());
		final String ammunitionType = item.getStringOrDefault("Geschoss:Typ", baseItem.getString("Geschoss:Typ"));
		this.ammunitionType.set(ammunitionType);
		if ("Bolzen".equals(ammunitionType) || "Pfeile".equals(ammunitionType)) {
			ammunition.set(ammunitionType);
		} else {
			final JSONObject quantity = item.getObjOrDefault("Anzahl", baseItem.getObj("Anzahl"));
			ammunition.set(quantity.getIntOrDefault("Aktuell", 1).toString());
		}
		bulletweight.set(item.getDoubleOrDefault("Geschoss:Gewicht", baseItem.getDoubleOrDefault("Geschoss:Gewicht", Double.NEGATIVE_INFINITY)) * 40);
	}

	@Override
	protected void recomputeAtPa() {
		at.set(computeAt());
	}

	public final void setAmmunition(final int amount) {
		item.getObj("Anzahl").put("Aktuell", amount);
		ammunition.set(Integer.toString(amount));
		item.notifyListeners(null);
	}

	public void setAmmunition(final JSONObject ammunition) {
		if (ammunition == null) {
			item.removeKey("Munition");
		} else {
			item.put("Munition", ammunition.clone(item));
		}
		item.notifyListeners(null);
	}

	public void setAmmunitionType(final String ammunitionType) {
		if (ammunitionType == null) {
			item.removeKey("Geschoss:Typ");
		} else {
			item.put("Geschoss:Typ", ammunitionType);
		}
		this.ammunitionType.set(ammunitionType);
		item.notifyListeners(null);
	}

	public final void setBulletweight(final double bulletweight) {
		if (bulletweight == Double.NEGATIVE_INFINITY) {
			item.removeKey("Geschoss:Gewicht");
			baseItem.removeKey("Geschoss:Gewicht");
		} else {
			item.put("Geschoss:Gewicht", bulletweight / 40);
		}
		item.notifyListeners(null);
	}

	public final void setDistances(final int veryClose, final int close, final int medium, final int far, final int veryFar) {
		final JSONObject distances = item.getObj("Reichweiten");

		if (veryClose == Integer.MIN_VALUE) {
			distances.removeKey("Sehr Nah");
		} else {
			distances.put("Sehr Nah", veryClose);
		}
		if (close == Integer.MIN_VALUE) {
			distances.removeKey("Nah");
		} else {
			distances.put("Nah", close);
		}
		if (medium == Integer.MIN_VALUE) {
			distances.removeKey("Mittel");
		} else {
			distances.put("Mittel", medium);
		}
		if (far == Integer.MIN_VALUE) {
			distances.removeKey("Weit");
		} else {
			distances.put("Weit", far);
		}
		if (veryFar == Integer.MIN_VALUE) {
			distances.removeKey("Extrem Weit");
		} else {
			distances.put("Extrem Weit", veryFar);
		}
		distances.notifyListeners(null);
	}

	public final void setDistanceTPs(final int veryClose, final int close, final int medium, final int far, final int veryFar) {
		final JSONObject distances = item.getObj("Trefferpunkte/Entfernung");

		if (veryClose == Integer.MIN_VALUE) {
			distances.removeKey("Sehr Nah");
		} else {
			distances.put("Sehr Nah", veryClose);
		}
		if (close == Integer.MIN_VALUE) {
			distances.removeKey("Nah");
		} else {
			distances.put("Nah", close);
		}
		if (medium == Integer.MIN_VALUE) {
			distances.removeKey("Mittel");
		} else {
			distances.put("Mittel", medium);
		}
		if (far == Integer.MIN_VALUE) {
			distances.removeKey("Weit");
		} else {
			distances.put("Weit", far);
		}
		if (veryFar == Integer.MIN_VALUE) {
			distances.removeKey("Extrem Weit");
		} else {
			distances.put("Extrem Weit", veryFar);
		}
		distances.notifyListeners(null);
	}

	public final void setLoad(final int load) {
		item.put("Ladedauer", load);
		item.notifyListeners(null);
	}

	public final void setMaxAmmunition(final int amount) {
		if (amount == 0) {
			item.removeKey("Anzahl");
		} else {
			item.getObj("Anzahl").put("Gesamt", amount);
		}
		item.notifyListeners(null);
	}
}
