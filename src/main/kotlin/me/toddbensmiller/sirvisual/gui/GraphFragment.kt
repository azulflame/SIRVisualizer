/*
   Copyright 2021 Todd Bensmiller

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package me.toddbensmiller.sirvisual.gui

import javafx.scene.chart.NumberAxis
import me.toddbensmiller.sirvisual.SIRModel
import tornadofx.*

class GraphFragment : Fragment("Population Graph") {

	private val timeAxis = NumberAxis()

	init {
		timeAxis.label = "Time"
	}

	private val popAxis = NumberAxis()

	init {
		popAxis.label = "Population"
	}

	override val root = linechart("", timeAxis, popAxis) {
		createSymbols = false
		series("Susceptible") {

			SIRModel.history.forEachIndexed { i, x ->
				data(i, x.s)
			}
		}
		series("Infected")
		{
			SIRModel.history.forEachIndexed { i, x ->
				data(i, x.i)
			}
		}
		series("Removed")
		{
			SIRModel.history.forEachIndexed { i, x ->
				data(i, x.r)
			}
		}
		series("Vaccinated")
		{
			SIRModel.history.forEachIndexed { i, x ->
				data(i, x.v)
			}
		}
	}
}