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
import cs5625.deferred.materials.LightningMaterial;
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
public class CloudSystem extends ParticleSystem
{
	/* Constants */
	public static final float TIME_PER_SPAWN = .0f;
	
	/* Adjustable parameters. */
	public float gravity = 0.0f;
	public float drag = 0.0f;
	public float wind = 0.2f;
	public float ywind = 0.00f;
	public float zwind = 0.00f;
	
	private int lightn = 0;
	
	private float zlimit = -7;
	private float xlimit = -7;
	
	private int counter = 0;
	private int counter2 = 0;

	
	private Point3f maxPoint = new Point3f(-110.0f, -110.0f, -110.0f);
	
	PerlinNoiseGenerator generator = new PerlinNoiseGenerator(12, 12);
	float[][] pixels = generator.seededPerlinNoiseTexture(10, 1);

	
	/* Particle things for memory allocation shenanigans */  
	private CloudMaterial material = new CloudMaterial(xlimit);
	private CloudMaterial material2 = new CloudMaterial(xlimit);
	private CloudMaterial material3 = new CloudMaterial(xlimit);
	private CloudMaterial material5 = new CloudMaterial(xlimit);
	private CloudMaterial material6 = new CloudMaterial(xlimit);
	private CloudMaterial material7 = new CloudMaterial(xlimit);
	private CloudMaterial material8 = new CloudMaterial(xlimit);
	
	private CloudMaterial clearmaterial = new CloudMaterial(xlimit);
	private Texture2D[] textures = new Texture2D[14];
	
	private ArrayList<Cloud> toBeAdded = new ArrayList<Cloud>();
	
    private Point3f start = new Point3f(0.0f, .5f, 0.0f);
    private Point3f newStart = new Point3f(0.0f, .5f, 0.0f);
    private int number;
    private float scale = (float) (getScale() * 0.5);
    
    private int lights = 0;
    
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
	
	public void setID(int i) {
		biomeID = i;
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
	public CloudSystem(int maxParticles, Texture2D[] list, int id, Point3f s) throws IOException
	{
		// TODO initialize particle pool with shared geometry and texture data\
		//CHANGES HERE LOOK
		number = maxParticles;
		biomeID = id;
		start = new Point3f(s);
		textures = list;
		
		materialStarts();
		materialTex(list);
		materialIDs(id);

		
		for (int i = 0; i < maxParticles; i++)
		{	
			Cloud particle = new Cloud();
			particle.setSystem(this);			
			Point3f point = new Point3f(start);
			
			particle.setPosition(point);
	
			List<Geometry> geom = Particle.load("models/plane.obj", true, true);
			List<Mesh> meshes = geom.get(0).getMeshes();
			
			Quadmesh mesh = (Quadmesh) meshes.get(0);
			mesh.setMaterial(clearmaterial);
			
			try {this.addChild(particle);} catch (ScenegraphException e) {e.printStackTrace();}
			particle.addMesh(mesh);

			addChildrens(particle);

			mParticlePool.add(particle);
		}
	}
	

	/**
	 * Create, destroy, and move particles.
	 */
	public void animate(float dt)
	{	
		if(!mParticlePool.isEmpty())
		{	
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
			
			Cloud part = mParticlePool.poll();
			Mesh mesh = part.getMeshes().get(0);
			
			
				
			//different biome textures
			if (biomeID == 0 || biomeID > 1) {
				part.maxx = point.x + 0.3f*scale;
				part.minx = point.x - 0.3f*scale;
				
				part.maxz = point.z + 0.5f*scale;
				part.minz = point.z - 0.5f*scale;
				
				part.maxy = point.y + 0.2f;
				part.miny = point.y - 0.1f;
				
				if (point.y < 0.21+start.y) {
					part.maxy = point.y + 0.1f*scale;
					part.miny = point.y;
					if (Math.random() > 0.7) mesh.setMaterial(material5);
					else mesh.setMaterial(material6);
					//set ID for storms
					if (biomeID == 3) {
						material5.setID(3);
						material6.setID(3);
					}
				}
				else if (point.y > 0.6+start.y) mesh.setMaterial(material2);
				else {
					if (Math.random() > 0.3) mesh.setMaterial(material);
					else mesh.setMaterial(material8);
					}
				if (Math.random() >= 0.9) {
					mesh.setMaterial(material3);
					part.maxy = point.y + 0.5f;
					part.miny = point.y - 0.2f;
				}
			}
			
			if (biomeID == 1) {
				point.y *= .3;
				//point.x *= 1.5;
				point.z *= 1.5;
				
				part.maxx = point.x + 1.0f*scale;
				part.minx = point.x - 1.0f*scale;
				
				part.maxz = point.z + 1.0f*scale;
				part.minz = point.z - 1.0f*scale;
				
				part.maxy = 0.0f*scale;
				part.miny = -0.1f*scale;
				
				if (Math.random() > 0.5) mesh.setMaterial(material6);
				else if (Math.random() > 0.5) mesh.setMaterial(material7);
				else mesh.setMaterial(material2);
			}
			
			part.getMeshes().set(0, mesh);
			
			transformPointToWorldSpace(point);
			toBeAdded.add(part.spawn(1f, new Vector3f(point.x, point.y, point.z), randomVel));
			
			if (part.getPosition().x > maxPoint.x) {
				maxPoint = part.getPosition();
			}
			
			if  (Math.abs(maxPoint.x - part.getPosition().x) > Math.abs(xlimit)/2) {
				maxPoint = part.getPosition();
				//System.out.println("setting to position");
			}
			materialMax();
			
			if(toBeAdded.size() >= number) {
				mLivingParticles.addAll(toBeAdded);
				toBeAdded.removeAll(toBeAdded);
				
			}
			//mLivingParticles.add(part.spawn(1f, new Vector3f(point.x, point.y, point.z), randomVel));
			mTimeSinceLastSpawn = 0;
		}
		//mix up for storms. snowstorms aren't so turbulent, so not for them
		float w = wind;
		if (biomeID > 1 && biomeID < 4) {
			if (Math.random() > 0.7) {
				ywind = 0.001f;
				w *= (Math.random() * 10);
			}
			else {ywind = 0.00f; w = wind;}
		}
		for(Cloud particle: mLivingParticles)
		{	
			if (biomeID > 1 && biomeID < 4) {
				if (Math.random() > 0.7) {
					ywind = 0.02f;
				}
			} else {ywind = 0.00f;}
			
			if (biomeID == 3 && particle.getChildren().size() != 0 && Math.random() > 0.9) {
				if (particle.getChildren().get(0) instanceof Light) {
				PointLight light = (PointLight) particle.getChildren().get(0);
				light.enable();
				if (light.getChildren().size() != 0) {
					if (light.getChildren().get(0) instanceof Lightning) {
						float difference = maxPoint.x - start.x;
						if (difference > 1.0) difference -= 1.0f;
						else difference = 0.0f;
						if (difference > 1.0) {
						Lightning l = (Lightning) light.getChildren().get(0);
						if (Math.random() > 0.9) l.getMeshes().get(0).getMaterial().enable();
						}
					}
				}
				}
				
			} else if (biomeID == 3 && particle.getChildren().size() != 0) {
				if (particle.getChildren().get(0) instanceof Light) {
					PointLight light = (PointLight) particle.getChildren().get(0);
					light.disable();

				
				
				if (light.getChildren().size() != 0) {
					if (light.getChildren().get(0) instanceof Lightning) {
						Lightning l = (Lightning) light.getChildren().get(0);
						l.getMeshes().get(0).getMaterial().disable();
					}
			}
				}
			}
			else if ((biomeID == 2 || biomeID == 4) && particle.getChildren().size() != 0) {
				if (particle.getChildren().get(0) instanceof PrecipitationSystem) {
					PrecipitationSystem s = (PrecipitationSystem) particle.getChildren().get(0);
					s.updateMax(maxPoint, start);
					s.updateMin(maxPoint, xlimit);
				}
			}
			
			
			//drag will only bring the speed to zero, never increase or decrease further
			Vector3f velocity = particle.getVelocity();
			Vector3f dragForce = new Vector3f(velocity.x, velocity.y, velocity.z);
			dragForce.scale(-1 * drag);
			
			Vector3f force = new Vector3f(w, gravity+ywind, 0+zwind);
			force.add(dragForce);
			
			if (particle.getChildren().size() != 0 && biomeID == 2 || biomeID == 4) {
				if (particle.getChildren().get(0) instanceof PrecipitationSystem) {
					particle.getChildren().get(0).animate(dt);
				}
			}
			
			particle.resetForces();
			particle.accumForce(force);
			particle.animate(dt);
			
			if  (Math.abs(maxPoint.x - particle.getPosition().x) > Math.abs(xlimit)/2) {
				maxPoint = new Point3f(start);
				//System.out.println("resetting");
			}	
			
			if (particle.getPosition().x > maxPoint.x) {
				maxPoint = particle.getPosition();
			}
			
			if  (Math.abs(maxPoint.x - particle.getPosition().x) > Math.abs(xlimit)/2) {
				maxPoint = particle.getPosition();
				//System.out.println("setting to position");
			}
		}
		
		ArrayList<Cloud> toBeRemoved = new ArrayList<Cloud>();
		for(Cloud particle: mLivingParticles)
		{
			if(particle.getPosition().z <= zlimit || particle.getPosition().x <= xlimit ||
					particle.getPosition().z >= -1 * zlimit || particle.getPosition().x >= -1 * xlimit)
				toBeRemoved.add(particle);
		}
		if (toBeRemoved.size() >= number) {
			//randomize the new positions
			mLivingParticles.removeAll(toBeRemoved);
			mParticlePool.addAll(toBeRemoved); 
			start = newStart;
			Point3f s = new Point3f(-15.0f, start.y, -5.0f);
			s.z += (Math.random() * (10)) + (Math.random()*2);
			if (s.z < -4.0) s.z *= 0.5;
			if (s.z > 5.0) s.z *= 0.5;
			
			newStart = new Point3f(-4.5f, s.y, s.z);
			materialStarts();
			maxPoint = new Point3f(start);
			materialMax();
		}
//		System.out.println("difference is " + (maxPoint.x - start.x));
//		System.out.println("max is " + (maxPoint));
//		System.out.println("start is " + (start));
	}
	
	
	
	public void dissapate() {
		xlimit = maxPoint.x + 1.0f;
		material.setLimis(xlimit);
		material2.setLimis(xlimit);
		material3.setLimis(xlimit);
		material5.setLimis(xlimit);
		material6.setLimis(xlimit);
		material7.setLimis(xlimit);
		material8.setLimis(xlimit);
	}
	
	public void materialStarts() {
		material.setPosition(start);
		material2.setPosition(start);
		material3.setPosition(start);
		material5.setPosition(start);
		material6.setPosition(start);
		material7.setPosition(start);
		material8.setPosition(start);
	}
	public void materialMax() {
		material.setMax(maxPoint);
		material2.setMax(maxPoint);
		material3.setMax(maxPoint);
		material5.setMax(maxPoint);
		material6.setMax(maxPoint);
		material7.setMax(maxPoint);
		material8.setMax(maxPoint);
	}
	
	public void materialTex(Texture2D[] list) {
		clearmaterial.setTexture(list[13]);
		
		material.setTexture(list[0]);
		material2.setTexture(list[1]);
		material3.setTexture(list[2]);
		material5.setTexture(list[4]);
		material6.setTexture(list[5]);
		material7.setTexture(list[6]);
		material8.setTexture(list[7]);
	}
	public void materialIDs(int id) {
		material7.setID(id);
		material8.setID(id);
		material6.setID(id);
		material5.setID(id);
		material3.setID(id);
		material2.setID(id);
		material.setID(id);
	}
	
	public void removeChildren() {
		for (Cloud cloud : mParticlePool) {
			if (cloud.getChildren().size() != 0) {
				if(cloud.getChildren().get(0).getChildren().size() != 0) {
					for(SceneObject s : cloud.getChildren()) {
					try {
						s.removeChildren(s.getChildren());
					} catch (ScenegraphException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				}
				
				
				try {
					cloud.removeChildren(cloud.getChildren());
				} catch (ScenegraphException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	
	public void addChildrens(Cloud particle) throws IOException {
		if ((biomeID == 2 || biomeID == 4) && particle.getChildren().size() == 0) {
			PrecipitationSystem system = new PrecipitationSystem();;
			try {
				system = new PrecipitationSystem(5, textures, biomeID);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Point3f position = system.getPosition();
			position.y -= (scale);
			system.setPosition(position);
			system.setScale(0.5f*scale);
			
			try {
				particle.addChild(system);
			} catch (ScenegraphException e) {
				e.printStackTrace();
			}
			
			
		}
		if (biomeID == 3 && particle.getChildren().size() == 0 && Math.random() > 0.9 && lights < 7) {
			//System.out.println("light Created");
			lights += 1;
			PointLight light = new PointLight(false);
			light.setLinearAttenuation(0.0f);
			light.setQuadraticAttenuation(0.06f);
			
			/* Pick a random fairly-saturated color. */
			Color3f lightColor = new Color3f(0.0f, 1.0f, 1.0f);
			light.setColor(lightColor);
			
			Lightning lightning = new Lightning();
			LightningMaterial lightningMaterial = new LightningMaterial(textures[9]);
			lightn += 1;
			if (lightn > 1)  lightningMaterial = new LightningMaterial(textures[10]);
			if (lightn > 2)  {
				lightningMaterial = new LightningMaterial(textures[11]);
				lightn = 0;
			}
			lightningMaterial.disable();
			
			List<Geometry> g = Particle.load("models/plane2.obj", true, true);
			List<Mesh> m = g.get(0).getMeshes();
			
			Quadmesh me = (Quadmesh) m.get(0);
			me.setMaterial(lightningMaterial);
			lightning.addMesh(me);
			
			
			try {light.addChild(lightning);} catch (ScenegraphException e) {e.printStackTrace();}
			Point3f pos = lightning.getPosition();
			pos.y -= (2.0);
			lightning.setPosition(pos);
			
			try {
				particle.addChild(light);
			} catch (ScenegraphException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}