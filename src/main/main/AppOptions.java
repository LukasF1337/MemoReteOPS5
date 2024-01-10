package main;

import java.util.List;

import com.google.devtools.common.options.*;

public class AppOptions extends OptionsBase {

	@Option(//
			name = "runNow", //
			abbrev = 'r', //
			help = "Run program immediately or open interactive command line mode.", //
			category = "startup", //
			defaultValue = "true"//
	)
	public boolean runNow;

	@Option(//
			name = "helpInteractive", //
			abbrev = 'h', //
			help = "Print commands for interactive mode", //
			defaultValue = "false" //
	)
	public boolean helpInteractive;

	@Option(//
			name = "file", //
			abbrev = 'f', //
			help = "OPS5 program file path", //
			category = "startup", //
			defaultValue = "" //
	)
	public String filePath;

	@Option(name = "files", //
			abbrev = 's', //
			help = "Names of multiple directories to serve static files.", //
			category = "startup", //
			allowMultiple = true, //
			defaultValue = "" //
	)
	public List<String> filePaths;

}
