package langdb.languages.microml.ast

import langdb.common.SourceSpan

/** Helper for constructing Terms with synthetic SourceSpans in tests. */
object TermTestHelper:
  private val synth = SourceSpan.synthetic

  def vr(name: String): Term = Term.Var(name, synth)
  def lam(param: String, paramType: Type, body: Term): Term =
    Term.Lambda(param, paramType, body, synth)
  def app(func:       Term, arg:     Term):             Term = Term.App(func, arg, synth)
  def let(name:       String, value: Term, body: Term): Term = Term.Let(name, value, body, synth)
  def intLit(value:   Int):                             Term = Term.IntLit(value, synth)
  def strLit(value:   String):                          Term = Term.StringLit(value, synth)
  def boolLit(value:  Boolean):                         Term = Term.BoolLit(value, synth)
  def unitLit:                                          Term = Term.UnitLit(synth)
  def add(left:       Term, right:   Term):             Term = Term.Add(left, right, synth)
  def mult(left:      Term, right:   Term):             Term = Term.Mult(left, right, synth)
  def eq(left:        Term, right:   Term):             Term = Term.Eq(left, right, synth)
  def and(left:       Term, right:   Term):             Term = Term.And(left, right, synth)
  def strConcat(left: Term, right:   Term):             Term = Term.StringConcat(left, right, synth)
  def not(operand:    Term):                            Term = Term.Not(operand, synth)
  def ifExpr(cond: Term, thenBranch: Term, elseBranch: Term): Term =
    Term.If(cond, thenBranch, elseBranch, synth)
  def print(operand:   Term): Term = Term.Print(operand, synth)
  def println(operand: Term): Term = Term.Println(operand, synth)
