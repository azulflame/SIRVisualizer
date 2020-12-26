package me.toddbensmiller.sirvisual

import me.toddbensmiller.sirvisual.gui.SIRGUIApp
import tornadofx.*
import com.natpryce.konfig.*
import javafx.scene.image.Image
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random.Default.nextInt

/*
 * Created by Todd on 11/13/2020.
 */

fun main(args: Array<String>)
{

	val sToIRate = Key("infectionRate", doubleType)
	val iToRRate = Key("recoveryRate", doubleType)
	val rToSRate = Key("relapseRate", doubleType)
	val isSIRS = Key("isSIRS", booleanType)
	val neighborRadius = Key("radius", intType)
	val initial = Key("initial", intType)

	val config = ConfigurationProperties.fromFile(File("sirs.properties")) overriding
			ConfigurationProperties.fromResource("default.properties")

	SIRModel.init(450, config[neighborRadius], config[sToIRate], config[iToRRate], config[rToSRate], config[isSIRS], config[initial])

	SIRModel.setStateOfCell(nextInt(300) + 50, nextInt(300) + 50, SIRState.INFECTED)
	GlobalScope.launch { SIRModel.reset() }
	launch<SIRGUIApp>(args)

}