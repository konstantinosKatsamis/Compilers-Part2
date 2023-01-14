
import java.io.*;
import minipython.lexer.Lexer;
import minipython.parser.Parser;
import minipython.node.*;
import java.util.*;
import utils.FunctionData;
import utils.FunctionCalls;
import utils.Types;

public class VisitorTester
{
  public static void main(String[] args)
  {
    try
    {

      Parser parser =
        new Parser(
        new Lexer(
        new PushbackReader(
        new FileReader(args[0].toString()), 1024)));

        Hashtable<String, LinkedList<FunctionData>> functions = new Hashtable<>();
            Hashtable<String, Types> variables = new Hashtable<>();
            Hashtable<String, FunctionCalls> functionCalls = new Hashtable<>();

            Start ast = parser.parse();
            Visitor1 visitor1 = new Visitor1(functions, variables);
            ast.apply(new Visitor1(functions, variables));
            // visitor1.printFunctionsData();

            Visitor2 visitor2 = new Visitor2(functions, variables, functionCalls);
            ast.apply(visitor2);
            visitor2.printFunctionsData();
            visitor2.printFunctionCalls();
            visitor2.printAllVariables();

      /*
     Hashtable symtable =  new Hashtable();
     Start ast = parser.parse();
     ast.apply(new visitor1(symtable));*/
     /* Gia ton deutero visitor grapste thn entolh
      * ast.apply(new mysecondvisitor(symtable));
      */
    }
    catch (Exception e)
    {
      System.err.println(e);
    }
  }
}

