package com.neuronrobotics.bowlerstudio.creature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.HashMap;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.python.core.exceptions;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;

import com.neuronrobotics.bowlerstudio.BowlerStudio;
import com.neuronrobotics.bowlerstudio.BowlerStudioController;
import com.neuronrobotics.bowlerstudio.ConnectionManager;
import com.neuronrobotics.bowlerstudio.scripting.IScriptEventListener;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingFileWidget;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingWebWidget;
import com.neuronrobotics.bowlerstudio.scripting.ShellType;
import com.neuronrobotics.bowlerstudio.tabs.AbstractBowlerStudioTab;
import com.neuronrobotics.nrconsole.util.DirectoryFilter;
import com.neuronrobotics.nrconsole.util.FileSelectionFactory;
import com.neuronrobotics.nrconsole.util.GroovyFilter;
import com.neuronrobotics.nrconsole.util.XmlFilter;
import com.neuronrobotics.sdk.addons.kinematics.AbstractKinematicsNR;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics;
import com.neuronrobotics.sdk.addons.kinematics.DhInverseSolver;
import com.neuronrobotics.sdk.addons.kinematics.DrivingType;
import com.neuronrobotics.sdk.addons.kinematics.IDriveEngine;
import com.neuronrobotics.sdk.addons.kinematics.MobileBase;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.IDeviceConnectionEventListener;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.util.FileChangeWatcher;
import com.neuronrobotics.sdk.util.IFileChangeListener;

import eu.mihosoft.vrl.v3d.CSG;
import eu.mihosoft.vrl.v3d.Cube;
import eu.mihosoft.vrl.v3d.FileUtil;
import eu.mihosoft.vrl.v3d.PrepForManufacturing;
import eu.mihosoft.vrl.v3d.STL;
import eu.mihosoft.vrl.v3d.Transform;

public class CreatureLab extends AbstractBowlerStudioTab implements IOnEngineeringUnitsChange {

	private ICadGenerator cadEngine;
	private BowlerAbstractDevice pm;
	private File openMobileBaseConfiguration;
	private File cadScript;
	private FileChangeWatcher watcher;
	private FileChangeWatcher driveEngineWitcher;
	private HashMap<DHParameterKinematics, FileChangeWatcher> dhKinematicsFileWatchers = new HashMap<>();
	private HashMap<DHParameterKinematics, FileChangeWatcher> dhCadWatchers = new HashMap<>();
	private HashMap<DHParameterKinematics, ICadGenerator> dhCadGen = new HashMap<>();
	private HashMap<DHParameterKinematics, ArrayList<CSG>> DHtoCadMap = new HashMap<>();
	private HashMap<MobileBase, ArrayList<CSG>> BasetoCadMap = new HashMap<>();

	private IDriveEngine defaultDriveEngine;
	// private DhInverseSolver defaultDHSolver;
	private Menu localMenue;
	private ProgressIndicator pi;
	private boolean cadGenerating = false;
	private boolean showingStl=false;
	private AbstractGameController gameController = new AbstractGameController() {

		@Override
		public double getNavUp() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getNavRight() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getNavLeft() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getNavDown() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls3Plus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls3Minus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls2Plus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls2Minus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls1Plus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls1Minus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls0Plus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getControls0Minus() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getActionRight() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getActionLeft() {
			// TODO Auto-generated method stub
			return 0;
		}
	};
	private ArrayList<CSG> allCad;

	@Override
	public void onTabClosing() {
		// TODO Auto-generated method stub
		if (watcher != null) {
			watcher.close();
		}
	}

	@Override
	public String[] getMyNameSpaces() {
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	public void initializeUI(BowlerAbstractDevice pm) {
		this.pm = pm;
		// TODO Auto-generated method stub
		setText(pm.getScriptingName());

		GridPane dhlabTopLevel = new GridPane();

		if (DHParameterKinematics.class.isInstance(pm)) {
			DHParameterKinematics device = (DHParameterKinematics) pm;
			try {
				setDefaultDhParameterKinematics(device);

			} catch (Exception e) {
				BowlerStudioController.highlightException(null, e);
			}
			Log.debug("Loading xml: " + device.getXml());
			dhlabTopLevel.add(new DhChainWidget(device, null), 0, 0);
		} else if (MobileBase.class.isInstance(pm)) {
			MobileBase device = (MobileBase) pm;
			Menu CreaturLabMenue = BowlerStudio.getCreatureLabMenue();
			localMenue = new Menu(pm.getScriptingName());
			MenuItem printable = new MenuItem("Generate Printable CAD");
			printable.setOnAction(event -> {
				File defaultStlDir = new File(System.getProperty("user.home") + "/bowler-workspace/STL/");
				if (!defaultStlDir.exists()) {
					defaultStlDir.mkdirs();
				}
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Select Output Directory For .STL files");

				chooser.setInitialDirectory(defaultStlDir);
				File baseDirForFiles = chooser.showDialog(BowlerStudio.getPrimaryStage());
				new Thread() {

					public void run() {

						if (baseDirForFiles == null) {
							return;
						}
						ArrayList<File> files;
						try {
							files = generateStls((MobileBase) pm, baseDirForFiles);
							Platform.runLater(() -> {
								Alert alert = new Alert(AlertType.INFORMATION);
								alert.setTitle("Stl Export Success!");
								alert.setHeaderText("Stl Export Success");
								alert.setContentText(
										"All SLT's for the Creature Generated at\n" + files.get(0).getAbsolutePath());
								alert.setWidth(500);
								alert.initModality(Modality.APPLICATION_MODAL);
								alert.show();
							});
						} catch (Exception e) {
							BowlerStudioController.highlightException(getCadScript(), e);
						}

					}
				}.start();
			});

			MenuItem updateRobotScripts = new MenuItem("Pull Scripts from Server");
			updateRobotScripts.setOnAction(event -> {
				new Thread() {

					public void run() {
						setName("Cad generation thread");
						cadEngine = null;
						defaultDriveEngine = null;
						try {
							setDefaultLinkLevelCadEngine();
							setDefaultWalkingEngine(device);
							generateCad();
						} catch (Exception e) {
							BowlerStudioController.highlightException(getCadScript(), e);
						}

					}
				}.start();
			});
			localMenue.getItems().addAll(printable);

			CreaturLabMenue.getItems().add(localMenue);
			CreaturLabMenue.setDisable(false);
			pm.addConnectionEventListener(new IDeviceConnectionEventListener() {
				@Override
				public void onDisconnect(BowlerAbstractDevice source) {
					// cleanup menues after add
					CreaturLabMenue.getItems().remove(localMenue);
					if (CreaturLabMenue.getItems().size() == 0)
						CreaturLabMenue.setDisable(true);
					BowlerStudioController.clearCSG();
				}

				@Override
				public void onConnect(BowlerAbstractDevice source) {
				}
			});

			// Button save = new Button("Save Configuration");

			setDefaultWalkingEngine(device);

			Group controls = new Group();
			Accordion advancedPanel = new Accordion();
			if (device.getDriveType() == DrivingType.WALKING) {
				TitledPane rp = new TitledPane("Walking Engine", new JogWidget(device));
				advancedPanel.getPanes().add(rp);
				advancedPanel.setExpandedPane(rp);
			}

			TreeItem<String> rootItem = new TreeItem<String>("Move Group " + device.getScriptingName());
			rootItem.setExpanded(true);
			HashMap<TreeItem<String>, Runnable> callbackMapForTreeitems = new HashMap<>();
			HashMap<TreeItem<String>, Group> widgetMapForTreeitems = new HashMap<>();

			TreeView<String> tree = new TreeView<String>(rootItem);
			MobleBaseFactory.load(device, tree, rootItem, callbackMapForTreeitems, widgetMapForTreeitems, this);

			tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
			tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Object>() {

				@Override
				public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
					@SuppressWarnings("unchecked")
					TreeItem<String> treeItem = (TreeItem<String>) newValue;
					new Thread() {
						public void run() {
							if (callbackMapForTreeitems.get(treeItem) != null) {
								callbackMapForTreeitems.get(treeItem).run();
							}
							if (widgetMapForTreeitems.get(treeItem) != null) {

								Platform.runLater(() -> {
									controls.getChildren().clear();
									controls.getChildren().add(widgetMapForTreeitems.get(treeItem));
								});
							} else {
								Platform.runLater(() -> {
									controls.getChildren().clear();
									controls.getChildren().add(advancedPanel);
								});
							}
						}
					}.start();

				}
			});
			// addAppendagePanel(device.getLegs(),"Legs",advancedPanel);
			// addAppendagePanel(device.getAppendages(),"Appandges",advancedPanel);
			// addAppendagePanel(device.getSteerable(),"Steerable",advancedPanel);
			// addAppendagePanel(device.getDrivable(),"Drivable",advancedPanel);
			HBox progress = new HBox(10);
			pi = new ProgressIndicator(0);
			progress.getChildren().addAll(new Label("Cad Progress:"), pi);
			// dhlabTopLevel.add(advancedPanel, 0, 0);
			dhlabTopLevel.add(progress, 0, 0);

			dhlabTopLevel.add(tree, 0, 1);

			dhlabTopLevel.add(controls, 1, 1);

		} else if (AbstractKinematicsNR.class.isInstance(pm)) {
			AbstractKinematicsNR device = (AbstractKinematicsNR) pm;
			dhlabTopLevel.add(new DhChainWidget(device, null), 0, 0);
		}
		generateCad();

		setContent(new ScrollPane(dhlabTopLevel));

	}

	private void setDefaultLinkLevelCadEngine() throws Exception {
		String[] cad = null;
		if (MobileBase.class.isInstance(pm)) {
			cad = ((MobileBase) pm).getCadEngine();
		} else if (DHParameterKinematics.class.isInstance(pm)) {
			DHParameterKinematics device = (DHParameterKinematics) pm;
			cad = device.getCadEngine();
		}
		if (cadEngine == null) {
			setCadEngine(cad[0], cad[1], (MobileBase) pm);
		}
	}

	private File setDefaultDhParameterKinematics(DHParameterKinematics device) {
		File code = null;
		try {
			code = ScriptingEngine.fileFromGistID(device.getDhEngine()[0], device.getDhEngine()[1]);
			DhInverseSolver defaultDHSolver = (DhInverseSolver) ScriptingEngine.inlineScriptRun(code, null,
					ShellType.GROOVY);

			if (dhKinematicsFileWatchers.get(device) != null) {
				dhKinematicsFileWatchers.get(device).close();
			}
			FileChangeWatcher w;
			try {
				w = new FileChangeWatcher(code);
				dhKinematicsFileWatchers.put(device, w);
				File c = code;
				w.addIFileChangeListener((fileThatChanged, event) -> {
					try {
						DhInverseSolver d = (DhInverseSolver) ScriptingEngine.inlineScriptRun(c, null,
								ShellType.GROOVY);
						device.setInverseSolver(d);
					} catch (Exception ex) {
						BowlerStudioController.highlightException(c, ex);
					}
				});
				w.start();
			} catch (IOException e) {
				BowlerStudioController.highlightException(code, e);
			}

			device.setInverseSolver(defaultDHSolver);
			return code;
		} catch (Exception e1) {
			BowlerStudioController.highlightException(code, e1);
		}
		return null;

	}

	private void setDefaultWalkingEngine(MobileBase device) {
		if (defaultDriveEngine == null) {
			setWalkingEngine(device.getWalkingEngine()[0], device.getWalkingEngine()[1], device);
		}
		for (DHParameterKinematics dh : device.getAllDHChains()) {
			setDefaultDhParameterKinematics(dh);
		}
	}

	public void setWalkingEngine(String gistID, String file, MobileBase device) {

		device.setWalkingEngine(new String[] { gistID, file });
		File code = null;
		try {
			code = ScriptingEngine.fileFromGistID(gistID, file);
		} catch (GitAPIException | IOException e) {
			BowlerStudioController.highlightException(code, e);
		}

		if (driveEngineWitcher != null)
			driveEngineWitcher.close();

		try {
			driveEngineWitcher = new FileChangeWatcher(code);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File c = code;
		driveEngineWitcher.addIFileChangeListener((fileThatChanged, event) -> {

			try {

				defaultDriveEngine = (IDriveEngine) ScriptingEngine.inlineScriptRun(c, null, ShellType.GROOVY);
				device.setWalkingDriveEngine(defaultDriveEngine);
			} catch (Exception ex) {
				BowlerStudioController.highlightException(c, ex);
			}

		});
		driveEngineWitcher.start();
		try {
			defaultDriveEngine = (IDriveEngine) ScriptingEngine.inlineScriptRun(c, null, ShellType.GROOVY);
			device.setWalkingDriveEngine(defaultDriveEngine);
		} catch (Exception ex) {
			BowlerStudioController.highlightException(c, ex);
		}
	}

	public void setCadEngine(String gitsId, String file, MobileBase device)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		setCadScript(ScriptingEngine.fileFromGistID(gitsId, file));
		device.setCadEngine(new String[]{gitsId,file});
	}

	public synchronized void generateCad() {
		if (cadGenerating)
			return;
		cadGenerating = true;
		// new RuntimeException().printStackTrace();
		new Thread() {
			public void run() {
				System.out.print("\r\nGenerating cad...");
				// new Exception().printStackTrace();
				ArrayList<CSG> allCad = new ArrayList<>();
				if (MobileBase.class.isInstance(pm)) {
					MobileBase device = (MobileBase) pm;
					allCad = generateBody(device, false);

				} else if (DHParameterKinematics.class.isInstance(pm)) {

					allCad = generateCad(((DHParameterKinematics) pm), false);
				}
				System.out.print("Done!\r\n");
				BowlerStudioController.setCsg(allCad,getCadScript());
				cadGenerating = false;
			}

		}.start();
	}

	public ArrayList<CSG> generateCad(DHParameterKinematics dh, boolean b) {
		ArrayList<DHLink> dhLinks = dh.getChain().getLinks();
		if (getCadScript() != null) {
			try {
				cadEngine = (ICadGenerator) ScriptingEngine.inlineFileScriptRun(getCadScript(), null);
			} catch (Exception e) {
				BowlerStudioController.highlightException(getCadScript(), e);
			}
		}
		if (cadEngine == null) {
			try {
				setDefaultLinkLevelCadEngine();
			} catch (Exception e) {
				BowlerStudioController.highlightException(getCadScript(), e);
			}
		}
		try {
			if (dhCadGen.get(dh) != null) {
				try {
					return dhCadGen.get(dh).generateCad(dh, false);
				} catch (Exception e) {
					BowlerStudioController.highlightException(dhCadWatchers.get(dh).getFileToWatch(), e);
				}
			}

			return cadEngine.generateCad(dh, false);
		} catch (Exception e) {
			BowlerStudioController.highlightException(getCadScript(), e);
		}
		return null;

	}

	@Override
	public void onTabReOpening() {
		setCadScript(getCadScript());
		try {
			generateCad();
		} catch (Exception ex) {

		}
	}

	public static String getFormatted(double value) {
		return String.format("%4.3f%n", (double) value);
	}

	public File getCadScript() {
		return cadScript;
	}

	public void setCadScript(File cadScript) {
		if (cadScript == null)
			return;
		if (watcher != null) {

			watcher.close();
		}
		try {
			watcher = new FileChangeWatcher(cadScript);
			watcher.addIFileChangeListener(new IFileChangeListener() {

				@Override
				public void onFileChange(File fileThatChanged, WatchEvent event) {
					try {
						cadEngine = (ICadGenerator) ScriptingEngine.inlineFileScriptRun(fileThatChanged, null);
						generateCad();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			watcher.start();
			pm.addConnectionEventListener(new IDeviceConnectionEventListener() {
				
				@Override
				public void onDisconnect(BowlerAbstractDevice arg0) {
					if (watcher != null) {
						watcher.close();
					}
				}
				
				@Override
				public void onConnect(BowlerAbstractDevice arg0) {}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.cadScript = cadScript;
	}

	public ArrayList<CSG> generateBody(MobileBase base, boolean b) {
		pi.setProgress(0);
		allCad = new ArrayList<>();
		//DHtoCadMap = new HashMap<>();
		//private HashMap<MobileBase, ArrayList<CSG>> BasetoCadMap = new HashMap<>();
		
		if (MobileBase.class.isInstance(pm)) {
			MobileBase device = (MobileBase) pm;
			if(BasetoCadMap.get(device)==null){
				BasetoCadMap.put(device, new ArrayList<CSG>());
			}
			
			if (cadEngine == null) {
				try {
					setDefaultLinkLevelCadEngine();
				} catch (Exception e) {
					BowlerStudioController.highlightException(null, e);
				}
				if (getCadScript() != null) {
					try {
						cadEngine = (ICadGenerator) ScriptingEngine.inlineFileScriptRun(getCadScript(), null);
					} catch (Exception e) {
						BowlerStudioController.highlightException(getCadScript(), e);
					}
				}
			}
			pi.setProgress(0.3);
			try {
				if(showingStl){
					//skip the regen
					for(CSG c:BasetoCadMap.get(device)){
						allCad.add(c);	
					}
				}else{
					allCad = cadEngine.generateBody(device, b);
					ArrayList<CSG> arrayList = BasetoCadMap.get(device);
					arrayList.clear();
					for(CSG c:allCad){
						arrayList.add(c);	
					}
				}
			} catch (Exception e) {
				BowlerStudioController.highlightException(getCadScript(), e);
			}
			// clears old robot and places base
			BowlerStudioController.setCsg(BasetoCadMap.get(device),getCadScript());

			pi.setProgress(0.4);
			ArrayList<DHParameterKinematics> limbs = base.getAllDHChains();
			double numLimbs = limbs.size();
			double i = 0;
			for (DHParameterKinematics l : limbs) {
				if(DHtoCadMap.get(l)==null){
					DHtoCadMap.put(l, new ArrayList<CSG>());
				}
				ArrayList<CSG> arrayList = DHtoCadMap.get(l);
				if(showingStl){
					for (CSG csg : arrayList) {
						allCad.add(csg);
						BowlerStudioController.addCsg(csg,getCadScript());
					}
				}else{
					arrayList.clear();
					for (CSG csg : generateCad(l, b)) {
						allCad.add(csg);
						arrayList.add(csg);
						BowlerStudioController.addCsg(csg,getCadScript());
					}
				}

				i += 1;
				double progress = (1.0 - ((numLimbs - i) / numLimbs)) / 2;
				// System.out.println(progress);
				pi.setProgress(0.5 + progress);
			}

		} else if (DHParameterKinematics.class.isInstance(pm)) {
			for (CSG csg : generateCad(((DHParameterKinematics) pm), b)) {
				allCad.add(csg);
			}
		}
		BowlerStudio.setSelectedTab(this);
		showingStl=false;
		pi.setProgress(1);
		return allCad;
	}

	public ArrayList<File> generateStls(MobileBase base, File baseDirForFiles) throws IOException {
		ArrayList<File> allCadStl = new ArrayList<>();
		int leg = 0;
		ArrayList<DHParameterKinematics> limbs = base.getAllDHChains();
		double numLimbs = limbs.size();
		double i = 0;
		// Start by generating the legs using the DH link based generator
		
		for (DHParameterKinematics l : limbs) {
			i += 1;
			double progress = (1.0 - ((numLimbs - i) / numLimbs)) / 2;
			pi.setProgress( progress);
			
			CSG legAssembly=null;
			for (CSG csg : DHtoCadMap.get(l)) {
				csg = csg.prepForManufacturing();
				if(legAssembly==null)
					legAssembly=csg;
				else{
					legAssembly = legAssembly
							.union(csg
									.movey(.5+legAssembly.getMaxY()+Math.abs(csg.getMinY()))
									)
							;
				}
//				legAssembly.setManufactuing(new PrepForManufacturing() {
//					public CSG prep(CSG arg0) {
//						return null;
//					}
//				});
			}
			File dir = new File(
					baseDirForFiles.getAbsolutePath() + "/" + 
					base.getScriptingName() + "/" + 
					l.getScriptingName());
			if (!dir.exists())
				dir.mkdirs();
			File stl = new File(dir.getAbsolutePath() + "/Leg_" + leg + ".stl");
			FileUtil.write(Paths.get(stl.getAbsolutePath()), legAssembly.toStlString());
			allCadStl.add(stl);
			BowlerStudioController.setCsg(legAssembly,getCadScript());
			leg++;
		}
		
		int link = 0;
		// now we genrate the base pieces
		for (CSG csg : BasetoCadMap.get(base)) {
			File dir = new File(baseDirForFiles.getAbsolutePath() + "/" + base.getScriptingName() + "/");
			if (!dir.exists())
				dir.mkdirs();
			File stl = new File(dir.getAbsolutePath() + "/Body_part_" + link + ".stl");
			FileUtil.write(Paths.get(stl.getAbsolutePath()), csg.prepForManufacturing().clone().toStlString());
			allCadStl.add(stl);
			link++;
		}
//		BowlerStudioController.setCsg(BasetoCadMap.get(base),getCadScript());
//		for(CSG c: DHtoCadMap.get(base.getAllDHChains().get(0))){
//			BowlerStudioController.addCsg(c,getCadScript());
//		}
		showingStl=true;
		pi.setProgress(1);
		return allCadStl;
	}

	@Override
	public void onSliderMoving(EngineeringUnitsSliderWidget source, double newAngleDegrees) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSliderDoneMoving(EngineeringUnitsSliderWidget source, double newAngleDegrees) {
		generateCad();
	}

	public AbstractGameController getController() {

		return gameController;
	}

	public void setDhEngine(String gitsId, String file, DHParameterKinematics dh) {
		dh.setDhEngine(new String[] { gitsId, file });

		setDefaultDhParameterKinematics(dh);

	}

	public void setCadEngine(String gitsId, String file, DHParameterKinematics dh)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		dh.setCadEngine(new String[]{gitsId,file});
		File code = ScriptingEngine.fileFromGistID(gitsId, file);
		try {
			ICadGenerator defaultDHSolver = (ICadGenerator) ScriptingEngine.inlineScriptRun(code, null,
					ShellType.GROOVY);
			dhCadGen.put(dh, defaultDHSolver);
		} catch (Exception e) {
			BowlerStudioController.highlightException(code, e);
		}

		if (dhCadWatchers.get(dh) != null) {
			dhCadWatchers.get(dh).close();
		}
		FileChangeWatcher w = new FileChangeWatcher(code);
		dhCadWatchers.put(dh, w);
		w.addIFileChangeListener((fileThatChanged, event) -> {
			try {
				ICadGenerator d = (ICadGenerator) ScriptingEngine.inlineScriptRun(code, null, ShellType.GROOVY);
				dhCadGen.put(dh, d);
				generateCad();
			} catch (Exception ex) {
				BowlerStudioController.highlightException(code, ex);
			}
		});
		w.start();

	}

}
