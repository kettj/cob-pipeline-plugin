/**
 * Copyright (c) 2012
 * Fraunhofer Institute for Manufacturing Engineering
 * and Automation (IPA)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 * - Neither the name of the Fraunhofer Institute for Manufacturing
 * Engineering and Automation (IPA) nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This program is free software: you can redistribute it and/or
 * modify
 * it under the terms of the GNU Lesser General Public License LGPL as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License LGPL for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License LGPL along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package de.fraunhofer.ipa;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.Descriptor.FormException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.util.List;
import java.util.ArrayList;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class RootRepositoryProperty extends RepositoryProperty {

	protected transient RootRepository rootRepository;
	
	private String suffix;
	
	private String fullName;
	
	@DataBoundConstructor
	public RootRepositoryProperty(String name) {
		super(name);
		this.fullName = fullName;
	}
	
	/*package*/ final void setRootRepository(RootRepository rootRepository) {
		this.rootRepository = rootRepository;
	}
	
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	@Extension
    public static class DescriptorImpl extends RepositoryPropertyDescriptor {
		@Override
        public String getDisplayName() {
            return "Root Repository Configurations";
        }
		
        public RepositoryProperty newInstance(Repository repository) {
            return new RootRepositoryProperty(repository.name);
        }
        
        /**
         * All {@link RepositoryDescriptor}s
         */
        public List<RepositoryPropertyDescriptor> getRepositoryDescriptors() {
        	List<RepositoryPropertyDescriptor> r = new ArrayList<RepositoryPropertyDescriptor>();
        	for (RepositoryPropertyDescriptor d : RepositoryProperty.all()) {
        		//TODO only add RepositoryDescriptors not Root
        		r.add(d);
        	}
        	return r;
        }
	}
	
    @Override
    public RootRepositoryProperty reconfigure(StaplerRequest req, JSONObject form) throws FormException {
    	req.bindJSON(this, form);
    	return this;
    }
    
    /**
     * Returns all the registered {@link RepositoryPropertyDescriptor}s.
     */
    //return only Root
    public static DescriptorExtensionList<RepositoryProperty, RepositoryPropertyDescriptor> all() {
        return Jenkins.getInstance().<RepositoryProperty, RepositoryPropertyDescriptor>getDescriptorList(RepositoryProperty.class);
    }
}
