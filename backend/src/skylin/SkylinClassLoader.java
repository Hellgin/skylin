package skylin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SkylinClassLoader extends ClassLoader{
	AMContainer amContainer;
    public SkylinClassLoader(AMContainer amContainer) {    
        this.amContainer = amContainer;
    }

    @Override
    public synchronized Class<?> loadClass(String name) 
            throws ClassNotFoundException {

    	//if ((!name.startsWith("com.pepkor") || name.startsWith("com.pepkor.core")) && !name.equals("skylin.util.ServiceUtil") && !name.startsWith("com.google.gson"))
    	if (!name.startsWith("com.pepkor") || name.startsWith("com.pepkor.core") || name.startsWith("servicecontainers"))
    	{
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
}
