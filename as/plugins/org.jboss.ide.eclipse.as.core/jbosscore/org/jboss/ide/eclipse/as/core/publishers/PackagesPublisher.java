/**
 * JBoss, a Division of Red Hat
 * Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
* This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ide.eclipse.as.core.publishers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.model.EventLogModel;
import org.jboss.ide.eclipse.as.core.model.EventLogModel.EventLogRoot;
import org.jboss.ide.eclipse.as.core.model.EventLogModel.EventLogTreeItem;
import org.jboss.ide.eclipse.as.core.server.attributes.IDeployableServer;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.ide.eclipse.as.core.util.SimpleTreeItem;
import org.jboss.ide.eclipse.packages.core.model.IPackage;
import org.jboss.ide.eclipse.packages.core.model.IPackagesBuildListener;
import org.jboss.ide.eclipse.packages.core.model.PackagesCore;

/**
 *
 * @author rob.stryker@jboss.com
 */
public class PackagesPublisher implements IJBossServerPublisher {
	
	// Event Constants
	public static final String REMOVE_TOP_EVENT = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.REMOVE_TOP_EVENT";
	public static final String REMOVE_PACKAGE_SUCCESS = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.REMOVE_PACKAGE_SUCCESS";
	public static final String REMOVE_PACKAGE_FAIL = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.REMOVE_PACKAGE_FAIL";
	public static final String REMOVE_PACKAGE_SKIPPED = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.REMOVE_PACKAGE_SKIPPED";
	

	public static final String PUBLISH_TOP_EVENT = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.PUBLISH_TOP_EVENT";
	public static final String PROJECT_BUILD_EVENT = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.PROJECT_BUILD_EVENT";
	public static final String MOVE_PACKAGE_SUCCESS = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.MOVE_PACKAGE_SUCCESS";
	public static final String MOVE_PACKAGE_FAIL = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.MOVE_PACKAGE_FAIL";
	public static final String MOVE_PACKAGE_SKIP = "org.jboss.ide.eclipse.as.core.publishers.PackagesPublisher.MOVE_PACKAGE_SKIP";
	
	protected IDeployableServer server;
	protected EventLogRoot eventRoot;
	
	public PackagesPublisher(IDeployableServer server) {
		this.server = server;
		eventRoot = EventLogModel.getModel(server.getServer()).getRoot();
	}
	public int getPublishState() {
		return IServer.PUBLISH_STATE_NONE;
	}

    public void publishModule(int kind, int deltaKind, int modulePublishState, 
    		IModule[] module, IProgressMonitor monitor) throws CoreException {
		
    	// if it's being removed
    	if( deltaKind == ServerBehaviourDelegate.REMOVED ) {
    		removeModule(module, monitor);
    		return;
    	}
    	
    	if( deltaKind == ServerBehaviourDelegate.ADDED || deltaKind == ServerBehaviourDelegate.CHANGED) {
    		boolean incremental = (kind == IServer.PUBLISH_INCREMENTAL);
    		publishModule(incremental, module, monitor);
    		return;
    	}
    	
    	if( kind == IServer.PUBLISH_INCREMENTAL ) {
    		boolean incremental = false;
    		if( modulePublishState == IServer.PUBLISH_STATE_NONE ) incremental = false;
    		if( modulePublishState == IServer.PUBLISH_STATE_INCREMENTAL ) incremental = true;
    		publishModule(incremental, module, monitor);
    		return;
    	}
    	
    	if( kind == IServer.PUBLISH_FULL ) {
    		publishModule(false, module, monitor);
    		return;
    	}
    	
	}

	protected void removeModule(IModule[] module, IProgressMonitor monitor) {
		
		// remove all of the deployed items
		PublishEvent event = new PublishEvent(eventRoot, REMOVE_TOP_EVENT, module[0]);
		EventLogModel.markChanged(eventRoot);
		
		String projectName = module[0].getName();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if( project.exists() ) {
			IPackage[] packages = PackagesCore.getProjectPackages(project, new NullProgressMonitor());
			for( int i = 0; i < packages.length; i++ ) {
				
				try {
				
					if( packages[i].isDestinationInWorkspace()) {
						IFile file = packages[i].getPackageFile();
						IPath sourcePath = file.getLocation();
						IPath destPath = new Path(server.getDeployDirectory()).append(sourcePath.lastSegment());
						boolean success = destPath.toFile().delete();
						addRemoveEvent(event, packages[i], destPath, success ? SUCCESS : FAILURE);
					} else {
						IPath path = packages[i].getPackageFilePath();
						Path deployDir = new Path(server.getDeployDirectory());
						if( path.toOSString().startsWith(deployDir.toOSString())) {
							boolean success = path.toFile().delete();
							addRemoveEvent(event, packages[i], path, success ? SUCCESS : FAILURE);
						} else {
							addRemoveEvent(event, packages[i], path, SKIPPED, null);
						}
					}
				} catch( Exception e ) {
					IPath dest = packages[i].getPackageFile().getLocation();
					addRemoveEvent(event, packages[i], dest, FAILURE, e);
				}
			}
		}
	}
	

	
	protected void publishModule(boolean incremental, IModule[] module, IProgressMonitor monitor) {
		PublishEvent event = new PublishEvent(eventRoot, PUBLISH_TOP_EVENT, module[0]);
		EventLogModel.markChanged(eventRoot);

		int inc2 = incremental ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD;
		String projectName = module[0].getName();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			addBuildEvent(event, project, inc2);
			PackagesCore.buildProject(project, inc2, monitor);
			IPackage[] packages = PackagesCore.getProjectPackages(project, new NullProgressMonitor());
			for( int i = 0; i < packages.length; i++ ) {
				if( packages[i].isDestinationInWorkspace()) {
					// destination is workspace. Move it. 
					IFile file = packages[i].getPackageFile();
					IPath sourcePath = file.getLocation();
					IPath destPath = new Path(server.getDeployDirectory()).append(sourcePath.lastSegment());
					IStatus status = FileUtil.copyFile(sourcePath.toFile(), destPath.toFile());
					addMoveEvent(event, packages[i], packages[i].isDestinationInWorkspace(), sourcePath, destPath, status.isOK() ? SUCCESS : FAILURE, status.getException());
				} else {					
					IPath sourcePath = packages[i].getPackageFilePath();
					addMoveEvent(event, packages[i], packages[i].isDestinationInWorkspace(), sourcePath, sourcePath, SUCCESS, null);
				}
			}
		}catch( Exception ce ) {
			ce.printStackTrace();
		}
	}
	
	protected void addBuildEvent(PublishEvent parent, IProject project, int incremental ) {
		EventLogModel.markChanged(parent);
	}
	protected void addMoveEvent(PublishEvent parent, IPackage pack, boolean inWorkspace, 
			IPath sourcePath, IPath destPath, int success, Throwable e) {
		String specType = null;
		switch( success ) {
			case SUCCESS: specType = MOVE_PACKAGE_SUCCESS; break;
			case FAILURE: specType = MOVE_PACKAGE_FAIL; break;
			case SKIPPED: specType = MOVE_PACKAGE_SKIP; break;
		}
		new PackagesPublisherMoveEvent(parent, specType, pack, sourcePath, destPath, e );
		EventLogModel.markChanged(parent);
	}
	
	
	
	protected void addRemoveEvent(PublishEvent parent, IPackage pack, IPath dest, int success ) {
		addRemoveEvent(parent, pack, dest, success, null);	
	}
	protected void addRemoveEvent(PublishEvent parent, IPackage pack, IPath dest, int success, Exception e ) {
		String specType = null;
		switch( success ) {
			case SUCCESS: specType = REMOVE_PACKAGE_SUCCESS; break;
			case FAILURE: specType = REMOVE_PACKAGE_FAIL; break;
			case SKIPPED: specType = REMOVE_PACKAGE_SKIPPED; break;
		}
		PackagesPublisherRemoveEvent event = 
			new PackagesPublisherRemoveEvent(parent, specType, pack, dest, e);
		EventLogModel.markChanged(parent);
	}
	
	public static class PackagesPublisherRemoveEvent extends EventLogTreeItem {
		// property names
		public static final String PACKAGE_NAME = "PackagesPublisherRemoveEvent.PACKAGE_NAME";
		public static final String DESTINATION = "PackagesPublisherRemoveEvent.DESTINATION";
		public static final String EXCEPTION_MESSAGE = "PackagesPublisherRemoveEvent.EXCEPTION_MESSAGE";
		
		
		public PackagesPublisherRemoveEvent(SimpleTreeItem parent, String specificType,
				IPackage pack, IPath dest, Exception e ) {
			super(parent, PUBLISH_MAJOR_TYPE, specificType);
			setProperty(PACKAGE_NAME, pack.getName());
			setProperty(DESTINATION, dest.toOSString());
			if( e != null )
				setProperty(EXCEPTION_MESSAGE, e.getLocalizedMessage());
		}
	}	
	public static class PackagesPublisherMoveEvent extends EventLogTreeItem {
		// property names
		public static final String PACKAGE_NAME = "PackagesPublisherRemoveEvent.PACKAGE_NAME";
		public static final String MOVE_DESTINATION = "PackagesPublisherMoveEvent.MOVE_DESTINATION";
		public static final String MOVE_SOURCE = "PackagesPublisherMoveEvent.MOVE_SOURCE";
		public static final String EXCEPTION_MESSAGE = "PackagesPublisherRemoveEvent.EXCEPTION_MESSAGE";
		
		
		public PackagesPublisherMoveEvent(SimpleTreeItem parent, String specificType,
				IPackage pack, IPath source, IPath dest, Throwable e ) {
			super(parent, PUBLISH_MAJOR_TYPE, specificType);
			setProperty(PACKAGE_NAME, pack.getName());
			setProperty(MOVE_SOURCE, source.toOSString());
			setProperty(MOVE_DESTINATION, dest.toOSString());
			if( e != null )
				setProperty(EXCEPTION_MESSAGE, e.getLocalizedMessage());
		}
	}	
	public static class PackagesPublisherBuildEvent extends EventLogTreeItem {
		// property names
		public static final String BUILD_TYPE = "PackagesPublisherRemoveEvent.BUILD_TYPE";
		
		public PackagesPublisherBuildEvent(SimpleTreeItem parent, String specificType, int type) {
			super(parent, PUBLISH_MAJOR_TYPE, specificType);
			setProperty(BUILD_TYPE, new Integer(type));
		}
	}
}
