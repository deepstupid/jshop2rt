import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.gamalocus.jshop2rt.*;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.ScriptEvaluator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

public class Basic {

    public static void main(String[] args) throws RecognitionException, TokenStreamException, IOException {
        String basic =
                "; This extremely simple example shows some of the most essential\n" +
                        "; features of SHOP2.\n" +

                        "(defdomain basic (\n" +

                        "  (:operator (!pickup ?a) () () ((have ?a)))\n" +
                        "  (:operator (!drop ?a) ((have ?a)) ((have ?a)) ())\n" +

                        "  (:method (swap ?x ?y)\n" +
                        "    ((have ?x) (not (have ?y)))\n" +
                        "    ((!drop ?x) (!pickup ?y))\n" +
                        "    ((have ?y) (not (have ?x)))\n" +
                        "    ((!drop ?y) (!pickup ?x)))))\n";

        Model model = new Model(basic, "abc");
        model.domain();


        System.out.println(model.src.toString());

        Solution problem = new Solution(model); //new ModelInstance(pkg, model));
        problem.getPlans().forEach(System.out::println);

//        try {
//        String src = problem.src.toString() + "\n" +
//                model.src.substring(model.src.indexOf("public class ") /* skip imports, package */);
//        src = src.replaceAll("package", "\\/\\/package");
//        src = src.replaceAll("import .*;", "");
//        System.out.println(src);

        //JavaSourceClassLoader cl = new JavaSourceClassLoader();
        //ScriptEvaluator se = new ScriptEvaluator();

//            CompilerFactory cf = new CompilerFactory();
//            ISimpleCompiler sc = cf.newSimpleCompiler();
//            sc.cook(src);

//            IScriptEvaluator se = cf.newScriptEvaluator();
//            se.cook(src);
//            se.evaluate(new String[] { });

//        } catch (CompileException | InvocationTargetException e) {
//            e.printStackTrace();
//        }

//        State state = new State(null);
//        System.out.println(problem.src.toString());

//        new JSHOP2(tasks, 3,0 , model, state);


    }

//    private static class ModelInstance extends Model {
//
//        private final Model model;
//
//        public ModelInstance(String pkg, Model model) throws RecognitionException, TokenStreamException, IOException {
//            super("(defproblem problem basic\n" +
//                    "  ((have kiwi)) ((swap banjo kiwi)))", pkg);
//            this.model = model;
//            problem();
//        }
//
//        @Override
//        public int getConstantsSize() {
//            return model.getConstantsSize();
//        }
//
//        @Override
//        public List<String> constants() {
//            return model.constants();
//        }
//
//        @Override
//        public List<String> getPrimitiveTasks() {
//            return model.getPrimitiveTasks();
//        }
//
//        @Override
//        public List<String> getCompoundTasks() {
//            return model.getCompoundTasks();
//        }
//    }

    static class Solution extends DomainElement {

        public Solution(Model m) {
            super(null, null);
            owner = new Domain(m);

            //TODO constants
            //TODO compoundTasks
            //TODO primitiveTasks

            owner.initializeTermVariables(m.varsMaxSize);

            //owner.initializeTermConstants();
            owner.addConstant("have");
            owner.addConstant("kiwi");
            owner.addConstant("banjo");

            //TODO Methods

            //TODO Preconditions


            int nOps = m.operators.size();
            owner.ops = new Operator[nOps][];
            for (int i = 0; i < nOps; i++) {
                try {
                    String os = m.operators.get(i).toCode("", true);
                    ExpressionEvaluator se = new ExpressionEvaluator();
                    se.setParameters(new String[] { "owner" }, new Class[] { owner.getClass() });
                    se.setDefaultImports("com.gamalocus.jshop2rt.*");
                    se.cook(os);
                    Operator o = (Operator)se.evaluate(new Object[] { owner } );
                    owner.ops[i] = new Operator[] { o };
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (CompileException e) {
                    e.printStackTrace();
                }
            }

            //TODO axioms
        }


        private void createState0(State s) {
        }

        public LinkedList<Plan> getPlans() {
            //TermConstant.initialize(3);


            //d.setProblemConstants(defineConstants());

            State s = new State(owner.getAxioms());

            TaskList tl;
            tl = new TaskList(1, true);
            tl.subtasks[0] = new TaskList(new TaskAtom(new Predicate(0, 0, new TermList(owner.getTermConstant(2) /*banjo*/, new TermList(owner.getTermConstant(1) /*kiwi*/, TermList.NIL))), false, false));

            JSHOP2 jShop2Planner = new JSHOP2(tl, 8,
                    new DoubleCost(0), owner, s);

            createState0(s);
            jShop2Planner.run();

            LinkedList<Plan> returnedPlans = new LinkedList<Plan>();

//                Thread thread = new SolverThread(jShop2Planner, tl, Integer.MAX_VALUE);
//                thread.start();
//
//                try {
//                    while (thread.isAlive())
//                        Thread.sleep(500);
//                } catch (InterruptedException e) {
//                }
//
//                returnedPlans.addAll(thread.getPlans());

            return returnedPlans;
        }

        //
        //        public LinkedList<Predicate> getFirstPlanOps() {
        //            return getPlans().getFirst().getOps();
        //        }
        public Precondition getIterator(State state, Term[] unifier, int which)
        {
            Precondition p = null;
//            switch (which)
//            {
//                case 0:
//                    p = (new Precondition0(owner, unifier)).setComparator(null);
//                    break;
//                case 1:
//                    p = (new Precondition1(owner, unifier)).setComparator(null);
//                    break;
//                default:
//                    return null;
//            }
//            p.reset(state);

            return p;
        }

    }
}
//import com.gamalocus.jshop2rt.*;
//
//import java.util.LinkedList;
//
//public class problem
//{
//	private static String[] defineConstants()
//	{
//		String[] problemConstants = new String[2];
//
//		problemConstants[0] = "y";
//		problemConstants[1] = "x";
//
//		return problemConstants;
//	}
//
//	private static void createState0(State s)	{
//		s.add(new Predicate(0, 0, new TermList(TermConstant.getConstant(3), TermList.NIL)));
//		s.add(new Predicate(1, 0, new TermList(TermConstant.getConstant(4), TermList.NIL)));
//		s.add(new Predicate(1, 0, new TermList(TermConstant.getConstant(3), TermList.NIL)));
//		s.add(new Predicate(2, 0, new TermList(TermConstant.getConstant(3), TermList.NIL)));
//	}
//
//	public static LinkedList<Plan> getPlans()
//	{
//		LinkedList<Plan> returnedPlans = new LinkedList<Plan>();
//		//TermConstant.initialize(5);
//
//		Domain d = new foralltest();
//
//		d.setProblemConstants(defineConstants());
//
//		State s = new State(3, d.getAxioms());
//
//		//JSHOP2.initialize(d, s);
//
//		TaskList tl;
//		Thread thread;
//
//		createState0(s);
//
//		tl = new TaskList(1, true);
//		tl.subtasks[0] = new TaskList(new TaskAtom(new Predicate(0, 0, TermList.NIL), false, false));
//
//		thread = new Thread(tl, 1);
//		thread.start();
//
//		try {
//			while (thread.isAlive())
//				Thread.sleep(500);
//		} catch (InterruptedException e) {
//		}
//
//		returnedPlans.addAll( thread.getPlans() );
//
//		return returnedPlans;
//	}
//
//	public static LinkedList<Predicate> getFirstPlanOps() {
//		return getPlans().getFirst().getOps();
//	}
//}