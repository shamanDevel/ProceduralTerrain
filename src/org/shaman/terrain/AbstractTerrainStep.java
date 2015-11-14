/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 *
 * @author Sebastian Weiss
 */
public abstract class AbstractTerrainStep implements AppState {
	
	public static final String KEY_HEIGHTMAP = "heightmap";
	
	protected AppStateManager stateManager;
	protected TerrainHeighmapCreator app;
	protected Node sceneNode;
	protected Node guiNode;
	private boolean initialized = false;
	private boolean enabled = false;
	
	protected Map<Object, Object> properties;
	
	@Override
	public final void initialize(AppStateManager stateManager, Application app) {
		this.stateManager = stateManager;
		this.app = (TerrainHeighmapCreator) app;
		this.sceneNode = new Node(getClass().getSimpleName()+"3D");
		this.guiNode = new Node(getClass().getSimpleName()+"Gui");
		initialized = true;
		if (enabled) {
			//force initialization
			enabled = false;
			setEnabled(true);
		}
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void setEnabled(boolean active) {
		if (!initialized) {
			this.enabled = true;
			return;
		}
		if (active && !enabled) {
			enabled = true;
			app.getRootNode().attachChild(sceneNode);
			app.getGuiNode().attachChild(guiNode);
			enable();
		} else if (!active && enabled) {
			enabled = false;
			app.getRootNode().detachChild(sceneNode);
			app.getGuiNode().detachChild(guiNode);
			disable();
		}
	}
	
	protected abstract void enable();
	protected abstract void disable();
	
	protected final boolean nextStep(Class<? extends AbstractTerrainStep> nextStepClass, Map<Object, Object> properties) {
		final AbstractTerrainStep next = stateManager.getState(nextStepClass);
		if (next==null) {
			return false;
		} else {
			next.properties = properties;
			setEnabled(false);
			app.enqueue(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					next.setEnabled(true);
					return null;
				}
			});
		}
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void stateAttached(AppStateManager stateManager) {}

	@Override
	public void stateDetached(AppStateManager stateManager) {}

	@Override
	public void render(RenderManager rm) {}

	@Override
	public void postRender() {	}

	@Override
	public void cleanup() {}
	
}
