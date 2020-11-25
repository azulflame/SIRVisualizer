package me.toddbensmiller.sirvisual.gui

import javafx.application.Platform
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import me.toddbensmiller.sirvisual.SIRModel
import tornadofx.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * Created by Todd on 11/14/2020.
 */

class SIRModelView : View() {


	init {
		generateRects()
		recolorRects()
	}

	companion object {
		var rects = Array(SIRModel.getSize()) { Array(SIRModel.getSize()) { Rectangle() } }

		fun generateRects() {
			SIRModel.getGrid().indices.forEach { row ->
				SIRModel.getGrid()[row].indices.forEach { col ->
					rects[row][col] = Rectangle(row.toDouble(), col.toDouble(), 1.0, 1.0)
				}
			}
		}

		fun recolorRects() {
			SIRModel.getGrid().indices.forEach { row ->
				SIRModel.getGrid()[row].indices.forEach { col ->
					rects[row][col].fill = SIRModel.getColorOfCell(row, col)
				}
			}
		}
	}

	override val root = stackpane {
		hbox {
			group {
				vbox {
					rects.indices.forEach { row ->
						hbox {
							rects[row].indices.forEach { col ->
								rectangle {
									width = rects[row][col].width
									height = rects[row][col].height
									x = rects[row][col].x
									y = rects[row][col].y
									fill = SIRModel.getColorOfCell(row, col)
									rects[row][col].fillProperty().onChange {
										fill = SIRModel.getColorOfCell(x.toInt(), y.toInt())
									}
								}
							}
						}
					}
				}
			}
			vbox {
				hbox {
					button("PLAY").action {
						GlobalScope.async {
							SIRModel.play()
						}
					}
					button("PAUSE").action {
						SIRModel.pause()
					}
					button("RESET").action {
						GlobalScope.launch { SIRModel.reset() }
					}
				}
				form()
				{
					fieldset("Statistics")
					{
						field("Susceptible")
						{
							label {
								text = "${SIRModel.susceptibleCount}"
								textFill = Color.rgb(50, 160, 50)
								SIRModel.sProp.onChange { x ->
									Platform.runLater { text = "$x" }
								}
							}
						}
						field("Infected")
						{
							label {
								text = "${SIRModel.infectedCount}"
								textFill = Color.rgb(254, 39, 18)
								SIRModel.iProp.onChange { x ->
									if (x == 0) {
										SIRModel.pause()
									}
									Platform.runLater { text = "$x" }
								}
							}
						}
						field("Removed")
						{
							label {
								text = "${SIRModel.removedCount}"
								textFill = Color.rgb(140, 34, 34)
								SIRModel.rProp.onChange { x ->
									// Avoid throwing IllegalStateException by running from a non-JavaFX thread.
									Platform.runLater { text = "$x" }
								}
							}
						}

						// infection neighborhood
						fieldset("Data Controls") {
							field("Radius") {
								hbox {
									slider {
										min = 1.0
										max = 12.0
										value = SIRModel.neighborRadius.toDouble()
										valueProperty().onChange { x ->
											val y = (10000 * x).roundToInt() / 10000.0
											SIRModel.neighborRadius = y.toInt()
											value = y
										}
									}

									label {
										text = SIRModel.neighborRadius.toString()
										SIRModel.radiusProp.onChange { x ->
											text = "$x"
										}
									}
								}
							}
							field("Infection Rate")
							{
								hbox {
									slider {
										min = 0.0
										max = 0.01
										value = SIRModel.susceptibleToInfectedChance
										valueProperty().onChange { x ->
											val y = (10000 * x).roundToInt() / 10000.0
											SIRModel.susceptibleToInfectedChance = y
											value = y
										}
									}
									label {
										text = SIRModel.susceptibleToInfectedChance.toString()
										SIRModel.infRateProp.onChange { x ->
											text = "$x"
										}
									}
								}
							}
							field("Recovery Rate")
							{
								hbox {
									slider {
										min = 0.0
										max = 0.04
										value = SIRModel.infectedToRemovedChance
										valueProperty().onChange { x ->
											val y = (10000 * x).roundToInt() / 10000.0
											SIRModel.infectedToRemovedChance = y
											value = y
										}
									}
									label {
										text = SIRModel.infectedToRemovedChance.toString()
										SIRModel.recRateProp.onChange { x ->
											text = "$x"
										}
									}
								}
							}
							field("Enable SIRS")
							{
								checkbox("", SIRModel.sirsProp)
							}
							field("Recycle Rate")
							{
								hbox {
									slider {
										min = 0.0
										max = 0.1
										value = SIRModel.removedToSusceptibleChance
										valueProperty().onChange { x ->
											val y = (10000 * x).roundToInt() / 10000.0
											SIRModel.removedToSusceptibleChance = y
											value = y
										}
										isDisable = !SIRModel.isSIRS
										SIRModel.sirsProp.onChange { x ->
											isDisable = !x
										}
									}
									label {
										text = SIRModel.removedToSusceptibleChance.toString()
										SIRModel.recycleProp.onChange { x ->
											text = "$x"
										}
									}
								}
							}
							field("Minimum Frame Time")
							{
								hbox {
									/*slider {
										min = 30.0
										max = 1000.0
										value = SIRModel.minFrameTime.toDouble()
										valueProperty().onChange { x ->
											val y = (x / 10).roundToLong() * 10
											SIRModel.minFrameTime = y
											value = y.toDouble()
										}
									}*/
									label {
										text = "${SIRModel.minFrameTime} ms"
										SIRModel.minFrameProp.onChange {x ->
											Platform.runLater{text = "$x ms"}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}