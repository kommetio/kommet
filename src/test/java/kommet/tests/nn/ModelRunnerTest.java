/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.nn;

import org.junit.Test;

import kommet.data.KommetException;
import kommet.tests.BaseUnitTest;

public class ModelRunnerTest extends BaseUnitTest
{
	@Test
	public void testRunner() throws KommetException
	{
		/*String path = "C:\\\\Users\\\\krawiecr\\\\Desktop\\\\test\\\\";
		
		List<Double> input = new ArrayList<Double>();
		for (int i = 0; i < 52; i++)
		{
			input.add(Math.random());
		}
		
		//float prediction = (new KerasNNRunner()).predict(path + "pb", input, path + "model-oulu-scaler.gz");
		String featureNameList = "atr,boll100_placement,boll20_placement,boll100_spread,boll20_spread,close,close_diff,dc100_placement,dc20_placement,dc5_placement,dc100_spread,dc20_spread,dc5_spread,ema100,ema20,ema200,ema5,ema5_price,high,high_diff,low,low_diff,open,open_diff,rsi_14_0,rsi_14_1,rsi_14_10,rsi_14_2,rsi_14_3,rsi_14_4,rsi_14_5,rsi_14_6,rsi_14_7,rsi_14_8,rsi_14_9,rsi_50_0,rsi_50_1,rsi_50_2,rsi_50_3,rsi_50_4,rsi_50_5,atr_span,ema_diff_20_100,ema_diff_100_200,ema_20_100_sign,ema_100_200_sign,gran_1,gran_2,gran_3,gran_4,gran_5,gran";
		String featureMap = "atr:0.01701108811162134,boll100_placement:0.20772033255117356,boll20_placement:0.6022540306120157,boll100_spread:12.166142133335974,boll20_spread:3.650574327635163,close:319.0859964032396,close_diff:-0.293925936259426,dc100_placement:0.2323943661971851,dc20_placement:1.1785714285714217,dc5_placement:0.0,dc100_spread:8.347496589767896,dc20_spread:1.645985243052848,dc5_spread:1.293274119541516,ema100:321.4452223232196,ema20:319.11644713023617,ema200:321.93337451815927,ema5:319.24177714945716,ema5_price:0.15578074621750151,high:319.0859964032396,high_diff:0.7054222470226641,low:318.49814453072077,low_diff:-0.293925936259426,open:318.90964084148396,open_diff:0.470281498015092,rsi_14_0:0.4854,rsi_14_1:0.46399999999999997,rsi_14_10:0.39649999999999996,rsi_14_2:0.5114,rsi_14_3:0.5553,rsi_14_4:0.5784,rsi_14_5:0.49219999999999997,rsi_14_6:0.5008,rsi_14_7:0.4796,rsi_14_8:0.47130000000000005,rsi_14_9:0.5092,rsi_50_0:0.4521,rsi_50_1:0.4456,rsi_50_2:0.4585,rsi_50_3:0.46950000000000003,rsi_50_4:0.47509999999999997,rsi_50_5:0.44659999999999994,atr_span:20.0,ema_diff_20_100:-2.328775192983453,ema_diff_100_200:-0.4881521949396573,ema_20_100_sign:-1.0,ema_100_200_sign:-1.0,gran_1:0,gran_2:0,gran_3:1,gran_4:1,gran_5:1,gran:7";
		
		KerasModel model = new KerasModel(path + "pb", path + "model-oulu-stats.dat", MiscUtils.splitAndTrim(featureNameList, ","));
		model.setVerbose(true);
		float prediction = model.predict(featureMap, true);
		
		System.out.println("prediction: " + prediction);*/
	}
}
