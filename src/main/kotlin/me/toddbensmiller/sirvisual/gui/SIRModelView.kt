package me.toddbensmiller.sirvisual.gui

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.toddbensmiller.sirvisual.SIRModel
import tornadofx.*
import kotlin.math.roundToInt

/*
 * Created by Todd on 11/14/2020.
 */

class SIRModelView : View("SIR Visualizer") {


	companion object {
		val gridImageProp = SimpleObjectProperty(SIRModel.getImage())
		var gridImage: WritableImage by gridImageProp
	}

	override val root = stackpane {
		hbox {
			imageview {
				image = gridImage
				gridImageProp.onChange {
					image = gridImage
				}
			}
			vbox {
				hbox {
					button("PLAY").action {
						GlobalScope.launch {
							SIRModel.play()
						}
					}
					button("PAUSE").action {
						SIRModel.pause()
					}
					button("END").action {
						GlobalScope.launch { SIRModel.invokeEnd() }
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
											val y = x.roundToInt()
											SIRModel.neighborRadius = y
											value = y.toDouble()
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
									slider {
										min = 0.0
										max = 100.0
										value = SIRModel.minFrameTime.toDouble()
										valueProperty().onChange { x ->
											val y = x.roundToInt()
											SIRModel.minFrameTime = y
											value = y.toDouble()
										}
									}
									label {
										text = "${SIRModel.minFrameTime} ms"
										SIRModel.minFrameProp.onChange { x ->
											Platform.runLater {
												text = "$x ms"
											}
										}
									}
								}
							}
							field("Initial Count")
							{
								hbox {
									slider {
										min = 1.0
										max = 10.0
										value = SIRModel.initialCount.toDouble()
										valueProperty().onChange { x ->
											val y = x.roundToInt()
											SIRModel.initialCount = y
											value = y.toDouble()
										}
									}
									label {
										text = "${SIRModel.initialCount}"
										SIRModel.initialProp.onChange { x ->
											Platform.runLater { text = "$x" }
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