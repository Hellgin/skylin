package com.distantmelody.framework.skylin.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.distantmelody.framework.skylin.SkylinClassLoader;

public class ServiceClassLoader extends ClassLoader{
	ServiceContainer amContainer;
    public ServiceClassLoader(ServiceContainer amContainer) {    
        this.amContainer = amContainer;
    }

    @Override
    public synchronized Class<?> loadClass(String name) 
            throws ClassNotFoundException {

    	//if ((!name.startsWith("com.pepkor") || name.startsWith("com.pepkor.core")) && !name.equals("skylin.util.ServiceUtil") && !name.startsWith("com.google.gson"))
    	//if (name.startsWith("skylin.") || name.startsWith("servicecontainers.") || name.startsWith("com.sun.") || name.startsWith("sun.") || name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("com.google.gson"))
    	block:
    	{
	    	for (String p:SkylinClassLoader.applicationPackages)
	    	{
	    		if (name.startsWith(p))
	    		{
	    			break block;
	    		} 			
	    	}
    		return super.loadClass(name);
    	}
    	
        Class c = findClass(name);
        String path = getPath(name);
        amContainer.classes.add(new ServiceClass(c,new File(path).lastModified(),path));
        return c;
    }
    
    public Class findClass(String name) throws ClassNotFoundException {
    	
    	File file = new File(getPath(name)); 
    	Path path = Paths.get(file.getAbsolutePath()); 
    	byte[] data;
		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		} 

    	return defineClass(name,data,0,data.length);
    }
    
    private String getPath(String name)
    {
    	return "bin/"+name.replace(".", "/")+".class";
    }
}
