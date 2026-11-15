package com.distantmelody.framework.skylin.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class SplitPrintStream extends PrintStream{

	private PrintStream second;

	public SplitPrintStream(OutputStream main,PrintStream second) {
		super(main);
		this.second = second;
		writeByteToPrimary('\n');
	}
	
	
	  @Override
      public void flush() {
          super.flush();
          second.flush();
      }

      @Override
      public void write(byte[] buf, int off, int len) {
          //super.write(buf, off, len);
    	  for (int i = off; i < off+len;i++)
    	  {
    		  writeByteToPrimary(buf[i]);
    	  }
          second.write(buf, off, len);
    	  

      }

      @Override
      public void write(int b) {
    	  writeByteToPrimary(b);
          second.write(b);
          


      }
      
      private void writeByteToPrimary(int b)
      {
    	  super.write(b);
          if (b == '\n')
          {

        	  
        	  String text = System.currentTimeMillis()+":";
        	  for (int i = 0; i < text.length();i++)
        	  {
        		  super.write(text.charAt(i));
        	  }
          }
      }

      @Override
      public void write(byte[] b) throws IOException {
          //super.write(b);
    	  for (byte bb:b)writeByteToPrimary(bb);
          second.write(b);

      }
}
