package cs5625.deferred.apps;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import cs5625.deferred.rendering.Camera;
import cs5625.deferred.rendering.Renderer;
import cs5625.deferred.scenegraph.SceneObject;
import cs5625.deferred.ui.MainViewWindow;

/**
 * SceneController.java
 * 
 * The SceneController class contains the application main() method. It's responsible for creating 
 * the OpenGL window and renderer, and its subclasses are responsible for handling user actions.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-23
 */
public abstract class SceneController implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	/*
	 * Member variables to store the renderer, scene root, and camera.
	 */
	protected MainViewWindow mMainWindow;
	protected Renderer mRenderer = new Renderer();
	protected SceneObject mSceneRoot = new SceneObject();
	protected Camera mCamera = new Camera();
	
	@SuppressWarnings("unused")
	private static SceneController globalController = null;
	
	/**
	 * SceneController contains the application main() method. It creates an OpenGL 
	 * window and renderer and a default controller instance to manage the scene.
	 * 
	 * @param args Unused.
	 */
	public static void main(String[] args)
	{
		/*
		 * There is some weird jar conflict issue on OS X loading the Point2i class, 
		 * so force it to load the right one.
		 */
		try
		{
			Class.forName("javax.vecmath.Point2i");
			Class.forName("javax.vecmath.Point2f");
			Class.forName("javax.vecmath.Point3i");
			Class.forName("javax.vecmath.Point3f");
			Class.forName("javax.vecmath.Point4i");
			Class.forName("javax.vecmath.Point4f");
		}
		catch (ClassNotFoundException err)
		{
			err.printStackTrace();
			System.exit(-1);
		}

		/* 
		 * Create the scene controller instance. If you want a different style of 
		 * control (e.g. game-style input), make a new subclass!
		 */


//		globalController = new ParticleSystemSceneController();
		//globalController = new ManyLightsSceneController();
		globalController = new SimulatorSceneController();
	}
	
	/*
	 * Default constructor initializes the OpenGL window. 
	 */
	public SceneController()
	{
		mMainWindow = new MainViewWindow("CS 5625 Deferred Renderer", this);
		mMainWindow.setVisible(true);
	}
	
	/**
	 * Called once on initialization to create the default scene. Subclasses must implement this method.
	 */
	public abstract void initializeScene();
	
	/**
	 * Can be called by anyone to request a re-render.
	 */
	public void requiresRender()
	{
		mMainWindow.repaint();
	}
	
	/**
	 * Can be called by anyone to tell a self-animating controller to update and render a new frame.
	 * Default implementation just calls `mSceneRoot.animate(dt)` and `requiresRender()`.
	 * 
	 * @param dt The time (in seconds) since the last frame update. Used for time-based (as opposed to 
	 *        frame-based) animation.
	 */
	public void nextFrame(float dt)
	{
		mSceneRoot.animate(dt);
		requiresRender();
	}
	
	/**
	 * Called by the OpenGL view to re-render the scene.
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 */
	public void renderGL(GLAutoDrawable drawable)
	{
		mRenderer.render(drawable, mSceneRoot, mCamera);
	}

	/**
	 * Called once after the OpenGL context has been set up to perform one-time initialization. 
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 */
	public void initGL(GLAutoDrawable drawable)
	{
		mRenderer.init(drawable);
		initializeScene();
	}
	
	/**
	 * Called once before the OpenGL context will be destroyed to free all context-related resources. 
	 * This implementation sets the camera, scene, and renderer to null. This ensures that all textures, 
	 * shaders, FBOs, and any other resources are no longer in use.
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 */
	public void disposeGL(GLAutoDrawable drawable)
	{
		GL2 gl = drawable.getGL().getGL2();
		mCamera.releaseGPUResources(gl);
		mSceneRoot.releaseGPUResources(gl);
		mRenderer.releaseGPUResources(gl);
	}

	/**
	 * Called by the OpenGL view to notify the renderer that its dimensions have changed.
	 * 
	 * @param drawable The OpenGL drawable representing the context.
	 * @param width The new width (in pixels) of the context.
	 * @param height The new height (in pixels) of the context.
	 */
	public void resizeGL(GLAutoDrawable drawable, int width, int height)
	{
		mRenderer.resize(drawable, width, height);
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void keyPressed(KeyEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void keyReleased(KeyEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void keyTyped(KeyEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseWheelMoved(MouseWheelEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseDragged(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseMoved(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseClicked(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseEntered(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseExited(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mousePressed(MouseEvent arg0)
	{
		/* No default response. */
	}

	/**
	 * Override this in your SceneController subclass to respond to this type of user action.
	 */
	
	public void mouseReleased(MouseEvent arg0)
	{
		/* No default response. */
	}
}
