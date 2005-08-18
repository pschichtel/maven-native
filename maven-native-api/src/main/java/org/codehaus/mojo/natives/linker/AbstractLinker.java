package org.codehaus.mojo.natives.linker;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.mojo.natives.NativeBuildException;
import org.codehaus.mojo.natives.util.CommandLineUtil;
import org.codehaus.mojo.natives.util.FileSet;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;


public abstract class AbstractLinker
    extends AbstractLogEnabled
    implements Linker 
{
	
	protected abstract Commandline createLinkerCommandLine( List objectFiles, LinkerConfiguration config );
	
	public List link ( LinkerConfiguration config, FileSet sources )
    throws NativeBuildException, IOException
    {
		File [] sourceFiles = sources.getFiles();
	
		List objectFiles = new ArrayList( sourceFiles.length );
	
		for ( int i = 0 ; i < sourceFiles.length; ++i )
		{
			objectFiles.add( this.getObjectFile( sourceFiles[i], config ));
		}		
	
		Commandline cl = this.createLinkerCommandLine( objectFiles, config );
	
		CommandLineUtil.execute( cl, this.getLogger() );
	
		return null;
    }
	
	/**
	 * Figure out the object file path from a given source file
	 * @param sourceFile
	 * @return
	 */
	private File getObjectFile ( File sourceFile, LinkerConfiguration config )
	{
		String fileName = sourceFile.getName();
		
		String fileNameWithNoExtension = FileUtils.removeExtension( fileName );
		
		return new File ( config.getOutputDirectory().getPath() + 
				          "/" +
				          fileNameWithNoExtension +
				          "." + 
				          config.getObjectFileExtention() 
				         );	
	}	
	
}
