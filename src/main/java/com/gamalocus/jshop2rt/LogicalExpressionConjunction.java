package com.gamalocus.jshop2rt;

import java.util.Vector;

/**
 * Each conjunction at compile time is represented as an instance of this
 * class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class LogicalExpressionConjunction extends LogicalExpression {
    private static final long serialVersionUID = 6751445655411440152L;

    /**
     * The number of objects instantiated from this class before this object was
     * instantiated. Used to make the name of the precondition class that
     * implements this conjunction unique.
     */
    private final int cnt;

    /**
     * An array of logical expressions the conjunction of which is represented
     * by this object.
     */
    private final LogicalExpression[] le;

    /**
     * To initialize this conjunction.
     *
     * @param leIn a <code>Vector</code> of logical expressions the conjunction of
     *             which is represented by this object. Note that we use a
     *             <code>Vector</code> rather than an array since at compile time
     *             we do not know how many conjuncts there are in this particular
     *             conjunction.
     */
    public LogicalExpressionConjunction(Vector<LogicalExpression> leIn) {
        le = new LogicalExpression[leIn.size()];

        for (int i = 0; i < leIn.size(); i++)
            le[i] = leIn.get(i);

        cnt = getClassCnt();
    }

    /**
     * This function produces Java code that implements the classes any object
     * of which can be used at run time to represent the conjuncts of this
     * conjunction, and the conjunction itself.
     */
    public String getInitCode(String label) {
        StringBuilder s = new StringBuilder();

        //-- First produce any code needed by the conjuncts.
        for (int i = 0; i < le.length; i++)
            s.append(le[i].getInitCode(String.format("Conjunct #%d of %s", i, label)));

        //-- The header of the class for this conjunction at run time. Note the use
        //-- of 'cnt' to make the name of this class unique.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t * ").append(getSourcePosForComment()).append(endl);
        s.append("\t */").append(endl);
        s.append("\tpublic static class Precondition").append(cnt).append(" extends Precondition").append(endl);

        //-- Defining two arrays for storing the iterators and bindings for each
        //-- conjunct.
        s.append("\t{").append(endl).append("\t\tPrecondition[] p;").append(endl).append("\t\tTerm[][] b;").append(endl);

        //-- The constructor of the class.
        s.append(endl).append("\t\tpublic Precondition").append(cnt).append("(Domain owner, Term[] unifier)").append(endl);

        //-- Allocate the array of iterators.
        //-- Set to one more than the length, and the first one will be blank
        //-- Meant to match up with the bindings, where the first binding will
        //-- be the initial binding.
        s.append("\t\t{").append(endl).append("\t\t\tp = new Precondition[").append(le.length + 1).append("];").append(endl);

        //-- For each conjunct,
        for (int i = 1; i <= le.length; i++) {
            //-- Set the corresponding element in the array to the code that produces
            //-- that conjunct.
            s.append("\t\t\t// ").append(le[i - 1].getSourcePosForComment()).append(endl);
            s.append("\t\t\tp[").append(i).append("] = ").append(le[i - 1].toCode(String.format("Conjunct %d of %s", i, label))).append(";").append(endl);
        }

        //-- Allocate the array of bindings.
        //-- Set to one more than the number of conjuncts.  The first position
        //-- will be the initial binding.
        s.append("\t\t\tb = new Term[").append(le.length + 1).append("][];").append(endl);
        s.append("\t\t\tb[0] = unifier;").append(endl);
        s.append("\t\t\tb[0] = Term.merge( b, 1 );").append(endl).append(endl);

        //-- A conjunction can be potentially satisfied more than once, so the
        //-- default for the 'isFirstCall' flag is false.
        s.append("\t\t\tsetFirst(false);").append(endl).append("\t\t}").append(endl).append(endl);

        //-- Define the 'bind' function.
        s.append("\t\tpublic void bind(Term[] binding)").append(endl).append("\t\t{").append(endl);

        //-- Implement the 'bind' function by:
        //-- First copy the initial binding into the first spot.
        s.append("\t\t\tb[0] = binding;").append(endl);
        s.append("\t\t\tb[0] = Term.merge( b, 1 );").append(endl);
        s.append("\t\t\tp[1].bind(binding);").append(endl);
        for (int i = 1; i <= le.length; i++)
            //-- Reset bindings
            s.append("\t\t\tb[").append(i).append("] = null;").append(endl);

        //-- Define the 'nextBindingHelper' function.
        s.append("\t\t}").append(endl).append(endl).append("\t\tprotected Term[] nextBindingHelper(State state)").append(endl);
        s.append("\t\t{").append(endl);

        //-- Implement the 'nextBindingHelper' function.
        s.append(getInitCodeNext());

        //-- Define the 'resetHelper' function.
        s.append("\t\t}").append(endl).append(endl).append("\t\tprotected void resetHelper(State state)").append(endl).append("\t\t{");
        s.append(endl);

        //-- Implement the 'resetHelper' function.
        s.append(getInitCodeReset());

        //-- Close the function definition
        s.append("\t\t}").append(endl);

        //-- Implement the toString function
        s.append("\t\t@Override").append(endl).append("\t\tpublic String toString()").append(endl).append("\t\t{").append(endl);

        //-- Define toString as the label
        s.append("\t\t\treturn \"").append(label).append(" ").append(getSourcePosForToString()).append("\";").append(endl);

        //-- Close the function definition
        s.append("\t\t}").append(endl);


        //-- Close the class definition and return the resulting string.
        return s + "\t}" + endl + endl;
    }

    /**
     * This function produces Java code that implements the
     * <code>nextBindingHelper</code> function for the precondition object that
     * represents this conjunction at run time.
     *
     * @return the produced code as a <code>String</code>.
     */
    private String getInitCodeNext() {
        StringBuilder s = new StringBuilder("\t\t\tbestMatch = 0;" + endl);
        int i;

        //-- To be used to add appropriate number of tabs to each line of code.
        String tabs;

        //-- Start with the outermost conjunct, and try to find a binding for that
        //-- conjunct. If there is no more binding for that conjunct, try to find
        //-- the next binding for the next outermost conjunct.
        for (i = le.length, tabs = "\t\t\t"; i >= 1; i--, tabs += "\t") {
            if (i != le.length)
                s.append(tabs).append("boolean b").append(i).append("changed = false;").append(endl);
            s.append(tabs).append("while (b[").append(i).append("] == null)").append(endl).append(tabs).append("{").append(endl);
        }

        //-- Try the outer most conjunct.
        s.append(tabs).append("b[1] = p[1].nextBinding(state);").append(endl);
        //-- If there is no more binding for the outermost conjunct, return null.
        s.append(tabs).append("if (b[1] == null)").append(endl);
        s.append(tabs).append("\treturn null;").append(endl);
        s.append(tabs).append("else").append(endl);
        s.append(tabs).append("\tbestMatch = Math.max(bestMatch, 1);").append(endl);
        s.append(tabs).append("b1changed = true;").append(endl);

        //-- Going from third outermost conjunct inward, try to apply newly-found
        //-- bindings for outermost conjuncts to each inner conjunct after reseting
        //-- it, and try to find bindings for inner conjuncts.
        tabs = tabs.substring(0, tabs.length() - 1);
        for (i = 2; i <= le.length; i++, tabs = tabs.substring(0, tabs.length() - 1)) {
            s.append(tabs).append("}").append(endl);
            s.append(tabs).append("if ( b").append(i - 1).append("changed ) {").append(endl);
            s.append(tabs).append("\tp[").append(i).append("].reset(state);").append(endl);
            s.append(tabs).append("\tp[").append(i).append("].bind(Term.merge(b, ").append(i).append("));").append(endl);
            s.append(tabs).append("}").append(endl);
            s.append(tabs).append("b[").append(i).append("] = p[").append(i).append("].nextBinding(state);").append(endl);
            //-- If no binding found, null out the next outermost conjunct so we
            //-- try another set of bindings.
            s.append(tabs).append("if (b[").append(i).append("] == null)").append(endl);
            s.append(tabs).append("\tb[").append(i - 1).append("] = null;").append(endl);
            s.append(tabs).append("else").append(endl);
            s.append(tabs).append("\tbestMatch = Math.max(bestMatch, ").append(i).append(");").append(endl);
            if (i != le.length)
                s.append(tabs).append("b").append(i).append("changed = true;").append(endl);

        }
        s.append("\t\t\t}").append(endl).append(endl);
        //-- Return the result of the merging of the bindings found for each
        //-- conjunct.
        s.append("\t\t\tTerm[] retVal = Term.merge(b, ").append(le.length + 1).append(");").append(endl);
        s.append("\t\t\tb[").append(le.length).append("] = null;").append(endl);
        s.append("\t\t\treturn retVal;").append(endl);
        return s.toString();
    }

    /**
     * This function produces Java code that implements the
     * <code>resetHelper</code> function for the precondtion object that
     * represents this conjunction at run time.
     *
     * @return the produced code as a <code>String</code>.
     */
    private String getInitCodeReset() {
        StringBuilder s = new StringBuilder();
        int i;

        //-- First, reset all the conjuncts.
        for (i = 1; i <= le.length; i++)
            s.append("\t\t\tp[").append(i).append("].reset(state);").append(endl);
        //-- null out intermediate bindings.
        for (i = 1; i <= le.length; i++)
            s.append("\t\t\tb[").append(i).append("] = null;").append(endl);

        return s.toString();
    }

    /**
     * To propagate the variable count to all the logical expressions the
     * conjunction of which this object represents.
     */
    protected void propagateVarCount(int varCount) {
        for (LogicalExpression logicalExpression : le) logicalExpression.setVarCount(varCount);
    }

    /**
     * This function produces the Java code to create an object of the class
     * that was implemented to represent this conjunction at run time.
     */
    public String toCode(String label) {
        return "new Precondition" + cnt + "(owner, unifier)";
    }
}
