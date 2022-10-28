/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.View;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewUtil;
import kommet.dao.FieldFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FileExtension;
import kommet.data.KeyPrefix;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.TypeFilter;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.DateDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.deployment.DeployableType;
import kommet.deployment.DeploymentConfig;
import kommet.deployment.DeploymentService;
import kommet.deployment.FailedPackageDeploymentException;
import kommet.deployment.FileDeploymentStatus;
import kommet.deployment.OverwriteHandling;
import kommet.deployment.PackageDeploymentStatus;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.LibraryZipUtil;
import kommet.utils.MiscUtils;

public class DeploymentTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	ClassService classService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DeploymentService deploymentService;
	
	@Inject
	EnvService envService;
	
	@Inject
	KommetCompiler compiler;
	
	@Test
	public void testDeployZipFile() throws KommetException, IOException
	{
		EnvData sourceEnv = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(sourceEnv);
		AuthData sourceEnvAuthData = dataHelper.getRootAuthData(sourceEnv);
		
		// create dest env
		EnvData destEnv = dataHelper.getTestEnv2Data(false);
		basicSetupService.runBasicSetup(destEnv);
		AuthData destEnvAuthData = dataHelper.getRootAuthData(destEnv);
		
		int initialViewCount = viewService.getAllViews(destEnv).size();
		int initialKollFileCount = classService.getClasses(null, destEnv).size();
		
		// create test class file
		Class kollFile = getTestClassFile("TestFile", "com.some", sourceEnv);
		kollFile = classService.fullSave(kollFile, dataService, sourceEnvAuthData, sourceEnv);
		assertNotNull(kollFile.getId());
		
		// create test view
		View ktlView = new View();
		String ktlCode = ViewUtil.getEmptyViewCode("SampleView", "any.package");
		ktlView.setIsSystem(false);
		
		ktlView = viewService.fullSave(ktlView, ktlCode, false, sourceEnvAuthData, sourceEnv);
		assertNotNull(ktlView.getId());
	
		// create another view, but don't save it
		View ktlView2 = new View();
		ktlView2.setName("SampleView2");
		ktlView2.setPackageName("any.package");
		ktlView2.setKeetleCode(ViewUtil.getEmptyViewCode("SampleView2", "any.package"));
		
		// create another koll file, but don't save it
		Class kollFile2 = getTestClassFile("TestFile2", "com.some", sourceEnv);
		
		Map<String, String> files = new HashMap<String, String>();
		files.put(kollFile.getQualifiedName() + "." + FileExtension.CLASS_EXT, kollFile.getKollCode());
		files.put(kollFile2.getQualifiedName() + "." + FileExtension.CLASS_EXT, kollFile2.getKollCode());
		files.put(ktlView.getQualifiedName() + "." + FileExtension.VIEW_EXT, ktlView.getKeetleCode());
		files.put(ktlView2.getQualifiedName() + "." + FileExtension.VIEW_EXT, ktlView2.getKeetleCode());
		
		byte[] zipFile = LibraryZipUtil.createZip(files);
		
		ViewFilter filter = new ViewFilter();
		filter.setName("SampleView");
		filter.setPackage("any.package");
		
		assertTrue(viewService.getViews(filter, destEnv).isEmpty());
		
		// deploy the zip file
		PackageDeploymentStatus deployStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(zipFile)), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), destEnvAuthData, destEnv);
		assertTrue(deployStatus.isSuccess());
		
		// make sure all items have been saved
		assertEquals(initialViewCount + 2, viewService.getAllViews(destEnv).size());
		assertEquals(initialKollFileCount + 2, classService.getClasses(null, destEnv).size());
		
		// now modify the code of one view and one koll file
		files.clear();
		String classCodeVersion1 = "package " + kollFile.getPackageName() + ";\npublic class " + kollFile.getName() + "{ public String getName() { return \"name\"; } }";
		files.put(kollFile.getQualifiedName() + ".koll", classCodeVersion1);
		String newViewCode = ViewUtil.wrapViewCode("inner code test", ktlView.getName(), ktlView.getPackageName());
		files.put(ktlView.getQualifiedName() + ".ktl", newViewCode);
		
		PackageDeploymentStatus status = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), destEnvAuthData, destEnv);
		assertEquals(2, status.getFileStatuses().size());
		assertTrue(status.getFileStatuses().get(0).isSuccess());
		assertTrue(status.getFileStatuses().get(1).isSuccess());
		
		View changedView = viewService.getView(ktlView.getQualifiedName(), true, destEnv);
		assertNotNull(changedView);
		assertEquals(newViewCode, changedView.getKeetleCode());
		
		Class changedKollFile = classService.getClass(kollFile.getQualifiedName(), destEnv);
		assertNotNull(changedKollFile);
		assertEquals("Class code has not been updated during deployment", classCodeVersion1, changedKollFile.getKollCode());
		
		deployTestPackage(files, kollFile, kollFile2, ktlView, ktlView2, classCodeVersion1, sourceEnv, destEnv);
		
		for (int i = 0; i < 2; i++)
		{
			// make sure three identical deployments in a row render the same results
			deployTestPackage(files, kollFile, kollFile2, ktlView, ktlView2, classCodeVersion1, sourceEnv, destEnv);
		}
		
		// make sure no new items have been saved, but the old ones have been updated
		assertEquals(initialViewCount + 2, viewService.getAllViews(destEnv).size());
		assertEquals(initialKollFileCount + 2, classService.getClasses(null, destEnv).size());
		
		testDeployTypesAndFields(kollFile2, ktlView2, sourceEnv, destEnv);
	}
	
	private void testDeployTypesAndFields(Class class1, View view1, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		Map<String, String> files = new HashMap<String, String>();
		files.put(class1.getQualifiedName() + "." + FileExtension.CLASS_EXT, class1.getKollCode());
		files.put(view1.getQualifiedName() + "." + FileExtension.VIEW_EXT, view1.getKeetleCode());
		
		DeployableType productType = new DeployableType();
		productType.setApiName("Product");
		productType.setPackage("krawiec.warehouse");
		productType.setLabel("Product");
		productType.setPluralLabel("Products");
		productType.setDescription("Product desc");
		String productTypeXML = DeploymentService.serialize(productType, sourceEnv);
		files.put(productType.getQualifiedName() + "." + FileExtension.TYPE_EXT, productTypeXML);
		
		DeployableType manufacturerType = new DeployableType();
		manufacturerType.setApiName("Manufacturer");
		manufacturerType.setPackage("krawiec.warehouse.manufacture");
		manufacturerType.setLabel("Manufacturer");
		manufacturerType.setPluralLabel("Manufacturers");
		files.put(manufacturerType.getQualifiedName() + "." + FileExtension.TYPE_EXT, DeploymentService.serialize(manufacturerType, sourceEnv));
		
		DeployableType brandType = new DeployableType();
		brandType.setApiName("Brand");
		String brandTypePackage = "krawiec.warehouse"; 
		brandType.setPackage(brandTypePackage);
		brandType.setLabel("Brand");
		brandType.setPluralLabel("Brands");
		
		// set the key prefix on brand type
		brandType.setKeyPrefix(KeyPrefix.get("xyz"));
		
		// TODO add type and field definitions to the package
		String brandTypeXML = DeploymentService.serialize(brandType, sourceEnv);
		files.put(brandType.getQualifiedName() + "." + FileExtension.TYPE_EXT, brandTypeXML);
		
		int initialTypeCount = dataService.getTypes(null, false, false, destEnv).size();
		
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		assertTrue(packageStatus.isSuccess());
		
		assertEquals(brandTypePackage, brandType.getPackage());
		
		int newTypeCount = dataService.getTypes(null, false, false, destEnv).size();
		
		assertEquals("Three new types should have been inserted, but the number of types is " + newTypeCount + "(previously: " + initialTypeCount + ")", initialTypeCount + 3, newTypeCount);
		
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			assertTrue("Deployment of file " + status.getFileName() + " failed", status.isSuccess());
			assertNull(status.getErrors());
		}
		
		Type sourceBrandType = dataService.createType(brandType, sourceEnv);
		assertNotNull(sourceBrandType.getKID());
		
		Type sourceProductType = dataService.createType(productType, sourceEnv);
		assertNotNull(sourceProductType.getKID());
		
		Type sourceManufacturerType = dataService.createType(manufacturerType, sourceEnv);
		assertNotNull(sourceManufacturerType.getKID());
		
		// make sure type has been deployed
		TypeFilter filter = new TypeFilter();
		filter.setQualifiedName(productType.getQualifiedName());
		List<Type> foundTypes = dataService.getTypes(filter, true, false, destEnv);
		assertEquals("Type with API name " + productType.getQualifiedName() + " not inserted during deployment", 1, foundTypes.size());
		assertEquals("If no default field was specified on the type, it should be set to the ID field automatically", Field.ID_FIELD_NAME, foundTypes.get(0).getDefaultFieldApiName());
		
		filter = new TypeFilter();
		filter.setQualifiedName(brandType.getQualifiedName());
		foundTypes = dataService.getTypes(filter, true, false, destEnv);
		assertEquals("Type with API name " + brandTypePackage + "." + brandType.getApiName() + " not inserted during deployment", 1, foundTypes.size());
		assertEquals("If no default field was specified on the type, it should be set to the ID field automatically", Field.ID_FIELD_NAME, foundTypes.get(0).getDefaultFieldApiName());
		
		// now add a few fields to the product type
		Field priceField = new Field();
		priceField.setApiName("price");
		priceField.setDataType(new NumberDataType(2, BigDecimal.class));
		priceField.setLabel("Item Price");
		priceField.setDescription("some description");
		priceField.setRequired(true);
		priceField.setType(productType);
		
		String priceFieldXML = DeploymentService.serialize(priceField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + priceField.getApiName() + "." + FileExtension.FIELD_EXT, priceFieldXML);
		
		Field nameField = new Field();
		nameField.setApiName("itemName");
		nameField.setDataType(new TextDataType(20));
		nameField.setLabel("Item Name");
		nameField.setDescription("The name of the product");
		nameField.setRequired(true);
		nameField.setType(sourceProductType);
		dataService.createField(nameField, sourceEnv);
		
		String nameFieldXML = DeploymentService.serialize(nameField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + nameField.getApiName() + "." + FileExtension.FIELD_EXT, nameFieldXML);
		
		Field discountField = new Field();
		discountField.setApiName("isDiscount");
		discountField.setDataType(new BooleanDataType());
		discountField.setLabel("Is Discount");
		discountField.setRequired(true);
		discountField.setType(sourceProductType);
		dataService.createField(discountField, sourceEnv);
		
		String discountFieldXML = DeploymentService.serialize(discountField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + discountField.getApiName() + "." + FileExtension.FIELD_EXT, discountFieldXML);
		
		// set the name field as the default field on the type
		productType.setDefaultFieldApiName(nameField.getApiName());
		// update type XML in the package to reflect this change
		files.put(productType.getQualifiedName() + "." + FileExtension.TYPE_EXT, DeploymentService.serialize(productType, sourceEnv));
		
		Field brandRefField = new Field();
		brandRefField.setApiName("brand");
		brandRefField.setDataType(new TypeReference(brandType));
		brandRefField.setLabel("Brand");
		brandRefField.setRequired(false);
		brandRefField.setType(productType);
		
		String brandRefFieldXML = DeploymentService.serialize(brandRefField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + brandRefField.getApiName() + "." + FileExtension.FIELD_EXT, brandRefFieldXML);
		
		// deploy the package with the new components
		packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			assertTrue("Deployment of file " + status.getFileName() + " failed", status.isSuccess());
			assertNull(status.getErrors());
		}
		
		Type destProductType = dataService.getTypeByName("krawiec.warehouse.Product", true, destEnv);
		assertNotNull(destProductType);
		assertEquals(destProductType.getDescription(), productType.getDescription());
		assertEquals(destProductType.getLabel(), productType.getLabel());
		assertEquals(destProductType.getApiName(), productType.getApiName());
		assertEquals(destProductType.getPluralLabel(), productType.getPluralLabel());
		
		Field foundItemNameField = destProductType.getField(nameField.getApiName());
		assertNotNull(foundItemNameField);
		assertTrue(foundItemNameField.getDataType() instanceof TextDataType);
		assertEquals(nameField.getDescription(), foundItemNameField.getDescription());
		assertEquals((Integer)20, ((TextDataType)foundItemNameField.getDataType()).getLength());
		
		Field foundPriceField = destProductType.getField(priceField.getApiName());
		assertNotNull(foundPriceField);
		assertTrue(foundPriceField.getDataType() instanceof NumberDataType);
		assertEquals((Integer)2, ((NumberDataType)foundPriceField.getDataType()).getDecimalPlaces());
		
		Field foundDiscountField = destProductType.getField(discountField.getApiName());
		assertNotNull(foundDiscountField);
		assertTrue(foundDiscountField.getDataType() instanceof BooleanDataType);
		
		Field foundBrandRefField = destProductType.getField(brandRefField.getApiName());
		assertNotNull(foundBrandRefField);
		assertTrue(foundBrandRefField.getDataType() instanceof TypeReference);
		assertEquals(brandType.getQualifiedName(), ((TypeReference)foundBrandRefField.getDataType()).getType().getQualifiedName());
		
		// retrieve product type
		assertEquals(nameField.getApiName(), destProductType.getDefaultFieldApiName());
		assertTrue(destProductType.getField(nameField.getApiName()).isRequired());
		assertNotNull("Brand field not added to product type", destProductType.getField(brandRefField.getApiName()));
		assertNotNull("Price field not added to product type", destProductType.getField(priceField.getApiName()));
		assertNotNull(destProductType.getCreated());
		assertNotNull(destProductType.getKeyPrefix());
		assertFalse(destProductType.getField(brandRefField.getApiName()).isRequired());
		
		// fetch the product type again, to make sure we have access to all its fields
		sourceProductType = sourceEnv.getType(sourceProductType.getKeyPrefix());
		
		// now test deploying a field without its type (type added earlier)
		testDeployEnumField(sourceProductType, sourceEnv, destEnv);
		testDeployDateField(sourceProductType, sourceEnv, destEnv);
		testDeployInverseCollectionField(sourceProductType, sourceBrandType, sourceEnv, destEnv);
		testDeployFormulaField(sourceProductType, sourceEnv, destEnv);
		testDeployAssociationField(sourceProductType, sourceBrandType, sourceManufacturerType, sourceEnv, destEnv);
		testDeployMultiEnumField(sourceProductType, sourceEnv, destEnv);
		
		// TODO
		// - test all other dts
	}
	
	private void testDeployMultiEnumField(Type productType, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		Map<String, String> files = new HashMap<String, String>();
		Set<String> values = new HashSet<String>();
		values.add("One");
		values.add("Two");
		values.add("Three");
		
		// add multi-enum field
		Field labelsField = new Field();
		labelsField.setApiName("labels");
		labelsField.setLabel("Labels");
		labelsField.setDataType(new MultiEnumerationDataType(values));
		labelsField.setRequired(false);
		labelsField.setType(productType);
		files.put(productType.getQualifiedName() + "." + labelsField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(labelsField, dataService, sourceEnv));
		
		// also deploy category field, just to make sure enum and multi-enum fields can be safely deployed
		// in the same package
		Field categoryField = new Field();
		categoryField.setApiName("category2");
		categoryField.setDataType(new EnumerationDataType("Food\nClother\nCosmetics"));
		categoryField.setLabel("Category2");
		categoryField.setRequired(true);
		categoryField.setType(productType);
		
		// add field to package
		String categoryFieldXML = DeploymentService.serialize(categoryField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + categoryField.getApiName() + "." + FileExtension.FIELD_EXT, categoryFieldXML);
		
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		
		// four items are deployed - association type, its two fields and the association field on the product type
		assertEquals(2, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			if (!status.isSuccess())
			{
				fail("Deployment of file " + status.getFileName() + " failed. Errors:\n:" + MiscUtils.implode(status.getErrors(), "\n"));
			}
			assertNull(status.getErrors());
		}
		
		FieldFilter filter = new FieldFilter();
		filter.setApiName(labelsField.getApiName());
		filter.setTypeQualifiedName(productType.getQualifiedName());
		List<Field> foundLabelsFields = dataService.getFields(filter, destEnv);
		assertEquals(1, foundLabelsFields.size());
		assertNotNull(((MultiEnumerationDataType)foundLabelsFields.get(0).getDataType()).getValues());
		assertEquals(values.size(), ((MultiEnumerationDataType)foundLabelsFields.get(0).getDataType()).getValues().size());
	}

	private void testDeployAssociationField(Type productType, Type brandType, Type manufacturerType, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		// keep it in a linked hash map to make sure fields are deployed in order
		// it is especially important for the test because we want to make sure the association field
		// is deployed before the type reference fields on the association type, and yet it does not
		// break the deployment process
		Map<String, String> files = new LinkedHashMap<String, String>();
		
		DeployableType product2BrandType = new DeployableType();
		product2BrandType.setApiName("ProductBrandAssoc");
		product2BrandType.setPackage("krawiec.warehouse");
		product2BrandType.setLabel("Product To Brand Assoc");
		product2BrandType.setPluralLabel("Products to Brands");
		String product2BrandTypeXML = DeploymentService.serialize(product2BrandType, sourceEnv);
		files.put(product2BrandType.getQualifiedName() + "." + FileExtension.TYPE_EXT, product2BrandTypeXML);
		
		// add association field on the product type
		Field assocField = new Field();
		assocField.setApiName("brandList");
		assocField.setDataType(new AssociationDataType(product2BrandType, brandType, "product", "brand"));
		assocField.setLabel("Brand List");
		assocField.setType(productType);
		files.put(productType.getQualifiedName() + "." + assocField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(assocField, dataService, sourceEnv));
		
		Field brandField = new Field();
		brandField.setApiName("brand");
		brandField.setDataType(new TypeReference(brandType));
		brandField.setLabel("Brand");
		brandField.setType(product2BrandType);
		files.put(product2BrandType.getQualifiedName() + "." + brandField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(brandField, dataService, sourceEnv));
		
		Field productField = new Field();
		productField.setApiName("product");
		productField.setDataType(new TypeReference(productType));
		productField.setLabel("Product");
		productField.setType(product2BrandType);
		files.put(product2BrandType.getQualifiedName() + "." + productField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(productField, dataService, sourceEnv));
		
		// add manufacturer field to product
		Field manuField = new Field();
		manuField.setApiName("manufacturer");
		manuField.setDataType(new TypeReference(manufacturerType));
		manuField.setLabel("Manufacturer");
		manuField.setType(productType);
		files.put(productType.getQualifiedName() + "." + manuField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(manuField, dataService, sourceEnv));
		
		// in the same package add an inverse field that depends on the manufacturer field
		Field productsField = new Field();
		productsField.setApiName("prods");
		productsField.setDataType(new InverseCollectionDataType(productType, "manufacturer"));
		productsField.setLabel("Products for manufacturer");
		productsField.setType(manufacturerType);
		files.put(manufacturerType.getQualifiedName() + "." + manuField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(productsField, dataService, sourceEnv));
		
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		
		// six items are deployed - association type, its two fields and the association field on the product type
		assertEquals(6, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			if (!status.isSuccess())
			{
				fail("Deployment of file " + status.getFileName() + " failed. Errors:\n:" + MiscUtils.implode(status.getErrors(), "\n"));
			}
			assertNull(status.getErrors());
		}
		
		FieldFilter filter = new FieldFilter();
		filter.setApiName(productsField.getApiName());
		filter.setTypeQualifiedName(manufacturerType.getQualifiedName());
		List<Field> foundProductsFields = dataService.getFields(filter, destEnv);
		assertEquals(1, foundProductsFields.size());
		assertEquals(productType.getQualifiedName(), ((InverseCollectionDataType)foundProductsFields.get(0).getDataType()).getInverseType().getQualifiedName());
	}

	private void testDeployFormulaField(Type productType, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{	
		Map<String, String> files = new HashMap<String, String>();
		Field formulaField = new Field();
		formulaField.setApiName("testField");
		formulaField.setDataType(new FormulaDataType(FormulaReturnType.TEXT, "itemName + \"cos tam\"", productType, sourceEnv));
		formulaField.setLabel("Test Formula Field");
		formulaField.setType(productType);
		
		files.put(productType.getQualifiedName() + "." + formulaField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(formulaField, dataService, sourceEnv));
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		assertEquals(1, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			assertTrue("Deployment of file " + status.getFileName() + " failed", status.isSuccess());
			assertNull(status.getErrors());
		}
		
		Type foundProductType = dataService.getTypeByName(productType.getQualifiedName(), true, destEnv);
		assertNotNull("Type with ID " + productType.getKID() + " not found", foundProductType);
		Field foundFormulaField = foundProductType.getField(formulaField.getApiName());
		assertNotNull(foundFormulaField);
		assertEquals((Integer)DateDataType.FORMULA, foundFormulaField.getDataTypeId());
		
		FormulaDataType dt = (FormulaDataType)foundFormulaField.getDataType();
		assertEquals("itemName + \"cos tam\"", dt.getUserDefinition());
		assertEquals(FormulaReturnType.TEXT, dt.getReturnType());
	}

	private void testDeployInverseCollectionField(Type productType, Type brandType, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		Map<String, String> files = new HashMap<String, String>();
		Field productsField = new Field();
		productsField.setApiName("products");
		productsField.setDataType(new InverseCollectionDataType(productType, "brand"));
		productsField.setLabel("Products");
		productsField.setType(brandType);
		
		files.put(productType.getQualifiedName() + "." + productsField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(productsField, dataService, sourceEnv));
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		assertEquals(1, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			assertTrue("Deployment of file " + status.getFileName() + " failed", status.isSuccess());
			assertNull(status.getErrors());
		}
		
		Type foundBrandType = dataService.getTypeByName(brandType.getQualifiedName(), true, destEnv);
		assertNotNull("Type with ID " + brandType.getKID() + " not found", foundBrandType);
		Field foundProductsField = foundBrandType.getField(productsField.getApiName());
		assertNotNull(foundProductsField);
		assertEquals((Integer)DateDataType.INVERSE_COLLECTION, foundProductsField.getDataTypeId());
		assertEquals("brand", ((InverseCollectionDataType)foundProductsField.getDataType()).getInverseProperty());
	}

	private void testDeployDateField(Type productType, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		Map<String, String> files = new HashMap<String, String>();
		Field bestBeforeField = new Field();
		bestBeforeField.setApiName("bestBefore");
		bestBeforeField.setDataType(new DateDataType());
		bestBeforeField.setLabel("Best Before");
		bestBeforeField.setRequired(false);
		bestBeforeField.setType(productType);
		
		files.put(productType.getQualifiedName() + "." + bestBeforeField.getApiName() + "." + FileExtension.FIELD_EXT, DeploymentService.serialize(bestBeforeField, dataService, sourceEnv));
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		assertEquals(1, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			assertTrue("Deployment of file " + status.getFileName() + " failed", status.isSuccess());
			assertNull(status.getErrors());
		}
		
		Type foundProductType = dataService.getTypeByName(productType.getQualifiedName(), true, destEnv);
		assertNotNull("Type with ID " + productType.getKID() + " not found", foundProductType);
		Field foundBestBeforeField = foundProductType.getField(bestBeforeField.getApiName());
		assertNotNull(foundBestBeforeField);
		assertEquals((Integer)DateDataType.DATE, foundBestBeforeField.getDataTypeId());
	}
	
	private void testDeployEnumField(Type productType, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		Map<String, String> files = new HashMap<String, String>();
		Field categoryField = new Field();
		categoryField.setApiName("category");
		categoryField.setDataType(new EnumerationDataType("Food\nClother\nCosmetics"));
		categoryField.setLabel("Category");
		categoryField.setRequired(true);
		categoryField.setType(productType);
		
		// add field to package
		String categoryFieldXML = DeploymentService.serialize(categoryField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + categoryField.getApiName() + "." + FileExtension.FIELD_EXT, categoryFieldXML);
		PackageDeploymentStatus packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		assertTrue(packageStatus.isSuccess());
		assertEquals(1, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			assertTrue("Deployment of file " + status.getFileName() + " failed", status.isSuccess());
			assertNull(status.getErrors());
		}
		
		Type foundProductType = dataService.getTypeByName(productType.getQualifiedName(), true, destEnv);
		assertNotNull("Type with ID " + productType.getKID() + " not found", foundProductType);
		Field foundCategoryField = foundProductType.getField(categoryField.getApiName());
		assertNotNull(foundCategoryField);
		assertEquals(3, ((EnumerationDataType)foundCategoryField.getDataType()).getValueList().size());
		
		// now add one more value to the enum field
		((EnumerationDataType)categoryField.getDataType()).setValues("Food\nClothes\nCosmetics\nDrugs");
		// also change the field label
		categoryField.setLabel("Product category");
		
		categoryFieldXML = DeploymentService.serialize(categoryField, dataService, sourceEnv);
		files.put(productType.getQualifiedName() + "." + categoryField.getApiName() + "." + FileExtension.FIELD_EXT, categoryFieldXML);
		packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
		assertEquals(1, packageStatus.getFileStatuses().size());
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			if (!status.isSuccess())
			{
				fail("Deployment of file " + status.getFileName() + " failed. Errors:\n:" + MiscUtils.implode(status.getErrors(), "\n"));
			}
			assertNull(status.getErrors());
		}
		
		foundProductType = dataService.getTypeByName(productType.getQualifiedName(), true, destEnv);
		FieldFilter filter = new FieldFilter();
		filter.setTypeQualifiedName(productType.getQualifiedName());
		filter.setApiName(categoryField.getApiName());
		List<Field> foundFields = dataService.getFields(filter, destEnv);
		assertEquals(1, foundFields.size());
		foundCategoryField = foundFields.get(0);
		assertNotNull(foundCategoryField);
		assertEquals("Product category", foundCategoryField.getLabel());
		assertEquals(4, ((EnumerationDataType)foundCategoryField.getDataType()).getValueList().size());
		
		Set<String> distinctVals = new HashSet<String>();
		
		for (String enumVal : ((EnumerationDataType)foundCategoryField.getDataType()).getValueList())
		{
			distinctVals.add(enumVal);
		}
		
		assertEquals(4, distinctVals.size());
	}

	private void deployTestPackage(Map<String, String> files, Class kollFile, Class kollFile2, View ktlView, View ktlView2, String classCodeVersion1, EnvData sourceEnv, EnvData destEnv) throws KommetException, IOException
	{
		// now try to deploy a koll file with compilation errors and make sure it fails
		files.clear();
		String classCodeVersion2 = "package " + kollFile.getPackageName() + ";\npublic clas " + kollFile.getName() + "{ public String getName() { return \"name\"; } }";
		files.put(kollFile.getQualifiedName() + "." + FileExtension.CLASS_EXT, classCodeVersion2);
		String newViewCode = ViewUtil.wrapViewCode("inner code test", ktlView.getName(), ktlView.getPackageName());
		files.put(ktlView.getQualifiedName() + "." + FileExtension.VIEW_EXT, newViewCode + "ERROR-SUFFIX");
		
		PackageDeploymentStatus packageStatus = null;
		
		try
		{
			packageStatus = deploymentService.deployZip(new ZipInputStream(new ByteArrayInputStream(LibraryZipUtil.createZip(files))), new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), dataHelper.getRootAuthData(destEnv), destEnv);
			fail("Deployment should have failed because of errors in deployed class");
		}
		catch (FailedPackageDeploymentException e)
		{
			// expected
			packageStatus = e.getStatus();
		}
		
		assertEquals(2, packageStatus.getFileStatuses().size());
		
		for (FileDeploymentStatus status : packageStatus.getFileStatuses())
		{
			if (kollFile.getQualifiedName().equals(status.getFileName()))
			{
				assertFalse(status.isSuccess());
				assertNotNull(status.getErrors());
				assertFalse(status.getErrors().isEmpty());
				assertNull(status.getDeployedComponentId());
				
				//compiler.getClass(kollFile.getQualifiedName(), false, env);
				
				// although the class file contained no errors, it should not have been deployed because other items in the
				// package contained errors
				Class changedKollFile = classService.getClass(kollFile.getQualifiedName(), destEnv);
				assertNotSame("Class code has been updated during deployment, although errors in other package items should have prevented it", classCodeVersion2, changedKollFile.getKollCode());
				assertEquals("Class code should be the same as previously. Instead, it has been updated during deployment, although errors in other package items should have prevented it", classCodeVersion1, changedKollFile.getKollCode());
			}
			else if (kollFile2.getQualifiedName().equals(status.getFileName()))
			{
				assertTrue(status.isSuccess());
				assertNull(status.getDeployedComponentId());
				assertNull(status.getErrors());
				assertTrue(status.getErrors().isEmpty());
			}
			else if (ktlView.getQualifiedName().equals(status.getFileName()))
			{
				assertFalse("Deployment service failed to report an error in view definition and instead allowed to deploy it: " + MiscUtils.implode(status.getErrors(), ". "), status.isSuccess());
			}
			else if (ktlView2.getQualifiedName().equals(status.getFileName()))
			{
				assertTrue(status.isSuccess());
			}
			else
			{
				//System.out.println("\n\n" + (MiscUtils.envToUserPackage(kollFile.getQualifiedName(), env) + "." + FileExtension.CLASS_EXT) + "...");
				fail("Unknown file name in deployment status: " + status.getFileName());
			}
		}
	}
	
	private Class getTestClassFile(String name, String packageName, EnvData env) throws KommetException
	{
		String code = "package " + packageName + ";\n";
		code += "public class " + name + " { ";
		code += "public String getText() { return \"kamila\"; }";
		code += "}";
		
		Class file = new Class();
		file.setIsSystem(false);
		file.setJavaCode("test code");
		file.setName(name);
		file.setPackageName(packageName);
		file.setKollCode(code);
		return file;
	}
}
