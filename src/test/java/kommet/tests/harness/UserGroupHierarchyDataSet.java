/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.harness;

import org.junit.Test;

import kommet.basic.UserGroup;
import kommet.data.Record;

public class UserGroupHierarchyDataSet
{
	private Record teacher1;
	private Record teacher2;
	private Record mathStudent1;
	private Record algebraStudent1;
	private Record algebraStudent2;
	private Record geometryStudent1;
	private UserGroup teacherGroup;
	private UserGroup studentGroup;
	private UserGroup algebraStudentGroup;
	private UserGroup mathStudentGroup;
	private UserGroup geometryStudentGroup;
	private Record profile;
	
	@Test
	public void stubTestMethod()
	{
		// empty
	}

	public Record getTeacher1()
	{
		return teacher1;
	}

	public void setTeacher1(Record teacher1)
	{
		this.teacher1 = teacher1;
	}

	public Record getTeacher2()
	{
		return teacher2;
	}

	public void setTeacher2(Record teacher2)
	{
		this.teacher2 = teacher2;
	}

	public UserGroup getTeacherGroup()
	{
		return teacherGroup;
	}

	public void setTeacherGroup(UserGroup teacherGroup)
	{
		this.teacherGroup = teacherGroup;
	}

	public UserGroup getStudentGroup()
	{
		return studentGroup;
	}

	public void setStudentGroup(UserGroup studentGroup)
	{
		this.studentGroup = studentGroup;
	}

	public UserGroup getAlgebraStudentGroup()
	{
		return algebraStudentGroup;
	}

	public void setAlgebraStudentGroup(UserGroup algebraStudentGroup)
	{
		this.algebraStudentGroup = algebraStudentGroup;
	}

	public UserGroup getMathStudentGroup()
	{
		return mathStudentGroup;
	}

	public void setMathStudentGroup(UserGroup mathStudentGroup)
	{
		this.mathStudentGroup = mathStudentGroup;
	}

	public UserGroup getGeometryStudentGroup()
	{
		return geometryStudentGroup;
	}

	public void setGeometryStudentGroup(UserGroup geometryStudentGroup)
	{
		this.geometryStudentGroup = geometryStudentGroup;
	}

	public Record getProfile()
	{
		return profile;
	}

	public void setProfile(Record profile)
	{
		this.profile = profile;
	}

	public Record getAlgebraStudent1()
	{
		return algebraStudent1;
	}

	public void setAlgebraStudent1(Record algebraStudent1)
	{
		this.algebraStudent1 = algebraStudent1;
	}

	public Record getMathStudent1()
	{
		return mathStudent1;
	}

	public void setMathStudent1(Record mathStudent1)
	{
		this.mathStudent1 = mathStudent1;
	}

	public Record getAlgebraStudent2()
	{
		return algebraStudent2;
	}

	public void setAlgebraStudent2(Record algebraStudent2)
	{
		this.algebraStudent2 = algebraStudent2;
	}

	public Record getGeometryStudent1()
	{
		return geometryStudent1;
	}

	public void setGeometryStudent1(Record geometryStudent1)
	{
		this.geometryStudent1 = geometryStudent1;
	}
}
