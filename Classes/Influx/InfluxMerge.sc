InfluxMerge {
	var <inValDict, <outValDict, <>mergeFunc;

	*new {
		^super.new.init;
	}
	init {
		inValDict = ();
		outValDict = ();
		mergeFunc = { |in, out|
			in.keysValuesDo { |key, values|
				///// sort of reverse equal power pan
				var outval = values.sum / values.size.sqrt;
				out.put(key, outval);
			}
		}
	}

	influence {|who ... keyValPairs|
		keyValPairs.pairsDo { |param, val|
			if (inValDict[param].isNil) { inValDict[param] = () };
			inValDict[param].put(who, val);
		};
		mergeFunc.value(inValDict, outValDict);
	}
}