InfluxMerge can receive influences from an unlimited number
of named sources onto an unlimited number of named parameters.

a = InfluxMerge();

a.influence(\srcA, \x, 0.5, \y, 0.1).outValDict;
a.influence(\srcB, \x, 0.3, \y, -0.2).outValDict;

Its inValDict collects all the current influences stored by name:

a.inValDict; // posts:
( 'y': ( 'srcA': 0.1, 'srcB': -0.2 ),
  'x': ( 'srcA': 0.5, 'srcB': 0.3 ) )

and it merges them by applying a mergeFunc to all the influences on one param.
by default, this is :
a.mergeFunc.postcs;

{ |in, out|
	in.keysValuesDo { |key, values|
		///// sort of reverse equal power pan
		var outval = values.sum / values.size.sqrt;
		// var outval = values.mean; // mean value
		// var outval = values.sum; // linear sum
		out.put(key, outval);
	}
};

// change influences:
a.influence(\srcA, \x, -0.5, \y, 0.1).outValDict.collect(_.round(0.001));
a.influence(\srcB, \x, -0.3, \y, -0.2).outValDict.collect(_.round(0.001));

a.influence(\srcC, \z, 0.3, \ccc, -0.2).outValDict.collect(_.round(0.001));
a.inValDict


(
w = Window("InfluxMerge - 3 top xy sliders influence big one").front;
w.addFlowLayout;

b = [\srcA, \srcB, \srcC].collect { |srcName|
//	var ktl = KtlLoop(srcName).looped = true;
	var sl = Slider2D(w, 90@90).action_({ |sl, x, y|
		a.influence(srcName, \x, sl.x * 2 - 1, \y, sl.y * 2 - 1);
		a.outValDict.collect(_.round(0.001)).postln;
		c.setXY(a.outValDict[\x] + 1 * 0.5, a.outValDict[\y] + 1 * 0.5);
	}).setXY(0.5, 0.5);
	// sl.mouseDownAction = { ktl.startRec };
	// sl.mouseUpAction = { ktl.play };
};
c = Slider2D(w, 280@280).setXY(0.5, 0.5);
)

// InfluxKtlGui sketch tests

a = Influx([\x, \y], [\a, \b, \c, \d]);
g = InfluxKtlGui(a, 5);

a.set(\x, 0.5, \y, 0.2);

