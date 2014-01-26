InfluxSpread {

	classvar <>sendFunc;

	var <>names, <inValDict, <destsDict;
	var <action;

	*initClass {
		sendFunc = { |destDict, infSpr|
			// have names to send for each destination?
			var sendVals = infSpr.inValDict;
			var objSpecs = destDict[\specs];
			var scaler = destDict[\scaler];
			var offsets = destDict[\offsets];

			if (scaler != 1) {
				sendVals = sendVals.collect(_ * scaler);
			};

			if (offsets.notNil) {
				// check if complete?
				sendVals = sendVals.collect({|val, key| val + offsets[key] });
			};

			// if (objSpecs.notNil) {
			// 	// check if complete?
			// 	sendVals = sendVals.collect { |value, key|
			// 		objSpecs[key].map(value.biuni);
			// 	};
			// };
			destDict[\object].set(*sendVals.asKeyValuePairs.postln);
		};
	}

	*new { |names|
		^super.newCopyArgs(names).init;
	}

	set { |... keyValPairs|
		keyValPairs.pairsDo { |key, val|
			inValDict.put(key, val);
		};
		action.value(this);
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
		names = names ?? { List[] };
		inValDict = ();
		destsDict = ();
		action = FuncChain();
	}

	add { |name, object, specs, func, offsets, scaler|
		var destDict = destsDict[name];
		destDict ?? { destsDict[name] = destDict = () };

		destDict.put(\name, name);
		destDict.put(\object, object);
		destDict.put(\specs, specs);
		destDict.put(\scaler, scaler ? 1);

		action.add(name, func ? { |infspr| sendFunc.value(destDict, infspr) });
	}

	send { action.value(this) }

}
