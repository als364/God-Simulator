package cs5625.deferred.apps;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.Timer;
import javax.vecmath.Point3f;

import cs5625.deferred.scenegraph.CloudSystem;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.ParticleSystem;
import cs5625.deferred.scenegraph.PointLight;
import cs5625.deferred.scenegraph.Precipitation;
import cs5625.deferred.scenegraph.PrecipitationSystem;
import cs5625.deferred.scenegraph.SceneObject;

/**
 * ParticleDemoSceneController.java
 * 
 * The particle demo scene controller creates a scene with a ground plane and particle system, 
 * and allows the user to orbit the camera and preview the renderer's gbuffer.
 * 
 * Drag the mouse to orbit the camera, and scroll to zoom. Numbers 1-9 preview individual gbuffer 
 * textures, and 0 views the shaded result.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-15
 */
public class PrecipitationSystemSceneController extends DefaultSceneController implements ActionListener
{
	private PrecipitationSystem mParticleSystem;

	/* For the update loop. */
	private long mPreviousFrameTime = 0;
	private Timer mUpdateTimer = new Timer(1000/60, this);
	private int ID = 2;
	
	private int numClouds = 0;
	
	private SceneObject mLightCloud;
	private Point3f start = new Point3f(0.0f, 0.5f, 0.0f);
	
		
	@Override
	public void initializeScene()
	{
		try
		{
			/* Move the whole scene down a bit, so orbits better. */
			mSceneRoot.getPosition().y = -3.0f;

			/* Load ground plane. */			
			/* Create a particle system. */
			
			
			//added texture to particlesystem LOOK
			/*for (int i = 0; i < numClouds; i++){
				mParticleSystem = new ParticleSystem(40, mRenderer.textures, ID);
				mParticleSystem.setStart(new Point3f((float)i, 0.5f, 0.0f));
				mParticleSystem.setScale(1.0f);
				mParticleSystem.getPosition().y = 0.1f * mParticleSystem.getScale();
				mSceneRoot.addChild(mParticleSystem); }*/
			
			mParticleSystem = new PrecipitationSystem(20, mRenderer.textures, ID);
			mParticleSystem.setScale(1.0f);
			mParticleSystem.getPosition().y = 0.1f * mParticleSystem.getScale();
			mSceneRoot.addChild(mParticleSystem);
			
			/* Add an unattenuated point light to provide overall illumination. */
			PointLight light = new PointLight();
			
			light.setConstantAttenuation(1.0f);
			light.setLinearAttenuation(0.0f);
			light.setQuadraticAttenuation(0.0f);
			
			light.setPosition(new Point3f(50.0f, 180.0f, 100.0f));
			mSceneRoot.addChild(light);
			
			/*mLightCloud = new SceneObject();
			mLightCloud.getPosition().y = 2.0f;
			mSceneRoot.addChild(mLightCloud);
			
			// Go ahead and create the lights. 
			for (int i = 0; i < 4; ++i)
			{
				PointLight light = new PointLight();
				Color3f color = new Color3f();
				if (i == 0) color = new Color3f(1.0f, 0.0f, 0.0f);
				if (i == 1) color = new Color3f(0.0f, 1.0f, 0.0f);
				if (i == 2) color = new Color3f(0.0f, 0.0f, 1.0f);
				if (i == 3) color = new Color3f(0.0f, 1.0f, 1.0f);
				// Pick a random fairly-saturated color. 
				Color3f lightColor = new Color3f(color);
				light.setColor(lightColor);
				
				// Pick a random position for the light.  
				Point3f position = new Point3f(0.0f, 0.0f, 0.0f);
				if (i == 0) position = new Point3f(1.0f, 1.0f, 0.0f);
				if (i == 1) position = new Point3f(-1.0f, 1.0f, 0.0f);
				if (i == 2) position = new Point3f(0.0f, 1.0f, 1.0f);
				if (i == 3) position = new Point3f(0.0f, 1.0f, -1.0f);
				
				light.setPosition(position);
				
				// Add a sphere as a child of the light (so we can see where it is), and set the sphere to the same color as the light.
				Geometry sphere = Geometry.load("models/lowpolysphere.obj", false, false).get(0);
				sphere.setScale(0.1f);
				sphere.getMeshes().get(0).setMaterial(new UnshadedMaterial(lightColor));
				light.addChild(sphere);
				
				// Add this new light to the cloud. 
				mLightCloud.addChild(light);
		}
		}*/
		}
		catch (Exception err)
		{
			/* If anything goes wrong, just die. */
			err.printStackTrace();
			System.exit(-1);
		}
		
		/* Initialize camera position. */
		updateCamera();
	}
	
	@Override
	public void keyTyped(KeyEvent key)
	{
		char c = key.getKeyChar();

		if (c == ' ')
		{
			if (mUpdateTimer.isRunning())
			{
				mUpdateTimer.stop();
			}
			else
			{
				mPreviousFrameTime = System.currentTimeMillis();
				mUpdateTimer.start();
			}
		}
		else if (c == 'f')
		{
			mRenderer.setRenderWireframes(!mRenderer.getRenderWireframes());
		}
		else if (c == 'w')
		{
			mParticleSystem.wind += 1.0f;
			System.out.println("Wind strength = " + mParticleSystem.wind);
		}
		else if (c == 'W')
		{
			mParticleSystem.wind -= 1.0f;
			System.out.println("Wind strength = " + mParticleSystem.wind);
		}
		else if (c == 'd')
		{
			mParticleSystem.drag += 1.0f;
			System.out.println("Drag coefficient = " + mParticleSystem.drag);
		}
		else if (c == 'D')
		{
			mParticleSystem.drag -= 1.0f;
			System.out.println("Drag coefficient = " + mParticleSystem.drag);
		}
		else if (c == 'g')
		{
			mParticleSystem.gravity += 1.0f;
			System.out.println("Gravity = " + mParticleSystem.gravity);
		}
		else if (c == 'G')
		{
			mParticleSystem.gravity -= 1.0f;
			System.out.println("Gravity = " + mParticleSystem.gravity);
		}
		else
		{
			super.keyTyped(key);
		}
	}

	/* Handles update timer events. */
	
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == mUpdateTimer)
		{
			long now = System.currentTimeMillis();
			long dt = now - mPreviousFrameTime;
			mPreviousFrameTime = now;
			
			mParticleSystem.mTimeSinceLastSpawn += dt;
			
			nextFrame(dt / 1000.0f);
		}
	}
}
