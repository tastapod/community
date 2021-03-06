/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen

import org.neo4j.graphdb.index.Index
import org.junit.{ Before, After }
import org.neo4j.test.ImpermanentGraphDatabase
import org.neo4j.test.GraphDescription
import scala.collection.JavaConverters._
import java.io.{ PrintWriter, File, FileWriter }
import org.neo4j.graphdb._
import org.neo4j.cypher.parser.CypherParser
import org.neo4j.cypher.{ExecutionResult, ExecutionEngine}
import org.scalatest.junit.JUnitSuite

abstract class DocumentingTestBase extends JUnitSuite {
  var db: GraphDatabaseService = null
  val parser: CypherParser = new CypherParser
  var engine: ExecutionEngine = null
  var nodes: Map[String, Node] = null
  var nodeIndex: Index[Node] = null
  var relIndex: Index[Relationship] = null
  val properties: Map[String, Map[String, Any]] = Map()

  def section: String

  def graphDescription: List[String]

  def indexProps: List[String] = List()

  def nicefy(in: String): String = in.toLowerCase.replace(" ", "-")

  def dumpToFile(writer: PrintWriter, title: String, query: String, returns: String, text: String, result: ExecutionResult) {
    writer.println("[[" + nicefy(section + " " + title) + "]]")
    writer.println("== " + title + " ==")
    writer.println(text)
    writer.println()
    writer.println("_Query_")
    writer.println()
    writer.println("[source]")
    writer.println("----")
    writer.println(query)
    writer.println("----")
    writer.println()
    writer.println(returns)
    writer.println()
    writer.println("_Result_")
    writer.println()
    writer.println("[source]")
    writer.println("----")
    writer.println(" " + result.dumpToString().replace("\n", "\n "))
    writer.println("----")
    writer.println()
    writer.println()
    writer.flush()
    writer.close()
  }

  def path: String = {
    "target/docs/ql/"
  }

  def testQuery(title: String, text: String, queryText: String, returns: String, assertions: (ExecutionResult => Unit)*) {
    var query = queryText
    nodes.keySet.foreach((key) => query = query.replace("%" + key + "%", node(key).getId.toString))
    val q = parser.parse(query)
    val result = engine.execute(q)
    assertions.foreach(_.apply(result))

    val dir = new File(path + nicefy(section))
    if (!dir.exists()) {
      dir.mkdirs()
    }

    val writer = new PrintWriter(new FileWriter(new File(dir, nicefy(title) + ".txt")))

    dumpToFile(writer, title, query, returns, text, result)
  }

  def indexProperties[T <: PropertyContainer](n: T, index: Index[T]) {
    indexProps.foreach((property) =>
      {
        if (n.hasProperty(property)) {
          val value = n.getProperty(property)
          index.add(n, property, value)
        }
      })
  }

  def node(name: String): Node = nodes.getOrElse(name, throw new NotFoundException(name))

  @After
  def teardown() {
    db.shutdown()
  }

  @Before
  def init() {
    db = new ImpermanentGraphDatabase()
    engine = new ExecutionEngine(db)

    val tx = db.beginTx()
    nodeIndex = db.index().forNodes("nodes")
    relIndex = db.index().forRelationships("rels")
    val description = GraphDescription.create(graphDescription: _*)

    nodes = description.create(db).asScala.toMap

    db.getAllNodes.asScala.foreach((n) =>
      {
        indexProperties(n, nodeIndex)
        n.getRelationships(Direction.OUTGOING).asScala.foreach(indexProperties(_, relIndex))
      })

    properties.foreach((n) =>
      {
        val nod = node(n._1)
        n._2.foreach((kv) => nod.setProperty(kv._1, kv._2))
      })

    tx.success()
    tx.finish()
  }
}