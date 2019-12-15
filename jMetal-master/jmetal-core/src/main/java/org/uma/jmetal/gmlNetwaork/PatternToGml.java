package org.uma.jmetal.gmlNetwaork;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.solution.IntegerSolution;

import br.cns.model.GmlData;
import br.cns.model.GmlEdge;
import br.cns.model.GmlNode;
import br.cns.persistence.GmlDao;
import cbic15.Pattern;

/**
 * classe criada para transformação do padrão da solution em redes para
 * trabalhar com o simulador simtom. estar classe era do projeto clusterPe, foi
 * trazida para o jmetamclusterPe por causa da necessidade de escrever as redes
 * em formato .gml a toda vez que se escrevesse as saídas do alg, tipo a cada 20
 * iterações, como o clusterPe tem depenência com o jmetal, ele não vai perder a
 * ultilização da classe.
 * 
 * @author jorge candeias
 *
 */

public class PatternToGml implements Serializable {
	private GmlData gml;
	private Map mapNode;

	@JsonCreator
	public PatternToGml(@JsonProperty("gml") GmlData gml) {
		Map<Integer, GmlNode> map = new HashMap<Integer, GmlNode>();
		this.gml = gml;
		for (GmlNode G : gml.getNodes()) {
			map.put(G.getId(), G);
		}
		this.mapNode = map;
	}

	public List<GmlNode> patternGml(Pattern[] ArrayPatterns) {
		List<GmlNode> listNode = new ArrayList<>();
		for (int i = 0; i < ArrayPatterns.length; i++) {
			Integer id = ArrayPatterns[i].getId();
			listNode.add((GmlNode) this.mapNode.get(ArrayPatterns[i].getId()));
		}
		return listNode;
	}
/**
 * o objetivo desse metodo eh analizar o array de bits da solution e 
 * transformar no formato edge do arquivo gml, a bem da verdade em uma 
 * lista de objetos GmlEdge
 * @param arrayPatterns
 * @param vars
 * @return objeto BooleanAndEdge
 */
	public BooleanAndEdge makelink(Pattern[] arrayPatterns, Integer[] vars) {
		int VarIndex = 0;
		boolean have = false;
		BooleanAndEdge B = new BooleanAndEdge();
		List<GmlEdge> edges = new ArrayList<>();
		List<GmlEdge> falseEdges = new ArrayList<>();
		GmlEdge falseEdge = new GmlEdge();
		falseEdge.setSource((GmlNode) this.mapNode.get(arrayPatterns[1].getId()));
		falseEdge.setTarget((GmlNode) this.mapNode.get(arrayPatterns[2].getId()));
		falseEdges.add(falseEdge);
		for (int i = 0; i < arrayPatterns.length; i++) {
			for (int j = i; j < arrayPatterns.length; j++) {
				if (i != j) {
					if (vars[VarIndex] == 1) {
						GmlEdge edge = new GmlEdge();
						edge.setSource((GmlNode) this.mapNode.get(arrayPatterns[i].getId()));
						edge.setTarget((GmlNode) this.mapNode.get(arrayPatterns[j].getId()));
						edges.add(edge);
						have = true;
					}
					VarIndex += 1;

				}

			}
		}

		if (have) {
			B.setEdges(edges);
		} else {
			B.setEdges(falseEdges);
		}

		B.setHave(have);

		return B;
	}

	public GmlData takeGmlData(Pattern[] arrayPatterns, Integer[] vars) {
		GmlDao G = new GmlDao();
		GmlData gmlLocal = new GmlData();
		gmlLocal.setNodes(patternGml(arrayPatterns));
		BooleanAndEdge B = makelink(arrayPatterns, vars);
		gmlLocal.setEdges(B.getEdges());
		gmlLocal.createComplexNetwork();
		gmlLocal = G.loadGmlDataFromContent(G.createFileContent(gmlLocal));
		return gmlLocal;
	}

	public void patternGmlData(Pattern[] arrayPatterns, Integer[] vars) {
		List<GmlNode> listNode = new ArrayList<>();
		GmlDao G = new GmlDao();
		GmlData gmlLocal = new GmlData();
		GmlEdge edge = new GmlEdge();
		gmlLocal.setNodes(patternGml(arrayPatterns));
		BooleanAndEdge B = makelink(arrayPatterns, vars);
		gmlLocal.setEdges(B.getEdges());
		gmlLocal.createComplexNetwork();
		G.save(gmlLocal, "src/GmlevaluatingMax.gml");
		// G.save(gmlLocal,
		// "C:/Users/jorge/workspace/ClusterPe/src/GmlevaluatingMax.gml");

	}

	public void saveGmlFromSolution(String patch, IntegerSolution solution) {
		Pattern[] arrayPatterns = solution.getLineColumn();
		Integer[] vars = new Integer[solution.getNumberOfVariables()];
		for (int i = 0; i < vars.length; i++) {
			vars[i] = solution.getVariableValue(i);
		}
		Map<String, String> informations = new HashMap();
		informations.put("Country", "Brazil");
		informations.put("PB", Double.toString(solution.getObjective(0)));
		informations.put("Capex", Double.toString(solution.getObjective(1)));
		informations.put("Consumo em Watts", Double.toString(solution.getObjective(2)));
		informations.put("Conectividade Alg�brica", Double.toString(solution.getObjective(3)));
		GmlDao G = new GmlDao();
		GmlData gmlLocal = new GmlData();
		gmlLocal.setNodes(patternGml(arrayPatterns));
		BooleanAndEdge B = makelink(arrayPatterns, vars);
		gmlLocal.setEdges(B.getEdges());
		gmlLocal.setInformations(informations);
		gmlLocal.createComplexNetwork();
		G.save(gmlLocal, patch);

	}

	public GmlData getGml() {
		return gml;
	}

	public void setGml(GmlData gml) {
		this.gml = gml;
	}

	public Map getMapNode() {
		return mapNode;
	}

	public void setMapNode(Map mapNode) {
		this.mapNode = mapNode;
	}

	/**
	 * gerando os gts e sets
	 * 
	 * 
	 */

	// ***************************************

}
