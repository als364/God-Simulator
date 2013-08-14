package cs5625.deferred.scenegraph;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;

/**
 * Quadmesh.java
 * 
 * Quadmesh subclasses Mesh to implement a mesh of quadrilaterals. 
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-06
 */
public class Quadmesh extends Mesh
{
	@Override
	public int getVerticesPerPolygon()
	{
		return 4;
	}
	
	@Override
	public FloatBuffer calculateTangentVectors()
	{
		if (getVertexCount() == 0 || getPolygonCount() == 0)
		{
			return null;
		}
		
		/* This is a very direct port of Eric Lengyel's code, so I leave most of the explanation
		 * to him. Go read his page if you don't understand! */
		
		/* Allocate temporary buffers and loop over triangles. */
		float tan1[] = new float[getVertexCount() * 3];
		float tan2[] = new float[getVertexCount() * 3];
		
		for (int quadIndex = 0; quadIndex < getPolygonCount(); ++quadIndex)
		{
			/* Treat this quad as being made of two triangles. */
			for (int triVertex = 0; triVertex < 2; ++triVertex)
			{
				/* Get vertex indices of this triangle. */
				int i1 = mPolygonData.get(3 * quadIndex + 0);
				int i2 = mPolygonData.get(3 * quadIndex + triVertex);
				int i3 = mPolygonData.get(3 * quadIndex + triVertex + 1);
				
				/* Get vertex coordinates of this triangle. */
				float v1x = mVertexData.get(3 * i1 + 0);
				float v1y = mVertexData.get(3 * i1 + 1);
				float v1z = mVertexData.get(3 * i1 + 2);
	
				float v2x = mVertexData.get(3 * i2 + 0);
				float v2y = mVertexData.get(3 * i2 + 1);
				float v2z = mVertexData.get(3 * i2 + 2);
	
				float v3x = mVertexData.get(3 * i3 + 0);
				float v3y = mVertexData.get(3 * i3 + 1);
				float v3z = mVertexData.get(3 * i3 + 2);
	
				/* Get texture coordinates of this triangle. */
				float w1x = mTexCoordData.get(2 * i1 + 0);
				float w1y = mTexCoordData.get(2 * i1 + 1);
	
				float w2x = mTexCoordData.get(2 * i2 + 0);
				float w2y = mTexCoordData.get(2 * i2 + 1);
	
				float w3x = mTexCoordData.get(2 * i3 + 0);
				float w3y = mTexCoordData.get(2 * i3 + 1);
				
				/* Get positions of vertices relative to first vertex. */
		        float x1 = v2x - v1x;
		        float x2 = v3x - v1x;
		        float y1 = v2y - v1y;
		        float y2 = v3y - v1y;
		        float z1 = v2z - v1z;
		        float z2 = v3z - v1z;
	
		        /* Same for texture coordinates. */
		        float s1 = w2x - w1x;
		        float s2 = w3x - w1x;
		        float t1 = w2y - w1y;
		        float t2 = w3y - w1y;
				
				/* Compute reciprocal of determinant as explained on Lengyel's site. */ 
				float r = 1.0f / (s1 * t2 - s2 * t1);
				
				/* Take linear combination of the Q vectors, as explained on Lengyel's site. */
				float sdir_x = (t2 * x1 - t1 * x2) * r;
				float sdir_y = (t2 * y1 - t1 * y2) * r;
				float sdir_z = (t2 * z1 - t1 * z2) * r;
				
				float tdir_x = (s1 * x2 - s2 * x1) * r;
				float tdir_y = (s1 * y2 - s2 * y1) * r;
				float tdir_z = (s1 * z2 - s2 * z1) * r;
				
				/* Accumulate into temporary arrays. */
				tan1[3 * i1 + 0] += sdir_x;
				tan1[3 * i1 + 1] += sdir_y;
				tan1[3 * i1 + 2] += sdir_z;
				
				tan1[3 * i2 + 0] += sdir_x;
				tan1[3 * i2 + 1] += sdir_y;
				tan1[3 * i2 + 2] += sdir_z;
	
				tan1[3 * i3 + 0] += sdir_x;
				tan1[3 * i3 + 1] += sdir_y;
				tan1[3 * i3 + 2] += sdir_z;
	
				tan2[3 * i1 + 0] += tdir_x;
				tan2[3 * i1 + 1] += tdir_y;
				tan2[3 * i1 + 2] += tdir_z;
				
				tan2[3 * i2 + 0] += tdir_x;
				tan2[3 * i2 + 1] += tdir_y;
				tan2[3 * i2 + 2] += tdir_z;
	
				tan2[3 * i3 + 0] += tdir_x;
				tan2[3 * i3 + 1] += tdir_y;
				tan2[3 * i3 + 2] += tdir_z;
			}
		}

		/* Allocate result buffer and loop over vertices. */
		FloatBuffer result = Buffers.newDirectFloatBuffer(4 * getVertexCount());
		
		for (int vIndex = 0; vIndex < getVertexCount(); ++vIndex)
		{
			/* Get vertex normal. */
			float nx = mNormalData.get(3 * vIndex + 0);
			float ny = mNormalData.get(3 * vIndex + 1);
			float nz = mNormalData.get(3 * vIndex + 2);
			
			/* Get tentative tangent vector at this vertex. */
			float tx = tan1[3 * vIndex + 0];
			float ty = tan1[3 * vIndex + 1];
			float tz = tan1[3 * vIndex + 2];
			
			/* Orthogonalize and normalize. */
			float n_dot_t = nx * tx + ny * ty + nz * tz;
			
			float resultx = tx - nx * n_dot_t;
			float resulty = ty - ny * n_dot_t;
			float resultz = tz - nz * n_dot_t;
			float result_norm = (float)Math.sqrt(resultx * resultx + resulty * resulty + resultz * resultz);
			
			/* Compute handedness of bitangent. 'nxt' means 'n cross t'. */
			float nxt_x = ny * tz - nz * ty;
			float nxt_y = nz * tx - nx * tz;
			float nxt_z = nx * ty - ny * tx;
			
			float nxt_dot_tan2 = nxt_x * tan2[3 * vIndex + 0] + nxt_y * tan2[3 * vIndex + 1] + nxt_z * tan2[3 * vIndex + 2];
			float handedness = (nxt_dot_tan2 < 0.0f ? -1.0f : 1.0f);
			
			/* Store result. */
			result.put(4 * vIndex + 0, resultx / result_norm);
			result.put(4 * vIndex + 1, resulty / result_norm);
			result.put(4 * vIndex + 2, resultz / result_norm);
			result.put(4 * vIndex + 3, handedness);
		}
		
		/* Done. */
		return result;
	}
}
