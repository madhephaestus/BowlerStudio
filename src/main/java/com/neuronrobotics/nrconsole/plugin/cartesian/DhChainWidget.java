package com.neuronrobotics.nrconsole.plugin.cartesian;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.neuronrobotics.bowlerstudio.BowlerStudioController;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.nrconsole.util.XmlFilter;
import com.neuronrobotics.sdk.addons.kinematics.AbstractKinematicsNR;
import com.neuronrobotics.sdk.addons.kinematics.DHChain;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration;
import com.neuronrobotics.sdk.addons.kinematics.LinkFactory;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.Transform;
import javafx.scene.Group;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class DhChainWidget extends Group implements ICadGenerator{
	private File currentFile=null;
	private VBox links;
	private VBox controls;
	JogWidget jog = null;
	
	private AbstractKinematicsNR device;
	private DHParameterKinematics dhdevice=null;
	private ICadGenerator cadEngine =null;

	private ArrayList<DHLinkWidget> widgets = new ArrayList<>();
	public DhChainWidget(AbstractKinematicsNR device2){
		this.device = device2;
		if(DHParameterKinematics.class.isInstance(device2)){
			dhdevice=(DHParameterKinematics)device2;
		}
		links = new VBox(20);
		controls = new VBox(10);
		jog = new JogWidget(device);
		
		VBox advanced = new VBox(10);
		
		
		
		Button save = new Button("Save Configuration");
		Button add = new Button("Add Link");
		Button refresh = new Button("Generate CAD");
		save.setOnAction(event -> {
			new Thread(){
				public void run(){
					File last = FileSelectionFactory.GetFile(currentFile==null?
										ScriptingEngine.getWorkspace():
										new File(ScriptingEngine.getWorkspace().getAbsolutePath()+"/"+currentFile.getName()),
							new XmlFilter());
					if (last != null) {
						try {
							Files.write(Paths.get(last.getAbsolutePath()),device.getXml().getBytes() );
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}.start();
		});
		add.setOnAction(event -> {
			LinkConfiguration newLink = new LinkConfiguration();
			if(dhdevice!=null)dhdevice.addNewLink(newLink,new DHLink(0, 0, 0, 0));
			onTabReOpening();
		});
		refresh.setOnAction(event -> {
			onTabReOpening();
		});
		
		advanced.getChildren().add(new TransformWidget("Limb to Base", 
				device.getRobotToFiducialTransform(), new IOnTransformChange() {
					
					@Override
					public void onTransformFinished(TransformNR newTrans) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onTransformChaging(TransformNR newTrans) {
						Log.debug("Limb to base"+newTrans.toString());
						device.setRobotToFiducialTransform(newTrans);
						device.getCurrentTaskSpaceTransform();
					}
				}
				));
		advanced.getChildren().add(new TransformWidget("Base to Global", 
				device.getFiducialToGlobalTransform(),new IOnTransformChange() {
					
					@Override
					public void onTransformFinished(TransformNR newTrans) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onTransformChaging(TransformNR newTrans) {
						Log.debug("Base to Global"+newTrans.toString());
						device.setGlobalToFiducialTransform(newTrans);
						device.getCurrentTaskSpaceTransform();
					}
				}
				));
		

		advanced.getChildren().add(save);
		advanced.getChildren().add(add);
		advanced.getChildren().add(refresh);
		Accordion advancedPanel = new Accordion();
		advancedPanel.getPanes().add(new TitledPane("Advanced Options", advanced));
		controls.getChildren().add(jog);
		controls.getChildren().add(advancedPanel);
		onTabReOpening();

		getChildren().add(new ScrollPane(links));
	}
	public ArrayList<CSG> onTabReOpening() {
		for(DHLinkWidget wid:widgets){
			device.removeJointSpaceUpdateListener(wid);
		}
		widgets.clear();
		links.getChildren().clear();
		ArrayList<DHLink> dhLinks=null;
		if(dhdevice!=null)
			dhLinks = dhdevice.getChain().getLinks();
		links.getChildren().add(controls);

		
		for(int i=0;i<device.getFactory().getLinkConfigurations().size();i++){
			Log.warning("Adding Link Widget: "+i);
			
			DHLink dh=null;
			if(dhdevice!=null)
				dh=dhLinks.get(i);
			Button del = new Button("Delete");
			final int linkIndex=i;
			del.setOnAction(event -> {
				LinkFactory factory  =device.getFactory();
				//remove the link listener while the number of links could chnage
				factory.removeLinkListener(device);
				if(dhdevice!=null){
					DHChain chain = dhdevice.getDhChain() ;
					chain.getLinks().remove(linkIndex);
					factory.deleteLink(linkIndex);
					//set the modified kinematics chain
					dhdevice.setChain(chain);
				}else{
					factory.deleteLink(linkIndex);
				}
				

				//once the new link configuration is set up, re add the listener
				factory.addLinkListener(device);
				onTabReOpening();
				
			});
			DHLinkWidget w = new DHLinkWidget(i,
					dh,
					device,
					del
					);
			widgets.add(w);
			links.getChildren().add(w);
			device.addJointSpaceListener(w);
		}
		//BowlerStudioController.setCsg(csg);
		jog.home();
		
		return generateCad(dhLinks);
	}
	
	public ArrayList<CSG> generateCad(ArrayList<DHLink> dhLinks ){
		if(cadEngine!=null)
			return cadEngine.generateCad(dhLinks);
		ArrayList<CSG> csg = new ArrayList<CSG>();
		for(int i=0;i<dhLinks.size();i++){
			Log.warning("Adding Link Widget: "+i);
			DHLink dh  =dhLinks.get(i);
			// Create an axis to represent the link
			double y = dh.getD()>0?dh.getD():2;
			double  x= dh.getRadius()>0?dh.getRadius():2;

			CSG cube = new Cube(x,y,2).toCSG();
			cube=cube.transformed(new Transform().translateX(-x/2));
			cube=cube.transformed(new Transform().translateY(y/2));
			//add listner to axis
			cube.setManipulator(dh.getListener());
			cube.setColor(Color.GOLD);
			// add ax to list of objects to be returned
			csg.add(cube);
		}
		BowlerStudioController.setCsg(csg);
		return csg;
	}
	public ICadGenerator getCadEngine() {
		return cadEngine;
	}
	public void setCadEngine(ICadGenerator cadEngine) {
		this.cadEngine = cadEngine;
	}

}
