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
import cs5625.deferred.rendering.Camera;

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
public class ParticleSystem extends SceneObject
{
	/* Constants */
	public static final float TIME_PER_SPAWN = .005f;
	
	/* Adjustable parameters. */
	public float gravity = -9.8f;
	public float drag = 0.1f;
	public float wind = .5f;
	
	/* Particle things for memory allocation shenanigans */
	private Quadmesh quadmesh = new Quadmesh();
	private ParticleMaterial material = new ParticleMaterial(new Color4f(1f, 1f, 0, 1f));
	private float[] verts = {0f, 0f, 0f, .1f, 0f, 0f, .1f, .1f, 0f, 0f, .1f, 0f};
    private int[] polys = {0, 1, 2, 3};
    
    private Point3f start = new Point3f(0.0f, 1f, 0.0f);
    
	FloatBuffer vertices = Buffers.newDirectFloatBuffer(verts);
	IntBuffer polygons = Buffers.newDirectIntBuffer(polys);

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
	 */
	
	public ParticleSystem() {
		
	}
	public ParticleSystem(int maxParticles, Texture2D[] list) throws IOException
	{
		material.setTexture(list[0]);
		System.out.println("texture is " + list[0].getID());
		
		for (int i = 0; i < maxParticles; i++)
		{	
			Particle particle = new Particle();
			Point3f point = new Point3f(start);
			
			transformPointToWorldSpace(point);
			particle.setPosition(point);
	
			List<Geometry> geom = Particle.load("models/plane.obj", true, true);
			List<Mesh> meshes = geom.get(0).getMeshes();
			
			Quadmesh mesh = (Quadmesh) meshes.get(0);
			mesh.setMaterial(material);
			
			try {this.addChild(particle);} catch (ScenegraphException e) {e.printStackTrace();}
			particle.addMesh(mesh);
			mParticlePool.add(particle);
		}
		/*quadmesh = new Quadmesh();
		quadmesh.setMaterial(material);
		quadmesh.setVertexData(vertices);
		//quadmesh.setEdgeData(edges);
		quadmesh.setPolygonData(polygons);*/
	}

	/**
	 * Create, destroy, and move particles.
	 */
	public void animate(float dt)
	{
		// TODO spawn new particles from pool if enough time has elapsed
		// TODO animate living particles
		// TODO reap dead particles and return them to pool
		if(mTimeSinceLastSpawn >= TIME_PER_SPAWN && !mParticlePool.isEmpty())
		{	//Color4f color = new Color4f((float)Math.random(), (float)Math.random(), (float)Math.random(), 1f);
			//material.setColor(color);
			
			//randomly decide if the x or z goes in a positive or negative direction
			float negater = -1;
			if (Math.random() > 0.5) negater = 1;
			float negater2 = -1;
			if (Math.random() > 0.5) negater2 = 1;
			
			//Randomize the velocities, but we want a positive, slightly larger y-velocity
			randomVel = new Vector3f(negater * random.nextFloat(), 2 * random.nextFloat(), negater2 * random.nextFloat());
			//System.out.println("Generated Vector: [" + randomVel.x + ", " + randomVel.y + ", " + randomVel.z + "]");
			randomVel.scale(2 * random.nextFloat());
			
			//clip the x and z velocities
			if (randomVel.x > 0.5) randomVel.x = 0.5f;
			if (randomVel.z > 0.5) randomVel.z = 0.5f;
			if (randomVel.x < -0.5) randomVel.x = -0.5f;
			if (randomVel.z < -0.5) randomVel.z = -0.5f;
			
			// System.out.println("x vel = " + randomVel.x + "y vel = " + randomVel.y + "z vel = " + randomVel.z);
			//System.out.println("Scaled Vector: [" + randomVel.x + ", " + randomVel.y + ", " + randomVel.z + "]");
			if(randomVel.dot(new Vector3f(0f, 1f, 0f)) <= 0)
			{
				randomVel.scale(-1);
				//System.out.println("Rescaled Vector: [" + randomVel.x + ", " + randomVel.y + ", " + randomVel.z + "]");
			}
			//System.out.println("Final Vector: [" + randomVel.x + ", " + randomVel.y + ", " + randomVel.z + "]");
			Point3f point = new Point3f(start);
			transformPointToWorldSpace(point);
			
			mLivingParticles.add(mParticlePool.poll().spawn(1f, new Vector3f(point.x, point.y, point.z), randomVel));
			mTimeSinceLastSpawn = 0;
		}
		for(Particle particle: mLivingParticles)
		{	
			float w = wind;
			float d = drag;
			float g = gravity;
			//drag will only bring the speed to zero, never increase or decrease further
			//VELOCITY ISN'T SPEED. That's why it's a vector. Fixed.
			Vector3f velocity = particle.getVelocity();
			Vector3f dragForce = new Vector3f(velocity.x, velocity.y, velocity.z);
			dragForce.scale(-1 * d);
			
			Vector3f force = new Vector3f(w, g, 0);
			force.add(dragForce);
			
			particle.resetForces();
			particle.accumForce(force);
			particle.animate(dt);
		}
		ArrayList<Particle> toBeRemoved = new ArrayList<Particle>();
		for(Particle particle: mLivingParticles)
		{
			//System.out.println("y-value: " + particle.getPosition().y);
			if(particle.getPosition().y <= -1)
			{
				//System.out.println("reaping particle");
				mParticlePool.add(particle);
				toBeRemoved.add(particle);
			}
		}
		mLivingParticles.removeAll(toBeRemoved);
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