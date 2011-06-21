package org.neo4j.kernel.impl;

import org.neo4j.kernel.Version;
import org.neo4j.helpers.Service;

@Service.Implementation(Version.class)
public class ComponentVersion extends Version
{
    public ComponentVersion()
    {
        super("@artifactId@", "@version@");
    }

    @Override
    public String getRevision()
    {
        return "@git.describe@";
    }
}
