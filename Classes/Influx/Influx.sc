// todo:
// * update plotter when weights change
// * method for skewed diagonals
//

Influx {
	var <inNames, <outNames, <inValDict;
	var <weights, <outValDict, <action;
	var <shape, <smallDim, <bigDim, <presets;

	var <>outOffsets, <>inScaler = 1;

	*new { |ins, outs, vals, weights|
		^super.newCopyArgs(ins, outs, vals, weights).init;
	}

	storeArgs { ^[inNames, outNames, inValDict, weights] }

	printOn { |receiver, stream|
		^this.storeOn(receiver, stream);
	}

	init {
		if (inValDict.isNil) {
			inValDict = ();
			inNames.do (inValDict.put(_, 0));
		};

		outValDict = ();
		outNames.do (outValDict.put(_, 0));
		weights = weights ?? { { 0 ! inNames.size } ! outNames.size };
		shape = weights.shape;
		smallDim = shape.minItem;
		bigDim = shape.maxItem;

		outOffsets = 0 ! outNames.size;

		this.makePresets;
		this.rand;
		this.calcOutVals;

		action = FuncChain.new;
	}

	makePresets {
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

	// prettyprint values
	postv { |round = 0.001|
		var str = "";
		[   ["inVals", inNames, inValDict],
			["outVals", outNames, outValDict]
		].do { |trip|
			var valName, names, vals; #valName, names, vals = trip;
			str = str ++ "\n// x.%: \n(\n".format(valName);
			names.do { |name|
				str = str ++
				"\t%: + %,\n".format(name, vals[name].round(round))
			};
			str = str ++ ");\n";
		}
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

	// set input params
	set { |...keyValPairs|
		keyValPairs.pairsDo { |key, val|
			inValDict.put(key, val);
		};
		this.calcOutVals;
		action.value(this);
	}

	calcOutVals {
		weights.do { |line, i|
			var outVal = line.sum({ |weight, j|
				weight * inValDict[inNames[j]] * inScaler;
			}) + outOffsets[i];
			outValDict.put(outNames[i], outVal);
		};
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

	attachDirect { |object, funcName|
		action.addLast(funcName, {
			object.set(*this.outValDict.asKeyValuePairs)
		});
	}

	offsetsFromProxy { |proxy|
		var setting = proxy.getKeysValues;
		var normVals = setting.collect { |pair|
			proxy.getSpec(pair[0]).unmap(pair[1]);
		};
		^outOffsets = normVals * 2 - 1;
	}

	offsetsFromPreset { |preset, setName|
		var setting = preset.getSet(setName);
		var normVals = setting.value.collect { |pair|
			preset.proxy.getSpec(pair[0]).unmap(pair[1]);
		};
		^outOffsets = normVals * 2 - 1;
	}

	attachMapped { |object, funcName, paramNames, specs|
		var mappedKeyValList;
		// paramNames = paramNames ?? { object.getHalo(\orderedNames); };
		specs = specs ?? { object.getSpec; };

		action.addLast(funcName, {
			mappedKeyValList = paramNames.collect { |extParName, i|
				var inflOutName = outNames[i];
				var inflVal = outValDict[inflOutName];
				var mappedVal = specs[extParName].map(inflVal + 1 * 0.5);
				[extParName, mappedVal];
			};
			object.set(*mappedKeyValList.flat);
		});
	}
}
