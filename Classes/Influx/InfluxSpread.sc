InfluxSpread : InfluxBase {

	classvar <>sendFuncSet;

	var <destsDict;

	*initClass {
		sendFuncSet = { |destDict, influx|
			var sendVals = influx.remapFor(destDict);
			destDict[\object].set(*sendVals.asKeyValuePairs.postln);
		};
	}

	setScaler { |name, val|
		destsDict[name].put(\scaler, val);
		action.value(this);
	}

	setOffsets { |name, values|
		destsDict[name].put(\offsets, values);
		action.value(this);
	}

	init {
		super.init;
		destsDict = ();
	}

	remapFor { |destDict|
		// have names to send for each destination?
		var newDict;
		var sendVals = inValDict;
		var object = destDict[\object];
		var paramMap = destDict[\paramMap];
		var objSpecs = destDict[\specs];
		var scaler = destDict[\scaler];
		var offsets = destDict[\offsets];

		if (paramMap.notNil) {
			newDict = ();
			sendVals.keysValuesDo { |key, val|
				//	[key, paramMap[key], val].postcs;
				if (paramMap[key].notNil) {
					newDict.put (paramMap[key], val);
				};
			};
			sendVals = newDict;
		};

		if (scaler != 1) {
			sendVals = sendVals.collect(_ * scaler);
		};

		if (offsets.notNil) {
			// check if complete?
			sendVals = sendVals.collect({|val, key| val + (offsets[key] ? 0) });
		};
		if (objSpecs.notNil) {
			// check if complete?
			sendVals = sendVals.collect { |value, key|
				objSpecs[key].map(value.biuni);
			};
		};

		^sendVals;
	}

	addDest { |name, object, specs, paramMap, sendFunc, offsets, scaler|
		var destDict = destsDict[name];
		destDict ?? { destsDict[name] = destDict = () };

		destDict.put(\name, name);
		destDict.put(\object, object);
		destDict.put(\specs, specs ?? { object.getSpec });
		destDict.put(\paramMap, paramMap);
		destDict.put(\scaler, scaler ? 1);

		sendFunc = sendFunc ? sendFuncSet;

		action.add(name, { |infspr| sendFunc.value(destDict, infspr) });
	}

}
