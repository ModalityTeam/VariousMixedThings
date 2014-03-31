/*

  [move scaler and offsets to InfluxBase?]

* InfluxBase is the base class for the Influx family.
  It passes on incoming values under the same name
  and spreads them as they are to multiple destinations
  by means named actions.

* InfluxMix can accept influences from multiple sources
  and decides on param values based on the influences.
  Different sources can have different trust values,
  which will determine the strength of their influence.

* InfluxSpread can distribute incoming values to
  multiple destinations, with optional rescaling,
  and optional mapping to other parameter names.

* Influx can entangle or disentangle inValues to outValues
  by means of a matrix of weights  which determine how strongly
  a given input param will affect a given output param.

*/

InfluxBase {
	var <inNames, <outNames, <inValDict, <outValDict;
	var <shape, <smallDim, <bigDim;
	var <action;

	*new { |inNames = 0, outNames, inValDict|
		^super.newCopyArgs(inNames, outNames, inValDict).init;
	}

	storeArgs { ^[inNames, outNames] }
	printOn { |receiver, stream|
		^this.storeOn(receiver, stream);
	}

	init {
		var newIns;

		// replace with x, y, z, w, v, u ... and a, b, c, ...
		if (inNames.isKindOf(SimpleNumber)) {
			newIns = [120, 121, 122].keep(inNames);
			if (inNames > 3) { newIns = newIns ++ (119 - (0 .. (inNames - 4))) };
			inNames = newIns.collect { |num| num.asAscii.asSymbol; };
		};

		outNames = outNames ? inNames;

		if (inValDict.isNil) {
			inValDict = ();
			inNames.do (inValDict.put(_, 0));
		};

		outValDict = ();
		action = FuncChain.new;
	}

	doAction { action.value(this) }

		// set input params
	set { |...keyValPairs|
		keyValPairs.pairsDo { |key, val|
			inValDict.put(key, val);
		};
		this.calcOutVals;
		this.doAction;
	}

	calcOutVals {
		// modifications in subclasses
		inValDict.keysValuesDo { |key, val|
			outValDict.put(key, val);
		};
	}

	// interface to FuncChain:
	add { |name, func| action.add(name, func) }
	remove { |name| action.removeAt(name) }
	addFunc { |func| action.addFunc(func) }
	removeFunc { |func| action.removeFunc(func) }

	// attach proxies directly
	attach { |object, funcName|
		funcName = funcName ?? { object.key };
		this.add(funcName, { object.set(*this.outValDict.asKeyValuePairs) });
	}

	detach { |name| this.remove(name); }


		// convenience methods //
	    // prettyprint values
	postv { |round = 0.001|
		var str = "";
		[   ["inVals", inNames, inValDict],
			["outVals", outNames, outValDict]
		].do { |trip|
			var valName, names, vals; #valName, names, vals = trip;
			if (names.notNil) {
				str = str ++ "\n// x.%: \n(\n".format(valName);
				names.do { |name|
					str = str ++
					"\t%: %,\n".format(name, vals[name].round(round))
				};
				str = str ++ ");\n";
			};
		}
		^str
	}

}

/* todo:

* method for making skewed diagonals
* crossfade background task:
*  xfade to new set of weights,
*  xfade to new offsets
*  xfade to multi-offsets,
*   e.g. locate them at (0.5@0.5), (-0.5 @ -0.5)
* same for 3dim controls

* Examples with Tdef, Pdef

* Example with multitouchpad:
*  new finger gets next ndef/tdef, 3 params (vol, x, y)

* PresetZone - a dense field of lots of presets, morph by distance
* PresetGrid - a grid with presets at each intersection


*/


Influx :InfluxBase {
	var <weights, <presets;
	var <>outOffsets, <>inScaler = 1;

	*new { |ins = 2, outs = 8, vals, weights|
		^super.newCopyArgs(ins, outs, vals, weights).init;
	}

	init {
		// replace with x, y, z, w, v, u ... and a, b, c, ...
		if (outNames.isKindOf(SimpleNumber)) {
			outNames = (97 .. (97 + outNames - 1)).collect { |char| char.asAscii.asSymbol };
		};

		super.init;

		outValDict = ();
		outNames.do (outValDict.put(_, 0));

		weights = weights ?? { { 0 ! inNames.size } ! outNames.size };

		outOffsets = 0 ! outNames.size;

		this.makePresets;
		this.rand;
		this.calcOutVals;
	}

	calcOutVals {
		weights.do { |line, i|
			var outVal = line.sum({ |weight, j|
				weight * inValDict[inNames[j]] * inScaler;
			}) + outOffsets[i];
			outValDict.put(outNames[i], outVal);
		};
	}

	makePresets {

		shape = weights.shape;
		smallDim = shape.minItem;
		bigDim = shape.maxItem;

		presets = ();
		// diagonals
		presets.put(\diagL, weights.collect { |inner, j|
			inner.collect { |el, i|
				if ( (i % smallDim) == (j % smallDim) ) { 1 } { 0 };
			}
		});
		// reverse diag
		presets.put(\diagR, weights.collect { |inner, j|
			inner.collect { |el, i|
				if ( ((i % smallDim) + (j % smallDim)) == (smallDim - 1) ) { 1 } { 0 };
			}
		});

		// skewed diags TBD later, like these:
		// 3 to 5 skewed diagonal
		// [
		// 	[1, 0, 0],
		// 	[0.5, 0.5, 0],
		// 	[0, 1, 0],
		// 	[0, 0.5, 0.5],
		// 	[0, 0, 1]
		// ]
		//
		// [
		// 	[ 1, 0.5, 0, 0, 0 ],
		// 	[ 0, 0.5, 1, 0.5, 0 ],
		// 	[ 0, 0, 0, 0.5, 1 ]
		// ]
	}

	// prettyprint weights
	postw { |round = 0.001|
		var str = "// x.weights:\n[\n";
		weights.do { |line| str = str ++ Char.tab ++ line.round(round) ++ ",\n" };
		str = str ++ "]";
		^str
	}

		// prettyprint presets
	postp { |round = 0.001|
		var str = "// x.presets:\n(\n";
		presets.keysValuesDo { |key, pre|
			str = str ++ key ++ ":";
			pre.do { |line| str = str ++ Char.tab ++ line.round(round) ++ ",\n" };
			str = str ++ "],\n";
		};
		str = str ++ ");\n";
		^str
	}

	// make a plotter that can display and edit weights
	plot { |name, bounds, parent, makeSkip = true, options=#[]|
		^InfluxPlot(this, inNames.size, parent, bounds, makeSkip, options)
		.name_(name);
	}

	// create new random weights
	rand { |maxval = 1.0|
		weights = weights.collect { |row|
			row.collect { maxval.rand2.fold2(1.0) }
		}
	}

	blend { |other, blend = 0.5|
		// any array will be made to fit:
		if (other.shape != shape) { other = other.reshapeLike(weights); };
		weights = weights.collect { |row, j|
			row.collect { |val, i|
				blend(val, other[j][i], blend).fold2(1.0) }
		};
	}

	// modify existing ones:
	entangle { |drift = 1.0|
		weights = weights.collect { |row|
			row.collect { |val, i| (val + drift.rand2).fold2(1.0) }
		}
	}

	disentangle { |blend, presetName|
		var pres = presets[presetName] ? presets[\diagL];
		this.blend(pres, blend);
	}

	setw { | arrays |
		if (arrays.shape == weights.shape) {
			weights = arrays;
		} {
			warn("Influx - new weights have wrong shape: %.\n"
				.format(weights.shape))
		}
	}

	setwPre { |name|
		var pre = presets[name];
		if (pre.notNil) { this.setw(pre) };
	}

	offsetsFromProxy { |proxy|
		var setting = proxy.getKeysValues;
		var normVals = setting.collect { |pair|
			proxy.getSpec(pair[0]).unmap(pair[1]);
		};
		^outOffsets = normVals.unibi;
	}

	offsetsFromPreset { |preset, setName|
		var setting = preset.getSet(setName);
		var normVals = setting.value.collect { |pair|
			preset.proxy.getSpec(pair[0]).unmap(pair[1]);
		};
		^outOffsets = normVals.unibi;
	}

	attachMapped { |object, funcName, paramNames, specs|
		var mappedKeyValList;
		specs = specs ?? { object.getSpec; };
		funcName = funcName ?? { object.key };
		paramNames = paramNames
		?? { object.getHalo(\orderedNames); }
		?? { object.controlKeys; };

		action.addLast(funcName, {
			mappedKeyValList = paramNames.collect { |extParName, i|
				var inflOutName = outNames[i];
				var inflVal = outValDict[inflOutName];
				var mappedVal;
				if (inflVal.notNil) {
					mappedVal = specs[extParName].map(inflVal + 1 * 0.5);
					[extParName, mappedVal];
				} { [] }
			};
			object.set(*mappedKeyValList.flat);
		});
	}

}
