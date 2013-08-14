package cs5625.deferred.materials;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Color3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.procedural.PerlinNoiseGenerator;
import cs5625.deferred.rendering.ShaderProgram;

public class ProceduralTerrainMaterial extends Material
{
	/* Terrain properties */
	private Color mGrassGradientBegin;
	private Color mGrassGradientEnd;
	private Color mRockGradientBegin;
	private Color mRockGradientEnd;
	private Color mSnowGradientBegin;
	private Color mSnowGradientEnd;
	private Color mSandGradientBegin;
	private Color mSandGradientEnd;
	private Texture2D mGrassTexture;
	private Texture2D mRockTexture;
	private Texture2D mSnowTexture;
	private Texture2D mSandTexture;
	int texWidth;
	PerlinNoiseGenerator generator;
	
	private int mTexWidthUniformLocation = -1;
	
	
	/* Uniform locations for the shader. */
//	private int mGrassUniformLocation = -1;
//	private int mRockUniformLocation = -1;
//	private int mSnowUniformLocation = -1;

	public ProceduralTerrainMaterial(Color grassStart, Color grassEnd, Color rockStart, Color rockEnd, Color snowStart, Color snowEnd, Color sandStart, Color sandEnd, int texWidth, long seed)
	{
		mGrassGradientBegin = grassStart;
		mGrassGradientEnd = grassEnd;
		mRockGradientBegin = rockStart;
		mRockGradientEnd = rockEnd;
		mSnowGradientBegin = snowStart;
		mSnowGradientEnd = snowEnd;
		mSandGradientBegin = sandStart;
		mSandGradientEnd = sandEnd;
		this.texWidth = texWidth;
		generator = new PerlinNoiseGenerator(texWidth, texWidth);
		
		BufferedImage grassImg = new BufferedImage(texWidth, texWidth, BufferedImage.TYPE_INT_RGB);
		float[][] grassChannel = generator.seededPerlinNoiseTexture(-1, 5);
		for(int x = 0; x < texWidth; x++)
		{
			for(int y = 0; y < texWidth; y++)
			{
				float t = grassChannel[x][y];
				float u = 1 - t;
				Color color = new Color((int)(mGrassGradientBegin.getRed() * u + mGrassGradientEnd.getRed() * t),
								        (int)(mGrassGradientBegin.getGreen() * u + mGrassGradientEnd.getGreen() * t),
								        (int)(mGrassGradientBegin.getBlue() * u +mGrassGradientEnd.getBlue() * t));
				grassImg.setRGB(x, y, color.getRGB());
			}
		}
		
		BufferedImage rockImg = new BufferedImage(texWidth, texWidth, BufferedImage.TYPE_INT_RGB);
		float[][] rockChannel = generator.seededPerlinNoiseTexture(-1, 5);
		for(int x = 0; x < texWidth; x++)
		{
			for(int y = 0; y < texWidth; y++)
			{
				float t = rockChannel[x][y];
				float u = 1 - t;
				Color color = new Color((int)(mRockGradientBegin.getRed() * u + mRockGradientEnd.getRed() * t),
								        (int)(mRockGradientBegin.getGreen() * u + mRockGradientEnd.getGreen() * t),
								        (int)(mRockGradientBegin.getBlue() * u +mRockGradientEnd.getBlue() * t));
				rockImg.setRGB(x, y, color.getRGB());
			}
		}
		
		BufferedImage snowImg = new BufferedImage(texWidth, texWidth, BufferedImage.TYPE_INT_RGB);
		float[][] snowChannel = generator.seededPerlinNoiseTexture(-1, 5);
		for(int x = 0; x < texWidth; x++)
		{
			for(int y = 0; y < texWidth; y++)
			{
				float t = snowChannel[x][y];
				float u = 1 - t;
				Color color = new Color((int)(mSnowGradientBegin.getRed() * u + mSnowGradientEnd.getRed() * t),
								        (int)(mSnowGradientBegin.getGreen() * u + mSnowGradientEnd.getGreen() * t),
								        (int)(mSnowGradientBegin.getBlue() * u +mSnowGradientEnd.getBlue() * t));
				snowImg.setRGB(x, y, color.getRGB());
			}
		}
		
		BufferedImage sandImg = new BufferedImage(texWidth, texWidth, BufferedImage.TYPE_INT_RGB);
		float[][] sandChannel = generator.seededPerlinNoiseTexture(-1, 5);
		for(int x = 0; x < texWidth; x++)
		{
			for(int y = 0; y < texWidth; y++)
			{
				float t = sandChannel[x][y];
				float u = 1 - t;
				Color color = new Color((int)(mSandGradientBegin.getRed() * u + mSandGradientEnd.getRed() * t),
								        (int)(mSandGradientBegin.getGreen() * u + mSandGradientEnd.getGreen() * t),
								        (int)(mSandGradientBegin.getBlue() * u +mSandGradientEnd.getBlue() * t));
				sandImg.setRGB(x, y, color.getRGB());
			}
		}
		
		File grassFile = new File("src/textures/grass.png");
		File rockFile = new File("src/textures/rock.png");
		File snowFile = new File("src/textures/snow.png");
		File sandFile = new File("src/textures/sand.png");
		
		try 
		{
			ImageIO.write(grassImg, "png", grassFile);
			ImageIO.write(rockImg, "png", rockFile);
			ImageIO.write(snowImg, "png", snowFile);
			ImageIO.write(sandImg, "png", sandFile);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			System.exit(-1);
		}
		
		try
		{
			mGrassTexture = Texture2D.load(GLU.getCurrentGL().getGL2(), "textures/grass.png");
			mRockTexture = Texture2D.load(GLU.getCurrentGL().getGL2(), "textures/rock.png");
			mSnowTexture = Texture2D.load(GLU.getCurrentGL().getGL2(), "textures/snow.png");
			mSandTexture = Texture2D.load(GLU.getCurrentGL().getGL2(), "textures/sand.png");
		}
		catch (OpenGLException e){
			e.printStackTrace();
			System.exit(-1);
		} catch (GLException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void bind(GL2 gl) throws OpenGLException {
		/* Bind shader and textures.*/
		getShaderProgram().bind(gl);
		gl.glUniform1i(mTexWidthUniformLocation, texWidth);
		if (mGrassTexture != null)
		{
			mGrassTexture.bind(gl, 0);
		}
		if (mRockTexture != null)
		{
			mRockTexture.bind(gl, 1);
		}
		if(mRockTexture != null)
		{
			mSnowTexture.bind(gl, 2);
		}
		if(mSandTexture != null)
		{
			mSandTexture.bind(gl, 3);
		}
	}

	@Override
	protected void initializeShader(GL2 gl, ShaderProgram shader)
	{
//		System.out.println("ping");
		mTexWidthUniformLocation = shader.getUniformLocation(gl, "TexWidth");
		
		/* These are only set once, so set them here. */
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "DiffuseTexture"), 0);
		gl.glUniform1i(shader.getUniformLocation(gl, "RockTexture"), 1);
		gl.glUniform1i(shader.getUniformLocation(gl, "SnowTexture"), 2);
		gl.glUniform1i(shader.getUniformLocation(gl, "SandTexture"), 3);
		shader.unbind(gl);
	}
	
	@Override
	public void unbind(GL2 gl) {
		/* Unbind everything bound in bind(). */
		getShaderProgram().unbind(gl);
		if (mGrassTexture != null)
		{
			mGrassTexture.unbind(gl);
		}
		if (mRockTexture != null)
		{
			mRockTexture.unbind(gl);
		}
		if(mRockTexture != null)
		{
			mSnowTexture.unbind(gl);
		}
		if(mSandTexture != null)
		{
			mSandTexture.unbind(gl);
		}
	}
	
	@Override
	public String getShaderIdentifier()
	{
		return "shaders/material_terrain";
	}
	
	private Color contrastThreshold(Color color)
	{
		int red;
		int green;
		int blue;
		
		if(color.getRed() > 150)
		{
			red = 255;
		}
		else if(color.getRed() > 75)
		{
			red = 150;
		}
		else
		{
			red = 0;
		}
		
		if(color.getGreen() > 150)
		{
			green = 255;
		}
		else if(color.getGreen() > 75)
		{
			green = 150;
		}
		else
		{
			green = 0;
		}
		
		if(color.getBlue() > 150)
		{
			blue = 255;
		}
		else if(color.getBlue() > 75)
		{
			blue = 150;
		}
		else
		{
			blue = 0;
		}
		
		return new Color(red, green, blue);
	}
}
