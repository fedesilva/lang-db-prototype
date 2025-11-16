package langdb.languages.nanoproc.typechecker

import cats.effect.IO
import cats.syntax.all.*
import langdb.languages.nanoproc.ast.{Expr, ProcDef, Program, Stmt, Type}

// Context for type checking - tracks variables and procedures
final case class TypeContext(
  vars:  Map[String, Type],
  procs: Map[String, ProcSignature]
):
  def bindVar(name: String, typ: Type): TypeContext =
    copy(vars = vars.updated(name, typ))

  def lookupVar(name: String): Option[Type] =
    vars.get(name)

  def bindProc(name: String, sig: ProcSignature): TypeContext =
    copy(procs = procs.updated(name, sig))

  def lookupProc(name: String): Option[ProcSignature] =
    procs.get(name)

// Procedure signature (parameter types and return type)
case class ProcSignature(paramTypes: List[Type], returnType: Type)

// Type errors
sealed trait TypeError extends Exception:
  def message:             String
  override def getMessage: String = message

final case class UnboundVariable(name: String) extends TypeError:
  def message: String = s"Unbound variable: $name"

final case class UnboundProcedure(name: String) extends TypeError:
  def message: String = s"Unbound procedure: $name"

final case class TypeMismatch(expected: Type, actual: Type) extends TypeError:
  def message: String = s"Type mismatch: expected $expected, got $actual"

final case class ArityMismatch(expected: Int, actual: Int) extends TypeError:
  def message: String = s"Arity mismatch: expected $expected arguments, got $actual"

final case class MissingReturn(procName: String) extends TypeError:
  def message: String = s"Procedure $procName must return a value of its declared type"

object TypeChecker:

  def typeCheck(program: Program): IO[Unit] =
    // Build initial context with all procedure signatures
    val initialContext = program.procs.foldLeft(TypeContext(Map.empty, Map.empty)) { (ctx, proc) =>
      val sig = ProcSignature(proc.params.map(_._2), proc.returnType)
      ctx.bindProc(proc.name, sig)
    }

    // Type check each procedure
    program.procs.traverse_(proc => typeCheckProc(proc, initialContext))

  def typeCheckProc(proc: ProcDef, ctx: TypeContext): IO[Unit] =
    // Build context with parameters
    val procCtx = proc.params.foldLeft(ctx) { case (c, (name, typ)) =>
      c.bindVar(name, typ)
    }

    // Check the body
    for
      _ <- typeCheckBlock(proc.body, procCtx, Some(proc.returnType))
      _ <- ensureReturns(proc.body, proc.returnType, proc.name)
    yield ()

  // Check if a block always returns (for non-Unit procedures)
  def ensureReturns(block: Stmt.Block, expectedType: Type, procName: String): IO[Unit] =
    if expectedType == Type.UnitType then IO.unit
    else
      // Check if the block has a return statement
      val hasReturn = blockHasReturn(block)
      if hasReturn then IO.unit
      else IO.raiseError(MissingReturn(procName))

  def blockHasReturn(block: Stmt.Block): Boolean =
    block.stmts.exists {
      case Stmt.Return(_) => true
      case Stmt.If(_, thenBlock, Some(elseBlock)) =>
        blockHasReturn(thenBlock) && blockHasReturn(elseBlock)
      case _ => false
    }

  def typeCheckBlock(
    block:          Stmt.Block,
    ctx:            TypeContext,
    expectedReturn: Option[Type]
  ): IO[TypeContext] =
    block.stmts.foldLeftM(ctx) { (currentCtx, stmt) =>
      typeCheckStmt(stmt, currentCtx, expectedReturn)
    }

  def typeCheckStmt(
    stmt:           Stmt,
    ctx:            TypeContext,
    expectedReturn: Option[Type]
  ): IO[TypeContext] =
    stmt match
      case Stmt.VarDecl(name, typ, init) =>
        for
          initType <- typeCheckExpr(init, ctx)
          _ <- ensureType(initType, typ)
        yield ctx.bindVar(name, typ)

      case Stmt.Assign(name, value) =>
        for
          varType <- ctx.lookupVar(name) match
            case Some(t) => IO.pure(t)
            case None => IO.raiseError(UnboundVariable(name))
          valueType <- typeCheckExpr(value, ctx)
          _ <- ensureType(valueType, varType)
        yield ctx

      case Stmt.ExprStmt(expr) =>
        typeCheckExpr(expr, ctx).as(ctx)

      case Stmt.Return(value) =>
        for
          valueType <- typeCheckExpr(value, ctx)
          _ <- expectedReturn match
            case Some(expected) => ensureType(valueType, expected)
            case None => IO.raiseError(new TypeError { def message = "Return outside procedure" })
        yield ctx

      case Stmt.If(cond, thenBlock, elseBlock) =>
        for
          condType <- typeCheckExpr(cond, ctx)
          _ <- ensureType(condType, Type.BoolType)
          _ <- typeCheckBlock(thenBlock, ctx, expectedReturn)
          _ <- elseBlock.traverse_(block => typeCheckBlock(block, ctx, expectedReturn))
        yield ctx

      case Stmt.While(cond, body) =>
        for
          condType <- typeCheckExpr(cond, ctx)
          _ <- ensureType(condType, Type.BoolType)
          _ <- typeCheckBlock(body, ctx, expectedReturn)
        yield ctx

      case Stmt.Block(stmts) =>
        typeCheckBlock(Stmt.Block(stmts), ctx, expectedReturn).as(ctx)

  def typeCheckExpr(expr: Expr, ctx: TypeContext): IO[Type] =
    expr match
      case Expr.Var(name) =>
        ctx.lookupVar(name) match
          case Some(typ) => IO.pure(typ)
          case None => IO.raiseError(UnboundVariable(name))

      case Expr.IntLit(_) => IO.pure(Type.IntType)
      case Expr.StringLit(_) => IO.pure(Type.StringType)
      case Expr.BoolLit(_) => IO.pure(Type.BoolType)
      case Expr.UnitLit => IO.pure(Type.UnitType)

      case Expr.Add(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.IntType)

      case Expr.Sub(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.IntType)

      case Expr.Mult(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.IntType)

      case Expr.Div(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.IntType)

      case Expr.Eq(left, right) =>
        for
          leftType <- typeCheckExpr(left, ctx)
          rightType <- typeCheckExpr(right, ctx)
          _ <-
            if leftType == rightType then IO.unit
            else IO.raiseError(TypeMismatch(leftType, rightType))
        yield Type.BoolType

      case Expr.Gt(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.BoolType)

      case Expr.Lt(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.BoolType)

      case Expr.Gte(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.BoolType)

      case Expr.Lte(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.BoolType)

      case Expr.And(left, right) =>
        for
          leftType <- typeCheckExpr(left, ctx)
          _ <- ensureType(leftType, Type.BoolType)
          rightType <- typeCheckExpr(right, ctx)
          _ <- ensureType(rightType, Type.BoolType)
        yield Type.BoolType

      case Expr.Not(operand) =>
        typeCheckExpr(operand, ctx).flatMap(ensureType(_, Type.BoolType)).as(Type.BoolType)

      case Expr.StringConcat(left, right) =>
        checkStringBinary(left, right, ctx).as(Type.StringType)

      case Expr.ProcCall(name, args) =>
        ctx.lookupProc(name) match
          case Some(sig) =>
            if sig.paramTypes.length != args.length then
              IO.raiseError(ArityMismatch(sig.paramTypes.length, args.length))
            else
              for
                argTypes <- args.traverse(arg => typeCheckExpr(arg, ctx))
                _ <- sig.paramTypes.zip(argTypes).traverse_ { case (expected, actual) =>
                  ensureType(actual, expected)
                }
              yield sig.returnType
          case None => IO.raiseError(UnboundProcedure(name))

      case Expr.Print(operand) =>
        typeCheckExpr(operand, ctx)
          .flatMap(ensureType(_, Type.StringType))
          .as(Type.UnitType)

      case Expr.Println(operand) =>
        typeCheckExpr(operand, ctx)
          .flatMap(ensureType(_, Type.StringType))
          .as(Type.UnitType)

  private def ensureType(actual: Type, expected: Type): IO[Unit] =
    if actual == expected then IO.unit
    else IO.raiseError(TypeMismatch(expected, actual))

  private def checkNumericBinary(left: Expr, right: Expr, ctx: TypeContext): IO[Unit] =
    for
      leftType <- typeCheckExpr(left, ctx)
      _ <- ensureType(leftType, Type.IntType)
      rightType <- typeCheckExpr(right, ctx)
      _ <- ensureType(rightType, Type.IntType)
    yield ()

  private def checkStringBinary(left: Expr, right: Expr, ctx: TypeContext): IO[Unit] =
    for
      leftType <- typeCheckExpr(left, ctx)
      _ <- ensureType(leftType, Type.StringType)
      rightType <- typeCheckExpr(right, ctx)
      _ <- ensureType(rightType, Type.StringType)
    yield ()
