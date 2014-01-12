// EXAMPLES
s.boot;

// make an Influx with KtlLoop for gesture recording.
// This requires version 3.6.6 or newer,
// and current JITLibExtensions and KeyPlayer quarks must be installed.

(

a = Influx([\x, \y], [\a, \b, \c,\d, \e, \f, \g, \h], (x: 0.5, y: 0.1));
b = InfluxIOWGui(a).moveTo(300, Window.screenBounds.height - 600);

// Make a KtlLoop. Its function is made by hand here for full detail
(
k = KtlLoop(\x, { |ev|
	if (ev.type == \set) {
			// filter just the params we are interested in
		ev = ev.select{ |val, key| k.paramsToRecord.includes(key) };
			// rescale if rescale is on
		ev = k.rescaleEvent(ev);
			// map to the objects out specs
		ev = ev.collect{ |val, key| k.object.getSpec(key).map(val) };
			// debug info?
	//	ev.round(0.001).postln;
			// then set the object to the new values!
		k.object.set(*ev.asKeyValuePairs);
	};
});
// which params to record (might be more coming from the source)
k.paramsToRecord_([\x, \y]);
// which in params go to which out params
k.ktlMap_((x: \x, y: y));
// which object gets set to the out param values
// here, the Influx a
k.object = a;
// make a gui for KtlLoop
KtlLoopGui(k, bounds: 250@180).moveTo(10, 320);

// add outval specs to Influx, so it knows how to rescale
a.addSpec(\x, \pan);
a.addSpec(\y, \pan);
);


// make an xy controller for Influx
// its action records into KtlLoop
// and sets its output object
(
w = Window("Influx control", Rect(10, 550, 300, 200)).front;
w.layout = VLayout(
    HLayout(
        VLayout(
            StaticText(w, 190@20).string_("Change  weights:"),
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
            StaticText(w, 190@20).string_("Change inputs:"),
			Slider2D(w, 190@190).x_(0.5).y_(0.5)
            .action_({|sl|
				// recording into KtlLoop here
				k.recordEvent((type: \set, x: sl.x, y: sl.y));
				// and this is the normal set function
				// bipolar mapping here done by hand
				a.set(\x, sl.x * 2 - 1, \y, sl.y * 2 - 1);
			})
        )
    )
);
);

// make a so-so sound with too many parameters
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

});
Ndef(\a).play(vol: 0.25);
NdefPreset(Ndef(\a));
NdefPresetGui(NdefPreset(\a), 9);

// connect the influx outputs to the Ndefs params
a.attachMapped(Ndef(\a), \setNdef, Ndef(\a).controlKeys);
);
)

/* ***** NOW Try this *******

play on the XY controller, hear how the sound changes;
change the weights, note how the sounds in the corners change

hit record on the KtlLoopGui,
record controller moves
stop record, hit play, hit loop

play with loop params:
  tempo,

change weights while loop plays:
   gesture stays the same, sound variety changes

****** NOW Try this ******  */