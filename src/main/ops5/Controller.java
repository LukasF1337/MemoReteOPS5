package ops5;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

import ops5.parser.Parser;

/**
 * Controller handles OPS5 like programs including parsing and execution.
 */
public class Controller {
	private boolean debugActive = false;
	final private Parser parser = new Parser();
	final public ModelRete modelRete = new ModelRete();

	public Controller() throws Exception {
		throw new IllegalStateException();
	}

	public Controller(String filepath) throws Exception {
		super();
		loadFile(filepath);
	}

	public void loadFile(String filepath) throws Exception {
		parser.parseOps5File(filepath);
		modelRete.addKnowledge(parser);
		parser.unlinkFacts(); // only keep literals and production rules in the model
	}

	public void addKnowledge(String OPS5Knowledge) throws Exception {
		parser.parseOps5String(OPS5Knowledge);
		modelRete.addKnowledge(parser);
		parser.unlinkFacts(); // only keep literals and production rules in the model
	}

	public boolean executeRecognizeActCycle() throws Exception {
		return this.modelRete.executeStep();
	}

	/**
	 * Run until no more rules fire or a halt is called.
	 */
	public void run(boolean runNow) throws Exception {
		String line;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		if (runNow) {
			line = "run";
		} else {
			line = reader.readLine();
			line = line.trim();
			line = line.toLowerCase();
		}

		// TODO https://www.cs.gordon.edu/local/courses/cs323/OPS5/ops5.html
		boolean programActive = true;
		while (programActive) {
			if (line.startsWith("run")) {
				if (line.equals("run")) {
					while (executeRecognizeActCycle()) {
					}
					if (runNow) { // do not use interactive mode, only run program
						break;
					}
				} else {
					String[] parts = line.split(" ", 2);
					if (!parts[0].equals("run")) {
						throw new IllegalArgumentException(line);
					}
					Integer numOfRuns = Integer.parseInt(parts[1]);
					for (int i = 0; i < numOfRuns; i++) {
						final boolean res = executeRecognizeActCycle();
						if (!res) {
							break;
						}
					}
				}
			} else if (line.startsWith("watch")) {
				String[] parts = line.split(" ", 2);
				if (!parts[0].equals("watch")) {
					throw new IllegalArgumentException(line);
				}
				try {
					Integer watchlevel = Integer.parseInt(parts[1]);
					this.modelRete.setWatchLevel(watchlevel);
				} catch (Exception e) {
					throw new IllegalArgumentException(line);
				}
			} else if (line.startsWith("cs")) {
				System.out.println(this.modelRete.printConflictSet());
			} else if (line.startsWith("wm")) {
				System.out.println(this.modelRete.printWorkingMemory());
			} else if (line.startsWith("matches")) {
				System.out.println("matches \033[3mrule\033[0m not implemented yet");
			} else if (line.equals("help")) {
				printHelpInteractiveCommands();
			} else if (line.equals("exit")) {
				programActive = false;
				break;
			}
			line = reader.readLine();
			line = line.trim();
			line = line.toLowerCase();
		}
	}

	public static void printHelpInteractiveCommands() {
		System.out.println("Commands you can enter in interactive command line mode"
				+ " (program started with OPTION: \"-r false\" or the equivalent \"--norunNow\"):");
		System.out.println("\"run \033[3mn\033[0m\": 	go forward n cycles, or until halted if no n is given");
		System.out.print("\"watch \033[3mn\033[0m\":");
		System.out.println("	0: no tracing");
		System.out.println("		1: display productions executed with time tags");
		System.out.println("\"cs\": 		show current conflict set");
		System.out.println("\"matches \033[3mrule\033[0m\": " + //
				"for each condition element of the rule, show Facts that match");
		System.out.println("\"help\": 	command to print this help message.");
		System.out.println("\"exit\": 	exit the program.");
	}
}
