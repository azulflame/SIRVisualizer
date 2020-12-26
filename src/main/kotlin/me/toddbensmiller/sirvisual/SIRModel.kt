/*
 * Created by Todd on 11/13/2020.
 */

package me.toddbensmiller.sirvisual

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.toddbensmiller.sirvisual.gui.SIRModelView
import tornadofx.getValue
import tornadofx.setValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random
import kotlin.random.Random.Default.nextDouble
import kotlin.system.measureTimeMillis


object SIRModel {
	var size: Int = 450
	private var grid: Array<Array<SIRState>> = Array(size) { Array(size) { SIRState.SUSCEPTIBLE } }

	val radiusProp = SimpleIntegerProperty(8)
	var neighborRadius by radiusProp
	val infRateProp = SimpleDoubleProperty(0.03)
	var susceptibleToInfectedChance by infRateProp
	val recRateProp = SimpleDoubleProperty(0.03)
	var infectedToRemovedChance by recRateProp
	val recycleProp = SimpleDoubleProperty(0.03)
	var removedToSusceptibleChance by recycleProp
	val sirsProp = SimpleBooleanProperty(false)
	var isSIRS by sirsProp
	val minFrameProp = SimpleLongProperty(16)
	var minFrameTime by minFrameProp
	val initialProp = SimpleIntegerProperty(1)
	var initialCount by initialProp

	var isPaused: Boolean = true

	val iProp = SimpleIntegerProperty(0)
	var infectedCount by iProp
	val sProp = SimpleIntegerProperty(0)
	var susceptibleCount by sProp
	val rProp = SimpleIntegerProperty(0)
	var removedCount by rProp
	val vProp = SimpleIntegerProperty(0)
	var vaccinatedCount by vProp
	val frameProp = SimpleLongProperty(0)
	var frameTime by frameProp
	val history: ArrayList<Triple<Int, Int, Int>> = ArrayList()
	var isReady: Boolean = true
	private val step_mutex = Mutex()
	private val image_mutex = Mutex()

	var imageOut = WritableImage(size, size)
	var tempImage = WritableImage(size, size)


	fun init(
		size: Int,
		neighborRadius: Int,
		susceptibleToInfectedChance: Double,
		infectedToRemovedChance: Double,
		removedToSusceptibleChance: Double,
		isSIRS: Boolean,
		initial: Int
	) {
		this.size = size
		this.neighborRadius = neighborRadius
		this.susceptibleToInfectedChance = susceptibleToInfectedChance
		this.infectedToRemovedChance = infectedToRemovedChance
		this.removedToSusceptibleChance = removedToSusceptibleChance
		this.isSIRS = isSIRS
		initialCount = initial
	}


	fun setStateOfCell(row: Int, col: Int, state: SIRState) {
		if (row < 0 || col < 0 || row >= grid.size || col >= grid[row].size)
			return
		if (grid[row][col] != state) {
			when (state) {
				SIRState.REMOVED -> removedCount++
				SIRState.INFECTED -> infectedCount++
				SIRState.SUSCEPTIBLE -> susceptibleCount++
				else -> {
				}
			}
			when (grid[row][col]) {
				SIRState.REMOVED -> removedCount--
				SIRState.INFECTED -> infectedCount--
				SIRState.SUSCEPTIBLE -> susceptibleCount--
				else -> {
				}
			}
		}
		grid[row][col] = state
	}


	private suspend fun step() {
		identifyChanges()
		applyChanges()
	}

	private suspend fun identifyChanges() {
		image_mutex.withLock {
			for (row in size - 1 downTo 0) {
				for (col in size - 1 downTo 0) {
					if (grid[row][col] == SIRState.INFECTED_DECAY_ONLY) {
						if (nextDouble() < infectedToRemovedChance) {
							grid[row][col] = SIRState.REMOVED_TRANSITION
						}
					} else if (grid[row][col] == SIRState.INFECTED) {
						var hasSusNearby = isSIRS
						for (verticalOffset in -neighborRadius..neighborRadius) {
							if (row + verticalOffset in 0 until size) {
								val taxiRadius = neighborRadius - abs(verticalOffset) // taxicab radius
								for (horizontalOffset in -taxiRadius..taxiRadius) {
									if (col + horizontalOffset in 0 until size) {
										if (grid[row + verticalOffset][col + horizontalOffset] == SIRState.SUSCEPTIBLE) {
											hasSusNearby = true
											if (nextDouble() < susceptibleToInfectedChance) {
												grid[row + verticalOffset][col + horizontalOffset] =
													SIRState.INFECTED_TRANSITION
											}
										}
									}
								}
							}
						}
						if (!hasSusNearby) {
							grid[row][col] = SIRState.INFECTED_DECAY_ONLY
						}
						if (nextDouble() < infectedToRemovedChance) {
							grid[row][col] = SIRState.REMOVED_TRANSITION
						}
					} else if (isSIRS && grid[row][col] == SIRState.REMOVED) {
						if (nextDouble() < removedToSusceptibleChance) {
							grid[row][col] = SIRState.SUSCEPTIBLE_TRANSITION
						}
					}
				}
			}
		}
	}

	private suspend fun applyChanges() {
		image_mutex.withLock {
			for (row in grid.indices) {
				for (col in grid[row].indices) {
					if (grid[row][col] == SIRState.INFECTED_TRANSITION) {
						grid[row][col] = SIRState.INFECTED
						infectedCount++
						susceptibleCount--
					} else if (grid[row][col] == SIRState.SUSCEPTIBLE_TRANSITION) {
						grid[row][col] = SIRState.SUSCEPTIBLE
						removedCount--
						susceptibleCount++
					} else if (grid[row][col] == SIRState.REMOVED_TRANSITION) {
						grid[row][col] = SIRState.REMOVED
						removedCount++
						infectedCount--
					}
				}
			}
		}
		updateImage()
	}


	fun getColorOfCell(row: Int, col: Int): Color {
		return when (grid[row][col]) {
			SIRState.INFECTED -> Color.rgb(254, 39, 18)
			SIRState.SUSCEPTIBLE -> Color.rgb(50, 250, 50)
			SIRState.REMOVED -> Color.rgb(34, 34, 134)
			SIRState.INFECTED_DECAY_ONLY -> Color.rgb(254, 39, 19)
			else -> Color.BLACK
		}
	}

	suspend fun play() {
		isPaused = false
		while (!isPaused) {
			step_mutex.withLock {
				if (!isPaused) // this exists in case multiple instances of play() are invoked. normally pause would wait for each instance to iterate 1 more time
				{
					frameTime = measureTimeMillis {
						isReady = false
						step()
						history.add(Triple(susceptibleCount, infectedCount, removedCount))
					}
					val delayTimer = minFrameTime - frameTime


					if (delayTimer > 0 && frameTime * 1.5 < minFrameTime && minFrameTime > SIRModelView.lastUserFrameTimeSetting) {
						minFrameTime = max((frameTime / 1.5).toLong(), SIRModelView.lastUserFrameTimeSetting)
						delay(frameTime / 2)
						SIRModelView.gridImage = getImage()
						delay(frameTime / 2)

					} else if (delayTimer <= 0) {
						minFrameTime = (1.75 * (frameTime - delayTimer)).toLong()
						delay(frameTime / 2)
						SIRModelView.gridImage = getImage()
						delay(frameTime / 2)
					} else {
						delay(delayTimer / 2)
						SIRModelView.gridImage = getImage()
						delay(delayTimer / 2)
					}
				}
			}
		}
	}

	fun pause() {
		isPaused = true
	}

	suspend fun reset() {
		isPaused = true
		step_mutex.withLock {
			grid = Array(size) { Array(size) { SIRState.SUSCEPTIBLE } }
			susceptibleCount = size * size
			infectedCount = 0
			removedCount = 0
			vaccinatedCount = 0
			for (x in 1..initialCount) {
				setStateOfCell(Random.nextInt(300) + 50, Random.nextInt(300) + 50, SIRState.INFECTED)
			}
			updateImage()
			history.clear()
			SIRModelView.gridImage = getImage()
		}
	}

	fun getImage(): WritableImage {
		return imageOut
	}

	suspend fun updateImage() {
		image_mutex.withLock {
			for (x in 0 until size) {
				for (y in 0 until size) {
					tempImage.pixelWriter.setColor(x, y, getColorOfCell(x, y))
				}
			}
		}
		moveImage()
	}

	suspend fun moveImage() {
		image_mutex.withLock {
			imageOut = tempImage
			tempImage = WritableImage(size, size)
		}
	}
}

