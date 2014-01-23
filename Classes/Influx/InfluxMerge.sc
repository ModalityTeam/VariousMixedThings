InfluxMerge {
	var <inValDict, <outValDict, <>mergeFunc;
	var <srcWeights;

	*new {
		^super.new.init;
	}
	init {
		inValDict = ();
		outValDict = ();
		srcWeights = ();

		// // default: sort of equal power pan
		// var outval = values.mean * weight / values.size.sqrt;
		// var outval = values.sum * weight; // alt - linear sum
		// var outval = values.mean * weight; // alt - mean value

		mergeFunc = { |in, out, srcWeights|
			in.keysValuesDo { |key, values|
				var outval = 0;
				values.keysValuesDo { |srcName, val|
					var contrib = val * (srcWeights[srcName]);
					outval = outval + contrib;
					// [srcName, srcWeights[srcName], contrib].postln;
				};
				outval = outval / values.size.sqrt;
				out.put(key, outval);
			}
		}
	}

	influence {|who ... keyValPairs|
		keyValPairs.pairsDo { |param, val|
			if (inValDict[param].isNil) { inValDict[param] = () };
			inValDict[param].put(who, val);
		};
		this.checkWeights(who);
		mergeFunc.value(inValDict, outValDict, srcWeights);
	}

	checkWeights { |who|
		if (srcWeights[who].isNil) { srcWeights[who] = 1 };
	}
}
