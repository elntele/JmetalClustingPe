package org.uma.jmetal.gmlNetwaork;

import java.util.ArrayList;
/**
 * classe criada para auxiliar a classse PatternToGML transformação do padrão 
 * da solution em redes para trabalhar com o simulador simtom. estar classe era
 *  do projeto clusterPe, foi trazida para o jmetamclusterPe por causa da 
 *  necessidade de escrever as redes em  formato .gml a toda vez que se escrevesse
 *   as saídas do alg,  tipo a cada 20 iterações
 * @author jorge candeias
 *
 */
import java.util.List;

import br.cns.model.GmlEdge;

public class BooleanAndEdge {
	private List<GmlEdge> edges = new ArrayList<>();
	private boolean have;
	public List<GmlEdge> getEdges() {
		return edges;
	}
	public void setEdges(List<GmlEdge> edges) {
		this.edges = edges;
	}
	public boolean isHave() {
		return have;
	}
	public void setHave(boolean have) {
		this.have = have;
	}
	public BooleanAndEdge() {
		super();
	}
	

}
