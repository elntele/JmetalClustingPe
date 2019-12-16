package org.uma.jmetal.algorithm.multiobjective.nsgaiii;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.util.EnvironmentalSelection;
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
	private int LocalSeachFoundNoDominated = 0;
	private UUID ParallelEvaluateId;
	private PatternToGml ptg;

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
		ptg=builder.getPtg();
		prop = builder.getProp();

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

	@Override
	protected void initProgress() {
		if (this.prop.get("startFromAstopedIteration").equals("y")) {
			iterations=Integer.parseInt(this.prop.getProperty("interationStopedInExecution"));
		}else{
			iterations = 1;
			}
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
			System.out.println("as soluções são iguais ? " + ((VariableFromRetornedSolution.getvariables()).toString())
					.equals(stringVariableFromOriginalPopulation));
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
	 * metodo recebe a população e uma lista com n-numerofobjetives com o maior
	 * valor de doubler e retorna um array com o indice das 4 soluções da população
	 * com menores valores de objetivo
	 * 
	 * @param arrayObjetiveValueLower
	 * @param population
	 * @return
	 */

	public Integer[] takeNLowerSolutiox(Double[] arrayObjetiveValueLower, List<S> population) {
		Integer[] arrayIndice = new Integer[this.problem.getNumberOfObjectives()];
		for (int i = 0; i < population.size(); i++) {
			if (population.get(i).getObjective(0) < arrayObjetiveValueLower[0]) {
				arrayObjetiveValueLower[0] = population.get(i).getObjective(0);
				arrayIndice[0] = i;
			}
		}
		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(1) < arrayObjetiveValueLower[1])
					&& (arrayIndice[0] != i)) {
				arrayObjetiveValueLower[1] = population.get(i).getObjective(1);
				arrayIndice[1] = i;
			}
		}

		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(2) < arrayObjetiveValueLower[2])
					&& (arrayIndice[0] != i) && (arrayIndice[1] != i)) {
				arrayObjetiveValueLower[2] =population.get(i).getObjective(2);
				arrayIndice[2] = i;
			}
		}

		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(3) < arrayObjetiveValueLower[3])
					&& (arrayIndice[0] != i) && (arrayIndice[1] != i) && (arrayIndice[2] != i)) {
				arrayObjetiveValueLower[3] = population.get(i).getObjective(3);
				arrayIndice[3] = i;
			}
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
	 * @param population
	 * @return
	 */
	public Integer[] takeNUpperSolutiox(Double[] arrayObjetiveValueUpper, List<S> population, Integer[] Lowers) {
		Integer[] arrayIndiceUpper = new Integer[this.problem.getNumberOfObjectives()];
		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(0) > arrayObjetiveValueUpper[0])
					&& (i != Lowers[0]) && (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3])) {
				arrayObjetiveValueUpper[0] =population.get(i).getObjective(0);
				arrayIndiceUpper[0] = i;
			}
		}

		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(1) > arrayObjetiveValueUpper[1])
					&& (i != Lowers[0]) && (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3])
							&& (arrayIndiceUpper[0] != i)) {
				arrayObjetiveValueUpper[1] = population.get(i).getObjective(1);
				arrayIndiceUpper[1] = i;
			}
		}

		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(2) > arrayObjetiveValueUpper[2])
					&& (i != Lowers[0]) && (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3])
							&& (arrayIndiceUpper[0] != i) && (arrayIndiceUpper[1] != i)) {
				arrayObjetiveValueUpper[2] = population.get(i).getObjective(2);
				arrayIndiceUpper[2] = i;
			}
		}

		for (int i = 0; i < population.size(); i++) {
			if ((population.get(i).getObjective(3) > arrayObjetiveValueUpper[3])
					&& (i != Lowers[0]) && (i != Lowers[1]) && (i != Lowers[2]) && (i != Lowers[3])
							&& (arrayIndiceUpper[0] != i) && (arrayIndiceUpper[1] != i)
							&& (arrayIndiceUpper[2] != i)){
				arrayObjetiveValueUpper[3] =population.get(i).getObjective(3);
				arrayIndiceUpper[3] = i;
			}
		}

		return arrayIndiceUpper;
	}

	/**
	 * operador de busca local metodo 1
	 * 
	 * @param population
	 * @return population alterada ou não
	 */

	public List<S> localSearch(List<S> population, int numberNeighbors) {
		List<S> copySolution = new ArrayList<>(population.size());
		if (prop.getProperty("eliteSearch").equals("y")) {
			Double[] arrayObjetiveValueLower = new Double[this.problem.getNumberOfObjectives()];
			Double[] arrayObjetiveValueUpper = new Double[this.problem.getNumberOfObjectives()];

			for (int i = 0; i < arrayObjetiveValueLower.length; i++) {
				arrayObjetiveValueLower[i] =Double.MIN_VALUE;
				arrayObjetiveValueUpper[i] = Double.MAX_VALUE;
			}

			Integer[] arrayIndiceLower = takeNLowerSolutiox(arrayObjetiveValueUpper.clone(), population);
			Integer[] arrayIndiceUpper = takeNUpperSolutiox(arrayObjetiveValueLower.clone(), population,
					arrayIndiceLower.clone());

			// Jorge candeias

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (i == arrayIndiceLower[0] || i == arrayIndiceLower[1] || i == arrayIndiceLower[2]
						|| i == arrayIndiceLower[3] || i == arrayIndiceUpper[0] || i == arrayIndiceUpper[1]
						|| i == arrayIndiceUpper[2] || i == arrayIndiceUpper[3]) {
					System.out.println("Solução top indice " + i);
					IntegerSolution s2 = (changeMatrixElement((IntegerSolution) population.get(i).copy(),
							numberNeighbors));// muda
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
		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {

			Random gerator = new Random();

			Integer[] arrayIndices = new Integer[this.problem.getNumberOfObjectives() * 2];

			for (int i = 0; i < arrayIndices.length; i++) {
				arrayIndices[i] = gerator.nextInt(population.size()-1);
			}

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (i == arrayIndices[0] || i == arrayIndices[1] || i == arrayIndices[2] || i == arrayIndices[3]
						|| i == arrayIndices[4] || i == arrayIndices[5] || i == arrayIndices[6]
						|| i == arrayIndices[7]) {
					System.out.println("Solução da escolha randomica indice " + i);
					IntegerSolution s2 = (changeMatrixElement((IntegerSolution) population.get(i).copy(),
							numberNeighbors));// muda
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
		if (prop.getProperty("eliteSearch").equals("y")) {
			Double[] arrayObjetiveValueLower = new Double[this.problem.getNumberOfObjectives()];
			Double[] arrayObjetiveValueUpper = new Double[this.problem.getNumberOfObjectives()];

			for (int i = 0; i < arrayObjetiveValueLower.length; i++) {
				arrayObjetiveValueLower[i] =Double.MIN_VALUE;
				arrayObjetiveValueUpper[i] = Double.MAX_VALUE;
			}
			//se liga, estou mandando o arrayObjetiveValueUpper para o arrayIndiceLower
			// e o arrayObjetiveValueLower arrayIndiceUpper, ou seja, cruzado.

			Integer[] arrayIndiceLower = takeNLowerSolutiox(arrayObjetiveValueUpper.clone(), population);
			Integer[] arrayIndiceUpper = takeNUpperSolutiox(arrayObjetiveValueLower.clone(), population,
					arrayIndiceLower.clone());

			// Jorge candeias

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (i == arrayIndiceLower[0] || i == arrayIndiceLower[1] || i == arrayIndiceLower[2]
						|| i == arrayIndiceLower[3] || i == arrayIndiceUpper[0] || i == arrayIndiceUpper[1]
						|| i == arrayIndiceUpper[2] || i == arrayIndiceUpper[3]) {
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

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
//							System.out.println("entrei em -1e este é x "+x);
							break;
						case 0:
//							System.out.println("entrei em 0 este é x "+x);
							break;
						case 1:
							sDominator = (S) s2.copy();
//							System.out.println("entrei em 1 e este é x "+x);
							stop = true;
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}
						if (stop) {
							break;
						}
//						x+=1;
					}
					copySolution.add((S) sDominator);

				} else {
					copySolution.add((S) population.get(i));

				}

			}
		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {

			Random gerator = new Random();

			Integer[] arrayIndices = new Integer[this.problem.getNumberOfObjectives() * 2];

			for (int i = 0; i < arrayIndices.length; i++) {
				arrayIndices[i] = gerator.nextInt(population.size()-1);
			}

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (i == arrayIndices[0] || i == arrayIndices[1] || i == arrayIndices[2] || i == arrayIndices[3]
						|| i == arrayIndices[4] || i == arrayIndices[5] || i == arrayIndices[6]
						|| i == arrayIndices[7]) {

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
//					int x=0;
					boolean stop = false;
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
//							System.out.println("entrei em -1e este é x "+x);
							break;
						case 0:
//							System.out.println("entrei em 0 este é x "+x);
							break;
						case 1:
							sDominator = (S) s2.copy();
//							System.out.println("entrei em 1 e este é x "+x);
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
//				int x=0;
				boolean stop = false;
				S sDominator = (S) s1.copy();

				for (Pattern p : Litlepattern) {
					Pattern[] tempPattern = s1.getLineColumn().clone();
					tempPattern[clusterNumber] = p;
					copyInterge.setLineColumn(tempPattern);
					IntegerSolution s2 = (IntegerSolution) copyInterge;

					this.problem.evaluate((S) s2);

					switch (coparation((IntegerSolution) s1, s2)) {
					case -1:
//						System.out.println("entrei em -1e este é x "+x);
						break;
					case 0:
//						System.out.println("entrei em 0 este é x "+x);
						break;
					case 1:
						sDominator = (S) s2.copy();
//						System.out.println("entrei em 1 e este é x "+x);
						stop = true;
						break;
					default:
						System.out.println("deu falha no compara");
						break;
					}
					if (stop) {
						break;
					}
//					x+=1;
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
			Double[] arrayObjetiveValueLower = new Double[this.problem.getNumberOfObjectives()];
			Double[] arrayObjetiveValueUpper = new Double[this.problem.getNumberOfObjectives()];

			for (int i = 0; i < arrayObjetiveValueLower.length; i++) {
				arrayObjetiveValueLower[i] =Double.MIN_VALUE;
				arrayObjetiveValueUpper[i] = Double.MAX_VALUE;
			}

			Integer[] arrayIndiceLower = takeNLowerSolutiox(arrayObjetiveValueUpper.clone(), population);
			Integer[] arrayIndiceUpper = takeNUpperSolutiox(arrayObjetiveValueLower.clone(), population,
					arrayIndiceLower.clone());

			// Jorge candeias

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (i == arrayIndiceLower[0] || i == arrayIndiceLower[1] || i == arrayIndiceLower[2]
						|| i == arrayIndiceLower[3] || i == arrayIndiceUpper[0] || i == arrayIndiceUpper[1]
						|| i == arrayIndiceUpper[2] || i == arrayIndiceUpper[3]) {
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
					int x = 0;
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
							break;
						case 0:
							break;
						case 1:
							sDominator = (S) s2.copy();// a nova solução assumi o lugar de dominador
							population.remove(i);
							population.add(i,(S) s2.copy());// a nova solução assumi o lugar de anterior para ser comparada com outros
												// vizinhos na
												// busca
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}

						x += 1;
					}
					copySolution.add((S) sDominator);
				} else {
					copySolution.add((S) population.get(i));

				}

			}
		} else if (prop.getProperty("eliteSearch").equals("n") && prop.getProperty("randomEliteSearch").equals("y")) {

			Random gerator = new Random();

			Integer[] arrayIndices = new Integer[this.problem.getNumberOfObjectives() * 2];

			for (int i = 0; i < arrayIndices.length; i++) {
				arrayIndices[i] = gerator.nextInt(population.size()-1);
			}

			for (int i = 0; i < population.size(); i++) {
				// chamada para a busca local
				if (i == arrayIndices[0] || i == arrayIndices[1] || i == arrayIndices[2] || i == arrayIndices[3]
						|| i == arrayIndices[4] || i == arrayIndices[5] || i == arrayIndices[6]
						|| i == arrayIndices[7]) {

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
					int x = 0;
					S sDominator = (S) population.get(i).copy();

					for (Pattern p : Litlepattern) {
						Pattern[] tempPattern = population.get(i).getLineColumn().clone();
						tempPattern[clusterNumber] = p;
						copyInterge.setLineColumn(tempPattern);
						IntegerSolution s2 = (IntegerSolution) copyInterge;

						this.problem.evaluate((S) s2);

						switch (coparation((IntegerSolution) population.get(i), s2)) {
						case -1:
							break;
						case 0:
							break;
						case 1:
							sDominator = (S) s2.copy();// a nova solução assumi o lugar de dominador
							population.remove(i);
							population.add(i,(S) s2.copy());// a nova solução assumi o lugar de anterior para ser comparada com outros
												// vizinhos na
												// busca
							break;
						default:
							System.out.println("deu falha no compara");
							break;
						}

						x += 1;
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
				int x = 0;
				S sDominator = (S) s1.copy();

				for (Pattern p : Litlepattern) {
					Pattern[] tempPattern = s1.getLineColumn().clone();
					tempPattern[clusterNumber] = p;
					copyInterge.setLineColumn(tempPattern);
					IntegerSolution s2 = (IntegerSolution) copyInterge;

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

					x += 1;
				}
				copySolution.add((S) sDominator);
			}

		}

		return copySolution;
	}

	@Override
	protected List<S> selection(List<S> population) {

		if (this.prop.getProperty("modo").equals("com busca")) {
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

	public int getCont() {
		return cont;
	}

	public void setCont(int cont) {
		this.cont = cont;
	}

	public void printFinalSolutionSet(List<? extends Solution<?>> population) {
		String path = this.prop.getProperty("local") + this.prop.getProperty("algName") + "/"
				+ this.prop.getProperty("modo") + "/" + this.prop.getProperty("execucao");
		new SolutionListOutput(population).setSeparator("\t")
				.setVarFileOutputContext(new DefaultFileOutputContext(path + "/" + "VAR" + this.iterations + ".tsv"))
				.setFunFileOutputContext(new DefaultFileOutputContext(path + "/" + "FUN" + this.iterations + ".tsv"))
				.print();
		//parte nova
		int w = 1;
		
		
		new File(prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo") + "/"
				+ prop.getProperty("execucao") + "/ResultadoGML"+this.iterations+"/").mkdir();
		
		String pathTogml = prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo")
		+ "/" + prop.getProperty("execucao");
		for (Solution<?> i : population) {
			//IntegerSolution
			String s = pathTogml + "/ResultadoGML"+this.iterations+"/" + Integer.toString(w) + ".gml";
			this.ptg.saveGmlFromSolution( s, (IntegerSolution)i);
			List<Integer> centros = new ArrayList<>();
			for (int j = 0; j < i.getLineColumn().length; j++) {
				centros.add(i.getLineColumn()[j].getId());
			}
			w += 1;

		}
		//***********************

		JMetalLogger.logger.info("Random Seed: " + JMetalRandom.getInstance().getSeed());
		JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
		JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
	}

}
