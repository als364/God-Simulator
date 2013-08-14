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
public class Lightning extends Particle
{	
	
	/**
	 * Default constructor. You must call `spawn()` for the particle to be ready to use.
	 */
	public Lightning()
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
	public Lightning spawn(Vector3f initialPosition)
	{
		getPosition().set(initialPosition);		
		return this;
	}
	
	
	
	/**
	 * Updates velocity and position by integrating applied forces for the current frame, and 
	 * then resets the applied force accumulator. Apply forces with the `accumForce()` function.
	 * @param dt Time step since the last frame, in seconds.
	 */
	public void animate()
	{
		//Don't do anything
	}
	
}
