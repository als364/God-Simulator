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
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.materials.ParticleMaterial;
import cs5625.deferred.materials.Texture;
import cs5625.deferred.materials.Texture2D;
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
public class LightningSystem extends ParticleSystem
{
	/* Constants */
	public static final float TIME_PER_SPAWN = 1.0f;
	
	/* Adjustable parameters. */
	
	private int counter = 0;
	private int counter2 = 0;
	
	PerlinNoiseGenerator generator = new PerlinNoiseGenerator(12, 12);
	float[][] pixels = generator.seededPerlinNoiseTexture(10, 1);

	
	/* Particle things for memory allocation shenanigans */  
	private ParticleMaterial material = new ParticleMaterial();

	private ArrayList<Particle> toBeAdded = new ArrayList<Particle>();
	
    private Point3f start = new Point3f(0.0f, .5f, 0.0f);
    private int number;
    private int partCount;
    
    //Biome info and textures
    private Texture2D[] textures;
    private int biomeID = 0; 
    //0 = cumulus - textures 1, 4, 8 - use 2 and 3 for thickening, rest to disperse
    //1 = stratus - textures 2, 5, 6, 7
    //2 = cumulonimbus
    //3 = thunderstorm
    //4 = snowstorm
    
	Random random = new Random();
	Vector3f randomVel;
	
	/** Array of dead/waiting particles which can be spawned during `animate()`. */
	LinkedList<Particle> mLivingParticles = new LinkedList<Particle>();
	LinkedList<Particle> mParticlePool = new LinkedList<Particle>();
	public float mTimeSinceLastSpawn;
	
	/**
	 * Creates a particle system with a certain maximum number of particles.
	 * @param maxParticles The maximum number of particles which can exist at a single time. 
	 *        Particles are created and destroyed in `animate()` depending on the behavior 
	 *        of this particular system.
	 * @throws IOException 
	 */
	public LightningSystem(int maxParticles, Texture2D[] list, int id) throws IOException
	{
		// TODO initialize particle pool with shared geometry and texture data\

		//CHANGES HERE LOOK
		textures = list;
		number = maxParticles;
		transformPointToWorldSpace(start);
		System.out.println("getting texture for particle");
		biomeID = id;
		material.setTexture(list[0]);
	

		
		for (int i = 0; i < maxParticles; i++)
		{	
			Particle particle = new Particle();
			Point3f point = new Point3f(-100.0f, -100.0f, -100.0f);
			
			transformPointToWorldSpace(point);
			particle.setPosition(point);
	
			List<Geometry> geom = Particle.load("models/plane.obj", true, true);
			List<Mesh> meshes = geom.get(0).getMeshes();
			
			Quadmesh mesh = (Quadmesh) meshes.get(0);
			
			try {this.addChild(particle);} catch (ScenegraphException e) {e.printStackTrace();}
			particle.addMesh(mesh);
			mParticlePool.add(particle);
		}

	}
	
	public int max() {
		return number;
	}
	
	public int count() {
		return partCount;
	}

	/**
	 * Create, destroy, and move particles.
	 */
	public void animate(float dt)
	{
		// TODO spawn new particles from pool if enough time has elapsed
		// TODO animate living particles
		// TODO reap dead particles and return them to pool
		if(!mParticlePool.isEmpty())
		{	
			//set textures LOOK
			partCount++;
			float speed = 20;
			if (biomeID <= 1) speed = 20;
			if (biomeID >= 2) speed = 40;
			
			
			//randomly decide if the x or z goes in a positive or negative direction
			float negater = -1;
			if (Math.random() > 0.5) negater = 1;
			float negater2 = -1;
			if (Math.random() > 0.5) negater2 = 1;
			
			//Randomize the velocities
			randomVel = new Vector3f(negater * random.nextFloat()/(1*speed), (float) Math.random()/(2*speed), negater2 * random.nextFloat()/(1*speed));
			if (randomVel.x > 0.1) randomVel.x = randomVel.x/10;
			if (randomVel.y > 0.1) randomVel.y = randomVel.y/10;
			if (randomVel.z > 0.1) randomVel.z = randomVel.z/10;
			
			if(randomVel.dot(new Vector3f(0f, 1f, 0f)) <= 0)
			{
				randomVel.scale(-1);
				//System.out.println("Rescaled Vector: [" + randomVel.x + ", " + randomVel.y + ", " + randomVel.z + "]");
			}
			//System.out.println("Final Vector: [" + randomVel.x + ", " + randomVel.y + ", " + randomVel.z + "]");
			Point3f point = new Point3f(start);
			point.x = (float) (pixels[counter][counter2]/1.5);
			counter2++;
			point.y = (float) (pixels[counter][counter2]/1.5);
			counter2++;
			point.z = (float) (pixels[counter][counter2]/1.5);
			counter2++;
			
			point.add(start);
			
			if (counter >= 11) counter = 0;
			if (counter2 >= 11) {
				counter++;
				counter2 = 0;
			}
			
			Particle part = mParticlePool.poll();
			Mesh mesh = part.getMeshes().get(0);
			
			
				
			
			
			//CHANGE THIS - add as a batch
			transformPointToWorldSpace(point);
			toBeAdded.add(part.spawn(1f, new Vector3f(point.x, point.y, point.z), randomVel));
			
			if(toBeAdded.size() >= number) {
				mLivingParticles.addAll(toBeAdded);
				toBeAdded.removeAll(toBeAdded);
			}
			//mLivingParticles.add(part.spawn(1f, new Vector3f(point.x, point.y, point.z), randomVel));
			mTimeSinceLastSpawn = 0;
		}
		/*for(Lightning lightning: mLivingParticles)
		{	

			lightning.animate();
		}
		
		ArrayList<Particle> toBeRemoved = new ArrayList<Particle>();
		for(Particle particle: mLivingParticles)
		{
			//System.out.println("y-value: " + particle.getPosition().y);
			if(particle.getPosition().z <= particle.getSystem().zlimit || particle.getPosition().x <= xlimit ||
					particle.getPosition().z >= -1 * zlimit || particle.getPosition().x >= -1 * xlimit)
			{
				//System.out.println("reaping particle");
				toBeRemoved.add(particle);
			}
		}
		if (toBeRemoved.size() >= number) {
			mLivingParticles.removeAll(toBeRemoved);
			mParticlePool.addAll(toBeRemoved);
			partCount = 0;
		}*/
	}
	
	/**
	 * Points all particles in this system towards the camera.
	 */
	public void billboard(Camera camera)
	{
		for (SceneObject obj : getChildren())
		{
			if (obj instanceof Particle)
			{
				((Particle)obj).billboard(camera);
			}
		}
	}
}