/*
 * Created by Todd on 11/13/2020.
 */

package me.toddbensmiller.sirvisual

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.scene.paint.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.toddbensmiller.sirvisual.gui.SIRModelView
import tornadofx.getValue
import tornadofx.setValue
import kotlin.random.Random
import kotlin.random.Random.Default.nextDouble
import kotlin.system.measureTimeMillis


object SIRModel {
    private var size: Int = 450
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
	val minFrameProp = SimpleLongProperty(100)
	var minFrameTime by minFrameProp

    var isPaused: Boolean = true

    //    val susProp = SimpleIntegerProperty(0)
//    val infProp = SimpleIntegerProperty(0)
//    val remProp = SimpleIntegerProperty(0)
//    val vacProp = SimpleIntegerProperty(0)
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

    private val step_mutex = Mutex()


    fun init(
        size: Int,
        neighborRadius: Int,
        susceptibleToInfectedChance: Double,
        infectedToRemovedChance: Double,
        removedToSusceptibleChance: Double,
        isSIRS: Boolean = false
    ) {
        this.size = size
        this.neighborRadius = neighborRadius
        this.susceptibleToInfectedChance = susceptibleToInfectedChance
        this.infectedToRemovedChance = infectedToRemovedChance
        this.removedToSusceptibleChance = removedToSusceptibleChance
        this.isSIRS = isSIRS
        resizeGrid()
        susceptibleCount = size * size
    }

    fun getStateOfCell(row: Int, col: Int): SIRState {
        if (row < 0 || col < 0 || row >= grid.size || col >= grid[row].size)
            return SIRState.BAD_STATE
        return grid[row][col]
    }

    fun setStateOfCell(row: Int, col: Int, state: SIRState) {
        if (row < 0 || col < 0 || row >= grid.size || col >= grid[row].size)
            return
        if (grid[row][col] != state) {
            when (state) {
                SIRState.REMOVED -> removedCount++
                SIRState.INFECTED -> infectedCount++
                SIRState.SUSCEPTIBLE -> susceptibleCount++
            }
            when (grid[row][col]) {
                SIRState.REMOVED -> removedCount--
                SIRState.INFECTED -> infectedCount--
                SIRState.SUSCEPTIBLE -> susceptibleCount--
            }
        }
        grid[row][col] = state
    }

    private fun resizeGrid() {
        grid = Array(size) { Array(size) { SIRState.SUSCEPTIBLE } }
    }

    fun resizeGrid(newSize: Int) {
        size = newSize
        resizeGrid()
    }

    private fun step() {
        identifyChanges()
        applyChanges()
        SIRModelView.recolorRects()
    }

    private fun identifyChanges() {
        for (row in size-1 downTo 0) {
            for (col in size-1 downTo 0) {
                if (grid[row][col] == SIRState.INFECTED) {
                    for (verticalOffset in -neighborRadius..neighborRadius) {
                        if (row + verticalOffset in 0 until size) {
                            for (horizontalOffset in -neighborRadius..neighborRadius) {
                                if (col + horizontalOffset in 0 until size) {
                                    if (grid[row + verticalOffset][col + horizontalOffset] == SIRState.SUSCEPTIBLE && nextDouble() < susceptibleToInfectedChance) {
                                        grid[row + verticalOffset][col + horizontalOffset] =
                                            SIRState.INFECTED_TRANSITION
                                    }
                                }
                            }
                        }
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

    private fun applyChanges() {
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

    fun getSize(): Int = size
    fun getGrid(): Array<Array<SIRState>> = grid;

    fun getColorOfCell(row: Int, col: Int): Color {
        return when (grid[row][col]) {
            SIRState.INFECTED -> Color.rgb(254, 39, 18)
            SIRState.SUSCEPTIBLE -> Color.rgb(50, 250, 50)
            SIRState.REMOVED -> Color.rgb(34, 34, 134)
            else -> Color.BLACK
        }
    }

    suspend fun play() {
        isPaused = false
        while (!isPaused) {
            step_mutex.withLock {
                frameTime = measureTimeMillis {
                    step()
                    history.add(Triple(susceptibleCount, infectedCount, removedCount))
                }
                val delayTimer = minFrameTime - frameTime
                if (delayTimer > 0) {
                    if(minFrameTime > 2* frameTime)
                    {
                        minFrameTime = frameTime * 3 / 4
                    }
                    delay(delayTimer)
                }
                else
                {
                    minFrameTime = (frameTime * 1.5).toLong()
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
            setStateOfCell(Random.nextInt(300) + 50, Random.nextInt(300) + 50, SIRState.INFECTED)
            SIRModelView.recolorRects()
            history.clear()
        }
    }
}