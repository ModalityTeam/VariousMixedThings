/*
a = Influx([\x, \y], [\a, \b, \c,\d, \e, \f, \g, \h], (x: 0.5, y: 0.1));
b = Influx([\x, \y, \z], [\a, \b, \c,\d, \e, \f, \g, \h]);
z = InfluxWGui(a, [2, 8]);
p = InfluxPlot(a);
y = InfluxWGui.new;
z.object = a;
z.object = b;

y = InfluxIOWGui.new;
y.object = a;
y.object = b;

To DO:

* should multisliders write back into weights
* if so, toggle edit mode on/off?
* invals gui is active,
* disable outvals gui?
* off by one size error in paramgui2

*/


InfluxWGui : JITGui {
	var <inNameView, <outNameView, <multiZone, <multiSliders;
	var <inNames, <outNames;

	*new { |obj, numItems = #[4, 8], parent, bounds, makeSkip = true, options|
		^super.new(obj, numItems, parent, bounds, makeSkip, options);
	}

	setDefaults { |options|
		if (parent.isNil) { defPos = 10@260 } { defPos = skin.margin; };
		minSize = 254 @ 224;
	}

	makeViews {
		var numIns = numItems[0];
		var numOuts = numItems[1];

		var font = Font("Monaco", 10);
		// Pen.font_(Font("", 12));

		zone.resize_(5);

		nameView = StaticText(zone, Rect(0,0, 50, 20))
		.font_(font).string_("weights");

		inNameView = UserView(zone, Rect(0,0, 200, 20))
		.font_(font).resize_(2); // elastic horiz
		inNameView.drawFunc = { |u|
			inNames.do { |name, i|
				var wid = u.bounds.width / inNames.size;
				Pen.stringCenteredIn(name.asString,
					Rect(wid*i, 0, wid, 20), font);
			};
		};

		outNameView = UserView(zone, Rect(0,0, 46, 200))
		.font_(font).resize_(4); // elastic vert;
		outNameView.drawFunc = { |u|
			var hi = u.bounds.height / outNames.size;
			outNames.do { |name, i|
				Pen.stringCenteredIn(name.asString,
					Rect(0, hi * i, 50, hi), font);
			};
		};

		// [nameView, inNameView, outNameView]
		// .do (_.background_(Color.grey(0.8)));

		multiZone = CompositeView(zone,
			Rect(0, 0, zone.bounds.width - 54,
				zone.bounds.height - 24)
		);
		multiZone.addFlowLayout(0@0, 0@0);
		multiZone.resize_(5);

		multiZone.onResize_{ |zn|
			var numSl = multiSliders.size;
			var wid = zn.bounds.width;
			var hi = zn.bounds.height;
			var mswid = wid / numSl;

			multiSliders.do { |ms, i|
				ms.bounds_(Rect( mswid * i, 0, mswid, hi));
				ms.indexThumbSize = (hi / ms.value.size).trunc - 1;
			}
		};

		this.makeMultis(numIns, numOuts);
	}

	makeMultis {|numIns, numOuts|

		var multiW = multiZone.bounds.width / numIns - 1;
		var multiH = multiZone.bounds.height;
		var gap = 1;
		var indexSize = (multiH / numOuts).asInt - 1;

		multiSliders.do(_.remove);
		multiZone.decorator.reset;

		multiSliders = numIns.collect {
			var m = MultiSliderView(multiZone, Rect(0,0, multiW, multiH));
			m.indexThumbSize_(indexSize);
			m.indexIsHorizontal_(false);
			m.action = { arg q; q.value.round(0.001).postln; };
			m.isFilled = true;
			m.reference = 0.5 ! numOuts;
			m.resize_(4);
		};
	}

	accepts { |obj| ^obj.isNil or: { obj.isKindOf(Influx) }	}


	getState {
		var newState = (object: object);
		if (object.notNil) {
			newState.put(\weights, object.weights)
			.put(\inNames, object.inNames)
			.put(\outNames, object.outNames)
		};
		^newState
	}

	inNames_ { |newNames|
		inNames = newNames;
		inNameView.refresh;
	}

	outNames_ { |newNames|
		outNames = newNames;
		outNameView.refresh;
	}

	weights_ { |weights|
		weights = weights.flop;
		if (weights.size != multiSliders.size) {
			this.makeMultis(*weights.shape);
		};
		multiSliders.do { |ms, i|
			ms.value = weights[i] + 1 * 0.5;
		};
	}

	checkUpdate {
		var newState = this.getState;
		if (object.notNil) {
			if (newState[\inNames] != prevState[\inNames]) {
				this.inNames_(newState[\inNames]); };
			if (newState[\outNames] != prevState[\outNames]) {
				this.outNames_(newState[\outNames]); };
			if (newState[\weights] != prevState[\weights]) {
				this.weights_(newState[\weights]); };
		};
		prevState = newState;
	}
}

InfluxIOWGui : JITGui {
	var <inValsGui, <outValsGui, <wGui;

	accepts { |obj| ^obj.isNil or: obj.isKindOf(Influx) }

	*new { |obj, numItems = #[4, 8], parent, bounds, makeSkip = true, options=#[\plot]|
		^super.new(obj, numItems, parent, bounds, makeSkip, options);
	}

	checkUpdate {
		if (object != prevState[\object]) {
			if (object.notNil) {
				object.inNames.do(inValsGui.specs.put(_, \pan));
				inValsGui.object_(object.inValDict);
				this.addInvalActions;

				wGui.object_(object);

				object.outNames.do(outValsGui.specs.put(_, \pan));
				outValsGui.object_(object.outValDict);
			};
		};
	}

	addInvalActions {
		inValsGui.widgets.do { |widge|
			if (widge.isKindOf(EZSlider)) {
				widge.action = widge.action.addFunc({ |widge|
					object.calcOutVals;
				});
			}
		}
	}

	setDefaults { |options|
		if (parent.isNil) {
			defPos = 10@260
		} {
			defPos = skin.margin;
		};
		minSize = 270 @ (numItems.sum + 2 * skin.buttonHeight
		+ skin.headHeight + 224);
		//	"minSize: %\n".postf(minSize);
	}


	makeViews { |options|
		inValsGui = ParamGui(nil, numItems[0], zone,
			bounds: 260@100,
			options: [\name]).name_(\inVals);

		wGui = InfluxWGui(nil, numItems, zone, 260 @ 200);
		outValsGui = ParamGui(nil, numItems[1] + 1, zone,
			bounds: 260@100,
			options: [\name]).name_(\outVals);
	}
}
