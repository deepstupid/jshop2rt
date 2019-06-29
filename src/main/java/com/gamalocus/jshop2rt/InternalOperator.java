package com.gamalocus.jshop2rt;

import java.util.Vector;

/**
 * Each operator at compile time is represented as an instance of this class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class InternalOperator extends InternalElement {
    /**
     * The number of objects already instantiated from this class.
     */
    private static int classCnt = 0;
    /**
     * This operator's add list, a <code>Vector</code> of objects of type
     * <code>DelAddElement</code>. Note that a <code>Vector</code>, rather than
     * an array, is used, since at compile time we do not know how many
     * delete/add elements there will be.
     */
    private final Vector add;
    /**
     * The cost of this operator.
     */
    private final Term cost;

    /**
     * This operator's delete list, a <code>Vector</code> of objects of type
     * <code>DelAddElement</code>. Note that a <code>Vector</code>, rather than
     * an array, is used, since at compile time we do not know how many
     * delete/add elements there will be.
     */
    private final Vector del;

    /**
     * The logical precondition of this operator.
     */
    private final LogicalPrecondition pre;

    /**
     * To initialize an <code>InternalOperator</code> object.
     *
     * @param head   head of the operator (i.e., the primitive task that can be
     *               achieved by applying this operator).
     * @param preIn  the logical precondition for this operator.
     * @param delIn  the delete list of the operator. The first element of the
     *               <code>Vector</code> is of type <code>Integer</code>. If it is not
     *               <code>null</code>, it means that the delete list of this operator
     *               is a variable, and the value of the <code>Integer</code> shows
     *               that variable's index. Otherwise, the rest of the
     *               <code>Vector</code> is of type <code>DelAddElement</code>,
     *               representing the atoms and protections that will be deleted from
     *               the state of the world when this operator is applied.
     * @param addIn  the add list of the operator. The first element of the
     *               <code>Vector</code> is of type <code>Integer</code>. If it is not
     *               <code>null</code>, it means that the add list of this operator
     *               is a variable, and the value of the <code>Integer</code> shows
     *               that variable's index. Otherwise, the rest of the
     *               <code>Vector</code> is of type <code>DelAddElement</code>,
     *               representing the atoms and protections that will be added to the
     *               state of the world when this operator is applied.
     * @param costIn the cost of the operator.
     */
    public InternalOperator(Predicate head, LogicalPrecondition preIn,
                            Vector delIn, Vector addIn, Term costIn) {
        //-- Set the head of this InternalOperator. Note the use of 'classCnt' to
        //-- make this object distinguishable from other objects instantiated from
        //-- the same class.
        super(head, classCnt++);

        //-- Set the precondition of this operator.
        pre = preIn;

        //-- Set the delete list of this operator.
        del = delIn;

        //-- Set the add list of this operator.
        add = addIn;

        //-- Set the cost of the operator.
        cost = costIn;

        //-- Set the number of variables in the precondition of the operator.
        //-- This will be used to produce the code that will be used to find
        //-- bindings, since a binding is an array of this size.
        pre.setVarCount(getHead().getVarCount());

        //-- If the delete list of the operator is not a variable,
        if (del.get(0) == null)
            //-- For each delete/add element there,
            for (int i = 1; i < del.size(); i++)
                //-- Set the number of variables.
                ((DelAddElement) del.get(i)).setVarCount(getHead().getVarCount());

        //-- If the add list of the operator is not a variable,
        if (add.get(0) == null)
            //-- For each delete/add element there,
            for (int i = 1; i < add.size(); i++)
                //-- Set the number of variables.
                ((DelAddElement) add.get(i)).setVarCount(getHead().getVarCount());
    }

    public final String toCode(String label) {
        return toCode(label, false);
    }

    /**
     * This function produces the Java code needed to implement this operator.
     */
    public String toCode(String label, boolean anonymous) {
        StringBuilder s;

        //-- The index of the variable that represents the delete/add list of the
        //-- operator, -1 otherwise (i.e., when the delete/add list of the operator
        //-- is a real list). To be used when the constructor of the operator is
        //-- called at run time.
        int delIdx, addIdx;

        //-- Is there a ForAll Element in this operator's delete/add list.
        boolean hasForAll = false;

        //-- First produce the initial code for the precondition of the operator.
        final String preconditionLabel = "Precondition of " + label;
        s = new StringBuilder(pre.getInitCode(preconditionLabel));

        Integer varIdx = (Integer) del.get(0);
        //-- If the first element of the delete list is null, it means the delete
        //-- list is a real list, so process the list.
        if (varIdx == null) {
            delIdx = -1;

            //-- For each element in the delete list, except the first one of course,
            for (int i = 1; i < del.size(); i++) {
                //-- If the delete/add element is a ForAll element,
                if (del.get(i) instanceof DelAddForAll) {
                    //-- Produce the code that will calculate the bindings that will
                    //-- satisfy its preconditions.
                    s.append(((DelAddForAll) del.get(i)).getExpCode(String.format("Bindings for preconditions of " +
                            "delete part of DelAddElement #%d of %s", i, label)));

                    hasForAll = true;
                }
            }
        } else
            //-- If the first element of the delete list is not null, it means the
            //-- delete list is a variable, and this element represents its index.
            delIdx = varIdx;

        varIdx = (Integer) add.get(0);
        //-- If the first element of the add list is null, it means the add list is
        //-- a real list, so process the list.
        if (varIdx == null) {
            addIdx = -1;

            //-- For each element in the add list, except the first one of course,
            for (int i = 1; i < add.size(); i++) {
                //-- If the delete/add element is a ForAll element,
                if (add.get(i) instanceof DelAddForAll) {
                    //-- Produce the code that will calculate the bindings that will
                    //-- satisfy its preconditions.
                    s.append(((DelAddForAll) add.get(i)).getExpCode(String.format("Bindings for precondition of " +
                            "add part of DelAddElement #%d of %s", i, label)));

                    hasForAll = true;
                }
            }
        } else
            //-- If the first element of the add list is not null, it means the add
            //-- list is a variable, and this element represents its index.
            addIdx = varIdx;

        //-- The header of the class for this operator at run time. Note the use of
        //-- 'getCnt()' to make the name of this class unique.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t * ").append(getSourcePosForComment()).append(endl);
        s.append("\t */").append(endl);

        if (anonymous) {
            s.append("new Operator(owner,");
            s.append(getHead().toCode("head of " + label)).append(", ").append(delIdx).append(", ").append(addIdx);
            s.append(", ").append(cost.toCode("cost of " + label));
            s.append("){").append(endl);

        } else {
            s.append("\tpublic static class Operator").append(getCnt()).append(" extends Operator").append(endl).append('{').append(endl);
        }

        //-- The constructor of the class.
        s.append("\t/**").append(endl);
        s.append("\t * ").append(label).append(endl);
        s.append("\t */").append(endl);

        if (!anonymous) {
            s.append("\t\tpublic Operator").append(getCnt()).append("(Domain owner)").append(endl).append("\t\t{").append(endl);


            //-- Call the constructor of the base class (class 'Operator') with the
            //-- code that produces the head of this method.
            s.append("\t\t\tsuper(").append("owner, ").append(getHead().toCode("head of " + label)).append(", ").append(delIdx).append(", ").append(addIdx);
            s.append(", ").append(cost.toCode("cost of " + label)).append(");").append(endl).append(endl);
        }

        //-- Define a variable that will be used in the constructors of the
        //-- 'LogicalExpression's if there are any ForAll elements.
        if (hasForAll)
            s.append("\t\t\tTerm[] unifier;").append(endl).append(endl);

        //-- If the delete list is a real list, produce the code that will
        //-- initialize the array that represents the delete list.
        if (delIdx == -1) {
            //-- Allocate the array.
            s.append(endl).append("\t\t\tDelAddElement[] delIn = new DelAddElement[");
            s.append(del.size() - 1).append("];").append(endl);

            if(anonymous)
                s.append(endl).append("{").append(endl);
            //-- For each element in the delete list, except for the first one of
            //-- course,
            for (int i = 1; i < del.size(); i++) {
                final String elementLabel = String.format("Delete list of DelAddElement #%d of %s", i, label);



                //-- If it is a ForAll element, produce the code that will initialize
                //-- the list of atoms to be deleted by this element.
                if (del.get(i) instanceof DelAddForAll)
                    s.append(((DelAddForAll) del.get(i)).getInitCode(elementLabel));

                //-- Set the corresponding element in the array to the code that
                //-- produces this delete/add element.
                s.append("\t\t\tdelIn[").append(i - 1).append("] = ");
                s.append(((DelAddElement) del.get(i)).toCode(elementLabel)).append(';').append(endl);

            }

            //-- Set the delete list of the operator to the array just created.
            s.append(endl).append("\t\t\tsetDel(delIn);").append(endl);
        }
        if(anonymous)
            s.append(endl).append("}").append(endl);

        //-- If the add list is a real list, produce the code that will initialize
        //-- the array that represents the add list.
        if (addIdx == -1) {
            s.append(endl).append("\t\t\tDelAddElement[] addIn = new DelAddElement[");
            s.append(add.size() - 1).append("];").append(endl);

            if(anonymous)
                s.append(endl).append("{").append(endl);

            //-- For each element in the add list, except for the first one of
            //-- course,
            for (int i = 1; i < add.size(); i++) {
                final String elementLabel = String.format("Add list of DelAddElement #%d of %s", i, label);

                //-- If it is a ForAll element, produce the code that will initialize
                //-- the list of atoms to be added by this element.
                if (add.get(i) instanceof DelAddForAll)
                    s.append(((DelAddForAll) add.get(i)).getInitCode(elementLabel));

                //-- Set the corresponding element in the array to the code that
                //-- produces this delete/add element.
                s.append("\t\t\taddIn[").append(i - 1).append("] = ");
                s.append(((DelAddElement) add.get(i)).toCode(elementLabel)).append(';').append(endl);
            }

            //-- Set the add list of the operator to the array just created.
            s.append(endl).append("\t\t\tsetAdd(addIn);").append(endl);
            if(anonymous)
                s.append(endl).append("}").append(endl);

        }

        if (!anonymous) {
            //-- Close the constructor.
            s.append("\t\t}").append(endl).append(endl);
        }

        //-- Implement the toString function
        s.append("\t\t@Override").append(endl).append("\t\tpublic String toString()").append(endl).append("\t\t{").append(endl);

        //-- Define toString as the label
        s.append("\t\t\treturn \"").append(label).append(' ').append(getSourcePosForToString()).append("\";").append(endl);

        //-- Close the function definition
        s.append("\t\t}").append(endl);

        //-- The function that returns an iterator that can be used to find all the
        //-- bindings that satisfy this operator's precondition and return them
        //-- one-by-one.
        s.append("\t\tpublic Precondition getIterator(State state, Term[] unifier, int which)");
        s.append(endl).append("\t\t{").append(endl).append("\t\t\tPrecondition p;").append(endl).append(endl);

        //-- Produce the code that will return the appropriate iterator.
        s.append("\t\t\tp = ").append(pre.toCode(preconditionLabel)).append(';').append(endl);

        //-- If the logical precondition is marker ':first', set the appropriate
        //-- flag.
        if (pre.getFirst())
            s.append("\t\t\tp.setFirst(true);").append(endl);

        //-- Reset the precondition and return it, and close the function.
        s.append("\t\t\tp.reset(state);").append(endl).append(endl).append("\t\t\treturn p;").append(endl).append("\t\t}");

        s.append(endl).append("\t}");
        if (!anonymous)
            s.append(endl).append(endl);

        //-- Close the class definition and return the resulting string.
        return s.toString();
    }
}
