package cs5625.deferred.materials;

import javax.media.opengl.GL2;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.rendering.ShaderProgram;

/**
 * ParticleMaterial.java
 * 
 * A textured translucent material aimed at particle systems. 
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-15
 */
public class PrecipitationMaterial extends Material
{
	/* Material properties. */
	private Color4f mColor = new Color4f(1.0f, 1.0f, 1.0f, 1.0f);
	private Texture2D mTexture = null;
	
	private Point3f startPosition;
	private float maxy;
	private float difference;
	private float minDiff;
	

	/* Uniform locations. */
	private int mUniformLocation = -1;
	private int mHasTextureUniformLocation = -1;
	private int mPosUniformLocation = -1;
	private int mMaxUniformLocation = -1;
	private int mDiffUniformLocation = -1;
	private int mMinUniformLocation = -1;
	
	
	 public void setDiff(float p) {
			difference = p;
		}
	 
	 public void setMin(float p) {
			minDiff = p;
		}
	 
    public void setPosition(Point3f p) {
		startPosition = p;
	}
	
	public void setMax(float p) {
		maxy = p;
	}


	public PrecipitationMaterial()
	{
		/* Default constructor. */
	}

	public PrecipitationMaterial(Texture2D tex)
	{
		mTexture = tex;
	}

	public PrecipitationMaterial(Color4f color)
	{
		mColor.set(color);
	}

	@Override
	public boolean isOpaque()
	{
		/* This material must be forward-shaded. */
		return false;
	}
		
	public Color4f getColor()
	{
		return mColor;
	}
	
	public void setColor(Color4f color)
	{
		mColor = color;
	}
	
	public Texture2D getTexture()
	{
		return mTexture;
	}
	
	public void setTexture(Texture2D texture)
	{
		mTexture = texture;
	}

	@Override
	public void bind(GL2 gl) throws OpenGLException
	{
		OpenGLException.checkOpenGLError(gl);
		/* Bind shader, and any textures, and update uniforms. */
		getShaderProgram().bind(gl);
		
		gl.glUniform4f(mUniformLocation, mColor.x, mColor.y, mColor.z, mColor.w);
		
		gl.glUniform1f(mMaxUniformLocation, maxy);
		gl.glUniform3f(mPosUniformLocation, startPosition.x, startPosition.y, startPosition.z);
		gl.glUniform1f(mDiffUniformLocation, difference);
		gl.glUniform1f(mMinUniformLocation, minDiff);
		
		
		gl.glUniform1i(mHasTextureUniformLocation, (mTexture == null ? 0 : 1));

		if (mTexture != null)
		{
			mTexture.bind(gl, 0);
		}
		
		/* Expect pre-multiplied alpha from the shader. This allows us to support both
		 * several types of blending in a single pass:
		 * 
		 *       no blending: gl_FragColor = vec4(color, 1.0);
		 *    alpha blending: gl_FragColor = vec4(color * alpha, alpha);
		 * additive blending: gl_FragColor = vec4(color, 0.0);
		 * 
		 * The default particle shader uses alpha blending, but you can subclass ParticleMaterial
		 * and write your own shader; if it follows this alpha convention it will "just work".
		 */
		gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_BLEND);
		
		/* Disable writing of depth values by these particles. They will still clip against the 
		 * opaque scene geometry, but not against other particles. */
		gl.glDepthMask(false);
		
		OpenGLException.checkOpenGLError(gl);
	}

	@Override
	public void unbind(GL2 gl)
	{
		/* Unbind anything bound in bind(). */
		getShaderProgram().unbind(gl);

		if (mTexture != null)
		{
			mTexture.unbind(gl);
		}
	}
	
	@Override
	protected void initializeShader(GL2 gl, ShaderProgram shader)
	{
		/* Get locations of uniforms in this shader. */
		mUniformLocation = shader.getUniformLocation(gl, "Color");
		mHasTextureUniformLocation = shader.getUniformLocation(gl, "HasTexture");
		mPosUniformLocation = shader.getUniformLocation(gl, "startPosition");
		mMaxUniformLocation = shader.getUniformLocation(gl, "maxPoint");
		mDiffUniformLocation = shader.getUniformLocation(gl, "difference");
		mMinUniformLocation = shader.getUniformLocation(gl, "maxDiff");

		/* This uniform won't ever change, so just set it here. */
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "Texture"), 0);
		shader.unbind(gl);
	}

	@Override
	public String getShaderIdentifier()
	{
		return "shaders/material_precipitation";
	}

}
