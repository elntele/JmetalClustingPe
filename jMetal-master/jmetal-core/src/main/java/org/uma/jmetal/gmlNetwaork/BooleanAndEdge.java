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
/**
 * objeto carrega uma lista de links ou edges e um validador booleano
 * chamado have, não faço ideia do que essse have sinaliza mas a logica
 * de atruibuicao de valor falso ou verdadeiro dele, ao que parece, eh que
 * ele eh setado como true se houver links validos na rede que ele 
 * faz parte. Um outro detalhe esquisito eh que se nao houver links validos
 * o metodo que lida com ele (o metodo que eu achei pode hever outros)
 * se nao encontrar links no array solution, do qual ele eh construido, seta o have
 *  como false e coloca um link entre a segunda e a terceira cidade do array
 *  de cidades da solution  que ele pertence, chama inclusive de falso endge.
 *  Me parece uma gambiarra, mas não lembro pra que eu fiz isso.
 * o metodo de que falo esta na classe PatternToGml e se chama
 * makelink
 * @author jorge canedeias
 *
 */
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
