InfluxMix : InfluxBase {

	classvar <defaultMergeFunc;

	var <trusts, <>mergeFunc, <>damping = 0.5;

	*initClass {
		Class.initClassTree(Halo);
		Class.initClassTree(Spec);
		this.addSpec(\damping, [0, 1]);

		// damping 0 is linear sum of contribs,
		// damping 0.5 is scaled by sqrt of contribs (equal power sum)
		// damping 1 is linear average

		defaultMergeFunc = { |in, out, trusts, damping = 0.5|
			in.keysValuesDo { |key, values|
				var outval = 0;
				values.keysValuesDo { |srcName, val|
					var contrib = val * (trusts[srcName]);
					outval = outval + contrib;
				};
				outval = outval / (values.size ** damping);
				out.put(key, outval);
			}
		}
	}

	*new { |inNames|
		^super.newCopyArgs(inNames).init;
	}

	init {
		super.init;
		inNames = inNames ?? { List[] };
		trusts = ();
		mergeFunc = mergeFunc ? defaultMergeFunc
	}

	set { warn("you cannot set an InfluxMix, please use influence!"); }

	influence {|who ... keyValPairs|
		keyValPairs.pairsDo { |param, val|
			if (inValDict[param].isNil) { inValDict[param] = () };
			inValDict[param].put(who, val);
		};
		this.checkTrusts(who);
		mergeFunc.value(inValDict, outValDict, trusts, damping);
		action.value(this);
	}

	checkTrusts { |who|
		if (inNames.includes(who.not)) { inNames.add(who) };
		if (trusts[who].isNil) { trusts[who] = 1 };
	}

	trust { |srcName, val|
		trusts.put(srcName, val);
		mergeFunc.value(inValDict, outValDict, trusts, damping);
		action.value(this);
	}
}
