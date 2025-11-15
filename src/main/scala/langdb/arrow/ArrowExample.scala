package langdb.arrow

import cats.effect.{IO, Resource}
import org.apache.arrow.memory.{BufferAllocator, RootAllocator}
import org.apache.arrow.vector.{VarCharVector, IntVector, VectorSchemaRoot}
import org.apache.arrow.vector.types.pojo.{ArrowType, Field, FieldType, Schema}
import org.apache.arrow.vector.ipc.{ArrowFileWriter, ArrowFileReader}
import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.channels.Channels
import scala.jdk.CollectionConverters.*

package langdb.arrow

import cats.effect.{IO, Resource}
import org.apache.arrow.memory.{BufferAllocator, RootAllocator}
import org.apache.arrow.vector.{VarCharVector, IntVector, VectorSchemaRoot}
import org.apache.arrow.vector.types.pojo.{ArrowType, Field, FieldType, Schema}
import org.apache.arrow.vector.ipc.{ArrowFileWriter, ArrowFileReader}
import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.channels.Channels
import scala.jdk.CollectionConverters.*
import cats.implicits.*

object ArrowExample {

  // Resource management for BufferAllocator
  private def allocatorResource: Resource[IO, BufferAllocator] =
    Resource.make(IO(new RootAllocator()))(allocator => IO(allocator.close()))

  // Create a simple schema for language data
  private def createLanguageSchema(): Schema = {
    val nameField = new Field("language_name", FieldType.nullable(new ArrowType.Utf8()), null)
    val speakersField = new Field("speakers_millions", FieldType.nullable(new ArrowType.Int(32, true)), null)
    val familyField = new Field("language_family", FieldType.nullable(new ArrowType.Utf8()), null)
    
    new Schema(List(nameField, speakersField, familyField).asJava)
  }

  // Write sample language data to Arrow file
  def writeLanguageData(filename: String): IO[Unit] =
    allocatorResource.use { allocator =>
      IO {
        val schema = createLanguageSchema()
        val root = VectorSchemaRoot.create(schema, allocator)
        
        // Get vectors for each column
        val nameVector = root.getVector("language_name").asInstanceOf[VarCharVector]
        val speakersVector = root.getVector("speakers_millions").asInstanceOf[IntVector]
        val familyVector = root.getVector("language_family").asInstanceOf[VarCharVector]
        
        // Allocate memory for vectors
        nameVector.allocateNew(5)
        speakersVector.allocateNew(5)
        familyVector.allocateNew(5)
        
        // Sample language data
        val languages = List(
          ("English", 1500, "Indo-European"),
          ("Mandarin", 1100, "Sino-Tibetan"),
          ("Spanish", 500, "Indo-European"),
          ("Arabic", 400, "Afro-Asiatic"),
          ("Japanese", 125, "Japonic")
        )
        
        // Populate vectors with data
        languages.zipWithIndex.foreach { case ((name, speakers, family), idx) =>
          nameVector.set(idx, name.getBytes())
          speakersVector.set(idx, speakers)
          familyVector.set(idx, family.getBytes())
        }
        
        // Set row count
        root.setRowCount(languages.size)
        
        // Write to file
        val file = new File(filename)
        val fileOutputStream = new FileOutputStream(file)
        val writer = new ArrowFileWriter(root, null, fileOutputStream.getChannel())
        
        try {
          writer.start()
          writer.writeBatch()
          writer.end()
          println(s"Successfully wrote ${languages.size} language records to $filename")
        } finally {
          writer.close()
          fileOutputStream.close()
          root.close()
        }
      }
    }

  // Read language data from Arrow file
  def readLanguageData(filename: String): IO[List[(String, Int, String)]] =
    allocatorResource.use { allocator =>
      IO {
        val file = new File(filename)
        val fileInputStream = new FileInputStream(file)
        val reader = new ArrowFileReader(fileInputStream.getChannel(), allocator)
        
        try {
          val schema = reader.getVectorSchemaRoot.getSchema
          println(s"Schema: ${schema.getFields.asScala.map(_.getName).mkString(", ")}")
          
          val results = scala.collection.mutable.ListBuffer[(String, Int, String)]()
          
          while (reader.loadNextBatch()) {
            val root = reader.getVectorSchemaRoot
            val rowCount = root.getRowCount
            
            val nameVector = root.getVector("language_name").asInstanceOf[VarCharVector]
            val speakersVector = root.getVector("speakers_millions").asInstanceOf[IntVector]
            val familyVector = root.getVector("language_family").asInstanceOf[VarCharVector]
            
            for (i <- 0 until rowCount) {
              val name = new String(nameVector.get(i))
              val speakers = speakersVector.get(i)
              val family = new String(familyVector.get(i))
              results += ((name, speakers, family))
            }
          }
          
          println(s"Successfully read ${results.size} language records from $filename")
          results.toList
          
        } finally {
          reader.close()
          fileInputStream.close()
        }
      }
    }

  // Demo function that writes and reads back data
  def demo(): IO[Unit] =
    for {
      _        <- IO.println("=== Apache Arrow Language Database Demo ===")
      filename = "languages.arrow"
      _        <- IO.println(s"Writing language data to $filename...")
      _        <- writeLanguageData(filename)
      _        <- IO.println(s"Reading language data from $filename...")
      data     <- readLanguageData(filename)
      _        <- IO.println("Language data:")
      _        <- data.traverse { case (name, speakers, family) =>
                    IO.println(f"  $name%-12s | $speakers%4d million speakers | $family")
                  }
      _        <- IO.println("Demo completed successfully!")
    } yield ()
}
