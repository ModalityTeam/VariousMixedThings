
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


// randomize to  maximum bipolar value
a.rand(0.5).postw;
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
			.action_({ a.setwPre(\diagL); }),
			Button(w, 190@20).states_([["disentangle"]])
			.action_({ a.disentangle(0.3); }),
			Button(w, 190@20).states_([["entangle"]])
			.action_({ a.entangle(0.3); }),
			Button(w, 190@20).states_([["RANDOM"]])
			.action_({  a.rand(1.0); })
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
	var eg = ParamGui(nil, [4, 8][i], w, 380@100, options: [\name]);
	eg.name_(["ins", "outs"][i]);
	dict.keysDo { |key| eg.specs.put(key, \bipolar) };
	eg.object_(dict);
	eg.skipjack.dt = 0.1;
}
);

// play relative to a setting:
// create offsets on the outs, so when ins are [0, 0],
// the outs are on the desired preset
a.outOffsets;
a.outOffsets = [0.4, 0.3, 0.2, 0.1, 0, -0.1, -0.2, -0.3];
a.outOffsets = [0.4, 0.3, 0.2, 0.1, 0, -0.1, -0.2, -0.3].scramble;
a.outOffsets = 0 ! a.outNames.size;

// now scale the ins to have high resolution near the preset
a.inScaler;   // default is 1
a.inScaler = 0.5;  // control smaller ranges
a.inScaler = 0.25; // and even smaller
a.inScaler = 1;   // default is 1


//// more tests by code
a.set(\x, 1.0, \y,0);
a.set(\x, 1.0.rand2, \y, 1.0.rand2);

// add an action to perform when outvals change
a.action.add(\test, { |influx| "post outValDict:".postln; influx.postv; });
)

// try playing a boring sound with too many parameters
// .addSpec requires Halo Class from JITLibExtensions quark
(

Ndef(\a).addSpec(\intv, [-24, 24]);
Ndef(\a).addSpec(\timescale, [0.01, 10, \exp]);
Ndef(\a).addSpec(\curve, [-10, 10]);
Ndef(\a).addSpec(\ffreq, \freq);
Ndef(\a).addSpec(\loopnode, [0, 12]);

Ndef(\a).addSpec(\dens, [10, 1000]);
Ndef(\a).addSpec(\resfreq, \freq);
Ndef(\a).addSpec(\ringtime, [0.01, 1, \exp]);

Ndef(\a, { |freq=250, intv=19, timescale=1, curve=0, loopnode=0, dens = 10, resfreq = 500, ringtime = 0.01|
	var n = 15;
	var env = EnvGen.kr(
		Env({ 1.0.rand2 }!(n+1), {1.0.rand}!n, curve, releaseNode: n-1, loopNode: loopnode.min(n-3)),
		timeScale: timescale);
	var pitch = (env * [1, 0.33, -1] * intv).midiratio * freq;
	var form = Formant.ar(pitch, pitch.rotate(1) * 1.6, pitch.rotate(2) * 2.6);

	var dusty = Ringz.ar(Dust.ar(dens!3), resfreq * LFNoise0.kr([3, 5, 7], 0.5, 1), ringtime) * ((dens ** -0.5) * (ringtime ** -0.3)) ;
	Splay.ar(form * dusty);

}).play(vol: 0.25);
Ndef(\a).gui.moveTo(0, 50);
);


// connect the influx action to the Ndef!


a.attachMapped(Ndef(\a), \setNdef, Ndef(\a).controlKeys);
a.action.removeAt(\setNdef);


// beautiful whistler setting
Ndef('a').set('dens', 535.47013904679, 'timescale', 0.33089288650281, 'loopnode', 6.2294464509812, 'curve', 4.7492848502265, 'freq', 12299.627634864, 'intv', -16.184204762419, 'resfreq', 2581.3301143271, 'ringtime', 0.23828407286013);

// explore playing around this setting:
a.offsetsFromProxy(Ndef(\a));
a.inScaler = 0.2;



// Next, try playing this in flow with the NdefPreset class!
// try finding interesting, contrasting settings,
// storing them, morphing between them;
// then try explore the areas around a preset by zooming in
// and creating offsets...
// requires the JITLibExtensions Quark.
(
n = NdefPreset(Ndef(\a));
g = NdefPresetGui(NdefPreset(\a), 12);
)

n.setRand(0.5);
n.addSet(\noisy);
n.setRand(0.5);
n.addSet(\crickety);


)

NdefPreset(\a).addSettings([
('noisy' -> [ [ 'freq', 9355.4819733367 ], [ 'intv', -11.951156593491 ], [ 'timescale', 0.28526310997542 ], [ 'curve', 3.9382026607088 ], [ 'loopnode', 3.0521517121423 ], [ 'dens', 534.25139028163 ], [ 'resfreq', 859.1602011514 ], [ 'ringtime', 0.013490217580931 ] ]),
('crickety' -> [ [ 'freq', 32.365903728774 ], [ 'intv', -5.1874753557533 ], [ 'timescale', 0.095415084353336 ], [ 'curve', 0.21559811161763 ], [ 'loopnode', 4.7123312805262 ], [ 'dens', 636.70062791051 ], [ 'resfreq', 6358.1470734915 ], [ 'ringtime', 0.96413387980452 ] ]),

('dense' -> [ [ 'freq', 690.50569396532 ], [ 'intv', -21.576085480345 ], [ 'timescale', 0.32701451121064 ], [ 'curve', 9.6469725145439 ], [ 'loopnode', 1.2012773830142 ], [ 'dens', 529.79083160502 ], [ 'resfreq', 1646.7137473561 ], [ 'ringtime', 0.053969422022618 ] ]),
('otto' -> [ [ 'freq', 454.81762612114 ], [ 'intv', -2.6666666666667 ], [ 'timescale', 0.80554873320093 ], [ 'curve', 0.76884115174515 ], [ 'loopnode', 8.2 ], [ 'dens', 510.5 ], [ 'resfreq', 305.0445913078 ], [ 'ringtime', 0.042986623470823 ] ]),
('whistle2' -> [ [ 'freq', 32.567484706562 ], [ 'intv', -20.467912506423 ], [ 'timescale', 0.067258016618603 ], [ 'curve', -0.94382619266085 ], [ 'loopnode', 6.8953538338859 ], [ 'dens', 518.31347372938 ], [ 'resfreq', 1582.3552276563 ], [ 'ringtime', 0.063898804025525 ] ]),
('whistle3' -> [ [ 'freq', 194.16632409718 ], [ 'intv', 6.3677901435535 ], [ 'timescale', 0.19556123840567 ], [ 'curve', -9.3105904384148 ], [ 'loopnode', 6.6194474484789 ], [ 'dens', 631.79063326559 ], [ 'resfreq', 9158.272803669 ], [ 'ringtime', 0.020222154356683 ] ])
]);

a.offsetsFromPreset(n, \crickety);

a.inScaler = 0.5;

n.getSet(\whistle2);
NdefPreset(\a).settings.printcsAll;

n.getSetNames.choose

a.offsetsFromPreset(n, \whistle2);

// how about softset or morph to new settings?
softSet ??

// sketch for a possible multislider interface :
(
n=8;


w = Window("Influx with MultiSlider", Rect(200, Window.screenBounds.height-550, 300, 400));
w.view.decorator = FlowLayout( w.view.bounds, 10@10, 10@2 );
w.front;

2.collect { |i|
	StaticText(w, 100@30).string_("in_" ++ i);
};
2.collect { |i|
	m = MultiSliderView(w, Rect(0,0,100,180)).indexThumbSize_(20);
	m.indexIsHorizontal_(false);
	m.value=Array.fill(n, {|v| 0.5+((0.3*v).sin*0.25)});
	m.action = { arg q; q.value.round(0.001).postln; };
	m.isFilled = true;
	m.reference = 0.5!n;
};

c = CompositeView(w, 40@180); c.addFlowLayout;
n.do { |i| StaticText(c, 40@20).string_("out_" ++ i); };
)