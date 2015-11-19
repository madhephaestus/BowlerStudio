package com.neuronrobotics.bowlerstudio.threed;

/*
 * Copyright (c) 2011, 2013 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.neuronrobotics.bowlerstudio.VirtualCameraMobileBase;
import com.neuronrobotics.imageprovider.AbstractImageProvider;
import com.neuronrobotics.imageprovider.IVirtualCameraFactory;
import com.neuronrobotics.imageprovider.VirtualCameraFactory;
import com.neuronrobotics.sdk.addons.kinematics.AbstractKinematicsNR;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.ITaskSpaceUpdateListenerNR;
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.BowlerAbstractConnection;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.IConnectionEventListener;
import com.neuronrobotics.sdk.common.IDeviceConnectionEventListener;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.dyio.DyIO;
import com.neuronrobotics.sdk.dyio.dypid.DyPIDConfiguration;
import com.neuronrobotics.sdk.dyio.peripherals.DigitalInputChannel;
import com.neuronrobotics.sdk.dyio.peripherals.IDigitalInputListener;
import com.neuronrobotics.sdk.pid.PIDConfiguration;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;

import javafx.application.Application;
import javafx.application.Platform;
import static javafx.application.Application.launch;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import static javafx.scene.input.KeyCode.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;
import javafx.scene.Node;

// TODO: Auto-generated Javadoc
/**
 * MoleculeSampleApp.
 */
public class Jfx3dManager extends JFXPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6744581340628622682L;

	/** The root. */
	private final Group root = new Group();
	
	/** The axis group. */
	final Group axisGroup = new Group();
	
	/** The world. */
	final Xform world = new Xform();
	
	/** The camera. */
	final PerspectiveCamera camera = new PerspectiveCamera(true);

	
	/** The camera distance. */
	final double cameraDistance = 3000;
	
	/** The molecule group. */
	final Xform moleculeGroup = new Xform();
	
	/** The one frame. */
	double ONE_FRAME = 1.0 / 24.0;
	
	/** The delta multiplier. */
	double DELTA_MULTIPLIER = 200.0;
	
	/** The control multiplier. */
	double CONTROL_MULTIPLIER = 0.1;
	
	/** The shift multiplier. */
	double SHIFT_MULTIPLIER = 0.1;
	
	/** The alt multiplier. */
	double ALT_MULTIPLIER = 0.5;
	
	/** The mouse pos x. */
	double mousePosX;
	
	/** The mouse pos y. */
	double mousePosY;
	
	/** The mouse old x. */
	double mouseOldX;
	
	/** The mouse old y. */
	double mouseOldY;
	
	/** The mouse delta x. */
	double mouseDeltaX;
	
	/** The mouse delta y. */
	double mouseDeltaY;

	/** The manipulator. */
	private final Group manipulator = new Group();
	
	/** The look group. */
	private final Group lookGroup = new Group();
	
	/** The scene. */
	private SubScene scene;
	
	
	/** The ground. */
	private Group ground;
	
	private boolean captureMouse = false;

	private VirtualCameraDevice virtualcam;

	private VirtualCameraMobileBase flyingCamera;

	/**
	 * Instantiates a new jfx3d manager.
	 */
	public Jfx3dManager() {
		buildScene();
		buildCamera();
		buildAxes();

		setSubScene(new SubScene(getRoot(), 1024, 1024, true, null));
		Stop[] stops = null;
		getSubScene().setFill(new LinearGradient(125, 0, 225, 0, false, CycleMethod.NO_CYCLE, stops));
		Scene s = new Scene(new Group(getSubScene()));
		handleKeyboard(s);
		handleMouse(getSubScene());
		getSubScene().setCamera(camera);
		camera.setRotationAxis(Rotate.Z_AXIS);
		camera.setRotate(180);
		camera.getTransforms().add(new Translate(0, -200, -600));
	
		setVirtualcam(new VirtualCameraDevice(camera));
		VirtualCameraFactory.setFactory(new IVirtualCameraFactory() {
			@Override
			public AbstractImageProvider getVirtualCamera() {
				// TODO Auto-generated method stub
				return getVirtualcam();
			}
		});
		
		try {
			setFlyingCamera(new VirtualCameraMobileBase());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setScene(s);

	}
	
	/**
	 * Removes the objects.
	 */
	public void removeObjects(){
		lookGroup.getChildren().clear();
	}

	/**
	 * Removes the object.
	 *
	 * @param previous the previous
	 */
	public void removeObject(MeshView previous) {
		if (previous != null) {
			lookGroup.getChildren().remove(previous);
		}

	}

	/**
	 * Adds the object.
	 *
	 * @param current the current
	 * @return the mesh view
	 */
	public MeshView addObject(MeshView current) {
		Group og = new Group();
		og.getChildren().add(current);
		Axis a = new Axis();
		a.getTransforms().addAll(current.getTransforms());
		og.getChildren().add(a);
		lookGroup.getChildren().add(og);
		//Log.warning("Adding new axis");
		return current;
	}

	/**
	 * Save to png.
	 *
	 * @param f the f
	 */
	public void saveToPng(File f) {
		String fName = f.getAbsolutePath();

		if (!fName.toLowerCase().endsWith(".png")) {
			fName += ".png";
		}

		int snWidth = 1024;
		int snHeight = 1024;

		double realWidth = getRoot().getBoundsInLocal().getWidth();
		double realHeight = getRoot().getBoundsInLocal().getHeight();

		double scaleX = snWidth / realWidth;
		double scaleY = snHeight / realHeight;

		double scale = Math.min(scaleX, scaleY);

		PerspectiveCamera snCam = new PerspectiveCamera(false);
		snCam.setTranslateZ(-200);

		SnapshotParameters snapshotParameters = new SnapshotParameters();
		snapshotParameters.setTransform(new Scale(scale, scale));
		snapshotParameters.setCamera(snCam);
		snapshotParameters.setDepthBuffer(true);
		snapshotParameters.setFill(Color.TRANSPARENT);

		WritableImage snapshot = new WritableImage(snWidth,
				(int) (realHeight * scale));

		getRoot().snapshot(snapshotParameters, snapshot);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png",
					new File(fName));
		} catch (IOException ex) {
			ex.printStackTrace();
			Log.error(ex.getMessage());
		}
	}


	/**
	 * Builds the scene.
	 */
	private void buildScene() {
		world.rx.setAngle(-90);// point z upwards
		world.ry.setAngle(180);// arm out towards user
		getRoot().getChildren().add(world);
	}

	/**
	 * Builds the camera.
	 */
	private void buildCamera() {

		camera.setNearClip(.1);
		camera.setFarClip(100000.0);
//		camera.setRotationAxis(Rotate.X_AXIS);
//		camera.setRotate(90);
	}
	
	/**
	 * Gets the camera field of view property.
	 *
	 * @return the camera field of view property
	 */
	public DoubleProperty getCameraFieldOfViewProperty(){
		return camera.fieldOfViewProperty();
	}

	/**
	 * Builds the axes.
	 */
	private void buildAxes() {
		
		int gridSize=1000;
		int gridDensity=gridSize/10;
		ground = new Group();
		Affine groundPlacment=new Affine();
		for(int i=-gridSize;i<gridSize;i++){
			for(int j=-gridSize;j<gridSize;j++){
				if(i%gridDensity==0 &&j%gridDensity==0){
					Sphere s = new Sphere(3);
					Affine sp=new Affine();
					sp.setTy(i);
					sp.setTx(j);
					//System.err.println("Placing sphere at "+i+" , "+j);
					s.getTransforms().add(sp);
					ground.getChildren().add(s);
				}
			}
		}

		groundPlacment.setTz(-1);
		//ground.setOpacity(.5);
		ground.getTransforms().add(groundPlacment);
		axisGroup.getChildren().addAll(new Axis(),ground);
		world.getChildren().addAll(axisGroup, lookGroup);
	}
	
	

	/**
	 * Handle mouse.
	 *
	 * @param scene the scene
	 * @param root the root
	 */
	private void handleMouse(SubScene scene) {
		scene.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent me) {
				mousePosX = me.getSceneX();
				mousePosY = me.getSceneY();
				mouseOldX = me.getSceneX();
				mouseOldY = me.getSceneY();
				if(me.isPrimaryButtonDown())
					captureMouse=true;
				else
					captureMouse=false;
			}
		});
		scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent me) {
				mouseOldX = mousePosX;
				mouseOldY = mousePosY;
				mousePosX = me.getSceneX();
				mousePosY = me.getSceneY();
				mouseDeltaX = (mousePosX - mouseOldX);
				mouseDeltaY = (mousePosY - mouseOldY);

				double modifier = 1.0;
				double modifierFactor = 0.1;

				if (me.isControlDown()) {
					modifier = 0.1;
				}
				if (me.isShiftDown()) {
					modifier = 10.0;
				}
				if (me.isPrimaryButtonDown()) {
//					cameraXform.ry.setAngle(cameraXform.ry.getAngle()
//							- mouseDeltaX * modifierFactor * modifier * 2.0); // +
//					cameraXform.rx.setAngle(cameraXform.rx.getAngle()
//							+ mouseDeltaY * modifierFactor * modifier * 2.0); // -
					if (me.isPrimaryButtonDown()) {
						getFlyingCamera()
						.DriveArc(new TransformNR(0,0,0,
								new RotationNR(
										mouseDeltaY * modifierFactor * modifier * 2.,
										-mouseDeltaX * modifierFactor * modifier * 2.0,
										0
										))
								, 0);
					} 
				} 
				else if (me.isMiddleButtonDown()) {
					double z = camera.getTranslateZ();
					double newZ = z + mouseDeltaX * modifierFactor * modifier;
					camera.setTranslateZ(newZ);
				} 
//				else if (me.isSecondaryButtonDown()) {
//					cameraXform.t.setX(cameraXform.t.getX() + mouseDeltaX
//							* modifierFactor * modifier * 1); // -
//					cameraXform.t.setY(cameraXform.t.getY() + mouseDeltaY
//							* modifierFactor * modifier * 1); // -
//				}
			}
		});
		scene.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {

			@Override
			public void handle(ScrollEvent t) {
				if (ScrollEvent.SCROLL == (t).getEventType()) {

//					double zoomFactor = (t.getDeltaY());
//
//					double z = camera.getTranslateY();
//					double newZ = z + zoomFactor;
//					camera.setTranslateY(newZ);
					// System.out.println("Z = "+newZ);
				}
				t.consume();
			}
		});

	}

	/**
	 * Handle keyboard.
	 *
	 * @param scene the scene
	 * @param root the root
	 */
	private void handleKeyboard(Scene scene) {
		//final boolean moveCamera = true;
		System.out.println("Adding keyboard listeners");
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			double modifier = 5.0;
			double modifierFactor = 0.1;

			@Override
			public void handle(KeyEvent event) {
				
				//Duration currentTime;
				switch (event.getCode()) {
				case W:
				case UP:
					getFlyingCamera()
					.DriveArc(new TransformNR(0,0,modifier,
							new RotationNR())
							, 0);
					break;
				case S:
				case DOWN:
					getFlyingCamera()
					.DriveArc(new TransformNR(0,0,-modifier,
							new RotationNR())
							, 0);
					break;
				case D:
				case RIGHT:
					getFlyingCamera()
					.DriveArc(new TransformNR(modifier,0,0,
							new RotationNR())
							, 0);
					break;
				case A:
				case LEFT:
					getFlyingCamera()
					.DriveArc(new TransformNR(-modifier,0,0,
							new RotationNR())
							, 0);
					break;
				default:
					break;
				}
			}
		});
	}

	// @Override
	// public void start(Stage primaryStage) {
	//
	//
	// }

	/**
	 * The main() method is ignored in correctly deployed JavaFX application.
	 * main() serves only as fallback in case the application can not be
	 * launched through deployment artifacts, e.g., in IDEs with limited FX
	 * support. NetBeans ignores main().
	 *
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		System.setProperty("prism.dirtyopts", "false");

		JFrame frame = new JFrame();
		frame.setContentPane(new Jfx3dManager());

		frame.setSize(1024, 1024);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	/**
	 * Gets the sub scene.
	 *
	 * @return the sub scene
	 */
	public SubScene getSubScene() {
		return scene;
	}

	/**
	 * Sets the sub scene.
	 *
	 * @param scene the new sub scene
	 */
	public void setSubScene(SubScene scene) {
		this.scene = scene;
	}

	/**
	 * Gets the root.
	 *
	 * @return the root
	 */
	public Group getRoot() {
		return root;
	}
	
	/**
	 * Removes the arm.
	 */
	public void removeArm() {
		world.getChildren().remove(manipulator);
	}

	public VirtualCameraDevice getVirtualcam() {
		return virtualcam;
	}

	public void setVirtualcam(VirtualCameraDevice virtualcam) {
		this.virtualcam = virtualcam;
	}

	public VirtualCameraMobileBase getFlyingCamera() {
		return flyingCamera;
	}

	public void setFlyingCamera(VirtualCameraMobileBase flyingCamera) {
		this.flyingCamera = flyingCamera;
	}
}