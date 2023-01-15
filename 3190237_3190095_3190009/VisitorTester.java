/*
* Konstantinos Andrinopoulos 3190009
* Giorgos Kouros 3190095
* Konstantinos Katsamis 3190237
*/

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
            ast.apply(new Visitor1(functions, variables));
            ast.apply(new Visitor2(functions, variables, functionCalls));

    }
    catch (Exception e)
    {
      System.err.println(e);
    }
  }
}

