package com.dotmarketing.osgi;

import java.util.Arrays;

import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import com.dotcms.repackage.felix_4_2_1.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.felix_4_2_1.org.osgi.framework.BundleContext;
import com.dotcms.repackage.felix_4_2_1.org.osgi.framework.ServiceEvent;
import com.dotcms.repackage.felix_4_2_1.org.osgi.framework.ServiceListener;
import com.dotcms.repackage.felix_4_2_1.org.osgi.framework.ServiceReference;

import com.dotmarketing.util.Logger;

/**
 * @see GenericBundleActivator
 * @see GenericBundleActivator#registerViewToolService(com.dotcms.repackage.felix_4_2_1.org.osgi.framework.BundleContext, org.apache.velocity.tools.view.ToolInfo)
 * @deprecated Class initially used to register ViewTool objects from and OSGI plugin, now you should use the {@link GenericBundleActivator} class instead of this class
 *             to register those ViewTool objects using the method {@link GenericBundleActivator#registerViewToolService(com.dotcms.repackage.felix_4_2_1.org.osgi.framework.BundleContext, org.apache.velocity.tools.view.ToolInfo)}.
 */
public class AbstractViewToolActivator implements BundleActivator, ServiceListener {
	
	private PrimitiveToolboxManager tm;
	
	private BundleContext context;
	
	private ToolInfo info;
	
	public AbstractViewToolActivator(ToolInfo info) {
		this.info = info;
	}
	
	public void start(BundleContext context) throws Exception {
		
		// Save OSGI context
		this.context = context;
		
		// Try to register to ViewTool service
		doRegister();

		// Register itself as listener for service adding/removal
		context.addServiceListener(this);
		
	}

	public void stop(BundleContext context) throws Exception {
		
		// Try to remove ViewTool
		unregister();
		
		// Remove itselt as listener
		context.removeServiceListener(this);

	}
	
	private void doRegister() {
		
		ServiceReference serviceRefSelected = context.getServiceReference(PrimitiveToolboxManager.class.getName());

		if ( serviceRefSelected == null )
			return;
		
		Object service = context.getService(serviceRefSelected);
		this.tm = (PrimitiveToolboxManager) service;
		register();
		
	}
		
	private void register() {
		tm.addTool(info);
	    Logger.info(this,"Added View Tool: " + info.getKey());
	}
	
	private void unregister() {
		if ( tm != null ) {
			tm.removeTool(info);
		}
		Logger.info(this,"Removed View Tool: " + info.getKey());
	}

	public void serviceChanged(ServiceEvent serviceEvent) {
		
		String[] objectClass = (String[]) serviceEvent.getServiceReference().getProperty("objectClass");

		if ( objectClass == null )
			return;
		
		if ( objectClass.length == 0 )
			return;
		
		if (!Arrays.asList(objectClass).contains(PrimitiveToolboxManager.class.getName()))
			return;

		switch (serviceEvent.getType()) {
			
			case ServiceEvent.MODIFIED:
				unregister();
				doRegister();
				break;

			case ServiceEvent.MODIFIED_ENDMATCH:
				break;

			case ServiceEvent.REGISTERED:
				doRegister();
				break;
				
			case ServiceEvent.UNREGISTERING:
				unregister();
				break;
		}
				
	}

}
