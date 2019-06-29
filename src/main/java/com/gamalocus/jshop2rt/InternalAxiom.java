package com.gamalocus.jshop2rt;

import java.util.Vector;

/**
 * Each axiom at compile time is represented as an instance of this class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class InternalAxiom extends InternalElement {
    /**
     * The number of objects already instantiated from this class.
     */
    private static int classCnt = 0;
    /**
     * A <code>Vector</code> of logical expressions each of which represents a
     * branch of the axiom. Note that we use a <code>Vector</code> rather than
     * an array because at compile time we do not know how many branches a
     * particular axiom will have.
     */
    private final Vector<LogicalPrecondition> branches;
    /**
     * A <code>Vector</code> of <code>String</code>s each of which represents
     * the label of a branch of this axiom.
     */
    private final Vector<String> labels;

    /**
     * To initialize an <code>InternalAxiom</code> object.
     *
     * @param head       head of the axiom.
     * @param branchesIn a <code>Vector</code> of logical expressions each of which
     *                   represents a branch of the axiom.
     * @param labelsIn   a <code>Vector</code> of <code>String</code> labels.
     */
    public InternalAxiom(Predicate head, Vector<LogicalPrecondition> branchesIn, Vector<String> labelsIn) {
        //-- Set the head of this InternalAxiom. Note the use of 'classCnt' to make
        //-- this object distinguishable from other objects instantiated from this
        //-- same class.
        super(head, classCnt++);

        //-- Set the branches of the axiom, and their labels.
        branches = branchesIn;
        labels = labelsIn;

        //-- For each branch, set the number of variables in the precondition for
        //-- that branch. This will be used to produce the code that will be used
        //-- to find bindings, since a binding is an array of this size.
        for (LogicalPrecondition pre : branchesIn)
            pre.setVarCount(getHead().getVarCount());
    }

    /**
     * This function produces the Java code needed to implement this axiom.
     */
    public String toCode(String label) {
        StringBuilder s = new StringBuilder();

        //-- First produce the initial code for the preconditions of each branch.
        for (int i = 0; i < branches.size(); i++)
            s.append(branches.get(i).getInitCode(String.format("Precondition of branch #%d of %s", i, label)));

        //-- The header of the class for this axiom at run time. Note the use of
        //-- 'getCnt()' to make the name of this class unique.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t * ").append(getSourcePosForComment()).append(endl);
        s.append("\t */").append(endl);
        s.append("\tpublic static class Axiom").append(getCnt()).append(" extends Axiom").append(endl).append("{").append(endl);

        //-- The constructor of the class.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t */").append(endl);
        s.append("\t\tpublic Axiom").append(getCnt()).append("(Domain owner)").append(endl).append("\t\t{").append(endl);

        //-- Call the constructor of the base class (class 'Axiom') with the code
        //-- that produces the head of this axiom, and number of branches of this
        //-- axiom as its parameters.
        s.append("\t\t\tsuper(owner, ").append(getHead().toCode(String.format("Head of %s", label))).append(", ").append(branches.size()).append(");");
        s.append(endl).append("\t\t}").append(endl).append(endl);

        //-- Implement the toString function
        s.append("\t\t@Override").append(endl).append("\t\tpublic String toString()").append(endl).append("\t\t{").append(endl);

        //-- Define toString as the label
        s.append("\t\t\treturn \"").append(label).append(" ").append(getSourcePosForToString()).append("\";").append(endl);

        //-- Close the function definition
        s.append("\t\t}").append(endl);

        //-- The function that returns an iterator that can be used to find all the
        //-- bindings that satisfy a given precondition of this axiom and return
        //-- them one-by-one.
        s.append("\t\tpublic Precondition getIterator(State state, Term[] unifier, int which)");
        s.append(endl).append("\t\t{").append(endl).append("\t\t\tPrecondition p;").append(endl).append(endl);

        //-- The switch statement to choose the appropriate precondition.
        s.append("\t\t\tswitch (which)").append(endl).append("\t\t\t{");

        //-- For each branch,
        for (int i = 0; i < branches.size(); i++) {
            //-- Retrieve the logical precondition.
            LogicalPrecondition pre = branches.get(i);

            //-- Produce the code that will return the appropriate iterator.
            s.append(endl).append("\t\t\t\tcase ").append(i).append(":").append(endl).append("\t\t\t\t\tp = ");
            s.append(pre.toCode(String.format("Precondition of branch #%d of %s", i, label))).append(";").append(endl);

            //-- If the logical precondition is marker ':first', set the appropriate
            //-- flag.
            if (pre.getFirst())
                s.append("\t\t\t\t\tp.setFirst(true);").append(endl);

            s.append("\t\t\t\tbreak;");
        }

        //-- Close the switch statement.
        s.append(endl).append("\t\t\t\tdefault:").append(endl).append("\t\t\t\t\treturn null;").append(endl);
        s.append("\t\t\t}").append(endl);

        //-- Reset the precondition and return it.
        s.append(endl).append("\t\t\tp.reset(state);").append(endl).append(endl).append("\t\t\treturn p;").append(endl);

        //-- This function returns the label of a given branch of this axiom.
        s.append("\t\t}").append(endl).append(endl).append("\t\tpublic String getLabel(int which)").append(endl);

        //-- The switch statement to choose the appropriate label.
        s.append("\t\t{").append(endl).append("\t\t\tswitch (which)").append(endl).append("\t\t\t{");

        //-- For each branch;
        for (int i = 0; i < labels.size(); i++)
            //-- Return its associated label.
            s.append(endl).append("\t\t\t\tcase ").append(i).append(": return \"").append(labels.get(i)).append("\";");

        //-- Close the switch statement.
        s.append(endl).append("\t\t\t\tdefault: return null;").append(endl).append("\t\t\t}").append(endl);

        //-- Close the function definition and the class definition and return the
        //-- resulting string.
        return s + "\t\t}" + endl + "\t}" + endl + endl;
    }
}
