/*******************************************************************************
 * Copyright (c) 2006 Jeff Mesnil
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Rob Stryker" <rob.stryker@redhat.com> - Initial implementation
 *******************************************************************************/
package org.jboss.tools.jmx.ui.internal.views.navigator;


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.LinkHelperService;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jboss.tools.jmx.ui.Messages;
import org.jboss.tools.jmx.ui.internal.actions.NewConnectionAction;

/**
 * The view itself
 */
public class JMXNavigator extends CommonNavigator implements ITabbedPropertySheetPageContributor {
	public static final String VIEW_ID = "org.jboss.tools.jmx.ui.internal.views.navigator.MBeanExplorer"; //$NON-NLS-1$
	private Text filterText;
	private QueryContribution query;
	
	public JMXNavigator() {
		super();
	}
	protected IAdaptable getInitialInput() {
		return this;
	}
	public void createPartControl(Composite aParent) {
		fillActionBars();
		Composite newParent = new Composite(aParent, SWT.NONE);
		newParent.setLayout(new FormLayout());
		super.createPartControl(newParent);
		filterText = new Text(newParent, SWT.SINGLE | SWT.BORDER );
		
		// layout the two objects
		FormData fd = new FormData();
		fd.left = new FormAttachment(0,5);
		fd.right = new FormAttachment(100,-5);
		fd.top = new FormAttachment(0,5);
		filterText.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(filterText, 5);
		fd.bottom = new FormAttachment(100,0);
		getCommonViewer().getTree().setLayoutData(fd);
		
		filterText.setToolTipText(Messages.TypeInAFilter); 
		filterText.setText(Messages.TypeInAFilter);
		
		Display.getDefault().asyncExec(new Runnable() { 
			public void run() {
				query = new QueryContribution(JMXNavigator.this);
			}
		});
	}

	public Text getFilterText() {
		return filterText;
	}
	
	public synchronized LinkHelperService getLinkHelperService() {
		return super.getLinkHelperService();
	}

	
	public void fillActionBars() {
//		queryContribution = new QueryContribution(this);
//	    getViewSite().getActionBars().getToolBarManager().add(queryContribution);
	    getViewSite().getActionBars().getToolBarManager().add(new NewConnectionAction());
	    getViewSite().getActionBars().getToolBarManager().add(new Separator());
	    getViewSite().getActionBars().updateActionBars();
	}
	
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if (adapter == IPropertySheetPage.class) {
                return new TabbedPropertySheetPage(this);
        }
        return super.getAdapter(adapter);
    }
    public String getContributorId() {
        return VIEW_ID;
    }
}
