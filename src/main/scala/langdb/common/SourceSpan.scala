package langdb.common

/** Represents a location in source code.
  *
  * @param source
  *   The source file name or identifier
  * @param startLine
  *   Starting line number (1-indexed)
  * @param startCol
  *   Starting column number (1-indexed)
  * @param endLine
  *   Ending line number (1-indexed)
  * @param endCol
  *   Ending column number (1-indexed)
  * @param startIndex
  *   Starting character index in source (0-indexed)
  * @param endIndex
  *   Ending character index in source (0-indexed)
  */
case class SourceSpan(
  source:     String,
  startLine:  Int,
  startCol:   Int,
  endLine:    Int,
  endCol:     Int,
  startIndex: Int,
  endIndex:   Int
) derives CanEqual:
  def pretty: String =
    if startLine == endLine then s"$source:$startLine:$startCol-$endCol"
    else s"$source:$startLine:$startCol-$endLine:$endCol"

object SourceSpan:
  case class Position(line: Int, col: Int)

  /** Precompute line/column positions for all character indices in the input.
    *
    * This should be called once per input string and the result reused for multiple SourceSpan
    * creations to avoid O(n²) performance when parsing.
    */
  def computePositions(input: String): Map[Int, Position] =
    val (finalPos, initialMap) = input.zipWithIndex
      .foldLeft((Position(1, 1), Map.empty[Int, Position])) {
        case ((pos @ Position(line, col), map), (char, idx)) =>
          val newMap = map + (idx -> pos)
          val nextPos =
            if char == '\n' then Position(line + 1, 1)
            else Position(line, col + 1)
          (nextPos, newMap)
      }

    // Add the position *after* the last character (input.length)
    initialMap + (input.length -> finalPos)

  /** Create a SourceSpan from precomputed positions.
    *
    * Use this method when you have already called computePositions() to avoid redundant scanning of
    * the input.
    */
  def fromPositions(
    source:    String,
    positions: Map[Int, Position],
    start:     Int,
    end:       Int
  ): SourceSpan =
    val startPos = positions.getOrElse(start, Position(1, 1))
    val endPos   = positions.getOrElse(end, startPos)

    SourceSpan(source, startPos.line, startPos.col, endPos.line, endPos.col, start, end)

  /** Create a SourceSpan from start and end indices by scanning the source text.
    *
    * Note: This method scans the entire input for each call. For parsing multiple spans from the
    * same input, use computePositions() once and then call fromPositions() repeatedly.
    */
  def fromIndices(source: String, input: String, start: Int, end: Int): SourceSpan =
    val positions = computePositions(input)
    fromPositions(source, positions, start, end)

  /** Create a SourceSpan for a single position. */
  def point(source: String, input: String, index: Int): SourceSpan =
    fromIndices(source, input, index, index)

  /** Create a synthetic SourceSpan (for generated code). */
  def synthetic: SourceSpan =
    SourceSpan("<synthetic>", 0, 0, 0, 0, 0, 0)
