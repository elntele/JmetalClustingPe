package org.uma.jmetal.algorithm.multiobjective.nsgaiii;

import java.util.ArrayList;
import java.util.List;
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

	protected SolutionListEvaluator<S> evaluator;

	protected Vector<Integer> numberOfDivisions;
	protected List<ReferencePoint<S>> referencePoints = new Vector<>();
	protected GmlData gml;
	protected List<Pattern>[] clustters;
	

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
		System.out.println("numero de iteraçõs"+iterations);
	}

	@Override
	protected boolean isStoppingConditionReached() {
		return iterations >= maxIterations;
	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		population = evaluator.evaluate(population, getProblem());

		return population;
	}

	/**
	 * método pra modificar a matriz inteira mudando cada elemento dela por um
	 * dos 3 patterns mais proximos de forma aleatória
	 */
	public Pattern[] createNewMatrix(Pattern[] lineColumn) {
		Pattern[] copyLineColumn = new Pattern[lineColumn.length];
		Random gerator = new Random();

		for (int i = 0; i < lineColumn.length; i++) {
			copyLineColumn[i] = takeTreNodeMinDistance(this.clustters[i], lineColumn[i]).get(gerator.nextInt(3));
		}
		return copyLineColumn;
	}

	/**
	 * metodo muda toda a matriz de cidades
	 * @param solution
	 * @return
	 */
	
	public IntegerSolution changeMatrix(IntegerSolution solution) {
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
//		if (i == -1) {
//			//System.out.println("s1 domina s2");
//		}
//		if (i == 0) {
//			System.out.println("não há dominação");
//		}
//		if (i == 1) {
//			System.out.println("s2 domina s1");
//		}
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

	public Pattern takeNodeMinDistance(Pattern node, List<Pattern> copyPatternList) {
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

	public List<Pattern> takeTreNodeMinDistance(List<Pattern> patternList, Pattern pattern) {
		List<Pattern> Litlepattern = new ArrayList<>();
		List<Pattern> copyPatternList = new ArrayList<>();
		copyPatternList.addAll(patternList);
		Litlepattern.add(takeNodeMinDistance(pattern, copyPatternList));
		copyPatternList.remove(Litlepattern.get(0));
		Litlepattern.add(takeNodeMinDistance(pattern, copyPatternList));
		copyPatternList.remove(Litlepattern.get(1));
		Litlepattern.add(takeNodeMinDistance(pattern, copyPatternList));
		return Litlepattern;
	}

	/**
	 * Muda um elemento do array matriz
	 * 
	 * @param position
	 * @param patterns
	 * @return
	 */

	public Pattern[] changeOneIndexOfMatrix(int position, Pattern[] patterns) {
		Random gerator = new Random();
		patterns[position] = takeTreNodeMinDistance(this.clustters[position], patterns[position])
				.get(gerator.nextInt(3));
		return patterns;
	}

	/**
	 * muda um elemento da matrix
	 * 
	 * @param solution
	 * @return
	 */

	public IntegerSolution changeMatrixElement(IntegerSolution solution) {
		Random gerator = new Random();
		// teste muda elemento da matriz
		Pattern[] patterns = changeOneIndexOfMatrix(gerator.nextInt(solution.getLineColumn().length),
				solution.getLineColumn());
		solution.setLineColumn(patterns.clone());
		return solution;
	}

	/**
	 * operador de busca local metodo 1
	 * 
	 * @param population
	 * @return population alterada ou não
	 */

	public List<S> localSearch(List<S> population) {
		List<S> copySolution = new ArrayList<>(population.size());
		int cont = 0;
		int resultado;
		for (Solution s1 : population) {
			// chamada para a busca local
//			IntegerSolution s2 = (changeMatrixElement((IntegerSolution)s1.copy()));// muda um elemento
			IntegerSolution s2 = (changeMatrix((IntegerSolution) s1)); // muda matrix inteira
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

		cont += 1;

		return copySolution;
	}

	@Override
	protected List<S> selection(List<S> population) {
		
		population = localSearch(population);// eu
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

}
