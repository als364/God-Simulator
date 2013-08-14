package cs5625.deferred.scenegraph;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import javax.media.opengl.GL2;

import cs5625.deferred.materials.BlinnPhongMaterial;
import cs5625.deferred.materials.Material;
import cs5625.deferred.misc.OpenGLResourceObject;

/**
 * Mesh.java
 * 
 * The Mesh abstract class represents a mesh of n-gons, where n is specified by the subclass.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-06
 */
public abstract class Mesh implements OpenGLResourceObject
{
	/* Material and name of this mesh. */
	private Material mMaterial = new BlinnPhongMaterial();
	private String mName = "";
	
	/* Buffers to hold vertex and polygon index data. Buffer formats are 
	 * described in the comments for the getter and setter methods, 
	 * farther down the file. */
	protected FloatBuffer mVertexData, mNormalData, mTexCoordData;
	protected IntBuffer mPolygonData, mEdgeData;
	
	/**
	 * Map of generic vertex attribute name -> generic vertex attribute buffer. The number of elements in 
	 * each buffer must match the number of vertices; each buffer's dimensionality (float, vec2, vec3, vec4) 
	 * will be inferred based on its size.
	 */
	public HashMap<String, FloatBuffer> vertexAttribData = new HashMap<String, FloatBuffer>();
	
	/**
	 * Implemented by subclasses to specify how many vertices per polygon this type of mesh has.
	 */
	public abstract int getVerticesPerPolygon();
	
	/**
	 * Implemented by subclasses to calculate surface tangent vectors based on the vertices, 
	 * normals, and texture coordinates of this mesh.
	 * 
	 * The output is a 4-vector for each vertex, storing the handedness in the w component. Bitangents 
	 * can be computed from normals and tangents as `cross(normal, tangent.xyz) * tangent.w`. 
	 */
	public abstract FloatBuffer calculateTangentVectors();

	/**
	 * Returns the name of this mesh, which can be specified by a model file or set in code.
	 * Meshes can be retrieved by name out of a `Geometry` object. 
	 */
	public String getName()
	{
		return mName;
	}
	
	/**
	 * Sets the name of this mesh.
	 */
	public void setName(String name)
	{
		mName = name;
	}

	/**
	 * Returns the material used to render this mesh.
	 */
	public Material getMaterial()
	{
		return mMaterial;
	}

	/**
	 * Sets the material used to render this mesh. Must not be null.
	 */
	public void setMaterial(Material mat)
	{
		mMaterial = mat;
	}
	
	/**
	 * Returns the number of vertices in this mesh.
	 * 
	 * This is computed from the size of the vertex data buffer.
	 */
	public int getVertexCount()
	{
		if (mVertexData == null)
		{
			return 0;
		}
		else
		{
			return mVertexData.capacity() / 3;
		}
	}
	
	/**
	 * Returns vertex data buffer. Format is 3 floats per vertex, tightly 
	 * packed: {x1, y1, z1, x2, y2, z2, ...}.
	 */
	public FloatBuffer getVertexData()
	{
		return mVertexData;
	}

	/**
	 * Sets the vertex data buffer. Format must be 3 floats per vertex, tightly 
	 * packed: {x1, y1, z1, x2, y2, z2, ...}.
	 */
	public void setVertexData(FloatBuffer vertices)
	{
		mVertexData = vertices;
	}

	/**
	 * Returns normal data buffer. Format is 3 floats per normal, tightly 
	 * packed: {x1, y1, z1, x2, y2, z2, ...}.
	 */
	public FloatBuffer getNormalData()
	{
		return mNormalData;
	}

	/**
	 * Sets normal data buffer. Format must be 3 floats per normal, tightly 
	 * packed: {x1, y1, z1, x2, y2, z2, ...}.
	 */
	public void setNormalData(FloatBuffer normals)
	{
		mNormalData = normals;
	}

	/**
	 * Returns texture coordinate data buffer. Format is 2 floats per texcoord, 
	 * tightly packed: {u1, v1, u2, v2, ...}.
	 */
	public FloatBuffer getTexCoordData()
	{
		return mTexCoordData;
	}
	
	/**
	 * Sets texture coordinate data buffer. Format is 2 floats per texcoord, 
	 * tightly packed: {u1, v1, u2, v2, ...}.
	 */
	public void setTexCoordData(FloatBuffer texcoords)
	{
		mTexCoordData = texcoords;
	}

	/**
	 * Returns the number of polygons in this mesh.
	 * 
	 * This is calculated based on the size of the polygon index buffer and the size of 
	 * polygons from the subclass.
	 */
	public int getPolygonCount()
	{
		if (mPolygonData == null)
		{
			return 0;
		}
		else
		{
			return mPolygonData.capacity() / getVerticesPerPolygon();
		}
	}
	
	/**
	 * Returns polygon index buffer. Format is `getVerticesPerPolygon()` ints per polygon 
	 * specifying the vertex indices of that polygon in counterclockwise winding order. 
	 * For example, for triangles: {i11, i12, i13, i21, i22, i23, ...}.
	 */
	public IntBuffer getPolygonData()
	{
		return mPolygonData;
	}
	
	/**
	 * Sets the polygon index buffer. Format Must be `getVerticesPerPolygon()` ints per polygon
	 * specifying the vertices of that polygon.
	 */
	public void setPolygonData(IntBuffer polys)
	{
		mPolygonData = polys;
	}

	/**
	 * Returns the edge index buffer. Format is the same as the polygon buffer (with only
	 * 2 indices per edge, of course). The edge buffer is not automatically initialized to all 
	 * edges in the mesh; it might contain a subset of edges, depending on the application. 
	 */
	public IntBuffer getEdgeData()
	{
		return mEdgeData;
	}
	
	/**
	 * Sets edge index buffer. Format is the same as the polygon index buffer (with
	 * only 2 indices per edge, of course).  
	 */
	public void setEdgeData(IntBuffer edges)
	{
		mEdgeData = edges;
	}
	
	/** 
	 * Releases OpenGL resources owned by this mesh or its material.
	 */
	public void releaseGPUResources(GL2 gl)
	{
		mMaterial.releaseGPUResources(gl);
	}
}
