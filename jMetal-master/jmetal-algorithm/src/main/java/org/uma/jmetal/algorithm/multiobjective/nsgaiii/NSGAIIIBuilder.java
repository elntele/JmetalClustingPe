package org.uma.jmetal.algorithm.multiobjective.nsgaiii;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.AlgorithmBuilder;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.ParallelSolutionListEvaluate;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SeverAndId;

import br.cns.model.GmlData;
import cbic15.Pattern;

/** Builder class */
public class NSGAIIIBuilder<S extends Solution<?>> implements AlgorithmBuilder<NSGAIII<S>> {
	// no access modifier means access from classes within the same package
	private Problem<S> problem;
	private int maxIterations;
	private int populationSize;
	private CrossoverOperator<S> crossoverOperator;
	private MutationOperator<S> mutationOperator;
	private SelectionOperator<List<S>, S> selectionOperator;
	private GmlData gml;
	private List<Pattern>[] clustters;
	private Properties prop;
	//organizae e deletar
//	private List <UUID> ParallelEvaluateId;
	List <SeverAndId> severAndIdList;
	private SolutionListEvaluator<S> evaluator;
	private SolutionListEvaluator<S> parallelEvaluator;

	
	
	/** Builder constructor */
	public NSGAIIIBuilder(Problem<S> problem, GmlData gml, List<Pattern>[] clustters, Properties prop, List <SeverAndId> severAndIdList) {
		this.problem = problem;
		maxIterations = 250;
		populationSize = 100;
		evaluator = new SequentialSolutionListEvaluator<S>();
		parallelEvaluator = new ParallelSolutionListEvaluate<S>(severAndIdList);
		this.gml = gml;
		this.clustters = clustters;
		this.prop=prop;
		severAndIdList=severAndIdList;
	}
	
	




	public SolutionListEvaluator<S> getParallelEvaluator() {
		return parallelEvaluator;
	}






	public void setParallelEvaluator(SolutionListEvaluator<S> parallelEvaluator) {
		this.parallelEvaluator = parallelEvaluator;
	}





//
//	public List<UUID> getParallelEvaluateId() {
//		return ParallelEvaluateId;
//	}
//
//
//
//
//
//
//	public void setParallelEvaluateId(List<UUID> parallelEvaluateId) {
//		ParallelEvaluateId = parallelEvaluateId;
//	}
//
//




	public Properties getProp() {
		return prop;
	}



	public List<Pattern>[] getClustters() {
		return clustters;
	}

	public GmlData getGml() {
		return gml;
	}

	public NSGAIIIBuilder<S> setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;

		return this;
	}

	public NSGAIIIBuilder<S> setPopulationSize(int populationSize) {
		this.populationSize = populationSize;

		return this;
	}

	public NSGAIIIBuilder<S> setCrossoverOperator(CrossoverOperator<S> crossoverOperator) {
		this.crossoverOperator = crossoverOperator;

		return this;
	}

	public NSGAIIIBuilder<S> setMutationOperator(MutationOperator<S> mutationOperator) {
		this.mutationOperator = mutationOperator;

		return this;
	}

	public NSGAIIIBuilder<S> setSelectionOperator(SelectionOperator<List<S>, S> selectionOperator) {
		this.selectionOperator = selectionOperator;

		return this;
	}

	public NSGAIIIBuilder<S> setSolutionListEvaluator(SolutionListEvaluator<S> evaluator) {
		this.evaluator = evaluator;

		return this;
	}

	public SolutionListEvaluator<S> getEvaluator() {
		return evaluator;
	}

	public Problem<S> getProblem() {
		return problem;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public int getPopulationSize() {
		return populationSize;
	}

	public CrossoverOperator<S> getCrossoverOperator() {
		return crossoverOperator;
	}

	public MutationOperator<S> getMutationOperator() {
		return mutationOperator;
	}

	public SelectionOperator<List<S>, S> getSelectionOperator() {
		return selectionOperator;
	}

	public NSGAIII<S> build() {
		return new NSGAIII<>(this);
	}
}
