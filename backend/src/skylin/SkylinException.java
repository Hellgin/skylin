package skylin;


class SkylinException extends RuntimeException
{
      public SkylinException() {}

      
      public SkylinException (AM am,String message)
      {
    	  super (message);
    	  am.fatal(message);
      }

      public SkylinException (Throwable cause)
      {
    	  super (cause);
      }

      public SkylinException (String message, Throwable cause)
      {
    	  super (message, cause);
      }
 }