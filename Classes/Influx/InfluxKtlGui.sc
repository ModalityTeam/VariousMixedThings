InfluxKtlGui : JITGui {
	var <leftTopV, <leftBotV, <leftButtonV, <rightButtonV, <xpop, <ypop, <xySlider, <loopGui;

	accepts { |obj| ^obj.isNil or: obj.isKindOf(Influx) }

	setDefaults {
		defPos = 530@660;
		minSize = 330 @ (numItems + 6 * (skin.buttonHeight + 2) + (skin.margin.y * 4) + 200);
	}

	makeViews {
		numItems = 5;

		if (hasWindow.not) {
			this.makeNameView;
		};
		this.name_("TEST");

		leftTopV = HLayoutView(zone, Rect(0, 0, zone.bounds.width, 240))
		.background_(Color.green.alpha_(0.2));

		leftButtonV = VLayoutView(leftTopV, Rect(0, 0, zone.bounds.width * 0.375, 240) );
		rightButtonV = VLayoutView(leftTopV, Rect(0, 0, zone.bounds.width * 0.615, 240) );

		this.makeButtons;
		this.makeSlider;

		loopGui = KtlLoopGui(KtlLoop(\test), 0, parent: zone, bounds: 310 @ 180);
		loopGui.taskGui.name_("KtlLoop for Influx");
	}

	makeButtons { |options|
		var butWid = leftButtonV.bounds.width;
		var butH = skin.buttonHeight + 2;

		StaticText(leftButtonV, Rect(0, 0, butWid, butH))
		.align_(\center).string_("Change  weights:");
		[
			["set to diagL",{ object.setwPre(\diagL); }],
			["disentangle",{ object.disentangle(0.3); }],
			["entangle",{ object.entangle(0.3); }],
			["RANDOM",{  object.rand(1.0); }]
		].collect { |labFunc|
			Button(leftButtonV, Rect(0, 0, butWid, butH))
			.states_([[labFunc[0]]]).action_(labFunc[1]);
		};

		StaticText(leftButtonV, Rect(0, 0, butWid, butH))
		.align_(\center).string_("Influence objects:");

		numItems.collect { |i|
			var name =i.asString;
			Button(leftButtonV, Rect(0, 0, butWid, butH))
			.states_([[name], [name, Color.white, Color.green(0.62)]])
			.action_({ |b, modif| this.influxFunc(i, b.value, modif); });
		};
	}

	makeSlider {
		var butH = skin.buttonHeight + 2;
		var rightWid = rightButtonV.bounds.width;
		var topLine = HLayoutView(rightButtonV, Rect(0, 0, rightWid, butH));

		xpop = EZPopUpMenu(topLine, (rightWid * 0.37)@butH, "x:", labelWidth: 12)
		.items_([\x, \y]).action_({ });
		StaticText(topLine, Rect(0, 0, rightWid * 0.26, butH)).align_(\center)
		.string_("Inputs:");
		ypop = EZPopUpMenu(topLine, (rightWid * 0.37)@butH, "y:", labelWidth: 12)
		.items_([\x, \y]).value_(1).action_({ });

		xySlider = Slider2D(rightButtonV, Rect(0, 0, rightWid, rightWid))
		.x_(0.5).y_(0.5)
		.background_( Color.new255(200, 100, 0) )
		.action_({|sl| this.slAction(sl); });

		xySlider.keyDownAction_({ |sl, ch, mod| this.slKeydown(ch, mod); });

		// SkipJack({  }, 0.05,
		// { w.isClosed }, "slider2D display");
		//
		// //			"n : min, c : center, x : max\n"
		// //			"r : jump to random\n"
		// //			"arrows u,d,l,r: go by steps\n"
		//
		// StaticText(rightButtons, Rect(buttWidth+10, 0, buttWidth, 100)).align_(\center)
		// .string_(
		// 	"         Shortcuts:\n"
		// 	"   o : rec/stoprec loop\n"
	}

	influxFunc { |index, butVal, modif|
		thisMethod.postln;
		[index, butVal, modif].postln;
	}

	slKeydown { |char, modif|
		thisMethod.postln;
		if (object.notNil) {
			char.switch(
				$o, { object.rec.toggleRec },
				$p, { object.rec.togglePlay },
				$ , { object.rec.togglePlay },
				$l, { object.rec.toggleLooped }
			);
		};
	}

	slAction { |sl|
		thisMethod.postln;
		if (object.notNil) {
			// how to attach KtlLoop to Influx? an instvar?
			// // recording into KtlLoop here
			// if (object.rec.notNil) {
			// object.rec.recordEvent((type: \set, x: sl.x, y: sl.y)); };
			// // // and this is the normal set function
			// // // bipolar mapping here done by hand
			object.set(\x, sl.x * 2 - 1, \y, sl.y * 2 - 1);
		}
	}

	checkUpdate {
		var newState = this.getState;

		if (object.notNil) {
			var newX = object.inValDict[\x] + 1 / 2;
			var newY = object.inValDict[\y] + 1 / 2;
			xySlider.setXY(newX, newY);
		};

		prevState = newState;
	}

	// if (b.value > 0) {
	// 	object.attachMapped( p[name]);
	// 	npg.object_(NodeProxyPreset(p[name]));
	// } {
	// 	object.removeMapped(name);
	// 	if (modif.isAlt) { p[name].stop };
	// };
	// });

	// rightButtons = VLayoutView(leftTopV, Rect(0, 0, leftV.bounds.width/2, 250) );
	//
	// StaticText(rightButtons, Rect(buttWidth+10, 0, buttWidth, 20)).align_(\center)
	// .string_("Change inputs:");
	//
	// slider2 = Slider2D(rightButtons,
	// Rect(buttWidth+10, 30, buttWidth, buttWidth))
	// .x_(0.5).y_(0.5)
	// .background_( Color.new255(200, 100, 0) )
	// .action_({|sl|
	// 	// recording into KtlLoop here
	// 	k.recordEvent((type: \set, x: sl.x, y: sl.y));
	// 	// and this is the normal set function
	// 	// bipolar mapping here done by hand
	// 	object.set(\x, sl.x * 2 - 1, \y, sl.y * 2 - 1);
	// });
	// slider2.keyDownAction_({ |sl, char|
	// 	char.switch(
	// 		$o, { k.toggleRec },
	// 		$p, { k.togglePlay },
	// 		$ , { k.togglePlay },
	// 		$l, { k.toggleLooped }
	// 	);
	// });
	// SkipJack({ slider2.setXY(object.inValDict[\x] + 1 / 2, object.inValDict[\y] + 1 / 2) }, 0.05,
	// { w.isClosed }, "slider2D display");
	//
	// //			"n : min, c : center, x : max\n"
	// //			"r : jump to random\n"
	// //			"arrows u,d,l,r: go by steps\n"
	//
	// StaticText(rightButtons, Rect(buttWidth+10, 0, buttWidth, 100)).align_(\center)
	// .string_(
	// 	"         Shortcuts:\n"
	// 	"   o : rec/stoprec loop\n"
	// 	"p / space : play/stop loop\n"
	// 	"     l : looped on/off\n"
	// );
	//
	// leftBotV = HLayoutView(leftV, Rect(0, 290, leftV.bounds.width, 180) );
	// //	ktlg = KtlLoopGui(k, bounds: 310 @ 180, parent: leftBotV);
	// ktlg = KtlLoopGui(k, bounds: 310 @ 180, parent: leftBotV);
	// ktlg.taskGui.name_("KtlLoop for Influx");


}
