/* 
 * JBoss, Home of Professional Open Source 
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved. 
 * See the copyright.txt in the distribution for a 
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions 
 * of the GNU Lesser General Public License, v. 2.1. 
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
 * You should have received a copy of the GNU Lesser General Public License, 
 * v.2.1 along with this distribution; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 */

package org.switchyard.internal;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.switchyard.Property;
import org.switchyard.Scope;

/**
 *  Tests for context-related operations.
 */
public class DefaultContextTest {
    
    private static final String PROP_NAME = "foo";
    private static final String PROP_VAL= "bar";
    private DefaultContext _context;
    
    @Before
    public void setUp()  {
        _context = new DefaultContext();
    }
    
    @Test
    public void testGetSet() {
        _context.setProperty(PROP_NAME, PROP_VAL);
        Assert.assertEquals(PROP_VAL, _context.getProperty(PROP_NAME).getValue());
    }
    
    @Test
    public void testGetSetScoped() {
        _context.setProperty(PROP_NAME, PROP_VAL, Scope.IN);
        Assert.assertEquals(PROP_VAL, _context.getProperty(PROP_NAME, Scope.IN).getValue());
        Assert.assertNull(_context.getProperty(PROP_NAME));
        Assert.assertNull(_context.getProperty(PROP_NAME, Scope.OUT));
    }
    
    @Test
    public void testGetPropertyValue() {
        final String key = "prop";
        final String value = "exchange";
        _context.setProperty(key, value);
        _context.setProperty(key, "in", Scope.IN);
        _context.setProperty(key, "out", Scope.OUT);
        Assert.assertEquals(value, _context.getPropertyValue(key));
    }

    @Test
    public void testRemove() {
        _context.setProperty(PROP_NAME, PROP_VAL);
        Property p = _context.getProperty(PROP_NAME);
        Assert.assertEquals(PROP_VAL, p.getValue());
        _context.removeProperty(p);
        Assert.assertNull(_context.getProperty(PROP_NAME));
    }
    
    @Test
    public void testRemoveScopedProperites() {
        _context.setProperty("out", "bar", Scope.OUT);
        _context.setProperty("in", "foo", Scope.IN);
        _context.removeProperties(Scope.IN);
        Assert.assertNull(_context.getProperty("in", Scope.IN));
        Assert.assertNotNull(_context.getProperty("out", Scope.OUT));
    }
    
    @Test
    public void testRemoveProperites() {
        _context.setProperty("out", "bar", Scope.OUT);
        _context.setProperty("in", "foo", Scope.IN);
        _context.setProperty("ex", "psst", Scope.EXCHANGE);
        _context.removeProperties();
        Assert.assertNull(_context.getPropertyValue("in"));
        Assert.assertNull(_context.getPropertyValue("out"));
        Assert.assertNull(_context.getPropertyValue("ex"));
    }
    
    @Test
    public void testNullContextValue() {
        _context.setProperty(PROP_NAME, null);
        Property p = _context.getProperty(PROP_NAME);
        Assert.assertNotNull(p);
        Assert.assertNull(p.getValue());
    }
    
    @Test
    public void testSetPropertySet() {
        _context.setProperty("one", "bar");
        _context.setProperty("two", "foo");
        Set<Property> props = _context.getProperties(Scope.EXCHANGE);
        DefaultContext ctx = new DefaultContext();
        ctx.setProperties(props);
        Assert.assertNotNull(ctx.getPropertyValue("one"));
        Assert.assertNotNull(ctx.getPropertyValue("two"));
    }

    @Test
    public void testGetProperties() {
        _context.setProperty(PROP_NAME, PROP_VAL);
        Set<Property> props = _context.getProperties();
        Assert.assertTrue(props.size() == 1);
        Assert.assertEquals(PROP_VAL, props.iterator().next().getValue());
        
        // operations to the returned map should *not* be reflected in the context
        props.remove(PROP_NAME);
        Assert.assertTrue(_context.getProperties().size() == 1);
    }
    
    @Test
    public void testCopy() {
        _context.setProperty("exchange", "val", Scope.EXCHANGE);
        _context.setProperty("in", "val", Scope.IN);
        _context.setProperty("out", "val", Scope.OUT);
        DefaultContext ctx = _context.copy();
        // verify that all fields were copied
        Assert.assertEquals(
                _context.getProperty("exchange", Scope.EXCHANGE),
                ctx.getProperty("exchange", Scope.EXCHANGE));
        Assert.assertEquals(
                _context.getProperty("in", Scope.IN),
                ctx.getProperty("in", Scope.IN));
        Assert.assertEquals(
                _context.getProperty("out", Scope.OUT),
                ctx.getProperty("out", Scope.OUT));
        // verify that mods to one context do not impact the other
        _context.removeProperties(Scope.EXCHANGE);
        Assert.assertNull(_context.getProperty("exchange", Scope.EXCHANGE));
        Assert.assertNotNull(ctx.getProperty("exchange", Scope.EXCHANGE));
    }
    
}
