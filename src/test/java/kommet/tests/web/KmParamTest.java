/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import kommet.tests.BaseUnitTest;
import kommet.web.rmparams.Event;
import kommet.web.rmparams.KmParamException;
import kommet.web.rmparams.KmParamNode;
import kommet.web.rmparams.KmParamNodeType;
import kommet.web.rmparams.KmParamUtils;
import kommet.web.rmparams.actions.Action;
import kommet.web.rmparams.actions.KeepParameters;
import kommet.web.rmparams.actions.SetField;
import kommet.web.rmparams.actions.ShowLookup;

public class KmParamTest extends BaseUnitTest
{
	@Test
	public void testParseRmParams() throws KmParamException
	{
		KmParamNode node = KmParamUtils.parseParam(new KmParamNode("rm"), "rm.save.set.someProp", "someVal");
		assertNotNull(node);
		assertEquals("rm", node.getName());
		assertEquals(KmParamNodeType.BASE, node.getType());
		assertEquals(1, node.getEventNodes().size());
		assertTrue(node.getEventNodes().containsKey("save"));
		assertEquals(KmParamNodeType.SAVE, node.getEventNodes().get("save").getType());
		
		KmParamNode saveNode = node.getEventNodes().get("save");
		Set<Action> setNodes = saveNode.getActionNodes("set");
		assertNotNull(setNodes);
		assertEquals(1, setNodes.size());
		KmParamNode setNode = setNodes.iterator().next();
		assertTrue("Expected node class " + SetField.class.getName() + " but found " + setNode.getClass().getName(), setNode instanceof SetField);
		
		assertEquals("someProp", ((SetField)setNode).getField());
		assertEquals("someVal", ((SetField)setNode).getValue());
		
		// now try the same evaluation with another parameter as well
		node = KmParamUtils.parseParam(node, "rm.save.msg", "Save successful");
		node = KmParamUtils.parseParam(node, "rm.save.keep", "2");
		node = KmParamUtils.parseParam(node, "rm.listselect.set.anotherProp", "44");
		node = KmParamUtils.parseParam(node, "rm.listselect.set.testProp", "45");
		node = KmParamUtils.parseParam(node, "rm.listselect.js", "jQuery('#ss').val('s')");
		node = KmParamUtils.parseParam(node, "rm.lookup", "xyz");
		assertNotNull(node);
		assertEquals("rm", node.getName());
		assertEquals(KmParamNodeType.BASE, node.getType());
		assertEquals(2, node.getEventNodes().size());
		assertTrue(node.getEventNodes().containsKey("save"));
		assertEquals(KmParamNodeType.SAVE, node.getEventNode("save").getType());
		
		saveNode = node.getEventNode("save");
		saveNode = node.getEventNodes().get("save");
		setNodes = saveNode.getActionNodes("set");
		assertNotNull(setNodes);
		assertEquals(1, setNodes.size());
		setNode = setNodes.iterator().next();
		assertTrue("Expected node class " + SetField.class.getName() + " but found " + setNode.getClass().getName(), setNode instanceof SetField);
		
		assertEquals("someProp", ((SetField)setNode).getField());
		assertEquals("someVal", ((SetField)setNode).getValue());
		
		Event listSelectNode = node.getEventNode("listselect");
		assertNotNull(listSelectNode);
		assertEquals(2, listSelectNode.getActionNodes("set").size());
		assertEquals(1, listSelectNode.getActionNodes("js").size());
		
		Set<Action> lookupNodes = node.getActionNodes("lookup"); 
		assertNotNull(lookupNodes);
		assertEquals(1, lookupNodes.size());
		assertTrue("Expected node class " + ShowLookup.class.getName() + " but got " + lookupNodes.iterator().next().getClass().getName(), lookupNodes.iterator().next() instanceof ShowLookup);
		
		assertEquals(2, ((KeepParameters)saveNode.getActionNodes("keep").iterator().next()).getKeepFor());
	}
}
