package org.hisp.dhis.dxf2.metadata2.objectbundle;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.InvalidReference;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatValidation;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Override
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
    }

    @Test
    public void testCreateObjectBundle()
    {
        ObjectBundleParams params = new ObjectBundleParams();
        ObjectBundle bundle = objectBundleService.create( params );

        assertNotNull( bundle );
    }

    @Test
    public void testCreateDoesPreheating()
    {
        DataElementGroup dataElementGroup = fromJson( "dxf2/degAUidRef.json", DataElementGroup.class );
        defaultSetup();

        ObjectBundleParams params = new ObjectBundleParams();
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.addObject( dataElementGroup );

        ObjectBundle bundle = objectBundleService.create( params );

        assertNotNull( bundle );
        assertFalse( bundle.getPreheat().isEmpty() );
        assertFalse( bundle.getPreheat().isEmpty( PreheatIdentifier.UID ) );
        assertFalse( bundle.getPreheat().isEmpty( PreheatIdentifier.UID, DataElement.class ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghA" ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghB" ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghC" ) );
        assertFalse( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghD" ) );
    }

    @Test
    public void testObjectBundleShouldAddToObjectAndPreheat()
    {
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );

        ObjectBundle bundle = objectBundleService.create( params );

        DataElementGroup dataElementGroup = fromJson( "dxf2/degAUidRef.json", DataElementGroup.class );
        bundle.addObject( dataElementGroup );

        assertTrue( bundle.getObjects().get( DataElementGroup.class ).contains( dataElementGroup ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElementGroup.class, dataElementGroup.getUid() ) );
    }

    @Test
    public void testPreheatValidations() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate1.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );
        assertFalse( validate.getPreheatValidations().isEmpty() );
        List<PreheatValidation> dataElementValidations = validate.getPreheatValidations().get( DataElement.class );
        assertFalse( dataElementValidations.isEmpty() );

        for ( PreheatValidation preheatValidation : dataElementValidations )
        {
            assertFalse( preheatValidation.getInvalidReferences().isEmpty() );

            for ( InvalidReference invalidReference : preheatValidation.getInvalidReferences() )
            {
                assertEquals( PreheatIdentifier.UID, invalidReference.getIdentifier() );

                if ( DataElementCategoryCombo.class.isInstance( invalidReference.getRefObject() ) )
                {
                    assertEquals( "p0KPaWEg3cf", invalidReference.getRefObject().getUid() );
                }
                else if ( User.class.isInstance( invalidReference.getRefObject() ) )
                {
                    assertEquals( "GOLswS44mh8", invalidReference.getRefObject().getUid() );
                }
                else if ( OptionSet.class.isInstance( invalidReference.getRefObject() ) )
                {
                    assertEquals( "pQYCiuosBnZ", invalidReference.getRefObject().getUid() );
                }
            }
        }
    }

    @Test
    public void testPreheatValidationsWithCatCombo() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate1.json" ).getInputStream(), RenderFormat.JSON );

        DataElementCategoryCombo categoryCombo = manager.getByName( DataElementCategoryCombo.class, "default" );
        categoryCombo.setUid( "p0KPaWEg3cf" );
        manager.update( categoryCombo );

        OptionSet optionSet = new OptionSet( "OptionSet: pQYCiuosBnZ" );
        optionSet.setAutoFields();
        optionSet.setUid( "pQYCiuosBnZ" );
        manager.save( optionSet );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );
        assertFalse( validate.getPreheatValidations().isEmpty() );
        List<PreheatValidation> dataElementValidations = validate.getPreheatValidations().get( DataElement.class );
        assertFalse( dataElementValidations.isEmpty() );

        for ( PreheatValidation preheatValidation : dataElementValidations )
        {
            assertFalse( preheatValidation.getInvalidReferences().isEmpty() );

            for ( InvalidReference invalidReference : preheatValidation.getInvalidReferences() )
            {
                assertEquals( PreheatIdentifier.UID, invalidReference.getIdentifier() );

                if ( DataElementCategoryCombo.class.isInstance( invalidReference.getRefObject() ) )
                {
                    assertFalse( true );
                }
                else if ( User.class.isInstance( invalidReference.getRefObject() ) )
                {
                    assertEquals( "GOLswS44mh8", invalidReference.getRefObject().getUid() );
                }
                else if ( OptionSet.class.isInstance( invalidReference.getRefObject() ) )
                {
                    assertFalse( true );
                }
            }
        }
    }

    @Test
    public void testPreheatValidationsInvalidObjects() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate2.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertFalse( validate.getValidationViolations().isEmpty() );
        assertEquals( 2, validate.getValidationViolations().get( DataElement.class ).size() );
    }

    @Test
    public void testUpdateRequiresValidReferencesUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate4.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportMode( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 3, validate.getInvalidObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testUpdateWithPersistedObjectsRequiresValidReferencesUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate7.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportMode( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 1, validate.getInvalidObjects().get( DataElement.class ).size() );
        assertEquals( 2, bundle.getObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testUpdateRequiresValidReferencesCODE() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate5.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportMode( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 3, validate.getInvalidObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testUpdateRequiresValidReferencesAUTO() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate6.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.AUTO );
        params.setImportMode( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 3, validate.getInvalidObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testDeleteRequiresValidReferencesUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate4.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportMode( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 3, validate.getInvalidObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testDeleteRequiresValidReferencesCODE() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate5.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportMode( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 3, validate.getInvalidObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testDeleteRequiresValidReferencesAUTO() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate6.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.AUTO );
        params.setImportMode( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );

        assertTrue( validate.getInvalidObjects().containsKey( DataElement.class ) );
        assertEquals( 3, validate.getInvalidObjects().get( DataElement.class ).size() );
    }

    @Test
    public void testPreheatValidationsIncludingMerge() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate3.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setMergeMode( MergeMode.REPLACE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );
    }

    @Test
    public void testSimpleDataElementDeleteUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_simple_delete_uid.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportMode( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleService.validate( bundle );
        objectBundleService.commit( bundle );

        List<DataElement> dataElements = manager.getAll( DataElement.class );
        assertEquals( 1, dataElements.size() );
        assertEquals( "deabcdefghB", dataElements.get( 0 ).getUid() );
    }

    @Test
    public void testSimpleDataElementDeleteCODE() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_simple_delete_code.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportMode( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleService.validate( bundle );
        objectBundleService.commit( bundle );

        List<DataElement> dataElements = manager.getAll( DataElement.class );
        assertEquals( 1, dataElements.size() );
        assertEquals( "DataElementCodeD", dataElements.get( 0 ).getCode() );
    }

    @Test
    public void testCreateSimpleMetadataUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata1.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportMode( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleService.validate( bundle );
        objectBundleService.commit( bundle );

        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        DataSet dataSet = dataSets.get( 0 );

        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = manager.getDefaults();

        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( dataSets.isEmpty() );

        for ( DataElement dataElement : dataElements )
        {
            assertNotNull( dataElement.getCategoryCombo() );
            assertEquals( defaults.get( DataElementCategoryCombo.class ), dataElement.getCategoryCombo() );
        }

        assertFalse( dataSet.getSources().isEmpty() );
        assertFalse( dataSet.getDataElements().isEmpty() );
        assertEquals( 1, dataSet.getSources().size() );
        assertEquals( 2, dataSet.getDataElements().size() );
        assertEquals( PeriodType.getPeriodTypeByName( "Monthly" ), dataSet.getPeriodType() );
    }

    private void defaultSetup()
    {
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );
        DataElement de4 = createDataElement( 'D' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );
        manager.save( de4 );

        User user = createUser( 'A' );
        manager.save( user );
    }
}