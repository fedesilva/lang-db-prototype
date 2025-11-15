package langdb.languages.microml.typechecker

import cats.effect.IO
import cats.syntax.all.*
import langdb.languages.microml.ast.{Term, Type}

final case class TypeContext(bindings: Map[String, Type]):
  def bind(name: String, typ: Type): TypeContext =
    copy(bindings = bindings.updated(name, typ))

  def lookup(name: String): Option[Type] =
    bindings.get(name)

sealed trait TypeError extends Exception:
  def message:             String
  override def getMessage: String = message

final case class UnboundVariable(name: String) extends TypeError:
  def message: String = s"Unbound variable: $name"

final case class TypeMismatch(expected: Type, actual: Type) extends TypeError:
  def message: String = s"Type mismatch: expected $expected, got $actual"

final case class NotAFunction(typ: Type) extends TypeError:
  def message: String = s"Expected function type, got $typ"

object TypeChecker:

  def typeCheck(term: Term): IO[Type] =
    typeCheckWithContext(term, TypeContext(Map.empty))

  def typeCheckWithContext(term: Term, ctx: TypeContext): IO[Type] =
    term match
      case Term.Var(name) =>
        ctx.lookup(name) match
          case Some(typ) => IO.pure(typ)
          case None => IO.raiseError(UnboundVariable(name))

      case Term.Lambda(param, paramType, body) =>
        val nextCtx = ctx.bind(param, paramType)
        typeCheckWithContext(body, nextCtx).map(bodyType => Type.FunctionType(paramType, bodyType))

      case Term.App(func, arg) =>
        for
          funcType <- typeCheckWithContext(func, ctx)
          argType <- typeCheckWithContext(arg, ctx)
          result <- funcType match
            case Type.FunctionType(paramType, returnType) =>
              if paramType == argType then IO.pure(returnType)
              else IO.raiseError(TypeMismatch(paramType, argType))
            case other => IO.raiseError(NotAFunction(other))
        yield result

      case Term.Let(name, value, body) =>
        for
          valueType <- typeCheckWithContext(value, ctx)
          nextCtx = ctx.bind(name, valueType)
          bodyType <- typeCheckWithContext(body, nextCtx)
        yield bodyType

      case Term.IntLit(_) => IO.pure(Type.IntType)
      case Term.StringLit(_) => IO.pure(Type.StringType)
      case Term.BoolLit(_) => IO.pure(Type.BoolType)

      case Term.Add(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.IntType)

      case Term.Mult(left, right) =>
        checkNumericBinary(left, right, ctx).as(Type.IntType)

      case Term.Eq(left, right) =>
        for
          leftType <- typeCheckWithContext(left, ctx)
          rightType <- typeCheckWithContext(right, ctx)
          _ <-
            if leftType == rightType then IO.unit
            else IO.raiseError(TypeMismatch(leftType, rightType))
        yield Type.BoolType

      case Term.If(condition, thenBranch, elseBranch) =>
        for
          condType <- typeCheckWithContext(condition, ctx)
          _ <- ensureType(condType, Type.BoolType)
          thenType <- typeCheckWithContext(thenBranch, ctx)
          elseType <- typeCheckWithContext(elseBranch, ctx)
          _ <-
            if thenType == elseType then IO.unit
            else IO.raiseError(TypeMismatch(thenType, elseType))
        yield thenType

      case Term.Not(operand) =>
        typeCheckWithContext(operand, ctx).flatMap(ensureType(_, Type.BoolType)).as(Type.BoolType)

      case Term.And(left, right) =>
        for
          leftType <- typeCheckWithContext(left, ctx)
          _ <- ensureType(leftType, Type.BoolType)
          rightType <- typeCheckWithContext(right, ctx)
          _ <- ensureType(rightType, Type.BoolType)
        yield Type.BoolType

      case Term.StringConcat(left, right) =>
        checkStringBinary(left, right, ctx).as(Type.StringType)

      case Term.Print(operand) =>
        typeCheckWithContext(operand, ctx)
          .flatMap(ensureType(_, Type.StringType))
          .as(Type.StringType)

      case Term.Println(operand) =>
        typeCheckWithContext(operand, ctx)
          .flatMap(ensureType(_, Type.StringType))
          .as(Type.StringType)

  private def ensureType(actual: Type, expected: Type): IO[Unit] =
    if actual == expected then IO.unit
    else IO.raiseError(TypeMismatch(expected, actual))

  private def checkNumericBinary(left: Term, right: Term, ctx: TypeContext): IO[Unit] =
    for
      leftType <- typeCheckWithContext(left, ctx)
      _ <- ensureType(leftType, Type.IntType)
      rightType <- typeCheckWithContext(right, ctx)
      _ <- ensureType(rightType, Type.IntType)
    yield ()

  private def checkStringBinary(left: Term, right: Term, ctx: TypeContext): IO[Unit] =
    for
      leftType <- typeCheckWithContext(left, ctx)
      _ <- ensureType(leftType, Type.StringType)
      rightType <- typeCheckWithContext(right, ctx)
      _ <- ensureType(rightType, Type.StringType)
    yield ()
