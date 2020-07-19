package org.uma.jmetal.algorithm.multiobjective.nsgaiii.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.uma.jmetal.qualityindicator.impl.HypervolumeConc;
import org.uma.jmetal.solution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.front.Front;
import org.uma.jmetal.util.front.imp.ArrayFront;
import org.uma.jmetal.util.front.util.FrontUtils;
import org.uma.jmetal.util.point.util.PointSolution;

public class MetricEvalRunTime {
	private int numberOfOjetives;
	private List<Boolean> doNormatization = new ArrayList<>();

	public MetricEvalRunTime(int numberOfObjetives) {
		this.numberOfOjetives = numberOfObjetives;

	}

	public Double evaluateFront(List<DefaultIntegerSolution> pop, List<Double> w) {

		Front frontRef = new ArrayFront(1, numberOfOjetives);
		HypervolumeConc hypervolume = new HypervolumeConc(frontRef);

//		// os maiores de pe
//		double wA = 19518.068353117065;
//		double wB = 433736.6460002156;

		Front normalizedFront = null;
		double hvm = 0;

		normalizedFront = new ArrayFront(pop);

		for (int s = 0; s < normalizedFront.getNumberOfPoints(); s++) {
			for (int k = 0; k < this.numberOfOjetives; k++) {
				
					normalizedFront.getPoint(s).setDimensionValue(k,
							normalizedFront.getPoint(s).getDimensionValue(k) / w.get(k));
				
				}
			}
			List<PointSolution> normalizedPopulation = FrontUtils.convertFrontToSolutionList(normalizedFront);
			hvm += hypervolume.evaluate(normalizedPopulation);

		
		return hvm;
	}
}
