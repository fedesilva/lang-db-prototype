# Plan: Fix SourceSpan Tracking Issues

## Status: Step 1 Complete

## Problem Overview

### Problem 1: Nested Expression Spans Collapse (REMAINING)

**Issue:** In chained operations like `f x y` or `1 + 2 + 3`, intermediate AST nodes reuse the
outer span for the entire expression instead of computing accurate spans for each subexpression.

**Example:** `1 + 2 + 3` should create:
- `Add(1, 2)` with span covering `1 + 2`
- `Add(Add(1, 2), 3)` with span covering `1 + 2 + 3`

**Currently:** Both `Add` nodes get the same span covering the entire `1 + 2 + 3`.

**Locations affected:**
- `src/main/scala/langdb/languages/microml/parser/TermParser.scala:106-164`
  * `application` (line 107-111)
  * `multiplicative` (line 129-133)
  * `additive` (line 136-147)
  * `logical` (line 160-164)
- `src/main/scala/langdb/languages/nanoproc/parser/ProgramParser.scala:110-160`
  * `multiplicative` (line 111-119)
  * `additive` (line 122-134)
  * `logical` (line 156-160)

**Fix:** For each `foldLeft` step, compute the span from the left and right operands:
- Start index = left operand's startIndex
- End index = right operand's endIndex

### Problem 2: Quadratic Performance in SourceSpan.fromIndices (COMPLETED)

**Issue:** Every call to `SourceSpan.fromIndices` scans the entire input string and rebuilds
an index→position map. This is O(n) per call, and with O(n) AST nodes, parsing becomes O(n²).

**Location:** `src/main/scala/langdb/common/SourceSpan.scala:36-55`

**Fix Applied:** Precompute a line/column lookup table once per parser instance and reuse it:
1. Added `SourceSpan.computePositions(input)` - builds position map once
2. Added `SourceSpan.fromPositions(...)` - O(1) lookups from precomputed map
3. Updated `TermParserInstance` to precompute positions in constructor
4. Updated `ProgramParserInstance` to precompute positions in constructor
5. Updated `makeSpan` methods to use precomputed positions

**Result:** Parsing is now O(n) instead of O(n²)

## Remaining Implementation Steps

### Step 2: Fix nested span collapse in MicroML parser

Both `Term` and `Expr` expose a uniform `sourceSpan` method for accessing spans.

**Changes needed:**
- `application` (line 107-111): In the fold, compute span from `func.sourceSpan` and
  `arg.sourceSpan` for each intermediate node
- `multiplicative` (line 129-133): Compute span from left.sourceSpan.startIndex and
  right.sourceSpan.endIndex in each fold step
- `additive` (line 136-147): Same approach as multiplicative
- `logical` (line 160-164): Same approach as multiplicative

**Pattern for foldLeft fixes:**
```scala
// Instead of:
rest.foldLeft(first)((left, right) => Term.Add(left, right, span))

// Use:
rest.foldLeft(first) { case (left, right) =>
  val nodeSpan = makeSpan(left.sourceSpan.startIndex, right.sourceSpan.endIndex)
  Term.Add(left, right, nodeSpan)
}
```

### Step 3: Fix nested span collapse in NanoProc parser

Same approach as MicroML:
- Update `multiplicative`, `additive`, `logical` to compute proper nested spans

### Step 4: Add targeted tests

Write focused tests to verify correct span tracking:
- Test operator chains: `1 + 2 + 3` has proper nested spans
- Test application chains: `f x y` has proper nested spans
- One test for MicroML, one for NanoProc

**Testing approach:** Parse the expression, extract the AST, verify each node's span
matches the expected source slice.

### Step 5: Final verification

- Run `sbt test` to ensure all tests pass
- Run `sbt scalafmtAll && sbt scalafixAll`
- Run `sbt compile` to verify clean compilation

## Notes

- This is a research project - tests should be focused, not exhaustive
- Most code will be discarded except the graph engine
- Keep tests practical and representative rather than comprehensive
