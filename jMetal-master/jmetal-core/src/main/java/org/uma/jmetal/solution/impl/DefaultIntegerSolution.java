package org.uma.jmetal.solution.impl;

import java.util.HashMap;
import java.util.List;

import org.uma.jmetal.problem.IntegerProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.IntegerSolution;

import cbic15.Pattern;


/**
 * Defines an implementation of an integer solution
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
@SuppressWarnings("serial")
public class DefaultIntegerSolution
    extends AbstractGenericSolution<Integer, IntegerProblem>
    implements IntegerSolution {
	private Pattern[] lineColumn;

  /** Constructor */
  public DefaultIntegerSolution(IntegerProblem problem) {
    super(problem) ;
    initializeIntegerVariables();
    initializeObjectiveValues();
   
  }
  
  /**
	 * construtor vazio colocado apenas por causa do mapeamento do json Jackson
	 * para a parte de paralelistmo, antes isso não existia no projeto, não use para outra coisa
	 */

  public DefaultIntegerSolution() {
	   

	  }
  

  /** Copy constructor */
  public DefaultIntegerSolution(DefaultIntegerSolution solution) {
    super(solution.problem) ;

    for (int i = 0; i < problem.getNumberOfVariables(); i++) {
      setVariableValue(i, solution.getVariableValue(i));
    }

    for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
      setObjective(i, solution.getObjective(i)) ;
    }

    attributes = new HashMap<Object, Object>(solution.attributes) ;
  }

  @Override
  public Integer getUpperBound(int index) {
    return problem.getUpperBound(index);
  }

  @Override
  public Integer getLowerBound(int index) {
    return problem.getLowerBound(index) ;
  }

  @Override
  public DefaultIntegerSolution copy() {
	  Pattern[] localLineCollunm=this.getLineColumn();
	  DefaultIntegerSolution solution= new DefaultIntegerSolution(this);
	  solution.setLineColumn(localLineCollunm);
    return solution;
	  //return new DefaultIntegerSolution(this);
  }

  @Override
  public String getVariableValueString(int index) {
    return getVariableValue(index).toString() ;
  }
  
  private void initializeIntegerVariables() {
    for (int i = 0 ; i < problem.getNumberOfVariables(); i++) {
      Integer value = randomGenerator.nextInt(getLowerBound(i), getUpperBound(i));
      setVariableValue(i, value) ;
    }
  }

@Override
public Pattern[] getLineColumn() {
	// TODO Auto-generated method stub
	return this.lineColumn;
}

@Override
public void setLineColumn(Pattern[] name) {
	this.lineColumn=name;	
}
@Override
public List getvariables() {
	return variables;
}
@Override
public double[] getObjectives() {
	return objectives;
}
@Override
public void setObjectives(double[] objectives) {
	this.objectives = objectives;
}


}
