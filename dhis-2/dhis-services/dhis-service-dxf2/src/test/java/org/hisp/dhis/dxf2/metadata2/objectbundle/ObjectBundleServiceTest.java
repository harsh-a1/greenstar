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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
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
            new ClassPathResource( "dxf2/metadata_preheat1.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidation validate = objectBundleService.validate( bundle );
        assertFalse( validate.getPreheatValidations().isEmpty() );
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