/*
   Copyright 2021 Todd Bensmiller

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package me.toddbensmiller.sirvisual

import com.natpryce.konfig.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.toddbensmiller.sirvisual.gui.SIRGUIApp
import me.toddbensmiller.sirvisual.gui.SIRModelView
import tornadofx.launch
import java.io.File

fun main(args: Array<String>) {

	val sToIRate = Key("infectionRate", doubleType)
	val iToRRate = Key("recoveryRate", doubleType)
	val rToSRate = Key("recycleRate", doubleType)
	val isSIRS = Key("isSIRS", booleanType)
	val neighborRadius = Key("radius", intType)
	val initial = Key("initial", intType)
	val delay = Key("delay", intType)
	val initialSize = Key("size", intType)
	val sizeMin = Key("sizeMin", doubleType)
	val sizeMax = Key("sizeMax", doubleType)
	val sizeStep = Key("sizeStep", doubleType)
	val radiusMin = Key("radiusMin", doubleType)
	val radiusMax = Key("radiusMax", doubleType)
	val radiusStep = Key("radiusStep", doubleType)
	val infRateMin = Key("infRateMin", doubleType)
	val infRateMax = Key("infRateMax", doubleType)
	val infRateStep = Key("infRateStep", doubleType)
	val recoveryRateMin = Key("recoveryRateMin", doubleType)
	val recoveryRateMax = Key("recoveryRateMax", doubleType)
	val recoveryRateStep = Key("recoveryRateStep", doubleType)
	val recycleRateMin = Key("recycleRateMin", doubleType)
	val recycleRateMax = Key("recycleRateMax", doubleType)
	val recycleRateStep = Key("recycleRateStep", doubleType)
	val delayMin = Key("delayMin", doubleType)
	val delayMax = Key("delayMax", doubleType)
	val delayStep = Key("delayStep", doubleType)
	val initialMin = Key("initialMin", doubleType)
	val initialMax = Key("initialMax", doubleType)
	val initialStep = Key("initialStep", doubleType)
	val vaccMin = Key("vaccMin", doubleType)
	val vaccMax = Key("vaccMax", doubleType)
	val vaccStep = Key("vaccStep", doubleType)
	val vaccRate = Key("vaccRate", doubleType)


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
		config[delay],
		config[vaccRate]
	)

	SIRModelView.setRanges( config[sizeMin],
		config[sizeMax],
		config[sizeStep],
		config[radiusMin],
		config[radiusMax],
		config[radiusStep],
		config[infRateMin],
		config[infRateMax],
		config[infRateStep],
		config[recoveryRateMin],
		config[recoveryRateMax],
		config[recoveryRateStep],
		config[recycleRateMin],
		config[recycleRateMax],
		config[recycleRateStep],
		config[delayMin],
		config[delayMax],
		config[delayStep],
		config[initialMin],
		config[initialMax],
		config[initialStep],
		config[vaccMin],
		config[vaccMax],
		config[vaccStep])

	GlobalScope.launch { SIRModel.reset() }
	launch<SIRGUIApp>(args)
}
