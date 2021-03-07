/*
 * Created by Todd on 11/13/2020.
 */

package me.toddbensmiller.sirvisual

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.toddbensmiller.sirvisual.gui.GraphFragment
import me.toddbensmiller.sirvisual.gui.SIRModelView
import tornadofx.getValue
import tornadofx.setValue
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random.Default.nextDouble
import kotlin.random.Random.Default.nextInt
import kotlin.system.measureTimeMillis


object SIRModel {
	val sizeProp = SimpleIntegerProperty(450)
	var size by sizeProp
	private val sizeStack = Stack<Int>()

	private lateinit var grid: Array<Array<SIRState>>

	val radiusProp = SimpleIntegerProperty(8)
	var neighborRadius by radiusProp
	val infRateProp = SimpleDoubleProperty(0.03)
	var susceptibleToInfectedChance by infRateProp
	val recRateProp = SimpleDoubleProperty(0.03)
	var infectedToRemovedChance by recRateProp
	val recycleProp = SimpleDoubleProperty(0.03)
	var removedToSusceptibleChance by recycleProp
	val vaccProp = SimpleDoubleProperty(0.01)
	var vaccRate by vaccProp
	val sirsProp = SimpleBooleanProperty(false)
	var isSIRS by sirsProp
	val vaccToggleProp = SimpleBooleanProperty(false)
	private var vaccToggle by vaccToggleProp
	val minFrameProp = SimpleIntegerProperty(16)
	var minFrameTime by minFrameProp
	val initialProp = SimpleIntegerProperty(1)
	var initialCount by initialProp

	private var isPaused: Boolean = true

	val iProp = SimpleIntegerProperty(0)
	var infectedCount by iProp
	val sProp = SimpleIntegerProperty(0)
	var susceptibleCount by sProp
	val rProp = SimpleIntegerProperty(0)
	var removedCount by rProp
	val vProp = SimpleIntegerProperty(0)
	var vaccinatedCount by vProp

	val history: MutableList<History> = mutableListOf()
	private val step_mutex = Mutex()
	private val image_mutex = Mutex()

	private var imageOut = WritableImage(size, size)
	private lateinit var tempImage: WritableImage

	private val infectedMap = HashSet<Int>()
	private val freshInfectedMap = HashSet<Int>()
	private val susceptibleMap = HashSet<Int>()
	private val removedMap = HashSet<Int>()
	private val freshRemovedMap = HashSet<Int>()
	private val vaccMap = HashSet<Int>()

	private fun keyToCoords(key: Int): Pair<Int, Int> = Pair(key / size, key % size)
	private fun coordsToKey(x: Int, y: Int) = x * size + y

	fun init(
		size: Int,
		neighborRadius: Int,
		susceptibleToInfectedChance: Double,
		infectedToRemovedChance: Double,
		removedToSusceptibleChance: Double,
		isSIRS: Boolean,
		initial: Int,
		delay: Int,
		vaccRate: Double
	) {
		this.size = size
		this.neighborRadius = neighborRadius
		this.susceptibleToInfectedChance = susceptibleToInfectedChance
		this.infectedToRemovedChance = infectedToRemovedChance
		this.removedToSusceptibleChance = removedToSusceptibleChance
		this.isSIRS = isSIRS
		initialCount = initial
		this.minFrameTime = delay
		this.vaccRate = vaccRate
	}

	private fun infect(x: Int, y: Int) {
		val hash = coordsToKey(x, y)
		susceptibleMap.remove(hash)
		grid[x][y] = SIRState.INFECTED
		freshInfectedMap.add(hash)
	}

	private fun initialInfect(x: Int, y: Int) {
		val hash = coordsToKey(x, y)
		susceptibleMap.remove(hash)
		grid[x][y] = SIRState.INFECTED
		infectedMap.add(hash)
	}

	/**
	 * Decay a cell from infected to removed
	 *
	 * @param x X coordinate of the cell
	 * @param y Y coordinate of the cell
	 */
	private fun decay(x: Int, y: Int) {
		val hash = coordsToKey(x, y)
		infectedMap.remove(hash)
		grid[x][y] = SIRState.REMOVED
		freshRemovedMap.add(hash)
	}

	private fun reintroduce(x: Int, y: Int) {
		val hash = coordsToKey(x, y)
		removedMap.remove(hash)
		grid[x][y] = SIRState.SUSCEPTIBLE
		susceptibleMap.add(hash)
	}

	private fun vaccinate(x: Int, y: Int) {
		val hash = coordsToKey(x, y)
		susceptibleMap.remove(hash)
		grid[x][y] = SIRState.VACCINATED
		vaccMap.add(hash)
	}

	private fun processGrid() {
		// use the map of lesser size (will normally be the infected cells map, with default parameters
		if (vaccToggle) {
			susceptibleMap.toSet().forEach { cell ->
				if (nextDouble() < vaccRate) {
					keyToCoords(cell).let { vaccinate(it.first, it.second) }
				}
			}
		}
		if (susceptibleMap.size < infectedMap.size) {
			// infect using the susceptible cells
			for (cell in susceptibleMap.toSet()) {
				val coords = keyToCoords(cell)
				val x = coords.first
				val y = coords.second

				// determine the in-range cells of this cell
				val xRangeMin = max(0, x - neighborRadius)
				val xRangeMax = min(size - 1, x + neighborRadius)
				val yRangeMin = max(0, y - neighborRadius)
				val yRangeMax = min(size - 1, y + neighborRadius)

				var localInfCount = 0
				for (xval in xRangeMin..xRangeMax) {
					for (hash in (size * xval) + yRangeMin..(size * xval) + yRangeMax) {
						if (infectedMap.contains(hash)) {
							localInfCount++
						}
					}
				}
				// use binomial distribution to find the chance that "at least one" cell will infect this way
				if (localInfCount > 0 && nextDouble() < 1 - (1 - susceptibleToInfectedChance).pow(localInfCount.toDouble())
				) {
					infect(x, y)
				}
			}
		} else {
			// infect using infected cells to get random susceptible cells
			for (cell in infectedMap.toSet()) {
				val coords = keyToCoords(cell)
				val x = coords.first
				val y = coords.second

				// determine the in-range cells of this cell
				val xRangeMin = max(0, x - neighborRadius)
				val xRangeMax = min(size - 1, x + neighborRadius)
				val yRangeMin = max(0, y - neighborRadius)
				val yRangeMax = min(size - 1, y + neighborRadius)

				// infect cells (by chance) in the range
				for (dx in xRangeMin..xRangeMax) {
					for (hash in (size * dx + yRangeMin)..(size * dx + yRangeMax)) {
						if (susceptibleMap.contains(hash)) {
							if (nextDouble() < susceptibleToInfectedChance) {
								keyToCoords(hash).let { infect(it.first, it.second) }
							}
						}
					}
				}
			}
		}
		// decay cells to removed
		for (cell in infectedMap.toSet()) {
			if (nextDouble() < infectedToRemovedChance) {
				keyToCoords(cell).let { decay(it.first, it.second) }
			}
		}
		// if we are using the SIRS model, reintroduce removed cells as susceptible
		if (isSIRS) {
			for (cell in removedMap.toSet()) {
				if (nextDouble() < removedToSusceptibleChance) {
					keyToCoords(cell).let { reintroduce(it.first, it.second) }
				}
			}
		}

		// load all new cells into their appropriate states
		infectedMap.addAll(freshInfectedMap)
		freshInfectedMap.clear()
		removedMap.addAll(freshRemovedMap)
		freshRemovedMap.clear()

		// update the size counts
		infectedCount = infectedMap.size
		susceptibleCount = susceptibleMap.size
		removedCount = removedMap.size
		vaccinatedCount = vaccMap.size
	}


	private suspend fun step() {
		processGrid()
		updateImage()
		SIRModelView.gridImage = getImage()
	}

	private fun getColorOfCell(row: Int, col: Int): Color {
		return when (grid[row][col]) {
			SIRState.INFECTED -> SIRColors.i
			SIRState.SUSCEPTIBLE -> SIRColors.s
			SIRState.REMOVED -> SIRColors.r
			SIRState.VACCINATED -> SIRColors.v
		}
	}

	suspend fun play() {
		isPaused = false
		while (!isPaused) {
			step_mutex.withLock {
				if (!isPaused) // this exists in case multiple instances of play() are invoked. normally pause would wait for each instance to iterate 1 more time
				{
					val milliTime = measureTimeMillis {
						GlobalScope.launch { step() }.join()
						GlobalScope.launch { SIRModelView.gridImage = getImage() }.join()
					}
					delay(minFrameTime - milliTime)
					history.add(History(susceptibleCount, infectedCount, removedCount, vaccinatedCount))
					if (infectedCount == 0) {
						end()
					}
				}
			}
		}
	}

	fun pause() {
		isPaused = true
	}

	private fun end() {
		pause()
		val graph = GraphFragment()
		Platform.runLater { graph.openWindow() }
	}

	suspend fun invokeEnd() {
		pause()
		step_mutex.withLock {
			end()
		}
	}

	suspend fun reset() {
		isPaused = true
		step_mutex.withLock {
			// hard reset everything, since we have no garuntee of a clean exit from whatever the previous state was
			grid = Array(size) { Array(size) { SIRState.SUSCEPTIBLE } }
			tempImage = WritableImage(size, size)
			susceptibleMap.clear()
			removedMap.clear()
			infectedMap.clear()
			freshRemovedMap.clear()
			freshInfectedMap.clear()
			vaccMap.clear()
			for (cell in 0 until (size * size - 1)) {
				susceptibleMap.add(cell)
			}
			for (x in 1..initialCount) {
				initialInfect(
					nextInt((.1 * size).toInt(), (.9 * size).toInt()),
					nextInt((.1 * size).toInt(), (.9 * size).toInt())
				)
			}
			susceptibleCount = susceptibleMap.size
			removedCount = removedMap.size
			infectedCount = infectedMap.size
			vaccinatedCount = vaccMap.size

			updateImage()
			history.clear()
			SIRModelView.gridImage = getImage()
		}
	}

	fun getImage(): WritableImage {
		return imageOut
	}

	private suspend fun updateImage() {
		image_mutex.withLock {
			for (x in 0 until size) {
				for (y in 0 until size) {
					tempImage.pixelWriter.setColor(x, y, getColorOfCell(x, y))
				}
			}
		}
		moveImage()
	}

	private suspend fun moveImage() {
		image_mutex.withLock {
			imageOut = tempImage
			tempImage = WritableImage(size, size)
		}
	}

	private suspend fun resize(newSize: Int) {
		pause()
		step_mutex.withLock {
			image_mutex.withLock {
				size = newSize
				tempImage = WritableImage(size, size)
			}
			println("$size $newSize")
		}
		reset()
	}

	suspend fun addSize(newSize: Int) { // debounce for resize
		sizeStack.add(newSize)
		val oldSize = sizeStack.size
		delay(250)
		if (sizeStack.size == oldSize && newSize == sizeStack.peek()) {
			resize(sizeStack.pop())
			sizeStack.clear()
		}
	}
}