
Influx explores the idea performing with running processes by means of influence on process parameters rather than direct control.
It maps input values to output values by allowing every input param to influence any output param by a given weight.
An initial matrix of such weights can be generated randomly and modified.

FAN IN/FAN OUT
One typical use would be with sound processes or patterns
with too many input parameters: Rather than mapping one control
directly to each param, one can entangle rather few control parameters
to influence a big bundle of process control inputs.
While this will not access to the full possibility space, it can reduce time to find interesting areas in the space (which one then might want to store)

DIS/ENTANGLE
One can also use the same param names for input and output,
starting with neutral weights like [0,0,1,0] where each param
only influences its same-named output. Then one can introduce
"entanglement" between params by adding small random deviations to the weights.

Displacement from a known location
One can use a known location in the param space (e.g. a stored good preset) as a starting point, and use Influx to play relative to the preset. Here, zooming allows very subtle explorations of shadings within a known good zone.

Playing with presets
one could morph between presets (as in ProxyPreset) and play Influx relative to the interpolated setting...

ToDo / Ideas
* shift param centers, and scale influence ranges to zoom in on interesting areas
* add warp tables or envelopes as alternatives to linear scaling (both I/O)
* add LFO-style automation? how best?

// EXAMPLES
// make an influx - fan-in, fan-out
a = Influx([\x, \y], [\a, \b, \c,\d, \e, \f, \g, \h], (x: 0.5, y: 0.1));
p = InfluxPlot(a, 12);

// tests - check internal state
a.dump;
a.inNames;
a.outNames;

// the internal values, weights, presets:
a.inValDict;
a.outValDict;
a.weights;  // randomly initialized
a.presets;  // useful weights presets

// pretty print them
a.postw;    // weights
a.postv;    // in/out vals
a.postp;    // presets

// make a plot/edit window for weights:
p = a.plot();  // plotter writes back into weights!


p.inNameView.string_("\n\nx\n\n\n\n\ny");
p.outNameView.string_(a.outNames.join("    "));

// randomize to  maximum bipolar value
a.rand(0.5).postw;
p.plotter.value_(a.weights.flop);
// blend toward a given array of values
a.blend([1,0.1, 0.1, 0.5]).postw;
a.blend([1,0.1, 0.1], 0.9).postw;

a.set(\x, 1.0.rand2, \y, 1.0.rand2).postw; // set to something

a.blend(a.presets[\diagL], 0).postw;
a.entangle(0.1).postw;

a.blend(a.presets.diagL, 1).postw;
a.entangle(0.1).postw;
a.disentangle(0.5, \diagR).postw;


///// interactive gui example
(
w = Window("Influx control", Rect(0, 800, 300, 200)).front;
w.layout = VLayout(
	HLayout(
		VLayout(
			StaticText(w, 190@20).string_("Change Influx weights:"),
			Button(w, 190@20).states_([["set to diagonal"]])
			.action_({ a.setwPre(\diagL); p.value_(a.weights.flop); }),
			Button(w, 190@20).states_([["disentangle"]])
			.action_({ a.disentangle(0.3); p.value_(a.weights.flop); }),
			Button(w, 190@20).states_([["entangle"]])
			.action_({ a.entangle(0.3); p.value_(a.weights.flop); }),
			Button(w, 190@20).states_([["RANDOM"]])
			.action_({  a.rand(1.0); p.value_(a.weights.flop); })
		),
		VLayout(
			StaticText(w, 190@20).string_("Change Influx inputs:"),
			Slider2D(w, 190@190)
			.action_({|sl| a.set(\x, sl.x * 2 - 1, \y, sl.y * 2 - 1); })
		)
	)
);

w = Window("Influx2 state", Rect(0, 300, 400, 300)).front;
w.addFlowLayout;
StaticText(w, 390@20).string_("Influx values:");

[a.inValDict, a.outValDict].collect { |dict, i|
	var eg = EnvirGui(nil, [4, 8][i], w, 380@100, options: [\name]);
	[a.eg.
	eg.name_(["ins", "outs"][i]);
	eg.object_(dict);
	eg.skipjack.dt = 0.1;
}
)

//// more tests by code
a.set(\x, 1.0, \y,0);
a.set(\x, 1.0.rand2, \y, 1.0.rand2);

// add an action to perform when outvals change
a.action.add(\test, { |influx| "post outValDict:".postln; influx.outValDict.collect(_.round(0.01)).postln });
)

// try playing a boring sound with too many parameters
(
Spec.add(\intv, [-24, 24]);
Spec.add(\timescale, [0.01, 10, \exp]);
Spec.add(\curve, [-10, 10]);
Spec.add(\ffreq, \freq);
Spec.add(\loopnode, [0, 12]);

Spec.add(\dens, [10, 1000]);
Spec.add(\resfreq, \freq);
Spec.add(\ringtime, [0.01, 1, \exp]);


Ndef(\a, { |freq=250, intv=19, timescale=1, curve=0, loopnode=0, dens = 10, resfreq = 500, ringtime = 0.01|
	var n = 15;
	var env = EnvGen.kr(
		Env({ 1.0.rand2 }!(n+1), {1.0.rand}!n, curve, releaseNode: n-1, loopNode: loopnode.min(n-3)),
		timeScale: timescale);
	var pitch = (env * [1, 0.33, -1] * intv).midiratio * freq;
	var form = Formant.ar(pitch, pitch.rotate(1) * 1.6, pitch.rotate(2) * 2.6);

	var dusty = Ringz.ar(Dust.ar(dens!3), resfreq * LFNoise0.kr([3, 5, 7], 0.5, 1), ringtime) * ((dens ** -0.5) * (ringtime ** -0.3)) ;
	Splay.ar(form * dusty);

});
Ndef(\a).gui.moveTo(0, 50);
);


s.boot;
// connect the influx action to the Ndef!

Ndef(\a).getKeysValues.flop[0]

a.attach(Ndef(\a), \setNdef, Ndef(\a).getKeysValues.flop[0]);

(
z = (); y = Ndef(\a).getKeysValues.flop.first;
a.outNames.do { |outName, i| z.put(outName, y[i]); };

a.action.add(\test, { |influx|
	var nameValPairs = influx.outNames.collect { |outName|
		var paramName = z[outName];
		[paramName, paramName.asSpec.map(influx.outValDict[outName] + 1 * 0.5)];
	}.flat;

	Ndef(\a).set(*nameValPairs);
});
)

// and try using it to create presets,
// try try morphing between those.
// requires the JITLibExtensions Quark.
(
n = NdefPreset(Ndef(\a));

w = Window("NdefPreset test", Rect(200, 200, 420, 250)).front;
w.addFlowLayout;
// a gui for the NdefPreset
g = ProxyPresetGui(n, parent: w);
// and one for the Ndef
b = NdefGui(Ndef(\a), 8, parent: w);
)


// sketch for multislider interface to make:
(
n=8;


w = Window("MultiSlider Options", Rect(200, Window.screenBounds.height-550, 300, 400));
w.view.decorator = FlowLayout( w.view.bounds, 10@10, 10@2 );
w.front;

2.collect { |i|
	StaticText(w, 100@30).string_("in_" ++ i);
};
2.collect { |i|
	m = MultiSliderView(w, Rect(0,0,100,180)).indexThumbSize_(20);
	m.indexIsHorizontal_(false);
	m.value=Array.fill(n, {|v| 0.5+((0.3*v).sin*0.25)});
	m.action = { arg q;q.value.postln; };
	m.isFilled = true;
	m.reference = 0.5!n;
};

c = CompositeView(w, 40@180); c.addFlowLayout;
n.do { |i| StaticText(c, 40@20).string_("out_" ++ i); };
)