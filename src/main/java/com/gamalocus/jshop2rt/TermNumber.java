package com.gamalocus.jshop2rt;

/**
 * Each numerical term both at compile time and at run time, is an instance
 * of this class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class TermNumber extends Term {
    private static final long serialVersionUID = -7696061382184586801L;

    /**
     * The value of the numerical term.
     */
    private final double number;

    /**
     * To initialize this numerical term.
     *
     * @param numberIn the value this numerical term is set to have.
     */
    public TermNumber(double numberIn) {
        number = numberIn;
    }

    /**
     * Since this term is a numerical term, binding will not change it,
     * therefore, simply this numerical term itself is returned.
     */
    public Term bind(Term[] binding) {
        return this;
    }

    /**
     * Whether or not another term is equivalent to this numerical term.
     */
    private boolean equals(Term t) {
        //-- 't' is not a numerical term.
        if (!(t instanceof TermNumber))
            return false;

        //-- Check if 't' has the same numerical value.
        return (number == ((TermNumber) t).number);
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(number);
        return 31 + (int) (temp ^ (temp >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TermNumber other = (TermNumber) obj;
        return Double.doubleToLongBits(number) == Double.doubleToLongBits(other.number);
    }

    /**
     * Find a unifier between this numerical term and another given term. Since
     * this term is a number, this boils down to whether or not the other given
     * term is equal to this one.
     */
    public boolean findUnifier(Term t, Term[] binding) {
        return ((t instanceof TermVariable) || equals(t));
    }

    /**
     * To get the value of this numerical term.
     *
     * @return the value of this numerical term.
     */
    public double getNumber() {
        return number;
    }

    public int intValue() {
        return (int) number;
    }

    public double floatValue() {
        return (float) number;
    }

    public double doubleValue() {
        return number;
    }

    /**
     * This function always returns <code>true</code> because a numerical term
     * is always ground by definition.
     */
    public boolean isGround() {
        return true;
    }

    /**
     * This function produces Java code to create this numerical.
     */
    public String toCode(String label) {
        return "new TermNumber(" + number + ")";
    }

    /**
     * This function returns the <code>String</code> representation of the value
     * of this numerical term.
     */
    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
