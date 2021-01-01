package me.toddbensmiller.sirvisual

import com.natpryce.konfig.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.toddbensmiller.sirvisual.gui.SIRGUIApp
import tornadofx.launch
import java.io.File

/*
 * Created by Todd on 11/13/2020.
 */

fun main(args: Array<String>) {

	val sToIRate = Key("infectionRate", doubleType)
	val iToRRate = Key("recoveryRate", doubleType)
	val rToSRate = Key("relapseRate", doubleType)
	val isSIRS = Key("isSIRS", booleanType)
	val neighborRadius = Key("radius", intType)
	val initial = Key("initial", intType)
	val delay = Key("delay", intType)
	val initialSize = Key("size", intType)

	val config = ConfigurationProperties.fromFile(File("sirs.properties")) overriding
			ConfigurationProperties.fromResource("default.properties")

	SIRModel.init(
		config[initialSize],
		config[neighborRadius],
		config[sToIRate],
		config[iToRRate],
		config[rToSRate],
		config[isSIRS],
		config[initial],
		config[delay]
	)

	GlobalScope.launch { SIRModel.reset() }
	launch<SIRGUIApp>(args)

}