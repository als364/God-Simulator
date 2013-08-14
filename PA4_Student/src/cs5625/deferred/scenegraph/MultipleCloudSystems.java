package cs5625.deferred.scenegraph;

import java.awt.Color;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.materials.CloudMaterial;
import cs5625.deferred.materials.ParticleMaterial;
import cs5625.deferred.materials.Texture;
import cs5625.deferred.materials.Texture2D;
import cs5625.deferred.materials.UnshadedMaterial;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.ScenegraphException;
import cs5625.deferred.procedural.PerlinNoiseGenerator;
import cs5625.deferred.rendering.Camera;
import cs5625.deferred.rendering.Renderer;

/**
 * ParticleSystem.java
 * 
 * The ParticleSystem class manages a collection of Particle objects with similar appearance 
 * and behavior.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-16
 */
public class MultipleCloudSystems extends ParticleSystem
{
	/* Constants */
	public static final float TIME_PER_SPAWN = .0f;
	
	/* Adjustable parameters. */
	public float gravity = 0.0f;
	public float drag = 0.0f;
	public float wind = 0.1f;
	public float ywind = 0.00f;
	public float zwind = 0.00f;
	
	private int iterator;
	
	private float zlimit = -12;
	private float xlimit = -12;
	

	
	private ArrayList<Cloud> toBeAdded = new ArrayList<Cloud>();
	
    private Point3f start = new Point3f(0.0f, .5f, 0.0f);
    private Point3f newStart = new Point3f(0.0f, .5f, 0.0f);
    private int number;
    private float scale = (float) (getScale() * 0.5);
    
    //Biome info and textures
    private int biomeID = 0; 
    //0 = cumulus - textures 1, 4, 8 - use 2 and 3 for thickening, rest to disperse
    //1 = stratus - textures 2, 5, 6, 7
    //2 = cumulonimbus
    //3 = thunderstorm
    //4 = snowstorm
    
	Random random = new Random();
	Vector3f randomVel;
	
	/** Array of dead/waiting particles which can be spawned during `animate()`. */
	LinkedList<Cloud> mLivingParticles = new LinkedList<Cloud>();
	LinkedList<Cloud> mParticlePool = new LinkedList<Cloud>();
	public float mTimeSinceLastSpawn;
	
	public int getID() {
		return biomeID;
	}
	
	public float getWind() {
		return wind;
	}
	
	public void setStart(Point3f s) {
		newStart = s;
	}
	
	/**
	 * Creates a particle system with a certain maximum number of particles.
	 * @param maxParticles The maximum number of particles which can exist at a single time. 
	 *        Particles are created and destroyed in `animate()` depending on the behavior 
	 *        of this particular system.
	 * @throws IOException 
	 */
	public MultipleCloudSystems(int maxParticles, Texture2D[] list, int id, Point3f s) throws IOException
	{
		
	}
	

	/**
	 * Create, destroy, and move particles.
	 */
	public void animate(float dt)
	{	
		
	}

	
}