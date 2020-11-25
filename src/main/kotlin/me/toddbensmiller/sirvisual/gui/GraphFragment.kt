package me.toddbensmiller.sirvisual.gui

import com.sun.javafx.css.Style
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import me.toddbensmiller.sirvisual.SIRModel
import tornadofx.Fragment
import tornadofx.data
import tornadofx.linechart
import tornadofx.series

/*
 * Created by Todd on 11/24/2020.
 */
class GraphFragment: Fragment()
{
	override val root = linechart("Population vs Time", NumberAxis(), NumberAxis()) {
		series("Susceptible")
		{
			SIRModel.history.indices.forEach { x ->
				data(x,SIRModel.history[x].first)
			}
		}
		series("Infected")
		{
			SIRModel.history.indices.forEach { x ->
				data(x,SIRModel.history[x].second)
			}
		}
		series("Removed")
		{
			SIRModel.history.indices.forEach { x ->
				data(x,SIRModel.history[x].third)
			}
		}
	}
}