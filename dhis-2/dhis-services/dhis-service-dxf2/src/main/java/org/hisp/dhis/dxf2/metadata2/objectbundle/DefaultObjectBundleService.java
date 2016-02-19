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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class DefaultObjectBundleService implements ObjectBundleService
{
    @Autowired
    private SchemaService schemaService;

    @Autowired
    private PreheatService preheatService;

    @Override
    public ObjectBundle create( ObjectBundleParams params )
    {
        ObjectBundle bundle = new ObjectBundle();
        bundle.putObjects( params.getObjects() );

        PreheatParams preheatParams = params.getPreheatParams();

        if ( PreheatMode.REFERENCE == preheatParams.getPreheatMode() )
        {
            preheatParams.setReferences( preheatService.collectReferences( params.getObjects() ) );
        }

        bundle.setPreheat( preheatService.preheat( preheatParams ) );

        return bundle;
    }

    @Override
    public ObjectBundleValidation validate( ObjectBundle bundle )
    {
        ObjectBundleValidation objectBundleValidation = new ObjectBundleValidation();

        for ( Class<? extends IdentifiableObject> klass : bundle.getObjects().keySet() )
        {
            Schema schema = schemaService.getDynamicSchema( klass );
            objectBundleValidation.addInvalidReferences( klass, preheatService.checkReferences(
                bundle.getObjects().get( klass ), bundle.getPreheat(), bundle.getPreheatIdentifier() ) );
        }

        return objectBundleValidation;
    }

    @Override
    public void commit( ObjectBundle bundle )
    {

    }
}
