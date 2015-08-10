/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig.piggybank.squeal.builtin;

import org.apache.pig.piggybank.squeal.builtin.KMIN.BaseMinInitInverse;
import org.apache.pig.piggybank.squeal.builtin.KMIN.MinIntermed;
import org.joda.time.DateTime;

/**
 * Replace MIN with a function that will keep k (defaults to 10) intermediate values
 * for buffering negative values.
 * 
 * @author JamesLampton
 *
 */
public class DateTimeMin extends MinMaxDoppleganger<org.apache.pig.builtin.DateTimeMin, DateTime> {

//  funcList.add(new FuncSpec(DateTimeMin.class.getName(), Schema.generateNestedSchema(DataType.BAG, DataType.BIGDECIMAL)));
	static public class DDateTimeMinInitInverse extends MinMaxDoppleganger.DopInitialInverse<DateTimeMin> { }
	static public class DDateTimeMinInit extends MinMaxDoppleganger.DopInitial<DateTimeMin> {
		public String getInitialInverse() {
			return BaseMinInitInverse.class.getName();
		}
	}
	static public class DDateTimeMinFinal extends MinMaxDoppleganger.DopFinal<DateTimeMin, DateTime> {}

	public DateTimeMin() {
	}

	@Override
	public String getInitial() {
		return DDateTimeMinInit.class.getName();
	}

	@Override
	public String getIntermed() {
		return MinIntermed.class.getName();
	}

	@Override
	public String getFinal() {
		return DDateTimeMinFinal.class.getName();
	}
}