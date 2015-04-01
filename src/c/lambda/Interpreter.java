package c.lambda;

import java.util.NoSuchElementException;

import c.lambda.IntArithmetic.Operation;
import util.List;

public class Interpreter {
  
  static void interpret (Program p, Environment env) {
    System.out.println(evaluate(p.e, env));
  }
  
  static Value evaluate (Expression e, final Environment env) {
    if (e instanceof IntConstant) {
      IntConstant intE = (IntConstant) e;

      return new IntValue(intE.javaInt);
    } else if (e instanceof BoolConstant) {
      BoolConstant boolE = (BoolConstant) e;

      return new BoolValue(boolE.javaBool);
    } else if (e instanceof IntArithmetic) {
      IntArithmetic arith = (IntArithmetic) e;

      IntValue op1 = (IntValue) evaluate(arith.operand1, env);
      IntValue op2 = (IntValue) evaluate(arith.operand2, env);
      
      switch (arith.op) {
        case PLUS:
          return new IntValue(op1.javaInt + op2.javaInt);
        case MINUS:
          return new IntValue(op1.javaInt - op2.javaInt);
        case MULT:
          return new IntValue(op1.javaInt * op2.javaInt);
        case DIV:
          return new IntValue(op1.javaInt / op2.javaInt);
        default:
          throw new Error();
      }
      
    } else if (e instanceof IntComparison) {
      IntComparison intComp = (IntComparison) e;

      IntValue op1 = (IntValue) evaluate(intComp.operand1, env);
      IntValue op2 = (IntValue) evaluate(intComp.operand2, env);
      
      return new BoolValue(op1.javaInt == op2.javaInt);
    } else if (e instanceof If) {
      If ifE = (If) e;
      
      BoolValue guard = (BoolValue) evaluate(ifE.guard, env);
      
      if (guard.javaBool)
        return evaluate(ifE.t, env);
      else
        return evaluate(ifE.f, env);
    } else if (e instanceof Variable) {
      Variable v = (Variable) e;
      
      return env.lookup(v);
    } else if (e instanceof LambdaDef) {
      final LambdaDef lambdaDef = (LambdaDef) e;
      
      return new LambdaValue(new JavaLambda() {
        @Override
        public Object l(Object arg) {
          Value argument = (Value) arg;
          
          Environment newEnv = env.bind(lambdaDef.formalArgument, argument);
          
          return evaluate(lambdaDef.body, newEnv);
        }
      });
    } else if (e instanceof LambdaApp) {
      LambdaApp lambdaApp = (LambdaApp) e;

      
      LambdaValue lambda = (LambdaValue) evaluate(lambdaApp.lambda, env);
      
      return (Value) lambda.javaLambda.l(evaluate(lambdaApp.argument, env));
    }
    
    throw new Error();
  }

  public static void main(String[] args) {
    // Simple lambda definition and application
    interpret (new Program(
        new LambdaApp(
            new LambdaDef(
                new Variable("x"),
                new IntArithmetic(
                    Operation.PLUS,
                      new Variable("x"),
                      new Variable("y"))),
            new IntConstant(3))),
        Environment.EMPTY.bind(new Variable("y"), new IntValue(3)));

    // Lambdas are first-order that capture state
    // Where is the bug here?
//    interpret (new Program(
//        new LambdaApp(
//            new Variable("l"),
//            new IntConstant(3))),
//            Environment.EMPTY
//              .bind(new Variable("y"), new IntValue(3))
//              .bind(new Variable("l"),
//                evaluate(
//                    new LambdaDef(
//                        new Variable("x"),
//                        new IntArithmetic(
//                            Operation.PLUS,
//                            new Variable("x"),
//                            new Variable("y"))),
//                    Environment.EMPTY)));
    
    // Lambdas are higher-order
    interpret (new Program(
        new LambdaApp(
            new LambdaApp(
                new Variable("l"),
                new IntConstant(3)),
            new LambdaDef(
                new Variable("i"),
                new IntArithmetic(
                    Operation.PLUS,
                    new Variable("i"),
                    new Variable("i"))))),
        Environment.EMPTY.bind(
            new Variable("l"),
            evaluate(
                new LambdaDef(
                    new Variable("x"),
                    new LambdaDef(
                        new Variable("y"),
                        new LambdaApp(
                            new Variable("y"),
                            new Variable("x")))),
                Environment.EMPTY)));
  }

}


class Bind {
	final Variable var;
	final Value	 val;

	public Bind(Variable var, Value val) {
		this.var = var;
		this.val = val;
	}
}

class Environment {
  public static Environment EMPTY = new Environment(List.EMPTY);
	private List binds;

	private Environment(List binds) { this.binds = binds; }
	
	public Environment bind(Variable va, Value vv) {
	  return new Environment(binds.add(new Bind(va,vv)));
	}

	public Value lookup(Variable v) {
		return lookup(v, binds);
	}

	public Value lookup(Variable v, List rest) {
		if (rest == null)
			throw new NoSuchElementException();

		Bind b = (Bind) rest.el;

		if (v.name.equals(b.var.name))
			return b.val;
		else
			return lookup(v, rest.rest);
	}
}

// Expressions

class Expression {
}

class IntConstant extends Expression {
  final int javaInt;

  public IntConstant(int javaInt) {
    this.javaInt = javaInt;
  }
}

class BoolConstant extends Expression {
  final boolean javaBool;

  public BoolConstant(boolean javaBool) {
    this.javaBool = javaBool;
  }
}

class IntArithmetic extends Expression {
  enum Operation { PLUS, MINUS, MULT, DIV }
  
  final Operation op;
  final Expression operand1, operand2;
  
  public IntArithmetic(Operation op, Expression operand1, Expression operand2) {
    this.op = op;
    this.operand1 = operand1;
    this.operand2 = operand2;
  }
}

class IntComparison extends Expression {
  final Expression operand1, operand2;
  
  public IntComparison(Expression operand1, Expression operand2) {
    this.operand1 = operand1;
    this.operand2 = operand2;
  }
}

class If extends Expression {
  final Expression guard, t, f;

  public If(Expression guard, Expression t, Expression f) {
    this.guard = guard;
    this.t = t;
    this.f = f;
  }
}

class Variable extends Expression {
  final String name;

  public Variable(String name) {
    this.name = name;
  }
}

class LambdaDef extends Expression {
  final Variable formalArgument;
  final Expression body;

  public LambdaDef(Variable formalArgument, Expression body) {
    this.formalArgument = formalArgument;
    this.body = body;
  }
}

class LambdaApp extends Expression {
  final Expression lambda;
  final Expression argument;

  public LambdaApp(Expression lambda, Expression argument) {
    this.lambda = lambda;
    this.argument = argument;
  }
}

// Values

class Value {
}

class IntValue extends Value {
  final int javaInt;

  public IntValue(int javaInt) {
    this.javaInt = javaInt;
  }

  @Override
  public String toString() {
    return "" + this.javaInt;
  }
}

class BoolValue extends Value {
  final boolean javaBool;

  public BoolValue(boolean javaBool) {
    this.javaBool = javaBool;
  }

  @Override
  public String toString() {
    return "" + this.javaBool;
  }
  
}

interface JavaLambda { public Object l(Object arg); }

class LambdaValue extends Value {
  final JavaLambda javaLambda;

  public LambdaValue(JavaLambda javaLambda) {
    this.javaLambda = javaLambda;
  }
}

class Program {
  final Expression e;

  public Program(Expression e) {
    this.e = e;
  }
}