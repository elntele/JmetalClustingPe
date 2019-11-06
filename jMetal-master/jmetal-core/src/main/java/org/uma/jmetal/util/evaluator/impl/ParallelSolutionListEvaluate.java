package org.uma.jmetal.util.evaluator.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.uma.jmetal.problem.IntegerProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.impl.AbstractGenericSolution;
import org.uma.jmetal.solution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author jorge candeias class implements the evaluating concurrent with a
 *         distributed system aideding
 * @param <S>
 */
public class ParallelSolutionListEvaluate<S> extends Thread implements SolutionListEvaluator<S> {
	private List<S> localEvaluate;
	private List<List<S>> remotEvaluate;
	private List<S> solutionList;
	private Problem<S> problem;
	private List<UUID> ParallelEvaluateIdList;

	public ParallelSolutionListEvaluate(List<UUID> ParallelEvaluateIdList) {
		super();
		this.ParallelEvaluateIdList = ParallelEvaluateIdList;
	}

	/**
	 * java passa parâmetro por referência, logo, ao colocar as solutions de
	 * solutionList em localEvaluate e remotEvaluate e alterar elas, estamos
	 * automaticamente alterando as soluções em solutionList também.
	 */

	@Override
	public List<S> evaluate(List<S> solutionList, Problem<S> problem) {

		// lista local vai cair em desuso
		List<S> localEvaluate = new ArrayList<>();
		// lista de lista remoras, tem o tamanho da lista de id de servidores
		List<List<S>> remotEvaluate = new ArrayList<>();
		// numero de servidores disponivel
		int numberOfServers = this.ParallelEvaluateIdList.size();
		// preparando x listas de solution onde x= numero de servidores
		for (int i = 0; i < numberOfServers; i++) {
			List<S> l = new ArrayList<>();
			remotEvaluate.add(l);
		}
		int i = 0;
		// distribuindo as sulutios entre as listas de solution no esquema uma m=pra mim
		// outra pra tu
		for (S s : solutionList) {
			remotEvaluate.get(i).add(s);
			i += 1;
			if (i == numberOfServers) {
				i = 0;
			}

		}

//		int SliceSolutioListSize=
//		double temp=solutionList.size()*50;
//		temp=temp/100;
//		int populationSize=(int)temp;
//		
//		
//		for (int i=0;i<populationSize;i++) {
//			remotEvaluate.add(solutionList.get(i));
//		}
//		for (int i=populationSize;i<solutionList.size();i++) {
//			localEvaluate.add(solutionList.get(i));
//		}
		this.localEvaluate = localEvaluate;
		this.remotEvaluate = remotEvaluate;
		this.problem = problem;
		List<RemotePoolEvaluate> rList = new ArrayList<>();
		
		
		for (int w = 0; w < this.ParallelEvaluateIdList.size(); w++) {
			RemotePoolEvaluate y = new RemotePoolEvaluate(this.remotEvaluate.get(w), problem,
					this.ParallelEvaluateIdList.get(w));
			rList.add(y);
		}
//		LocalPoolEvaluate x = new LocalPoolEvaluate(localEvaluate, problem);
//		RemotePoolEvaluate y = new RemotePoolEvaluate(remotEvaluate, problem, this.ParallelEvaluateId);
		for (RemotePoolEvaluate r:rList) {
			r.start();
			
		}
//		x.start();
//		y.start();
//		this.start();
		try {
//			x.join();
//			y.join();
			for (RemotePoolEvaluate r:rList) {
				r.join();
				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return solutionList;
	}

//	@Override
//	public void run() {
//
//		localEvaluate.stream().forEach(s -> problem.evaluate(s));
//		evaluatePopulationparallel(this.remotEvaluate);
//
//	}

//	
//	protected /* List<S> */void evaluatePopulationparallel(List<S> population) {
//		Socket soc = null;
//		ObjectMapper mapper = new ObjectMapper();
//		String textOut = null;
//		try {
//			textOut = mapper.writeValueAsString(population);
//			List<String> l = new ArrayList<>();
//			l.add((this.ParallelEvaluateId).toString());
//			l.add(textOut);
//			textOut = mapper.writeValueAsString(l);
//
//		} catch (JsonProcessingException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//		String adress = "localhost";
//		try {
//			int serverPort = 7896;
//			soc = new Socket(adress, serverPort);
//			DataInputStream in = new DataInputStream(soc.getInputStream());
//			DataOutputStream out = new DataOutputStream(soc.getOutputStream());
//
////			int length = out.readInt(); // read length of incoming message
//
//			byte[] b = textOut.getBytes(StandardCharsets.UTF_8);
//			out.writeInt(b.length); // write length of the message
//			out.write(b);
//			// retorno
//			int length = in.readInt();
//			String data = null;
//			if (length > 0) {
//				byte[] message = new byte[length];
//				in.readFully(message, 0, message.length); // read the message
//				data = new String(message, StandardCharsets.US_ASCII);
////				System.out.println("mensagem aqui " + s);
//			}
//			// String data = in.readUTF(); // read a line of data from the stream
//			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//			List<DefaultIntegerSolution> pReturned = new ArrayList<>();
//
//			pReturned = mapper.readValue(data, new TypeReference<List<DefaultIntegerSolution>>() {
//			});
//			// testEqualityBetweenOriginalSolutionAndReturnOfParallelEvaluate(pr,population);
//			int i = 0;
//			for (DefaultIntegerSolution s : pReturned) {
//				for (int numberOfobjetive = 0; numberOfobjetive < this.problem
//						.getNumberOfObjectives(); numberOfobjetive++) {
//					((AbstractGenericSolution<Integer, IntegerProblem>) population.get(i))
//							.setObjective(numberOfobjetive, s.getObjective(numberOfobjetive));
//				}
//				i += 1;
//			}
//
//		} catch (UnknownHostException e) {
//			System.out.println("Socket:" + e.getMessage());
//		} catch (EOFException e) {
//			System.out.println("EOF:" + e.getMessage());
//		} catch (IOException e) {
//			System.out.println("readline:" + e.getMessage());
//		} finally {
//			if (soc != null)
//				try {
//					soc.close();
//				} catch (IOException e) {
//					System.out.println("close:" + e.getMessage());
//				}
//		}
//
//		// return population;
//
//	}
//	

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

}

class LocalPoolEvaluate<S> extends Thread {
	private List<S> localEvaluate;
	private Problem<S> problem;

	public LocalPoolEvaluate(List<S> localEvaluate, Problem<S> problem) {

		this.localEvaluate = localEvaluate;
		this.problem = problem;
	}

	@Override
	public void run() {
		localEvaluate.stream().forEach(s -> this.problem.evaluate(s));

	}
}

class RemotePoolEvaluate<S> extends Thread {
	private List<S> remotEvaluate;
	private Problem<S> problem;
	private UUID ParallelEvaluateId;

	public RemotePoolEvaluate(List<S> remotEvaluate, Problem<S> problem, UUID ParallelEvaluateId) {

		this.remotEvaluate = remotEvaluate;
		this.problem = problem;
		this.ParallelEvaluateId = ParallelEvaluateId;
	}

	@Override
	public void run() {
		Socket soc = null;
		ObjectMapper mapper = new ObjectMapper();
		String textOut = null;
		try {
			textOut = mapper.writeValueAsString(remotEvaluate);
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

//				int length = out.readInt(); // read length of incoming message

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
//					System.out.println("mensagem aqui " + s);
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
					((AbstractGenericSolution<Integer, IntegerProblem>) remotEvaluate.get(i))
							.setObjective(numberOfobjetive, s.getObjective(numberOfobjetive));
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

		// return population;

	}
}
