Influx {
	var <inNames, <outNames, <inValDict, <specs;
	var <weights, <outValDict, <action;
	var <shape, <smallDim, <bigDim, <presets;

	*new { |ins, outs, vals, specs|
		^super.newCopyArgs(ins, outs, vals, specs).init;
	}
	init {
		inValDict = inValDict ?? { () };
		specs = specs ?? {()};
		[inNames, outNames].flat.do { |name|
			if (specs[name].isNil) {
				specs.put(name, \pan.asSpec)
			};
		};

		outValDict = ();
		weights = { 0 ! inNames.size } ! outNames.size;
		this.rand;

		shape = weights.shape;
		smallDim = shape.minItem;
		bigDim = shape.maxItem;

		this.makePresets;

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
	// create new random weights
	rand { |maxval = 1.0|
		weights = weights.collect { |row|
			row.collect { maxval.rand2.fold2(1.0) }
		}
	}
	// modify existing ones
	entangle { |drift = 1.0|
		weights = weights.collect { |row|
			row.collect { |val, i| (val + drift.rand2).fold2(1.0) }
		}
	}

	// modify existing ones
	disentangle { |blend, presetName|
		var pres = presets[presetName] ? presets[\diagL];
		this.blend(pres, blend);
	}

	// modify existing ones
	blend { |other, blend = 0.5|
		if (other.shape != shape) { other = other.reshapeLike(weights); };
		weights = weights.collect { |row, j|
			row.collect { |val, i|
				blend(val, other[j][i]).fold2(1.0) }
		};
	}

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
				weight * inValDict[inNames[j]]
			});
			outValDict.put(outNames[i], outVal);
		};
	}
}