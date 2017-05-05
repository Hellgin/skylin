package skylin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import skylin.util.Util;

public class SkylinClassLoader extends ClassLoader{
	
	public static String[] applicationPackages;
	
	AMContainer amContainer;
    public SkylinClassLoader(AMContainer amContainer) {    
        this.amContainer = amContainer;
    }

    @Override
    public synchronized Class<?> loadClass(String name) 
            throws ClassNotFoundException {

    	//if ((!name.startsWith("com.pepkor") || name.startsWith("com.pepkor.core")) && !name.equals("skylin.util.ServiceUtil") && !name.startsWith("com.google.gson"))
    	//if (name.startsWith("skylin.") || name.startsWith("servicecontainers.") || name.startsWith("com.sun.") || name.startsWith("sun.") || name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("com.google.gson"))
    	loop:
    	for (String p:applicationPackages)
    	{
    		if (name.startsWith(p))
    		{
    			break loop;
    		} 			
    		return super.loadClass(name);
    	}
        Class c = findClass(name);
        String path = getPath(name);
        amContainer.classes.add(new AMClass(c,new File(path).lastModified(),path));
        return c;
    }
    
    public Class findClass(String name) {
    	
    	File file = new File(getPath(name)); 
    	Path path = Paths.get(file.getAbsolutePath()); 
    	byte[] data = null;
		try {
			data = Files.readAllBytes(path);
			//data = readFile("test/"+name.replaceAll(".", "/"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

    	return defineClass(name,data,0,data.length);
    }
    
    private String getPath(String name)
    {
    	return "bin/"+name.replace(".", "/")+".class";
    }

	public static void setApplicationPaths() {
		String path = Util.getFrameworkParam("APPLICATION_PACKAGES");
		if (path == null)
		{
			//--------------------------------------------- temp code until APPLICATION_PACKAGES parameter is added to all environments
			try
			{
				applicationPackages = new String[]{"com.pepkor."};
			}
			catch (Exception e){e.printStackTrace();};
			//----------------------------------------------
			
			return;
		}
		
		applicationPackages = path.split(",");
		for (int i = 0; i < applicationPackages.length;i++)
		{
			applicationPackages[i]+=".";
		}
	}
}
