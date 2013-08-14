package cs5625.deferred.materials;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.Timer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.procedural.PerlinNoiseGenerator;
import cs5625.deferred.rendering.ShaderProgram;

public class OceanMaterial extends Material implements ActionListener
{
	private Color3f mDiffuseColor = new Color3f(130f/255f, 215f/255f, 210f/255f);
	private Texture2D mBumpMapTexture;
	Timer timer = new Timer(1000/60, this);
	public long time = 0;

	int texWidth;
	PerlinNoiseGenerator generator;

	private int mDiffuseUniformLocation = -1;
	private int mTimeUniformLocation = -1;

	public OceanMaterial(Color3f baseColor, int texWidth)
	{
		this.texWidth = texWidth;
		mDiffuseColor = baseColor;
		generator = new PerlinNoiseGenerator(texWidth, texWidth);

		BufferedImage bumpMap = new BufferedImage(texWidth, texWidth, BufferedImage.TYPE_INT_RGB);
		float[][] heightChannel = generator.seededPerlinNoiseTexture(-1, 3);
		for(int x = 0; x < texWidth; x++)
		{
			for(int y = 0; y < texWidth; y++)
			{
				Vector3f normal = getNormalAtPoint(x, y, heightChannel);
//				System.out.println(normal);
//				Color color = new Color(heightChannel[x][y], heightChannel[x][y], heightChannel[x][y]);
//				System.out.println(color);
				Color color = new Color(normal.x, normal.y, normal.z);
				bumpMap.setRGB(x, y, color.getRGB());
			}
		}

		File bumpFile = new File("src/textures/ocean.png");
		
		try 
		{
			ImageIO.write(bumpMap, "png", bumpFile);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			mBumpMapTexture = Texture2D.load(GLU.getCurrentGL().getGL2(), "textures/ocean.png");
		} catch (GLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		timer.start();
	}

	@Override
	public void bind(GL2 gl) throws OpenGLException {
		getShaderProgram().bind(gl);
		gl.glUniform3f(mDiffuseUniformLocation, mDiffuseColor.x, mDiffuseColor.y, mDiffuseColor.z);
		gl.glUniform1f(mTimeUniformLocation, time);
		
//		System.out.println("ping");
//		gl.glEnableVertexAttribArray(1);
		
		if (mBumpMapTexture != null)
		{
			mBumpMapTexture.bind(gl, 0);
		}
	}

	public void initializeShader(GL2 gl, ShaderProgram shader)
	{
		mDiffuseUniformLocation = shader.getUniformLocation(gl, "DiffuseColor");
		mTimeUniformLocation = shader.getUniformLocation(gl, "Time");
	
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "BumpMap"), 0);
//		gl.glBindAttribLocation(shader.getHandle(), 1, "noiseval");
//		gl.glVertexAttribPointer(1, 1, gl.GL_FLOAT, false, 0, (Buffer)buffer);
		shader.unbind(gl);
	}

	@Override
	public void unbind(GL2 gl) {
		getShaderProgram().unbind(gl);
		
//		gl.glDisableVertexAttribArray(1);
		
		if (mBumpMapTexture != null)
		{
			mBumpMapTexture.unbind(gl);
		}
	}

	@Override
	public String getShaderIdentifier() {
		return "shaders/material_ocean";
	}

	private Vector3f getNormalAtPoint(int x, int y, float[][] heightmap)
	{
		Vector3f vert = new Vector3f();
		Vector3f norm = new Vector3f();
		Vector3f line1 = new Vector3f();
		Vector3f line2 = new Vector3f();
		Vector3f line3 = new Vector3f();
		Vector3f line4 = new Vector3f();
		Vector3f tri1 = new Vector3f();
		Vector3f tri2 = new Vector3f();
		
		float xPos = (2 * x) - texWidth;
		float yPos = heightmap[x][y];
		float zPos = (2 * y) - texWidth;

		vert.set(xPos, yPos, zPos);

		if(x == 0 && y == 0)
		{
			norm = new Vector3f(vert);
			norm.sub(new Vector3f(x+2, heightmap[x+1][y], y));
			Vector3f temp = new Vector3f(vert);
			temp.sub(new Vector3f(x, heightmap[x][y+1], y+2));
			norm.cross(norm, temp);
		}
		else if(x == texWidth && y == 0)
		{
			norm = new Vector3f(vert);
			norm.sub(new Vector3f(x-2, heightmap[x-1][y], y));
			Vector3f temp = new Vector3f(vert);
			temp.sub(new Vector3f(x, heightmap[x][y+1], y+2));
			norm.cross(norm, temp);
		}
		else if(x == 0 && y == texWidth)
		{
			norm = new Vector3f(vert);
			norm.sub(new Vector3f(x, heightmap[x][y-1], y-2));
			Vector3f temp = new Vector3f(vert);
			temp.sub(new Vector3f(x+2, heightmap[x+1][y], y));
			norm.cross(norm, temp);
		}
		else if(x == texWidth && y == texWidth)
		{
			norm = new Vector3f(vert);
			norm.sub(new Vector3f(x-2, heightmap[x-1][y], y));
			Vector3f temp = new Vector3f(vert);
			temp.sub(new Vector3f(x, heightmap[x][y-1], y-2));
			norm.cross(norm, temp);
		}
		else if(x == 0)
		{
			line1 = new Vector3f(vert);
			line1.sub(new Vector3f(x+2, heightmap[x+1][y], y));
			line2 = new Vector3f(vert);
			line2.sub(new Vector3f(x, heightmap[x][y+1], y+2));
			line3 = new Vector3f(vert);
			line3.sub(new Vector3f(x, heightmap[x][y-1], y-2));
			tri1 = new Vector3f();
			tri1.cross(line1, line2);
			tri2 = new Vector3f();
			tri2.cross(line1, line3);
			norm = new Vector3f();
			norm.add(tri1, tri2);
		}
		else if(y == 0)
		{
			line1 = new Vector3f(vert);
			line1.sub(new Vector3f(x, heightmap[x][y+1], y+2));
			line2 = new Vector3f(vert);
			line2.sub(new Vector3f(x+2, heightmap[x+1][y], y));
			line3 = new Vector3f(vert);
			line3.sub(new Vector3f(x-2, heightmap[x-1][y], y));
			tri1 = new Vector3f();
			tri1.cross(line1, line2);
			tri2 = new Vector3f();
			tri2.cross(line1, line3);
			norm = new Vector3f();
			norm.add(tri1, tri2);
		}
		else if(x == texWidth)
		{
			line1 = new Vector3f(vert);
			line1.sub(new Vector3f(x-2, heightmap[x-1][y], y));
			line2 = new Vector3f(vert);
			line2.sub(new Vector3f(x, heightmap[x][y+1], y+2));
			line3 = new Vector3f(vert);
			line3.sub(new Vector3f(x, heightmap[x][y-1], y-2));
			tri1 = new Vector3f();
			tri1.cross(line1, line2);
			tri2 = new Vector3f();
			tri2.cross(line1, line3);
			norm = new Vector3f();
			norm.add(tri1, tri2);
		}
		else if(y == texWidth)
		{
			line1 = new Vector3f(vert);
			line1.sub(new Vector3f(x, heightmap[x][y-1], y+2));
			line2 = new Vector3f(vert);
			line2.sub(new Vector3f(x+2, heightmap[x+1][y], y));
			line3 = new Vector3f(vert);
			line3.sub(new Vector3f(x-2, heightmap[x-1][y], y));
			tri1 = new Vector3f();
			tri1.cross(line1, line2);
			tri2 = new Vector3f();
			tri2.cross(line1, line3);
			norm = new Vector3f();
			norm.add(tri1, tri2);
		}
		else
		{
			line1 = new Vector3f(vert);
			line1.sub(new Vector3f(vert.x, heightmap[x][y+1], vert.z+2));
			line2 = new Vector3f(vert);
			line2.sub(new Vector3f(vert.x+2, heightmap[x+1][y], vert.z));
			line3 = new Vector3f(vert);
			line3.sub(new Vector3f(vert.x-2, heightmap[x-1][y], vert.z));
			line4 = new Vector3f(vert);
			line4.sub(new Vector3f(vert.x, heightmap[x][y-1], vert.z-2));
			tri1 = new Vector3f();
			tri1.cross(line3, line1);
			tri2 = new Vector3f();
			tri2.cross(line2, line4);
			norm = new Vector3f();
			norm.add(tri1, tri2);
		}
		norm.normalize();
//		System.out.println("Norm: " + norm);
		if(norm.x < 0)
		{
			norm.x = 1+norm.x;
		}
		if(norm.y < 0)
		{
			norm.y = 1+norm.y;
		}
		if(norm.z < 0)
		{
			norm.z = 1+norm.z;
		}
		return norm;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// TODO Auto-generated method stub
		if (event.getSource() == timer)
		{
			long now = System.currentTimeMillis();
			time = now;
//			System.out.println("now: " + now);
		}
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		System.out.println(time);
		this.time = time;
	}
}
