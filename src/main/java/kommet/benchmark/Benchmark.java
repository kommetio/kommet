/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Benchmark
{

	public static void main(String[] args) throws IOException
	{
		Integer loops = Integer.parseInt(args[0]);
		Integer files = 0;
		long startTime = System.currentTimeMillis();
		
		if (args.length > 1)
		{
			files = Integer.parseInt(args[1]);
		}
		
		for (int i = 0; i < loops; i++)
		{
			if (i % 100 == 0)
			{
				System.out.println("Loop " + i);
			}
			executeLoop(files);
		}
		
		System.out.println("Total time: " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
	private static void executeLoop(Integer files) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		long sum = 0;
		
		for (int i = 0; i < 1000000; i++)
		{
			sum += i;
			sb.append("a");
		}
		
		if (files != null)
		{
			for (int i = 0; i < files; i++)
			{
				File file = File.createTempFile("abc", "abc");
				FileWriter fw = new FileWriter(file);
				fw.append("this is some long text");
				fw.close();
				file.delete();
			}
		}
	}

}