package org.codehaus.mojo.natives.plugin;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.artifact.Artifact;

import org.codehaus.mojo.natives.NativeBuildException;
import org.codehaus.mojo.natives.javah.Javah;
import org.codehaus.mojo.natives.javah.JavahConfiguration;
import org.codehaus.mojo.natives.manager.JavahManager;
import org.codehaus.mojo.natives.manager.NoSuchNativeProviderException;
import org.codehaus.plexus.util.FileUtils;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.io.IOException;
import java.io.File;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * @goal javah
 * @description generate jni include files
 * @phase generate-sources
 * @author <a href="dantran@gmail.com">Dan T. Tran</a>
 * @version $Id:$
 * @requiresDependencyResolution compile
 */

public class NativeJavahMojo
    extends AbstractNativeMojo
{
    /**
     * @parameter default-value="sun"
     * @required
     * @description Javah Provider Type
     */
    private String javahType;	
	
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.directory}/native/javah" 
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter default-value="false"
     * @optional
     */

    private boolean verboseJavah;
    
    /**
     * @parameter expression="${component.org.codehaus.mojo.natives.manager.JavahManager}"
     * @required
     */

    private JavahManager manager;
    
    public void execute()
        throws MojoExecutionException
    {
    	if ( ! this.outputDirectory.exists() )
    	{
    		this.outputDirectory.mkdirs();
    	}

    	JavahConfiguration config = new JavahConfiguration();
    	config.setVerbose( this.verboseJavah );
    	config.setDestDir( this.outputDirectory );
    	config.setClassPath( this.getJavahClassPath() );
    	config.setClassNames( this.getNativeClassNames() );
    	
    	try
    	{
       	    Javah javah = this.manager.getJavah( this.javahType );
       	     		
            javah.compile( config );
    	}
    	catch ( NoSuchNativeProviderException pe )
    	{
    		throw new MojoExecutionException( pe.getMessage() );
    	}    	
    	catch ( NativeBuildException e )
    	{
    		throw new MojoExecutionException( "Error running javah command", e );
    	}
    	
    	this.project.addCompileSourceRoot( this.outputDirectory.getAbsolutePath() );
    	
    }
    
    private List getJavahArtifacts()
    {
        List list = new ArrayList();
        
        Set artifacts = this.project.getDependencyArtifacts();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
        	
            // TODO: utilise appropriate methods from project builder
            // TODO: scope handler
            // Include runtime and compile time libraries
            if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) &&
                !Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
            {
            	list.add( artifact );
            }
        }
        
        return list;
    }

    private String getJavahClassPath()
    {
    	StringBuffer buffer = new StringBuffer();
    	
        List artifacts = this.getJavahArtifacts();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            
     		if ( buffer.length() != 0 )
     		{
     			buffer.append(",");
   		    }
			
     		buffer.append( artifact.getFile().getPath() );
        }
        
        return buffer.toString();
    	
    }
    
    
	/**
	 * 
	 * Get appliable class names to be "javahed"
	 * 
     */
 
    private String getNativeClassNames() 
        throws MojoExecutionException
    {
    	// store classnames separated by comas
		StringBuffer classes = new StringBuffer();

        List artifacts = this.getJavahArtifacts();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();

        	this.getLog().info("Parsing " + artifact.getFile() + " for native classes." );
            
           	try 
           	{
           		Enumeration zipEntries  = new ZipFile( artifact.getFile() ).entries();
           		
           		while ( zipEntries.hasMoreElements() )
           		{
           			ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
            			
           			if ( "class".equals( FileUtils.extension( zipEntry.getName() ) ) )
           			{
           	            ClassParser parser = new ClassParser( artifact.getFile().getPath(), zipEntry.getName() );
            	            
           	    		JavaClass clazz  = parser.parse();

           	    		Method [] methods = clazz.getMethods();
            	    		
           	    		for ( int j = 0; j < methods.length; ++j )
           	    		{
           	    			if ( methods[j].isNative() )
           	    			{
               	    			if ( classes.length() != 0 )
               	    			{
               	    				classes.append(",");
               	    			}
            	    			
           	        			classes.append( clazz.getClassName() );

           	    	        	this.getLog().info("Found native class: " + clazz.getClassName() );
           	        			
           	        			break;
           	    			}
           	    		}
           			}
           		}//endwhile
            }
            catch ( IOException ioe )
            {
            	throw new MojoExecutionException( "Error searching for native class in dependencies", ioe );
            }
        }
    	
    	return classes.toString();
    }
    
}
