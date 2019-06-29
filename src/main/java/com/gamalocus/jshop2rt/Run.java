//package com.gamalocus.jshop2rt;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//
//public class Run {
//
//    /**
//     * The main function that is called to do the compilation.
//     *
//     * @param args the command line arguments.
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        //-- Check the number of arguments.
//        if (args.length == 1) {
//            System.err.println(String.format("usage: java %s input [output [output package]]",
//                    InternalDomain.class.getName()));
//            System.exit(1);
//        }
//
//        final File input = new File(args[0]);
//        final File output = args.length >= 2 ? new File(args[1]) : null;
//        final String outputPackage = args.length >= 3 ? args[2] : InternalDomain.DEFAULT_OUTPUT_PACKAGE;
//
//        //-- If this is a planning problem, call the 'command' rule in the parser.
//        final boolean isProblem = false;
//
//        final InternalDomain internalDomain = new InternalDomain(input, outputPackage);
//        if (!isProblem) {
//            internalDomain.parser.domain();
//
//            //-- Open the file with the appropriate name.
//            BufferedWriter dest = new BufferedWriter(new FileWriter(internalDomain.outputFile));
//
//            //-- Write the String.
//            String s = internalDomain.getOutput();
//            dest.write(s, 0, s.length());
//
//            //-- Close the file.
//            dest.close();
//
//            //-- Open another file with extension '.txt' to store the String names of
//            //-- the constant symbols, the compound tasks and the primitive tasks in
//            //-- the domain description. This data will be used when compiling planning
//            //-- problems in this domain.
//            dest = new BufferedWriter(new FileWriter(internalDomain.getSymbolDumpOutputPath()));
//
//            //-- Store the constant symbols.
//            InternalDomain.dumpStringArray(dest, internalDomain.constants);
//
//            //-- Store the compound tasks.
//            InternalDomain.dumpStringArray(dest, internalDomain.compoundTasks);
//
//            //-- Store the primitive tasks.
//            InternalDomain.dumpStringArray(dest, internalDomain.primitiveTasks);
//
//            //-- Close the file.
//            dest.close();
//        } else {
//            internalDomain.parser.command();
//
//            BufferedWriter dest;
//
//            //-- Open the file with the appropriate name.
//            dest = new BufferedWriter(new FileWriter(internalDomain.getProblemOutputPath()));
//
//            //-- Write the String.
//            String s = internalDomain.getOutput();
//            dest.write(s, 0, s.length());
//
//            //-- Close the file.
//            dest.close();
//        }
//
//    }
//}
