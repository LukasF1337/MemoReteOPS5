package main;

import java.util.Collections;

import com.google.common.collect.Sets;
import com.google.devtools.common.options.*;

import emo.ifs.ecar.ECar;
import emo.ifs.ecar.ECarDefines;
import ops5.Controller;

/**
 * OPS5 interpreter with a robot interface (ECar). Manuals here TODO .
 *
 */
public class AppMain {
	public static void main(String[] args) throws Exception {
		checkJavaVersion();

		OptionsParser optionsParser = OptionsParser.newOptionsParser(AppOptions.class);
		optionsParser.parseAndExitUponError(args);
		AppOptions options = optionsParser.getOptions(AppOptions.class);
		if (!options.filePaths.isEmpty()) {
			System.out.println("Multiple filepaths not implemented yet. Can only run one OPS5 program at a time.");
			printUsage(optionsParser);
			return;
		}
		if (options.filePath.isEmpty()) {
			System.out.println("Filepath of ops5 program not specified.");
			printUsage(optionsParser);
			return;
		}
		if (options.helpInteractive) {
			Controller.printHelpInteractiveCommands();
			return;
		}

		Controller ctrl = new Controller(options.filePath);
		ctrl.run(options.runNow);

//		ECar ecar = new ECar("SIM_DX1", ECarDefines.JWORLD);
//		
//		ecar.connect();
//		ecar.speed(200);
//		ecar.rotate(30);
//		ecar.wait(100);
//		ecar.speed(0);
//		ecar.rotate(0);
//		
//		int d3 = ecar.getSonarRange(3);
//		int d4 = ecar.getSonarRange(4);
//		System.out.println("Abstand nach vorne = " + d3 + " und " + d4);
//		
//		int[] laserRanges = ecar.getLaserRanges();
//		System.out.print("Laser ranges:");
//		for(int i=0; i<laserRanges.length; i++)
//			System.out.print(" " + laserRanges[i]);
//		
//		ecar.disconnect();
		System.exit(0);
	}

	private static void printUsage(OptionsParser parser) {
		System.out.println("Usage: java -jar app.jar OPTIONS");
		System.out.println(
				parser.describeOptions(Collections.<String, String>emptyMap(), OptionsParser.HelpVerbosity.LONG));
	}

	private static void checkJavaVersion() {
		String version = System.getProperty("java.version");
		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}
		Integer v = Integer.parseInt(version);
		if (v < 21) {
			throw new RuntimeException("Java version is " + v + ". Need at least Java version 21.");
		}
	}
}
