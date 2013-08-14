package cs5625.deferred.apps;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.media.opengl.GLException;
import javax.swing.Timer;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.quadtree.Bintree;
import cs5625.deferred.materials.OceanMaterial;
import cs5625.deferred.materials.ProceduralTerrainMaterial;
import cs5625.deferred.misc.ScenegraphException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.scenegraph.Cloud;
import cs5625.deferred.scenegraph.CloudSystem;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.PointLight;
import cs5625.deferred.scenegraph.Quadmesh;
import cs5625.deferred.scenegraph.SceneObject;
import cs5625.deferred.scenegraph.Trimesh;
import cs5625.deferred.procedural.PerlinNoiseGenerator;

public class SimulatorSceneController extends SceneController implements ActionListener
{
	/* Keeps track of camera's orbit position. Latitude and longitude are in degrees. */
	private float mCameraLongitude = 50.0f, mCameraLatitude = -40.0f;
	private float mCameraRadius = 300.0f;

	//Particle system shenanigans
	private CloudSystem mParticleSystem;
	private long mPreviousFrameTime = 0;
	private Timer mUpdateTimer = new Timer(1000/60, this);
	private int ID = 3;
	private ArrayList<CloudSystem> clouds = new ArrayList<CloudSystem>();
	
	private float scale = 40.0f;
	private int numClouds = 5;
	private float cloudHeight = 0.5f;
	private Point3f start = new Point3f(-15.0f, 2.0f, -5.0f);
	//private Point3f origin = new Point3f(-10.5f, 1.5f, -10.5f);
	
	Quadmesh oceanMesh;
	Bintree leftBintree;
	Bintree rightBintree;

	/* Used to calculate mouse deltas to orbit the camera in mouseDragged(). */ 
	private Point mLastMouseDrag;
	
	int worldWidth = 128;
	int texWidth = 256;
	int worldHeight = 40;
	int oceanWidth = 2 * worldWidth;
	int oceanHeight = 5;
	int numVertices = (worldWidth + 1) * (worldWidth + 1);
	float threshold = (float) Math.sqrt(Math.pow(worldWidth/2, 2) * 2);
	PerlinNoiseGenerator worldGenerator = new PerlinNoiseGenerator(worldWidth, worldWidth);
	PerlinNoiseGenerator texGenerator = new PerlinNoiseGenerator(texWidth, texWidth);
	
	public void setX(Vector3f v, float x) {
		float y = v.y;
		float z = v.z;
		v.set(x, y, z);
	}
	
	public void setY(Vector3f v, float y) {
		float x = v.x;
		float z = v.z;
		v.set(x, y, z);
	}
	public void setZ(Vector3f v, float z) {
		float x = v.x;
		float y = v.y;
		v.set(x, y, z);
	}
	
	public void setX(Point3f v, float x) {
		float y = v.y;
		float z = v.z;
		v.set(x, y, z);
	}
	
	public void setY(Point3f v, float y) {
		float x = v.x;
		float z = v.z;
		v.set(x, y, z);
	}
	public void setZ(Point3f v, float z) {
		float x = v.x;
		float y = v.y;
		v.set(x, y, z);
	}

	@Override
	public void initializeScene() {		
		try 
		{	
			mSceneRoot = new SceneObject();
			BufferedImage perlinNoise = new BufferedImage(worldWidth + 1, worldWidth + 1, BufferedImage.TYPE_INT_RGB);
			float[][] heightmap = worldGenerator.seededPerlinNoiseTexture(-1, 5);
			leftBintree = new Bintree(worldWidth * worldWidth);
			rightBintree = new Bintree(worldWidth * worldWidth);
			leftBintree.initBintree(heightmap, true);
			rightBintree.initBintree(heightmap, false);
			float[][] islandShape = new float[worldWidth+1][worldWidth+1];
			for(int x = 0; x <= worldWidth; x++)
			{
				for(int y = 0; y <= worldWidth; y++)
				{
					islandShape[x][y] = 1f - (float) (Math.max(Math.sqrt(Math.pow((worldWidth/2 - x), 2) + Math.pow((worldWidth/2 - y), 2)), 0)) / threshold;
//					System.out.println((int)(islandShape[x][y] * 255));
					Color color = new Color(Math.max(0, (int)(islandShape[x][y] * 255)), (int)(heightmap[x][y] * 255), (int)(heightmap[x][y] * 255));
					perlinNoise.setRGB(x, y, color.getRGB());
				}
			}
			File outputFile = new File("src/textures/perlin.png");
			ImageIO.write(perlinNoise, "png", outputFile);

			Trimesh trimesh = generateMeshFromHeightmap(heightmap, islandShape);

			Geometry terrainGeometry = new Geometry();
			terrainGeometry.addMesh(trimesh);
			
			oceanMesh = new Quadmesh();
			oceanMesh.setMaterial(new OceanMaterial(new Color3f(0f, .5f, 1f), oceanWidth));
			oceanMesh.setName("Ocean");
			
			FloatBuffer oceanNormalData = Buffers.newDirectFloatBuffer(12);
			FloatBuffer oceanVertexData = Buffers.newDirectFloatBuffer(12);
			FloatBuffer oceanTexCoordData = Buffers.newDirectFloatBuffer(8);
			IntBuffer oceanQuadData = Buffers.newDirectIntBuffer(4);
			
			for(int x = 0; x < 2; x++)
			{
				for(int y = 0; y < 2; y++)
				{
					float xPos = (2 * x * oceanWidth) - oceanWidth;
					float yPos = oceanHeight;
					float zPos = (2 * y * oceanWidth) - oceanWidth;
					
					oceanVertexData.put(xPos);
					oceanVertexData.put(yPos);
					oceanVertexData.put(zPos);

					float xCoord = (float)x / (float)worldWidth / 2f * (float)texWidth;
					float yCoord = (float)y / (float)worldWidth / 2f * (float)texWidth;
					oceanTexCoordData.put(xCoord);
					oceanTexCoordData.put(yCoord);
					
					oceanNormalData.put(0f);
					oceanNormalData.put(1f);
					oceanNormalData.put(0f);
				}
			}
			
			oceanQuadData.put(0);
			oceanQuadData.put(2);
			oceanQuadData.put(3);
			oceanQuadData.put(1);
			
			oceanNormalData.rewind();
			oceanVertexData.rewind();
			oceanTexCoordData.rewind();
			oceanQuadData.rewind();
			
			oceanMesh.setVertexData(oceanVertexData);
			oceanMesh.setPolygonData(oceanQuadData);
			oceanMesh.setTexCoordData(oceanTexCoordData);
			oceanMesh.setNormalData(oceanNormalData);
			
			Geometry oceanGeometry = new Geometry();
			oceanGeometry.addMesh(oceanMesh);
			
			ArrayList<Geometry> terrainList = new ArrayList<Geometry>();
			terrainList.add(terrainGeometry);
			terrainList.add(oceanGeometry);
			mSceneRoot.addGeometry(terrainList);
			
			PointLight light = new PointLight();
			light.setConstantAttenuation(1.0f);
			light.setLinearAttenuation(0.0f);
			light.setQuadraticAttenuation(0.0f);
			light.setPosition(new Point3f(50.0f, 180.0f, 100.0f));
			mSceneRoot.addChild(light);

			mCamera.setPosition(new Point3f(0f, 0f, 300f));
			mCamera.setOrientation(new Quat4f(0f, 0f, 0f, 1f));

//			mRenderer.setRenderWireframes(!mRenderer.getRenderWireframes());
			
			//if (ID == 3 || ID == 4) numClouds = 10;
			//if (ID == 0) numClouds = 20;
			//numClouds = 1;
			for (int i = 0; i < numClouds; i++) {
				//added texture to particlesystem LOOK
				Point3f s = new Point3f(start);
				s.x += (Math.random() * (15)) + (Math.random()*2);
				s.z += (Math.random() * (10));

				if (s.z < -4.0) s.z *= 0.5;
				if (s.z > 5.0) s.z *= 0.5;
				if (s.x < -5) s.x *= -0.5;
				if (s.x > 5) s.x *= 0.5;

				mParticleSystem = new CloudSystem(55, mRenderer.textures, ID, s);
				mParticleSystem.setScale(scale);
				mParticleSystem.getPosition().y = cloudHeight;
				mParticleSystem.setStart(new Point3f(-5.0f, s.y, s.z));
				//System.out.println(s.y);
				mSceneRoot.addChild(mParticleSystem);
				clouds.add(mParticleSystem);
			}

			initCamera();
			requiresRender();
		}
		//Crash on error. Bwa ha.
		catch (ScenegraphException e) 
		{
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (GLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent mouseWheel) {
		/* Zoom in and out by the scroll wheel. */
		int mouseUnits = mouseWheel.getUnitsToScroll();
		if(mCameraRadius > 250.0f || mouseUnits > 0)
		{
			mCameraRadius += mouseWheel.getUnitsToScroll();
//			System.out.println(mCameraRadius);
//			mCamera.setPosition(new Point3f(mCamera.getPosition().x + mouseWheel.getUnitsToScroll(),
//											mCamera.getPosition().y + mouseWheel.getUnitsToScroll(),
//											mCamera.getPosition().z + mouseWheel.getUnitsToScroll()));
			Vector3f dPos = mCamera.transformVectorToParentSpace(new Vector3f(0, mouseUnits, 2 * mouseUnits));
			mCamera.getPosition().add(dPos);
			requiresRender();
		}
	}

	@Override
	public void mousePressed(MouseEvent mouse)
	{
		/* Remember the starting point of a drag. */
		mLastMouseDrag = mouse.getPoint();
	}

	@Override
	public void mouseDragged(MouseEvent mouse)
	{
		/* Calculate dragged delta. */
		float deltaX = -(mouse.getPoint().x - mLastMouseDrag.x);
		mLastMouseDrag = mouse.getPoint();

		/* Update longitude, wrapping as necessary. */
		mCameraLongitude += deltaX;

		if (mCameraLongitude > 360.0f)
		{
			mCameraLongitude -= 360.0f;
		}
		else if (mCameraLongitude < 0.0f)
		{
			mCameraLongitude += 360.0f;
		}
		
		Quat4f longitudeQuat = new Quat4f();
		longitudeQuat.set(new AxisAngle4f(0.0f, 1.0f, 0.0f, mCameraLongitude * (float)Math.PI / 180.0f));

		Quat4f latitudeQuat = new Quat4f();
		latitudeQuat.set(new AxisAngle4f(1.0f, 0.0f, 0.0f, mCameraLatitude * (float)Math.PI / 180.0f));
        
		Point3f position = new Point3f(mCamera.getPosition());		
		
		mCamera.setPosition(position);
		mCamera.getOrientation().mul(longitudeQuat, latitudeQuat);

		requiresRender();
	}

	/**
	 * Updates the camera position and orientation based on orbit parameters.
	 */
	private void initCamera()
	{
		/* Compose the "horizontal" and "vertical" rotations. */
		Quat4f longitudeQuat = new Quat4f();
		longitudeQuat.set(new AxisAngle4f(0.0f, 1.0f, 0.0f, mCameraLongitude * (float)Math.PI / 180.0f));

		Quat4f latitudeQuat = new Quat4f();
		latitudeQuat.set(new AxisAngle4f(1.0f, 0.0f, 0.0f, mCameraLatitude * (float)Math.PI / 180.0f));

		mCamera.getOrientation().mul(longitudeQuat, latitudeQuat);

		/* Set the camera's position so that it looks towards the origin. */
		mCamera.setPosition(new Point3f(0.0f, 0.0f, mCameraRadius));
		Util.rotateTuple(mCamera.getOrientation(), mCamera.getPosition());
	}

	public void keyPressed(KeyEvent key)
	{
		/* No default response. */
	}


	public void keyReleased(KeyEvent key)
	{
		/* No default response. */
	}

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
		if (c >= '0' && c <= '9')
		{
			mRenderer.previewGBuffer(c - '0' - 1);
			requiresRender();
		}
		else if (c == 't')
		{
			mRenderer.setToonShading(!mRenderer.getToonShading());
			requiresRender();
		}
		else if (c == 'i')
		{
			Point3f s = new Point3f(start);
			s.x += Math.random() * (15);
			s.z += Math.random() * (10);

			if (s.z < -4.0) s.z *= 0.5;
			if (s.z > 3.0) s.z *= 0.5;
			if (s.x < -5) s.x *= -0.5;
			if (s.x > 5) s.x *= 0.5;

			try {
				mParticleSystem = new CloudSystem(55, mRenderer.textures, ID, s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mParticleSystem.setScale(scale);
			mParticleSystem.getPosition().y = cloudHeight;
			mParticleSystem.setStart(new Point3f(-5.0f, s.y, s.z));
			try {
				mSceneRoot.addChild(mParticleSystem);
			} catch (ScenegraphException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			if (ID + 1 < 5) ID = ID+1;
//			else ID = 0;
//			for (CloudSystem sys : clouds) {
//				sys.removeChildren();
//				sys.setID(ID);
//				for (SceneObject cl: sys.getChildren())
//					if (cl instanceof Cloud)
//						try {
//							sys.addChildrens((Cloud) cl);
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//				
//			}
		}
		else if (c == 'f')
		{
			mRenderer.setRenderWireframes(!mRenderer.getRenderWireframes());
			requiresRender();
		}
		else if (c == 'w')
		{
			Vector3f dPos = mCamera.transformVectorToParentSpace(new Vector3f(0f, 0f, -5f));
			setY(dPos, 0);
			mCamera.getPosition().add(dPos);
			requiresRender();
		}
		else if(c == 'a')
		{
			Vector3f dPos = mCamera.transformVectorToParentSpace(new Vector3f(-5f, 0f, 0f));
			setY(dPos, 0);
			mCamera.getPosition().add(dPos);
			requiresRender();
		}
		else if(c == 's')
		{
			Vector3f dPos = mCamera.transformVectorToParentSpace(new Vector3f(0f, 0f, 5f));
			setY(dPos, 0);
			mCamera.getPosition().add(dPos);
			requiresRender();
		}
		else if(c == 'd')
		{
			Vector3f dPos = mCamera.transformVectorToParentSpace(new Vector3f(5f, 0f, 0f));
			setY(dPos, 0);
			mCamera.getPosition().add(dPos);
			requiresRender();
		}
	}

	public void actionPerformed(ActionEvent event) {
		// TODO Auto-generated method stub
		if (event.getSource() == mUpdateTimer)
		{
			long now = System.currentTimeMillis();
			long dt = now - mPreviousFrameTime;
			mPreviousFrameTime = now;

			mParticleSystem.mTimeSinceLastSpawn += dt;

			nextFrame(dt / 1000.0f);
		}
	}
	
	private Trimesh generateMeshFromHeightmap(float[][] heightmap, float[][] islandShape)
	{
		Trimesh trimesh = new Trimesh();
		trimesh.setMaterial(new ProceduralTerrainMaterial(new Color(150, 100, 0), new Color(90, 200, 50), 
														  new Color(75, 65, 55), new Color(200, 200, 200), 
														  new Color(255, 255, 255), new Color(220, 220, 240), 
														  new Color(255, 255, 220), new Color(240, 210, 120),
														  texWidth, -1));
//		trimesh.setMaterial(new LambertianMaterial());
		trimesh.setName("Terrain");
		System.out.println("Terrain Material Created");
		
		Vector3f vert = new Vector3f(); 
		Vector3f norm = new Vector3f();
		Vector3f line1 = new Vector3f();
		Vector3f line2 = new Vector3f();
		Vector3f line3 = new Vector3f();
		Vector3f line4 = new Vector3f();
		Vector3f tri1 = new Vector3f();
		Vector3f tri2 = new Vector3f();

		FloatBuffer normalData = Buffers.newDirectFloatBuffer(3 * ((worldWidth + 1) * (worldWidth + 1)));	
		FloatBuffer verts = Buffers.newDirectFloatBuffer(3 * ((worldWidth + 1) * (worldWidth + 1)));
		FloatBuffer texCoordData = Buffers.newDirectFloatBuffer(2 * ((worldWidth + 1)* (worldWidth + 1)));	
		for(int x = 0; x <= worldWidth; x++)
		{
			for(int y = 0; y <= worldWidth; y++)
			{					
				float xPos = (2 * x) - worldWidth;
				float yPos = (heightmap[x][y] * islandShape[x][y]) * worldHeight;
				float zPos = (2 * y) - worldWidth;
				
				vert.set(xPos, yPos, zPos);
				
				verts.put(xPos);
				verts.put(yPos);
				verts.put(zPos);

				float xCoord = (float)x / (float)worldWidth;
				float yCoord = (float)y / (float)worldWidth;
				texCoordData.put(xCoord);
				texCoordData.put(yCoord);
//				System.out.println("TexCoords: " + xCoord + ", " + yCoord);
				
				if(x == 0 && y == 0)
				{
					norm = new Vector3f(vert);
					norm.sub(new Vector3f(x+2, (heightmap[x+1][y] * islandShape[x+1][y]) * worldHeight, y));
					Vector3f temp = new Vector3f(vert);
					temp.sub(new Vector3f(x, (heightmap[x][y+1] * islandShape[x][y+1]) * worldHeight, y+2));
					norm.cross(norm, temp);
				}
				else if(x == worldWidth && y == 0)
				{
					norm = new Vector3f(vert);
					norm.sub(new Vector3f(x-2, (heightmap[x-1][y] * islandShape[x-1][y]) * worldHeight, y));
					Vector3f temp = new Vector3f(vert);
					temp.sub(new Vector3f(x, (heightmap[x][y+1] * islandShape[x][y+1]) * worldHeight, y+2));
					norm.cross(norm, temp);
				}
				else if(x == 0 && y == worldWidth)
				{
					norm = new Vector3f(vert);
					norm.sub(new Vector3f(x, (heightmap[x][y-1] * islandShape[x][y-1]) * worldHeight, y-2));
					Vector3f temp = new Vector3f(vert);
					temp.sub(new Vector3f(x+2, (heightmap[x+1][y] * islandShape[x+1][y]) * worldHeight, y));
					norm.cross(norm, temp);
				}
				else if(x == worldWidth && y == worldWidth)
				{
					norm = new Vector3f(vert);
					norm.sub(new Vector3f(x-2, (heightmap[x-1][y] * islandShape[x-1][y]) * worldHeight, y));
					Vector3f temp = new Vector3f(vert);
					temp.sub(new Vector3f(x, (heightmap[x][y-1] * islandShape[x][y-1]) * worldHeight, y-2));
					norm.cross(norm, temp);
				}
				else if(x == 0)
				{
					line1 = new Vector3f(vert);
					line1.sub(new Vector3f(x+2, (heightmap[x+1][y] * islandShape[x+1][y]) * worldHeight, y));
					line2 = new Vector3f(vert);
					line2.sub(new Vector3f(x, (heightmap[x][y+1] * islandShape[x][y+1]) * worldHeight, y+2));
					line3 = new Vector3f(vert);
					line3.sub(new Vector3f(x, (heightmap[x][y-1] * islandShape[x][y-1]) * worldHeight, y-2));
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
					line1.sub(new Vector3f(x, (heightmap[x][y+1] * islandShape[x][y+1]) * worldHeight, y+2));
					line2 = new Vector3f(vert);
					line2.sub(new Vector3f(x+2, (heightmap[x+1][y] * islandShape[x+1][y]) * worldHeight, y));
					line3 = new Vector3f(vert);
					line3.sub(new Vector3f(x-2, (heightmap[x-1][y] * islandShape[x-1][y]) * worldHeight, y));
					tri1 = new Vector3f();
					tri1.cross(line1, line2);
					tri2 = new Vector3f();
					tri2.cross(line1, line3);
					norm = new Vector3f();
					norm.add(tri1, tri2);
				}
				else if(x == worldWidth)
				{
					line1 = new Vector3f(vert);
					line1.sub(new Vector3f(x-2, (heightmap[x-1][y] * islandShape[x][y]) * worldHeight, y));
					line2 = new Vector3f(vert);
					line2.sub(new Vector3f(x, (heightmap[x][y+1] * islandShape[x][y+1]) * worldHeight, y+2));
					line3 = new Vector3f(vert);
					line3.sub(new Vector3f(x, (heightmap[x][y-1] * islandShape[x][y-1]) * worldHeight, y-2));
					tri1 = new Vector3f();
					tri1.cross(line1, line2);
					tri2 = new Vector3f();
					tri2.cross(line1, line3);
					norm = new Vector3f();
					norm.add(tri1, tri2);
				}
				else if(y == worldWidth)
				{
					line1 = new Vector3f(vert);
					line1.sub(new Vector3f(x, (heightmap[x][y-1] * islandShape[x][y-1]) * worldHeight, y+2));
					line2 = new Vector3f(vert);
					line2.sub(new Vector3f(x+2, (heightmap[x+1][y] * islandShape[x+1][y]) * worldHeight, y));
					line3 = new Vector3f(vert);
					line3.sub(new Vector3f(x-2, (heightmap[x-1][y] * islandShape[x-1][y]) * worldHeight, y));
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
					line1.sub(new Vector3f(vert.x, (heightmap[x][y+1] * islandShape[x][y+1]) * worldHeight, vert.z+2));
					line2 = new Vector3f(vert);
					line2.sub(new Vector3f(vert.x+2, (heightmap[x+1][y] * islandShape[x+1][y]) * worldHeight, vert.z));
					line3 = new Vector3f(vert);
					line3.sub(new Vector3f(vert.x-2, (heightmap[x-1][y] * islandShape[x-1][y]) * worldHeight, vert.z));
					line4 = new Vector3f(vert);
					line4.sub(new Vector3f(vert.x, (heightmap[x][y-1] * islandShape[x][y-1]) * worldHeight, vert.z-2));
					tri1 = new Vector3f();
					tri1.cross(line3, line1);
					tri2 = new Vector3f();
					tri2.cross(line2, line4);
					norm = new Vector3f();
					norm.add(tri1, tri2);
				}
				norm.normalize();
				if(norm.y < 0)
				{
					norm.scale(-1);
				}
				normalData.put(norm.x);
				normalData.put(norm.y);
				normalData.put(norm.z);
			}
		}
		verts.rewind();
		normalData.rewind();
		texCoordData.rewind();

		IntBuffer polys = Buffers.newDirectIntBuffer(6 * (worldWidth * worldWidth));
		for(int column = 0; column < worldWidth; column++)
		{
			for(int row = 0; row < worldWidth; row++)
			{
				polys.put((((worldWidth + 1) * column) + row) % numVertices);
				polys.put((((worldWidth + 1) * column) + (row + 1)) % numVertices);
				polys.put((((worldWidth + 1) * (column + 1)) + row) % numVertices);
				polys.put((((worldWidth + 1) * column) + (row + 1)) % numVertices);
				polys.put((((worldWidth + 1) * (column + 1)) + row) % numVertices);
				polys.put((((worldWidth + 1) * (column + 1)) + (row + 1)) % numVertices);
			}
		}
		polys.rewind();	

		trimesh.setVertexData(verts);
		trimesh.setPolygonData(polys);
		trimesh.setTexCoordData(texCoordData);
		trimesh.setNormalData(normalData);
		
		return trimesh;
	}
}