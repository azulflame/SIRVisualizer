package me.toddbensmiller.sirvisual.gui

import javafx.scene.chart.NumberAxis
import me.toddbensmiller.sirvisual.SIRModel
import tornadofx.*

/*
 * Created by Todd on 11/24/2020.
 */
class GraphFragment : Fragment() {
	override val root = linechart("Population vs Time", NumberAxis(), NumberAxis()) {
		createSymbols = false
		multiseries("Susceptible", "Infected", "Removed")
		{
			SIRModel.history.forEachIndexed { i, (first, second, third) ->
				data(i, first, second, third)
			}
		}
	}
}