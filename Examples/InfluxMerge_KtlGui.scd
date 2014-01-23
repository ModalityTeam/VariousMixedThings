ToDo:
InfluxMerge
- add optional scalers for input from sources

general -
unibi, biuni - add proper help file


InfluxMerge can receive influences from an unlimited number
of named sources onto an unlimited number of named parameters.


a = InfluxMerge();
a.checkWeights(\srcA);
a.srcWeights;

a.influence(\srcA, \x, 0.5, \y, 0.1).outValDict;
a.influence(\srcB, \x, -0.3, \y, 0.2).outValDict;

// remove influence of srcB
a.influence(\srcB, \x, nil, \y, nil).outValDict;

Its inValDict collects all the current influences stored by name:


a.inValDict; // posts:
( 'y': ( 'srcA': 0.1, 'srcB': -0.2 ),
  'x': ( 'srcA': 0.5, 'srcB': 0.3 ) )

and it merges them by applying a mergeFunc to all the influences on one param.
by default, this is :
a.mergeFunc.postcs;
a.outValDict;

a.srcWeights; // 1 by default for every source
a.srcWeights.put(\srcA, 0.5);

{ |in, out, srcWeights|
			in.keysValuesDo { |key, values|
				var weight = srcWeights[key];
				var outval = values.sum * weight / values.size.sqrt;
				out.put(key, outval);
			}
		}

// change influences:
a.influence(\srcA, \x, -0.5, \y, 0.1).outValDict.collect(_.round(0.001));
a.influence(\srcB, \x, -0.3, \y, -0.2).outValDict.collect(_.round(0.001));

a.influence(\srcA, \x, 0, \y, 0).outValDict.collect(_.round(0.001));
a.influence(\srcB, \x, 0, \y, 0).outValDict.collect(_.round(0.001));

a.influence(\srcC, \z, 0.3, \ccc, -0.2).outValDict.collect(_.round(0.001));
a.inValDict;
a.outValDict;
a.srcWeights

(
w = Window("InfluxMerge - 3 top xy sliders influence big one", Rect(10, 10, 550, 400)).front;
w.addFlowLayout;
// EnvirGui(a.inValDict, 4, w);

b = [\srcA, \srcB, \srcC].collect { |srcName|
//	var ktl = KtlLoop(srcName).looped = true;

	var sl2d = Slider2D(w, 130@130).action_({ |sl, x, y|
		a.influence(*[srcName, \x, sl.x.unibi.round(0.001), \y, sl.y.unibi.round(0.001)].postln);
		a.outValDict.collect(_.round(0.001)).postln;
		c.setXY(a.outValDict[\x] + 1 * 0.5, a.outValDict[\y] + 1 * 0.5);
	}).setXY(0.5, 0.5);
	var slInf = EZSlider(w, 40@130, srcName, [0, 4, \amp].asSpec, { |sl|
		a.srcWeights.put(srcName, sl.value.postln); }, 1, layout: \vert);

	// sl.mouseDownAction = { ktl.startRec };
	// sl.mouseUpAction = { ktl.play };
};
c = Slider2D(w, 250@250).setXY(0.5, 0.5);
)

// InfluxKtlGui sketch tests

a = Influx([\x, \y], [\a, \b, \c, \d]);
g = InfluxKtlGui(a, 5);

a.set(\x, 0.5, \y, 0.2);

