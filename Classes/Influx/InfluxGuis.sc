InfluxPlot : JITGui {
	var <inNameView, <outNameView, <plotter;
	var <inNames, <outNames;

	*new { |obj, numItems = #[4, 8], parent, bounds, makeSkip = true, options|
		^super.new(obj, numItems, parent, bounds, makeSkip, options);
	}

	setDefaults { |options|
		if (parent.isNil) { defPos = 10@260 } { defPos = skin.margin; };
		minSize = 250 @ 200;
	}

	makeViews {
		var font = Font("Monaco", 10);
		// Pen.font_(Font("", 12));
		nameView = StaticText(zone, Rect(0,0,50, 20))
		.font_(font).string_("weights");
		outNameView = StaticText(zone, Rect(0,0, 200, 20))
		.font_(font).string_("<outNames>")
		.align_(\center).resize_(2); // horz el
		inNameView = StaticText(zone, Rect(0,0, 50, 150))
		.font_(font).string_("<in\nNames>").align_(\center).resize_(4); // vert el
		plotter = Plotter(\weights,  Rect(0,0, 200, 180), zone);
		plotter
		//    .specs_(\pan)
		    // .findSpecs_(false)
		    .plotMode_(\plines)
		    .editMode_(true)
		    .editFunc_({ |plotter, xi, yi, value|
		    	object.weights[yi].put(xi, value);
		    });
		plotter
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

	// name display is just a hack for now, FIXME later
	// p.inNames = a.inNames; p.outNames = a.outNames;
	inNames_ { |newNames|
		var inNameStr = "";
		if (newNames != inNames) {
			newNames.do { |name, i|
				inNameStr = inNameStr
				++ if (i == 0) { "\n\n" } { "\n\n\n\n\n" }
				++ name;
			};
			inNameView.string_(inNameStr);
			inNames = newNames;
		};
	}

	outNames_ { |newNames|
		var outNameStr = "  ";
		if (newNames != outNames) {

			newNames.do { |name, i|
				outNameStr = outNameStr ++ (name.asString.keep(3) ++ " ").clipExtend(4);
			};
			outNameView.string_(outNameStr);
			outNames = newNames;
		};
	}

	checkUpdate {
		var newState = this.getState;
		if (object.notNil) {
			plotter.value_(object.weights.flop);
			plotter.specs_(\pan);
			this.inNames_(newState[\inNames]);
			this.outNames_(newState[\outNames]);
		//	plotter.calcDomainSpecs;
		};
		prevState = newState;
	}
}

InfluxIOGui : JITGui {
	var <inValsGui, <outValsGui, <plotter;

	accepts { |obj| ^obj.isNil or: obj.isKindOf(Influx) }

	*new { |obj, numItems = #[4, 8], parent, bounds, makeSkip = true, options=#[\plot]|
		^super.new(obj, numItems, parent, bounds, makeSkip, options);
	}

	getState {
		var newState = (object: object);
		if (object.notNil) {

		};

		^newState
	}

	checkUpdate {
		if (object != prevState[\object]) {
			if (object.notNil) {
				plotter.specs_([\pan]).domainSpecs_([[0, object.outNames.lastIndex, \lin, 1]]);
				plotter.setValue(object.weights.flop, false);
				object.inNames.do(inValsGui.specs.put(_, \pan));
				inValsGui.object_(object.inValDict);
				this.addInvalActions;

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

	// these methods should be overridden in subclasses:
	setDefaults { |options|
		if (parent.isNil) {
			defPos = 10@260
		} {
			defPos = skin.margin;
		};
		minSize = 350 @ (numItems.sum + 2 * skin.buttonHeight
			+ skin.headHeight + 200);
		//	"minSize: %\n".postf(minSize);
	}
	makePlotter {
	}

	makeViews { |options|
		if (options.includes(\plot)) { this.makePlotter };
		inValsGui = EnvirGui(nil, numItems[0] + 1, zone, options: [\name]).name_(\inVals);
		outValsGui = EnvirGui(nil, numItems[1] + 1, zone, options: [\name]).name_(\outVals);

		inValsGui.valFields
	}
}
