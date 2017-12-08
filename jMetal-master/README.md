# jMetal 5 Development Site

### Status
[![Build Status](https://travis-ci.org/jMetal/jMetal.svg?branch=master)](https://travis-ci.org/jMetal/jMetal)

**jMetal** is an object-oriented Java-based framework for multi-objective optimization with metaheuristics
The Web page of the project is: [http://jmetal.github.io/jMetal/](http://jmetal.github.io/jMetal/). Former jMetal versions can be found in [SourceForge](http://jmetal.sourceforge.net). The current version is jMetal 5.3. 

The jMetal development version is hosted in this repository; this way, interested users can take a look to
the new incoming features in advance.

If you are interested in contributing with your ideas and comments, please take a look the current discussions in the [Issues section](https://github.com/jMetal/jMetal/issues).

## Changelog of the next incoming release (jMetal 5.4)

### Algorithms
* The algorithm R-NSGA-II (https://doi.org/10.1145/1143997.1144112) is available. Contribution of Cristóbal Barba (@cbarba).
* Algorithm WASF-GA now can solve constrained problems (https://github.com/jMetal/jMetal/blob/master/jmetal-exec/src/main/java/org/uma/jmetal/runner/multiobjective/WASFGAConstraintRunner.java). Contribution of Cristóbal Barba (@cbarba).
* Updated version of MOEA/DD and added a [runner class](https://github.com/jMetal/jMetal/blob/master/jmetal-exec/src/main/java/org/uma/jmetal/runner/multiobjective/MOEADDRunner.java) for it. Contribution of @fritsche.

### Bugs fixed
* Algorithm WASF-GA now can solve problems with more than two objectives.

### Operators
* The `CrossoverOperator` interface contains a new method `numberOfGeneratedChildren()` and the former `getNumberOfParents()` has been renamed to `getNumberOfRequiredParents()`.

### Util classes
* Added a [`WeightVectorNeighborhood`](https://github.com/jMetal/jMetal/blob/master/jmetal-core/src/main/java/org/uma/jmetal/util/neighborhood/impl/WeightVectorNeighborhood.java) class, which implements the [`Neighborhood`](https://github.com/jMetal/jMetal/blob/master/jmetal-core/src/main/java/org/uma/jmetal/util/neighborhood/Neighborhood.java) interface with the neighborhood scheme of MOEA/D.

### Refactoring
* Changed case of class names in CEC benchmark problems. Contribution of @leechristie

## jMetal is available as a Maven Project in The Central Repository

The link to the modules is: https://search.maven.org/#search%7Cga%7C1%7Cjmetal

## jMetal documentation
The documentation is hosted in https://github.com/jMetal/jMetalDocumentation

## Publications
A.J. Nebro, J.J. Durillo, M. Vergne: "Redesigning the jMetal Multi-Objective Optimization Framework". Proceedings of the Companion Publication of the 2015 on Genetic and Evolutionary Computation Conference (GECCO Companion '15) Pages 1093-1100. DOI: http://dx.doi.org/10.1145/2739482.2768462

## Code coverage (4th April 2016)
Coverage data of the jmetal-core package reported by IntelliJ Idea:

|Class % |Method %| Line % |
|--------|--------|--------|
|51,8% (93/181) |	40.0% (375/393) | 37% (1183/5084)



