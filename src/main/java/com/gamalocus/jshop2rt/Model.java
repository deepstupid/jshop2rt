package com.gamalocus.jshop2rt;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;

/**
 * Each domain at compile time is represented as an instance of this class.
 *
 * @author Okhtay Ilghami
 * @author <a href="http://www.cs.umd.edu/~okhtay">http://www.cs.umd.edu/~okhtay</a>
 * @version 1.0.3
 */
public class Model {
    private static final String DEFAULT_OUTPUT_PACKAGE = "net.gamalocus.cotwl2.ai.htnplanner";
    /**
     * The new line character in the platform JSHOP2 is running on.
     */
    private final static String endl = System.getProperty("line.separator");
    public final StringBuffer src = new StringBuffer(16 * 1024);
    /**
     * A <code>List</code> of <code>String</code> names of compound tasks seen
     * so far in the domain.
     */
    private final List<String> compoundTasks = new ArrayList<>();
    public final List<String> constants = new ArrayList<>();
    /**
     * The parser object that will parse this domain.
     */
    private final JSHOP2Parser parser;
    private final List<String> primitiveTasks = new ArrayList<>();
    /**
     * Package header of generated java files.
     */
    private String outputPackage = DEFAULT_OUTPUT_PACKAGE;
    /**
     * A <code>List</code> of axioms seen so far in the domain description.
     * Each member is of type <code>InternalAxiom</code>.
     */
    public final List<InternalAxiom> axioms = new ArrayList<>();
    /**
     * A <code>List</code> of <code>String</code> names of user-defined
     * external code calls that must be imported before being used in the
     * domain description.
     */
    private final List<String> calcs = new ArrayList<>();
    /**
     * Map of comparator instances to class names and constructor parameters.
     */
    private final Set<java.util.List<String>> comparators = new HashSet<>();
    private int constantsSize;
    /**
     * A <code>List</code> of methods seen so far in the domain description.
     * Each member is of type <code>InternalMethod</code>.
     */
    private final List<InternalMethod> methods = new ArrayList<>();
    /**
     * The <code>String</code> name of the domain.
     */
    private String name;
    /**
     * A <code>List</code> of operators seen so far in the domain description.
     * Each member is of type <code>InternalOperator</code>.
     */
    public final List<InternalOperator> operators = new ArrayList<>();
    /**
     * The <code>String</code> name of the planning problem.
     */
    private String probName;
    public int varsMaxSize;

    public Model(String input, String outputPackageIn) {
        this(new ByteArrayInputStream(input.getBytes()),
                outputPackageIn);
    }

    /**
     * To initialize this domain.
     *
     * @param fin           the file from which the domain description is to be read.
     * @param outputPackage Package header for generated java files.
     */
    private Model(InputStream is, String outputPackageIn) {
        outputPackage = outputPackageIn;

        //-- Initialize the lexer and the parser associated with this object.
        JSHOP2Lexer lexer = new JSHOP2Lexer(is);
        parser = new JSHOP2Parser(lexer);
        parser.initialize(lexer, this);
    }

    /**
     * Replaces the file extension of the file.
     */
    private static String replaceExtension(String name, String newExtension) {
        int dotPos = name.lastIndexOf(".");
        if (dotPos != -1) {
            return name.substring(0, dotPos) + newExtension;
        } else {
            return name + newExtension;
        }
    }

    public static long getSourceHash(File inputFile, String algorithm)
            throws IOException {
        MessageDigest d;
        try {
            d = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("Digest algorithm %s was not available.", algorithm), e);
        }

        try (InputStream in = new BufferedInputStream(new FileInputStream(inputFile))) {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();

            byte[] buf = new byte[4096];
            int count;
            while ((count = in.read(buf)) != -1) {
                d.update(buf, 0, count);
                tmp.write(buf, 0, count);
            }

            if (tmp.size() != inputFile.length()) {
                throw new AssertionError("Bug: Did not cover the entire file.");
            }

            final byte[] digest = d.digest();

            long collapsedDigest = 0;
            for (int i = 0; i < digest.length; ++i) {
                final long b = digest[i] & 255;
                final long part = b << ((i % 8) * 8);
                collapsedDigest ^= part;
            }

            return collapsedDigest;
        }
    }

    /**
     * This function saves a given <code>List</code> of <code>String</code>s
     * in a given file.
     *
     * @param dest the file where the <code>List</code> is to be saved.
     * @param list the <code>List</code> to be saved.
     * @throws IOException
     */
    public static void dumpStringArray(BufferedWriter dest, List<?> list)
            throws IOException {
        String buff;

        //-- First write the size of the List.
        buff = list.size() + endl;
        dest.write(buff, 0, buff.length());

        //-- Then, write the elements of the List one-by-one.
        for (Object o : list) {
            buff = o + endl;
            dest.write(buff, 0, buff.length());
        }
    }

    public Model domain() throws RecognitionException, TokenStreamException, IOException {
        parser.domain();
        return this;
    }

    public Model problem() throws RecognitionException, TokenStreamException, IOException {
        parser.command();
        return this;
    }

    /**
     * To add an axiom to the list of axioms read from the file.
     *
     * @param axiom the axiom to be added.
     */
    public void addAxiom(InternalAxiom axiom) {
        axioms.add(axiom);
    }

    /**
     * To add a <code>String</code> used as a name of a compound task in the
     * domain description to the list of compound task names, in case it has not
     * been added before.
     *
     * @param s the <code>String</code> to be added.
     * @return the index assigned to this name.
     */
    public int addCompoundTask(String s) {
        int index;

        //-- If this name has not been added before, add it to the end of the
        //-- List and return its index.
        if ((index = compoundTasks.indexOf(s)) == -1) {
            compoundTasks.add(s);
            return compoundTasks.size() - 1;
        }

        //-- Otherwise, just return its index.
        return index;
    }

    /**
     * To add a <code>String</code> used as a constant symbol in the domain
     * description to the list of constant symbols, in case it has not been
     * added before.
     *
     * @param s the <code>String</code> to be added.
     * @return the index assigned to this name.
     */
    public int addConstant(String s) {
        int index;

        //-- If this name has not been added before, add it to the end of the
        //-- List and return its index.
        List<String> c = constants();
        if ((index = c.indexOf(s)) == -1) {
            c.add(s);
            return c.size() - 1;
        }

        //-- Otherwise, just return its index.
        return index;
    }

    /**
     * To add the <code>String</code> name of an external code call to the list
     * of such code calls.
     *
     * @param what the name of the code call being added.
     */
    public void addCalc(String what) {
        if (!calcs.contains(what))
            calcs.add(what);
    }

    /**
     * To add the <code>String</code> name of an external code call to the list
     * of such code calls.
     *
     * @param what the name of the code call being added.
     */
    public void addComparator(String what, String instanceName, String... args) {
        ArrayList<String> value = new ArrayList<>(args.length + 2);
        value.add(what);
        value.add(instanceName);
        Collections.addAll(value, args);
        comparators.add(value);
    }

    /**
     * To add a method to the list of methods read from the file.
     *
     * @param method the method to be added.
     */
    public void addMethod(InternalMethod method) {
        methods.add(method);
    }

    /**
     * To add an operator to the list of operators read from the file.
     *
     * @param op the operator to be added.
     */
    public void addOperator(InternalOperator op) {
        operators.add(op);
    }

    /**
     * To add a <code>String</code> used as a name of a primitive task in the
     * domain description to the list of primitive task names, in case it has not
     * been added before.
     *
     * @param s the <code>String</code> to be added.
     * @return the index assigned to this name.
     */
    public int addPrimitiveTask(String s) {
        int index;

        //-- If this name has not been added before, add it to the end of the
        //-- List and return its index.
        if ((index = getPrimitiveTasks().indexOf(s)) == -1) {
            getPrimitiveTasks().add(s);
            return getPrimitiveTasks().size() - 1;
        }

        //-- Otherwise, just return its index.
        return index;
    }

    /**
     * This function writes the Java code necessary to produce this domain at
     * run time in the appropriate file.
     *
     * @param varsMaxSize the maximum number of variables seen in any variable scope in
     *                    this domain.
     */
    public void close(int varsMaxSize) {
        //-- To hold the String to be written.
        StringBuilder s;

        //-- Package placement.
        s = new StringBuilder("package " + outputPackage + ';' + endl);

        //-- JSHOP2 classes should be imported first.
        s.append(String.format("import %s.*;", getClass().getPackage().getName())).append(endl).append(endl);

        //-- Produce the class that represents the domain itself.
        s.append("public class ").append(name).append(" extends Domain").append(endl).append('{').append(endl);

//    long sourceHash = getSourceHash(inputFile, "SHA");
//    s += String.format("\tprivate static final long serialVersionUID = %dL;\n",
//        sourceHash) + endl + endl;

        // FIXME Compute indices of methods, operators and axioms.

        //-- Produce the nested classes that represent the operators.
        for (InternalOperator op : operators)
            s.append(op.toCode(String.format("Operator #%d for primitive task %s", -1, getPrimitiveTasks().get(op.getHead().head))));

        //-- Produce the nested classes that represent the methods.
        for (InternalMethod me : methods)
            s.append(me.toCode(String.format("Method %d for compound task %s", -1, compoundTasks.get(me.getHead().head))));

        //-- Produce the nested classes that represent the axioms.
        for (InternalAxiom ax : axioms)
            s.append(ax.toCode(String.format("Branch %d for axiom %s", -1, constants().get(ax.getHead().head))));

//    //-- Add time stamp and location of source file.
//    s += String.format("\tpublic static final String sourcePath = \"%s\";" + endl,
//    		inputFile.getCanonicalPath().replace("\\", "\\\\").replace("\"", "\\\""));
//    s += String.format("\tpublic static final long sourceLastModified = %dL;" + endl + endl,
//    		inputFile.lastModified());

        //-- Take care of the user-defined external code calls first by
        //-- instantiating an  object of that class to do the calculations.
        for (String imp : calcs) {
            s.append("\tpublic ").append(imp).append(" calculate").append(imp).append(" = new ").append(imp).append("();").append(endl).append(endl);
        }

        for (java.util.List<String> comparator : comparators) {
            StringBuilder args = new StringBuilder();
            for (String arg : comparator.subList(2, comparator.size())) {
                args.append(String.format(args.length() == 0 ? "%s" : ", %s", arg));
            }

            s.append(String.format("\tpublic %s %s = new %1$s(%s);",
                    comparator.get(0),
                    comparator.get(1),
                    args.toString())).append(endl).append(endl);
        }

        //-- Produce the constructor for the class that represents this domain.
        s.append("\tpublic ").append(name).append("()").append(endl).append("\t{").append(endl);

        //-- Produce the array that maps constant symbols to integers.
        s.append(listToCode(constants(), "constants"));
        //-- Produce the array that maps compound tasks to integers.
        s.append(listToCode(compoundTasks, "compoundTasks"));
        //-- Produce the array that maps primitive tasks to integers.
        s.append(listToCode(getPrimitiveTasks(), "primitiveTasks"));

        //-- To initialize an array of the variable symbols the size of which is
        //-- equal to the maximum number of variables seen in any scope in the
        //-- domain. This way, all the variable symbols that have the same index
        //-- will point to the same thing rather than pointing to duplicate copies.
        s.append("\t\tinitializeTermVariables(").append(this.varsMaxSize = varsMaxSize).append(");").append(endl).append(endl);

        //-- Same, but for constant symbols.
        s.append("\t\tinitializeTermConstants();").append(endl).append(endl);

        //-- Allocate an array of type 'Method[]'. The size of the array is the
        //-- number of compound tasks in the domain, and each element of the array
        //-- represents all the methods that can be used to decompose the
        //-- corresponding compound task.
        s.append("\t\tmethods = new Method[").append(compoundTasks.size()).append("][];").append(endl).append(endl);

        //-- For each compound task,
        for (int i = 0; i < compoundTasks.size(); i++) {
            //-- To store the number of methods that can decompose this compound
            //-- task.
            int j = 0;

            //-- To iterate over the methods.
            //-- First iterate over the methods to find out how many methods can
            //-- decompose this compound task.
            for (InternalMethod m : methods) {
                if (m.getHead().head == i)
                    j++;
            }

            //-- Allocate an array of right size.
            s.append("\t\tmethods[").append(i).append("] = new Method[").append(j).append("];").append(endl);

            j = 0;

            //-- Next, iterate over the methods again, this time to add the methods
            //-- that can decompose this compound task to the array.
            for (InternalMethod m : methods) {
                if (m.getHead().head == i)
                    s.append("\t\tmethods[").append(i).append("][").append(j++).append("] = new Method").append(m.getCnt()).append("(this);").append(endl);
            }

            s.append(endl);
        }

        //-- Allocate an array of type 'Operator[]'. The size of the array is the
        //-- number of primitive tasks in the domain, and each element of the array
        //-- represents all the operators that can be used to achieve the
        //-- corresponding primitive task.
        int size = getPrimitiveTasks().size();
        s.append(endl).append("\t\tops = new Operator[").append(size).append("][];").append(endl).append(endl);

        //-- For each primitive task,
        for (int i = 0; i < size; i++) {
            //-- To store the number of operators that can achieve this primitive
            //-- task.
            int j = 0;

            //-- To iterate over the operators.
            //-- First iterate over the operators to find out how many operators can
            //-- achieve this primitive task.
            for (InternalOperator o : operators) {
                if (o.getHead().head == i)
                    j++;
            }

            //-- Allocate an array of the right size.
            s.append("\t\tops[").append(i).append("] = new Operator[").append(j).append("];").append(endl);

            j = 0;
            //-- Next, iterate over the operators again, this time to add the
            //-- operators that can achieve this primitive task to the array.
            for (InternalOperator o : operators) {
                if (o.getHead().head == i)
                    s.append("\t\tops[").append(i).append("][").append(j++).append("] = new Operator").append(o.getCnt()).append("(this);").append(endl);
            }

            s.append(endl);
        }

        //-- Allocate an array of type 'Axiom[]'. The size of the array is the
        //-- number of constant symbols in the domain, and each element of the
        //-- array represents all the axioms that can be used to prove predicates
        //-- which start with the corresponding constant symbol.
        s.append("\t\taxioms = new Axiom[").append(constants().size()).append("][];").append(endl).append(endl);

        //-- For each constant symbol,
        for (int i = 0; i < constants().size(); i++) {
            //-- To store the number of axioms that can prove predicates that start
            //-- with this constant symbol.
            int j = 0;

            //-- To iterate over the axioms.
            //-- First iterate over the axioms to find out how many axioms can be
            //-- used to prove the predicates that start with this constant symbol.
            for (InternalAxiom a : axioms) {
                if (a.getHead().head == i)
                    j++;
            }

            //-- Allocate an array of the right size.
            s.append("\t\taxioms[").append(i).append("] = new Axiom[").append(j).append("];").append(endl);

            j = 0;

            //-- Next, iterate over the axioms again, this time to add the axioms
            //-- that can be used to prove the predicates that start with this
            //-- constant symbol to the array.
            for (InternalAxiom a : axioms) {
                if (a.getHead().head == i)
                    s.append("\t\taxioms[").append(i).append("][").append(j++).append("] = new Axiom").append(a.getCnt()).append("(this);").append(endl);
            }

            s.append(endl);
        }

        //-- Close the constructor and the class.
        s.append("\t}").append(endl).append('}');

        src.append(s);
    }

    /**
     * This function performs some necessary initialization when a problem file
     * is being compiled, mainly reading and parsing the text file associated
     * with the domain the planning problem is defined in.
     *
     * @throws IOException
     */
    public void commandInitialize() throws IOException {
//        //-- To read the text file that stores the names of the constant symbols
//        //-- that appeared in the domain description.
//        BufferedReader src;
//
//        //-- Open the file.
//        src = new BufferedReader(new FileReader(name + ".txt"));
//
//        //-- Read in the constant symbols.
//        constantsSize = readStringArray(src, constants);
//
//        //-- Read in the compound task names.
//        readStringArray(src, compoundTasks);
//
//        //-- Read in the primitive task names.
//        readStringArray(src, primitiveTasks);
//
//        //-- Close the file.
//        src.close();
    }

    /**
     * This function writes the Java code necessary to produce these planning
     * problems at run time in the appropriate file.
     *
     * @param states    the list of initial state of the world, one per each planning
     *                  problem.
     * @param taskLists the list of the task lists to be achieved, one per each planning
     *                  problem.
     */
    public void commandToCode(LinkedList<List<Predicate>> states, LinkedList<TaskList> taskLists) {
        //-- To hold the String to be written.
        StringBuilder s;

        //-- Package placement.
        s = new StringBuilder("package " + outputPackage + ';' + endl);

        //-- Import the appropriate packages.
        s.append("import java.util.LinkedList;").append(endl).append(String.format("import %s.*;", getClass().getPackage().getName())).append(endl).append(endl);

        //-- Define the class that represents this planning problem.
        s.append("public class ").append(probName).append(endl).append('{').append(endl);

//    //-- Add time stamp and location of source file.
//    s += String.format("\tpublic static final String sourcePath = \"%s\";" + endl,
//    		inputFile.getAbsolutePath().replace("\\", "\\\\"));
//    s += String.format("\tpublic static final long sourceLastModified = %dL;" + endl + endl,
//    		inputFile.lastModified());

        //-- This function defines and allocate the array that will hold the String
        //-- names of the constant symbols that appeared in the problem description
        //-- but not in the domain description.
        s.append("\tprivate static String[] defineConstants()").append(endl).append("\t{").append(endl);
        s.append("\t\tString[] problemConstants = new String[").append(constants().size() - getConstantsSize()).append("];").append(endl).append(endl);

        //-- Set the values of elements of that array.
        for (int i = getConstantsSize(); i < constants().size(); i++)
            s.append("\t\tproblemConstants[").append(i - getConstantsSize()).append("] = \"").append(constants().get(i)).append("\";").append(endl);

        s.append(endl).append("\t\treturn problemConstants;").append(endl).append("\t}").append(endl).append(endl);

        //-- For each planning problem, initialize the current state of the world
        //-- to the initial state of the world in the problem description.

        //-- The index of the problem being solved.
        int problemIdx = 0;

        //-- For each problem,
        for (List<Predicate> state : states) {
            s.append("\tprivate static void createState").append(problemIdx++).append("(State s)").append("\t{").append(endl);


            //-- For each predicate, in the initial world state of the problem
            for (Predicate p : state) {
                //-- Check if the predicate's head appears in the domain too. If not,
                //-- we don't need to add it to the world state because it doesn't make
                //-- a difference.
                // FIXME Use toString method with domain and namespace.
                if (p.head < getConstantsSize())
                    s.append("\t\ts.add(").append(p.toCode(p.toString())).append(");").append(endl);
            }

            s.append("\t}").append(endl).append(endl);
        }

        //-- Define the main function.
        s.append("\tpublic static LinkedList<Plan> getPlans()").append(endl).append("\t{").append(endl);
        //-- List for all plans to be stored in
        s.append("\t\tLinkedList<Plan> returnedPlans = new LinkedList<Plan>();").append(endl);

        //-- To initialize an array of the constant symbols that we already know
        //-- exist so that there will be no duplicate copies of those constant
        //-- symbols.
        s.append("\t\tTermConstant.initialize(").append(constants().size()).append(");").append(endl).append(endl);

        //-- Instantiate an object of the class that represents the planning
        //-- domain.
        s.append("\t\tDomain d = new ").append(name).append("();").append(endl).append(endl);

        //-- Call the function that passes this array to the the object that
        //-- represents the domain.
        s.append("\t\td.setProblemConstants(defineConstants());").append(endl).append(endl);

        //-- Initialize the object that will represent the current state of the
        //-- world.
        s.append("\t\tState s = new State(").append(getConstantsSize()).append(", d.getAxioms());").append(endl);

        //-- Pass the domain description and the initial state of the world to the
        //-- JSHOP2 algorithm.
        s.append(endl);
        s.append("\t\tJSHOP2 jShop2Planner = new JSHOP2();").append(endl);
        s.append("\t\tjShop2Planner.initialize(d, s);").append(endl).append(endl);

        //-- Define the task list variable and the thread that solves the problems.
        s.append("\t\tTaskList tl;").append(endl).append("\t\tSolverThread thread;").append(endl).append(endl);

        //-- The index of the problem being solved.
        problemIdx = 0;

        //-- For each problem,
        for (TaskList tl : taskLists) {
            //-- If this is not the first problem, clear the variable that represents
            //-- the initial world state.
            if (problemIdx != 0)
                s.append(endl).append("\t\ts.clear();").append(endl);

            //-- Create the world state for this problem.
            s.append("\t\tcreateState").append(problemIdx).append("(s);").append(endl);

            //-- Create the initial task list.
            s.append(endl).append(tl.getInitCode(String.format("Task list of problem #%d", problemIdx), "tl")).append(endl);

            //-- Define the thread that will solve this planning problem.
            s.append("\t\tthread = new SolverThread(jShop2Planner, tl, Integer.MAX_VALUE);").append(endl);

            //-- Start the thread that will solve this planning problem.
            s.append("\t\tthread.start();").append(endl).append(endl);

            //-- Wait till thread is done, since JSHOP2's data members are static and
            //-- can handle only one problem at a time.
            // FIXME JSHOP2 is now thread-safe.
            s.append("\t\ttry {").append(endl).append("\t\t\twhile (thread.isAlive())").append(endl);
            s.append("\t\t\t\tThread.sleep(500);").append(endl);
            s.append("\t\t} catch (InterruptedException e) {").append(endl).append("\t\t}").append(endl);
            s.append(endl).append("\t\treturnedPlans.addAll( thread.getPlans() );").append(endl).append(endl);

            problemIdx++;
        }
        s.append("\t\treturn returnedPlans;").append(endl);
        s.append("\t}").append(endl).append(endl).append("\tpublic static LinkedList<Predicate> getFirstPlanOps() {");
        s.append(endl).append("\t\treturn getPlans().getFirst().getOps();").append(endl);
        s.append("\t}").append(endl).append('}');

        src.append(s);
    }

    /**
     * This function returns the number of axioms in this domain.
     *
     * @return the number of axioms in this domain.
     */
    public int getAxiomNo() {
        return axioms.size();
    }

    /**
     * This function returns the <code>List</code> where the
     * <code>String</code> names of the compound tasks in this domain are
     * stored.
     *
     * @return the <code>List</code> where the <code>String</code> names of
     * the compound tasks in this domain are stored.
     */
    public List<String> getCompoundTasks() {
        return compoundTasks;
    }

    /**
     * A <code>List</code> of <code>String</code> names of constant symbols
     * seen so far in the domain.
     */ /**
     * This function returns the <code>List</code> where the
     * <code>String</code> names of the constant symbols in this domain are
     * stored.
     *
     * @return the <code>List</code> where the <code>String</code> names of
     * the constant symbols in this domain are stored.
     */
    public List<String> constants() {
        return constants;
    }

    /**
     * This function returns the number of methods in this domain.
     *
     * @return the number of methods in this domain.
     */
    public int getMethodNo() {
        return methods.size();
    }

    /**
     * This function returns the <code>String</code> name of this domain.
     *
     * @return the <code>String</code> name of this domain.
     */
    public String getName() {
        return name;
    }

    /**
     * To set the name of this planning domain.
     *
     * @param nameIn the name of this planning domain.
     */
    public void setName(String nameIn) {
        name = nameIn;
    }

//  public File getSymbolDumpOutputPath()
//  {
//	return new File(outputFile.getParent(), replaceExtension(outputFile.getName(), ".txt"));
//  }
//
//  public File getProblemOutputPath()
//  {
//	 return new File(outputFile.getParent(), probName + ".java");
//  }

    /**
     * A <code>List</code> of <code>String</code> names of primitive tasks
     * seen so far in the domain.
     */ /**
     * This function returns the <code>List</code> where the
     * <code>String</code> names of the primitive tasks in this domain are
     * stored.
     *
     * @return the <code>List</code> where the <code>String</code> names of
     * the primitive tasks in this domain are stored.
     */
    public List<String> getPrimitiveTasks() {
        return primitiveTasks;
    }

    /**
     * Retreive generated Java source code.
     */
    public String getOutput() {
        return src.toString();
    }

    /**
     * This function reads a <code>List</code> of <code>String</code>s from
     * a given file.
     *
     * @param src  the input file.
     * @param list the <code>List</code> to be read.
     * @return the number of the elements in the <code>List</code>.
     * @throws IOException
     */
    private int readStringArray(BufferedReader src, List<String> list)
            throws IOException {
        //-- Read in the first line,
        String buff = src.readLine();
        //-- Which holds the size of the List to be read.
        int j = Integer.parseInt(buff);

        //-- Read in the 'j' elements of the List as Strings.
        for (int i = 0; i < j; i++) {
            buff = src.readLine();
            list.add(buff);
        }

        //-- Return the number of elements read.
        return j;
    }

    /**
     * To set the name of this planning problem.
     *
     * @param probNameIn the name of this planning problem.
     */
    public void setProbName(String probNameIn) {
        probName = probNameIn;
    }

    /**
     * This function produces the Java code needed to allocate and initialize an
     * array the elements of which are drawn from a given <code>List</code> of
     * <code>String</code>s.
     *
     * @param list the <code>List</code> the elements of which are to be stored in
     *             the resulting array.
     * @param name the name of the array where the elements of the
     *             <code>List</code> are to be stored.
     * @return the produced Java code.
     */
    private String listToCode(List<String> list, String name) {
        StringBuilder retVal;

        //-- First, allocate the array.
        retVal = new StringBuilder("\t\t" + name + " = new String[" + list.size() + "];" + endl);

        //-- Then, assign the elements of the array one by one.
        for (int i = 0; i < list.size(); i++)
            retVal.append("\t\t").append(name).append('[').append(i).append("] = \"").append(list.get(i)).append("\";").append(endl);

        return retVal + endl;
    }

    public JSHOP2Parser getParser() {
        return parser;
    }

    /**
     * The number of constant symbols already seen in the planning domain. Any
     * number of constant symbols in the planning problem more than this
     * indicates presence of constant symbols that appear exclusively in the
     * problem description.
     */
    public int getConstantsSize() {
        return constantsSize;
    }

//    public void setConstantsSize(int constantsSize) {
//        this.constantsSize = constantsSize;
//    }
}
