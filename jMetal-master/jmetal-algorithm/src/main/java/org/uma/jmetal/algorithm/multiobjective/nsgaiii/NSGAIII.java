package org.uma.jmetal.algorithm.multiobjective.nsgaiii;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.AnIndividualAndHisVector;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.HyperplaneObsevation;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.MetricEvalRunTime;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.ReferencePoint;
import org.uma.jmetal.gmlNetwaork.PatternToGml;
import org.uma.jmetal.solution.IntegerSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.solutionattribute.Ranking;
import org.uma.jmetal.util.solutionattribute.impl.DominanceRanking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	protected int cont;

	protected SolutionListEvaluator<S> evaluator;
	protected SolutionListEvaluator<S> parallelEvaluator;
	protected Vector<Integer> numberOfDivisions;
	protected List<ReferencePoint<S>> referencePoints = new Vector<>();
	protected GmlData gml;
	protected List<Pattern>[] clustters;
	private EnvironmentalSelection envi;
	private int LocalSeachFoundNoDominated = 0;
	private int localSeachEvaluateCount = 0;
	private UUID ParallelEvaluateId;
	private PatternToGml ptg;
	private List<S> betterPareto;// adiononado por jorge candeias para pegar o front menos dominado
	private List<List<S>> paretos = new ArrayList<>();
	private List<S> lastPareto;
	private HyperplaneObsevation hp; // add por jorge candeias
	private List<List<Integer>> indexOfIndividualSelectionedToTheSearch = new ArrayList<>();
	private List<List<Integer>> EqualizadListe = new ArrayList<>();
	private boolean iDidTheFirstTimeAfterInciationFromAStopedExecution = false;
	private List<Integer> iDidDominate = new ArrayList<>();
	private List<List<S>> fronts = new ArrayList<>();// esse fio inserido para o tratamento de controle de reprodução
	private List<Boolean> doNormatization = new ArrayList<>();
	private List<Double> listW = new ArrayList<>();
	private List<Integer> fitnessPrint = new ArrayList<>();
	private List<List<Double>> tracksync = new ArrayList<>();
	private int rebootCount = 0;
	private int maxEvaluate=0;

	/** Constructor */
	public NSGAIII(NSGAIIIBuilder<S> builder) { // can be created from the
												// NSGAIIIBuilder within the
												// same package
		super(builder.getProblem());
//		this.ParallelEvaluateId = builder.getParallelEvaluateId();
		this.gml = builder.getGml();
		this.clustters = builder.getClustters();
		maxIterations = builder.getMaxIterations();
		crossoverOperator = builder.getCrossoverOperator();
		mutationOperator = builder.getMutationOperator();
		selectionOperator = builder.getSelectionOperator();
		parallelEvaluator = builder.getParallelEvaluator();
		evaluator = builder.getEvaluator();
		ptg = builder.getPtg();
		prop = builder.getProp();
		doNormatization();
		initializaArrayMarksForFitnessPrint();

		/// NSGAIII
		numberOfDivisions = new Vector<>(1);
		numberOfDivisions.add(12); // Default value for 3D problems (is 12 never forget jorge)

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

	/**
	 * method was created to initialize a list of marks to print results accord of
	 * number of fitness function
	 */

	public void initializaArrayMarksForFitnessPrint() {
		if (this.prop.getProperty("resultForFitness").equals("y")) {
			int interview = Integer.parseInt(this.prop.getProperty("fitnessInterval"));
			int maxEvaluate = Integer.parseInt(this.prop.getProperty("maxEvaluate"));
			this.maxEvaluate=maxEvaluate;
			int timeToPrint = maxEvaluate / interview;
			int initialPoint = interview;
			for (int i = 0; i < timeToPrint; i++) {
				this.fitnessPrint.add(initialPoint);
				initialPoint += interview;
			}
		}
	}

	/**
	 * method created to set a boolean arraylist according the objective that needs
	 * be normatized when calculated the hipervolume Author: jorge candeias
	 */
	public void doNormatization() {
		String doNormatization[] = prop.getProperty("doNormatization").split(",");
		for (String s : doNormatization) {
			if (s.equals("1")) {
				this.doNormatization.add(true);
			} else {
				this.doNormatization.add(false);
			}
		}

		for (int i = 0; i < this.doNormatization.size(); i++) {
			if (this.doNormatization.get(i) == false) {
				this.listW.add(i, 1.0);
			} else {
				this.listW.add(i, Double.NEGATIVE_INFINITY);
			}
		}

	}

	@Override
	protected void initProgress() {
		if (this.prop.get("startFromAstopedIteration").equals("y")) {
			iterations = Integer.parseInt(this.prop.getProperty("interationStopedInExecution"));

		} else {
			iterations = 1;
		}
	}

	/**
	 * author jorge candeias method created to reboot of algorithm
	 */
	public void reboot() {
		this.tracksync.removeAll(this.tracksync);
		this.rebootCount += 1;
		System.out.println("there is have a reboot: create a initial population");
		this.prop.setProperty("startFromAstopedIteration", "n");
		this.population = this.createInitialPopulation();
		System.out.println("evaluate the new population after reboot pivotement");
		population = this.evaluatePopulation(population);
		this.initProgress();

		if (this.rebootCount > Integer.parseInt(this.prop.getProperty("rebootnumber"))) {
			this.prop.setProperty("trackSynchronization", "n");
			this.prop.setProperty("modo", "sem busca");
			this.prop.setProperty("populationMatingControl", "n");

		}

	}

	/**
	 * author jorge candeias method created to analyze the Synchronization of
	 * population, if the Synchronization not good, this method call a reboot of
	 * algorithm
	 */

	public void trackSynchronization() {
		if (this.iterations >= Integer.parseInt(this.prop.getProperty("begintrack"))) {
			if (this.iterations <= Integer.parseInt(this.prop.getProperty("endtrack"))) {
				this.tracksync.add(this.hp.geteQualizationList());
			}

		}
		if (this.iterations == Integer.parseInt(this.prop.getProperty("endtrack"))) {
			Variance v = new Variance(false);
			List<Double> controlSync = new ArrayList<>();
			for (List<Double> l : this.tracksync) {
				double[] a = new double[l.size()];
				for (int i = 0; i < a.length; i++) {
					a[i] = l.get(i);
				}
				double variancia = v.evaluate(a);
				controlSync.add(variancia);
			}
			boolean pivotement = false;
			boolean arriveInlower = false;
			int passedTheUpperLimit = 0;
			for (double d : controlSync) {
				if (d < Double.parseDouble(this.prop.getProperty("lowerObjectve"))) {
					arriveInlower = true;
				}
				if (d > Double.parseDouble(this.prop.getProperty("upperSyncLimit"))) {
					passedTheUpperLimit += 1;
				}
			}
			if (!arriveInlower || passedTheUpperLimit > Integer.parseInt(this.prop.getProperty("timeToExceed"))) {
				reboot();
			} else if (this.rebootCount > 0) {
				this.iterations = Integer.parseInt(this.prop.getProperty("endtrack"))
						+ Integer.parseInt(this.prop.getProperty("endtrack")) * this.rebootCount;

//				correctSaving();
			}
		}

	}

	@Override
	protected void updateProgress() {
		iterations++;
		System.out.println("numero de iterações " + iterations);

		if (this.prop.getProperty("resultForFitness").equals("y")) {
			int fitnessCount= this.iterations * this.population.size() + this.localSeachEvaluateCount;
			if (fitnessCount >= this.fitnessPrint.get(0)) {
				printforFitness(this.population);
				if (fitnessCount>=this.maxEvaluate) {
					this.iterations=maxIterations;
				}
			}

		} else {
			if (this.iterations % 10 == 0 || iterations == 198 || iterations == 202) {
				printFinalSolutionSet(this.population);
			}
		}

		if (this.prop.getProperty("trackSynchronization").equals("y") && this.fronts.size() == 1) {
			trackSynchronization();
		}

	}

	@Override
	protected boolean isStoppingConditionReached() {
		return iterations >= maxIterations;
	}

	/**
	 * método que alterei pra gerar um aqruivo .tsv com 456 redes aleatorias resolvi
	 * deixar comentada
	 */
//	@Override
//	protected List<S> evaluatePopulation(List<S> population) {
//		population = evaluator.evaluate(population, getProblem());
//		String path="C:\\Users\\elnte\\OneDrive\\Área de Trabalho\\fixedSolution18Pops.tsv";
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
	/**
	 * função criada para o processamento dormir nos fds durante o dia quando o ar
	 * condicionado estará desligado e estará mais quente.
	 * 
	 * @throws InterruptedException
	 */
	public void delay() throws InterruptedException {
		Date now = new Date();

		SimpleDateFormat simpleDateformat = new SimpleDateFormat("E"); // the day of the week abbreviated
		String dia = simpleDateformat.format(now);
		if (dia.equals("sáb") || dia.equals("dom")) {
			Calendar agora = new GregorianCalendar();
			int nowHour = agora.get(Calendar.HOUR);
			if (nowHour == 8) {
				System.out.println("são mais que 8 horas");
				TimeUnit.HOURS.sleep(11);
			}

		}

	}

	@Override
	protected List<S> evaluatePopulation(List<S> population) {
		/*
		 * try { delay();// fução criada por jorge } catch (InterruptedException e) { //
		 * TODO Auto-generated catch block e.printStackTrace(); }
		 */
		if (this.prop.getProperty("parallelFitness").equals("y")) {
			try {
				return population = parallelEvaluator.evaluate(population, getProblem());
			} catch (Exception e) {
				// avalie local
				return population = evaluator.evaluate(population, getProblem());
			}
		} else {
			// avalie local
			return population = evaluator.evaluate(population, getProblem());
		}

	}

	protected List<S> evaluatePopulationparallel(List<S> population) {
		Socket soc = null;
		ObjectMapper mapper = new ObjectMapper();
		String textOut = null;
		try {
			textOut = mapper.writeValueAsString(population);
			List<String> l = new ArrayList<>();
			l.add((this.ParallelEvaluateId).toString());
			l.add(textOut);
			textOut = mapper.writeValueAsString(l);

		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String adress = "localhost";
		try {
			int serverPort = 7896;
			soc = new Socket(adress, serverPort);
			DataInputStream in = new DataInputStream(soc.getInputStream());
			DataOutputStream out = new DataOutputStream(soc.getOutputStream());

//			int length = out.readInt(); // read length of incoming message

			byte[] b = textOut.getBytes(StandardCharsets.UTF_8);
			out.writeInt(b.length); // write length of the message
			out.write(b);
			// retorno
			int length = in.readInt();
			String data = null;
			if (length > 0) {
				byte[] message = new byte[length];
				in.readFully(message, 0, message.length); // read the message
				data = new String(message, StandardCharsets.US_ASCII);
//				System.out.println("mensagem aqui " + s);
			}
			// String data = in.readUTF(); // read a line of data from the stream
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			List<DefaultIntegerSolution> pReturned = new ArrayList<>();

			pReturned = mapper.readValue(data, new TypeReference<List<DefaultIntegerSolution>>() {
			});
			// testEqualityBetweenOriginalSolutionAndReturnOfParallelEvaluate(pr,population);
			int i = 0;
			for (DefaultIntegerSolution s : pReturned) {
				for (int numberOfobjetive = 0; numberOfobjetive < this.problem
						.getNumberOfObjectives(); numberOfobjetive++) {
					population.get(i).setObjective(numberOfobjetive, s.getObjective(numberOfobjetive));
				}
				i += 1;
			}

		} catch (UnknownHostException e) {
			System.out.println("Socket:" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("readline:" + e.getMessage());
		} finally {
			if (soc != null)
				try {
					soc.close();
				} catch (IOException e) {
					System.out.println("close:" + e.getMessage());
				}
		}

		return population;

	}

//	public void otherTestEqaulsSolution(DefaultIntegerSolution ITs, S oS) {
//		boolean one =ITs.getvariables().toString().equals(oS.getva);
//	}

	public void testEqualityBetweenOriginalSolutionAndReturnOfParallelEvaluate(List<DefaultIntegerSolution> pReturned,
			List<S> population) {
		int i = 0;
		for (DefaultIntegerSolution VariableFromRetornedSolution : pReturned) {
			String stringVariableFromOriginalPopulation = "[";
			for (int k = 0; k < this.problem.getNumberOfVariables(); k++) {
				if (k == 0) {
					stringVariableFromOriginalPopulation += population.get(i).getVariableValueString(k);
				} else {
					stringVariableFromOriginalPopulation += ", " + population.get(i).getVariableValueString(k);
				}
			}
			stringVariableFromOriginalPopulation += "]";
			boolean a = VariableFromRetornedSolution.getvariables().toString()
					.equals(stringVariableFromOriginalPopulation);
			boolean b = (VariableFromRetornedSolution.getLineColumn().toString()
					.equals(population.get(i).getLineColumn().toString()));
			System.out.println("as soluções são iguais ? " + (a && b));
			// .out.println(s.getVariableValue(1));
			i += 1;
		}

	}

	/**
	 * método pra modificar a matriz inteira mudando cada elemento dela por um dos 3
	 * patterns mais proximos de forma aleatória
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
	 * @param solutio1 e solution2
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
		this.localSeachEvaluateCount += 1;
		return i;
	}

	/**
	 * metodo recebe um nó e uma lista de nós e retorna o nó da lista mais próximo
	 * ao nó parametro
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
		if (numberNeighbors > maxNumberNeighbors) { // garante que o numero
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
	 * novo modelo para testar todos os vizinhos em busca de uma solução que domine.
	 * este não retorna mais um array de pattern e sim uma pequena lista de pattern
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
		// isso foi acrescido para que algum cluster de um elemento só não seja
		// selecionado
		int position = gerator.nextInt(solution.getLineColumn().length);
		while (this.clustters[position].size() < 2) {
			position = gerator.nextInt(solution.getLineColumn().length);
		}

		// muda elemento da matriz
		Pattern[] patterns = changeOneIndexOfMatrix(position, solution.getLineColumn().clone(), numberNeighbors);
		solution.setLineColumn(patterns);
		return solution;
	}

	/**
	 * metodo crado para auxiliar bringMeBetterObjectives ele verifica se o indice
	 * em questão esta em um array e retorna um booleano
	 * 
	 * @return
	 */
	public boolean dontContainIndice(Integer[] b, int indice) {
		boolean retorno = true;
		for (int i = 0; i < b.length; i++) {
			try {
				if (b[i] == indice) {
					retorno = false;
					break;
				}
			} catch (Exception e) {
				break;
			}

		}

		return retorno;
	}

	/**
	 * metodo receve os k objetivos em a lista onde terá que trabalhar e returnna
	 * uam lista de indice desses objetivos na população
	 * 
	 * @param arrayObjetiveValueLower
	 * @param popReceived
	 * @return
	 */

	public Integer[] bringMeTheIndiceOfIndividualsWhithBetterObjectives(int individualsNumber, List<S> popReceived,
			int slice, List<S> populationParateste) {
		int deslocamento = 0;

		if (slice == 0) {
			deslocamento = 0;
		} else if (slice > 1) {
			for (int i = 0; i < slice - 1; i++) {
				deslocamento = deslocamento + this.paretos.get(i).size();
			}
		}

		Integer[] arrayIndice = new Integer[individualsNumber];
		Double[] arrayObjetiveBetterValue = new Double[individualsNumber];

		for (int i = 0; i < arrayObjetiveBetterValue.length; i++) {
			arrayObjetiveBetterValue[i] = Double.MAX_VALUE;
		}

		int numberOfObjetive = 0;
		for (int i = 0; i < individualsNumber; i++) {
			for (int k = 0; k < popReceived.size(); k++) {

				if (numberOfObjetive == this.problem.getNumberOfObjectives()) {
					numberOfObjetive = 0;
				}

				if (numberOfObjetive == 3) {
					double inv = popReceived.get(k).getObjective(numberOfObjetive);
					inv = 1 / (1 + inv);
					if (inv < arrayObjetiveBetterValue[i] && dontContainIndice(arrayIndice, k)) {
						arrayObjetiveBetterValue[i] = popReceived.get(k).getObjective(numberOfObjetive);
						arrayIndice[i] = k;
					}

				} else if (popReceived.get(k).getObjective(numberOfObjetive) < arrayObjetiveBetterValue[i]
						&& dontContainIndice(arrayIndice, k)) {
					arrayObjetiveBetterValue[i] = popReceived.get(k).getObjective(numberOfObjetive);
					arrayIndice[i] = k;
				}

			}
			numberOfObjetive += 1;
		}

		// se for usra o teste comentar esse for
		for (int i = 0; i < arrayIndice.length; i++) {
			arrayIndice[i] += deslocamento;
		}

//		teste: tem que alterar o recebimento do método pra receber a populacao tambem
//		no teste comentado abaixo a chamei de populationParateste e recebi ela como parametro
//		do metodo (obs: tem que comentar o for acima pos ele entra na logica logo abaixo):
//		Integer[] copy = arrayIndice.clone();
//
//		for (int i = 0; i < arrayIndice.length; i++) {
//			arrayIndice[i] += deslocamento;
//		}
//		for (int i = 0; i < copy.length; i++) {
//			List<DefaultIntegerSolution> population = new ArrayList<>();
//			List<S> listaEmTeste = new ArrayList<>();
//			population.add((DefaultIntegerSolution) populationParateste.get(arrayIndice[i]));
//			listaEmTeste.add(popReceived.get(copy[i]));
//			this.testEqualityBetweenOriginalSolutionAndReturnOfParallelEvaluate(population, listaEmTeste);
//		}
		return arrayIndice;
	}

	/**
	 * metodo recebe a população e uma lista com n-numerofobjetives com o maior
	 * valor de doubler e retorna um array com o indice das 4 soluções da população
	 * com menores valores de objetivo
	 * 
	 * @param arrayObjetiveValueLower
	 * @param popReceived
	 * @return
	 */

	public Integer[] takeNLowerSolutiox(Double[] arrayObjetiveValueLower, List<S> popReceived, int slice,
			List<S> populationParateste) {
		int deslocamento = 0;

		if (slice == 0) {
			deslocamento = 0;
		} else if (slice > 1) {
			for (int i = 0; i < slice - 1; i++) {
				deslocamento = deslocamento + this.paretos.get(i).size();
			}
		}

		Integer[] arrayIndice = new Integer[this.problem.getNumberOfObjectives()];
		for (int i = 0; i < popReceived.size(); i++) {
			if (popReceived.get(i).getObjective(0) < arrayObjetiveValueLower[0]) {
				arrayObjetiveValueLower[0] = popReceived.get(i).getObjective(0);
				arrayIndice[0] = i;
			}
		}
		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(1) < arrayObjetiveValueLower[1]) && (arrayIndice[0] != i)) {
				arrayObjetiveValueLower[1] = popReceived.get(i).getObjective(1);
				arrayIndice[1] = i;
			}
		}

		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(2) < arrayObjetiveValueLower[2]) && (arrayIndice[0] != i)
					&& (arrayIndice[1] != i)) {
				arrayObjetiveValueLower[2] = popReceived.get(i).getObjective(2);
				arrayIndice[2] = i;
			}
		}

		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(3) < arrayObjetiveValueLower[3]) && (arrayIndice[0] != i)
					&& (arrayIndice[1] != i) && (arrayIndice[2] != i)) {
				arrayObjetiveValueLower[3] = popReceived.get(i).getObjective(3);
				arrayIndice[3] = i;
			}
		}
//		for (int i = 0; i < arrayIndice.length; i++) {
//			arrayIndice[i] += deslocamento;
//		}

//		teste: tem que alterar o recebimento do método pra receber a populacao tambem
//		no teste comentado abaixo a chamei de populationParateste e recebi ela como parametro
//		do metodo (obs: tem que comentar o for acima pos ele entra na logica logo abaixo):
		Integer[] copy = arrayIndice.clone();

		for (int i = 0; i < arrayIndice.length; i++) {
			arrayIndice[i] += deslocamento;
		}
		for (int i = 0; i < copy.length; i++) {
			List<DefaultIntegerSolution> population = new ArrayList<>();
			List<S> listaEmTeste = new ArrayList<>();
			population.add((DefaultIntegerSolution) populationParateste.get(arrayIndice[i]));
			listaEmTeste.add(popReceived.get(copy[i]));
			this.testEqualityBetweenOriginalSolutionAndReturnOfParallelEvaluate(population, listaEmTeste);
		}

		return arrayIndice;
	}

	/**
	 * metodo recebe a população e uma lista com n-numerofobjetives com o menor
	 * valor de doubler e um arrey contendo os indices das suluções com menores
	 * valores na população, a inteção de receber esse array é so pra não repetir os
	 * indices que já foram escolhidos como os indices das soluções com menores
	 * valor retorna um array com os indice das 4 soluções da população com maiores
	 * valores de objetivo
	 * 
	 * @param arrayObjetiveValueLower
	 * @param popReceived
	 * @return
	 */
	public Integer[] takeNUpperSolutiox(Double[] arrayObjetiveValueUpper, List<S> popReceived, Integer[] Lowers,
			int slice, List<S> populationParateste) {
		int deslocamento = 0;

		if (slice == 0) {
			deslocamento = 0;
		} else if (slice > 1) {
			for (int i = 0; i < slice - 1; i++) {
				deslocamento = deslocamento + this.paretos.get(i).size();
			}
		}

		Integer[] arrayIndiceUpper = new Integer[this.problem.getNumberOfObjectives()];
		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(0) > arrayObjetiveValueUpper[0]) && (i != Lowers[0])
					&& (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3])) {
				arrayObjetiveValueUpper[0] = popReceived.get(i).getObjective(0);
				arrayIndiceUpper[0] = i;
			}
		}

		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(1) > arrayObjetiveValueUpper[1]) && (i != Lowers[0])
					&& (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3]) && (arrayIndiceUpper[0] != i)) {
				arrayObjetiveValueUpper[1] = popReceived.get(i).getObjective(1);
				arrayIndiceUpper[1] = i;
			}
		}

		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(2) > arrayObjetiveValueUpper[2]) && (i != Lowers[0])
					&& (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3]) && (arrayIndiceUpper[0] != i)
					&& (arrayIndiceUpper[1] != i)) {
				arrayObjetiveValueUpper[2] = popReceived.get(i).getObjective(2);
				arrayIndiceUpper[2] = i;
			}
		}

		for (int i = 0; i < popReceived.size(); i++) {
			if ((popReceived.get(i).getObjective(3) > arrayObjetiveValueUpper[3]) && (i != Lowers[0])
					&& (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3]) && (arrayIndiceUpper[0] != i)
					&& (arrayIndiceUpper[1] != i) && (arrayIndiceUpper[2] != i)) {
				arrayObjetiveValueUpper[3] = popReceived.get(i).getObjective(3);
				arrayIndiceUpper[3] = i;
			}
		}

//		teste: tem que alterar o recebimento do método pra receber a populacao tambem
//		no teste comentado abaixo a chamei de populationParateste e recebi ela como parametro
//		do metodo (obs: tem que comentar o for acima pos ele entra na logica logo abaixo):
		Integer[] copy = arrayIndiceUpper.clone();

		for (int i = 0; i < arrayIndiceUpper.length; i++) {
			arrayIndiceUpper[i] += deslocamento;
		}
		for (int i = 0; i < copy.length; i++) {
			List<DefaultIntegerSolution> population = new ArrayList<>();
			List<S> listaEmTeste = new ArrayList<>();
			population.add((DefaultIntegerSolution) populationParateste.get(arrayIndiceUpper[i]));
			listaEmTeste.add(popReceived.get(copy[i]));
			this.testEqualityBetweenOriginalSolutionAndReturnOfParallelEvaluate(population, listaEmTeste);
		}

		return arrayIndiceUpper;
	}

//	/**
//	 * metodo criado para acrescetar ou retirar link a duas 
//	 * cidades, complementando a busca local
//	 * 
//	 */
//	public DefaultIntegerSolution putRemoveEdge(DefaultIntegerSolution s2) {
//		
//		Random gerator = new Random();
//		int cit1 =0;
//		int cit2 =0;
//		int numberOfCities= s2.getLineColumn().length;
//		while (cit1==cit2) {
//			 cit1= gerator.nextInt((numberOfCities - 1) + 1) + 1;
//			 cit2= gerator.nextInt((numberOfCities - 1) + 1) + 1;	
//			
//		}
//		int matrixLine=Math.min(cit1, cit2);
//		int matrixCollumn=Math.max(cit1, cit2);
//		 int chromosomePosition =0;
//		 int collumn= matrixCollumn-1;
//		 int offset=matrixLine;
//		 for (int i=1; i<matrixLine; i++) {
//			 chromosomePosition+= numberOfCities-i;
//		 }
//		 chromosomePosition+=matrixCollumn-offset;
//		int decision=gerator.nextInt(101);
//		if (/*decision>50*/ true) {
//			
//			int currently =s2.getVariableValue(chromosomePosition-1);
//			if (currently==0) {
//				System.out.println("link acrescentado");
//				s2.setVariableValue(chromosomePosition-1, 1);
//			}else {
//				System.out.println("link remov1do");
//				s2.setVariableValue(chromosomePosition-1, 0);
//			}
//		}
//		return s2;
//	}

	/**
	 * metodo criado para acrescetar ou retirar link a duas cidades, complementando
	 * a busca local
	 * 
	 */
	public DefaultIntegerSolution putRemoveEdge(DefaultIntegerSolution s2) {

		Random gerator = new Random();
		int pos = 0;
		int max = 0;
		boolean continueTrue = true;
		while (continueTrue || max > this.problem.getNumberOfVariables()) {
			pos = gerator.nextInt(this.problem.getNumberOfVariables());
			if (s2.getVariableValue(pos) == 0) {
				continueTrue = false;
				s2.setVariableValue(pos, 1);
				System.out.println("Acrescentei um link");

			} else {
				continue;
			}

			max += 1;
		}

		return s2;
	}

// quando era 8 individuos pior e melhor
//	/**
//	 * operador de busca local metodo 1
//	 * 
//	 * @param population
//	 * @return population alterada ou não
//	 */
//
//	public List<S> localSearch(List<S> population, int numberNeighbors) {
//		List<S> copySolution = new ArrayList<>(population.size());
//		if (prop.getProperty("eliteSearch").equals("y")) {
//			Double[] arrayObjetiveValueLower = new Double[this.problem.getNumberOfObjectives()];
//			Double[] arrayObjetiveValueUpper = new Double[this.problem.getNumberOfObjectives()];
//
//			for (int i = 0; i < arrayObjetiveValueLower.length; i++) {
//				arrayObjetiveValueLower[i] =Double.MIN_VALUE;
//				arrayObjetiveValueUpper[i] = Double.MAX_VALUE;
//			}
//
//			Integer[] arrayIndiceLower = takeNLowerSolutiox(arrayObjetiveValueUpper.clone(), population);
//			Integer[] arrayIndiceUpper = takeNUpperSolutiox(arrayObjetiveValueLower.clone(), population,
//					arrayIndiceLower.clone());
//
//			// Jorge candeias
//
//			for (int i = 0; i < population.size(); i++) {
//				// chamada para a busca local
//				if (i == arrayIndiceLower[0] || i == arrayIndiceLower[1] || i == arrayIndiceLower[2]
//						|| i == arrayIndiceLower[3] || i == arrayIndiceUpper[0] || i == arrayIndiceUpper[1]
//						|| i == arrayIndiceUpper[2] || i == arrayIndiceUpper[3]) {
//					System.out.println("Solução top indice " + i);
//					IntegerSolution s2 = (changeMatrixElement((IntegerSolution) population.get(i).copy(),
//							numberNeighbors));// muda
//					this.problem.evaluate((S) s2);
//
//					switch (coparation((IntegerSolution) population.get(i), s2)) {
//					case -1:
//						copySolution.add((S) population.get(i));
//						break;
//					case 0:
//						copySolution.add((S) population.get(i));
//						break;
//					case 1:
//						copySolution.add((S) s2);
//						break;
//					default:
//						copySolution.add((S) population.get(i));
//						System.out.println("deu falha no compara");
//						break;
//					}
//
//				} else {
//					copySolution.add((S) population.get(i));
//
//				}
//
//			}
//		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {
//
//			Random gerator = new Random();
//
//			Integer[] arrayIndices = new Integer[this.problem.getNumberOfObjectives() * 2];
//
//			for (int i = 0; i < arrayIndices.length; i++) {
//				arrayIndices[i] = gerator.nextInt(population.size()-1);
//			}
//
//			for (int i = 0; i < population.size(); i++) {
//				// chamada para a busca local
//				if (i == arrayIndices[0] || i == arrayIndices[1] || i == arrayIndices[2] || i == arrayIndices[3]
//						|| i == arrayIndices[4] || i == arrayIndices[5] || i == arrayIndices[6]
//						|| i == arrayIndices[7]) {
//					System.out.println("Solução da escolha randomica indice " + i);
//					IntegerSolution s2 = (changeMatrixElement((IntegerSolution) population.get(i).copy(),
//							numberNeighbors));// muda
//					this.problem.evaluate((S) s2);
//
//					switch (coparation((IntegerSolution) population.get(i), s2)) {
//					case -1:
//						copySolution.add((S) population.get(i));
//						break;
//					case 0:
//						copySolution.add((S) population.get(i));
//						break;
//					case 1:
//						copySolution.add((S) s2);
//						break;
//					default:
//						copySolution.add((S) population.get(i));
//						System.out.println("deu falha no compara");
//						break;
//					}
//
//				} else {
//					copySolution.add((S) population.get(i));
//
//				}
//
//			}
//
//		} else {
//
//			for (Solution s1 : population) {
//				// chamada para a busca local
//				IntegerSolution s2 = (changeMatrixElement((IntegerSolution) s1.copy(), numberNeighbors));// muda
//																											// um
//				// elemento
//				// IntegerSolution s2 = (changeMatrix((IntegerSolution) s1.copy()));
//				// // muda
//				// matrix
//				// inteira
//				this.problem.evaluate((S) s2);
//
//				switch (coparation((IntegerSolution) s1, s2)) {
//				case -1:
//					copySolution.add((S) s1);
//					break;
//				case 0:
//					copySolution.add((S) s1);
//					break;
//				case 1:
//					copySolution.add((S) s2);
//					break;
//				default:
//					copySolution.add((S) s1);
//					System.out.println("deu falha no compara");
//					break;
//				}
//
//			}
//		}
//
//		return copySolution;
//	}

	/**
	 * operador de busca local metodo 1
	 * 
	 * @param population
	 * @return population alterada ou não
	 */

	public List<S> localSearch(List<S> population, int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());
		if (prop.getProperty("eliteSearch").equals("y")) {

			List<Integer> individualsToLocSearc = this.hp.selectTheCandidatesTolocalsearch(this.prop);
			this.indexOfIndividualSelectionedToTheSearch.add(individualsToLocSearc);
			System.out.println("soluções que atendem a regra " + this.hp.getTesteSolQueAtendExecge());
			System.out.println("soluções que não atendem  a regra " + this.hp.getTesteSolQueNaoAtendExecge());

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (individualsToLocSearc.contains(i)) {
					System.out.println("Solução top indice " + i);
					IntegerSolution s2 = (changeMatrixElement((IntegerSolution) population.get(i).copy(),
							numberNeighbors));// muda
					// colocar a nova abordagem da busca local aqui
					if (this.prop.getProperty("theSearch").equals("new")) {
						s2 = putRemoveEdge((DefaultIntegerSolution) s2);
					}

					this.problem.evaluate((S) s2);

					switch (coparation((IntegerSolution) population.get(i), s2)) {
					case -1:
						copySolution.add((S) population.get(i));
						break;
					case 0:
						copySolution.add((S) population.get(i));
						break;
					case 1:
						copySolution.add((S) s2);
						this.iDidDominate.add(i);// novo
						break;
					default:
						copySolution.add((S) population.get(i));
						System.out.println("deu falha no compara");
						break;
					}

				} else {
					copySolution.add((S) population.get(i));

				}

			}
		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {

			Random gerator = new Random();

			List<Integer> individualsToLocSearc = new ArrayList<>();

			for (int i = 0; i < Integer.parseInt(this.prop.getProperty("nIndividuosToSearch")); i++) {
				individualsToLocSearc.add(gerator.nextInt(population.size() - 1));
			}

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (individualsToLocSearc.contains(i)) {
					System.out.println("Solução da escolha randomica indice " + i);
					IntegerSolution s2 = (changeMatrixElement((IntegerSolution) population.get(i).copy(),
							numberNeighbors));// muda

					if (this.prop.getProperty("theSearch").equals("new")) {
						s2 = putRemoveEdge((DefaultIntegerSolution) s2);
					}

					this.problem.evaluate((S) s2);

					switch (coparation((IntegerSolution) population.get(i), s2)) {
					case -1:
						copySolution.add((S) population.get(i));
						break;
					case 0:
						copySolution.add((S) population.get(i));
						break;
					case 1:
						copySolution.add((S) s2);
						break;
					default:
						copySolution.add((S) population.get(i));
						System.out.println("deu falha no compara");
						break;
					}

				} else {
					copySolution.add((S) population.get(i));

				}

			}

		} else {

			for (Solution s1 : population) {
				// chamada para a busca local
				IntegerSolution s2 = (changeMatrixElement((IntegerSolution) s1.copy(), numberNeighbors));// muda

				if (this.prop.getProperty("theSearch").equals("new")) {
					s2 = putRemoveEdge((DefaultIntegerSolution) s2);
				}

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
		}
		this.paretos.removeAll(this.paretos);
		return copySolution;
	}

	/**
	 * nova abordagem fazer varredura nos vizinhos em busca de um dominante
	 * 
	 */

	public List<S> localSearchTestingAll(List<S> population, int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());
		// voltar aqui
		if (prop.getProperty("eliteSearch").equals("y")) {

			List<Integer> individualsToLocSearc = this.hp.selectTheCandidatesTolocalsearch(this.prop);
			this.indexOfIndividualSelectionedToTheSearch.add(individualsToLocSearc);
			System.out.println("soluções que atendem a regra " + this.hp.getTesteSolQueAtendExecge());
			System.out.println("soluções que não atendem  a regra " + this.hp.getTesteSolQueNaoAtendExecge());

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (individualsToLocSearc.contains(i)) {
					System.out.println("Solução top indice " + i);

					Solution copyInterge = population.get(i).copy();
					Random clusteRandomPicker = new Random();
					int clusterNumber = clusteRandomPicker.nextInt(population.get(i).copy().getLineColumn().length);
					// *****************************************************************
					// esta parte foi add para evitar que cluster de um elemento só seja sorteado
					while (this.clustters[clusterNumber].size() < 2) {
						clusterNumber = clusteRandomPicker.nextInt(population.get(i).getLineColumn().length);
					}
					// ******************************************************************
					// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais
					// próximos
					List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
							population.get(i).copy().getLineColumn().clone(), numberNeighbors);

					// iteração na lista de vizinhos mais próximos
//					int x=0;
					boolean stop = false;
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;

						if (this.prop.getProperty("theSearch").equals("new")) {
							s2 = putRemoveEdge((DefaultIntegerSolution) s2);
						}

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
							break;
						case 0:
							break;
						case 1:
							sDominator = (S) s2.copy();
							stop = true;
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}
						if (stop) {
							break;
						}
					}
					copySolution.add((S) sDominator);

				} else {
					copySolution.add((S) population.get(i));

				}

			}
		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {

			Random gerator = new Random();

			List<Integer> individualsToLocSearc = new ArrayList<>();

			for (int i = 0; i < Integer.parseInt(this.prop.getProperty("nIndividuosToSearch")); i++) {
				individualsToLocSearc.add(gerator.nextInt(population.size() - 1));
			}

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (individualsToLocSearc.contains(i)) {

					System.out.println("Solução da escolha randomica indice " + i);

					Solution copyInterge = population.get(i).copy();
					Random clusteRandomPicker = new Random();
					int clusterNumber = clusteRandomPicker.nextInt(population.get(i).copy().getLineColumn().length);
					// *****************************************************************
					// esta parte foi add para evitar que cluster de um elemento só seja sorteado
					while (this.clustters[clusterNumber].size() < 2) {
						clusterNumber = clusteRandomPicker.nextInt(population.get(i).getLineColumn().length);
					}
					// ******************************************************************
					// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais
					// próximos
					List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
							population.get(i).copy().getLineColumn().clone(), numberNeighbors);

					// iteração na lista de vizinhos mais próximos
					boolean stop = false;
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;

						if (this.prop.getProperty("theSearch").equals("new")) {
							s2 = putRemoveEdge((DefaultIntegerSolution) s2);
						}

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
							break;
						case 0:
							break;
						case 1:
							sDominator = (S) s2.copy();
							stop = true;
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}
						if (stop) {
							break;
						}
					}
					copySolution.add((S) sDominator);

				} else {
					copySolution.add((S) population.get(i));

				}

			}

		} else {

			for (Solution s1 : population) {
				Solution copyInterge = s1.copy();
				Random gerator = new Random();
				int clusterNumber = gerator.nextInt(s1.copy().getLineColumn().length);
				// *****************************************************************
				// esta parte foi add para evitar que cluster de um elemento só seja sorteado
				while (this.clustters[clusterNumber].size() < 2) {
					clusterNumber = gerator.nextInt(s1.getLineColumn().length);
				}
				// ******************************************************************
				// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais
				// próximos
				List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
						s1.copy().getLineColumn().clone(), numberNeighbors);

				// iteração na lista de vizinhos mais próximos
				boolean stop = false;
				S sDominator = (S) s1.copy();

				for (Pattern p : Litlepattern) {
					Pattern[] tempPattern = s1.getLineColumn().clone();
					tempPattern[clusterNumber] = p;
					copyInterge.setLineColumn(tempPattern);
					IntegerSolution s2 = (IntegerSolution) copyInterge;
					
					if (this.prop.getProperty("theSearch").equals("new")) {
						s2 = putRemoveEdge((DefaultIntegerSolution) s2);
					}

					this.problem.evaluate((S) s2);

					switch (coparation((IntegerSolution) s1, s2)) {
					case -1:
						break;
					case 0:
						break;
					case 1:
						sDominator = (S) s2.copy();
						stop = true;
						break;
					default:
						System.out.println("deu falha no compara");
						break;
					}
					if (stop) {
						break;
					}
				}
				copySolution.add((S) sDominator);
			}

		}

		return copySolution;

		// fim da injeção de código

	}

	/**
	 * nova abordagem fazer varredura nos vizinhos em busca de um dominante e do
	 * dominante do dominante
	 */

	public List<S> localSearchTestingAllAndDontStopUntilArriveInFInalevenFindAFirstDominator(List<S> population,
			int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());

		if (prop.getProperty("eliteSearch").equals("y")) {

			List<Integer> individualsToLocSearc = this.hp.selectTheCandidatesTolocalsearch(this.prop);
			this.indexOfIndividualSelectionedToTheSearch.add(individualsToLocSearc);
			System.out.println("soluções que atendem a regra " + this.hp.getTesteSolQueAtendExecge());
			System.out.println("soluções que não atendem  a regra " + this.hp.getTesteSolQueNaoAtendExecge());

			// Jorge candeias

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (individualsToLocSearc.contains(i)) {
					System.out.println("Solução top indice " + i);

					Solution copyInterge = population.get(i).copy();
					Random gerator = new Random();
					int clusterNumber = gerator.nextInt(population.get(i).copy().getLineColumn().length);
					// *****************************************************************
					// esta parte foi add para evitar que cluster de um elemento só seja sorteado
					while (this.clustters[clusterNumber].size() < 2) {
						clusterNumber = gerator.nextInt(population.get(i).getLineColumn().length);
					}
					// ******************************************************************

					// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais
					// próximos
					List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
							population.get(i).copy().getLineColumn().clone(), numberNeighbors);

					// iteração na lista de vizinhos mais próximos
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;

						if (this.prop.getProperty("theSearch").equals("new")) {
							s2 = putRemoveEdge((DefaultIntegerSolution) s2);
						}

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
							break;
						case 0:
							break;
						case 1:
							sDominator = (S) s2.copy();// a nova solução assumi o lugar de dominador
							population.remove(i);
							population.add(i, (S) s2.copy());// a nova solução assumi o lugar de anterior para ser
																// comparada com outros
							// vizinhos na
							// busca
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}

					}
					copySolution.add((S) sDominator);
				} else {
					copySolution.add((S) population.get(i));

				}

			}
		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {

			Random gerator = new Random();
			List<Integer> individualsToLocSearc= new ArrayList<>();

			for (int i = 0; i < Integer.parseInt(this.prop.getProperty("nIndividuosToSearch")); i++) {
				individualsToLocSearc.add(gerator.nextInt(population.size() - 1));
			}
			
			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (individualsToLocSearc.contains(i)) {

					System.out.println("Solução da escolha randomica indice " + i);

					// *************************
					Solution copyInterge = population.get(i).copy();
					Random clusteRandomPicker = new Random();
					int clusterNumber = clusteRandomPicker.nextInt(population.get(i).copy().getLineColumn().length);
					// *****************************************************************
					// esta parte foi add para evitar que cluster de um elemento só seja sorteado
					while (this.clustters[clusterNumber].size() < 2) {
						clusterNumber = clusteRandomPicker.nextInt(population.get(i).getLineColumn().length);
					}
					// ******************************************************************

					// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais
					// próximos
					List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
							population.get(i).copy().getLineColumn().clone(), numberNeighbors);

					// iteração na lista de vizinhos mais próximos
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;
						if (this.prop.getProperty("theSearch").equals("new")) {
							s2 = putRemoveEdge((DefaultIntegerSolution) s2);
						}
						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
							break;
						case 0:
							break;
						case 1:
							sDominator = (S) s2.copy();// a nova solução assumi o lugar de dominador
							population.remove(i);
							population.add(i, (S) s2.copy());// a nova solução assumi o lugar de anterior para ser
																// comparada com outros
							// vizinhos na
							// busca
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}

					}
					copySolution.add((S) sDominator);
				} else {
					copySolution.add((S) population.get(i));

				}

			}

		} else {

			for (Solution s1 : population) {
				Solution copyInterge = s1.copy();
				Random gerator = new Random();
				int clusterNumber = gerator.nextInt(s1.copy().getLineColumn().length);
				// *****************************************************************
				// esta parte foi add para evitar que cluster de um elemento só seja sorteado
				while (this.clustters[clusterNumber].size() < 2) {
					clusterNumber = gerator.nextInt(s1.getLineColumn().length);
				}
				// ******************************************************************

				// retorna uma lista (Litlepattern) com os "numberNeighbors" vizinhos mais
				// próximos
				List<Pattern> Litlepattern = changeOneIndexOfMatrixTestingAll((clusterNumber),
						s1.copy().getLineColumn().clone(), numberNeighbors);

				// iteração na lista de vizinhos mais próximos
				S sDominator = (S) s1.copy();

				for (Pattern p : Litlepattern) {
					Pattern[] tempPattern = s1.getLineColumn().clone();
					tempPattern[clusterNumber] = p;
					copyInterge.setLineColumn(tempPattern);
					IntegerSolution s2 = (IntegerSolution) copyInterge;
					if (this.prop.getProperty("theSearch").equals("new")) {
						s2 = putRemoveEdge((DefaultIntegerSolution) s2);
					}


					this.problem.evaluate((S) s2);

					switch (coparation((IntegerSolution) s1, s2)) {
					case -1:
						break;
					case 0:
						break;
					case 1:
						sDominator = (S) s2.copy();// a nova solução assumi o lugar de dominador
						s1 = (S) s2.copy();// a nova solução assumi o lugar de S1 para ser comparada com outros vizinhos
											// na
											// busca
						break;
					default:
						System.out.println("deu falha no compara");
						break;
					}

				}
				copySolution.add((S) sDominator);
			}

		}

		return copySolution;
	}

	/**
	 * metodo criado para equalizar a populacao... Foi chamado de universidade
	 * porque, apesar de ser um operador de crossOver, a ideia é de que seja um
	 * operador memetico no qual um individuo desenvolva suas habilidades durante
	 * sua vida. dessa forma nada melhor pra ensinar a um individuo novas
	 * habilidades do que uma faculdade. O objetivo é colocar, como "professores",
	 * individuos raros. E colocar, como "alunos", individuos comuns que tenham
	 * "tendencia" (Segundo melhor valor de objetivo) do mesmo tipo do primeiro
	 * melhor do professor. apos o crossOver ha uma chance desse individuo "aluno"
	 * se caracterizar como o do professor. se a nova solucao dominar o "aluno" ou
	 * que pelo menos ambos sejam não dominados havera uma substituição.
	 */
	public void universityGraduate(List<S> population) {
		List<Integer> indexOfProfessors = new ArrayList();

		int indexOfProfessor = this.hp.getTheProfessor();

		List<Integer> indexofstudents = this.hp.getTheStudent(this.getProblem().getNumberOfObjectives(),
				indexOfProfessor);

		for (int i = 0; i < indexofstudents.size(); i++) {
			List<S> listIndivAtEndSemester = new ArrayList<>(2);
			listIndivAtEndSemester.add(population.get(indexOfProfessor));
			listIndivAtEndSemester.add(population.get((int) indexofstudents.get(i)));
			List<S> trainee = crossoverOperator.execute(listIndivAtEndSemester);
			mutationOperator.execute(trainee.get(0));
			mutationOperator.execute(trainee.get(1));
			this.evaluatePopulation(trainee);

			/**
			 * compara duas soluções e retorna: -1 se s1 domina s2 0 se s1 e s2 são não
			 * dominadas 1 se s2 domina s1
			 * 
			 * @param solutio1 e solution2
			 */
			switch (coparation((IntegerSolution) population.get((int) indexofstudents.get(i)),
					(IntegerSolution) trainee.get(0))) {
			case -1:

				break;
			case 0:
				population.set((int) indexofstudents.get(i), trainee.get(1));
				break;
			case 1:
				population.set((int) indexofstudents.get(i), trainee.get(1));
				break;
			default:
				System.out.println("deu falha no compara do universit");
				break;
			}

//				switch (coparation((IntegerSolution) population.get((int) indexofstudents.get(i)),(IntegerSolution)trainee.get(1))) {
//				case -1:
//					
//					break;
//				case 0:
//					population.set((int) indexofstudents.get(i),trainee.get(1));
//					break;
//				case 1:
//					population.set((int) indexofstudents.get(i),trainee.get(1));
//					break;
//				default:
//					System.out.println("deu falha no compara do universit");
//					break;
//				}
		}

	}
	/**
	 * this method was created to determine if a solution if part
	 * of o risch's list in hiperplan distribution
	 * Actor: Jorge candeoas
	 * @param s
	 * @return boolean
	 */
	
	public boolean containsInBigGroup(Solution s) {
		List richToPor = this.hp.getRichToPoor();
		List<List> hiperPlanDistribuction = hp.getFamilyOfIndividualInPopulation();
		if (this.hp.contaiSolution(s, hiperPlanDistribuction.get((int) richToPor.get(0)))) {
			return true;
		}else {
		return false;
		}
	}

	@Override
	protected List<S> selection(List<S> population) {

		boolean first = false;
		if (this.prop.get("startFromAstopedIteration").equals("y")
				&& !this.iDidTheFirstTimeAfterInciationFromAStopedExecution) {
			first = true;
			this.iDidTheFirstTimeAfterInciationFromAStopedExecution = true;
			this.prop.setProperty("startFromAstopedIteration", "n");
		}

		if (this.prop.getProperty("modo").equals("com busca") && !first
				&& this.iterations >= Integer.parseInt(this.prop.getProperty("BeginSeach"))) {
			int numberNeighbors = Integer.parseInt(this.prop.getProperty("numberNeighbors"));// mudar numero de vizinhos
																								// da busca aqui
			if (this.prop.getProperty("modo").equals("com busca")) {
				if (this.prop.getProperty("buscalocal").equals("localSearch")) {
					System.out.println("busca local = buscalocal");
					population = localSearch(population, numberNeighbors);// eu
				} else if (this.prop.getProperty("buscalocal").equals("localSearchTestingAll")) {
					System.out.println("busca local = localSearchTestingAll");
					population = localSearchTestingAll(population, numberNeighbors);// este testar percorrendo os
																					// vizinhos ate encontrar o primeiro
																					// dominador
				} else {
					System.out.println(
							"busca local = localSearchTestingAllAndDontStopUntilArriveInFInalevenFindAFirstDominator");
					population = localSearchTestingAllAndDontStopUntilArriveInFInalevenFindAFirstDominator(population,
							numberNeighbors);// este testar percorrendo os vizinhos encontrando dominadores aaté esgotar
												// os vizinhos
				}
			}

		}
//		
//		
//		original preservado
//		List<S> matingPopulation = new ArrayList<>(population.size());
//		for (int i = 0; i < getMaxPopulationSize(); i++) {
//			S solution = selectionOperator.execute(population);
//			matingPopulation.add(solution);
//		}
		// daqui pra baixo ate a proxima marca de original
		// é uma modificação proposta por jorge candeias pra
		// introduzir os garanhões ou professores(a decidir o nome)
		List<S> matingPopulation = new ArrayList<>(population.size());

		// ************teste comtrole populacional mais assistido
		if (this.fronts.size() == 1 && !this.hp.isEqualized() && this.prop.get("populationMatingControl").equals("y")) {
			for (int k = 0; k < this.iDidDominate.size(); k++) {
				matingPopulation.add(population.get(k));
			}

			this.hp.PopulationControlMating(matingPopulation, this.iDidDominate);
			int cont = matingPopulation.size();
			while (cont < getMaxPopulationSize()) {
				S solution = selectionOperator.execute(population);
				matingPopulation.add(solution);
				cont += 1;
			}
		}else if (this.prop.getProperty("makeHefather").equals("y")&&this.iDidDominate.size()>0&&this.fronts.size() == 1) {
			for (int i = 0; i < getMaxPopulationSize(); i++) {
				S solution = selectionOperator.execute(population);
				if (this.iDidDominate.size()>0&&containsInBigGroup(solution)) {
					matingPopulation.add(population.get(this.iDidDominate.get(0)));
					this.iDidDominate.remove(0);
				}else {
				matingPopulation.add(solution);
				}
			}
		}
		else {
			for (int i = 0; i < getMaxPopulationSize(); i++) {
				S solution = selectionOperator.execute(population);
				matingPopulation.add(solution);
			}

		}
		// ************fim do códigoteste comtrole populacional mais assistido
		this.iDidDominate.removeAll(this.iDidDominate);// se for voltar ao código de antes do controle populacional mais
														// assistdo essa linha fica também.
		// daqui pra baixo volta a ser o original
		return matingPopulation;// origina
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

	/**
	 * returna o w da população para calcular o hp author: jorge candeias
	 * 
	 * @return
	 */
	public void wForhv(List<List<S>> fatPopulation) {
		List<S> localFatPopulation = new ArrayList<>();
		for (List<S> l : fatPopulation) {
			localFatPopulation.addAll(l);
		}

		for (int i = 0; i < this.problem.getNumberOfObjectives(); i++) {
			Double bigD = Double.NEGATIVE_INFINITY;
			if (this.doNormatization.get(i) == true) {
				for (S s : localFatPopulation) {
					if (s.getObjective(i) > bigD) {
						bigD = s.getObjective(i);
					}
				}
				if (bigD > this.listW.get(i)) {
					this.listW.set(i, bigD);
				}

			}
		}
	}

	/**
	 * metodo criado garantir que não haja solucões duplicadas no pareto local
	 * 
	 * @param index
	 * @param pareto
	 */
	private List<S> RemoveSolutionsAddingMultipleTimes(List<S> slicePareto) {
		List<S> returnPareto = new ArrayList<>();
		for (S s : slicePareto) {
			if (!returnPareto.contains(s)) {
				returnPareto.add(s);
			}
		}
		return returnPareto;
	}

	/**
	 * methodo criando para retornar uma de 3 população de acordop com o melhor hv
	 */

	public List<S> betterThree(List<List<S>> threPopulation) {
		MetricEvalRunTime met = new MetricEvalRunTime(this.problem.getNumberOfObjectives());
		double bigHv = Double.NEGATIVE_INFINITY;
		List<S> returned = new ArrayList<>();
		wForhv(threPopulation);
		for (List<S> pop : threPopulation) {
			double hv = met.evaluateFront((List<DefaultIntegerSolution>) pop, this.listW);
			if (hv > bigHv) {
				bigHv = hv;
				returned = pop;
			}
		}
		return returned;
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
//			fronts.add(ranking.getSubfront(rankingIndex)); original
			fronts.add(RemoveSolutionsAddingMultipleTimes(ranking.getSubfront(rankingIndex)));// autor jorge candeias, e
																								// a linah de cima
																								// modificada
			candidateSolutions += ranking.getSubfront(rankingIndex).size();
			if ((pop.size() + ranking.getSubfront(rankingIndex).size()) <= getMaxPopulationSize())
				addRankedSolutionsToPopulation(ranking, rankingIndex, pop);// aqui que ele adiciona a sulução ranquiada
																			// pra população, mas só no inicio
			rankingIndex++;
		}

		// A copy of the reference list should be used as parameter of the
		// environmental selection

		EnvironmentalSelection<S> selection = new EnvironmentalSelection<>(fronts, getMaxPopulationSize(),
				getReferencePointsCopy(), getProblem().getNumberOfObjectives());

		pop = selection.execute(pop);
		this.fronts = fronts;// auro jorge candeias pata tratamento de controle de reprodução
		this.envi = selection;// autor jorge candeias
		this.hp = selection.getHpo();// autor jorge candeias
		tradeTheObservationPlane(pop);// autor jorge candeias
		this.EqualizadListe.add(this.hp.geteQualizationList());// autor jorge candeias
		// this.hp.externalAssociateTheLargestToThefamilyOfIndividualInPopulation(prop,this.problem.getNumberOfVariables());//
		// adicioinado patra teste jorge candeias
		return pop;
	}

//	@Override
//	protected List<S> replacement(List<S> population, List<S> offspringPopulation) {
//
//		List<S> jointPopulation = new ArrayList<>();
//		jointPopulation.addAll(population);
//		jointPopulation.addAll(offspringPopulation);
//
//		Ranking<S> ranking = computeRanking(jointPopulation);
//
//		// List<Solution> pop = crowdingDistanceSelection(ranking);
//		List<S> pop = new ArrayList<>();
//		List<List<S>> fronts = new ArrayList<>();
//		int rankingIndex = 0;
//		int candidateSolutions = 0;
//		while (candidateSolutions < getMaxPopulationSize()) {
////			fronts.add(ranking.getSubfront(rankingIndex)); original
//			fronts.add(RemoveSolutionsAddingMultipleTimes(ranking.getSubfront(rankingIndex)));// autor jorge candeias, e
//																								// a linah de cima
//																								// modificada
//			candidateSolutions += ranking.getSubfront(rankingIndex).size();
//			if ((pop.size() + ranking.getSubfront(rankingIndex).size()) <= getMaxPopulationSize())
//				addRankedSolutionsToPopulation(ranking, rankingIndex, pop);// aqui que ele adiciona a sulução ranquiada
//																			// pra população, mas só no inicio
//			rankingIndex++;
//		}
//
//		// A copy of the reference list should be used as parameter of the
//		// environmental selection
//
//		EnvironmentalSelection<S> selection = new EnvironmentalSelection<>(fronts, getMaxPopulationSize(),
//				getReferencePointsCopy(), getProblem().getNumberOfObjectives());
//
//		this.fronts = fronts;// autor jorge candeias pata tratamento de controle de reprodução
//
//		if (this.iterations > 2) {// autor jorge candeias
//			List<S> safePop = new ArrayList<>();
////			for (S o:pop ){
////					DefaultIntegerSolution i=((DefaultIntegerSolution) o).getClone();
////				   safePop.add((S) i);
////				}
//			safePop.addAll(pop);
//			if (fronts.size() == 1) {
//				List<List<S>> threPopulation = new ArrayList<>();
//				for (int i = 0; i < 3; i++) {
//					EnvironmentalSelection<S> selection1 = new EnvironmentalSelection<>(fronts, getMaxPopulationSize(),
//							getReferencePointsCopy(), getProblem().getNumberOfObjectives());
//					List<S> popTime = new ArrayList<>();
//					popTime.addAll(safePop);
//					pop = selection1.execute(popTime);// original
//					threPopulation.add(pop);
//					// autor jorge candeias atenção, a linha abaixo esta quebrando o
//					// largestreferePoint, mas por enquanto não esta sendo usado
//					this.hp = selection1.getHpo();
//
//				}
//				pop = betterThree(threPopulation);
//
//			} else {
//				pop = selection.execute(pop);// original
//				this.hp = selection.getHpo();// autor jorge candeias
//			}
//			tradeTheObservationPlane(pop);// autor jorge candeias
//			this.EqualizadListe.add(this.hp.geteQualizationList());// autor jorge candeias
//		} else {
//			pop = selection.execute(pop);// original
//			this.hp = selection.getHpo();// autor jorge candeias
//		}
////		this.envi = selection;// autor jorge candeias
//
//		// this.hp.externalAssociateTheLargestToThefamilyOfIndividualInPopulation(prop,this.problem.getNumberOfVariables());//
//		// adicioinado patra teste jorge candeias
//		return pop;
//	}

	/**
	 * adicionado por jorge candeias para trabalhar a alimentacao da obsevarcao do
	 * hyperplano
	 */
	public void tradeTheObservationPlane(List<S> population) {
		int i = 0;
		for (S s : population) {
			AnIndividualAndHisVector<S> ind = new AnIndividualAndHisVector((DefaultIntegerSolution) s, i);
			int teste = this.hp.includeSolutionInGroupAppropriate(ind);
			i += 1;
		}
		this.hp.eQualization(this.prop);
		this.hp.separateIndividualsAccordingToTheTrend();
	}

	public List<List<Integer>> getEqualizadListe() {
		return EqualizadListe;
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
		// adiononado por jorge candeias para pegar o front menos dominado
		if (rank == 0) {
			this.betterPareto = front;
		}
//		this.betterPareto = front;

		this.paretos.add(front);

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

	public int getCont() {
		return cont;
	}

	public void setCont(int cont) {
		this.cont = cont;
	}

	public List<List<Integer>> getIndexOfIndividualSelectionedToTheSearch() {
		return indexOfIndividualSelectionedToTheSearch;
	}

	public void printforFitness(List<? extends Solution<?>> population) {
		String path = this.prop.getProperty("local") + this.prop.getProperty("algName") + "/"
				+ this.prop.getProperty("modo") + "/" + this.prop.getProperty("execucao");
		new SolutionListOutput(population).setSeparator("\t")
				.setVarFileOutputContext(
						new DefaultFileOutputContext(path + "/" + "VAR" + this.fitnessPrint.get(0) + ".tsv"))
				.setFunFileOutputContext(
						new DefaultFileOutputContext(path + "/" + "FUN" + this.fitnessPrint.get(0) + ".tsv"))
				.print();
		// parte nova
		int w = 1;

		new File(prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo") + "/"
				+ prop.getProperty("execucao") + "/ResultadoGML" + this.fitnessPrint.get(0) + "/").mkdir();

		String pathTogml = prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo")
				+ "/" + prop.getProperty("execucao");
		for (Solution<?> i : population) {
			// IntegerSolution
			String s = pathTogml + "/ResultadoGML" + this.fitnessPrint.get(0) + "/" + Integer.toString(w) + ".gml";
			this.ptg.saveGmlFromSolution(s, (IntegerSolution) i);
			List<Integer> centros = new ArrayList<>();
			for (int j = 0; j < i.getLineColumn().length; j++) {
				centros.add(i.getLineColumn()[j].getId());
			}
			w += 1;

		}

		try {
			this.fitnessPrint.remove(0);
		} catch (Exception e) {
			System.out.println("array de marcos de print vazio");
		}
		// ***********************

		JMetalLogger.logger.info("Random Seed: " + JMetalRandom.getInstance().getSeed());
		JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
		JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
	}

	public void printFinalSolutionSet(List<? extends Solution<?>> population) {
		String path = this.prop.getProperty("local") + this.prop.getProperty("algName") + "/"
				+ this.prop.getProperty("modo") + "/" + this.prop.getProperty("execucao");
		new SolutionListOutput(population).setSeparator("\t")
				.setVarFileOutputContext(new DefaultFileOutputContext(path + "/" + "VAR" + this.iterations + ".tsv"))
				.setFunFileOutputContext(new DefaultFileOutputContext(path + "/" + "FUN" + this.iterations + ".tsv"))
				.print();
		// parte nova
		int w = 1;

		new File(prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo") + "/"
				+ prop.getProperty("execucao") + "/ResultadoGML" + this.iterations + "/").mkdir();

		String pathTogml = prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo")
				+ "/" + prop.getProperty("execucao");
		for (Solution<?> i : population) {
			// IntegerSolution
			String s = pathTogml + "/ResultadoGML" + this.iterations + "/" + Integer.toString(w) + ".gml";
			this.ptg.saveGmlFromSolution(s, (IntegerSolution) i);
			List<Integer> centros = new ArrayList<>();
			for (int j = 0; j < i.getLineColumn().length; j++) {
				centros.add(i.getLineColumn()[j].getId());
			}
			w += 1;

		}
		// ***********************

		JMetalLogger.logger.info("Random Seed: " + JMetalRandom.getInstance().getSeed());
		JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
		JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
	}

}
