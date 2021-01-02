package me.toddbensmiller.sirvisual.gui

import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.WritableImage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.toddbensmiller.sirvisual.SIRColors
import me.toddbensmiller.sirvisual.SIRModel
import me.toddbensmiller.sirvisual.SIRModel.addSize
import tornadofx.*
import kotlin.math.roundToInt

/*
 * Created by Todd on 11/14/2020.
 */

class SIRModelView : View("SIR Visualizer") {


	companion object {
		var sizeMin = 10.0
		var sizeMax = 1000.0
		var sizeStep = 10.0

		var radiusMin = 1.0
		var radiusMax = 12.0
		var radiusStep = 1.0

		var infRateMin = 0.0
		var infRateMax = 0.001
		var infRateStep = .0005

		var recoveryRateMin = 0.0
		var recoveryRateMax = 0.25
		var recoveryRateStep = 0.01

		var recycleRateMin = 0.0
		var recycleRateMax = 0.25
		var recycleRateStep = 0.01

		var delayMin = 0.0
		var delayMax = 100.0
		var delayStep = 1.0

		var initialMin = 1.0
		var initialMax = 25.0
		var initialStep = 1.0

		var vaccMin = 0.0
		var vaccMax = 0.25
		var vaccStep = 0.01

		val gridImageProp = SimpleObjectProperty(SIRModel.getImage())
		var gridImage: WritableImage by gridImageProp

		fun setRanges(
			sizeMin: Double, sizeMax: Double, sizeStep: Double,
			radiusMin: Double, radiusMax: Double, radiusStep: Double,
			infRateMin: Double, infRateMax: Double, infRateStep: Double,
			recoveryRateMin: Double, recoveryRateMax: Double, recoveryRateStep: Double,
			recycleRateMin: Double, recycleRateMax: Double, recycleRateStep: Double,
			delayMin: Double, delayMax: Double, delayStep: Double,
			initialMin: Double, initialMax: Double, initialStep: Double,
			vaccMin: Double, vaccMax: Double, vaccStep: Double
		) {
			this.sizeMin = sizeMin
			this.sizeMax = sizeMax
			this.sizeStep = sizeStep

			this.radiusMin = radiusMin
			this.radiusMax = radiusMax
			this.radiusStep = radiusStep

			this.infRateMin = infRateMin
			this.infRateMax = infRateMax
			this.infRateStep = infRateStep

			this.recoveryRateMin = recoveryRateMin
			this.recoveryRateMax = recoveryRateMax
			this.recoveryRateStep = recoveryRateStep

			this.recycleRateMin = recycleRateMin
			this.recycleRateMax = recycleRateMax
			this.recycleRateStep = recycleRateStep

			this.delayMin = delayMin
			this.delayMax = delayMax
			this.delayStep = delayStep

			this.initialMin = initialMin
			this.initialMax = initialMax
			this.initialStep = initialStep

			this.vaccMin = vaccMin
			this.vaccMax = vaccMax
			this.vaccStep = vaccStep
		}
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
								textFill = SIRColors.s
								SIRModel.sProp.onChange { x ->
									Platform.runLater { text = "$x" }
								}
							}
						}
						field("Infected")
						{
							label {
								text = "${SIRModel.infectedCount}"
								textFill = SIRColors.i
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
								textFill = SIRColors.r
								SIRModel.rProp.onChange { x ->
									// Avoid throwing IllegalStateException by running from a non-JavaFX thread.
									Platform.runLater { text = "$x" }
								}
							}
						}
						field("Vaccinated")
						{
							label {
								text = "${SIRModel.vaccinatedCount}"
								textFill = SIRColors.v
								SIRModel.vProp.onChange { x ->
									Platform.runLater { text = "$x" }
								}
							}
						}
						// infection neighborhood
						fieldset("Data Controls") {
							field("Size")
							{
								hbox {
									slider {
										min = sizeMin
										max = sizeMax
										value = SIRModel.size.toDouble()
										valueProperty().onChange { x ->
											val y = roundToNearest(x, sizeStep).toInt()
											GlobalScope.launch { addSize(y) }
											value = y.toDouble()
										}
									}
									label {
										text = SIRModel.size.toString()
										SIRModel.sizeProp.onChange { x ->
											autosize()
											Platform.runLater { text = "$x" }
										}
									}
								}
							}
							field("Radius") {
								hbox {
									slider {
										min = radiusMin
										max = radiusMax
										value = SIRModel.neighborRadius.toDouble()
										valueProperty().onChange { x ->
											val y = roundToNearest(x, radiusStep).toInt()
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
										min = infRateMin
										max = infRateMax
										value = SIRModel.susceptibleToInfectedChance
										valueProperty().onChange { x ->
											val y = roundToNearest(x, infRateStep)
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
										min = recoveryRateMin
										max = recoveryRateMax
										value = SIRModel.infectedToRemovedChance
										valueProperty().onChange { x ->
											val y = roundToNearest(x, recoveryRateStep)
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
										min = recycleRateMin
										max = recycleRateMax
										value = SIRModel.removedToSusceptibleChance
										valueProperty().onChange { x ->
											val y = roundToNearest(x, recycleRateStep)
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
							field("Vaccinate")
							{
								hbox {
									checkbox("", SIRModel.vaccToggleProp)
								}
							}
							field("Vaccination Rate")
							{
								hbox {
									slider {
										min = vaccMin
										max = vaccMax
										value = SIRModel.vaccRate
										valueProperty().onChange { x ->
											val y = roundToNearest(x, vaccStep)
											SIRModel.vaccRate = y
											value = y
										}
									}
									label {
										text = "${SIRModel.vaccRate}"
										SIRModel.vaccProp.onChange { x ->
											Platform.runLater { text = "$x" }
										}
									}
								}
							}
							field("Minimum Frame Time")
							{
								hbox {
									slider {
										min = delayMin
										max = delayMax
										value = SIRModel.minFrameTime.toDouble()
										valueProperty().onChange { x ->
											val y = roundToNearest(x, delayStep).toInt()
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
										min = initialMin
										max = initialMax
										value = SIRModel.initialCount.toDouble()
										valueProperty().onChange { x ->
											val y = roundToNearest(x, initialStep).toInt()
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

fun roundToNearest(num: Double, step: Double): Double {
	return (num / step).roundToInt() * step
}