package org.theseed.sample.utils;

import java.util.Arrays;

import org.theseed.utils.BaseProcessor;

/**
 * Commands for Annotated Sample Processing Utilities.
 *
 *	import	copy samples from binning output directories
 *
 */
public class App
{
    public static void main( String[] args )
    {
        // Get the control parameter.
        String command = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        BaseProcessor processor;
        // Determine the command to process.
        switch (command) {
        case "import" :
            processor = new SampleImportProcessor();
            break;
        default:
            throw new RuntimeException("Invalid command " + command);
        }
        // Process it.
        boolean ok = processor.parseCommand(newArgs);
        if (ok) {
            processor.run();
        }
    }
}
