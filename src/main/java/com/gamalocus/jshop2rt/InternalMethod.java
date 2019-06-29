package com.gamalocus.jshop2rt;

import java.util.Vector;

/**
 * Each method at compile time is represented as an instance of this class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class InternalMethod extends InternalElement {
    /**
     * The number of objects already instantiated from this class.
     */
    private static int classCnt = 0;

    /**
     * A <code>Vector</code> of <code>String</code>s each of which represents
     * the label of a branch of this method.
     */
    private final Vector<String> labels;

    /**
     * A <code>Vector</code> of logical preconditions each of which represents
     * the precondition of a branch of this method. Each branch is an
     * alternative on how to decompose the task associated with this method.
     */
    private final Vector<LogicalPrecondition> pres;

    /**
     * A <code>Vector</code> of task lists each of which represents a possible
     * way to decompose the task associated with this method if the
     * corresponding precondition is satisfied in the current state of the
     * world.
     */
    private final Vector<TaskList> subs;

    /**
     * To initialize an <code>InternalMethod</code> object.
     *
     * @param head     head of the method (i.e., the compound task which can be
     *                 decomposed by using this method).
     * @param labelsIn a <code>Vector</code> of <code>String</code> labels.
     * @param presIn   a <code>Vector</code> of logical preconditions.
     * @param subsIn   a <code>Vector</code> of task lists.
     */
    public InternalMethod(Predicate head, Vector<String> labelsIn, Vector<LogicalPrecondition> presIn,
                          Vector<TaskList> subsIn) {
        //-- Set the head of this InternalMethod. Note the use of 'classCnt' to
        //-- make this object distinguishable from other objects instantiated from
        //-- this same class.
        super(head, classCnt++);

        //-- Set the labels, preconditions and task decompositions of
        //-- branches in this method.
        labels = labelsIn;
        pres = presIn;
        subs = subsIn;

        //-- To iterate over branch preconditions.
        //-- For each branch, set the number of variables in the precondition for
        //-- that branch. This will be used to produce the code that will be used
        //-- to find bindings, since a binding is an array of this size.
        for (LogicalPrecondition pre : pres)
            pre.setVarCount(getHead().getVarCount());

        //-- To iterate over task decompositions.
        //-- For each task decomposition, set the number of variables in the task
        //-- list for that decomposition.
        for (TaskList tl : subs)
            tl.setVarCount(getHead().getVarCount());
    }

    /**
     * This function produces the Java code needed to implement this method.
     */
    public String toCode(String label) {
        StringBuilder s = new StringBuilder();

        //-- First produce the initial code for the preconditions of each branch.
        for (int i = 0; i < pres.size(); i++)
            s.append(pres.get(i).getInitCode(String.format("Precondition #%d of %s", i, label)));

        //-- The header of the class for this method at run time. Note the use of
        //-- 'getCnt()' to make the name of this class unique.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t * ").append(getSourcePosForComment()).append(endl);
        s.append("\t */").append(endl);
        s.append("\tpublic static class Method").append(getCnt()).append(" extends Method").append(endl).append("\t{").append(endl);

        //-- The constructor of the class.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t */").append(endl);
        s.append("\t\tpublic Method").append(getCnt()).append("(Domain owner)").append(endl).append("\t\t{").append(endl);

        //-- Call the constructor of the base class (class 'Method') with the code
        //-- that produces the head of this method.
        s.append("\t\t\tsuper(owner, ").append(getHead().toCode(String.format("head of %s", label))).append(");").append(endl);

        //-- Allocate the array to keep the possible task lists that represent
        //-- possible decompositions of this method.
        s.append("\t\t\tTaskList[] subsIn = new TaskList[").append(subs.size()).append("];").append(endl);
        s.append(endl);

        //-- For each possible decomposition,
        for (int i = 0; i < subs.size(); i++) {
            if ((subs.get(i)).isEmpty())
                //-- This decomposition is an empty task list.
                s.append("\t\t\tsubsIn[").append(i).append("] = TaskList.empty;").append(endl);
            else
                //-- This decomposition is not an empty task list, so call the function
                //-- that will produce the task list for this decomposition. This
                //-- function will be implemented later on. Note the use of variable
                //-- 'i' to make the header of the function being called unique.
                s.append("\t\t\tsubsIn[").append(i).append("] = createTaskList").append(i).append("();").append(endl);
        }

        //-- Call the function that sets the method's task list to the array that
        //-- was created and initialized.
        s.append(endl).append("\t\t\tsetSubs(subsIn);").append(endl).append("\t\t}").append(endl).append(endl);

        //-- For each possible decomposition,
        for (int i = 0; i < subs.size(); i++) {
            //-- If the decomposition is not an empty list, we need to implement the
            //-- function that returns this decomposition.
            if (!(subs.get(i)).isEmpty()) {
                //-- The function header.
                s.append("\t\tTaskList createTaskList").append(i).append("()").append(endl).append("\t\t{").append(endl);

                //-- The code that will produce this task list.
                s.append((subs.get(i)).toCode(String.format("Sub-list %d of %s", i, label))).append("\t\t}").append(endl).append(endl);
            }
        }

        //-- Implement the toString function
        s.append("\t\t@Override").append(endl).append("\t\tpublic String toString()").append(endl).append("\t\t{").append(endl);

        //-- Define toString as the label
        s.append("\t\t\treturn \"").append(label).append(" ").append(getSourcePosForToString()).append("\";").append(endl);

        //-- Close the function definition
        s.append("\t\t}").append(endl);

        //-- The function that returns an iterator that can be used to find all the
        //-- bindings that satisfy a given precondition of this method and return
        //-- them one-by-one.
        s.append("\t\tpublic Precondition getIterator(State state, Term[] unifier, int which)").append(endl);
        s.append("\t\t{").append(endl).append("\t\t\tPrecondition p;").append(endl).append(endl);

        //-- The switch statement to choose the appropriate precondition.
        s.append("\t\t\tswitch (which)").append(endl).append("\t\t\t{");

        //-- For each possible decomposition,
        for (int i = 0; i < pres.size(); i++) {
            //-- Retrieve the logical precondition.
            LogicalPrecondition pre = pres.get(i);

            //-- Produce the code that will return the appropriate iterator.
            s.append(endl).append("\t\t\t\tcase ").append(i).append(":").append(endl).append("\t\t\t\t\tp = ");
            s.append(pre.toCode(String.format("Precondition #%d of %s", i, label))).append(";").append(endl);

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

        //-- This function returns the label of a given branch of this method.
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
