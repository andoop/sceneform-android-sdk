package com.google.ar.sceneform.samples.utils

import android.util.Log
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.RenderableDefinition
import com.google.ar.sceneform.rendering.Vertex
import org.smurn.jply.Element
import org.smurn.jply.ElementReader
import org.smurn.jply.ElementType
import org.smurn.jply.PlyReaderFile
import java.io.File
import java.util.*


class ModelUtils {

    companion object {
        private var tag = "loadply";
        private fun fillVertices(vertices: MutableList<Vertex>, reader: ElementReader) {

            // The number of elements is known in advance. This is great for
            // allocating buffers and a like.
            // You can even get the element counts for each type before getting
            // the corresponding reader via the PlyReader.getElementCount(..)
            // method.
            Log.e(tag, "There are " + reader.count + " vertices:")
            // Read the elements. They all share the same type.
            var element: Element? = reader.readElement()
            while (element != null) {

                // Use the the 'get' methods to access the properties.
                // jPly automatically converts the various data types supported
                // by PLY for you.
                val vector3 = Vector3(element.getDouble("x").toFloat(), element.getDouble("y").toFloat(), element.getDouble("z").toFloat())
                val vertex = Vertex.builder()
                        .setPosition(vector3)
                        .setNormal(Vector3(0f,0f,1f))
                        .setColor(Color(1f,0f,0f)) //.setNormal(vector3)
                        .build()
                vertices.add(vertex)
                element = reader.readElement()
            }
        }

        private fun fillFaces(material: Material, submeshes: MutableList<RenderableDefinition.Submesh>, reader: ElementReader) {
            Log.e(tag, "There are " + reader.count + " faces:")
            var element: Element? = reader.readElement()
            while (element != null) {
                val doubleList = element.getDoubleList("vertex_indices")
                val triangleIndices: MutableList<Int> = ArrayList()
                triangleIndices.add(doubleList[0].toInt())
                triangleIndices.add(doubleList[1].toInt())
                triangleIndices.add(doubleList[2].toInt())
                val submesh = RenderableDefinition.Submesh.builder()
                        .setMaterial(material)
                        .setTriangleIndices(triangleIndices)
                        .build()
                submeshes.add(submesh)
                element = reader.readElement()
            }
        }

        fun loadPly(material: Material, vertices: MutableList<Vertex>, submeshes: MutableList<RenderableDefinition.Submesh>, path: String) {
            val ply = PlyReaderFile(File(path))
            // The elements are stored in order of their types. For each
            // type we get a reader that reads the elements of this type.
            // The elements are stored in order of their types. For each
            // type we get a reader that reads the elements of this type.
            var reader = ply.nextElementReader()
            while (reader != null) {
                val type: ElementType = reader.elementType

                // In PLY files vertices always have a type named "vertex".
                if (type.name == "vertex") {
                    fillVertices(vertices, reader)
                }

                // In PLY files vertices always have a type named "vertex".
                if (type.name == "face") {
                    fillFaces(material, submeshes, reader)
                }

                // Close the reader for the current type before getting the next one.
                reader.close()
                reader = ply.nextElementReader()
            }

            ply.close()
        }

    }
}