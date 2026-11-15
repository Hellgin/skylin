package com.distantmelody.framework.skylin.security;

import com.distantmelody.framework.skylin.SecurityContext;
import com.distantmelody.framework.skylin.bridge.SkylinBridgeVirtualMethods;

public class QuickRoleLookup implements SkylinBridgeVirtualMethods{

	SecurityContext securityContext;
	
	public QuickRoleLookup(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public Object handleVirtualMethodCall(String name, Object[] params, boolean[] matchFound) {
		matchFound[0] = true;
		return securityContext.userInRole(name);
	}

}
