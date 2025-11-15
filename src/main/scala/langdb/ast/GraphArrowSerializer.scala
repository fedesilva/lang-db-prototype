package langdb.ast

import cats.effect.{IO, Resource}
import org.apache.arrow.memory.{BufferAllocator, RootAllocator}
import org.apache.arrow.vector.{VarCharVector, BigIntVector, VectorSchemaRoot}
import org.apache.arrow.vector.types.pojo.{ArrowType, Field, FieldType, Schema}
import org.apache.arrow.vector.ipc.{ArrowFileWriter, ArrowFileReader}
import java.io.{File, FileInputStream, FileOutputStream}
import scala.jdk.CollectionConverters.*

object GraphArrowSerializer:
  
  // Resource management for BufferAllocator
  private def allocatorResource: Resource[IO, BufferAllocator] =
    Resource.make(IO(new RootAllocator()))(allocator => IO(allocator.close()))

  // Schema for nodes table
  private def createNodesSchema(): Schema =
    val idField = new Field("node_id", FieldType.nullable(new ArrowType.Int(64, true)), null)
    val typeField = new Field("node_type", FieldType.nullable(new ArrowType.Utf8()), null)
    val dataKeysField = new Field("data_keys", FieldType.nullable(new ArrowType.Utf8()), null)
    val dataValuesField = new Field("data_values", FieldType.nullable(new ArrowType.Utf8()), null)
    
    new Schema(List(idField, typeField, dataKeysField, dataValuesField).asJava)

  // Schema for edges table
  private def createEdgesSchema(): Schema =
    val fromField = new Field("from_id", FieldType.nullable(new ArrowType.Int(64, true)), null)
    val toField = new Field("to_id", FieldType.nullable(new ArrowType.Int(64, true)), null)
    val typeField = new Field("edge_type", FieldType.nullable(new ArrowType.Utf8()), null)
    val labelField = new Field("label", FieldType.nullable(new ArrowType.Utf8()), null)
    
    new Schema(List(fromField, toField, typeField, labelField).asJava)

  // Serialize graph to Arrow files (separate files for nodes and edges)
  def saveGraph(graph: ASTGraph, nodesFile: String, edgesFile: String): IO[Unit] =
    for {
      _ <- saveNodes(graph, nodesFile)
      _ <- saveEdges(graph, edgesFile)
    } yield ()

  private def saveNodes(graph: ASTGraph, filename: String): IO[Unit] =
    allocatorResource.use { allocator =>
      IO {
        val schema = createNodesSchema()
        val root = VectorSchemaRoot.create(schema, allocator)
        
        val nodes = graph.getAllNodes
        
        // Get vectors
        val idVector = root.getVector("node_id").asInstanceOf[BigIntVector]
        val typeVector = root.getVector("node_type").asInstanceOf[VarCharVector]
        val keysVector = root.getVector("data_keys").asInstanceOf[VarCharVector]
        val valuesVector = root.getVector("data_values").asInstanceOf[VarCharVector]
        
        // Allocate memory
        idVector.allocateNew(nodes.size)
        typeVector.allocateNew(nodes.size)
        keysVector.allocateNew(nodes.size)
        valuesVector.allocateNew(nodes.size)
        
        // Populate vectors
        nodes.zipWithIndex.foreach { case (node, idx) =>
          idVector.set(idx, node.id.value)
          typeVector.set(idx, node.nodeType.getBytes())
          
          // Serialize data map as JSON-like strings
          val keys = node.data.keys.mkString(",")
          val values = node.data.values.mkString(",")
          keysVector.set(idx, keys.getBytes())
          valuesVector.set(idx, values.getBytes())
        }
        
        root.setRowCount(nodes.size)
        
        // Write to file
        val file = new File(filename)
        val fileOutputStream = new FileOutputStream(file)
        val writer = new ArrowFileWriter(root, null, fileOutputStream.getChannel())
        
        try {
          writer.start()
          writer.writeBatch()
          writer.end()
        } finally {
          writer.close()
          fileOutputStream.close()
          root.close()
        }
      }
    }

  private def saveEdges(graph: ASTGraph, filename: String): IO[Unit] =
    allocatorResource.use { allocator =>
      IO {
        val schema = createEdgesSchema()
        val root = VectorSchemaRoot.create(schema, allocator)
        
        val edges = graph.getAllEdges
        
        // Get vectors
        val fromVector = root.getVector("from_id").asInstanceOf[BigIntVector]
        val toVector = root.getVector("to_id").asInstanceOf[BigIntVector]
        val typeVector = root.getVector("edge_type").asInstanceOf[VarCharVector]
        val labelVector = root.getVector("label").asInstanceOf[VarCharVector]
        
        // Allocate memory
        fromVector.allocateNew(edges.size)
        toVector.allocateNew(edges.size)
        typeVector.allocateNew(edges.size)
        labelVector.allocateNew(edges.size)
        
        // Populate vectors
        edges.zipWithIndex.foreach { case (edge, idx) =>
          fromVector.set(idx, edge.from.value)
          toVector.set(idx, edge.to.value)
          typeVector.set(idx, edge.edgeType.getBytes())
          labelVector.set(idx, edge.label.getOrElse("").getBytes())
        }
        
        root.setRowCount(edges.size)
        
        // Write to file
        val file = new File(filename)
        val fileOutputStream = new FileOutputStream(file)
        val writer = new ArrowFileWriter(root, null, fileOutputStream.getChannel())
        
        try {
          writer.start()
          writer.writeBatch()
          writer.end()
        } finally {
          writer.close()
          fileOutputStream.close()
          root.close()
        }
      }
    }

  // Load graph from Arrow files
  def loadGraph(nodesFile: String, edgesFile: String): IO[ASTGraph] =
    for {
      nodes <- loadNodes(nodesFile)
      edges <- loadEdges(edgesFile)
    } yield {
      val graph = nodes.foldLeft(ASTGraph.empty)(_.addNode(_))
      edges.foldLeft(graph)(_.addEdge(_))
    }

  private def loadNodes(filename: String): IO[List[ASTNode]] =
    allocatorResource.use { allocator =>
      IO {
        val file = new File(filename)
        val fileInputStream = new FileInputStream(file)
        val reader = new ArrowFileReader(fileInputStream.getChannel(), allocator)
        
        try {
          val nodes = scala.collection.mutable.ListBuffer[ASTNode]()
          
          while reader.loadNextBatch() do
            val root = reader.getVectorSchemaRoot
            val rowCount = root.getRowCount
            
            val idVector = root.getVector("node_id").asInstanceOf[BigIntVector]
            val typeVector = root.getVector("node_type").asInstanceOf[VarCharVector]
            val keysVector = root.getVector("data_keys").asInstanceOf[VarCharVector]
            val valuesVector = root.getVector("data_values").asInstanceOf[VarCharVector]
            
            for i <- 0 until rowCount do
              val id = NodeId(idVector.get(i))
              val nodeType = new String(typeVector.get(i))
              val keys = new String(keysVector.get(i)).split(",").filter(_.nonEmpty)
              val values = new String(valuesVector.get(i)).split(",").filter(_.nonEmpty)
              val data = keys.zip(values).toMap
              
              nodes += ASTNode(id, nodeType, data)
          
          nodes.toList
          
        } finally {
          reader.close()
          fileInputStream.close()
        }
      }
    }

  private def loadEdges(filename: String): IO[List[ASTEdge]] =
    allocatorResource.use { allocator =>
      IO {
        val file = new File(filename)
        val fileInputStream = new FileInputStream(file)
        val reader = new ArrowFileReader(fileInputStream.getChannel(), allocator)
        
        try {
          val edges = scala.collection.mutable.ListBuffer[ASTEdge]()
          
          while reader.loadNextBatch() do
            val root = reader.getVectorSchemaRoot
            val rowCount = root.getRowCount
            
            val fromVector = root.getVector("from_id").asInstanceOf[BigIntVector]
            val toVector = root.getVector("to_id").asInstanceOf[BigIntVector]
            val typeVector = root.getVector("edge_type").asInstanceOf[VarCharVector]
            val labelVector = root.getVector("label").asInstanceOf[VarCharVector]
            
            for i <- 0 until rowCount do
              val from = NodeId(fromVector.get(i))
              val to = NodeId(toVector.get(i))
              val edgeType = new String(typeVector.get(i))
              val label = {
                val labelBytes = labelVector.get(i)
                if labelBytes.nonEmpty then Some(new String(labelBytes)) else None
              }
              
              edges += ASTEdge(from, to, edgeType, label)
          
          edges.toList
          
        } finally {
          reader.close()
          fileInputStream.close()
        }
      }
    }
