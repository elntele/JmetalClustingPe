package org.uma.jmetal.algorithm.multiobjective.nsgaiii.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.uma.jmetal.algorithm.impl.AbstractCoralReefsOptimization.Coordinate;
import org.uma.jmetal.solution.IntegerSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.impl.DefaultIntegerSolution;

/**
 * Classe calcula estatisticas sobre a distribuição da população no Hyperplano
 * 
 * @author Jorge Candeias
 *
 * @param <S>
 */

public class HyperplaneObsevation<S extends Solution<?>> {
	// for n objectives, n lists in this list of list
	private List<List<AnIndividualAndHisVector<S>>> familyOfIndividualInPopulation = new ArrayList<>();
	private List<ReferencePoint> AllRefencePoints;
	private List<List<ReferencePoint>> LargestReferencePointClusters;
	private int numberOfObjectives;
	private Double percentEmpt;
	private boolean equalized;
	private Map<String, List<Integer>> seachType = new HashMap<String, List<Integer>>();
	private List<AnIndividualAndHisVector> strikeTargetGroup = new ArrayList<>();
	private List<AnIndividualAndHisVector> candidateGroup = new ArrayList<>();
	private List<Map<String, ReferencePoint>> mapLargest = new ArrayList<>();
	private List<Double> eQualizationList = new ArrayList<>();
	private List<DefaultIntegerSolution> ListaParaTeste = new ArrayList<>();
	private int TesteSolQueAtendExecge = 0;
	private int TesteSolQueNaoAtendExecge = 0;
	private List<Integer> poorToRich;
	private List<Integer> richToPoor;
	private List<List<AnIndividualAndHisVector<S>>> thoseIndividualsAndThisTrend = new ArrayList<>();

	public HyperplaneObsevation(int numberOfObjectives) {
		this.numberOfObjectives = numberOfObjectives;
		this.AllRefencePoints = new ArrayList<>();
		for (int i = 0; i < numberOfObjectives; i++) {
			List<AnIndividualAndHisVector<S>> l = new ArrayList<>();
			this.familyOfIndividualInPopulation.add(l);
		}
		// acrescentando a lista de individuos segundo sua tendencia
		for (int i = 0; i < numberOfObjectives; i++) {
			List<AnIndividualAndHisVector<S>> k = new ArrayList<>();
			this.thoseIndividualsAndThisTrend.add(k);
		}

		this.LargestReferencePointClusters = new ArrayList<>();
	}

	public int getTesteSolQueAtendExecge() {
		return TesteSolQueAtendExecge;
	}

	public int getTesteSolQueNaoAtendExecge() {
		return TesteSolQueNaoAtendExecge;
	}

	public List<DefaultIntegerSolution> getListaParaTeste() {
		return ListaParaTeste;
	}

	public int includeSolutionInGroupAppropriate(AnIndividualAndHisVector<S> ind) {
		List<Double> d = getAttribute(ind.getSolution());
		int mG = myGroup(d);
		ind.setMyGroups(mG);
		setTheIndividualTrend(d, ind);
		if (mG >= 4 || mG < 0) {
			System.out.println("index de grupo indesejado");
		}
		// int antes = this.familyOfIndividualInPopulation.get(mG).size();
		this.familyOfIndividualInPopulation.get(mG).add((AnIndividualAndHisVector<S>) ind);
//		int depois = this.familyOfIndividualInPopulation.get(mG).size();
//		if (antes == depois) {
//			System.out.println();
//		}

		// investigar porque tem este teste retorno, acho que pode ser apagado
		// a função virar void
		int testeRetorno = 0;
		for (int i = 0; i < this.familyOfIndividualInPopulation.size(); i++) {
			testeRetorno += this.familyOfIndividualInPopulation.get(i).size();
		}
		return testeRetorno;
	}

	public void setSearchTypeS(Properties prop) {
		String[] t = prop.getProperty("typeHelp").split(",");
		List<Integer> l = new ArrayList<>();

		for (int i = 0; i < t.length; i++) {
			l.add(Integer.parseInt(t[i]));
		}
		this.seachType.put(prop.getProperty("buscalocal"), l);
	}

	public List<Double> getAttribute(DefaultIntegerSolution solution) {
		return (List<Double>) solution.getAttribute(getAttributeIdentifier());
	}

	public Object getAttributeIdentifier() {
		return org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection.class;
	}

//	/**
//	 * this method calculate what is the better objective values using 
//	 * the attributes. Its is lake a podium, for a minimization problem, 
//	 * when the smallest  attribute is the third, then the third attribute 
//	 * is in the first place on the podium  
//	 * @param atributes
//	 * @return
//	 */
//	public int myGroup(List<Double> atributes) {
//		Double lower = Double.MAX_VALUE;
//		List<Integer> equals = new ArrayList<>();
//		int iLower = 0;
//		int icurrent = 0;
//		Random gerator = new Random();
//		for (Double d : atributes) {
//			if (d < lower) {
//				lower = d;
//				iLower = icurrent;
//			} else if (d == lower) {
//
//				if ((equals).size() == 0) {
//					equals.add(iLower);
//					equals.add(icurrent);
//				} else {
//					equals.add(icurrent);
//				}
//			}
//			icurrent++;
//		}
//		if (equals.size() == 0) {
//			// teste
//			if (iLower >= 4) {
//				System.out.println("situação indesejada em myGroup ");
//			}
//			// *************
//			return iLower;
//		} else {
//			// teste
//			int r = gerator.nextInt(equals.size() - 1);
//			if (r >= 4) {
//				System.out.println("situação indesejada em myGroup ");
//			}
//			// **********
//			return r;
//		}
//
//	}
	/**
	 * this method calculate what is the better objective value using the
	 * attributes. Its is as a podium for example, for a minimization problem, were
	 * the first smallest attribute is the third attribute, then the third attribute
	 * is in the first place on the podium. And the first place characterizes my
	 * group
	 * 
	 * @param atributes
	 * @return int myGroup
	 */
	public int myGroup(List<Double> attributes) {
		List<Double> ordenadAttributes = new ArrayList<>();
		ordenadAttributes.addAll(attributes);
		Collections.sort(ordenadAttributes);
		List<Integer> betterForWorse = new ArrayList<>();
		for (Double d : ordenadAttributes) {
			for (int i = 0; i < attributes.size(); i++) {
				if (d == attributes.get(i)) {
					betterForWorse.add(i);
				}
			}
		}

		return betterForWorse.get(0);

	}
//	/**
//	 *  this method calculate what is the second better objective value using
//	 *  the attributes. Its is as a podium for example, for a minimization problem,  
//	 *  were  the second smallest  attribute is the third attribute, then the third 
//	 *  attribute  is in the second place on the podium. And the second place is the 
//	 *  trend
//	 * @param atributes
//	 * @return
//	 */

//	public void setTheIndividualTrend(List<Double> atributes, AnIndividualAndHisVector ind) {
//		Double lower = Double.MAX_VALUE;
//		List<Integer> equals = new ArrayList<>();
//		int iLower = 0;
//		int icurrent = 0;
//		Random gerator = new Random();
//		for (Double d : atributes) {
//			if (icurrent != ind.getMyGroups()) {
//				if (d < lower) {
//					lower = d;
//					iLower = icurrent;
//				} else if (d == lower) {
//					if ((equals).size() == 0) {
//						equals.add(iLower);
//						equals.add(icurrent);
//					} else {
//						equals.add(icurrent);
//					}
//				}
//			}
//			icurrent++;
//		}
//		if (equals.size() == 0) {
//			ind.setMyTrends(iLower);
//		} else {
//			ind.setMyTrends(gerator.nextInt(equals.size() - 1));
//		}
//
//	}
	/**
	 * this method calculate what is the second better objective value using the
	 * attributes. Its is as a podium for example, for a minimization problem, were
	 * the second smallest attribute is the third attribute, then the third
	 * attribute is in the second place on the podium. And the second place is the
	 * trend
	 * 
	 * @param atributes
	 * @return
	 */

	public void setTheIndividualTrend(List<Double> attributes, AnIndividualAndHisVector ind) {
		List<Double> ordenadAttributes = new ArrayList<>();
		ordenadAttributes.addAll(attributes);
		Collections.sort(ordenadAttributes);
		List<Integer> betterForWorse = new ArrayList<>();
		for (Double d : ordenadAttributes) {
			for (int i = 0; i < attributes.size(); i++) {
				if (d == attributes.get(i)) {
					betterForWorse.add(i);
				}
			}
		}

		ind.setMyTrends(betterForWorse.get(1));

	}

	/**
	 * this method was created to allocate the all individuals in population accord
	 * with his trend in a list that separate the individuals accord his trend
	 */

	public void separateIndividualsAccordingToTheTrend() {
		for (List<AnIndividualAndHisVector<S>> l : this.familyOfIndividualInPopulation) {
			for (AnIndividualAndHisVector<S> a : l) {
				int myTrends = a.getMyTrends();
				switch (myTrends) {
				case 0:
					this.thoseIndividualsAndThisTrend.get(0).add(a);
					break;
				case 1:
					this.thoseIndividualsAndThisTrend.get(1).add(a);
					break;
				case 2:
					this.thoseIndividualsAndThisTrend.get(2).add(a);
					break;
				case 3:
					this.thoseIndividualsAndThisTrend.get(3).add(a);
					break;

				default:
					break;
				}
			}

		}

	}

	/**
	 * this method returns a list of lists thats have in the inner list in position
	 * 0 the group. and in position 1 the quantity needed of individuals to balance
	 * the population
	 */
	public List<List<Integer>> InformQuantityToBalancePopulationControl() {
		int media = 0;
		int populationSize = 0;
		for (int i = 0; i < this.familyOfIndividualInPopulation.size(); i++) {
			populationSize += this.familyOfIndividualInPopulation.get(i).size();
		}
		media = populationSize / this.numberOfObjectives;
//		list of list, the inner list have in position 0 the group.
//		And in position 1 the quantity needed
		List<List<Integer>> needs = new ArrayList<>();
		for (int i = 0; i < this.eQualizationList.size(); i++) {
			if ((this.eQualizationList.get(i) * populationSize) < media) {
				List<Integer> l = new ArrayList<>();
				l.add(i);
				double d = this.eQualizationList.get(i);
				int neededtNumber = (int) (media - ((int) populationSize * d));
				l.add(neededtNumber);
				needs.add(l);
			}

		}
		return needs;

	}

	/**
	 * this method returns a matingPopulationList filled with part of population
	 * that need get bigger you members
	 * 
	 * @param matingPopulation
	 * @param didBetterToTheSearch
	 */

	public void PopulationControlMating(List<S> matingPopulation, List<Integer> didBetterToTheSearch) {
		List<S> arrived = new ArrayList<>();
		int limit = 0;
		int time = 0;
		arrived.addAll(matingPopulation);
		List<List<Integer>> needs = InformQuantityToBalancePopulationControl();
		for (List<Integer> l : needs) {
			limit += (int) l.get(1);
			List<S> listGoupSolution = new ArrayList<>();
			List<S> copyListGoupSolution = new ArrayList<>();
			List<S> listTrendSolution = new ArrayList<>();
			List<S> copyListTrendSolution = new ArrayList<>();

			for (AnIndividualAndHisVector<S> a : this.familyOfIndividualInPopulation.get(l.get(0))) {
				if (!didBetterToTheSearch.contains(a.getMyIndexInPopulation())) {
					listGoupSolution.add((S) a.getSolution());
				}

			}

			copyListGoupSolution.addAll(listGoupSolution);
			int stop = copyListGoupSolution.size();
			for (int i = 0; i < stop; i++) {
				Collections.shuffle(copyListGoupSolution);
				matingPopulation.add(copyListGoupSolution.get(0));
				copyListGoupSolution.remove(0);
				if (matingPopulation.size() == l.get(1))
					break;
			}

			for (AnIndividualAndHisVector<S> a : this.thoseIndividualsAndThisTrend.get(l.get(0))) {
				if (matingPopulation.size() == l.get(1))
					break;
				if (!didBetterToTheSearch.contains(a.getMyIndexInPopulation())) {
					listTrendSolution.add((S) a.getSolution());
				}
			}

			while (matingPopulation.size() < limit || time < limit) {
				if (matingPopulation.size() == l.get(1))
					break;

				if (listTrendSolution.size() > 0) {
					Collections.shuffle(listTrendSolution);
					matingPopulation.add(listTrendSolution.get(0));
				}

				if (listGoupSolution.size() > 0) {
					Collections.shuffle(listGoupSolution);
					matingPopulation.add(listGoupSolution.get(0));
				}

				time += 1;
			}
		}

	}

	public void includeReferencePoint(ReferencePoint rp) {

		this.AllRefencePoints.add(rp);
		calculateTheEmptReferencesPercent();

	}

	public void calculateTheEmptReferencesPercent() {
		int count = 0;
		for (ReferencePoint p : this.AllRefencePoints) {
			if (p.getMemberList().size() == 0) {
				count++;
			}
		}

		this.percentEmpt = (double) count / this.AllRefencePoints.size();
	}

	public void completTheAllReferencePoints(List<ReferencePoint<S>> largestReferencePointClusters) {
		for (ReferencePoint p : largestReferencePointClusters) {
			this.AllRefencePoints.add(p);
		}
	}

	public List getAllRefencePoints() {
		return AllRefencePoints;
	}

	public void setAllRefencePoints(List allRefencePoints) {

		AllRefencePoints = allRefencePoints;
	}

	public List getLargestReferencePointClusters() {
		return LargestReferencePointClusters;
	}

	public List<Double> geteQualizationList() {
		return eQualizationList;
	}

	public void setLargestReferencePointClusters(List<ReferencePoint<S>> largestReferencePointClusters) {

		completTheAllReferencePoints(largestReferencePointClusters);
		for (int i = 0; i < this.numberOfObjectives; i++) {
			List<ReferencePoint> l = new ArrayList<>();
			HashMap h = new HashMap<String, List<ReferencePoint>>();
			this.LargestReferencePointClusters.add(l);
			this.mapLargest.add(h);
		}
		for (int i = 0; i < largestReferencePointClusters.size(); i++) {
			int g = mayLargestGroup(largestReferencePointClusters.get(i).pos());
			this.LargestReferencePointClusters.get(g).add((ReferencePoint) largestReferencePointClusters.get(i));
			this.mapLargest.get(g).put(largestReferencePointClusters.get(i).pos().toString(),
					(ReferencePoint) largestReferencePointClusters.get(i));

		}

	}

	public int mayLargestGroup(List<Double> pos) {
		Double lower = Double.MAX_VALUE;
		List<Integer> equals = new ArrayList<>();
		int iLower = 0;
		int icurrent = 0;
		Random gerator = new Random();
		for (Double d : pos) {
			if (d < lower) {
				lower = d;
				iLower = icurrent;
			} else if (d == lower) {
				if ((equals).size() == 0) {
					equals.add(iLower);
					equals.add(icurrent);
				} else {
					equals.add(icurrent);
				}

			}

			icurrent++;
		}
		if (equals.size() == 0) {
			return iLower;
		} else {
			return gerator.nextInt(equals.size() - 1);
		}

	}

	public List<List<AnIndividualAndHisVector<S>>> getFamilyOfIndividualInPopulation() {
		return familyOfIndividualInPopulation;
	}

	public void setFamilyOfIndividualInPopulation(
			List<List<AnIndividualAndHisVector<S>>> familyOfIndividualInPopulation) {
		this.familyOfIndividualInPopulation = familyOfIndividualInPopulation;
	}

	public int getNumberOfObjectives() {
		return numberOfObjectives;
	}

	public void setNumberOfObjectives(int numberOfObjectives) {
		this.numberOfObjectives = numberOfObjectives;
	}

	/**
	 * this method use variance to calculate if the population is unbalanced in
	 * terms of distribution, if variance > 10% the population are unbalanced
	 * (review this percentage)
	 */

	public void eQualization() {
		int totality = 0;
		Variance v = new Variance(false);
		List<Double> equali = new ArrayList<>();

		for (List l : this.familyOfIndividualInPopulation) {
			equali.add((double) l.size());
			totality += l.size();
		}

		double[] equaliArray = new double[equali.size()];
		for (int i = 0; i < equali.size(); i++) {
			double d = equali.get(i) / totality;
			equali.remove(i);
			equali.add(i, d);
			equaliArray[i] = d;

		}

		double variancia = v.evaluate(equaliArray);
		if (variancia > 0.01) {
			this.equalized = false;
		} else {
			this.equalized = true;
		}
		if (equali.size() == 0) {
			System.out.println("situação indesejada em eQualization()");
		}
		this.eQualizationList = equali;
	}

	public boolean isEqualized() {
		return equalized;
	}

	public void setEqualized(boolean equalized) {
		this.equalized = equalized;
	}

	public Map<String, List<Integer>> getSeachType() {
		return seachType;
	}

	public void setSeachType(Map<String, List<Integer>> seachType) {
		this.seachType = seachType;
	}

	public void poorToRichCalc() {
		List<Double> sortedEQualizationList = new ArrayList<>();
		sortedEQualizationList.addAll(this.eQualizationList);
		Collections.sort(sortedEQualizationList);
		List<Integer> poorToRich = new ArrayList<>();
		for (Double d : sortedEQualizationList) {
			for (int i = 0; i < this.eQualizationList.size(); i++) {
				if (d == this.eQualizationList.get(i)) {
					poorToRich.add(i);
				}
			}
		}
		this.poorToRich = poorToRich;
	}

	public void richToPoorCalc() {
		List<Double> reversedEQualizationList = new ArrayList<>();
		reversedEQualizationList.addAll(this.eQualizationList);
		Collections.sort(reversedEQualizationList);
		Collections.reverse(reversedEQualizationList);
		List<Integer> richToPoor = new ArrayList<>();
		for (Double d : reversedEQualizationList) {
			for (int i = 0; i < this.eQualizationList.size(); i++) {
				if (d == this.eQualizationList.get(i)) {
					richToPoor.add(i);
				}
			}
		}
		this.richToPoor = richToPoor;
	}

	public List<Integer> getTheProfessors(int numberofProfessors) {
		List<Integer> indexList = new ArrayList<>();
		poorToRichCalc();

		for (List<AnIndividualAndHisVector<S>> l : this.familyOfIndividualInPopulation) {
			Collections.sort(l);
		}

		for (Integer i : this.poorToRich) {
			for (AnIndividualAndHisVector<S> a : this.familyOfIndividualInPopulation.get(i)) {
				if (indexList.size() >= numberofProfessors) {
					break;
				}
				indexList.add(a.getMyIndexInPopulation());
			}
		}
		return indexList;
	}

	public int getTheProfessor() {
		int indexList = 0;
		poorToRichCalc();

		for (List<AnIndividualAndHisVector<S>> l : this.familyOfIndividualInPopulation) {
			Collections.sort(l);
		}

		For1: for (Integer i : this.poorToRich) {
			for (AnIndividualAndHisVector<S> a : this.familyOfIndividualInPopulation.get(i)) {
				if (indexList >= 1) {
					break For1;
				}
				indexList = a.getMyIndexInPopulation();
			}
		}
		return indexList;
	}

	public List<Integer> getTheStudent(int numberofStudent, int indexOfMyProfessor) {
		int groupOfMyProfessor = 0;
		for1: for (Integer i : this.poorToRich) {
			for (AnIndividualAndHisVector<S> a : this.familyOfIndividualInPopulation.get(i)) {
				if (a.getMyIndexInPopulation() == indexOfMyProfessor) {
					groupOfMyProfessor = a.getMyGroups();
					break for1;
				}

			}
		}

		List<Integer> indexList = new ArrayList<>();
		richToPoorCalc();

		for (List<AnIndividualAndHisVector<S>> l : this.familyOfIndividualInPopulation) {
			Collections.shuffle(l);
		}

		For2: for (Integer i : this.richToPoor) {
			for (AnIndividualAndHisVector<S> a : this.familyOfIndividualInPopulation.get(i)) {
				if (indexList.size() >= numberofStudent) {
					break For2;
				}
				if (a.getMyTrends() == groupOfMyProfessor) {
					indexList.add(a.getMyIndexInPopulation());
				}

			}
		}
		if (indexList.size() > numberofStudent) {
			for (int i = 0; i < numberofStudent; i++) {
				if (indexList.size() >= numberofStudent) {
					break;
				}
				Collections.shuffle(this.familyOfIndividualInPopulation.get(this.richToPoor.get(0)));
				indexList.add(this.familyOfIndividualInPopulation.get(this.richToPoor.get(0)).get(0)
						.getMyIndexInPopulation());
			}
		}
		return indexList;
	}

	public List<Integer> selectTheCandidatesTolocalsearch(Properties prop) {
		List<Integer> r = new ArrayList<>();
		if (this.equalized) {
			return r;
		} else {
			// AssociateTheLargestToThefamilyOfIndividualInPopulation(prop);
			r = calcTheCandidates(prop);
			return r;
		}

	}

	public List<Integer> calcTheCandidates(Properties prop) {

		int SolutNumber = Integer.parseInt(prop.getProperty("nIndividuosToSearch"));

		// se este metodo funcionar adeguar tudo inclisive deletando irich e ipoor
		List<Double> sortedEQualizationList = new ArrayList<>();
		sortedEQualizationList.addAll(this.eQualizationList);
		Collections.sort(sortedEQualizationList);
		List<Integer> poorToRich = new ArrayList<>();
		for (Double d : sortedEQualizationList) {
			for (int i = 0; i < this.eQualizationList.size(); i++) {
				if (d == this.eQualizationList.get(i)) {
					poorToRich.add(i);
				}
			}
		}

		List<Integer> indexOfPossibleTobefact = new ArrayList<>();

		for (int i = 0; i < this.familyOfIndividualInPopulation.size(); i++) {
			if (this.familyOfIndividualInPopulation.get(i).size() != 0) {
				Collections.shuffle(this.familyOfIndividualInPopulation.get(i));// teste embaralhamento
			}
		}

		for (Integer index : poorToRich) {
			for (AnIndividualAndHisVector<S> a : this.familyOfIndividualInPopulation.get(index)) {
				if (indexOfPossibleTobefact.size() < SolutNumber) {// teste colocar o máximo do strik goup
					indexOfPossibleTobefact.add(a.getMyIndexInPopulation());
					this.ListaParaTeste.add(a.getSolution());
					this.TesteSolQueAtendExecge += 1;

				} else {
					break;
				}

			}

		}

		if (indexOfPossibleTobefact.size() == 0) {
			System.out.println("deu problema no hp");
		}
		return indexOfPossibleTobefact;
	}

	public List<Integer> selectArearRichCandidatesTolocalsearch(Properties prop, int numberOfVariables) {
		List<Integer> r = new ArrayList<>();
		if (this.equalized) {
			return r;
		} else {
			externalAssociateTheLargestToThefamilyOfIndividualInPopulation(prop, numberOfVariables);
			r = calcTheareaRichCandidates(prop);
			return r;
		}

	}

	/**
	 * este metodo eh uma segunda tentativa do modelo area rica com tendencia a area
	 * pobre a diferença é que a população agora esta sendo observada de fora da
	 * classe enviropimentalSelection (dentro da classe nsga 3) parece que quando se
	 * observa de dentro dá em resultados péssimos, precisa-se investigar isso.
	 * 
	 * @param prop
	 * @return
	 */
	public List<Integer> calcTheareaRichCandidates(Properties prop) {
		int SolutNumber = Integer.parseInt(prop.getProperty("nIndividuosToSearch"));
		Random gerator = new Random();
		String ls = prop.getProperty("buscalocal");
		List<Integer> l = this.seachType.get(ls);
		List<Double> distributed = this.eQualizationList;
		Double rich = -1.0;
		double poor = Double.MAX_VALUE;
		int iRich = 0;
		int iPoor = 0;

		for (int i = 0; i < distributed.size(); i++) {
			if (l.contains(i)) {
				if (distributed.get(i) < poor) {
					poor = distributed.get(i);
					iPoor = i;
				} else if (distributed.get(i) == poor) {
					iPoor = gerator.nextInt((i - iPoor) + 1) + iPoor;// gambiarra que sorteia entre i e iPoor
				}
				if (distributed.get(i) > rich) {
					rich = distributed.get(i);
					iRich = i;
				} else if (distributed.get(i) == rich) {
					iRich = gerator.nextInt((i - iRich) + 1) + iRich;
				}
			}
		}

		if (iRich == iPoor) {
			System.out.println("situação indesejada");
		}

		List<Integer> indexOfPossibleTobefact = new ArrayList<>();
//		// primeiro coloca-se na lista de individuos para busca os elementos
//		// do strik ou dos maiores clustes que atenden a regra de esta no
//		// rico e tender pro pobre.
		for (AnIndividualAndHisVector<S> a : this.strikeTargetGroup) {
			if (indexOfPossibleTobefact.size() < SolutNumber / 2) {// teste colocar o máximo do strik goup
				if (a.getMyGroups() == iRich) {
					if (a.getMyTrends() == iPoor) {
						indexOfPossibleTobefact.add(a.getMyIndexInPopulation());
						this.ListaParaTeste.add(a.getSolution());
						this.TesteSolQueAtendExecge += 1;
					}
				}
			} else {
				break;
			}

		}

//		depois, caso não tenha chegado ao menos a metade de individuos. 
//		coloca-se a metade dos individos que estao na lista de strik 
//		como uma unica condicao: que ele estaja em um eixo de atuação
//		da busca, inclusive essa questao de ter que pertencer a um
//		eixo afetado pela busca ja foi tratada no preenchimento da lista
//		strikeTargetGroup.
		if (indexOfPossibleTobefact.size() < (SolutNumber / 2)) {// teste colocar o máximo do strik goup
			for (AnIndividualAndHisVector<S> a : this.strikeTargetGroup) {
				if (a.getMyGroups() == iRich) {

					indexOfPossibleTobefact.add(a.getMyIndexInPopulation());
					this.ListaParaTeste.add(a.getSolution());
					this.TesteSolQueNaoAtendExecge += 1;
					if (indexOfPossibleTobefact.size() >= (SolutNumber / 2)) {
						break;
					}

				}
			}
		}

//		 teste teste colocar o máximo do strik goup
//		a partir de agora serão colocados mebros da area rica, acontece que nem sempre 
//		os maiores clusteres estão na area rica, então eh preciso dividir os eleitos pra
//		busca entre esses dois grupos: individous que estao nos clusters grande e individuos
//		que estao na area rica sem esta em cluster grande.
//		esses individuos da area rica, por enquato, são selecionado tendo tendencia 
//		para area pobre

		// rotulando loops: novidade :D
		if (indexOfPossibleTobefact.size() < SolutNumber) {
			for1: for (int i = 0; i < l.size(); i++) {// i representa o objetivo que a busca interfere
				for (int k = 0; k < this.familyOfIndividualInPopulation.size(); k++) {// k a o indice da lista de listas
																						// de
																						// individuos
					if (l.contains(k)) {// veja condicao 1: a lista de lista de individuos é estruturada conforme os
										// objetivos
						for (AnIndividualAndHisVector<S> a : familyOfIndividualInPopulation.get(k)) {
							if (a.getMyGroups() == iRich && a.getMyTrends() == iPoor) {
								if (!(indexOfPossibleTobefact.contains(a.getMyIndexInPopulation()))) {
									if (indexOfPossibleTobefact.size() < SolutNumber) {
										indexOfPossibleTobefact.add(a.getMyIndexInPopulation());
										this.ListaParaTeste.add(a.getSolution());
										this.TesteSolQueAtendExecge += 1;
									} else {
										break for1;
									}

								}
							}
						}

					}

				}
			}
		}
//		se ainda assim a lista nao tiver o numero requido de solucoes
//		vai adicionando da area rica ate que complete

		if (indexOfPossibleTobefact.size() < SolutNumber) {

//		    preenche o restante da lista com os mais ricos com a unica 
//		    restricao de nao  ser repetido
			List<Integer> rl = new ArrayList<>();
			List<Integer> oldr = new ArrayList<>();
			rl.add(iRich);
			while (indexOfPossibleTobefact.size() < SolutNumber) {

				for2: for (int g = 0; g < this.familyOfIndividualInPopulation.size(); g++) {
					if (g == rl.get(0) && !oldr.contains(g)) {
						for (int i = 0; i < this.familyOfIndividualInPopulation.get(g).size(); i++) {
							if (indexOfPossibleTobefact.size() < SolutNumber) {
								if (!(indexOfPossibleTobefact.contains(
										this.familyOfIndividualInPopulation.get(g).get(i).getMyIndexInPopulation()))) {
									indexOfPossibleTobefact.add(
											this.familyOfIndividualInPopulation.get(g).get(i).getMyIndexInPopulation());
									this.ListaParaTeste
											.add(this.familyOfIndividualInPopulation.get(g).get(i).getSolution());
									this.TesteSolQueNaoAtendExecge += 1;
								}
							} else {
								break for2;
							}

						}

					}

				}
				oldr.add(rl.get(0));
				rl.remove(rl.get(0));
				rich = -1.0;
				iRich = 0;
				for (int j = 0; j < distributed.size(); j++) {
					if (distributed.get(j) > rich && !oldr.contains(j) && l.contains(j)) {
						rich = distributed.get(j);
						iRich = j;
					}

				}
			}
		}

//		while(indexOfPossibleTobefact.size()<SolutNumber) {
//			for (int i=0;i<indexOfPossibleTobefact.size();i++) {
//				if  (indexOfPossibleTobefact.size()<SolutNumber) {
//					indexOfPossibleTobefact.add(indexOfPossibleTobefact.get(i));
//				}else {
//					break;
//				}
//			}
//			
//		}

		if (indexOfPossibleTobefact.size() < SolutNumber) {
			System.out.println();
		}
		return indexOfPossibleTobefact;
	}

	public void AssociateTheLargestToThefamilyOfIndividualInPopulation(Properties prop) {
		setSearchTypeS(prop);// setando a lista de tipos de vetores da busca local
		List<AnIndividualAndHisVector> possibleSelectioned = new ArrayList<>();
		// o array retornado tem em suas posicoes em que objetivo a busca ajuda
		String ls = prop.getProperty("buscalocal");
		// condicao 1
		// se a busca exerce uma forca nos objetivos 1 e 2, por exemplo, so
		// interessa entrar nas posicoes 1 e 2 de mapLargest a qual vamos
		// causar excitacoes em algum membro entre os seus references points
		List<Integer> l = this.seachType.get(ls);

		for (int i = 0; i < l.size(); i++) {// i representa o objetivo que a busca interfere
			for (int k = 0; k < this.familyOfIndividualInPopulation.size(); k++) {// k a o indice da lista de listas de
																					// individuos
				// if (l.contains(k)) {// veja condicao 1: a lista de lista de individuos é
				// estruturada conforme os
				// objetivos
				for (AnIndividualAndHisVector<S> a : familyOfIndividualInPopulation.get(k)) {
					ReferencePoint p = this.mapLargest.get(l.get(i))
							.get(a.getPositionOfMyProprietaryPoint().toString());
					try {
						if (!p.equals(null)) {
							possibleSelectioned.add(a);
						}
					} catch (Exception e) {
						continue;
					}
				}
				// }

			}
		}

		this.strikeTargetGroup = possibleSelectioned;

	}

	public void externalAssociateTheLargestToThefamilyOfIndividualInPopulation(Properties prop, int numberOfVariables) {
		setSearchTypeS(prop);// setando a lista de tipos de vetores da busca local
		List<AnIndividualAndHisVector> possibleSelectioned = new ArrayList<>();
		// o array retornado tem em suas posicoes em que objetivo a busca ajuda
		String ls = prop.getProperty("buscalocal");
		// condicao 1
		// se a busca exerce uma forca nos objetivos 1 e 2, por exemplo, so
		// interessa entrar nas posicoes 1 e 2 de mapLargest a qual vamos
		// causar excitacoes em algum membro entre os seus references points
		List<Integer> l = this.seachType.get(ls);

		for (int i = 0; i < l.size(); i++) {
			// List <AnIndividualAndHisVector<S>>
			// iList=this.familyOfIndividualInPopulation.get(l.get(i));

			for (AnIndividualAndHisVector<S> a : familyOfIndividualInPopulation.get(l.get(i))) {
				for (ReferencePoint p : this.LargestReferencePointClusters.get(l.get(i))) {
					for (Object s : p.getMemberList()) {
						s = (IntegerSolution) s;
						boolean teste = solutionsEquals((IntegerSolution) a.getSolution(), (IntegerSolution) s);
						if (teste) {
							possibleSelectioned.add(a);
						}
					}
				}
			}

		}

		this.strikeTargetGroup = possibleSelectioned;

	}

	// parei aqui
	/*
	 * List<DefaultIntegerSolution> pReturned, List<S> population
	 */
	public boolean solutionsEquals(IntegerSolution s1, IntegerSolution s2) {
		DefaultIntegerSolution d1 = (DefaultIntegerSolution) s1;
		DefaultIntegerSolution d2 = (DefaultIntegerSolution) s2;
		boolean a = d1.getvariables().equals(d2.getvariables());
		boolean b = d1.getLineColumn().equals(d2.getLineColumn());
		return (a && b);

	}

	public List<Integer> getRichToPoor() {
		return richToPoor;
	}

	public boolean contaiSolution(S s, List<AnIndividualAndHisVector<S>> l) {
		boolean retorno = false;
		for (AnIndividualAndHisVector<S> a : l) {
			if (solutionsEquals((IntegerSolution) s, a.getSolution())) {
				retorno = true;
				break;
			}
		}

		return retorno;
	}

	/**
	 * metdo construido para pegar o professor: retorna o indice na populacao do
	 * primeiro membro do grupo com menos individuos, ordenado pelo valor de
	 * objetivo normatizado que representa seu grupo
	 * 
	 * @return
	 */
	public int takeTheProfessor() {

		// se este metodo funcionar adeguar tudo inclisive deletando irich e ipoor
		List<Double> sortedEQualizationList = new ArrayList<>();
		sortedEQualizationList.addAll(this.eQualizationList);
		Collections.sort(sortedEQualizationList);
		List<Integer> poorToRich = new ArrayList<>();
		for (Double d : sortedEQualizationList) {
			for (int i = 0; i < this.eQualizationList.size(); i++) {
				if (d == this.eQualizationList.get(i)) {
					poorToRich.add(i);
				}
			}
		}

		List<Integer> indexOfPossibleTobefact = new ArrayList<>();

		for (int i = 0; i < this.familyOfIndividualInPopulation.size(); i++) {
			Collections.sort(this.familyOfIndividualInPopulation.get(i));// teste embaralhamento
		}
		int l = 0;
		while (this.familyOfIndividualInPopulation.get(poorToRich.get(l)).size() == 0) {
			l += 1;
		}
		int i = -1;
		try {
			i = this.familyOfIndividualInPopulation.get(poorToRich.get(l)).get(0).getMyIndexInPopulation();
		} catch (Exception e) {
			System.out.println("deu errado no metodo do professor");
			// TODO: handle exception
		}

		return i;

	}

}
