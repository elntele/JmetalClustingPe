package org.uma.jmetal.util.evaluator.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

import br.cns.metrics.TotalDistance;
import br.cns.model.GmlData;

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
	private List<SeverAndId> severAndIdList;
	private int AuxiliarCountParallelEvaluation;
	private boolean isStartEvaluate = true;
	private int calsRemoteEvatuatorTimes = 0;

	public ParallelSolutionListEvaluate(List<SeverAndId> severAndIdList) {
		super();
		this.severAndIdList = severAndIdList;
	}
	
	
	

	/**
	 * java passa parâmetro por referência, logo, ao colocar as solutions de
	 * solutionList em localEvaluate e remotEvaluate e alterar elas, estamos
	 * automaticamente alterando as soluções em solutionList também.
	 */

	@Override
	public List<S> evaluate(List<S> solutionList, Problem<S> problem) {
		this.solutionList = solutionList;
		// lista local vai cair em desuso
		List<S> localEvaluate = new ArrayList<>();
		// lista de lista remotas, tem o tamanho da lista de id de servidores
		List<List<S>> remotEvaluate = new ArrayList<>();
		if (this.calsRemoteEvatuatorTimes % 50 == 0 && calsRemoteEvatuatorTimes != 0) {
			this.consultServerOnline();
		}
		this.calsRemoteEvatuatorTimes += 1;

		// numero de servidores disponivel
		int numberOfServers = this.severAndIdList.size();
		// preparando x listas de solution onde x= numero de servidores
		for (int i = 0; i < numberOfServers; i++) {
			if (this.severAndIdList.get(i).isStatusOnLine()) {
				List<S> l = new ArrayList<>();
				remotEvaluate.add(l);
			}
		}

		if (this.isStartEvaluate) {
			// distribuindo as sulutios entre as listas de solution no esquema uma pra mim
			// outra pra tu
			int SeverIdIndice = 0;
			int remotIndice = 0;
			List<SeverAndId> holdNullThere = new ArrayList<>();
			int nullThethereIndex = 0;
			for (SeverAndId s : severAndIdList) {
				if (!(this.severAndIdList.get(SeverIdIndice).isStatusOnLine())) {
					holdNullThere.add(s);
				}
				SeverIdIndice += 1;
			}
			severAndIdList.removeAll(holdNullThere);
			int i = 0;
			// distribuindo as sulutios entre as listas de solution no esquema uma m=pra mim
			// outra pra tu
			for (S s : solutionList) {
				remotEvaluate.get(i).add(s);
				i += 1;
				if (i == this.severAndIdList.size()) {
					i = 0;
				}

			}
			severAndIdList.addAll(holdNullThere);
			this.isStartEvaluate = false;
		} else {// ditribuindo solucion de acordo com a velocidade do servidor
			consultVelocity();
			int remotIndice = 0;
			int severIndice = 0;
			int slice = 0;
			int lastSlice = 0;
			for (SeverAndId s : this.severAndIdList) {
				slice += s.getSlice();
				if (severIndice == (this.severAndIdList.size() - 1)) {
					slice = solutionList.size();
				}
				for (int i = lastSlice; i < slice; i++) {
					remotEvaluate.get(remotIndice).add(solutionList.get(i));
				}
				if (s.isStatusOnLine()) {
					remotIndice += 1;
				}
				lastSlice = slice;
				severIndice += 1;
			}
		}

		this.localEvaluate = localEvaluate;
		this.remotEvaluate = remotEvaluate;
		this.problem = problem;
		List<RemotePoolEvaluate> rList = new ArrayList<>();
		int remotIndice = 0;
		for (int w = 0; w < this.severAndIdList.size(); w++) {
			if (this.severAndIdList.get(w).isStatusOnLine()) {
				RemotePoolEvaluate y = new RemotePoolEvaluate(this.remotEvaluate.get(remotIndice), problem,
						this.severAndIdList.get(w));
				rList.add(y);
				remotIndice += 1;
			}

		}

		for (int namThSliceRemEvaluate = 0; namThSliceRemEvaluate < this.remotEvaluate
				.size(); namThSliceRemEvaluate++) {
			rList.get(namThSliceRemEvaluate).setName(Integer.toString(namThSliceRemEvaluate));
		}

		for (RemotePoolEvaluate r : rList) {
			r.start();

		}
		try {
			for (RemotePoolEvaluate r : rList) {
				r.join();

			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		for (RemotePoolEvaluate r : rList) {
			if (r.getSeverAnId().isStatusOnLine()) {
				this.AuxiliarCountParallelEvaluation += r.getAuxiliarCountParallelEvaluation();
			}

		}

		for (RemotePoolEvaluate r : rList) {
			for (SeverAndId s : this.severAndIdList) {
				if (r.getParallelEvaluateId() == s.getId()) {
					s.setExecutionTime(r.getExecutionTime());
					s.setLastEvaluateSize(r.getLastEvaluateSize());
				}
			}

		}

		for (RemotePoolEvaluate r : rList) {
			if (r.getSeverAnId().isStatusOnLine()) {
				this.AuxiliarCountParallelEvaluation += r.getAuxiliarCountParallelEvaluation();
			}

		}

		return solutionList;
	}

	public void consultVelocity() {
		List<Double> velocity = new ArrayList<>();
		double totalDurationTime = 0;
		// descobrir o tempo totatl da ultima avaliação entre os servidore
		for (SeverAndId s : this.severAndIdList) {
			if (s.isStatusOnLine()) {
				totalDurationTime += s.getExecutionTime();
			}

		}
		// criando o tempo desejavel para cada um do servidores
		int timeSliceDivisor = 0;
		for (SeverAndId s : this.severAndIdList) {
			if (s.isStatusOnLine()) {
				timeSliceDivisor += 1;
			}

		}
		double timeSlice = totalDurationTime / timeSliceDivisor;// obsercar, se não houver servidores aqui pode dar
																// infinito ou exceção
		// calculando a velocidade e a fatia de soluções
		// que sera entregue para cada um dos servidores
		int totalSlice = 0;
		for (SeverAndId s : this.severAndIdList) {
			if (s.isStatusOnLine()) {
				if (s.getExecutionTime() < 1) {
					s.setExecutionTime(1);
				}
				s.setVelocity((double) s.getLastEvaluateSize() / s.getExecutionTime());
				if (s.getVelocity() > 10000000000.00) {
					System.out.println(s.getVelocity());
				}
				s.setSlice((int) (s.getVelocity() * timeSlice));
				totalSlice += s.getSlice();
			}

		}
		int index = 0;
		while ((totalSlice > this.solutionList.size())) {
			if (this.severAndIdList.get(index).isStatusOnLine()) {
				if (this.severAndIdList.get(index).getSlice() > 1) {
					this.severAndIdList.get(index).setSlice(this.severAndIdList.get(index).getSlice() - 1);
				} else {
					this.severAndIdList.get(index).setSlice(1);
				}

				index += 1;
				if (index == this.severAndIdList.size()) {
					index = 0;
				}
				totalSlice = 0;
				for (SeverAndId s : this.severAndIdList) {
					if (s.isStatusOnLine()) {
						totalSlice = totalSlice += s.getSlice();
					}

				}

			} else {
				index += 1;
				if (index == this.severAndIdList.size()) {
					index = 0;
				}
			}

		}

		for (SeverAndId s : this.severAndIdList) {
			if (s.isStatusOnLine()) {
				if (s.getSlice() < 1) {
					System.out.println(s.getSlice());
				}
			}

		}

		Collections.sort(this.severAndIdList);
	}

	public void consultServerOnline() {
		ObjectMapper mapper = new ObjectMapper();
		for (SeverAndId s : this.severAndIdList) {

			String adress = s.getUrl().get(0);
			Socket soc = null;
			boolean answer = false;
			boolean isProblemInstaceate = false;

			try {
				int serverPort = Integer.parseInt(s.getUrl().get(1));
				soc = new Socket(adress, serverPort);
				DataInputStream in = new DataInputStream(soc.getInputStream());
				DataOutputStream out = new DataOutputStream(soc.getOutputStream());
				List<String> l = new ArrayList<>();
				String textOut = "AreYouOnline";
				l.add(textOut);
				textOut = mapper.writeValueAsString(l);
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
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					// se houve resposta ele ta on line
					s.setStatusOnLine(true);
					System.out.println("servidor esta online");

				}
				answer = mapper.readValue(data, boolean.class);

			} catch (UnknownHostException e) {
				System.out.println("Socket:" + e.getMessage());
			} catch (EOFException e) {
				System.out.println("EOF:" + e.getMessage());
			} catch (IOException e) {
				System.out.println("readline:" + e.getMessage() + " in sever " + adress + " was not possible"
						+ ": it will be marked as not online");
				s.setStatusOnLine(false);
			} finally {
				if (soc != null)
					try {
						soc.close();
					} catch (IOException e) {
						System.out.println("close:" + e.getMessage());
					}
			}

			if (answer) {
				try {
					int serverPort = Integer.parseInt(s.getUrl().get(1));
					soc = new Socket(adress, serverPort);
					DataInputStream in = new DataInputStream(soc.getInputStream());
					DataOutputStream out = new DataOutputStream(soc.getOutputStream());
					List<String> l = new ArrayList<>();
					String textOut = "DoYouHaveThisIdProblem";
					l.add(textOut);
					l.add(s.getId().toString());
					textOut = mapper.writeValueAsString(l);
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
						mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
						isProblemInstaceate = mapper.readValue(data, boolean.class);
						if (isProblemInstaceate) {
							System.out.println("o problema ainda esta instanciado");
						} else {
							System.out.println("não existe mais instância do problema sera criado uma outra");
						}

					}

				} catch (UnknownHostException e) {
					System.out.println("Socket:" + e.getMessage());
				} catch (EOFException e) {
					System.out.println("EOF:" + e.getMessage());
				} catch (IOException e) {
					System.out.println("readline:" + e.getMessage() + " in sever " + adress + " was not possible"
							+ ": it will be marked as not online");
					s.setStatusOnLine(false);
				} finally {
					if (soc != null)
						try {
							soc.close();
						} catch (IOException e) {
							System.out.println("close:" + e.getMessage());
						}
				}

			}

			if (!isProblemInstaceate && answer) {
				try {
					int serverPort = Integer.parseInt(s.getUrl().get(1));
					soc = new Socket(adress, serverPort);
					DataInputStream in = new DataInputStream(soc.getInputStream());
					DataOutputStream out = new DataOutputStream(soc.getOutputStream());
					// int length = in.readInt();
					String textOut = s.getCreateProblema();
					byte[] b = textOut.getBytes(StandardCharsets.UTF_8);
					out.writeInt(b.length); // write length of the message
					out.write(b);
					String data = in.readUTF();
					// retorno
					UUID id = UUID.fromString(data);
					s.setId(id);
					s.setStatusOnLine(true);
					System.out.println("foi instanciado um novo problema");

				} catch (UnknownHostException e) {
					System.out.println("Socket:" + e.getMessage());
				} catch (EOFException e) {
					System.out.println("EOF:" + e.getMessage());
				} catch (IOException e) {
					System.out.println("readline:" + e.getMessage() + " in sever " + adress + " was not possible"
							+ ": it will be marked as not online");
					s.setStatusOnLine(false);
				} finally {
					if (soc != null)
						try {
							soc.close();
						} catch (IOException e) {
							System.out.println("close:" + e.getMessage());
						}
				}

			}
		}
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	public int getAuxiliarCountParallelEvaluation() {
		return AuxiliarCountParallelEvaluation;
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
	private String url;
	private Integer serverPort;
	private int AuxiliarCountParallelEvaluation;
	private long executionTime;
	private int lastEvaluateSize;
	private SeverAndId severAnId;

	public RemotePoolEvaluate(List<S> remotEvaluate, Problem<S> problem, SeverAndId severAnId) {

		this.remotEvaluate = remotEvaluate;
		this.problem = problem;
		this.ParallelEvaluateId = severAnId.getId();
		url = severAnId.getUrl().get(0);
		serverPort = Integer.parseInt(severAnId.getUrl().get(1));
		this.AuxiliarCountParallelEvaluation = 0;
		this.severAnId = severAnId;
	}

	@Override
	public void run() {
		Instant start = Instant.now();
		Socket soc = null;
		ObjectMapper mapper = new ObjectMapper();
		String textOut = null;
		try {
			textOut = mapper.writeValueAsString(remotEvaluate);
			List<String> l = new ArrayList<>();
			l.add("EvaluateSolution");
			l.add((this.ParallelEvaluateId).toString());
			l.add(textOut);
			textOut = mapper.writeValueAsString(l);

		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String adress = this.url;
		try {
			int serverPort = this.serverPort;
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
			this.AuxiliarCountParallelEvaluation = pReturned.size();
			this.lastEvaluateSize = pReturned.size();
			Instant finish = Instant.now();
			this.executionTime = Duration.between(start, finish).toSeconds();

		} catch (UnknownHostException e) {
			System.out.println("Socket:" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("readline:" + e.getMessage() + " in sever " + adress + " was not possible"
					+ ": it will be marked as not online");
			this.severAnId.setStatusOnLine(false);
			LocalPoolEvaluate l = new LocalPoolEvaluate(this.remotEvaluate, this.problem);
			l.start();
		} finally {
			if (soc != null)
				try {
					soc.close();
				} catch (IOException e) {
					System.out.println("close:" + e.getMessage());
				}
		}

	}

	public int getAuxiliarCountParallelEvaluation() {
		return AuxiliarCountParallelEvaluation;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public UUID getParallelEvaluateId() {
		return ParallelEvaluateId;
	}

	public int getLastEvaluateSize() {
		return lastEvaluateSize;
	}

	public SeverAndId getSeverAnId() {
		return severAnId;
	}

	public List<S> getRemotEvaluate() {
		return remotEvaluate;
	}

}
