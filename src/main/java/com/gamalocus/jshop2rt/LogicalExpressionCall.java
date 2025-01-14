package com.gamalocus.jshop2rt;

/**
 * Each call term in a logical expression at compile time is represented as an
 * instance of this class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class LogicalExpressionCall extends LogicalExpression {
    private static final long serialVersionUID = -4181529820747119843L;

    /**
     * The call term this object represents.
     */
    private final TermCall term;

    /**
     * To initialize this call logical expression.
     *
     * @param termIn the call term this object represents.
     */
    public LogicalExpressionCall(TermCall termIn) {
        term = termIn;
    }

    /**
     * This class does not need any initialization code, therefore, this
     * function simply returns an empty <code>String</code>.
     */
    public String getInitCode(String label) {
        return "";
    }

    public LogicalExpression getNNF(boolean negated) {
        return negated ? new LogicalExpressionNegation(this) : this;
    }

    /**
     * This class does not need to propagate the variable count, therefore, this
     * function does nothing.
     */
    protected void propagateVarCount(int varCount) {
    }

    /**
     * This function produces the Java code to create a
     * <code>PreconditionCall</code> object that represents this call logical
     * expression at run time.
     */
    public String toCode(String label) {
        return "new PreconditionCall(" + term.toCode(label) + ", unifier)";
    }
}
