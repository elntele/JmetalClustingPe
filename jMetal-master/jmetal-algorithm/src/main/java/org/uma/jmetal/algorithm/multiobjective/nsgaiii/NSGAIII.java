package org.uma.jmetal.algorithm.multiobjective.nsgaiii;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.ReferencePoint;
import org.uma.jmetal.solution.IntegerSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.solutionattribute.Ranking;
import org.uma.jmetal.util.solutionattribute.impl.DominanceRanking;

import br.cns.model.GmlData;
import cbic15.Pattern;

/**
 * Created by ajnebro on 30/10/14. Modified by Juanjo on 13/11/14
 *
 * This implementation is based on the code of Tsung-Che Chiang
 * http://web.ntnu.edu.tw/~tcchiang/publications/nsga3cpp/nsga3cpp.htm
 */
@SuppressWarnings("serial")
public class NSGAIII<S extends Solution<?>> extends AbstractGeneticAlgorithm<S, List<S>> {
	protected int iterations;
	protected int maxIterations;
	// prop vem do arquivo scr/dados.properties
	protected Properties prop;

	protected SolutionListEvaluator<S> evaluator;

	protected Vector<Integer> numberOfDivisions;
	protected List<ReferencePoint<S>> referencePoints = new Vector<>();
	protected GmlData gml;
	protected List<Pattern>[] clustters;
	private int LocalSeachFoundNoDominated = 0;

	/** Constructor */
	public NSGAIII(NSGAIIIBuilder<S> builder) { // can be created from the
												// NSGAIIIBuilder within the
												// same package
		super(builder.getProblem());
		this.gml = builder.getGml();
		this.clustters = builder.getClustters();
		maxIterations = builder.getMaxIterations();
		crossoverOperator = builder.getCrossoverOperator();
		mutationOperator = builder.getMutationOperator();
		selectionOperator = builder.getSelectionOperator();

		evaluator = builder.getEvaluator();
		prop=builder.getProp();

		/// NSGAIII
		numberOfDivisions = new Vector<>(1);
		numberOfDivisions.add(12); // Default value for 3D problems

		(new ReferencePoint<S>()).generateReferencePoints(referencePoints, getProblem().getNumberOfObjectives(),
				numberOfDivisions);

		int populationSize = referencePoints.size();
		System.out.println(referencePoints.size());
		while (populationSize % 4 > 0) {
			populationSize++;
		}

		setMaxPopulationSize(populationSize);

		JMetalLogger.logger.info("rpssize: " + referencePoints.size());
		;
	}

	@Override
	protected void initProgress() {
		iterations = 1;
	}

	@Override
	protected void updateProgress() {
		iterations++;
		System.out.println("numero de iteraçõs" + iterations);
		if (this.iterations % 20 == 0) {
			printFinalSolutionSet(this.population);
		}
	}

	@Override
	protected boolean isStoppingConditionReached() {
		return iterations >= maxIterations;
	}
	
	/**
	 * método que alterei pra gerar um aqruivo .tsv com 456 redes aleatorias
	 * resolvi deixar comentada
	 */
//	@Override
//	protected List<S> evaluatePopulation(List<S> population) {
//		population = evaluator.evaluate(population, getProblem());
//		String path="C:\\Users\\elnte\\OneDrive\\Área de Trabalho\\fixedSolution.tsv";
//		FileWriter arq;
//		try {
//			arq = new FileWriter(path);
//			 PrintWriter gravarArq = new PrintWriter(arq);
//			 int i=1;
//			 for (Solution s:population) {
//				 if (i==428) {
//					 System.out.println("aqui");
//				 }
//				 String[] Str=s.toString().split("Objectives");
//				 String[] Strf=Str[0].split(": ");
//				 gravarArq.printf(Strf[1]+"\n");
//				 i+=1;
//			 }
//			 arq.close();
//			 
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    
//
//		return population;
//	}


	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		population = evaluator.evaluate(population, getProblem());
		return population;
	}

	/**
	 * método pra modificar a matriz inteira mudando cada elemento dela por um
	 * dos 3 patterns mais proximos de forma aleatória
	 */
	public Pattern[] createNewMatrix(Pattern[] lineColumn) {// lineColumn é uma
															// copia
		Pattern[] copyLineColumn = new Pattern[lineColumn.length];
		Random gerator = new Random();

		for (int i = 0; i < lineColumn.length; i++) {
			// hou uma falha nesta linha abaixo que foi comentada pq não houve
			// inclussão do parametro numberNeighbors que passou a ser neceário
			// apos deixa o tamanho da busca dinâmico
			// copyLineColumn[i] = takeTreNodeMinDistance(this.clustters[i],
			// lineColumn[i]).get(gerator.nextInt(3));
		}
		return copyLineColumn;
	}

	/**
	 * metodo muda toda a matriz de cidades
	 * 
	 * @param solution
	 * @return
	 */

	public IntegerSolution changeMatrix(IntegerSolution solution) {// solution é
																	// uma cópia
		Random gerator = new Random();
		// teste muda elemento da matriz
		Pattern[] patterns = createNewMatrix(solution.getLineColumn().clone());
		solution.setLineColumn(patterns.clone());
		return solution;
	}

	/**
	 * compara duas soluções e retorna: -1 se s1 domina s2 0 se s1 e s2 são não
	 * dominadas 1 se s2 domina s1
	 * 
	 * @param solutio1
	 *            e solution2
	 */
	public int coparation(IntegerSolution s1, IntegerSolution s2) {
		DominanceComparator comparater = new DominanceComparator();
		int i = comparater.compare(s1, s2);
		// if (i == -1) {
		// //System.out.println("s1 domina s2");
		// }
		// if (i == 0) {
		// System.out.println("não há dominação");
		// }
		if (i == 1) {
			System.out.println("s2 domina s1");
			this.LocalSeachFoundNoDominated += 1;
		}
		return i;
	}

	/**
	 * metodo recebe um nó e uma lista de nós e retorna o nó da lista mais
	 * próximo ao nó parametro
	 * 
	 * @param node
	 * @param copyPatternList
	 * @return
	 */

	public Pattern takeNodeMinDistance(Pattern node, List<Pattern> copyPatternList) {// copyPatternList
																						// é
																						// uma
																						// copia
		double minDinstace = Double.MAX_VALUE;
		Pattern patternNode = null;
		for (Pattern P : copyPatternList) {
			if (P.getId() != node.getId()) {
				if (this.gml.getDistances()[node.getId()][P.getId()] < minDinstace) {
					minDinstace = this.gml.getDistances()[node.getId()][P.getId()];
					patternNode = P;
				}
			}
		}

		return patternNode;
	}

	/**
	 * seleciona os três nos mais próximos e retorna eles
	 * 
	 * @param patternList
	 * @param pattern
	 * @return
	 */

	public List<Pattern> takeNnumberNodeMinDistance(List<Pattern> patternList, Pattern pattern, int numberNeighbors) {
		List<Pattern> Litlepattern = new ArrayList<>();
		List<Pattern> copyPatternList = new ArrayList<>();
		int maxNumberNeighbors = patternList.size() - 1;
		copyPatternList.addAll(patternList);
		if (numberNeighbors > maxNumberNeighbors ) { // garante que o numero
														// de cidades da busca
														// não exceda o numero
														// de cidades do cluster
			numberNeighbors = maxNumberNeighbors;
														
		}
		for (int i = 0; i < numberNeighbors; i++) {
			Litlepattern.add(takeNodeMinDistance(pattern, copyPatternList));
			copyPatternList.remove(Litlepattern.get(i));
		}

		// Litlepattern.add(takeNodeMinDistance(pattern, copyPatternList));
		// copyPatternList.remove(Litlepattern.get(1));
		// Litlepattern.add(takeNodeMinDistance(pattern, copyPatternList));
		System.out.println("tamanho da lista: " + Litlepattern.size());

		return Litlepattern;
	}

	/**
	 * Muda um elemento do array matriz 
	 * 
	 * @param position
	 * @param patterns
	 * @return
	 */

	public Pattern[] changeOneIndexOfMatrix(int position, Pattern[] patterns, int numberNeighbors) {// patterns
																									// é
																									// uma
																									// cópia
		// aqui é a hora de testar se a lis
		Random gerator = new Random();
		List<Pattern> Litlepattern = new ArrayList<>();
		
		
		Litlepattern = takeNnumberNodeMinDistance(this.clustters[position], patterns[position], numberNeighbors);
		patterns[position] = Litlepattern.get(gerator.nextInt(Litlepattern.size()));
		return patterns;
	}

	/**
	 * novo modelo para testar todos os vizinhos em busca de uma solução que
	 * domine. este não retorna mais um array de pattern e sim uma pequena lista
	 * de pattern
	 */
	public List<Pattern> changeOneIndexOfMatrixTestingAll(int position, Pattern[] patterns, int numberNeighbors) {// patterns
																													// é
																													// uma
																													// cópia
		// Random gerator = new Random();
		List<Pattern> Litlepattern = new ArrayList<>();
		Litlepattern = takeNnumberNodeMinDistance(this.clustters[position], patterns[position], numberNeighbors);
		// patterns[position] =
		// Litlepattern.get(gerator.nextInt(Litlepattern.size()));
		return Litlepattern;
	}

	/**
	 * muda um elemento da matrix
	 * 
	 * @param solution
	 * @return
	 */

	public IntegerSolution changeMatrixElement(IntegerSolution solution, int numberNeighbors) {// solution
																								// é
																								// uma
																								// cópia
		Random gerator = new Random();
		// isso foi acrescido para que algum cluster de um elemento só não seja selecionado 
		int position= gerator.nextInt(solution.getLineColumn().length);
		while (this.clustters[position].size()<2){
			position= gerator.nextInt(solution.getLineColumn().length);
		}
		
		// muda elemento da matriz
		Pattern[] patterns = changeOneIndexOfMatrix(position,
				solution.getLineColumn().clone(), numberNeighbors);
		solution.setLineColumn(patterns);
		return solution;
	}

	/**
	 * operador de busca local metodo 1
	 * 
	 * @param population
	 * @return population alterada ou não
	 */

	public List<S> localSearch(List<S> population, int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());
		
		//Jorge candeias
		for (Solution s1 : population) {
			// chamada para a busca local
			IntegerSolution s2 = (changeMatrixElement((IntegerSolution) s1.copy(), numberNeighbors));// muda
																										// um
			// elemento
			// IntegerSolution s2 = (changeMatrix((IntegerSolution) s1.copy()));
			// // muda
			// matrix
			// inteira
			this.problem.evaluate((S) s2);

			switch (coparation((IntegerSolution) s1, s2)) {
			case -1:
				copySolution.add((S) s1);
				break;
			case 0:
				copySolution.add((S) s1);
				break;
			case 1:
				copySolution.add((S) s2);
				break;
			default:
				copySolution.add((S) s1);
				System.out.println("deu falha no compara");
				break;
			}

		}

		return copySolution;
	}

	/**
	 * nova abordagem fazer varredura nos vizinhos em busca de um dominante
	 * 
	 */

	public List<S> localSearchTestingAll(List<S> population, int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());
		// voltar aqui
		for (Solution s1 : population) {
			Solution copyInterge = s1.copy();
			Random gerator = new Random();
			int clusterNumber = gerator.nextInt(s1.copy().getLineColumn().length);
			//*****************************************************************
			// esta parte foi add para evitar que cluster de um elemento só seja sorteado
			while (this.clustters[clusterNumber].size()<2){
				clusterNumber= gerator.nextInt(s1.getLineColumn().length);
			}
			//******************************************************************
			// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais próximos
			List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
					s1.copy().getLineColumn().clone(), numberNeighbors);

			// iteração na lista de vizinhos mais próximos
//			int x=0;
			boolean stop=false;
			S sDominator =  (S) s1.copy();
			
			for (Pattern p : Litlepattern) {
				Pattern[] tempPattern = s1.getLineColumn().clone();
				tempPattern[clusterNumber] = p;
				copyInterge.setLineColumn(tempPattern);
				IntegerSolution s2 = (IntegerSolution) copyInterge;

				this.problem.evaluate((S) s2);

				switch (coparation((IntegerSolution) s1, s2)) {
				case -1:
//					System.out.println("entrei em -1e este é x "+x);
					break;
				case 0:
//					System.out.println("entrei em 0 este é x "+x);
					break;
				case 1:
					sDominator=(S)s2.copy();
//					System.out.println("entrei em 1 e este é x "+x);
					stop=true;
					break;
				default:
					System.out.println("deu falha no compara");
					break;
				}
				if(stop) {
					break;
				} 	
//				x+=1;
			}
			copySolution.add((S)sDominator);
		}

		return copySolution;
	}
	
	/**
	 * nova abordagem fazer varredura nos vizinhos em busca de um dominante e do dominante do dominante
	 */
	
	public List<S> localSearchTestingAllAndDontStopUntilArriveInFInalevenFindAFirstDominator(List<S> population, int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());

		for (Solution s1 : population) {
			Solution copyInterge = s1.copy();
			Random gerator = new Random();
			int clusterNumber = gerator.nextInt(s1.copy().getLineColumn().length);
			//*****************************************************************
			// esta parte foi add para evitar que cluster de um elemento só seja sorteado
			while (this.clustters[clusterNumber].size()<2){
				clusterNumber= gerator.nextInt(s1.getLineColumn().length);
			}
			//******************************************************************

			
			// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais próximos
			List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
					s1.copy().getLineColumn().clone(), numberNeighbors);

			// iteração na lista de vizinhos mais próximos
			int x=0;
			S sDominator =  (S) s1.copy();
			
			for (Pattern p : Litlepattern) {
				Pattern[] tempPattern = s1.getLineColumn().clone();
				tempPattern[clusterNumber] = p;
				copyInterge.setLineColumn(tempPattern);
				IntegerSolution s2 = (IntegerSolution) copyInterge;

				this.problem.evaluate((S) s2);

				switch (coparation((IntegerSolution) s1, s2)) {
				case -1:
					System.out.println("entrei em -1e este é x "+x);
					break;
				case 0:
					System.out.println("entrei em 0 este é x "+x);
					break;
				case 1:
					sDominator=(S)s2.copy();// a nova soção assumi o lugar de dominador
					s1=(S)s2.copy();// a nova soção assumi o lugar de S1 para ser comparada com outros vizinhos na busca
					System.out.println("entrei em 1 e este é x "+x);
					break;
				default:
					System.out.println("deu falha no compara");
					break;
				}
		
				x+=1;
			}
			copySolution.add((S)sDominator);
		}

		return copySolution;
		}

	@Override
	protected List<S> selection(List<S> population) {
		
		
		if (this.prop.getProperty("modo").equals("com busca")) {
			int numberNeighbors = Integer.parseInt(this.prop.getProperty("numberNeighbors"));// mudar numero de vizinhos da busca aqui
			if (this.prop.getProperty("modo").equals("com busca")) {
				if (this.prop.getProperty("buscalocal").equals("localSearch")) {
					System.out.println("busca local = buscalocal");
					population = localSearch(population, numberNeighbors);// eu
				}else if(this.prop.getProperty("buscalocal").equals("localSearchTestingAll")) {
					System.out.println("busca local = localSearchTestingAll");
					population = localSearchTestingAll(population, numberNeighbors);// este testar percorrendo os vizinhos ate encontrar o primeiro dominador
				}else {
					System.out.println("busca local = localSearchTestingAllAndDontStopUntilArriveInFInalevenFindAFirstDominator");
					population =localSearchTestingAllAndDontStopUntilArriveInFInalevenFindAFirstDominator(population, numberNeighbors);//este testar percorrendo os vizinhos encontrando dominadores aaté esgotar os vizinhos
				}
			}
			
		}
//		
//		
//		
		List<S> matingPopulation = new ArrayList<>(population.size());
		for (int i = 0; i < getMaxPopulationSize(); i++) {
			S solution = selectionOperator.execute(population);
			matingPopulation.add(solution);
		}

		return matingPopulation;
	}

	// @Override
	// protected List<S> selection(List<S> population) {
	// List<S> matingPopulation = new ArrayList<>(population.size());
	// for (int i = 0; i < getMaxPopulationSize(); i++) {
	// S solution = selectionOperator.execute(population);
	// matingPopulation.add(solution);
	// }
	//
	// return matingPopulation;
	// }

	@Override
	protected List<S> reproduction(List<S> population) {

		List<S> offspringPopulation = new ArrayList<>(getMaxPopulationSize());
		for (int i = 0; i < getMaxPopulationSize(); i += 2) {
			List<S> parents = new ArrayList<>(2);
			parents.add(population.get(i));
			parents.add(population.get(Math.min(i + 1, getMaxPopulationSize() - 1)));

			List<S> offspring = crossoverOperator.execute(parents);

			mutationOperator.execute(offspring.get(0));
			mutationOperator.execute(offspring.get(1));

			offspringPopulation.add(offspring.get(0));
			offspringPopulation.add(offspring.get(1));
		}
		return offspringPopulation;
	}

	private List<ReferencePoint<S>> getReferencePointsCopy() {
		List<ReferencePoint<S>> copy = new ArrayList<>();
		for (ReferencePoint<S> r : this.referencePoints) {
			copy.add(new ReferencePoint<>(r));
		}
		return copy;
	}

	@Override
	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {

		List<S> jointPopulation = new ArrayList<>();
		jointPopulation.addAll(population);
		jointPopulation.addAll(offspringPopulation);

		Ranking<S> ranking = computeRanking(jointPopulation);

		// List<Solution> pop = crowdingDistanceSelection(ranking);
		List<S> pop = new ArrayList<>();
		List<List<S>> fronts = new ArrayList<>();
		int rankingIndex = 0;
		int candidateSolutions = 0;
		while (candidateSolutions < getMaxPopulationSize()) {
			fronts.add(ranking.getSubfront(rankingIndex));
			candidateSolutions += ranking.getSubfront(rankingIndex).size();
			if ((pop.size() + ranking.getSubfront(rankingIndex).size()) <= getMaxPopulationSize())
				addRankedSolutionsToPopulation(ranking, rankingIndex, pop);
			rankingIndex++;
		}

		// A copy of the reference list should be used as parameter of the
		// environmental selection
		EnvironmentalSelection<S> selection = new EnvironmentalSelection<>(fronts, getMaxPopulationSize(),
				getReferencePointsCopy(), getProblem().getNumberOfObjectives());

		pop = selection.execute(pop);

		return pop;
	}

	@Override
	public List<S> getResult() {
		return getNonDominatedSolutions(getPopulation());
	}

	protected Ranking<S> computeRanking(List<S> solutionList) {
		Ranking<S> ranking = new DominanceRanking<>();
		ranking.computeRanking(solutionList);

		return ranking;
	}

	protected void addRankedSolutionsToPopulation(Ranking<S> ranking, int rank, List<S> population) {
		List<S> front;

		front = ranking.getSubfront(rank);
		for (int i = 0; i < front.size(); i++) {
			if (front.get(i).getObjective(3) < 1) {
				population.add(front.get(i));
			}
		}
	}

	protected List<S> getNonDominatedSolutions(List<S> solutionList) {
		return SolutionListUtils.getNondominatedSolutions(solutionList);
	}

	@Override
	public String getName() {
		return "NSGAIII";
	}

	@Override
	public String getDescription() {
		return "Nondominated Sorting Genetic Algorithm version III";
	}

	public int getLocalSeachFoundNoDominated() {
		return LocalSeachFoundNoDominated;
	}

	public void printFinalSolutionSet(List<? extends Solution<?>> population) {
		String path=this.prop.getProperty("local")+this.prop.getProperty("algName")
		+"/"+this.prop.getProperty("modo")+"/"+this.prop.getProperty("execucao"); 
		new SolutionListOutput(population).setSeparator("\t")
				.setVarFileOutputContext(new DefaultFileOutputContext(path+"/"+"VAR" + this.iterations + ".tsv"))
				.setFunFileOutputContext(new DefaultFileOutputContext(path+"/"+"FUN" + this.iterations + ".tsv")).print();

		JMetalLogger.logger.info("Random Seed: " + JMetalRandom.getInstance().getSeed());
		JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
		JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
	}

}
