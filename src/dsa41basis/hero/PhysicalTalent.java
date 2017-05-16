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

import dsa41basis.util.DSAUtil;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONObject;

public class PhysicalTalent extends Talent {

	private final StringProperty be;

	public PhysicalTalent(String name, JSONObject talentGroup, JSONObject talent, JSONObject actual, JSONObject actualGroup) {
		super(name, talentGroup, talent, actual, actualGroup);

		be = new SimpleStringProperty(DSAUtil.getBEString(talent));
	}

	public final ReadOnlyStringProperty beProperty() {
		return be;
	}

	public final String getBe() {
		return be.get();
	}
}
