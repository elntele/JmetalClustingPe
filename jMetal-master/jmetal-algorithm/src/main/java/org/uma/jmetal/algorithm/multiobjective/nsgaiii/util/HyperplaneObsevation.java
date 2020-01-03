package org.uma.jmetal.algorithm.multiobjective.nsgaiii.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
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
	private List<List<AnIndividualAndHisVector>> familyOfIndividualInPopulation;
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
	private int TesteSolQueAtendExecge=0 ;
	private int TesteSolQueNaoAtendExecge=0;

	public HyperplaneObsevation(int numberOfObjectives) {
		this.numberOfObjectives = numberOfObjectives;
		this.AllRefencePoints = new ArrayList<>();
		this.familyOfIndividualInPopulation = new ArrayList<>();
		for (int i = 0; i < numberOfObjectives; i++) {
			List<AnIndividualAndHisVector> l = new ArrayList<>();
			this.familyOfIndividualInPopulation.add(l);
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





	public void includeSolutionInGroupAppropriate(AnIndividualAndHisVector ind) {
		List<Double> d = getAttribute(ind.getSolution());
		int mG = myGroup(d);
		ind.setMyGroups(mG);
		setTheIndividualTrend(d, ind);
		if (mG>=4){
			System.out.println("index de grupo indesejado");
		}
		this.familyOfIndividualInPopulation.get(mG).add(ind);

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

	public int myGroup(List<Double> atributes) {
		Double lower = Double.MAX_VALUE;
		List<Integer> equals = new ArrayList<>();
		int iLower = 0;
		int icurrent = 0;
		Random gerator = new Random();
		for (Double d : atributes) {
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
			//teste
			if (iLower>=4) {
				System.out.println("situação indesejada em myGroup ");
			}
			//*************
			return iLower;
		} else {
			//teste
			int r =gerator.nextInt(equals.size() - 1);
			if (r>=4) {
				System.out.println("situação indesejada em myGroup ");
			}
			//**********
			return r;
		}

	}

	public void setTheIndividualTrend(List<Double> atributes, AnIndividualAndHisVector ind) {
		Double lower = Double.MAX_VALUE;
		List<Integer> equals = new ArrayList<>();
		int iLower = 0;
		int icurrent = 0;
		Random gerator = new Random();
		for (Double d : atributes) {
			if (icurrent != ind.getMyGroups()) {
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
			}
			icurrent++;
		}
		if (equals.size() == 0) {
			ind.setMyTrends(iLower);
		} else {
			ind.setMyTrends(gerator.nextInt(equals.size() - 1));
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

	public List<List<AnIndividualAndHisVector>> getFamilyOfIndividualInPopulation() {
		return familyOfIndividualInPopulation;
	}

	public void setFamilyOfIndividualInPopulation(List<List<AnIndividualAndHisVector>> familyOfIndividualInPopulation) {
		this.familyOfIndividualInPopulation = familyOfIndividualInPopulation;
	}

	public int getNumberOfObjectives() {
		return numberOfObjectives;
	}

	public void setNumberOfObjectives(int numberOfObjectives) {
		this.numberOfObjectives = numberOfObjectives;
	}

	public void eQualization() {
		int totality = 0;
		Variance v = new Variance();
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
		if (equali.size()==0) {
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

	public List<Integer> selectTheCandidatesTolocalsearch(Properties prop) {
		List<Integer> r = new ArrayList<>();
		if (this.equalized) {
			return r;
		} else {
			AssociateTheLargestToThefamilyOfIndividualInPopulation(prop);
			r = calcTheCandidates(prop);
			return r;
		}

	}

	public List<Integer> calcTheCandidates(Properties prop) {
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
		// primeiro coloca-se na lista de individous para busca os elementos
		// do strik ou dos maiores clustes que atenden a gegra de esta no
		// rico e tender pro pobre.
		for (AnIndividualAndHisVector<S> a : this.strikeTargetGroup) {
			if (indexOfPossibleTobefact.size() < SolutNumber/* / 2*/) {// teste colocar o máximo do strik goup
				if (a.getMyGroups() == iRich) {
					if (a.getMyTrends() == iPoor) {
						indexOfPossibleTobefact.add(a.getMyIndexInPopulation());
						this.ListaParaTeste.add(a.getSolution());
						this.TesteSolQueAtendExecge+=1;
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
		if (indexOfPossibleTobefact.size() < (SolutNumber /*/ 2*/)) {// teste colocar o máximo do strik goup
			for (AnIndividualAndHisVector<S> a : this.strikeTargetGroup) {
				if (a.getMyGroups() == iRich) {

					indexOfPossibleTobefact.add(a.getMyIndexInPopulation());
					this.ListaParaTeste.add(a.getSolution());
					this.TesteSolQueNaoAtendExecge+=1;
					if (indexOfPossibleTobefact.size() >= (SolutNumber /* / 2*/)) {
						break;
					}

				}
			}
		}
		/* teste teste colocar o máximo do strik goup
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
		
		while(indexOfPossibleTobefact.size()<SolutNumber) {
			for (int i=0;i<indexOfPossibleTobefact.size();i++) {
				if  (indexOfPossibleTobefact.size()<SolutNumber) {
					indexOfPossibleTobefact.add(indexOfPossibleTobefact.get(i));
				}else {
					break;
				}
			}
			
		}
*/
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
				if (l.contains(k)) {// veja condicao 1: a lista de lista de individuos é estruturada conforme os
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
				}

			}
		}

		this.strikeTargetGroup = possibleSelectioned;

	}

}
