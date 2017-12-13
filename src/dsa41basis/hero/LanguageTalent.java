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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class LanguageTalent extends Talent {
	private final IntegerProperty complexity;
	private final StringProperty mlsltl;

	protected LanguageTalent(final String name, final JSONObject talentGroup, final JSONObject talent, final JSONObject actual, final JSONObject actualGroup) {
		super(name, talentGroup, talent, actual, actualGroup);

		boolean ml;
		boolean sl;
		boolean tl;
		if (actual == null) {
			ml = false;
			sl = false;
			tl = false;
		} else {
			ml = actual.getBoolOrDefault("Muttersprache", false);
			sl = actual.getBoolOrDefault("Zweitsprache", false);
			tl = actual.getBoolOrDefault("Lehrsprache", false);
		}
		if (ml) {
			mlsltl = new SimpleStringProperty("MS");
		} else if (sl) {
			mlsltl = new SimpleStringProperty("ZS");
		} else if (tl) {
			mlsltl = new SimpleStringProperty("LS");
		} else {
			mlsltl = new SimpleStringProperty("");
		}

		complexity = new SimpleIntegerProperty(talent.getInt("Komplexität"));
	}

	public final ReadOnlyIntegerProperty complexityProperty() {
		return complexity;
	}

	public final int getComplexity() {
		return complexity.get();
	}

	public final String getMlsltl() {
		return mlsltl.get();
	}

	public final StringProperty mlsltlProperty() {
		return mlsltl;
	}

	public final void setMlsltl(final String mlsltl) {
		switch (mlsltl) {
		case "MS":
			actual.removeKey("Zweitsprache");
			actual.removeKey("Lehrsprache");
			actual.put("Muttersprache", true);
			break;
		case "ZS":
			actual.removeKey("Muttersprache");
			actual.removeKey("Lehrsprache");
			actual.put("Zweitsprache", true);
			break;
		case "LS":
			actual.removeKey("Muttersprache");
			actual.removeKey("Zweitsprache");
			actual.put("Lehrsprache", true);
			break;
		case "":
			actual.removeKey("Muttersprache");
			actual.removeKey("Zweitsprache");
			actual.removeKey("Lehrsprache");
		default:
			return;
		}
		actual.notifyListeners(null);
		this.mlsltl.set(mlsltl);
	}
}
