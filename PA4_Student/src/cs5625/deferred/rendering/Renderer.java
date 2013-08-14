package cs5625.deferred.rendering;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import cs5625.deferred.materials.CloudMaterial;
import cs5625.deferred.materials.Material;
import cs5625.deferred.materials.Texture.Datatype;
import cs5625.deferred.materials.Texture.Format;
import cs5625.deferred.materials.Texture2D;
import cs5625.deferred.materials.UnshadedMaterial;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.ScenegraphException;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.Light;
import cs5625.deferred.scenegraph.Mesh;
import cs5625.deferred.scenegraph.ParticleSystem;
import cs5625.deferred.scenegraph.PointLight;
import cs5625.deferred.scenegraph.SceneObject;

/**
 * Renderer.java
 * 
 * The Renderer class is in charge of rendering a scene using deferred shading. This happens in 5 stages, 
 * described below. In this description, numbers in {curly braces} indicate g-buffer texture indices.
 * 
 * 1. Render into gbuffer {0 = diffuse, 1 = material props, 2 = position, 3 = normal} of each fragment.
 * 2. Render into gbuffer {4 = gradients} based on position and normal buffers, for edge detection.
 * 3. Render into gbuffer {5 = shaded scene} the final opaque scene, using all previous buffers.
 * 4. Render any translucent geometry on top of {5}, lit using forward shading. 
 * 5. Output {5} to window.   
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-23
 */
public class Renderer
{
	/* Viewport attributes. */
	private float mViewportWidth, mViewportHeight;
	
	/* The GBuffer FBO. */
	private FramebufferObject mGBufferFBO;
	
	/* Name the indices in the GBuffer so code is easier to read. */
	private final int GBuffer_DiffuseIndex = 0;
	private final int GBuffer_MaterialIndex = 1;
	private final int GBuffer_PositionIndex = 2;
	private final int GBuffer_NormalIndex = 3;
	private final int GBuffer_GradientsIndex = 4;
	private final int GBuffer_FinalSceneIndex = 5;
	private final int GBuffer_Count = 6;
	
	
	
	
	//TEXTURES LOOK
	public Texture2D[] textures = new Texture2D[14];
	
	
	
	
	
	/* The index of the texture to preview in GBufferFBO, or -1 for no preview. */
	private int mPreviewIndex = -1;
	
	/* List of lights in the scene, assembled every frame. */
	private ArrayList<Light> mLights = new ArrayList<Light>();
	
	/* Cache of shaders used by all the materials in the scene. Storing the shaders here instead of in 
	 * the Material classes themselves allows the shaders to be local to the renderer and the OpenGL 
	 * context, which is appropriate. */
	private HashMap<Class<? extends Material>, ShaderProgram> mShaderCache = new HashMap<Class<? extends Material>, ShaderProgram>();

	/* The "ubershader" used for performing deferred shading on the gbuffer, 
	 * and the silhouette shader to compute edges for toon rendering. */
	private ShaderProgram mUberShader, mSilhouetteShader;
	private boolean mEnableToonShading = false;
	
	/* Material for rendering generic wireframes and crease edges, and flag to enable/disable that. */
	private Material mWireframeMaterial, mWireframeMarkedEdgeMaterial;
	private boolean mRenderWireframes = false;
	
	/* Locations of uniforms in the ubershader. */
	private int mLightPositionsUniformLocation = -1;
	private int mLightColorsUniformLocation = -1;
	private int mLightAttenuationsUniformLocation = -1;
	private int mNumLightsUniformLocation = -1;
	private int mEnableToonShadingUniformLocation = -1;
	private int mMaxLightsInUberShader = -1;
	
	
	
	
	/**
	 * Renders a single frame of the scene. This is the main method of the Renderer class.
	 * 
	 * @param drawable The drawable to render into.
	 * @param sceneRoot The root node of the scene to render.
	 * @param camera The camera describing the perspective to render from.
	 */
	public void render(GLAutoDrawable drawable, SceneObject sceneRoot, Camera camera)
	{
		GL2 gl = drawable.getGL().getGL2();
		
		
		
		/* Reset lights array. It will be re-filled as the scene is traversed. */
		mLights.clear();
				
		try
		{
			/* 1. Fill the gbuffer given this scene and camera. */ 
			fillGBuffer(gl, sceneRoot, camera);
			
			/* 2. Compute gradient buffer based on positions and normals, used for toon shading. */
			computeGradientBuffer(gl);
			
			/* 3. Apply deferred lighting to the g-buffer. At this point, the opaque scene has 
			 * been rendered. */
			lightGBuffer(gl, camera);
			
			/* 4. Render any translucent geometry in a forward shading pass. */
			compositeTranslucentGeometry(gl, sceneRoot, camera);

			/* 5. If we're supposed to preview one gbuffer texture, do that now. 
			 * Otherwise, output the final scene. */
			if (mPreviewIndex >= 0 && mPreviewIndex < GBuffer_FinalSceneIndex)
			{
				renderTextureFullscreen(gl, mGBufferFBO.getColorTexture(mPreviewIndex));
			}
			else
			{
				renderTextureFullscreen(gl, mGBufferFBO.getColorTexture(GBuffer_FinalSceneIndex));
			}
		}
		catch (Exception err)
		{
			/* If an error occurs in all that, print it, but don't kill the whole program. */
			err.printStackTrace();
		}
	}
	
	/**
	 * Clears the gbuffer and renders scene objects with opaque materials into it.
	 * Translucent materials are rendered in a forward shading pass by `compositeTranslucentGeometry()`.
	 *
	 * @param gl The OpenGL state
	 * @param sceneRoot The root node of the scene to render.
	 * @param camera The camera describing the perspective to render from.
	 */
	private void fillGBuffer(GL2 gl, SceneObject sceneRoot, Camera camera) throws OpenGLException
	{
		GLU glu = GLU.createGLU(gl);

		/* First, bind and clear the gbuffer. */
		mGBufferFBO.bindSome(gl, new int[]{GBuffer_DiffuseIndex, GBuffer_MaterialIndex, GBuffer_PositionIndex, GBuffer_NormalIndex});
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		/* Update the projection matrix with this camera's projection matrix. */
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(camera.getFOV(), mViewportWidth / mViewportHeight, camera.getNear(), camera.getFar());
		
		/* Update the modelview matrix with this camera's eye transform. */
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		/* Find the inverse of the camera scale, position, and orientation in world space, accounting
		 * for the fact that the camera might be nested inside other objects in the scenegraph.*/
		float cameraScale = 1.0f / camera.transformDistanceToWorldSpace(1.0f);
		Point3f cameraPosition = camera.transformPointToWorldSpace(new Point3f(0.0f, 0.0f, 0.0f));
		AxisAngle4f cameraOrientation = new AxisAngle4f();
		cameraOrientation.set(camera.transformOrientationToWorldSpace(new Quat4f(0.0f, 0.0f, 0.0f, 1.0f)));
		
		/* Apply the camera transform to OpenGL. */
		gl.glScalef(cameraScale, cameraScale, cameraScale);
		gl.glRotatef(cameraOrientation.angle * 180.0f / (float)Math.PI, -cameraOrientation.x, -cameraOrientation.y, -cameraOrientation.z);
		gl.glTranslatef(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		
		/* Check for errors before rendering, to help isolate. */
		OpenGLException.checkOpenGLError(gl);
		
		/* Render the scene, opaque only. */
		renderObject(gl, camera, sceneRoot, true);

		/* GBuffer is filled, so unbind it. */
		mGBufferFBO.unbind(gl);
		
		/* Check for errors after rendering, to help isolate. */
		OpenGLException.checkOpenGLError(gl);
	}

	/**
	 * Computes position and normal gradients based on the position and normal textures of the GBuffer, for 
	 * use in edge detection (e.g. toon rendering). 
	 */
	private void computeGradientBuffer(GL2 gl) throws OpenGLException
	{
		/* Bind silhouette buffer as output. */
		mGBufferFBO.bindOne(gl, GBuffer_GradientsIndex);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		
		/* Save state before we disable depth testing for blitting. */
		gl.glPushAttrib(GL2.GL_ENABLE_BIT);
		
		/* Disable depth test and blend, since we just want to replace the contents of the framebuffer.
		 * Since we are rendering an opaque fullscreen quad here, we don't bother clearing the buffer
		 * first. */
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		
		/* Bind the position and normal textures so the edge-detection shader ecan read them. */
		mGBufferFBO.getColorTexture(GBuffer_PositionIndex).bind(gl, 0);
		mGBufferFBO.getColorTexture(GBuffer_NormalIndex).bind(gl, 1);
		
		/* Bind silhouette shader and render. */
		mSilhouetteShader.bind(gl);
		drawFullscreenQuad(gl, mViewportWidth, mViewportHeight);
		
		/* Unbind everything. */
		mSilhouetteShader.unbind(gl);
		mGBufferFBO.getColorTexture(GBuffer_PositionIndex).unbind(gl);
		mGBufferFBO.getColorTexture(GBuffer_NormalIndex).unbind(gl);

		mGBufferFBO.unbind(gl);

		/* Restore attributes (blending and depth-testing) to as they were before. */
		gl.glPopAttrib();
	}
	
	/**
	 * Applies lighting to an already-filled gbuffer to produce the final scene. Output is sent 
	 * to the main framebuffer of the view/window.
	 * 
	 * @param gl The OpenGL state.
	 * @param camera Camera from whose perspective we are rendering.
	 */
	private void lightGBuffer(GL2 gl, Camera camera) throws OpenGLException, ScenegraphException
	{
		/* Need some lights, otherwise it will just be black! */
		if (mLights.size() == 0)
		{
			throw new ScenegraphException("Must have at least one light in the scene!");
		}
		
		/* Can't have more lights than the shader supports. */
		if (mLights.size() > mMaxLightsInUberShader)
		{
			throw new ScenegraphException(mLights.size() + " is too many lights; ubershader only supports " + mMaxLightsInUberShader + ".");
		}
		
		/* Bind final scene buffer as output target for this pass. */
		mGBufferFBO.bindOne(gl, GBuffer_FinalSceneIndex);
		
		/* Save state before we disable depth testing for blitting. */
		gl.glPushAttrib(GL2.GL_ENABLE_BIT);
		
		/* Disable depth test and blend, since we just want to replace the contents of the framebuffer.
		 * Since we are rendering an opaque fullscreen quad here, we don't bother clearing the buffer
		 * first. */
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		
		/* Bind all GBuffer source textures so the ubershader can read them. */
		for (int i = 0; i < GBuffer_FinalSceneIndex; ++i)
		{
			mGBufferFBO.getColorTexture(i).bind(gl, i);
		}
		
		/* Bind ubershader. */
		mUberShader.bind(gl);
		
		int lightIndex = 0;
		/* Update all the ubershader uniforms with up-to-date light information. */
		for (int i = 0; i < mLights.size(); ++i)
		{
			/* Transform each light position to eye space. */
			Light light = mLights.get(i);
			
			//only adds the light if it's enabled
			if (light.isEnabled()) {
				Point3f eyespacePosition = camera.transformPointFromWorldSpace(light.transformPointToWorldSpace(new Point3f()));
//				System.out.println(Integer.toString(i) + ": " + light.getPosition().toString());
				
				/* Send light color and eyespace position to the ubershader. */
				gl.glUniform3f(mLightPositionsUniformLocation + lightIndex, eyespacePosition.x, eyespacePosition.y, eyespacePosition.z);
				gl.glUniform3f(mLightColorsUniformLocation + lightIndex, light.getColor().x, light.getColor().y, light.getColor().z);
				
				if (light instanceof PointLight)
				{
					gl.glUniform3f(mLightAttenuationsUniformLocation + lightIndex, 
							((PointLight)light).getConstantAttenuation(), 
							((PointLight)light).getLinearAttenuation(), 
							((PointLight)light).getQuadraticAttenuation());
				}
				else
				{
					gl.glUniform3f(mLightAttenuationsUniformLocation + lightIndex, 1.0f, 0.0f, 0.0f);
				}
				lightIndex++;
			}
		}
		
		/* Ubershader needs to know how many lights. */
		gl.glUniform1i(mNumLightsUniformLocation, lightIndex);	
		gl.glUniform1i(mEnableToonShadingUniformLocation, (mEnableToonShading ? 1 : 0));	

		/* Let there be light! */
		drawFullscreenQuad(gl, mViewportWidth, mViewportHeight);
		
		/* Unbind everything. */
		mUberShader.unbind(gl);
		
		for (int i = 0; i < GBuffer_FinalSceneIndex; ++i)
		{
			mGBufferFBO.getColorTexture(i).unbind(gl);
		}

		/* Unbind rendering target. */
		mGBufferFBO.unbind(gl);

		/* Restore attributes (blending and depth-testing) to as they were before. */
		gl.glPopAttrib();
	}
	
	/**
	 * Renders translucent geometry on top of a gbuffer already containing the opaque scene. 
 	 * 
	 * @param gl The OpenGL state
	 * @param sceneRoot The root node of the scene to render.
	 * @param camera The camera describing the perspective to render from.
	 */
	private void compositeTranslucentGeometry(GL2 gl, SceneObject sceneRoot, Camera camera) throws OpenGLException
	{
		GLU glu = GLU.createGLU(gl);

		/* First, bind the gbuffer. We want composite over the existing color and depth buffers of the 
		 * opaque scene, so don't clear anything. */
		mGBufferFBO.bindOne(gl, GBuffer_FinalSceneIndex);
		
		/* Update the projection matrix with this camera's projection matrix. */
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(camera.getFOV(), mViewportWidth / mViewportHeight, camera.getNear(), camera.getFar());
		
		/* Update the modelview matrix with this camera's eye transform. */
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		/* Find the inverse of the camera scale, position, and orientation in world space, accounting
		 * for the fact that the camera might be nested inside other objects in the scenegraph.*/
		float cameraScale = 1.0f / camera.transformDistanceToWorldSpace(1.0f);
		Point3f cameraPosition = camera.transformPointToWorldSpace(new Point3f(0.0f, 0.0f, 0.0f));
		AxisAngle4f cameraOrientation = new AxisAngle4f();
		cameraOrientation.set(camera.transformOrientationToWorldSpace(new Quat4f(0.0f, 0.0f, 0.0f, 1.0f)));
		
		/* Apply the camera transform to OpenGL. */
		gl.glScalef(cameraScale, cameraScale, cameraScale);
		gl.glRotatef(cameraOrientation.angle * 180.0f / (float)Math.PI, -cameraOrientation.x, -cameraOrientation.y, -cameraOrientation.z);
		gl.glTranslatef(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		
		/* Check for errors before rendering, to help isolate. */
		OpenGLException.checkOpenGLError(gl);
		
		/* Render the scene, translucent only. */
		renderObject(gl, camera, sceneRoot, false);

		/* GBuffer is filled, so unbind it. */
		mGBufferFBO.unbind(gl);
		
		/* Check for errors after rendering, to help isolate. */
		OpenGLException.checkOpenGLError(gl);
	}
	
	/**
	 * Renders a scenegraph node and its children.
	 * 
	 * @param gl The OpenGL state.
	 * @param camera The camera rendering the scene.
	 * @param obj The object to render. If this is a Geometry object, its meshes are rendered.
	 *        If this is a Light object, it is added to the list of lights. Other objects are ignored.
	 * @param opaque Only meshes with materials whose `isOpaque()` flag matches this flag will be rendered.
	 */
	private void renderObject(GL2 gl, Camera camera, SceneObject obj, boolean opaque) throws OpenGLException
	{
		/* Save matrix before applying this object's transformation. */
		gl.glPushMatrix();
		
		/* Get this object's transformation. */
		float scale = obj.getScale();
		Point3f position = obj.getPosition();
		AxisAngle4f orientation = new AxisAngle4f();
		orientation.set(obj.getOrientation());
		
		/* Apply this object's transformation. */
		gl.glTranslatef(position.x, position.y, position.z);
		gl.glRotatef(orientation.angle * 180.0f / (float)Math.PI, orientation.x, orientation.y, orientation.z);
		gl.glScalef(scale, scale, scale);
		
		/* Render this object as appropriate for its type. */
		if (obj instanceof Geometry)
		{
			for (Mesh mesh : ((Geometry)obj).getMeshes())
			{
				renderMesh(gl, mesh, opaque, camera);
			}
		}
		else if (obj instanceof Light)
		{
			/* Assume the opaque pass happens first; by the translucent pass, we already 
			 * have the list of lights. */
			if (opaque)
			{
				mLights.add((Light)obj);
			}
		}
		else if (obj instanceof ParticleSystem)
		{
			((ParticleSystem)obj).billboard(camera);
		}
		
		/* Render this object's children. */
		for (SceneObject child : obj.getChildren())
		{
			renderObject(gl, camera, child, opaque);
		}
		
		/* Restore transformation matrix and check for errors. */
		gl.glPopMatrix();
		OpenGLException.checkOpenGLError(gl);
	}

	/**
	 * Renders a single trimesh.
	 * 
	 * @param gl The OpenGL state.
	 * @param mesh The mesh to render.
	 * @param opaque Only meshes with materials whose `isOpaque()` flag matches this flag will be rendered.
	 */
	private void renderMesh(GL2 gl, Mesh mesh, boolean opaque, Camera camera) throws OpenGLException
	{
		/* Skip any materials which aren't supposed to be rendered in this pass. */ 
		if (mesh.getMaterial().isOpaque() != opaque)
		{
			return;
		}
		
		/* Save all state to isolate any changes made by this mesh's material. */
		gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
		gl.glPushClientAttrib((int)GL2.GL_CLIENT_ALL_ATTRIB_BITS);
		
		/* Activate the material. */
		mesh.getMaterial().retrieveShader(gl, mShaderCache);
		mesh.getMaterial().setLights(mLights);
		mesh.getMaterial().setCamera(camera);
		
		mesh.getMaterial().bind(gl);
			
		/* Enable the required vertex arrays and send data. */
		if (mesh.getVertexData() == null)
		{
			throw new OpenGLException("Mesh must have non-null vertex data to render!");
		}
		else
		{
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, mesh.getVertexData());
		}

		if (mesh.getNormalData() == null)
		{
			gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		}
		else
		{
			gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL2.GL_FLOAT, 0, mesh.getNormalData());
		}
		
		if (mesh.getTexCoordData() == null)
		{
			gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		}
		else
		{
			gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, mesh.getTexCoordData());
		}

		/* Send custom vertex attributes (if any) to OpenGL. */
		bindRequiredMeshAttributes(gl, mesh);
		
		/* Render polygons. */
		gl.glDrawElements(getOpenGLPrimitiveType(mesh.getVerticesPerPolygon()), 
						  mesh.getVerticesPerPolygon() * mesh.getPolygonCount(), 
						  GL2.GL_UNSIGNED_INT, 
						  mesh.getPolygonData());
				
		/* Deactivate material and restore state. */
		mesh.getMaterial().unbind(gl);
		
		/* Render mesh wireframe if we're supposed to. */
		if (mRenderWireframes && mesh.getVerticesPerPolygon() > 2)
		{
			mWireframeMaterial.retrieveShader(gl, mShaderCache);
			mWireframeMaterial.bind(gl);

			gl.glLineWidth(1.0f);
			gl.glPolygonOffset(0.0f, 1.0f);
			gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);

			/* Render polygons. */
			gl.glDrawElements(getOpenGLPrimitiveType(mesh.getVerticesPerPolygon()), 
					  mesh.getVerticesPerPolygon() * mesh.getPolygonCount(), 
					  GL2.GL_UNSIGNED_INT, 
					  mesh.getPolygonData());					
			
			mWireframeMaterial.unbind(gl);
		}

		/* Render marked edges (e.g. for subdiv creases), if we're supposed to and if they exist. */
		if (mRenderWireframes && mesh.getEdgeData() != null)
		{
			mWireframeMarkedEdgeMaterial.retrieveShader(gl, mShaderCache);
			mWireframeMarkedEdgeMaterial.bind(gl);

			gl.glLineWidth(5.0f);
			gl.glPolygonOffset(0.0f, 1.0f);
			gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
			gl.glDrawElements(GL2.GL_LINES, mesh.getEdgeData().capacity(), GL2.GL_UNSIGNED_INT, mesh.getEdgeData());
			
			mWireframeMarkedEdgeMaterial.unbind(gl);
		}
		
		gl.glPopClientAttrib();
		gl.glPopAttrib();
		
		/* Check for errors. */
		OpenGLException.checkOpenGLError(gl);
	}
	
	/**
	 * Binds all custom vertex attributes required by a mesh's material to buffers provided
	 * by that mesh.
	 * 
	 * @param gl The OpenGL state.
	 * @param mesh All custom vertex attributes required by mesh's material and shader are bound to the 
	 *        correspondingly-named buffers in the mesh's `vertexAttribData` map.
	 *        
	 * @throws OpenGLException If a required attribute isn't supplied by the mesh.
	 */
	void bindRequiredMeshAttributes(GL2 gl, Mesh mesh) throws OpenGLException
	{
		ShaderProgram shader = mesh.getMaterial().getShaderProgram();
		
		for (String attrib : mesh.getMaterial().getRequiredVertexAttributes())
		{
			/* Ignore attributes which aren't actually used in the shader. */
			int location = shader.getAttribLocation(gl, attrib);
			if (location < 0)
			{
				continue;
			}
			
			/* Get data for this attribute from the mesh. */
			FloatBuffer attribData = mesh.vertexAttribData.get(attrib);
			
			/* This attribute is required, so throw an exception if the mesh doesn't supply it. */
			if (attribData == null)
			{
				throw new OpenGLException("Material requires vertex attribute '" + attrib + "' which is not present in mesh's vertexAttribData.");
			}
			else
			{
				gl.glEnableVertexAttribArray(location);
				gl.glVertexAttribPointer(location, attribData.capacity() / mesh.getVertexCount(), GL2.GL_FLOAT, false, 0, attribData);
			}
		}
	}

	/**
	 * Returns the OpenGL primitive type for the given size of polygon (e.g. GL_TRIANGLES for 3).
	 * @throws OpenGLException For values not in {1, 2, 3, 4}.
	 */
	private int getOpenGLPrimitiveType(int verticesPerPolygon) throws OpenGLException
	{
		switch (verticesPerPolygon)
		{
		case 1: return GL2.GL_POINTS;
		case 2: return GL2.GL_LINES;
		case 3: return GL2.GL_TRIANGLES;
		case 4: return GL2.GL_QUADS;
		default: throw new OpenGLException("Don't know how to render mesh with " + verticesPerPolygon + " vertices per polygon.");
		}
	}
	
	/**
	 * Requests that the renderer should render a preview of the indicated gbuffer texture, instead of the final shaded scene.
	 * 
	 * @param bufferIndex The index of the texture to preview. If `bufferIndex` is out of range (less than 0 or greater than
	 *        the index of the last gbuffer texture), the preview request will be ignored, and the renderer will render a 
	 *        shaded scene.
	 */
	public void previewGBuffer(int bufferIndex)
	{
		mPreviewIndex = bufferIndex;
	}

	/**
	 * Cancels a preview request made with `previewGBuffer()`, causing the renderer to render the final shaded scene when it renders.
	 */
	public void unpreviewGBuffer()
	{
		mPreviewIndex = -1;
	}
	
	/**
	 * Enables or disables toon shading.
	 */
	public void setToonShading(boolean toonShade)
	{
		mEnableToonShading = toonShade;
	}
	
	/**
	 * Returns true if toon shading is enabled.
	 */
	public boolean getToonShading()
	{
		return mEnableToonShading;
	}
	
	/**
	 * Enables or disables rendering of mesh edges.
	 * 
	 * All edges are rendered in thin grey wireframe, and marked edges (e.g. creases) are rendered in thick pink. 
	 */
	public void setRenderWireframes(boolean wireframe)
	{
		mRenderWireframes = wireframe;
	}
	
	/**
	 * Returns true if mesh edges are being rendered.
	 */
	public boolean getRenderWireframes()
	{
		return mRenderWireframes;
	}
	
	/**
	 * Clears the display and renders a fullscreen quad with the passed texture.
	 * 
	 * @param gl The OpenGL state.
	 * @param texture The texture to display.
	 */
	private void renderTextureFullscreen(GL2 gl, Texture2D texture) throws OpenGLException
	{
		/* Save state and make sure the output will overwrite whatever was there. This way
		 * we don't have to waste time clearing buffers. */
		gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_ENABLE_BIT);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		
		/* Draw the texture. */
		texture.blit(gl);
		
		/* Restore state. */
		gl.glPopAttrib();
	}
	
	/**
	 * Utility function which draws a fullscreen quad.
	 * 
	 * Can be used whenever you need one (e.g. postprocessing).
	 * 
	 * @param gl The OpenGL state.
	 * @param smax The maximum s (or u) texture coordinate for the quad. The minimum is assumed to be 0.
	 * @param tmax The maximum t (or v) texture coordinate for the quad. The minimum is assumed to be 0.
	 */
	public static void drawFullscreenQuad(GL2 gl, float smax, float tmax) throws OpenGLException
	{
		/* Save which matrix is active. */
		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
		
		/* Reset projection and modelview matrices. */
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		
		/* Render the quad. */
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex2f(-1.0f, -1.0f);
		gl.glTexCoord2f(smax, 0.0f); gl.glVertex2f( 1.0f, -1.0f);
		gl.glTexCoord2f(smax, tmax); gl.glVertex2f( 1.0f,  1.0f);
		gl.glTexCoord2f(0.0f, tmax); gl.glVertex2f(-1.0f,  1.0f);
		gl.glEnd();
		
		/* Restore matrices. */
		gl.glPopMatrix();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();

		/* Restore active matrix. */
		gl.glPopAttrib();
		
		/* Make sure nothing went wrong. */
		OpenGLException.checkOpenGLError(gl);
	}
	
	/**
	 * Performs one-time initialization of OpenGL state and shaders used by this renderer.
	 * @param drawable The OpenGL drawable this renderer will be rendering to.
	 */
	public void init(GLAutoDrawable drawable)
	{
		GL2 gl = drawable.getGL().getGL2();
		//ALL TEXTURES CREATED HERE LOOK
		if (textures[0] == null) {
		try {
			System.out.println("rendering");
			Texture2D tex1 = Texture2D.load(gl, "models/cloud.png");
			Texture2D tex2 = Texture2D.load(gl, "models/cloud2.png");
			Texture2D tex3 = Texture2D.load(gl, "models/cloud3.png");
			Texture2D tex4 = Texture2D.load(gl, "models/cloud4.png");
			Texture2D tex5 = Texture2D.load(gl, "models/cloud5.png");
			Texture2D tex6 = Texture2D.load(gl, "models/cloud6.png");
			Texture2D tex7 = Texture2D.load(gl, "models/cloud7.png");
			Texture2D tex8 = Texture2D.load(gl, "models/cloud8.png");
			Texture2D tex9 = Texture2D.load(gl, "models/snowflake.png");
			Texture2D tex10 = Texture2D.load(gl, "models/lightning.png");
			Texture2D tex11 = Texture2D.load(gl, "models/lightning2.png");
			Texture2D tex12 = Texture2D.load(gl, "models/lightning3.png");
			Texture2D tex13 = Texture2D.load(gl, "models/raindrop.png");
			
			Texture2D tex14 = Texture2D.load(gl, "models/clear.png");
			if (tex1 == null) System.out.println("texture was null WTF");
			else {
				textures[0] = tex1;
				textures[1] = tex2;
				textures[2] = tex3;
				textures[3] = tex4;
				textures[4] = tex5;
				textures[5] = tex6;
				textures[6] = tex7;
				textures[7] = tex8;
				textures[8] = tex9;
				textures[9] = tex10;
				textures[10] = tex11;
				textures[11] = tex12;
				textures[12] = tex13;
				textures[13] = tex14;
			}
			
			System.out.println("set texture in renderer");
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}

		/* Enable depth testing. */
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);

		try
		{
			/* Load the ubershader. */
			mUberShader = new ShaderProgram(gl, "shaders/ubershader");

			/* Set material buffer indices once here, since they never have to change. */
			mUberShader.bind(gl);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "DiffuseBuffer"), 0);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "MaterialParamsBuffer"), 1);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "PositionBuffer"), 2);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "NormalBuffer"), 3);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "SilhouetteBuffer"), 4);
			gl.glUniform3f(mUberShader.getUniformLocation(gl, "SkyColor"), 0.1f, 0.1f, 0.1f);
			mUberShader.unbind(gl);
			
			/* Get locations of the lighting uniforms, since these will have to be updated every frame. */
			mLightPositionsUniformLocation = mUberShader.getUniformLocation(gl, "LightPositions");
			mLightColorsUniformLocation = mUberShader.getUniformLocation(gl, "LightColors");
			mLightAttenuationsUniformLocation = mUberShader.getUniformLocation(gl, "LightAttenuations");
			mNumLightsUniformLocation = mUberShader.getUniformLocation(gl, "NumLights");
			mEnableToonShadingUniformLocation = mUberShader.getUniformLocation(gl, "EnableToonShading");
			
			/* Get the maximum number of lights the shader supports. */
			//int arraySize[] = new int[1];
			//gl.glGetActiveUniform(mUberShader.getHandle(), mLightPositionsUniformLocation, /* shader and uniform handles */ 
			//					  0, null, 0, /* name string buffer size (don't care) */ 
			//					  arraySize, 0, /* uniform array size buffer */
			//					  null, 0, /* uniform type buffer (don't care) */
			//					  null, 0); /* name buffer (don't care) */
			mMaxLightsInUberShader = 100;
			
			/* Load the silhouette (edge-detection) shader. */
			mSilhouetteShader = new ShaderProgram(gl, "shaders/silhouette");

			mSilhouetteShader.bind(gl);
			gl.glUniform1i(mSilhouetteShader.getUniformLocation(gl, "PositionBuffer"), 0);
			gl.glUniform1i(mSilhouetteShader.getUniformLocation(gl, "NormalBuffer"), 1);
			mSilhouetteShader.unbind(gl);
			
			/* Load the material used to render mesh edges (e.g. creases for subdivs). */
			mWireframeMaterial = new UnshadedMaterial(new Color3f(0.8f, 0.8f, 0.8f));
			mWireframeMarkedEdgeMaterial = new UnshadedMaterial(new Color3f(1.0f, 0.0f, 1.0f));
			
			/* Make sure nothing went wrong. */
			OpenGLException.checkOpenGLError(gl);
		}
		catch (Exception err)
		{
			/* If something did go wrong, we can't render - so just die. */
			err.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Called whenever the OpenGL context changes size. This renderer resizes the gbuffer 
	 * so it's always the same size as the viewport.
	 * 
	 * @param drawable The drawable being rendered to.
	 * @param width The new viewport width.
	 * @param height The new viewport height.
	 */
	public void resize(GLAutoDrawable drawable, int width, int height)
	{
		GL2 gl = drawable.getGL().getGL2();
		
		/* Store viewport size. */
		mViewportWidth = width;
		mViewportHeight = height;
		
		/* If we already had a gbuffer, release it. */
		if (mGBufferFBO != null)
		{
			mGBufferFBO.releaseGPUResources(gl);
		}
		
		/* Make a new gbuffer with the new size. */
		try
		{
			mGBufferFBO = new FramebufferObject(gl, Format.RGBA, Datatype.FLOAT16, width, height, GBuffer_Count, true, true);
		}
		catch (OpenGLException err)
		{
			/* If that fails, we can't render - so just die. */
			err.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Releases all OpenGL resources (shaders and FBOs) owned by this renderer.
	 */
	public void releaseGPUResources(GL2 gl)
	{
		mGBufferFBO.releaseGPUResources(gl);
		mUberShader.releaseGPUResources(gl);
		mSilhouetteShader.releaseGPUResources(gl);
	}
	

}
