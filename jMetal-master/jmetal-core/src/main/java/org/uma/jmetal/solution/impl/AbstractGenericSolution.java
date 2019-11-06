package org.uma.jmetal.solution.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import cbic15.Pattern;

/**
 * Abstract class representing a generic solution
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public abstract class AbstractGenericSolution<T, P extends Problem<?>> implements Solution<T> {
  protected double[] objectives;// era provate coloquei protected para maper o jon
  protected List<T> variables; // era provate coloquei protected para maper o jon
  protected P problem ;
  protected Map<Object, Object> attributes ;
  protected final JMetalRandom randomGenerator ;

  /**
   * Constructor
   */
  protected AbstractGenericSolution(P problem) {
    this.problem = problem ;
    attributes = new HashMap<>() ;
    randomGenerator = JMetalRandom.getInstance() ;

    objectives = new double[problem.getNumberOfObjectives()] ;
    variables = new ArrayList<>(problem.getNumberOfVariables()) ;
    for (int i = 0; i < problem.getNumberOfVariables(); i++) {
      variables.add(i, null) ;
    }
  }
  /**
	 * construtor vazio colocado apenas por causa do mapeamento do json Jackson
	 * para a parte de paralelistmo, antes isso não existia no projeto, não use para outra coisa
	 */

  protected AbstractGenericSolution() {
	  randomGenerator = JMetalRandom.getInstance() ;
	  attributes = new HashMap<>() ;// teste
  }
  

  @Override
  public void setAttribute(Object id, Object value) {
    attributes.put(id, value) ;
  }

  @Override
  public Object getAttribute(Object id) {
    return attributes.get(id) ;
  }

  @Override
  public void setObjective(int index, double value) {
    objectives[index] = value ;
  }

  @Override
  public double getObjective(int index) {
    return objectives[index];
  }

  @Override
  public T getVariableValue(int index) {
    return variables.get(index);
  }

  @Override
  public void setVariableValue(int index, T value) {
    variables.set(index, value);
  }

  @Override
  public int getNumberOfVariables() {
    return variables.size();
  }

  @Override
  public int getNumberOfObjectives() {
    return objectives.length;
  }

  protected void initializeObjectiveValues() {
    for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
      objectives[i] = 0.0 ;
    }
  }

  @Override
  public String toString() {
    String result = "Variables: " ;
    for (T var : variables) {
      result += "" + var + " " ;
    }
    result += "Objectives: " ;
    for (Double obj : objectives) {
      result += "" + obj + " " ;
    }
    result += "\t" ;
    result += "AlgorithmAttributes: " + attributes + "\n" ;

    return result ;
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AbstractGenericSolution<?, ?> that = (AbstractGenericSolution<?, ?>) o;

    if (!attributes.equals(that.attributes))
      return false;
    if (!Arrays.equals(objectives, that.objectives))
      return false;
    if (!variables.equals(that.variables))
      return false;

    return true;
  }

  @Override public int hashCode() {
    int result = Arrays.hashCode(objectives);
    result = 31 * result + variables.hashCode();
    result = 31 * result + attributes.hashCode();
    return result;
  }
  

  /**
	 * construtor vazio colocado apenas por causa do mapeamento do json Jackson
	 * para a parte de paralelistmo, antes isso não existia no projeto, não use para outra coisa
	 */

public void setVariables(List<T> variables) {
	this.variables = variables;
}
public List<T> getvariables() {
	// TODO Auto-generated method stub
	return variables;
}
public double[] getObjectives() {
	return objectives;
}
public void setObjectives(double[] objectives) {
	this.objectives = objectives;
}



  
}
