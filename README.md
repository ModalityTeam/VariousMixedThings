## VMT -- Various Mixed Things

Useful functions and performance tools.

Requirements: 
The Influx classes use the Halo class, which is in the JITLibExtensions quark.


Currently contains:

+ **SFunction** -- a function with an environment within which the function is evaluated

+ **CtlPlayer** -- combination of a NodeProxy and CtLoop with methods to map input controls to parameters (depends on GamePad)

+ **DefLib** make libs where lots of Xdefs can be present in the background, 
(but not in .all, so they dont clutter Xdef.all, resp. mixer)

+ **Influx** controller-reduction made easy : 
   fan-out few controller params to many process parameters, 
   mix multiple control sources to single destination

      