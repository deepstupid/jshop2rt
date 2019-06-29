package com.gamalocus.jshop2rt;

import java.util.Vector;

/**
 * Each disjunction at compile time is represented as an instance of this
 * class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class LogicalExpressionDisjunction extends LogicalExpression {
    private static final long serialVersionUID = 8012782595759344067L;

    /**
     * The number of objects instantiated from this class before this object was
     * instantiated. Used to make the name of the precondition class that
     * implements this disjunction unique.
     */
    private final int cnt;

    /**
     * An array of logical expressions the disjunction of which is represented
     * by this object.
     */
    private final LogicalExpression[] le;

    /**
     * To initialize this disjunction.
     *
     * @param leIn a <code>Vector</code> of logical expressions the disjunction of
     *             which is represented by this object. Note that we use a
     *             <code>Vector</code> rather than an array since at compile time
     *             we do not know how many disjuncts there are in this particular
     *             disjunction.
     */
    public LogicalExpressionDisjunction(Vector<LogicalExpression> leIn) {
        le = new LogicalExpression[leIn.size()];

        for (int i = 0; i < leIn.size(); i++)
            le[i] = leIn.get(i);

        cnt = getClassCnt();
    }

    /**
     * This function produces Java code that implements the classes any object
     * of which can be used at run time to represent the disjuncts of this
     * disjunction, and the disjunction itself.
     */
    public String getInitCode(String label) {
        StringBuilder s = new StringBuilder();
        int i;

        //-- First produce any code needed by the disjuncts.
        for (i = 0; i < le.length; i++)
            s.append(le[i].getInitCode(String.format("Disjunct #%d of %s", i, label)));

        //-- The header of the class for this disjunction at run time. Note the use
        //-- of 'cnt' to make the name of this class unique.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t * ").append(getSourcePosForComment()).append(endl);
        s.append("\t */").append(endl);
        s.append("\tpublic static class Precondition").append(cnt).append(" extends Precondition").append(endl);

        //-- Defining two arrays for storing the iterators for each disjunct and
        //-- the current binding.
        s.append("\t{").append(endl).append("\t\tPrecondition[] p;").append(endl).append("\t\tTerm[] b;").append(endl);

        //-- Defining an integer to keep track of which disjunct has already been
        //-- considered.
        s.append("\t\tint whichClause;").append(endl).append(endl);

        //-- The constructor of the class.
        s.append("\t\tpublic Precondition").append(cnt).append("(Domain owner, Term[] unifier)").append(endl).append("\t\t{");

        //-- Allocate the array of iterators.
        s.append(endl).append("\t\t\tp = new Precondition[").append(le.length).append("];").append(endl);

        //-- For each disjunct,
        for (i = 0; i < le.length; i++)
            //-- Set the corresponding element in the array to the code that produces
            //-- that disjunct.
            s.append("\t\t\tp[").append(i).append("] = ").append(le[i].toCode(String.format("Disjunct #%d of %s", i, label))).append(";").append(endl).append(endl);

        //-- A conjucntion can be potentially satisfied more than once, so the
        //-- default for the 'isFirstCall' flag is false.
        s.append("\t\t\tsetFirst(false);").append(endl).append("\t\t}").append(endl).append(endl);

        //-- Define the 'bind' function.
        s.append("\t\tpublic void bind(Term[] binding)").append(endl).append("\t\t{").append(endl);

        //-- Implement the 'bind' function by:
        for (i = 0; i < le.length; i++)
            //-- Binding each disjunct in this disjunction.
            s.append("\t\t\tp[").append(i).append("].bind(binding);").append(endl);

        //-- Define the 'nextBindingHelper' function.
        s.append("\t\t}").append(endl).append(endl).append("\t\tprotected Term[] nextBindingHelper(State state)").append(endl);
        s.append("\t\t{");

        //-- Implement the 'nextBindingHelper' function by iterating over all
        //-- disjuncts:
        s.append(endl).append("\t\t\twhile (whichClause < ").append(le.length).append(")").append(endl);

        //-- Look for the next binding for the current disjunct.
        s.append("\t\t\t{").append(endl).append("\t\t\t\tb = p[whichClause].nextBinding(state);").append(endl);

        //-- If there is such a binding, return it.
        s.append("\t\t\t\tif (b != null)").append(endl).append("\t\t\t\t\t return b;").append(endl);

        //-- Otherwise, try the next disjunct.
        s.append("\t\t\t\twhichClause++;").append(endl).append("\t\t\t}");

        //-- If there are no more disjuncts left, return null.
        s.append(endl).append(endl).append("\t\t\treturn null;").append(endl).append("\t\t}").append(endl).append(endl);

        //-- Implement the toString function
        s.append("\t\t@Override").append(endl).append("\t\tpublic String toString()").append(endl).append("\t\t{").append(endl);

        //-- Define toString as the label
        s.append("\t\t\treturn \"").append(label).append(" ").append(getSourcePosForToString()).append("\";").append(endl);

        //-- Close the function definition
        s.append("\t\t}").append(endl);

        //-- Define the 'resetHelper' function.
        s.append("\t\tprotected void resetHelper(State state)").append(endl).append("\t\t{").append(endl);

        //-- Implement the 'resetHelper' function by resetting all the disjuncts
        //-- and set the varaible that keeps track of which disjuncts have already
        //-- been considered to 0.
        for (i = 0; i < le.length; i++)
            s.append("\t\t\tp[").append(i).append("].reset(state);").append(endl);

        return s + "\t\t\twhichClause = 0;" + endl + "\t\t}" + endl + "\t}" + endl +
                endl;
    }

    /**
     * To propagate the variable count to all the logical expressions the
     * disjunction of which this object represents.
     */
    protected void propagateVarCount(int varCount) {
        for (LogicalExpression logicalExpression : le) logicalExpression.setVarCount(varCount);
    }

    /**
     * This function produces the Java code to create an object of the class
     * that was implemented to represent this disjunction at run time.
     */
    public String toCode(String label) {
        return "new Precondition" + cnt + "(owner, unifier) /*" + label + "*/";
    }


}
