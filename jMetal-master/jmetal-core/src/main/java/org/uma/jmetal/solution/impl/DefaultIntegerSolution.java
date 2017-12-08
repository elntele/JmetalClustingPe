package org.uma.jmetal.solution.impl;

import java.util.HashMap;

import org.uma.jmetal.problem.IntegerProblem;
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
    return new DefaultIntegerSolution(this);
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



}
