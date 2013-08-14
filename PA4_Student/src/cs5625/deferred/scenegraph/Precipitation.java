package cs5625.deferred.scenegraph;

import java.nio.FloatBuffer;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import cs5625.deferred.rendering.Camera;

/**
 * Particle.java
 * 
 * Particle objects are owned by a ParticleSystem and handle animating a single particle.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-17
 */
public class Precipitation extends Geometry
{
	/* Particle state. */
	private Vector3f mForcesThisFrame = new Vector3f();
	private Vector3f mVelocity = new Vector3f();
	private float mMass = 0.0f;
	private float mAge = 0.0f;
	
	/**
	 * Default constructor. You must call `spawn()` for the particle to be ready to use.
	 */
	public Precipitation()
	{
		/* nothing */
	}

	/**
	 * Spawns or respawns this particle with the passed attributes.
	 * 
	 * @param mass The mass of the new particle.
	 * @param initialPosition The initial position of the particle.
	 * @param initialVelocity The initial velocity of the particle.
	 * 
	 * @return Returns self to facilitate patterns like `myParticleSystem.addChild(myParticle.spawn(...))`.
	 */
	public Precipitation spawn(float mass, Vector3f initialPosition, Vector3f initialVelocity)
	{
		mMass = mass;
		getPosition().set(initialPosition);
		mVelocity.set(initialVelocity);
		mAge = 0.0f;
		
		return this;
	}
	
	/**
	 * Returns the time since this particle spawned, in seconds.
	 */
	public float getAge()
	{
		return mAge;
	}
	
	/**
	 * Returns the current velocity of this particle.
	 */
	public Vector3f getVelocity()
	{
		return mVelocity;
	}
	
	/**
	 * Resets accumuated forces for the next animation frame. 
	 */
	public void resetForces()
	{
		mForcesThisFrame.set(0.0f, 0.0f, 0.0f);
	}

	/**
	 * Adds a force to this particle for the current frame.
	 */
	public void accumForce(Vector3f force)
	{
		mForcesThisFrame.add(force);
	}
	
	/**
	 * Updates velocity and position by integrating applied forces for the current frame, and 
	 * then resets the applied force accumulator. Apply forces with the `accumForce()` function.
	 * @param dt Time step since the last frame, in seconds.
	 */
	public void animate(float dt)
	{	//System.out.println("precipitation animating");
		// TODO integrate to compute new velocity and position
		Point3f velocAdd = new Point3f(mVelocity);
		velocAdd.scale(dt);
		Point3f newPosition = getPosition();
		newPosition.add(velocAdd);
		getPosition().set(newPosition);
		
		Vector3f force = mForcesThisFrame;
		
		force.scale(1/mMass);
		force.scale(dt);
		Vector3f newVelocity = mVelocity;
		newVelocity.add(force);
		mVelocity = newVelocity; 
		
		resetForces();


	}
	
}
