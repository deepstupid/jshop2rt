package com.gamalocus.jshop2rt;

/**
 * This class represents a predicate with an integer number associated with
 * it. It serves two different purposes: First, to represent a protection on
 * some predicate (the integer number being the number of times the predicate
 * is protected), and second, when a predicate is deleted from the current
 * state of the world, an object of this class represents the deleted
 * predicate and where it was deleted from so that in case of a backtrack the
 * deleted predicate can be added back exactly where it was before.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
class NumberedPredicate {
    /**
     * The integer.
     */
    private int number;

    /**
     * The predicate.
     */
    private final Predicate pre;

    /**
     * To initialize an object of this class. The integer will be set to 1.
     *
     * @param preIn the predicate.
     */
    public NumberedPredicate(Predicate preIn) {
        pre = preIn;
        number = 1;
    }

    /**
     * To initialize an object of this class.
     *
     * @param preIn    the predicate.
     * @param numberIn the integer.
     */
    public NumberedPredicate(Predicate preIn, int numberIn) {
        pre = preIn;
        number = numberIn;
    }

    /**
     * To decrease the integer by one. This is used when a protection is
     * deleted.
     *
     * @return <code>false</code> if the integer become zero, <code>true</code>
     * otherwise.
     */
    public boolean dec() {
        if (number > 1) {
            number--;
            return true;
        } else
            return false;
    }

    /**
     * To get the head of the predicate.
     *
     * @return the head of the predicate.
     */
    public int getHead() {
        return pre.head;
    }

    /**
     * To get the integer associated with this object.
     *
     * @return the integer associated with this object.
     */
    public int getNumber() {
        return number;
    }

    /**
     * To get the parameters of the predicate.
     *
     * @return the parameters of the predicate.
     */
    public Term getParam() {
        return pre.param;
    }

    /**
     * @return the predicate itself.
     */
    public Predicate getPredicate() {
        return pre;
    }

    /**
     * To increase the integer by one. This is used when a protection is added.
     */
    public void inc() {
        number++;
    }

    public String toString() {
        return pre.toString();
    }
}
