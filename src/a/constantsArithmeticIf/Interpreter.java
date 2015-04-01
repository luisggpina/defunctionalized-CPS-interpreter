package a.constantsArithmeticIf;

import a.constantsArithmeticIf.IntArithmetic.Operation;

public class Interpreter {
  
  static void interpret (Program p) {
    System.out.println(evaluate(p.e));
  }
  
  static Value evaluate (Expression e) {
    if (e instanceof IntConstant) {
      IntConstant intE = (IntConstant) e;

      return new IntValue(intE.javaInt);
    } else if (e instanceof BoolConstant) {
      BoolConstant boolE = (BoolConstant) e;

      return new BoolValue(boolE.javaBool);
    } else if (e instanceof IntArithmetic) {
      IntArithmetic arith = (IntArithmetic) e;

      IntValue op1 = (IntValue) evaluate(arith.operand1);
      IntValue op2 = (IntValue) evaluate(arith.operand2);
      
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

      IntValue op1 = (IntValue) evaluate(intComp.operand1);
      IntValue op2 = (IntValue) evaluate(intComp.operand2);
      
      return new BoolValue(op1.javaInt == op2.javaInt);
    } else if (e instanceof If) {
      If ifE = (If) e;
      
      BoolValue guard = (BoolValue) evaluate(ifE.guard);
      
      if (guard.javaBool)
        return evaluate(ifE.t);
      else
        return evaluate(ifE.f);
    }
    
    throw new Error();
  }

  public static void main(String[] args) {
    // Int arithmetic
    interpret (new Program(
        new IntArithmetic(
            Operation.PLUS,
            new IntConstant(3),
            new IntConstant(1))));

    // Int comparison
    interpret (new Program(
        new IntComparison(
            new IntConstant(3),
            new IntConstant(3))));
    
    // If expressions
    interpret (new Program(
        new If(
            new IntComparison(
                new IntConstant(3),
                new IntConstant(2)),
            new IntConstant(0),
            new IntConstant(1))));
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

class Program {
  final Expression e;

  public Program(Expression e) {
    this.e = e;
  }
}