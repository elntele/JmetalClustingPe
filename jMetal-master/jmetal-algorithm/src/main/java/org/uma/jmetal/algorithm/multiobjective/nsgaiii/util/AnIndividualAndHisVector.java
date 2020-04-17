package org.uma.jmetal.algorithm.multiobjective.nsgaiii.util;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.solution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.evaluator.impl.SeverAndId;

/**
 * Classe criada para auxiliar HyperPlaneObservation esse objeto guarda
 * informações sobre a situação de um individuo dentro da popuação como seu
 * indice. a posição do seu reference point e sua tendencia
 * 
 * @author Jorge Candeias
 *
 * @param <S>
 */

public class AnIndividualAndHisVector<S> implements Comparable {
	private DefaultIntegerSolution solution;
	private int myTrends;
	List<Double> positionOfMyProprietaryPoint;
	private int myIndexInPopulation;
	private int myGroups;

	public AnIndividualAndHisVector() {
		this.positionOfMyProprietaryPoint = new ArrayList<>();

	}

	public int getMyGroups() {
		return myGroups;
	}

	public void setMyGroups(int myGroups) {
		this.myGroups = myGroups;
	}

	public AnIndividualAndHisVector(S solution/* , List<Double> positionOfMyProprietaryPoint */,
			int myIndexInPopulation) {
		super();
		this.solution = (DefaultIntegerSolution) solution;
		this.positionOfMyProprietaryPoint = positionOfMyProprietaryPoint;
		this.myIndexInPopulation = myIndexInPopulation;
	}

	public DefaultIntegerSolution getSolution() {
		return solution;
	}

	public void setSolution(S solution) {
		this.solution = (DefaultIntegerSolution) solution;
	}

	public List<Double> getPositionOfMyProprietaryPoint() {
		return positionOfMyProprietaryPoint;
	}

	public void setPositionOfMyProprietaryPoint(List<Double> positionOfMyProprietaryPoint) {
		this.positionOfMyProprietaryPoint = positionOfMyProprietaryPoint;
	}

	public int getMyTrends() {
		return myTrends;
	}

	public void setMyTrends(int myTrends) {
		this.myTrends = myTrends;
	}

	public List<Double> getMyPointPosition() {
		return positionOfMyProprietaryPoint;
	}

	public void setMyPointPosition(List<Double> myPointPosition) {
		this.positionOfMyProprietaryPoint = myPointPosition;
	}

	public int getMyIndexInPopulation() {
		return myIndexInPopulation;
	}

	public void setMyIndexInPopulation(int myIndexInPopulation) {
		this.myIndexInPopulation = myIndexInPopulation;
	}

	public List<Double> getAttribute(DefaultIntegerSolution solution) {
		return (List<Double>) solution.getAttribute(getAttributeIdentifier());
	}

	public Object getAttributeIdentifier() {
		return org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection.class;
	}

	/**
	 * metodo inserido para ordenação de listas de objetos severAndId
	 */

	@Override
	public int compareTo(Object indivi) {
		AnIndividualAndHisVector<S> a = (AnIndividualAndHisVector<S>) indivi;
		DefaultIntegerSolution sArrived = (DefaultIntegerSolution) a.getSolution();
		DefaultIntegerSolution sThisOne = (DefaultIntegerSolution) this.solution;
		List<Double> arreved = this.getAttribute(sArrived);
		List<Double> thisOne = this.getAttribute(sThisOne);
		Double arriveAttribute = arreved.get(this.myGroups);
		Double thisOneAttribute = thisOne.get(this.myGroups);
		return thisOneAttribute.compareTo(arriveAttribute);
	}

//	@Override
//	public int compareTo(Object indivi) {
//		double objetive=(int)((AnIndividualAndHisVector<S>)indivi).getSolution().getAttribute(this.myGroups);
//		return (int) ((Double) this.solution.getAttribute(this.myGroups)-objetive);
//	}

}
