package me.toddbensmiller.sirvisual.gui

import javafx.scene.chart.NumberAxis
import me.toddbensmiller.sirvisual.SIRModel
import tornadofx.*

/*
 * Created by Todd on 11/24/2020.
 */
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