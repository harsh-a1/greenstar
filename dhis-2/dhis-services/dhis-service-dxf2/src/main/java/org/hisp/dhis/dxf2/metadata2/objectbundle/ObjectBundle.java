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
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundle
{
    private final User user;

    private final ObjectBundleMode objectBundleMode;

    private final PreheatIdentifier preheatIdentifier;

    private final PreheatMode preheatMode;

    private final ImportStrategy importMode;

    private final MergeMode mergeMode;

    private ObjectBundleStatus objectBundleStatus = ObjectBundleStatus.CREATED;

    private Preheat preheat = new Preheat();

    private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects = new HashMap<>();

    public ObjectBundle( ObjectBundleParams params )
    {
        this.user = params.getUser();
        this.objectBundleMode = params.getObjectBundleMode();
        this.preheatIdentifier = params.getPreheatIdentifier();
        this.importMode = params.getImportMode();
        this.preheatMode = params.getPreheatMode();
        this.mergeMode = params.getMergeMode();
    }

    public User getUser()
    {
        return user;
    }

    public ObjectBundleMode getObjectBundleMode()
    {
        return objectBundleMode;
    }

    public PreheatIdentifier getPreheatIdentifier()
    {
        return preheatIdentifier;
    }

    public PreheatMode getPreheatMode()
    {
        return preheatMode;
    }

    public ImportStrategy getImportMode()
    {
        return importMode;
    }

    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public ObjectBundleStatus getObjectBundleStatus()
    {
        return objectBundleStatus;
    }

    public void setObjectBundleStatus( ObjectBundleStatus objectBundleStatus )
    {
        this.objectBundleStatus = objectBundleStatus;
    }

    public Preheat getPreheat()
    {
        return preheat;
    }

    public void setPreheat( Preheat preheat )
    {
        this.preheat = preheat;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> void addObject( T object )
    {
        if ( object == null )
        {
            return;
        }

        if ( !objects.containsKey( object.getClass() ) )
        {
            objects.put( object.getClass(), new ArrayList<>() );
        }

        objects.get( object.getClass() ).add( object );
        preheat.put( preheatIdentifier, object );
    }

    public <T extends IdentifiableObject> void addObjects( List<T> objects )
    {
        objects.forEach( this::addObject );
    }

    public void putObjects( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        for ( Class<? extends IdentifiableObject> klass : objects.keySet() )
        {
            if ( !this.objects.containsKey( klass ) )
            {
                this.objects.put( klass, new ArrayList<>() );
            }

            this.objects.get( klass ).addAll( objects.get( klass ) );
        }
    }

    public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects()
    {
        return objects;
    }

    public void setObjects( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        this.objects = objects;
    }
}
