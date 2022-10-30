/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.harness;

import java.util.Date;

import org.junit.Test;

import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

public class StudentCourseDataSet
{
	public static final String STUDENT_API_NAME = "Student";
	public static final String COURSE_API_NAME = "Course";
	public static final String ENROLLMETN_API_NAME = "Enrollment";
	public static final String PACKAGE = "com.test";
	
	private Type studentType;
	private Type courseType;
	private Type enrollmentType;
	
	@Test
	public void stubTestMethod()
	{
		// empty
	}
	
	public static StudentCourseDataSet getInstance(DataService dataService, EnvData env) throws KommetException
	{
		StudentCourseDataSet dataSet = new StudentCourseDataSet();
		
		Type studentType = dataService.createType(createStudentType(env), env);
		Type courseType = dataService.createType(createCourseType(env), env);
		Type enrollmentType = dataService.createType(createEnrollmentType(studentType, courseType, env), env);
		
		Field coursesField = getCoursesField(enrollmentType, courseType);
		studentType.addField(coursesField);
		dataService.createField(coursesField, env);
		
		dataSet.setCourseType(courseType);
		dataSet.setStudentType(studentType);
		dataSet.setEnrollmentType(enrollmentType);
		
		return dataSet;
	}

	private static Field getCoursesField(Type enrollmentType, Type courseType) throws KommetException
	{
		Field field = new Field();
		field.setApiName("courses");
		field.setDataType(new AssociationDataType(enrollmentType, courseType, "student", "course"));
		field.setLabel("Courses");
		field.setRequired(false);
		return field;
	}

	private static Type createEnrollmentType(Type studentType, Type courseType, EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(ENROLLMETN_API_NAME);
		type.setPackage(PACKAGE);
		type.setLabel("Enrollment");
		type.setPluralLabel("Enrollments");
		type.setCreated(new Date());
		
		type.addField(getStudentField(studentType));
		type.addField(getCourseField(courseType));
		
		return type;
	}

	private static Field getStudentField(Type studentType) throws KommetException
	{
		Field field = new Field();
		field.setApiName("student");
		field.setDataType(new TypeReference(studentType));
		field.setLabel("Student");
		field.setRequired(true);
		return field;
	}
	
	private static Field getCourseField(Type courseType) throws KommetException
	{
		Field field = new Field();
		field.setApiName("course");
		field.setDataType(new TypeReference(courseType));
		field.setLabel("Course");
		field.setRequired(true);
		return field;
	}

	private static Type createStudentType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(STUDENT_API_NAME);
		type.setPackage(PACKAGE);
		type.setLabel("Student");
		type.setPluralLabel("Students");
		type.setCreated(new Date());
		
		type.addField(getNameField());
		
		return type;
	}
	
	private static Type createCourseType(EnvData env) throws KommetException
	{
		Type type = new Type();
		type.setApiName(COURSE_API_NAME);
		type.setPackage(PACKAGE);
		type.setLabel("Course");
		type.setPluralLabel("Courses");
		type.setCreated(new Date());
		
		type.addField(getNameField());
		
		return type;
	}

	private static Field getNameField() throws KommetException
	{
		Field field = new Field();
		field.setApiName("name");
		field.setDataType(new TextDataType(20));
		field.setLabel("Name");
		field.setRequired(true);
		return field;
	}

	public void setStudentType(Type studentType)
	{
		this.studentType = studentType;
	}

	public Type getStudentType()
	{
		return studentType;
	}

	public void setCourseType(Type courseType)
	{
		this.courseType = courseType;
	}

	public Type getCourseType()
	{
		return courseType;
	}

	public void setEnrollmentType(Type enrollmentType)
	{
		this.enrollmentType = enrollmentType;
	}

	public Type getEnrollmentType()
	{
		return enrollmentType;
	}
}
