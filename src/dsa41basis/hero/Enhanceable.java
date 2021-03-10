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
import jsonant.value.JSONObject;

public interface Enhanceable {

	public JSONObject getActual();

	default public int getSes() {
		return sesProperty().get();
	}

	public IntegerProperty sesProperty();

	default public void setSes(final int ses) {
		final JSONObject actual = getActual();
		sesProperty().set(ses);
		if (ses == 0) {
			actual.removeKey("SEs");
		} else {
			actual.put("SEs", ses);
		}
		actual.notifyListeners(null);
	}
}
